package org.sRandomRTP.Commands;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.command.CommandSender;
import org.sRandomRTP.DifferentMethods.Variables;
import org.sRandomRTP.Files.LoadMessages;

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
            Variables.getMessageService().send(sender, LoadMessages.settings_unknown, "%id%", args[2]);
            return;
        }
        if (!flag.isToggleable()) {
            Variables.getMessageService().send(sender, LoadMessages.settings_locked,
                    "%command%", flag.getCommandLabel());
            return;
        }

        Boolean explicit = args.length >= 4 ? parseMode(args[3]) : null;
        if (args.length >= 4 && explicit == null) {
            Variables.getMessageService().send(sender, LoadMessages.settings_invalid_mode,
                    "%id%", flag.getId());
            return;
        }

        boolean newValue = explicit != null ? explicit.booleanValue() : !flag.isConfiguredEnabled();
        try {
            CommandFeatureFlag.setEnabled(flag, newValue);
        } catch (IOException e) {
            Variables.getMessageService().send(sender, LoadMessages.settings_save_failed,
                    "%error%", e.getMessage());
            return;
        }

        Variables.getMessageService().send(sender, LoadMessages.settings_changed,
                "%command%", flag.getCommandLabel(),
                "%state%", newValue ? LoadMessages.settings_status_on : LoadMessages.settings_status_off);
        sendPage(sender, pageFor(flag));
    }

    private static void sendPage(CommandSender sender, int requestedPage) {
        CommandFeatureFlag[] flags = CommandFeatureFlag.values();
        int maxPage = Math.max(1, (int) Math.ceil(flags.length / (double) PAGE_SIZE));
        int page = Math.max(1, Math.min(requestedPage, maxPage));
        int start = (page - 1) * PAGE_SIZE;
        int end = Math.min(flags.length, start + PAGE_SIZE);

        sender.sendMessage("");
        sender.sendMessage(format(LoadMessages.settings_header,
                "%page%", String.valueOf(page),
                "%max_page%", String.valueOf(maxPage)));
        sender.sendMessage(format(LoadMessages.settings_description));
        String lastCategory = "";
        for (int i = start; i < end; i++) {
            CommandFeatureFlag flag = flags[i];
            if (!flag.getCategory().equals(lastCategory)) {
                lastCategory = flag.getCategory();
                sender.sendMessage(format(LoadMessages.settings_category,
                        "%category%", localizedCategory(lastCategory)));
            }
            sendFlagLine(sender, flag);
        }

        sendNavigation(sender, page, maxPage);
        sender.sendMessage("");
    }

    private static void sendFlagLine(CommandSender sender, CommandFeatureFlag flag) {
        String effectiveStatus = statusLabel(flag);
        String line = format(LoadMessages.settings_flag_line,
                "%command%", flag.getCommandLabel(),
                "%id%", flag.getId(),
                "%permission%", flag.getPermission());
        if (!flag.isToggleable()) {
            sender.sendMessage(line + format(LoadMessages.settings_status_locked));
            return;
        }

        TextComponent message = new TextComponent(TextComponent.fromLegacyText(line));
        TextComponent button = new TextComponent(TextComponent.fromLegacyText(effectiveStatus));
        button.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/rtp settings toggle " + flag.getId()));
        button.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                TextComponent.fromLegacyText(format(LoadMessages.settings_toggle_hover,
                        "%command%", flag.getCommandLabel()))));
        message.addExtra(button);
        sender.spigot().sendMessage(message);
    }

    private static void sendNavigation(CommandSender sender, int page, int maxPage) {
        TextComponent nav = new TextComponent("");
        if (page > 1) {
            TextComponent prev = new TextComponent(TextComponent.fromLegacyText(format(LoadMessages.settings_prev_button)));
            prev.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/rtp settings " + (page - 1)));
            prev.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    TextComponent.fromLegacyText(format(LoadMessages.settings_prev_hover))));
            nav.addExtra(prev);
        } else {
            nav.addExtra(new TextComponent(TextComponent.fromLegacyText(format(LoadMessages.settings_prev_disabled))));
        }
        nav.addExtra(new TextComponent(TextComponent.fromLegacyText(" §7| ")));
        if (page < maxPage) {
            TextComponent next = new TextComponent(TextComponent.fromLegacyText(format(LoadMessages.settings_next_button)));
            next.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/rtp settings " + (page + 1)));
            next.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    TextComponent.fromLegacyText(format(LoadMessages.settings_next_hover))));
            nav.addExtra(next);
        } else {
            nav.addExtra(new TextComponent(TextComponent.fromLegacyText(format(LoadMessages.settings_next_disabled))));
        }
        sender.spigot().sendMessage(nav);
    }

    private static String statusLabel(CommandFeatureFlag flag) {
        return format(flag.isConfiguredEnabled()
                ? LoadMessages.settings_status_on
                : LoadMessages.settings_status_off);
    }

    private static String localizedCategory(String category) {
        if ("Player".equals(category)) {
            return LoadMessages.settings_category_player;
        }
        if ("Admin".equals(category)) {
            return LoadMessages.settings_category_admin;
        }
        if ("Debug".equals(category)) {
            return LoadMessages.settings_category_debug;
        }
        return category;
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
        Variables.getMessageService().send(sender, LoadMessages.settings_usage);
    }

    private static String format(String line, String... replacements) {
        return Variables.getMessageService().format(line, replacements);
    }
}
