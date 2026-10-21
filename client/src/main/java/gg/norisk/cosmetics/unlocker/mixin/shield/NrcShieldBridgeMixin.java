package gg.norisk.cosmetics.unlocker.mixin.shield;

import gg.norisk.animations.model.GeoModel;
import gg.norisk.animations.resource.GeoResourceLocation;
import gg.norisk.cosmetics.unlocker.cosmetics.HeldShieldDraw;
import gg.norisk.cosmetics.v2.shield.NrcShieldBridge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Answers for the add-on shield while NoRisk's shield model is set up with it (see CosmeticItemReplaceModelMixin). */
@Mixin(value = NrcShieldBridge.class, remap = false)
public abstract class NrcShieldBridgeMixin {

    @Inject(method = "hasActiveShield", at = @At("HEAD"), cancellable = true)
    private void nrc$hasShield(Object player, CallbackInfoReturnable<Boolean> cir) {
        if (HeldShieldDraw.current != null) cir.setReturnValue(true);
    }

    @Inject(method = "getShieldModel", at = @At("HEAD"), cancellable = true)
    private void nrc$model(Object player, CallbackInfoReturnable<GeoModel> cir) {
        var shield = HeldShieldDraw.current;
        if (shield != null) cir.setReturnValue(shield.model());
    }

    @Inject(method = "getShieldTexture", at = @At("HEAD"), cancellable = true)
    private void nrc$texture(Object player, CallbackInfoReturnable<GeoResourceLocation> cir) {
        var shield = HeldShieldDraw.current;
        if (shield != null) cir.setReturnValue(shield.texture());
    }

    @Inject(method = "getShieldGlowTexture", at = @At("HEAD"), cancellable = true)
    private void nrc$glow(Object player, CallbackInfoReturnable<GeoResourceLocation> cir) {
        if (HeldShieldDraw.current != null) cir.setReturnValue(null);
    }

    @Inject(method = "getShieldTransformJson", at = @At("HEAD"), cancellable = true)
    private void nrc$transform(Object player, boolean blocking, CallbackInfoReturnable<String> cir) {
        var shield = HeldShieldDraw.current;
        if (shield != null) cir.setReturnValue(blocking ? shield.blockingTransform() : shield.transform());
    }
}
