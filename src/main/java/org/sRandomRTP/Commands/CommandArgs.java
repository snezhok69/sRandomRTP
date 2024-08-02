package org.sRandomRTP.Commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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
                // ARGUMENT NEAR \\
                case "near":
                    if (args.length < 2) {
                        CommandNear.commandnear(sender);
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
                //ARGUMENT DENY \\
                case "deny":
                    if (args.length < 2) {
                        Player targetPlayer = (Player) sender;
                        if (!targetPlayer.hasPermission("sRandomRTP.Command.Deny")) {
                            List<String> formattedMessage = LoadMessages.nopermissioncommand;
                            for (String line : formattedMessage) {
                                String formattedLine = TranslateRGBColors.translateRGBColors(ChatColor.translateAlternateColorCodes('&', line));
                                sender.sendMessage(formattedLine);
                            }
                            return true;
                        }
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
                        if (!targetPlayer.hasPermission("sRandomRTP.Command.Accept")) {
                            List<String> formattedMessage = LoadMessages.nopermissioncommand;
                            for (String line : formattedMessage) {
                                String formattedLine = TranslateRGBColors.translateRGBColors(ChatColor.translateAlternateColorCodes('&', line));
                                sender.sendMessage(formattedLine);
                            }
                            return true;
                        }
                        if (Variables.teleportfile.getBoolean("teleport.rtp-player-messages")) {
                            if (Variables.playerConfirmStatus.getOrDefault(targetPlayer.getName(), false)) {
                                CooldownBypassBossBarPlayer.startBossBarCountdown(sender, targetPlayer);
                                Variables.playerConfirmStatus.put(targetPlayer.getName(), false);
                            } else {
                                sender.sendMessage(Variables.pluginName + " §cNo teleportation request found or it has expired.");
                            }
                        } else {
                            sender.sendMessage(Variables.pluginName + " §cThe command does not work because §a'rtp-player-messages: false'!");
                        }
                    } else {
                        sender.sendMessage(Variables.pluginName + " §cInvalid command!");
                    }
                    break;
                // ARGUMENT CANCEL \
                case "cancel":
                    if (args.length < 2) {
                        CommandCancel.commandRtpCancel(sender);
                    } else {
                        sender.sendMessage(Variables.pluginName + " §cInvalid command!");
                    }
                    break;
                // ARGUMENT PLAYER \\
                case "player":
                    if (args.length > 0) {
                        if (args[0].equalsIgnoreCase("player")) {
                            if (args.length == 2) {
                                Player targetPlayer = Bukkit.getPlayer(args[1]);
                                if (targetPlayer == null) {
                                    sender.sendMessage(ChatColor.RED + "Player not found!");
                                    return true;
                                }
                                if (targetPlayer != null && !targetPlayer.hasPermission("sRandomRTP.Command.Player")) {
                                    List<String> formattedMessage = LoadMessages.nopermissioncommand;
                                    for (String line : formattedMessage) {
                                        String formattedLine = TranslateRGBColors.translateRGBColors(ChatColor.translateAlternateColorCodes('&', line));
                                        sender.sendMessage(formattedLine);
                                    }
                                    return true;
                                }
                                if (targetPlayer.equals(sender)) {
                                    sender.sendMessage(ChatColor.RED + "You cannot teleport yourself!");
                                    return true;
                                }
                                Variables.senderSendMessage.put(targetPlayer.getName(), sender);
                                CommandPlayer.commandplayer(sender, targetPlayer);
                                if (!targetPlayer.hasPermission("sRandomRTP.Cooldown.bypass")) {
                                    Variables.playerConfirmStatus.put(targetPlayer.getName(), true);
                                } else {
                                }
                                return true;
                            } else {
                                if (sender != null && !sender.hasPermission("sRandomRTP.Command.Player")) {
                                    List<String> formattedMessage = LoadMessages.nopermissioncommand;
                                    for (String line : formattedMessage) {
                                        String formattedLine = TranslateRGBColors.translateRGBColors(ChatColor.translateAlternateColorCodes('&', line));
                                        sender.sendMessage(formattedLine);
                                    }
                                    return true;
                                }
                                sender.sendMessage(ChatColor.RED + "Invalid command usage! Use: /rtp player <nickname>");
                                return true;
                            }
                        }
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
                        CommandWorld.commandWorld(sender, worldName);
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
