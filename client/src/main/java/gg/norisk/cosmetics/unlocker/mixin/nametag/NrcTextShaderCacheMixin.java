package gg.norisk.cosmetics.unlocker.mixin.nametag;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Set;

/**
 * NrcTextShaderCache.apply() scans rendered text with {@code indexOf(name, idx + 1)} until it returns -1.
 * For an empty name indexOf never returns -1, so the render thread spins forever (froze the game when the
 * tab list was drawn). Synced players whose name is not known yet have an empty IGN, so never register those;
 * the 5s NRC+ refresh adds them once the name is known.
 */
@Mixin(targets = "gg.norisk.abstraction.text.NrcTextShaderCache", remap = false)
public abstract class NrcTextShaderCacheMixin {

    @Inject(method = "add", at = @At("HEAD"), cancellable = true, remap = false)
    private static void nrc$skipBlankName(String name, List<?> animations, Set<?> contexts, CallbackInfo ci) {
        if (name == null || name.isBlank()) ci.cancel();
    }
}
