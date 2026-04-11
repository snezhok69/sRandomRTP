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
import org.bukkit.event.player.PlayerTeleportEvent;
import org.sRandomRTP.DataPortals.PortalData;
import org.sRandomRTP.DifferentMethods.Teleport.CompatibleTeleport;
import org.sRandomRTP.DifferentMethods.Teleport.RegionTaskExecutor;
import org.sRandomRTP.DifferentMethods.Text.TranslateRGBColors;
import org.sRandomRTP.DifferentMethods.Variables;
import org.sRandomRTP.Files.LoadMessages;
import org.sRandomRTP.Services.RuntimeStateRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CommandListPortals {

    private static final int PORTALS_PER_PAGE = 5;

    public static void commandListPortals(CommandSender sender, String[] args) {
        int currentPage = 1;

        if (args == null || args.length == 0) {
            showPortalsList(sender, currentPage);
            return;
        }

        if (args.length == 1) {
            if (args[0].startsWith("-p:")) {
                int page = parsePage(args[0]);
                if (page == -1) {
                    Variables.getMessageService().send(sender, LoadMessages.error_page_number_invalid);
                    return;
                }
                currentPage = page;
            } else {
                String portalName = args[0];
                if (sender instanceof Player) {
                    teleportToPortal((Player) sender, portalName);
                } else {
                    Variables.sendPlayersOnly(sender);
                }
                return;
            }
            showPortalsList(sender, currentPage);
            return;
        }

        if (args.length >= 2) {
            if (args[1].startsWith("-p:")) {
                int page = parsePage(args[1]);
                if (page == -1) {
                    Variables.getMessageService().send(sender, LoadMessages.error_page_number_invalid);
                    return;
                }
                currentPage = page;
                showPortalsList(sender, currentPage);
            } else {
                String portalName = args[1];
                if (sender instanceof Player) {
                    teleportToPortal((Player) sender, portalName);
                } else {
                    Variables.sendPlayersOnly(sender);
                }
            }
        }
    }

    /**
     * Parses a page number from a "-p:N" argument.
     * @return the page number (≥1), or -1 if the argument is invalid
     */
    private static int parsePage(String arg) {
        try {
            int page = Integer.parseInt(arg.split(":")[1]);
            return page >= 1 ? page : -1;
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            return -1;
        }
    }

    private static void showPortalsList(CommandSender sender, int currentPage) {
        java.util.Optional<Player> playerOpt = CommandUtils.requirePlayer(sender);
        if (!playerOpt.isPresent()) return;
        if (!CommandUtils.checkPermission(sender, Permissions.PORTAL)) return;
        Player player = playerOpt.get();
        RuntimeStateRegistry state = Variables.getRuntimeState();
        Map<String, PortalData> playerPortals = state.getPlayerPortals(player.getName());
        if (playerPortals == null || playerPortals.isEmpty()) {
            Variables.getMessageService().send(sender, LoadMessages.error_no_portals);
            return;
        }
        List<String> allPortals = new ArrayList<>(playerPortals.keySet());
        int totalPortals = allPortals.size();
        int totalPages = (int) Math.ceil((double) totalPortals / PORTALS_PER_PAGE);
        if (currentPage > totalPages) {
            Variables.getMessageService().send(sender, LoadMessages.error_page_not_found, "%page%", String.valueOf(currentPage));
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
                double x = portalData.getX();
                double y = portalData.getY();
                double z = portalData.getZ();

                BaseComponent[] deleteButtonText = TextComponent.fromLegacyText(TranslateRGBColors.translateRGBColors(LoadMessages.portal_delete_button));
                TextComponent deleteButton = new TextComponent(deleteButtonText);
                deleteButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/rtp portal del " + portalName));
                deleteButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(TranslateRGBColors.translateRGBColors(LoadMessages.portal_delete_hover.replace("%portal%", portalName)))));

                String coordinates = String.format(LoadMessages.portal_coordinates, worldName, (int) x, (int) y, (int) z);
                BaseComponent[] tpButtonText = TextComponent.fromLegacyText(TranslateRGBColors.translateRGBColors(LoadMessages.portal_tp_button.replace("%portal%", portalName).replace("%coordinates%", coordinates)));
                TextComponent tpButton = new TextComponent(tpButtonText);
                tpButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/rtp portal list " + portalName));
                tpButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(TranslateRGBColors.translateRGBColors(LoadMessages.portal_tp_hover.replace("%portal%", portalName)))));

                tpButton.addExtra(deleteButton);
                sender.spigot().sendMessage(tpButton);
            }
        }

        BaseComponent[] prevButtonText = TextComponent.fromLegacyText(TranslateRGBColors.translateRGBColors(LoadMessages.portal_prev_button));
        TextComponent prevPageButton = new TextComponent(prevButtonText);
        if (currentPage > 1) {
            prevPageButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/rtp portal list -p:" + (currentPage - 1)));
            prevPageButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(TranslateRGBColors.translateRGBColors(LoadMessages.portal_prev_hover))));
        } else {
            prevPageButton.setClickEvent(null);
            prevPageButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(TranslateRGBColors.translateRGBColors(LoadMessages.portal_prev_disabled))));
        }

        BaseComponent[] nextButtonText = TextComponent.fromLegacyText(TranslateRGBColors.translateRGBColors(LoadMessages.portal_next_button));
        TextComponent nextPageButton = new TextComponent(nextButtonText);
        if (currentPage < totalPages) {
            nextPageButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/rtp portal list -p:" + (currentPage + 1)));
            nextPageButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(TranslateRGBColors.translateRGBColors(LoadMessages.portal_next_hover))));
        } else {
            nextPageButton.setClickEvent(null);
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
        RuntimeStateRegistry state = Variables.getRuntimeState();
        PortalData portalData = state.getPlayerPortal(player.getName(), portalName);
        if (portalData == null) {
            Variables.getMessageService().send(player, LoadMessages.error_portal_not_found, "%portal%", portalName);
            return;
        }
        World world = Bukkit.getWorld(portalData.getWorldName());
        if (world == null) {
            Variables.getMessageService().send(player, LoadMessages.error_world_not_found);
            return;
        }
        double x = portalData.getX();
        double y = portalData.getY() + 5;
        double z = portalData.getZ();
        CompatibleTeleport.teleport(
                player,
                new Location(world, x, y, z),
                PlayerTeleportEvent.TeleportCause.PLUGIN,
                Variables.isLoggingEnabled(),
                "portal list teleport"
        ).whenComplete((success, throwable) -> RegionTaskExecutor.runAtEntity(player, () -> {
            if (throwable != null || !Boolean.TRUE.equals(success)) {
                Variables.getMessageService().send(player,
                        java.util.Collections.singletonList("&cTeleport to portal failed. Check LogsErrors/latest-error.log"));
                return;
            }
            Variables.getMessageService().send(player, LoadMessages.portal_teleport_success, "%portal%", portalName);
        }));
    }
}
