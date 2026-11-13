package cn.autoforged.module_shop.api.event;

import java.util.UUID;
import net.neoforged.bus.api.Event;

/**
 * 玩家职业等级提升通知事件（API 1.4.0 新增，挂载于 {@link cn.autoforged.module_shop.api.ModuleShopApi#events()}）。
 */
public final class ProfessionLevelUpEvent extends Event {
    private final UUID playerId;
    private final String professionId;
    private final int oldLevel;
    private final int newLevel;

    public ProfessionLevelUpEvent(UUID playerId, String professionId, int oldLevel, int newLevel) {
        this.playerId = playerId;
        this.professionId = professionId;
        this.oldLevel = oldLevel;
        this.newLevel = newLevel;
    }

    public UUID playerId() {
        return playerId;
    }

    public String professionId() {
        return professionId;
    }

    public int oldLevel() {
        return oldLevel;
    }

    public int newLevel() {
        return newLevel;
    }
}
