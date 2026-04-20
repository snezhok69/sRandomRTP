package org.sRandomRTP.BlockBiomes;

import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.plugin.Plugin;
import org.sRandomRTP.DifferentMethods.LoggerUtility;
import org.sRandomRTP.DifferentMethods.Variables;
import org.sRandomRTP.BlockBiomes.BiomeFilterSnapshot;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class LoadBlockList {

    // Computed once at class-load time — biome categories are static and never change at runtime.
    // Do NOT move this initialisation inside loadAllCaches() or any reload path:
    // Biome.values() is a server-constant enum and iterating it on every /reload is wasteful.
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
                        Variables.getInstance().getLogger().info("Forbidden Biomes: " + Variables.biomeFilters.bannedBiomesNames().size());
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

    /**
     * Populates all cached config fields from teleportfile.
     * Safe to call from any thread: only reads FileConfiguration maps and writes volatile fields.
     */
    private static void loadAllCaches() {
        org.bukkit.configuration.file.FileConfiguration teleportfile =
                Variables.getPluginContext().getConfigRegistry().getTeleportFile();
        if (teleportfile == null) return;

        // Banned blocks
        Set<Material> blocks = new HashSet<>();
        if (teleportfile.contains("teleport.bannedBlocks")) {
            List<String> blockNames = teleportfile.getStringList("teleport.bannedBlocks");
            for (String materialName : blockNames) {
                Material material = Material.matchMaterial(materialName.toUpperCase());
                if (material != null) {
                    blocks.add(material);
                }
            }
        }
        Variables.blockList = Collections.unmodifiableSet(blocks);

        // Banned biomes (String set kept for logging; EnumSet built for O(1) runtime checks)
        Set<String> biomes = new HashSet<>(teleportfile.getStringList("teleport.bannedBiomes"));
        Set<Biome> bannedEnumSet = new HashSet<>();
        for (String biomeName : biomes) {
            if (biomeName == null || biomeName.isBlank()) continue;
            try {
                bannedEnumSet.add(Biome.valueOf(biomeName.toUpperCase()));
            } catch (IllegalArgumentException e) {
                Variables.getInstance().getLogger().warning("Unknown biome in bannedBiomes config: '" + biomeName + "' — skipping");
            }
        }

        // Biome category flags
        boolean blockCaves = teleportfile.getBoolean("teleport.block-cave-biomes", true);
        boolean blockOceanRiver = teleportfile.getBoolean("teleport.block-ocean-river-biomes", true);

        // Atomic snapshot — one volatile write replaces all 6 separate fields
        Variables.biomeFilters = new BiomeFilterSnapshot(
                Collections.unmodifiableSet(biomes),
                Collections.unmodifiableSet(bannedEnumSet),
                CAVE_BIOMES,
                OCEAN_RIVER_BIOMES,
                blockCaves,
                blockOceanRiver
        );

        // Required items map (economy requirement)
        org.bukkit.configuration.file.FileConfiguration economyfile =
                Variables.getPluginContext().getConfigRegistry().getEconomyFile();
        if (economyfile != null && economyfile.getBoolean("teleport.Items.enabled", false)) {
            Map<Material, Integer> items = new HashMap<>();
            List<String> requiredItems = economyfile.getStringList("teleport.Items.requiredItems");
            for (String itemString : requiredItems) {
                String[] parts = itemString.split(": ");
                if (parts.length < 2) continue;
                Material material = Material.getMaterial(parts[0]);
                if (material == null) continue;
                try {
                    items.put(material, Integer.parseInt(parts[1].trim()));
                } catch (NumberFormatException e) {
                    Variables.getInstance().getLogger().fine("[sRandomRTP] Skipping invalid item count '" + parts[1].trim() + "' for material " + parts[0]);
                }
            }
            Variables.itemMap = Collections.unmodifiableMap(items);
        } else {
            Variables.itemMap = Collections.emptyMap();
        }

        // Build and atomically publish immutable ConfigCache snapshot.
        // New code should read from Variables.configCache; cachedXxx fields remain for legacy callers.
        if (Variables.getPluginContext() != null) {
            Variables.configCache = org.sRandomRTP.Services.ConfigCache.buildFrom(
                    Variables.getPluginContext().getConfigRegistry(),
                    Variables.getInstance() != null ? Variables.getInstance().getConfig() : null);
        }
    }
}
