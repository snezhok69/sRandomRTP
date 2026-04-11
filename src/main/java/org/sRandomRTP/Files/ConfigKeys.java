package org.sRandomRTP.Files;

/**
 * Central registry of configuration key paths.
 * Use these constants instead of magic strings when accessing Variables.teleportfile,
 * Variables.bossbarfile, Variables.soundfile, etc.
 * Replace raw string literals incrementally — each replaced usage reduces the risk of
 * undetected typos silently returning default values.
 */
public final class ConfigKeys {

    private ConfigKeys() {}

    // ── teleport.yml ────────────────────────────────────────────────────────

    public static final String COOLDOWNS_ENABLED              = "teleport.Cooldowns.enabled";
    public static final String COOLDOWNS_COOLDOWN             = "teleport.Cooldowns.cooldown";
    public static final String COOLDOWNS_MOVE_CANCEL          = "teleport.Cooldowns.move-cancel-cooldown";
    public static final String COOLDOWNS_MOUSE_MOVE_CANCEL    = "teleport.Cooldowns.mouse-move-cancel-cooldown";
    public static final String COOLDOWNS_BREAK_BLOCK_CANCEL   = "teleport.Cooldowns.break-block-cooldown";
    public static final String COOLDOWNS_DMG_CANCEL           = "teleport.Cooldowns.dmg-cancel-cooldown";

    public static final String MAX_TRIES                      = "teleport.maxtries";
    public static final String RADIUS                         = "teleport.radius";
    public static final String MIN_RADIUS                     = "teleport.minradius";
    public static final String COORDINATE_GENERATION         = "teleport.coordinate-generation";
    public static final String USE_ABSOLUTE_COORDINATES      = "teleport.use-absolute-coordinates";

    public static final String CHECKING_IN_REGIONS           = "teleport.checking-in-regions";
    public static final String BANNED_WORLD_ENABLED          = "teleport.bannedworld.enabled";
    public static final String BANNED_WORLD_REDIRECT_ENABLED  = "teleport.bannedworld.redirect.enabled";
    public static final String BANNED_WORLD_REDIRECT_WORLD   = "teleport.bannedworld.redirect.world";

    public static final String BREAK_BLOCK_CANCEL_RTP        = "teleport.break-block-cancel-rtp";
    public static final String DAMAGED_CANCEL_RTP            = "teleport.damaged-cancel-rtp";
    public static final String MOVE_CANCEL_RTP               = "teleport.move-cancel-rtp";
    public static final String MOUSE_MOVE_CANCEL_RTP         = "teleport.mouse-move-cancel-rtp";

    public static final String MIN_Y                         = "teleport.minY";
    public static final String MIN_Y_NETHER                  = "teleport.minY-nether";
    public static final String MIN_Y_END                     = "teleport.minY-end";

    public static final String BLOCK_CAVE_BIOMES             = "teleport.block-cave-biomes";
    public static final String BLOCK_OCEAN_RIVER_BIOMES      = "teleport.block-ocean-river-biomes";

    public static final String RTP_PLAYER_MESSAGES           = "teleport.rtp-player-messages";
    public static final String COMMANDS_TELEPORT_ENABLED     = "teleport.Commandsteleport.enabled";

    // ── bossbar.yml ─────────────────────────────────────────────────────────

    public static final String BOSSBAR_ENABLED               = "teleport.bossbarEnabled";
    public static final String BOSSBAR_TIME                  = "teleport.bossbar-time";
    public static final String ACTION_BAR_ENABLED            = "teleport.actionBarEnabled";

    // ── sound.yml ───────────────────────────────────────────────────────────

    public static final String BOSSBAR_SOUND_ENABLED         = "teleport.boss-bar-teleport-sound.enabled";
    public static final String BOSSBAR_SOUND_NAME            = "teleport.boss-bar-teleport-sound.sound";
    public static final String BOSSBAR_SOUND_VOLUME          = "teleport.boss-bar-teleport-sound.volume";
    public static final String BOSSBAR_SOUND_PITCH           = "teleport.boss-bar-teleport-sound.pitch";

    public static final String COMPLETED_SOUND_ENABLED       = "teleport.completed-teleport-sound.enabled";
    public static final String COMPLETED_SOUND_NAME          = "teleport.completed-teleport-sound.sound";
    public static final String COMPLETED_SOUND_VOLUME        = "teleport.completed-teleport-sound.volume";
    public static final String COMPLETED_SOUND_PITCH         = "teleport.completed-teleport-sound.pitch";
}
