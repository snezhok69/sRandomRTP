package org.sRandomRTP.DifferentMethods.Teleport;

import com.tcoded.folialib.wrapper.task.WrappedTask;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;
import org.sRandomRTP.DifferentMethods.BossBars.RemoveAllBossBars;
import org.sRandomRTP.DifferentMethods.Variables;
import org.sRandomRTP.Services.RuntimeStateRegistry;

import java.util.List;

/**
 * Shared cancellation helper for move/damage/break/manual RTP interruption flows.
 */
public final class TeleportCancellationSupport {

    private TeleportCancellationSupport() {
    }

    public static boolean hasActiveTeleport(Player player) {
        if (player == null) {
            return false;
        }
        RuntimeStateRegistry state = Variables.getRuntimeState();
        return TeleportRequestManager.getContext(player.getUniqueId()) != null || state.hasTeleportTask(player);
    }

    /**
     * Convenience: checks for an active teleport, then cancels and notifies in one call.
     */
    public static boolean cancelIfActive(Player player,
                                         String reason,
                                         List<String> messages,
                                         boolean clearCooldown) {
        if (!hasActiveTeleport(player)) {
            return false;
        }
        return cancelAndNotify(player, Variables.isLoggingEnabled(), reason, messages, clearCooldown);
    }

    public static boolean cancelAndNotify(Player player,
                                          boolean loggingEnabled,
                                          String reason,
                                          List<String> messages,
                                          boolean clearCooldown) {
        if (player == null) {
            return false;
        }

        RuntimeStateRegistry state = Variables.getRuntimeState();
        TeleportRequestContext context = TeleportRequestManager.getContext(player.getUniqueId());
        boolean hasTask = state.hasTeleportTask(player);

        if (context == null && !hasTask) {
            return false;
        }

        if (context != null) {
            TeleportRequestManager.cancelRequest(player.getUniqueId(), loggingEnabled, reason);
        }

        WrappedTask task = state.removeTeleportTask(player);
        if (task != null && !task.isCancelled()) {
            task.cancel();
        }

        if (messages != null && !messages.isEmpty()) {
            Variables.getMessageService().send(player, messages);
        }

        if (clearCooldown) {
            state.getCooldowns().remove(player.getUniqueId());
        }

        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(""));
        RemoveAllBossBars.removeBossBar(player);
        CleanupTasks.finalizeTeleportStatus(player, loggingEnabled);
        // Отмена поиска — позиция /back не нужна, очищаем чтобы не копилась в памяти
        state.removeInitialPosition(player);
        return true;
    }
}
