package gg.norisk.cosmetics.unlocker.shop;

import net.minecraft.resources.Identifier;

import java.util.List;

/**
 * Optional catalog shown inside the regular NoRisk cosmetics grid. Private addons can
 * register an implementation at runtime; the public build simply has no catalog.
 */
public interface ShopCatalogBridge {

    boolean available();

    /** Title (tooltip) and 16x16 icon of the toggle button next to the shop filter. */
    String title();

    Identifier icon();

    boolean ready();

    List<Item> items();

    boolean isSelected(int id);

    boolean toggle(int id);

    /** Shows the item on a shop preview entity until {@link #detachPreview(long)}. */
    void attachPreview(int id, long entityId, boolean slim);

    /** Shows the locally selected catalog items on the wardrobe's player preview until detached. */
    void attachOutfitPreview(long entityId);

    void detachPreview(long entityId);

    /**
     * Extreme points of the item as its card shows it, as xyz triples in blocks relative to the preview player's
     * feet (x = the player's left, y = up, z = the player's front); {@code null} to use the category's camera.
     */
    default float[] previewShape(int id, boolean slim) {
        return null;
    }

    /** Empty while loading or ready, otherwise a short error text for the card. */
    String previewError(int id);

    /**
     * @param type name of the NoRisk {@code CosmeticType} the item belongs to in the sidebar
     *             (e.g. {@code CAPE}, {@code WINGS}); used for category filtering and posing
     */
    record Item(int id, String name, String category, String label, String type) {
    }
}
