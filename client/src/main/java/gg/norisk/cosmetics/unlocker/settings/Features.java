package gg.norisk.cosmetics.unlocker.settings;

import gg.norisk.cosmetics.unlocker.settings.ModSettings.Toggle;

/**
 * The core features as switches. Check them with {@link Toggle#on()}.
 * Label and description show in the mod menu, the info text behind the "i" (German in de_de.json).
 */
public final class Features {

    public static final Toggle COSMETICS = ModSettings.toggle("cosmetics", "Cosmetics & Capes",
                    "Every cosmetic and cape in the wardrobe, saved outfits", true)
            .withInfo("Unlocks every cosmetic and cape. Only you and players with this mod (Server Sync) see them. "
                    + "Normal NoRisk players see your real cosmetics.");
    public static final Toggle EMOTES = ModSettings.toggle("emotes", "Emotes",
                    "Every emote in the emote menu and wheel", true)
            .withInfo("Every emote works in the emote menu and wheel. The wheel and AFK emote are saved on your PC, "
                    + "because NoRisk removes emotes you don't own on every start.");
    public static final Toggle NRC_PLUS = ModSettings.toggle("nrcPlus", "NRC+ Nametags",
                    "NRC+ nametag effects and extra nametag without NRC+", true)
            .withInfo("NRC+ nametag effects, styles and the extra nametag line without NRC+. "
                    + "Saved on your PC, not on your NoRisk account.");
    public static final Toggle ICONS = ModSettings.toggle("icons", "Custom Icons",
                    "Every custom icon next to your name", true)
            .withInfo("Every custom icon next to your name. Others see it only with this mod.");
    public static final Toggle TEXT_EFFECTS = ModSettings.toggle("textEffects", "Extra Text Effects",
                    "Bounce, Glitch, Neon, Rainbow Wave and Sparks for nametags", true)
            .withInfo("Adds Bounce, Glitch, Neon, Rainbow_wave and Sparks to the Custom nametag effects. "
                    + "Only players with this mod see them.");
    public static final Toggle SPOTIFY_NAMETAG = ModSettings.toggle("spotifyNametag", "Song In Nametag",
                    "Show your song in the nametag even with the Spotify overlay off", true)
            .withInfo("Shows your current song in the nametag although the Spotify overlay is off. "
                    + "Needs \"Show your song\" in the NRC+ settings.");
    public static final Toggle SONG_COVERS = ModSettings.toggle("songCovers", "Song Covers",
                    "Share your song cover without NRC+ (local files too)", true)
            .withInfo("Shares your song cover through the server instead of NoRisk's NRC+ upload, local files too. "
                    + "Others see it with Server Sync.");
    public static final Toggle SHOP_CULLING = ModSettings.toggle("shopCulling", "Faster Shop",
                    "Skip drawing shop and wardrobe cards that are off screen", true)
            .withInfo("Doesn't draw shop and wardrobe cards that are scrolled out of view. Turn off if cards stay empty.");
    public static final Toggle FREEZE_WATCHDOG = ModSettings.toggle("freezeWatchdog", "Freeze Log",
                    "Write a thread dump to logs/nrc-freeze-*.txt when the game hangs for 10 s", true)
            .withInfo("If the game hangs for 10 seconds, writes what every thread was doing to logs/nrc-freeze-*.txt. "
                    + "Helps to find the cause of freezes.");

    private Features() {
    }

    /** Loads the class, so the core switches exist before anything reads them. */
    public static void init() {
    }
}
