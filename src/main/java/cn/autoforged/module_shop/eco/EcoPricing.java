package cn.autoforged.module_shop.eco;

import cn.autoforged.module_shop.Config;
import cn.autoforged.module_shop.data.BlackMarketBucket;
import cn.autoforged.module_shop.data.ConfiguredOffer;
import cn.autoforged.module_shop.data.MallItem;
import cn.autoforged.module_shop.data.ShopSavedData;
import cn.autoforged.module_shop.data.SupplyMode;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * 阿尔比恩式官方做市商与黑市无套利定价引擎。
 *
 * <p>纯数学核与业务包装分层设计，确保所有核心公式均可脱离 MC 注册表直接执行 JUnit 高频防套利单测。
 */
public final class EcoPricing {
    private EcoPricing() {
    }

    // =========================================================================
    // 纯数学核（零外部静态状态依赖，供业务与 JUnit 单测共用）
    // =========================================================================

    /**
     * 购物城单件售价乘数 M_ask(r)：
     * 当 r = 1.0（目标平衡库存）时严格等于 1.00；r = 0（断货边缘）时升至 askCeiling；r 越大越趋近 askFloor。
     */
    public static double computeAskMultiplier(int virtualStock, int targetStock, double askFloor, double askCeiling) {
        if (targetStock <= 0) {
            return 1.0D;
        }
        double floor = Math.max(0.05D, Math.min(0.99D, askFloor));
        double ceil = Math.max(1.01D, askCeiling);
        double r = Math.max(0.0D, (double) Math.max(0, virtualStock) / (double) targetStock);
        double kAsk = (ceil - 1.0D) / Math.max(0.05D, 1.0D - floor);
        return floor + (ceil - floor) / (1.0D + kAsk * r);
    }

    public static long computeAskUnitPure(long anchorPrice, int virtualStock, int targetStock,
                                          double askFloor, double askCeiling) {
        long anchor = Math.max(1L, anchorPrice);
        if (targetStock <= 0) {
            return anchor;
        }
        double mult = computeAskMultiplier(virtualStock, targetStock, askFloor, askCeiling);
        return Math.max(1L, Math.round(anchor * mult));
    }

    /**
     * 批量购买逐件积分计价：保证「一次买 N 个」与「分 N 次每次买 1 个」总花费 100% 相等，彻底消灭拆单漏洞。
     */
    public static long quoteBatchAskPure(long anchorPrice, int currentStock, int targetStock, int count,
                                         double askFloor, double askCeiling) {
        if (count <= 0) {
            return 0L;
        }
        long sum = 0L;
        int cur = Math.max(0, currentStock);
        for (int i = 0; i < count; i++) {
            int stepStock = Math.max(0, cur - i);
            long unit = computeAskUnitPure(anchorPrice, stepStock, targetStock, askFloor, askCeiling);
            long room = Long.MAX_VALUE - sum;
            sum += Math.min(unit, room);
        }
        return sum;
    }

    /**
     * 黑市/官方收购库存乘数 M_bid(r)：
     * - 当库存达到 targetStock * fullRatio 时返回 0.0（满仓直接停收！专治全自动刷物机倾销）；
     * - 当 r = 0 时为 bidCeiling * (1 + scarcityBonus)（缺货悬赏）；
     * - 当 r = 1.0 且无缺货累积时严格等于 1.00。
     */
    public static double computeBidInventoryMultiplier(int effectiveStock, int targetStock,
                                                       double bidFloor, double bidCeiling,
                                                       double fullRatio, double scarcityBonus) {
        if (targetStock <= 0) {
            return 1.0D + Math.max(0.0D, scarcityBonus);
        }
        int maxCap = Math.max(1, (int) Math.round(targetStock * Math.max(1.0D, fullRatio)));
        if (effectiveStock >= maxCap) {
            return 0.0D; // 满仓停收
        }
        double floor = Math.max(0.05D, Math.min(0.99D, bidFloor));
        double ceil = Math.max(1.00D, bidCeiling);
        double r = Math.max(0.0D, (double) Math.max(0, effectiveStock) / (double) targetStock);
        double kBid = (ceil - 1.0D) / Math.max(0.05D, 1.0D - floor);
        double baseMult = floor + (ceil - floor) / (1.0D + kBid * r);
        // 仅在库存低于目标容量一半时叠加上浮的缺货爬坡奖金
        double scarcityFactor = r < 0.5D ? (1.0D + Math.max(0.0D, scarcityBonus) * (1.0D - r * 2.0D)) : 1.0D;
        return baseMult * scarcityFactor;
    }

