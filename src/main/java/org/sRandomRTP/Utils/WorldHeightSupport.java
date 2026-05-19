package org.sRandomRTP.Utils;

import org.bukkit.World;

import java.lang.reflect.Method;

public final class WorldHeightSupport {

    private static final Method GET_MIN_HEIGHT = resolveGetMinHeight();

    private WorldHeightSupport() {
    }

    public static int getMinHeight(World world) {
        if (world == null || GET_MIN_HEIGHT == null) {
            return 0;
        }
        try {
            Object value = GET_MIN_HEIGHT.invoke(world);
            return value instanceof Number ? ((Number) value).intValue() : 0;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return 0;
        }
    }

    private static Method resolveGetMinHeight() {
        try {
            return World.class.getMethod("getMinHeight");
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return null;
        }
    }
}
