package gg.norisk.cosmetics.unlocker.mixin.unlock;

import gg.norisk.cosmetics.unlocker.cosmetics.CosmeticsUnlocker;
import gg.norisk.cosmetics.v2.ui.IWardrobeContainer;
import gg.norisk.cosmetics.v2.ui.ShopUtils;
import gg.norisk.cosmetics.v2.ui.shop.ShopItemInfo;
import gg.norisk.networking.model.shop.ShopItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;
import java.util.UUID;

@Mixin(value = ShopUtils.class, remap = false)
public abstract class ShopUtilsMixin {

    @Inject(method = "isEffectivelyOwned", at = @At("HEAD"), cancellable = true, remap = false)
    private void unlockIsEffectivelyOwned(ShopItemInfo item, Collection<UUID> ownedItems, CallbackInfoReturnable<Boolean> cir) {
        if (CosmeticsUnlocker.isEnabled()) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "canSeeShopItem", at = @At("HEAD"), cancellable = true, remap = false)
    private void unlockCanSeeShopItem(ShopItem shopItem, IWardrobeContainer shop, CallbackInfoReturnable<Boolean> cir) {
        if (CosmeticsUnlocker.isEnabled()) {
            cir.setReturnValue(true);
        }
    }
}
