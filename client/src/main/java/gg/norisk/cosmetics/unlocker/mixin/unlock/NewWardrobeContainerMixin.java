package gg.norisk.cosmetics.unlocker.mixin.unlock;

import gg.norisk.cosmetics.unlocker.cosmetics.CosmeticsUnlocker;
import gg.norisk.cosmetics.unlocker.cosmetics.LocalOutfitStorage;
import gg.norisk.cosmetics.unlocker.api.ClientHooks;
import gg.norisk.cosmetics.unlocker.settings.Features;
import gg.norisk.cosmetics.v2.model.NrcCosmetic;
import gg.norisk.cosmetics.v2.player.NrcCosmeticPlayerData;
import gg.norisk.cosmetics.v2.ui.newwardrobe.NewWardrobeContainer;
import gg.norisk.networking.model.cosmetics.CosmeticOutfit;
import gg.norisk.networking.model.cosmetics.CosmeticSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.*;

@Mixin(value = NewWardrobeContainer.class, remap = false)
public abstract class NewWardrobeContainerMixin {

    @Shadow
    public abstract NrcCosmeticPlayerData getPreviewData();

    @Inject(method = "getOwnedItems", at = @At("RETURN"), cancellable = true, remap = false)
    private void unlockGetOwnedItems(CallbackInfoReturnable<List<UUID>> cir) {
        if (Features.COSMETICS.on()) {
            List<UUID> current = cir.getReturnValue();
            Set<UUID> all = new HashSet<>(current != null ? current : Collections.emptyList());
            all.addAll(CosmeticsUnlocker.getAllCosmeticIds());
            cir.setReturnValue(new ArrayList<>(all));
        }
    }

    @Inject(method = "hasTempItem", at = @At("HEAD"), cancellable = true, remap = false)
    private void overrideHasTempItem(UUID itemId, CallbackInfoReturnable<Boolean> cir) {
        if (Features.COSMETICS.on()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "toggleCosmetic", at = @At("TAIL"), remap = false)
    private void onToggleCosmetic(NrcCosmetic cosmetic, CosmeticSettings settings, boolean toggle, boolean removeVariants, boolean wasVariantClick, CallbackInfo ci) {
        if (Features.COSMETICS.on()) {
            try {
                CosmeticOutfit current = this.getPreviewData().getCurrentOutfit();
                if (current != null) {
                    LocalOutfitStorage.saveOutfit(current);
                    ClientHooks.outfitChanged();
                }
            } catch (Throwable ignored) {
            }
        }
    }

    @Inject(method = "selectOutfit", at = @At("TAIL"), remap = false)
    private void onSelectOutfit(CosmeticOutfit nextOutfit, boolean activate, CallbackInfo ci) {
        if (Features.COSMETICS.on() && nextOutfit != null) {
            try {
                LocalOutfitStorage.saveOutfit(nextOutfit);
                ClientHooks.outfitChanged();
            } catch (Throwable ignored) {
            }
        }
    }
}
