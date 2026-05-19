package org.sRandomRTP.GetYGet;

import org.bukkit.Material;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.sRandomRTP.DifferentMethods.Variables;

import java.util.Collections;
import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GetSafeYCoordinateMaterialSafetyTest {

    @AfterEach
    void tearDown() {
        Variables.blockList = Collections.emptySet();
    }

    @Test
    void occupantMaterialRejectsWaterAndLeaves() {
        assertFalse(GetSafeYCoordinate.isSafeTeleportOccupantMaterial(Material.WATER));
        assertFalse(GetSafeYCoordinate.isSafeTeleportOccupantMaterial(Material.OAK_LEAVES));
        assertTrue(GetSafeYCoordinate.isSafeTeleportOccupantMaterial(Material.AIR));
        assertTrue(GetSafeYCoordinate.isSafeTeleportOccupantMaterial(Material.TALL_GRASS));
    }

    @Test
    void supportMaterialRejectsUnsafeAndBannedBlocks() {
        assertTrue(GetSafeYCoordinate.isSafeTeleportSupportMaterial(Material.STONE));
        assertFalse(GetSafeYCoordinate.isSafeTeleportSupportMaterial(Material.WATER));
        assertFalse(GetSafeYCoordinate.isSafeTeleportSupportMaterial(Material.MAGMA_BLOCK));

        Variables.blockList = Collections.unmodifiableSet(EnumSet.of(Material.GRASS_BLOCK));
        assertFalse(GetSafeYCoordinate.isSafeTeleportSupportMaterial(Material.GRASS_BLOCK));
    }
}
