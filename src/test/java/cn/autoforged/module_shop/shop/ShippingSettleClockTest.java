package cn.autoforged.module_shop.shop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ShippingSettleClockTest {

    @Test
    void normalDaylightCycleStaysOnDayTimeAndIgnoresSingleTimeSet() {
        ShippingSettleClock clock = new ShippingSettleClock();
        assertFalse(clock.sample(true, 1000L, 1000L).fallbackGameTime());
        for (int i = 1; i <= 200; i++) {
            assertFalse(clock.sample(true, 1000L + i, 1000L + i).fallbackGameTime());
        }
        assertFalse(clock.sample(true, 1000L, 1201L).fallbackGameTime());
    }

    @Test
    void gameruleDoDaylightCycleFalseImmediatelyFallsBackAndRecovers() {
        ShippingSettleClock clock = new ShippingSettleClock();
        ShippingSettleClock.Sample s0 = clock.sample(false, 6000L, 48_500L);
        assertTrue(s0.fallbackGameTime());
        assertEquals(2L, s0.cycleDay());
        ShippingSettleClock.Sample recovered = null;
        for (int i = 1; i <= 100; i++) {
            recovered = clock.sample(true, 6000L + i, 48_500L + i);
        }
        assertFalse(recovered.fallbackGameTime());
    }

    @Test
    void frozenDayTimeAndDaytimeLoopTriggerFallback() {
        ShippingSettleClock frozen = new ShippingSettleClock();
        frozen.sample(true, 6000L, 10_000L);
        assertFalse(frozen.sample(true, 6000L, 10_099L).fallbackGameTime());
        assertTrue(frozen.sample(true, 6000L, 10_100L).fallbackGameTime());

        ShippingSettleClock loop = new ShippingSettleClock();
        loop.sample(true, 1000L, 1000L);
        loop.sample(true, 12_000L, 12_000L);
        assertFalse(loop.sample(true, 1000L, 12_001L).fallbackGameTime());
        loop.sample(true, 12_000L, 23_001L);
        assertTrue(loop.sample(true, 1000L, 23_002L).fallbackGameTime());
    }

    @Test
    void remainingTicksHandlesWrapAround() {
        assertEquals(2000L, ShippingSettleClock.remainingTicks(22_000L, 0L));
        assertEquals(23_000L, ShippingSettleClock.remainingTicks(7000L, 6000L));
    }
}
