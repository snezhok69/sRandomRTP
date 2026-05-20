package org.sRandomRTP.Services;

import org.sRandomRTP.Main;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class DiagnosticsService {

    private final Main plugin;
    private final File diagnosticsFolder;

    public DiagnosticsService(Main plugin) {
        this.plugin = plugin;
        this.diagnosticsFolder = new File(plugin.getDataFolder(), "Diagnostics");
    }

    public Report startReport(String reportName) {
        return new Report(reportName, new File(diagnosticsFolder, reportName + ".txt"));
    }

    public final class Report {
        private final String reportName;
        private final File reportFile;
        private final List<String> lines = new ArrayList<>();

        private Report(String reportName, File reportFile) {
            this.reportName = reportName;
            this.reportFile = reportFile;
            line("Report: " + reportName);
            line("Started: " + timestamp());
            line("Plugin version: " + plugin.getDescription().getVersion());
            line("Thread: " + Thread.currentThread().getName());
            line("Server: " + plugin.getServer().getName() + " " + plugin.getServer().getVersion());
            line("Java: " + System.getProperty("java.version"));
            line("OS: " + System.getProperty("os.name") + " " + System.getProperty("os.version")
                    + " (" + System.getProperty("os.arch") + ")");
            line("Data folder: " + plugin.getDataFolder().getAbsolutePath());
        }

        public void stepOk(String stepName, String details) {
            line("[OK] " + stepName + (details == null || details.isEmpty() ? "" : " :: " + details));
        }

        public void stepWarn(String stepName, String details) {
            line("[WARN] " + stepName + (details == null || details.isEmpty() ? "" : " :: " + details));
        }

        public void stepFail(String stepName, Throwable throwable) {
            line("[FAIL] " + stepName + " :: " + (throwable == null ? "unknown error" : throwable.toString()));
            Throwable cause = throwable == null ? null : throwable.getCause();
            while (cause != null) {
                line("  caused by: " + cause.toString());
                cause = cause.getCause();
            }
            if (throwable != null) {
                for (StackTraceElement element : throwable.getStackTrace()) {
                    line("  at " + element.toString());
                }
            }
        }

        public void finishSuccess() {
            line("Finished: " + timestamp());
            line("Status: SUCCESS");
            flush();
        }

        public void finishFailure(Throwable throwable) {
            if (throwable != null) {
                stepFail("terminal failure", throwable);
            }
            line("Finished: " + timestamp());
            line("Status: FAILURE");
            flush();
        }

        private void line(String value) {
            lines.add(value);
        }

        private void flush() {
            if (!org.sRandomRTP.DifferentMethods.Variables.isDiagnosticEnabled()) {
                return;
            }
            if (!diagnosticsFolder.exists()) {
                diagnosticsFolder.mkdirs();
            }
            try (PrintWriter writer = new PrintWriter(new FileWriter(reportFile, false))) {
                for (String line : lines) {
                    writer.println(line);
                }
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to write diagnostics report " + reportName + ": " + e.getMessage());
            }
        }

        private String timestamp() {
            return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }
    }
}
