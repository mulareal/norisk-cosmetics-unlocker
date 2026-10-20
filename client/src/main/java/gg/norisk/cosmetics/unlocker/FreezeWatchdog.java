package gg.norisk.cosmetics.unlocker;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;

import java.lang.management.LockInfo;
import gg.norisk.cosmetics.unlocker.settings.Features;

import java.lang.management.ManagementFactory;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * If the client stops ticking for more than {@link #STALL_MS}, writes a full thread dump (with lock owners)
 * to logs/nrc-freeze-*.txt so freezes can be diagnosed after the game was killed.
 */
public final class FreezeWatchdog {

    private static final long STALL_MS = 10_000;
    private static volatile long lastTick = System.currentTimeMillis();

    private FreezeWatchdog() {
    }

    public static void start() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> lastTick = System.currentTimeMillis());
        Thread thread = new Thread(FreezeWatchdog::run, "nrc-freeze-watchdog");
        thread.setDaemon(true);
        thread.start();
    }

    private static void run() {
        long dumpedFor = 0;
        while (true) {
            try {
                Thread.sleep(2000);
                long tick = lastTick;
                if (Features.FREEZE_WATCHDOG.on() && System.currentTimeMillis() - tick > STALL_MS && dumpedFor != tick) {
                    dumpedFor = tick;
                    dump();
                }
            } catch (InterruptedException e) {
                return;
            } catch (Throwable t) {
                Log.warn("Watchdog error: " + t);
            }
        }
    }

    private static void dump() throws Exception {
        ThreadMXBean bean = ManagementFactory.getThreadMXBean();
        StringBuilder out = new StringBuilder("NRC freeze dump " + LocalDateTime.now() + "\n");
        long[] deadlocked = bean.findDeadlockedThreads();
        if (deadlocked != null) out.append("DEADLOCKED THREAD IDS: ").append(java.util.Arrays.toString(deadlocked)).append("\n");
        out.append("\n");

        for (ThreadInfo info : bean.dumpAllThreads(true, true)) {
            out.append('"').append(info.getThreadName()).append("\" id=").append(info.getThreadId())
                    .append(' ').append(info.getThreadState());
            LockInfo lock = info.getLockInfo();
            if (lock != null) out.append(" on ").append(lock);
            if (info.getLockOwnerName() != null) out.append(" owned by \"").append(info.getLockOwnerName()).append('"');
            out.append('\n');
            StackTraceElement[] stack = info.getStackTrace();
            MonitorInfo[] monitors = info.getLockedMonitors();
            int limit = info.getThreadName().contains("Render") ? stack.length : Math.min(stack.length, 40);
            for (int i = 0; i < limit; i++) {
                out.append("\tat ").append(stack[i]).append('\n');
                for (MonitorInfo monitor : monitors) {
                    if (monitor.getLockedStackDepth() == i) out.append("\t  - locked ").append(monitor).append('\n');
                }
            }
            for (LockInfo sync : info.getLockedSynchronizers()) out.append("\t  - locked sync ").append(sync).append('\n');
            out.append('\n');
        }

        Path dir = FabricLoader.getInstance().getGameDir().resolve("logs");
        Files.createDirectories(dir);
        Path file = dir.resolve("nrc-freeze-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")) + ".txt");
        Files.writeString(file, out, StandardCharsets.UTF_8);
        Log.warn("Client frozen >10s, thread dump: " + file);
    }
}
