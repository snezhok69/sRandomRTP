package org.sRandomRTP.Commands;

import com.tcoded.folialib.wrapper.task.WrappedTask;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.sRandomRTP.BlockBiomes.LoadBlockList;
import org.sRandomRTP.DifferentMethods.*;
import org.sRandomRTP.DifferentMethods.Text.LoadLanguageFile;
import org.sRandomRTP.Files.*;
import org.sRandomRTP.DataPortals.PortalDataTasks;
import org.sRandomRTP.Services.BootstrapCoordinator;
import org.sRandomRTP.Services.DiagnosticsService;
import org.sRandomRTP.Services.RuntimeStateRegistry;

import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class CommandReload {
    private static final AtomicBoolean isReloading = new AtomicBoolean(false);

    public static void commandReload(CommandSender sender) {
        DiagnosticsService.Report reloadReport = Variables.getPluginContext()
                .getDiagnosticsService().startReport("latest-reload-report");
        long startTime = System.currentTimeMillis();
        if (!sender.hasPermission(Permissions.RELOAD)) {
            Variables.getMessageService().send(sender, LoadMessages.nopermissionreload);
            reloadReport.stepWarn("permission-check", "sender has no reload permission");
            reloadReport.finishSuccess();
            return;
        }

        if (!isReloading.compareAndSet(false, true)) {
            Variables.getMessageService().send(sender, LoadMessages.reloadingwait);
            reloadReport.stepWarn("reload-guard", "reload already in progress");
            reloadReport.finishSuccess();
            return;
        }

        try {
            Variables.getInstance().reloadConfig();
            BootstrapCoordinator.FileChangeSummary summary =
                    Variables.getPluginContext().getBootstrapCoordinator().synchronizeFiles();
            if (!summary.isSuccessful()) {
                reloadReport.stepFail("files-sync", summary.getFailure());
                Variables.getMessageService().send(sender, Collections.singletonList(
                        "&cReload failed during file synchronization. Check Diagnostics/latest-reload-report.txt and LogsErrors/latest-error.log"));
                reloadReport.finishFailure(summary.getFailure());
                isReloading.set(false);
                return;
            }
            reloadReport.stepOk("files-sync", "created=" + summary.getCreatedFiles().size()
                    + ", updated=" + summary.getUpdatedFiles().size());
            Bukkit.getConsoleSender().sendMessage(Variables.pluginName + " §8- §eChecking files...");
            long createTime = System.currentTimeMillis();
            for (String message : summary.getCreatedFiles()) {
                if (message != null) {
                    long elapsedTime = System.currentTimeMillis() - createTime;
                    String formattedLine = String.format(Variables.pluginName + " §8- §aFile %s created successfully §6(%d ms)", message, elapsedTime);
                    Bukkit.getConsoleSender().sendMessage(formattedLine);
                }
            }

            Bukkit.getConsoleSender().sendMessage(Variables.pluginName + " §8- §eUpdating files...");
            long updateTime = System.currentTimeMillis();
            for (String message : summary.getUpdatedFiles()) {
                long elapsedTime = System.currentTimeMillis() - updateTime;
                String formattedLine = String.format(Variables.pluginName + " §8- §aFile %s updated successfully §6(%d ms)", message, elapsedTime);
                Bukkit.getConsoleSender().sendMessage(formattedLine);
            }

            File configFile = new File(Variables.getInstance().getDataFolder(), "config.yml");
            FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
            LoadKeys.loadKeys(config);
            LoadLanguageFile loadLanguageFile = new LoadLanguageFile();
            loadLanguageFile.loadLanguageFile();
            YamlConfiguration langFile = loadLanguageFile.getLangFile();
            LoadMessages.loadMessages(langFile);
            reloadReport.stepOk("language-load", "language and messages reloaded");

            Variables.getMessageService().send(sender, LoadMessages.reloadingstart);

            final int[] step = {0};
            WrappedTask task = Variables.getFoliaLib().getImpl().runTimerAsync(() -> {
                try {
                    switch (step[0]) {
                        case 0:
                            loadLanguageFile.loadLanguageFile();
                            YamlConfiguration reloadedLang = loadLanguageFile.getLangFile();
                            LoadMessages.loadMessages(reloadedLang);
                            reloadReport.stepOk("step-0", "language files reloaded");
                            break;
                        case 1:
                            Variables.getInstance().reloadConfig();
                            LoadFiles.loadFiles();
                            LoadKeys.loadKeys(Variables.getInstance().getConfig());
                            LoadBlockList.loadBlockList();
                            refreshPortalSettings();
                            warnIfConfigInvalid();
                            reloadReport.stepOk("step-1", "configs, block lists and portal settings refreshed");
                            break;
                        case 2:
                            Variables.getReleaseCheckService().restartAutoChecks();
                            reloadReport.stepOk("step-2", "background version checker restarted");
                            break;
                        case 3:
                            for (String line : LoadMessages.successfullyreload) {
                                long endTime = System.currentTimeMillis();
                                long reloadPluginTime = endTime - startTime;
                                line = line.replace("%mc%", reloadPluginTime + "");
                                sender.sendMessage(Variables.getMessageService().format(line));
                            }
                            if (Variables.commandReloadTask != null) {
                                Variables.commandReloadTask.cancel();
                                Variables.commandReloadTask = null;
                            }
                            isReloading.set(false);
                            reloadReport.finishSuccess();
                            return;
                    }
                    step[0]++;
                } catch (RuntimeException e) {
                    reloadReport.stepFail("reload-step-" + step[0], e);
                    reloadReport.finishFailure(e);
                    LoggerUtility.loggerUtility(CommandReload.class, e);
                    if (Variables.commandReloadTask != null) {
                        Variables.commandReloadTask.cancel();
                        Variables.commandReloadTask = null;
                    }
                    isReloading.set(false);
                }
            }, 1L, 1L);
            Variables.commandReloadTask = task;
        } catch (RuntimeException e) {
            reloadReport.stepFail("reload-bootstrap", e);
            reloadReport.finishFailure(e);
            LoggerUtility.loggerUtility(CommandReload.class, e);
            if (Variables.commandReloadTask != null) {
                Variables.commandReloadTask.cancel();
                Variables.commandReloadTask = null;
            }
            isReloading.set(false);
        }
    }

    /**
     * Logs a warning if critical config sections are null after reload.
     * This can happen when a YAML file is malformed or has a wrong structure.
     */
    private static void warnIfConfigInvalid() {
        if (Variables.teleportfile == null || !Variables.teleportfile.contains("teleport")) {
            Variables.getInstance().getLogger().warning(
                    "[sRandomRTP] teleport.yml is missing or has no 'teleport' section — some features may not work correctly.");
        }
        if (Variables.getInstance().getConfig() == null) {
            Variables.getInstance().getLogger().warning(
                    "[sRandomRTP] config.yml failed to reload — plugin may behave incorrectly.");
        }
    }

    private static void refreshPortalSettings() {
        try {
            RuntimeStateRegistry state = Variables.getRuntimeState();
            for (PortalDataTasks portalData : state.getPlayerPortalsTasks().values()) {

                if (portalData.getParticlesTask() != null) {
                    portalData.getParticlesTask().cancel();
                }

                if (portalData.getTaskType().contains("particles")) {
                    WrappedTask newParticlesTask = Variables.getFoliaLib().getImpl().runTimerAsync(
                            () -> CommandSetPortal.spawnParticles(portalData.getCenter(), portalData.getRadius(), portalData.getShape()),
                            portalData.getDelay(),
                            portalData.getPeriod()
                    );

                    portalData.setParticlesTask(newParticlesTask);
                }
            }
        } catch (RuntimeException e) {
            Variables.getInstance().getLogger().warning("[sRandomRTP] Error refreshing portal settings: " + e.getMessage());
        }
    }
}
