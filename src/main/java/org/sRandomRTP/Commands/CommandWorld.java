package org.sRandomRTP.Commands;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.sRandomRTP.DifferentMethods.Variables;
import org.sRandomRTP.Utils.ChatUtils;
import org.sRandomRTP.Files.LoadMessages;
import org.sRandomRTP.Rtp.AbstractRtpHandler;

public class CommandWorld extends AbstractRtpCommand {

    private static final String ADVANCEMENT_NETHER = "story/enter_the_nether";
    private static final String ADVANCEMENT_END    = "story/enter_the_end";

    private final World requestedWorld;

    private CommandWorld(World requestedWorld) {
        this.requestedWorld = requestedWorld;
    }

    public static void commandWorld(CommandSender sender, String worldName) {
        World targetWorld = Bukkit.getWorld(worldName);
        if (targetWorld == null) {
            ChatUtils.sendError(sender, "World §4'" + worldName + "' §cdoes not exist!");
            return;
        }
        new CommandWorld(targetWorld).execute(sender);
    }

    @Override
    protected String requiredPermission() {
        return Permissions.WORLD;
    }

    @Override
    protected World initialWorld(Player player) {
        return requestedWorld != null ? requestedWorld : player.getWorld();
    }

    @Override
    protected boolean additionalChecks(Player player, CommandSender sender, World world, boolean loggingEnabled) {
        if (world.getEnvironment() == World.Environment.NETHER
                && !hasAdvancement(player, ADVANCEMENT_NETHER, LoadMessages.noadvancementnether)) {
            return false;
        }
        return world.getEnvironment() != World.Environment.THE_END
                || hasAdvancement(player, ADVANCEMENT_END, LoadMessages.noadvancementend);
    }

    @Override
    protected Runnable buildAction(CommandSender sender, Player player, World world) {
        return () -> AbstractRtpHandler.launchForPlayer(player, world);
    }

    private boolean hasAdvancement(Player player, String key, java.util.List<String> denialMessage) {
        boolean enabled = key.contains("nether")
                ? Variables.configCache.netherAchievementEnabled
                : Variables.configCache.endAchievementEnabled;
        if (!enabled) {
            return true;
        }
        Advancement advancement = Bukkit.getAdvancement(NamespacedKey.minecraft(key));
        if (advancement == null) {
            return true;
        }
        AdvancementProgress progress = player.getAdvancementProgress(advancement);
        if (progress.isDone()) {
            return true;
        }
        Variables.getMessageService().send(player, denialMessage);
        return false;
    }
}
