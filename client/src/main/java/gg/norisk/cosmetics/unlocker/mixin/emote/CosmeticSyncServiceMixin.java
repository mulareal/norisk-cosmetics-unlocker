package gg.norisk.cosmetics.unlocker.mixin.emote;

import gg.norisk.cosmetics.unlocker.api.ClientHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = {
        "gg.norisk.cosmetics.v2.sync.CosmeticSyncServiceOnline",
        "gg.norisk.cosmetics.v2.sync.CosmeticSyncServiceOffline"
}, remap = false)
public abstract class CosmeticSyncServiceMixin {

    @Inject(method = "sendEmoteStart", at = @At("HEAD"), remap = false)
    private void onSendEmoteStart(String emoteId, CallbackInfo ci) {
        if (emoteId != null && !emoteId.isEmpty()) {
            try {
                ClientHooks.localEmote(emoteId, true);
            } catch (Throwable ignored) {
            }
        }
    }

    @Inject(method = "sendEmoteStop", at = @At("HEAD"), remap = false)
    private void onSendEmoteStop(String emoteId, CallbackInfo ci) {
        if (emoteId != null && !emoteId.isEmpty()) {
            try {
                ClientHooks.localEmote(emoteId, false);
            } catch (Throwable ignored) {
            }
        }
    }
}
