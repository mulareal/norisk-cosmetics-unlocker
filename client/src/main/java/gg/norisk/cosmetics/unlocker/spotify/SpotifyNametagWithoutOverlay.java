package gg.norisk.cosmetics.unlocker.spotify;

import gg.norisk.client.v2.modules.spotify.SpotifyHud;
import gg.norisk.client.v2.modules.spotify.SpotifyMediaState;
import gg.norisk.compat.auth.NoriskAuth;
import gg.norisk.compat.spotify.SpotifyStatusCache;
import gg.norisk.cosmetics.unlocker.Log;
import gg.norisk.cosmetics.v2.NRCPlusModule;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

import java.util.UUID;

/**
 * NoRisk only reads the playing song while the "Spotify Overlay" HUD module is enabled, so the
 * "Show your song" nametag option does nothing without the overlay. When the overlay is off but
 * "Show your song" is on, run the same media polling ourselves (without drawing the HUD), so that
 * checkbox alone switches the song in the nametag on and off.
 */
public final class SpotifyNametagWithoutOverlay {

    private static boolean active;
    private static boolean broken;
    private static int ticks;

    private SpotifyNametagWithoutOverlay() {
    }

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> tick());
    }

    private static void tick() {
        if (broken) return;
        try {
            SpotifyHud hud = SpotifyHud.INSTANCE;
            boolean want = !hud.isEnabled() && hud.getShowInNameTag();
            if (want) {
                if (!active) {
                    active = true;
                    ticks = 0;
                    SpotifyMediaState.INSTANCE.initialize(hud.getArtworkResolution().intValue());
                    NRCPlusModule.INSTANCE.setShouldShowSpotifyInNameTag(() -> SpotifyHud.INSTANCE.getShowInNameTag());
                }
                if (++ticks % 10 == 0) hud.tickMediaState(); // same rate as the overlay
            } else if (active) {
                active = false;
                // If the overlay was switched on, its onEnable has taken over; otherwise stop polling
                if (!hud.isEnabled()) {
                    SpotifyMediaState.INSTANCE.shutdown();
                    NRCPlusModule.INSTANCE.setShouldShowSpotifyInNameTag(() -> false);
                    UUID local = NoriskAuth.INSTANCE.getPlayerUuid();
                    if (local != null) SpotifyStatusCache.INSTANCE.clear(local);
                }
            }
        } catch (Throwable t) {
            broken = true; // NoRisk changed something; don't spam every tick
            Log.warn("Spotify nametag without overlay disabled: " + t);
        }
    }
}
