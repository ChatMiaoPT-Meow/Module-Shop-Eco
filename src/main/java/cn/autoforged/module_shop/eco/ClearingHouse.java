package cn.autoforged.module_shop.eco;

import cn.autoforged.module_shop.Config;
import cn.autoforged.module_shop.api.ShippingApi;
import cn.autoforged.module_shop.api.event.ApiEvents;
import cn.autoforged.module_shop.api.event.SellCompletedEvent;
import cn.autoforged.module_shop.api.event.ShippingSettledEvent;
import cn.autoforged.module_shop.data.BlackMarketBucket;
import cn.autoforged.module_shop.data.ConfiguredOffer;
import cn.autoforged.module_shop.data.DailyBuybackEntry;
import cn.autoforged.module_shop.data.MallItem;
import cn.autoforged.module_shop.data.PlayerAccount;
import cn.autoforged.module_shop.data.Rarity;
import cn.autoforged.module_shop.data.ShippingBin;
import cn.autoforged.module_shop.data.ShopNbt;
import cn.autoforged.module_shop.data.ShopSavedData;
import cn.autoforged.module_shop.data.SupplyMode;
import cn.autoforged.module_shop.eco.profession.ProfessionConfig;
import cn.autoforged.module_shop.eco.profession.ProfessionManager;
import cn.autoforged.module_shop.eco.quality.QualityEvaluator;
import cn.autoforged.module_shop.shop.ShippingRules;
import cn.autoforged.module_shop.shop.ShopManager;
import cn.autoforged.module_shop.shop.TransactionLog;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

/**
 * 全服唯一原子收银结算总台（ClearingHouse）。
 *
 * <p>统一管理：
 * <ul>
 *   <li>出货箱动态报价、品质识别、专注点扣减、总钱箱限额兑付、做市库存入库与实业营收登记；</li>
 *   <li>每日收购「4 本职 + 2 跨职」定向生成与「30% 入库 + 70% 销毁」泄洪结算；</li>
 *   <li>购物城 10% 高价微补货底仓与黑市每日自然消化/缺货爬坡；</li>
 *   <li>外币（如东篱五元纸币）单向三重限额结汇阀门；</li>
 *   <li>客户端职业与总钱箱实时快照拼装。</li>
 * </ul>
 */
public final class ClearingHouse {
    private ClearingHouse() {
    }

    /** 每日推进黑市自然消化与零库存缺货悬赏爬坡。 */
    public static void tickBlackMarketDaily(MinecraftServer server, ShopSavedData data) {
        if (server == null || data == null || !EcoPricing.isEcoActive()) {
            return;
        }
        long gameDay = server.overworld().getDayTime() / 24000L;
        if (data.lastBlackMarketDecayDay == gameDay) {
            return;
        }
        if (data.lastBlackMarketDecayDay == Long.MIN_VALUE) {
            data.lastBlackMarketDecayDay = gameDay;
            data.setDirty();
            return;
        }
        data.lastBlackMarketDecayDay = gameDay;

        double decay = Config.INSTANCE.blackMarketDailyDecayRatio.get();
        double step = Config.INSTANCE.blackMarketScarcityDailyStep.get();
        double maxScarcity = Config.INSTANCE.blackMarketScarcityMaxBonus.get();

        // 1) 先统一归口结算购物城 PLAYER_POOL 做市货架关联的键（严格单日结算 1 次，杜绝与黑市桶重复累加悬赏或覆盖库存）
        java.util.Set<String> mallPoolKeys = new java.util.HashSet<>();
        for (MallItem item : data.mallPool) {
            if (item.supplyMode == SupplyMode.PLAYER_POOL && item.stockMax > 0 && !item.unlimited()) {
                String key = EcoPricing.bucketKey(item.stack);
                if (!key.isEmpty()) {
                    mallPoolKeys.add(key);
                    if (item.stock > item.stockMax) {
                        // 超出目标平衡库存的积压部分每天自然消化 20%，正常水位内的玩家供货予以保留
                        int excess = item.stock - item.stockMax;
                        int digest = Math.max(1, (int) Math.round(excess * decay));
                        item.stock = Math.max(item.stockMax, item.stock - digest);
                    }
                    BlackMarketBucket b = data.blackMarket.computeIfAbsent(key,
                            k -> new BlackMarketBucket(item.stock, item.stockMax));
                    b.currentStock = Math.max(0, item.stock);
                    b.targetCapacity = Math.max(1, item.stockMax);
                    if (b.lastSettleDay != gameDay) {
                        b.lastSettleDay = gameDay;
                        if (item.stock <= 0 && step > 0.0D && maxScarcity > 0.0D) {
                            b.scarcityBonus = Math.min(maxScarcity, b.scarcityBonus + step);
                        } else if (item.stock >= item.stockMax / 2) {
                            b.scarcityBonus = 0.0D;
                        }
                    }
                }
            }
        }
        // 2) 对未绑定做市货架的独立黑市库存桶执行每日自然消化与断供爬坡
        for (Map.Entry<String, BlackMarketBucket> entry : data.blackMarket.entrySet()) {
            if (!mallPoolKeys.contains(entry.getKey()) && entry.getValue() != null) {
                entry.getValue().tickDaily(gameDay, decay, step, maxScarcity);
            }
        }
        data.setDirty();
    }

