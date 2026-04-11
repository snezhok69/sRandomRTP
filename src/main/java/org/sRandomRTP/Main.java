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
import org.sRandomRTP.Services.BootstrapCoordinator;
import org.sRandomRTP.Services.DiagnosticsService;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Main extends JavaPlugin {

    private WrappedTask cooldownCleanupTask;

    @Override
    public void onEnable() {
        DiagnosticsService.Report startupReport = null;
        try {
            Variables.initializePlugin(this);
            startupReport = Variables.getPluginContext().getDiagnosticsService().startReport("latest-startup-report");
            long startTime = System.currentTimeMillis();
            Bukkit.getConsoleSender().sendMessage("");
            Bukkit.getConsoleSender().sendMessage(Variables.pluginName + " §8- §c>==========================================<");
            Bukkit.getConsoleSender().sendMessage("");
            Bukkit.getConsoleSender().sendMessage(Variables.pluginName + " §8- §ePlugin initialization started...");
            Bukkit.getConsoleSender().sendMessage(Variables.pluginName + " §8- §eVersion check...");
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
            Bukkit.getConsoleSender().sendMessage(Variables.pluginName + " §8- §eLoading data...");
            RtpCountDataStore.load();
            if (startupReport != null) {
                startupReport.stepOk("data-load", "runtime data loaded");
            }
            registerEvents();
            if (startupReport != null) {
                startupReport.stepOk("event-registration", "listeners registered");
            }

            FileConfiguration config = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "config.yml"));
            LoadKeys.loadKeys(config);
            setupIntegrations(config);
            if (startupReport != null) {
                startupReport.stepOk("integration-setup", "economy, worldguard and console filters prepared");
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
            Bukkit.getConsoleSender().sendMessage(Variables.pluginName + " §8- §c>==========================================<");
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
            Bukkit.getConsoleSender().sendMessage(Variables.pluginName + " §8- §cUsed Folia there may be errors or bugs!...");
            metrics.addCustomChart(new Metrics.SimplePie("using_folia", () -> "Yes"));
        } else {
            metrics.addCustomChart(new Metrics.SimplePie("using_folia", () -> "No"));
        }
        LoadFiles.loadFiles();
    }

    private void checkOptionalPlugins() {
        Bukkit.getConsoleSender().sendMessage(Variables.pluginName + " §8- §eChecking installed PlaceHolderAPI...");
        CheckingInstalledPlaceHolderAPI.checkingInstalledPlaceHolderAPI();
        Bukkit.getConsoleSender().sendMessage(Variables.pluginName + " §8- §eChecking installed Chunky...");
        CheckingInstalledChunky.checkingInstalledChunky();
    }

    private void registerEvents() {
        Bukkit.getConsoleSender().sendMessage(Variables.pluginName + " §8- §eLoading events...");
        getServer().getPluginManager().registerEvents(new TeleportCancellationListener(), this);
        getServer().getPluginManager().registerEvents(new PortalAndEffectsListener(), this);
    }

    private BootstrapCoordinator.FileChangeSummary createAndUpdateFiles() {
        Bukkit.getConsoleSender().sendMessage(Variables.pluginName + " §8- §eCreating and synchronizing files...");
        BootstrapCoordinator.FileChangeSummary summary =
                Variables.getPluginContext().getBootstrapCoordinator().synchronizeFiles();
        if (!summary.isSuccessful()) {
            Bukkit.getConsoleSender().sendMessage(Variables.pluginName
                    + " §8- §cFailed to synchronize plugin files. Check LogsErrors/latest-error.log and Diagnostics/latest-startup-report.txt");
            return summary;
        }
        long createTime = System.currentTimeMillis();
        for (String message : summary.getCreatedFiles()) {
            if (message != null) {
                long elapsedTime = System.currentTimeMillis() - createTime;
                Bukkit.getConsoleSender().sendMessage(String.format(
                        Variables.pluginName + " §8- §aFile %s successfully created §6(%d ms)", message, elapsedTime));
            }
        }
        FilesUpdate filesUpdate = new FilesUpdate();
        long updateTime = System.currentTimeMillis();
        for (String message : summary.getUpdatedFiles()) {
            long elapsedTime = System.currentTimeMillis() - updateTime;
            Bukkit.getConsoleSender().sendMessage(String.format(
                    Variables.pluginName + " §8- §aFile %s successfully updated §6(%d ms)", message, elapsedTime));
        }
        return summary;
    }

    private void setupIntegrations(FileConfiguration config) {
        Variables.setupEconomy();
        Variables.isVaultAvailable = Bukkit.getPluginManager().getPlugin("Vault") != null;
        try {
            Class.forName("com.sk89q.worldguard.bukkit.WorldGuardPlugin");
            Variables.isWorldGuardAvailable = true;
        } catch (ClassNotFoundException ignored) {}
        boolean disableMovedTooQuicklyMessages = config.getBoolean("Disable-Moved-Too-Quickly-Messages", true);
        if (disableMovedTooQuicklyMessages) {
            Bukkit.getConsoleSender().sendMessage(Variables.pluginName + " §8- §eInitializing a console filter to block fast move messages...");
            ConsoleFilter.registerFilter(true);
            Bukkit.getConsoleSender().sendMessage(Variables.pluginName + " §8- §aThe console filter has been successfully initialized");
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
        PortalSQLRepository.loadPortalTasksFromDatabaseSQL();
        PortalSQLRepository.loadPortalsPlayerFromDatabaseSQL();
        PortalSQLRepository.loadPortalBlocksPlayerToDatabaseSQL();
        Bukkit.getConsoleSender().sendMessage(Variables.pluginName + " §8- §eUploading lists of banned blocks and biomes...");
        LoadBlockList.loadBlockListAsync(this).thenRun(() ->
            Bukkit.getConsoleSender().sendMessage(Variables.pluginName + " §8- §eThe lists of banned blocks and biomes have been successfully uploaded")
        ).exceptionally(ex -> {
            Bukkit.getConsoleSender().sendMessage(Variables.pluginName + " §8- §cError when loading lists: " + ex.getMessage());
            return null;
        });
    }

    private void registerCommands() {
        Bukkit.getConsoleSender().sendMessage(Variables.pluginName + " §8- §eLoading commands...");
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
    }

    private void startBackgroundTasks(Metrics metrics) {
        Bukkit.getConsoleSender().sendMessage(Variables.pluginName + " §8- §eRunning tasks...");
        Variables.getReleaseCheckService().triggerStartupConsoleCheck();
        Variables.getReleaseCheckService().startAutoChecks();
        if (cooldownCleanupTask != null) {
            cooldownCleanupTask.cancel();
        }
        cooldownCleanupTask = Variables.getFoliaLib().getImpl().runTimerAsync(
                () -> {
                    Variables.cleanExpiredCooldowns(org.sRandomRTP.Utils.PluginConstants.COOLDOWN_CLEANUP_MAX_AGE_MS);
                    org.sRandomRTP.Cooldowns.CooldownManager.evictExpiredCacheEntries();
                    org.sRandomRTP.Commands.CommandSetPortal.cleanExpiredPortalCooldowns();
                },
                org.sRandomRTP.Utils.PluginConstants.BACKGROUND_TASK_PERIOD_TICKS,
                org.sRandomRTP.Utils.PluginConstants.BACKGROUND_TASK_PERIOD_TICKS);
        Bukkit.getConsoleSender().sendMessage(Variables.pluginName + " §8- §eSending anonymous statistics...");
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
        if (cooldownCleanupTask != null) cooldownCleanupTask.cancel();
        org.bukkit.event.HandlerList.unregisterAll(this);
        TeleportRequestContext.shutdownTimeoutScheduler();
        PerformTeleport.clearAll();
        CleanupTasks.clearAll();
        ChunkWarmManager.shutdown();
        if (!Variables.pluginToggle) {
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
                Variables.getInstance().getLogger().severe("An error occurred while disabling the plugin: " + e.getMessage());
            }
        }
    }
}
