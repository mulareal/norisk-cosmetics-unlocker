package gg.norisk.cosmetics.unlocker.mixin.player;

import gg.norisk.cosmetics.unlocker.api.ClientHooks;
import net.minecraft.client.player.AbstractClientPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractClientPlayer.class)
public abstract class AbstractClientPlayerMixin {

    @Inject(method = "<init>", at = @At("RETURN"))
    private void nrc$onClientPlayerInit(CallbackInfo ci) {
        AbstractClientPlayer player = (AbstractClientPlayer) (Object) this;
        ClientHooks.PLAYER_LOADED.fire(listener -> listener.accept(player));
    }
}
