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
    
    @Override
    public Result filter(LogEvent event) {
        if (!enabled) {
            return Result.NEUTRAL;
        }
        
        if (event != null && event.getMessage() != null) {
            String message = event.getMessage().getFormattedMessage();
            if (message != null) {
                if (message.contains("moved too quickly!") || 
                    (message.contains("FoliaLib") && message.contains("tick based delay or timer was scheduled with a time span of 0 ticks"))) {
                    return Result.DENY;
                }
            }
        }
        
        return Result.NEUTRAL;
    }
    
    @Override
    public Result filter(Logger logger, Level level, Marker marker, Message msg, Throwable t) {
        if (!enabled) {
            return Result.NEUTRAL;
        }
        
        if (msg != null) {
            String message = msg.getFormattedMessage();
            if (message != null) {
                if (message.contains("moved too quickly!") || 
                    (message.contains("FoliaLib") && message.contains("tick based delay or timer was scheduled with a time span of 0 ticks"))) {
                    return Result.DENY;
                }
            }
        }
        
        return Result.NEUTRAL;
    }
    
    @Override
    public Result filter(Logger logger, Level level, Marker marker, String msg, Object... params) {
        if (!enabled) {
            return Result.NEUTRAL;
        }
        
        if (msg != null) {
            if (msg.contains("moved too quickly!") || 
                (msg.contains("FoliaLib") && msg.contains("tick based delay or timer was scheduled with a time span of 0 ticks"))) {
                return Result.DENY;
            }
        }
        
        return Result.NEUTRAL;
    }
    
    @Override
    public Result filter(Logger logger, Level level, Marker marker, Object msg, Throwable t) {
        if (!enabled) {
            return Result.NEUTRAL;
        }
        
        if (msg != null) {
            String message = msg.toString();
            if (message.contains("moved too quickly!") || 
                (message.contains("FoliaLib") && message.contains("tick based delay or timer was scheduled with a time span of 0 ticks"))) {
                return Result.DENY;
            }
        }
        
        return Result.NEUTRAL;
    }
} 