package org.sRandomRTP.DifferentMethods;

import com.tcoded.folialib.FoliaLib;
import com.tcoded.folialib.wrapper.task.WrappedTask;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.popcraft.chunky.api.ChunkyAPI;
import org.sRandomRTP.Main;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import org.bukkit.block.Biome;
import org.sRandomRTP.Services.AdminBarService;
import org.sRandomRTP.Services.ConfigVersionSupport;
import org.sRandomRTP.Services.MessageService;
import org.sRandomRTP.Services.PluginContext;
import org.sRandomRTP.Services.PortalRepository;
import org.sRandomRTP.Services.ReleaseCheckService;
import org.sRandomRTP.Services.RngProvider;
import org.sRandomRTP.Services.RuntimeStateRegistry;
import org.sRandomRTP.Services.ServerMetricsProvider;
import org.sRandomRTP.Services.TeleportMetrics;
import org.sRandomRTP.Services.TeleportService;
    
public class Variables {
    private static volatile RuntimeStateRegistry runtimeState = new RuntimeStateRegistry();
    //
    public static Main instance;
    public static ChunkyAPI chunkyAPI;

    public static Main getInstance() {
        return instance;
    }

    public static boolean isLoggingEnabled() {
        return instance != null
                && instance.getConfig() != null
                && instance.getConfig().getBoolean("logs", false);
    }

