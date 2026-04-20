package org.sRandomRTP.Files;

import org.bukkit.configuration.file.FileConfiguration;
import org.sRandomRTP.Chunk.ChunkWarmManager;
import org.sRandomRTP.DifferentMethods.Variables;

public class LoadKeys {

    public static String language;

    public static void loadKeys(FileConfiguration config) {
        language = config.getString("Language");

        if (Variables.getInstance() != null) {
            FileConfiguration chunkFile = Variables.getPluginContext() != null
                    ? Variables.getPluginContext().getConfigRegistry().getChunkFile()
                    : null;
            ChunkWarmManager.getInstance(Variables.getInstance()).reload(chunkFile);
        }
    }
}