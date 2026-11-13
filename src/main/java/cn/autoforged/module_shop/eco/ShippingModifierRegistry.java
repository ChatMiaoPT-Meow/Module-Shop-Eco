package cn.autoforged.module_shop.eco;

import cn.autoforged.module_shop.api.ShippingApi;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.item.ItemStack;

/**
 * 外部出货修正器注册表（实现 {@link ShippingApi#registerModifier}）。
 */
public final class ShippingModifierRegistry {
    public record Entry(String id, int priority, ShippingApi.ShippingModifier modifier) {}

    private static final Map<String, Entry> MODIFIERS = new ConcurrentHashMap<>();

    private ShippingModifierRegistry() {
    }

    public static void register(String id, int priority, ShippingApi.ShippingModifier modifier) {
        if (id == null || id.isBlank() || modifier == null) {
            return;
        }
        MODIFIERS.put(id.trim(), new Entry(id.trim(), priority, modifier));
    }

    public static void unregister(String id) {
        if (id != null) {
            MODIFIERS.remove(id.trim());
        }
    }

    public static List<Entry> ordered() {
        if (MODIFIERS.isEmpty()) {
            return List.of();
        }
        return MODIFIERS.values().stream()
                .sorted(Comparator.comparingInt(Entry::priority).thenComparing(Entry::id))
                .toList();
    }

    public static long extraDailyLimit(UUID playerId, long baseLimit) {
        if (MODIFIERS.isEmpty() || baseLimit == Long.MAX_VALUE) {
            return 0L;
        }
        long extra = 0L;
        for (Entry e : ordered()) {
            try {
                long add = Math.max(0L, e.modifier().extraSpecialtyDailyLimit(playerId, baseLimit));
                extra = Math.min(Long.MAX_VALUE - extra, add) + extra;
            } catch (Throwable ignored) {
            }
        }
        return extra;
    }

    public static ShippingApi.EvalResult evaluate(UUID playerId, ItemStack stack, long basePrice, int baseDailyMax) {
        if (MODIFIERS.isEmpty()) {
            return ShippingApi.EvalResult.IDENTITY;
        }
        boolean specChannel = false;
        double priceMult = 1.0D;
        double maxMult = 1.0D;
        String badge = "";
        for (Entry e : ordered()) {
            try {
                ShippingApi.EvalResult r = e.modifier().evaluateItem(playerId, stack, basePrice, baseDailyMax);
                if (r != null) {
                    specChannel |= r.useSpecialtyChannel();
                    priceMult *= Math.max(1.0D, r.priceMultiplier());
                    maxMult *= Math.max(1.0D, r.ruleDailyMaxMultiplier());
                    if (badge.isEmpty() && r.badgeTag() != null && !r.badgeTag().isEmpty()) {
                        badge = r.badgeTag();
                    }
                }
            } catch (Throwable ignored) {
            }
        }
        return new ShippingApi.EvalResult(specChannel, priceMult, maxMult, badge);
    }
}
