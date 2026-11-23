package cn.autoforged.module_shop.shop;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ShopExpireSupportTest {

    @Test
    void normalizeAndActiveTreatPermanentAndZeroAsInactive() {
        assertEquals("permanent", ShopExpireSupport.normalizeExpireMode(null));
        assertEquals("permanent", ShopExpireSupport.normalizeExpireMode("unknown"));
        assertEquals("game", ShopExpireSupport.normalizeExpireMode(" GAME "));
        assertEquals("real", ShopExpireSupport.normalizeExpireMode("Real"));

        assertFalse(ShopExpireSupport.isExpireActive("permanent", 7));
        assertFalse(ShopExpireSupport.isExpireActive("game", 0));
        assertTrue(ShopExpireSupport.isExpireActive("game", 7));
        assertTrue(ShopExpireSupport.isExpireActive("real", 24));
    }

    @Test
    void parseExpireAmountAllowsBlankWhenPermanentAndValidatesRange() {
        assertEquals(7, ShopExpireSupport.parseExpireAmount("permanent", "", 7));
        assertEquals(0, ShopExpireSupport.parseExpireAmount("permanent", "0", 7));
        assertNull(ShopExpireSupport.parseExpireAmount("game", "", 7));
        assertNull(ShopExpireSupport.parseExpireAmount("real", "-1", 7));
        assertNull(ShopExpireSupport.parseExpireAmount("real", "100001", 7));
        assertEquals(72, ShopExpireSupport.parseExpireAmount("real", "72", 7));
    }

    @Test
    void shouldResetTimestampsOnActivateOrClockSwitchOnly() {
        assertTrue(ShopExpireSupport.shouldResetExpireTimestamps("permanent", 7, "game", 7));
        assertTrue(ShopExpireSupport.shouldResetExpireTimestamps("game", 7, "real", 24));
        assertFalse(ShopExpireSupport.shouldResetExpireTimestamps("game", 7, "permanent", 7));
        assertFalse(ShopExpireSupport.shouldResetExpireTimestamps("real", 24, "real", 48));
    }

    @Test
    void splitStackCountsPreservesTotalAndRespectsMaxStackSize() {
        assertArrayEquals(new int[0], ShopExpireSupport.splitStackCounts(0, 64));
        assertArrayEquals(new int[]{64, 64, 22}, ShopExpireSupport.splitStackCounts(150, 64));
        assertArrayEquals(new int[]{1, 1, 1, 1, 1}, ShopExpireSupport.splitStackCounts(5, 1));
    }

    @Test
    void activityDayHourGuardBlocksBeforeScheduledHourAndHonorsCatchUpAfter() {
        assertTrue(ShopExpireSupport.canGrantActivityDayAtHour(-1, 3, false, false));
        assertFalse(ShopExpireSupport.canGrantActivityDayAtHour(20, 19, true, true));
        assertTrue(ShopExpireSupport.canGrantActivityDayAtHour(20, 20, false, false));
        assertFalse(ShopExpireSupport.canGrantActivityDayAtHour(20, 21, false, false));
        assertTrue(ShopExpireSupport.canGrantActivityDayAtHour(20, 21, true, false));
        assertFalse(ShopExpireSupport.canGrantActivityOfflineAtHour(20, 19));
        assertTrue(ShopExpireSupport.canGrantActivityOfflineAtHour(20, 20));
    }

    @Test
    void patchTomlSectionKeysOnlyTouchesTargetSection(@TempDir Path dir) throws Exception {
        Path cfg = dir.resolve("module_shop_eco-common.toml");
        Files.write(cfg, List.of("[stall]", "\tstallExpireMode = \"game\"", "\tstallExpireAmount = 7",
                "[demand]", "\tdemandExpireMode = \"real\""), StandardCharsets.UTF_8);
        Map<String, String> patch = new LinkedHashMap<>();
        patch.put("stallExpireMode", "\"permanent\"");
        patch.put("stallExpireAmount", "0");
        assertTrue(ShopExpireSupport.writeTomlSectionAtomic(cfg, "stall", patch));
        List<String> after = Files.readAllLines(cfg, StandardCharsets.UTF_8);
        assertTrue(after.contains("\tstallExpireMode = \"permanent\""));
        assertTrue(after.contains("\tdemandExpireMode = \"real\""));
    }
}
