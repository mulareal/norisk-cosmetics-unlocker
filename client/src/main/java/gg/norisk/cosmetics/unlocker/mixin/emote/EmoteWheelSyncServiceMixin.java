package gg.norisk.cosmetics.unlocker.mixin.emote;

import gg.norisk.cosmetics.unlocker.Log;
import gg.norisk.cosmetics.unlocker.cosmetics.LocalEmoteWheel;
import gg.norisk.cosmetics.v2.sync.EmoteWheelSyncService;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;
import java.util.UUID;

/**
 * Keeps the emote wheel and AFK emote across restarts: save on every edit, and whenever NoRisk writes
 * a wheel from its backend or cache (which lacks unowned emotes) use our saved one instead.
 */
@Mixin(targets = "gg.norisk.cosmetics.v2.sync.EmoteWheelSyncService", remap = false)
public abstract class EmoteWheelSyncServiceMixin {

    @Inject(method = "pushNow", at = @At("HEAD"), remap = false)
    private void nrc$saveWheel(CallbackInfo ci) {
        try {
            LocalEmoteWheel.saveCurrent();
        } catch (Throwable t) {
            Log.warn("Emote wheel save failed: " + t);
        }
    }

    @ModifyVariable(method = "writeModule", at = @At("HEAD"), argsOnly = true, remap = false)
    private EmoteWheelSyncService.WheelSnapshot nrc$useSavedWheel(EmoteWheelSyncService.WheelSnapshot snapshot) {
        if (snapshot == EmoteWheelSyncService.WheelSnapshot.Companion.getEMPTY()) return snapshot; // account switch
        try {
            EmoteWheelSyncService.WheelSnapshot saved = LocalEmoteWheel.load();
            return saved != null ? saved : snapshot;
        } catch (Throwable t) {
            return snapshot;
        }
    }

    /** NoRisk strips emotes the account doesn't own; all of them are usable here. */
    @Inject(method = "isUsable", at = @At("HEAD"), cancellable = true, remap = false)
    private void nrc$allUsable(UUID shopId, Set<UUID> owned, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(true);
    }
}
