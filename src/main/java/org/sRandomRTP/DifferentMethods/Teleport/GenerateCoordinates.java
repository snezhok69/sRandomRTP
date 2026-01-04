package org.sRandomRTP.DifferentMethods.Teleport;

import org.bukkit.Bukkit;
import org.sRandomRTP.DifferentMethods.Variables;

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
        int newX, newZ;
        String generationDetails = "";

        if (generationMethod == null) {
            generationMethod = "CIRCLE";
            if (Variables.getInstance().getConfig().getBoolean("logs", false)) {
                Bukkit.getConsoleSender().sendMessage("Warning: generationMethod was null, defaulting to CIRCLE");
            }
        }

        int effectiveMinRadius = Math.max(0, Math.min(minRadius, radius));
        int effectiveMaxRadius = Math.max(effectiveMinRadius, radius);
        if ((effectiveMinRadius != minRadius || effectiveMaxRadius != radius) &&
                Variables.getInstance().getConfig().getBoolean("logs", false)) {
            Bukkit.getConsoleSender().sendMessage("Adjusted radii for generation: min=" + effectiveMinRadius + ", max=" + effectiveMaxRadius);
        }

        boolean deterministic = attemptNumber > 0;
        long baseSeed = playerName != null ? playerName.hashCode() : 0L;
        long combinedSeed = mixSeeds(baseSeed, sessionNonce);
        SplittableRandom seedRandom = new SplittableRandom(combinedSeed);
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
                randomDistance = effectiveMinRadius + Math.random() * (effectiveMaxRadius - effectiveMinRadius);
            }

            if (generationMethod.equalsIgnoreCase("SQUARE")) {
                int side = (int) (Math.random() * 4);
                String sideName;

                switch (side) {
                    case 0:
                        newX = centerX + (int) ((Math.random() * 2 - 1) * effectiveMaxRadius);
                        newZ = centerZ - (int) randomDistance;
                        sideName = "top";
                        break;
                    case 1:
                        newX = centerX + (int) randomDistance;
                        newZ = centerZ + (int) ((Math.random() * 2 - 1) * effectiveMaxRadius);
                        sideName = "right";
                        break;
                    case 2:
                        newX = centerX + (int) ((Math.random() * 2 - 1) * effectiveMaxRadius);
                        newZ = centerZ + (int) randomDistance;
                        sideName = "bottom";
                        break;
                    default:
                        newX = centerX - (int) randomDistance;
                        newZ = centerZ + (int) ((Math.random() * 2 - 1) * effectiveMaxRadius);
                        sideName = "left";
                        break;
                }
                generationDetails = "SQUARE (absolute), side: " + sideName + ", distance: " + (int) randomDistance;
            } else {
                double angle = Math.random() * 2 * Math.PI;
                newX = centerX + (int) (randomDistance * Math.cos(angle));
                newZ = centerZ + (int) (randomDistance * Math.sin(angle));
                generationDetails = "CIRCLE (absolute), angle: " + Math.toDegrees(angle) + "°, distance: " + (int) randomDistance;
            }

        } else {
            double maxRadiusD = effectiveMaxRadius;
            double minRadiusD = effectiveMinRadius;
            double minSquared = minRadiusD * minRadiusD;
            double radiusDiff = Math.max(maxRadiusD * maxRadiusD - minSquared, 0);

            if (generationMethod.equalsIgnoreCase("SQUARE")) {
                int side = (int) (Math.random() * 4);
                String sideName;

                int randomValue = (effectiveMaxRadius <= effectiveMinRadius)
                        ? effectiveMinRadius
                        : effectiveMinRadius + (int) (Math.random() * (effectiveMaxRadius - effectiveMinRadius));

                switch (side) {
                    case 0:
                        newX = centerX + (int) ((Math.random() * 2 - 1) * effectiveMaxRadius);
                        newZ = centerZ - randomValue;
                        sideName = "top";
                        break;
                    case 1:
                        newX = centerX + randomValue;
                        newZ = centerZ + (int) ((Math.random() * 2 - 1) * effectiveMaxRadius);
                        sideName = "right";
                        break;
                    case 2:
                        newX = centerX + (int) ((Math.random() * 2 - 1) * effectiveMaxRadius);
                        newZ = centerZ + randomValue;
                        sideName = "bottom";
                        break;
                    default:
                        newX = centerX - randomValue;
                        newZ = centerZ + (int) ((Math.random() * 2 - 1) * effectiveMaxRadius);
                        sideName = "left";
                        break;
                }
                generationDetails = "SQUARE (relative), side: " + sideName + ", random value: " + randomValue;
            } else {
                double angle = Math.random() * 2 * Math.PI;
                double randomRadius = Math.sqrt(minSquared + Math.random() * radiusDiff);
                newX = centerX + (int) (randomRadius * Math.cos(angle));
                newZ = centerZ + (int) (randomRadius * Math.sin(angle));
                generationDetails = "CIRCLE (relative), angle: " + Math.toDegrees(angle) + "°, random radius: " + (int) randomRadius;
            }
        }

        if (Variables.getInstance().getConfig().getBoolean("logs", false)) {
            Bukkit.getConsoleSender().sendMessage("Coordinate generation details: " + generationDetails);
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

    private static long mixSeeds(long baseSeed, long sessionNonce) {
        long mixed = baseSeed ^ sessionNonce;
        mixed ^= (mixed >>> 30);
        mixed *= 0xbf58476d1ce4e5b9L;
        mixed ^= (mixed >>> 27);
        mixed *= 0x94d049bb133111ebL;
        mixed ^= (mixed >>> 31);
        return mixed;
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
}
