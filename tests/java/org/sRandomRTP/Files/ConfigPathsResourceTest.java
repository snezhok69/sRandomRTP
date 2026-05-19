package org.sRandomRTP.Files;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigPathsResourceTest {

    @Test
    void allConfiguredResourcesArePackagedWithMatchingCase() {
        List<String> missing = new ArrayList<String>();
        assertResourcesExist(ConfigPaths.UPDATABLE_FILES, missing);
        assertResourcesExist(ConfigPaths.CREATE_ONLY_FILES, missing);

        assertTrue(missing.isEmpty(), "Missing packaged resources: " + missing);
    }

    private void assertResourcesExist(String[] paths, List<String> missing) {
        for (String path : paths) {
            if (ConfigPathsResourceTest.class.getClassLoader().getResource(path) == null) {
                missing.add(path);
            }
        }
    }
}
