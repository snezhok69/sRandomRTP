package org.sRandomRTP.Files;

import org.bukkit.configuration.file.YamlConfiguration;
import org.sRandomRTP.DifferentMethods.Variables;

import java.io.File;

public class LoadFiles {
    public static void loadFiles() {
        Variables.effectfile = YamlConfiguration.loadConfiguration(new File(Variables.getInstance().getDataFolder(), "Settings/effects.yml"));
        Variables.particlesfile = YamlConfiguration.loadConfiguration(new File(Variables.getInstance().getDataFolder(), "Settings/particles.yml"));
        Variables.teleportfile = YamlConfiguration.loadConfiguration(new File(Variables.getInstance().getDataFolder(), "Settings/teleport.yml"));
        Variables.bossbarfile = YamlConfiguration.loadConfiguration(new File(Variables.getInstance().getDataFolder(), "Settings/bossbar.yml"));
        Variables.nearfile = YamlConfiguration.loadConfiguration(new File(Variables.getInstance().getDataFolder(), "Settings/near.yml"));
        Variables.titlefile = YamlConfiguration.loadConfiguration(new File(Variables.getInstance().getDataFolder(), "Settings/title.yml"));
        Variables.economyfile = YamlConfiguration.loadConfiguration(new File(Variables.getInstance().getDataFolder(), "Settings/economy.yml"));
        Variables.soundfile = YamlConfiguration.loadConfiguration(new File(Variables.getInstance().getDataFolder(), "Settings/sound.yml"));
    }
}
