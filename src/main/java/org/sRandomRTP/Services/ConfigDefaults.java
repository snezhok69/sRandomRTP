package org.sRandomRTP.Services;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Single source of truth for default configuration values that appear both in
 * {@link ConfigCache.Builder} field initialisers and in
 * {@link MigrationRunner#applyManagedDefaults}.
 *
 * <p>When a default changes, update it here — both consumers pick it up automatically.</p>
 */
public final class ConfigDefaults {

    private ConfigDefaults() {}

    // ── Parallel search ───────────────────────────────────────────────────────
    public static final boolean PARALLEL_SEARCH_ENABLED             = false;
    public static final int     PARALLEL_SEARCH_CANDIDATES_PER_BATCH = 2;
    public static final int     PARALLEL_SEARCH_MAX_GLOBAL_INFLIGHT  = 24;

    // ── Prefer-generated chunks ───────────────────────────────────────────────
    public static final boolean PREFER_GENERATED_CHUNKS_ENABLED     = false;
    public static final long    PREFER_GENERATED_CHUNKS_WINDOW_MS   = 1000L;
    public static final int     PREFER_GENERATED_CHUNKS_MAX_ATTEMPTS = 8;

    // ── Slow-request threshold ────────────────────────────────────────────────
    public static final long    SLOW_REQUEST_THRESHOLD_MS           = 3000L;

    // ── Main command aliases ─────────────────────────────────────────────────
    public static final boolean COMMAND_ALIASES_ENABLED = false;
    public static final List<String> COMMAND_ALIASES = Collections.unmodifiableList(
            Arrays.asList("randomtp", "randomteleport"));

    // ── Coordinate generation ─────────────────────────────────────────────────
    /** Default coordinate-generation strategy. Matches the value written by BootstrapCoordinator into teleport.yml. */
    public static final String   DEFAULT_COORDINATE_GENERATION        = "CIRCLE";

    // ── Admin bars ────────────────────────────────────────────────────────────
    public static final boolean ADMIN_BARS_ENABLED                  = true;
    public static final long    ADMIN_BARS_UPDATE_INTERVAL_TICKS    = 20L;
    public static final boolean ADMIN_BARS_TPS_ENABLED              = true;
    public static final boolean ADMIN_BARS_RAM_ENABLED              = true;
    public static final boolean ADMIN_BARS_MSPT_ENABLED             = true;
}
