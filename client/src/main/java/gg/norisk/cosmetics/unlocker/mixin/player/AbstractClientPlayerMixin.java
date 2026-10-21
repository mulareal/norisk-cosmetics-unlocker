package gg.norisk.cosmetics.unlocker.mixin.player;

import gg.norisk.cosmetics.unlocker.api.ClientHooks;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayer.class)
public abstract class AbstractClientPlayerMixin {

    // Last override, so an animated cape does not allocate a skin every frame.
    @Unique
    private PlayerSkin nrc$baseSkin;
    @Unique
    private Identifier nrc$cape;
    @Unique
    private PlayerSkin nrc$skin;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void nrc$onClientPlayerInit(CallbackInfo ci) {
        AbstractClientPlayer player = (AbstractClientPlayer) (Object) this;
        ClientHooks.PLAYER_LOADED.fire(listener -> listener.accept(player));
    }

    /** Vanilla's cape layer and cape mods read the cape from here, so an override replaces it everywhere. */
    @Inject(method = "getSkin", at = @At("RETURN"), cancellable = true)
    private void nrc$overrideCape(CallbackInfoReturnable<PlayerSkin> cir) {
        var override = ClientHooks.capeOverride;
        PlayerSkin skin = cir.getReturnValue();
        if (override == null || skin == null) return;
        Identifier cape;
        try {
            cape = override.apply((AbstractClientPlayer) (Object) this);
        } catch (Throwable t) {
            return;
        }
        if (cape == null) return;
        if (skin != nrc$baseSkin || !cape.equals(nrc$cape)) {
            // Without its own elytra texture Minecraft draws the elytra with the cape, so it shows the new one too.
            ClientAsset.Texture texture = cape.equals(ClientHooks.NO_CAPE) ? null : new ClientAsset.ResourceTexture(cape, cape);
            nrc$skin = new PlayerSkin(skin.body(), texture, skin.elytra(), skin.model(), skin.secure());
            nrc$baseSkin = skin;
            nrc$cape = cape;
        }
        cir.setReturnValue(nrc$skin);
    }
}
