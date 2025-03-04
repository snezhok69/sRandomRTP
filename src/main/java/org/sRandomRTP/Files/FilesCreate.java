package org.sRandomRTP.Files;

import org.sRandomRTP.DifferentMethods.LoggerUtility;
import org.sRandomRTP.DifferentMethods.Variables;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FilesCreate {
    public List<String> filesCreate() {
        try {
        List<String> createdFiles = new ArrayList<>();
        File dataFolder = Variables.getInstance().getDataFolder();
        String[] fileNames = {
                "config.yml",
                "Settings/effects.yml",
                "Data/rtpCount.yml",
                "Settings/particles.yml",
                "Settings/sound.yml",
                "Settings/teleport.yml",
                "Settings/near.yml",
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
        for (String fileName : fileNames) {
            File file = new File(dataFolder, fileName);
            if (!file.exists()) {
                Variables.getInstance().saveResource(fileName, false);
                createdFiles.add(fileName);
            }
        }
        return createdFiles;
        } catch (Throwable e) {
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            String callingClassName = stackTrace[2].getClassName();
            LoggerUtility.loggerUtility(callingClassName, e);
        }
        return java.util.Collections.emptyList();
    }
}