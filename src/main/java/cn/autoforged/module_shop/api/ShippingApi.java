package cn.autoforged.module_shop.api;

import java.util.OptionalLong;
import java.util.UUID;
import net.minecraft.world.item.ItemStack;

/**
 * 出货箱查询与外部修正扩展门面（API 1.4.0 新增）。
 * 通过 {@link ModuleShopApi#shipping()} 获取单例。
 */
public interface ShippingApi {
    /** 查询指定物品在当前出货箱规则下的基础收购单价（无匹配规则返回 empty）。 */
    OptionalLong basePrice(ItemStack stack);

    /** 查询指定物品对某玩家当前的实时官方预估收购单价（含库存涨跌、品质溢价与专注加成；停收或拒收返回 0）。 */
    long liveQuoteUnitPrice(UUID playerId, ItemStack stack);

    /** 查询玩家出货箱快照状态。 */
    ShippingStatus status(UUID playerId);

    /** 注册外部出货结算修正器（可选扩展点，按 priority 升序执行）。 */
    void registerModifier(String id, int priority, ShippingModifier modifier);

    void unregisterModifier(String id);

    record ShippingStatus(
            int binLevel,
            long baseDailyLimit,
            long baseSoldToday,
            long lastSettleGameDay,
            long lastPayout,
            int lastCount
    ) {}

    interface ShippingModifier {
        default long extraSpecialtyDailyLimit(UUID playerId, long baseDailyLimit) {
            return 0L;
        }

        EvalResult evaluateItem(UUID playerId, ItemStack stack, long baseUnitPrice, int baseRuleDailyMax);
    }

    record EvalResult(
            boolean useSpecialtyChannel,
            double priceMultiplier,
            double ruleDailyMaxMultiplier,
            String badgeTag
    ) {
        public static final EvalResult IDENTITY = new EvalResult(false, 1.0D, 1.0D, "");
    }
}
