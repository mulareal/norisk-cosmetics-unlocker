package gg.norisk.cosmetics.unlocker.mixin.shield;

import gg.norisk.cosmetics.geckolib.bridge.shield.CosmeticItemReplaceModel;
import gg.norisk.cosmetics.unlocker.api.ClientHooks;
import gg.norisk.cosmetics.unlocker.api.HeldShield;
import gg.norisk.cosmetics.unlocker.cosmetics.HeldShieldDraw;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** NoRisk's shield item model: lets {@link ClientHooks#heldShield} replace or hide a player's held shield. */
@Mixin(value = CosmeticItemReplaceModel.class, remap = false)
public abstract class CosmeticItemReplaceModelMixin {

    @Inject(method = "update", at = @At("HEAD"), cancellable = true)
    private void nrc$heldShield(ItemStackRenderState state, ItemStack stack, ItemModelResolver resolver, ItemDisplayContext context,
                                ClientLevel level, ItemOwner owner, int seed, CallbackInfo ci) {
        HeldShieldDraw.current = null;
        var provider = ClientHooks.heldShield;
        if (provider == null || owner == null || !(owner.asLivingEntity() instanceof AbstractClientPlayer player)) return;
        HeldShield shield;
        try {
            shield = provider.apply(player, context);
        } catch (Throwable t) {
            return;
        }
        if (shield == null) return;
        if (shield.hidden()) {
            state.clear();
            ci.cancel();
            return;
        }
        // NoRisk asks its shield bridge for model, texture and transform next; see NrcShieldBridgeMixin.
        HeldShieldDraw.current = shield;
    }

    @Inject(method = "update", at = @At("RETURN"))
    private void nrc$heldShieldDone(ItemStackRenderState state, ItemStack stack, ItemModelResolver resolver, ItemDisplayContext context,
                                    ClientLevel level, ItemOwner owner, int seed, CallbackInfo ci) {
        HeldShieldDraw.current = null;
    }
}
