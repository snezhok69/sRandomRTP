package org.sRandomRTP.Services;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Collections;
import java.util.List;

/**
 * Immutable snapshot of all parsed config scalar values.
 * Built by {@link #buildFrom(ConfigRegistry)} and atomically swapped in
 * {@link org.sRandomRTP.DifferentMethods.Variables#configCache} on every reload.
 *
 * <p>One atomic write (replacing the volatile reference) eliminates the previous
 * 55 non-atomic separate volatile writes that could leave the config in a
 * partially-updated state during a /reload.
 */
public final class ConfigCache {

    // ── Coordinate / search ─────────────────────────────────────────────────────────
    public final String coordinateGenerationMethod;
    public final boolean useAbsoluteCoordinates;
    public final int maxTries;
    /** Global teleport radius ({@code teleport.radius}). */
    public final int maxRadius;
    /** Global minimum teleport radius ({@code teleport.minradius}). */
    public final int minRadius;
    public final int minY;
    public final int minYNether;
    public final int minYEnd;

    // ── Cancel flags (teleport) ──────────────────────────────────────────────
    public final boolean moveCancelRtp;
    public final boolean mouseMoveCancelRtp;
    public final boolean breakBlockCancelRtp;
    public final boolean breakBlockCooldown;
    public final boolean damagedCancelRtp;
    public final boolean dmgCancelCooldown;

    // ── Effects ──────────────────────────────────────────────────────────────
    public final boolean effectsEnabled;
    public final List<String> effectList;
    public final int effectDuration;
    public final int effectAmplifier;
    public final boolean freezeEnabled;
    public final int freezeTime;

    // ── Teleport sound ───────────────────────────────────────────────────────
    public final boolean teleportSoundEnabled;
    public final String teleportSoundName;
    /** Pre-parsed Sound enum — null if the name is blank or invalid (avoids try/catch in hot-path). */
    public final Sound teleportSound;
    public final float teleportSoundVolume;
    public final float teleportSoundPitch;

    // ── Title / subtitle ─────────────────────────────────────────────────────
    public final boolean titleEnabled;
    public final boolean subtitleEnabled;
    public final int titleFadeIn;
    public final int titleStay;
    public final int titleFadeOut;

    // ── Loading title / subtitle ──────────────────────────────────────────────
    /** {@code teleport.title-loading.titleEnabled-loading} */
    public final boolean titleLoadingEnabled;
    /** {@code teleport.title-loading.subtitleEnabled-loading} */
    public final boolean subtitleLoadingEnabled;
    public final int titleFadeInLoading;
    public final int titleStayLoading;
    public final int titleFadeOutLoading;

    // ── Economy (hunger / health / levels / items) ───────────────────────────
    public final boolean hungerEnabled;
    public final int hungerAmount;
    public final boolean healthEnabled;
    public final double healthAmount;
    public final boolean levelsEnabled;
    public final int levelsAmount;
    public final boolean itemsEnabled;

    // ── Particles ────────────────────────────────────────────────────────────
    public final boolean particlesEnabled;
    public final int particleDuration;
    public final boolean particleVisibleToPlayerOnly;
    public final int particleCount;
    public final double particleOffsetX;
    public final double particleOffsetY;
    public final double particleOffsetZ;
    public final double particleExtra;
    public final List<Particle> particleTypes;

    // ── Boss-bar ─────────────────────────────────────────────────────────────
    public final boolean bossBarEnabled;
    public final boolean actionBarEnabled;
    public final int bossBarTime;

    // ── Boss-bar sound ───────────────────────────────────────────────────────
    public final boolean bossBarSoundEnabled;
    public final String bossBarSoundName;
    public final float bossBarSoundVolume;
    public final float bossBarSoundPitch;

    // ── Cooldown flags ───────────────────────────────────────────────────────
    public final boolean moveCancelCooldown;
    public final boolean mouseMoveCancelCooldown;
    public final boolean cooldownsEnabled;
    public final int defaultCooldown;

    // ── Misc ─────────────────────────────────────────────────────────────────
    public final boolean rtpPlayerMessages;
    /** Cached value of {@code config.yml → logs}. Updated atomically on every /reload. */
    public final boolean loggingEnabled;

    // ── Banned-world ─────────────────────────────────────────────────────────
    public final boolean bannedWorldEnabled;
    public final List<String> bannedWorlds;
    public final boolean bannedWorldRedirectEnabled;
    public final String bannedWorldRedirectWorld;

