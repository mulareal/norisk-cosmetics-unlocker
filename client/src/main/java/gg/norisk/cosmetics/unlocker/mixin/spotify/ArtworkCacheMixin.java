package gg.norisk.cosmetics.unlocker.mixin.spotify;

import gg.norisk.abstraction.resource.TextureInfo;
import gg.norisk.client.v2.modules.spotify.ArtworkCache;
import gg.norisk.cosmetics.unlocker.spotify.SharedArtwork;
import gg.norisk.cosmetics.unlocker.settings.Features;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Nametag covers are looked up by id on NoRisk's CDN, where they never land without NRC+.
 * Load covers known locally (see {@link SharedArtwork}) from their file instead.
 */
@Mixin(targets = "gg.norisk.client.v2.modules.spotify.ArtworkCache", remap = false)
public abstract class ArtworkCacheMixin {

    @Inject(method = "getRemote", at = @At("HEAD"), cancellable = true, remap = false)
    private void nrc$localArtwork(String artworkId, int radius, boolean allCorners, boolean circular,
                                  CallbackInfoReturnable<TextureInfo> cir) {
        if (!Features.SONG_COVERS.on()) return;
        String url = SharedArtwork.localUrl(artworkId);
        if (url != null) cir.setReturnValue(ArtworkCache.INSTANCE.get(url, radius, allCorners, circular));
    }
}
