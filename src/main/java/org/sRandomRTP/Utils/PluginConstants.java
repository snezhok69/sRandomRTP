package org.sRandomRTP.Utils;

/**
 * Central location for numeric constants used across the plugin.
 * Keeping magic numbers here makes them easy to find and change.
 */
public final class PluginConstants {

    private PluginConstants() {}

    // ── Metrics ─────────────────────────────────────────────────────────────

    /** bStats plugin ID. */
    public static final int METRICS_PLUGIN_ID = 21603;

    // ── Periodic tasks ───────────────────────────────────────────────────────

    /** Interval in ticks for background maintenance tasks (~30 minutes). */
    public static final long BACKGROUND_TASK_PERIOD_TICKS = 36_000L;

    /** Max cooldown age before the entry is evicted from the runtime cooldown map (24 h). */
    public static final long COOLDOWN_CLEANUP_MAX_AGE_MS = 24L * 60 * 60 * 1_000;

    // ── Cooldown permission cache ────────────────────────────────────────────

    /** TTL for the per-player cooldown permission cache. */
    public static final long COOLDOWN_PERMISSION_CACHE_TTL_MS = 60_000L;

    // ── Folia timeouts ───────────────────────────────────────────────────────

    /**
     * Minimum per-attempt timeout on Folia (ms).
     * Folia's async scheduler can have higher latency than Spigot's sync scheduler,
     * so we enforce a floor to avoid premature timeouts.
     */
    public static final long FOLIA_MIN_PER_ATTEMPT_TIMEOUT_MS = 15_000L;

    /**
     * Minimum total request timeout on Folia (ms).
     */
    public static final long FOLIA_MIN_TOTAL_TIMEOUT_MS = 60_000L;

    // ── Request tracking ─────────────────────────────────────────────────────

    /** Maximum number of recent teleport locations tracked per player. */
    public static final int RECENT_TELEPORT_CACHE_CAPACITY = 20;
}
