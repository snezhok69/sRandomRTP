package org.sRandomRTP.Rtp;

import com.tcoded.folialib.wrapper.task.WrappedTask;
import org.bukkit.*;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.sRandomRTP.BlockBiomes.BiomeBlockValidator;
import org.sRandomRTP.DifferentMethods.*;
import org.sRandomRTP.DifferentMethods.IsIn.IsInProtectedRegion;
import org.sRandomRTP.Utils.ChatUtils;
import org.sRandomRTP.DifferentMethods.Teleport.ChunkAcquireService;
import org.sRandomRTP.DifferentMethods.Teleport.GenerateCoordinates;
import org.sRandomRTP.DifferentMethods.Teleport.FoliaSchedulerFacade;
import org.sRandomRTP.DifferentMethods.Teleport.RtpCandidateResolution;
import org.sRandomRTP.DifferentMethods.Teleport.SearchPhasePolicy;
import org.sRandomRTP.DifferentMethods.Teleport.TeleportRequestContext;
import org.sRandomRTP.DifferentMethods.Teleport.TeleportRequestManager;
import org.sRandomRTP.DifferentMethods.Teleport.TeleportExecutionService;
import org.sRandomRTP.DifferentMethods.Text.TranslateRGBColors;
import org.sRandomRTP.Files.LoadMessages;
import org.sRandomRTP.GetYGet.*;
import org.sRandomRTP.GetYGet.NetherEndSafeYResolver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import org.sRandomRTP.DifferentMethods.rtprtps.DifferentRtpMethods;
import org.sRandomRTP.Utils.ConfigUtils;
import org.sRandomRTP.Services.RuntimeStateRegistry;
import org.sRandomRTP.Utils.WorldHeightSupport;
import org.sRandomRTP.Utils.WorldUtils;

import static org.sRandomRTP.DifferentMethods.rtprtps.DifferentRtpMethods.*;
import static org.sRandomRTP.DifferentMethods.rtprtps.HandleFailedAttempt.handleFailedAttempt;
import static org.sRandomRTP.DifferentMethods.rtprtps.ResolveTargetWorld.resolveTargetWorld;
import static org.sRandomRTP.DifferentMethods.rtprtps.SendLoadingFeedback.sendLoadingFeedback;

/**
 * Shared teleport search pipeline used by all RtpRtp* handlers.
 *
 * Subclasses implement {@link #generateXZ} to produce candidate coordinates,
 * and may override the hook methods to add biome filtering or pre-attempt logic.
 *
 * One instance is created per teleport request.
 */
public abstract class AbstractRtpHandler {

    // ── Abstract method ──────────────────────────────────────────────────────

    /**
     * Generate candidate X/Z coordinates for the given generation index.
     * Default implementation delegates to {@link GenerateCoordinates#generateCoordinates}.
     * Override only when custom coordinate logic is required (e.g. Near, Base handlers).
     *
     * @return {@code [x, z]} on success, or {@code null} to skip this candidate (triggers retry)
     */
    protected int[] generateXZ(Player player, World world, int centerX, int centerZ,
                                int radius, int minRadius, int generationIndex,
                                long sessionNonce, String method, boolean absolute,
                                boolean loggingEnabled, int attemptNumber) {
        return GenerateCoordinates.generateCoordinates(player.getName(), generationIndex,
                sessionNonce, centerX, centerZ, radius, minRadius, method, absolute);
    }

    // ── Hook methods (override to customize) ─────────────────────────────────

    /**
     * Called once per attempt before coordinate generation.
     * Return {@code false} to cancel the entire search (send error messages first).
     */
    protected boolean preAttemptChecks(Player player, World world, TeleportRequestContext ctx,
                                       boolean loggingEnabled, int attemptNumber,
                                       int maxAttempts, Runnable retryCallback) {
        return true;
    }

    /** Biome whitelist — empty list means no biome filtering. */
    protected List<Biome> getBiomeTargets() {
        return Collections.emptyList();
    }

    protected String getCoordinateGenerationMethod() {
        return Variables.configCache.coordinateGenerationMethod;
    }

    protected boolean getUseAbsoluteCoordinates() {
        return Variables.configCache.useAbsoluteCoordinates;
    }

    protected int resolveMaxAttempts(World world) {
        return Variables.configCache.maxTries;
    }

    protected String resolveSearchStage(TeleportRequestContext context, int attemptNumber) {
        return "fast-random";
    }

    protected int resolveBiomeProbeSamples(TeleportRequestContext context, int attemptNumber) {
        return 1;
    }

    // ── Entry-point boilerplate template ────────────────────────────────────

