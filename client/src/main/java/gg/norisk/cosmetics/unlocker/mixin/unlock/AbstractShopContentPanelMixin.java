package gg.norisk.cosmetics.unlocker.mixin.unlock;

import gg.norisk.abstraction.text.TextKt;
import gg.norisk.cosmetics.unlocker.shop.CatalogGroup;
import gg.norisk.cosmetics.unlocker.shop.CatalogToggleButton;
import gg.norisk.cosmetics.unlocker.shop.ShopCatalog;
import gg.norisk.cosmetics.unlocker.shop.ShopCatalogBridge;
import gg.norisk.cosmetics.v2.ui.ShopDivider;
import gg.norisk.cosmetics.v2.ui.ShopFilter;
import gg.norisk.cosmetics.v2.ui.newwardrobe.AbstractShopContentPanel;
import gg.norisk.cosmetics.v2.ui.newwardrobe.CosmeticContentPanel;
import gg.norisk.cosmetics.v2.ui.newwardrobe.NewWardrobeContainer;
import gg.norisk.owolib.owo.ui.component.LabelComponent;
import gg.norisk.owolib.owo.ui.container.FlowLayout;
import gg.norisk.owolib.owo.ui.container.UIContainers;
import gg.norisk.owolib.owo.ui.core.HorizontalAlignment;
import gg.norisk.owolib.owo.ui.core.Insets;
import gg.norisk.owolib.owo.ui.core.Sizing;
import gg.norisk.owolib.owo.ui.core.VerticalAlignment;
import gg.norisk.networking.model.cosmetics.CosmeticType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;

/**
 * Adds a toggle button for the optional catalog next to the cosmetics filter. While it is
 * active the catalog is laid out like NoRisk's own page: one divider + grid per sidebar
 * category, registered in {@code categoryDividers} so sidebar clicks scroll to the group
 * and the sidebar follows the scroll position.
 */
@Mixin(AbstractShopContentPanel.class)
public abstract class AbstractShopContentPanelMixin {

    @Shadow
    @Final
    private FlowLayout scrollChild;
    @Shadow
    @Final
    private FlowLayout leftGroup;
    @Shadow
    @Final
    private NewWardrobeContainer shop;
    @Shadow
    @Final
    private Map<String, ShopDivider> categoryDividers;
    @Shadow
    @Final
    private Map<String, ?> categoryGridMap;
    @Shadow
    @Final
    private List<?> filterableGrids;
    @Shadow
    private String searchText;
    @Shadow
    private Object currentCategory;

    @Shadow
    protected abstract void applyFilters();

    @Shadow
    protected abstract void rebuildGrid(Object category);

    @Shadow
    protected abstract void scrollToCategory(String categoryName, boolean instant);

    @Unique
    private boolean nrc$catalogMode;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void nrc$installCatalogButton(NewWardrobeContainer shop, Sizing horizontalSizing, Sizing verticalSizing, CallbackInfo ci) {
        if (!((Object) this instanceof CosmeticContentPanel)) return;
        ShopCatalogBridge bridge = ShopCatalog.get();
        if (bridge == null || !bridge.available()) return;
        leftGroup.child(new CatalogToggleButton(bridge, () -> nrc$catalogMode, () -> nrc$setCatalogMode(!nrc$catalogMode)));
    }

    /** Search changes end up here; in catalog mode they rebuild the catalog page. */
    @Inject(method = "applyFilters", at = @At("HEAD"), cancellable = true)
    private void nrc$applyCatalogFilters(CallbackInfo ci) {
        if (!nrc$catalogMode) return;
        nrc$buildCatalogPage();
        ci.cancel();
    }

    /** Picking a NoRisk filter (all, owned, ...) returns to the regular items. */
    @Inject(method = "onFilterSelected", at = @At("HEAD"))
    private void nrc$leaveCatalogOnFilter(ShopFilter filter, CallbackInfo ci) {
        if (nrc$catalogMode) nrc$setCatalogMode(false);
    }

    @Unique
    private void nrc$setCatalogMode(boolean enabled) {
        nrc$catalogMode = enabled;
        if (enabled) {
            nrc$buildCatalogPage();
        } else {
            rebuildGrid(currentCategory);
            applyFilters();
        }
    }

    @Unique
    private void nrc$buildCatalogPage() {
        ShopCatalogBridge bridge = ShopCatalog.get();
        if (bridge == null) return;

        String query = searchText == null ? "" : searchText.trim().toLowerCase(Locale.ROOT);
        Map<String, List<ShopCatalogBridge.Item>> groups = new LinkedHashMap<>();
        for (CosmeticType type : CosmeticType.values()) groups.put(type.name(), new ArrayList<>());
        for (ShopCatalogBridge.Item item : bridge.items()) {
            if (!query.isEmpty()
                    && !item.name().toLowerCase(Locale.ROOT).contains(query)
                    && !item.label().toLowerCase(Locale.ROOT).contains(query)
                    && !item.category().toLowerCase(Locale.ROOT).contains(query)) continue;
            groups.computeIfAbsent(item.type(), key -> new ArrayList<>()).add(item);
        }

        scrollChild.clearChildren();
        filterableGrids.clear();
        categoryGridMap.clear();
        categoryDividers.clear();

        boolean any = false;
        for (Map.Entry<String, List<ShopCatalogBridge.Item>> group : groups.entrySet()) {
            if (group.getValue().isEmpty()) continue;
            any = true;
            CatalogGroup content = new CatalogGroup(group.getValue());
            ShopDivider divider = new ShopDivider(group.getKey(), content, null, () -> true);
            categoryDividers.put(group.getKey(), divider);
            scrollChild.child(divider);
        }

        if (!any) {
            FlowLayout empty = UIContainers.verticalFlow(Sizing.fill(), Sizing.content());
            empty.alignment(HorizontalAlignment.CENTER, VerticalAlignment.CENTER);
            empty.padding(Insets.of(12));
            String message = bridge.ready() ? "Keine Lunar-Cosmetics gefunden" : "Lunar-Katalog wird geladen ...";
            LabelComponent label = new LabelComponent(TextKt.toSmallCapsText(message), 0.8f);
            label.setBrightnessMultiplier(0.72f);
            empty.child(label);
            scrollChild.child(empty);
        }
        String target = currentCategory instanceof CosmeticType type && categoryDividers.containsKey(type.name()) ? type.name() : null;
        scrollToCategory(target, true);
    }
}
