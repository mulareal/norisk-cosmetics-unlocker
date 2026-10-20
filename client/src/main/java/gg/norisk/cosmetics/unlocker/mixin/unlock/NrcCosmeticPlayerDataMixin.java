package gg.norisk.cosmetics.unlocker.mixin.unlock;

import gg.norisk.cosmetics.unlocker.cosmetics.CosmeticsUnlocker;
import gg.norisk.cosmetics.v2.model.NrcCosmetic;
import gg.norisk.cosmetics.v2.player.NrcCosmeticPlayerData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;
import java.util.UUID;

@Mixin(value = NrcCosmeticPlayerData.class, remap = false)
public abstract class NrcCosmeticPlayerDataMixin {

    @Unique
    private Set<UUID> nrc$filledSet;

    @Unique
    private int nrc$filledSize = -1;

    @Unique
    private int nrc$filledAllSize = -1;

    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true, remap = false)
    private void unlockShouldRender(NrcCosmetic cosmetic, CallbackInfoReturnable<Boolean> cir) {
        if (CosmeticsUnlocker.isEnabled() && cosmetic != null) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "getOwnedCosmeticIds", at = @At("RETURN"), cancellable = true, remap = false)
    private void unlockGetOwnedCosmeticIds(CallbackInfoReturnable<Set<UUID>> cir) {
        if (CosmeticsUnlocker.isEnabled()) {
            Set<UUID> set = cir.getReturnValue();
            if (set != null) {
                // Called per player per frame: only re-add when the set or the unlocked list changed
                Set<UUID> all = CosmeticsUnlocker.getAllCosmeticIds();
                if (set == nrc$filledSet && set.size() == nrc$filledSize && all.size() == nrc$filledAllSize) return;
                set.addAll(all);
                nrc$filledSet = set;
                nrc$filledSize = set.size();
                nrc$filledAllSize = all.size();
            }
        }
    }
}
