package org.sRandomRTP.Data;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.sRandomRTP.DifferentMethods.Variables;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Handles persistence of the total RTP-use counter.
 * Replaces the former DataLoad and DataSave classes.
 */
public class RtpCountDataStore {

    private RtpCountDataStore() {}

    public static void load() {
        File file = new File(Variables.getInstance().getDataFolder(), "Data/rtpCount.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        Variables.getRuntimeState().getRtpCount().set(config.getInt("Uses", 0));
    }

    public static void save() {
        File file = new File(Variables.getInstance().getDataFolder(), "Data/rtpCount.yml");
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        int uses = Variables.getRuntimeState().getRtpCount().get();
        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            writer.println("Uses: " + uses);
        } catch (IOException e) {
            Variables.getInstance().getLogger().severe("Failed to save RTP count: " + e.getMessage());
        }
    }
}
