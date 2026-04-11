package org.sRandomRTP.BlockBiomes;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.plugin.Plugin;
import org.sRandomRTP.DifferentMethods.LoggerUtility;
import org.sRandomRTP.DifferentMethods.Variables;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class LoadBlockList {

    // Computed once at class-load time — biome categories are static and never change at runtime.
    private static final Set<Biome> CAVE_BIOMES;
    private static final Set<Biome> OCEAN_RIVER_BIOMES;
    static {
        Set<Biome> caves = new HashSet<>();
        Set<Biome> oceanRiver = new HashSet<>();
        for (Biome b : Biome.values()) {
            String n = b.name();
            if (n.contains("CAVES")) caves.add(b);
            if (n.contains("OCEAN") || n.contains("RIVER")) oceanRiver.add(b);
        }
        CAVE_BIOMES = Collections.unmodifiableSet(caves);
        OCEAN_RIVER_BIOMES = Collections.unmodifiableSet(oceanRiver);
    }

    public static void loadBlockList() {
        try {
            loadAllCaches();
            boolean loggingEnabled = Variables.isLoggingEnabled();
            if (loggingEnabled) {
                Variables.getInstance().getLogger().info("Loaded " + Variables.blockList.size() + " banned blocks");
            }
        } catch (RuntimeException e) {
            LoggerUtility.loggerUtility(LoadBlockList.class, e);
        }
    }

    public static CompletableFuture<Void> loadBlockListAsync(Plugin plugin) {
        CompletableFuture<Void> result = new CompletableFuture<>();
        boolean loggingEnabled = Variables.isLoggingEnabled();
        if (loggingEnabled) {
            Variables.getInstance().getLogger().info("Asynchronous loading of lists of banned blocks and biomes...");
        }
        CompletableFuture.runAsync(() -> {
            Variables.getFoliaLib().getImpl().runLater(() -> {
                try {
                    loadAllCaches();
                    if (loggingEnabled) {
                        Variables.getInstance().getLogger().info("The lists of banned blocks and biomes have been successfully uploaded");
                        Variables.getInstance().getLogger().info("Forbidden Blocks: " + Variables.blockList.size());
                        Variables.getInstance().getLogger().info("Forbidden Biomes: " + Variables.bannedBiomesSet.size());
                    }
                    result.complete(null);
                } catch (RuntimeException e) {
                    Variables.getInstance().getLogger().severe("Error during synchronous update of lists: " + e.getMessage());
                    result.completeExceptionally(e);
                }
            }, 1);
        }).exceptionally(ex -> {
            Variables.getInstance().getLogger().severe("Error during asynchronous loading of lists: " + ex.getMessage());
            result.completeExceptionally(ex);
            return null;
        });
        return result;
    }

    /** Populates all cached config fields from teleportfile. Must be called on the primary thread. */
    private static void loadAllCaches() {
        if (!Bukkit.isPrimaryThread()) {
            Variables.getInstance().getLogger().warning(
                    "[sRandomRTP] loadAllCaches() called off the primary thread — this is a bug, skipping");
            return;
        }
        if (Variables.teleportfile == null) return;

        // Banned blocks
        Set<Material> blocks = new HashSet<>();
        if (Variables.teleportfile.contains("teleport.bannedBlocks")) {
            List<String> blockNames = Variables.teleportfile.getStringList("teleport.bannedBlocks");
            for (String materialName : blockNames) {
                Material material = Material.matchMaterial(materialName.toUpperCase());
                if (material != null) {
                    blocks.add(material);
                }
            }
        }
        Variables.blockList = Collections.unmodifiableSet(blocks);

        // Banned biomes (String set kept for logging; EnumSet built for O(1) runtime checks)
        Set<String> biomes = new HashSet<>(Variables.teleportfile.getStringList("teleport.bannedBiomes"));
        Variables.bannedBiomesSet = Collections.unmodifiableSet(biomes);

        Set<Biome> bannedEnumSet = new HashSet<>();
        for (String biomeName : biomes) {
            if (biomeName == null || biomeName.isBlank()) continue;
            try {
                bannedEnumSet.add(Biome.valueOf(biomeName.toUpperCase()));
            } catch (IllegalArgumentException e) {
                Variables.getInstance().getLogger().warning("Unknown biome in bannedBiomes config: '" + biomeName + "' — skipping");
            }
        }
        Variables.bannedBiomesEnumSet = Collections.unmodifiableSet(bannedEnumSet);

        // Biome category flags
        boolean blockCaves = Variables.teleportfile.getBoolean("teleport.block-cave-biomes", true);
        boolean blockOceanRiver = Variables.teleportfile.getBoolean("teleport.block-ocean-river-biomes", true);
        Variables.blockCaveBiomes = blockCaves;
        Variables.blockOceanRiverBiomes = blockOceanRiver;

        // Assign pre-computed static category sets (computed once at class load, not on every reload)
        Variables.caveBiomesEnumSet = CAVE_BIOMES;
        Variables.oceanRiverBiomesEnumSet = OCEAN_RIVER_BIOMES;

        // Y-level minimums
        Variables.cachedMinY = Variables.teleportfile.getInt("teleport.minY", 0);
        Variables.cachedMinYNether = Variables.teleportfile.getInt("teleport.minY-nether", 0);
        Variables.cachedMinYEnd = Variables.teleportfile.getInt("teleport.minY-end", 0);

        // Coordinate generation hotpath values
        String genMethod = Variables.teleportfile.getString("teleport.coordinate-generation");
        Variables.cachedCoordinateGenerationMethod = (genMethod != null && !genMethod.isEmpty()) ? genMethod : "random";
        Variables.cachedUseAbsoluteCoordinates = Variables.teleportfile.getBoolean("teleport.use-absolute-coordinates", false);
        Variables.cachedMaxTries = Math.max(1, Variables.teleportfile.getInt("teleport.maxtries", 10));

        // Movement / break / damage cancel flags — кешируем чтобы не читать конфиг в каждом event-хендлере
        Variables.cachedMoveCancelRtp        = Variables.teleportfile.getBoolean("teleport.move-cancel-rtp", false);
        Variables.cachedMouseMoveCancelRtp   = Variables.teleportfile.getBoolean("teleport.mouse-move-cancel-rtp", false);
        Variables.cachedBreakBlockCancelRtp  = Variables.teleportfile.getBoolean("teleport.break-block-cancel-rtp", false);
        Variables.cachedBreakBlockCooldown   = Variables.teleportfile.getBoolean("teleport.Cooldowns.break-block-cooldown", false);
        Variables.cachedDamagedCancelRtp     = Variables.teleportfile.getBoolean("teleport.damaged-cancel-rtp", false);
        Variables.cachedDmgCancelCooldown    = Variables.teleportfile.getBoolean("teleport.Cooldowns.dmg-cancel-cooldown", false);

        // Кеш конфига эффектов (effectfile)
        if (Variables.effectfile != null) {
            Variables.cachedEffectsEnabled  = Variables.effectfile.getBoolean("teleport.Enabled", false);
            Variables.cachedEffectList      = java.util.Collections.unmodifiableList(
                    Variables.effectfile.getStringList("teleport.Effect"));
            Variables.cachedEffectDuration  = Variables.effectfile.getInt("teleport.effectDuration", 0);
            Variables.cachedEffectAmplifier = Variables.effectfile.getInt("teleport.effectAmplifier", 0);
            Variables.cachedFreezeEnabled   = Variables.effectfile.getBoolean("teleport.Freeze.enabled", false);
            Variables.cachedFreezeTime      = Variables.effectfile.getInt("teleport.Freeze.time", 0);
        }

        // Кеш конфига звуков (soundfile)
        if (Variables.soundfile != null) {
            Variables.cachedTeleportSoundEnabled = Variables.soundfile.getBoolean("teleport.completed-teleport-sound.enabled", false);
            Variables.cachedTeleportSoundName    = Variables.soundfile.getString("teleport.completed-teleport-sound.sound", "");
            Variables.cachedTeleportSoundVolume  = (float) Variables.soundfile.getDouble("teleport.completed-teleport-sound.volume", 1.0);
            Variables.cachedTeleportSoundPitch   = (float) Variables.soundfile.getDouble("teleport.completed-teleport-sound.pitch", 1.0);
        }

        // Кеш конфига title/subtitle (titlefile)
        if (Variables.titlefile != null) {
            Variables.cachedTitleEnabled    = Variables.titlefile.getBoolean("teleport.titleEnabled", false);
            Variables.cachedSubtitleEnabled = Variables.titlefile.getBoolean("teleport.subtitleEnabled", false);
            Variables.cachedTitleFadeIn     = (int) (Variables.titlefile.getDouble("teleport.titleFadeIn", 0.5) * 20);
            Variables.cachedTitleStay       = (int) (Variables.titlefile.getDouble("teleport.titleStay", 3.5) * 20);
            Variables.cachedTitleFadeOut    = (int) (Variables.titlefile.getDouble("teleport.titleFadeOut", 1.0) * 20);
        }

        // Кеш конфига экономики (economyfile) — Hunger, Health, Levels, Items
        if (Variables.economyfile != null) {
            Variables.cachedHungerEnabled = Variables.economyfile.getBoolean("teleport.Hunger.enabled", false);
            Variables.cachedHungerAmount  = Variables.economyfile.getInt("teleport.Hunger.hunger", 0);
            Variables.cachedHealthEnabled = Variables.economyfile.getBoolean("teleport.Health.enabled", false);
            Variables.cachedHealthAmount  = Variables.economyfile.getDouble("teleport.Health.health", 0.0);
            Variables.cachedLevelsEnabled = Variables.economyfile.getBoolean("teleport.Levels.enabled", false);
            Variables.cachedLevelsAmount  = Variables.economyfile.getInt("teleport.Levels.level", 0);
            Variables.cachedItemsEnabled  = Variables.economyfile.getBoolean("teleport.Items.enabled", false);
        }

        // Кеш конфига частиц (particlesfile)
        if (Variables.particlesfile != null) {
            Variables.cachedParticlesEnabled          = Variables.particlesfile.getBoolean("teleport.particles.enabled", false);
            Variables.cachedParticleDuration          = Variables.particlesfile.getInt("teleport.particles.duration", 5) * 20;
            Variables.cachedParticleVisibleToPlayerOnly = Variables.particlesfile.getBoolean("teleport.particles.visibleToPlayerOnly", true);
            Variables.cachedParticleCount             = Variables.particlesfile.getInt("teleport.particles.count", 30);
            Variables.cachedParticleOffsetX           = Variables.particlesfile.getDouble("teleport.particles.offsetX", 0.5);
            Variables.cachedParticleOffsetY           = Variables.particlesfile.getDouble("teleport.particles.offsetY", 0.5);
            Variables.cachedParticleOffsetZ           = Variables.particlesfile.getDouble("teleport.particles.offsetZ", 0.5);
            Variables.cachedParticleExtra             = Variables.particlesfile.getDouble("teleport.particles.extra", 0.01);
            List<org.bukkit.Particle> particleList = new java.util.ArrayList<>();
            for (String type : Variables.particlesfile.getStringList("teleport.particles.types")) {
                try {
                    particleList.add(org.bukkit.Particle.valueOf(type.toUpperCase()));
                } catch (IllegalArgumentException ignored) {}
            }
            Variables.cachedParticleTypes = java.util.Collections.unmodifiableList(particleList);
        }

        // Кеш конфига боссбара (bossbarfile)
        if (Variables.bossbarfile != null) {
            Variables.cachedBossBarEnabled   = Variables.bossbarfile.getBoolean("teleport.bossbarEnabled", true);
            Variables.cachedActionBarEnabled = Variables.bossbarfile.getBoolean("teleport.actionBarEnabled", true);
            Variables.cachedBossBarTime      = Variables.bossbarfile.getInt("teleport.bossbar-time", 7);
        }

        // Кеш звука боссбара (soundfile — boss-bar-teleport-sound)
        if (Variables.soundfile != null) {
            Variables.cachedBossBarSoundEnabled = Variables.soundfile.getBoolean("teleport.boss-bar-teleport-sound.enabled", true);
            Variables.cachedBossBarSoundName    = Variables.soundfile.getString("teleport.boss-bar-teleport-sound.sound", "BLOCK_NOTE_BLOCK_BIT");
            Variables.cachedBossBarSoundVolume  = (float) Variables.soundfile.getDouble("teleport.boss-bar-teleport-sound.volume", 1.0);
            Variables.cachedBossBarSoundPitch   = (float) Variables.soundfile.getDouble("teleport.boss-bar-teleport-sound.pitch", 1.0);
        }

        // Кеш cooldown-флагов для отмены при движении/повороте + основных настроек кулдауна
        if (Variables.teleportfile != null) {
            Variables.cachedMoveCancelCooldown      = Variables.teleportfile.getBoolean("teleport.Cooldowns.move-cancel-cooldown", false);
            Variables.cachedMouseMoveCancelCooldown = Variables.teleportfile.getBoolean("teleport.Cooldowns.mouse-move-cancel-cooldown", false);
            Variables.cachedCooldownsEnabled        = Variables.teleportfile.getBoolean("teleport.Cooldowns.enabled", false);
            Variables.cachedDefaultCooldown         = Variables.teleportfile.getInt("teleport.Cooldowns.cooldown", 60);
            Variables.cachedRtpPlayerMessages       = Variables.teleportfile.getBoolean("teleport.rtp-player-messages", false);
        }

        // Required items map (economy requirement)
        if (Variables.economyfile != null && Variables.economyfile.getBoolean("teleport.Items.enabled", false)) {
            Map<Material, Integer> items = new HashMap<>();
            List<String> requiredItems = Variables.economyfile.getStringList("teleport.Items.requiredItems");
            for (String itemString : requiredItems) {
                String[] parts = itemString.split(": ");
                if (parts.length < 2) continue;
                Material material = Material.getMaterial(parts[0]);
                if (material == null) continue;
                try {
                    items.put(material, Integer.parseInt(parts[1].trim()));
                } catch (NumberFormatException e) {
                    Variables.instance.getLogger().fine("[sRandomRTP] Skipping invalid item count '" + parts[1].trim() + "' for material " + parts[0]);
                }
            }
            Variables.itemMap = Collections.unmodifiableMap(items);
        } else {
            Variables.itemMap = Collections.emptyMap();
        }
    }
}
