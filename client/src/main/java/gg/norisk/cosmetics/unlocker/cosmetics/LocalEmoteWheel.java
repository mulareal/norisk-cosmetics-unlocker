package gg.norisk.cosmetics.unlocker.cosmetics;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import gg.norisk.compat.auth.NoriskAuth;
import gg.norisk.cosmetics.unlocker.Log;
import gg.norisk.cosmetics.v2.sync.EmoteWheelSyncService;
import gg.norisk.cosmetics.v2.ui.emote.wheel.EmoteWheelModule;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Emote wheel + AFK emote, saved locally. NoRisk loads the wheel from its backend on every start and drops
 * every emote the account doesn't really own, so the wheel was reset after each restart.
 */
public final class LocalEmoteWheel {

    private static final Gson GSON = new Gson();
    private static final long LOADED_AT = System.currentTimeMillis();
    private static volatile boolean restored; // until our wheel is applied, the module still holds NoRisk's

    private LocalEmoteWheel() {
    }

    /** Called when the player edits the wheel (NoRisk's pushNow). */
    public static void saveCurrent() {
        UUID uuid = NoriskAuth.INSTANCE.getPlayerUuid();
        if (uuid == null) return;
        if (!restored && Files.exists(file(uuid)) && System.currentTimeMillis() - LOADED_AT < 120_000) return;
        EmoteWheelModule module = EmoteWheelModule.INSTANCE;
        JsonObject json = new JsonObject();
        json.add("slots", GSON.toJsonTree(new ArrayList<>(module.getFavorites())));
        List<String> afk = module.getAutoAfkEmotes();
        json.addProperty("afk", afk.isEmpty() ? null : afk.get(0));
        json.addProperty("page", module.getLastOpenedPage());
        try {
            Path file = file(uuid);
            Files.createDirectories(file.getParent());
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(tmp, GSON.toJson(json), StandardCharsets.UTF_8);
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            Log.warn("Could not save emote wheel: " + e);
        }
    }

    /** Our saved wheel for the active account, or null if there is none yet. */
    public static EmoteWheelSyncService.WheelSnapshot load() {
        UUID uuid = NoriskAuth.INSTANCE.getPlayerUuid();
        if (uuid == null) return null;
        try {
            Path file = file(uuid);
            if (!Files.exists(file)) return null;
            JsonObject json = GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8), JsonObject.class);
            List<String> slots = new ArrayList<>();
            if (json.has("slots") && json.get("slots").isJsonArray()) {
                json.getAsJsonArray("slots").forEach(e -> slots.add(e.isJsonNull() ? null : e.getAsString()));
            }
            String afk = json.has("afk") && !json.get("afk").isJsonNull() ? json.get("afk").getAsString() : null;
            int page = json.has("page") ? json.get("page").getAsInt() : 0;
            restored = true;
            return new EmoteWheelSyncService.WheelSnapshot(slots, afk, page);
        } catch (Exception e) {
            Log.warn("Could not read emote wheel: " + e);
            return null;
        }
    }

    private static Path file(UUID uuid) {
        return FabricLoader.getInstance().getConfigDir().resolve("norisk-cosmetics-unlocker").resolve("emote-wheel-" + uuid + ".json");
    }
}
