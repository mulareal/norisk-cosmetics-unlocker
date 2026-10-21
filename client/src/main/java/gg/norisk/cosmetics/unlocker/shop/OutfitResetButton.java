package gg.norisk.cosmetics.unlocker.shop;

import gg.norisk.cosmetics.v2.model.NrcCosmetic;
import gg.norisk.cosmetics.v2.ui.newwardrobe.NewWardrobeContainer;
import gg.norisk.networking.model.cosmetics.CosmeticSettings;
import gg.norisk.owolib.owo.ui.component.TextureComponent;
import gg.norisk.owolib.owo.ui.component.UIComponents;
import gg.norisk.owolib.owo.ui.core.OwoUIGraphics;
import gg.norisk.owolib.owo.ui.core.Sizing;
import gg.norisk.owolib.owo.ui.util.UISounds;
import gg.norisk.ui.modules.v3.V3Button;
import gg.norisk.ui.modules.v3.V3Theme;
import kotlin.Unit;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.List;

/**
 * Icon button next to the shop filter (same style as the catalog toggle) that takes off everything worn: NoRisk
 * cosmetics and the catalog selection. For a few seconds afterwards it turns into an undo button.
 */
public final class OutfitResetButton extends V3Button {

    private static final Identifier RESET = Identifier.fromNamespaceAndPath("norisk-cosmetics-unlocker", "textures/gui/reset.png");
    private static final Identifier UNDO = Identifier.fromNamespaceAndPath("norisk-cosmetics-unlocker", "textures/gui/undo.png");
    private static final long UNDO_MS = 8000L;
    private static final int DIM = 0xFF6E6E78;

    private record Worn(NrcCosmetic cosmetic, CosmeticSettings settings) {
    }

    private final NewWardrobeContainer shop;
    private final TextureComponent resetIcon = icon(RESET);
    private final TextureComponent undoIcon = icon(UNDO);
    private List<Worn> worn = List.of();
    private List<Integer> catalog = List.of();
    private long undoUntil;
    private boolean showingUndo;

    public OutfitResetButton(NewWardrobeContainer shop) {
        super("", (button, mouseX, mouseY, click) -> {
            ((OutfitResetButton) button).click();
            return Unit.INSTANCE;
        });
        this.shop = shop;
        child(resetIcon);
        sizing(Sizing.fixed(13));
        tooltip(Component.literal("Alles ablegen"));
    }

    private static TextureComponent icon(Identifier texture) {
        TextureComponent icon = UIComponents.texture(texture, 0, 0, 10, 10, 10, 10);
        icon.sizing(Sizing.fixed(10));
        return icon;
    }

    private boolean undoing() {
        return System.currentTimeMillis() < undoUntil;
    }

    private void click() {
        if (undoing()) undo();
        else reset();
    }

    private void reset() {
        var data = shop.getPreviewData();
        List<NrcCosmetic> equipped = List.copyOf(data.getEquipped());
        ShopCatalogBridge bridge = ShopCatalog.get();
        List<Integer> selection = bridge == null ? List.of() : List.copyOf(bridge.selection());
        if (equipped.isEmpty() && selection.isEmpty()) return;
        UISounds.playButtonSound();
        worn = equipped.stream().map(cosmetic -> new Worn(cosmetic, data.getSettings(cosmetic.getId()))).toList();
        catalog = selection;
        // NoRisk's own toggle takes variants along, refreshes the preview and saves the outfit.
        for (NrcCosmetic cosmetic : equipped) {
            if (data.getEquippedCosmeticIds().contains(cosmetic.getId())) shop.toggleCosmetic(cosmetic, null, true, false, false);
        }
        if (bridge != null && !selection.isEmpty()) bridge.select(List.of());
        undoUntil = System.currentTimeMillis() + UNDO_MS;
    }

    private void undo() {
        UISounds.playButtonSound();
        for (Worn item : worn) shop.toggleCosmetic(item.cosmetic(), item.settings(), false, false, false);
        ShopCatalogBridge bridge = ShopCatalog.get();
        if (bridge != null && !catalog.isEmpty()) bridge.select(catalog);
        worn = List.of();
        catalog = List.of();
        undoUntil = 0L;
    }

    private boolean anythingWorn() {
        if (!shop.getPreviewData().getEquipped().isEmpty()) return true;
        ShopCatalogBridge bridge = ShopCatalog.get();
        return bridge != null && bridge.available() && !bridge.selection().isEmpty();
    }

    @Override
    public void draw(OwoUIGraphics context, int mouseX, int mouseY, float partialTicks, float delta) {
        boolean undo = undoing();
        if (undo != showingUndo) {
            showingUndo = undo;
            removeChild(undo ? resetIcon : undoIcon);
            child(undo ? undoIcon : resetIcon);
            tooltip(Component.literal(undo ? "Rückgängig" : "Alles ablegen"));
        }
        if (undo) undoIcon.setTint(V3Theme.INSTANCE.accent(9));
        else resetIcon.setTint(anythingWorn() ? 0xFFFFFFFF : DIM);
        super.draw(context, mouseX, mouseY, partialTicks, delta);
        if (undo) context.drawRectOutline(x(), y(), width(), height(), V3Theme.INSTANCE.accent(8));
    }
}
