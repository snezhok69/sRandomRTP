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
                "Lang/en_us.yml",
                "Lang/ru_ru.yml",
                "Lang/es_es.yml",
                "Lang/de_de.yml",
                "Lang/fr_fr.yml",
                "Lang/it_it.yml",
                "Lang/pt_br.yml",
                "Lang/zh_cn.yml",
                "Lang/ja_jp.yml",
                "Lang/ko_kr.yml",
                "Lang/ar_sa.yml",
                "Lang/pl_pl.yml",
                "Lang/vi_vn.yml",
                "Lang/uk_ua.yml",
                "Lang/tr_tr.yml",
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