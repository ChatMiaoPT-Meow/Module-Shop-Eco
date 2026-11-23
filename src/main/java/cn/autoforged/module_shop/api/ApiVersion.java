package cn.autoforged.module_shop.api;

/**
 * 公共 API 的版本信息。
 *
 * <p><b>模组版本与 API 版本相互独立</b>：模组升级（如 1.0.0 → 1.16.0）时 API 版本可能不变；
 * 只有接口签名/语义发生**不兼容**变化时才提升主版本号。
 *
 * <p>兼容约定：
 * <ul>
 *     <li>同一主版本内**只增不改**：新增方法会保留旧方法（至少一个大版本标 {@code @Deprecated}）；</li>
 *     <li>接口不暴露内部实现类（{@code ShopManager} / {@code ShopSavedData} / {@code PlayerAccount}）；</li>
 *     <li>API 层不改变存档结构与网络协议。</li>
 * </ul>
 *
 * <p><b>兼容范围</b>（同一主版本内只增不改，附属对旧版本编译后在新版本上照样能跑）：
 * <ul>
 *     <li>{@code cn.autoforged.module_shop.api.**}（不含 {@code api.internal}）；</li>
 *     <li>附属页面用的 UI 套件：{@code client.ui.AtomUi}（REGULAR / BOLD / available）、
 *     {@code client.ui.render.Draw / Text / SkiaFrame}、{@code client.ui.widget.Button / SkiaTextField /
 *     Motion / ScrollController / UiComponent} 的 public 成员。</li>
 * </ul>
 * 万一附属仍和新版本不兼容（例如它用了上面以外的类），商店会停用该附属页面并打开它的回退界面，不会崩溃。
 */
public final class ApiVersion {
    /** 当前 API 版本（语义化版本）。 */
    public static final String API_VERSION = "1.5.0";
    /** 主版本号：外部可用它做依赖检查。 */
    public static final int API_MAJOR = 1;
    /**
     * 次版本号：1.1 起新增客户端扩展点 {@link cn.autoforged.module_shop.api.client.ShopModules}；
     * 1.2 起 {@link cn.autoforged.module_shop.api.client.ShopTheme} 支持皮肤；
     * 1.4 起新增 {@link TreasuryApi}、{@link ProfessionApi} 与 {@link ShippingApi}；
     * 1.5 起新增统一货币模型（{@link cn.autoforged.module_shop.api.view.CurrencyUnit} /
     * {@link cn.autoforged.module_shop.api.view.CurrencyTierView}）与附属默认实体货币支持。
     */
    public static final int API_MINOR = 5;

    private ApiVersion() {
    }

    private static String runtime() {
        return API_VERSION;
    }

    /** 运行期主版本号（非编译期内联常量，供附属在运行期判断宿主能力）。 */
    public static int runtimeMajor() {
        return parsePart(runtime(), 0);
    }

    /** 运行期次版本号（非编译期内联常量，供附属在运行期判断宿主能力）。 */
    public static int runtimeMinor() {
        return parsePart(runtime(), 1);
    }

    /** 判断运行期 API 是否满足指定主版本及最低次版本要求。 */
    public static boolean atLeast(int major, int minor) {
        return runtimeMajor() == major && runtimeMinor() >= minor;
    }

    private static int parsePart(String version, int index) {
        if (version == null || version.isEmpty()) {
            return 0;
        }
        String[] parts = version.split("\\.");
        if (index < 0 || index >= parts.length) {
            return 0;
        }
        String raw = parts[index];
        int end = 0;
        while (end < raw.length() && Character.isDigit(raw.charAt(end))) {
            end++;
        }
        if (end == 0) {
            return 0;
        }
        try {
            return Integer.parseInt(raw.substring(0, end));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    /** 供外部打印/日志用。 */
    public static String describe() {
        return "module_shop_api " + runtime();
    }
}