    /**
     * Shared launch parameters returned by {@link #buildLaunchParams}.
     */
    /**
     * Shared per-world radius resolution used by subclasses.
     * Checks a per-world override path first, then falls back to the global config key.
     *
     * @param cfg          config file to read from
     * @param perWorldBase base path, e.g. {@code "teleport-biome.per-world-biome."}
     * @param worldName    the world name
     * @param radiusKey    full config key for max-radius (used when {@code isRadius=true})
     * @param minRadiusKey full config key for min-radius (used when {@code isRadius=false})
     * @param isRadius     {@code true} to resolve max radius, {@code false} for min radius
     * @param fallback     default value if the config key is absent
     */
    protected static int resolveRadiusFromConfig(FileConfiguration cfg, String perWorldBase,
            String worldName, String radiusKey, String minRadiusKey, boolean isRadius, int fallback) {
        // Delegate to the shared utility — keeps the per-world lookup logic in one place.
        String key = isRadius ? "radius" : "minradius";
        return ConfigUtils.getWorldSpecificInt(cfg, worldName, key, cfg.getInt(isRadius ? radiusKey : minRadiusKey, fallback));
    }

    /**
     * Validates that {@code minRadius < radius} and their difference is at least 50.
     * Sends an error message to the player and returns {@code false} if invalid.
     */
    protected static boolean validateRadius(int minRadius, int radius, Player player) {
        if (minRadius >= radius) {
            player.sendMessage(ChatUtils.PLUGIN_NAME + " §8- §cThe minimum radius cannot be greater than or equal to the maximum radius.");
            return false;
        }
        if (radius - minRadius < 50) {
            player.sendMessage(ChatUtils.PLUGIN_NAME + " §8- §cThe difference between the minimum and maximum radius must be at least 50 blocks.");
            return false;
        }
        return true;
    }

    /** Null-safe world border center X. Delegates to {@link WorldUtils#worldCenterX}. */
    protected static int worldCenterX(World world) {
        return WorldUtils.worldCenterX(world);
    }

    /** Null-safe world border center Z. Delegates to {@link WorldUtils#worldCenterZ}. */
    protected static int worldCenterZ(World world) {
        return WorldUtils.worldCenterZ(world);
    }


    /**
     * Returns the launch parameters for this handler.
     *
     * <p>Subclasses override this to supply handler-specific radius values
     * (e.g. from {@code far.yml} or {@code middle.yml}).  Return {@code null}
     * to abort the launch after sending an appropriate message to the player.
     *
     * <p>Default implementation uses the standard teleport.yml radii with
     * per-world config support.
     */
    protected LaunchParams buildLaunchParams(Player player, World world, boolean loggingEnabled) {
        int centerX = worldCenterX(world);
        int centerZ = worldCenterZ(world);
        String worldName = world.getName();
        FileConfiguration teleportFile = Variables.getPluginContext().getConfigRegistry().getTeleportFile();
        // Inlined from the former DifferentRtpMethods.getWorldSpecificRadius wrapper
        int radius    = ConfigUtils.getWorldSpecificInt(teleportFile, worldName, "radius",    0);
        int minRadius = ConfigUtils.getWorldSpecificInt(teleportFile, worldName, "minradius", 0);
        DifferentRtpMethods.ClampedRadius clamped =
                clampRadiusToBorder(world, radius, minRadius, "[sRandomRTP]", loggingEnabled);
        radius    = clamped.radius;
        minRadius = clamped.minRadius;
        if (!validateRadius(minRadius, radius, player)) return null;
        return new LaunchParams(centerX, centerZ, radius, minRadius, resolveMaxAttempts(world), true);
    }

    /**
     * Performs the common RTP entry-point setup and starts the search.
     *
     * <p>Concrete handlers call this from their static entry method:
     * <pre>
     *   public static void rtpRtp(CommandSender sender, World targetWorld) {
     *       new Handler().launchRtp(sender, targetWorld);
     *   }
     * </pre>
     *
     * @param sender      the command sender (must be a Player)
     * @param targetWorld the requested destination world, or {@code null} to resolve automatically
     */
    protected final void launchRtp(CommandSender sender, World targetWorld) {
        if (!(sender instanceof Player)) {
            ChatUtils.sendPlayersOnly(sender);
            return;
        }
        Player player = (Player) sender;
        if (!player.isOnline()) return;
        executeLaunch(player, targetWorld);
    }

    /**
     * Overload for handlers that receive {@code Player} directly (Portal, World).
     *
     * @param player      the player to teleport
     * @param targetWorld the requested destination world, or {@code null} to resolve automatically
     */
    protected final void launchRtpForPlayer(Player player, World targetWorld) {
        if (player == null || !player.isOnline()) return;
        executeLaunch(player, targetWorld);
    }

    // ── Static convenience factory methods ──────────────────────────────────

