package gg.norisk.cosmetics.unlocker.mixin.emote;

import gg.norisk.cosmetics.unlocker.api.ClientHooks;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

/**
 * Partner (interactive) emotes: NoRisk's backend refuses unowned emotes. If an addon provides a
 * {@link ClientHooks.PartnerEmoteRouter} that is active, start/join/leave go there instead; it must answer
 * with the same session events NoRisk's backend would send.
 */
@Mixin(targets = {
        "gg.norisk.cosmetics.v2.sync.CosmeticSyncServiceOnline",
        "gg.norisk.cosmetics.v2.sync.CosmeticSyncServiceOffline"
}, remap = false)
public abstract class InteractiveEmoteServiceMixin {

    @Inject(method = "startInteractiveEmote", at = @At("HEAD"), cancellable = true, remap = false)
    private void nrc$start(UUID emoteUuid, int maxInteractions, int randomCount, Function0<Unit> onRefused, CallbackInfo ci) {
        try {
            ClientHooks.PartnerEmoteRouter router = ClientHooks.partnerEmotes;
            if (emoteUuid != null && router != null && router.isActive()) {
                router.start(emoteUuid, maxInteractions, randomCount);
                ci.cancel();
            }
        } catch (Throwable ignored) {
        }
    }

    @Inject(method = "joinInteractiveEmote", at = @At("HEAD"), cancellable = true, remap = false)
    private void nrc$join(String sessionId, CallbackInfo ci) {
        try {
            ClientHooks.PartnerEmoteRouter router = ClientHooks.partnerEmotes;
            if (sessionId != null && router != null && router.isActive()) {
                router.join(sessionId);
                ci.cancel();
            }
        } catch (Throwable ignored) {
        }
    }

    @Inject(method = "leaveInteractiveEmote", at = @At("HEAD"), cancellable = true, remap = false)
    private void nrc$leave(String sessionId, CallbackInfo ci) {
        try {
            ClientHooks.PartnerEmoteRouter router = ClientHooks.partnerEmotes;
            if (sessionId != null && router != null && router.isActive()) {
                router.leave(sessionId);
                ci.cancel();
            }
        } catch (Throwable ignored) {
        }
    }
}