    /** 购物城定时补货（Eco 模式：PLAYER_POOL 仅在低于 10% 应急底仓时补齐至 10%，绝不凭空补满抢玩家市场）。 */
    public static int restockMallEco(ShopSavedData data, int unifiedAmount, boolean personal) {
        int restored = 0;
        boolean eco = EcoPricing.isEcoActive();
        for (MallItem item : data.mallPool) {
            boolean changed = false;
            if (!item.unlimited() && item.stockMax >= 0) {
                int target;
                if (eco && item.supplyMode == SupplyMode.PLAYER_POOL && !personal) {
                    int pct = Math.max(0, Math.min(100, item.floorRestockPercent));
                    target = pct <= 0 ? 0 : Math.max(1, item.stockMax * pct / 100);
                } else {
                    target = unifiedAmount > 0 ? unifiedAmount : item.stockMax;
                }
                if (target > 0 && item.stock < target) {
                    item.stock = target;
                    changed = true;
                }
            }
            if (personal && !item.personalUsed.isEmpty()) {
                item.clearPersonalUsed();
                changed = true;
            }
            if (changed) {
                restored++;
            }
        }
        return restored;
    }

    /** 将成交物品按比例注入购物城做市库存或黑市库存桶。 */
    public static void injectSoldItemIntoMarket(ShopSavedData data, ItemStack soldStack, int count,
                                                int ruleDailyMax, double toMallRatio) {
        if (data == null || soldStack == null || soldStack.isEmpty() || count <= 0 || !EcoPricing.isEcoActive()) {
            return;
        }
        double fullRatio = Config.INSTANCE.blackMarketWarehouseFullRatio.get();
        // 概率取整：期望值严格等于 count×ratio，逐件卖出与整批卖出的长期入库量一致（避免 round(1×0.3)=0 导致逐件卖永不入库）
        double exactInto = count * Math.max(0.0D, Math.min(1.0D, toMallRatio));
        int intoStock = (int) Math.floor(exactInto);
        if (java.util.concurrent.ThreadLocalRandom.current().nextDouble() < exactInto - intoStock) {
            intoStock++;
        }
        if (intoStock <= 0) {
            return;
        }

        // 优先注入白板同款 PLAYER_POOL 购物城货架（注意：带个人定制/特殊品质组件的精品如仅商城卖白板，只向白板货架注入或进黑市桶）
        MallItem linked = EcoPricing.findPlayerPoolItem(data, soldStack);
        if (linked != null && linked.stockMax > 0 && !linked.unlimited()) {
            int maxCap = Math.max(linked.stockMax, (int) Math.round(linked.stockMax * fullRatio));
            linked.stock = Math.min(maxCap, Math.max(0, linked.stock) + intoStock);
            BlackMarketBucket b = data.blackMarket.get(EcoPricing.bucketKey(soldStack));
            if (b != null) {
                b.currentStock = linked.stock;
                if (linked.stock >= linked.stockMax / 2) {
                    b.scarcityBonus = 0.0D;
                }
            }
        } else {
            BlackMarketBucket bucket = EcoPricing.getOrCreateBucket(data, soldStack, ruleDailyMax);
            bucket.addStock(intoStock, fullRatio);
        }
    }

    /** 实时查询某玩家出售单件物品的官方到手预估单价（不扣减专注，供 API 与界面展示）。 */
    public static long liveQuoteShippingUnit(MinecraftServer server, UUID playerId, ItemStack stack) {
        if (server == null || stack == null || stack.isEmpty()) {
            return 0L;
        }
        ShippingRules.Rule rule = ShippingRules.match(stack);
        if (rule == null) {
            return 0L;
        }
        QualityEvaluator.QualityResult qr = QualityEvaluator.evaluate(stack);
        if (qr.rejected()) {
            return 0L;
        }
        ShopSavedData data = ShopManager.data(server);
        PlayerAccount account = playerId == null ? null : data.account(playerId);
        long gameDay = server.overworld().getDayTime() / 24000L;
        double profMult = ProfessionManager.previewProfessionMultiplier(data, account, gameDay, stack);
        ShippingApi.EvalResult ext = ShippingModifierRegistry.evaluate(playerId, stack, rule.price(), rule.dailyMax());
        long basePrice = Math.max(rule.price(), qr.intrinsicBaseFloor());
        return EcoPricing.computeBuyoutUnitAtStock(
                data, stack, basePrice,
                qr.multiplier(),
                profMult * ext.priceMultiplier(),
                rule.dailyMax(), 0
        );
    }

