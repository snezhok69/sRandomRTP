package org.sRandomRTP.DifferentMethods;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.sRandomRTP.Commands.ConfiguredCommandAliases;
import org.sRandomRTP.DifferentMethods.Text.LoadLanguageFile;
import org.sRandomRTP.DifferentMethods.Teleport.FoliaSchedulerFacade;
import org.sRandomRTP.Services.ConfigCache;
import org.sRandomRTP.Utils.ChatUtils;
import org.sRandomRTP.Files.LoadKeys;
import org.sRandomRTP.Files.LoadMessages;

import org.bukkit.configuration.InvalidConfigurationException;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;


public class FilesAutoReload {

    public static List<WatchKey> registeredKeys = new CopyOnWriteArrayList<>();
    private static WatchService watchService;
    private static Thread watchThread;
    private static volatile boolean running = false;

    public static synchronized void startFilesAutoReload() {
        stopFilesAutoReload();
        try {
            String dataFolderPath = Variables.getInstance().getDataFolder().toString();
            watchService = FileSystems.getDefault().newWatchService();
            registeredKeys.clear();
            List<Path> directoriesToWatch = Arrays.asList(
                    Paths.get(dataFolderPath),
                    Paths.get(dataFolderPath, "Settings"),
                    Paths.get(dataFolderPath, "lang")
            );
            for (Path path : directoriesToWatch) {
                if (Files.notExists(path)) {
                    Bukkit.getConsoleSender().sendMessage(ChatUtils.PLUGIN_NAME + " §8- §cDirectory not found for monitoring: §e" + path);
                    continue;
                }
                try {
                    WatchKey key = path.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
                    registeredKeys.add(key);
                } catch (IOException e) {
                    Bukkit.getConsoleSender().sendMessage(ChatUtils.PLUGIN_NAME + " §8- §cFailed to register path: §e" + path);
                }
            }
            running = true;
            watchThread = new Thread(() -> {
                while (running) {
                    try {
                        WatchKey key = watchService.take();
                        for (WatchEvent<?> event : key.pollEvents()) {
                            if (event.kind() == StandardWatchEventKinds.ENTRY_MODIFY) {
                                Path filePath = (Path) event.context();
                                if (filePath.toString().endsWith(".yml")) {
                                    reloadFile(filePath);
                                }
                            }
                        }
                        if (!key.reset()) {
                            registeredKeys.remove(key);
                            if (registeredKeys.isEmpty()) {
                                break;
                            }
                        }
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                    } catch (ClosedWatchServiceException ignored) {
                        break;
                    }
                }
            }, "sRandomRTP-FileWatcher");
            watchThread.setDaemon(true);
            watchThread.start();
        } catch (IOException e) {
            Bukkit.getConsoleSender().sendMessage(ChatUtils.PLUGIN_NAME + " §8- §cFailed to initialize file monitor: §e" + e.getMessage());
            stopFilesAutoReload();
        }
    }

