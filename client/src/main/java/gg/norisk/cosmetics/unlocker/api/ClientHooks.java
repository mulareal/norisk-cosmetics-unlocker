package gg.norisk.cosmetics.unlocker.api;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;

import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Extension points of the mod. Mixins and core code fire these; {@link ClientAddon}s listen.
 * Without addons every hook is a no-op and the mod behaves exactly the same.
 */
public final class ClientHooks {

    /** The local player's active outfit changed (wardrobe toggle or outfit switch). Client thread. */
    public static final HookList<Runnable> OUTFIT_CHANGED = new HookList<>("outfit_changed");

    /**
     * The local player switched from one saved outfit to another in the wardrobe (hex outfit ids; the
     * previous one is null if unknown). Fired before the new one is stored as active. Client thread.
     */
    public static final HookList<BiConsumer<String, String>> OUTFIT_SELECTED = new HookList<>("outfit_selected");

    /** The local player's nametag (custom or additional) changed and was saved. Client thread. */
    public static final HookList<Runnable> NAMETAG_CHANGED = new HookList<>("nametag_changed");

    /** The local player started ({@code true}) or stopped an emote. {@code "all"} = stop every emote. */
    public static final HookList<BiConsumer<String, Boolean>> LOCAL_EMOTE = new HookList<>("local_emote");

    /** A player entity was created on the client. Client thread. */
    public static final HookList<Consumer<AbstractClientPlayer>> PLAYER_LOADED = new HookList<>("player_loaded");

    /** The local song cover was stored under this id (SHA-256 of the bytes). Any thread. */
    public static final HookList<BiConsumer<String, byte[]>> ARTWORK_SHARED = new HookList<>("artwork_shared");

    /** A switch in the mod menu changed (see {@code ModSettings}). Client thread. */
    public static final HookList<Runnable> SETTINGS_CHANGED = new HookList<>("settings_changed");

    /** Extra lines for {@code /nrcu status}. */
    public static final HookList<Consumer<List<Component>>> STATUS = new HookList<>("status");

    /** Add subcommands to {@code /nrcu}. Fired once while the client commands are registered. */
    public static final HookList<Consumer<LiteralArgumentBuilder<FabricClientCommandSource>>> COMMAND = new HookList<>("command");

    /** Players allowed more than 3 NRC+ nametag effects. Default: nobody. */
    public static volatile Predicate<String> unlimitedNametag = name -> false;

    /** Handles partner emotes instead of NoRisk's backend when set and {@link PartnerEmoteRouter#isActive()}. */
    public static volatile PartnerEmoteRouter partnerEmotes;

    /**
     * Replaces a player's cape texture, so cape mods animate it like a normal cape. {@link #NO_CAPE} hides the
     * cape; {@code null} keeps it. The elytra is drawn with the new cape like with a normal one. Render thread.
     */
    public static volatile Function<AbstractClientPlayer, Identifier> capeOverride;

    public static final Identifier NO_CAPE = Identifier.fromNamespaceAndPath("norisk-cosmetics-unlocker", "no_cape");

    /** Draws the shield a player holds in a display context instead of NoRisk or vanilla; {@code null} keeps it. */
    public static volatile BiFunction<AbstractClientPlayer, ItemDisplayContext, HeldShield> heldShield;

    private ClientHooks() {
    }

    public static void outfitChanged() {
        OUTFIT_CHANGED.fire(Runnable::run);
    }

    public static void outfitSelected(String previous, String next) {
        OUTFIT_SELECTED.fire(listener -> listener.accept(previous, next));
    }

    /** Hex id of the local player's active saved outfit, or null. */
    public static String activeOutfitId() {
        try {
            var outfit = gg.norisk.cosmetics.unlocker.cosmetics.LocalOutfitStorage.getActiveOutfit();
            return outfit != null && outfit.getOutfitId() != null ? outfit.getOutfitId().toHexString() : null;
        } catch (Throwable t) {
            return null;
        }
    }

    public static void nametagChanged() {
        NAMETAG_CHANGED.fire(Runnable::run);
    }

    public static void settingsChanged() {
        SETTINGS_CHANGED.fire(Runnable::run);
    }

    public static void localEmote(String emoteId, boolean playing) {
        LOCAL_EMOTE.fire(listener -> listener.accept(emoteId, playing));
    }

    /** Partner (interactive) emote requests of the local player. */
    public interface PartnerEmoteRouter {
        boolean isActive();

        void start(UUID emote, int maxPlayers, int randomCount);

        void join(String sessionId);

        void leave(String sessionId);
    }
}
