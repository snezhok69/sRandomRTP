package org.sRandomRTP.DifferentMethods;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

public class EffectGivePlayer {
    public static void effectGivePlayer(Player player) {
        boolean loggingEnabled = Variables.getInstance().getConfig().getBoolean("logs", false);
        if (Variables.effectfile.getBoolean("teleport.Enabled")) {
            List<String> effectGive = Variables.effectfile.getStringList("teleport.Effect");
            int duration = Variables.effectfile.getInt("teleport.effectDuration") * 20;
            int amplifier = Variables.effectfile.getInt("teleport.effectAmplifier");
            Variables.getFoliaLib().getImpl().runAtEntity(player, (s) -> {
                for (String effect : effectGive) {
                    try {
                        int effectId = Integer.parseInt(effect);
                        PotionEffectType effectType = PotionEffectType.getById(effectId);
                        if (effectType == null) {
                            if (loggingEnabled) {
                                Bukkit.getConsoleSender().sendMessage("Invalid effect ID: " + effectId);
                            }
                            continue;
                        }
                        player.addPotionEffect(new PotionEffect(effectType, duration, amplifier, true, false), true);
                        if (loggingEnabled) {
                            Bukkit.getConsoleSender().sendMessage("Applied effect: " + effectType.getName() + " with duration: " + duration + " and amplifier: " + amplifier);
                        }
                    } catch (NumberFormatException e) {
                        if (loggingEnabled) {
                            Bukkit.getConsoleSender().sendMessage("Invalid effect format: " + effect);
                        }
                    } catch (Exception e) {
                        if (loggingEnabled) {
                            Bukkit.getConsoleSender().sendMessage("Error applying effect: " + effect + " - " + e.getMessage());
                        }
                    }
                }
            });
        }
    }
}