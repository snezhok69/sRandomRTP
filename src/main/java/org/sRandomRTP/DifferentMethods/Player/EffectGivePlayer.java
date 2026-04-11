package org.sRandomRTP.DifferentMethods.Player;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.sRandomRTP.DifferentMethods.Variables;

import java.util.List;

public class EffectGivePlayer {
    public static void effectGivePlayer(Player player) {
        boolean loggingEnabled = Variables.isLoggingEnabled();
        if (Variables.cachedEffectsEnabled) {
            List<String> effectGive = Variables.cachedEffectList;
            int duration = Variables.cachedEffectDuration * 20;
            int amplifier = Variables.cachedEffectAmplifier;
            Variables.getFoliaLib().getImpl().runAtEntity(player, (s) -> {
                for (String effect : effectGive) {
                    try {
                        // Сначала пробуем по имени (современный API: SPEED, JUMP_BOOST и т.д.)
                        PotionEffectType effectType = PotionEffectType.getByName(effect.toUpperCase());
                        if (effectType == null) {
                            // Fallback: числовой ID для обратной совместимости со старыми конфигами
                            try {
                                int effectId = Integer.parseInt(effect);
                                //noinspection deprecation
                                effectType = PotionEffectType.getById(effectId);
                            } catch (NumberFormatException ignored) {
                                // не число и не имя — невалидное значение
                            }
                        }
                        if (effectType == null) {
                            if (loggingEnabled) {
                                Bukkit.getConsoleSender().sendMessage("Invalid effect (unknown name or ID): " + effect);
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
