package gg.norisk.cosmetics.unlocker.mixin.spotify;

import gg.norisk.cosmetics.unlocker.Log;
import gg.norisk.cosmetics.unlocker.spotify.SharedArtwork;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * NoRisk uploads the song cover to its backend (NRC+ only, 403 otherwise) and puts the returned id in the
 * nametag status. Store it locally under the same id instead (see {@link SharedArtwork}).
 */
@Mixin(targets = "gg.norisk.client.v2.modules.spotify.SpotifyHud", remap = false)
public abstract class SpotifyHudShareMixin {

    @Inject(method = "shareArtwork", at = @At("HEAD"), cancellable = true, remap = false)
    private void nrc$shareArtwork(String artworkUrl, CallbackInfoReturnable<String> cir) {
        try {
            cir.setReturnValue(SharedArtwork.share(artworkUrl));
        } catch (Throwable t) {
            Log.warn("Song cover share failed: " + t); // fall back to NoRisk's upload
        }
    }
}
