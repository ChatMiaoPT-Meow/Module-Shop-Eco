package cn.autoforged.module_shop.api.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** 商店界面提供给附属页面的能力。 */
public interface AddonPageHost {
    /** 所在的 Screen（创建 SkiaTextField 要用）。 */
    Screen screen();

    /** 原版字体。 */
    Font font();

    /** 登记一个隐藏的原版输入框（SkiaTextField 背后的 EditBox），让它能接收键盘与输入法。 */
    void registerInput(EditBox box);

    /** 本帧的文字提示（悬停说明），所有内容画完后统一显示。 */
    void textTooltip(Component text);

    /** 客户端已知的当前玩家余额（服务端快照同步，仅供显示）。 */
    long balance();

    /** 当前货币名（管理员可改名，已处理默认名的翻译）。 */
    String currencyName();

    /**
     * 本帧的物品层（UI v2）：在页面里 {@code items().add(stack, x, y, size, decorations)} 登记要画的物品图标，
     * 商店会在 Skija 内容之后统一用原版渲染画出来，并处理悬停提示与 JEI 快捷键。返回 null 表示当前环境不支持
     * （旧版主模组）。
     *
     * @since API 1.3
     */
    default cn.autoforged.module_shop.client.ui.render.ItemOverlay items() {
        return null;
    }

    /**
     * 客户端已知的实体货币开关状态。
     *
     * @since API 1.5
     */
    default boolean physicalCurrencyEnabled() {
        return false;
    }

    /**
     * 客户端已知的实体货币档位列表（含本地玩家实时背包持有数）。
     *
     * @since API 1.5
     */
    default java.util.List<cn.autoforged.module_shop.api.view.CurrencyTierView> physicalTiers() {
        return java.util.List.of();
    }

    /**
     * 当前附属默认生效的结算货币单位（若未配置或未启用实体货币则回退虚拟主货币）。
     *
     * @since API 1.5
     */
    default cn.autoforged.module_shop.api.view.CurrencyUnit activeCurrency() {
        return cn.autoforged.module_shop.api.view.CurrencyUnit.star(currencyName());
    }

    /**
     * 玩家在 {@link #activeCurrency()} 下的实时可用余额（附属默认实体币时实时扫描客户端背包阶梯计算）。
     *
     * @since API 1.5
     */
    default long activeBalance() {
        return balance();
    }

    /**
     * 当前 {@link #activeCurrency()} 的显示名称。
     *
     * @since API 1.5
     */
    default String activeCurrencyName() {
        return activeCurrency().displayName();
    }
}
