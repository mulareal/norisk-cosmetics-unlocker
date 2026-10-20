package gg.norisk.cosmetics.unlocker.mixin.nametag;

import gg.norisk.cosmetics.unlocker.cosmetics.CosmeticsUnlocker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(targets = "gg.norisk.cosmetics.v2.ui.plus.PlusExtraNameTag", remap = false)
public abstract class PlusExtraNameTagMixin {

    @ModifyVariable(method = "<init>(ZLgg/norisk/owolib/owo/ui/core/Sizing;Lgg/norisk/owolib/owo/ui/core/Sizing;Lgg/norisk/owolib/owo/ui/core/Sizing;)V", at = @At("HEAD"), argsOnly = true, ordinal = 0, remap = false)
    private static boolean unlockPlusExtraNameTag(boolean hasNrcPlus) {
        if (CosmeticsUnlocker.isEnabled()) {
            return true;
        }
        return hasNrcPlus;
    }
}
