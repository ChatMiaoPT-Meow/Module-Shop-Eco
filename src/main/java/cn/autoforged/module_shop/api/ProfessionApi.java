package cn.autoforged.module_shop.api;

import java.util.List;
import java.util.UUID;

/**
 * 内置职业专精与每日专注点查询/联动 API（API 1.4.0 新增）。
 *
 * <p>供股市附属（玩家实业公司 IPO 挂牌资格与每周核验出货营收）、任务附属或 KubeJS 脚本调用。
 */
public interface ProfessionApi {
    /** 当前服务器已激活显示的职业 ID 列表（例如 combat, enchanting, brewing, farming, cooking, fishing, bartending, archmage）。 */
    List<String> activeProfessions();

    /** 查询某玩家指定职业的当前等级（1 ~ 100；未开启或未知职业返回 1）。 */
    int level(UUID playerId, String professionId);

    /** 查询某玩家指定职业的当前等级内已攒经验值。 */
    long xp(UUID playerId, String professionId);

    /** 查询某玩家当前等级最高的主力职业 ID（并列时取累计经验更高者）。 */
    String topProfession(UUID playerId);

    /** 查询某玩家当前剩余专注点（整数精度；内部保留小数余量）。 */
    int focusPoints(UUID playerId);

    /** 专注点累积上限（默认 3000）。 */
    int maxFocusPoints();

    /** 玩家当前是否开启了「出货消耗专注点精销」开关。 */
    boolean focusSellEnabled(UUID playerId);

    /**
     * 查询某玩家过去 7 个游戏日在指定职业上的「官方核验真实出货营收」（星币）。
     * <p>专门用于股市附属 {@code module_shop_stock} 核算玩家实业公司 IPO 门槛与每周财报公允价值。
     */
    long verifiedWeeklyRevenue(UUID playerId, String professionId);

    /**
     * 查询某职业当前的全服紧缺加成倍率（1.00 ~ 1.25；越少人做的冷门行业越高，永不低于 1.00）。
     */
    double scarcityMultiplier(String professionId);

    /**
     * 由外部附属或脚本向玩家发放指定职业经验（必须在服务端主线程调用）。
     */
    ApiResult<Integer> grantXp(UUID playerId, String professionId, long amount, String reason);
}
