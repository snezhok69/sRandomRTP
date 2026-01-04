package org.sRandomRTP.DifferentMethods;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.sRandomRTP.DifferentMethods.Text.LoadLanguageFile;
import org.sRandomRTP.Files.LoadMessages;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;


public class FilesAutoReload {

    public static List<WatchKey> registeredKeys = new ArrayList<>();
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
                                if (filePath.toString().endsWith(".yml") && !filePath.getFileName().toString().equals("config.yml")) {
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
            watchThread.interrupt();
            watchThread = null;
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

    private static void reloadFile(Path filePath) {
        try {
            String fileName = filePath.getFileName().toString();
            String dataFolderPath = Variables.getInstance().getDataFolder().toString();
            Map<String, Consumer<Path>> reloadActions = new HashMap<>();
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
            reloadActions.put("portal.yml", p -> Variables.portalfile = YamlConfiguration.loadConfiguration(p.toFile()));
            reloadActions.put("chunk-loading.yml", p -> Variables.chunkfile = YamlConfiguration.loadConfiguration(p.toFile()));
            //
            reloadActions.put("en.yml", p -> Variables.langEnFile = YamlConfiguration.loadConfiguration(p.toFile()));
            reloadActions.put("ru.yml", p -> Variables.langRuFile = YamlConfiguration.loadConfiguration(p.toFile()));
            reloadActions.put("es.yml", p -> Variables.langEsFile = YamlConfiguration.loadConfiguration(p.toFile()));
            reloadActions.put("de.yml", p -> Variables.langDeFile = YamlConfiguration.loadConfiguration(p.toFile()));
            reloadActions.put("fr.yml", p -> Variables.langFrFile = YamlConfiguration.loadConfiguration(p.toFile()));
            reloadActions.put("it.yml", p -> Variables.langItFile = YamlConfiguration.loadConfiguration(p.toFile()));
            reloadActions.put("pt.yml", p -> Variables.langPtFile = YamlConfiguration.loadConfiguration(p.toFile()));
            reloadActions.put("zh.yml", p -> Variables.langZhFile = YamlConfiguration.loadConfiguration(p.toFile()));
            reloadActions.put("ja.yml", p -> Variables.langJaFile = YamlConfiguration.loadConfiguration(p.toFile()));
            reloadActions.put("ko.yml", p -> Variables.langKoFile = YamlConfiguration.loadConfiguration(p.toFile()));
            reloadActions.put("ar.yml", p -> Variables.langArFile = YamlConfiguration.loadConfiguration(p.toFile()));
            reloadActions.put("pl.yml", p -> Variables.langPlFile = YamlConfiguration.loadConfiguration(p.toFile()));
            reloadActions.put("vi.yml", p -> Variables.langViFile = YamlConfiguration.loadConfiguration(p.toFile()));
            reloadActions.put("ua.yml", p -> Variables.langUaFile = YamlConfiguration.loadConfiguration(p.toFile()));
            reloadActions.put("tr.yml", p -> Variables.langTrFile = YamlConfiguration.loadConfiguration(p.toFile()));
            reloadActions.put("custom_messages.yml", p -> Variables.langCustomFile = YamlConfiguration.loadConfiguration(p.toFile()));
            //
            Path fullPath;
            if (fileName.equals("en.yml") || fileName.equals("ru.yml") || fileName.equals("es.yml") || 
                fileName.equals("de.yml") || fileName.equals("fr.yml") || fileName.equals("it.yml") || 
                fileName.equals("pt.yml") || fileName.equals("zh.yml") || fileName.equals("ja.yml") || 
                fileName.equals("ko.yml") || fileName.equals("ar.yml") || fileName.equals("pl.yml") || 
                fileName.equals("vi.yml") || fileName.equals("ua.yml") || fileName.equals("tr.yml") || 
                fileName.equals("custom_messages.yml")) {
                fullPath = Paths.get(dataFolderPath, "lang", fileName);
            } else {
                fullPath = Paths.get(dataFolderPath, "Settings", fileName);
            }
            reloadActions.getOrDefault(fileName, p -> Bukkit.getConsoleSender().sendMessage(Variables.pluginName + " §8- §cUnknown file modified: §e" + fileName)).accept(fullPath);
            LoadLanguageFile loadLanguageFile = new LoadLanguageFile();
            loadLanguageFile.loadLanguageFile();
            YamlConfiguration langFile = loadLanguageFile.getLangFile();
            LoadMessages.loadMessages(langFile);
        } catch (Exception e) {
            Bukkit.getConsoleSender().sendMessage(Variables.pluginName + " §8- §cFailed to reload file: §e" + e.getMessage());
        }
    }
}
