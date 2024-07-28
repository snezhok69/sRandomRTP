package org.sRandomRTP.BlockBiomes;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.sRandomRTP.DifferentMethods.LoggerUtility;
import org.sRandomRTP.DifferentMethods.Variables;

import java.util.List;

public class LoadBlockListBiome {
    public static void loadBlockList() {
        try {
            Variables.blockListbiome.clear();
            FileConfiguration config = Variables.getInstance().getConfig();
            if (Variables.teleportfile.contains("teleport.bannedBlockscmdbiome")) {
                List<String> blockNames = Variables.teleportfile.getStringList("teleport.bannedBlockscmdbiome");
                for (String materialName : blockNames) {
                    Material material = Material.matchMaterial(materialName.toUpperCase());
                    if (material != null) {
                        Variables.blockListbiome.add(material);
                    } else {
                    }
                }
            }
        } catch (Throwable e) {
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            String callingClassName = stackTrace[2].getClassName();
            LoggerUtility.loggerUtility(callingClassName, e);
        }
    }
}