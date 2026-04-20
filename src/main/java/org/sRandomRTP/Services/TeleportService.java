package org.sRandomRTP.Services;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.sRandomRTP.Cooldowns.BossBarCountdownEngine;
import org.sRandomRTP.Cooldowns.CooldownManager;
import org.sRandomRTP.DifferentMethods.EconomyPaymentManager;
import org.sRandomRTP.DifferentMethods.Text.TranslateRGBColors;
import org.sRandomRTP.DifferentMethods.Variables;
import org.sRandomRTP.Files.LoadMessages;

public class TeleportService {

    private final MessageService messageService;

    public TeleportService(MessageService messageService) {
        this.messageService = messageService;
    }

    public boolean dispatchStandardTeleport(Player player, CommandSender sender,
                                            boolean useBiomeCooldown, int teleportCost,
                                            Runnable action) {
        if (isAlreadySearching(player, sender)) {
            return false;
        }

        boolean cooldownBlocked = useBiomeCooldown
                ? CooldownManager.checkBiome(player, sender)
                : CooldownManager.checkRtp(player, sender);
        if (cooldownBlocked) {
            return false;
        }

        if (Variables.configCache.moneyEnabled && !EconomyPaymentManager.chargePlayer(player, player, teleportCost)) {
            messageService.send(player, LoadMessages.error_withdrawing);
            return false;
        }

        BossBarCountdownEngine.dispatch(player, sender, action);
        return true;
    }

    public boolean dispatchTargetTeleport(CommandSender sender, Player payer,
                                          Player teleportedPlayer, int teleportCost,
                                          Runnable action) {
        if (isAlreadySearching(teleportedPlayer, sender)) {
            return false;
        }

        if (CooldownManager.checkRtp(teleportedPlayer, sender)) {
            return false;
        }

        if (Variables.configCache.moneyEnabled && payer != null
                && !EconomyPaymentManager.chargePlayer(payer, teleportedPlayer, teleportCost)) {
            messageService.send(payer, LoadMessages.error_withdrawing);
            return false;
        }

        BossBarCountdownEngine.dispatch(teleportedPlayer, sender, action);
        return true;
    }

    private static boolean isAlreadySearching(Player player, CommandSender sender) {
        if (Variables.getRuntimeState().isPlayerSearching(player)) {
            for (String line : LoadMessages.teleportationinprogress) {
                // translateRGBColors already calls ChatColor.translateAlternateColorCodes internally
                sender.sendMessage(TranslateRGBColors.translateRGBColors(line));
            }
            return true;
        }
        return false;
    }
}
