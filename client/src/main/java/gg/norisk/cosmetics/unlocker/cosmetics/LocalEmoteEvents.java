package gg.norisk.cosmetics.unlocker.cosmetics;

import gg.norisk.animations.emote.GeoEmoteManager;
import gg.norisk.cosmetics.unlocker.Log;
import gg.norisk.cosmetics.unlocker.api.ClientHooks;
import gg.norisk.cosmetics.unlocker.compat.CompatCheck;
import gg.norisk.cosmetics.v2.sync.CosmeticSyncEvents;
import kotlin.Unit;
import net.minecraft.client.Minecraft;

import java.util.UUID;

/**
 * Turns NoRisk's own emote events for the local player into {@link ClientHooks#LOCAL_EMOTE}.
 * The emote mixins fire the same hook; listeners should ignore quick duplicates.
 */
public final class LocalEmoteEvents {

    private LocalEmoteEvents() {
    }

    public static void init() {
        CompatCheck.require(
                "gg.norisk.cosmetics.v2.sync.CosmeticSyncEvents#getEmoteStarted",
                "gg.norisk.cosmetics.v2.sync.CosmeticSyncEvents#getEmoteStopped",
                "gg.norisk.animations.emote.GeoEmoteManager#addOnAllEmotesStoppedListener",
                "gg.norisk.animations.emote.GeoEmoteManager#addOnEmoteNaturalStopListener");
        try {
            CosmeticSyncEvents.INSTANCE.getEmoteStarted().listen(event -> {
                if (isLocal(event.getSenderUuid())) ClientHooks.localEmote(event.getEmoteId(), true);
                return Unit.INSTANCE;
            });
            CosmeticSyncEvents.INSTANCE.getEmoteStopped().listen(event -> {
                if (isLocal(event.getSenderUuid())) ClientHooks.localEmote(event.getEmoteId(), false);
                return Unit.INSTANCE;
            });
            GeoEmoteManager.INSTANCE.addOnAllEmotesStoppedListener((playerId, interrupted) -> {
                if (isLocal(playerId)) ClientHooks.localEmote("all", false);
                return Unit.INSTANCE;
            });
            GeoEmoteManager.INSTANCE.addOnEmoteNaturalStopListener((playerId, emoteId) -> {
                if (isLocal(playerId)) ClientHooks.localEmote(emoteId, false);
                return Unit.INSTANCE;
            });
        } catch (Throwable t) {
            Log.warn("Could not listen to NoRisk emote events: " + t);
        }
    }

    private static boolean isLocal(UUID uuid) {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && uuid != null && uuid.equals(mc.player.getUUID());
    }

    /** NoRisk passes entity ids as Long. */
    private static boolean isLocal(Long entityId) {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && entityId != null && mc.player.getId() == entityId.intValue();
    }
}