    /**
     * 单件物品消耗的专注点（阿尔比恩专精省点曲线）：
     * Lv.1 消耗 baseCost（默认 24），Lv.50 约 9.7，Lv.100 约 6.0（同样 1000 专注可多精销近 4 倍货量）。
     */
    public static double computeFocusCostPerItem(int level, double baseCost) {
        int lv = Math.max(1, Math.min(100, level));
        return Math.max(0.5D, baseCost) / (1.0D + 0.03D * (lv - 1));
    }

    /**
     * 开启专注精销时的职业加成倍率（含 Lv.1 保底 +5% 起步爽感，线性升至 Lv.100 的 1 + maxBonus，再乘冷门紧缺倍率）。
     */
    public static double computeProfessionFocusMultiplier(int level, double maxBonus, double scarcityMult) {
        int lv = Math.max(1, Math.min(100, level));
        double top = Math.max(0.0D, maxBonus);
        double minStarter = Math.min(0.05D, top);
        double bonus = minStarter + (top - minStarter) * ((lv - 1) / 99.0D);
        return (1.0D + bonus) * Math.max(1.0D, scarcityMult);
    }

    /**
     * 防套利硬钳位：无论叠加多少加成，单件收购实付价绝不超过官方同款在售价格的 maxBidToAskRatio（且严格低于官方卖价）。
     */
    public static long clampBidAgainstAsk(long rawBid, long officialAskPrice, double maxBidToAskRatio) {
        if (rawBid <= 0L) {
            return 0L;
        }
        if (officialAskPrice <= 0L) {
            return rawBid;
        }
        if (officialAskPrice <= 1L) {
            return 0L; // 官方只卖 1 星币的物品绝不允许卖回给官方套利
        }
        double ratio = Math.max(0.05D, Math.min(0.98D, maxBidToAskRatio));
        long cap = Math.max(1L, (long) Math.floor(officialAskPrice * ratio));
        cap = Math.min(cap, officialAskPrice - 1L);
        return Math.min(rawBid, cap);
    }

    // =========================================================================
    // 游戏内业务包装（对接 Config、ShopSavedData、MallItem 与 BlackMarketBucket）
    // =========================================================================

    public static boolean isEcoActive() {
        try {
            return Config.INSTANCE.ecoEnabled.get();
        } catch (RuntimeException notLoaded) {
            return true;
        }
    }

    /** 计算购物城某商品在指定虚拟库存下的实时单件售价。 */
    public static long computeMallAskAtStock(MallItem item, int virtualStock) {
        if (item == null) {
            return 1L;
        }
        long fallback = Math.max(1L, item.price);
        if (!isEcoActive() || item.supplyMode != SupplyMode.PLAYER_POOL || item.stockMax <= 0 || item.unlimited()) {
            return fallback;
        }
        long anchor = Math.max(1L, item.basePrice > 0L ? item.basePrice : item.price);
        return computeAskUnitPure(
                anchor,
                virtualStock,
                item.stockMax,
                Config.INSTANCE.ecoAskFloor.get(),
                Config.INSTANCE.ecoAskCeiling.get()
        );
    }

    /** 购物城批量购买逐件积分总价。 */
    public static long quoteMallBatchAsk(MallItem item, int count) {
        if (item == null || count <= 0) {
            return 0L;
        }
        if (!isEcoActive() || item.supplyMode != SupplyMode.PLAYER_POOL || item.stockMax <= 0 || item.unlimited()
                || Config.mallStockPersonal()) {
            long unit = Math.max(1L, item.price);
            long room = Long.MAX_VALUE / count;
            return unit > room ? Long.MAX_VALUE : unit * count;
        }
        long anchor = Math.max(1L, item.basePrice > 0L ? item.basePrice : item.price);
        return quoteBatchAskPure(
                anchor,
                Math.max(0, item.stock),
                item.stockMax,
                count,
                Config.INSTANCE.ecoAskFloor.get(),
                Config.INSTANCE.ecoAskCeiling.get()
        );
    }

    /** 查找购物城里关联的做市货架商品（匹配同物品 ID 的白板货架或精确组件匹配）。 */
    public static MallItem findPlayerPoolItem(ShopSavedData data, ItemStack stack) {
        if (data == null || stack == null || stack.isEmpty()) {
            return null;
        }
        for (MallItem item : data.mallPool) {
            if (item.supplyMode == SupplyMode.PLAYER_POOL && !item.stack.isEmpty()
                    && ItemStack.isSameItemSameComponents(item.stack, stack)) {
                return item;
            }
        }
        return null;
    }

