package gg.norisk.cosmetics.unlocker.mixin.unlock;

import gg.norisk.cosmetics.unlocker.shop.ShopCatalog;
import gg.norisk.cosmetics.unlocker.shop.ShopCatalogBridge;
import gg.norisk.cosmetics.v2.ui.newwardrobe.NewPlayerPreviewPanel;
import gg.norisk.owolib.owo.ui.core.ParentUIComponent;
import gg.norisk.owolib.owo.ui.core.UIComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The wardrobe's player preview also wears the items selected from the optional catalog. */
@Mixin(NewPlayerPreviewPanel.class)
public abstract class NewPlayerPreviewPanelMixin {

    @Shadow
    public abstract long getPreviewEntityId();

    @Inject(method = "mount", at = @At("RETURN"))
    private void nrc$attachCatalogOutfit(ParentUIComponent parent, double x, double y, CallbackInfo ci) {
        ShopCatalogBridge bridge = ShopCatalog.get();
        if (bridge != null) bridge.attachOutfitPreview(getPreviewEntityId());
    }

    @Inject(method = "dismount", at = @At("RETURN"))
    private void nrc$detachCatalogOutfit(UIComponent.DismountReason reason, CallbackInfo ci) {
        if (reason != UIComponent.DismountReason.REMOVED) return;
        ShopCatalogBridge bridge = ShopCatalog.get();
        if (bridge != null) bridge.detachPreview(getPreviewEntityId());
    }
}
