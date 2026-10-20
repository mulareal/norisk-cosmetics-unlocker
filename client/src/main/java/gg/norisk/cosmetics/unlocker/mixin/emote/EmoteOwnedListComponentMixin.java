package gg.norisk.cosmetics.unlocker.mixin.emote;

import gg.norisk.cosmetics.unlocker.Log;
import gg.norisk.cosmetics.unlocker.settings.Features;
import gg.norisk.cosmetics.v2.manager.NrcEmoteIndex;
import gg.norisk.cosmetics.v2.model.NrcEmoteEntry;
import gg.norisk.cosmetics.v2.ui.emote.EmoteOwnedListComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Mixin(value = EmoteOwnedListComponent.class, remap = false)
public abstract class EmoteOwnedListComponentMixin {

    @ModifyVariable(method = "<init>(ILjava/util/List;Lgg/norisk/owolib/owo/ui/core/Sizing;Lgg/norisk/owolib/owo/ui/core/Sizing;)V", at = @At("HEAD"), argsOnly = true, ordinal = 0, remap = false)
    private static List<NrcEmoteEntry> unlockEmoteList(List<NrcEmoteEntry> emotes) {
        if (Features.EMOTES.on()) {
            try {
                Collection<NrcEmoteEntry> all = NrcEmoteIndex.INSTANCE.allEntries();
                if (all != null && !all.isEmpty()) {
                    return new ArrayList<>(all);
                }
            } catch (Throwable t) {
                Log.warn("Failed to load all emotes: " + t.getMessage());
            }
        }
        return emotes;
    }
}
