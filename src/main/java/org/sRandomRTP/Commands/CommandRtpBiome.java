package org.sRandomRTP.Commands;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.sRandomRTP.BlockBiomes.BiomeBlockValidator;
import org.sRandomRTP.DifferentMethods.Text.TranslateRGBColors;
import org.sRandomRTP.DifferentMethods.Variables;
import org.sRandomRTP.Utils.ChatUtils;
import org.sRandomRTP.Utils.ConfigValueParser;
import org.sRandomRTP.Files.LoadMessages;
import org.sRandomRTP.Rtp.BiomeConfigurableRtpHandler;

import java.util.*;

public class CommandRtpBiome extends AbstractRtpCommand {

    static final String BIOME_USAGE = "§cUsage: /rtp biome <biome|category[, biome2, ...]> or /rtp biome list [page]";
    private static final int BIOMES_PER_PAGE = 12;
    private static final Map<String, List<String>> BIOME_CATEGORIES = buildBiomeCategories();

    private final List<Biome> biomes;

    private CommandRtpBiome(List<Biome> biomes) {
        this.biomes = biomes;
    }

    public static void commandRtpBiome(CommandSender sender, String[] biomeArgs) {
        if (!(sender instanceof Player)) {
            ChatUtils.sendPlayersOnly(sender);
            return;
        }
        if (biomeArgs != null && biomeArgs.length > 0 && "list".equalsIgnoreCase(biomeArgs[0])) {
            int page = biomeArgs.length > 1 ? parsePage(biomeArgs[1]) : 1;
            sendBiomeList(sender, page);
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
            sender.sendMessage(ChatUtils.PLUGIN_NAME + " " + BIOME_USAGE);
            return null;
        }

        List<Biome> resolved = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (String token : tokens) {
            List<String> expanded = expandBiomeToken(token);
            if (expanded.isEmpty()) {
                sender.sendMessage(ChatUtils.PLUGIN_NAME + " §cBiome or category '" + token + "' not found.");
                return null;
            }
            for (String expandedToken : expanded) {
                String normalized = normalizeBiomeToken(expandedToken);
                if (seen.contains(normalized)) {
                    continue;
                }
                Biome biome = ConfigValueParser.parseBiome(expandedToken);
                if (biome == null) {
                    continue;
                }
                normalized = biome.name();

                if (BiomeBlockValidator.isBiomeBanned(biome)) {
                    sendBannedBiomeMessage(sender, biome);
                    return null;
                }

                resolved.add(biome);
                seen.add(normalized);
            }
        }
        return resolved;
    }

    public static Set<String> categoryNames() {
        return BIOME_CATEGORIES.keySet();
    }

    private static List<String> expandBiomeToken(String token) {
        String normalized = normalizeBiomeToken(token).toLowerCase(Locale.ROOT);
        List<String> category = BIOME_CATEGORIES.get(normalized);
        if (category != null) {
            return category;
        }
        Biome biome = ConfigValueParser.parseBiome(token);
        if (biome == null) {
            return Collections.emptyList();
        }
        return Collections.singletonList(biome.name());
    }

    private static void sendBiomeList(CommandSender sender, int requestedPage) {
        Biome[] biomes = Biome.values();
        int totalPages = Math.max(1, (int) Math.ceil(biomes.length / (double) BIOMES_PER_PAGE));
        int page = Math.max(1, Math.min(totalPages, requestedPage));
        sender.sendMessage(ChatUtils.PLUGIN_NAME + " §6Biomes page §f" + page + "§7/§f" + totalPages
                + " §8| §7Categories: §f" + String.join(", ", BIOME_CATEGORIES.keySet()));
        int start = (page - 1) * BIOMES_PER_PAGE;
        int end = Math.min(biomes.length, start + BIOMES_PER_PAGE);
        StringBuilder line = new StringBuilder();
        for (int i = start; i < end; i++) {
            if (line.length() > 0) {
                line.append("§7, §f");
            } else {
                line.append("§f");
            }
            line.append(readableBiomeName(biomes[i]));
        }
        sender.sendMessage(line.toString());
    }

    private static int parsePage(String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException ignored) {
            return 1;
        }
    }

    private static String readableBiomeName(Biome biome) {
        String[] words = biome.name().toLowerCase(Locale.ROOT).split("_");
        StringBuilder builder = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) continue;
            if (builder.length() > 0) builder.append(' ');
            builder.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return builder.toString();
    }

    private static Map<String, List<String>> buildBiomeCategories() {
        Map<String, List<String>> categories = new LinkedHashMap<>();
        categories.put("ocean", Arrays.asList("OCEAN", "DEEP_OCEAN", "WARM_OCEAN", "LUKEWARM_OCEAN",
                "DEEP_LUKEWARM_OCEAN", "COLD_OCEAN", "DEEP_COLD_OCEAN", "FROZEN_OCEAN", "DEEP_FROZEN_OCEAN"));
        categories.put("nether", Arrays.asList("NETHER_WASTES", "CRIMSON_FOREST", "WARPED_FOREST",
                "SOUL_SAND_VALLEY", "BASALT_DELTAS"));
        categories.put("warm", Arrays.asList("DESERT", "SAVANNA", "SAVANNA_PLATEAU", "BADLANDS",
                "WOODED_BADLANDS_PLATEAU", "ERODED_BADLANDS", "JUNGLE", "JUNGLE_EDGE", "BAMBOO_JUNGLE"));
        categories.put("cold", Arrays.asList("SNOWY_TUNDRA", "SNOWY_TAIGA", "SNOWY_TAIGA_HILLS",
                "ICE_SPIKES", "FROZEN_RIVER", "SNOWY_MOUNTAINS", "MOUNTAINS", "GRAVELLY_MOUNTAINS"));
        return Collections.unmodifiableMap(categories);
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
            sender.sendMessage(ChatUtils.PLUGIN_NAME + " §cBiome " + biome.name() + " is banned.");
            return;
        }
        for (String line : formattedMessage) {
            String formattedLine = TranslateRGBColors.translateRGBColors(ChatColor.translateAlternateColorCodes(
                    '&', line.replace("%biome%", biome.name())));
            sender.sendMessage(formattedLine);
        }
    }
}
