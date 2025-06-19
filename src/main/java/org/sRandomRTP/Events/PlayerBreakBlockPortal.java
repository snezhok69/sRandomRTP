package org.sRandomRTP.Events;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.sRandomRTP.Commands.CommandSetPortal;
import org.sRandomRTP.DataPortals.PortalDataBlocks;
import org.sRandomRTP.DifferentMethods.Text.TranslateRGBColors;
import org.sRandomRTP.DifferentMethods.Variables;
import org.sRandomRTP.Files.LoadMessages;

import java.util.List;

public class PlayerBreakBlockPortal implements Listener {

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();

        if (isPortalBlock(block) && CommandSetPortal.isPortalBlocksProtected()) {
            event.setCancelled(true);

            List<String> formattedMessage1 = LoadMessages.error_break_portal_block;
            for (String line : formattedMessage1) {
                String formattedLine = TranslateRGBColors.translateRGBColors(ChatColor.translateAlternateColorCodes('&', line));
                event.getPlayer().sendMessage(formattedLine);
            }
        }
    }

    private boolean isPortalBlock(Block block) {
        if (block == null) {
            return false;
        }

        Location blockLocation = block.getLocation();
        String world = blockLocation.getWorld().getName();
        int x = blockLocation.getBlockX();
        int y = blockLocation.getBlockY();
        int z = blockLocation.getBlockZ();

        for (String key : Variables.playerPortalsBlocks.keySet()) {
            PortalDataBlocks portalBlock = Variables.playerPortalsBlocks.get(key);

            if (portalBlock != null &&
                    portalBlock.getWorld().equals(world) &&
                    portalBlock.getX() == x &&
                    portalBlock.getY() == y &&
                    portalBlock.getZ() == z) {
                return true;
            }
        }

        return false;
    }
}