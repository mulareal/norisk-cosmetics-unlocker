package gg.norisk.cosmetics.unlocker.cosmetics;

import gg.norisk.cosmetics.unlocker.Log;
import gg.norisk.cosmetics.v2.manager.NrcCosmeticManager;
import gg.norisk.cosmetics.v2.manager.NrcEmoteIndex;
import gg.norisk.cosmetics.v2.model.NrcCosmetic;
import gg.norisk.cosmetics.v2.model.NrcEmoteEntry;
import gg.norisk.cosmetics.v2.ui.shop.ShopCache;
import gg.norisk.cosmetics.v2.ui.shop.ShopItemInfo;
import gg.norisk.networking.model.cosmetics.CosmeticOutfit;
import gg.norisk.networking.model.cosmetics.CosmeticUser;
import gg.norisk.networking.model.cosmetics.api.WardrobeResponse;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Core unlocker that unlocks all NoRiskClient cosmetics, capes, and emotes client-side.
 */
public class CosmeticsUnlocker {
    private static boolean enabled = true;
    private static final Set<UUID> cachedCosmeticIds = ConcurrentHashMap.newKeySet();
    private static final Set<String> cachedEmoteIds = ConcurrentHashMap.newKeySet();
    private static long lastCacheUpdate = 0L;

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean value) {
        enabled = value;
    }

    public static void toggle() {
        enabled = !enabled;
    }

    /**
     * Retrieves all registered cosmetic IDs from NoRisk's registry.
     */
    public static Set<UUID> getAllCosmeticIds() {
        long now = System.currentTimeMillis();
        if (now - lastCacheUpdate > 2000L || cachedCosmeticIds.isEmpty()) {
            refreshCache();
        }
        return cachedCosmeticIds;
    }

    public static Set<String> getAllEmoteIds() {
        long now = System.currentTimeMillis();
        if (now - lastCacheUpdate > 2000L || cachedEmoteIds.isEmpty()) {
            refreshCache();
        }
        return cachedEmoteIds;
    }

    public static synchronized void refreshCache() {
        try {
            // 1. All cosmetics from NrcCosmeticManager
            for (NrcCosmetic cosmetic : NrcCosmeticManager.INSTANCE.all()) {
                if (cosmetic != null && cosmetic.getId() != null) {
                    cachedCosmeticIds.add(cosmetic.getId());
                    if (cosmetic.getShopItem() != null && cosmetic.getShopItem().getId() != null) {
                        cachedCosmeticIds.add(cosmetic.getShopItem().getId());
                    }
                    if (cosmetic.getVariants() != null) {
                        for (NrcCosmetic variant : cosmetic.getVariants()) {
                            if (variant != null && variant.getId() != null) {
                                cachedCosmeticIds.add(variant.getId());
                            }
                        }
                    }
                }
            }

            // 2. All shop items from ShopCache
            try {
                for (ShopItemInfo item : ShopCache.INSTANCE.getAllShopItems()) {
                    if (item != null && item.getId() != null) {
                        cachedCosmeticIds.add(item.getId());
                        if (item.getEmoteId() != null) {
                            cachedEmoteIds.add(item.getEmoteId());
                        }
                    }
                }
            } catch (Throwable ignored) {
            }

            // 3. All emotes from NrcEmoteIndex
            try {
                for (NrcEmoteEntry emote : NrcEmoteIndex.INSTANCE.allEntries()) {
                    if (emote != null && emote.getEmoteId() != null) {
                        cachedEmoteIds.add(emote.getEmoteId());
                    }
                }
            } catch (Throwable ignored) {
            }

            // 4. All custom icons from CustomIconInfo
            try {
                for (gg.norisk.networking.model.cosmetics.CustomIcon icon : gg.norisk.networking.model.cosmetics.CustomIconInfo.Companion.getICONS()) {
                    if (icon != null && icon.getUuid() != null) {
                        cachedCosmeticIds.add(icon.getUuid());
                    }
                }
            } catch (Throwable ignored) {
            }

            lastCacheUpdate = System.currentTimeMillis();
        } catch (Throwable t) {
            Log.warn("Failed to refresh cosmetics cache: " + t.getMessage());
        }
    }

    /**
     * Injects unlocked items and locally saved outfits into a WardrobeResponse.
     */
    public static void populateWardrobeResponse(WardrobeResponse response) {
        if (!enabled || response == null) return;
        try {
            CosmeticUser user = response.getCosmeticUser();
            if (user != null) {
                Set<UUID> allIds = getAllCosmeticIds();
                user.addItems(allIds);
            }

            // Inject local outfits if available
            List<CosmeticOutfit> localOutfits = LocalOutfitStorage.loadOutfits();
            if (localOutfits != null && !localOutfits.isEmpty()) {
                List<CosmeticOutfit> serverOutfits = response.getOutfits();
                for (CosmeticOutfit local : localOutfits) {
                    boolean exists = false;
                    for (CosmeticOutfit s : serverOutfits) {
                        if (s.getOutfitId().equals(local.getOutfitId())) {
                            exists = true;
                            break;
                        }
                    }
                    if (!exists) {
                        serverOutfits.add(local);
                    }
                }
            }
        } catch (Throwable t) {
            Log.warn("Error populating wardrobe response: " + t.getMessage());
        }
    }
}
