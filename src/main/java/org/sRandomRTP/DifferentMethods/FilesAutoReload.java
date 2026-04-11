package org.sRandomRTP.DifferentMethods;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.sRandomRTP.DifferentMethods.Text.LoadLanguageFile;
import org.sRandomRTP.Files.LoadKeys;
import org.sRandomRTP.Files.LoadMessages;

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
                    Bukkit.getConsoleSender().sendMessage(Variables.pluginName + " §8- §cDirectory not found for monitoring: §e" + path);
                    continue;
                }
                try {
                    WatchKey key = path.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
                    registeredKeys.add(key);
                } catch (IOException e) {
                    Bukkit.getConsoleSender().sendMessage(Variables.pluginName + " §8- §cFailed to register path: §e" + path);
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
            Bukkit.getConsoleSender().sendMessage(Variables.pluginName + " §8- §cFailed to initialize file monitor: §e" + e.getMessage());
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
                Bukkit.getConsoleSender().sendMessage(Variables.pluginName + " §8- §cFailed to close file monitor: §e" + e.getMessage());
            }
            watchService = null;
        }
    }

    private static final Set<String> LANG_FILE_NAMES = new HashSet<>(Arrays.asList(
            "en.yml", "ru.yml", "es.yml", "de.yml", "fr.yml", "it.yml",
            "pt.yml", "zh.yml", "ja.yml", "ko.yml", "ar.yml", "pl.yml",
            "vi.yml", "ua.yml", "tr.yml", "custom_messages.yml"
    ));

    private static void reloadFile(Path filePath) {
        try {
            String fileName = filePath.getFileName().toString();
            String dataFolderPath = Variables.getInstance().getDataFolder().toString();
            Map<String, Consumer<Path>> reloadActions = new HashMap<>();
            reloadActions.put("config.yml", p -> {
                Variables.getInstance().reloadConfig();
                LoadKeys.loadKeys(Variables.getInstance().getConfig());
                if (Variables.getPluginContext() != null) {
                    Variables.getReleaseCheckService().restartAutoChecks();
                }
            });
            reloadActions.put("teleport.yml", p -> Variables.teleportfile = YamlConfiguration.loadConfiguration(p.toFile()));
            reloadActions.put("sound.yml", p -> Variables.soundfile = YamlConfiguration.loadConfiguration(p.toFile()));
            reloadActions.put("bossbar.yml", p -> Variables.bossbarfile = YamlConfiguration.loadConfiguration(p.toFile()));
            reloadActions.put("near.yml", p -> Variables.nearfile = YamlConfiguration.loadConfiguration(p.toFile()));
            reloadActions.put("title.yml", p -> Variables.titlefile = YamlConfiguration.loadConfiguration(p.toFile()));
            reloadActions.put("economy.yml", p -> Variables.economyfile = YamlConfiguration.loadConfiguration(p.toFile()));
            reloadActions.put("effects.yml", p -> Variables.effectfile = YamlConfiguration.loadConfiguration(p.toFile()));
            reloadActions.put("particles.yml", p -> Variables.particlesfile = YamlConfiguration.loadConfiguration(p.toFile()));
            reloadActions.put("far.yml", p -> Variables.farfile = YamlConfiguration.loadConfiguration(p.toFile()));
            reloadActions.put("middle.yml", p -> Variables.middlefile = YamlConfiguration.loadConfiguration(p.toFile()));
            reloadActions.put("biome.yml", p -> Variables.biomefile = YamlConfiguration.loadConfiguration(p.toFile()));
            reloadActions.put("portal.yml", p -> Variables.portalfile = YamlConfiguration.loadConfiguration(p.toFile()));
            reloadActions.put("chunk-loading.yml", p -> Variables.chunkfile = YamlConfiguration.loadConfiguration(p.toFile()));
            reloadActions.put("admin-bars.yml", p -> Variables.adminbarsfile = YamlConfiguration.loadConfiguration(p.toFile()));

            Path fullPath;
            if (LANG_FILE_NAMES.contains(fileName)) {
                fullPath = Paths.get(dataFolderPath, "lang", fileName);
            } else if (fileName.equals("config.yml")) {
                fullPath = Paths.get(dataFolderPath, fileName);
            } else {
                fullPath = Paths.get(dataFolderPath, "Settings", fileName);
            }
            reloadActions.getOrDefault(fileName, p -> {
                if (!LANG_FILE_NAMES.contains(fileName)) {
                    Bukkit.getConsoleSender().sendMessage(Variables.pluginName + " §8- §cUnknown file modified: §e" + fileName);
                }
            }).accept(fullPath);
            LoadLanguageFile loadLanguageFile = new LoadLanguageFile();
            loadLanguageFile.loadLanguageFile();
            YamlConfiguration langFile = loadLanguageFile.getLangFile();
            LoadMessages.loadMessages(langFile);
        } catch (RuntimeException e) {
            Bukkit.getConsoleSender().sendMessage(Variables.pluginName + " §8- §cFailed to reload file: §e" + e.getMessage());
        }
    }
}
