package org.sRandomRTP.Services;

import org.bukkit.Bukkit;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Locale;

/**
 * Provides TPS/MSPT readings across different server families.
 *
 * <p>The provider prefers stable public API where available and then falls back to
 * CraftBukkit/Spigot-style reflection paths. Unsupported runtimes simply report the metric
 * as unavailable instead of throwing or returning misleading values.</p>
 */
public class ServerMetricsProvider {

    interface RuntimeServerAccess {
        Object getBukkitServer();
    }

    private static final String[] TPS_FIELD_CANDIDATES = {
            "recentTps",
            "recentTPS"
    };

    private static final String[] TICK_TIME_FIELD_CANDIDATES = {
            "tickTimes",
            "recentTickTimes"
    };

    private static final String[] TICK_TIME_METHOD_CANDIDATES = {
            "getTickTimes",
            "getRecentTickTimes"
    };

    private static final String[] AVERAGE_TICK_METHOD_CANDIDATES = {
            "getAverageTickTime",
            "getAverageTickMillis",
            "getAverageMspt"
    };

    private final RuntimeServerAccess runtimeServerAccess;

    private volatile Class<?> resolvedServerClass;
    private volatile Method directGetTpsMethod;
    private volatile Method directAverageTickTimeMethod;
    private volatile Method directTickTimesMethod;
    private volatile Method spigotMethod;
    private volatile Method spigotGetTpsMethod;
    private volatile Method spigotTickTimesMethod;
    private volatile Method craftGetServerMethod;
    private volatile Method internalAverageTickTimeMethod;
    private volatile Method internalTickTimesMethod;
    private volatile Field internalRecentTpsField;
    private volatile Field internalTickTimesField;

    public ServerMetricsProvider() {
        this(new RuntimeServerAccess() {
            @Override
            public Object getBukkitServer() {
                return Bukkit.getServer();
            }
        });
    }

    ServerMetricsProvider(RuntimeServerAccess runtimeServerAccess) {
        this.runtimeServerAccess = runtimeServerAccess;
    }

    public boolean isMetricAvailable(AdminBarType type) {
        if (type == null) {
            return false;
        }
        switch (type) {
            case TPS:
                return !Double.isNaN(getPrimaryTps());
            case MSPT:
                return !Double.isNaN(getAverageTickTimeMs());
            case RAM:
                return true;
            default:
                return false;
        }
    }

    public double getPrimaryTps() {
        Object server = getServer();
        if (server == null) {
            return Double.NaN;
        }
        ensureResolved(server);

        double direct = extractTps(invoke(server, directGetTpsMethod));
        if (!Double.isNaN(direct)) {
            return direct;
        }

        Object spigot = invoke(server, spigotMethod);
        double spigotValue = extractTps(invoke(spigot, spigotGetTpsMethod));
        if (!Double.isNaN(spigotValue)) {
            return spigotValue;
        }

        Object internalServer = invoke(server, craftGetServerMethod);
        double internalValue = extractTps(readField(internalServer, internalRecentTpsField));
        if (!Double.isNaN(internalValue)) {
            return internalValue;
        }

        return Double.NaN;
    }

    public double getAverageTickTimeMs() {
        Object server = getServer();
        if (server == null) {
            return Double.NaN;
        }
        ensureResolved(server);

        double directAverage = extractSingleNumber(invoke(server, directAverageTickTimeMethod));
        if (!Double.isNaN(directAverage)) {
            return directAverage;
        }

        double directTickTimesAverage = averageTickTimes(invoke(server, directTickTimesMethod));
        if (!Double.isNaN(directTickTimesAverage)) {
            return directTickTimesAverage;
        }

        Object spigot = invoke(server, spigotMethod);
        double spigotTickTimesAverage = averageTickTimes(invoke(spigot, spigotTickTimesMethod));
        if (!Double.isNaN(spigotTickTimesAverage)) {
            return spigotTickTimesAverage;
        }

        Object internalServer = invoke(server, craftGetServerMethod);
        double internalAverage = extractSingleNumber(invoke(internalServer, internalAverageTickTimeMethod));
        if (!Double.isNaN(internalAverage)) {
            return internalAverage;
        }

        double internalTickTimesAverage = averageTickTimes(invoke(internalServer, internalTickTimesMethod));
        if (!Double.isNaN(internalTickTimesAverage)) {
            return internalTickTimesAverage;
        }

        internalTickTimesAverage = averageTickTimes(readField(internalServer, internalTickTimesField));
        if (!Double.isNaN(internalTickTimesAverage)) {
            return internalTickTimesAverage;
        }

        return Double.NaN;
    }

    private Object getServer() {
        return runtimeServerAccess == null ? null : runtimeServerAccess.getBukkitServer();
    }

