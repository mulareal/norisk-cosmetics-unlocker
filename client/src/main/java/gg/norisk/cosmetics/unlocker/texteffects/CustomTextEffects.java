package gg.norisk.cosmetics.unlocker.texteffects;

import gg.norisk.abstraction.text.TextEffectRegistry;
import gg.norisk.cosmetics.unlocker.Log;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Own nametag text effects next to NoRisk's (Shaking, Rainbow, ...).
 * <p>
 * NoRisk's effects are bits in an int mask: {@link TextEffectRegistry} maps an id to a bit, and
 * {@code nrc_text_effects_v2.glsl} runs one GLSL function per set bit in {@code applyTextEffects()}.
 * We register our ids on high bits (NoRisk uses 0-16) and splice our GLSL into that include when the
 * shader manager loads it. Other players only see these effects if they have this mod too.
 * Switched off in the menu, the registry hides them (see TextEffectRegistryMixin).
 */
public final class CustomTextEffects {

    /** One effect: id (also the button label), bit, default colors, GLSL call in applyTextEffects(). */
    private record Effect(String id, int bit, int color1, int color2, String call) {
    }

    private static final List<Effect> EFFECTS = List.of(
            new Effect("BOUNCE", 24, 0, 0, "nrcu_bounce(SPEED, INTENSITY)"),
            new Effect("GLITCH", 25, 0, 0, "nrcu_glitch(SPEED, INTENSITY)"),
            new Effect("NEON", 26, 0x00F0FF, 0, "nrcu_neon(COLOR1, SPEED, INTENSITY)"),
            new Effect("RAINBOW_WAVE", 27, 0, 0, "nrcu_rainbow_wave(SPEED, INTENSITY)"),
            new Effect("SPARKS", 28, 0xFFBE3C, 0, "nrcu_sparks(COLOR1, SPEED, INTENSITY)"));

    private static final String SHADER = "/assets/norisk-cosmetics-unlocker/shaders/custom_text_effects.glsl";
    /** Marks the include we patch; our functions go right before it. */
    private static final String DISPATCH_START = "bool applyTextEffects() {";
    /** First line after NoRisk's last effect inside applyTextEffects(). */
    private static final String DISPATCH_END = "    if (Animations != 0) {";
    private static final String PATCHED = "nrcu_bounce(";

    private CustomTextEffects() {
    }

    /** True for the ids of our effects. */
    public static boolean isCustom(String id) {
        if (id == null) return false;
        for (Effect e : EFFECTS) {
            if (e.id().equals(id)) return true;
        }
        return false;
    }

    /** Adds our effects to the registry. NoRisk drops bits >= 16 on every catalog reload, so this runs again then. */
    public static void register() {
        for (Effect e : EFFECTS) {
            TextEffectRegistry.INSTANCE.register(e.id(), e.bit(), e.color1(), e.color2(), 1.0F, 1.0F, true);
        }
    }

    /** Returns the include source with our effects added, or {@code source} unchanged if it is not NoRisk's effect include. */
    public static String patchShader(String source) {
        if (source == null || source.contains(PATCHED)) return source;
        int start = source.indexOf(DISPATCH_START);
        int end = source.indexOf(DISPATCH_END, Math.max(start, 0));
        if (start < 0) return source;
        if (end < 0) {
            Log.warn("Text effect shader changed, custom effects disabled");
            return source;
        }
        String functions = loadFunctions();
        if (functions == null) return source;

        StringBuilder dispatch = new StringBuilder();
        for (Effect e : EFFECTS) {
            dispatch.append("    CHECK_DO_EFFECT(1 << ").append(e.bit()).append(", ").append(e.call()).append(");\n");
        }
        Log.info("Custom text effects added to shader (" + EFFECTS.size() + ")");
        return source.substring(0, start) + functions + "\n" + source.substring(start, end)
                + dispatch + "\n" + source.substring(end);
    }

    private static String loadFunctions() {
        try (InputStream in = CustomTextEffects.class.getResourceAsStream(SHADER)) {
            if (in == null) throw new IOException("missing " + SHADER);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            Log.warn("Custom text effects not loaded: " + e);
            return null;
        }
    }
}
