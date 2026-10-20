package gg.norisk.cosmetics.unlocker.mixin.nametag;

import gg.norisk.compat.auth.NoriskAuth;
import gg.norisk.compat.icon.NrcPlayerCache;
import gg.norisk.cosmetics.unlocker.cosmetics.LocalOutfitStorage;
import gg.norisk.cosmetics.v2.sync.NrcAdditionalNameTagService;
import gg.norisk.networking.model.core.NoRiskUserMinimal;
import gg.norisk.networking.model.cosmetics.AdditionalNameTag;
import gg.norisk.networking.model.cosmetics.api.AdditionalNameTagUpdateRequest;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(NrcAdditionalNameTagService.class)
public class NrcAdditionalNameTagServiceMixin {

    @Inject(method = "apply", at = @At("HEAD"), cancellable = true, remap = false)
    private void nrc$applyAdditionalNameTagUnlocked(AdditionalNameTagUpdateRequest request, Function1<? super Throwable, Unit> onError, CallbackInfo ci) {
        UUID uuid = LocalOutfitStorage.getLocalPlayerUuid();
        if (uuid != null) {
            NoRiskUserMinimal user = NrcPlayerCache.INSTANCE.getCached(uuid);
            if (user != null) {
                AdditionalNameTag tag = user.getAdditionalNameTag();
                if (tag != null) {
                    if (request.getText() != null) tag.setText(request.getText());
                    if (request.isEnabled() != null) tag.setEnabled(request.isEnabled());
                    if (request.getPosition() != null) tag.setPosition(request.getPosition());
                    if (request.getSize() != null) tag.setSize(request.getSize());
                    if (request.getHasBackground() != null) tag.setHasBackground(request.getHasBackground());
                    LocalOutfitStorage.saveAdditionalNameTag(tag);
                }

            }
        }
        ci.cancel();
    }
}
