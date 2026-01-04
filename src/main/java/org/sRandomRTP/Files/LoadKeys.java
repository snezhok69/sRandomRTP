package org.sRandomRTP.Files;

import org.bukkit.configuration.file.FileConfiguration;
import org.sRandomRTP.Chunk.ChunkWarmManager;
import org.sRandomRTP.DifferentMethods.Variables;

import static org.sRandomRTP.DifferentMethods.Variables.chunkfile;

public class LoadKeys {

    public static String language;

    public static void loadKeys(FileConfiguration config) {
        language = config.getString("Language");

        if (Variables.getInstance() != null) {
            ChunkWarmManager.getInstance(Variables.getInstance()).reload(chunkfile);
        }
    }
}