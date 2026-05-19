package org.sRandomRTP.Services;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.sRandomRTP.DifferentMethods.Variables;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class AdminBarServiceTest {

    @BeforeEach
    void setUp() throws Exception {
        System.clearProperty(LocalFeatureGate.ADMIN_BARS_PROPERTY);
        YamlConfiguration adminBarsConfig = new YamlConfiguration();
        adminBarsConfig.set("admin-bars.enabled", true);
        adminBarsConfig.set("admin-bars.tpsbar.enabled", true);
        adminBarsConfig.set("admin-bars.rambar.enabled", true);

        ConfigRegistry configRegistry = Mockito.mock(ConfigRegistry.class);
        when(configRegistry.getAdminBarsFile()).thenReturn(adminBarsConfig);

        PluginContext pluginContext = Mockito.mock(PluginContext.class);
        when(pluginContext.getConfigRegistry()).thenReturn(configRegistry);

        // Инжектируем мок-контекст через reflection (Variables.pluginContext — private volatile)
        Field field = Variables.class.getDeclaredField("pluginContext");
        field.setAccessible(true);
        field.set(null, pluginContext);
    }

    @AfterEach
    void tearDown() {
        System.clearProperty(LocalFeatureGate.ADMIN_BARS_PROPERTY);
    }

    @Test
    void shouldHideUnavailableMetricFromTab() {
        System.setProperty(LocalFeatureGate.ADMIN_BARS_PROPERTY, "true");
        AdminBarService service = new AdminBarService(new MessageService(), new ServerMetricsProvider(() -> new Object()));
        Player player = Mockito.mock(Player.class);
        when(player.hasPermission("sRandomRTP.Command.TpsBar")).thenReturn(true);

        assertFalse(service.shouldShowInTab(player, AdminBarType.TPS));
    }

    @Test
    void shouldShowAllBarsWhenAtLeastOneMetricIsAvailable() {
        System.setProperty(LocalFeatureGate.ADMIN_BARS_PROPERTY, "true");
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

    @Test
    void shouldHideAdminBarsWhenLocalGateDisabled() {
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

        assertFalse(service.isEnabled(AdminBarType.TPS));
        assertFalse(service.shouldShowInTab(player, AdminBarType.TPS));
        assertFalse(service.shouldShowAllInTab(player));
    }
}
