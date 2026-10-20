package gg.norisk.cosmetics.unlocker.mixin.icon;

import gg.norisk.cosmetics.unlocker.cosmetics.CosmeticsUnlocker;
import gg.norisk.networking.model.cosmetics.CustomIcon;
import gg.norisk.networking.model.cosmetics.CustomIconInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Mixin(targets = "gg.norisk.client.v2.modules.impl.IconSelectorComponent", remap = false)
public abstract class IconSelectorComponentMixin {

    @ModifyVariable(method = "buildIconEntry", at = @At("HEAD"), argsOnly = true, ordinal = 0, remap = false)
    private boolean unlockCustomIconEntry(boolean isOwned) {
        if (CosmeticsUnlocker.isEnabled()) {
            return true;
        }
        return isOwned;
    }

    @ModifyVariable(method = "build", at = @At(value = "STORE", ordinal = 0), remap = false)
    private List<UUID> forceAllOwnedIconsInBuild(List<UUID> ownedItems) {
        if (CosmeticsUnlocker.isEnabled()) {
            List<UUID> all = new ArrayList<>();
            if (ownedItems != null) {
                all.addAll(ownedItems);
            }
            try {
                for (CustomIcon icon : CustomIconInfo.Companion.getICONS()) {
                    if (icon != null && icon.getUuid() != null) {
                        all.add(icon.getUuid());
                    }
                }
            } catch (Throwable ignored) {
            }
            return all;
        }
        return ownedItems;
    }
}
