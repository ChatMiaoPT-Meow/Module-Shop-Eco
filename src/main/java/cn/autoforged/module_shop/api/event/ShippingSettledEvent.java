package cn.autoforged.module_shop.api.event;

import java.util.List;
import java.util.UUID;
import net.minecraftforge.eventbus.api.Event;

/**
 * 出货箱结算完成通知事件（API 1.4.0 新增，挂载于 {@link cn.autoforged.module_shop.api.ModuleShopApi#events()}）。
 * 供股市实业公司营收统计、任务通行证等附属监听。
 */
public final class ShippingSettledEvent extends Event {
    private final UUID playerId;
    private final long gameDay;
    private final long totalPayout;
    private final long specialtyPayout;
    private final int totalItemsCount;
    private final List<SettledLine> lines;

    public ShippingSettledEvent(UUID playerId, long gameDay, long totalPayout, long specialtyPayout,
                                int totalItemsCount, List<SettledLine> lines) {
        this.playerId = playerId;
        this.gameDay = gameDay;
        this.totalPayout = totalPayout;
        this.specialtyPayout = specialtyPayout;
        this.totalItemsCount = totalItemsCount;
        this.lines = lines == null ? List.of() : List.copyOf(lines);
    }

    public UUID playerId() {
        return playerId;
    }

    public long gameDay() {
        return gameDay;
    }

    public long totalPayout() {
        return totalPayout;
    }

    public long specialtyPayout() {
        return specialtyPayout;
    }

    public int totalItemsCount() {
        return totalItemsCount;
    }

    public List<SettledLine> lines() {
        return lines;
    }

    public record SettledLine(
            String itemId,
            int count,
            long unitPriceApplied,
            long lineTotal,
            String professionId,
            boolean usedFocus
    ) {}
}
