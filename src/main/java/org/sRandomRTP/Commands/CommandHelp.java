package org.sRandomRTP.Commands;

import org.bukkit.command.CommandSender;
import org.sRandomRTP.DifferentMethods.Variables;
import org.sRandomRTP.Files.LoadMessages;

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
        lines.add(LoadMessages.commandHelpLine("header", 1, "&cPlugin command help:"));
        lines.add("");
        addIf(sender, lines, CommandFeatureFlag.RTP, LoadMessages.commandHelpLine("rtp", 3, "&a/rtp - &6random teleportation."));
        addIf(sender, lines, CommandFeatureFlag.WORLD, LoadMessages.commandHelpLine("world", 4, "&a/rtp world <world> - &6teleportation in the specified world."));
        addIf(sender, lines, CommandFeatureFlag.NEAR, LoadMessages.commandHelpLine("near", 5, "&a/rtp near - &6teleports near players."));
        addIf(sender, lines, CommandFeatureFlag.HELP, LoadMessages.commandHelpLine("help", 6, "&a/rtp help - &6displays command help."));
        addIf(sender, lines, CommandFeatureFlag.BIOME, LoadMessages.commandHelpLine("biome", -1, "&a/rtp biome <biome|category> - &6teleports to selected biome."));
        addIf(sender, lines, CommandFeatureFlag.FAR, LoadMessages.commandHelpLine("far", -1, "&a/rtp far - &6teleports farther than default."));
        addIf(sender, lines, CommandFeatureFlag.MIDDLE, LoadMessages.commandHelpLine("middle", -1, "&a/rtp middle - &6teleports a medium distance."));
        addIf(sender, lines, CommandFeatureFlag.BACK, LoadMessages.commandHelpLine("back", 10, "&a/rtp back - &6returns to the place before teleportation."));
        addIf(sender, lines, CommandFeatureFlag.BASE, LoadMessages.commandHelpLine("base", 11, "&a/rtp base - &6teleports near WorldGuard regions."));
        addIf(sender, lines, CommandFeatureFlag.PLAYER, LoadMessages.commandHelpLine("player", 12, "&a/rtp player <player> [world] - &6teleports another player."));
        addIf(sender, lines, CommandFeatureFlag.ACCEPT, LoadMessages.commandHelpLine("accept", 13, "&a/rtp accept - &6accepts a teleport request."));
        addIf(sender, lines, CommandFeatureFlag.DENY, LoadMessages.commandHelpLine("deny", 14, "&a/rtp deny - &6denies a teleport request."));
        addIf(sender, lines, CommandFeatureFlag.CANCEL, LoadMessages.commandHelpLine("cancel", 9, "&a/rtp cancel - &6cancels your active teleport."));
        addIf(sender, lines, CommandFeatureFlag.PORTAL, LoadMessages.commandHelpLine("portal", -1, "&a/rtp portal <set|del|list> - &6manages RTP portals."));
        addIf(sender, lines, CommandFeatureFlag.PORTAL_CHECK, LoadMessages.commandhelp_portal_check);
        addIf(sender, lines, CommandFeatureFlag.CHUNKY, LoadMessages.commandHelpLine("chunky", 18, "&a/rtp chunky <radius|stop> - &6controls Chunky generation."));
        addIf(sender, lines, CommandFeatureFlag.VERSION, LoadMessages.commandHelpLine("version", 7, "&a/rtp version - &6checks for a new plugin version."));
        addIf(sender, lines, CommandFeatureFlag.RELOAD, LoadMessages.commandHelpLine("reload", 8, "&a/rtp reload - &6reloads the plugin."));
        addIf(sender, lines, CommandFeatureFlag.SETTINGS, LoadMessages.commandhelp_settings);
        addIf(sender, lines, CommandFeatureFlag.DOCTOR, LoadMessages.commandhelp_doctor);
        addIf(sender, lines, CommandFeatureFlag.STATS, LoadMessages.commandhelp_stats);
        addIf(sender, lines, CommandFeatureFlag.DUMP, LoadMessages.commandhelp_dump);
        addIf(sender, lines, CommandFeatureFlag.TPS_BAR, LoadMessages.commandhelp_tpsbar);
        addIf(sender, lines, CommandFeatureFlag.RAM_BAR, LoadMessages.commandhelp_rambar);
        addIf(sender, lines, CommandFeatureFlag.MSPT_BAR, LoadMessages.commandhelp_msptbar);
        addIf(sender, lines, CommandFeatureFlag.ALL_BARS, LoadMessages.commandhelp_allbars);
        lines.add("");
        return lines;
    }

    private static void addIf(CommandSender sender, List<String> lines, CommandFeatureFlag flag, String line) {
        if (flag.isVisibleTo(sender)) {
            lines.add(line);
        }
    }
}
