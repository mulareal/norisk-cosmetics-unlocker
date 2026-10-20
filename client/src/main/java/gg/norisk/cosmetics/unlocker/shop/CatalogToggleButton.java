package gg.norisk.cosmetics.unlocker.shop;

import gg.norisk.owolib.owo.ui.component.TextureComponent;
import gg.norisk.owolib.owo.ui.component.UIComponents;
import gg.norisk.owolib.owo.ui.core.OwoUIGraphics;
import gg.norisk.owolib.owo.ui.core.Sizing;
import gg.norisk.owolib.owo.ui.util.UISounds;
import gg.norisk.ui.modules.v3.V3Button;
import gg.norisk.ui.modules.v3.V3Theme;
import kotlin.Unit;
import net.minecraft.network.chat.Component;

import java.util.function.BooleanSupplier;

/** Icon button next to the shop filter (same style as NoRisk's cart button) that toggles the catalog. */
public final class CatalogToggleButton extends V3Button {

    private final TextureComponent icon;
    private final BooleanSupplier active;

    public CatalogToggleButton(ShopCatalogBridge bridge, BooleanSupplier active, Runnable toggle) {
        super("", (button, mouseX, mouseY, click) -> {
            UISounds.playButtonSound();
            toggle.run();
            return Unit.INSTANCE;
        });
        this.active = active;
        this.icon = UIComponents.texture(bridge.icon(), 0, 0, 10, 10, 10, 10);
        icon.sizing(Sizing.fixed(10));
        child(icon);
        sizing(Sizing.fixed(13));
        tooltip(Component.literal(bridge.title()));
    }

    @Override
    public void draw(OwoUIGraphics context, int mouseX, int mouseY, float partialTicks, float delta) {
        boolean on = active.getAsBoolean();
        icon.setTint(on ? V3Theme.INSTANCE.accent(9) : 0xFFFFFFFF);
        super.draw(context, mouseX, mouseY, partialTicks, delta);
        if (on) context.drawRectOutline(x(), y(), width(), height(), V3Theme.INSTANCE.accent(8));
    }
}
