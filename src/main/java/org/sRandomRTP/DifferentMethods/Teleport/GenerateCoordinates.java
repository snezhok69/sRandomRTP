package org.sRandomRTP.DifferentMethods.Teleport;

import org.bukkit.Bukkit;
import org.sRandomRTP.DifferentMethods.Variables;
import org.sRandomRTP.Services.RngProvider;
import org.sRandomRTP.Services.TeleportMetrics;
import org.sRandomRTP.Utils.CoordinateUtils;

import java.util.SplittableRandom;

public class GenerateCoordinates {
    private static final double GOLDEN_ANGLE_RADIANS = Math.toRadians(137.50776405003785);
    private static final double GOLDEN_RATIO_CONJUGATE = (Math.sqrt(5.0) - 1.0) / 2.0;

    public static int[] generateCoordinates(int centerX, int centerZ, int radius, int minRadius,
                                            String generationMethod, boolean useAbsoluteCoordinates) {
        return generateCoordinates(null, -1, 0L, centerX, centerZ, radius, minRadius, generationMethod, useAbsoluteCoordinates);
    }

    public static int[] generateCoordinates(String playerName, int attemptNumber, int centerX, int centerZ, int radius,
                                            int minRadius, String generationMethod, boolean useAbsoluteCoordinates) {
        return generateCoordinates(playerName, attemptNumber, 0L, centerX, centerZ, radius, minRadius, generationMethod, useAbsoluteCoordinates);
    }

    public static int[] generateCoordinates(String playerName, int attemptNumber, long sessionNonce, int centerX, int centerZ, int radius,
                                            int minRadius, String generationMethod, boolean useAbsoluteCoordinates) {
        long startedAt = System.nanoTime();
        int newX, newZ;
        String generationDetails = "";
        boolean loggingEnabled = Variables.isLoggingEnabled();
        // Cache RNG provider reference — Variables.getRngProvider() is called 15+ times below
        RngProvider rng = Variables.getRngProvider();

        if (generationMethod == null) {
            generationMethod = "CIRCLE";
            if (loggingEnabled) {
                Bukkit.getConsoleSender().sendMessage("Warning: generationMethod was null, defaulting to CIRCLE");
            }
        }

        int effectiveMinRadius = Math.max(0, Math.min(minRadius, radius));
        int effectiveMaxRadius = Math.max(effectiveMinRadius, radius);
        if ((effectiveMinRadius != minRadius || effectiveMaxRadius != radius) && loggingEnabled) {
            Bukkit.getConsoleSender().sendMessage("Adjusted radii for generation: min=" + effectiveMinRadius + ", max=" + effectiveMaxRadius);
        }

        boolean deterministic = attemptNumber > 0;
        long baseSeed = playerName != null ? playerName.hashCode() : 0L;
        long combinedSeed = CoordinateUtils.mixSeeds(baseSeed, sessionNonce);
        SplittableRandom seedRandom = rng.deterministic(combinedSeed);
        double angleSeed = seedRandom.nextDouble();
        double radialSeed = seedRandom.nextDouble();

        if (deterministic && !generationMethod.equalsIgnoreCase("SQUARE")) {
            double angle = normalizeAngle(angleSeed * Math.PI * 2 + attemptNumber * GOLDEN_ANGLE_RADIANS);
            double radialFactor = fractionalPart(radialSeed + attemptNumber * GOLDEN_RATIO_CONJUGATE);
            double randomRadius = computeRadius(radialFactor, effectiveMinRadius, effectiveMaxRadius, useAbsoluteCoordinates);

            newX = centerX + (int) Math.round(randomRadius * Math.cos(angle));
            newZ = centerZ + (int) Math.round(randomRadius * Math.sin(angle));
            generationDetails = String.format("CIRCLE (golden), angle: %.2f°, radius: %d",
                    Math.toDegrees(angle), (int) randomRadius);

        } else if (deterministic) {
            double sequence = fractionalPart(radialSeed + attemptNumber * GOLDEN_RATIO_CONJUGATE);
            int side = Math.min(3, (int) Math.floor(sequence * 4));
            double distanceFactor = fractionalPart(angleSeed + attemptNumber * GOLDEN_RATIO_CONJUGATE * 0.5);
            int randomValue = computeLinearDistanceRange(distanceFactor, effectiveMinRadius, effectiveMaxRadius);
            String sideName;

            switch (side) {
                case 0:
                    newX = centerX + computeSignedDistance(fractionalPart(angleSeed + attemptNumber * 0.25),
                            -effectiveMaxRadius, effectiveMaxRadius);
                    newZ = centerZ - randomValue;
                    sideName = "top";
                    break;
                case 1:
                    newX = centerX + randomValue;
                    newZ = centerZ + computeSignedDistance(fractionalPart(angleSeed + attemptNumber * 0.35),
                            -effectiveMaxRadius, effectiveMaxRadius);
                    sideName = "right";
                    break;
                case 2:
                    newX = centerX + computeSignedDistance(fractionalPart(angleSeed + attemptNumber * 0.45),
                            -effectiveMaxRadius, effectiveMaxRadius);
                    newZ = centerZ + randomValue;
                    sideName = "bottom";
                    break;
                default:
                    newX = centerX - randomValue;
                    newZ = centerZ + computeSignedDistance(fractionalPart(angleSeed + attemptNumber * 0.55),
                            -effectiveMaxRadius, effectiveMaxRadius);
                    sideName = "left";
                    break;
            }
            generationDetails = "SQUARE (golden), side: " + sideName + ", distance: " + randomValue;

        } else if (useAbsoluteCoordinates) {
            double randomDistance = effectiveMinRadius;
            if (effectiveMaxRadius > effectiveMinRadius) {
                randomDistance = rng.nextDouble(effectiveMinRadius, effectiveMaxRadius);
            }

            if (generationMethod.equalsIgnoreCase("SQUARE")) {
                int[] sq = squareXZ(centerX, centerZ, rng.nextInt(4), (int) randomDistance, effectiveMaxRadius, rng);
                newX = sq[0]; newZ = sq[1];
                generationDetails = "SQUARE (absolute), side: " + sideName(sq[2]) + ", distance: " + (int) randomDistance;
            } else {
                double angle = rng.nextDouble(0.0D, Math.PI * 2);
                newX = centerX + (int) (randomDistance * Math.cos(angle));
                newZ = centerZ + (int) (randomDistance * Math.sin(angle));
                generationDetails = "CIRCLE (absolute), angle: " + Math.toDegrees(angle) + "°, distance: " + (int) randomDistance;
            }

        } else {
            double minRadiusD = effectiveMinRadius;
            double minSquared = minRadiusD * minRadiusD;
            double radiusDiff = Math.max((double) effectiveMaxRadius * effectiveMaxRadius - minSquared, 0);

            if (generationMethod.equalsIgnoreCase("SQUARE")) {
                int randomValue = (effectiveMaxRadius <= effectiveMinRadius)
                        ? effectiveMinRadius
                        : rng.nextInt(effectiveMinRadius, effectiveMaxRadius);
                int[] sq = squareXZ(centerX, centerZ, rng.nextInt(4), randomValue, effectiveMaxRadius, rng);
                newX = sq[0]; newZ = sq[1];
                generationDetails = "SQUARE (relative), side: " + sideName(sq[2]) + ", random value: " + randomValue;
            } else {
                double angle = rng.nextDouble(0.0D, Math.PI * 2);
                double randomRadius = Math.sqrt(minSquared + rng.nextDouble() * radiusDiff);
                newX = centerX + (int) (randomRadius * Math.cos(angle));
                newZ = centerZ + (int) (randomRadius * Math.sin(angle));
                generationDetails = "CIRCLE (relative), angle: " + Math.toDegrees(angle) + "°, random radius: " + (int) randomRadius;
            }
        }

        // Clamp to safe Minecraft world bounds to prevent integer overflow near world edge
        final int WORLD_LIMIT = 29_999_984;
        newX = Math.max(-WORLD_LIMIT, Math.min(WORLD_LIMIT, newX));
        newZ = Math.max(-WORLD_LIMIT, Math.min(WORLD_LIMIT, newZ));

        if (loggingEnabled) {
            Bukkit.getConsoleSender().sendMessage("Coordinate generation details: " + generationDetails);
        }
        TeleportMetrics metrics = Variables.getTeleportMetrics();
        if (metrics != null) {
            metrics.recordCoordinateGeneration(System.nanoTime() - startedAt);
        }
        return new int[]{newX, newZ};
    }

