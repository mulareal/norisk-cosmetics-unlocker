package gg.norisk.cosmetics.unlocker.mixin.icon;

import gg.norisk.compat.icon.IconResolver;
import gg.norisk.cosmetics.unlocker.cosmetics.LocalOutfitStorage;
import gg.norisk.cosmetics.unlocker.settings.Features;
import gg.norisk.networking.model.core.NoRiskUserMinimal;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Constructor;
import java.util.UUID;

@Mixin(value = IconResolver.class, remap = false)
public abstract class IconResolverMixin {

    private static Constructor<?> iconResultCtor = null;

    @Inject(method = "resolve", at = @At("HEAD"), cancellable = true, remap = false)
    private void overrideResolveIcon(NoRiskUserMinimal user, CallbackInfoReturnable<IconResolver.IconResult> cir) {
        if (!Features.ICONS.on() || user == null) {
            return;
        }

        try {
            String iconName = LocalOutfitStorage.getSelectedIconName(user.getUuid());
            if (iconName == null && LocalOutfitStorage.isLocalPlayer(user.getUuid())) {
                iconName = LocalOutfitStorage.getActiveIconName();
            }

            if (iconName != null && !iconName.isEmpty()) {
                String path = "textures/iconfont/" + iconName.toLowerCase() + "_icons.png";
                Identifier id = Identifier.fromNamespaceAndPath("noriskclient", path);

                if (iconResultCtor == null) {
                    iconResultCtor = IconResolver.IconResult.class.getDeclaredConstructors()[0];
                    iconResultCtor.setAccessible(true);
                }
                IconResolver.IconResult result = (IconResolver.IconResult) iconResultCtor.newInstance(id, user.isNoRiskPlus());
                cir.setReturnValue(result);
            }
        } catch (Throwable ignored) {
        }
    }
}
