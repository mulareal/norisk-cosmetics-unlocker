package gg.norisk.cosmetics.unlocker.settings;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import gg.norisk.cosmetics.unlocker.Log;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * On/off switches of the mod, saved in {@code config/norisk-cosmetics-unlocker/settings.json}.
 * Shown in NoRisk's mod menu through {@link ToggleModule}s; addons add their own with {@link #toggle}.
 */
public final class ModSettings {

    private static final Gson GSON = new Gson();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir()
            .resolve("norisk-cosmetics-unlocker").resolve("settings.json");

    private static final Map<String, Toggle> toggles = new LinkedHashMap<>();
    private static JsonObject stored;

    /** The whole mod. Every other toggle is off while this is off. */
    public static final Toggle ENABLED = new Toggle("enabled", "Enabled", "", true, null);

    private ModSettings() {
    }

    /** Registers a switch (once per key) and returns it with its saved value. */
    public static synchronized Toggle toggle(String key, String label, String description, boolean defaultValue) {
        Toggle existing = toggles.get(key);
        if (existing != null) return existing;
        Toggle toggle = new Toggle(key, label, description, defaultValue, ENABLED);
        toggles.put(key, toggle);
        return toggle;
    }

    private static synchronized boolean storedValue(String key, boolean fallback) {
        if (stored == null) stored = read();
        JsonElement value = stored.get(key);
        return value != null && value.isJsonPrimitive() ? value.getAsBoolean() : fallback;
    }

    private static synchronized void save(Toggle changed) {
        if (stored == null) stored = read();
        stored.addProperty(changed.key, changed.value);
        try {
            Files.createDirectories(FILE.getParent());
            Path tmp = FILE.resolveSibling("settings.json.tmp");
            Files.writeString(tmp, GSON.toJson(stored), StandardCharsets.UTF_8);
            Files.move(tmp, FILE, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (Exception e) {
            Log.warn("Settings not saved: " + e);
        }
    }

    private static JsonObject read() {
        try {
            if (Files.exists(FILE)) {
                JsonObject json = GSON.fromJson(Files.readString(FILE, StandardCharsets.UTF_8), JsonObject.class);
                if (json != null) return json;
            }
        } catch (Exception e) {
            Log.warn("Settings unreadable, using defaults: " + e);
        }
        return new JsonObject();
    }

    /** One persistent switch. */
    public static final class Toggle {
        private final String key;
        private final String label;
        private final String description;
        private final boolean defaultValue;
        private final Toggle parent;
        private volatile String info;
        private volatile boolean value;

        private Toggle(String key, String label, String description, boolean defaultValue, Toggle parent) {
            this.key = key;
            this.label = label;
            this.description = description;
            this.defaultValue = defaultValue;
            this.parent = parent;
            this.value = storedValue(key, defaultValue);
        }

        /** True if switched on and the mod itself is enabled. Use this for feature checks. */
        public boolean on() {
            return value && (parent == null || parent.on());
        }

        /** The switch itself, ignoring the mod switch. */
        public boolean get() {
            return value;
        }

        public void set(boolean newValue) {
            if (value == newValue) return;
            value = newValue;
            save(this);
        }

        public String key() {
            return key;
        }

        public String label() {
            return label;
        }

        public String description() {
            return description;
        }

        public boolean defaultValue() {
            return defaultValue;
        }

        /**
         * Text behind the "i" next to the switch, for switches whose label doesn't say enough. English here;
         * other languages through the lang key {@code nrcu.setting.<key>.info}.
         */
        public Toggle withInfo(String info) {
            this.info = info;
            return this;
        }

        /** Info text in English, or {@code null}. */
        public String info() {
            return info;
        }
    }
}
