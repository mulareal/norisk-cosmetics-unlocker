package gg.norisk.cosmetics.unlocker.settings;

import gg.norisk.cosmetics.unlocker.Log;
import gg.norisk.cosmetics.unlocker.api.ClientHooks;
import gg.norisk.cosmetics.unlocker.settings.ModSettings.Toggle;
import gg.norisk.ui.api.module.Module;
import gg.norisk.ui.api.module.ModuleCategory;
import gg.norisk.ui.api.value.BooleanValue;
import gg.norisk.ui.api.value.Value;
import gg.norisk.ui.modules.api.ModuleProvider;
import kotlin.Unit;
import kotlin.jvm.internal.PropertyReference1Impl;
import net.minecraft.network.chat.Component;

/**
 * A card in NoRisk's mod menu backed by {@link ModSettings}: the card switch is {@code master}, the settings
 * page shows one switch per {@link #bind bound} toggle.
 * <p>
 * NoRisk builds the settings page by reflection: every value needs a field whose name ends in
 * {@code $delegate} (what Kotlin generates for {@code by} delegates), declared in the same order the values
 * are bound. So subclasses write {@code private final Value<?> name$delegate = bind(toggle);} per switch.
 * The descriptions come from the lang key {@code nrc.ui.modules.description.<name in snake_case>}.
 */
public abstract class ToggleModule extends Module {

    private final Toggle master;

    protected ToggleModule(String name, ModuleCategory category, Toggle master) {
        super(name, category, true, false, false);
        this.master = master;
    }

    /** Adds the module to the menu; logs instead of failing if NoRisk changed its API. */
    public static void register(ToggleModule module) {
        try {
            ModuleProvider.Companion.register(module);
        } catch (Throwable t) {
            Log.warn("Mod menu entry " + module.getName() + " not available: " + t);
        }
    }

    /** Creates the switch for {@code toggle}; assign it to a {@code <key>$delegate} field. */
    protected final Value<Boolean> bind(Toggle toggle) {
        BooleanValue value = new BooleanValue(toggle.defaultValue(), toggle.label(), Component.literal(toggle.description()), v -> {
            if (v != null && v != toggle.get()) {
                toggle.set(v);
                ClientHooks.settingsChanged();
            }
            return Unit.INSTANCE;
        });
        // provideDelegate adds the value to this module; the property only supplies the name
        value.provideDelegate(this, new PropertyReference1Impl(getClass(), toggle.key(), "get" + toggle.key() + "()Z", 0));
        value.set(toggle.get());
        if (toggle.info() != null) {
            value.setInfoText(Component.translatableWithFallback("nrcu.setting." + toggle.key() + ".info", toggle.info()));
        }
        return value;
    }

    @Override
    public boolean isEnabled() {
        return master.get();
    }

    @Override
    public void setEnabled(boolean enabled) {
        boolean changed = enabled != master.get();
        master.set(enabled);
        super.setEnabled(enabled);
        if (changed) ClientHooks.settingsChanged();
    }

    @Override
    public boolean isNew() {
        return false;
    }
}
