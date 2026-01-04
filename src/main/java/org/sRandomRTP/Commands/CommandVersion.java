package org.sRandomRTP.Commands;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.plugin.PluginDescriptionFile;
import org.sRandomRTP.DifferentMethods.LoggerUtility;
import org.sRandomRTP.DifferentMethods.TranslateRGBColors;
import org.sRandomRTP.DifferentMethods.Variables;
import org.sRandomRTP.Files.LoadMessages;

public class CommandVersion {
    public static void commandVersion(CommandSender sender) {
        try {
            if (!sender.hasPermission("sRandomRTP.Сommand.Version")) {
                List<String> formattedMessage = LoadMessages.nopermissioncommand;
                for (String line : formattedMessage) {
                    String formattedLine = TranslateRGBColors.translateRGBColors(ChatColor.translateAlternateColorCodes('&', line));
                    sender.sendMessage(formattedLine);
                }
                return;
            }

            List<String> formattedMessage = LoadMessages.CheckingVersion;
            for (String line : formattedMessage) {
                line = TranslateRGBColors.translateRGBColors(line);
                String formattedLine = net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('&', line);
                sender.sendMessage(formattedLine);
            }

            CompletableFuture.runAsync(() -> {
                try {
                    URL url = new URL("https://gitlab.com/snezh0k69/sRandomRTP/-/raw/main/VERSION");
                    Variables.connection = (HttpURLConnection) url.openConnection();
                    Variables.connection.setConnectTimeout(10000);
                    Variables.connection.setRequestMethod("GET");
                    int responseCode = Variables.connection.getResponseCode();

                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
                        String latestVersion = reader.readLine();
                        reader.close();

                        if (latestVersion != null) {
                            latestVersion = latestVersion.trim();
                            PluginDescriptionFile description = Variables.getInstance().getDescription();
                            String pluginVersion = description.getVersion();

                            if (pluginVersion.equals(latestVersion)) {
                                List<String> formattedMessage2 = LoadMessages.LatestVersionMessage;
                                for (String line : formattedMessage2) {
                                    line = line.replace("%latest-CommandVersion%", latestVersion);
                                    line = TranslateRGBColors.translateRGBColors(line);
                                    String formattedLine = net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('&', line);
                                    sender.sendMessage(formattedLine);
                                }
                            } else {
                                List<String> formattedMessage3 = LoadMessages.newVersionMessage;
                                for (String line : formattedMessage3) {
                                    line = line.replace("%new-CommandVersion%", latestVersion);
                                    line = line.replace("%old-CommandVersion%", pluginVersion);
                                    line = TranslateRGBColors.translateRGBColors(line);
                                    String formattedLine = net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('&', line);
                                    sender.sendMessage(formattedLine);
                                }
                            }
                        }
                    }
                } catch (IOException e) {
                    List<String> formattedMessage6 = LoadMessages.ErrorCheckingVersionMessage;
                    for (String line : formattedMessage6) {
                        line = TranslateRGBColors.translateRGBColors(line);
                        line = line.replace("%error%", e.getMessage());
                        String formattedLine = net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('&', line);
                        sender.sendMessage(formattedLine);
                    }
                }
            });
        } catch (Throwable e) {
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            String callingClassName = stackTrace[2].getClassName();
            LoggerUtility.loggerUtility(callingClassName, e);
        }
    }
}