package org.sRandomRTP.Files;

import org.sRandomRTP.DifferentMethods.LoggerUtility;
import org.sRandomRTP.DifferentMethods.Variables;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FilesUpdate {
    public List<String> filesUpdate() {
        try {
        List<String> updatedFiles = new ArrayList<>();
        String[] filePaths = {
                "config.yml",
                "Settings/effects.yml",
                "Settings/particles.yml",
                "Settings/teleport.yml",
                "Settings/near.yml",
                "Settings/sound.yml",
                "Settings/title.yml",
                "Settings/bossbar.yml",
                "Settings/economy.yml",
                "Lang/en.yml",
                "Lang/ru.yml",
                "Lang/es.yml",
                "Lang/de.yml",
                "Lang/fr.yml",
                "Lang/it.yml",
                "Lang/pt.yml",
                "Lang/zh.yml",
                "Lang/ja.yml",
                "Lang/ko.yml",
                "Lang/ar.yml",
                "Lang/pl.yml",
                "Lang/vi.yml",
                "Lang/ua.yml",
                "Lang/tr.yml",
                "Lang/custom_messages.yml",
        };
        File dataFolder = Variables.getInstance().getDataFolder();
        for (String filePath : filePaths) {
            File file = new File(dataFolder, filePath);
            try {
                boolean updated = ConfigUpdater.update(filePath, file);
                if (updated) {
                    updatedFiles.add(filePath);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return updatedFiles;
        } catch (Throwable e) {
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            String callingClassName = stackTrace[2].getClassName();
            LoggerUtility.loggerUtility(callingClassName, e);
        }
        return java.util.Collections.emptyList();
    }
}