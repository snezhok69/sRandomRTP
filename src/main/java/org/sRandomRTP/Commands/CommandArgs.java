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
import org.sRandomRTP.DifferentMethods.Teleport.CompatibleTeleport;
import org.sRandomRTP.DifferentMethods.Teleport.RegionTaskExecutor;
import org.sRandomRTP.DifferentMethods.Text.TranslateRGBColors;
import org.sRandomRTP.Files.LoadMessages;
import org.sRandomRTP.Services.RuntimeStateRegistry;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class CommandArgs implements CommandExecutor {

    private static void sendInvalidCommand(CommandSender sender) {
        sender.sendMessage(Variables.pluginName + " §cInvalid command!");
    }

    private static boolean requirePlayerSender(CommandSender sender) {
        if (sender instanceof Player) {
            return true;
        }
        Variables.sendPlayersOnly(sender);
        return false;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        try {
            RuntimeStateRegistry state = Variables.getRuntimeState();
            if (args.length == 0) {
                Map<String, Map<String, Object>> commands = Variables.getInstance().getDescription().getCommands();
                if (commands.containsKey(label.toLowerCase())) {
                    CommandRtp.commandRtp(sender);
                    return true;
                }
                sender.sendMessage(Variables.pluginName + " §cInvalid command!");
                return false;
            }
            String subCommand = args[0].toLowerCase();
            switch (subCommand) {
                case "reload":
                    if (args.length < 2) {
                        CommandReload.commandReload(sender);
                    } else {
                        sendInvalidCommand(sender);
                    }
                    break;
                case "far":
                    if (args.length < 2) {
                        CommandFar.commandFar(sender);
                    } else {
                        sendInvalidCommand(sender);
                    }
                    break;
                case "biome":
                    if (!requirePlayerSender(sender)) {
                        return false;
                    }
                    if (args.length >= 2) {
                        String[] biomeArgs = Arrays.copyOfRange(args, 1, args.length);
                        CommandRtpBiome.commandRtpBiome(sender, biomeArgs);
                    } else {
                        sender.sendMessage(Variables.pluginName + " " + CommandRtpBiome.BIOME_USAGE);
                    }
                    break;
                case "middle":
                    if (args.length < 2) {
                        CommandMiddle.commandMiddle(sender);
                    } else {
                        sendInvalidCommand(sender);
                    }
                    break;
                case "tpsbar":
                case "rambar":
                case "msptbar":
                case "allbars":
                    CommandAdminBar.handle(sender, args);
                    break;
                case "portal":
                    PortalCommandSupport.handle(sender, args);
                    break;
                case "chunky":
                    if (args.length < 2) {
                        sender.sendMessage(Variables.pluginName + " §cUsage: /rtp chunky <radius> or /rtp chunky stop");
                    } else if (args.length == 2) {
                        if (args[1].equalsIgnoreCase("stop")) {
                            ChunkyCancelCommand.chunkyCancelCommand(sender, args);
                        } else {
                            ChunkyCommand.chunkyCommand(sender, args);
                        }
                    } else {
                        sender.sendMessage(Variables.pluginName + " §cWrong team! Use: /rtp chunky <radius> or /rtp chunky stop");
                    }
                    break;
                case "near":
                    return handleNearCommand(sender, args, state);
                case "base":
                    if (args.length < 2) {
                        CommandBase.commandBase(sender);
                    } else {
                        sendInvalidCommand(sender);
                    }
                    break;
                case "help":
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
                    return handleConfirmResponse(sender, args, state, false);
                case "accept":
                    return handleConfirmResponse(sender, args, state, true);
                case "cancel":
                    if (args.length < 2) {
                        CommandCancel.commandRtpCancel(sender);
                    } else {
                        sendInvalidCommand(sender);
                    }
                    break;
                case "player":
                    return handlePlayerCommand(sender, args, state);
                case "back":
                    if (args.length < 2) {
                        CommandBack.handleBackCommand(sender);
                    } else {
                        sendInvalidCommand(sender);
                    }
                    break;
                case "world":
                    return handleWorldCommand(sender, args, state);
                case "version":
                    if (args.length < 2) {
                        CommandVersion.commandVersion(sender);
                    } else {
                        sendInvalidCommand(sender);
                    }
                    break;
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

        state.getTargetWorlds().put(player.getName(), nearBwResult.world);
        CompatibleTeleport.teleport(
                player,
                nearBwResult.world.getSpawnLocation(),
                PlayerTeleportEvent.TeleportCause.PLUGIN,
                Variables.isLoggingEnabled(),
                "near redirect"
        ).whenComplete((success, throwable) -> RegionTaskExecutor.runAtEntity(player, () -> {
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
        if (!Variables.cachedRtpPlayerMessages) {
            sender.sendMessage(Variables.pluginName + " §cThe command does not work because §a'rtp-player-messages: false'!");
            return true;
        }

        if (!accept) {
            CooldownBypassBossBarPlayer.denyTeleport(sender, targetPlayer);
            state.getPlayerConfirmStatus().put(targetPlayer.getName(), false);
            return true;
        }

        if (!state.getPlayerConfirmStatus().containsKey(targetPlayer.getName())) {
            Variables.getMessageService().send(sender, LoadMessages.no_active_requests_accept);
            return true;
        }
        if (!state.getPlayerConfirmStatus().getOrDefault(targetPlayer.getName(), false)) {
            Variables.getMessageService().send(sender, LoadMessages.no_active_requests_accept);
            return true;
        }

        World world = state.getTargetWorlds().getOrDefault(targetPlayer.getName(), targetPlayer.getWorld());
        if (Variables.teleportfile.getBoolean("teleport.bannedworld.enabled")) {
            List<String> bannedWorlds = Variables.teleportfile.getStringList("teleport.bannedworld.worlds");
            if (bannedWorlds.contains(world.getName())) {
                state.clearPendingPlayerRouting(targetPlayer.getName());
                Variables.getMessageService().send(sender, LoadMessages.no_active_requests_accept);
                return true;
            }
        }
        CooldownBypassBossBarPlayer.startBossBarCountdown(sender, targetPlayer, world);
        state.getPlayerConfirmStatus().put(targetPlayer.getName(), false);
        state.getTargetWorlds().remove(targetPlayer.getName());
        return true;
    }

    private boolean handlePlayerCommand(CommandSender sender, String[] args, RuntimeStateRegistry state) {
        if (args.length < 2 || args.length > 3) {
            Variables.sendError(sender, "Invalid command usage! Use: /rtp player <nickname> [world]");
            return true;
        }

        Player targetPlayer = Bukkit.getPlayer(args[1]);
        if (targetPlayer == null) {
            Variables.sendError(sender, "Player not found!");
            return true;
        }

        World targetWorld = null;
        if (args.length == 3) {
            targetWorld = Bukkit.getWorld(args[2]);
            if (targetWorld == null) {
                Variables.sendError(sender, "World not found!");
                return true;
            }
        }

        World effectiveWorld = targetWorld != null
                ? targetWorld
                : state.getTargetWorlds().getOrDefault(targetPlayer.getName(), targetPlayer.getWorld());

        if (Variables.teleportfile.getBoolean("teleport.bannedworld.enabled")) {
            List<String> bannedWorlds = Variables.teleportfile.getStringList("teleport.bannedworld.worlds");
            if (bannedWorlds.contains(effectiveWorld.getName())) {
                if (Variables.teleportfile.getBoolean("teleport.bannedworld.redirect.enabled")) {
                    String redirectWorldName = Variables.teleportfile.getString("teleport.bannedworld.redirect.world");
                    World redirectWorld = Bukkit.getWorld(redirectWorldName);
                    if (redirectWorld != null && !bannedWorlds.contains(redirectWorld.getName())) {
                        String originalWorldName = effectiveWorld.getName();
                        effectiveWorld = redirectWorld;
                        state.getTargetWorlds().put(targetPlayer.getName(), redirectWorld);
                        Variables.getMessageService().send(targetPlayer, LoadMessages.redirect_world,
                                "%from_world%", originalWorldName, "%to_world%", redirectWorld.getName());
                    } else {
                        Variables.getMessageService().send(sender, LoadMessages.banned_world_sender,
                                "%player%", targetPlayer.getName(), "%world%", effectiveWorld.getName());
                        Variables.getMessageService().send(targetPlayer, LoadMessages.banned_world, "%world%", effectiveWorld.getName());
                        state.clearPendingPlayerRouting(targetPlayer.getName());
                        return true;
                    }
                } else {
                    Variables.getMessageService().send(sender, LoadMessages.banned_world_sender,
                            "%player%", targetPlayer.getName(), "%world%", effectiveWorld.getName());
                    Variables.getMessageService().send(targetPlayer, LoadMessages.banned_world, "%world%", effectiveWorld.getName());
                    state.clearPendingPlayerRouting(targetPlayer.getName());
                    return true;
                }
            }
        }

        state.getTargetWorlds().put(targetPlayer.getName(), effectiveWorld);
        state.getSenderSendMessage().put(targetPlayer.getName(), sender);
        CommandPlayer.commandPlayer(sender, targetPlayer, effectiveWorld);
        return true;
    }

    private boolean handleWorldCommand(CommandSender sender, String[] args, RuntimeStateRegistry state) {
        if (args.length != 2) {
            Variables.sendError(sender, "Invalid command! Usage: /rtp world <world>");
            return true;
        }

        String worldName = args[1];
        World targetWorld = Bukkit.getWorld(worldName);
        if (targetWorld == null) {
            Variables.sendError(sender, "World not found!");
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
            state.getTargetWorlds().put(worldPlayer.getName(), worldBwResult.world);
        }
        CommandWorld.commandWorld(sender, worldBwResult.world.getName());
        return true;
    }
}
