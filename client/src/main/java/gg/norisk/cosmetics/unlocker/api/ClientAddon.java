package gg.norisk.cosmetics.unlocker.api;

/**
 * Optional module that extends the mod through {@link ClientHooks}. Addons are found with
 * {@link java.util.ServiceLoader}: list the implementation in
 * {@code META-INF/services/gg.norisk.cosmetics.unlocker.api.ClientAddon}.
 */
public interface ClientAddon {

    /** Short name for logs. */
    String name();

    /** Called once at the end of client init. Register hooks, events and commands here. */
    void init();
}
