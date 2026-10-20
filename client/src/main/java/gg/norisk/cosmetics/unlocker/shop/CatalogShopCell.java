package gg.norisk.cosmetics.unlocker.shop;

import gg.norisk.owolib.owo.ui.component.LabelComponent;
import gg.norisk.owolib.owo.ui.container.FlexGridLayout;
import gg.norisk.owolib.owo.ui.container.FlowLayout;
import gg.norisk.owolib.owo.ui.core.OwoUIGraphics;
import gg.norisk.owolib.owo.ui.core.Sizing;
import gg.norisk.ui.modules.v3.V3Theme;
import net.minecraft.network.chat.Component;

/**
 * Grid cell laid out like AbstractShopContentPanel.buildItemCell: one-line title above the card.
 * The title and card are built on the first draw, i.e. when the cell first scrolls into view.
 */
public final class CatalogShopCell extends FlowLayout {

    private final ShopCatalogBridge.Item item;
    private final FlexGridLayout grid;
    private LabelComponent title;

    public CatalogShopCell(ShopCatalogBridge.Item item, FlexGridLayout grid) {
        super(Sizing.fill(), Sizing.fill(), Algorithm.VERTICAL);
        this.item = item;
        this.grid = grid;
        allowOverflow(true);
    }

    @Override
    public void onFirstDraw() {
        super.onFirstDraw();
        title = new LabelComponent(Component.literal(item.name()).withColor(V3Theme.INSTANCE.gray(12)), 0.75f);
        child(title);
        child(new CatalogShopEntryComponent(item));
    }

    @Override
    public void draw(OwoUIGraphics context, int mouseX, int mouseY, float partialTicks, float delta) {
        // NoRisk ellipsizes titles to the cell width instead of wrapping them.
        int cellWidth = grid == null ? 0 : grid.getCellWidth();
        if (title != null && cellWidth > 0) {
            int ellipsis = (int) (cellWidth * 1.75f);
            Integer current = title.getMaxEllipsis();
            if (current == null || current != ellipsis) title.setMaxEllipsis(ellipsis);
        }
        super.draw(context, mouseX, mouseY, partialTicks, delta);
    }
}