    /** Entry point for standard /rtp — no handler customisation. */
    public static void launch(CommandSender sender, World targetWorld) {
        new StandardRtpHandler().launchRtp(sender, targetWorld);
    }

    /** Entry point for player-targeted teleport (e.g. /rtp world). */
    public static void launchForPlayer(Player player, World targetWorld) {
        new StandardRtpHandler().launchRtpForPlayer(player, targetWorld);
    }

    /**
     * Common launch body shared by {@link #launchRtp} and {@link #launchRtpForPlayer}.
     */
    private void executeLaunch(Player player, World targetWorld) {
        boolean searchStarted = false;
        try {
            RuntimeStateRegistry state = Variables.getRuntimeState();
            boolean loggingEnabled = Variables.isLoggingEnabled();

            World world = resolveTargetWorld(player, targetWorld, loggingEnabled);
            if (world == null) {
                player.sendMessage(ChatUtils.PLUGIN_NAME + " §8- §cUnable to determine a valid world for teleportation.");
                return;
            }

            sendLoadingFeedback(player);
            state.rememberInitialPosition(player);

            LaunchParams params = buildLaunchParams(player, world, loggingEnabled);
            if (params == null) return;

            TeleportRequestContext context = TeleportRequestManager.beginRequest(player, loggingEnabled);
            Bukkit.getPluginManager().callEvent(new org.sRandomRTP.Events.RtpSearchStartEvent(player, world));
            searchStarted = true;
            long sessionNonce = params.useSessionNonce ? computeSessionSeed(player, context.getRequestId()) : 0L;
            state.setPlayerSearching(player, true);

            this.scheduleNextAttempt(player, world, params.centerX, params.centerZ,
                    params.radius, params.minRadius, params.maxAttempts,
                    loggingEnabled, context, sessionNonce, 0L);
        } catch (RuntimeException e) {
            LoggerUtility.loggerUtility(AbstractRtpHandler.class, e);
        } finally {
            if (!searchStarted) EconomyPaymentManager.refund(player);
        }
    }

    // ── Pipeline entry point ─────────────────────────────────────────────────

    /**
     * Schedules the first (or next) search attempt with a delay.
     * Called from concrete class entry methods after initial setup.
     */
    final void scheduleNextAttempt(Player player, World world, int centerX, int centerZ,
                                   int radius, int minRadius, int maxAttempts,
                                   boolean loggingEnabled,
                                   TeleportRequestContext context, long sessionNonce,
                                   long delayTicks) {
        if (!player.isOnline()) {
            if (loggingEnabled) {
                Bukkit.getLogger().info("Skipping teleport search for player " + player.getName() + " because they are offline.");
            }
            TeleportRequestManager.cancelRequest(player.getUniqueId(), loggingEnabled, "player offline");
            Variables.getRuntimeState().setPlayerSearching(player, false);
            return;
        }

        if (context.isCancelled() || context.isCompleted()) {
            return;
        }

        if (context.isExpired()) {
            handleTimedOutSearch(player, loggingEnabled);
            TeleportRequestManager.cancelRequest(player.getUniqueId(), loggingEnabled, "request expired");
            Variables.getRuntimeState().setPlayerSearching(player, false);
            return;
        }

        WrappedTask task = FoliaSchedulerFacade.runLaterEntityAware(
                player,
                Math.max(delayTicks, 0L),
                () -> attemptCoordinate(player, world, centerX, centerZ, radius, minRadius,
                        maxAttempts, loggingEnabled, context, sessionNonce));
        TeleportRequestManager.registerTask(player, task);
    }

    // ── Internal pipeline ────────────────────────────────────────────────────

