package org.sRandomRTP.Commands;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.sRandomRTP.BlockBiomes.BiomeBlockValidator;
import org.sRandomRTP.DifferentMethods.Text.TranslateRGBColors;
import org.sRandomRTP.DifferentMethods.Variables;
import org.sRandomRTP.Files.LoadMessages;
import org.sRandomRTP.Rtp.BiomeConfigurableRtpHandler;

import java.util.*;

public class CommandRtpBiome extends AbstractRtpCommand {

    static final String BIOME_USAGE = "§cUsage: /rtp biome <biome[, biome2, ...]> or /rtp biome <biome1 biome2 ...>";

    private final List<Biome> biomes;

    private CommandRtpBiome(List<Biome> biomes) {
        this.biomes = biomes;
    }

    public static void commandRtpBiome(CommandSender sender, String[] biomeArgs) {
        if (!(sender instanceof Player)) {
            Variables.sendPlayersOnly(sender);
            return;
        }
        List<Biome> requestedBiomes = parseBiomes(sender, biomeArgs);
        if (requestedBiomes == null || requestedBiomes.isEmpty()) return;
        new CommandRtpBiome(Collections.unmodifiableList(requestedBiomes)).execute(sender);
    }

    @Override
    protected String requiredPermission() { return Permissions.RTP_BIOME; }

    @Override
    protected boolean clearsTeleportFlags() { return true; }

    @Override
    protected boolean usesBiomeCooldown() { return true; }

    @Override
    protected Runnable buildAction(CommandSender sender, Player player, World world) {
        return () -> BiomeConfigurableRtpHandler.rtpBiome(sender, world,
                biomes == null ? Collections.emptyList() : new ArrayList<>(biomes));
    }

    static List<Biome> parseBiomes(CommandSender sender, String[] biomeArgs) {
        List<String> tokens = tokenizeBiomeArguments(biomeArgs);
        if (tokens.isEmpty()) {
            sender.sendMessage(Variables.pluginName + " " + BIOME_USAGE);
            return null;
        }

        List<Biome> resolved = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (String token : tokens) {
            String normalized = normalizeBiomeToken(token);
            if (seen.contains(normalized)) {
                continue;
            }
            Biome biome;
            try {
                biome = Biome.valueOf(normalized);
            } catch (IllegalArgumentException ex) {
                sender.sendMessage(Variables.pluginName + " §cBiome '" + token + "' not found.");
                return null;
            }

            if (BiomeBlockValidator.isBiomeBanned(biome)) {
                sendBannedBiomeMessage(sender, biome);
                return null;
            }

            resolved.add(biome);
            seen.add(normalized);
        }
        return resolved;
    }

    static List<String> tokenizeBiomeArguments(String[] biomeArgs) {
        if (biomeArgs == null || biomeArgs.length == 0) {
            return Collections.emptyList();
        }

        List<String> tokens = new ArrayList<>();
        for (String arg : biomeArgs) {
            if (arg == null) {
                continue;
            }
            String[] split = arg.split(",");
            for (String part : split) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty()) {
                    tokens.add(trimmed);
                }
            }
        }
        return tokens;
    }

    static String normalizeBiomeToken(String token) {
        return token == null ? "" : token.trim()
                .toUpperCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');
    }

    private static void sendBannedBiomeMessage(CommandSender sender, Biome biome) {
        List<String> formattedMessage = LoadMessages.bannedbiome;
        if (formattedMessage == null || formattedMessage.isEmpty()) {
            sender.sendMessage(Variables.pluginName + " §cBiome " + biome.name() + " is banned.");
            return;
        }
        for (String line : formattedMessage) {
            String formattedLine = TranslateRGBColors.translateRGBColors(ChatColor.translateAlternateColorCodes(
                    '&', line.replace("%biome%", biome.name())));
            sender.sendMessage(formattedLine);
        }
    }
}
