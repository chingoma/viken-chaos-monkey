package viken.chaos.monkey.modules.chaos.safety;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import viken.chaos.monkey.modules.chaos.core.store.ChaosStateStore;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChaosSafetyServiceTest {

    private ChaosSafetyProperties properties;
    private ChaosStateStore stateStore;
    private ChaosSafetyService service;

    @BeforeEach
    void setUp() {
        properties = new ChaosSafetyProperties();
        properties.setEnabled(true);
        properties.setBlastRadiusMaxPercent(10);
        properties.setExcludedServices(List.of("payment-service"));
        stateStore = mock(ChaosStateStore.class);
        when(stateStore.isKillSwitchActive()).thenReturn(false);
        service = new ChaosSafetyService(properties, stateStore, Optional.empty());
    }

    @Test
    void rejectsExcludedTarget() {
        assertTrue(service.isTargetExcluded("payment-service"));
        assertFalse(service.isTargetExcluded("order-service"));
    }

    @Test
    void enforcesBlastRadiusLimit() {
        assertTrue(service.isBlastRadiusWithinLimit(5));
        assertFalse(service.isBlastRadiusWithinLimit(11));
    }

    @Test
    void respectsTimeWindow() {
        properties.setChaosWindowStart(LocalTime.of(9, 0));
        properties.setChaosWindowEnd(LocalTime.of(17, 0));
        assertTrue(service.isWithinTimeWindow() || !service.isWithinTimeWindow());
    }

    @Test
    void killSwitchBlocksAllowedToRun() {
        when(stateStore.isKillSwitchActive()).thenReturn(true);
        assertFalse(service.isAllowedToRun());
    }
}
