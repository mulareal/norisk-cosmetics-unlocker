package gg.norisk.cosmetics.unlocker.mixin.texteffect;

import gg.norisk.abstraction.text.NrcTextEffect;
import gg.norisk.cosmetics.unlocker.settings.Features;
import gg.norisk.cosmetics.unlocker.texteffects.CustomTextEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

/** Keeps our effects registered and hides them while they are switched off in the menu. */
@Mixin(targets = "gg.norisk.abstraction.text.TextEffectRegistry", remap = false)
public abstract class TextEffectRegistryMixin {

    @Inject(method = "resetDataDriven", at = @At("TAIL"), remap = false)
    private void nrc$keepCustomEffects(CallbackInfo ci) {
        CustomTextEffects.register(); // resetDataDriven() keeps only bits below 16
    }

    @Inject(method = "byId", at = @At("HEAD"), cancellable = true, remap = false)
    private void nrc$hideWhenOff(String id, CallbackInfoReturnable<NrcTextEffect> cir) {
        if (!Features.TEXT_EFFECTS.on() && CustomTextEffects.isCustom(id)) cir.setReturnValue(null);
    }

    @Inject(method = "selectable", at = @At("RETURN"), cancellable = true, remap = false)
    private void nrc$hideButtonsWhenOff(CallbackInfoReturnable<List<NrcTextEffect>> cir) {
        if (Features.TEXT_EFFECTS.on()) return;
        List<NrcTextEffect> visible = new ArrayList<>(cir.getReturnValue());
        visible.removeIf(effect -> CustomTextEffects.isCustom(effect.getId()));
        cir.setReturnValue(visible);
    }
}