    private void attemptCoordinate(Player player, World world, int centerX, int centerZ,
                                   int radius, int minRadius, int maxAttempts,
                                   boolean loggingEnabled,
                                   TeleportRequestContext context, long sessionNonce) {
        if (!player.isOnline()) {
            TeleportRequestManager.cancelRequest(player.getUniqueId(), loggingEnabled, "player offline");
            Variables.getRuntimeState().setPlayerSearching(player, false);
            return;
        }

        if (context.isCancelled() || context.isCompleted()) {
            return;
        }

        if (context.isExpired()) {
            handleTimedOutSearch(player, loggingEnabled);
            TeleportRequestManager.cancelRequest(player.getUniqueId(), loggingEnabled, "request expired");
            Variables.getRuntimeState().setPlayerSearching(player, false);
            return;
        }

        if (radius == minRadius) {
            Variables.getMessageService().send(player, LoadMessages.error_radius);
            TeleportRequestManager.cancelRequest(player.getUniqueId(), loggingEnabled, "invalid radius");
            Variables.getRuntimeState().setPlayerSearching(player, false);
            return;
        }

        int attemptNumber = context.incrementAttempt();
        // Compute once: shouldLogAttempt is called 17+ times in this method.
        boolean log = loggingEnabled && shouldLogAttempt(attemptNumber);

        if (log) {
            Bukkit.getConsoleSender().sendMessage("Starting teleport attempt #" + attemptNumber
                    + " for player " + player.getName());
        }

        Runnable retryCallback = () -> scheduleNextAttempt(player, world, centerX, centerZ, radius,
                minRadius, maxAttempts, loggingEnabled, context, sessionNonce, 1L);

        if (attemptNumber > maxAttempts) {
            handleFailedAttempt(player, loggingEnabled, attemptNumber, maxAttempts, retryCallback, context);
            return;
        }

        if (!preAttemptChecks(player, world, context, loggingEnabled, attemptNumber, maxAttempts, retryCallback)) {
            return;
        }

        String generationMethod = getCoordinateGenerationMethod();
        boolean useAbsoluteCoordinates = getUseAbsoluteCoordinates();
        List<Biome> biomeTargets = getBiomeTargets();

        int batchSize = SearchPhasePolicy.resolveBatchSize(context);

        if (loggingEnabled) {
            StringBuilder builder = new StringBuilder("Teleportation mode: ")
                    .append(generationMethod)
                    .append(", Absolute coordinates: ")
                    .append(useAbsoluteCoordinates ? "enabled" : "disabled")
                    .append(", Min radius: ").append(minRadius)
                    .append(", Max radius: ").append(radius);
            if (batchSize > 1) {
                builder.append(", Parallel candidates: ").append(batchSize);
            }
            Bukkit.getConsoleSender().sendMessage(builder.toString());
        }

        int baseGenerationIndex = Math.max(0, (attemptNumber - 1) * batchSize);
        List<CandidateSearchTask> candidateTasks = new ArrayList<>(batchSize);
        for (int i = 0; i < batchSize; i++) {
            int generationIndex = baseGenerationIndex + i;
            CandidateSearchTask task = createCandidateSearchTask(player, world, centerX, centerZ,
                    radius, minRadius, sessionNonce, generationMethod, useAbsoluteCoordinates,
                    loggingEnabled, attemptNumber, generationIndex, i + 1, batchSize, context, biomeTargets);
            if (task != null) {
                candidateTasks.add(task);
            }
        }

        if (candidateTasks.isEmpty()) {
            handleFailedAttempt(player, loggingEnabled, attemptNumber, maxAttempts, retryCallback, context);
            return;
        }

        // Single AtomicBoolean guards all resolution paths — the first CAS winner "owns" the outcome.
        AtomicBoolean winnerChosen = new AtomicBoolean(false);
        AtomicInteger remaining = new AtomicInteger(candidateTasks.size());

        CompletableFuture<Void> attemptFuture = new CompletableFuture<>();
        context.trackFuture(attemptFuture);
        attemptFuture.whenComplete((ignored, throwable) -> {
            if (throwable != null) {
                if (throwable instanceof CancellationException || context.isCancelled() || context.isCompleted()) {
                    return;
                }
                // Unexpected future failure — treat as a failed attempt if no winner yet
                if (winnerChosen.compareAndSet(false, true)) {
                    if (log) {
                        Bukkit.getLogger().warning("Teleport attempt #" + attemptNumber
                                + " timed out for player " + player.getName());
                    }
                    handleFailedAttempt(player, loggingEnabled, attemptNumber, maxAttempts, retryCallback, context);
                }
            }
        });

        for (CandidateSearchTask task : candidateTasks) {
            task.future.whenComplete((candidate, throwable) -> {
                if (context.isCancelled() || context.isCompleted()) {
                    winnerChosen.compareAndSet(false, true); // mark resolved to suppress further handling
                    attemptFuture.complete(null);
                    return;
                }

                if (throwable != null) {
                    if (loggingEnabled) {
                        Bukkit.getLogger().log(Level.SEVERE,
                                "Error while processing Y-coordinate for player " + player.getName(), throwable);
                    }
                }

                boolean success = throwable == null && candidate != null && candidate.getY() != -1;
                if (success) {
                    if (winnerChosen.compareAndSet(false, true)) {
                        attemptFuture.complete(null);

                        if (context.isCancelled() || context.isCompleted()) {
                            return;
                        }

                        Location processingLocation = new Location(world, task.x,
                                Math.max(candidate.getY(), WorldHeightSupport.getMinHeight(world)), task.z);
                        FoliaSchedulerFacade.runAtLocation(processingLocation, () -> {
                            processCandidateLocation(player, world, centerX, centerZ,
                                    candidate, task.searchStage, loggingEnabled, attemptNumber, maxAttempts,
                                    retryCallback, context, biomeTargets);
                        });
                    }
                    return;
                }

                if (throwable == null && candidate != null && candidate.getY() == -1 && log) {
                    Bukkit.getConsoleSender().sendMessage("Teleportation attempt #" + attemptNumber
                            + " failed due to unsafe location.");
                }

                if (remaining.decrementAndGet() == 0) {
                    // All candidates exhausted — claim the resolution slot and handle failure
                    if (winnerChosen.compareAndSet(false, true)) {
                        attemptFuture.complete(null);
                        handleFailedAttempt(player, loggingEnabled, attemptNumber, maxAttempts, retryCallback, context);
                    }
                }
            });
        }
    }

