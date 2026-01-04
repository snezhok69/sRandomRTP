package org.sRandomRTP.DifferentMethods.Text;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.sRandomRTP.DifferentMethods.Variables;
import org.sRandomRTP.Files.LoadKeys;

import java.io.File;

public class LoadLanguageFile {
    private static final String DEFAULT_LANGUAGE = "en";
    public YamlConfiguration langConfig;
    public void loadLanguageFile() {
        String language = LoadKeys.language;
        if (language == null || language.trim().isEmpty()) {
            language = DEFAULT_LANGUAGE;
        }
        File langDirectory = new File(Variables.getInstance().getDataFolder(), "lang");
        File langFile = new File(langDirectory, language + ".yml");
        if (!langFile.exists()) {
            Bukkit.getConsoleSender().sendMessage(Variables.pluginName + " §8- §cLanguage file not found: §e" + language + ".yml. §cUsing default translation.");
            langFile = new File(langDirectory, DEFAULT_LANGUAGE + ".yml");
        }
        if (!langFile.exists()) {
            Bukkit.getConsoleSender().sendMessage(Variables.pluginName + " §8- §cDefault language file is missing. Falling back to empty configuration.");
            langConfig = new YamlConfiguration();
            return;
        }
        langConfig = YamlConfiguration.loadConfiguration(langFile);
    }
    public YamlConfiguration getLangFile() {
        return langConfig;
    }
}