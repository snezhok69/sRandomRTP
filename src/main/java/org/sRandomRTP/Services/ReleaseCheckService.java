package org.sRandomRTP.Services;

import com.tcoded.folialib.wrapper.task.WrappedTask;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.sRandomRTP.DifferentMethods.Teleport.FoliaSchedulerFacade;
import org.sRandomRTP.DifferentMethods.Variables;
import org.sRandomRTP.Utils.ChatUtils;
import org.sRandomRTP.Files.LoadMessages;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class ReleaseCheckService {

    private static final long CACHE_TTL_MILLIS = 60_000L;

    private final MessageService messageService;
    private final ExecutorService executor;
    private final AtomicReference<ReleaseCheckResult> cachedResult = new AtomicReference<ReleaseCheckResult>();
    private final AtomicReference<CompletableFuture<ReleaseCheckResult>> inFlight = new AtomicReference<CompletableFuture<ReleaseCheckResult>>();
    private final AtomicInteger severeOutdatedWarningCount = new AtomicInteger();

    private volatile WrappedTask autoCheckTask;

    public ReleaseCheckService(MessageService messageService) {
        this.messageService = messageService;
        this.executor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "sRandomRTP-release-check");
            thread.setDaemon(true);
            return thread;
        });
    }

    public void triggerStartupConsoleCheck() {
        requestLatestVersion(false).whenComplete((result, throwable) -> {
            if (throwable != null || result == null) {
                return;
            }

            if (result.isUpdateAvailable()) {
                sendLines(Bukkit.getConsoleSender(), LoadMessages.newVersionMessage,
                        "%new-CommandVersion%", result.getLatestVersion(),
                        "%old-CommandVersion%", result.getCurrentVersion());
            }
            maybeWarnSeverelyOutdated(result);
        });
    }

    public void startAutoChecks() {
        stopAutoChecks();

        if (Variables.getInstance() == null || Variables.getInstance().getConfig() == null) {
            return;
        }

        // Read config values once on the main thread (FileConfiguration is not thread-safe).
        // The lambda captures these as effectively-final so the async timer never touches YAML.
        final boolean playersEnabled = Variables.getInstance().getConfig().getBoolean("Auto-Checking-New-Version-Players-Enabled");
        final boolean consoleEnabled = Variables.getInstance().getConfig().getBoolean("Auto-Checking-New-Version-Console-Enabled");
        if (!playersEnabled && !consoleEnabled) {
            return;
        }

        long periodSeconds = Math.max(30L, Variables.getInstance().getConfig().getLong("Period-Checking-New-Version", 1800L));
        long periodTicks = periodSeconds * 20L;

        autoCheckTask = Variables.getFoliaLib().getImpl().runTimerAsync(() -> {
            requestLatestVersion(true).whenComplete((result, throwable) -> {
                if (throwable != null || result == null) {
                    Bukkit.getConsoleSender().sendMessage("§b[sRandomRTP] §8- §cError when checking new plugin version: §6"
                            + safe(throwable == null ? null : throwable.getMessage()));
                    return;
                }

                if (result.getStatus() == Status.ERROR) {
                    Bukkit.getConsoleSender().sendMessage("§b[sRandomRTP] §8- §cError when checking new plugin version: §6"
                            + safe(result.getErrorMessage()));
                    return;
                }

                if (result.isUpdateAvailable()) {
                    if (consoleEnabled) {
                        sendLines(Bukkit.getConsoleSender(), LoadMessages.newVersionMessage,
                                "%new-CommandVersion%", result.getLatestVersion(),
                                "%old-CommandVersion%", result.getCurrentVersion());
                    }
                    if (playersEnabled) {
                        for (Player player : Bukkit.getOnlinePlayers()) {
                            if (player != null && player.isOnline()) {
                                sendLines(player, LoadMessages.newVersionMessage,
                                        "%new-CommandVersion%", result.getLatestVersion(),
                                        "%old-CommandVersion%", result.getCurrentVersion());
                            }
                        }
                    }
                }

                maybeWarnSeverelyOutdated(result);
            });
        }, periodTicks, periodTicks);
        Variables.autoCheckVersionTask = autoCheckTask;
    }

    public void restartAutoChecks() {
        startAutoChecks();
    }

    public void stopAutoChecks() {
        WrappedTask task = autoCheckTask;
        if (task != null) {
            task.cancel();
        }
        autoCheckTask = null;
        Variables.autoCheckVersionTask = null;
    }

    public void sendVersionStatus(CommandSender sender) {
        sendLines(sender, LoadMessages.CheckingVersion);
        requestLatestVersion(false).whenComplete((result, throwable) -> {
            if (throwable != null || result == null || result.getStatus() == Status.ERROR) {
                String error = throwable != null ? throwable.getMessage() : result == null ? "unknown error" : result.getErrorMessage();
                sendLines(sender, LoadMessages.ErrorCheckingVersionMessage, "%error%", safe(error));
                return;
            }

            if (result.isUpdateAvailable()) {
                sendLines(sender, LoadMessages.newVersionMessage,
                        "%new-CommandVersion%", result.getLatestVersion(),
                        "%old-CommandVersion%", result.getCurrentVersion());
            } else {
                sendLines(sender, LoadMessages.LatestVersionMessage,
                        "%latest-CommandVersion%", result.getCurrentVersion());
            }
        });
    }

    public void shutdown() {
        stopAutoChecks();
        executor.shutdownNow();
    }

    CompletableFuture<ReleaseCheckResult> requestLatestVersion(boolean forceRefresh) {
        ReleaseCheckResult cached = cachedResult.get();
        long now = System.currentTimeMillis();
        if (!forceRefresh && cached != null && (now - cached.getCheckedAtMillis()) <= CACHE_TTL_MILLIS) {
            return CompletableFuture.completedFuture(cached);
        }

        CompletableFuture<ReleaseCheckResult> existing = inFlight.get();
        if (existing != null && !existing.isDone()) {
            return existing;
        }

        CompletableFuture<ReleaseCheckResult> created = CompletableFuture.supplyAsync(() -> fetchLatestVersion(), executor);
        if (!inFlight.compareAndSet(existing, created)) {
            CompletableFuture<ReleaseCheckResult> current = inFlight.get();
            return current != null ? current : created;
        }

        created.whenComplete((result, throwable) -> {
            if (result != null) {
                cachedResult.set(result);
            } else if (throwable != null) {
                cachedResult.set(ReleaseCheckResult.error(currentPluginVersion(), throwable.getMessage()));
            }
            inFlight.compareAndSet(created, null);
        });
        return created;
    }

    private ReleaseCheckResult fetchLatestVersion() {
        HttpURLConnection connection = null;
        String currentVersion = currentPluginVersion();
        try {
            URL url = new URL(PluginVersionCatalog.REMOTE_VERSION_URL);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(10_000);
            connection.setReadTimeout(10_000);
            connection.setRequestMethod("GET");

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                return ReleaseCheckResult.error(currentVersion, "HTTP " + responseCode);
            }

            String latestVersion;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                latestVersion = reader.readLine();
            }

            if (latestVersion == null || latestVersion.trim().isEmpty()) {
                return ReleaseCheckResult.error(currentVersion, "Empty version response");
            }

            latestVersion = latestVersion.trim();
            boolean updateAvailable = compareVersions(latestVersion, currentVersion) > 0;
            return ReleaseCheckResult.success(currentVersion, latestVersion, updateAvailable);
        } catch (IOException | RuntimeException e) {
            return ReleaseCheckResult.error(currentVersion, e.getMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private void maybeWarnSeverelyOutdated(ReleaseCheckResult result) {
        if (result == null || !result.isUpdateAvailable()) {
            return;
        }

        int versionGap = computeMinorGap(result.getLatestVersion(), result.getCurrentVersion());
        if (versionGap < 5) {
            severeOutdatedWarningCount.set(0);
            return;
        }

        Bukkit.getConsoleSender().sendMessage("");
        Bukkit.getConsoleSender().sendMessage(ChatUtils.PLUGIN_NAME + " §8- §c> WARNING ========================================== WARNING <");
        Bukkit.getConsoleSender().sendMessage("");
        Bukkit.getConsoleSender().sendMessage(ChatUtils.PLUGIN_NAME + " §8- §cYour plugin is behind by §6" + versionGap
                + " §cversions! The latest version: §6" + result.getLatestVersion()
                + "§c. Your version: §6" + result.getCurrentVersion() + "§c.");
        Bukkit.getConsoleSender().sendMessage(ChatUtils.PLUGIN_NAME + " §8- §cPlease update the plugin!");
        Bukkit.getConsoleSender().sendMessage("");
        Bukkit.getConsoleSender().sendMessage(ChatUtils.PLUGIN_NAME + " §8- §c> WARNING ========================================== WARNING <");
        Bukkit.getConsoleSender().sendMessage("");

        if (severeOutdatedWarningCount.incrementAndGet() >= 10 && Variables.getInstance() != null) {
            Variables.getInstance().getServer().getPluginManager().disablePlugin(Variables.getInstance());
        }
    }

    private int computeMinorGap(String latestVersion, String currentVersion) {
        int[] latest = firstTwoNumericParts(latestVersion);
        int[] current = firstTwoNumericParts(currentVersion);
        int latestNormalized = latest[0] * 100 + latest[1];
        int currentNormalized = current[0] * 100 + current[1];
        return latestNormalized - currentNormalized;
    }

    static int compareVersions(String left, String right) {
        int[] leftParts = numericParts(left);
        int[] rightParts = numericParts(right);
        int maxLength = Math.max(leftParts.length, rightParts.length);
        for (int i = 0; i < maxLength; i++) {
            int leftValue = i < leftParts.length ? leftParts[i] : 0;
            int rightValue = i < rightParts.length ? rightParts[i] : 0;
            if (leftValue != rightValue) {
                return leftValue - rightValue;
            }
        }
        return 0;
    }

    private static int[] firstTwoNumericParts(String version) {
        int[] all = numericParts(version);
        int major = all.length > 0 ? all[0] : 0;
        int minor = all.length > 1 ? all[1] : 0;
        return new int[]{major, minor};
    }

    private static int[] numericParts(String version) {
        if (version == null || version.trim().isEmpty()) {
            return new int[0];
        }
        String[] tokens = version.trim().split("[^0-9]+");
        int count = 0;
        int[] temp = new int[tokens.length];
        for (String token : tokens) {
            if (token == null || token.isEmpty()) {
                continue;
            }
            try {
                temp[count++] = Integer.parseInt(token);
            } catch (NumberFormatException ignored) {
            }
        }
        int[] result = new int[count];
        System.arraycopy(temp, 0, result, 0, count);
        return result;
    }

    private String currentPluginVersion() {
        return Variables.getInstance() == null ? "unknown" : Variables.getInstance().getDescription().getVersion();
    }

    private String safe(String value) {
        return value == null || value.trim().isEmpty() ? "unknown error" : value;
    }

    private void sendLines(CommandSender sender, List<String> lines, String... replacements) {
        if (sender == null || lines == null) {
            return;
        }
        if (sender instanceof Player) {
            Player player = (Player) sender;
            FoliaSchedulerFacade.runAtEntity(player,
                    () -> messageService.send(player, lines, replacements));
            return;
        }
        messageService.send(sender, lines, replacements);
    }

    static final class ReleaseCheckResult {
        private final String currentVersion;
        private final String latestVersion;
        private final Status status;
        private final String errorMessage;
        private final long checkedAtMillis;

        private ReleaseCheckResult(String currentVersion, String latestVersion, Status status, String errorMessage) {
            this.currentVersion = currentVersion;
            this.latestVersion = latestVersion;
            this.status = status;
            this.errorMessage = errorMessage;
            this.checkedAtMillis = System.currentTimeMillis();
        }

        static ReleaseCheckResult success(String currentVersion, String latestVersion, boolean updateAvailable) {
            return new ReleaseCheckResult(currentVersion, latestVersion,
                    updateAvailable ? Status.UPDATE_AVAILABLE : Status.UP_TO_DATE, null);
        }

        static ReleaseCheckResult error(String currentVersion, String errorMessage) {
            return new ReleaseCheckResult(currentVersion, currentVersion, Status.ERROR, errorMessage);
        }

        String getCurrentVersion() {
            return currentVersion;
        }

        String getLatestVersion() {
            return latestVersion;
        }

        Status getStatus() {
            return status;
        }

        String getErrorMessage() {
            return errorMessage;
        }

        long getCheckedAtMillis() {
            return checkedAtMillis;
        }

        boolean isUpdateAvailable() {
            return status == Status.UPDATE_AVAILABLE;
        }
    }

    enum Status {
        UP_TO_DATE,
        UPDATE_AVAILABLE,
        ERROR
    }
}
