package cn.autoforged.module_shop.data;

import net.minecraft.nbt.CompoundTag;

/**
 * 阿尔比恩式官方黑市单品库存与缺货爬坡状态。
 *
 * <ul>
 *   <li>当玩家不断出售某物资时，{@link #currentStock} 增加，收购价随库存率下降；一旦达到 {@code targetCapacity * fullRatio}，<b>满仓直接停收</b>；</li>
 *   <li>每天结算时自动自然消化一部分库存（{@code decayRatio}）；</li>
 *   <li>若某物资全天零库存没人交货，{@link #scarcityBonus} 自动按日累加（如每天 +5%），悬赏吸引玩家去生产，一旦有货入库则平滑回落。</li>
 * </ul>
 */
public final class BlackMarketBucket {
    public int currentStock;
    public int targetCapacity;
    public double scarcityBonus;
    public long lastSettleDay = Long.MIN_VALUE;

    public BlackMarketBucket() {
        this(0, 256);
    }

    public BlackMarketBucket(int currentStock, int targetCapacity) {
        this.currentStock = Math.max(0, currentStock);
        this.targetCapacity = Math.max(1, targetCapacity);
    }

    /** 仓库最大爆仓停收容量。 */
    public int maxWarehouseStock(double fullRatio) {
        return Math.max(1, (int) Math.round(Math.max(1, targetCapacity) * Math.max(1.0D, fullRatio)));
    }

    /** 还能收进仓库的剩余件数（0 = 已满仓停收）。 */
    public int remainingCapacity(double fullRatio) {
        return Math.max(0, maxWarehouseStock(fullRatio) - Math.max(0, currentStock));
    }

    public boolean isWarehouseFull(double fullRatio) {
        return remainingCapacity(fullRatio) <= 0;
    }

    /** 玩家向黑市交货入库：入库后缺货悬赏按入库比例回落。 */
    public void addStock(int count, double fullRatio) {
        if (count <= 0) {
            return;
        }
        int cap = maxWarehouseStock(fullRatio);
        this.currentStock = Math.min(cap, Math.max(0, this.currentStock) + count);
        if (this.scarcityBonus > 0.0D && this.targetCapacity > 0) {
            double drop = (double) count / (double) this.targetCapacity;
            this.scarcityBonus = Math.max(0.0D, this.scarcityBonus - drop * 0.20D);
            if (this.currentStock >= this.targetCapacity / 2) {
                this.scarcityBonus = 0.0D;
            }
        }
    }

    /** 每日自然消化库存 + 零库存断供爬坡加价。 */
    public void tickDaily(long day, double decayRatio, double scarcityStep, double scarcityMax) {
        if (this.lastSettleDay == day) {
            return;
        }
        this.lastSettleDay = day;
        if (this.currentStock <= 0) {
            if (scarcityStep > 0.0D && scarcityMax > 0.0D) {
                this.scarcityBonus = Math.min(scarcityMax, this.scarcityBonus + scarcityStep);
            }
        } else {
            int digest = Math.max(1, (int) Math.round(this.currentStock * Math.max(0.0D, Math.min(1.0D, decayRatio))));
            this.currentStock = Math.max(0, this.currentStock - digest);
        }
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("stock", currentStock);
        tag.putInt("cap", targetCapacity);
        tag.putDouble("scarcity", scarcityBonus);
        tag.putLong("lastDay", lastSettleDay);
        return tag;
    }

    public static BlackMarketBucket load(CompoundTag tag, int fallbackCap) {
        BlackMarketBucket b = new BlackMarketBucket(0, fallbackCap);
        if (tag != null) {
            b.currentStock = Math.max(0, tag.getInt("stock"));
            b.targetCapacity = Math.max(1, tag.contains("cap") ? tag.getInt("cap") : fallbackCap);
            b.scarcityBonus = Math.max(0.0D, tag.getDouble("scarcity"));
            b.lastSettleDay = tag.contains("lastDay") ? tag.getLong("lastDay") : Long.MIN_VALUE;
        }
        return b;
    }
}
