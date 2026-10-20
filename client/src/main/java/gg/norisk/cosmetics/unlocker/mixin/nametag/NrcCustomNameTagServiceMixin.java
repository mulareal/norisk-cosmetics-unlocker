package gg.norisk.cosmetics.unlocker.mixin.nametag;

import gg.norisk.cosmetics.unlocker.cosmetics.LocalOutfitStorage;
import gg.norisk.cosmetics.v2.NRCPlusModule;
import gg.norisk.cosmetics.v2.sync.NrcCustomNameTagService;
import gg.norisk.networking.model.cosmetics.CustomNameTag;
import gg.norisk.networking.model.cosmetics.NameTagAnimationStyle;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(NrcCustomNameTagService.class)
public class NrcCustomNameTagServiceMixin {

    @Inject(method = "setAnimationsLocal", at = @At("HEAD"), cancellable = true, remap = false)
    private void nrc$setAnimationsLocalUnlocked(CustomNameTag nameTag, List<NameTagAnimationStyle> animations, CallbackInfo ci) {
        if (nameTag != null && animations != null) {
            nameTag.setStyleId(null);
            nameTag.getAnimations().clear();
            nameTag.getAnimations().addAll(animations);
            LocalOutfitStorage.saveCustomNameTag(nameTag);
            NRCPlusModule.INSTANCE.refreshNrcPlusAnimations();
        }
        ci.cancel();
    }

    @Inject(method = "setStyleIdLocal", at = @At("HEAD"), cancellable = true, remap = false)
    private void nrc$setStyleIdLocalUnlocked(CustomNameTag nameTag, String styleId, CallbackInfo ci) {
        if (nameTag != null) {
            nameTag.setStyleId(styleId);
            nameTag.getAnimations().clear();
            LocalOutfitStorage.saveCustomNameTag(nameTag);
            NRCPlusModule.INSTANCE.refreshNrcPlusAnimations();
        }
        ci.cancel();
    }

    @Inject(method = "setAnimations", at = @At("HEAD"), cancellable = true, remap = false)
    private void nrc$setAnimationsUnlocked(CustomNameTag nameTag, List<NameTagAnimationStyle> animations, Function1<? super Throwable, Unit> onError, CallbackInfo ci) {
        NrcCustomNameTagService.INSTANCE.setAnimationsLocal(nameTag, animations);
        ci.cancel();
    }

    @Inject(method = "setStyleId", at = @At("HEAD"), cancellable = true, remap = false)
    private void nrc$setStyleIdUnlocked(CustomNameTag nameTag, String styleId, Function1<? super Throwable, Unit> onError, CallbackInfo ci) {
        NrcCustomNameTagService.INSTANCE.setStyleIdLocal(nameTag, styleId);
        ci.cancel();
    }
}
