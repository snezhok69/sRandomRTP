package org.sRandomRTP.Commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.sRandomRTP.DifferentMethods.Variables;
import org.sRandomRTP.Files.LoadMessages;
import org.sRandomRTP.Utils.ChatUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

final class PortalCommandSupport {

    private static final List<String> ACTIONS = Arrays.asList("set", "del", "list");
    private static final List<String> SHAPES = Arrays.asList("circle", "square");

    private PortalCommandSupport() {
    }

    static boolean handle(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            ChatUtils.sendPlayersOnly(sender);
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatUtils.PLUGIN_NAME + " §cUsage: /rtp portal <set|del|list> [parameters]");
            return true;
        }

        String portalAction = args[1].toLowerCase();
        if ("set".equals(portalAction)) {
            return handleSet(sender, args);
        }
        if ("del".equals(portalAction)) {
            if (args.length == 3) {
                CommandDelPortal.commandDelPortal(sender, args[2]);
            } else {
                sender.sendMessage(ChatUtils.PLUGIN_NAME + " §cUsage: /rtp portal del <name>");
            }
            return true;
        }
        if ("list".equals(portalAction)) {
            String[] newArgs = args.length > 2
                    ? Arrays.copyOfRange(args, 2, args.length)
                    : new String[0];
            CommandListPortals.commandListPortals(sender, newArgs);
            return true;
        }

        sender.sendMessage(ChatUtils.PLUGIN_NAME + " §cInvalid portal command! Use: set, del or list");
        return true;
    }

    static List<String> actionSuggestions() {
        return ACTIONS;
    }

    static List<String> shapeSuggestions() {
        return SHAPES;
    }

    static List<String> portalNameSuggestions(CommandSender sender, String prefix, int limit) {
        if (!(sender instanceof Player)) {
            return Collections.emptyList();
        }
        return Variables.getRuntimeState().getMatchingPortalNames(sender.getName(), prefix, limit);
    }

    private static boolean handleSet(CommandSender sender, String[] args) {
        if (args.length == 4) {
            Integer radius = parseRadius(sender, args[3]);
            if (radius != null) {
                CommandSetPortal.commandSetPortal(sender, radius.intValue(), args[2], "circle");
            }
            return true;
        }

        if (args.length == 5) {
            Integer radius = parseRadius(sender, args[3]);
            if (radius == null) {
                return true;
            }
            String shape = args[4].toLowerCase();
            if (!SHAPES.contains(shape)) {
                Variables.getMessageService().send(sender, LoadMessages.portalform);
                return true;
            }
            CommandSetPortal.commandSetPortal(sender, radius.intValue(), args[2], shape);
            return true;
        }

        sender.sendMessage(ChatUtils.PLUGIN_NAME + " §cUsage: /rtp portal set <name> <radius> [shape]");
        sender.sendMessage(ChatUtils.PLUGIN_NAME + " §7shape: circle (by default) or square");
        return true;
    }

    private static Integer parseRadius(CommandSender sender, String rawRadius) {
        try {
            return Integer.valueOf(Integer.parseInt(rawRadius));
        } catch (NumberFormatException e) {
            Variables.getMessageService().send(sender, LoadMessages.portalradius);
            return null;
        }
    }
}
