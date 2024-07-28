package org.sRandomRTP.DifferentMethods;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import org.bukkit.Location;

public class IsInProtectedRegion {
    public static boolean isInProtectedRegion(Location loc) {
        try {
        RegionManager regionManager = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(loc.getWorld()));
        if (regionManager != null) {
            BlockVector3 pt = BlockVector3.at(loc.getX(), loc.getY(), loc.getZ());
            ApplicableRegionSet set = regionManager.getApplicableRegions(pt);
            return set.size() > 0;
        }
        return false;
        } catch (Throwable e) {
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            String callingClassName = stackTrace[2].getClassName();
            LoggerUtility.loggerUtility(callingClassName, e);
        }
        return false;
    }
}