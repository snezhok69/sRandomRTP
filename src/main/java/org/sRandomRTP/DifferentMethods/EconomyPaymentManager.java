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
        if (payer == null || teleportedPlayer == null || amount <= 0) {
            return false;
        }
        if (Variables.econ == null) {
            return false;
        }

        EconomyResponse response = Variables.econ.withdrawPlayer(payer, amount);
        if (!response.transactionSuccess()) {
            return false;
        }

        refund(teleportedPlayer.getUniqueId());

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
        if (teleportedPlayerId == null || Variables.econ == null) {
            return;
        }

        Payment payment = pendingPayments.remove(teleportedPlayerId);
        if (payment == null) {
            return;
        }

        OfflinePlayer payer = Bukkit.getOfflinePlayer(payment.payerId());
        Variables.econ.depositPlayer(payer, payment.amount());
        if (Variables.getTeleportMetrics() != null) {
            Variables.getTeleportMetrics().recordRefund();
        }
    }

    private static final class Payment {
        private final UUID payerId;
        private final double amount;

        private Payment(UUID payerId, double amount) {
            this.payerId = payerId;
            this.amount = amount;
        }

        public UUID payerId() {
            return payerId;
        }

        public double amount() {
            return amount;
        }
    }
}
