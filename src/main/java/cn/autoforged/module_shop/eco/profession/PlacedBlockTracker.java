package cn.autoforged.module_shop.eco.profession;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * 人工放置方块追踪表：防止玩家把西瓜、南瓜、甘蔗、竹子、可可豆摆在地上反复敲碎刷农牧经验。
 * 采用定长 LRU 缓存，零存档体积膨胀、零内存泄漏。
 */
public final class PlacedBlockTracker {
    private static final int MAX_TRACKED_PER_DIM = 8192;
    private static final Map<ResourceKey<Level>, Set<Long>> PLACED = new ConcurrentHashMap<>();

    private PlacedBlockTracker() {
    }

    private static Set<Long> setFor(Level level) {
        return PLACED.computeIfAbsent(level.dimension(), k -> Collections.synchronizedSet(
                Collections.newSetFromMap(new LinkedHashMap<>(256, 0.75F, true) {
                    @Override
                    protected boolean removeEldestEntry(Map.Entry<Long, Boolean> eldest) {
                        return size() > MAX_TRACKED_PER_DIM;
                    }
                })
        ));
    }

    public static void markPlaced(Level level, BlockPos pos) {
        if (level == null || level.isClientSide() || pos == null) {
            return;
        }
        setFor(level).add(pos.asLong());
    }

    public static boolean consumeIfPlayerPlaced(Level level, BlockPos pos) {
        if (level == null || level.isClientSide() || pos == null) {
            return false;
        }
        Set<Long> set = PLACED.get(level.dimension());
        return set != null && set.remove(pos.asLong());
    }

    public static void clearAll() {
        PLACED.clear();
    }
}
