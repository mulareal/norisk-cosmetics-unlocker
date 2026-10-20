package gg.norisk.cosmetics.unlocker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** The mod's logger ("NRC-Unlocker" in latest.log). */
public final class Log {

    private static final Logger LOGGER = LoggerFactory.getLogger("NRC-Unlocker");

    private Log() {
    }

    public static void info(String message) {
        LOGGER.info(message);
    }

    public static void warn(String message) {
        LOGGER.warn(message);
    }

    public static void debug(String message) {
        LOGGER.debug(message);
    }
}
