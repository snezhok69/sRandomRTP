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
                "Settings/far.yml",
                "Settings/middle.yml",
                "Settings/portal.yml",
                "Settings/chunk-loading.yml",
                "lang/en.yml",
                "lang/ru.yml",
                "lang/es.yml",
                "lang/de.yml",
                "lang/fr.yml",
                "lang/it.yml",
                "lang/pt.yml",
                "lang/zh.yml",
                "lang/ja.yml",
                "lang/ko.yml",
                "lang/ar.yml",
                "lang/pl.yml",
                "lang/vi.yml",
                "lang/ua.yml",
                "lang/tr.yml",
                "lang/custom_messages.yml",
                "Settings_ru/bossbar.yml",
                "Settings_ru/chunk-loading.yml",
                "Settings_ru/config.yml",
                "Settings_ru/economy.yml",
                "Settings_ru/effects.yml",
                "Settings_ru/far.yml",
                "Settings_ru/middle.yml",
                "Settings_ru/near.yml",
                "Settings_ru/particles.yml",
                "Settings_ru/portal.yml",
                "Settings_ru/README.md",
                "Settings_ru/sound.yml",
                "Settings_ru/teleport.yml",
                "Settings_ru/title.yml"
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