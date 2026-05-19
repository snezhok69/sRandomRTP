package org.sRandomRTP.Files;

import org.sRandomRTP.DifferentMethods.Variables;
import org.sRandomRTP.Services.LocalFeatureGate;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FilesUpdate {
    public List<String> filesUpdate() {
        List<String> updatedFiles = new ArrayList<>();
        File dataFolder = Variables.getInstance().getDataFolder();

        for (String filePath : ConfigPaths.UPDATABLE_FILES) {
            if (ConfigPaths.isLocalAdminBarsFile(filePath)
                    && !LocalFeatureGate.isLocalAdminBarsEnabled()) {
                continue;
            }
            File file = new File(dataFolder, filePath);
            try {
                boolean updated = ConfigUpdater.update(filePath, file);
                if (updated) {
                    updatedFiles.add(filePath);
                }
            } catch (IOException e) {
                Variables.getInstance().getLogger().warning("Failed to update file " + filePath + ": " + e.getMessage());
            }
        }
        return updatedFiles;
    }
}
