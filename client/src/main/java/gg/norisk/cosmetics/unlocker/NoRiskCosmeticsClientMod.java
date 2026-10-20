package gg.norisk.cosmetics.unlocker;

import gg.norisk.cosmetics.unlocker.api.ClientAddon;
import gg.norisk.cosmetics.unlocker.api.ClientHooks;
import gg.norisk.cosmetics.unlocker.compat.CompatCheck;
import gg.norisk.cosmetics.unlocker.cosmetics.CosmeticsUnlocker;
import gg.norisk.cosmetics.unlocker.cosmetics.LocalEmoteEvents;
import gg.norisk.cosmetics.unlocker.cosmetics.LocalOutfitStorage;
import gg.norisk.cosmetics.unlocker.settings.Features;
import gg.norisk.cosmetics.unlocker.settings.ToggleModule;
import gg.norisk.cosmetics.unlocker.settings.UnlockerModule;
import gg.norisk.cosmetics.unlocker.spotify.SpotifyNametagWithoutOverlay;
import gg.norisk.cosmetics.unlocker.texteffects.CustomTextEffects;
import gg.norisk.compat.icon.NrcPlayerCache;
import gg.norisk.cosmetics.v2.NRCPlusModule;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.ServiceLoader;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Entry point. Everything here works on its own; optional {@link ClientAddon}s (found via
 * {@link ServiceLoader}) plug in through {@link gg.norisk.cosmetics.unlocker.api.ClientHooks}.
 */
public class NoRiskCosmeticsClientMod implements ClientModInitializer {

    public static final String MOD_ID = "norisk-cosmetics-unlocker";

    /** NRC+ animations are sometimes not ready right at join, so they are refreshed a few times. */
    private static final int[] NRC_PLUS_REFRESH_DELAYS_MS = {500, 1500, 4000};

    private static boolean compatNoticeShown;

    @Override
    public void onInitializeClient() {
        Log.info("Starting " + version());
        Features.init();
        FreezeWatchdog.start();

        CompatCheck.require(
                "gg.norisk.cosmetics.v2.NRCPlusModule#refreshNrcPlusAnimations",
                "gg.norisk.compat.icon.NrcPlayerCache#getCached");
        LocalOutfitStorage.loadOutfits();
        CosmeticsUnlocker.refreshCache();
        refreshNrcPlus();

        LocalEmoteEvents.init();
        SpotifyNametagWithoutOverlay.init();
        CustomTextEffects.register();
        NrcCommands.register();
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> onJoin(client));
        ClientHooks.SETTINGS_CHANGED.register(NoRiskCosmeticsClientMod::onSettingsChanged);

        loadAddons();
        ToggleModule.register(new UnlockerModule());
    }

    /** Mod version from fabric.mod.json. */
    public static String version() {
        return FabricLoader.getInstance().getModContainer(MOD_ID)
                .map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("0");
    }

    private static void onJoin(Minecraft client) {
        client.execute(NoRiskCosmeticsClientMod::refreshNrcPlus);
        for (int delay : NRC_PLUS_REFRESH_DELAYS_MS) {
            CompletableFuture.delayedExecutor(delay, TimeUnit.MILLISECONDS)
                    .execute(() -> client.execute(NoRiskCosmeticsClientMod::refreshNrcPlus));
        }

        List<String> issues = CompatCheck.issues();
        if (!issues.isEmpty() && !compatNoticeShown) {
            compatNoticeShown = true;
            Chat.send(Component.literal("§c[NRC] §fNoRisk-Update: " + issues.size() + " Hooks kaputt. ")
                    .append(Chat.command("§7[Details]", "/nrcu status")));
        }
    }

    /** A switch in the mod menu changed: rebuild what NoRisk caches from our data. */
    private static void onSettingsChanged() {
        CosmeticsUnlocker.refreshCache();
        refreshNrcPlus();
    }

    private static void refreshNrcPlus() {
        try {
            UUID localUuid = LocalOutfitStorage.getLocalPlayerUuid();
            if (localUuid != null) NrcPlayerCache.INSTANCE.getCached(localUuid);
            NRCPlusModule.INSTANCE.refreshNrcPlusAnimations();
        } catch (Throwable t) {
            Log.debug("NRC+ refresh failed: " + t);
        }
    }

    private static void loadAddons() {
        for (ClientAddon addon : ServiceLoader.load(ClientAddon.class, NoRiskCosmeticsClientMod.class.getClassLoader())) {
            try {
                addon.init();
                Log.info("Addon loaded: " + addon.name());
            } catch (Throwable t) {
                Log.warn("Addon " + addon.name() + " failed to start: " + t);
            }
        }
    }
}
