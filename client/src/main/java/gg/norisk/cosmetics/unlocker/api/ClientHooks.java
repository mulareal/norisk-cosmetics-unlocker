package gg.norisk.cosmetics.unlocker.api;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Extension points of the mod. Mixins and core code fire these; {@link ClientAddon}s listen.
 * Without addons every hook is a no-op and the mod behaves exactly the same.
 */
public final class ClientHooks {

    /** The local player's active outfit changed (wardrobe toggle or outfit switch). Client thread. */
    public static final HookList<Runnable> OUTFIT_CHANGED = new HookList<>("outfit_changed");

    /** The local player's nametag (custom or additional) changed and was saved. Client thread. */
    public static final HookList<Runnable> NAMETAG_CHANGED = new HookList<>("nametag_changed");

    /** The local player started ({@code true}) or stopped an emote. {@code "all"} = stop every emote. */
    public static final HookList<BiConsumer<String, Boolean>> LOCAL_EMOTE = new HookList<>("local_emote");

    /** A player entity was created on the client. Client thread. */
    public static final HookList<Consumer<AbstractClientPlayer>> PLAYER_LOADED = new HookList<>("player_loaded");

    /** The local song cover was stored under this id (SHA-256 of the bytes). Any thread. */
    public static final HookList<BiConsumer<String, byte[]>> ARTWORK_SHARED = new HookList<>("artwork_shared");

    /** Extra lines for {@code /nrcu status}. */
    public static final HookList<Consumer<List<Component>>> STATUS = new HookList<>("status");

    /** Add subcommands to {@code /nrcu}. Fired once while the client commands are registered. */
    public static final HookList<Consumer<LiteralArgumentBuilder<FabricClientCommandSource>>> COMMAND = new HookList<>("command");

    /** Players allowed more than 3 NRC+ nametag effects. Default: nobody. */
    public static volatile Predicate<String> unlimitedNametag = name -> false;

    /** Handles partner emotes instead of NoRisk's backend when set and {@link PartnerEmoteRouter#isActive()}. */
    public static volatile PartnerEmoteRouter partnerEmotes;

    private ClientHooks() {
    }

    public static void outfitChanged() {
        OUTFIT_CHANGED.fire(Runnable::run);
    }

    public static void nametagChanged() {
        NAMETAG_CHANGED.fire(Runnable::run);
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
