package org.sRandomRTP.DataPortals;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
class PortalDataBlocksTest {

    @Test
    void blocksWithSameLocationButDifferentPlayersAreNotEqual() {
        PortalDataBlocks a = new PortalDataBlocks("Alice", "world", 10, 64, 20, "GLASS", "myPortal");
        PortalDataBlocks b = new PortalDataBlocks("Bob",   "world", 10, 64, 20, "GLASS", "myPortal");
        assertNotEquals(a, b, "Blocks for different players at the same location must not be equal");
    }

    @Test
    void blocksWithSameLocationButDifferentPlayersHaveDifferentHashCodes() {
        PortalDataBlocks a = new PortalDataBlocks("Alice", "world", 10, 64, 20, "GLASS", "myPortal");
        PortalDataBlocks b = new PortalDataBlocks("Bob",   "world", 10, 64, 20, "GLASS", "myPortal");
        assertNotEquals(a.hashCode(), b.hashCode(),
                "Blocks for different players must produce different hash codes");
    }

    @Test
    void identicalBlocksAreEqual() {
        PortalDataBlocks a = new PortalDataBlocks("Alice", "world", 10, 64, 20, "GLASS", "myPortal");
        PortalDataBlocks b = new PortalDataBlocks("Alice", "world", 10, 64, 20, "GLASS", "myPortal");
        assertEquals(a, b, "Identical blocks must be equal");
    }

    @Test
    void identicalBlocksHaveSameHashCode() {
        PortalDataBlocks a = new PortalDataBlocks("Alice", "world", 10, 64, 20, "GLASS", "myPortal");
        PortalDataBlocks b = new PortalDataBlocks("Alice", "world", 10, 64, 20, "GLASS", "myPortal");
        assertEquals(a.hashCode(), b.hashCode(), "Identical blocks must have equal hash codes");
    }

    @Test
    void blocksWithDifferentPortalNameAreNotEqual() {
        PortalDataBlocks a = new PortalDataBlocks("Alice", "world", 10, 64, 20, "GLASS", "portalA");
        PortalDataBlocks b = new PortalDataBlocks("Alice", "world", 10, 64, 20, "GLASS", "portalB");
        assertNotEquals(a, b, "Blocks with different portal names must not be equal");
    }

    @Test
    void blockDoesNotEqualNull() {
        PortalDataBlocks a = new PortalDataBlocks("Alice", "world", 10, 64, 20, "GLASS", "myPortal");
        assertNotEquals(null, a);
    }

    @Test
    void blockEqualsItself() {
        PortalDataBlocks a = new PortalDataBlocks("Alice", "world", 10, 64, 20, "GLASS", "myPortal");
        assertEquals(a, a, "A block must equal itself (reflexive)");
    }
}