    public static synchronized void stopFilesAutoReload() {
        running = false;
        for (WatchKey key : registeredKeys) {
            key.cancel();
        }
        registeredKeys.clear();
        if (watchThread != null) {
            Thread threadToJoin = watchThread;
            watchThread = null;
            threadToJoin.interrupt();
            try {
                threadToJoin.join(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        if (watchService != null) {
            try {
                watchService.close();
            } catch (IOException e) {
                Bukkit.getConsoleSender().sendMessage(ChatUtils.PLUGIN_NAME + " §8- §cFailed to close file monitor: §e" + e.getMessage());
            }
            watchService = null;
        }
    }

    private static final Set<String> LANG_FILE_NAMES = new HashSet<>(Arrays.asList(
            "en.yml", "ru.yml", "es.yml", "de.yml", "fr.yml", "it.yml",
            "pt.yml", "zh.yml", "ja.yml", "ko.yml", "ar.yml", "pl.yml",
            "vi.yml", "ua.yml", "tr.yml", "custom_messages.yml"
    ));

    // Built once at class-load time — avoids HashMap + lambda allocation on every file-change event.
    private static final Map<String, Consumer<Path>> RELOAD_ACTIONS;
    static {
        Map<String, Consumer<Path>> map = new HashMap<>();
        map.put("config.yml", p -> {
            Variables.getInstance().reloadConfig();
            reloadRegistryAndCache();
            LoadKeys.loadKeys(Variables.getInstance().getConfig());
            LoadLanguageFile loadLanguageFile = new LoadLanguageFile();
            loadLanguageFile.loadLanguageFile();
            LoadMessages.loadMessages(loadLanguageFile.getLangFile());
            if (Variables.getPluginContext() != null) {
                Variables.getReleaseCheckService().restartAutoChecks();
            }
            FoliaSchedulerFacade.runLater(() -> ConfiguredCommandAliases.apply(Variables.getInstance()), 1L);
        });
        Consumer<Path> standardReload = p -> {
            reloadRegistryAndCache();
        };
        for (String name : Arrays.asList(
                "teleport.yml", "sound.yml", "bossbar.yml", "near.yml", "title.yml",
                "economy.yml", "effects.yml", "particles.yml", "far.yml", "middle.yml",
                "biome.yml", "portal.yml", "chunk-loading.yml", "commands.yml")) {
            map.put(name, standardReload);
        }
        map.put("admin-bars.yml", p -> {
            if (Variables.getPluginContext() != null) {
                Variables.getPluginContext().getConfigRegistry().reload();
            }
        });
        RELOAD_ACTIONS = Collections.unmodifiableMap(map);
    }

    private static void reloadRegistryAndCache() {
        if (Variables.getPluginContext() == null) {
            return;
        }
        Variables.getPluginContext().getConfigRegistry().reload();
        Variables.configCache = ConfigCache.buildFrom(
                Variables.getPluginContext().getConfigRegistry(),
                Variables.getInstance() != null ? Variables.getInstance().getConfig() : null);
    }

    private static void reloadFile(Path filePath) {
        try {
            String fileName = filePath.getFileName().toString();
            String dataFolderPath = Variables.getInstance().getDataFolder().toString();

            Path fullPath;
            if (LANG_FILE_NAMES.contains(fileName)) {
                fullPath = Paths.get(dataFolderPath, "lang", fileName);
            } else if (fileName.equals("config.yml")) {
                fullPath = Paths.get(dataFolderPath, fileName);
            } else {
                fullPath = Paths.get(dataFolderPath, "Settings", fileName);
            }

            // Guard: validate YAML before dispatching the reload action.
            // YamlConfiguration.loadConfiguration() silently returns an empty config on
            // parse errors, which would replace valid in-memory state with an empty one.
            if (Files.exists(fullPath)) {
                YamlConfiguration test = new YamlConfiguration();
                try {
                    test.load(fullPath.toFile());
                } catch (IOException | InvalidConfigurationException e) {
                    Bukkit.getConsoleSender().sendMessage(ChatUtils.PLUGIN_NAME
                            + " §8- §cAuto-reload skipped: §e" + fileName + " §cis malformed: §e" + e.getMessage());
                    return;
                }
                if (test.getKeys(false).isEmpty()) {
                    Bukkit.getConsoleSender().sendMessage(ChatUtils.PLUGIN_NAME
                            + " §8- §cAuto-reload skipped: §e" + fileName + " §cappears empty.");
                    return;
                }
            }

            RELOAD_ACTIONS.getOrDefault(fileName, p -> {
                if (!LANG_FILE_NAMES.contains(fileName)) {
                    Bukkit.getConsoleSender().sendMessage(ChatUtils.PLUGIN_NAME + " §8- §cUnknown file modified: §e" + fileName);
                }
            }).accept(fullPath);

            // Only reload language messages when a lang file actually changed.
            if (LANG_FILE_NAMES.contains(fileName)) {
                LoadLanguageFile loadLanguageFile = new LoadLanguageFile();
                loadLanguageFile.loadLanguageFile();
                YamlConfiguration langFile = loadLanguageFile.getLangFile();
                LoadMessages.loadMessages(langFile);
            }
        } catch (RuntimeException e) {
            Bukkit.getConsoleSender().sendMessage(ChatUtils.PLUGIN_NAME + " §8- §cFailed to reload file: §e" + e.getMessage());
            LoggerUtility.loggerUtility(FilesAutoReload.class, e);
        }
    }
}
