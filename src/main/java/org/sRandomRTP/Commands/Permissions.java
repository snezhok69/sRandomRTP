package org.sRandomRTP.Commands;

import org.bukkit.permissions.Permissible;

/** Centralised permission-node constants. */
public final class Permissions {
    private Permissions() {}

    public static final String RTP        = "sRandomRTP.Command.Rtp";
    public static final String FAR        = "sRandomRTP.Command.Far";
    public static final String MIDDLE     = "sRandomRTP.Command.Middle";
    public static final String NEAR       = "sRandomRTP.Command.Near";
    public static final String BASE       = "sRandomRTP.Command.Base";
    public static final String RTP_BIOME  = "sRandomRTP.Command.RtpBiome";
    public static final String BACK       = "sRandomRTP.Command.Back";
    public static final String CANCEL     = "sRandomRTP.Command.Cancel";
    public static final String VERSION    = "sRandomRTP.Command.Version";
    public static final String PLAYER     = "sRandomRTP.Command.Player";
    public static final String PORTAL     = "sRandomRTP.Command.Portal";
    public static final String RELOAD     = "sRandomRTP.Command.Reload";
    public static final String CHUNKY     = "sRandomRTP.Command.Chunky";
    public static final String WORLD      = "sRandomRTP.Command.World";
    public static final String HELP       = "sRandomRTP.Command.Help";
    public static final String ACCEPT     = "sRandomRTP.Command.Accept";
    public static final String DENY       = "sRandomRTP.Command.Deny";
    public static final String SETTINGS   = "sRandomRTP.Command.Settings";
    public static final String DOCTOR     = "sRandomRTP.Command.Doctor";
    public static final String DUMP       = "sRandomRTP.Command.Dump";
    public static final String STATS      = "sRandomRTP.Command.Stats";
    public static final String PORTAL_CHECK = "sRandomRTP.Command.Portal.Check";
    public static final String ALL_BARS       = "sRandomRTP.Command.AllBars";
    public static final String TPS_BAR        = "sRandomRTP.Command.TpsBar";
    public static final String RAM_BAR        = "sRandomRTP.Command.RamBar";
    public static final String MSPT_BAR       = "sRandomRTP.Command.MsptBar";
    public static final String COOLDOWN_BYPASS = "sRandomRTP.Command.bypass";
    public static final String LEGACY_COOLDOWN_BYPASS = "sRandomRTP.Cooldown.bypass";

    public static boolean hasCooldownBypass(Permissible permissible) {
        return permissible != null
                && (permissible.hasPermission(COOLDOWN_BYPASS)
                || permissible.hasPermission(LEGACY_COOLDOWN_BYPASS));
    }
}