    /**
     * 查找官方商店（购物城任意模式 + 每日商店池）中同物品 ID 的最低可能售价（用于 0.82 防倒卖硬钳位）。
     * 按物品 Item 本身查：防止玩家在商城低价买白板物品、附点低成本属性或直接倒手卖回出货箱套利！
     */
    public static long findCheapestOfficialAskForItem(ShopSavedData data, ItemStack stack, int virtualStockOffset) {
        if (data == null || stack == null || stack.isEmpty()) {
            return -1L;
        }
        long cheapest = Long.MAX_VALUE;
        for (MallItem item : data.mallPool) {
            if (item.stack.isEmpty() || item.stack.getItem() != stack.getItem()) {
                continue;
            }
            long ask;
            if (item.supplyMode == SupplyMode.PLAYER_POOL && item.stockMax > 0 && !item.unlimited()) {
                // 玩家从商城买走 1 件再卖回 1 件时，商城那一步收的是 stock + offset + 1 处的 Ask 价
                ask = computeMallAskAtStock(item, Math.max(0, item.stock + virtualStockOffset + 1));
            } else {
                ask = Math.max(1L, item.price);
            }
            cheapest = Math.min(cheapest, ask);
        }
        for (ConfiguredOffer offer : data.shopPool) {
            if (!offer.stack.isEmpty() && offer.stack.getItem() == stack.getItem() && offer.price > 0L) {
                cheapest = Math.min(cheapest, offer.price);
            }
        }
        return cheapest == Long.MAX_VALUE ? -1L : cheapest;
    }

    /** 生成黑市仓库桶键名（按注册表物品 ID）。 */
    public static String bucketKey(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "";
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id == null ? "" : id.toString();
    }

    /** 获取或创建某物品对应的黑市库存桶。 */
    public static BlackMarketBucket getOrCreateBucket(ShopSavedData data, ItemStack stack, int ruleDailyMax) {
        String key = bucketKey(stack);
        int defaultCap = 256;
        try {
            defaultCap = Config.INSTANCE.blackMarketDefaultCap.get();
        } catch (RuntimeException ignored) {
        }
        int targetCap = ruleDailyMax > 0 ? Math.max(32, ruleDailyMax * 4) : defaultCap;
        if (data == null || key.isEmpty()) {
            return new BlackMarketBucket(0, targetCap);
        }
        return data.blackMarket.computeIfAbsent(key, k -> new BlackMarketBucket(0, targetCap));
    }

    /**
     * 计算官方单件实时收购价（综合：基础价、库存/黑市涨跌与满仓停收、总钱箱健康度、模组品质溢价、职业专注加成、0.82 防套利锁）。
     *
     * @return 0 = 满仓停收或废品拒收；>0 = 实付星币单价
     */
    public static long computeBuyoutUnitAtStock(ShopSavedData data, ItemStack stack, long baseBidPrice,
                                                double qualityMult, double professionAndFocusMult,
                                                int ruleDailyMax, int virtualStockOffset) {
        if (baseBidPrice <= 0L || qualityMult <= 0.0D || stack == null || stack.isEmpty()) {
            return 0L;
        }
        if (!isEcoActive() || data == null) {
            return Math.max(1L, Math.round(baseBidPrice * qualityMult * Math.max(1.0D, professionAndFocusMult)));
        }

        double fullRatio = Config.INSTANCE.blackMarketWarehouseFullRatio.get();
        double bidFloor = Config.INSTANCE.ecoBidFloor.get();
        double bidCeiling = Config.INSTANCE.ecoBidCeiling.get();

        MallItem linked = findPlayerPoolItem(data, stack);
        double invMult;
        if (linked != null && linked.stockMax > 0 && !linked.unlimited()) {
            int effStock = Math.max(0, linked.stock + virtualStockOffset);
            BlackMarketBucket b = data.blackMarket.get(bucketKey(stack));
            double scarcity = b == null ? 0.0D : b.scarcityBonus;
            invMult = computeBidInventoryMultiplier(effStock, linked.stockMax, bidFloor, bidCeiling, fullRatio, scarcity);
        } else {
            BlackMarketBucket bucket = getOrCreateBucket(data, stack, ruleDailyMax);
            int effStock = Math.max(0, bucket.currentStock + virtualStockOffset);
            invMult = computeBidInventoryMultiplier(effStock, bucket.targetCapacity, bidFloor, bidCeiling,
                    fullRatio, bucket.scarcityBonus);
        }

        if (invMult <= 0.0D) {
            return 0L; // 仓库已满，暂停收购该物资
        }

        double treasuryMult = data.treasury.healthFactor(
                Config.INSTANCE.treasuryMinFactor.get(),
                Config.INSTANCE.treasuryMaxFactor.get()
        );

        double combined = invMult * treasuryMult * qualityMult * Math.max(0.1D, professionAndFocusMult);
        long rawBid = Math.max(1L, Math.round(baseBidPrice * combined));

        // 0.82 硬钳位防套利：如果该物品在官方购物城或每日商店有售，绝不允许收购价高于商城卖价的 82%
        // 注意：若是带高年份/黄金鱼等无法从商城白板直接买到的极品（qualityMult > 1.05 且商城卖的是白板），
        // 仅当与商城货架完全同组件（白板）时锁死在白板卖价 82% 以内；若商城卖的是同组件物品也严格锁死！
        MallItem exactMall = findExactAnyMallItem(data, stack);
        if (exactMall != null) {
            long exactAsk = (exactMall.supplyMode == SupplyMode.PLAYER_POOL && exactMall.stockMax > 0 && !exactMall.unlimited())
                    ? computeMallAskAtStock(exactMall, Math.max(0, exactMall.stock + virtualStockOffset + 1))
                    : Math.max(1L, exactMall.price);
            rawBid = clampBidAgainstAsk(rawBid, exactAsk, Config.INSTANCE.ecoMaxBidToAskRatio.get());
        } else {
            // 即便物品带了简单组件（例如铁砧改名），也不能用白板进货刷钱：只有真实模组品质溢价才允许按品质比例放大硬上限，普通改名 qualityMult == 1.0 仍严格受白板卖价 82% 限制！
            long baseOfficialAsk = findCheapestOfficialAskForItem(data, stack, virtualStockOffset);
            if (baseOfficialAsk > 0L) {
                long adjustedOfficialAsk = qualityMult > 1.001D
                        ? Math.max(baseOfficialAsk, Math.round(baseOfficialAsk * qualityMult))
                        : baseOfficialAsk;
                rawBid = clampBidAgainstAsk(rawBid, adjustedOfficialAsk, Config.INSTANCE.ecoMaxBidToAskRatio.get());
            }
        }
        return rawBid;
    }

