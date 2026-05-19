package org.sRandomRTP.Services;

import org.sRandomRTP.DifferentMethods.Variables;
import org.sRandomRTP.Main;

import java.io.File;
import java.util.Locale;

public final class LocalFeatureGate {

    public static final String ADMIN_BARS_PROPERTY = "srandomrtp.local-admin-bars";
    public static final String ADMIN_BARS_ENV = "SRANDOMRTP_LOCAL_ADMIN_BARS";
    public static final String ADMIN_BARS_MARKER = ".srandomrtp-local-admin-bars";
    public static final String ADMIN_BARS_READABLE_MARKER = "local-admin-bars.enabled";

    private LocalFeatureGate() {
    }

    public static boolean isLocalAdminBarsEnabled() {
        if (isTruthy(System.getProperty(ADMIN_BARS_PROPERTY))) {
            return true;
        }
        if (isTruthy(System.getenv(ADMIN_BARS_ENV))) {
            return true;
        }

        Main plugin = Variables.getInstance();
        File dataFolder = plugin == null ? null : plugin.getDataFolder();
        return dataFolder != null
                && (new File(dataFolder, ADMIN_BARS_MARKER).isFile()
                || new File(dataFolder, ADMIN_BARS_READABLE_MARKER).isFile());
    }

    static boolean isTruthy(String value) {
        if (value == null) {
            return false;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return "true".equals(normalized)
                || "yes".equals(normalized)
                || "on".equals(normalized)
                || "1".equals(normalized);
    }
}
