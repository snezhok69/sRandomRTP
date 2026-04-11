package org.sRandomRTP.Services;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.sRandomRTP.DifferentMethods.Variables;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class AdminBarServiceTest {

    @BeforeEach
    void setUp() {
        Variables.adminbarsfile = new YamlConfiguration();
        Variables.adminbarsfile.set("admin-bars.enabled", true);
        Variables.adminbarsfile.set("admin-bars.tpsbar.enabled", true);
        Variables.adminbarsfile.set("admin-bars.rambar.enabled", true);
    }

    @Test
    void shouldHideUnavailableMetricFromTab() {
        AdminBarService service = new AdminBarService(new MessageService(), new ServerMetricsProvider(() -> new Object()));
        Player player = Mockito.mock(Player.class);
        when(player.hasPermission("sRandomRTP.Command.TpsBar")).thenReturn(true);

        assertFalse(service.shouldShowInTab(player, AdminBarType.TPS));
    }

    @Test
    void shouldShowAllBarsWhenAtLeastOneMetricIsAvailable() {
        ServerMetricsProvider provider = new ServerMetricsProvider(() -> new Object() {
            @SuppressWarnings("unused")
            public double[] getTPS() {
                return new double[] { 20.0D, 20.0D, 20.0D };
            }
        });

        AdminBarService service = new AdminBarService(new MessageService(), provider);
        Player player = Mockito.mock(Player.class);
        when(player.hasPermission(AdminBarService.ALL_BARS_PERMISSION)).thenReturn(true);
        when(player.hasPermission("sRandomRTP.Command.TpsBar")).thenReturn(true);
        when(player.hasPermission("sRandomRTP.Command.RamBar")).thenReturn(true);

        assertTrue(service.shouldShowAllInTab(player));
    }
}
