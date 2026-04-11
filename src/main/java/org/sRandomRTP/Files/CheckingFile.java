package org.sRandomRTP.Files;

import org.bukkit.Bukkit;
import org.sRandomRTP.DifferentMethods.LoggerUtility;
import org.sRandomRTP.DifferentMethods.Variables;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CheckingFile {
    public void compareLanguageFiles(String baseFileName, String compareToFileName) {
        try (InputStream baseInputStream = Variables.getInstance().getResource("lang/" + baseFileName);
             InputStream compareToInputStream = Variables.getInstance().getResource("lang/" + compareToFileName)) {
            if (baseInputStream == null || compareToInputStream == null) {
                Bukkit.getConsoleSender().sendMessage(Variables.pluginName + " §8- §cUnable to compare language files: §e" + baseFileName + " §cor §e" + compareToFileName + " §cnot found.");
                return;
            }
            Yaml yaml = new Yaml();
            Map<String, Object> baseData = yaml.load(baseInputStream);
            Map<String, Object> compareToData = yaml.load(compareToInputStream);
            Set<String> baseKeys = extractKeys(baseData);
            Set<String> compareToKeys = extractKeys(compareToData);
            Set<String> missingKeys = new HashSet<>(compareToKeys);
            missingKeys.removeAll(baseKeys);
            if (missingKeys.isEmpty()) {
                return;
            }
            Bukkit.getConsoleSender().sendMessage("File " + baseFileName + " is missing the following keys from " + compareToFileName + ":");
            for (String missingKey : missingKeys) {
                Variables.getInstance().getLogger().info("- " + missingKey);
            }
        } catch (IOException | RuntimeException e) {
            LoggerUtility.loggerUtility(CheckingFile.class, e);
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
