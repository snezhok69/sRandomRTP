package org.sRandomRTP;

import com.tcoded.folialib.FoliaLib;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.sRandomRTP.BlockBiomes.LoadBlockList;
import org.sRandomRTP.Checkings.AutoCheckingVersion;
import org.sRandomRTP.Checkings.CheckingInstalledPlaceHolderAPI;
import org.sRandomRTP.Checkings.CheckingServerVersion;
import org.sRandomRTP.Checkings.StartPluginCheckingNewVersion;
import org.sRandomRTP.Commands.CommandArgs;
import org.sRandomRTP.Commands.onTabCompletes;
import org.sRandomRTP.Data.DataLoad;
import org.sRandomRTP.DifferentMethods.*;
import org.sRandomRTP.Events.PlayerDamage;
import org.sRandomRTP.Events.PlayerMouseMove;
import org.sRandomRTP.Events.PlayerMove;
import org.sRandomRTP.Events.PlayerParticles;
import org.sRandomRTP.Files.*;
import org.sRandomRTP.Metrics.Metrics;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main extends JavaPlugin {

    @Override
    public void onEnable() {
        try {
            //
            Variables.instance = this;
            Variables.foliaLib = new FoliaLib(this);
            long startTime = System.currentTimeMillis();
            Bukkit.getConsoleSender().sendMessage("");
            Bukkit.getConsoleSender().sendMessage(Variables.pluginName + " §8- §c>==========================================<");
            Bukkit.getConsoleSender().sendMessage("");
            Bukkit.getConsoleSender().sendMessage(Variables.pluginName + " §8- §ePlugin initialization started...");
            //
            Bukkit.getConsoleSender().sendMessage(Variables.pluginName + " §8- §eVersion check...");
            if (CheckingServerVersion.checkingServerVersion()) {
                return;
            }
            //
            if (Bukkit.getServer().getName().equalsIgnoreCase("Folia")) {
                Variables.pluginToggle = true;
                Bukkit.getConsoleSender().sendMessage(Variables.pluginName + " §8- §cFolia core is not supported, plugin is disabled!");
                Bukkit.getPluginManager().disablePlugin(this);
                return;
            } else {
            }
            //
            Bukkit.getConsoleSender().sendMessage(Variables.pluginName + " §8- §eChecking installed PlaceHolderAPI...");
            if (CheckingInstalledPlaceHolderAPI.checkingInstalledPlaceHolderAPI()) {
            }
            //
            Bukkit.getConsoleSender().sendMessage(Variables.pluginName + " §8- §eLoading data...");
            DataLoad.dataLoad();
            //
            LoadFiles.loadFiles();
            //
            Bukkit.getConsoleSender().sendMessage(Variables.pluginName + " §8- §eLoading events...");
            getServer().getPluginManager().registerEvents(new PlayerParticles(), this);
            getServer().getPluginManager().registerEvents(new PlayerDamage(), this);
            getServer().getPluginManager().registerEvents(new PlayerMove(), this);
            getServer().getPluginManager().registerEvents(new PlayerMouseMove(), this);
            //
            Bukkit.getConsoleSender().sendMessage(Variables.pluginName + " §8- §eCreating files...");
            FilesCreate filesCreate = new FilesCreate();
            List<String> createdFiles = filesCreate.filesCreate();
            long createTime = System.currentTimeMillis();
            for (String message : createdFiles) {
                if (message != null) {
                    long elapsedTime = System.currentTimeMillis() - createTime;
                    String formattedLine = String.format(Variables.pluginName + " §8- §aFile %s successfully created §6(%d ms)", message, elapsedTime);
                    Bukkit.getConsoleSender().sendMessage(formattedLine);
                }
            }
            //
            Bukkit.getConsoleSender().sendMessage(Variables.pluginName + " §8- §eUpdating files...");
            FilesUpdate filesUpdate = new FilesUpdate();
            List<String> filesUpdates = filesUpdate.filesUpdate();
            long updateTime = System.currentTimeMillis();
            for (String message : filesUpdates) {
                long elapsedTime = System.currentTimeMillis() - updateTime;
                String formattedLine = String.format(Variables.pluginName + " §8- §aFile %s successfully updated §6(%d ms)", message, elapsedTime);
                Bukkit.getConsoleSender().sendMessage(formattedLine);
            }
            //
            File configFile = new File(getDataFolder(), "config.yml");
            FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
            LoadKeys.loadKeys(config);
            //
            CheckingFile checkingFile = new CheckingFile();
            checkingFile.compareLanguageFiles("ar_sa.yml", "ru_ru.yml");
            checkingFile.compareLanguageFiles("custom_messages.yml", "ru_ru.yml");
            checkingFile.compareLanguageFiles("de_de.yml", "ru_ru.yml");
            checkingFile.compareLanguageFiles("en_us.yml", "ru_ru.yml");
            checkingFile.compareLanguageFiles("es_es.yml", "ru_ru.yml");
            checkingFile.compareLanguageFiles("fr_fr.yml", "ru_ru.yml");
            checkingFile.compareLanguageFiles("it_it.yml", "ru_ru.yml");
            checkingFile.compareLanguageFiles("ja_jp.yml", "ru_ru.yml");
            checkingFile.compareLanguageFiles("ko_kr.yml", "ru_ru.yml");
            checkingFile.compareLanguageFiles("pl_pl.yml", "ru_ru.yml");
            checkingFile.compareLanguageFiles("pt_br.yml", "ru_ru.yml");
            checkingFile.compareLanguageFiles("uk_ua.yml", "ru_ru.yml");
            checkingFile.compareLanguageFiles("vi_vn.yml", "ru_ru.yml");
            checkingFile.compareLanguageFiles("zh_cn.yml", "ru_ru.yml");
            checkingFile.compareLanguageFiles("tr_tr.yml", "ru_ru.yml");
            //
            LoadLanguageFile loadLanguageFile = new LoadLanguageFile();
            loadLanguageFile.loadLanguageFile();
            YamlConfiguration langFile = loadLanguageFile.getLangFile();
            LoadMessages.loadMessages(langFile);
            //
            LoadBlockList.loadBlockList();
            Bukkit.getConsoleSender().sendMessage(Variables.pluginName + " §8- §eLoading commands...");
            Map<String, Map<String, Object>> commands = getDescription().getCommands();
            if (commands != null) {
                for (String commandName : commands.keySet()) {
                    getCommand(commandName).setExecutor(new CommandArgs());
                    getCommand(commandName).setTabCompleter(new onTabCompletes());
                }
            }
            Bukkit.getConsoleSender().sendMessage(Variables.pluginName + " §8- §eRunning tasks...");
            StartPluginCheckingNewVersion.startPluginCheckingNewVersion();
            IsOutdatedByMultipleVersionsTask.isOutdatedByMultipleVersionsTask();
            AutoCheckingVersion.autoCheckingVersion();
            //
            Bukkit.getConsoleSender().sendMessage(Variables.pluginName + " §8- §eSending anonymous statistics...");
            try {
                Metrics metrics = new Metrics(this, 21603);
                metrics.addCustomChart(new Metrics.DrilldownPie("lang", () -> {
                    Map<String, Map<String, Integer>> map = new HashMap<>();
                    String language = Variables.getInstance().getConfig().getString("Language");
                    if (language != null && !language.trim().isEmpty()) {
                        Map<String, Integer> entry = new HashMap<>();
                        entry.put(language, 1);
                        map.put(language, entry);
                    } else {
                    }
                    return map;
                }));
            } catch (Throwable e) {
                StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
                String callingClassName = stackTrace[2].getClassName();
                LoggerUtility.loggerUtility(callingClassName, e);
            }
            //
            for (String line : LoadMessages.PluginEnabledMessage) {
                String serverVersions = Bukkit.getServer().getVersion();
                String osName = System.getProperty("os.name");
                String osVersion = System.getProperty("os.version");
                String osArch = System.getProperty("os.arch");
                String javaVersion = System.getProperty("java.version");
                long endTime = System.currentTimeMillis();
                long enabledPluginTime = endTime - startTime;
                line = line.replace("%mc%", enabledPluginTime + "")
                        .replace("%server-version%", serverVersions)
                        .replace("%os-version%", osName + " " + osVersion + " (" + osArch + ")")
                        .replace("%java-version-server%", javaVersion);
                String formattedLine = TranslateRGBColors.translateRGBColors(line);
                Bukkit.getConsoleSender().sendMessage(formattedLine);
            }
            Bukkit.getConsoleSender().sendMessage(Variables.pluginName + " §8- §c>==========================================<");
            Bukkit.getConsoleSender().sendMessage("");
            //
        } catch (Throwable e) {
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            String callingClassName = stackTrace[2].getClassName();
            LoggerUtility.loggerUtility(callingClassName, e);
        }
    }

    @Override
    public void onDisable() {
        if (!Variables.pluginToggle) {
            try {
                for (String line : LoadMessages.PluginDisabledMessage) {
                    long endTime = System.currentTimeMillis();
                    long startTime = System.currentTimeMillis();
                    long disabledPluginTime = endTime - startTime;
                    line = line.replace("%mc%", disabledPluginTime + "");
                    String formattedLines = TranslateRGBColors.translateRGBColors(line);
                    Bukkit.getConsoleSender().sendMessage(formattedLines);
                }
                RemoveAllBossBars.removeAllBossBars();
            } catch (Throwable e) {
            }
        }
    }
}




