package org.sRandomRTP.DifferentMethods;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class RemovePlayerItems {
    public static void removePlayerItems(Player player, Material material, int amount) {
        Inventory inventory = player.getInventory();
        for (ItemStack item : inventory) {
            if (item != null && item.getType() == material) {
                int itemAmount = item.getAmount();
                if (itemAmount <= amount) {
                    inventory.remove(item);
                    amount -= itemAmount;
                } else {
                    item.setAmount(itemAmount - amount);
                    amount = 0;
                }
                if (amount == 0) {
                    break;
                }
            }
        }
    }
}