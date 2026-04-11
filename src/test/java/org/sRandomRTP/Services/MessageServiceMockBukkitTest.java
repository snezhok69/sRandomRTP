package org.sRandomRTP.Services;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.command.ConsoleCommandSenderMock;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MessageServiceMockBukkitTest {

    private ServerMock server;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        if (MockBukkit.isMocked()) {
            MockBukkit.unmock();
        }
    }

    @Test
    void sendsFormattedMessageToConsoleSender() {
        ConsoleCommandSenderMock console = server.getConsoleSender();
        MessageService service = new MessageService();

        service.send(console, java.util.Collections.singletonList("&aReloaded"));

        assertEquals("§aReloaded", console.nextMessage());
    }
}
