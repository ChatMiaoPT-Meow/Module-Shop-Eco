package cn.autoforged.module_shop.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/**
 * 货币兑换条目：管理员配置“1 个该物品 = value 星币”。
 * <p>
 * 双向使用：
 * <ul>
 *   <li>兑换（物品 → 星币）：按 value 把玩家背包里的该物品折算成星币。</li>
 *   <li>取款（星币 → 物品）：按 value 扣除星币后把物品发给玩家（背包放不下则进邮箱）。</li>
 * </ul>
 * stack 只作为“模板/图标”保存，管理员放入的物品本身不会被消耗，删除条目也不退还。
 */
public final class ExchangeEntry {
    /** 模板物品（只取 1 个作为展示与匹配依据）。 */
    public ItemStack stack = ItemStack.EMPTY;
    /** 单个物品可兑换的星币数。 */
    public long value;
    /** 单人每日可兑换该货币的最大件数（<=0 表示不限；仍受星币日上限约束）。 */
    public int dailyLimit = 0;

    public ExchangeEntry() {
    }

    public ExchangeEntry(ItemStack stack, long value) {
        this(stack, value, 0);
    }

    public ExchangeEntry(ItemStack stack, long value, int dailyLimit) {
        this.stack = stack == null ? ItemStack.EMPTY : stack;
        this.value = value;
        this.dailyLimit = Math.max(0, dailyLimit);
    }

    /** 归一化：模板恒为 1 个，避免把数量当成价格的一部分。 */
    public void normalize() {
        if (stack == null) {
            stack = ItemStack.EMPTY;
        }
        if (!stack.isEmpty()) {
            stack.setCount(1);
        }
        if (dailyLimit < 0) {
            dailyLimit = 0;
        }
    }

    public boolean valid() {
        return !stack.isEmpty() && value > 0L;
    }

    /** 稳定的单品限额追踪键（物品 ID + NBT SNBT），不因列表增删改序漂移。 */
    public String limitKey(HolderLookup.Provider registries) {
        if (stack == null || stack.isEmpty()) {
            return "";
        }
        var rl = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
        String id = rl == null ? "" : rl.toString();
        String snbt = cn.autoforged.module_shop.shop.currency.PhysicalCurrencyConfig.extractComponentsSnbt(stack, registries);
        return snbt.isEmpty() ? id : (id + "|" + snbt);
    }

    public CompoundTag save(HolderLookup.Provider registries) {
        CompoundTag tag = ShopNbt.saveStack(stack, registries);
        tag.putLong("value", value);
        if (dailyLimit > 0) {
            tag.putInt("dailyLimit", dailyLimit);
        }
        return tag;
    }

    public static ExchangeEntry load(CompoundTag tag, HolderLookup.Provider registries) {
        ExchangeEntry entry = new ExchangeEntry();
        entry.stack = ShopNbt.loadStack(tag, registries);
        entry.value = tag.getLong("value");
        entry.dailyLimit = Math.max(0, tag.getInt("dailyLimit"));
        entry.normalize();
        return entry;
    }
}
