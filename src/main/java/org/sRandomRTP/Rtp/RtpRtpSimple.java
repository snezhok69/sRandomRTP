package org.sRandomRTP.Rtp;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.sRandomRTP.DifferentMethods.Variables;
import org.sRandomRTP.DifferentMethods.rtprtps.DifferentRtpMethods;

import static org.sRandomRTP.DifferentMethods.rtprtps.DifferentRtpMethods.clampRadiusToBorder;
import static org.sRandomRTP.DifferentMethods.rtprtps.DifferentRtpMethods.getWorldSpecificRadius;

/**
 * Универсальный одномировой RTP-хендлер.
 * Заменяет дублирующиеся внутренние Handler-классы из RtpRtpPlayer и RtpRtpPortal.
 *
 * @param useWorldSpecificRadius {@code true} — читает per-world радиус из конфига,
 *                               {@code false} — использует глобальный teleport.radius.
 */
public final class RtpRtpSimple {

    private RtpRtpSimple() {}

    /**
     * Запускает RTP-поиск для игрока в указанном мире.
     *
     * @param player                 игрок для телепортации
     * @param targetWorld            целевой мир
     * @param useWorldSpecificRadius {@code true} — per-world радиус, {@code false} — глобальный
     * @param logPrefix              префикс для сообщений об ошибках радиуса
     */
    public static void launch(Player player, World targetWorld,
                              boolean useWorldSpecificRadius, String logPrefix) {
        new AbstractRtpHandler() {
            @Override
            protected LaunchParams buildLaunchParams(Player p, World world, boolean loggingEnabled) {
                int centerX = worldCenterX(world);
                int centerZ = worldCenterZ(world);

                int radius;
                int minRadius;
                if (useWorldSpecificRadius) {
                    String worldName = world.getName();
                    radius    = getWorldSpecificRadius(worldName, "radius");
                    minRadius = getWorldSpecificRadius(worldName, "minradius");
                } else {
                    radius    = Variables.teleportfile.getInt("teleport.radius");
                    minRadius = Variables.teleportfile.getInt("teleport.minradius");
                }

                DifferentRtpMethods.ClampedRadius clamped =
                        clampRadiusToBorder(world, radius, minRadius, logPrefix, loggingEnabled);
                radius    = clamped.radius;
                minRadius = clamped.minRadius;

                if (!validateRadius(minRadius, radius, p)) return null;

                int maxAttempts = Math.max(1, Variables.teleportfile.getInt("teleport.maxtries"));
                return new LaunchParams(centerX, centerZ, radius, minRadius, maxAttempts, true);
            }
        }.launchRtpForPlayer(player, targetWorld);
    }
}