    private synchronized void ensureResolved(Object server) {
        if (server == null) {
            return;
        }
        Class<?> serverClass = server.getClass();
        if (serverClass == resolvedServerClass) {
            return;
        }

        directGetTpsMethod = findNoArgMethod(serverClass, "getTPS");
        directAverageTickTimeMethod = findFirstNoArgMethod(serverClass, AVERAGE_TICK_METHOD_CANDIDATES);
        directTickTimesMethod = findFirstNoArgMethod(serverClass, TICK_TIME_METHOD_CANDIDATES);
        spigotMethod = findNoArgMethod(serverClass, "spigot");
        craftGetServerMethod = findNoArgMethod(serverClass, "getServer");

        Object spigotDelegate = invoke(server, spigotMethod);
        Class<?> spigotClass = spigotDelegate == null ? null : spigotDelegate.getClass();
        spigotGetTpsMethod = spigotClass == null ? null : findNoArgMethod(spigotClass, "getTPS");
        spigotTickTimesMethod = spigotClass == null ? null : findFirstNoArgMethod(spigotClass, TICK_TIME_METHOD_CANDIDATES);

        Object internalServer = invoke(server, craftGetServerMethod);
        Class<?> internalClass = internalServer == null ? null : internalServer.getClass();
        internalAverageTickTimeMethod = internalClass == null ? null : findFirstNoArgMethod(internalClass, AVERAGE_TICK_METHOD_CANDIDATES);
        internalTickTimesMethod = internalClass == null ? null : findFirstNoArgMethod(internalClass, TICK_TIME_METHOD_CANDIDATES);
        internalRecentTpsField = internalClass == null ? null : findFirstField(internalClass, TPS_FIELD_CANDIDATES);
        internalTickTimesField = internalClass == null ? null : findFirstField(internalClass, TICK_TIME_FIELD_CANDIDATES);

        resolvedServerClass = serverClass;
    }

    private Method findFirstNoArgMethod(Class<?> type, String[] candidates) {
        if (type == null || candidates == null) {
            return null;
        }
        for (String candidate : candidates) {
            Method method = findNoArgMethod(type, candidate);
            if (method != null) {
                return method;
            }
        }
        return null;
    }

    private Method findNoArgMethod(Class<?> type, String name) {
        Class<?> current = type;
        while (current != null && current != Object.class) {
            if (name == null) {
                return null;
            }
            try {
                Method method = current.getMethod(name);
                method.setAccessible(true);
                return method;
            } catch (NoSuchMethodException | RuntimeException ignored) {
            }
            try {
                Method method = current.getDeclaredMethod(name);
                method.setAccessible(true);
                return method;
            } catch (NoSuchMethodException | RuntimeException ignored) {
            }
            current = current.getSuperclass();
        }
        return null;
    }

    private Field findFirstField(Class<?> type, String[] candidates) {
        Class<?> current = type;
        while (current != null && current != Object.class) {
            if (candidates == null) {
                return null;
            }
            for (String candidate : candidates) {
                try {
                    Field field = current.getField(candidate);
                    field.setAccessible(true);
                    return field;
                } catch (NoSuchFieldException | RuntimeException ignored) {
                }
                try {
                    Field field = current.getDeclaredField(candidate);
                    field.setAccessible(true);
                    return field;
                } catch (NoSuchFieldException | RuntimeException ignored) {
                }
            }
            current = current.getSuperclass();
        }
        return null;
    }

    private Object invoke(Object target, Method method) {
        if (target == null || method == null) {
            return null;
        }
        try {
            return method.invoke(target);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
    }

    private Object readField(Object target, Field field) {
        if (target == null || field == null) {
            return null;
        }
        try {
            return field.get(target);
        } catch (IllegalAccessException | RuntimeException ignored) {
            return null;
        }
    }

    private double extractTps(Object rawValue) {
        if (rawValue instanceof double[]) {
            double[] tps = (double[]) rawValue;
            return tps.length == 0 ? Double.NaN : tps[0];
        }
        if (rawValue instanceof float[]) {
            float[] tps = (float[]) rawValue;
            return tps.length == 0 ? Double.NaN : tps[0];
        }
        if (rawValue instanceof Number) {
            return ((Number) rawValue).doubleValue();
        }
        return Double.NaN;
    }

    private double extractSingleNumber(Object rawValue) {
        if (rawValue instanceof Number) {
            return ((Number) rawValue).doubleValue();
        }
        return Double.NaN;
    }

    private double averageTickTimes(Object rawValue) {
        if (rawValue instanceof long[]) {
            long[] tickTimes = (long[]) rawValue;
            if (tickTimes.length == 0) {
                return Double.NaN;
            }
            long total = 0L;
            int counted = 0;
            for (long tickTime : tickTimes) {
                if (tickTime <= 0L) {
                    continue;
                }
                total += tickTime;
                counted++;
            }
            if (counted == 0) {
                return Double.NaN;
            }
            return (total / (double) counted) / 1_000_000.0D;
        }

        if (rawValue instanceof double[]) {
            double[] tickTimes = (double[]) rawValue;
            if (tickTimes.length == 0) {
                return Double.NaN;
            }
            double total = 0.0D;
            int counted = 0;
            for (double tickTime : tickTimes) {
                if (Double.isNaN(tickTime) || Double.isInfinite(tickTime) || tickTime <= 0.0D) {
                    continue;
                }
                total += tickTime;
                counted++;
            }
            return counted == 0 ? Double.NaN : total / counted;
        }

        return Double.NaN;
    }

    @Override
    public String toString() {
        return "ServerMetricsProvider[" + String.format(Locale.ROOT, "tps=%s, mspt=%s",
                String.valueOf(getPrimaryTps()), String.valueOf(getAverageTickTimeMs())) + "]";
    }
}
