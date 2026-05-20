package org.sRandomRTP.Commands;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.sRandomRTP.Files.LoadMessages;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CommandReloadTest {

    @AfterEach
    void resetMessages() {
        LoadMessages.successfullyreload = null;
    }

    @Test
    void buildsReloadSuccessMessagesWithConfiguredText() {
        LoadMessages.successfullyreload = Arrays.asList(
                "&aReloaded in %mc% ms",
                null,
                "&7Done"
        );

        assertEquals(Arrays.asList("&aReloaded in 125 ms", "&7Done"),
                CommandReload.buildReloadSuccessMessages(1000L, 1125L));
    }

    @Test
    void buildsFallbackReloadSuccessMessageWhenConfigListIsEmpty() {
        LoadMessages.successfullyreload = Collections.emptyList();

        assertEquals(Collections.singletonList("&a[sRandomRTP] &aPlugin successfully reloaded. 50 ms"),
                CommandReload.buildReloadSuccessMessages(100L, 150L));
    }
}
