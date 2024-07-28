package org.sRandomRTP.Data;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.sRandomRTP.DifferentMethods.Variables;
import java.io.File;

public class DataLoad {
    public static void dataLoad() {
        File file = new File(Variables.getInstance().getDataFolder(), "Data/rtpCount.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        try {
            int uses = config.getInt("Uses", 0);
            Variables.rtpCount.put(1, uses);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
    }
}