    private CandidateSearchTask createCandidateSearchTask(Player player, World world,
                                                          int centerX, int centerZ,
                                                          int radius, int minRadius,
                                                          long sessionNonce, String generationMethod,
                                                          boolean useAbsoluteCoordinates,
                                                          boolean loggingEnabled, int attemptNumber,
                                                          int generationIndex, int candidateNumber,
                                                          int batchSize, TeleportRequestContext context,
                                                          List<Biome> biomeTargets) {
        boolean log = loggingEnabled && shouldLogAttempt(attemptNumber);
        String searchStage = resolveSearchStage(context, attemptNumber);
        if (context.isCancelled() || context.isCompleted()) {
            return new CandidateSearchTask(centerX, centerZ, searchStage, CompletableFuture.completedFuture(null));
        }

        WorldBorder border = world.getWorldBorder();
        int normalizedIndex = Math.max(0, generationIndex);
        int[] coords = selectCandidateCoordinates(player, world, centerX, centerZ, radius, minRadius,
                normalizedIndex, sessionNonce, generationMethod, useAbsoluteCoordinates,
                loggingEnabled, attemptNumber, context, biomeTargets);
        if (coords == null) {
            return new CandidateSearchTask(centerX, centerZ, searchStage, CompletableFuture.completedFuture(null));
        }
        int finalNewX = coords[0];
        int finalNewZ = coords[1];

        if (log) {
            double distance = Math.hypot(finalNewX - centerX, finalNewZ - centerZ);
            StringBuilder sb = new StringBuilder("Generated coordinates [").append(searchStage).append(']');
            if (batchSize > 1) {
                sb.append(" (candidate ").append(candidateNumber).append('/').append(batchSize).append(')');
            }
            sb.append(": X=").append(finalNewX).append(", Z=").append(finalNewZ)
              .append(", Distance from center: ").append((int) distance).append(" blocks");
            Bukkit.getConsoleSender().sendMessage(sb.toString());
        }

        UUID playerId = player.getUniqueId();
        int candidateChunkX = finalNewX >> 4;
        int candidateChunkZ = finalNewZ >> 4;
        if (TeleportRequestManager.isLocationRecentlyUsed(playerId, candidateChunkX, candidateChunkZ)) {
            if (log) {
                Bukkit.getConsoleSender().sendMessage("Skipping recently used chunk at X=" + finalNewX + ", Z=" + finalNewZ);
            }
            return new CandidateSearchTask(finalNewX, finalNewZ, searchStage, CompletableFuture.completedFuture(null));
        }

        if (!WorldUtils.isWithinBorder(border, finalNewX + 0.5, finalNewZ + 0.5)) {
            if (log) {
                Bukkit.getConsoleSender().sendMessage("Generated coordinates outside world border: X=" + finalNewX + ", Z=" + finalNewZ);
            }
            return new CandidateSearchTask(finalNewX, finalNewZ, searchStage, CompletableFuture.completedFuture(null));
        }

        final boolean preferGeneratedOnly = SearchPhasePolicy.shouldPreferGeneratedChunks(context, attemptNumber);
        final SearchPhasePolicy.CandidatePermit permit = SearchPhasePolicy.acquireCandidatePermit(candidateNumber == 1);
        if (!permit.isUsable()) {
            return new CandidateSearchTask(finalNewX, finalNewZ, searchStage, CompletableFuture.completedFuture(null));
        }

        CompletableFuture<RtpCandidateResolution> resolutionFuture =
                ChunkAcquireService.acquireTargetChunk(world, finalNewX, finalNewZ, preferGeneratedOnly)
                        .thenCompose(acquireResult -> {
                            if (context.isCancelled() || context.isCompleted() || acquireResult == null || !acquireResult.isReady()) {
                                return CompletableFuture.completedFuture(null);
                            }

                            context.recordChunkAcquire(acquireResult.getDurationNanos(),
                                    acquireResult.isAlreadyLoaded(), acquireResult.isGenerationAllowed());

                            CompletableFuture<RtpCandidateResolution> future = new CompletableFuture<>();
                            Location candidateLocation = new Location(world, finalNewX, WorldHeightSupport.getMinHeight(world), finalNewZ);
                            FoliaSchedulerFacade.runAtLocation(candidateLocation, () -> {
                                try {
                                    future.complete(resolveCandidateOnLoadedChunk(world, acquireResult, finalNewX, finalNewZ,
                                            context, biomeTargets, loggingEnabled, attemptNumber));
                                } catch (RuntimeException throwable) {
                                    future.completeExceptionally(throwable);
                                }
                            });
                            return future;
                        });
        resolutionFuture.whenComplete((ignored, throwable) -> permit.release());
        context.trackFuture(resolutionFuture);
        return new CandidateSearchTask(finalNewX, finalNewZ, searchStage, resolutionFuture);
    }

