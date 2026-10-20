package gg.norisk.cosmetics.unlocker.mixin.nametag;

import gg.norisk.cosmetics.unlocker.api.ClientHooks;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * The NRC+ nametag editor only allows 3 animation effects at once ({@code animations.size() >= 3}).
 * Lifts that limit for players allowed by {@link ClientHooks#unlimitedNametag} (nobody by default).
 */
@Mixin(targets = "gg.norisk.cosmetics.v2.ui.plus.PlusSettingsComponent$PlusNameTag", remap = false)
public abstract class PlusNameTagLimitMixin {

    @Unique
    private static int nrc$limit(int original) {
        try {
            if (ClientHooks.unlimitedNametag.test(Minecraft.getInstance().getUser().getName())) {
                return Integer.MAX_VALUE;
            }
        } catch (Throwable ignored) {
        }
        return original;
    }

    // Toggle handler of an effect button: "if (animations.size() >= 3) return" + disabled state
    @ModifyConstant(method = "lambda$20$lambda$19$lambda$14", constant = @Constant(intValue = 3), remap = false)
    private static int nrc$liftToggleLimit(int original) {
        return nrc$limit(original);
    }

    // Re-evaluates which effect buttons are greyed out
    @ModifyConstant(method = "_init_$refreshSelectionStates", constant = @Constant(intValue = 3), remap = false)
    private static int nrc$liftRefreshLimit(int original) {
        return nrc$limit(original);
    }

    // Initial disabled state in the constructor (the other 3s there are layout gaps, so only ordinal 1)
    @ModifyConstant(method = "<init>", constant = @Constant(intValue = 3, ordinal = 1), remap = false)
    private int nrc$liftInitialLimit(int original) {
        return nrc$limit(original);
    }
}
