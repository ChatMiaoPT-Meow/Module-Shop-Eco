package cn.autoforged.module_shop.eco;

import cn.autoforged.module_shop.data.BlackMarketBucket;
import cn.autoforged.module_shop.data.PlayerProfessionProfile;
import cn.autoforged.module_shop.data.ShopTreasury;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Module-Shop Eco 核心做市定价、防套利硬锁、黑市满仓停收、官方总钱箱保底与回补、职业专注系统自动化单测。
 */
class EcoPricingAndTreasuryTest {

    @Test
    @DisplayName("1. 批量购买逐件积分恒等式：一次买64个 == 分64次每次买1个 == 分8次每次买8个（零拆单漏洞）")
    void batchAskIntegralIdentity() {
        long[] anchors = {5L, 48L, 120L, 1500L};
        int[] startStocks = {256, 128, 64, 30, 0};
        int targetStock = 128;
        double askFloor = 0.75D;
        double askCeiling = 2.20D;

        for (long anchor : anchors) {
            for (int startStock : startStocks) {
                long allAtOnce = EcoPricing.quoteBatchAskPure(anchor, startStock, targetStock, 64, askFloor, askCeiling);

                long oneByOne = 0L;
                for (int i = 0; i < 64; i++) {
                    oneByOne += EcoPricing.quoteBatchAskPure(anchor, Math.max(0, startStock - i), targetStock, 1, askFloor, askCeiling);
                }

                long eightByEight = 0L;
                for (int chunk = 0; chunk < 8; chunk++) {
                    int curStock = Math.max(0, startStock - chunk * 8);
                    eightByEight += EcoPricing.quoteBatchAskPure(anchor, curStock, targetStock, 8, askFloor, askCeiling);
                }

                assertEquals(allAtOnce, oneByOne,
                        "一次买64个必须严格等于连点64次单买 (anchor=" + anchor + ", stock=" + startStock + ")");
                assertEquals(allAtOnce, eightByEight,
                        "一次买64个必须严格等于分8批每批8个 (anchor=" + anchor + ", stock=" + startStock + ")");
            }
        }

        // 平衡库存 r=1.0 时单价应等于锚定价
        assertEquals(100L, EcoPricing.computeAskUnitPure(100L, 128, 128, askFloor, askCeiling));
        // 库存越少，售价越高
        long lowStockAsk = EcoPricing.computeAskUnitPure(100L, 10, 128, askFloor, askCeiling);
        long highStockAsk = EcoPricing.computeAskUnitPure(100L, 300, 128, askFloor, askCeiling);
        assertTrue(lowStockAsk > 100L && 100L > highStockAsk, "缺货时应涨价，积压时应降价");
    }

    @Test
    @DisplayName("2. 0.82 防套利硬钳位：叠满 Lv.100 职业+专注+紧缺+满健康度，回收价仍永不超过官方卖价 82%")
    void antiArbitrageHardLock() {
        double maxProfBonus = 0.70D;
        double maxScarcity = 1.25D; // +25% 冷门紧缺
        double maxTreasuryHealth = 1.25D;
        double maxBidInv = EcoPricing.computeBidInventoryMultiplier(0, 128, 0.35D, 1.35D, 2.0D, 0.40D);
        double profFocusMult = EcoPricing.computeProfessionFocusMultiplier(100, maxProfBonus, maxScarcity);

        for (long officialAsk : new long[]{1L, 2L, 5L, 10L, 50L, 100L, 999L, 10_000L}) {
            long baseBid = officialAsk; // 极端配置：腐竹误把基础收购价配得和卖价一样高
            long inflatedBid = Math.round(baseBid * maxBidInv * maxTreasuryHealth * profFocusMult);
            long clamped = EcoPricing.clampBidAgainstAsk(inflatedBid, officialAsk, 0.82D);

            if (officialAsk <= 1L) {
                assertEquals(0L, clamped, "官方只卖 1 星币的物品必须禁止倒卖套利");
            } else {
                long expectedCap = Math.min(officialAsk - 1L, Math.max(1L, (long) Math.floor(officialAsk * 0.82D)));
                assertTrue(clamped <= expectedCap,
                        "钳位后收购价(" + clamped + ")超过 0.82 上限(" + expectedCap + ")，ask=" + officialAsk);
                assertTrue(clamped < officialAsk,
                        "任何情况下官方回收单价必须严格低于官方卖价，ask=" + officialAsk);
            }
        }
    }