    private void processCandidateLocation(Player player, World world, int centerX, int centerZ,
                                          RtpCandidateResolution resolution, String searchStage, boolean loggingEnabled,
                                          int attemptNumber,
                                          int maxAttempts, Runnable retryCallback,
                                          TeleportRequestContext context, List<Biome> biomeTargets) {
        int finalNewX = resolution.getX();
        int finalNewZ = resolution.getZ();
        int newY = resolution.getY();
        Biome targetBiome = resolution.getBiome();
        Material targetBlockType = resolution.getSurfaceBlockType();
        Material blockAboveType = resolution.getFeetBlockType();
        Material blockTwoAboveType = resolution.getHeadBlockType();
        boolean log = loggingEnabled && shouldLogAttempt(attemptNumber);

        if (log) {
            Bukkit.getConsoleSender().sendMessage(new StringBuilder("Trying teleport to: X=").append(finalNewX)
                    .append(", Y=").append(newY).append(", Z=").append(finalNewZ)
                    .append(" | surface=").append(targetBlockType)
                    .append(" feet=").append(blockAboveType)
                    .append(" head=").append(blockTwoAboveType)
                    .append(" biome=").append(targetBiome == null ? "UNKNOWN" : targetBiome.name())
                    .toString());
        }

        // Biome filter (only active when getBiomeTargets() returns non-empty list)
        if (!biomeTargets.isEmpty()) {
            if (targetBiome == null || !biomeTargets.contains(targetBiome)) {
                if (log) {
                    String biomeName = targetBiome == null ? "UNKNOWN" : targetBiome.name();
                    Bukkit.getConsoleSender().sendMessage("Rejected location due to biome mismatch: " + biomeName);
                }
                handleFailedAttempt(player, loggingEnabled, attemptNumber, maxAttempts, retryCallback, context);
                return;
            }
        }

        WorldBorder border = world.getWorldBorder();
        double targetCenterX = finalNewX + 0.5;
        double targetCenterZ = finalNewZ + 0.5;
        if (!WorldUtils.isWithinBorder(border, targetCenterX, targetCenterZ)) {
            if (log) {
                Bukkit.getConsoleSender().sendMessage("Attempted to teleport outside world border to coordinates: "
                        + finalNewX + ", " + newY + ", " + finalNewZ);
            }
            if (attemptNumber >= maxAttempts) {
                Variables.getMessageService().send(player, LoadMessages.worldborder_error);
            }
            handleFailedAttempt(player, loggingEnabled, attemptNumber, maxAttempts, retryCallback, context);
            return;
        }

        // Use configCache to avoid a hot-path YAML map lookup per candidate
        boolean checkingInRegions = Variables.configCache.checkingInRegions;
        org.sRandomRTP.Services.PluginContext pCtx = Variables.getPluginContext();
        if (checkingInRegions && (pCtx == null || !pCtx.isWorldGuardAvailable())) {
            if (loggingEnabled) {
                Bukkit.getConsoleSender().sendMessage("Install the WorldGuard plugin or disable checking regions in the configuration (checkinginregions: false).");
            }
            player.sendMessage(ChatColor.RED + "Check the console. If there is nothing in the console, enable diagnostics in the configuration (diagnostic: true) and try teleportation again.");
            TeleportRequestManager.cancelRequest(player.getUniqueId(), loggingEnabled, "missing WorldGuard");
            Variables.getRuntimeState().setPlayerSearching(player, false);
            return;
        }

        // Allocate Location only when the region check actually needs one — the
        // WorldGuard API still requires a Location object.
        Location regionCheckLocation = null;
        if (checkingInRegions && pCtx != null) {
            regionCheckLocation = new Location(world, targetCenterX, newY, targetCenterZ);
            if (IsInProtectedRegion.isInProtectedRegion(regionCheckLocation)) {
                if (log) {
                    String regionName = GetProtectedRegionName.getProtectedRegionName(regionCheckLocation);
                    Bukkit.getConsoleSender().sendMessage("Attempted to teleport into protected region: " + regionName);
                }
                handleFailedAttempt(player, loggingEnabled, attemptNumber, maxAttempts, retryCallback, context);
                return;
            }
        }

        boolean safeSurface = resolution.isSafe();

        if (safeSurface && newY != -1) {
            if (loggingEnabled) {
                Bukkit.getLogger().info("Found suitable location for player " + player.getName()
                        + " at X:" + finalNewX + ", Y:" + newY + ", Z:" + finalNewZ
                        + " via stage " + searchStage);
            }

            UUID playerId = player.getUniqueId();
            TeleportRequestManager.rememberLocation(playerId, finalNewX >> 4, finalNewZ >> 4);

            // teleportY/headY/belowY are derived from newY directly — avoids an
            // extra Location allocation when the region check above did not need one.
            int teleportY = newY;
            int headY = teleportY + 1;
            int belowY = teleportY - 1;
            int worldMaxY = world.getMaxHeight() - 1;
            int worldMinY = WorldHeightSupport.getMinHeight(world);

            if (teleportY > worldMaxY || headY > worldMaxY || belowY < worldMinY) {
                if (log) {
                    Bukkit.getConsoleSender().sendMessage("Rejected teleport location for "
                            + player.getName() + " due to invalid height range.");
                }
                handleFailedAttempt(player, loggingEnabled, attemptNumber, maxAttempts, retryCallback, context);
                return;
            }

            if (isHazardousFluid(targetBlockType)
                    || isHazardousFluid(blockAboveType)
                    || isHazardousFluid(blockTwoAboveType)) {
                if (log) {
                    Bukkit.getConsoleSender().sendMessage("Teleportation attempt #" + attemptNumber
                            + " failed due to hazardous fluid near player position.");
                }
                handleFailedAttempt(player, loggingEnabled, attemptNumber, maxAttempts, retryCallback, context);
                return;
            }

            if (log) {
                int chunkX = finalNewX >> 4;
                int chunkZ = finalNewZ >> 4;
                Bukkit.getLogger().info("Starting asynchronous chunk loading for blocks: "
                        + finalNewX + "," + finalNewZ + " (Chunk: " + chunkX + ", " + chunkZ
                        + ") in world " + world.getName());
            }

            TeleportExecutionService.execute(player, resolution, loggingEnabled, context);
            return;
        }

        if (log) {
            Bukkit.getConsoleSender().sendMessage("Teleportation attempt #" + attemptNumber
                    + " failed due to banned block, biome, or unsafe block above.");
        }
        handleFailedAttempt(player, loggingEnabled, attemptNumber, maxAttempts, retryCallback, context);
    }

