package org.sRandomRTP.DifferentMethods;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.OfflinePlayer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class EconomyPaymentManagerMockBukkitTest {

    private ServerMock server;
    private PlayerMock player;
    private Economy economy;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        player = server.addPlayer("economy-tester");
        economy = mock(Economy.class);
        Variables.econ = economy;
    }

    @AfterEach
    void tearDown() {
        if (player != null) {
            EconomyPaymentManager.refund(player.getUniqueId());
        }
        Variables.econ = null;
        if (MockBukkit.isMocked()) {
            MockBukkit.unmock();
        }
    }

    @Test
    void refundIsOnlyAppliedOnceForPendingPayment() {
        when(economy.withdrawPlayer(eq(player), eq(25.0)))
                .thenReturn(new EconomyResponse(25.0, 75.0, EconomyResponse.ResponseType.SUCCESS, null));
        when(economy.depositPlayer(any(OfflinePlayer.class), eq(25.0)))
                .thenReturn(new EconomyResponse(25.0, 100.0, EconomyResponse.ResponseType.SUCCESS, null));

        assertTrue(EconomyPaymentManager.chargePlayer(player, player, 25.0));

        EconomyPaymentManager.refund(player.getUniqueId());
        EconomyPaymentManager.refund(player.getUniqueId());

        verify(economy, times(1)).withdrawPlayer(eq(player), eq(25.0));
        verify(economy, times(1)).depositPlayer(any(OfflinePlayer.class), eq(25.0));
    }

    @Test
    void confirmSuccessPreventsLaterRefund() {
        when(economy.withdrawPlayer(eq(player), eq(10.0)))
                .thenReturn(new EconomyResponse(10.0, 90.0, EconomyResponse.ResponseType.SUCCESS, null));

        assertTrue(EconomyPaymentManager.chargePlayer(player, player, 10.0));

        EconomyPaymentManager.confirmSuccess(player);
        EconomyPaymentManager.refund(player.getUniqueId());

        verify(economy, times(1)).withdrawPlayer(eq(player), eq(10.0));
        verify(economy, never()).depositPlayer(any(OfflinePlayer.class), eq(10.0));
    }
}