    /**
     * 原子结算单个玩家的出货箱：
     * 逐件检查品质拒收 -> 黑市满仓停收 -> 专注点余额动态切换 -> 个人日限额 -> 钱包空间 -> 官方总钱箱保底余额。
     * 任何一步受限都只截断本次卖出数量，未卖出的物品 100% 原封不动留在箱内，绝不吞半个物品！
     */
    public static void settleShippingBinEco(MinecraftServer server, ShopSavedData data, UUID owner,
                                            PlayerAccount account, long gameDay, long realDay,
                                            Runnable postSyncCallback) {
        ShippingBin bin = account.shipping;
        bin.resize(Config.shippingSlotsFor(bin.level));
        ProfessionManager.ensureFocusRefreshed(account);

        if (bin.soldDay != realDay) {
            bin.soldDay = realDay;
            bin.soldToday = 0L;
            bin.ruleUsed.clear();
        }
        if (bin.lastSettleGameDay != Long.MIN_VALUE && bin.lastSettleGameDay > gameDay) {
            bin.lastSettleGameDay = gameDay - 1L;
        }
        if (bin.lastSettleGameDay >= gameDay) {
            return;
        }
        bin.lastSettleGameDay = gameDay;
        if (bin.isEmpty()) {
            return;
        }

        long baseLimit = Config.shippingDailyLimit(bin.level);
        long extraLimit = ShippingModifierRegistry.extraDailyLimit(owner, baseLimit);
        long totalLimit = baseLimit == Long.MAX_VALUE ? Long.MAX_VALUE
                : (Math.min(Long.MAX_VALUE - baseLimit, extraLimit) + baseLimit);
        long remainingQuota = totalLimit == Long.MAX_VALUE ? Long.MAX_VALUE : Math.max(0L, totalLimit - bin.soldToday);

        long maxBalance = Config.maxBalanceLimit();
        boolean bankFull = false;
        if (maxBalance != Long.MAX_VALUE) {
            long walletRoom = Math.max(0L, maxBalance - account.balance);
            if (walletRoom < remainingQuota) {
                bankFull = true;
                remainingQuota = walletRoom;
            }
        }

        boolean eco = EcoPricing.isEcoActive();
        long treasuryMinFloor = Config.INSTANCE.treasuryMinFloor.get();
        long treasurySpendable = eco ? data.treasury.spendableAboveFloor(treasuryMinFloor) : Long.MAX_VALUE;
        boolean treasuryCapped = false;
        boolean warehouseCapped = false;
        boolean wasteRejected = false;

        long totalPayout = 0L;
        long specialtyPayout = 0L;
        int totalSoldCount = 0;
        List<String> soldSummary = new ArrayList<>();
        List<ShippingSettledEvent.SettledLine> settledLines = new ArrayList<>();
        double toMallRatio = Config.INSTANCE.ecoShippingToMallRatio.get();

        for (int slotIdx = 0; slotIdx < bin.slots.size(); slotIdx++) {
            ItemStack stack = bin.slots.get(slotIdx);
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            ShippingRules.Rule rule = ShippingRules.match(stack);
            if (rule == null) {
                continue;
            }
            QualityEvaluator.QualityResult qr = QualityEvaluator.evaluate(stack);
            if (qr.rejected()) {
                wasteRejected = true; // 烧糊菜 / 黑暗料理 / 失败品留在箱内拒收，绝不给钱也不销毁
                continue;
            }

            ShippingApi.EvalResult ext = ShippingModifierRegistry.evaluate(owner, stack, rule.price(), rule.dailyMax());
            int perDayMax = rule.dailyMax() <= 0 ? 0
                    : Math.max(1, (int) Math.round(rule.dailyMax() * ext.ruleDailyMaxMultiplier()));
            int usedCount = bin.ruleUsed.getOrDefault(rule.line(), 0);
            int maxByRule = perDayMax <= 0 ? stack.getCount() : Math.max(0, perDayMax - usedCount);
            if (maxByRule <= 0) {
                continue;
            }

            String profId = ProfessionManager.matchProfessionForOutput(stack);
            int profLevel = profId.isEmpty() ? 1 : account.career.getLevel(profId);
            double scarcityMult = profId.isEmpty() ? 1.0D : ProfessionManager.computeScarcityMultiplier(data, gameDay, profId);
            double focusCostPerUnit = EcoPricing.computeFocusCostPerItem(
                    profLevel, Config.INSTANCE.baseFocusCostPerItem.get());
            double focusBonusMult = EcoPricing.computeProfessionFocusMultiplier(
                    profLevel, Config.INSTANCE.maxProfessionPriceBonus.get(), scarcityMult);
            long baseUnit = Math.max(rule.price(), qr.intrinsicBaseFloor());

            int soldInThisSlot = 0;
            long gainInThisSlot = 0L;
            boolean anyFocusUsedInSlot = false;

            int tryCount = Math.min(stack.getCount(), maxByRule);
            for (int k = 0; k < tryCount; k++) {
                boolean canUseFocus = ProfessionManager.isEnabled()
                        && !profId.isEmpty()
                        && account.career.focusSellEnabled
                        && account.career.focusPoints + 1e-6D >= focusCostPerUnit;
                double profMult = (canUseFocus ? focusBonusMult : scarcityMult) * ext.priceMultiplier();

                long unitPrice = EcoPricing.computeBuyoutUnitAtStock(
                        data, stack, baseUnit, qr.multiplier(), profMult, rule.dailyMax(), soldInThisSlot);
                if (unitPrice <= 0L) {
                    warehouseCapped = true; // 该物资已满仓停收
                    break;
                }
                if (remainingQuota != Long.MAX_VALUE && unitPrice > remainingQuota) {
                    break;
                }
                if (eco && unitPrice > treasurySpendable) {
                    treasuryCapped = true;
                    break;
                }

                // 单件校验全部通过，扣减本件专注与额度
                if (canUseFocus) {
                    account.career.focusPoints = Math.max(0.0D, account.career.focusPoints - focusCostPerUnit);
                    anyFocusUsedInSlot = true;
                }
                soldInThisSlot++;
                gainInThisSlot += unitPrice;
                if (remainingQuota != Long.MAX_VALUE) {
                    remainingQuota -= unitPrice;
                }
                if (eco) {
                    treasurySpendable -= unitPrice;
                }
            }

            if (soldInThisSlot <= 0 || gainInThisSlot <= 0L) {
                continue;
            }

            ItemStack soldCopy = stack.copyWithCount(soldInThisSlot);
            injectSoldItemIntoMarket(data, soldCopy, soldInThisSlot, rule.dailyMax(), toMallRatio);

            if (perDayMax > 0) {
                bin.ruleUsed.put(rule.line(), usedCount + soldInThisSlot);
            }
            bin.slots.set(slotIdx, stack.getCount() > soldInThisSlot
                    ? stack.copyWithCount(stack.getCount() - soldInThisSlot)
                    : ItemStack.EMPTY);

            totalPayout += gainInThisSlot;
            totalSoldCount += soldInThisSlot;
            if (!profId.isEmpty()) {
                specialtyPayout += gainInThisSlot;
                account.career.recordVerifiedRevenue(gameDay, profId, gainInThisSlot);
            }
            soldSummary.add(soldCopy.getHoverName().getString() + "×" + soldInThisSlot);
            ResourceLocation itemKey = BuiltInRegistries.ITEM.getKey(soldCopy.getItem());
            settledLines.add(new ShippingSettledEvent.SettledLine(
                    itemKey == null ? "unknown" : itemKey.toString(),
                    soldInThisSlot,
                    gainInThisSlot / soldInThisSlot,
                    gainInThisSlot,
                    profId,
                    anyFocusUsedInSlot
            ));
        }

        ServerPlayer online = server.getPlayerList().getPlayer(owner);
        if (totalPayout <= 0L) {
            if (online != null && !bin.isEmpty()) {
                String msgKey;
                if (treasuryCapped) {
                    msgKey = "msg.module_shop.treasury_capped";
                } else if (warehouseCapped) {
                    msgKey = "msg.module_shop.warehouse_full";
                } else if (wasteRejected) {
                    msgKey = "msg.module_shop.waste_rejected";
                } else if (bankFull) {
                    msgKey = "msg.module_shop.shipping_bank_full";
                } else {
                    msgKey = "msg.module_shop.shipping_capped";
                }
                online.displayClientMessage(Component.translatable(msgKey).withStyle(ChatFormatting.YELLOW), false);
            }
            return;
        }

        // 从官方总钱箱实扣星币并入玩家钱包
        long paidByTreasury = eco
                ? data.treasury.outflowUpTo(totalPayout, treasuryMinFloor, realDay)
                : totalPayout;
        long credited = ShopManager.credit(account, paidByTreasury, maxBalance);
        if (credited < paidByTreasury) {
            long rest = paidByTreasury - credited;
            ShopManager.addMoneyMail(server, owner, rest, ItemStack.EMPTY, "shipping_unpaid", "", null);
        }

        bin.soldToday += paidByTreasury;
        bin.lastPayout = paidByTreasury;
        bin.lastCount = totalSoldCount;
        data.setDirty();

        TransactionLog.record(server, owner, TransactionLog.SELL, "出货箱", paidByTreasury, account.balance,
                String.join("、", soldSummary.size() > 6 ? soldSummary.subList(0, 6) : soldSummary)
                        + (soldSummary.size() > 6 ? " 等" : ""));

        // 补齐事件广播：驱动星途纪行与股市实业公司模块
        ApiEvents.post(new SellCompletedEvent(owner, "shipping", "shipping-bin", ItemStack.EMPTY, totalSoldCount, paidByTreasury));
        ApiEvents.post(new ShippingSettledEvent(owner, gameDay, paidByTreasury, specialtyPayout,
                totalSoldCount, settledLines));

        if (online != null) {
            online.displayClientMessage(Component.translatable("msg.module_shop.shipping_sold",
                    totalSoldCount, paidByTreasury).withStyle(ChatFormatting.GREEN), false);
            if (!bin.isEmpty()) {
                if (treasuryCapped) {
                    online.displayClientMessage(Component.translatable("msg.module_shop.treasury_capped")
                            .withStyle(ChatFormatting.YELLOW), false);
                } else if (warehouseCapped) {
                    online.displayClientMessage(Component.translatable("msg.module_shop.warehouse_full")
                            .withStyle(ChatFormatting.YELLOW), false);
                } else if (wasteRejected) {
                    online.displayClientMessage(Component.translatable("msg.module_shop.waste_rejected")
                            .withStyle(ChatFormatting.YELLOW), false);
                }
            }
            if (postSyncCallback != null) {
                postSyncCallback.run();
            }
        } else {
            bin.pendingNotice = true;
        }
    }

