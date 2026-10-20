package gg.norisk.cosmetics.unlocker.mixin.unlock;

import gg.norisk.compat.auth.NrcPermissionCache;
import gg.norisk.cosmetics.unlocker.cosmetics.CosmeticsUnlocker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = NrcPermissionCache.class, remap = false)
public class NrcPermissionCacheMixin {

    @Inject(method = "has", at = @At("HEAD"), cancellable = true, remap = false)
    private void nrc$unlockAllPermissions(String node, CallbackInfoReturnable<Boolean> cir) {
        if (CosmeticsUnlocker.isEnabled()) {
            cir.setReturnValue(true);
        }
    }
}
