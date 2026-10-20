package gg.norisk.cosmetics.unlocker.mixin.unlock;

import gg.norisk.cosmetics.unlocker.cosmetics.CosmeticsUnlocker;
import gg.norisk.cosmetics.v2.ui.WardrobeCache;
import gg.norisk.networking.model.cosmetics.api.WardrobeResponse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = WardrobeCache.class, remap = false)
public abstract class WardrobeCacheMixin {

    @Inject(method = "getCached", at = @At("RETURN"), remap = false)
    private void onGetCached(CallbackInfoReturnable<WardrobeResponse> cir) {
        if (CosmeticsUnlocker.isEnabled()) {
            WardrobeResponse response = cir.getReturnValue();
            if (response != null) {
                CosmeticsUnlocker.populateWardrobeResponse(response);
            }
        }
    }

    @Inject(method = "setCached", at = @At("HEAD"), remap = false)
    private void onSetCached(WardrobeResponse response, CallbackInfo ci) {
        if (CosmeticsUnlocker.isEnabled() && response != null) {
            CosmeticsUnlocker.populateWardrobeResponse(response);
        }
    }
}