    /** 预估当前出货箱内待结算物品可得总金额（含动态供需、品质溢价、专注点与总钱箱可用上限）。 */
    public static long previewShippingPendingValue(MinecraftServer server, ShopSavedData data,
                                                   UUID owner, PlayerAccount account, long realDay) {
        ShippingBin bin = account.shipping;
        ProfessionManager.ensureFocusRefreshed(account);
        long gameDay = server == null ? 0L : server.overworld().getDayTime() / 24000L;

        long baseLimit = Config.shippingDailyLimit(bin.level);
        long extraLimit = ShippingModifierRegistry.extraDailyLimit(owner, baseLimit);
        long totalLimit = baseLimit == Long.MAX_VALUE ? Long.MAX_VALUE
                : (Math.min(Long.MAX_VALUE - baseLimit, extraLimit) + baseLimit);
        long soldToday = bin.soldDay == realDay ? bin.soldToday : 0L;
        long remaining = totalLimit == Long.MAX_VALUE ? Long.MAX_VALUE : Math.max(0L, totalLimit - soldToday);

        boolean eco = EcoPricing.isEcoActive();
        long treasurySpendable = eco
                ? data.treasury.spendableAboveFloor(Config.INSTANCE.treasuryMinFloor.get())
                : Long.MAX_VALUE;
        remaining = Math.min(remaining, treasurySpendable);

        double simulatedFocus = account.career.focusPoints;
        long total = 0L;
        Map<Integer, Integer> countedByRule = new HashMap<>();
        Map<String, Integer> virtualStockOffsets = new HashMap<>();

        for (ItemStack stack : bin.slots) {
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            ShippingRules.Rule rule = ShippingRules.match(stack);
            if (rule == null) {
                continue;
            }
            QualityEvaluator.QualityResult qr = QualityEvaluator.evaluate(stack);
            if (qr.rejected()) {
                continue;
            }
            ShippingApi.EvalResult ext = ShippingModifierRegistry.evaluate(owner, stack, rule.price(), rule.dailyMax());
            int perDay = rule.dailyMax() <= 0 ? 0
                    : Math.max(1, (int) Math.round(rule.dailyMax() * ext.ruleDailyMaxMultiplier()));
            int used = bin.soldDay == realDay ? bin.ruleUsed.getOrDefault(rule.line(), 0) : 0;
            int allowed = perDay <= 0 ? stack.getCount()
                    : Math.max(0, perDay - used - countedByRule.getOrDefault(rule.line(), 0));
            int tryCount = Math.min(stack.getCount(), allowed);
            if (tryCount <= 0) {
                continue;
            }

            String profId = ProfessionManager.matchProfessionForOutput(stack);
            int profLv = profId.isEmpty() ? 1 : account.career.getLevel(profId);
            double scarcity = profId.isEmpty() ? 1.0D : ProfessionManager.computeScarcityMultiplier(data, gameDay, profId);
            double focusCost = EcoPricing.computeFocusCostPerItem(profLv, Config.INSTANCE.baseFocusCostPerItem.get());
            double focusMult = EcoPricing.computeProfessionFocusMultiplier(
                    profLv, Config.INSTANCE.maxProfessionPriceBonus.get(), scarcity);
            long baseUnit = Math.max(rule.price(), qr.intrinsicBaseFloor());
            String bKey = EcoPricing.bucketKey(stack);

            int soldHere = 0;
            for (int k = 0; k < tryCount; k++) {
                boolean useFocus = ProfessionManager.isEnabled() && !profId.isEmpty()
                        && account.career.focusSellEnabled && simulatedFocus + 1e-6D >= focusCost;
                double profMult = (useFocus ? focusMult : scarcity) * ext.priceMultiplier();
                int offset = virtualStockOffsets.getOrDefault(bKey, 0);
                long unit = EcoPricing.computeBuyoutUnitAtStock(
                        data, stack, baseUnit, qr.multiplier(), profMult, rule.dailyMax(), offset);
                if (unit <= 0L || (remaining != Long.MAX_VALUE && total + unit > remaining)) {
                    break;
                }
                if (useFocus) {
                    simulatedFocus -= focusCost;
                }
                total += unit;
                soldHere++;
                virtualStockOffsets.put(bKey, offset + 1);
            }
            if (perDay > 0 && soldHere > 0) {
                countedByRule.merge(rule.line(), soldHere, Integer::sum);
            }
        }
        return total;
    }

