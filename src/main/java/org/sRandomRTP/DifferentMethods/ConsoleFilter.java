package org.sRandomRTP.DifferentMethods;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.filter.AbstractFilter;
import org.apache.logging.log4j.message.Message;

public class ConsoleFilter extends AbstractFilter {

    // Strings to suppress — extracted as constants so all four filter() overrides
    // share one definition and future updates are single-point.
    private static final String MOVED_TOO_QUICKLY = "moved too quickly!";
    private static final String FOLIA_LIB_ZERO_TICK = "tick based delay or timer was scheduled with a time span of 0 ticks";

    private static ConsoleFilter instance;
    private boolean enabled;

    public ConsoleFilter(boolean enabled) {
        super(Result.DENY, Result.NEUTRAL);
        this.enabled = enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public static void registerFilter(boolean enabled) {
        if (instance == null) {
            instance = new ConsoleFilter(enabled);
            Logger logger = (Logger) LogManager.getRootLogger();
            logger.addFilter(instance);
        } else {
            instance.setEnabled(enabled);
        }
    }

    public static void removeFilter() {
        if (instance != null) {
            instance.setEnabled(false);
            instance = null;
        }
    }

    /** Returns {@code true} if this message should be suppressed from the console. */
    private boolean shouldSuppress(String message) {
        if (message == null) return false;
        return message.contains(MOVED_TOO_QUICKLY)
                || (message.contains("FoliaLib") && message.contains(FOLIA_LIB_ZERO_TICK));
    }

    @Override
    public Result filter(LogEvent event) {
        if (!enabled) return Result.NEUTRAL;
        if (event != null && event.getMessage() != null
                && shouldSuppress(event.getMessage().getFormattedMessage())) {
            return Result.DENY;
        }
        return Result.NEUTRAL;
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, Message msg, Throwable t) {
        if (!enabled) return Result.NEUTRAL;
        if (msg != null && shouldSuppress(msg.getFormattedMessage())) return Result.DENY;
        return Result.NEUTRAL;
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String msg, Object... params) {
        if (!enabled) return Result.NEUTRAL;
        if (shouldSuppress(msg)) return Result.DENY;
        return Result.NEUTRAL;
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, Object msg, Throwable t) {
        if (!enabled) return Result.NEUTRAL;
        if (msg != null && shouldSuppress(msg.toString())) return Result.DENY;
        return Result.NEUTRAL;
    }
}
