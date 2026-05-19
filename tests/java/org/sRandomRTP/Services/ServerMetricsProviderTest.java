package org.sRandomRTP.Services;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerMetricsProviderTest {

    @Test
    void resolvesDirectApiMetrics() {
        ServerMetricsProvider provider = new ServerMetricsProvider(() -> new DirectServer());

        assertEquals(19.75D, provider.getPrimaryTps(), 0.001D);
        assertEquals(12.5D, provider.getAverageTickTimeMs(), 0.001D);
    }

    @Test
    void resolvesCraftFallbackMetrics() {
        ServerMetricsProvider provider = new ServerMetricsProvider(() -> new CraftLikeServer());

        assertEquals(18.5D, provider.getPrimaryTps(), 0.001D);
        assertEquals(50.0D, provider.getAverageTickTimeMs(), 0.001D);
    }

    @Test
    void reportsUnavailableForUnsupportedServer() {
        ServerMetricsProvider provider = new ServerMetricsProvider(() -> new EmptyServer());

        assertTrue(Double.isNaN(provider.getPrimaryTps()));
        assertTrue(Double.isNaN(provider.getAverageTickTimeMs()));
    }

    static final class DirectServer {
        public double[] getTPS() {
            return new double[]{19.75D, 19.80D, 19.90D};
        }

        public double getAverageTickTime() {
            return 12.5D;
        }
    }

    static final class CraftLikeServer {
        public InternalServer getServer() {
            return new InternalServer();
        }
    }

    static final class InternalServer {
        public double[] recentTps = new double[]{18.5D, 18.0D, 17.5D};
        public long[] tickTimes = new long[]{50_000_000L, 49_000_000L, 51_000_000L};
    }

    static final class EmptyServer {
    }
}
