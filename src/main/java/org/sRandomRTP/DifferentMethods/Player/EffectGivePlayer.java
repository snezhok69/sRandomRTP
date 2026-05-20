package org.sRandomRTP.DifferentMethods.Player;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.sRandomRTP.DifferentMethods.Variables;
import org.sRandomRTP.Utils.ConfigValueParser;

import java.util.List;

public class EffectGivePlayer {
    public static void effectGivePlayer(Player player) {
        boolean loggingEnabled = Variables.isLoggingEnabled();
        if (Variables.configCache.effectsEnabled) {
            List<String> effectGive = Variables.configCache.effectList;
            int duration = Variables.configCache.effectDuration * 20;
            int amplifier = Variables.configCache.effectAmplifier;
            Variables.getFoliaLib().getImpl().runAtEntity(player, (s) -> {
                for (String effect : effectGive) {
                    try {
                        PotionEffectType effectType = ConfigValueParser.parsePotionEffectType(effect);
                        if (effectType == null) {
                            if (loggingEnabled) {
                                Bukkit.getConsoleSender().sendMessage(
                                        "Invalid effect in config: '" + effect + "'. Use names like SPEED, RESISTANCE, JUMP_BOOST, or legacy numeric IDs like 11.");
                            }
                            continue;
                        }
                        player.addPotionEffect(new PotionEffect(effectType, duration, amplifier, true, false), true);
                        if (loggingEnabled) {
                            Bukkit.getConsoleSender().sendMessage("Applied effect: " + effectType.getName() + " with duration: " + duration + " and amplifier: " + amplifier);
                        }
                    } catch (RuntimeException e) {
                        if (loggingEnabled) {
                            Bukkit.getConsoleSender().sendMessage("Error applying effect: " + effect + " - " + e.getMessage());
                        }
                    }
                }
            });
        }
    }
}
