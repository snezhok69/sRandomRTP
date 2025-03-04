package org.sRandomRTP.Checkings;

import com.tcoded.folialib.wrapper.task.WrappedTask;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginDescriptionFile;
import org.sRandomRTP.DifferentMethods.LoggerUtility;
import org.sRandomRTP.DifferentMethods.TranslateRGBColors;
import org.sRandomRTP.DifferentMethods.Variables;
import org.sRandomRTP.Files.LoadMessages;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class AutoCheckingVersion {
    public static void autoCheckingVersion() {
        try {
            boolean autoCheckingPlayersEnabled = Variables.getInstance().getConfig().getBoolean("Auto-Checking-New-Version-Players-Enabled");
            boolean autoCheckingConsoleEnabled = Variables.getInstance().getConfig().getBoolean("Auto-Checking-New-Version-Console-Enabled");
            //
            if (autoCheckingPlayersEnabled || autoCheckingConsoleEnabled) {
                int seconds = Variables.getInstance().getConfig().getInt("Period-Checking-New-Version");
                int periodInTicks = seconds * 20;
                //
                WrappedTask task = Variables.getFoliaLib().getImpl().runTimerAsync(() -> {
                    try {
                        URL url = new URL("https://gitlab.com/snezh0k69/sRandomRTP/-/raw/main/VERSION");
                        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                        connection.setRequestMethod("GET");
                        int responseCode = connection.getResponseCode();
                        if (responseCode == HttpURLConnection.HTTP_OK) {
                            BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
                            String latestVersion = reader.readLine();
                            reader.close();
                            if (latestVersion != null) {
                                latestVersion = latestVersion.trim();
                                PluginDescriptionFile description = Variables.getInstance().getDescription();
                                String pluginVersion = description.getVersion();
                                if (!latestVersion.equals(pluginVersion)) {
                                    List<String> formattedMessage = (LoadMessages.newVersionMessage);
                                    for (String line : formattedMessage) {
                                        line = line.replace("%new-CommandVersion%", latestVersion);
                                        line = line.replace("%old-CommandVersion%", pluginVersion);
                                        line = TranslateRGBColors.translateRGBColors(line);
                                        String formattedLine = net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('&', line);
                                        if (Variables.getInstance().getConfig().getBoolean("Auto-Checking-New-Version-Console-Enabled")) {
                                            Bukkit.getConsoleSender().sendMessage(formattedLine);
                                        }
                                        if (Variables.getInstance().getConfig().getBoolean("Auto-Checking-New-Version-Players-Enabled")) {
                                            for (Player player : Bukkit.getOnlinePlayers()) {
                                                if (player.hasPermission("CommandRtp.CommandVersion-check")) {
                                                    player.sendMessage(formattedLine);
                                                } else {
                                                    player.sendMessage(formattedLine);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } catch (IOException e) {
                        Bukkit.getConsoleSender().sendMessage("§b[sRandomRTP] §8- §cError when checking new plugin CommandVersion: §6" + e.getMessage());
                    }
                }, periodInTicks, periodInTicks);
                Variables.autoCheckVersionTask = task;
            } else {
            }
        } catch (Throwable e) {
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            String callingClassName = stackTrace[2].getClassName();
            LoggerUtility.loggerUtility(callingClassName, e);
        }
    }
}
