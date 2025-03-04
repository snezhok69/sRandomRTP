package org.sRandomRTP.Commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.sRandomRTP.Cooldowns.CooldownBypassBossBarPlayer;
import org.sRandomRTP.DifferentMethods.*;
import org.sRandomRTP.Files.LoadMessages;

import java.util.List;
import java.util.Map;

public class CommandArgs implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        try {
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
                // ARGUMENT RELOAD \\
                case "reload":
                    if (args.length < 2) {
                        CommandReload.commandReload(sender);
                    } else {
                        sender.sendMessage(Variables.pluginName + " §cInvalid command!");
                    }
                    break;
                // ARGUMENT SETPORTAL \\
                case "setportal":
                    if (args.length == 3) {
                        try {
                            int radius = Integer.parseInt(args[2]);
                            CommandSetPortal.commandSetPortal(sender, radius, args[1], "circle");
                        } catch (NumberFormatException e) {
                            List<String> formattedMessage = LoadMessages.portalradius;
                            for (String line : formattedMessage) {
                                String formattedLine = TranslateRGBColors.translateRGBColors(ChatColor.translateAlternateColorCodes('&', line));
                                sender.sendMessage(formattedLine);
                            }
                        }
                    } else if (args.length == 4) {
                        try {
                            int radius = Integer.parseInt(args[2]);
                            String shape = args[3].toLowerCase();
                            if (!shape.equals("circle") && !shape.equals("square")) {
                                List<String> formattedMessage = LoadMessages.portalform;
                                for (String line : formattedMessage) {
                                    String formattedLine = TranslateRGBColors.translateRGBColors(ChatColor.translateAlternateColorCodes('&', line));
                                    sender.sendMessage(formattedLine);
                                }
                                return true;
                            }
                            CommandSetPortal.commandSetPortal(sender, radius, args[1], shape);
                        } catch (NumberFormatException e) {
                            List<String> formattedMessage = LoadMessages.portalradius;
                            for (String line : formattedMessage) {
                                String formattedLine = TranslateRGBColors.translateRGBColors(ChatColor.translateAlternateColorCodes('&', line));
                                sender.sendMessage(formattedLine);
                            }
                        }
                    } else {
                        sender.sendMessage(Variables.pluginName + " §cUsage: /rtp setportal <name> <radius> [shape]");
                        sender.sendMessage(Variables.pluginName + " §7shape: circle (by default) or square");
                    }
                    break;
                // ARGUMENT DELPORTAL \\
                case "delportal":
                    if (args.length == 2) {
                        CommandDelPortal.commandDelPortal(sender, args[1]);
                    } else {
                        sender.sendMessage(Variables.pluginName + " §cUsage: /rtp delportal <name>");
                    }
                    break;
                // ARGUMENT LISTPORTAL \\
                case "listportal":
                    if (args.length == 2) {
                        CommandDelPortal.commandDelPortal(sender, args[1]);
                    } else {
                        sender.sendMessage(Variables.pluginName + " §cUsage: /rtp listportal <name>");
                    }
                    break;
                // ARGUMENT CHUNKY \\
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
                // ARGUMENT NEAR \\
                case "near":
                    if (args.length < 2) {
                        if (sender instanceof Player) {
                            Player player = (Player) sender;
                            World world = player.getWorld();
                            if (Variables.teleportfile.getBoolean("teleport.bannedworld.enabled")) {
                                List<String> bannedWorlds = Variables.teleportfile.getStringList("teleport.bannedworld.worlds");
                                if (bannedWorlds.contains(world.getName())) {
                                    if (Variables.teleportfile.getBoolean("teleport.bannedworld.redirect.enabled")) {
                                        String redirectWorldName = Variables.teleportfile.getString("teleport.bannedworld.redirect.world");
                                        World redirectWorld = Bukkit.getWorld(redirectWorldName);
                                        if (redirectWorld != null && !bannedWorlds.contains(redirectWorldName)) {
                                            String originalWorldName = world.getName();
                                            List<String> redirectMessages = LoadMessages.redirect_world;
                                            for (String line : redirectMessages) {
                                                String formattedLine = TranslateRGBColors.translateRGBColors(
                                                        ChatColor.translateAlternateColorCodes('&',
                                                                line.replace("%from_world%", originalWorldName)
                                                                        .replace("%to_world%", redirectWorld.getName()))
                                                );
                                                player.sendMessage(formattedLine);
                                            }
                                            if (!redirectWorld.getPlayers().isEmpty()) {
                                                Variables.targetWorlds.put(player.getName(), redirectWorld);
                                                player.teleport(redirectWorld.getSpawnLocation());
                                                CommandNear.commandnear(player);
                                            } else {
                                                List<String> formattedMessage3 = LoadMessages.rederictworldnear_error;
                                                for (String line : formattedMessage3) {
                                                    String formattedLine = TranslateRGBColors.translateRGBColors(ChatColor.translateAlternateColorCodes('&', line));
                                                    player.sendMessage(formattedLine);
                                                }
                                                return true;
                                            }
                                        } else {
                                            List<String> formattedMessage = LoadMessages.banned_world;
                                            for (String line : formattedMessage) {
                                                String formattedLine = TranslateRGBColors.translateRGBColors(
                                                        ChatColor.translateAlternateColorCodes('&',
                                                                line.replace("%world%", world.getName()))
                                                );
                                                player.sendMessage(formattedLine);
                                            }
                                            return true;
                                        }
                                    } else {
                                        List<String> formattedMessage = LoadMessages.banned_world;
                                        for (String line : formattedMessage) {
                                            String formattedLine = TranslateRGBColors.translateRGBColors(
                                                    ChatColor.translateAlternateColorCodes('&',
                                                            line.replace("%world%", world.getName()))
                                            );
                                            player.sendMessage(formattedLine);
                                        }
                                        return true;
                                    }
                                } else {
                                    CommandNear.commandnear(sender);
                                }
                            } else {
                                CommandNear.commandnear(sender);
                            }
                        } else {
                            CommandNear.commandnear(sender);
                        }
                    } else {
                        sender.sendMessage(Variables.pluginName + " §cInvalid command!");
                    }
                    break;
                // ARGUMENT BASE \\
                case "base":
                    if (args.length < 2) {
                        CommandBase.commandbase(sender);
                    } else {
                        sender.sendMessage(Variables.pluginName + " §cInvalid command!");
                    }
                    break;
                // ARGUMENT HELP \\
                case "help":
                    if (args.length < 2) {
                        CommandHelp.commandhelp(sender);
                    } else {
                        sender.sendMessage(Variables.pluginName + " §cInvalid command!");
                    }
                    break;
                // ARGUMENT DENY \\
                case "deny":
                    if (args.length < 2) {
                        Player targetPlayer = (Player) sender;
                        if (Variables.teleportfile.getBoolean("teleport.rtp-player-messages")) {
                            CooldownBypassBossBarPlayer.denyTeleport(sender, targetPlayer);
                            Variables.playerConfirmStatus.put(targetPlayer.getName(), false);
                        } else {
                            sender.sendMessage(Variables.pluginName + " §cThe command does not work because §a'rtp-player-messages: false'!");
                        }
                    } else {
                        sender.sendMessage(Variables.pluginName + " §cInvalid command!");
                    }
                    break;
                // ARGUMENT ACCEPT \\
                case "accept":
                    if (args.length < 2) {
                        Player targetPlayer = (Player) sender;
                        if (Variables.teleportfile.getBoolean("teleport.rtp-player-messages")) {
                            if (!Variables.playerConfirmStatus.containsKey(targetPlayer.getName())) {
                                List<String> formattedMessage = LoadMessages.no_active_requests_accept;
                                for (String line : formattedMessage) {
                                    String formattedLine = TranslateRGBColors.translateRGBColors(
                                            ChatColor.translateAlternateColorCodes('&', line)
                                    );
                                    sender.sendMessage(formattedLine);
                                }
                                return true;
                            }
                            if (Variables.playerConfirmStatus.getOrDefault(targetPlayer.getName(), false)) {
                                World world = Variables.targetWorlds.getOrDefault(targetPlayer.getName(), targetPlayer.getWorld());
                                if (Variables.teleportfile.getBoolean("teleport.bannedworld.enabled")) {
                                    List<String> bannedWorlds = Variables.teleportfile.getStringList("teleport.bannedworld.worlds");
                                    if (bannedWorlds.contains(world.getName())) {
                                        Variables.playerConfirmStatus.remove(targetPlayer.getName());
                                        Variables.commandSenderMap.remove(targetPlayer.getName());
                                        Variables.targetWorlds.remove(targetPlayer.getName());
                                        List<String> formattedMessage = LoadMessages.no_active_requests_accept;
                                        for (String line : formattedMessage) {
                                            String formattedLine = TranslateRGBColors.translateRGBColors(
                                                    ChatColor.translateAlternateColorCodes('&', line)
                                            );
                                            sender.sendMessage(formattedLine);
                                        }
                                        return true;
                                    }
                                }
                                CooldownBypassBossBarPlayer.startBossBarCountdown(sender, targetPlayer, world);
                                Variables.playerConfirmStatus.put(targetPlayer.getName(), false);
                                Variables.targetWorlds.remove(targetPlayer.getName());
                            } else {
                                List<String> formattedMessage = LoadMessages.no_active_requests_accept;
                                for (String line : formattedMessage) {
                                    String formattedLine = TranslateRGBColors.translateRGBColors(
                                            ChatColor.translateAlternateColorCodes('&', line)
                                    );
                                    sender.sendMessage(formattedLine);
                                }
                            }
                        } else {
                            sender.sendMessage(Variables.pluginName + " §cThe command does not work because §a'rtp-player-messages: false'!");
                        }
                    } else {
                        sender.sendMessage(Variables.pluginName + " §cInvalid command!");
                    }
                    break;
                // ARGUMENT CANCEL \\
                case "cancel":
                    if (args.length < 2) {
                        CommandCancel.commandRtpCancel(sender);
                    } else {
                        sender.sendMessage(Variables.pluginName + " §cInvalid command!");
                    }
                    break;
                // ARGUMENT PLAYER \\
                case "player":
                    if (args.length >= 2 && args.length <= 3) {
                        Player targetPlayer = Bukkit.getPlayer(args[1]);
                        if (targetPlayer == null) {
                            sender.sendMessage(ChatColor.RED + "Player not found!");
                            return true;
                        }
                        World targetWorld = null;
                        if (args.length == 3) {
                            targetWorld = Bukkit.getWorld(args[2]);
                            if (targetWorld == null) {
                                sender.sendMessage(ChatColor.RED + "World not found!");
                                return true;
                            }
                        }
                        World effectiveWorld = (targetWorld != null)
                                ? targetWorld
                                : Variables.targetWorlds.getOrDefault(targetPlayer.getName(), targetPlayer.getWorld());
                        if (Variables.teleportfile.getBoolean("teleport.bannedworld.enabled")) {
                            List<String> bannedWorlds = Variables.teleportfile.getStringList("teleport.bannedworld.worlds");
                            if (bannedWorlds.contains(effectiveWorld.getName())) {
                                if (Variables.teleportfile.getBoolean("teleport.bannedworld.redirect.enabled")) {
                                    String redirectWorldName = Variables.teleportfile.getString("teleport.bannedworld.redirect.world");
                                    World redirectWorld = Bukkit.getWorld(redirectWorldName);
                                    if (redirectWorld != null && !bannedWorlds.contains(redirectWorld.getName())) {
                                        String originalWorldName = effectiveWorld.getName();
                                        effectiveWorld = redirectWorld;
                                        Variables.targetWorlds.put(targetPlayer.getName(), redirectWorld);

                                        List<String> redirectMessages = LoadMessages.redirect_world;
                                        for (String line : redirectMessages) {
                                            String formattedLine = TranslateRGBColors.translateRGBColors(
                                                    ChatColor.translateAlternateColorCodes('&',
                                                            line.replace("%from_world%", originalWorldName)
                                                                    .replace("%to_world%", redirectWorld.getName()))
                                            );
                                            targetPlayer.sendMessage(formattedLine);
                                        }
                                    } else {
                                        List<String> senderMessage = LoadMessages.banned_world_sender;
                                        for (String line : senderMessage) {
                                            String formattedLine = TranslateRGBColors.translateRGBColors(
                                                    ChatColor.translateAlternateColorCodes('&',
                                                            line.replace("%player%", targetPlayer.getName())
                                                                    .replace("%world%", effectiveWorld.getName()))
                                            );
                                            sender.sendMessage(formattedLine);
                                        }
                                        List<String> formattedMessage = LoadMessages.banned_world;
                                        for (String line : formattedMessage) {
                                            String formattedLine = TranslateRGBColors.translateRGBColors(
                                                    ChatColor.translateAlternateColorCodes('&',
                                                            line.replace("%world%", effectiveWorld.getName()))
                                            );
                                            targetPlayer.sendMessage(formattedLine);
                                        }
                                        Variables.playerConfirmStatus.remove(targetPlayer.getName());
                                        Variables.commandSenderMap.remove(targetPlayer.getName());
                                        Variables.targetWorlds.remove(targetPlayer.getName());
                                        return true;
                                    }
                                } else {
                                    List<String> senderMessage = LoadMessages.banned_world_sender;
                                    for (String line : senderMessage) {
                                        String formattedLine = TranslateRGBColors.translateRGBColors(
                                                ChatColor.translateAlternateColorCodes('&',
                                                        line.replace("%player%", targetPlayer.getName())
                                                                .replace("%world%", effectiveWorld.getName()))
                                        );
                                        sender.sendMessage(formattedLine);
                                    }
                                    List<String> formattedMessage = LoadMessages.banned_world;
                                    for (String line : formattedMessage) {
                                        String formattedLine = TranslateRGBColors.translateRGBColors(
                                                ChatColor.translateAlternateColorCodes('&',
                                                        line.replace("%world%", effectiveWorld.getName()))
                                        );
                                        targetPlayer.sendMessage(formattedLine);
                                    }
                                    Variables.playerConfirmStatus.remove(targetPlayer.getName());
                                    Variables.commandSenderMap.remove(targetPlayer.getName());
                                    Variables.targetWorlds.remove(targetPlayer.getName());
                                    return true;
                                }
                            }
                        }
                        Variables.targetWorlds.put(targetPlayer.getName(), effectiveWorld);
                        Variables.senderSendMessage.put(targetPlayer.getName(), sender);
                        CommandPlayer.commandplayer(sender, targetPlayer, effectiveWorld);
                        if (!targetPlayer.hasPermission("sRandomRTP.Cooldown.bypass")) {
                            Variables.playerConfirmStatus.put(targetPlayer.getName(), true);
                        }
                        return true;
                    } else {
                        sender.sendMessage(ChatColor.RED + "§a[sRandomRTP] §cInvalid command usage! Use: /rtp player <nickname> [world]");
                    }
                    break;
                // ARGUMENT BACK \\
                case "back":
                    if (args.length < 2) {
                        CommandBack.handleBackCommand(sender);
                    } else {
                        sender.sendMessage(Variables.pluginName + " §cInvalid command!");
                    }
                    break;
                // ARGUMENT WORLD \\
                case "world":
                    if (args.length == 2) {
                        String worldName = args[1];
                        World targetWorld = Bukkit.getWorld(worldName);

                        if (targetWorld == null) {
                            sender.sendMessage("§a[sRandomRTP] §cМир не найден!");
                            return true;
                        }
                        if (Variables.teleportfile.getBoolean("teleport.bannedworld.enabled")) {
                            List<String> bannedWorlds = Variables.teleportfile.getStringList("teleport.bannedworld.worlds");
                            if (bannedWorlds.contains(targetWorld.getName())) {
                                if (Variables.teleportfile.getBoolean("teleport.bannedworld.redirect.enabled")) {
                                    String redirectWorldName = Variables.teleportfile.getString("teleport.bannedworld.redirect.world");
                                    World redirectWorld = Bukkit.getWorld(redirectWorldName);
                                    if (redirectWorld != null && !bannedWorlds.contains(redirectWorld.getName())) {
                                        String originalWorldName = targetWorld.getName();
                                        targetWorld = redirectWorld;
                                        Variables.targetWorlds.put(((Player)sender).getName(), redirectWorld);

                                        List<String> redirectMessages = LoadMessages.redirect_world;
                                        for (String line : redirectMessages) {
                                            String formattedLine = TranslateRGBColors.translateRGBColors(
                                                    ChatColor.translateAlternateColorCodes('&',
                                                            line.replace("%from_world%", originalWorldName)
                                                                    .replace("%to_world%", redirectWorld.getName()))
                                            );
                                            sender.sendMessage(formattedLine);
                                        }
                                        CommandWorld.commandWorld(sender, redirectWorld.getName());
                                    } else {
                                        List<String> formattedMessage = LoadMessages.banned_world;
                                        for (String line : formattedMessage) {
                                            String formattedLine = TranslateRGBColors.translateRGBColors(
                                                    ChatColor.translateAlternateColorCodes('&',
                                                            line.replace("%world%", targetWorld.getName()))
                                            );
                                            sender.sendMessage(formattedLine);
                                        }
                                        return true;
                                    }
                                } else {
                                    List<String> formattedMessage = LoadMessages.banned_world;
                                    for (String line : formattedMessage) {
                                        String formattedLine = TranslateRGBColors.translateRGBColors(
                                                ChatColor.translateAlternateColorCodes('&',
                                                        line.replace("%world%", targetWorld.getName()))
                                        );
                                        sender.sendMessage(formattedLine);
                                    }
                                    return true;
                                }
                            } else {
                                CommandWorld.commandWorld(sender, worldName);
                            }
                        } else {
                            CommandWorld.commandWorld(sender, worldName);
                        }
                    } else {
                        sender.sendMessage("§a[sRandomRTP] §cInvalid command! Usage: /rtp world <world>");
                    }
                    break;
                // ARGUMENT VERSION \\
                case "version":
                    if (args.length < 2) {
                        CommandVersion.commandVersion(sender);
                    } else {
                        sender.sendMessage(Variables.pluginName + " §cInvalid command!");
                    }
                    break;
                default:
                    sender.sendMessage(Variables.pluginName + " §cInvalid command!");
                    break;
            }
            return true;
        } catch (Throwable e) {
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            String callingClassName = stackTrace[2].getClassName();
            LoggerUtility.loggerUtility(callingClassName, e);
        }
        return false;
    }
}