    private static MallItem findExactAnyMallItem(ShopSavedData data, ItemStack stack) {
        for (MallItem item : data.mallPool) {
            if (!item.stack.isEmpty() && ItemStack.isSameItemSameComponents(item.stack, stack)) {
                return item;
            }
        }
        return null;
    }

    /** 查询某物资在官方做市货架或黑市仓库桶中的当前剩余可收容量（非 Eco 模式返回 Integer.MAX_VALUE）。 */
    public static int remainingWarehouseCapacity(ShopSavedData data, ItemStack stack, int ruleDailyMax) {
        if (!isEcoActive() || data == null || stack == null || stack.isEmpty()) {
            return Integer.MAX_VALUE;
        }
        double fullRatio = Config.INSTANCE.blackMarketWarehouseFullRatio.get();
        MallItem linked = findPlayerPoolItem(data, stack);
        if (linked != null && linked.stockMax > 0 && !linked.unlimited()) {
            int maxCap = Math.max(1, (int) Math.round(linked.stockMax * Math.max(1.0D, fullRatio)));
            return Math.max(0, maxCap - Math.max(0, linked.stock));
        }
        BlackMarketBucket bucket = getOrCreateBucket(data, stack, ruleDailyMax);
        return bucket.remainingCapacity(fullRatio);
    }

    /**
     * 每日收购实时结算二次钳位：防止早间锁定的收购单价在日内官店补货降价后反超 82% 防套利上限（或 1 星币禁收）。
     */
    public static long clampLiveBuybackUnit(ShopSavedData data, ItemStack stack,
                                            long lockedUnitPrice, int virtualStockOffset) {
        if (lockedUnitPrice <= 0L || stack == null || stack.isEmpty()) {
            return 0L;
        }
        if (!isEcoActive() || data == null) {
            return lockedUnitPrice;
        }
        cn.autoforged.module_shop.eco.quality.QualityEvaluator.QualityResult qr =
                cn.autoforged.module_shop.eco.quality.QualityEvaluator.evaluate(stack);
        if (qr.rejected()) {
            return 0L;
        }
        double maxRatio = Config.INSTANCE.ecoMaxBidToAskRatio.get();
        MallItem exactMall = findExactAnyMallItem(data, stack);
        if (exactMall != null) {
            long exactAsk = (exactMall.supplyMode == SupplyMode.PLAYER_POOL
                    && exactMall.stockMax > 0 && !exactMall.unlimited())
                    ? computeMallAskAtStock(exactMall, Math.max(0, exactMall.stock + virtualStockOffset + 1))
                    : Math.max(1L, exactMall.price);
            return clampBidAgainstAsk(lockedUnitPrice, exactAsk, maxRatio);
        }
        long baseOfficialAsk = findCheapestOfficialAskForItem(data, stack, virtualStockOffset);
        if (baseOfficialAsk > 0L) {
            double qMult = qr.multiplier();
            long adjustedOfficialAsk = qMult > 1.001D
                    ? Math.max(baseOfficialAsk, Math.round(baseOfficialAsk * qMult))
                    : baseOfficialAsk;
            return clampBidAgainstAsk(lockedUnitPrice, adjustedOfficialAsk, maxRatio);
        }
        return lockedUnitPrice;
    }
}
