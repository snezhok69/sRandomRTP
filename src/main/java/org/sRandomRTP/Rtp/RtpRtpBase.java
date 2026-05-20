package org.sRandomRTP.Rtp;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.sRandomRTP.DifferentMethods.*;
import org.sRandomRTP.DifferentMethods.Teleport.TeleportRequestContext;
import org.sRandomRTP.DifferentMethods.Teleport.TeleportRequestManager;
import org.sRandomRTP.DifferentMethods.Text.TranslateRGBColors;
import org.sRandomRTP.Files.LoadMessages;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.bukkit.ChatColor.translateAlternateColorCodes;
import org.sRandomRTP.Utils.ChatUtils;

public class RtpRtpBase {
    public static void rtpRtpbase(CommandSender sender, World targetWorld) {
        new Handler().launchRtp(sender, targetWorld);
    }

    private static class Handler extends AbstractRtpHandler {
        /** Holds the region selected for the current attempt; refreshed each attempt in preAttemptChecks. */
        private final AtomicReference<ProtectedRegion> selectedRegion = new AtomicReference<>();

        @Override
        protected LaunchParams buildLaunchParams(Player player, World world, boolean loggingEnabled) {
            int maxAttempts = Math.max(1, Variables.getPluginContext().getConfigRegistry().getTeleportFile().getInt("teleport.maxtries"));
            // Region-based RTP ignores radius entirely; pass distinct dummy values so
            // the radius==minRadius guard in AbstractRtpHandler.attemptCoordinate() never fires.
            return new LaunchParams(0, 0, 100, 0, maxAttempts, false);
        }

        @Override
        protected boolean preAttemptChecks(Player player, World world, TeleportRequestContext ctx,
                                           boolean loggingEnabled, int attemptNumber,
                                           int maxAttempts, Runnable retryCallback) {
            RegionManager regionManager = WorldGuard.getInstance()
                    .getPlatform().getRegionContainer().get(BukkitAdapter.adapt(world));
            if (regionManager == null) {
                if (loggingEnabled) {
                    Bukkit.getConsoleSender().sendMessage("RegionManager is null for world: " + world.getName());
                }
                player.sendMessage(ChatUtils.PLUGIN_NAME + " §8- §cError getting regions. Please check diagnostics.");
                TeleportRequestManager.cancelRequest(player.getUniqueId(), loggingEnabled, "no region manager");
                Variables.getRuntimeState().setPlayerSearching(player, false);
                return false;
            }

            List<ProtectedRegion> regions = regionManager.getRegions().values().stream()
                    .filter(r -> !r.getId().equalsIgnoreCase("__global__"))
                    .collect(Collectors.toList());

            if (regions.isEmpty()) {
                Variables.getMessageService().send(player, LoadMessages.regionsempty);
                TeleportRequestManager.cancelRequest(player.getUniqueId(), loggingEnabled, "no regions");
                Variables.getRuntimeState().setPlayerSearching(player, false);
                return false;
            }

            selectedRegion.set(regions.get(Variables.getRngProvider().nextInt(regions.size())));
            return true;
        }

        @Override
        protected int[] generateXZ(Player player, World world, int ignoredCenterX, int ignoredCenterZ,
                                   int ignoredRadius, int ignoredMinRadius, int generationIndex,
                                   long sessionNonce, String method, boolean absolute,
                                   boolean loggingEnabled, int attemptNumber) {
            ProtectedRegion region = selectedRegion.get();
            if (region == null) return null;

            BlockVector3 min = region.getMinimumPoint();
            BlockVector3 max = region.getMaximumPoint();
            int regionRadius = Variables.getPluginContext().getConfigRegistry().getTeleportFile().getInt("teleport.regionradius");

            int centerX = worldCenterX(world);
            int centerZ = worldCenterZ(world);

            boolean offsetXPositive = Variables.getRngProvider().nextInt(2) == 0;
            boolean offsetZPositive = Variables.getRngProvider().nextInt(2) == 0;

            int newX = offsetXPositive
                    ? max.getBlockX() + 1 + Variables.getRngProvider().nextInt(Math.max(1, regionRadius))
                    : min.getBlockX() - 1 - Variables.getRngProvider().nextInt(Math.max(1, regionRadius));

            int newZ = offsetZPositive
                    ? max.getBlockZ() + 1 + Variables.getRngProvider().nextInt(Math.max(1, regionRadius))
                    : min.getBlockZ() - 1 - Variables.getRngProvider().nextInt(Math.max(1, regionRadius));

            if (newX == 0 && newZ == 0) {
                if (loggingEnabled) {
                    Bukkit.getConsoleSender().sendMessage("Teleportation attempt #" + attemptNumber
                            + " failed due to unsafe location (coordinates 0,0).");
                }
                return null;
            }

            if (loggingEnabled) {
                double distance = Math.hypot(newX - centerX, newZ - centerZ);
                Bukkit.getConsoleSender().sendMessage("Generated coordinates near region: X=" + newX
                        + ", Z=" + newZ + ", Distance from center: " + (int) distance + " blocks");
            }

            return new int[]{newX, newZ};
        }
    }
}