    // ── WorldGuard / region check ─────────────────────────────────────────────
    public final boolean checkingInRegions;

    // ── Economy / money ───────────────────────────────────────────────────────
    public final boolean moneyEnabled;
    public final int moneyCost;

    // ── Parallel search (SearchPhasePolicy) ──────────────────────────────────
    public final boolean parallelSearchEnabled;
    public final int parallelSearchCandidatesPerBatch;
    public final int parallelSearchMaxGlobalInflight;

    // ── Prefer-generated-chunks (SearchPhasePolicy) ───────────────────────────
    public final boolean preferGeneratedChunksEnabled;
    public final long preferGeneratedChunksWindowMs;
    public final int preferGeneratedChunksMaxAttempts;

    // ── Achievements ─────────────────────────────────────────────────────────
    public final boolean netherAchievementEnabled;
    public final boolean endAchievementEnabled;

    // ── Teleport timeout ──────────────────────────────────────────────────────
    /** {@code teleport.teleport-timeout.enabled} */
    public final boolean teleportTimeoutEnabled;
    /** {@code teleport.teleport-timeout.attempt-timeout-ms} */
    public final long perAttemptTimeoutMs;
    /** {@code teleport.teleport-timeout.total-timeout-ms} */
    public final long totalTimeoutMs;

    // ────────────────────────────────────────────────────────────────────────

    /** Package-private for unit tests in the same package ({@code org.sRandomRTP.Services}). */
    ConfigCache(Builder b) {
        this.coordinateGenerationMethod    = b.coordinateGenerationMethod;
        this.useAbsoluteCoordinates        = b.useAbsoluteCoordinates;
        this.maxTries                      = b.maxTries;
        this.maxRadius                     = b.maxRadius;
        this.minRadius                     = b.minRadius;
        this.minY                          = b.minY;
        this.minYNether                    = b.minYNether;
        this.minYEnd                       = b.minYEnd;
        this.moveCancelRtp                 = b.moveCancelRtp;
        this.mouseMoveCancelRtp            = b.mouseMoveCancelRtp;
        this.breakBlockCancelRtp           = b.breakBlockCancelRtp;
        this.breakBlockCooldown            = b.breakBlockCooldown;
        this.damagedCancelRtp              = b.damagedCancelRtp;
        this.dmgCancelCooldown             = b.dmgCancelCooldown;
        this.effectsEnabled                = b.effectsEnabled;
        this.effectList                    = b.effectList;
        this.effectDuration                = b.effectDuration;
        this.effectAmplifier               = b.effectAmplifier;
        this.freezeEnabled                 = b.freezeEnabled;
        this.freezeTime                    = b.freezeTime;
        this.teleportSoundEnabled          = b.teleportSoundEnabled;
        this.teleportSoundName             = b.teleportSoundName;
        this.teleportSound                 = parseSound(b.teleportSoundName);
        this.teleportSoundVolume           = b.teleportSoundVolume;
        this.teleportSoundPitch            = b.teleportSoundPitch;
        this.titleEnabled                  = b.titleEnabled;
        this.subtitleEnabled               = b.subtitleEnabled;
        this.titleFadeIn                   = b.titleFadeIn;
        this.titleStay                     = b.titleStay;
        this.titleFadeOut                  = b.titleFadeOut;
        this.titleLoadingEnabled           = b.titleLoadingEnabled;
        this.subtitleLoadingEnabled        = b.subtitleLoadingEnabled;
        this.titleFadeInLoading            = b.titleFadeInLoading;
        this.titleStayLoading              = b.titleStayLoading;
        this.titleFadeOutLoading           = b.titleFadeOutLoading;
        this.hungerEnabled                 = b.hungerEnabled;
        this.hungerAmount                  = b.hungerAmount;
        this.healthEnabled                 = b.healthEnabled;
        this.healthAmount                  = b.healthAmount;
        this.levelsEnabled                 = b.levelsEnabled;
        this.levelsAmount                  = b.levelsAmount;
        this.itemsEnabled                  = b.itemsEnabled;
        this.particlesEnabled              = b.particlesEnabled;
        this.particleDuration              = b.particleDuration;
        this.particleVisibleToPlayerOnly   = b.particleVisibleToPlayerOnly;
        this.particleCount                 = b.particleCount;
        this.particleOffsetX               = b.particleOffsetX;
        this.particleOffsetY               = b.particleOffsetY;
        this.particleOffsetZ               = b.particleOffsetZ;
        this.particleExtra                 = b.particleExtra;
        this.particleTypes                 = b.particleTypes;
        this.bossBarEnabled                = b.bossBarEnabled;
        this.actionBarEnabled              = b.actionBarEnabled;
        this.bossBarTime                   = b.bossBarTime;
        this.bossBarSoundEnabled           = b.bossBarSoundEnabled;
        this.bossBarSoundName              = b.bossBarSoundName;
        this.bossBarSoundVolume            = b.bossBarSoundVolume;
        this.bossBarSoundPitch             = b.bossBarSoundPitch;
        this.moveCancelCooldown            = b.moveCancelCooldown;
        this.mouseMoveCancelCooldown       = b.mouseMoveCancelCooldown;
        this.cooldownsEnabled              = b.cooldownsEnabled;
        this.defaultCooldown               = b.defaultCooldown;
        this.rtpPlayerMessages             = b.rtpPlayerMessages;
        this.loggingEnabled                = b.loggingEnabled;
        this.bannedWorldEnabled            = b.bannedWorldEnabled;
        this.bannedWorlds                  = b.bannedWorlds;
        this.bannedWorldRedirectEnabled    = b.bannedWorldRedirectEnabled;
        this.bannedWorldRedirectWorld      = b.bannedWorldRedirectWorld;
        this.checkingInRegions                 = b.checkingInRegions;
        this.moneyEnabled                      = b.moneyEnabled;
        this.moneyCost                         = b.moneyCost;
        this.netherAchievementEnabled          = b.netherAchievementEnabled;
        this.endAchievementEnabled             = b.endAchievementEnabled;
        this.parallelSearchEnabled             = b.parallelSearchEnabled;
        this.parallelSearchCandidatesPerBatch  = b.parallelSearchCandidatesPerBatch;
        this.parallelSearchMaxGlobalInflight   = b.parallelSearchMaxGlobalInflight;
        this.preferGeneratedChunksEnabled      = b.preferGeneratedChunksEnabled;
        this.preferGeneratedChunksWindowMs     = b.preferGeneratedChunksWindowMs;
        this.preferGeneratedChunksMaxAttempts  = b.preferGeneratedChunksMaxAttempts;
        this.teleportTimeoutEnabled            = b.teleportTimeoutEnabled;
        this.perAttemptTimeoutMs               = b.perAttemptTimeoutMs;
        this.totalTimeoutMs                    = b.totalTimeoutMs;
    }

