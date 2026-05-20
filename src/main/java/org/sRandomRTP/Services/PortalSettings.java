package org.sRandomRTP.Services;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.configuration.file.FileConfiguration;
import org.sRandomRTP.DifferentMethods.Variables;
import org.sRandomRTP.Utils.ConfigValueParser;

/**
 * Lightweight typed view over {@code Settings/portal.yml}.
 *
 * <p>The legacy portal command code historically read the YAML file directly in many tiny helper
 * methods. This wrapper keeps the reads in one place, makes the defaults explicit and keeps
 * command/runtime code focused on portal behaviour instead of config parsing.
 */
public final class PortalSettings {

    private final FileConfiguration config;

    public PortalSettings(FileConfiguration config) {
        this.config = config;
    }

    public static PortalSettings current() {
        return new PortalSettings(Variables.getPluginContext().getConfigRegistry().getPortalFile());
    }

    public Material getFloorMaterial() {
        return material("portal.materials.floor", Material.GLASS, "portal floor");
    }

    public Material getBorderMaterial() {
        return material("portal.materials.border", Material.OBSIDIAN, "portal border");
    }

    public Particle getFloorParticle() {
        return particle("portal.particles.floor", Particle.PORTAL, "portal floor");
    }

    public Particle getBorderParticle() {
        return particle("portal.particles.border", Particle.FLAME, "portal border");
    }

    public int getFloorParticleCount() {
        return getInt("portal.particles.floor_count", 2);
    }

    public int getBorderParticleCount() {
        return getInt("portal.particles.border_count", 2);
    }

    public double getFloorParticleDensity() {
        return getDouble("portal.particles.floor_density", 1.0D);
    }

    public double getBorderParticleDensity() {
        return getDouble("portal.particles.border_density", 0.5D);
    }

    public double getFloorParticleSpread() {
        return getDouble("portal.particles.floor_spread", 0.0D);
    }

    public double getBorderParticleSpread() {
        return getDouble("portal.particles.border_spread", 0.0D);
    }

    public boolean isPermanentBorderParticlesEnabled() {
        return getBoolean("portal.particles.permanent_border", false);
    }

    public boolean isPortalRtpEnabled() {
        return getBoolean("portal.rtp.enabled", false);
    }

    public String getPortalRtpWorld() {
        return getString("portal.rtp.world", "");
    }

    public boolean isPortalBlocksProtected() {
        return getBoolean("portal.protect_blocks", false);
    }

    public boolean isCooldownEnabled() {
        return getBoolean("portal.cooldown.enabled", true);
    }

    public long getCooldownSeconds() {
        return Math.max(0L, getLong("portal.cooldown.time", 0L));
    }

    public boolean isPortalCommandsEnabled() {
        return getBoolean("portal-commands.enabled", false);
    }

    public java.util.List<String> getPortalCommands() {
        return config == null
                ? java.util.Collections.<String>emptyList()
                : config.getStringList("portal-commands.commands");
    }

    private boolean getBoolean(String path, boolean defaultValue) {
        return config != null && config.getBoolean(path, defaultValue);
    }

    private int getInt(String path, int defaultValue) {
        return config == null ? defaultValue : config.getInt(path, defaultValue);
    }

    private long getLong(String path, long defaultValue) {
        return config == null ? defaultValue : config.getLong(path, defaultValue);
    }

    private double getDouble(String path, double defaultValue) {
        return config == null ? defaultValue : config.getDouble(path, defaultValue);
    }

    private String getString(String path, String defaultValue) {
        return config == null ? defaultValue : config.getString(path, defaultValue);
    }

    private Material material(String path, Material fallback, String label) {
        String materialName = getString(path, fallback.name());
        Material material = ConfigValueParser.parseMaterial(materialName);
        if (material == null) {
            Bukkit.getLogger().warning("[sRandomRTP] Invalid material for " + label + " in config: "
                    + materialName + ". " + fallback.name() + " is used.");
            return fallback;
        }
        return material;
    }

    private Particle particle(String path, Particle fallback, String label) {
        String particleName = getString(path, fallback.name());
        Particle particle = ConfigValueParser.parseParticle(particleName);
        if (particle == null) {
            Bukkit.getLogger().warning("[sRandomRTP] Invalid particle for " + label + " in config: "
                    + particleName + ". " + fallback.name() + " is used.");
            return fallback;
        }
        return particle;
    }
}
