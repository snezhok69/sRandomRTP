package org.sRandomRTP.Commands;

import com.tcoded.folialib.wrapper.task.WrappedTask;
import org.bukkit.command.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.sRandomRTP.BlockBiomes.LoadBlockList;
import org.sRandomRTP.DifferentMethods.*;
import org.sRandomRTP.Files.LoadFiles;
import org.sRandomRTP.Files.LoadKeys;
import org.sRandomRTP.Files.LoadMessages;
import org.sRandomRTP.Checkings.AutoCheckingVersion;
import java.io.File;
import java.util.*;

public class CommandReload {
    public static boolean isReloaded = false;

    public static void commandReload(CommandSender sender) {
        try {
            long startTime = System.currentTimeMillis();
            if (!sender.hasPermission("sRandomRTP.Command.Reload")) {
                List<String> formattedMessage = LoadMessages.nopermissionreload;
                for (String line : formattedMessage) {
                    String formattedLine = TranslateRGBColors.translateRGBColors(line);
                    sender.sendMessage(formattedLine);
                }
                return;
            }
            //
            Variables.getInstance().reloadConfig();
            LoadFiles.loadFiles();
            //
            File configFile = new File(Variables.getInstance().getDataFolder(), "config.yml");
            FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
            LoadKeys.loadKeys(config);
            LoadLanguageFile loadLanguageFile = new LoadLanguageFile();
            loadLanguageFile.loadLanguageFile();
            YamlConfiguration langFile = loadLanguageFile.getLangFile();
            LoadMessages.loadMessages(langFile);
            //
            if (isReloaded) {
                List<String> formattedMessage = LoadMessages.reloadingwait;
                for (String line : formattedMessage) {
                    String formattedLine = TranslateRGBColors.translateRGBColors(line);
                    sender.sendMessage(formattedLine);
                }
                return;
            }
            List<String> formattedMessage = LoadMessages.reloadingstart;
            for (String line : formattedMessage) {
                String formattedLine = TranslateRGBColors.translateRGBColors(line);
                sender.sendMessage(formattedLine);
            }
            final int[] step = {0};
            WrappedTask task = Variables.getFoliaLib().getImpl().runTimerAsync(() -> {
                try {
                        switch (step[0]) {
                            case 0:
                                loadLanguageFile.loadLanguageFile();
                                LoadMessages.loadMessages(langFile);
                                break;
                            case 1:
                                Variables.getInstance().reloadConfig();
                                LoadFiles.loadFiles();
                                //
                                LoadKeys.loadKeys(config);
                                LoadBlockList.loadBlockList();
                                //
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
                                }
                                isReloaded = false;
                                return;
                        }
                        step[0]++;
                } catch (Throwable e) {
                    StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
                    String callingClassName = stackTrace[2].getClassName();
                    LoggerUtility.loggerUtility(callingClassName, e);
                }
            }, 1L, 1L);
            isReloaded = true;
            Variables.commandReloadTask = task;
        } catch (Throwable e) {
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            String callingClassName = stackTrace[2].getClassName();
            LoggerUtility.loggerUtility(callingClassName, e);
        }
    }
}