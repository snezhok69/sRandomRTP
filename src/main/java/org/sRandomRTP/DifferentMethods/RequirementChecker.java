package org.sRandomRTP.DifferentMethods;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.sRandomRTP.DifferentMethods.Text.TranslateRGBColors;
import org.sRandomRTP.Files.LoadMessages;

import java.util.List;
import java.util.Map;

/**
 * Centralised pre-teleport requirement checks (money, hunger, levels, health, items).
 * Each check sends the appropriate message to the player on failure.
 *
 * Usage:
 *   int cost = RequirementChecker.checkRequirements(player, loggingEnabled);
 *   if (cost < 0) return;  // failed, message already sent
 *   // ... later ...
 *   EconomyPaymentManager.chargePlayer(player, player, cost);
 */
public final class RequirementChecker {

    private RequirementChecker() {}

    /**
     * Checks all economy/resource requirements before teleportation.
     * Both money and resource (hunger/levels/health/items) checks apply to the same player.
     *
     * @param player         the player to check
     * @param loggingEnabled whether debug logging is active
     * @return teleport money cost (>= 0) when all checks pass; -1 when any check fails
     */
    public static int checkRequirements(Player player, boolean loggingEnabled) {
        return checkRequirements(player, player, loggingEnabled);
    }

    /**
     * Checks all economy/resource requirements before teleportation.
     * Used when an admin teleports another player: money is taken from {@code payer},
     * while resource checks (hunger, levels, health, items) apply to {@code teleported}.
     * If {@code payer} is null, the money check is skipped.
     *
     * @param payer          the player who pays (may be null for console)
     * @param teleported     the player whose resources are checked
     * @param loggingEnabled whether debug logging is active
     * @return teleport money cost (>= 0) when all checks pass; -1 when any check fails
     */
    public static int checkRequirements(Player payer, Player teleported, boolean loggingEnabled) {
        int teleportCost = 0;

        // ── Money (charged from payer) ──────────────────────────────────────
        if (payer != null && Variables.economyfile.getBoolean("teleport.Money.enabled")) {
            if (!Variables.isVaultAvailable) {
                if (loggingEnabled) {
                    org.bukkit.Bukkit.getConsoleSender().sendMessage(
                            "Install the Vault plugin to make the economy function work. " +
                            "Or disable the economy function (Money: enabled: false)");
                }
                payer.sendMessage(ChatColor.RED +
                        "Check the console. If there is nothing in the console, enable logs " +
                        "in the configuration (logs: true) and try teleportation again.");
                return -1;
            }
            if (Variables.econ == null) {
                payer.sendMessage(ChatColor.RED +
                        "Economy service is not available. Contact the server administrator.");
                return -1;
            }
            teleportCost = Variables.economyfile.getInt("teleport.Money.money");
            if (!Variables.econ.has(payer, teleportCost)) {
                sendMessage(payer, LoadMessages.insufficient_funds,
                        "%money%", String.valueOf(teleportCost));
                return -1;
            }
        }

        // ── Hunger (checked against teleported) ────────────────────────────
        if (Variables.economyfile.getBoolean("teleport.Hunger.enabled")) {
            int requiredHunger = Variables.economyfile.getInt("teleport.Hunger.hunger");
            if (teleported.getFoodLevel() < requiredHunger) {
                sendMessage(teleported, LoadMessages.insufficient_hunger,
                        "%hunger%", String.valueOf(requiredHunger));
                return -1;
            }
        }

        // ── Experience levels (checked against teleported) ─────────────────
        if (Variables.economyfile.getBoolean("teleport.Levels.enabled")) {
            int requiredLevel = Variables.economyfile.getInt("teleport.Levels.level");
            if (teleported.getLevel() < requiredLevel) {
                sendMessage(teleported, LoadMessages.insufficient_levels,
                        "%level%", String.valueOf(requiredLevel));
                return -1;
            }
        }

        // ── Health (checked against teleported) ────────────────────────────
        if (Variables.economyfile.getBoolean("teleport.Health.enabled")) {
            double requiredHealth = Variables.economyfile.getDouble("teleport.Health.health");
            if (teleported.getHealth() < requiredHealth) {
                sendMessage(teleported, LoadMessages.insufficient_health,
                        "%health%", String.valueOf(requiredHealth));
                return -1;
            }
        }

        // ── Items (checked against teleported) ─────────────────────────────
        if (Variables.economyfile.getBoolean("teleport.Items.enabled")) {
            boolean hasAllItems = true;
            StringBuilder missingItems = new StringBuilder();
            for (Map.Entry<Material, Integer> entry : Variables.itemMap.entrySet()) {
                int playerItemCount = countItems(teleported, entry.getKey());
                if (playerItemCount < entry.getValue()) {
                    hasAllItems = false;
                    if (missingItems.length() > 0) missingItems.append(", ");
                    missingItems.append(entry.getKey().name())
                                .append(": ")
                                .append(entry.getValue() - playerItemCount);
                }
            }
            if (!hasAllItems) {
                sendMessage(teleported, LoadMessages.insufficient_items,
                        "%items%", missingItems.toString());
                return -1;
            }
        }

        return teleportCost;
    }

    private static int countItems(Player player, org.bukkit.Material material) {
        int count = 0;
        for (org.bukkit.inventory.ItemStack item : player.getInventory()) {
            if (item != null && item.getType() == material) {
                count += item.getAmount();
            }
        }
        return count;
    }

    private static void sendMessage(Player player, List<String> lines,
                                    String placeholder, String value) {
        for (String line : lines) {
            String formatted = TranslateRGBColors.translateRGBColors(
                    ChatColor.translateAlternateColorCodes('&',
                            line.replace(placeholder, value)));
            player.sendMessage(formatted);
        }
    }
}
