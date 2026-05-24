package org.sRandomRTP.Utils;

public final class ServerVersionParser {

    private ServerVersionParser() {
    }

    /**
     * Returns the compatibility number used by the plugin:
     * 1.16.x -> 16, 1.21.x -> 21, 26.1.x -> 26.
     */
    public static int parseCompatibilityNumber(String version) {
        if (version == null) {
            return -1;
        }
        String trimmed = version.trim();
        if (trimmed.isEmpty()) {
            return -1;
        }

        String[] dashParts = trimmed.split("-", 2);
        String[] dotParts = dashParts[0].split("\\.");
        if (dotParts.length == 0) {
            return -1;
        }

        int first = parseLeadingInt(dotParts[0]);
        if (first < 0) {
            return -1;
        }

        if (first == 1 && dotParts.length >= 2) {
            int legacyMinor = parseLeadingInt(dotParts[1]);
            return legacyMinor >= 0 ? legacyMinor : -1;
        }

        return first;
    }

    /**
     * Parses CraftBukkit package fragments like v1_20_R3 or v26_1_R1.
     */
    public static int parseCraftBukkitPackageCompatibilityNumber(String versionFragment) {
        if (versionFragment == null) {
            return -1;
        }
        String fragment = versionFragment.trim();
        if (fragment.startsWith("v") || fragment.startsWith("V")) {
            fragment = fragment.substring(1);
        }
        String[] parts = fragment.split("_");
        if (parts.length == 0) {
            return -1;
        }

        int first = parseLeadingInt(parts[0]);
        if (first < 0) {
            return -1;
        }
        if (first == 1 && parts.length >= 2) {
            int legacyMinor = parseLeadingInt(parts[1]);
            return legacyMinor >= 0 ? legacyMinor : -1;
        }
        return first;
    }

    private static int parseLeadingInt(String value) {
        if (value == null) {
            return -1;
        }
        int end = 0;
        while (end < value.length() && Character.isDigit(value.charAt(end))) {
            end++;
        }
        if (end == 0) {
            return -1;
        }
        try {
            return Integer.parseInt(value.substring(0, end));
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