    /** Builds an immutable snapshot from the current ConfigRegistry state. */
    public static ConfigCache buildFrom(ConfigRegistry registry) {
        return buildFrom(registry, null);
    }

    /**
     * Builds an immutable snapshot from ConfigRegistry + the plugin's main config.yml.
     *
     * @param mainConfig the plugin's main {@code config.yml} (may be {@code null} — then
     *                   {@code loggingEnabled} defaults to {@code false}).
     */
    public static ConfigCache buildFrom(ConfigRegistry registry, org.bukkit.configuration.file.FileConfiguration mainConfig) {
        Builder b = new Builder();
        FileConfiguration teleport  = registry.getTeleportFile();
        FileConfiguration effect    = registry.getEffectFile();
        FileConfiguration sound     = registry.getSoundFile();
        FileConfiguration title     = registry.getTitleFile();
        FileConfiguration economy   = registry.getEconomyFile();
        FileConfiguration particles = registry.getParticlesFile();
        FileConfiguration bossbar   = registry.getBossBarFile();

        if (teleport != null) {
            String genMethod = teleport.getString("teleport.coordinate-generation");
            b.coordinateGenerationMethod   = (genMethod != null && !genMethod.isEmpty()) ? genMethod : ConfigDefaults.DEFAULT_COORDINATE_GENERATION;
            b.useAbsoluteCoordinates       = teleport.getBoolean("teleport.use-absolute-coordinates", false);
            b.maxTries                     = Math.max(1, teleport.getInt("teleport.maxtries", 10));
            b.maxRadius                    = teleport.getInt("teleport.radius", 3000);
            b.minRadius                    = teleport.getInt("teleport.minradius", 0);
            b.minY                         = teleport.getInt("teleport.minY", 0);
            b.minYNether                   = teleport.getInt("teleport.minY-nether", 0);
            b.minYEnd                      = teleport.getInt("teleport.minY-end", 0);
            b.moveCancelRtp                = teleport.getBoolean("teleport.move-cancel-rtp", false);
            b.mouseMoveCancelRtp           = teleport.getBoolean("teleport.mouse-move-cancel-rtp", false);
            b.breakBlockCancelRtp          = teleport.getBoolean("teleport.break-block-cancel-rtp", false);
            b.breakBlockCooldown           = teleport.getBoolean("teleport.Cooldowns.break-block-cooldown", false);
            b.damagedCancelRtp             = teleport.getBoolean("teleport.damaged-cancel-rtp", false);
            b.dmgCancelCooldown            = teleport.getBoolean("teleport.Cooldowns.dmg-cancel-cooldown", false);
            b.moveCancelCooldown           = teleport.getBoolean("teleport.Cooldowns.move-cancel-cooldown", false);
            b.mouseMoveCancelCooldown      = teleport.getBoolean("teleport.Cooldowns.mouse-move-cancel-cooldown", false);
            b.cooldownsEnabled             = teleport.getBoolean("teleport.Cooldowns.enabled", false);
            b.defaultCooldown              = teleport.getInt("teleport.Cooldowns.cooldown", 60);
            b.rtpPlayerMessages            = teleport.getBoolean("teleport.rtp-player-messages", false);
            b.bannedWorldEnabled           = teleport.getBoolean("teleport.bannedworld.enabled", false);
            b.bannedWorlds                 = Collections.unmodifiableList(teleport.getStringList("teleport.bannedworld.worlds"));
            b.bannedWorldRedirectEnabled   = teleport.getBoolean("teleport.bannedworld.redirect.enabled", false);
            String redirectWorld           = teleport.getString("teleport.bannedworld.redirect.world", "");
            b.bannedWorldRedirectWorld     = redirectWorld != null ? redirectWorld : "";
            b.checkingInRegions            = teleport.getBoolean("teleport.checking-in-regions", false);
            b.netherAchievementEnabled             = teleport.getBoolean("teleport.achievement.nether-enabled", false);
            b.endAchievementEnabled                = teleport.getBoolean("teleport.achievement.the-end-enabled", false);
            b.parallelSearchEnabled                = teleport.getBoolean("teleport.parallel-search.enabled", false);
            b.parallelSearchCandidatesPerBatch     = Math.max(1, teleport.getInt("teleport.parallel-search.candidates-per-batch", 2));
            b.parallelSearchMaxGlobalInflight      = Math.max(1, teleport.getInt("teleport.parallel-search.max-global-inflight", 24));
            b.preferGeneratedChunksEnabled         = teleport.getBoolean("teleport.prefer-generated-chunks.enabled", false);
            b.preferGeneratedChunksWindowMs        = Math.max(0L, teleport.getLong("teleport.prefer-generated-chunks.window-ms", 1000L));
            b.preferGeneratedChunksMaxAttempts     = Math.max(1, teleport.getInt("teleport.prefer-generated-chunks.max-attempts", 8));
            b.teleportTimeoutEnabled               = teleport.getBoolean("teleport.teleport-timeout.enabled", false);
            b.perAttemptTimeoutMs                  = teleport.getLong("teleport.teleport-timeout.attempt-timeout-ms", Long.MAX_VALUE);
            b.totalTimeoutMs                       = teleport.getLong("teleport.teleport-timeout.total-timeout-ms", Long.MAX_VALUE);
        }
        if (effect != null) {
            b.effectsEnabled   = effect.getBoolean("teleport.Enabled", false);
            b.effectList       = Collections.unmodifiableList(effect.getStringList("teleport.Effect"));
            b.effectDuration   = effect.getInt("teleport.effectDuration", 0);
            b.effectAmplifier  = effect.getInt("teleport.effectAmplifier", 0);
            b.freezeEnabled    = effect.getBoolean("teleport.Freeze.enabled", false);
            b.freezeTime       = effect.getInt("teleport.Freeze.time", 0);
        }
        if (sound != null) {
            b.teleportSoundEnabled  = sound.getBoolean("teleport.completed-teleport-sound.enabled", false);
            b.teleportSoundName     = sound.getString("teleport.completed-teleport-sound.sound", "");
            b.teleportSoundVolume   = (float) sound.getDouble("teleport.completed-teleport-sound.volume", 1.0);
            b.teleportSoundPitch    = (float) sound.getDouble("teleport.completed-teleport-sound.pitch", 1.0);
            b.bossBarSoundEnabled   = sound.getBoolean("teleport.boss-bar-teleport-sound.enabled", true);
            b.bossBarSoundName      = sound.getString("teleport.boss-bar-teleport-sound.sound", "BLOCK_NOTE_BLOCK_BIT");
            b.bossBarSoundVolume    = (float) sound.getDouble("teleport.boss-bar-teleport-sound.volume", 1.0);
            b.bossBarSoundPitch     = (float) sound.getDouble("teleport.boss-bar-teleport-sound.pitch", 1.0);
        }
        if (title != null) {
            b.titleEnabled    = title.getBoolean("teleport.titleEnabled", false);
            b.subtitleEnabled = title.getBoolean("teleport.subtitleEnabled", false);
            b.titleFadeIn     = (int) (title.getDouble("teleport.titleFadeIn", 0.5) * 20);
            b.titleStay       = (int) (title.getDouble("teleport.titleStay", 3.5) * 20);
            b.titleFadeOut    = (int) (title.getDouble("teleport.titleFadeOut", 1.0) * 20);
            b.titleLoadingEnabled    = title.getBoolean("teleport.title-loading.titleEnabled-loading", false);
            b.subtitleLoadingEnabled = title.getBoolean("teleport.title-loading.subtitleEnabled-loading", false);
            b.titleFadeInLoading     = (int) (title.getDouble("teleport.title-loading.titleFadeIn-loading", 0.5) * 20);
            b.titleStayLoading       = (int) (title.getDouble("teleport.title-loading.titleStay-loading", 3.5) * 20);
            b.titleFadeOutLoading    = (int) (title.getDouble("teleport.title-loading.titleFadeOut-loading", 1.0) * 20);
        }
        if (economy != null) {
            b.hungerEnabled  = economy.getBoolean("teleport.Hunger.enabled", false);
            b.hungerAmount   = economy.getInt("teleport.Hunger.hunger", 0);
            b.healthEnabled  = economy.getBoolean("teleport.Health.enabled", false);
            b.healthAmount   = economy.getDouble("teleport.Health.health", 0.0);
            b.levelsEnabled  = economy.getBoolean("teleport.Levels.enabled", false);
            b.levelsAmount   = economy.getInt("teleport.Levels.level", 0);
            b.itemsEnabled   = economy.getBoolean("teleport.Items.enabled", false);
            b.moneyEnabled   = economy.getBoolean("teleport.Money.enabled", false);
            b.moneyCost      = economy.getInt("teleport.Money.money", 0);
        }
        if (particles != null) {
            b.particlesEnabled            = particles.getBoolean("teleport.particles.enabled", false);
            b.particleDuration            = particles.getInt("teleport.particles.duration", 5) * 20;
            b.particleVisibleToPlayerOnly = particles.getBoolean("teleport.particles.visibleToPlayerOnly", true);
            b.particleCount               = particles.getInt("teleport.particles.count", 30);
            b.particleOffsetX             = particles.getDouble("teleport.particles.offsetX", 0.5);
            b.particleOffsetY             = particles.getDouble("teleport.particles.offsetY", 0.5);
            b.particleOffsetZ             = particles.getDouble("teleport.particles.offsetZ", 0.5);
            b.particleExtra               = particles.getDouble("teleport.particles.extra", 0.01);
            List<Particle> particleList = new java.util.ArrayList<>();
            for (String type : particles.getStringList("teleport.particles.types")) {
                try {
                    particleList.add(Particle.valueOf(type.toUpperCase()));
                } catch (IllegalArgumentException ignored) {}
            }
            b.particleTypes = Collections.unmodifiableList(particleList);
        }
        if (bossbar != null) {
            b.bossBarEnabled   = bossbar.getBoolean("teleport.bossbarEnabled", true);
            b.actionBarEnabled = bossbar.getBoolean("teleport.actionBarEnabled", true);
            b.bossBarTime      = bossbar.getInt("teleport.bossbar-time", 7);
        }
        if (mainConfig != null) {
            b.loggingEnabled = mainConfig.getBoolean("logs", false);
        }
        return new ConfigCache(b);
    }

