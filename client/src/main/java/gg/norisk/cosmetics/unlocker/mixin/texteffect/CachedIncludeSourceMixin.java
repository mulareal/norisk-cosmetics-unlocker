package gg.norisk.cosmetics.unlocker.mixin.texteffect;

import gg.norisk.cosmetics.unlocker.texteffects.CustomTextEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Every shader include passes through here; NoRisk's text effect include gets our effects spliced in. */
@Mixin(targets = "com.mojang.renderpearl.api.pipeline.ShaderSource$CachedIncludeSource")
public abstract class CachedIncludeSourceMixin {

    @ModifyVariable(method = "create", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private static String nrc$addCustomTextEffects(String source) {
        return CustomTextEffects.patchShader(source);
    }
}