    //
    public static volatile Map<Material, Integer> itemMap = Collections.emptyMap();
    //
    public static volatile FileConfiguration teleportfile;
    public static volatile FileConfiguration soundfile;
    public static volatile FileConfiguration bossbarfile;
    public static volatile FileConfiguration nearfile;
    public static volatile FileConfiguration titlefile;
    public static volatile FileConfiguration economyfile;
    public static volatile FileConfiguration effectfile;
    public static volatile FileConfiguration particlesfile;
    public static volatile FileConfiguration farfile;
    public static volatile FileConfiguration middlefile;
    public static volatile FileConfiguration biomefile;
    public static volatile FileConfiguration portalfile;
    public static volatile FileConfiguration chunkfile;
    public static volatile FileConfiguration adminbarsfile;
    //
    public static volatile Set<Material> blockList = Collections.emptySet();
    public static volatile Set<String> bannedBiomesSet = Collections.emptySet();
    // Pre-built Biome EnumSets — O(1) lookup, built once in LoadBlockList.loadAllCaches()
    public static volatile Set<Biome> bannedBiomesEnumSet = Collections.emptySet();
    public static volatile Set<Biome> caveBiomesEnumSet = Collections.emptySet();
    public static volatile Set<Biome> oceanRiverBiomesEnumSet = Collections.emptySet();
    public static volatile boolean blockCaveBiomes = true;
    public static volatile boolean blockOceanRiverBiomes = true;
    public static volatile int cachedMinY = 0;
    public static volatile int cachedMinYNether = 0;
    public static volatile int cachedMinYEnd = 0;
    public static volatile String cachedCoordinateGenerationMethod = "random";
    public static volatile boolean cachedUseAbsoluteCoordinates = false;
    public static volatile int cachedMaxTries = 10;
    public static volatile boolean cachedMoveCancelRtp = false;
    public static volatile boolean cachedMouseMoveCancelRtp = false;
    public static volatile boolean cachedBreakBlockCancelRtp = false;
    public static volatile boolean cachedBreakBlockCooldown = false;
    public static volatile boolean cachedDamagedCancelRtp = false;
    public static volatile boolean cachedDmgCancelCooldown = false;
    // Кеш конфига эффектов — чтобы не читать effectfile на каждый телепорт
    public static volatile boolean cachedEffectsEnabled = false;
    public static volatile java.util.List<String> cachedEffectList = java.util.Collections.emptyList();
    public static volatile int cachedEffectDuration = 0;
    public static volatile int cachedEffectAmplifier = 0;
    // Кеш конфига freeze-эффекта
    public static volatile boolean cachedFreezeEnabled = false;
    public static volatile int cachedFreezeTime = 0;
    // Кеш конфига звуков при телепорте
    public static volatile boolean cachedTeleportSoundEnabled = false;
    public static volatile String cachedTeleportSoundName = "";
    public static volatile float cachedTeleportSoundVolume = 1.0f;
    public static volatile float cachedTeleportSoundPitch = 1.0f;
    // Кеш конфига title/subtitle
    public static volatile boolean cachedTitleEnabled = false;
    public static volatile boolean cachedSubtitleEnabled = false;
    public static volatile int cachedTitleFadeIn = 10;
    public static volatile int cachedTitleStay = 70;
    public static volatile int cachedTitleFadeOut = 20;
    // Кеш конфига экономики (потребляемые ресурсы)
    public static volatile boolean cachedHungerEnabled = false;
    public static volatile int cachedHungerAmount = 0;
    public static volatile boolean cachedHealthEnabled = false;
    public static volatile double cachedHealthAmount = 0.0;
    public static volatile boolean cachedLevelsEnabled = false;
    public static volatile int cachedLevelsAmount = 0;
    public static volatile boolean cachedItemsEnabled = false;
    // Кеш конфига частиц
    public static volatile boolean cachedParticlesEnabled = false;
    public static volatile int cachedParticleDuration = 100;
    public static volatile boolean cachedParticleVisibleToPlayerOnly = true;
    public static volatile int cachedParticleCount = 30;
    public static volatile double cachedParticleOffsetX = 0.5;
    public static volatile double cachedParticleOffsetY = 0.5;
    public static volatile double cachedParticleOffsetZ = 0.5;
    public static volatile double cachedParticleExtra = 0.01;
    public static volatile java.util.List<org.bukkit.Particle> cachedParticleTypes = java.util.Collections.emptyList();
    // Кеш конфига боссбара
    public static volatile boolean cachedBossBarEnabled = true;
    public static volatile boolean cachedActionBarEnabled = true;
    public static volatile int cachedBossBarTime = 7;
    // Кеш звука боссбара
    public static volatile boolean cachedBossBarSoundEnabled = true;
    public static volatile String cachedBossBarSoundName = "BLOCK_NOTE_BLOCK_BIT";
    public static volatile float cachedBossBarSoundVolume = 1.0f;
    public static volatile float cachedBossBarSoundPitch = 1.0f;
    // Кеш cooldown-флагов для отмены при движении/повороте
    public static volatile boolean cachedMoveCancelCooldown = false;
    public static volatile boolean cachedMouseMoveCancelCooldown = false;
    // Кеш основных настроек кулдауна — читаются на каждом /rtp, поэтому кешируем
    public static volatile boolean cachedCooldownsEnabled = false;
    public static volatile int cachedDefaultCooldown = 60;
    // Кеш флага rtp-player-messages — читается при каждой команде /rtp player
    public static volatile boolean cachedRtpPlayerMessages = false;
    // Версия сервера — кешируем один раз при старте, не парсим на каждый телепорт
    public static int cachedServerMajorVersion = 0;
    //
    public static WrappedTask autoCheckVersionTask;
    public static WrappedTask commandReloadTask;
    //
    public static String pluginName = "§a[sRandomRTP]";

    /** Sends a formatted error message: "[sRandomRTP] §8- §c{msg}". */
    public static void sendError(CommandSender sender, String msg) {
        sender.sendMessage(pluginName + " §8- §c" + msg);
    }

    /** Sends a formatted success message: "[sRandomRTP] §8- §a{msg}". */
    public static void sendSuccess(CommandSender sender, String msg) {
        sender.sendMessage(pluginName + " §8- §a" + msg);
    }

    /** Sends the standard "players only" message to a console/non-player sender. */
    public static void sendPlayersOnly(CommandSender sender) {
        sender.sendMessage(pluginName + " §8- §cOnly players can use this command.");
    }