    private int[] selectCandidateCoordinates(Player player, World world, int centerX, int centerZ,
                                             int radius, int minRadius, int generationIndex,
                                             long sessionNonce, String generationMethod,
                                             boolean useAbsoluteCoordinates, boolean loggingEnabled,
                                             int attemptNumber, TeleportRequestContext context,
                                             List<Biome> biomeTargets) {
        boolean log = loggingEnabled && shouldLogAttempt(attemptNumber);
        String searchStage = resolveSearchStage(context, attemptNumber);
        int probeSamples = Math.max(1, resolveBiomeProbeSamples(context, attemptNumber));
        if (probeSamples <= 1 || biomeTargets == null || biomeTargets.isEmpty()) {
            return generateXZ(player, world, centerX, centerZ, radius, minRadius, generationIndex,
                    sessionNonce, generationMethod, useAbsoluteCoordinates, loggingEnabled, attemptNumber);
        }

        for (int sample = 0; sample < probeSamples; sample++) {
            int expandedIndex = Math.max(0, generationIndex * probeSamples + sample);
            int[] coords = generateXZ(player, world, centerX, centerZ, radius, minRadius, expandedIndex,
                    sessionNonce, generationMethod, useAbsoluteCoordinates, loggingEnabled, attemptNumber);
            if (coords == null) {
                continue;
            }
            Biome probeBiome = resolveProbeBiome(world, coords[0], coords[1]);
            if (probeBiome != null && biomeTargets.contains(probeBiome)) {
                if (log) {
                    Bukkit.getConsoleSender().sendMessage("Biome probe [" + searchStage + "] matched "
                            + probeBiome.name() + " at X=" + coords[0] + ", Z=" + coords[1]);
                }
                return coords;
            }
        }

        if (log) {
            Bukkit.getConsoleSender().sendMessage("Biome probe [" + searchStage + "] found no matching biome in "
                    + probeSamples + " sampled candidates.");
        }
        return null;
    }

