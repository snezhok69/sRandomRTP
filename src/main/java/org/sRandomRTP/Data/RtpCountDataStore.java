package org.sRandomRTP.Data;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.sRandomRTP.DifferentMethods.Variables;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Handles persistence of the total RTP-use counter.
 * Replaces the former DataLoad and DataSave classes.
 */
public class RtpCountDataStore {
    private static final AtomicBoolean dirty = new AtomicBoolean(false);

    private RtpCountDataStore() {}

    public static void load() {
        File file = new File(Variables.getInstance().getDataFolder(), "Data/rtpCount.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        Variables.getRuntimeState().getRtpCount().set(config.getInt("Uses", 0));
        dirty.set(false);
    }

    public static void incrementAndMarkDirty() {
        if (Variables.getRuntimeState() == null) {
            return;
        }
        Variables.getRuntimeState().getRtpCount().incrementAndGet();
        dirty.set(true);
    }

    public static void markDirty() {
        dirty.set(true);
    }

    public static boolean isDirty() {
        return dirty.get();
    }

    public static void saveIfDirty() {
        if (dirty.get()) {
            save();
        }
    }

    public static void save() {
        if (Variables.getRuntimeState() == null || Variables.getInstance() == null) {
            return;
        }
        File file = new File(Variables.getInstance().getDataFolder(), "Data/rtpCount.yml");
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        int uses = Variables.getRuntimeState().getRtpCount().get();
        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            writer.println("Uses: " + uses);
            dirty.set(false);
        } catch (IOException e) {
            Variables.getInstance().getLogger().severe("Failed to save RTP count: " + e.getMessage());
        }
    }
}
