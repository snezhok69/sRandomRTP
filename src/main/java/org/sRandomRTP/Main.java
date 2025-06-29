package org.sRandomRTP;

import com.tcoded.folialib.FoliaLib;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.sRandomRTP.BlockBiomes.IsBiomeBanned;
import org.sRandomRTP.BlockBiomes.IsBlockBanned;
import org.sRandomRTP.BlockBiomes.LoadBlockList;
import org.sRandomRTP.Checkings.*;
import org.sRandomRTP.Commands.CommandArgs;
import org.sRandomRTP.Commands.onTabCompletes;
import org.sRandomRTP.Data.DataLoad;
import org.sRandomRTP.DataPortals.LoadPortalsPlayerFromDatabaseSQL;
import org.sRandomRTP.DataPortals.SQLManagerPortals;
import org.sRandomRTP.DifferentMethods.*;
import org.sRandomRTP.DifferentMethods.BossBars.RemoveAllBossBars;
import org.sRandomRTP.DifferentMethods.IsIn.IsOutdatedByMultipleVersionsTask;
import org.sRandomRTP.DifferentMethods.Text.LoadLanguageFile;
import org.sRandomRTP.DifferentMethods.Text.TranslateRGBColors;
import org.sRandomRTP.Events.*;
import org.sRandomRTP.Files.*;
import org.sRandomRTP.Metrics.Metrics;
import org.sRandomRTP.api.RandomRTPAPI;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class Main extends JavaPlugin implements RandomRTPAPI {

    @Override
    public void onEnable() {
        try {
            //
            Variables.instance = this;
            Variables.foliaLib = new FoliaLib(this);
            //
            try {
                Class.forName("org.sqlite.JDBC");
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            try {
                SQLManagerPortals.openConnectionSQL().get();
                SQLManagerPortals.createTableSQL().get();
                //
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
            //
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
            Metrics metrics = new Metrics(this, 21603);
            if (Bukkit.getServer().getName().equalsIgnoreCase("Folia")) {
                Bukkit.getConsoleSender().sendMessage(Variables.pluginName + " §8- §cUsed Folia there may be errors or bugs!...");
                LoadFiles.loadFiles();
                metrics.addCustomChart(new Metrics.SimplePie("using_folia", () -> {
                    return ("Yes");
                }));
            } else {
                LoadFiles.loadFiles();
                metrics.addCustomChart(new Metrics.SimplePie("using_folia", () -> {
                    return ("No");
                }));
            }
            
            //
            Bukkit.getConsoleSender().sendMessage(Variables.pluginName + " §8- §eChecking installed PlaceHolderAPI...");
            if (CheckingInstalledPlaceHolderAPI.checkingInstalledPlaceHolderAPI()) {
            }
            Bukkit.getConsoleSender().sendMessage(Variables.pluginName + " §8- §eChecking installed Chunky...");
            if (CheckingInstalledChunky.сheckingInstalledсhunky()) {
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
            getServer().getPluginManager().registerEvents(new PlayerBreak(), this);
            getServer().getPluginManager().registerEvents(new PlayerMouseMove(), this);
            getServer().getPluginManager().registerEvents(new PlayerBreakBlockPortal(), this);
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
            Variables.setupEconomy();
            
            // Инициализация фильтра консоли для блокировки сообщений "moved too quickly"
            boolean disableMovedTooQuicklyMessages = config.getBoolean("Disable-Moved-Too-Quickly-Messages", true);
            if (disableMovedTooQuicklyMessages) {
                Bukkit.getConsoleSender().sendMessage(Variables.pluginName + " §8- §eInitializing a console filter to block fast move messages...");
                ConsoleFilter.registerFilter(true);
                Bukkit.getConsoleSender().sendMessage(Variables.pluginName + " §8- §aThe console filter has been successfully initialized");
            }
            
            //
            //CheckingFile checkingFile = new CheckingFile();
            //checkingFile.compareLanguageFiles("ar.yml", "ru.yml");
            //checkingFile.compareLanguageFiles("custom_messages.yml", "ru.yml");
            //checkingFile.compareLanguageFiles("de.yml", "ru.yml");
            //checkingFile.compareLanguageFiles("en.yml", "ru.yml");
            //checkingFile.compareLanguageFiles("es.yml", "ru.yml");
            //checkingFile.compareLanguageFiles("fr.yml", "ru.yml");
            //checkingFile.compareLanguageFiles("it.yml", "ru.yml");
            //checkingFile.compareLanguageFiles("ja.yml", "ru.yml");
            //checkingFile.compareLanguageFiles("ko.yml", "ru.yml");
            //checkingFile.compareLanguageFiles("pl.yml", "ru.yml");
            //checkingFile.compareLanguageFiles("pt.yml", "ru.yml");
            //checkingFile.compareLanguageFiles("ua.yml", "ru.yml");
            //checkingFile.compareLanguageFiles("vi.yml", "ru.yml");
            //checkingFile.compareLanguageFiles("zh.yml", "ru.yml");
            //checkingFile.compareLanguageFiles("tr.yml", "ru.yml");
            //
            LoadLanguageFile loadLanguageFile = new LoadLanguageFile();
            loadLanguageFile.loadLanguageFile();
            YamlConfiguration langFile = loadLanguageFile.getLangFile();
            LoadMessages.loadMessages(langFile);
            LoadPortalsPlayerFromDatabaseSQL.loadPortalTasksFromDatabaseSQL();
            LoadPortalsPlayerFromDatabaseSQL.loadPortalsPlayerFromDatabaseSQL();
            LoadPortalsPlayerFromDatabaseSQL.loadPortalBlocksPlayerToDatabaseSQL();
            //
            Bukkit.getConsoleSender().sendMessage(Variables.pluginName + " §8- §eUploading lists of banned blocks and biomes...");
            LoadBlockList.loadBlockListAsync(this).thenRun(() -> {
                Bukkit.getConsoleSender().sendMessage(Variables.pluginName + " §8- §eThe lists of banned blocks and biomes have been successfully uploaded");
            }).exceptionally(ex -> {
                Bukkit.getConsoleSender().sendMessage(Variables.pluginName + " §8- §cError when loading lists: " + ex.getMessage());
                return null;
            });
            //
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
            Variables.initializePlugin(this);
            //
            Bukkit.getConsoleSender().sendMessage(Variables.pluginName + " §8- §eSending anonymous statistics...");
            try {
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
                // Удаляем фильтр консоли при выключении плагина
                ConsoleFilter.removeFilter();
                
                for (String line : LoadMessages.PluginDisabledMessage) {
                    long endTime = System.currentTimeMillis();
                    long startTime = System.currentTimeMillis();
                    long disabledPluginTime = endTime - startTime;
                    line = line.replace("%mc%", disabledPluginTime + "");
                    String formattedLines = TranslateRGBColors.translateRGBColors(line);
                    Bukkit.getConsoleSender().sendMessage(formattedLines);
                }
                RemoveAllBossBars.removeAllBossBars();
                SQLManagerPortals.closeConnectionMYSQL().thenRun(() -> {
                }).exceptionally(ex -> {
                    ex.printStackTrace();
                    return null;
                });
            } catch (Throwable e) {
            }
        }
    }
    
    // Реализация методов API
    
    @Override
    public boolean isAPIWorking() {
        return true;
    }
    
    @Override
    public List<String> getBannedBiomes() {
        if (Variables.teleportfile == null) {
            return new ArrayList<>();
        }
        return Variables.teleportfile.getStringList("teleport.bannedBiomes");
    }
    
    @Override
    public boolean isBiomeBanned(Biome biome) {
        return IsBiomeBanned.isBiomeBanned(biome);
    }
    
    @Override
    public List<Material> getBannedBlocks() {
        if (Variables.blockList == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(Variables.blockList);
    }
    
    @Override
    public boolean isBlockBanned(Material material) {
        return IsBlockBanned.isBlockBanned(material);
    }
    
    @Override
    public String getPluginVersion() {
        return getDescription().getVersion();
    }
}




