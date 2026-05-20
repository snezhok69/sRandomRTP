package org.sRandomRTP.Commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.sRandomRTP.DifferentMethods.LoggerUtility;
import org.sRandomRTP.DifferentMethods.RequirementChecker;
import org.sRandomRTP.DifferentMethods.Variables;
import org.sRandomRTP.Files.LoadMessages;
import org.sRandomRTP.Utils.ChatUtils;


/**
 * Template for all player-only RTP commands.
 *
 * Subclasses implement {@link #requiredPermission()} and {@link #buildAction}
 * and optionally override the hook methods to customise the pipeline.
 *
 * The pipeline executed by {@link #execute(CommandSender)} is:
 * 1. Player-only guard
 * 2. Optional clearTeleportFlags
 * 3. WorldGuard unconditional check (CommandBase)  <-- before permission
 * 4. Permission check
 * 5. WorldGuard conditional check (all other commands)
 * 6. additionalChecks (e.g. RegionManager for CommandBase)
 * 7. RequirementChecker
 * 8. BannedWorldResolver
 * 9. PlayerSearchStatus
 * 10. Cooldown check
 * 11. Economy charge
 * 12. CooldownDispatcher.dispatch
 */
public abstract class AbstractRtpCommand {

    /** The permission node a player must have to use this command. */
    protected abstract String requiredPermission();

    /**
     * Whether WorldGuard must be present unconditionally (CommandBase).
     * When {@code true} the WG check runs BEFORE the permission check and
     * the conditional WG check is skipped.
     * Default: {@code false}.
     */
    protected boolean requiresWorldGuardUnconditionally() { return false; }

    /** Whether {@code Variables.clearTeleportFlags(player)} is called at the start. */
    protected boolean clearsTeleportFlags() { return false; }

    /** Whether to use the biome cooldown map instead of the standard one. */
    protected boolean usesBiomeCooldown() { return false; }

    /**
     * Optional command-specific validation executed after the WG check and
     * permission check but before {@link RequirementChecker}.
     *
     * Send any messages to the player before returning {@code false}.
     * Default implementation always returns {@code true} (no extra check).
     *
     * @param world the player's current world (before any banned-world redirect)
     */
    protected boolean additionalChecks(Player player, CommandSender sender,
                                       World world, boolean loggingEnabled) {
        return true;
    }

    /**
     * Build the teleport action that will be dispatched via {@link CooldownDispatcher}.
     *
     * @param world the effective world after banned-world resolution
     */
    protected abstract Runnable buildAction(CommandSender sender, Player player, World world);

    /**
     * Starting world for command-specific logic before banned-world redirection.
     * Default: player's current world.
     */
    protected World initialWorld(Player player) {
        return player.getWorld();
    }

    // -----------------------------------------------------------------------
    // Pipeline
    // -----------------------------------------------------------------------

    public final void execute(CommandSender sender) {
        try {
            if (!(sender instanceof Player)) {
                ChatUtils.sendPlayersOnly(sender);
                return;
            }
            Player player = (Player) sender;

            if (clearsTeleportFlags()) {
                Variables.clearTeleportFlags(player);
            }

            World world = initialWorld(player);
            boolean loggingEnabled = Variables.isLoggingEnabled();

            // WorldGuard unconditional (CommandBase): before permission check
            if (requiresWorldGuardUnconditionally() && !Variables.getPluginContext().isWorldGuardAvailable()) {
                reportMissingWorldGuard(player, loggingEnabled);
                return;
            }

            // Permission check
            if (!player.hasPermission(requiredPermission())) {
                Variables.getMessageService().send(sender, LoadMessages.nopermissioncommand);
                return;
            }

            // WorldGuard conditional (all commands except CommandBase)
            if (!requiresWorldGuardUnconditionally()
                    && Variables.configCache.checkingInRegions
                    && !Variables.getPluginContext().isWorldGuardAvailable()) {
                reportMissingWorldGuard(player, loggingEnabled);
                return;
            }

            // Command-specific extra checks (e.g. RegionManager for CommandBase)
            if (!additionalChecks(player, sender, world, loggingEnabled)) return;

            // Requirements (items, hunger, levels, health, money prerequisite)
            int teleportCost = RequirementChecker.checkRequirements(player, loggingEnabled);
            if (teleportCost < 0) return;

            // Banned-world resolution (may redirect to a different world)
            BannedWorldResolver.Result bwResult = BannedWorldResolver.resolve(player, world);
            if (!bwResult.ok) return;
            world = bwResult.world;

            World finalWorld = world;
            Variables.getTeleportService().dispatchStandardTeleport(
                    player,
                    sender,
                    usesBiomeCooldown(),
                    teleportCost,
                    buildAction(sender, player, finalWorld)
            );

        } catch (RuntimeException e) {
            LoggerUtility.loggerUtility(AbstractRtpCommand.class, e);
        }
    }

    // -----------------------------------------------------------------------
    // Shared helpers
    // -----------------------------------------------------------------------

    private static void reportMissingWorldGuard(Player player, boolean loggingEnabled) {
        if (loggingEnabled) {
            Bukkit.getConsoleSender().sendMessage(
                    "Install the WorldGuard plugin or disable checking regions "
                    + "in the configuration (checkinginregions: false).");
        }
        player.sendMessage(ChatColor.RED + "Check the console. If there is nothing in the console, "
                + "enable diagnostics in the configuration (diagnostic: true) and try teleportation again.");
    }
}
