package org.sRandomRTP.Commands;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.sRandomRTP.DifferentMethods.Variables;
import org.sRandomRTP.Files.LoadKeys;
import org.sRandomRTP.Services.ConfigRegistry;
import org.sRandomRTP.Services.LocalFeatureGate;
import org.sRandomRTP.Services.PluginVersionCatalog;
import org.sRandomRTP.Services.RuntimeStateRegistry;
import org.sRandomRTP.Services.TeleportMetrics;
import org.sRandomRTP.Utils.ChatUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
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
        sender.sendMessage(ChatUtils.PLUGIN_NAME + " §6Runtime stats");
        sender.sendMessage("§7Active searches: §f" + state.getPlayerSearchStatus().size());
        sender.sendMessage("§7Total RTP uses: §f" + state.getRtpCount().get());
        sender.sendMessage("§7Cooldowns: §f" + state.getCooldowns().size()
                + " §8| §7Biome cooldowns: §f" + state.getBiomeCooldowns().size());
        sender.sendMessage("§7Portal tasks: §f" + state.getPlayerPortalsTasks().size()
                + " §8| §7Portal blocks: §f" + state.getPlayerPortalsBlocks().size());
        sender.sendMessage("§7Completed/cancelled/refunds: §f" + metrics.getCompletedRequests()
                + "§7/§f" + metrics.getCancellations() + "§7/§f" + metrics.getRefunds());
        sender.sendMessage("§7Avg coordinate/safeY/chunk ms: §f" + metrics.getCoordinateAverageMillis()
                + "§7/§f" + metrics.getSafeYAverageMillis()
                + "§7/§f" + metrics.getChunkAverageMillis());
    }

    public static void dump(CommandSender sender) {
        if (!CommandUtils.checkPermission(sender, Permissions.DUMP)) return;
        File dump;
        try {
            dump = createSupportDump();
        } catch (IOException e) {
            sender.sendMessage(ChatUtils.PLUGIN_NAME + " §cFailed to create support dump: " + e.getMessage());
            return;
        }
        sender.sendMessage(ChatUtils.PLUGIN_NAME + " §aSupport dump created: §f" + dump.getAbsolutePath());
    }

    static List<String> buildDoctorLines() {
        java.util.ArrayList<String> lines = new java.util.ArrayList<>();
        RuntimeStateRegistry state = Variables.getRuntimeState();
        lines.add(ChatUtils.PLUGIN_NAME + " §6Doctor");
        lines.add("§7Plugin: §f" + Variables.getInstance().getDescription().getVersion()
                + " §8| §7Java: §f" + System.getProperty("java.version"));
        lines.add("§7Server: §f" + Bukkit.getServer().getVersion());
        lines.add("§7Folia: §f" + (Variables.getFoliaLib() != null && Variables.getFoliaLib().isFolia()));
        lines.add("§7Language: §f" + LoadKeys.language);
        lines.add("§7Integrations: §fWorldGuard=" + isPluginEnabled("WorldGuard")
                + ", Vault=" + isPluginEnabled("Vault")
                + ", Chunky=" + isPluginEnabled("Chunky")
                + ", PlaceholderAPI=" + isPluginEnabled("PlaceholderAPI"));
        lines.add("§7Local admin bars gate: §f" + LocalFeatureGate.isLocalAdminBarsEnabled());
        lines.add("§7Active searches: §f" + state.getPlayerSearchStatus().size()
                + " §8| §7Portal tasks: §f" + state.getPlayerPortalsTasks().size());
        lines.add("§7Config versions: §f" + configVersionSummary());
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
