package org.sRandomRTP.DifferentMethods.Teleport;

import org.bukkit.Chunk;
import org.bukkit.World;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class PerformTeleportStrategyTest {

    @Test
    void prefersSynchronousTeleportForLoadedWinningChunk() {
        Chunk chunk = Mockito.mock(Chunk.class);
        World world = Mockito.mock(World.class);
        when(chunk.isLoaded()).thenReturn(true);

        RtpCandidateResolution resolution = new RtpCandidateResolution(
                world, chunk, 100, 80, 200, null, null, null, null,
                15, true, true, true, 10L, 10L);

        assertTrue(PerformTeleport.shouldPreferSynchronousTeleport(resolution));
    }

    @Test
    void doesNotPreferSynchronousTeleportWhenChunkIsMissing() {
        RtpCandidateResolution resolution = new RtpCandidateResolution(
                null, null, 100, 80, 200, null, null, null, null,
                15, true, false, true, 10L, 10L);

        assertFalse(PerformTeleport.shouldPreferSynchronousTeleport(resolution));
    }
}
