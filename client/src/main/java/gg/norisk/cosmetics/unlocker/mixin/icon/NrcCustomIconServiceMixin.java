package gg.norisk.cosmetics.unlocker.mixin.icon;

import gg.norisk.cosmetics.unlocker.cosmetics.LocalOutfitStorage;
import gg.norisk.cosmetics.unlocker.settings.Features;
import gg.norisk.networking.model.core.NoRiskUserMinimal;
import gg.norisk.networking.model.cosmetics.CustomIcon;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(targets = "gg.norisk.cosmetics.v2.sync.NrcCustomIconService", remap = false)
public abstract class NrcCustomIconServiceMixin {

    @Inject(method = "selectIcon", at = @At("HEAD"), cancellable = true, remap = false)
    private void onSelectIcon(CustomIcon customIcon, Function0<Object> onSuccess, Function1<Throwable, Object> onError, CallbackInfo ci) {
        if (Features.ICONS.on() && customIcon != null) {
            try {
                LocalOutfitStorage.setActiveIconName(customIcon.getName());
                
                UUID localUuid = LocalOutfitStorage.getLocalPlayerUuid();
                if (localUuid != null) {
                    NoRiskUserMinimal cached = gg.norisk.compat.icon.NrcPlayerCache.INSTANCE.getCached(localUuid);
                    if (cached != null && cached.getCustomIconInfo() != null) {
                        cached.getCustomIconInfo().setCurrentIcon(customIcon.getUuid());
                    }
                }

                if (onSuccess != null) {
                    onSuccess.invoke();
                }
                ci.cancel();
            } catch (Throwable ignored) {
            }
        }
    }
}
