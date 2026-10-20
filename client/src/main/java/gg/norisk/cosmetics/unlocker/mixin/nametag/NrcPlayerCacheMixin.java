package gg.norisk.cosmetics.unlocker.mixin.nametag;

import gg.norisk.compat.icon.NrcPlayerCache;
import gg.norisk.compat.icon.NrcUuidResolver;
import gg.norisk.cosmetics.unlocker.cosmetics.LocalOutfitStorage;
import gg.norisk.cosmetics.unlocker.settings.ModSettings;
import gg.norisk.networking.model.core.NoRiskUserMinimal;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mixin(value = NrcPlayerCache.class, remap = false)
public class NrcPlayerCacheMixin {

    @Unique
    private static final ThreadLocal<Boolean> nrc$UPDATING = ThreadLocal.withInitial(() -> Boolean.FALSE);

    @Unique
    private static final ThreadLocal<Boolean> nrc$IN_FOR_EACH = ThreadLocal.withInitial(() -> Boolean.FALSE);

    @Unique
    private static final Map<UUID, NoRiskUserMinimal> nrc$lastCachedUsers = new ConcurrentHashMap<>();

    @Unique
    private static UUID nrc$resolveUuid(UUID uuid) {
        if (uuid == null) return null;
        try {
            UUID real = NrcUuidResolver.INSTANCE.realFor(uuid);
            return real != null ? real : uuid;
        } catch (Throwable ignored) {
            return uuid;
        }
    }

    @Unique
    private static volatile long nrc$lastEnsure;

    @Unique
    private static void nrc$ensureTrackedPlayersCached() {
        if (nrc$UPDATING.get()) return;
        // Called for every rendered text line; making sure synced players are cached once a second is plenty
        long now = System.currentTimeMillis();
        if (now - nrc$lastEnsure < 1000) return;
        nrc$lastEnsure = now;
        try {
            UUID localUuid = LocalOutfitStorage.getLocalPlayerUuid();
            if (localUuid != null) {
                NrcPlayerCache.INSTANCE.getCached(localUuid);
            }
            Set<UUID> remoteUuids = LocalOutfitStorage.getRemoteTrackedUuids();
            for (UUID remoteUuid : remoteUuids) {
                if (remoteUuid != null) {
                    NrcPlayerCache.INSTANCE.getCached(remoteUuid);
                }
            }
        } catch (Throwable ignored) {
        }
    }

    @Inject(method = "getCached", at = @At("RETURN"), cancellable = true, remap = false)
    private void nrc$injectSavedNametag(UUID uuid, CallbackInfoReturnable<NoRiskUserMinimal> cir) {
        if (uuid == null || nrc$UPDATING.get() || !ModSettings.ENABLED.on()) return;
        try {
            UUID real = nrc$resolveUuid(uuid);
            if (LocalOutfitStorage.isLocalPlayer(real) || LocalOutfitStorage.hasRemoteData(real)) {
                NoRiskUserMinimal current = cir.getReturnValue();
                NoRiskUserMinimal updated = LocalOutfitStorage.createOrUpdateUser(real, current);
                if (updated != null) {
                    if (current == null || updated != current || nrc$lastCachedUsers.get(real) != updated) {
                        nrc$lastCachedUsers.put(real, updated);
                        nrc$UPDATING.set(Boolean.TRUE);
                        try {
                            NrcPlayerCache.INSTANCE.addToCache(real, updated, false);
                        } finally {
                            nrc$UPDATING.set(Boolean.FALSE);
                        }
                        if (!nrc$IN_FOR_EACH.get() && !updated.getIgn().isEmpty()) {
                            try {
                                gg.norisk.cosmetics.v2.NRCPlusModule.INSTANCE.refreshNrcPlusAnimations();
                            } catch (Throwable ignored) {
                            }
                        }
                    }
                    cir.setReturnValue(updated);
                }
            }
        } catch (Throwable ignored) {
        }
    }

    @Inject(method = "addToCache", at = @At("HEAD"), cancellable = true, remap = false)
    private void nrc$preserveCustomNametagOnAdd(UUID uuid, NoRiskUserMinimal user, boolean notify, CallbackInfo ci) {
        if (uuid == null || nrc$UPDATING.get() || !ModSettings.ENABLED.on()) return;
        try {
            UUID real = nrc$resolveUuid(uuid);
            if (LocalOutfitStorage.isLocalPlayer(real) || LocalOutfitStorage.hasRemoteData(real)) {
                NoRiskUserMinimal base = user != null ? user : nrc$lastCachedUsers.get(real);
                NoRiskUserMinimal updated = LocalOutfitStorage.createOrUpdateUser(real, base);
                if (updated != null) {
                    nrc$lastCachedUsers.put(real, updated);
                    nrc$UPDATING.set(Boolean.TRUE);
                    try {
                        NrcPlayerCache.INSTANCE.addToCache(real, updated, notify);
                    } finally {
                        nrc$UPDATING.set(Boolean.FALSE);
                    }
                    if (!nrc$IN_FOR_EACH.get() && !updated.getIgn().isEmpty()) {
                        try {
                            gg.norisk.cosmetics.v2.NRCPlusModule.INSTANCE.refreshNrcPlusAnimations();
                        } catch (Throwable ignored) {
                        }
                    }
                    ci.cancel();
                }
            }
        } catch (Throwable ignored) {
        }
    }

    @Inject(method = "forEachCached", at = @At("HEAD"), remap = false)
    private void nrc$beforeForEachCached(Function1<? super NoRiskUserMinimal, Unit> action, CallbackInfo ci) {
        nrc$IN_FOR_EACH.set(Boolean.TRUE);
        try {
            nrc$ensureTrackedPlayersCached();
        } finally {
            nrc$IN_FOR_EACH.set(Boolean.FALSE);
        }
    }

    @Inject(method = "findCachedByNameInText", at = @At("HEAD"), remap = false)
    private void nrc$beforeFindCachedByNameInText(String plain, CallbackInfoReturnable<NoRiskUserMinimal> cir) {
        nrc$IN_FOR_EACH.set(Boolean.TRUE);
        try {
            nrc$ensureTrackedPlayersCached();
        } finally {
            nrc$IN_FOR_EACH.set(Boolean.FALSE);
        }
    }

    @Inject(method = "invalidate", at = @At("HEAD"), remap = false)
    private void nrc$onInvalidate(CallbackInfo ci) {
        nrc$lastCachedUsers.clear();
    }
}

