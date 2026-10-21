package gg.norisk.cosmetics.unlocker.settings;

import gg.norisk.cosmetics.unlocker.NoRiskCosmeticsClientMod;
import gg.norisk.ui.api.annotations.Category;
import gg.norisk.ui.api.module.ModuleCategory;
import gg.norisk.ui.api.value.Value;
import net.minecraft.resources.Identifier;

/**
 * "Cosmetics Unlocker" in NoRisk's mod menu: the card turns the whole mod on or off. Addons may subclass it
 * to add their switches below these (see {@link gg.norisk.cosmetics.unlocker.api.ClientAddon#settingsCard()}).
 */
public class UnlockerModule extends ToggleModule {

    private static final Identifier ICON =
            Identifier.fromNamespaceAndPath(NoRiskCosmeticsClientMod.MOD_ID, "textures/module/unlocker.png");
    private static final String[] SEO_TAGS = {"unlocker", "cosmetics", "emotes", "nrc+", "nametag", "icons"};

    // One field per switch, in menu order (see ToggleModule)
    @Category(name = "Unlocks")
    private final Value<Boolean> cosmetics$delegate = bind(Features.COSMETICS);
    @Category(name = "Unlocks")
    private final Value<Boolean> emotes$delegate = bind(Features.EMOTES);
    @Category(name = "Unlocks")
    private final Value<Boolean> nrcPlus$delegate = bind(Features.NRC_PLUS);
    @Category(name = "Unlocks")
    private final Value<Boolean> icons$delegate = bind(Features.ICONS);
    @Category(name = "Nametag")
    private final Value<Boolean> textEffects$delegate = bind(Features.TEXT_EFFECTS);
    @Category(name = "Nametag")
    private final Value<Boolean> spotifyNametag$delegate = bind(Features.SPOTIFY_NAMETAG);
    @Category(name = "Nametag")
    private final Value<Boolean> songCovers$delegate = bind(Features.SONG_COVERS);
    @Category(name = "Other")
    private final Value<Boolean> shopCulling$delegate = bind(Features.SHOP_CULLING);
    @Category(name = "Other")
    private final Value<Boolean> freezeWatchdog$delegate = bind(Features.FREEZE_WATCHDOG);

    public UnlockerModule() {
        super("Cosmetics Unlocker", ModuleCategory.COSMETIC, ModSettings.ENABLED);
    }

    @Override
    public Identifier getModuleIcon() {
        return ICON;
    }

    @Override
    public String[] getSeoTags() {
        return SEO_TAGS;
    }
}
