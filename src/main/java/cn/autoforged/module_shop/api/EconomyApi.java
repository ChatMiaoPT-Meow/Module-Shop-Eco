package cn.autoforged.module_shop.api;

import cn.autoforged.module_shop.api.view.BalanceView;
import cn.autoforged.module_shop.api.view.CurrencyTierView;
import cn.autoforged.module_shop.api.view.CurrencyUnit;
import java.util.List;
import java.util.UUID;

/**
 * 经济 API：**所有**货币变化都必须经过这里（服务端权威、受上限约束、写流水）。
 *
 * <p>线程：必须在**服务端主线程**调用（内部会做线程断言，非主线程直接返回 {@link ApiErrorCode#WRONG_THREAD}）。
 *
 * <p>权限级别：
 * <ul>
 *     <li>查询：{@code SYSTEM}（任意模组）</li>
 *     <li>{@code add} / {@code take}：{@code SYSTEM}（受上限、流水、幂等约束）</li>
 *     <li>{@code setBalance}：**不开放**（保留给 {@code /shop admin} 命令）</li>
 * </ul>
 */
public interface EconomyApi {
    /** 余额（离线玩家同样可查）；玩家不存在返回 0。 */
    long balance(UUID target);

    /** 余额视图（含上限与货币名）；玩家不存在返回 {@code null}。 */
    BalanceView balanceView(UUID target);

    /** 余额上限：{@code 0} 表示不限。 */
    long maxBalance();

    /**
     * 加钱（受余额上限约束，装不下的部分转**金钱邮件**）。
     *
     * @return 成功时 value = **实际入账金额**（可能小于请求值）
     */
    ApiResult<Long> add(UUID target, long amount, String reason);

    /**
     * 扣钱：**余额不足时整笔失败**（不会扣到 0）。
     *
     * @return 成功时 value = 实际扣除金额（等于请求值）
     */
    ApiResult<Long> take(UUID target, long amount, String reason);

    // ---------------------------------------------------------------- API 1.5：统一货币（虚拟主货币 / 实体货币）

    /**
     * 服务端当前虚拟主货币显示名（优先管理员游戏内改名，其次配置项，默认 {@code "星币"}）。
     *
     * @since API 1.5
     */
    default String currencyName() {
        return "星币";
    }

    /**
     * 当前服务端是否已启用实体货币系统（开关开启且存在有效档位）。
     *
     * @since API 1.5
     */
    default boolean isPhysicalCurrencyEnabled() {
        return false;
    }

    /**
     * 查询所有已解析的实体货币档位视图；{@code target} 在线时填充其背包各档持有数量，否则持有数填 0。
     *
     * @since API 1.5
     */
    default List<CurrencyTierView> physicalTiers(UUID target) {
        return List.of();
    }

    /**
     * 当前附属模组默认生效的结算货币：若管理员启用了实体货币并指定了「附属默认货币档位」则返回该实体币，
     * 否则返回虚拟主货币 {@link CurrencyUnit#star(String)}。
     *
     * @since API 1.5
     */
    default CurrencyUnit activeCurrency() {
        return CurrencyUnit.star(currencyName());
    }

    /**
     * 按配置键解析目标货币：
     * {@code null} / 空串 / {@code "star"} 严格返回虚拟主货币；
     * 非空则查找对应实体币档位，若档位被删则回退第 0 档实体币或虚拟主货币。
     *
     * @since API 1.5
     */
    default CurrencyUnit resolveCurrency(String tierKeyOrEmpty) {
        return activeCurrency();
    }

    /**
     * 按指定货币单位查询玩家当前可用余额：
     * 虚拟主货币返回账户余额（离线可查）；实体货币离线返回 {@code 0L}，在线按背包阶梯链折算最大可精确支付枚数。
     *
     * @since API 1.5
     */
    default long balance(UUID target, CurrencyUnit unit) {
        if (unit != null && unit.physical()) {
            return 0L;
        }
        return balance(target);
    }

    /** 查询玩家在 {@link #activeCurrency()} 下的当前可用余额。 */
    default long balanceActive(UUID target) {
        return balance(target, activeCurrency());
    }

    /**
     * 按指定货币单位向玩家发款：
     * 虚拟主货币超出余额上限时将溢出（或全额）转金钱邮件并返回 {@code OK(amount)}；
     * 实体货币支持在线进包/满转物品邮件与离线直发物品邮件（绝不触碰虚拟星币国库），并实时同步在线玩家快照。
     *
     * @since API 1.5
     */
    default ApiResult<Long> add(UUID target, CurrencyUnit unit, long amount, String reason) {
        if (unit != null && unit.physical()) {
            return ApiResult.fail(ApiErrorCode.DISABLED, "当前主模组版本不支持实体货币 API");
        }
        return add(target, amount, reason);
    }

    /** 按 {@link #activeCurrency()} 向玩家发款。 */
    default ApiResult<Long> addActive(UUID target, long amount, String reason) {
        return add(target, activeCurrency(), amount, reason);
    }

    /**
     * 按指定货币单位扣除玩家余额：
     * 虚拟主货币扣款并在在线时刷新快照；
     * 实体货币要求玩家在线（离线返回 {@link ApiErrorCode#PLAYER_OFFLINE}），自动链内破整找零并刷新快照。
     *
     * @since API 1.5
     */
    default ApiResult<Long> take(UUID target, CurrencyUnit unit, long amount, String reason) {
        if (unit != null && unit.physical()) {
            return ApiResult.fail(ApiErrorCode.DISABLED, "当前主模组版本不支持实体货币 API");
        }
        return take(target, amount, reason);
    }

    /** 按 {@link #activeCurrency()} 扣除玩家余额。 */
    default ApiResult<Long> takeActive(UUID target, long amount, String reason) {
        return take(target, activeCurrency(), amount, reason);
    }
}