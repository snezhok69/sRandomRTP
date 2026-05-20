package org.sRandomRTP;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.sRandomRTP.BlockBiomes.LoadBlockList;
import org.sRandomRTP.Checkings.*;
import org.sRandomRTP.Commands.CommandArgs;
import org.sRandomRTP.Commands.ConfiguredCommandAliases;
import org.sRandomRTP.Commands.OnTabCompletes;
import org.sRandomRTP.Chunk.ChunkWarmManager;
import org.sRandomRTP.Data.RtpCountDataStore;
import org.sRandomRTP.DataPortals.PortalSQLRepository;
import org.sRandomRTP.DataPortals.SQLManagerPortals;
import org.sRandomRTP.DifferentMethods.*;
import org.sRandomRTP.DifferentMethods.Teleport.CleanupTasks;
import org.sRandomRTP.DifferentMethods.Teleport.PerformTeleport;
import org.sRandomRTP.DifferentMethods.Teleport.TeleportRequestContext;
import org.sRandomRTP.DifferentMethods.BossBars.RemoveAllBossBars;
import org.sRandomRTP.DifferentMethods.Text.LoadLanguageFile;
import org.sRandomRTP.DifferentMethods.Text.TranslateRGBColors;
import org.sRandomRTP.Events.PortalAndEffectsListener;
import org.sRandomRTP.Events.TeleportCancellationListener;
import org.sRandomRTP.Files.*;
import org.sRandomRTP.Metrics.Metrics;
import com.tcoded.folialib.wrapper.task.WrappedTask;
import io.papermc.lib.PaperLib;
import org.sRandomRTP.Services.BootstrapCoordinator;
import org.sRandomRTP.Services.DiagnosticsService;
import org.sRandomRTP.Utils.ChatUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Main extends JavaPlugin {

    @Override
    public void onEnable() {
        DiagnosticsService.Report startupReport = null;
        try {
            Variables.initializePlugin(this);
            // Inform Spigot server owners that Paper provides better async chunk
            // loading, lower latency teleports, and improved thread safety for this plugin.
            PaperLib.suggestPaper(this);
            startupReport = Variables.getPluginContext().getDiagnosticsService().startReport("latest-startup-report");
            long startTime = System.currentTimeMillis();
            Bukkit.getConsoleSender().sendMessage("");
            ChatUtils.logError(">==========================================<");
            Bukkit.getConsoleSender().sendMessage("");
            ChatUtils.logInfo("Plugin initialization started...");
            ChatUtils.logInfo("Version check...");
            if (CheckingServerVersion.checkingServerVersion()) {
                if (startupReport != null) {
                    startupReport.stepWarn("server-version-check", "plugin disabled due to unsupported server version");
                    startupReport.finishSuccess();
                }
                return;
            }
            if (startupReport != null) {
                startupReport.stepOk("server-version-check", "supported");
            }

            Metrics metrics = new Metrics(this, org.sRandomRTP.Utils.PluginConstants.METRICS_PLUGIN_ID);
            BootstrapCoordinator.FileChangeSummary fileSummary = createAndUpdateFiles();
            if (startupReport != null) {
                if (fileSummary.isSuccessful()) {
                    startupReport.stepOk("files-sync", "created=" + fileSummary.getCreatedFiles().size()
                            + ", updated=" + fileSummary.getUpdatedFiles().size());
                } else {
                    startupReport.stepFail("files-sync", fileSummary.getFailure());
                }
            }
            if (!fileSummary.isSuccessful()) {
                throw new IllegalStateException("Failed to synchronize plugin files", fileSummary.getFailure());
            }
            loadFilesAndMetrics(metrics);
            ValidateConfigEntries.validateManagedConfigs(Variables.getPluginContext().getConfigRegistry());
            if (startupReport != null) {
                startupReport.stepOk("config-load", "managed configuration files loaded");
            }
            initDatabase();
            if (startupReport != null) {
                startupReport.stepOk("database-init", "portal repository and schema migrations ready");
            }
            checkOptionalPlugins();
            if (startupReport != null) {
                startupReport.stepOk("integration-scan", "optional plugins checked");
            }
            ChatUtils.logInfo("Loading data...");
            RtpCountDataStore.load();
            if (startupReport != null) {
                startupReport.stepOk("data-load", "runtime data loaded");
            }
            LoadKeys.loadKeys(Variables.getPluginContext().getConfigRegistry().getMainConfig());
            setupIntegrations();
            if (startupReport != null) {
                startupReport.stepOk("integration-setup", "economy, worldguard and console filters prepared");
            }
            registerEvents();
            if (startupReport != null) {
                startupReport.stepOk("event-registration", "listeners registered");
            }
            compareLanguageFiles();
            if (startupReport != null) {
                startupReport.stepOk("language-compare", "language files compared");
            }
            loadLanguagePortalsAndBlocks();
            if (startupReport != null) {
                startupReport.stepOk("language-and-portals-load", "messages, portal cache and banned lists scheduled");
            }
            registerCommands();
            if (startupReport != null) {
                startupReport.stepOk("command-registration", "commands registered");
            }
            startBackgroundTasks(metrics);
            if (startupReport != null) {
                startupReport.stepOk("background-tasks", "async background tasks started");
            }
            printStartupMessage(startTime);
            ChatUtils.logError(">==========================================<");
            Bukkit.getConsoleSender().sendMessage("");
            if (startupReport != null) {
                startupReport.finishSuccess();
            }
        } catch (RuntimeException e) {
            if (startupReport != null) {
                startupReport.finishFailure(e);
            }
            LoggerUtility.loggerUtility(Main.class, e);
        }
    }

    private void initDatabase() {
        Variables.getPluginContext().getBootstrapCoordinator().initializeDatabase();
    }

    private void loadFilesAndMetrics(Metrics metrics) {
        if (Bukkit.getServer().getName().equalsIgnoreCase("Folia")) {
            ChatUtils.logError("Used Folia there may be errors or bugs!...");
            metrics.addCustomChart(new Metrics.SimplePie("using_folia", () -> "Yes"));
        } else {
            metrics.addCustomChart(new Metrics.SimplePie("using_folia", () -> "No"));
        }
        if (Variables.getPluginContext() != null) {
            Variables.getPluginContext().getConfigRegistry().reload();
        }
    }

    private void checkOptionalPlugins() {
        ChatUtils.logInfo("Checking installed PlaceHolderAPI...");
        CheckingInstalledPlaceHolderAPI.checkingInstalledPlaceHolderAPI();
        ChatUtils.logInfo("Checking installed Chunky...");
        CheckingInstalledChunky.checkingInstalledChunky();
    }

    private void registerEvents() {
        ChatUtils.logInfo("Loading events...");
        getServer().getPluginManager().registerEvents(new TeleportCancellationListener(), this);
        getServer().getPluginManager().registerEvents(new PortalAndEffectsListener(), this);
    }

    private BootstrapCoordinator.FileChangeSummary createAndUpdateFiles() {
        ChatUtils.logInfo("Creating and synchronizing files...");
        BootstrapCoordinator.FileChangeSummary summary =
                Variables.getPluginContext().getBootstrapCoordinator().synchronizeFiles();
        if (!summary.isSuccessful()) {
            String hint = Variables.isDiagnosticEnabled()
                    ? "Check LogsErrors/latest-error.log and Diagnostics/latest-startup-report.txt"
                    : "Check LogsErrors/latest-error.log or enable diagnostic: true for startup reports";
            ChatUtils.logError("Failed to synchronize plugin files. " + hint);
            return summary;
        }
        long createTime = System.currentTimeMillis();
        for (String message : summary.getCreatedFiles()) {
            if (message != null) {
                long elapsedTime = System.currentTimeMillis() - createTime;
                ChatUtils.logSuccess(String.format("File %s successfully created §6(%d ms)", message, elapsedTime));
            }
        }
        long updateTime = System.currentTimeMillis();
        for (String message : summary.getUpdatedFiles()) {
            long elapsedTime = System.currentTimeMillis() - updateTime;
            ChatUtils.logSuccess(String.format("File %s successfully updated §6(%d ms)", message, elapsedTime));
        }
        return summary;
    }

    private void setupIntegrations() {
        boolean vaultPresent = Bukkit.getPluginManager().getPlugin("Vault") != null;
        Variables.getPluginContext().setVaultAvailable(vaultPresent);
        Variables.getPluginContext().setupEconomy();
        // Use plugin manager instead of Class.forName() for consistent detection
        // and compatibility with dynamic WorldGuard installation patterns.
        boolean wgPresent = Bukkit.getPluginManager().getPlugin("WorldGuard") != null;
        Variables.getPluginContext().setWorldGuardAvailable(wgPresent);
        FileConfiguration config = Variables.getPluginContext().getConfigRegistry().getMainConfig();
        boolean disableMovedTooQuicklyMessages = config != null
                && config.getBoolean("Disable-Moved-Too-Quickly-Messages", true);
        if (disableMovedTooQuicklyMessages) {
            ChatUtils.logInfo("Initializing a console filter to block fast move messages...");
            ConsoleFilter.registerFilter(true);
            ChatUtils.logSuccess("The console filter has been successfully initialized");
        }
    }

    // All bundled language files that should be compared against the reference (ru.yml).
    // To add a new language, add its filename here.
    private static final String[] LANG_FILES = {
        "ar.yml", "custom_messages.yml", "de.yml", "en.yml", "es.yml",
        "fr.yml", "it.yml", "ja.yml", "ko.yml", "pl.yml", "pt.yml",
        "ua.yml", "vi.yml", "zh.yml", "tr.yml"
    };

    private void compareLanguageFiles() {
        CheckingFile checkingFile = new CheckingFile();
        for (String langFile : LANG_FILES) {
            checkingFile.compareLanguageFiles(langFile, "ru.yml");
        }
    }

    private void loadLanguagePortalsAndBlocks() {
        LoadLanguageFile loadLanguageFile = new LoadLanguageFile();
        loadLanguageFile.loadLanguageFile();
        YamlConfiguration langFile = loadLanguageFile.getLangFile();
        LoadMessages.loadMessages(langFile);
        FilesAutoReload.startFilesAutoReload();
        ChatUtils.logInfo("Loading portal data from database...");
        java.util.concurrent.CompletableFuture<Void> portalFutures = java.util.concurrent.CompletableFuture.allOf(
                PortalSQLRepository.loadPortalTasksFromDatabaseSQL(),
                PortalSQLRepository.loadPortalsPlayerFromDatabaseSQL(),
                PortalSQLRepository.loadPortalBlocksPlayerToDatabaseSQL()
        );
        // Non-blocking: portal data becomes available shortly after startup.
        // Do NOT call portalFutures.join() — blocking the main thread during startup
        // is illegal on Folia and causes a TPS spike on Paper.
        portalFutures
                .thenRun(() -> ChatUtils.logInfo("Portal data loaded."))
                .exceptionally(ex -> {
                    ChatUtils.logError("Failed to load portal data: " + ex.getMessage());
                    getLogger().severe("sRandomRTP: portal data load failure");
                    if (ex.getCause() != null) {
                        getLogger().severe("Cause: " + ex.getCause().toString());
                    }
                    return null;
                });
        ChatUtils.logInfo("Uploading lists of banned blocks and biomes...");
        LoadBlockList.loadBlockListAsync(this).thenRun(() ->
            ChatUtils.logInfo("The lists of banned blocks and biomes have been successfully uploaded")
        ).exceptionally(ex -> {
            ChatUtils.logError("Failed to load banned block/biome lists — RTP safety filters are empty: " + ex.getMessage());
            // Log the full stack trace so the server owner can diagnose the root cause
            getLogger().severe("sRandomRTP: block/biome list load failure");
            if (ex.getCause() != null) {
                getLogger().severe("Cause: " + ex.getCause().toString());
            }
            return null;
        });
    }

    private void registerCommands() {
        ChatUtils.logInfo("Loading commands...");
        Map<String, Map<String, Object>> commands = getDescription().getCommands();
        if (commands != null) {
            for (String commandName : commands.keySet()) {
                PluginCommand pluginCommand = getCommand(commandName);
                if (pluginCommand == null) {
                    getLogger().warning("Command '" + commandName + "' is not defined in plugin.yml");
                    continue;
                }
                pluginCommand.setExecutor(new CommandArgs());
                pluginCommand.setTabCompleter(new OnTabCompletes());
            }
        }
        ConfiguredCommandAliases.apply(this);
    }

    private void startBackgroundTasks(Metrics metrics) {
        ChatUtils.logInfo("Running tasks...");
        Variables.getReleaseCheckService().triggerStartupConsoleCheck();
        Variables.getReleaseCheckService().startAutoChecks();
        Variables.getPluginContext().cancelBackgroundTasks();
        WrappedTask cleanupTask = org.sRandomRTP.DifferentMethods.Teleport.FoliaSchedulerFacade.runTimerAsync(
                () -> {
                    Variables.cleanExpiredCooldowns(org.sRandomRTP.Utils.PluginConstants.COOLDOWN_CLEANUP_MAX_AGE_MS);
                    org.sRandomRTP.Cooldowns.CooldownManager.instance().evictExpiredCache();
                    RtpCountDataStore.saveIfDirty();
                    org.sRandomRTP.Commands.portal.PortalTeleportCooldownManager mgr =
                            Variables.getPortalTeleportCooldownManager();
                    if (mgr != null) mgr.cleanExpired();
                },
                org.sRandomRTP.Utils.PluginConstants.BACKGROUND_TASK_PERIOD_TICKS,
                org.sRandomRTP.Utils.PluginConstants.BACKGROUND_TASK_PERIOD_TICKS);
        Variables.getPluginContext().setCooldownCleanupTask(cleanupTask);
        ChatUtils.logInfo("Sending anonymous statistics...");
        try {
            metrics.addCustomChart(new Metrics.SimplePie("lang", this::resolveMetricsLanguage));
        } catch (RuntimeException e) {
            LoggerUtility.loggerUtility(Main.class, e);
        }
    }

    private String resolveMetricsLanguage() {
        String language = LoadKeys.language;
        if ((language == null || language.trim().isEmpty()) && Variables.getInstance() != null
                && Variables.getInstance().getConfig() != null) {
            language = Variables.getInstance().getConfig().getString("Language");
        }
        if (language == null || language.trim().isEmpty()) {
            language = "en";
        }
        return language.trim().toLowerCase(Locale.ROOT);
    }

    private void printStartupMessage(long startTime) {
        for (String line : LoadMessages.PluginEnabledMessage) {
            long enabledPluginTime = System.currentTimeMillis() - startTime;
            line = line.replace("%mc%", enabledPluginTime + "")
                    .replace("%server-version%", Bukkit.getServer().getVersion())
                    .replace("%os-version%", System.getProperty("os.name") + " " + System.getProperty("os.version") + " (" + System.getProperty("os.arch") + ")")
                    .replace("%java-version-server%", System.getProperty("java.version"));
            Bukkit.getConsoleSender().sendMessage(TranslateRGBColors.translateRGBColors(line));
        }
    }

    @Override
    public void onDisable() {
        RtpCountDataStore.saveIfDirty();
        org.sRandomRTP.Commands.ConfiguredCommandAliases.unregister(this);
        Variables.getPluginContext().cancelBackgroundTasks();
        org.bukkit.event.HandlerList.unregisterAll(this);
        TeleportRequestContext.shutdownTimeoutScheduler();
        PerformTeleport.clearAll();
        CleanupTasks.clearAll();
        ChunkWarmManager.shutdown();
        if (!Variables.getPluginContext().isPluginToggle()) {
            long startTime = System.currentTimeMillis();
            try {
                ConsoleFilter.removeFilter();
                for (String line : LoadMessages.PluginDisabledMessage) {
                    long disabledPluginTime = System.currentTimeMillis() - startTime;
                    line = line.replace("%mc%", disabledPluginTime + "");
                    Bukkit.getConsoleSender().sendMessage(TranslateRGBColors.translateRGBColors(line));
                }
                RemoveAllBossBars.removeAllBossBars();
                FilesAutoReload.stopFilesAutoReload();
                Variables.getPluginContext().getBootstrapCoordinator().shutdown();
            } catch (RuntimeException e) {
                getLogger().severe("An error occurred while disabling the plugin: " + e.getMessage());
            }
        }
    }
}
