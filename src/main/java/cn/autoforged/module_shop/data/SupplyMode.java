package cn.autoforged.module_shop.data;

import java.util.Locale;

/**
 * 购物城商品的货架供需模式。
 */
public enum SupplyMode {
    /** 固定价模式（原版逻辑：价格恒定，补货按原配置执行）。 */
    STATIC("static"),
    /** 玩家供货做市模式（阿尔比恩式：玩家出货入库，库存越少越贵、库存越多越便宜，断货仅补 10% 高价应急底仓）。 */
    PLAYER_POOL("player_pool"),
    /** 系统消耗回笼模式（如装饰方块、抽卡券：定量补货，消费 100% 回笼进官方总钱箱）。 */
    SYSTEM_SINK("system_sink");

    private final String id;

    SupplyMode(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static SupplyMode byId(String raw, SupplyMode fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        String clean = raw.trim().toLowerCase(Locale.ROOT);
        for (SupplyMode mode : values()) {
            if (mode.id.equals(clean)) {
                return mode;
            }
        }
        return fallback;
    }
}
