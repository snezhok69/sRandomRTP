package org.sRandomRTP.Commands;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.sRandomRTP.DifferentMethods.Variables;
import org.sRandomRTP.Files.LoadMessages;

import java.util.List;

/**
 * Centralised banned-world resolution logic.
 * Replaces the 40-line copy-pasted block that previously appeared
 * in every Command* class.
 */
public final class BannedWorldResolver {

    private BannedWorldResolver() {}

    /** Outcome of a banned-world check. */
    public static final class Result {
        /** True if the teleport should proceed (possibly to a redirected world). */
        public final boolean ok;
        /**
         * The world to use for the teleport.
         * Equals the input world when {@code ok} is true and no redirect occurred.
         * Equals the redirect world when a redirect happened.
         * Null when {@code ok} is false.
         */
        public final World world;

        private Result(boolean ok, World world) {
            this.ok = ok;
            this.world = world;
        }

        static Result blocked()              { return new Result(false, null); }
        static Result allow(World world)     { return new Result(true, world); }
    }

    /**
     * Standard banned-world resolution used by CommandRtp, CommandFar,
     * CommandMiddle, CommandBase, and CommandRtpBiome.
     *
     * <ul>
     *   <li>If banned-world feature is disabled → {@code allow(world)}</li>
     *   <li>If world is not banned → {@code allow(world)}</li>
     *   <li>If world is banned and a valid redirect exists → sends redirect message, {@code allow(redirectWorld)}</li>
     *   <li>Otherwise → sends banned-world message, {@code blocked()}</li>
     * </ul>
     */
    public static Result resolve(Player player, World world) {
        return resolveInternal(player, world, false);
    }

    /**
     * Variant for the {@code near} sub-command.
     *
     * Same as {@link #resolve} but adds an extra check: the redirect world must
     * have at least one online player. If it is empty, sends the
     * {@code rederictworldnear_error} message and returns {@code blocked()}.
     */
    public static Result resolveForNear(Player player, World world) {
        return resolveInternal(player, world, true);
    }

    /**
     * Shared implementation for {@link #resolve} and {@link #resolveForNear}.
     *
     * @param requirePlayersInRedirect when {@code true}, the redirect world must
     *                                 have at least one online player (near variant)
     */
    private static Result resolveInternal(Player player, World world, boolean requirePlayersInRedirect) {
        org.sRandomRTP.Services.ConfigCache cache = Variables.configCache;
        if (!cache.bannedWorldEnabled) {
            return Result.allow(world);
        }
        List<String> bannedWorlds = cache.bannedWorlds;
        if (!bannedWorlds.contains(world.getName())) {
            return Result.allow(world);
        }
        if (cache.bannedWorldRedirectEnabled) {
            String redirectWorldName = cache.bannedWorldRedirectWorld;
            World redirectWorld = Bukkit.getWorld(redirectWorldName);
            if (redirectWorld != null && !bannedWorlds.contains(redirectWorldName)) {
                Variables.getMessageService().send(player, LoadMessages.redirect_world,
                        "%from_world%", world.getName(), "%to_world%", redirectWorld.getName());
                if (requirePlayersInRedirect && redirectWorld.getPlayers().isEmpty()) {
                    Variables.getMessageService().send(player, LoadMessages.rederictworldnear_error);
                    return Result.blocked();
                }
                return Result.allow(redirectWorld);
            }
        }
        Variables.getMessageService().send(player, LoadMessages.banned_world, "%world%", world.getName());
        return Result.blocked();
    }

}