    @Test
    @DisplayName("3. 黑市满仓停收与零库存每日缺货悬赏单次爬坡（防同日双加）")
    void blackMarketFullStopAndScarcityClimb() {
        int targetCap = 100;
        double fullRatio = 2.0D; // 200 满仓停收
        BlackMarketBucket bucket = new BlackMarketBucket(0, targetCap);

        // 第 1 天结算严格只涨 +5%，同日重复触发不二次累加
        bucket.tickDaily(1L, 0.20D, 0.05D, 0.40D);
        assertEquals(0.05D, bucket.scarcityBonus, 1e-6D, "单日断供悬赏必须严格只加 1 次 +5%");
        bucket.tickDaily(1L, 0.20D, 0.05D, 0.40D);
        assertEquals(0.05D, bucket.scarcityBonus, 1e-6D, "同日重复结算必须幂等，绝不双倍叠加");

        // 连续到第 10 天 0 库存断供：最高卡在 +40%
        for (long day = 2; day <= 10; day++) {
            bucket.tickDaily(day, 0.20D, 0.05D, 0.40D);
        }
        assertEquals(0.40D, bucket.scarcityBonus, 1e-6D, "零库存断供悬赏最高应封顶在 +40%");

        // 0 库存时收购乘数享受上限 + 缺货加成
        double emptyMult = EcoPricing.computeBidInventoryMultiplier(
                bucket.currentStock, bucket.targetCapacity, 0.35D, 1.35D, fullRatio, bucket.scarcityBonus);
        assertTrue(emptyMult > 1.35D, "零库存 + 缺货悬赏时收购乘数应高于基础封顶值");

        // 玩家交货 200 个直接塞满仓库（target * fullRatio = 200）
        bucket.addStock(200, fullRatio);
        assertTrue(bucket.isWarehouseFull(fullRatio), "达到 2.0x 目标容量时仓库应处于满仓状态");
        assertEquals(0.0D, bucket.scarcityBonus, 1e-6D, "库存过半后缺货悬赏应自动归零");

        double fullMult = EcoPricing.computeBidInventoryMultiplier(
                bucket.currentStock, bucket.targetCapacity, 0.35D, 1.35D, fullRatio, bucket.scarcityBonus);
        assertEquals(0.0D, fullMult, 0.0D, "满仓时收购乘数必须严格归 0（停收防机器挂机刷钱）");

        // 次日自然消化 20% 库存后重新恢复收购
        bucket.tickDaily(11L, 0.20D, 0.05D, 0.40D);
        assertEquals(160, bucket.currentStock, "满仓 200 经单日 20% 消化后应剩余 160");
        assertFalse(bucket.isWarehouseFull(fullRatio));
        double resumedMult = EcoPricing.computeBidInventoryMultiplier(
                bucket.currentStock, bucket.targetCapacity, 0.35D, 1.35D, fullRatio, bucket.scarcityBonus);
        assertTrue(resumedMult > 0.0D, "自然消化腾出仓位后应恢复收购报价");
    }

    @Test
    @DisplayName("4. 官方总钱箱保底安全线截断与未入账差额原路冲正回补（refundOutflow 零漏账）")
    void treasuryFloorProtectionAndRefundOutflow() {
        ShopTreasury treasury = new ShopTreasury(50_000L, 50_000L);
        long minFloor = 20_000L;

        // 储备等于锚定目标时健康系数严格为 1.00
        assertEquals(1.00D, treasury.healthFactor(0.65D, 1.25D), 1e-6D);
        assertEquals(30_000L, treasury.spendableAboveFloor(minFloor));

        // 模拟钱包仅剩 12,000 容量，但请求出账 20,000：未入账的 8,000 走 refundOutflow 原路退回
        long drawn = treasury.outflowUpTo(20_000L, minFloor, 100L);
        assertEquals(20_000L, drawn);
        assertEquals(30_000L, treasury.balance);
        assertEquals(20_000L, treasury.outflowToday);

        treasury.refundOutflow(8_000L, 100L);
        assertEquals(38_000L, treasury.balance, "未入账差额必须精确加回总钱箱余额");
        assertEquals(12_000L, treasury.outflowToday, "当日支出统计必须同步冲减差额");
        assertEquals(0L, treasury.inflowToday, "差额退回绝不能虚增今日商店消费进账");

        // 玩家再请求结算 45,000 星币，总钱箱最多只允许拨付剩余超出保底线的 18,000 星币
        long paidRest = treasury.outflowUpTo(45_000L, minFloor, 100L);
        assertEquals(18_000L, paidRest, "超出保底安全线的部分必须精确截断");
        assertEquals(minFloor, treasury.balance, "总钱箱余额绝不能跌破保底安全线");
        assertEquals(30_000L, treasury.outflowToday);

        // 触底后再请求兑付，返回 0，余额纹丝不动
        assertEquals(0L, treasury.outflowUpTo(5_000L, minFloor, 100L));
        assertEquals(minFloor, treasury.balance);

        // 有玩家在购物城消费回流 12,000 星币，钱箱重新具备兑付能力
        treasury.inflow(12_000L, 100L);
        assertEquals(32_000L, treasury.balance);
        assertEquals(12_000L, treasury.inflowToday);

        // 跨日后当日统计归零，但总钱箱余额完整保留
        treasury.rollDayIfNeeded(101L);
        assertEquals(32_000L, treasury.balance);
        assertEquals(0L, treasury.inflowToday);
        assertEquals(0L, treasury.outflowToday);
    }

