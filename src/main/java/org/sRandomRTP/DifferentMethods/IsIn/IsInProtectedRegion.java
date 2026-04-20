package org.sRandomRTP.DifferentMethods.IsIn;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import org.bukkit.Location;
import org.sRandomRTP.DifferentMethods.LoggerUtility;

public class IsInProtectedRegion {
    public static boolean isInProtectedRegion(Location loc) {
        // Fast guard — if WorldGuard is not loaded, skip its API entirely
        try {
            WorldGuard wg = WorldGuard.getInstance();
            if (wg == null) return false;
            RegionManager regionManager = wg.getPlatform().getRegionContainer()
                    .get(BukkitAdapter.adapt(loc.getWorld()));
            if (regionManager == null) return false;
            BlockVector3 pt = BlockVector3.at(loc.getX(), loc.getY(), loc.getZ());
            ApplicableRegionSet set = regionManager.getApplicableRegions(pt);
            return set.size() > 0;
        } catch (NoClassDefFoundError ignored) {
            // WorldGuard not installed on this server
            return false;
        } catch (RuntimeException e) {
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            String callingClassName = stackTrace[2].getClassName();
            LoggerUtility.loggerUtility(callingClassName, e);
        }
        return false;
    }
}
