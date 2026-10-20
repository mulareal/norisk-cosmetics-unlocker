package gg.norisk.cosmetics.unlocker.mixin.texteffect;

import gg.norisk.abstraction.text.AnimationStyle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Speed/intensity/colors are uploaded in list order, but the shader reads them in bit order.
 * NoRisk's own effects are usually added in bit order; ours (high bits) can end up anywhere, so sort.
 */
@Mixin(targets = "gg.norisk.abstraction.text.shader.TextShaderUniforms", remap = false)
public abstract class TextShaderUniformsMixin {

    private static final Comparator<AnimationStyle> BY_BIT = Comparator.comparingInt(TextShaderUniformsMixin::order);

    @ModifyVariable(method = "byAnimationStyle", at = @At("HEAD"), argsOnly = true, remap = false)
    private static List<AnimationStyle> nrc$sortByBit(List<AnimationStyle> styles) {
        if (styles == null || styles.size() < 2) return styles;
        for (int i = 1; i < styles.size(); i++) {
            if (order(styles.get(i - 1)) > order(styles.get(i))) {
                List<AnimationStyle> sorted = new ArrayList<>(styles);
                sorted.sort(BY_BIT);
                return sorted;
            }
        }
        return styles;
    }

    /** Bit index; unknown effects (mask 0) last, they take no slot in the shader. */
    private static int order(AnimationStyle style) {
        int mask = style.getMask();
        return mask == 0 ? Integer.MAX_VALUE : Integer.numberOfTrailingZeros(mask);
    }
}