    private static double computeRadius(double radialFactor, int minRadius, int maxRadius, boolean useAbsolute) {
        if (maxRadius <= minRadius) {
            return minRadius;
        }
        if (useAbsolute) {
            return minRadius + radialFactor * (maxRadius - minRadius);
        }
        double minSquared = (double) minRadius * minRadius;
        double maxSquared = (double) maxRadius * maxRadius;
        double radiusSquared = minSquared + radialFactor * (maxSquared - minSquared);
        return Math.sqrt(radiusSquared);
    }

    private static int computeLinearDistanceRange(double factor, int minValue, int maxValue) {
        if (maxValue <= minValue) {
            return minValue;
        }
        double scaled = minValue + factor * (maxValue - minValue);
        return (int) Math.round(scaled);
    }

    private static int computeSignedDistance(double factor, int negativeMax, int positiveMax) {
        double scaled = negativeMax + factor * (positiveMax - negativeMax);
        return (int) Math.round(scaled);
    }

    private static double normalizeAngle(double angle) {
        double twoPi = Math.PI * 2;
        angle = angle % twoPi;
        if (angle < 0) {
            angle += twoPi;
        }
        return angle;
    }

    private static double fractionalPart(double value) {
        return value - Math.floor(value);
    }

    /**
     * Assigns X/Z coordinates for a single side of a square shape.
     *
     * <p>Returns a 3-element int array {@code [newX, newZ, sideIndex]} where {@code sideIndex}
     * encodes the side name: 0=top, 1=right, 2=bottom, 3=left. Encoding as an int avoids a
     * String allocation in the non-logging fast path (caller converts only when logging is on).
     *
     * @param side      0..3 selected randomly by the caller
     * @param distValue radial distance along the main axis for this side
     * @param maxRadius full extent used for the perpendicular (jitter) axis
     */
    private static int[] squareXZ(int centerX, int centerZ, int side, int distValue, int maxRadius, RngProvider rng) {
        int newX, newZ;
        switch (side) {
            case 0:
                newX = centerX + (int) Math.round(rng.nextDouble(-maxRadius, maxRadius));
                newZ = centerZ - distValue;
                break;
            case 1:
                newX = centerX + distValue;
                newZ = centerZ + (int) Math.round(rng.nextDouble(-maxRadius, maxRadius));
                break;
            case 2:
                newX = centerX + (int) Math.round(rng.nextDouble(-maxRadius, maxRadius));
                newZ = centerZ + distValue;
                break;
            default:
                newX = centerX - distValue;
                newZ = centerZ + (int) Math.round(rng.nextDouble(-maxRadius, maxRadius));
                break;
        }
        return new int[]{newX, newZ, side};
    }

    /** Returns a human-readable side name for logging purposes only. */
    private static String sideName(int side) {
        switch (side) {
            case 0: return "top";
            case 1: return "right";
            case 2: return "bottom";
            default: return "left";
        }
    }
}
