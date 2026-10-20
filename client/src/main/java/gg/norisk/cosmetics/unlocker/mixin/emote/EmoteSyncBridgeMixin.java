package gg.norisk.cosmetics.unlocker.mixin.emote;

import gg.norisk.cosmetics.unlocker.api.ClientHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "gg.norisk.cosmetics.v2.sync.EmoteSyncBridge", remap = false)
public abstract class EmoteSyncBridgeMixin {

    @Inject(method = "playEmoteLocal", at = @At("HEAD"), remap = false)
    private void onPlayEmoteLocal(String emoteId, CallbackInfoReturnable<Boolean> cir) {
        if (emoteId != null && !emoteId.isEmpty()) {
            ClientHooks.localEmote(emoteId, true);
        }
    }

    @Inject(method = "stopEmoteLocal", at = @At("HEAD"), remap = false)
    private void onStopEmoteLocal(String emoteId, CallbackInfo ci) {
        if (emoteId != null && !emoteId.isEmpty()) {
            ClientHooks.localEmote(emoteId, false);
        }
    }

    @Inject(method = "stopAllEmotesLocal", at = @At("HEAD"), remap = false)
    private void onStopAllEmotesLocal(CallbackInfo ci) {
        ClientHooks.localEmote("all", false);
    }
}
