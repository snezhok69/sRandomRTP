package org.sRandomRTP.Commands;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.command.CommandSender;
import org.sRandomRTP.Services.LocalFeatureGate;
import org.sRandomRTP.Utils.ChatUtils;

import java.io.IOException;

public final class CommandSettings {

    private static final int PAGE_SIZE = 8;

    private CommandSettings() {
    }

    public static boolean handle(CommandSender sender, String[] args) {
        if (!CommandFeatureFlag.ensurePermissionAndEnabled(sender, CommandFeatureFlag.SETTINGS)) {
            return true;
        }

        if (args.length == 1) {
            sendPage(sender, 1);
            return true;
        }

        if (args.length == 2) {
            Integer page = parsePage(args[1]);
            if (page != null) {
                sendPage(sender, page.intValue());
                return true;
            }
        }

        if (args.length >= 3 && "toggle".equalsIgnoreCase(args[1])) {
            handleToggle(sender, args);
            return true;
        }

        sendUsage(sender);
        return true;
    }

    private static void handleToggle(CommandSender sender, String[] args) {
        CommandFeatureFlag flag = CommandFeatureFlag.fromId(args[2]);
        if (flag == null) {
            sender.sendMessage(ChatUtils.PLUGIN_NAME + " §cUnknown setting: §f" + args[2]);
            return;
        }
        if (!flag.isToggleable()) {
            sender.sendMessage(ChatUtils.PLUGIN_NAME + " §e" + flag.getCommandLabel() + " is protected from in-game toggles.");
            return;
        }

        Boolean explicit = args.length >= 4 ? parseMode(args[3]) : null;
        if (args.length >= 4 && explicit == null) {
            sender.sendMessage(ChatUtils.PLUGIN_NAME + " §cUse: /rtp settings toggle " + flag.getId() + " [on|off]");
            return;
        }

        boolean newValue = explicit != null ? explicit.booleanValue() : !flag.isConfiguredEnabled();
        try {
            CommandFeatureFlag.setEnabled(flag, newValue);
        } catch (IOException e) {
            sender.sendMessage(ChatUtils.PLUGIN_NAME + " §cFailed to save Settings/commands.yml: " + e.getMessage());
            return;
        }

        sender.sendMessage(ChatUtils.PLUGIN_NAME + " §a" + flag.getCommandLabel()
                + " command is now " + (newValue ? "§aenabled" : "§edisabled") + "§a.");
        if (flag.isLocalOnly() && !LocalFeatureGate.isLocalAdminBarsEnabled()) {
            sender.sendMessage(ChatUtils.PLUGIN_NAME + " §7Local gate is still closed, so this command stays hidden in public builds.");
        }
        sendPage(sender, pageFor(flag));
    }

    private static void sendPage(CommandSender sender, int requestedPage) {
        CommandFeatureFlag[] flags = CommandFeatureFlag.values();
        int maxPage = Math.max(1, (int) Math.ceil(flags.length / (double) PAGE_SIZE));
        int page = Math.max(1, Math.min(requestedPage, maxPage));
        int start = (page - 1) * PAGE_SIZE;
        int end = Math.min(flags.length, start + PAGE_SIZE);

        sender.sendMessage("");
        sender.sendMessage(ChatUtils.PLUGIN_NAME + " §6Settings §7(" + page + "/" + maxPage + ")");
        sender.sendMessage("§7Debug commands and local admin bossbars are controlled here. Permissions still apply.");
        sender.sendMessage("§7Local admin bars gate: §f" + LocalFeatureGate.isLocalAdminBarsEnabled());

        String lastCategory = "";
        for (int i = start; i < end; i++) {
            CommandFeatureFlag flag = flags[i];
            if (!flag.getCategory().equals(lastCategory)) {
                lastCategory = flag.getCategory();
                sender.sendMessage("§8- §e" + lastCategory);
            }
            sendFlagLine(sender, flag);
        }

        sendNavigation(sender, page, maxPage);
        sender.sendMessage("");
    }

    private static void sendFlagLine(CommandSender sender, CommandFeatureFlag flag) {
        String effectiveStatus = statusLabel(flag);
        String line = "  §7" + flag.getCommandLabel() + " §8(" + flag.getId() + ") §7perm: §f" + flag.getPermission() + " ";
        if (!flag.isToggleable()) {
            sender.sendMessage(line + "§8[locked]");
            return;
        }

        TextComponent message = new TextComponent(TextComponent.fromLegacyText(line));
        TextComponent button = new TextComponent(TextComponent.fromLegacyText(effectiveStatus));
        button.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/rtp settings toggle " + flag.getId()));
        button.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                TextComponent.fromLegacyText("Toggle " + flag.getCommandLabel())));
        message.addExtra(button);
        sender.spigot().sendMessage(message);
    }

    private static void sendNavigation(CommandSender sender, int page, int maxPage) {
        TextComponent nav = new TextComponent("");
        if (page > 1) {
            TextComponent prev = new TextComponent(TextComponent.fromLegacyText("§a[< Previous]"));
            prev.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/rtp settings " + (page - 1)));
            prev.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText("Open previous settings page")));
            nav.addExtra(prev);
        } else {
            nav.addExtra(new TextComponent(TextComponent.fromLegacyText("§8[< Previous]")));
        }
        nav.addExtra(new TextComponent(TextComponent.fromLegacyText(" §7| ")));
        if (page < maxPage) {
            TextComponent next = new TextComponent(TextComponent.fromLegacyText("§a[Next >]"));
            next.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/rtp settings " + (page + 1)));
            next.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText("Open next settings page")));
            nav.addExtra(next);
        } else {
            nav.addExtra(new TextComponent(TextComponent.fromLegacyText("§8[Next >]")));
        }
        sender.spigot().sendMessage(nav);
    }

    private static String statusLabel(CommandFeatureFlag flag) {
        if (flag.isLocalOnly() && !flag.isLocalGateOpen()) {
            return flag.isConfiguredEnabled() ? "§6[LOCAL GATE OFF]" : "§c[OFF]";
        }
        return flag.isConfiguredEnabled() ? "§a[ON]" : "§c[OFF]";
    }

    private static int pageFor(CommandFeatureFlag flag) {
        CommandFeatureFlag[] flags = CommandFeatureFlag.values();
        for (int i = 0; i < flags.length; i++) {
            if (flags[i] == flag) {
                return (i / PAGE_SIZE) + 1;
            }
        }
        return 1;
    }

    private static Integer parsePage(String raw) {
        try {
            return Integer.valueOf(Integer.parseInt(raw));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static Boolean parseMode(String raw) {
        if ("on".equalsIgnoreCase(raw) || "true".equalsIgnoreCase(raw) || "enable".equalsIgnoreCase(raw)) {
            return Boolean.TRUE;
        }
        if ("off".equalsIgnoreCase(raw) || "false".equalsIgnoreCase(raw) || "disable".equalsIgnoreCase(raw)) {
            return Boolean.FALSE;
        }
        return null;
    }

    private static void sendUsage(CommandSender sender) {
        sender.sendMessage(ChatUtils.PLUGIN_NAME + " §6Usage:");
        sender.sendMessage("§7/rtp settings [page]");
        sender.sendMessage("§7/rtp settings toggle <id> [on|off]");
    }
}
