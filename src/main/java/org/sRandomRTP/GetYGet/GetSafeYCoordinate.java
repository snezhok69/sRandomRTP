package org.sRandomRTP.GetYGet;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.sRandomRTP.DifferentMethods.LoggerUtility;

public class GetSafeYCoordinate {
    public static int getSafeYCoordinate(World world, int x, int z) {
        try {
            for (int y = world.getMaxHeight() - 1; y > 0; y--) {
                Block block = world.getBlockAt(x, y, z);
                Block blockAbove = world.getBlockAt(x, y + 1, z);
                Block blockTwoAbove = world.getBlockAt(x, y + 2, z);

                // Проверяем, есть ли над этим местом открытое небо
                if (block.getLightFromSky() > 0) {
                    // Проверка на безопасные блоки
                    if (block.getType().isSolid() && blockAbove.getType() == Material.AIR && blockTwoAbove.getType() == Material.AIR) {
                        return y + 1; // Возвращаем блок выше найденного безопасного блока
                    }
                }
            }
        } catch (Throwable e) {
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            String callingClassName = stackTrace[2].getClassName();
            LoggerUtility.loggerUtility(callingClassName, e);
        }
        return -1; // No safe spot found
    }

    public static class CoordinateWithBiome {
        public final int y;
        public final Biome biome;

        public CoordinateWithBiome(int y, Biome biome) {
            this.y = y;
            this.biome = biome;
        }
    }

    public static CoordinateWithBiome getSafeYCoordinateWithAirCheck(World world, int x, int z) {
        try {
            int y = getSafeYCoordinate(world, x, z);
            if (y == -1) return null;

            while (y < world.getMaxHeight() - 2) {
                Block blockAbove = world.getBlockAt(x, y + 1, z);
                Block blockTwoAbove = world.getBlockAt(x, y + 2, z);

                if (blockAbove.getType() == Material.AIR && blockTwoAbove.getType() == Material.AIR) {
                    Biome biome = world.getBiome(x, y, z); // Получаем биом на новой высоте
                    return new CoordinateWithBiome(y, biome);
                }
                y++;
            }

            return null;
        } catch (Throwable e) {
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            String callingClassName = stackTrace[2].getClassName();
            LoggerUtility.loggerUtility(callingClassName, e);
        }
        return null;
    }

    public static int getSafeYCoordinateFromBottom(World world, int x, int z) {
        try {
            for (int y = 0; y < world.getMaxHeight() - 2; y++) {
                Block block = world.getBlockAt(x, y, z);
                Block blockAbove = world.getBlockAt(x, y + 1, z);
                Block blockTwoAbove = world.getBlockAt(x, y + 2, z);
                if (block.getLightFromSky() > 0) {
                    if (block.getType().isSolid() && blockAbove.getType() == Material.AIR && blockTwoAbove.getType() == Material.AIR) {
                        return y + 1; // Возвращаем блок выше найденного безопасного блока
                    }
                }
            }
        } catch (Throwable e) {
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            String callingClassName = stackTrace[2].getClassName();
            LoggerUtility.loggerUtility(callingClassName, e);
        }
        return -1; // No safe spot found
    }
}
