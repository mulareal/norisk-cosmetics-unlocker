package gg.norisk.cosmetics.unlocker;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import gg.norisk.cosmetics.unlocker.api.ClientHooks;
import gg.norisk.cosmetics.unlocker.compat.CompatCheck;
import gg.norisk.cosmetics.unlocker.cosmetics.LocalOutfitStorage;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Client-side commands: {@code /nrcicon} and {@code /nrcu [status]}. Addons add subcommands via {@link ClientHooks#COMMAND}. */
final class NrcCommands {

    private static final List<String> ICON_SUGGESTIONS = List.of("vip", "gold", "silver", "bronze", "dev", "admin", "partner", "special", "bug", "default");
    private static final Map<String, String> ICON_ALIASES = Map.of(
            "gold", "gold_donator",
            "silver", "silver_donator",
            "bronze", "bronze_donator",
            "dev", "developer",
            "special", "special_rank",
            "bug", "bug_hunter"
    );

    private NrcCommands() {
    }

    static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, buildContext) -> {
            dispatcher.register(ClientCommands.literal("nrcicon")
                    .executes(ctx -> {
                        showIconHelp(ctx.getSource());
                        return 1;
                    })
                    .then(ClientCommands.argument("name", StringArgumentType.word())
                            .suggests((ctx, builder) -> {
                                ICON_SUGGESTIONS.forEach(builder::suggest);
                                return builder.buildFuture();
                            })
                            .executes(ctx -> {
                                String name = StringArgumentType.getString(ctx, "name").toLowerCase(Locale.ROOT);
                                name = ICON_ALIASES.getOrDefault(name, name);
                                LocalOutfitStorage.setActiveIconName(name);
                                ctx.getSource().sendFeedback(Component.literal("§a[NRC] Icon geändert zu: §6" + name.toUpperCase(Locale.ROOT) + "§a!"));
                                return 1;
                            })));

            LiteralArgumentBuilder<FabricClientCommandSource> nrcu = ClientCommands.literal("nrcu")
                    .executes(ctx -> {
                        showStatus(ctx.getSource());
                        return 1;
                    })
                    .then(ClientCommands.literal("status").executes(ctx -> {
                        showStatus(ctx.getSource());
                        return 1;
                    }));
            ClientHooks.COMMAND.fire(listener -> listener.accept(nrcu));
            dispatcher.register(nrcu);
        });
    }

    private static void showStatus(FabricClientCommandSource source) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal(Chat.PREFIX + "Mod " + NoRiskCosmeticsClientMod.version()));
        List<String> issues = CompatCheck.issues();
        lines.add(Component.literal(issues.isEmpty() ? "§7Hooks: §aOK" : "§7Hooks kaputt: §c" + String.join(", ", issues)));
        ClientHooks.STATUS.fire(listener -> listener.accept(lines));
        lines.forEach(source::sendFeedback);
    }

    private static void showIconHelp(FabricClientCommandSource source) {
        source.sendFeedback(Component.literal("§e[NRC] Aktuelles Icon: §6" + LocalOutfitStorage.getActiveIconName().toUpperCase(Locale.ROOT)));
        source.sendFeedback(Component.literal("§7Befehl: §f/nrcicon <name>"));
        source.sendFeedback(Component.literal("§6vip §7(👑 VIP Krone), §6gold §7(🏆 Gold Krone), §6dev §7(🛠️), §6admin §7(🛡️), §6partner §7(🤝), §6special §7(🌟), §6bug §7(🐛), §6default §7(⚡)"));
    }
}
