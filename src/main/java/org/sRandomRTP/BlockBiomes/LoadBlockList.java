package org.sRandomRTP.BlockBiomes;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;
import org.sRandomRTP.DifferentMethods.LoggerUtility;
import org.sRandomRTP.DifferentMethods.Variables;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class LoadBlockList {
    public static void loadBlockList() {
        try {
            Variables.blockList.clear();
            if (Variables.teleportfile.contains("teleport.bannedBlocks")) {
                List<String> blockNames = Variables.teleportfile.getStringList("teleport.bannedBlocks");
                for (String materialName : blockNames) {
                    Material material = Material.matchMaterial(materialName.toUpperCase());
                    if (material != null) {
                        Variables.blockList.add(material);
                    }
                }
            }
            boolean loggingEnabled = Variables.getInstance().getConfig().getBoolean("logs", false);
            if (loggingEnabled) {
                Variables.getInstance().getLogger().info("Loaded " + Variables.blockList.size() + " banned blocks");
            }
        } catch (Throwable e) {
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            String callingClassName = stackTrace[2].getClassName();
            LoggerUtility.loggerUtility(callingClassName, e);
        }
    }
    public static CompletableFuture<Void> loadBlockListAsync(Plugin plugin) {
        return CompletableFuture.runAsync(() -> {
            try {
                boolean loggingEnabled = Variables.getInstance().getConfig().getBoolean("logs", false);
                if (loggingEnabled) {
                    Variables.getInstance().getLogger().info("Asynchronous loading of lists of banned blocks and biomes...");
                }

                Variables.getFoliaLib().getImpl().runLater(() -> {
                    try {
                        if (Variables.teleportfile.contains("teleport.bannedBlocks")) {
                            Variables.blockList.clear();
                            List<String> blockNames = Variables.teleportfile.getStringList("teleport.bannedBlocks");
                            for (String materialName : blockNames) {
                                Material material = Material.matchMaterial(materialName.toUpperCase());
                                if (material != null) {
                                    Variables.blockList.add(material);
                                }
                            }
                        }
                        
                        if (loggingEnabled) {
                            Variables.getInstance().getLogger().info("The lists of banned blocks and biomes have been successfully uploaded");
                            Variables.getInstance().getLogger().info("Forbidden Blocks: " + Variables.blockList.size());
                            Variables.getInstance().getLogger().info("Forbidden Biomes: " + Variables.teleportfile.getStringList("teleport.bannedBiomes").size());
                        }
                    } catch (Exception e) {
                        Variables.getInstance().getLogger().severe("Error during synchronous update of lists: " + e.getMessage());
                    }
                }, 1);
            } catch (Throwable e) {
                Variables.getInstance().getLogger().severe("Error during asynchronous loading of lists: " + e.getMessage());
                if (Variables.getInstance().getConfig().getBoolean("logs", false)) {
                    e.printStackTrace();
                }
            }
        });
    }
}