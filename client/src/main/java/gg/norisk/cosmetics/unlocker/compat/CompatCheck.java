package gg.norisk.cosmetics.unlocker.compat;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import gg.norisk.cosmetics.unlocker.Log;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Detects NoRisk updates that broke our hooks. Mixins use {@code defaultRequire: 0}, so a renamed
 * class or method would otherwise fail silently.
 * <ul>
 *   <li>Every mixin in the mixin config is checked automatically: its target classes and the methods
 *       its injectors name ({@code @Inject(method = ...)} etc.) must exist.</li>
 *   <li>NoRisk APIs called directly (not through a mixin) are registered with {@link #require}.</li>
 * </ul>
 */
public final class CompatCheck {

    private static final String MIXIN_CONFIG = "noriskcosmetics.mixins.json";
    private static final String MIXIN = "Lorg/spongepowered/asm/mixin/Mixin;";

    private static final Set<String> required = new LinkedHashSet<>();
    private static volatile List<String> issues;

    private CompatCheck() {
    }

    /** Registers direct API uses as {@code "pkg.Class"} or {@code "pkg.Class#method"}. */
    public static synchronized void require(String... targets) {
        required.addAll(List.of(targets));
        issues = null;
    }

    /** Missing hooks (empty = all fine). Computed on first use. */
    public static List<String> issues() {
        List<String> result = issues;
        if (result == null) {
            result = run();
            issues = result;
        }
        return result;
    }

    private static synchronized List<String> run() {
        Set<String> targets = new LinkedHashSet<>(required);
        try {
            targets.addAll(mixinTargets());
        } catch (Throwable t) {
            Log.warn("Could not read mixin targets: " + t);
        }
        List<String> missing = new ArrayList<>();
        ClassLoader loader = CompatCheck.class.getClassLoader();
        for (String target : targets) {
            int hash = target.indexOf('#');
            String className = hash < 0 ? target : target.substring(0, hash);
            String method = hash < 0 ? null : target.substring(hash + 1);
            try {
                Class<?> clazz = Class.forName(className, false, loader);
                if (method != null && !hasMethod(clazz, method)) missing.add(shortName(target));
            } catch (Throwable t) {
                missing.add(shortName(target));
            }
        }
        if (!missing.isEmpty()) Log.warn("Missing NoRisk hooks: " + missing);
        return missing;
    }

    /** "Class" and "Class#method" for every injector of every mixin in our mixin config. */
    private static Set<String> mixinTargets() throws Exception {
        ClassLoader loader = CompatCheck.class.getClassLoader();
        JsonObject config;
        try (InputStream in = loader.getResourceAsStream(MIXIN_CONFIG)) {
            if (in == null) return Set.of();
            config = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        }
        String pkg = config.get("package").getAsString();
        Set<String> result = new LinkedHashSet<>();
        for (String side : new String[]{"mixins", "client"}) {
            if (!config.has(side)) continue;
            for (JsonElement name : config.getAsJsonArray(side)) {
                ClassNode mixin = readClass(loader, pkg + "." + name.getAsString());
                if (mixin != null) addTargets(mixin, result);
            }
        }
        return result;
    }

    private static ClassNode readClass(ClassLoader loader, String className) throws Exception {
        try (InputStream in = loader.getResourceAsStream(className.replace('.', '/') + ".class")) {
            if (in == null) return null;
            ClassNode node = new ClassNode();
            new ClassReader(in).accept(node, ClassReader.SKIP_CODE | ClassReader.SKIP_FRAMES);
            return node;
        }
    }

    private static void addTargets(ClassNode mixin, Set<String> result) {
        List<String> classes = new ArrayList<>();
        for (AnnotationNode annotation : annotations(mixin.visibleAnnotations, mixin.invisibleAnnotations)) {
            if (!MIXIN.equals(annotation.desc) || annotation.values == null) continue;
            for (int i = 0; i + 1 < annotation.values.size(); i += 2) {
                String key = (String) annotation.values.get(i);
                if (!(annotation.values.get(i + 1) instanceof List<?> list)) continue;
                for (Object entry : list) {
                    if ("value".equals(key) && entry instanceof Type type) classes.add(type.getClassName());
                    if ("targets".equals(key) && entry instanceof String target) classes.add(target.replace('/', '.'));
                }
            }
        }
        result.addAll(classes);
        for (MethodNode method : mixin.methods) {
            for (AnnotationNode annotation : annotations(method.visibleAnnotations, method.invisibleAnnotations)) {
                for (String targetMethod : injectorMethods(annotation)) {
                    for (String clazz : classes) result.add(clazz + "#" + targetMethod);
                }
            }
        }
    }

    /** Method names from an injector's {@code method} attribute, without descriptors. */
    private static List<String> injectorMethods(AnnotationNode annotation) {
        List<String> names = new ArrayList<>();
        if (annotation.values == null) return names;
        for (int i = 0; i + 1 < annotation.values.size(); i += 2) {
            if (!"method".equals(annotation.values.get(i))) continue;
            if (annotation.values.get(i + 1) instanceof List<?> list) {
                for (Object entry : list) {
                    if (entry instanceof String name) {
                        int paren = name.indexOf('(');
                        names.add(paren >= 0 ? name.substring(0, paren) : name);
                    }
                }
            }
        }
        return names;
    }

    private static List<AnnotationNode> annotations(List<AnnotationNode> visible, List<AnnotationNode> invisible) {
        List<AnnotationNode> all = new ArrayList<>();
        if (visible != null) all.addAll(visible);
        if (invisible != null) all.addAll(invisible);
        return all;
    }

    private static boolean hasMethod(Class<?> clazz, String name) {
        if (name.equals("<init>")) return clazz.getDeclaredConstructors().length > 0;
        for (Method m : clazz.getDeclaredMethods()) {
            if (m.getName().equals(name)) return true;
        }
        return false;
    }

    private static String shortName(String target) {
        int hash = target.indexOf('#');
        int dot = target.lastIndexOf('.', hash < 0 ? target.length() : hash);
        return target.substring(dot + 1);
    }
}
