package org.sRandomRTP.Commands;

import com.tcoded.folialib.wrapper.task.WrappedTask;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.sRandomRTP.BlockBiomes.LoadBlockList;
import org.sRandomRTP.DifferentMethods.*;
import org.sRandomRTP.DifferentMethods.Text.LoadLanguageFile;
import org.sRandomRTP.DifferentMethods.Text.TranslateRGBColors;
import org.sRandomRTP.Files.*;
import org.sRandomRTP.Checkings.AutoCheckingVersion;
import org.sRandomRTP.DataPortals.PortalDataTasks;

import java.io.File;
import java.util.*;

public class CommandReload {
    public static boolean isReloaded = false;

    public static void commandReload(CommandSender sender) {
        long startTime = System.currentTimeMillis();
        if (!sender.hasPermission("sRandomRTP.Command.Reload")) {
            List<String> formattedMessage = LoadMessages.nopermissionreload;
            for (String line : formattedMessage) {
                String formattedLine = TranslateRGBColors.translateRGBColors(line);
                sender.sendMessage(formattedLine);
            }
            return;
        }

        if (isReloaded) {
            List<String> formattedMessage = LoadMessages.reloadingwait;
            for (String line : formattedMessage) {
                String formattedLine = TranslateRGBColors.translateRGBColors(line);
                sender.sendMessage(formattedLine);
            }
            return;
        }

        try {
            Variables.getInstance().reloadConfig();
            LoadFiles.loadFiles();

            Bukkit.getConsoleSender().sendMessage(Variables.pluginName + " §8- §eПроверка файлов...");
            FilesCreate filesCreate = new FilesCreate();
            List<String> createdFiles = filesCreate.filesCreate();
            long createTime = System.currentTimeMillis();
            for (String message : createdFiles) {
                if (message != null) {
                    long elapsedTime = System.currentTimeMillis() - createTime;
                    String formattedLine = String.format(Variables.pluginName + " §8- §aФайл %s успешно создан §6(%d мс)", message, elapsedTime);
                    Bukkit.getConsoleSender().sendMessage(formattedLine);
                }
            }

            Bukkit.getConsoleSender().sendMessage(Variables.pluginName + " §8- §eОбновление файлов...");
            FilesUpdate filesUpdate = new FilesUpdate();
            List<String> filesUpdates = filesUpdate.filesUpdate();
            long updateTime = System.currentTimeMillis();
            for (String message : filesUpdates) {
                long elapsedTime = System.currentTimeMillis() - updateTime;
                String formattedLine = String.format(Variables.pluginName + " §8- §aФайл %s успешно обновлен §6(%d мс)", message, elapsedTime);
                Bukkit.getConsoleSender().sendMessage(formattedLine);
            }

            File configFile = new File(Variables.getInstance().getDataFolder(), "config.yml");
            FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
            LoadKeys.loadKeys(config);
            LoadLanguageFile loadLanguageFile = new LoadLanguageFile();
            loadLanguageFile.loadLanguageFile();
            YamlConfiguration langFile = loadLanguageFile.getLangFile();
            LoadMessages.loadMessages(langFile);

            List<String> formattedMessage = LoadMessages.reloadingstart;
            for (String line : formattedMessage) {
                String formattedLine = TranslateRGBColors.translateRGBColors(line);
                sender.sendMessage(formattedLine);
            }

            isReloaded = true;
            final int[] step = {0};
            WrappedTask task = Variables.getFoliaLib().getImpl().runTimerAsync(() -> {
                try {
                    switch (step[0]) {
                        case 0:
                            loadLanguageFile.loadLanguageFile();
                            YamlConfiguration reloadedLang = loadLanguageFile.getLangFile();
                            LoadMessages.loadMessages(reloadedLang);
                            break;
                        case 1:
                            Variables.getInstance().reloadConfig();
                            LoadFiles.loadFiles();
                            LoadKeys.loadKeys(Variables.getInstance().getConfig());
                            LoadBlockList.loadBlockList();
                            refreshPortalSettings();
                            break;
                        case 2:
                            if (Variables.autoCheckVersionTask != null) {
                                Variables.autoCheckVersionTask.cancel();
                            }
                            AutoCheckingVersion.autoCheckingVersion();
                            break;
                        case 3:
                            List<String> formattedMessage2 = LoadMessages.successfullyreload;
                            for (String line : formattedMessage2) {
                                long endTime = System.currentTimeMillis();
                                long reloadPluginTime = endTime - startTime;
                                line = line.replace("%mc%", reloadPluginTime + "");
                                String formattedLine = TranslateRGBColors.translateRGBColors(line);
                                sender.sendMessage(formattedLine);
                            }
                            if (Variables.commandReloadTask != null) {
                                Variables.commandReloadTask.cancel();
                                Variables.commandReloadTask = null;
                            }
                            isReloaded = false;
                            return;
                    }
                    step[0]++;
                } catch (Throwable e) {
                    StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
                    String callingClassName = stackTrace[2].getClassName();
                    LoggerUtility.loggerUtility(callingClassName, e);
                    if (Variables.commandReloadTask != null) {
                        Variables.commandReloadTask.cancel();
                        Variables.commandReloadTask = null;
                    }
                    isReloaded = false;
                }
            }, 1L, 1L);
            Variables.commandReloadTask = task;
        } catch (Throwable e) {
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            String callingClassName = stackTrace[2].getClassName();
            LoggerUtility.loggerUtility(callingClassName, e);
            if (Variables.commandReloadTask != null) {
                Variables.commandReloadTask.cancel();
                Variables.commandReloadTask = null;
            }
            isReloaded = false;
        }
    }

    private static void refreshPortalSettings() {
        try {
            for (Map.Entry<String, PortalDataTasks> entry : Variables.playerPortalsTasks.entrySet()) {
                PortalDataTasks portalData = entry.getValue();

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
        } catch (Exception e) {
            Variables.getInstance().getLogger().warning("[sRandomRTP] Error refreshing portal settings: " + e.getMessage());
        }
    }
}