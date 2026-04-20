package org.sRandomRTP.DifferentMethods;

import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.sRandomRTP.Services.ConfigCache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link RequirementChecker}.
 * Uses ConfigCache.DEFAULT (all checks disabled) so no Bukkit server is needed.
 */
@Tag("unit")
class RequirementCheckerTest {

    private ConfigCache previousCache;

    @BeforeEach
    void setUp() {
        previousCache = Variables.configCache;
        // Default: all resource/economy checks disabled → any online player passes
        Variables.configCache = ConfigCache.DEFAULT;
    }

    @AfterEach
    void tearDown() {
        Variables.configCache = previousCache;
    }

    @Test
    void nullTeleportedPlayerReturnsMinusOne() {
        int result = RequirementChecker.checkRequirements(null, null, false);
        assertEquals(-1, result, "null teleported player must fail immediately");
    }

    @Test
    void offlineTeleportedPlayerReturnsMinusOne() {
        Player offline = Mockito.mock(Player.class);
        when(offline.isOnline()).thenReturn(false);

        int result = RequirementChecker.checkRequirements(offline, offline, false);
        assertEquals(-1, result, "offline teleported player must fail immediately");
    }

    @Test
    void allChecksDisabledOnlinePlayerReturnsZero() {
        // All checks (money, hunger, levels, health, items) are disabled in ConfigCache.DEFAULT
        Player player = Mockito.mock(Player.class);
        when(player.isOnline()).thenReturn(true);

        int result = RequirementChecker.checkRequirements(player, player, false);
        assertEquals(0, result, "all checks disabled → cost must be 0 (free teleport)");
    }

    @Test
    void nullPayerSkipsMoneyCheckAndPassesWhenOtherChecksDisabled() {
        Player teleported = Mockito.mock(Player.class);
        when(teleported.isOnline()).thenReturn(true);

        // payer is null → money check is explicitly skipped per javadoc
        int result = RequirementChecker.checkRequirements(null, teleported, false);
        assertEquals(0, result, "null payer must skip money check and pass all other disabled checks");
    }
}
