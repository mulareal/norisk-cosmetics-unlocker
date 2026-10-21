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

    /** Small-caps tag in each card's footer; blank for none. */
    default String tag() {
        return "";
    }

    /** Shown instead of the grid when no item matches (or the catalog is still loading). */
    default String emptyText() {
        return ready() ? "Nichts gefunden" : "Wird geladen ...";
    }

    boolean ready();

    List<Item> items();

    boolean isSelected(int id);

    boolean toggle(int id);

    /** Shows the item on a shop preview entity until {@link #detachPreview(long)}. */
    void attachPreview(int id, long entityId, boolean slim);

    /** Shows the locally selected catalog items on the wardrobe's player preview until detached. */
    void attachOutfitPreview(long entityId);

    /** Shows the catalog items saved with NoRisk outfit {@code outfitId} (hex id) on its outfit card until detached. */
    default void attachOutfitPreview(long entityId, String outfitId) {
    }

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

    /** Fetches the item's files ahead of its card scrolling into view. */
    default void prefetch(int id, boolean slim) {
    }

    /** The locally selected items, in selection order. */
    default List<Integer> selection() {
        return List.of();
    }

    /** Replaces the local selection; an empty list clears it. */
    default void select(List<Integer> ids) {
    }

    /**
     * @param type name of the NoRisk {@code CosmeticType} the item belongs to in the sidebar
     *             (e.g. {@code CAPE}, {@code WINGS}); used for category filtering and posing
     */
    record Item(int id, String name, String category, String label, String type) {
    }
}