    //
    public static Economy econ = null;
    //
    public static void setupEconomy() {
        if (Bukkit.getServer().getPluginManager().getPlugin("Vault") == null) {
            Bukkit.getConsoleSender().sendMessage("Vault plugin not found!");
            return;
        }

        RegisteredServiceProvider<Economy> rsp =
                Bukkit.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp != null) {
            econ = rsp.getProvider();
        }
    }

    //
    public static void clearTeleportFlags(Player player) {
        runtimeState.clearTeleportFlags(player);
    }

    // Removes expired cooldown entries to prevent unbounded memory growth.
    // Safe to call from any thread since maps are ConcurrentHashMap.
    public static void cleanExpiredCooldowns(long maxCooldownMs) {
        runtimeState.cleanExpiredCooldowns(maxCooldownMs);
    }

    //
    public static boolean pluginToggle = false;
    // Cached at startup — avoids repeated Class.forName() on every teleport.
    public static boolean isWorldGuardAvailable = false;
    public static boolean isVaultAvailable = false;
    //
    public static FoliaLib foliaLib;
    private static volatile PluginContext pluginContext;

    public static FoliaLib getFoliaLib() {
        return foliaLib;
    }

    public static RuntimeStateRegistry getRuntimeState() {
        return runtimeState;
    }

    public static PluginContext getPluginContext() {
        return pluginContext;
    }

    public static MessageService getMessageService() {
        return pluginContext != null ? pluginContext.getMessageService() : new MessageService();
    }

    public static RngProvider getRngProvider() {
        return pluginContext != null ? pluginContext.getRngProvider() : new RngProvider();
    }

    public static PortalRepository getPortalRepository() {
        return pluginContext != null ? pluginContext.getPortalRepository() : null;
    }

    public static TeleportMetrics getTeleportMetrics() {
        return pluginContext != null ? pluginContext.getTeleportMetrics() : null;
    }

    public static TeleportService getTeleportService() {
        return pluginContext != null ? pluginContext.getTeleportService() : new TeleportService(getMessageService());
    }

    public static AdminBarService getAdminBarService() {
        return pluginContext != null ? pluginContext.getAdminBarService()
                : new AdminBarService(getMessageService(), getServerMetricsProvider());
    }

    public static ConfigVersionSupport getConfigVersionSupport() {
        return pluginContext != null ? pluginContext.getConfigVersionSupport() : null;
    }

    public static ServerMetricsProvider getServerMetricsProvider() {
        return pluginContext != null ? pluginContext.getServerMetricsProvider() : new ServerMetricsProvider();
    }

    public static ReleaseCheckService getReleaseCheckService() {
        return pluginContext != null ? pluginContext.getReleaseCheckService() : new ReleaseCheckService(getMessageService());
    }

    //

    //
    public static void initializePlugin(Main plugin) {
        instance = plugin;
        if (foliaLib == null) {
            foliaLib = new FoliaLib(plugin);
        }
        if (pluginContext == null || pluginContext.getPlugin() != plugin) {
            pluginContext = new PluginContext(plugin);
        }
        runtimeState = pluginContext.getRuntimeStateRegistry() != null
                ? pluginContext.getRuntimeStateRegistry() : new RuntimeStateRegistry();
        // Кешируем major-версию сервера один раз при старте плагина
        // Bukkit.getVersion() → "git-Spigot-1.19.4-..." — split по точке даёт [1]="19" и т.п.
        try {
            String ver = Bukkit.getBukkitVersion(); // "1.19.4-R0.1-SNAPSHOT"
            String[] parts = ver.split("[.\\-]");   // ["1","19","4","R0","1","SNAPSHOT"]
            cachedServerMajorVersion = Integer.parseInt(parts[1]);
        } catch (Exception e) {
            cachedServerMajorVersion = 17; // безопасный fallback — поддерживает freeze
        }
    }
}
