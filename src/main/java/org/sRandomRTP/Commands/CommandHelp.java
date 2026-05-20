package org.sRandomRTP.Commands;

import org.bukkit.command.CommandSender;
import org.sRandomRTP.DifferentMethods.Variables;

import java.util.ArrayList;
import java.util.List;

public class CommandHelp {

    public static void commandHelp(CommandSender sender) {
        if (!CommandUtils.checkPermission(sender, Permissions.HELP)) return;
        Variables.getMessageService().send(sender, visibleHelpLines(sender));
    }

    private static List<String> visibleHelpLines(CommandSender sender) {
        List<String> lines = new ArrayList<>();
        lines.add("");
        lines.add("&cPlugin command help:");
        lines.add("");
        addIf(sender, lines, CommandFeatureFlag.RTP, "&a/rtp - &6random teleportation.");
        addIf(sender, lines, CommandFeatureFlag.WORLD, "&a/rtp world <world> - &6teleportation in the specified world.");
        addIf(sender, lines, CommandFeatureFlag.NEAR, "&a/rtp near - &6teleports near players.");
        addIf(sender, lines, CommandFeatureFlag.BIOME, "&a/rtp biome <biome|category> - &6teleports to selected biome.");
        addIf(sender, lines, CommandFeatureFlag.FAR, "&a/rtp far - &6teleports farther than default.");
        addIf(sender, lines, CommandFeatureFlag.MIDDLE, "&a/rtp middle - &6teleports a medium distance.");
        addIf(sender, lines, CommandFeatureFlag.BACK, "&a/rtp back - &6returns to the place before teleportation.");
        addIf(sender, lines, CommandFeatureFlag.BASE, "&a/rtp base - &6teleports near WorldGuard regions.");
        addIf(sender, lines, CommandFeatureFlag.PLAYER, "&a/rtp player <player> [world] - &6teleports another player.");
        addIf(sender, lines, CommandFeatureFlag.ACCEPT, "&a/rtp accept - &6accepts a teleport request.");
        addIf(sender, lines, CommandFeatureFlag.DENY, "&a/rtp deny - &6denies a teleport request.");
        addIf(sender, lines, CommandFeatureFlag.CANCEL, "&a/rtp cancel - &6cancels your active teleport.");
        addIf(sender, lines, CommandFeatureFlag.PORTAL, "&a/rtp portal <set|del|list> - &6manages RTP portals.");
        addIf(sender, lines, CommandFeatureFlag.PORTAL_CHECK, "&a/rtp portal check - &6checks portal health.");
        addIf(sender, lines, CommandFeatureFlag.CHUNKY, "&a/rtp chunky <radius|stop> - &6controls Chunky generation.");
        addIf(sender, lines, CommandFeatureFlag.VERSION, "&a/rtp version - &6checks for a new plugin version.");
        addIf(sender, lines, CommandFeatureFlag.RELOAD, "&a/rtp reload - &6reloads the plugin.");
        addIf(sender, lines, CommandFeatureFlag.SETTINGS, "&a/rtp settings - &6opens command toggles.");
        addIf(sender, lines, CommandFeatureFlag.DOCTOR, "&a/rtp doctor - &6shows startup/config health.");
        addIf(sender, lines, CommandFeatureFlag.STATS, "&a/rtp stats - &6shows runtime metrics.");
        addIf(sender, lines, CommandFeatureFlag.DUMP, "&a/rtp dump - &6creates a support bundle.");
        addIf(sender, lines, CommandFeatureFlag.TPS_BAR, "&a/rtp tpsbar - &6toggles the TPS admin bossbar.");
        addIf(sender, lines, CommandFeatureFlag.RAM_BAR, "&a/rtp rambar - &6toggles the RAM admin bossbar.");
        addIf(sender, lines, CommandFeatureFlag.MSPT_BAR, "&a/rtp msptbar - &6toggles the MSPT admin bossbar.");
        addIf(sender, lines, CommandFeatureFlag.ALL_BARS, "&a/rtp allbars - &6toggles all admin bossbars.");
        lines.add("");
        return lines;
    }

    private static void addIf(CommandSender sender, List<String> lines, CommandFeatureFlag flag, String line) {
        if (flag.isVisibleTo(sender)) {
            lines.add(line);
        }
    }
}
