package org.sRandomRTP.Events;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.sRandomRTP.DifferentMethods.Variables;

public class PlayerParticles implements Listener {
    public static void playerParticles(Player player) {
        BukkitTask task = new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= Variables.particlesfile.getInt("teleport.particles.duration") * 20) {
                    if (Variables.particleTasks.containsKey(player)) {
                        BukkitTask searchTask = Variables.particleTasks.get(player);
                        searchTask.cancel();
                        Variables.particleTasks.remove(player);
                    }
                    return;
                }
                Location location = player.getLocation();
                for (String type : Variables.particlesfile.getStringList("teleport.particles.types")) {
                    Particle particleType;
                    try {
                        particleType = Particle.valueOf(type.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        continue;
                    }
                    if (Variables.particlesfile.getBoolean("teleport.particles.visibleToPlayerOnly")) {
                        player.spawnParticle(particleType, location,
                                Variables.particlesfile.getInt("teleport.particles.count"),
                                Variables.particlesfile.getDouble("teleport.particles.offsetX"),
                                Variables.particlesfile.getDouble("teleport.particles.offsetY"),
                                Variables.particlesfile.getDouble("teleport.particles.offsetZ"),
                                Variables.particlesfile.getDouble("teleport.particles.extra"));
                    } else {
                        player.getWorld().spawnParticle(particleType, location,
                                Variables.particlesfile.getInt("teleport.particles.count"),
                                Variables.particlesfile.getDouble("teleport.particles.offsetX"),
                                Variables.particlesfile.getDouble("teleport.particles.offsetY"),
                                Variables.particlesfile.getDouble("teleport.particles.offsetZ"),
                                Variables.particlesfile.getDouble("teleport.particles.extra"));
                    }
                }
                ticks++;
            }
        }.runTaskTimerAsynchronously(Variables.getInstance(), 0, 1);
        Variables.particleTasks.put(player, task);
    }

    @EventHandler
    public void playerquitevent(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (Variables.particleTasks.containsKey(player)) {
            BukkitTask searchTask = Variables.particleTasks.get(player);
            searchTask.cancel();
            Variables.particleTasks.remove(player);
        }
    }
}