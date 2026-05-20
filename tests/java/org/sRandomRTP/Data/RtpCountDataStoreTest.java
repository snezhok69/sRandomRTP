package org.sRandomRTP.Data;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.sRandomRTP.DifferentMethods.Variables;
import org.sRandomRTP.Services.RuntimeStateRegistry;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RtpCountDataStoreTest {

    private RuntimeStateRegistry previousState;

    @AfterEach
    void tearDown() throws Exception {
        setRuntimeState(previousState);
        setDirty(false);
    }

    @Test
    void incrementAndMarkDirtyUpdatesMemoryWithoutImmediateSaveRequirement() throws Exception {
        previousState = Variables.getRuntimeState();
        RuntimeStateRegistry state = new RuntimeStateRegistry();
        setRuntimeState(state);
        setDirty(false);

        RtpCountDataStore.incrementAndMarkDirty();

        assertEquals(1, state.getRtpCount().get());
        assertTrue(RtpCountDataStore.isDirty());
    }

    private void setRuntimeState(RuntimeStateRegistry state) throws Exception {
        Field field = Variables.class.getDeclaredField("runtimeState");
        field.setAccessible(true);
        field.set(null, state);
    }

    private void setDirty(boolean value) throws Exception {
        Field field = RtpCountDataStore.class.getDeclaredField("dirty");
        field.setAccessible(true);
        ((AtomicBoolean) field.get(null)).set(value);
    }
}
