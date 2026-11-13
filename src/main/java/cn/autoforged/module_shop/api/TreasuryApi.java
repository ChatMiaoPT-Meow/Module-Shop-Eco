package cn.autoforged.module_shop.api;

import java.util.UUID;

/**
 * 官方总钱箱（闭环国库）公共 API（API 1.4.0 新增）。
 *
 * <p>供外部系统型消耗附属（如抽卡、盲仓扣税、股市印花税、实业注册费）将净消耗回流官方总钱箱，
 * 避免与玩家间资金托管流转（炒股买卖本金、玩家红包）混淆。
 */
public interface TreasuryApi {
    /** 当前官方总钱箱可用星币余额。 */
    long balance();

    /** 官方总钱箱锚定目标储备水位。 */
    long targetReserve();

    /** 当前官方总钱箱健康系数（1.00 = 标准水位）。 */
    double healthFactor();

    /** 当日总钱箱累计进账星币。 */
    long inflowToday();

    /** 当日总钱箱累计支出星币。 */
    long outflowToday();

    /**
     * 原子扣除玩家余额并 100% 注入官方总钱箱（必须在服务端主线程调用）。
     *
     * @return 成功时返回实扣星币数；余额不足整笔失败，不扣钱也不进总钱箱。
     */
    ApiResult<Long> chargeToTreasury(UUID playerId, long amount, String reason);

    /**
     * 将已由附属自行扣除的系统净税费划入官方总钱箱（必须在服务端主线程调用）。
     */
    ApiResult<Void> depositSink(long amount, String source);
}
