package gg.norisk.cosmetics.unlocker;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;

/** Short chat messages to the local player. */
public final class Chat {

    public static final String PREFIX = "§e[NRC] §f";

    private Chat() {
    }

    /** Shows {@code PREFIX + message}. Any thread. */
    public static void send(String message) {
        send(Component.literal(PREFIX + message));
    }

    public static void send(Component message) {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            if (mc.player != null) mc.player.sendSystemMessage(message);
        });
    }

    /** Clickable text that runs a command. */
    public static Component command(String label, String command) {
        return Component.literal(label).withStyle(style -> style.withClickEvent(new ClickEvent.RunCommand(command)));
    }
}
