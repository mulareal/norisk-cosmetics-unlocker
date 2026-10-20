package gg.norisk.cosmetics.unlocker.shop;

import gg.norisk.owolib.owo.ui.container.FlexGridLayout;
import gg.norisk.owolib.owo.ui.core.Insets;
import gg.norisk.owolib.owo.ui.core.Sizing;

import java.util.List;

/**
 * One category of the catalog page: a NoRisk-style grid holding every item of the category.
 * Cells stay empty until they first scroll into view, so thousands of items cost almost nothing.
 */
public final class CatalogGroup extends FlexGridLayout {

    public CatalogGroup(List<ShopCatalogBridge.Item> items) {
        // Same grid parameters as AbstractShopContentPanel.buildFilterableGrid.
        super(Sizing.fill(), Sizing.content(), 4, 3, 24, 1.0f);
        allowOverflow(true);
        padding(Insets.bottom(5));
        for (ShopCatalogBridge.Item item : items) child(new CatalogShopCell(item, this));
    }
}