    /** Default instance used before the first config load. */
    public static final ConfigCache DEFAULT = new ConfigCache(new Builder());

    // ── Builder ──────────────────────────────────────────────────────────────

    /** Package-private for unit tests in the same package ({@code org.sRandomRTP.Services}). */
    static final class Builder {
        String coordinateGenerationMethod  = ConfigDefaults.DEFAULT_COORDINATE_GENERATION;
        boolean useAbsoluteCoordinates     = false;
        int maxTries                       = 10;
        int maxRadius                      = 3000;
        int minRadius                      = 0;
        int minY                           = 0;
        int minYNether                     = 0;
        int minYEnd                        = 0;
        boolean moveCancelRtp              = false;
        boolean mouseMoveCancelRtp         = false;
        boolean breakBlockCancelRtp        = false;
        boolean breakBlockCooldown         = false;
        boolean damagedCancelRtp           = false;
        boolean dmgCancelCooldown          = false;
        boolean effectsEnabled             = false;
        List<String> effectList            = Collections.emptyList();
        int effectDuration                 = 0;
        int effectAmplifier                = 0;
        boolean freezeEnabled              = false;
        int freezeTime                     = 0;
        boolean teleportSoundEnabled       = false;
        String teleportSoundName           = "";
        float teleportSoundVolume          = 1.0f;
        float teleportSoundPitch           = 1.0f;
        boolean titleEnabled               = false;
        boolean subtitleEnabled            = false;
        int titleFadeIn                    = 10;
        int titleStay                      = 70;
        int titleFadeOut                   = 20;
        boolean titleLoadingEnabled        = false;
        boolean subtitleLoadingEnabled     = false;
        int titleFadeInLoading             = 10;
        int titleStayLoading               = 70;
        int titleFadeOutLoading            = 20;
        boolean hungerEnabled              = false;
        int hungerAmount                   = 0;
        boolean healthEnabled              = false;
        double healthAmount                = 0.0;
        boolean levelsEnabled              = false;
        int levelsAmount                   = 0;
        boolean itemsEnabled               = false;
        boolean particlesEnabled           = false;
        int particleDuration               = 100;
        boolean particleVisibleToPlayerOnly = true;
        int particleCount                  = 30;
        double particleOffsetX             = 0.5;
        double particleOffsetY             = 0.5;
        double particleOffsetZ             = 0.5;
        double particleExtra               = 0.01;
        List<Particle> particleTypes       = Collections.emptyList();
        boolean bossBarEnabled             = true;
        boolean actionBarEnabled           = true;
        int bossBarTime                    = 7;
        boolean bossBarSoundEnabled        = true;
        String bossBarSoundName            = "BLOCK_NOTE_BLOCK_BIT";
        float bossBarSoundVolume           = 1.0f;
        float bossBarSoundPitch            = 1.0f;
        boolean moveCancelCooldown         = false;
        boolean mouseMoveCancelCooldown    = false;
        boolean cooldownsEnabled           = false;
        int defaultCooldown                = 60;
        boolean rtpPlayerMessages          = false;
        boolean loggingEnabled             = false;
        boolean bannedWorldEnabled         = false;
        List<String> bannedWorlds          = Collections.emptyList();
        boolean bannedWorldRedirectEnabled = false;
        String bannedWorldRedirectWorld    = "";
        boolean checkingInRegions          = false;
        boolean moneyEnabled               = false;
        int moneyCost                      = 0;
        boolean netherAchievementEnabled   = false;
        boolean endAchievementEnabled      = false;
        boolean parallelSearchEnabled             = ConfigDefaults.PARALLEL_SEARCH_ENABLED;
        int parallelSearchCandidatesPerBatch      = ConfigDefaults.PARALLEL_SEARCH_CANDIDATES_PER_BATCH;
        int parallelSearchMaxGlobalInflight       = ConfigDefaults.PARALLEL_SEARCH_MAX_GLOBAL_INFLIGHT;
        boolean preferGeneratedChunksEnabled      = ConfigDefaults.PREFER_GENERATED_CHUNKS_ENABLED;
        long preferGeneratedChunksWindowMs        = ConfigDefaults.PREFER_GENERATED_CHUNKS_WINDOW_MS;
        int preferGeneratedChunksMaxAttempts      = ConfigDefaults.PREFER_GENERATED_CHUNKS_MAX_ATTEMPTS;
        boolean teleportTimeoutEnabled            = false;
        long perAttemptTimeoutMs                  = Long.MAX_VALUE;
        long totalTimeoutMs                       = Long.MAX_VALUE;
    }

    /** Parses a Bukkit {@link Sound} from its name; returns {@code null} if blank or unknown. */
    private static Sound parseSound(String name) {
        if (name == null || name.trim().isEmpty()) return null;
        try {
            return Sound.valueOf(name.trim().toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
