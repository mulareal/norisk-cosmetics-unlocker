package gg.norisk.cosmetics.unlocker.mixin.nametag;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import gg.norisk.cosmetics.unlocker.Log;
import gg.norisk.cosmetics.v2.NRCPlusModule;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.concurrent.atomic.AtomicBoolean;

@Mixin(value = NRCPlusModule.class, remap = false)
public abstract class NRCPlusModuleMixin {

    @Unique
    private static final AtomicBoolean nrc$QUEUED = new AtomicBoolean(false);

    @Unique
    private static boolean nrc$inRefresh;

    /**
     * NoRisk re-runs this every 5s from a background task, and it is also called from the websocket, network
     * and render threads. It mutates plain (non thread-safe) collections that the render thread also uses:
     * concurrent calls threw ConcurrentModificationException (which killed the 5s task -> animated nametags
     * stopped mid-game) and can corrupt the HashSet so the render thread spins forever (game freezes).
     *
     * Fix: only ever run it on the render thread. Calls from other threads just queue one run (never block,
     * never run twice in a row) and return immediately.
     */
    @WrapMethod(method = "refreshNrcPlusAnimations")
    private void nrc$safeRefresh(Operation<Void> original) {
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && !mc.isSameThread()) {
            if (nrc$QUEUED.compareAndSet(false, true)) {
                mc.execute(() -> {
                    nrc$QUEUED.set(false);
                    NRCPlusModule.INSTANCE.refreshNrcPlusAnimations();
                });
            }
            return;
        }
        if (nrc$inRefresh) return; // re-entrant call from inside the rebuild; the outer run covers it
        nrc$inRefresh = true;
        try {
            original.call();
        } catch (Throwable t) {
            Log.warn("refreshNrcPlusAnimations failed (kept loop alive): " + t);
        } finally {
            nrc$inRefresh = false;
        }
    }
}
