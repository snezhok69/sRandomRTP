package org.sRandomRTP.Cooldowns;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.sRandomRTP.DifferentMethods.Variables;
import org.sRandomRTP.Services.ConfigCache;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link CooldownManager}.
 * Uses ConfigCache.DEFAULT (cooldowns disabled) to avoid Bukkit server dependency.
 */
@Tag("unit")
class CooldownManagerTest {

    private ConfigCache previousCache;
    private Player player;
    private CommandSender sender;
    private UUID playerId;
    private CooldownManager manager;

    @BeforeEach
    void setUp() {
        previousCache = Variables.configCache;
        Variables.configCache = ConfigCache.DEFAULT; // cooldowns disabled
        player = Mockito.mock(Player.class);
        sender = Mockito.mock(CommandSender.class);
        playerId = UUID.randomUUID();
        manager = new CooldownManager();

        when(player.getUniqueId()).thenReturn(playerId);
        when(player.getName()).thenReturn("testplayer");
        when(player.hasPermission(Mockito.anyString())).thenReturn(false);
    }

    @AfterEach
    void tearDown() {
        Variables.configCache = previousCache;
    }

    @Test
    void recordsTimestampOnFirstUseWhenCooldownsDisabled() {
        Map<UUID, Long> cooldownMap = new HashMap<>();
        boolean blocked = manager.checkCooldown(player, sender, cooldownMap, false);

        assertFalse(blocked);
        assertTrue(cooldownMap.containsKey(playerId), "timestamp should be recorded for future cooldown tracking");
    }

    @Test
    void notBlockedOnSecondCallWhenCooldownsDisabled() {
        Map<UUID, Long> cooldownMap = new HashMap<>();
        manager.checkCooldown(player, sender, cooldownMap, false);
        assertFalse(manager.checkCooldown(player, sender, cooldownMap, false));
    }

    @Test
    void invalidatePermissionCacheIsIdempotent() {
        manager.invalidatePermissionCache(UUID.randomUUID());
        manager.invalidatePermissionCache(UUID.randomUUID());
    }

    @Test
    void evictExpiredCacheDoesNotThrowOnEmptyCache() {
        manager.evictExpiredCache();
    }

    @Test
    void customCooldownPermissionUsesCacheUntilInvalidated() {
        PermissionAttachmentInfo fourSeconds = Mockito.mock(PermissionAttachmentInfo.class);
        when(fourSeconds.getValue()).thenReturn(true);
        when(fourSeconds.getPermission()).thenReturn("sRandomRtp.Cooldown.4");
        when(player.getEffectivePermissions()).thenReturn(Collections.singleton(fourSeconds));

        assertEquals(4, manager.resolveCustomCooldown(player, 60, false));

        PermissionAttachmentInfo twoSeconds = Mockito.mock(PermissionAttachmentInfo.class);
        when(twoSeconds.getValue()).thenReturn(true);
        when(twoSeconds.getPermission()).thenReturn("srandomrtp.cooldown.2");
        when(player.getEffectivePermissions()).thenReturn(Collections.singleton(twoSeconds));

        assertEquals(4, manager.resolveCustomCooldown(player, 60, false),
                "cached cooldown should be reused until invalidated or expired");

        manager.invalidatePermissionCache(playerId);
        assertEquals(2, manager.resolveCustomCooldown(player, 60, false));
    }

    @Test
    void singletonInstanceIsAlwaysSameObject() {
        assertSame(CooldownManager.instance(), CooldownManager.instance());
    }

    /**
     * Verifies the null-safety fix: when the plugin is not initialized (getRuntimeState()
     * returns null), checkRtp/checkBiome must not throw NPE — they should return false.
     */
    @Test
    void checkRtpReturnsFalseWhenRuntimeStateIsNull() {
        assertFalse(CooldownManager.checkRtp(player, sender),
                "checkRtp should return false (allow teleport) when RuntimeStateRegistry is null");
    }

    @Test
    void checkBiomeReturnsFalseWhenRuntimeStateIsNull() {
        assertFalse(CooldownManager.checkBiome(player, sender),
                "checkBiome should return false (allow teleport) when RuntimeStateRegistry is null");
    }
}
