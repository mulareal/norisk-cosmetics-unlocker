package gg.norisk.cosmetics.unlocker.mixin.nametag;

import gg.norisk.networking.model.core.NoRiskUserMinimal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(NoRiskUserMinimal.class)
public class NoRiskUserMinimalMixin {

    /**
     * Unlocks NRC+ features (Custom animated nametags, extra nametag subtitles, AFK emotes).
     */
    @Inject(method = "isNoRiskPlus", at = @At("HEAD"), cancellable = true, remap = false)
    private void nrc$alwaysNoRiskPlus(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(true);
    }

    @Inject(method = "getNoRiskPlusExpirationDate", at = @At("RETURN"), cancellable = true, remap = false)
    private void nrc$ensureExpirationDate(CallbackInfoReturnable<Long> cir) {
        if (cir.getReturnValue() == null) {
            cir.setReturnValue(4102444800000L);
        }
    }
}
