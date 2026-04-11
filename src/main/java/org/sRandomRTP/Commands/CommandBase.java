package org.sRandomRTP.Commands;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.sRandomRTP.DifferentMethods.Variables;
import org.sRandomRTP.Files.LoadMessages;
import org.sRandomRTP.Rtp.RtpRtpBase;

import java.util.ArrayList;
import java.util.List;

public class CommandBase extends AbstractRtpCommand {

    public static void commandBase(CommandSender sender) {
        new CommandBase().execute(sender);
    }

    @Override
    protected String requiredPermission() { return Permissions.BASE; }

    @Override
    protected boolean requiresWorldGuardUnconditionally() { return true; }

    @Override
    protected boolean additionalChecks(Player player, CommandSender sender,
                                       World world, boolean loggingEnabled) {
        RegionManager regionManager = WorldGuard.getInstance().getPlatform()
                .getRegionContainer().get(BukkitAdapter.adapt(world));
        if (regionManager == null) {
            Variables.getMessageService().send(sender,LoadMessages.regionManager);
            return false;
        }
        List<ProtectedRegion> filteredRegions = new ArrayList<>();
        for (ProtectedRegion region : regionManager.getRegions().values()) {
            if (!region.getId().equals("__global__")) {
                filteredRegions.add(region);
            }
        }
        if (filteredRegions.isEmpty()) {
            Variables.getMessageService().send(sender,LoadMessages.regionsempty);
            return false;
        }
        return true;
    }

    @Override
    protected Runnable buildAction(CommandSender sender, Player player, World world) {
        return () -> RtpRtpBase.rtpRtpbase(sender, world);
    }
}
