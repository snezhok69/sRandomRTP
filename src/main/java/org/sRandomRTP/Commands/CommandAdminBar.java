package org.sRandomRTP.Commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.sRandomRTP.DifferentMethods.LoggerUtility;
import org.sRandomRTP.DifferentMethods.Variables;
import org.sRandomRTP.Files.LoadMessages;
import org.sRandomRTP.Services.AdminBarService;
import org.sRandomRTP.Services.AdminBarType;

public final class CommandAdminBar {

    private CommandAdminBar() {
    }

    public static boolean handle(CommandSender sender, String[] args) {
        try {
            if (args == null || args.length == 0) {
                return false;
            }

            AdminBarService service = Variables.getAdminBarService();
            if ("allbars".equalsIgnoreCase(args[0])) {
                return handleAllBars(sender, args, service);
            }

            AdminBarType type = AdminBarType.fromSubCommand(args[0]);
            if (type == null) {
                return false;
            }
            if (!service.isEnabled(type)) {
                service.sendDisabledInConfig(sender);
                return true;
            }

            if (!(sender instanceof Player)) {
                service.sendPlayersOnly(sender);
                return true;
            }

            if (!sender.hasPermission(type.getPermissionNode())) {
                Variables.getMessageService().send(sender, LoadMessages.nopermissioncommand);
                return true;
            }

            if (args.length > 2) {
                service.sendUsage(sender, type);
                return true;
            }

            Player player = (Player) sender;
            String mode = args.length == 2 ? args[1].toLowerCase() : "";
            if ("".equals(mode)) {
                if (service.isActive(player, type)) {
                    service.disable(player, type, true);
                } else {
                    service.enable(player, type, true);
                }
                return true;
            }

            if ("on".equals(mode)) {
                service.enable(player, type, true);
                return true;
            }

            if ("off".equals(mode)) {
                service.disable(player, type, true);
                return true;
            }

            service.sendUsage(sender, type);
            return true;
        } catch (RuntimeException e) {
            LoggerUtility.loggerUtility(CommandAdminBar.class, e);
            return true;
        }
    }

    private static boolean handleAllBars(CommandSender sender, String[] args, AdminBarService service) {
        if (!sender.hasPermission(AdminBarService.ALL_BARS_PERMISSION)) {
            Variables.getMessageService().send(sender, LoadMessages.nopermissioncommand);
            return true;
        }

        if (!(sender instanceof Player)) {
            service.sendPlayersOnly(sender);
            return true;
        }

        if (args.length > 2) {
            service.sendUsage(sender, "allbars");
            return true;
        }

        Player player = (Player) sender;
        String mode = args.length == 2 ? args[1].toLowerCase() : "";
        if ("".equals(mode)) {
            if (service.areAllEligibleActive(player)) {
                service.disableAll(player, true);
            } else {
                service.enableAll(player, true);
            }
            return true;
        }

        if ("on".equals(mode)) {
            service.enableAll(player, true);
            return true;
        }

        if ("off".equals(mode)) {
            service.disableAll(player, true);
            return true;
        }

        service.sendUsage(sender, "allbars");
        return true;
    }
}
