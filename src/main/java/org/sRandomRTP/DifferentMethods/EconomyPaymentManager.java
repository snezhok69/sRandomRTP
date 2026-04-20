package org.sRandomRTP.DifferentMethods;

import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class EconomyPaymentManager {

    private static final Map<UUID, Payment> pendingPayments = new ConcurrentHashMap<>();

    private EconomyPaymentManager() {
    }

    public static boolean chargePlayer(Player payer, Player teleportedPlayer, double amount) {
        if (payer == null || teleportedPlayer == null) {
            return false;
        }
        // Zero-cost teleport: treat as success without touching Vault or creating a payment entry.
        if (amount <= 0) {
            return true;
        }
        if (Variables.getPluginContext().getEconomy() == null) {
            return false;
        }

        // Guard against double-charge: refuse if a pending payment already exists for this player
        if (pendingPayments.containsKey(teleportedPlayer.getUniqueId())) {
            return false;
        }

        EconomyResponse response = Variables.getPluginContext().getEconomy().withdrawPlayer(payer, amount);
        if (!response.transactionSuccess()) {
            return false;
        }

        pendingPayments.put(
                teleportedPlayer.getUniqueId(),
                new Payment(payer.getUniqueId(), amount)
        );
        return true;
    }

    public static void confirmSuccess(Player teleportedPlayer) {
        if (teleportedPlayer == null) {
            return;
        }
        pendingPayments.remove(teleportedPlayer.getUniqueId());
    }

    public static void refund(Player teleportedPlayer) {
        if (teleportedPlayer == null) {
            return;
        }
        refund(teleportedPlayer.getUniqueId());
    }

    public static void refund(UUID teleportedPlayerId) {
        if (teleportedPlayerId == null || Variables.getPluginContext() == null || Variables.getPluginContext().getEconomy() == null) {
            return;
        }

        Payment payment = pendingPayments.remove(teleportedPlayerId);
        if (payment == null) {
            return;
        }

        OfflinePlayer payer = Bukkit.getOfflinePlayer(payment.payerId());
        Variables.getPluginContext().getEconomy().depositPlayer(payer, payment.amount());
        if (Variables.getTeleportMetrics() != null) {
            Variables.getTeleportMetrics().recordRefund();
        }
    }

    private record Payment(UUID payerId, double amount) {}
}
