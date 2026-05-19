package org.sRandomRTP.DifferentMethods;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
class EconomyPaymentManagerTest {

    private Map<UUID, ?> pendingPayments;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void accessPendingPaymentsField() throws Exception {
        Field field = EconomyPaymentManager.class.getDeclaredField("pendingPayments");
        field.setAccessible(true);
        pendingPayments = (Map<UUID, ?>) field.get(null);
        pendingPayments.clear();
    }

    @AfterEach
    void cleanUp() {
        pendingPayments.clear();
    }

    // ── null guard ────────────────────────────────────────────────────────────

    @Test
    void chargePlayer_nullPayer_returnsFalse() {
        assertFalse(EconomyPaymentManager.chargePlayer(null, player(UUID.randomUUID()), 100));
    }

    @Test
    void chargePlayer_nullTeleported_returnsFalse() {
        assertFalse(EconomyPaymentManager.chargePlayer(player(UUID.randomUUID()), null, 100));
    }

    // ── Zero-cost teleport  (Bug #4 regression) ──────────────────────────────

    /**
     * Regression for Bug #4: when {@code money-enabled=true} and {@code money-cost=0},
     * the old guard {@code amount <= 0 → return false} incorrectly sent the
     * "error withdrawing" message to players doing a legitimately free teleport.
     *
     * After the fix, zero/negative cost is treated as a free teleport:
     * {@code true} is returned immediately without any Vault or pending-entry interaction.
     */
    @Test
    void chargePlayer_zeroCost_returnsTrueWithoutPendingPayment() {
        UUID teleportedId = UUID.randomUUID();

        boolean result = EconomyPaymentManager.chargePlayer(
                player(UUID.randomUUID()), player(teleportedId), 0.0);

        assertTrue(result, "zero-cost charge must return true");
        assertFalse(pendingPayments.containsKey(teleportedId),
                "zero-cost charge must NOT create a pending payment entry");
    }

    @Test
    void chargePlayer_negativeCost_returnsTrueWithoutPendingPayment() {
        UUID teleportedId = UUID.randomUUID();

        boolean result = EconomyPaymentManager.chargePlayer(
                player(UUID.randomUUID()), player(teleportedId), -50.0);

        assertTrue(result, "negative-cost charge must return true (treated as free)");
        assertFalse(pendingPayments.containsKey(teleportedId),
                "negative-cost charge must NOT create a pending payment entry");
    }

    // ── confirmSuccess / refund null-safety ───────────────────────────────────

    @Test
    void confirmSuccess_nullPlayer_doesNotThrow() {
        assertDoesNotThrow(() -> EconomyPaymentManager.confirmSuccess(null));
    }

    @Test
    void refund_nullPlayerOverload_doesNotThrow() {
        assertDoesNotThrow(() -> EconomyPaymentManager.refund((org.bukkit.entity.Player) null));
    }

    @Test
    void refund_nullUuidOverload_doesNotThrow() {
        assertDoesNotThrow(() -> EconomyPaymentManager.refund((UUID) null));
    }

    @Test
    void refund_unknownUuid_isNoOp() {
        // Refunding an ID with no pending payment must silently succeed.
        assertDoesNotThrow(() -> EconomyPaymentManager.refund(UUID.randomUUID()),
                "refund of unknown UUID must be a safe no-op");
    }

    @Test
    void confirmSuccess_playerWithNoPendingPayment_isNoOp() {
        assertDoesNotThrow(() -> EconomyPaymentManager.confirmSuccess(player(UUID.randomUUID())),
                "confirmSuccess with no pending payment must be a safe no-op");
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    /**
     * Creates a minimal {@link org.bukkit.entity.Player} proxy that only implements
     * {@code getUniqueId()} — the single method needed by the code paths under test.
     * Any other method call throws {@link UnsupportedOperationException}.
     */
    private static org.bukkit.entity.Player player(UUID id) {
        return (org.bukkit.entity.Player) Proxy.newProxyInstance(
                EconomyPaymentManagerTest.class.getClassLoader(),
                new Class[]{org.bukkit.entity.Player.class},
                (proxy, method, args) -> {
                    if ("getUniqueId".equals(method.getName())) {
                        return id;
                    }
                    if ("getName".equals(method.getName())) {
                        return "TestPlayer-" + id.toString().substring(0, 8);
                    }
                    if ("isOnline".equals(method.getName())) {
                        return true;
                    }
                    throw new UnsupportedOperationException(
                            "EconomyPaymentManagerTest stub: " + method.getName() + " not implemented");
                });
    }
}