    /**
     * 为指定玩家生成动态出货箱规则文本（下发给客户端 ShippingBinScreen / ShippingPage 实时显示涨跌后单价）。
     * 注意：管理页编辑读取的是 adminShippingRaw，因此这里下发动态单价绝不会污染 TOML 原文！
     */
    public static String buildDynamicShipRulesText(MinecraftServer server, ServerPlayer player, ShopSavedData data) {
        if (!EcoPricing.isEcoActive() || server == null || player == null) {
            StringBuilder raw = new StringBuilder();
            for (String line : Config.INSTANCE.shippingRules.get()) {
                raw.append(line).append('\n');
            }
            return raw.toString();
        }
        PlayerAccount account = data.account(player.getUUID());
        long gameDay = server.overworld().getDayTime() / 24000L;
        List<ShippingRules.Entry> entries = ShippingRules.entries();
        List<ItemStack> sampleStacks = ShippingRules.ruleStacks();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < entries.size(); i++) {
            ShippingRules.Entry entry = entries.get(i);
            if (entry.rule() == null) {
                sb.append(entry.raw()).append('\n');
                continue;
            }
            ShippingRules.Rule r = entry.rule();
            ItemStack sample = i < sampleStacks.size() ? sampleStacks.get(i) : ItemStack.EMPTY;
            long dynPrice = r.price();
            int dynMax = r.dailyMax();
            if (!sample.isEmpty()) {
                double profMult = ProfessionManager.previewProfessionMultiplier(data, account, gameDay, sample);
                ShippingApi.EvalResult ext = ShippingModifierRegistry.evaluate(
                        player.getUUID(), sample, r.price(), r.dailyMax());
                long q = EcoPricing.computeBuyoutUnitAtStock(
                        data, sample, r.price(), 1.0D, profMult * ext.priceMultiplier(), r.dailyMax(), 0);
                // 若满仓停收(q==0)，保留配置行但压至 1 或在出货箱概览展示停收，避免格式报错
                dynPrice = Math.max(1L, q);
                if (dynMax > 0) {
                    dynMax = Math.max(1, (int) Math.round(dynMax * ext.ruleDailyMaxMultiplier()));
                }
            }
            String left = switch (r.kind()) {
                case TAG -> "#" + r.value();
                case NAMESPACE -> r.value() + ":*";
                case ITEM -> r.value();
            };
            sb.append(left).append('=').append(dynPrice);
            if (dynMax > 0) {
                sb.append(" x").append(dynMax);
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    /**
     * 每日收购定向生成（「4 本职槽 + 2 跨职社交槽」+ 当日锁定动态报价）：
     * 保证专精玩家每天有 4 个本职消耗急单，同时留 2 个跨职槽促进玩家逛摊位互买物资凑全清大奖。
     */
    public static void generateDailyBuybackEco(ServerPlayer player, PlayerAccount account, ShopSavedData data,
                                               int maxDailySlots) {
        if (data.buybackPool.isEmpty()) {
            return;
        }
        long gameDay = player.server.overworld().getDayTime() / 24000L;
        boolean eco = EcoPricing.isEcoActive();
        int defaultCap = Config.INSTANCE.buybackCapacity.get();

        // 先逐项试算今日动态收购单价：已满仓停收(dyn == 0)或受 1 星币防套利锁禁收的条目直接剔除出候选池，
        // 绝不退回原价绕过限制，也避免刷出无法交付的死单卡住玩家的「全清奖励」。
        java.util.IdentityHashMap<ConfiguredOffer, Long> quotedPrices = new java.util.IdentityHashMap<>();
        List<ConfiguredOffer> allPool = new ArrayList<>();
        for (ConfiguredOffer offer : data.buybackPool) {
            if (offer == null || offer.stack == null || offer.stack.isEmpty() || offer.price <= 0L) {
                continue;
            }
            long unitPrice = offer.price;
            if (eco) {
                QualityEvaluator.QualityResult qr = QualityEvaluator.evaluate(offer.stack);
                if (qr.rejected()) {
                    continue;
                }
                double profMult = ProfessionManager.previewProfessionMultiplier(data, account, gameDay, offer.stack);
                long dyn = EcoPricing.computeBuyoutUnitAtStock(
                        data, offer.stack, Math.max(offer.price, qr.intrinsicBaseFloor()),
                        Math.max(1.0D, qr.multiplier()), profMult,
                        offer.amountOr(defaultCap), 0);
                if (dyn <= 0L) {
                    continue;
                }
                unitPrice = dyn;
            }
            quotedPrices.put(offer, unitPrice);
            allPool.add(offer);
        }
        if (allPool.isEmpty()) {
            return;
        }

        RandomSource random = player.getRandom();
        int slots = Math.min(maxDailySlots, Math.min(Math.max(0, data.buybackSlotCount), allPool.size()));
        if (slots <= 0) {
            return;
        }

        String majorJob = ProfessionManager.topProfessionOf(account);
        List<ConfiguredOffer> pickedList = new ArrayList<>(slots);

        if (!majorJob.isEmpty() && slots >= 4) {
            int wantMajor = Math.max(1, (slots * 2) / 3); // 6 槽时取 4 本职 + 2 跨职
            List<ConfiguredOffer> majorPool = new ArrayList<>();
            List<ConfiguredOffer> crossPool = new ArrayList<>();
            for (ConfiguredOffer offer : allPool) {
                String pId = offer.professionId != null && !offer.professionId.isEmpty()
                        ? offer.professionId
                        : ProfessionManager.matchProfessionForOutput(offer.stack);
                if (majorJob.equals(pId)) {
                    majorPool.add(offer);
                } else {
                    crossPool.add(offer);
                }
            }
            while (pickedList.size() < wantMajor && !majorPool.isEmpty()) {
                ConfiguredOffer p = majorPool.remove(random.nextInt(majorPool.size()));
                allPool.remove(p);
                pickedList.add(p);
            }
            while (pickedList.size() < slots && !crossPool.isEmpty()) {
                ConfiguredOffer p = crossPool.remove(random.nextInt(crossPool.size()));
                allPool.remove(p);
                pickedList.add(p);
            }
        }

        while (pickedList.size() < slots && !allPool.isEmpty()) {
            ConfiguredOffer p = allPool.remove(random.nextInt(allPool.size()));
            pickedList.add(p);
        }

        for (ConfiguredOffer picked : pickedList) {
            long unitPrice = quotedPrices.getOrDefault(picked, picked.price);
            if (unitPrice <= 0L) {
                continue;
            }
            DailyBuybackEntry entry = new DailyBuybackEntry(picked.stack.copy(), unitPrice);
            entry.rarity = picked.rarity == null ? Rarity.COMMON : picked.rarity;
            entry.limit = Math.max(1, picked.amountOr(defaultCap));
            account.dailyBuyback.add(entry);
        }
    }

    /** 拼装下发给客户端的 Eco 职业专精、每日专注点、官方总钱箱与外币额度快照。 */
    public static void writeEcoSnapshot(ServerPlayer player, PlayerAccount account, ShopSavedData data,
                                        CompoundTag tag, HolderLookup.Provider regs) {
        ProfessionManager.ensureFocusRefreshed(account);
        long quotaDay = Config.shippingQuotaDay();
        long gameDay = player.server.overworld().getDayTime() / 24000L;
        data.treasury.rollDayIfNeeded(quotaDay);

        CompoundTag ecoTag = new CompoundTag();
        ecoTag.putBoolean("enabled", EcoPricing.isEcoActive());
        ecoTag.putBoolean("profEnabled", ProfessionManager.isEnabled());
        ecoTag.putLong("treasuryBalance", data.treasury.balance);
        ecoTag.putLong("treasuryTarget", data.treasury.targetReserve);
        ecoTag.putDouble("treasuryHealth", data.treasury.healthFactor(
                Config.INSTANCE.treasuryMinFactor.get(),
                Config.INSTANCE.treasuryMaxFactor.get()));
        ecoTag.putLong("treasuryInToday", data.treasury.inflowToday);
        ecoTag.putLong("treasuryOutToday", data.treasury.outflowToday);

        // 兑换页单向开关与额度信息
        boolean allowWithdraw = Config.INSTANCE.exchangeAllowWithdraw.get();
        ecoTag.putBoolean("exchangeAllowWithdraw", allowWithdraw);
        long personalExCap = computePersonalExchangeCap(account);
        long personalExUsed = account.exchangeDay == quotaDay ? account.exchangeToday : 0L;
        ecoTag.putLong("exchangePersonalCap", personalExCap);
        ecoTag.putLong("exchangePersonalUsed", personalExUsed);
        ecoTag.putLong("exchangeGlobalRoom", computeGlobalExchangeRoom(data, quotaDay));

        // 玩家专注点与职业列表
        ecoTag.putInt("focus", (int) Math.floor(Math.max(0.0D, account.career.focusPoints)));
        ecoTag.putInt("focusMax", Config.INSTANCE.maxFocusCap.get());
        ecoTag.putInt("focusDaily", Config.INSTANCE.dailyFocusRestore.get());
        ecoTag.putBoolean("focusOn", account.career.focusSellEnabled);

        ListTag profList = new ListTag();
        for (ProfessionConfig.ProfessionSpec spec : ProfessionManager.activeSpecs()) {
            CompoundTag p = new CompoundTag();
            p.putString("id", spec.id);
            p.putString("nameZh", spec.nameZh == null ? spec.id : spec.nameZh);
            p.putString("nameEn", spec.nameEn == null ? spec.id : spec.nameEn);
            p.putString("descZh", spec.descZh == null ? "" : spec.descZh);
            p.putString("descEn", spec.descEn == null ? "" : spec.descEn);
            p.put("icon", ShopNbt.saveStack(ProfessionManager.iconStackOf(spec), regs));

            int lv = account.career.getLevel(spec.id);
            long xp = account.career.getXp(spec.id);
            long nextXp = cn.autoforged.module_shop.data.PlayerProfessionProfile.xpRequiredForNextLevel(lv);
            double scarcity = ProfessionManager.computeScarcityMultiplier(data, gameDay, spec.id);
            double bonusMult = EcoPricing.computeProfessionFocusMultiplier(
                    lv, Config.INSTANCE.maxProfessionPriceBonus.get(), scarcity);
            double focusCost = EcoPricing.computeFocusCostPerItem(
                    lv, Config.INSTANCE.baseFocusCostPerItem.get());
            long weekRev = account.career.sumVerifiedRevenue(gameDay, spec.id, 7);

            p.putInt("level", lv);
            p.putLong("xp", xp);
            p.putLong("nextXp", nextXp);
            p.putDouble("bonusPct", (bonusMult - 1.0D) * 100.0D);
            p.putDouble("scarcityPct", (scarcity - 1.0D) * 100.0D);
            p.putDouble("focusCost", focusCost);
            p.putLong("weekRev", weekRev);
            profList.add(p);
        }
        ecoTag.put("professions", profList);

        // 下发所有商城与出货规则物品的「官方收价 / 官方卖价」参考字典，供玩家摊位同台比价显示！
        CompoundTag refPrices = new CompoundTag();
        for (ShippingRules.Entry e : ShippingRules.entries()) {
            if (e.rule() != null && e.rule().kind() == ShippingRules.Kind.ITEM) {
                CompoundTag rp = new CompoundTag();
                rp.putLong("bid", e.rule().price());
                refPrices.put(e.rule().value(), rp);
            }
        }
        for (MallItem mi : data.mallPool) {
            if (!mi.stack.isEmpty()) {
                ResourceLocation rl = BuiltInRegistries.ITEM.getKey(mi.stack.getItem());
                if (rl != null) {
                    String k = rl.toString();
                    CompoundTag rp = refPrices.contains(k) ? refPrices.getCompound(k) : new CompoundTag();
                    long ask = (!mi.unlimited() && mi.supplyMode == SupplyMode.PLAYER_POOL)
                            ? EcoPricing.computeMallAskAtStock(mi, Math.max(0, mi.stock))
                            : mi.price;
                    rp.putLong("ask", ask);
                    refPrices.put(k, rp);
                }
            }
        }
        ecoTag.put("refPrices", refPrices);

        tag.put("eco", ecoTag);
    }

    /** 玩家当日外币结汇个人上限（随最高职业等级温和提额，最高放大 2.0x）。 */
    public static long computePersonalExchangeCap(PlayerAccount account) {
        long base = Config.INSTANCE.playerDailyExchangeCap.get();
        if (base <= 0L) {
            return Long.MAX_VALUE;
        }
        int bestLv = 1;
        if (account != null) {
            for (var p : account.career.professions.values()) {
                bestLv = Math.max(bestLv, p.level);
            }
        }
        double mult = 1.0D + ((bestLv - 1) / 99.0D);
        return Math.round(base * mult);
    }

    /** 全服官方总钱箱今日剩余可用于外币结汇的星币池空间。 */
    public static long computeGlobalExchangeRoom(ShopSavedData data, long quotaDay) {
        if (!EcoPricing.isEcoActive() || data == null) {
            return Long.MAX_VALUE;
        }
        data.treasury.rollDayIfNeeded(quotaDay);
        long globalCapCfg = Config.INSTANCE.globalDailyFxCap.get();
        long spendable = data.treasury.spendableAboveFloor(Config.INSTANCE.treasuryMinFloor.get());
        long shareCap = (long) Math.floor(spendable * Config.INSTANCE.treasuryDailyFxShare.get());
        long effectiveCap = globalCapCfg <= 0L ? shareCap : Math.min(globalCapCfg, shareCap);
        return Math.max(0L, effectiveCap - data.treasury.fxPaidToday);
    }
}
