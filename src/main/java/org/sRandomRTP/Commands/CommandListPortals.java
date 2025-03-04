package org.sRandomRTP.Commands;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.sRandomRTP.DataPortals.PortalData;
import org.sRandomRTP.DifferentMethods.TranslateRGBColors;
import org.sRandomRTP.DifferentMethods.Variables;
import org.sRandomRTP.Files.LoadMessages;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CommandListPortals {

    private static final int PORTALS_PER_PAGE = 5;

    public static void commandListPortals(CommandSender sender, String[] args) {
        int currentPage = 1;
        if (args.length == 1) {
            showPortalsList(sender, currentPage);
            return;
        }
        if (args[1].startsWith("-p:")) {
            try {
                String pageStr = args[1].split(":")[1];
                currentPage = Integer.parseInt(pageStr);
                if (currentPage < 1) {
                    for (String line : LoadMessages.error_page_number_invalid) {
                        sender.sendMessage(TranslateRGBColors.translateRGBColors(ChatColor.translateAlternateColorCodes('&', line)));
                    }
                    return;
                }
            } catch (NumberFormatException e) {
                for (String line : LoadMessages.error_page_number_invalid) {
                    sender.sendMessage(TranslateRGBColors.translateRGBColors(ChatColor.translateAlternateColorCodes('&', line)));
                }
                return;
            } catch (ArrayIndexOutOfBoundsException e) {
                for (String line : LoadMessages.error_command_format) {
                    sender.sendMessage(TranslateRGBColors.translateRGBColors(ChatColor.translateAlternateColorCodes('&', line)));
                }
                return;
            }
            showPortalsList(sender, currentPage);
        } else {
            String portalName = args[1];
            if (sender instanceof Player) {
                teleportToPortal((Player) sender, portalName);
            } else {
                for (String line : LoadMessages.nopermissioncommand) {
                    sender.sendMessage(TranslateRGBColors.translateRGBColors(ChatColor.translateAlternateColorCodes('&', line)));
                }
            }
        }
    }

    private static void showPortalsList(CommandSender sender, int currentPage) {
        Map<String, PortalData> playerPortals = Variables.playerPortals.get(sender.getName());
        if (playerPortals == null || playerPortals.isEmpty()) {
            for (String line : LoadMessages.error_no_portals) {
                sender.sendMessage(TranslateRGBColors.translateRGBColors(ChatColor.translateAlternateColorCodes('&', line)));
            }
            return;
        }
        List<String> allPortals = new ArrayList<>(playerPortals.keySet());
        int totalPortals = allPortals.size();
        int totalPages = (int) Math.ceil((double) totalPortals / PORTALS_PER_PAGE);
        if (currentPage > totalPages) {
            for (String line : LoadMessages.error_page_not_found) {
                sender.sendMessage(TranslateRGBColors.translateRGBColors(ChatColor.translateAlternateColorCodes('&', line.replace("%page%", String.valueOf(currentPage)))));
            }
            return;
        }

        for (String line : LoadMessages.portal_list_header) {
            sender.sendMessage("");
            sender.sendMessage(TranslateRGBColors.translateRGBColors(ChatColor.translateAlternateColorCodes('&', line.replace("%page%", String.valueOf(currentPage)).replace("%total%", String.valueOf(totalPages)))));
            sender.sendMessage("");
        }

        int startIndex = (currentPage - 1) * PORTALS_PER_PAGE;
        int endIndex = Math.min(startIndex + PORTALS_PER_PAGE, totalPortals);

        for (int i = startIndex; i < endIndex; i++) {
            String portalName = allPortals.get(i);
            PortalData portalData = playerPortals.get(portalName);

            if (portalData != null) {
                String worldName = portalData.getWorldName();
                double x = Double.parseDouble(portalData.getX());
                double y = Double.parseDouble(portalData.getY());
                double z = Double.parseDouble(portalData.getZ());

                BaseComponent[] deleteButtonText = TextComponent.fromLegacyText(TranslateRGBColors.translateRGBColors(LoadMessages.portal_delete_button));
                TextComponent deleteButton = new TextComponent(deleteButtonText);
                deleteButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/rtp delportal " + portalName));
                deleteButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(TranslateRGBColors.translateRGBColors(LoadMessages.portal_delete_hover.replace("%portal%", portalName)))));

                String coordinates = String.format(LoadMessages.portal_coordinates, worldName, (int) x, (int) y, (int) z);
                BaseComponent[] tpButtonText = TextComponent.fromLegacyText(TranslateRGBColors.translateRGBColors(LoadMessages.portal_tp_button.replace("%portal%", portalName).replace("%coordinates%", coordinates)));
                TextComponent tpButton = new TextComponent(tpButtonText);
                tpButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/rtp listportal " + portalName));
                tpButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(TranslateRGBColors.translateRGBColors(LoadMessages.portal_tp_hover.replace("%portal%", portalName)))));

                tpButton.addExtra(deleteButton);
                sender.spigot().sendMessage(tpButton);
            }
        }

        BaseComponent[] prevButtonText = TextComponent.fromLegacyText(TranslateRGBColors.translateRGBColors(LoadMessages.portal_prev_button));
        TextComponent prevPageButton = new TextComponent(prevButtonText);
        if (currentPage > 1) {
            prevPageButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/rtp listportal -p:" + (currentPage - 1)));
            prevPageButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(TranslateRGBColors.translateRGBColors(LoadMessages.portal_prev_hover))));
        } else {
            prevPageButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/rtp listportal -p:" + (currentPage - 1)));
            prevPageButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(TranslateRGBColors.translateRGBColors(LoadMessages.portal_prev_disabled))));
        }

        BaseComponent[] nextButtonText = TextComponent.fromLegacyText(TranslateRGBColors.translateRGBColors(LoadMessages.portal_next_button));
        TextComponent nextPageButton = new TextComponent(nextButtonText);
        if (currentPage < totalPages) {
            nextPageButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/rtp listportal -p:" + (currentPage + 1)));
            nextPageButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(TranslateRGBColors.translateRGBColors(LoadMessages.portal_next_hover))));
        } else {
            nextPageButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/rtp listportal -p:" + (currentPage + 1)));
            nextPageButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(TranslateRGBColors.translateRGBColors(LoadMessages.portal_next_disabled))));
        }

        BaseComponent[] spaceText = TextComponent.fromLegacyText(TranslateRGBColors.translateRGBColors(LoadMessages.portal_space));
        TextComponent space = new TextComponent(spaceText);

        TextComponent finalMessage = new TextComponent();
        finalMessage.addExtra(prevPageButton);
        finalMessage.addExtra(space);
        finalMessage.addExtra(nextPageButton);

        sender.sendMessage("");
        sender.spigot().sendMessage(finalMessage);
        sender.sendMessage("");

    }

    private static void teleportToPortal(Player player, String portalName) {
        Map<String, PortalData> playerPortals = Variables.playerPortals.get(player.getName());
        if (playerPortals == null || !playerPortals.containsKey(portalName)) {
            for (String line : LoadMessages.error_portal_not_found) {
                player.sendMessage(TranslateRGBColors.translateRGBColors(ChatColor.translateAlternateColorCodes('&', line.replace("%portal%", portalName))));
            }
            return;
        }
        PortalData portalData = playerPortals.get(portalName);
        World world = Bukkit.getWorld(portalData.getWorldName());
        if (world == null) {
            for (String line : LoadMessages.error_world_not_found) {
                player.sendMessage(TranslateRGBColors.translateRGBColors(ChatColor.translateAlternateColorCodes('&', line)));
            }
            return;
        }
        double x = Double.parseDouble(portalData.getX());
        double y = Double.parseDouble(portalData.getY());
        double z = Double.parseDouble(portalData.getZ());
        player.teleportAsync(new Location(world, x, y, z));
        for (String line : LoadMessages.portal_teleport_success) {
            player.sendMessage(TranslateRGBColors.translateRGBColors(ChatColor.translateAlternateColorCodes('&', line.replace("%portal%", portalName))));
        }
    }
}