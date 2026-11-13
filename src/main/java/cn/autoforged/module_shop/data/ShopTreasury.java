package cn.autoforged.module_shop.data;

import net.minecraft.nbt.CompoundTag;

/**
 * 全局官方总钱箱（闭环国库账本，挂在 {@link ShopSavedData} 下持久化）。
 *
 * <p>负责维护官方做市可用星币水位、单日外币结汇已用额度与当日收支统计。
 * 当总钱箱余额低于保底安全线时，自动按剩余可支配金额截断收购量，绝不凭空印钞，也绝不吞没玩家物品。
 */
public final class ShopTreasury {
    /** 当前总钱箱可用星币余额。 */
    public long balance;
    /** 锚定目标储备水位（h = 1.00 时对应的标准储备）。 */
    public long targetReserve;
    /** 外币结汇与当日收支统计自然日（与出货箱额度自然日同口径）。 */
    public long fxDay = Long.MIN_VALUE;
    /** 当日已向「单向货币兑换」兑付出的星币总额。 */
    public long fxPaidToday;
    /** 当日总钱箱累计进账星币。 */
    public long inflowToday;
    /** 当日总钱箱累计支出星币。 */
    public long outflowToday;

    public ShopTreasury(long initialReserve, long targetReserve) {
        this.balance = Math.max(0L, initialReserve);
        this.targetReserve = Math.max(1L, targetReserve);
    }

    /** 跨日滚动清零当日统计与全服外币结汇已用额度。 */
    public void rollDayIfNeeded(long quotaDay) {
        if (this.fxDay != quotaDay) {
            this.fxDay = quotaDay;
            this.fxPaidToday = 0L;
            this.inflowToday = 0L;
            this.outflowToday = 0L;
        }
    }

    /**
     * 总钱箱健康系数（平滑夹取在 [minFactor, maxFactor]，h = 1.0 时严格等于 1.00）：
     * raw = 0.50 + 0.50 * (2.0 * h / (h + 1.0))
     */
    public double healthFactor(double minFactor, double maxFactor) {
        double lo = Math.min(minFactor, maxFactor);
        double hi = Math.max(minFactor, maxFactor);
        double h = (double) Math.max(0L, this.balance) / (double) Math.max(1L, this.targetReserve);
        double raw = 0.50D + 0.50D * ((2.0D * h) / (h + 1.0D));
        return Math.max(lo, Math.min(hi, raw));
    }

    /** 当前超出保底安全线、实际可用于收购或兑汇的星币上限。 */
    public long spendableAboveFloor(long minFloor) {
        return Math.max(0L, this.balance - Math.max(0L, minFloor));
    }

    /** 总钱箱进账（带 Long.MAX_VALUE 饱和防溢出）。 */
    public void inflow(long amount, long quotaDay) {
        if (amount <= 0L) {
            return;
        }
        rollDayIfNeeded(quotaDay);
        long room = Long.MAX_VALUE - this.balance;
        long added = Math.min(amount, room);
        this.balance += added;
        this.inflowToday = Math.min(Long.MAX_VALUE - this.inflowToday, this.inflowToday + added);
    }

    /** 总钱箱出账：最多扣到 minFloor 保底安全线，返回实际成功拨付的星币数。 */
    public long outflowUpTo(long requested, long minFloor, long quotaDay) {
        if (requested <= 0L) {
            return 0L;
        }
        rollDayIfNeeded(quotaDay);
        long spendable = spendableAboveFloor(minFloor);
        long paid = Math.min(requested, spendable);
        this.balance -= paid;
        this.outflowToday = Math.min(Long.MAX_VALUE - this.outflowToday, this.outflowToday + paid);
        return paid;
    }

    /**
     * 冲正回补未实际入账到玩家钱包的出账差额：
     * 原路退回总钱箱余额并冲减当日支出统计，不计入今日消费进账（inflowToday），确保总钱箱账目严丝合缝。
     */
    public void refundOutflow(long unusedAmount, long quotaDay) {
        if (unusedAmount <= 0L) {
            return;
        }
        rollDayIfNeeded(quotaDay);
        long room = Long.MAX_VALUE - this.balance;
        long restored = Math.min(unusedAmount, room);
        this.balance += restored;
        this.outflowToday = Math.max(0L, this.outflowToday - restored);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putLong("balance", balance);
        tag.putLong("targetReserve", targetReserve);
        tag.putLong("fxDay", fxDay);
        tag.putLong("fxPaidToday", fxPaidToday);
        tag.putLong("inflowToday", inflowToday);
        tag.putLong("outflowToday", outflowToday);
        return tag;
    }

    public static ShopTreasury load(CompoundTag tag, long defaultInitial, long defaultTarget) {
        ShopTreasury t = new ShopTreasury(defaultInitial, defaultTarget);
        if (tag != null && !tag.isEmpty()) {
            t.balance = Math.max(0L, tag.getLong("balance"));
            t.targetReserve = Math.max(1L, tag.contains("targetReserve") ? tag.getLong("targetReserve") : defaultTarget);
            t.fxDay = tag.contains("fxDay") ? tag.getLong("fxDay") : Long.MIN_VALUE;
            t.fxPaidToday = Math.max(0L, tag.getLong("fxPaidToday"));
            t.inflowToday = Math.max(0L, tag.getLong("inflowToday"));
            t.outflowToday = Math.max(0L, tag.getLong("outflowToday"));
        }
        return t;
    }
}
