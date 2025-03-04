package org.sRandomRTP.BlockBiomes;

import org.bukkit.Material;
import org.sRandomRTP.DifferentMethods.LoggerUtility;
import org.sRandomRTP.DifferentMethods.Variables;
import java.util.List;

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