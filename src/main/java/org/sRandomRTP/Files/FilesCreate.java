package org.sRandomRTP.Files;

import org.sRandomRTP.DifferentMethods.Variables;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FilesCreate {
    public List<String> filesCreate() {
        List<String> createdFiles = new ArrayList<>();
        File dataFolder = Variables.getInstance().getDataFolder();

        for (String fileName : ConfigPaths.UPDATABLE_FILES) {
            createIfAbsent(dataFolder, fileName, createdFiles);
        }
        for (String fileName : ConfigPaths.CREATE_ONLY_FILES) {
            createIfAbsent(dataFolder, fileName, createdFiles);
        }
        return createdFiles;
    }

    private void createIfAbsent(File dataFolder, String fileName, List<String> created) {
        File file = new File(dataFolder, fileName);
        if (!file.exists()) {
            Variables.getInstance().saveResource(fileName, false);
            created.add(fileName);
        }
    }
}
