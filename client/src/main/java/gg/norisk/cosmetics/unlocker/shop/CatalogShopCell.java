package gg.norisk.cosmetics.unlocker.shop;

import gg.norisk.owolib.owo.ui.component.LabelComponent;
import gg.norisk.owolib.owo.ui.container.FlexGridLayout;
import gg.norisk.owolib.owo.ui.container.FlowLayout;
import gg.norisk.owolib.owo.ui.core.OwoUIGraphics;
import gg.norisk.owolib.owo.ui.core.Sizing;
import gg.norisk.ui.modules.v3.V3Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Grid cell laid out like AbstractShopContentPanel.buildItemCell: one-line title above the card.
 * The title and card are built on the first draw, i.e. when the cell first scrolls into view.
 */
public final class CatalogShopCell extends FlowLayout {

    /** Cards fetched ahead of and behind a cell coming into view: two rows down, one row up. */
    private static final int AHEAD = 8, BEHIND = 4;

    private final ShopCatalogBridge.Item item;
    private final List<ShopCatalogBridge.Item> group;
    private final int index;
    private final FlexGridLayout grid;
    private LabelComponent title;

    public CatalogShopCell(List<ShopCatalogBridge.Item> group, int index, FlexGridLayout grid) {
        super(Sizing.fill(), Sizing.fill(), Algorithm.VERTICAL);
        this.item = group.get(index);
        this.group = group;
        this.index = index;
        this.grid = grid;
        allowOverflow(true);
    }

    @Override
    public void onFirstDraw() {
        super.onFirstDraw();
        title = new LabelComponent(Component.literal(item.name()).withColor(V3Theme.INSTANCE.gray(12)), 0.75f);
        child(title);
        child(new CatalogShopEntryComponent(item));
        prefetchNeighbours();
    }

    /** Lets the neighbours' files download while the user is still looking at this row. */
    private void prefetchNeighbours() {
        ShopCatalogBridge bridge = ShopCatalog.get();
        if (bridge == null) return;
        var player = Minecraft.getInstance().player;
        boolean slim = player != null && player.getSkin().model().toString().equalsIgnoreCase("SLIM");
        for (int i = Math.max(0, index - BEHIND); i <= Math.min(group.size() - 1, index + AHEAD); i++) {
            if (i != index) bridge.prefetch(group.get(i).id(), slim);
        }
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