    private RtpCandidateResolution resolveCandidateOnLoadedChunk(World world,
                                                                 ChunkAcquireService.ChunkAcquireResult acquireResult,
                                                                 int x,
                                                                 int z,
                                                                 TeleportRequestContext context,
                                                                 List<Biome> biomeTargets,
                                                                 boolean loggingEnabled,
                                                                 int attemptNumber) {
        long safeSearchStartedAt = System.nanoTime();
        Chunk chunk = acquireResult.getChunk();
        boolean log = loggingEnabled && shouldLogAttempt(attemptNumber);
        if (shouldRejectByBiomeBeforeSafeSearch(world, chunk, x, z, biomeTargets)) {
            if (log) {
                Biome probeBiome = resolveProbeBiome(world, x, z);
                Bukkit.getConsoleSender().sendMessage("Rejected location before safe-Y due to biome mismatch: "
                        + (probeBiome == null ? "UNKNOWN" : probeBiome.name()));
            }
            return null;
        }
        int y = -1;
        Biome biome = null;

        if (world.getEnvironment() == World.Environment.NETHER) {
            y = NetherEndSafeYResolver.netherSafeY(world, x, z, context);
            if (y != -1) biome = world.getBiome(x, y, z);
        } else if (world.getEnvironment() == World.Environment.THE_END) {
            y = NetherEndSafeYResolver.endSafeY(world, x, z, context);
            if (y != -1) biome = world.getBiome(x, y, z);
        } else {
            GetSafeYCoordinate.CoordinateWithBiome coordinate =
                    GetSafeYCoordinate.findSafeYOnLoadedChunk(world, chunk, x, z, context);
            if (coordinate != null) {
                y = coordinate.y;
                biome = coordinate.biome;
            }
        }

        long safeSearchNanos = System.nanoTime() - safeSearchStartedAt;
        context.recordSafeSearch(safeSearchNanos);

        if (y == -1) {
            return RtpCandidateResolution.unsafe(world, chunk, x, z, acquireResult.isAlreadyLoaded(),
                    acquireResult.isGenerationAllowed(), acquireResult.getDurationNanos(), safeSearchNanos);
        }

        // Chunk is already loaded and known — reading through chunk.getBlock(relX, y, relZ)
        // skips the world→chunk lookup that world.getBlockAt(x, y, z) performs internally.
        int relativeX = x & 0xF;
        int relativeZ = z & 0xF;
        Material targetBlockType = chunk.getBlock(relativeX, y - 1, relativeZ).getType();
        Material blockAboveType = chunk.getBlock(relativeX, y, relativeZ).getType();
        Material blockTwoAboveType = chunk.getBlock(relativeX, y + 1, relativeZ).getType();
        if (biome == null) {
            biome = world.getBiome(x, y, z);
        }

        int skyLight = 0;

        boolean safe = GetSafeYCoordinate.isSafeTeleportSupportMaterial(targetBlockType)
                && !BiomeBlockValidator.isBiomeBanned(biome)
                && GetSafeYCoordinate.isSafeTeleportOccupantMaterial(blockAboveType)
                && GetSafeYCoordinate.isSafeTeleportOccupantMaterial(blockTwoAboveType);

        return new RtpCandidateResolution(world, chunk, x, y, z, biome, targetBlockType,
                blockAboveType, blockTwoAboveType, skyLight, safe,
                acquireResult.isAlreadyLoaded(), acquireResult.isGenerationAllowed(),
                acquireResult.getDurationNanos(), safeSearchNanos);
    }

    private boolean shouldRejectByBiomeBeforeSafeSearch(World world, Chunk chunk, int x, int z, List<Biome> biomeTargets) {
        if (world == null || chunk == null || biomeTargets == null || biomeTargets.isEmpty()) {
            return false;
        }

        Biome probeBiome = resolveProbeBiome(world, x, z);
        return probeBiome != null && !biomeTargets.contains(probeBiome);
    }

    private static long computeSessionSeed(Player player, java.util.UUID requestId) {
        java.util.UUID playerId = player.getUniqueId();
        long seed = requestId.getMostSignificantBits() ^ requestId.getLeastSignificantBits();
        seed ^= playerId.getMostSignificantBits();
        seed ^= playerId.getLeastSignificantBits();
        seed ^= System.nanoTime();
        return seed;
    }

    protected Biome resolveProbeBiome(World world, int x, int z) {
        if (world == null) {
            return null;
        }
        int minY = WorldHeightSupport.getMinHeight(world);
        int maxY = Math.max(minY, world.getMaxHeight() - 1);
        int probeY = world.getHighestBlockYAt(x, z);
        if (probeY < minY) {
            probeY = minY;
        } else if (probeY > maxY) {
            probeY = maxY;
        }
        try {
            return world.getBiome(x, probeY, z);
        } catch (RuntimeException ignored) {
            return null;
        }
    }
}
