package org.sRandomRTP.Services;

import org.sRandomRTP.DifferentMethods.Variables;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public final class ConfigChangeReporter {

    private static final SimpleDateFormat STAMP = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private ConfigChangeReporter() {
    }

    public static void record(String filePath, String action, List<String> details) {
        if (Variables.getInstance() == null) {
            return;
        }
        if (!Variables.isDiagnosticEnabled()) {
            return;
        }
        File diagnostics = new File(Variables.getInstance().getDataFolder(), "Diagnostics");
        if (!diagnostics.exists() && !diagnostics.mkdirs()) {
            return;
        }
        File report = new File(diagnostics, "config-changes.txt");
        try (PrintWriter writer = new PrintWriter(new FileWriter(report, true))) {
            writer.println("[" + STAMP.format(new Date()) + "] " + filePath + " - " + action);
            if (details != null) {
                for (String detail : details) {
                    if (detail != null && !detail.trim().isEmpty()) {
                        writer.println("  - " + detail);
                    }
                }
            }
        } catch (IOException e) {
            Variables.getInstance().getLogger().warning("Failed to write config change report: " + e.getMessage());
        }
    }
}
