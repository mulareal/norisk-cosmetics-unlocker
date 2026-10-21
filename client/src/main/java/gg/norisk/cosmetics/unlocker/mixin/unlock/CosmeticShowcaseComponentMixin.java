package gg.norisk.cosmetics.unlocker.mixin.unlock;

import gg.norisk.cosmetics.unlocker.settings.Features;
import gg.norisk.cosmetics.unlocker.shop.ShopCatalog;
import gg.norisk.cosmetics.unlocker.shop.ShopCatalogBridge;
import gg.norisk.cosmetics.v2.ui.cosmetic.CosmeticShowcaseComponent;
import gg.norisk.owolib.owo.ui.base.BaseUIComponent;
import gg.norisk.networking.model.cosmetics.CosmeticOutfit;
import gg.norisk.owolib.owo.ui.core.OwoUIGraphics;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CosmeticShowcaseComponent.class)
public abstract class CosmeticShowcaseComponentMixin extends BaseUIComponent {

    @Shadow
    @Final
    private CosmeticOutfit outfit;

    @Shadow
    public abstract long getPreviewEntityId();

    /** Outfit cards also show the catalog items saved with their outfit. */
    @Inject(method = "trackPlayer", at = @At("RETURN"))
    private void nrc$attachCatalogOutfit(CallbackInfo ci) {
        ShopCatalogBridge bridge = ShopCatalog.get();
        if (bridge != null && outfit != null && outfit.getOutfitId() != null)
            bridge.attachOutfitPreview(getPreviewEntityId(), outfit.getOutfitId().toHexString());
    }

    @Inject(method = "untrackPlayer", at = @At("HEAD"))
    private void nrc$detachCatalogOutfit(CallbackInfo ci) {
        ShopCatalogBridge bridge = ShopCatalog.get();
        if (bridge != null && outfit != null) bridge.detachPreview(getPreviewEntityId());
    }

    /**
     * Massive performance optimization:
     * Prevents rendering 3D Player entities, GeckoLib models, animations, and particles
     * when the cosmetic card is scrolled off-screen.
     */
    @Inject(method = "draw", at = @At("HEAD"), cancellable = true)
    private void nrc$cullOffscreenCosmeticShowcase(OwoUIGraphics context, int mouseX, int mouseY, float partialTicks, float delta, CallbackInfo ci) {
        if (!Features.SHOP_CULLING.on()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.getWindow() == null) return;

        double compX = this.x();
        double compY = this.y();
        double compW = this.width();
        double compH = this.height();

        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();

        // Cull if outside visible screen viewport (with buffer)
        if (compX + compW < -10 || compX > screenW + 10 || compY + compH < 20 || compY > screenH + 10) {
            ci.cancel();
        }
    }
}
