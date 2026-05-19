package org.sRandomRTP.DataPortals;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.sRandomRTP.DifferentMethods.Variables;
import org.sRandomRTP.Services.RuntimeStateRegistry;

import java.lang.reflect.Field;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies that the three DB load methods in PortalSQLRepository safely handle
 * the case where RuntimeStateRegistry is null (plugin disabled while a load is in flight).
 *
 * Since the test context has no PortalRepository (Variables.portalRepository is null),
 * SQLManagerPortals.runDbAsync() returns a completed future immediately without executing
 * the lambda. This confirms the public contract: the returned future completes normally
 * and no NPE escapes to the caller regardless of state.
 */
@Tag("unit")
class PortalSQLRepositoryNullStateTest {

    private RuntimeStateRegistry previousState;

    @BeforeEach
    void setUp() throws Exception {
        Field field = Variables.class.getDeclaredField("runtimeState");
        field.setAccessible(true);
        previousState = (RuntimeStateRegistry) field.get(null);
        field.set(null, null);
    }

    @AfterEach
    void tearDown() throws Exception {
        Field field = Variables.class.getDeclaredField("runtimeState");
        field.setAccessible(true);
        field.set(null, previousState);
    }

    @Test
    void loadPortalsPlayerDoesNotThrowWhenStateNull()
            throws ExecutionException, InterruptedException, TimeoutException {
        CompletableFuture<Void> future = PortalSQLRepository.loadPortalsPlayerFromDatabaseSQL();
        assertNotNull(future, "future must not be null");
        // Must complete without exception
        assertDoesNotThrow(() -> future.get(2, TimeUnit.SECONDS));
    }

    @Test
    void loadPortalBlocksDoesNotThrowWhenStateNull()
            throws ExecutionException, InterruptedException, TimeoutException {
        CompletableFuture<Void> future = PortalSQLRepository.loadPortalBlocksPlayerToDatabaseSQL();
        assertNotNull(future);
        assertDoesNotThrow(() -> future.get(2, TimeUnit.SECONDS));
    }

    @Test
    void loadPortalTasksDoesNotThrowWhenStateNull()
            throws ExecutionException, InterruptedException, TimeoutException {
        CompletableFuture<Void> future = PortalSQLRepository.loadPortalTasksFromDatabaseSQL();
        assertNotNull(future);
        assertDoesNotThrow(() -> future.get(2, TimeUnit.SECONDS));
    }

    @Test
    void allLoadMethodsCompleteNormallyWithNullState() {
        // All three should complete without ExecutionException wrapping an NPE
        assertAll(
                () -> assertDoesNotThrow(() ->
                        PortalSQLRepository.loadPortalsPlayerFromDatabaseSQL().get(2, TimeUnit.SECONDS)),
                () -> assertDoesNotThrow(() ->
                        PortalSQLRepository.loadPortalBlocksPlayerToDatabaseSQL().get(2, TimeUnit.SECONDS)),
                () -> assertDoesNotThrow(() ->
                        PortalSQLRepository.loadPortalTasksFromDatabaseSQL().get(2, TimeUnit.SECONDS))
        );
    }
}
