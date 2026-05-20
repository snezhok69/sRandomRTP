package org.sRandomRTP.Commands;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.sRandomRTP.Cooldowns.CooldownBypassBossBarPlayer;
import org.sRandomRTP.DifferentMethods.*;
import org.sRandomRTP.Utils.ChatUtils;
import org.sRandomRTP.DifferentMethods.Teleport.CompatibleTeleport;
import org.sRandomRTP.DifferentMethods.Teleport.FoliaSchedulerFacade;
import org.sRandomRTP.DifferentMethods.Text.TranslateRGBColors;
import org.sRandomRTP.Files.LoadMessages;
import org.sRandomRTP.Services.LocalFeatureGate;
import org.sRandomRTP.Services.RuntimeStateRegistry;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class CommandArgs implements CommandExecutor {

    /**
     * Simple no-arg sub-commands — each takes a single {@link CommandSender}.
     * Avoids 7 near-identical switch cases (args-length check + static dispatch).
     */
    private static final Map<String, Consumer<CommandSender>> SIMPLE_COMMANDS;
    static {
        Map<String, Consumer<CommandSender>> m = new HashMap<>();
        m.put("reload",  CommandReload::commandReload);
        m.put("far",     CommandFar::commandFar);
        m.put("middle",  CommandMiddle::commandMiddle);
        m.put("base",    CommandBase::commandBase);
        m.put("cancel",  CommandCancel::commandRtpCancel);
        m.put("back",    CommandBack::handleBackCommand);
        m.put("version", CommandVersion::commandVersion);
        m.put("doctor",  CommandDiagnostics::doctor);
        m.put("dump",    CommandDiagnostics::dump);
        m.put("stats",   CommandDiagnostics::stats);
        SIMPLE_COMMANDS = Collections.unmodifiableMap(m);
    }

    private static void sendInvalidCommand(CommandSender sender) {
        Variables.getMessageService().send(sender, LoadMessages.invalidcommand);
    }

    private static boolean requirePlayerSender(CommandSender sender) {
        if (sender instanceof Player) {
            return true;
        }
        ChatUtils.sendPlayersOnly(sender);
        return false;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        try {
            RuntimeStateRegistry state = Variables.getRuntimeState();
            if (args.length == 0) {
                if (!CommandFeatureFlag.ensureEnabled(sender, CommandFeatureFlag.RTP)) {
                    return true;
                }
                Map<String, Map<String, Object>> commands = Variables.getInstance().getDescription().getCommands();
                if (commands.containsKey(label.toLowerCase())) {
                    CommandRtp.commandRtp(sender);
                    return true;
                }
                sendInvalidCommand(sender);
                return false;
            }
            String subCommand = args[0].toLowerCase();

            // Simple no-arg commands: dispatch via map (eliminates 7 identical switch cases)
            Consumer<CommandSender> simple = SIMPLE_COMMANDS.get(subCommand);
            if (simple != null) {
                if (!CommandFeatureFlag.ensureEnabled(sender, CommandFeatureFlag.fromSubCommand(subCommand))) {
                    return true;
                }
                if (args.length < 2) {
                    simple.accept(sender);
                } else {
                    sendInvalidCommand(sender);
                }
                return true;
            }

            switch (subCommand) {
                case "settings":
                    return CommandSettings.handle(sender, args);
                case "biome":
                    if (!CommandFeatureFlag.ensureEnabled(sender, CommandFeatureFlag.BIOME)) {
                        return true;
                    }
                    if (!requirePlayerSender(sender)) {
                        return false;
                    }
                    if (args.length >= 2) {
                        String[] biomeArgs = Arrays.copyOfRange(args, 1, args.length);
                        CommandRtpBiome.commandRtpBiome(sender, biomeArgs);
                    } else {
                        sender.sendMessage(ChatUtils.PLUGIN_NAME + " " + CommandRtpBiome.BIOME_USAGE);
                    }
                    break;
                case "tpsbar":
                case "rambar":
                case "msptbar":
                case "allbars":
                    if (LocalFeatureGate.isLocalAdminBarsEnabled()) {
                        if (!CommandFeatureFlag.ensureEnabled(sender, CommandFeatureFlag.fromSubCommand(subCommand))) {
                            return true;
                        }
                        CommandAdminBar.handle(sender, args);
                    } else {
                        sendInvalidCommand(sender);
                    }
                    break;
                case "portal":
                    PortalCommandSupport.handle(sender, args);
                    break;
                case "chunky":
                    if (!CommandFeatureFlag.ensureEnabled(sender, CommandFeatureFlag.CHUNKY)) {
                        return true;
                    }
                    if (args.length < 2) {
                        sender.sendMessage(ChatUtils.PLUGIN_NAME + " §cUsage: /rtp chunky <radius> or /rtp chunky stop");
                    } else if (args.length == 2) {
                        if (args[1].equalsIgnoreCase("stop")) {
                            ChunkyCancelCommand.chunkyCancelCommand(sender, args);
                        } else {
                            ChunkyCommand.chunkyCommand(sender, args);
                        }
                    } else {
                        sender.sendMessage(ChatUtils.PLUGIN_NAME + " §cWrong team! Use: /rtp chunky <radius> or /rtp chunky stop");
                    }
                    break;
                case "near":
                    if (!CommandFeatureFlag.ensureEnabled(sender, CommandFeatureFlag.NEAR)) {
                        return true;
                    }
                    return handleNearCommand(sender, args, state);
                case "help":
                    if (!CommandFeatureFlag.ensureEnabled(sender, CommandFeatureFlag.HELP)) {
                        return true;
                    }
                    if (!requirePlayerSender(sender)) {
                        return false;
                    }
                    if (args.length < 2) {
                        CommandHelp.commandHelp(sender);
                    } else {
                        sendInvalidCommand(sender);
                    }
                    break;
                case "deny":
                    if (!CommandFeatureFlag.ensureEnabled(sender, CommandFeatureFlag.DENY)) {
                        return true;
                    }
                    return handleConfirmResponse(sender, args, state, false);
                case "accept":
                    if (!CommandFeatureFlag.ensureEnabled(sender, CommandFeatureFlag.ACCEPT)) {
                        return true;
                    }
                    return handleConfirmResponse(sender, args, state, true);
                case "player":
                    if (!CommandFeatureFlag.ensureEnabled(sender, CommandFeatureFlag.PLAYER)) {
                        return true;
                    }
                    return handlePlayerCommand(sender, args, state);
                case "world":
                    if (!CommandFeatureFlag.ensureEnabled(sender, CommandFeatureFlag.WORLD)) {
                        return true;
                    }
                    return handleWorldCommand(sender, args, state);
                default:
                    sendInvalidCommand(sender);
                    break;
            }
            return true;
        } catch (RuntimeException e) {
            LoggerUtility.loggerUtility(CommandArgs.class, e);
        }
        return false;
    }

    private boolean handleNearCommand(CommandSender sender, String[] args, RuntimeStateRegistry state) {
        if (args.length >= 2) {
            sendInvalidCommand(sender);
            return true;
        }
        if (!(sender instanceof Player)) {
            CommandNear.commandNear(sender);
            return true;
        }

        Player player = (Player) sender;
        World world = player.getWorld();
        BannedWorldResolver.Result nearBwResult = BannedWorldResolver.resolveForNear(player, world);
        if (!nearBwResult.ok) {
            return true;
        }
        if (nearBwResult.world.getName().equals(world.getName())) {
            CommandNear.commandNear(sender);
            return true;
        }

        state.getTargetWorlds().put(player.getUniqueId(), nearBwResult.world);
        CompatibleTeleport.teleport(
                player,
                nearBwResult.world.getSpawnLocation(),
                PlayerTeleportEvent.TeleportCause.PLUGIN,
                Variables.isLoggingEnabled(),
                "near redirect"
        ).whenComplete((success, throwable) -> FoliaSchedulerFacade.runAtEntity(player, () -> {
            if (throwable != null || !Boolean.TRUE.equals(success)) {
                Variables.getMessageService().send(player,
                        Collections.singletonList("&cTeleport to redirect world failed. Check LogsErrors/latest-error.log"));
                return;
            }
            CommandNear.commandNear(sender);
        }));
        return true;
    }

    private boolean handleConfirmResponse(CommandSender sender, String[] args, RuntimeStateRegistry state, boolean accept) {
        if (!requirePlayerSender(sender)) {
            return false;
        }
        if (args.length >= 2) {
            sendInvalidCommand(sender);
            return true;
        }

        Player targetPlayer = (Player) sender;
        if (!Variables.configCache.rtpPlayerMessages) {
            sender.sendMessage(ChatUtils.PLUGIN_NAME + " §cThe command does not work because §a'rtp-player-messages: false'!");
            return true;
        }

        if (!accept) {
            CooldownBypassBossBarPlayer.denyTeleport(sender, targetPlayer);
            state.getPlayerConfirmStatus().put(targetPlayer.getUniqueId(), false);
            return true;
        }

        if (!state.getPlayerConfirmStatus().containsKey(targetPlayer.getUniqueId())) {
            Variables.getMessageService().send(sender, LoadMessages.no_active_requests_accept);
            return true;
        }
        if (!state.getPlayerConfirmStatus().getOrDefault(targetPlayer.getUniqueId(), false)) {
            Variables.getMessageService().send(sender, LoadMessages.no_active_requests_accept);
            return true;
        }

        World world = state.getTargetWorlds().getOrDefault(targetPlayer.getUniqueId(), targetPlayer.getWorld());
        if (Variables.configCache.bannedWorldEnabled
                && Variables.configCache.bannedWorlds.contains(world.getName())) {
            state.clearPendingPlayerRouting(targetPlayer.getUniqueId());
            Variables.getMessageService().send(sender, LoadMessages.no_active_requests_accept);
            return true;
        }
        CooldownBypassBossBarPlayer.startBossBarCountdown(sender, targetPlayer, world);
        state.getPlayerConfirmStatus().put(targetPlayer.getUniqueId(), false);
        state.getTargetWorlds().remove(targetPlayer.getUniqueId());
        return true;
    }

    private boolean handlePlayerCommand(CommandSender sender, String[] args, RuntimeStateRegistry state) {
        if (args.length < 2 || args.length > 3) {
            ChatUtils.sendError(sender, "Invalid command usage! Use: /rtp player <nickname> [world]");
            return true;
        }

        Player targetPlayer = Bukkit.getPlayer(args[1]);
        if (targetPlayer == null) {
            ChatUtils.sendError(sender, "Player not found!");
            return true;
        }

        World targetWorld = null;
        if (args.length == 3) {
            targetWorld = Bukkit.getWorld(args[2]);
            if (targetWorld == null) {
                ChatUtils.sendError(sender, "World not found!");
                return true;
            }
        }

        World effectiveWorld = targetWorld != null
                ? targetWorld
                : state.getTargetWorlds().getOrDefault(targetPlayer.getUniqueId(), targetPlayer.getWorld());

        org.sRandomRTP.Services.ConfigCache tpCache = Variables.configCache;
        if (tpCache.bannedWorldEnabled && tpCache.bannedWorlds.contains(effectiveWorld.getName())) {
            if (tpCache.bannedWorldRedirectEnabled) {
                World redirectWorld = Bukkit.getWorld(tpCache.bannedWorldRedirectWorld);
                if (redirectWorld != null && !tpCache.bannedWorlds.contains(redirectWorld.getName())) {
                    String originalWorldName = effectiveWorld.getName();
                    effectiveWorld = redirectWorld;
                    state.getTargetWorlds().put(targetPlayer.getUniqueId(), redirectWorld);
                    Variables.getMessageService().send(targetPlayer, LoadMessages.redirect_world,
                            "%from_world%", originalWorldName, "%to_world%", redirectWorld.getName());
                } else {
                    Variables.getMessageService().send(sender, LoadMessages.banned_world_sender,
                            "%player%", targetPlayer.getName(), "%world%", effectiveWorld.getName());
                    Variables.getMessageService().send(targetPlayer, LoadMessages.banned_world, "%world%", effectiveWorld.getName());
                    state.clearPendingPlayerRouting(targetPlayer.getUniqueId());
                    return true;
                }
            } else {
                Variables.getMessageService().send(sender, LoadMessages.banned_world_sender,
                        "%player%", targetPlayer.getName(), "%world%", effectiveWorld.getName());
                Variables.getMessageService().send(targetPlayer, LoadMessages.banned_world, "%world%", effectiveWorld.getName());
                state.clearPendingPlayerRouting(targetPlayer.getUniqueId());
                return true;
            }
        }

        state.getTargetWorlds().put(targetPlayer.getUniqueId(), effectiveWorld);
        state.getSenderSendMessage().put(targetPlayer.getUniqueId(), sender);
        CommandPlayer.commandPlayer(sender, targetPlayer, effectiveWorld);
        return true;
    }

    private boolean handleWorldCommand(CommandSender sender, String[] args, RuntimeStateRegistry state) {
        if (args.length != 2) {
            ChatUtils.sendError(sender, "Invalid command! Usage: /rtp world <world>");
            return true;
        }

        String worldName = args[1];
        World targetWorld = Bukkit.getWorld(worldName);
        if (targetWorld == null) {
            ChatUtils.sendError(sender, "World not found!");
            return true;
        }

        if (!(sender instanceof Player)) {
            CommandWorld.commandWorld(sender, worldName);
            return true;
        }

        Player worldPlayer = (Player) sender;
        BannedWorldResolver.Result worldBwResult = BannedWorldResolver.resolve(worldPlayer, targetWorld);
        if (!worldBwResult.ok) {
            return true;
        }
        if (!worldBwResult.world.getName().equals(targetWorld.getName())) {
            state.getTargetWorlds().put(worldPlayer.getUniqueId(), worldBwResult.world);
        }
        CommandWorld.commandWorld(sender, worldBwResult.world.getName());
        return true;
    }
}
