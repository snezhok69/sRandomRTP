package org.sRandomRTP.BlockBiomes;

import org.bukkit.Material;
import org.sRandomRTP.DifferentMethods.LoggerUtility;
import org.sRandomRTP.DifferentMethods.Variables;

import java.util.List;

public class IsBlockBanned {
    public static boolean isBlockBanned(Material material) {
        if (material == null || Variables.teleportfile == null) {
            return false;
        }
        List<String> bannedBlocksList = Variables.teleportfile.getStringList("teleport.bannedBlocks");
        return bannedBlocksList.contains(material.name());
    }
}