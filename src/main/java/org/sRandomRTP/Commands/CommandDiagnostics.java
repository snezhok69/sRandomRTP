package org.sRandomRTP.Commands;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.sRandomRTP.DifferentMethods.Variables;
import org.sRandomRTP.Files.LoadKeys;
import org.sRandomRTP.Files.LoadMessages;
import org.sRandomRTP.Services.ConfigRegistry;
import org.sRandomRTP.Services.PluginVersionCatalog;
import org.sRandomRTP.Services.RuntimeStateRegistry;
import org.sRandomRTP.Services.TeleportMetrics;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class CommandDiagnostics {

    private static final SimpleDateFormat FILE_STAMP = new SimpleDateFormat("yyyyMMdd-HHmmss");

    private CommandDiagnostics() {
    }

    public static void doctor(CommandSender sender) {
        if (!CommandUtils.checkPermission(sender, Permissions.DOCTOR)) return;
        for (String line : buildDoctorLines()) {
            sender.sendMessage(line);
        }
    }

    public static void stats(CommandSender sender) {
        if (!CommandUtils.checkPermission(sender, Permissions.STATS)) return;
        RuntimeStateRegistry state = Variables.getRuntimeState();
        TeleportMetrics metrics = Variables.getTeleportMetrics();
        Variables.getMessageService().send(sender, LoadMessages.diagnostics_stats_lines,
                "%active_searches%", String.valueOf(state.getPlayerSearchStatus().size()),
                "%rtp_uses%", String.valueOf(state.getRtpCount().get()),
                "%cooldowns%", String.valueOf(state.getCooldowns().size()),
                "%biome_cooldowns%", String.valueOf(state.getBiomeCooldowns().size()),
                "%portal_tasks%", String.valueOf(state.getPlayerPortalsTasks().size()),
                "%portal_blocks%", String.valueOf(state.getPlayerPortalsBlocks().size()),
                "%completed%", String.valueOf(metrics.getCompletedRequests()),
                "%cancelled%", String.valueOf(metrics.getCancellations()),
                "%refunds%", String.valueOf(metrics.getRefunds()),
                "%coordinate_avg%", String.valueOf(metrics.getCoordinateAverageMillis()),
                "%safe_y_avg%", String.valueOf(metrics.getSafeYAverageMillis()),
                "%chunk_avg%", String.valueOf(metrics.getChunkAverageMillis()));
    }

    public static void dump(CommandSender sender) {
        if (!CommandUtils.checkPermission(sender, Permissions.DUMP)) return;
        File dump;
        try {
            dump = createSupportDump();
        } catch (IOException e) {
            Variables.getMessageService().send(sender, LoadMessages.diagnostics_dump_failed,
                    "%error%", e.getMessage());
            return;
        }
        Variables.getMessageService().send(sender, LoadMessages.diagnostics_dump_created,
                "%path%", dump.getAbsolutePath());
    }

    static List<String> buildDoctorLines() {
        RuntimeStateRegistry state = Variables.getRuntimeState();
        java.util.ArrayList<String> lines = new java.util.ArrayList<>();
        for (String line : LoadMessages.diagnostics_doctor_lines) {
            lines.add(Variables.getMessageService().format(line,
                    "%plugin_version%", Variables.getInstance().getDescription().getVersion(),
                    "%java_version%", System.getProperty("java.version"),
                    "%server_version%", Bukkit.getServer().getVersion(),
                    "%folia%", String.valueOf(Variables.getFoliaLib() != null && Variables.getFoliaLib().isFolia()),
                    "%language%", LoadKeys.language,
                    "%worldguard%", String.valueOf(isPluginEnabled("WorldGuard")),
                    "%vault%", String.valueOf(isPluginEnabled("Vault")),
                    "%chunky%", String.valueOf(isPluginEnabled("Chunky")),
                    "%placeholderapi%", String.valueOf(isPluginEnabled("PlaceholderAPI")),
                    "%active_searches%", String.valueOf(state.getPlayerSearchStatus().size()),
                    "%portal_tasks%", String.valueOf(state.getPlayerPortalsTasks().size()),
                    "%config_versions%", configVersionSummary()));
        }
        return lines;
    }

    private static File createSupportDump() throws IOException {
        File diagnostics = new File(Variables.getInstance().getDataFolder(), "Diagnostics");
        if (!diagnostics.exists()) {
            diagnostics.mkdirs();
        }
        File dump = new File(diagnostics, "support-dump-" + FILE_STAMP.format(new Date()) + ".zip");
        try (ZipOutputStream zip = new ZipOutputStream(new java.io.FileOutputStream(dump))) {
            addText(zip, "support-info.txt", plain(buildDoctorLines()) + "\n\n" + plainStats());
            addDirectoryFiles(zip, new File(Variables.getInstance().getDataFolder(), "Diagnostics"), "Diagnostics", ".txt", ".log");
            addDirectoryFiles(zip, new File(Variables.getInstance().getDataFolder(), "LogsErrors"), "LogsErrors", ".yml", ".log", ".txt");
        }
        return dump;
    }

    private static String plainStats() {
        RuntimeStateRegistry state = Variables.getRuntimeState();
        TeleportMetrics metrics = Variables.getTeleportMetrics();
        return "Stats\n"
                + "active_searches=" + state.getPlayerSearchStatus().size() + "\n"
                + "rtp_uses=" + state.getRtpCount().get() + "\n"
                + "completed=" + metrics.getCompletedRequests() + "\n"
                + "cancelled=" + metrics.getCancellations() + "\n"
                + "refunds=" + metrics.getRefunds() + "\n"
                + "coordinate_avg_ms=" + metrics.getCoordinateAverageMillis() + "\n"
                + "safe_y_avg_ms=" + metrics.getSafeYAverageMillis() + "\n"
                + "chunk_avg_ms=" + metrics.getChunkAverageMillis() + "\n";
    }

    private static String plain(List<String> lines) {
        StringBuilder builder = new StringBuilder();
        for (String line : lines) {
            builder.append(line.replaceAll("§.", "")).append('\n');
        }
        return builder.toString();
    }

    private static void addText(ZipOutputStream zip, String name, String text) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(text.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private static void addDirectoryFiles(ZipOutputStream zip, File folder, String prefix, String... extensions) throws IOException {
        if (folder == null || !folder.isDirectory()) {
            return;
        }
        File[] files = folder.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (file == null || !file.isFile() || !hasExtension(file.getName(), extensions)) {
                continue;
            }
            zip.putNextEntry(new ZipEntry(prefix + "/" + file.getName()));
            try (FileInputStream in = new FileInputStream(file)) {
                byte[] buffer = new byte[4096];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    zip.write(buffer, 0, read);
                }
            }
            zip.closeEntry();
        }
    }

    private static boolean hasExtension(String name, String... extensions) {
        String lower = name == null ? "" : name.toLowerCase(java.util.Locale.ROOT);
        for (String extension : extensions) {
            if (lower.endsWith(extension)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isPluginEnabled(String name) {
        return Bukkit.getPluginManager().getPlugin(name) != null;
    }

    private static String configVersionSummary() {
        ConfigRegistry registry = Variables.getPluginContext().getConfigRegistry();
        StringBuilder builder = new StringBuilder();
        for (File file : registry.getManagedConfigFiles()) {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
            int version = yaml.getInt(PluginVersionCatalog.CONFIG_VERSION_PATH, 0);
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append(file.getName()).append('=').append(version);
        }
        return builder.toString();
    }
}
