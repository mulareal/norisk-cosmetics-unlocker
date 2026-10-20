package gg.norisk.cosmetics.unlocker.shop;

/** Registry for the optional shop catalog. */
public final class ShopCatalog {

    private static volatile ShopCatalogBridge bridge;

    private ShopCatalog() {
    }

    public static void register(ShopCatalogBridge value) {
        bridge = value;
    }

    public static ShopCatalogBridge get() {
        return bridge;
    }

    public static boolean available() {
        ShopCatalogBridge current = bridge;
        return current != null && current.available();
    }
}
