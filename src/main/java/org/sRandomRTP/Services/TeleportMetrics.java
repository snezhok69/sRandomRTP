package org.sRandomRTP.Services;

import org.bukkit.Bukkit;
import org.sRandomRTP.Main;
import org.sRandomRTP.DifferentMethods.Teleport.TeleportRequestContext;
import org.sRandomRTP.DifferentMethods.Variables;
import org.sRandomRTP.Services.PluginContext;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import java.util.concurrent.atomic.AtomicLong;

public class TeleportMetrics {

    private final Main plugin;
    private final AtomicLong coordinateSamples = new AtomicLong();
    private final AtomicLong coordinateTotalNanos = new AtomicLong();
    private final AtomicLong safeYSamples = new AtomicLong();
    private final AtomicLong safeYTotalNanos = new AtomicLong();
    private final AtomicLong chunkSamples = new AtomicLong();
    private final AtomicLong chunkTotalNanos = new AtomicLong();
    private final AtomicLong cancellations = new AtomicLong();
    private final AtomicLong completedRequests = new AtomicLong();
    private final AtomicLong refunds = new AtomicLong();

    public TeleportMetrics(Main plugin) {
        this.plugin = plugin;
    }

    public void recordCoordinateGeneration(long durationNanos) {
        record(durationNanos, coordinateSamples, coordinateTotalNanos);
    }

    public void recordSafeYSearch(long durationNanos) {
        record(durationNanos, safeYSamples, safeYTotalNanos);
    }

    public void recordChunkRequest(long durationNanos) {
        record(durationNanos, chunkSamples, chunkTotalNanos);
    }

    public void recordCancellation() {
        cancellations.incrementAndGet();
    }

    public void recordCompletedRequest() {
        completedRequests.incrementAndGet();
    }

    public void recordRefund() {
        refunds.incrementAndGet();
    }

    public void logSlowRequestIfNeeded(String playerName, TeleportRequestContext context, String status) {
        if (plugin == null || context == null) {
            return;
        }

        long thresholdMs = 3000L;
        if (plugin.getConfig() != null) {
            thresholdMs = plugin.getConfig().getLong("metrics.rtp.slow-request-threshold-ms",
                    plugin.getConfig().getLong("teleport.metrics.slow-request-threshold-ms", 3000L));
        } else {
            PluginContext ctx = Variables.getPluginContext();
            if (ctx != null && ctx.getConfigRegistry().getTeleportFile() != null) {
                thresholdMs = ctx.getConfigRegistry().getTeleportFile().getLong("teleport.metrics.slow-request-threshold-ms", 3000L);
            }
        }

        long totalMs = context.getElapsedMillis();
        if (totalMs < thresholdMs) {
            return;
        }

        String message = "[sRandomRTP] Slow RTP request [" + status + "] player=" + playerName
                + ", total=" + totalMs + "ms"
                + ", attempts=" + context.getAttemptCount()
                + ", chunk=" + nanosToMillis(context.getChunkAcquireNanos()) + "ms"
                + ", safeY=" + nanosToMillis(context.getSafeSearchNanos()) + "ms"
                + ", finalTeleport=" + nanosToMillis(context.getFinalTeleportNanos()) + "ms"
                + ", chunk_reused=" + context.getChunkReusedCount()
                + ", chunk_regenerated=" + context.getChunkRegeneratedCount()
                + ", generated_only_phase_hits=" + context.getGeneratedOnlyPhaseHits()
                + ", fallback_to_generation=" + context.getFallbackToGenerationCount()
                + ", parallel_batches_used=" + context.getParallelBatchesUsed();

        plugin.getLogger().warning(message);
        appendSlowRequestReport(message);
    }

    public void logSummaryIfEnabled() {
        if (plugin == null || !plugin.getConfig().getBoolean("logs", false)) {
            return;
        }
        Bukkit.getLogger().info("[sRandomRTP] RTP metrics summary: coordinate avg="
                + formatAverage(coordinateTotalNanos.get(), coordinateSamples.get())
                + "ms, safeY avg=" + formatAverage(safeYTotalNanos.get(), safeYSamples.get())
                + "ms, chunk avg=" + formatAverage(chunkTotalNanos.get(), chunkSamples.get())
                + "ms, completed=" + completedRequests.get()
                + ", cancelled=" + cancellations.get()
                + ", refunds=" + refunds.get());
    }

    public long getCoordinateSamples() {
        return coordinateSamples.get();
    }

    public long getSafeYSamples() {
        return safeYSamples.get();
    }

    public long getChunkSamples() {
        return chunkSamples.get();
    }

    public long getCancellations() {
        return cancellations.get();
    }

    public long getCompletedRequests() {
        return completedRequests.get();
    }

    public long getRefunds() {
        return refunds.get();
    }

    public String getCoordinateAverageMillis() {
        return formatAverage(coordinateTotalNanos.get(), coordinateSamples.get());
    }

    public String getSafeYAverageMillis() {
        return formatAverage(safeYTotalNanos.get(), safeYSamples.get());
    }

    public String getChunkAverageMillis() {
        return formatAverage(chunkTotalNanos.get(), chunkSamples.get());
    }

    private void record(long durationNanos, AtomicLong samples, AtomicLong totalNanos) {
        if (durationNanos < 0L) {
            return;
        }
        samples.incrementAndGet();
        totalNanos.addAndGet(durationNanos);
    }

    private String formatAverage(long totalNanos, long samples) {
        if (samples <= 0L) {
            return "0.00";
        }
        double averageMillis = (totalNanos / 1_000_000.0D) / samples;
        return String.format(java.util.Locale.ROOT, "%.2f", averageMillis);
    }

    private long nanosToMillis(long nanos) {
        return nanos <= 0L ? 0L : nanos / 1_000_000L;
    }

    private void appendSlowRequestReport(String message) {
        if (!org.sRandomRTP.DifferentMethods.Variables.isLoggingEnabled()) {
            return;
        }
        File folder = new File(plugin.getDataFolder(), "Diagnostics");
        if (!folder.exists() && !folder.mkdirs()) {
            return;
        }

        File reportFile = new File(folder, "slow-rtp-requests.log");
        try (PrintWriter writer = new PrintWriter(new FileWriter(reportFile, true))) {
            writer.println("[" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "] " + message);
        } catch (IOException ignored) {
        }
    }
}
