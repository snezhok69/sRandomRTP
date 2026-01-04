package org.sRandomRTP.Files;

import org.bukkit.Bukkit;
import org.sRandomRTP.DifferentMethods.Variables;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CheckingFile {
    public void compareLanguageFiles(String baseFileName, String compareToFileName) {
        try {
            InputStream baseInputStream = Variables.getInstance().getResource("Lang/" + baseFileName);
            InputStream compareToInputStream = Variables.getInstance().getResource("Lang/" + compareToFileName);
            Yaml yaml = new Yaml();
            Map<String, Object> baseData = yaml.load(baseInputStream);
            Map<String, Object> compareToData = yaml.load(compareToInputStream);
            Set<String> baseKeys = extractKeys(baseData);
            Set<String> compareToKeys = extractKeys(compareToData);
            compareToKeys.removeAll(baseKeys);
            Bukkit.getConsoleSender().sendMessage("В файле " + compareToFileName + " есть следующие ключи, которых нет в файле " + baseFileName + ":");
            for (String missingKey : compareToKeys) {
                Variables.getInstance().getLogger().info("- " + missingKey);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public Set<String> extractKeys(Map<String, Object> data) {
        Set<String> keys = new HashSet<>();
        if (data != null) {
            for (String key : data.keySet()) {
                keys.add(key);
                if (data.get(key) instanceof Map) {
                    keys.addAll(extractKeys((Map<String, Object>) data.get(key)));
                }
            }
        }
        return keys;
    }
}

