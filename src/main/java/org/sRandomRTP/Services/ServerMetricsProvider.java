package org.sRandomRTP.Services;

import org.bukkit.Bukkit;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
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

    // ── Result cache ────────────────────────────────────────────────────────
    // Reflection invoke() is called on every metric read (up to 20×/sec for admin bars).
    // A 500ms cache reduces this to ≤2 reflection calls per second without meaningful staleness.
    private static final long CACHE_TTL_NANOS = 500_000_000L; // 500 ms
    private volatile double cachedTps = Double.NaN;
    private volatile double cachedMspt = Double.NaN;
    private volatile long lastRefreshNanos = 0L;

    // MethodHandle is preferred over Method.invoke() because the JIT can inline through it
    // once the call-site becomes monomorphic, eliminating per-call reflection overhead.
    private volatile Class<?> resolvedServerClass;
    private volatile MethodHandle directGetTpsHandle;
    private volatile MethodHandle directAverageTickTimeHandle;
    private volatile MethodHandle directTickTimesHandle;
    private volatile MethodHandle spigotHandle;
    private volatile MethodHandle spigotGetTpsHandle;
    private volatile MethodHandle spigotTickTimesHandle;
    private volatile MethodHandle craftGetServerHandle;
    private volatile MethodHandle internalAverageTickTimeHandle;
    private volatile MethodHandle internalTickTimesHandle;
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
        // Fast path: return cached value if still fresh
        if (System.nanoTime() - lastRefreshNanos < CACHE_TTL_NANOS && !Double.isNaN(cachedTps)) {
            return cachedTps;
        }
        refreshCache();
        return cachedTps;
    }

    public double getAverageTickTimeMs() {
        // Fast path: return cached value if still fresh
        if (System.nanoTime() - lastRefreshNanos < CACHE_TTL_NANOS && !Double.isNaN(cachedMspt)) {
            return cachedMspt;
        }
        refreshCache();
        return cachedMspt;
    }

    private synchronized void refreshCache() {
        // Double-check: another thread may have refreshed while we waited for the lock
        if (System.nanoTime() - lastRefreshNanos < CACHE_TTL_NANOS) {
            return;
        }
        cachedTps = readPrimaryTps();
        cachedMspt = readAverageTickTimeMs();
        lastRefreshNanos = System.nanoTime();
    }

    private double readPrimaryTps() {
        Object server = getServer();
        if (server == null) {
            return Double.NaN;
        }
        ensureResolved(server);

        double direct = extractTps(invoke(server, directGetTpsHandle));
        if (!Double.isNaN(direct)) {
            return direct;
        }

        Object spigot = invoke(server, spigotHandle);
        double spigotValue = extractTps(invoke(spigot, spigotGetTpsHandle));
        if (!Double.isNaN(spigotValue)) {
            return spigotValue;
        }

        Object internalServer = invoke(server, craftGetServerHandle);
        double internalValue = extractTps(readField(internalServer, internalRecentTpsField));
        if (!Double.isNaN(internalValue)) {
            return internalValue;
        }

        return Double.NaN;
    }

    private double readAverageTickTimeMs() {
        Object server = getServer();
        if (server == null) {
            return Double.NaN;
        }
        ensureResolved(server);

        double directAverage = extractSingleNumber(invoke(server, directAverageTickTimeHandle));
        if (!Double.isNaN(directAverage)) {
            return directAverage;
        }

        double directTickTimesAverage = averageTickTimes(invoke(server, directTickTimesHandle));
        if (!Double.isNaN(directTickTimesAverage)) {
            return directTickTimesAverage;
        }

        Object spigot = invoke(server, spigotHandle);
        double spigotTickTimesAverage = averageTickTimes(invoke(spigot, spigotTickTimesHandle));
        if (!Double.isNaN(spigotTickTimesAverage)) {
            return spigotTickTimesAverage;
        }

        Object internalServer = invoke(server, craftGetServerHandle);
        double internalAverage = extractSingleNumber(invoke(internalServer, internalAverageTickTimeHandle));
        if (!Double.isNaN(internalAverage)) {
            return internalAverage;
        }

        double internalTickTimesAverage = averageTickTimes(invoke(internalServer, internalTickTimesHandle));
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

    private void ensureResolved(Object server) {
        if (server == null) {
            return;
        }
        Class<?> serverClass = server.getClass();
        // Fast path: volatile read without locking — covers 99.9% of calls after first resolution
        if (serverClass == resolvedServerClass) {
            return;
        }
        // Slow path: acquire lock and re-check (double-check locking pattern)
        synchronized (this) {
            if (serverClass == resolvedServerClass) {
                return;
            }
            resolveUnderLock(server, serverClass);
        }
    }

    private void resolveUnderLock(Object server, Class<?> serverClass) {
        MethodHandles.Lookup lookup = MethodHandles.lookup();

        directGetTpsHandle          = toHandle(lookup, findNoArgMethod(serverClass, "getTPS"));
        directAverageTickTimeHandle = toHandle(lookup, findFirstNoArgMethod(serverClass, AVERAGE_TICK_METHOD_CANDIDATES));
        directTickTimesHandle       = toHandle(lookup, findFirstNoArgMethod(serverClass, TICK_TIME_METHOD_CANDIDATES));
        spigotHandle                = toHandle(lookup, findNoArgMethod(serverClass, "spigot"));
        craftGetServerHandle        = toHandle(lookup, findNoArgMethod(serverClass, "getServer"));

        Object spigotDelegate = invoke(server, spigotHandle);
        Class<?> spigotClass = spigotDelegate == null ? null : spigotDelegate.getClass();
        spigotGetTpsHandle      = spigotClass == null ? null : toHandle(lookup, findNoArgMethod(spigotClass, "getTPS"));
        spigotTickTimesHandle   = spigotClass == null ? null : toHandle(lookup, findFirstNoArgMethod(spigotClass, TICK_TIME_METHOD_CANDIDATES));

        Object internalServer = invoke(server, craftGetServerHandle);
        Class<?> internalClass = internalServer == null ? null : internalServer.getClass();
        internalAverageTickTimeHandle = internalClass == null ? null : toHandle(lookup, findFirstNoArgMethod(internalClass, AVERAGE_TICK_METHOD_CANDIDATES));
        internalTickTimesHandle       = internalClass == null ? null : toHandle(lookup, findFirstNoArgMethod(internalClass, TICK_TIME_METHOD_CANDIDATES));
        internalRecentTpsField  = internalClass == null ? null : findFirstField(internalClass, TPS_FIELD_CANDIDATES);
        internalTickTimesField  = internalClass == null ? null : findFirstField(internalClass, TICK_TIME_FIELD_CANDIDATES);

        resolvedServerClass = serverClass;
    }

    /** Converts a reflected {@link Method} to a {@link MethodHandle} for JIT-inlinable dispatch. */
    private static MethodHandle toHandle(MethodHandles.Lookup lookup, Method method) {
        if (method == null) return null;
        try {
            return lookup.unreflect(method);
        } catch (IllegalAccessException ignored) {
            return null;
        }
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

    private static Object invoke(Object target, MethodHandle handle) {
        if (target == null || handle == null) {
            return null;
        }
        try {
            return handle.invoke(target);
        } catch (Throwable ignored) {
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
