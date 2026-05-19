package org.sRandomRTP.Services;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AdminBarTypeTest {

    @Test
    void resolvesSubCommandsCaseInsensitively() {
        assertEquals(AdminBarType.TPS, AdminBarType.fromSubCommand("tpsbar"));
        assertEquals(AdminBarType.RAM, AdminBarType.fromSubCommand("RAMBAR"));
        assertEquals(AdminBarType.MSPT, AdminBarType.fromSubCommand("msptbar"));
        assertNull(AdminBarType.fromSubCommand("unknown"));
    }
}
