package gg.norisk.cosmetics.unlocker.shop;

import gg.norisk.abstraction.text.TextKt;
import gg.norisk.cosmetics.v2.ui.ShopEntryComponent;
import gg.norisk.cosmetics.v2.ui.ShopEntryData;
import gg.norisk.networking.model.cosmetics.CosmeticRarity;
import gg.norisk.owolib.owo.ui.component.LabelComponent;
import gg.norisk.owolib.owo.ui.util.UISounds;
import gg.norisk.ui.modules.v3.V3Theme;
import net.minecraft.network.chat.Component;

/** A regular NoRisk showcase card backed by an entry from the optional catalog. */
public final class CatalogShopEntryComponent extends ShopEntryComponent {

    public CatalogShopEntryComponent(ShopCatalogBridge.Item item) {
        super(rarity(item.type()), new ShopEntryData());

        setSelected(() -> {
            ShopCatalogBridge bridge = ShopCatalog.get();
            return bridge != null && bridge.isSelected(item.id());
        });

        getShowcase().child(new CatalogPreviewComponent(item));

        Component tagText = TextKt.toSmallCapsText(Component.literal("LUNAR").withColor(V3Theme.INSTANCE.accent(8)));
        LabelComponent tag = new LabelComponent(tagText, 0.55f);
        tag.shadow(true);
        getShowcaseFooter().allowOverflow(true);
        getShowcaseFooter().child(tag);

        getShowcase().mouseDown().subscribe((event, doubled) -> {
            UISounds.playInteractionSound();
            ShopCatalogBridge bridge = ShopCatalog.get();
            if (bridge != null) bridge.toggle(item.id());
            return true;
        });
    }

    private static CosmeticRarity rarity(String type) {
        return switch (type == null ? "" : type) {
            case "WINGS", "AURA" -> CosmeticRarity.LEGENDARY;
            case "PET", "SUIT" -> CosmeticRarity.EPIC;
            case "CAPE", "BACK", "SHIELD" -> CosmeticRarity.RARE;
            case "HAT", "FACE", "ACCESSORIES" -> CosmeticRarity.UNCOMMON;
            default -> CosmeticRarity.COMMON;
        };
    }
}