    @Test
    @DisplayName("5. 每日收购日内官店降价二次钳位防套利：早间锁价不得反超实时官店卖价 82%")
    void dailyBuybackLiveReclampAgainstIntradayMallDrop() {
        // 早上库存偏低(stock=20/100)时官店卖价较高，早间生成的收购锁价为 110
        long morningMallAsk = EcoPricing.computeAskUnitPure(100L, 20, 100, 0.75D, 2.20D);
        long morningLockedBid = EcoPricing.clampBidAgainstAsk(110L, morningMallAsk, 0.82D);
        assertTrue(morningLockedBid > 82L, "早上官店缺货高价时允许较高的早间收购报价");

        // 下午大量玩家交货后官店库存升至满积压(stock=200/100)，官店实时售价跌至约 85
        long afternoonMallAsk = EcoPricing.computeAskUnitPure(100L, 200, 100, 0.75D, 2.20D);
        long liveClampedBid = EcoPricing.clampBidAgainstAsk(morningLockedBid, afternoonMallAsk, 0.82D);

        assertTrue(liveClampedBid <= (long) Math.floor(afternoonMallAsk * 0.82D),
                "日内官店售价下跌后，结算期二次钳位必须将早间锁价压回实时售价的 82% 以内");
        assertTrue(liveClampedBid < afternoonMallAsk,
                "结算期实付单价必须严格低于当刻官店实时卖价，彻底封死日内时间差套利");
    }

    @Test
    @DisplayName("6. 职业经验连升封顶与专注点跨天累积恢复（3000 点硬上限）")
    void professionLevelingAndFocusCap() {
        PlayerProfessionProfile profile = new PlayerProfessionProfile();

        // 首次初始化给 1 天专注点（1000）
        profile.refreshFocusIfNeeded(10L, 1000, 3000);
        assertEquals(1000.0D, profile.focusPoints, 1e-6D);

        // 同一天重复刷新不多给
        profile.refreshFocusIfNeeded(10L, 1000, 3000);
        assertEquals(1000.0D, profile.focusPoints, 1e-6D);

        // 隔 1 天登录恢复至 2000
        profile.refreshFocusIfNeeded(11L, 1000, 3000);
        assertEquals(2000.0D, profile.focusPoints, 1e-6D);

        // 离线 7 天回归，恢复后严格封顶在 3000（攒 3 天护肝上限）
        profile.refreshFocusIfNeeded(18L, 1000, 3000);
        assertEquals(3000.0D, profile.focusPoints, 1e-6D);

        // 升级测试：Lv.1 -> Lv.2 需要 60 XP
        assertEquals(1, profile.getLevel("cooking"));
        int lvAfter60 = profile.addXp("cooking", 60L);
        assertEquals(2, lvAfter60);
        assertEquals(0L, profile.getXp("cooking"));

        // 海量经验直冲满级 Lv.100 封顶不溢出
        int maxLv = profile.addXp("cooking", 10_000_000L);
        assertEquals(PlayerProfessionProfile.MAX_LEVEL, maxLv);
        assertEquals(0L, profile.getXp("cooking"));

        // 专精省点曲线验证：Lv.1 耗 24 点，Lv.100 耗约 6.05 点
        double costLv1 = EcoPricing.computeFocusCostPerItem(1, 24.0D);
        double costLv100 = EcoPricing.computeFocusCostPerItem(100, 24.0D);
        assertEquals(24.0D, costLv1, 1e-6D);
        assertTrue(costLv100 < 6.1D && costLv100 > 6.0D, "Lv.100 单件专注消耗应降至约 6.0 点");

        // 近 7 日官方核验实业营收滚动窗口验证
        profile.recordVerifiedRevenue(100L, "cooking", 1200L);
        profile.recordVerifiedRevenue(105L, "cooking", 800L);
        profile.recordVerifiedRevenue(106L, "farming", 500L);
        assertEquals(2000L, profile.sumVerifiedRevenue(106L, "cooking", 7));
        assertEquals(800L, profile.sumVerifiedRevenue(107L, "cooking", 7), "第 100 天的营收在第 107 天应滑出 7 日窗口");
    }

    @Test
    @DisplayName("7. 方案 1 零初库做市曲线与开服预设包核心防套利红线校验")
    void optionOneZeroInitialStockAndPresetSanity() throws Exception {
        // 方案 1：进度物资开服 0 库存时卖价处于断货高位，随着先驱玩家供货逐步回落至锚定价
        long diamondZeroStockAsk = EcoPricing.computeAskUnitPure(490L, 0, 32, 0.75D, 1.85D);
        long diamondHalfStockAsk = EcoPricing.computeAskUnitPure(490L, 16, 32, 0.75D, 1.85D);
        long diamondFullTargetAsk = EcoPricing.computeAskUnitPure(490L, 32, 32, 0.75D, 1.85D);
        assertTrue(diamondZeroStockAsk > diamondHalfStockAsk && diamondHalfStockAsk > diamondFullTargetAsk,
                "0 初库进度物资随玩家实业供货增加，官店售价应平滑回落");
        assertEquals(490L, diamondFullTargetAsk);
    }
}
