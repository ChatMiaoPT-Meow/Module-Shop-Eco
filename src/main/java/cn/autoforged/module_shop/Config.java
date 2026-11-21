package cn.autoforged.module_shop;

import net.neoforged.neoforge.common.ModConfigSpec;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;

/**
 * 模块商店的公共配置。所有数值都可以由服主在 config/module_shop-common.toml 中调整。
 * 需求里出现冲突的属性，这里采用“以玩法意图优先”的折中默认值，并在注释中标注。
 */
public final class Config {
    public static final Config INSTANCE;
    public static final ModConfigSpec SPEC;

    static {
        Pair<Config, ModConfigSpec> pair = new ModConfigSpec.Builder().configure(Config::new);
        INSTANCE = pair.getLeft();
        SPEC = pair.getRight();
    }

    // ---- 钱包 ----
    public final ModConfigSpec.ConfigValue<String> currencyName;
    public final ModConfigSpec.ConfigValue<String> currencySymbol;
    public final ModConfigSpec.LongValue initialBalance;
    public final ModConfigSpec.LongValue maxBalance;
    public final ModConfigSpec.IntValue transferCooldownTicks;
    public final ModConfigSpec.BooleanValue allowPlayerTransfer;
    /** 新世界是否用客户端 / 服务器默认模板初始化商店。 */
    public final ModConfigSpec.BooleanValue templateInitEnabled;

    // ---- 每日商店 ----
    public final ModConfigSpec.IntValue dailyShopSlots;
    /** 每日商店里“普通”档位最多出现几个（0=不限制）。 */
    public final ModConfigSpec.IntValue dailyShopCommonLimit;
    /** 付费刷新每日商店的价格表（按次数顺序取用：第 1 次、第 2 次…），用完即不可再刷新。 */
    public final ModConfigSpec.ConfigValue<List<? extends String>> dailyRefreshCosts;
    /** 每日收购的付费刷新价格表（与每日商店**完全分开**：价格、次数各自独立）。 */
    public final ModConfigSpec.ConfigValue<List<? extends String>> buybackRefreshCosts;
    public final ModConfigSpec.IntValue weightLegendary;
    public final ModConfigSpec.IntValue weightEpic;
    public final ModConfigSpec.IntValue weightRare;
    public final ModConfigSpec.IntValue weightCommon;
    public final ModConfigSpec.IntValue limitLegendary;
    public final ModConfigSpec.IntValue limitEpic;
    public final ModConfigSpec.IntValue limitRare;
    public final ModConfigSpec.IntValue limitCommon;

    // ---- 每日收购 ----
    public final ModConfigSpec.IntValue buybackSlots;
    public final ModConfigSpec.IntValue buybackCapacity;
    /** 当日把收购清单全部卖完的一次性奖励（0=关闭，默认 1000）。 */
    public final ModConfigSpec.LongValue buybackAllBonus;

    // ---- 拍卖（时长全部按现实时间，单位秒）----
    public final ModConfigSpec.IntValue auctionDurationSeconds;
    public final ModConfigSpec.LongValue auctionMinIncrement;
    public final ModConfigSpec.IntValue maxConcurrentAuctions;
    /** 需求 2：拍卖成交后从卖家所得中扣除的手续费百分比（默认 5%）。 */
    public final ModConfigSpec.DoubleValue auctionFeePercent;
    public final ModConfigSpec.BooleanValue antiSnipe;
    public final ModConfigSpec.IntValue antiSnipeSeconds;
    public final ModConfigSpec.IntValue antiSnipeExtensionSeconds;
    // ---- 拍卖行升级（新增，全部有安全默认值）----
    public final ModConfigSpec.BooleanValue auctionEnabled;
    public final ModConfigSpec.IntValue maxAuctionsGlobal;
    public final ModConfigSpec.IntValue antiSnipeMaxExtensions;
    public final ModConfigSpec.ConfigValue<List<? extends String>> auctionDurationOptions;
    public final ModConfigSpec.LongValue auctionListingFeeFixed;
    public final ModConfigSpec.DoubleValue auctionListingFeePercent;
    public final ModConfigSpec.LongValue auctionMinStartPrice;
    public final ModConfigSpec.LongValue auctionMaxStartPrice;
    public final ModConfigSpec.BooleanValue auctionAllowBuyout;
    public final ModConfigSpec.IntValue auctionMaxItemCount;
    public final ModConfigSpec.ConfigValue<List<? extends String>> auctionBlacklist;
    public final ModConfigSpec.BooleanValue auctionAllowBound;
    public final ModConfigSpec.ConfigValue<List<? extends String>> auctionBoundMarkers;
    public final ModConfigSpec.BooleanValue auctionAllowCustomData;
    public final ModConfigSpec.IntValue auctionMaxItemNbtBytes;
    public final ModConfigSpec.BooleanValue auctionBlockCreative;
    public final ModConfigSpec.IntValue auctionEndingSoonSeconds;
    public final ModConfigSpec.IntValue auctionHistoryLimit;

    // ---- 邮箱 ----
    /** 邮箱上限（默认 100；界面每页 45 封，可翻页）。达到上限时系统资产返还优先改为掉落入包，防止溢出刷物品。 */
    public final ModConfigSpec.IntValue maxMailboxSize;
    public final ModConfigSpec.IntValue mailExpireDays;
    /** 每名玩家保留的交易流水条数（管理员用 /shop admin history 查看）。 */
    public final ModConfigSpec.IntValue transactionHistorySize;

    // ---- 摊位 ----
    public final ModConfigSpec.IntValue stallSlots;
    public final ModConfigSpec.DoubleValue stallListingFeePercent;
    /** 玩家摊位成交税百分比（默认 5%，从卖家实收扣除并注入官方总钱箱）。 */
    public final ModConfigSpec.DoubleValue stallSaleFeePercent;
    /** 单件商品的库存上限（上架与补货共用）。 */
    public final ModConfigSpec.IntValue stallMaxStock;
    /** 玩家摊位自动下架方式：permanent=永久 / game=按游戏时间 / real=按现实时间。 */
    /** 购物城：库存刷新方式（off / real / game）与周期（real=小时、game=天）。 */
    public final ModConfigSpec.ConfigValue<String> mallRefreshMode;
    public final ModConfigSpec.IntValue mallRefreshAmount;
    /** 购物城库存模式：global = 全服共用一份库存 / personal = 每个玩家各自一份库存。 */
    public final ModConfigSpec.ConfigValue<String> mallStockMode;
    public final ModConfigSpec.ConfigValue<String> stallExpireMode;
    /** 限时时长：game 按游戏天数，real 按现实小时（0 = 永不自动下架）。 */
    public final ModConfigSpec.IntValue stallExpireAmount;

    // ---- 玩家求购（悬赏）----
    /** 是否开启玩家求购模块。 */
    public final ModConfigSpec.BooleanValue demandEnabled;
    /** 每名玩家最多同时发布几条求购。 */
    public final ModConfigSpec.IntValue demandSlots;
    /** 发布手续费百分比（按悬赏金币算，0 = 免费）。 */
    public final ModConfigSpec.DoubleValue demandFeePercent;
    /** 单笔悬赏金币上限（0 = 不限）。 */
    public final ModConfigSpec.IntValue demandMaxReward;
    /** 全服最多保留多少条求购（超出时最老的自动下架并退款）。 */
    public final ModConfigSpec.IntValue demandMaxListed;
    /** 求购自动下架方式：permanent=永久 / game=按游戏时间 / real=按现实时间。 */
    public final ModConfigSpec.ConfigValue<String> demandExpireMode;
    /** 限时时长：game 按游戏天数，real 按现实小时（0 = 永不自动下架）。 */
    public final ModConfigSpec.IntValue demandExpireAmount;

    // ---- 玩家交易（现实时间，单位秒）----
    public final ModConfigSpec.IntValue tradeCountdownSeconds;
    /** 需求 1：交易请求失效时间（现实秒），超时自动清理。 */
    public final ModConfigSpec.IntValue tradeRequestExpireSeconds;
    /** 已接受的交易多久没有任何操作就自动结束并退回物品（现实秒，默认 10 分钟）。 */
    public final ModConfigSpec.IntValue tradeSessionTimeoutSeconds;

    // ---- 出货箱（新增模块，参考星露谷售货箱）----
    public final ModConfigSpec.BooleanValue shippingEnabled;
    /** 兑换规则（每行一条：物品ID=价格 / #标签=价格 / 命名空间:*=价格，价格后可加 x数量 限定每日份数）。 */
    public final ModConfigSpec.ConfigValue<List<? extends String>> shippingRules;
    /** 每个等级的槽位数（列表长度 = 等级数）。 */
    public final ModConfigSpec.ConfigValue<List<? extends String>> shippingLevelSlots;
    /** 升级花费（第 1 项 = 1→2 级）。 */
    public final ModConfigSpec.ConfigValue<List<? extends String>> shippingUpgradeCosts;
    /** 每个等级的每日结算上限（现实 0 点刷新）。 */
    public final ModConfigSpec.ConfigValue<List<? extends String>> shippingDailyLimits;
    /** 每天结算的游戏刻（0 = 早上 06:00）。 */
    public final ModConfigSpec.IntValue shippingSettleTick;
    /** 结算窗口长度（刻）。 */
    public final ModConfigSpec.IntValue shippingSettleWindow;
    /** 每日额度按**现实时间**在几点重置（HH:mm，默认 00:00）。 */
    public final ModConfigSpec.ConfigValue<String> shippingResetTime;

    // ---- 购物城（新增模块）----
    /** 购物城是否定时补货（默认关；管理员可在游戏内「补货设置」里打开）。 */
    public final ModConfigSpec.BooleanValue mallRestockEnabled;
    /** 补货时刻的时间口径：{@code real} = 现实时间，{@code game} = 游戏时间。 */
    public final ModConfigSpec.ConfigValue<String> mallRestockMode;
    /** 补货时刻（HH:mm）。 */
    public final ModConfigSpec.ConfigValue<String> mallRestockTime;
    /** 补货数量：0 = 每件商品补回各自原量；>0 = 所有商品统一补到该数量。 */
    public final ModConfigSpec.IntValue mallRestockAmount;

    // ---------------------------------------------------------------- HTTP / REST API（默认关闭）
    /** 是否启用 HTTP API（默认关闭；启用前请务必设置 token 并保持只监听 127.0.0.1）。 */
    public final ModConfigSpec.BooleanValue httpApiEnabled;
    /** 监听地址（默认 127.0.0.1；填 0.0.0.0 会把经济接口暴露到网络，除非你很清楚在做什么）。 */
    public final ModConfigSpec.ConfigValue<String> httpApiBind;
    /** 监听端口。 */
    public final ModConfigSpec.IntValue httpApiPort;
    /** 访问令牌（Bearer）。为空时 HTTP API 拒绝启动。 */
    public final ModConfigSpec.ConfigValue<String> httpApiToken;
    /** 是否允许写操作（加钱/扣钱/发奖/发邮件）。默认 false = 只读。 */
    public final ModConfigSpec.BooleanValue httpApiAllowWrite;
    /** 每个 IP 每分钟最多请求数（0 = 不限，不建议）。 */
    public final ModConfigSpec.IntValue httpApiRateLimit;

    // ---------------------------------------------------------------- Eco 做市商与闭环总钱箱
    public final ModConfigSpec.BooleanValue ecoEnabled;
    public final ModConfigSpec.LongValue treasuryInitialReserve;
    public final ModConfigSpec.LongValue treasuryTargetReserve;
    public final ModConfigSpec.LongValue treasuryMinFloor;
    public final ModConfigSpec.DoubleValue treasuryMinFactor;
    public final ModConfigSpec.DoubleValue treasuryMaxFactor;
    public final ModConfigSpec.DoubleValue ecoAskFloor;
    public final ModConfigSpec.DoubleValue ecoAskCeiling;
    public final ModConfigSpec.DoubleValue ecoBidFloor;
    public final ModConfigSpec.DoubleValue ecoBidCeiling;
    public final ModConfigSpec.DoubleValue ecoMaxBidToAskRatio;
    public final ModConfigSpec.DoubleValue ecoShippingToMallRatio;
    public final ModConfigSpec.DoubleValue ecoBuybackToMallRatio;
    public final ModConfigSpec.IntValue blackMarketDefaultCap;
    public final ModConfigSpec.DoubleValue blackMarketWarehouseFullRatio;
    public final ModConfigSpec.DoubleValue blackMarketDailyDecayRatio;
    public final ModConfigSpec.DoubleValue blackMarketScarcityDailyStep;
    public final ModConfigSpec.DoubleValue blackMarketScarcityMaxBonus;
    public final ModConfigSpec.ConfigValue<List<? extends String>> ecoTreasurySinkReasonPrefixes;
    public final ModConfigSpec.ConfigValue<List<? extends String>> ecoTreasuryRefundReasonPrefixes;

    // ---------------------------------------------------------------- Eco 单向/限额货币结汇阀门
    public final ModConfigSpec.BooleanValue exchangeAllowWithdraw;
    public final ModConfigSpec.LongValue playerDailyExchangeCap;
    public final ModConfigSpec.LongValue globalDailyFxCap;
    public final ModConfigSpec.DoubleValue treasuryDailyFxShare;

    // ---------------------------------------------------------------- Eco 八大职业、专注点与品质溢价
    public final ModConfigSpec.BooleanValue professionsEnabled;
    public final ModConfigSpec.IntValue dailyFocusRestore;
    public final ModConfigSpec.IntValue maxFocusCap;
    public final ModConfigSpec.DoubleValue baseFocusCostPerItem;
    public final ModConfigSpec.DoubleValue maxProfessionPriceBonus;
    public final ModConfigSpec.DoubleValue scarcityBonusMax;
    public final ModConfigSpec.BooleanValue qualityBonusEnabled;
    public final ModConfigSpec.IntValue combatGrinderKillThreshold;

    private Config(ModConfigSpec.Builder builder) {
        builder.push("wallet");
        currencyName = builder.comment("货币名称（需求：币种统一为星币，所有金额显示统一追加该后缀）").define("currencyName", "星币");
        currencySymbol = builder.comment("货币符号，留空则只显示数字").define("currencySymbol", "");
        initialBalance = builder.comment("新玩家初始启动余额（默认 200 星币，方便买口粮/火把或体验首刷）").defineInRange("initialBalance", 200L, 0L, 9999999999L);
        // 0 = 不限：服主想「不设上限」时直接写 0 即可（以前最小值是 1，写 0 会被自动纠正成 1，
        // 于是玩家余额一超过 1 就显示「（已满）」——这就是那个「已满」bug 的根源）。
        maxBalance = builder.comment("余额上限（0 = 不限）").defineInRange("maxBalance", 9999999999L, 0L, Long.MAX_VALUE);
        transferCooldownTicks = builder.comment("玩家间转账冷却（tick）").defineInRange("transferCooldownTicks", 20, 0, 100000);
        allowPlayerTransfer = builder.comment("是否允许玩家间转账").define("allowPlayerTransfer", true);
        builder.pop();

        builder.push("shop_template");
        templateInitEnabled = builder.comment("新世界首次启动时，是否用 config/moduleshop/shop_config.json 初始化商店（只在新世界且商品仍是默认时生效）").define("templateInitEnabled", true);
        builder.pop();

        builder.push("daily_shop");
        // 界面固定 3 列 × 2 行卡片（每日商店没有翻页），配置上限就是 6。
        dailyShopSlots = builder.comment("每日商店槽位数量（界面固定 3 列 × 2 行，最多 6）").defineInRange("dailyShopSlots", 6, 1, 6);
        dailyShopCommonLimit = builder.comment("每日商店“普通”档位最多出现数量（0=不限制，默认 3）")
                .defineInRange("dailyShopCommonLimit", 3, 0, 6);
        dailyRefreshCosts = builder.comment("付费刷新【每日商店】的价格表（按次数顺序：默认第 1 次 200、第 2 次 500；列表长度=每天可刷新次数）")
                .defineList("dailyRefreshCosts", List.of("200", "500"), () -> "200", o -> o instanceof String);
        buybackRefreshCosts = builder.comment("付费刷新【每日收购】的价格表（与每日商店相互独立：可分别设置价格与每天次数）")
                .defineList("buybackRefreshCosts", List.of("200", "500"), () -> "200", o -> o instanceof String);
        weightLegendary = builder.comment("传说权重").defineInRange("weightLegendary", 5, 0, 100000);
        weightEpic = builder.comment("史诗权重").defineInRange("weightEpic", 15, 0, 100000);
        weightRare = builder.comment("稀有权重").defineInRange("weightRare", 30, 0, 100000);
        weightCommon = builder.comment("普通权重").defineInRange("weightCommon", 50, 0, 100000);
        limitLegendary = builder.comment("每日每玩家可购买传说数量").defineInRange("limitLegendary", 1, 1, 1000);
        limitEpic = builder.comment("每日每玩家可购买史诗数量").defineInRange("limitEpic", 2, 1, 1000);
        limitRare = builder.comment("每日每玩家可购买稀有数量").defineInRange("limitRare", 5, 1, 1000);
        limitCommon = builder.comment("每日每玩家可购买普通数量").defineInRange("limitCommon", 10, 1, 1000);
        builder.pop();

        builder.push("daily_buyback");
        // 需求冲突：daily_slots=4 与 buyback_slots="6项(丰富)"。取可玩性更高的 6。
        // 界面只渲染 6 行（没有翻页），所以每日最多随机 6 项收购需求。
        buybackSlots = builder.comment("每日收购槽位数量（每日最多随机 6 项，界面只显示 6 行）").defineInRange("buybackSlots", 6, 1, 6);
        buybackCapacity = builder.comment("每个收购项每日可收购次数").defineInRange("buybackCapacity", 16, 1, 100000);
        buybackAllBonus = builder.comment("当日收购清单全部完成的一次性奖励星币（0=关闭，默认 400 星币=2.5 抽）")
                .defineInRange("buybackAllBonus", 400L, 0L, 9999999999L);
        builder.pop();

        builder.push("auction");
        // 现实时间：7200 秒 = 2 小时
        auctionDurationSeconds = builder.comment("拍卖持续时间（现实秒，7200=2小时）").defineInRange("auctionDurationSeconds", 7200, 1, Integer.MAX_VALUE);
        // 需求冲突：min_bid_increment=1 与 bid_increment="10星币(标准)"。取标准值 10。
        auctionMinIncrement = builder.comment("最小加价幅度").defineInRange("auctionMinIncrement", 10L, 1L, Long.MAX_VALUE);
        maxConcurrentAuctions = builder.comment("每玩家同时进行的拍卖上限").defineInRange("maxConcurrentAuctions", 6, 1, 100);
        // 需求 2：交易成功扣除卖家 5% 手续费。
        auctionFeePercent = builder.comment("拍卖成交手续费百分比（从卖家所得中扣除）").defineInRange("auctionFeePercent", 5.0D, 0.0D, 100.0D);
        antiSnipe = builder.comment("是否开启防狙击").define("antiSnipe", true);
        antiSnipeSeconds = builder.comment("剩余多少秒内出价算狙击（现实秒）").defineInRange("antiSnipeSeconds", 60, 0, 3600);
        antiSnipeExtensionSeconds = builder.comment("每次狙击延长多少秒（现实秒，0 表示不延长）").defineInRange("antiSnipeExtensionSeconds", 30, 0, Integer.MAX_VALUE);
        auctionEnabled = builder.comment("是否开放拍卖行（关闭后不能上架/出价/一口价，已有拍卖照常到期结算）").define("auctionEnabled", true);
        maxAuctionsGlobal = builder.comment("全服同时进行的拍卖上限").defineInRange("maxAuctionsGlobal", 500, 1, 5000);
        antiSnipeMaxExtensions = builder.comment("同一场拍卖防截拍最多延长几次（0=不限，仍受总时长 2 倍封顶）").defineInRange("antiSnipeMaxExtensions", 10, 0, 1000);
        auctionDurationOptions = builder.comment("上架时可选的拍卖时长（现实分钟），第一个为默认值；最长时长即其中的最大值")
                .defineList("auctionDurationOptions", List.of("30", "60", "180", "360", "720", "1440", "2880"), () -> "60", o -> o instanceof String);
        auctionListingFeeFixed = builder.comment("上架手续费：固定部分（星币，0=不收）").defineInRange("auctionListingFeeFixed", 0L, 0L, 1_000_000_000L);
        auctionListingFeePercent = builder.comment("上架手续费：按起拍价百分比（0=不收）").defineInRange("auctionListingFeePercent", 0.0D, 0.0D, 100.0D);
        auctionMinStartPrice = builder.comment("最低起拍价").defineInRange("auctionMinStartPrice", 1L, 1L, Long.MAX_VALUE);
        auctionMaxStartPrice = builder.comment("最高起拍价（0=只受余额上限约束）").defineInRange("auctionMaxStartPrice", 0L, 0L, Long.MAX_VALUE);
        auctionAllowBuyout = builder.comment("是否允许卖家设置一口价").define("auctionAllowBuyout", true);
        auctionMaxItemCount = builder.comment("单场拍卖最多上架几件（不超过该物品的堆叠上限）").defineInRange("auctionMaxItemCount", 64, 1, 64);
        auctionBlacklist = builder.comment("禁止拍卖的物品：物品 id（minecraft:bedrock）、标签（#c:ores）或整个模组（somemod:*）。容器里装的物品同样检查")
                .defineList("auctionBlacklist", List.of("minecraft:bedrock", "minecraft:barrier", "minecraft:command_block",
                        "minecraft:chain_command_block", "minecraft:repeating_command_block", "minecraft:command_block_minecart",
                        "minecraft:structure_block", "minecraft:structure_void", "minecraft:jigsaw", "minecraft:light",
                        "minecraft:debug_stick", "minecraft:knowledge_book", "minecraft:spawner", "minecraft:trial_spawner"),
                        () -> "minecraft:bedrock", o -> o instanceof String);
        auctionAllowBound = builder.comment("是否允许拍卖绑定物品（带绑定诅咒，或自定义数据里含下方标记的物品）").define("auctionAllowBound", false);
        auctionBoundMarkers = builder.comment("自定义数据（custom_data）里出现这些键名即视为绑定物品")
                .defineList("auctionBoundMarkers", List.of("Soulbound", "soulbound", "Bound", "bound", "BoundTo", "Owner"), () -> "Soulbound", o -> o instanceof String);
        auctionAllowCustomData = builder.comment("是否允许拍卖带自定义数据（custom_data，即特殊 NBT）的物品").define("auctionAllowCustomData", true);
        auctionMaxItemNbtBytes = builder.comment("拍品序列化后的最大字节数（防止超大 NBT 物品拖垮网络与存档）").defineInRange("auctionMaxItemNbtBytes", 32768, 1024, 1_048_576);
        auctionBlockCreative = builder.comment("创造模式玩家不能上架拍卖（创造模式可凭空生成物品）").define("auctionBlockCreative", true);
        auctionEndingSoonSeconds = builder.comment("剩余多少秒时提醒卖家与领先者「即将结束」（0=不提醒）").defineInRange("auctionEndingSoonSeconds", 300, 0, 86400);
        auctionHistoryLimit = builder.comment("保留多少条已结束的拍卖记录（全服）").defineInRange("auctionHistoryLimit", 2000, 100, 100000);
        builder.pop();

        builder.push("mailbox");
        maxMailboxSize = builder.comment("邮箱最大邮件数（默认 100，界面每页 45 封可翻页）").defineInRange("maxMailboxSize", 100, 1, 1000);
        mailExpireDays = builder.comment("邮件过期天数（0=永久，遵循 expire_time=永久；只清理已领取/已查看的邮件）").defineInRange("mailExpireDays", 0, 0, 100000);
        transactionHistorySize = builder.comment("每名玩家保留的交易流水条数（/shop admin history 查看）").defineInRange("transactionHistorySize", 50, 10, 200);
        builder.pop();

        builder.push("stall");
        stallSlots = builder.comment("摊位槽位数量").defineInRange("stallSlots", 12, 1, 45);
        // 需求冲突：free_listing=true 与 listing_fee="上架价5%"。默认免费，百分比留给服主调节。
        stallListingFeePercent = builder.comment("上架手续费百分比（0=免费上架）").defineInRange("stallListingFeePercent", 0.0D, 0.0D, 100.0D);
        stallSaleFeePercent = builder.comment("摊位成交税百分比（从卖家货款扣除并流入官方总钱箱，默认 5%）")
                .defineInRange("stallSaleFeePercent", 5.0D, 0.0D, 100.0D);
        stallMaxStock = builder.comment("摊位单件商品的库存上限（上架与补货共用，默认 64）").defineInRange("stallMaxStock", 64, 1, 100000);

        // 玩家摊位自动下架：商店设置里的「下架时间」（永久 / 按游戏天数 / 按现实小时）。
        stallExpireMode = builder.comment("玩家摊位自动下架方式：permanent=永久 / game=按游戏时间 / real=按现实时间")
                .define("stallExpireMode", "permanent");
        stallExpireAmount = builder.comment("限时时长：game 按游戏天数、real 按现实小时（0 = 永不自动下架）")
                .defineInRange("stallExpireAmount", 7, 0, 100000);
        builder.pop();

        builder.push("demand");
        demandEnabled = builder.comment("是否开启玩家求购（悬赏）模块")
                .define("demandEnabled", true);
        demandSlots = builder.comment("每名玩家最多同时发布几条求购").defineInRange("demandSlots", 5, 1, 45);
        demandFeePercent = builder.comment("发布手续费百分比（按悬赏金币算，0=免费）")
                .defineInRange("demandFeePercent", 0.0D, 0.0D, 100.0D);
        demandMaxReward = builder.comment("单笔悬赏金币上限（0=不限）")
                .defineInRange("demandMaxReward", 0, 0, Integer.MAX_VALUE);
        demandMaxListed = builder.comment("全服最多保留多少条求购（超出时最老的自动下架并退款）")
                .defineInRange("demandMaxListed", 60, 1, 400);
        demandExpireMode = builder.comment("求购自动下架方式：permanent=永久 / game=按游戏时间 / real=按现实时间")
                .define("demandExpireMode", "permanent");
        demandExpireAmount = builder.comment("限时时长：game 按游戏天数、real 按现实小时（0 = 永不自动下架）")
                .defineInRange("demandExpireAmount", 7, 0, 100000);
        builder.pop();

        builder.push("http_api");
        httpApiEnabled = builder.comment("是否启用 HTTP API（默认关闭）。开启前必须设置 httpApiToken，且建议保持只监听 127.0.0.1")
                .define("httpApiEnabled", false);
        httpApiBind = builder.comment("HTTP API 监听地址（默认只监听本机）").define("httpApiBind", "127.0.0.1");
        httpApiPort = builder.comment("HTTP API 端口").defineInRange("httpApiPort", 8765, 1024, 65535);
        httpApiToken = builder.comment("HTTP API 访问令牌（Bearer）；为空则不启动 HTTP API").define("httpApiToken", "");
        httpApiAllowWrite = builder.comment("是否允许写操作（加钱/扣钱/发奖/发邮件）；false = 只读")
                .define("httpApiAllowWrite", false);
        httpApiRateLimit = builder.comment("每个 IP 每分钟最大请求数（0 = 不限）")
                .defineInRange("httpApiRateLimit", 60, 0, 100000);
        builder.pop();

        builder.push("trade");
        tradeCountdownSeconds = builder.comment("双方同意后的确认倒计时（现实秒）").defineInRange("tradeCountdownSeconds", 3, 1, 60);
        // 需求 1：未被对方接受的交易请求超时后自动清理。
        tradeRequestExpireSeconds = builder.comment("交易请求失效时间（现实秒）").defineInRange("tradeRequestExpireSeconds", 60, 5, 3600);
        tradeSessionTimeoutSeconds = builder.comment("已接受的交易闲置多久自动结束并退回物品（现实秒，默认 600=10 分钟）")
                .defineInRange("tradeSessionTimeoutSeconds", 600, 30, 86400);
        builder.pop();

        builder.push("shipping_bin");
        shippingEnabled = builder.comment("是否启用「出货箱」模块（关掉后侧栏不显示，也不结算）")
                .define("shippingEnabled", true);
        shippingRules = builder.comment(
                "兑换规则：每行一条，格式为 物品ID=价格（也支持 #标签=价格、命名空间:*=价格）",
                "价格后面可以再加 x数量 限定该物品每天最多结算多少个，例如：",
                "  minecraft:wheat=4 x256",
                "  #minecraft:logs=3 x256",
                "  minecraft:diamond=90 x64",
                "注释行用 // 开头；第三方可选模组未安装时会自动静默跳过，安装后自动生效")
                .defineList("shippingRules", List.of(
                        "// ===== 1. 农牧 (farming)：原版与模组作物、果蔬、禽畜生鲜、药材与花卉 =====",
                        "minecraft:wheat=4 x256",
                        "minecraft:carrot=4 x256",
                        "minecraft:potato=4 x256",
                        "minecraft:beetroot=5 x256",
                        "minecraft:sugar_cane=3 x256",
                        "minecraft:pumpkin=6 x128",
                        "minecraft:melon_slice=2 x256",
                        "minecraft:sweet_berries=3 x256",
                        "minecraft:glow_berries=5 x192",
                        "minecraft:cocoa_beans=5 x192",
                        "minecraft:apple=6 x128",
                        "minecraft:nether_wart=6 x160",
                        "minecraft:chorus_fruit=6 x160",
                        "minecraft:bamboo=2 x256",
                        "minecraft:cactus=3 x256",
                        "minecraft:kelp=2 x256",
                        "minecraft:brown_mushroom=5 x192",
                        "minecraft:red_mushroom=5 x192",
                        "minecraft:crimson_fungus=6 x128",
                        "minecraft:warped_fungus=6 x128",
                        "minecraft:egg=4 x192",
                        "minecraft:feather=4 x192",
                        "minecraft:leather=5 x128",
                        "minecraft:rabbit_hide=2 x128",
                        "minecraft:rabbit_foot=16 x48",
                        "minecraft:armadillo_scute=12 x48",
                        "minecraft:turtle_scute=18 x48",
                        "minecraft:honey_bottle=10 x64",
                        "minecraft:honeycomb=9 x64",
                        "minecraft:beef=7 x128",
                        "minecraft:porkchop=7 x128",
                        "minecraft:chicken=5 x128",
                        "minecraft:mutton=7 x128",
                        "minecraft:rabbit=7 x128",
                        "kaleidoscope_cookery:tomato=6 x192",
                        "kaleidoscope_cookery:lettuce=6 x192",
                        "kaleidoscope_cookery:red_chili=6 x192",
                        "kaleidoscope_cookery:green_chili=6 x192",
                        "kaleidoscope_cookery:rice=3 x256",
                        "kaleidoscope_cookery:rice_panicle=6 x192",
                        "kaleidoscope_tavern:grape=7 x160",
                        "kaleidoscope_tavern:green_grape=9 x128",
                        "kaleidoscope_tavern:ice_grape=11 x128",
                        "kaleidoscope_tavern:gold_grape=20 x64",
                        "bakeries:coffee_bean=6 x192",
                        "bakeries:butter_cube=8 x128",
                        "bakeries:cheese_cube=9 x128",
                        "baishi_pantry:basil=6 x192",
                        "baishi_pantry:gai_lan=6 x192",
                        "baishi_pantry:scallion=5 x192",
                        "baishi_pantry:sand_onion=7 x160",
                        "baishi_pantry:goji_berry=8 x160",
                        "baishi_pantry:longan=8 x160",
                        "baishi_pantry:osmanthus=7 x160",
                        "baishi_pantry:smoked_plum=8 x160",
                        "baishi_pantry:ice_jelly_seed=7 x160",
                        "baishi_pantry:ginseng=15 x80",
                        "flavor_immersed_daily_argo:duck_egg=5 x128",
                        "flavor_immersed_daily_argo:goose_egg=5 x128",
                        "flavor_immersed_daily_argo:pigeon_egg=5 x128",
                        "flavor_immersed_daily_argo:quail_egg=5 x128",
                        "flavor_immersed_daily_argo:raw_animal_blood=8 x128",
                        "flavor_immersed_daily_argo:raw_animal_heart=8 x128",
                        "flavor_immersed_daily_argo:raw_animal_intestine=8 x128",
                        "flavor_immersed_daily_argo:raw_animal_liver=8 x128",
                        "flavor_immersed_daily_argo:raw_animal_lung=8 x128",
                        "flavor_immersed_daily_argo:raw_animal_stomach=8 x128",
                        "flavor_immersed_daily_argo:raw_camel_hump=8 x128",
                        "flavor_immersed_daily_argo:raw_camel_meat=8 x128",
                        "flavor_immersed_daily_argo:raw_dog_meat=8 x128",
                        "flavor_immersed_daily_argo:raw_donkey_meat=8 x128",
                        "flavor_immersed_daily_argo:raw_dressed_duck=8 x128",
                        "flavor_immersed_daily_argo:raw_dressed_goose=8 x128",
                        "flavor_immersed_daily_argo:raw_dressed_pigeon=8 x128",
                        "flavor_immersed_daily_argo:raw_dressed_quail=8 x128",
                        "flavor_immersed_daily_argo:raw_dressed_turkey=8 x128",
                        "flavor_immersed_daily_argo:raw_duck_blood=8 x128",
                        "flavor_immersed_daily_argo:raw_duck_gizzard=8 x128",
                        "flavor_immersed_daily_argo:raw_duck_head=8 x128",
                        "flavor_immersed_daily_argo:raw_duck_heart=8 x128",
                        "flavor_immersed_daily_argo:raw_duck_intestine=8 x128",
                        "flavor_immersed_daily_argo:raw_duck_leg=8 x128",
                        "flavor_immersed_daily_argo:raw_duck_liver=8 x128",
                        "flavor_immersed_daily_argo:raw_duck_meat=8 x128",
                        "flavor_immersed_daily_argo:raw_duck_neck=8 x128",
                        "flavor_immersed_daily:kaolianggrain=6 x192",
                        "flavor_immersed_daily:radish=6 x192",
                        "flavor_immersed_daily:whitemushroom=6 x192",
                        "bountifulfares:elderberries=6 x192",
                        "bountifulfares:lapisberries=6 x192",
                        "bountifulfares:leek=6 x192",
                        "bountifulfares:maize=6 x192",
                        "bountifulfares:passion_fruit=6 x192",
                        "bountifulfares:tea_berries=6 x192",
                        "kaleidoscope_chinesefood:eggplant=7 x160",
                        "bakeries:tomato=6 x192",
                        "guaniao:feather_barred=10 x96",
                        "guaniao:feather_black=10 x96",
                        "guaniao:feather_blue=10 x96",
                        "guaniao:feather_brown=10 x96",
                        "guaniao:feather_chestnut=10 x96",
                        "guaniao:feather_green=10 x96",
                        "guaniao:feather_grey=10 x96",
                        "guaniao:feather_orange=10 x96",
                        "guaniao:feather_pink=10 x96",
                        "guaniao:feather_purple=10 x96",
                        "guaniao:feather_rainbow=15 x96",
                        "guaniao:feather_red=10 x96",
                        "guaniao:feather_speckled=10 x96",
                        "guaniao:feather_white=10 x96",
                        "guaniao:feather_yellow=10 x96",
                        "wilderflowers:clovers=7 x128",
                        "#minecraft:wool=5 x128",
                        "#c:crops=4 x256",
                        "// ===== 2. 厨师 (cooking)：原版与全模组中餐、药膳、烘焙、火锅、维度料理（严格锚定配方原料成本 1.3x~1.8x） =====",
                        "minecraft:bread=15 x96",
                        "minecraft:baked_potato=6 x128",
                        "minecraft:cookie=2 x256",
                        "minecraft:dried_kelp=3 x256",
                        "minecraft:cooked_chicken=8 x128",
                        "minecraft:cooked_cod=9 x128",
                        "minecraft:cooked_rabbit=10 x128",
                        "minecraft:cooked_mutton=10 x128",
                        "minecraft:cooked_beef=10 x128",
                        "minecraft:cooked_porkchop=10 x128",
                        "minecraft:cooked_salmon=12 x128",
                        "minecraft:pumpkin_pie=18 x96",
                        "minecraft:mushroom_stew=15 x64",
                        "minecraft:beetroot_soup=38 x48",
                        "minecraft:rabbit_stew=33 x48",
                        "minecraft:golden_carrot=24 x48",
                        "minecraft:golden_apple=138 x16",
                        "minecraft:cake=36 x32",
                        "kaleidoscope_cookery:fried_egg=6 x96",
                        "kaleidoscope_cookery:tea_egg=13 x96",
                        "kaleidoscope_cookery:shengjian_mantou=7 x96",
                        "kaleidoscope_cookery:scramble_egg_with_tomatoes=14 x96",
                        "kaleidoscope_cookery:scramble_egg_with_tomatoes_rice_bowl=20 x64",
                        "kaleidoscope_cookery:stir_fried_pork_with_peppers=18 x64",
                        "kaleidoscope_cookery:stir_fried_pork_with_peppers_rice_bowl=26 x64",
                        "kaleidoscope_cookery:fish_flavored_shredded_pork=24 x64",
                        "kaleidoscope_cookery:sweet_and_sour_pork=14 x96",
                        "kaleidoscope_cookery:sweet_and_sour_pork_rice_bowl=20 x64",
                        "kaleidoscope_cookery:braised_beef=18 x64",
                        "kaleidoscope_cookery:braised_beef_rice_bowl=26 x64",
                        "kaleidoscope_chinesefood:tomato_egg_drop_soup=11 x96",
                        "kaleidoscope_chinesefood:seaweed_egg_drop_soup=26 x64",
                        "kaleidoscope_chinesefood:century_egg_congee=80 x48",
                        "kaleidoscope_chinesefood:sichuan_wonton=37 x48",
                        "kaleidoscope_chinesefood:wonton_noodles=36 x48",
                        "kaleidoscope_chinesefood:stir_fried_three_fresh_vegetables=48 x48",
                        "kaleidoscope_chinesefood:twice_cooked_pork_rice=85 x48",
                        "kaleidoscope_chinesefood:stir_fried_yellow_beef_rice=67 x48",
                        "kaleidoscope_chinesefood:maocai=37 x48",
                        "kaleidoscope_chinesefood:yangrou_paomo=61 x48",
                        "fidworkblock:plate=20 x64",
                        "fidworkblock:long_plate=22 x64",
                        "fidworkblock:soup=20 x64",
                        "bakeries:bagel=7 x96",
                        "bakeries:baguette=7 x96",
                        "bakeries:ciabatta=7 x96",
                        "bakeries:country_bread=7 x96",
                        "bakeries:cheese_cream_bread=13 x96",
                        "bakeries:brown_sugar_latte=26 x64",
                        "bakeries:carrot_cake=14 x96",
                        "bakeries:basque_cake=14 x96",
                        "bakeries:cream_cake=38 x48",
                        "orients_hotpot_chronicles:cooked_fatty_beef_roll=5 x96",
                        "orients_hotpot_chronicles:cooked_mutton_roll=5 x96",
                        "orients_hotpot_chronicles:cooked_tripe=7 x96",
                        "orients_hotpot_chronicles:cooked_beef_loin_slice=7 x96",
                        "orients_hotpot_chronicles:spicy_red_broth_bowl=18 x64",
                        "orients_hotpot_chronicles:rich_stock_bowl=18 x64",
                        "bountifulfares:artisan_bread=23 x64",
                        "bountifulfares:artisan_cookie=6 x96",
                        "bountifulfares:black_tea_bottle=14 x96",
                        "bountifulfares:chamomile_tea_bottle=14 x96",
                        "bountifulfares:apple_pie=24 x64",
                        "bountifulfares:bountiful_stew=30 x48",
                        "tlm_overture:sweet_berry_cake=68 x48",
                        "tlm_overture:sweet_berry_candy=38 x48",
                        "guaniao:cooked_fries=11 x96",
                        "upgrade_aquatic:cooked_pike=13 x96",
                        "upgrade_aquatic:cooked_perch=13 x96",
                        "upgrade_aquatic:cooked_lionfish=20 x64",
                        "twilightforest:cooked_venison=7 x96",
                        "twilightforest:cooked_meef=7 x96",
                        "twilightforest:meef_stroganoff=24 x48",
                        "twilightforest:hydra_chop=55 x16",
                        "twilightforest:maze_wafer=16 x48",
                        "twilightforest:experiment_115=18 x48",
                        "kaleidoscope_cookery:apple_platter=36 x48",
                        "kaleidoscope_cookery:bamboo_tube_rice=7 x96",
                        "kaleidoscope_cookery:baozi=7 x96",
                        "kaleidoscope_cookery:baozi_plate=51 x48",
                        "kaleidoscope_cookery:barley_tea=7 x96",
                        "kaleidoscope_cookery:beef_meatball_soup=16 x96",
                        "kaleidoscope_cookery:beef_noodle=17 x96",
                        "kaleidoscope_cookery:berry_platter=47 x48",
                        "kaleidoscope_cookery:biluochun=7 x96",
                        "kaleidoscope_cookery:blaze_lamb_chop=17 x96",
                        "kaleidoscope_cookery:borscht=18 x64",
                        "kaleidoscope_cookery:braised_pork_ribs=14 x96",
                        "kaleidoscope_cookery:butter_tea=7 x96",
                        "kaleidoscope_cookery:chicken_and_mushroom_stew=13 x96",
                        "kaleidoscope_cookery:chorus_fried_egg=14 x96",
                        "kaleidoscope_cookery:chorus_fruit_platter=44 x48",
                        "kaleidoscope_cookery:cold_roasted_meat=44 x48",
                        "kaleidoscope_cookery:cold_style_sashimi=34 x48",
                        "kaleidoscope_cookery:cooked_meatball=7 x96",
                        "kaleidoscope_cookery:cooked_rice=5 x96",
                        "kaleidoscope_cookery:crystal_lamb_chop=14 x96",
                        "kaleidoscope_cookery:desert_style_sashimi=21 x64",
                        "kaleidoscope_cookery:dongpo_pork=13 x96",
                        "kaleidoscope_cookery:donkey_burger=17 x96",
                        "kaleidoscope_cookery:dumpling=7 x96",
                        "kaleidoscope_cookery:egg_fried_rice=13 x96",
                        "kaleidoscope_cookery:end_style_sashimi=38 x48",
                        "kaleidoscope_cookery:fearsome_thick_soup=64 x48",
                        "kaleidoscope_cookery:fish_flavored_shredded_pork_rice_bowl=34 x48",
                        "kaleidoscope_cookery:flower_tea=6 x96",
                        "kaleidoscope_cookery:fondant_pie=30 x48",
                        "kaleidoscope_cookery:fondant_spider_eye=10 x96",
                        "kaleidoscope_cookery:four_joy_meatball_soup=7 x96",
                        "kaleidoscope_cookery:fried_caterpillar=7 x96",
                        "kaleidoscope_cookery:fried_spring_roll=36 x48",
                        "kaleidoscope_cookery:frost_lamb_chop=17 x96",
                        "kaleidoscope_chinesefood:bamboo_steamed_egg=7 x96",
                        "kaleidoscope_chinesefood:beef_with_scrambled_eggs=37 x48",
                        "kaleidoscope_chinesefood:beef_with_scrambled_eggs_rice=53 x48",
                        "kaleidoscope_chinesefood:big_plate_chicken=43 x48",
                        "kaleidoscope_chinesefood:big_plate_chicken_noodles=50 x48",
                        "kaleidoscope_chinesefood:century_egg=18 x64",
                        "kaleidoscope_chinesefood:dianhong_tea=7 x96",
                        "kaleidoscope_chinesefood:douzhi=13 x96",
                        "kaleidoscope_chinesefood:four_joy_meatballs=37 x48",
                        "kaleidoscope_chinesefood:hk_milk_tea=7 x96",
                        "kaleidoscope_chinesefood:lamb_pilaf=54 x48",
                        "kaleidoscope_chinesefood:mooncake=7 x96",
                        "kaleidoscope_chinesefood:pork_chili_noodles=62 x48",
                        "kaleidoscope_chinesefood:red_rice_roll=23 x64",
                        "kaleidoscope_chinesefood:steamed_rice_rolls=7 x96",
                        "kaleidoscope_chinesefood:stir_fried_yellow_beef=47 x48",
                        "kaleidoscope_chinesefood:straw_bed=58 x48",
                        "kaleidoscope_chinesefood:stuffed_eggplant=49 x48",
                        "kaleidoscope_chinesefood:tomato_egg_noodles=58 x48",
                        "kaleidoscope_chinesefood:twice_cooked_pork=60 x48",
                        "kaleidoscope_chinesefood:yangzhou_fried_rice=31 x48",
                        "kaleidoscope_chinesefood:yellow_croaker_soup=26 x64",
                        "kaleidoscope_chinesefood:yellow_croaker_tofu_soup=28 x48",
                        "baishi_pantry:cx_brown_sugar_rice_cake=18 x64",
                        "baishi_pantry:cx_mung_bean_soup=13 x96",
                        "baishi_pantry:db_frozen_pear=14 x96",
                        "baishi_pantry:db_pear_juice=20 x64",
                        "baishi_pantry:db_pork_vermicelli=36 x48",
                        "baishi_pantry:jing_braised_fish=43 x48",
                        "baishi_pantry:jing_hawthorn_drink=13 x96",
                        "baishi_pantry:jing_pear_drink=13 x96",
                        "baishi_pantry:jing_radish_salad=36 x48",
                        "baishi_pantry:jing_sauteed_fish=41 x48",
                        "baishi_pantry:jing_shredded_pork=34 x48",
                        "baishi_pantry:jp_citrus_tea=18 x64",
                        "baishi_pantry:jp_cold_tofu=23 x64",
                        "baishi_pantry:jp_eel_sushi=31 x48",
                        "baishi_pantry:jp_kimchi=34 x48",
                        "baishi_pantry:jp_matcha_latte=20 x64",
                        "baishi_pantry:jp_oyakodon=28 x48",
                        "baishi_pantry:jp_strawberry_daifuku=18 x64",
                        "baishi_pantry:jp_tonkatsu=33 x48",
                        "baishi_pantry:lh_honey_tea=23 x64",
                        "baishi_pantry:lh_tofu_skin_salad=28 x48",
                        "baishi_pantry:mascarpone=23 x64",
                        "baishi_pantry:nether_flame_tea=38 x48",
                        "baishi_pantry:sz_green_tea=9 x96",
                        "baishi_pantry:sz_lotus_root_sweet=11 x96",
                        "baishi_pantry:west_beef_wellington=31 x48",
                        "kaleidoscope_nether:blaze_soup=16 x96",
                        "kaleidoscope_nether:blazing_kabob=22 x64",
                        "kaleidoscope_nether:braised_lion_head=54 x48",
                        "kaleidoscope_nether:braised_pork_rice=55 x48",
                        "kaleidoscope_nether:braised_strider=37 x48",
                        "kaleidoscope_nether:caramel_nether_caterpillar=24 x64",
                        "kaleidoscope_nether:caramel_nether_caterpillar_rice=16 x96",
                        "kaleidoscope_nether:chongqing_noodles=22 x64",
                        "kaleidoscope_nether:cooked_piglin_meat=7 x96",
                        "kaleidoscope_nether:cooked_strider_meat=7 x96",
                        "kaleidoscope_nether:crimson_kabob=14 x96",
                        "kaleidoscope_nether:crimson_magma_stew=65 x48",
                        "kaleidoscope_nether:crimson_salad=43 x48",
                        "kaleidoscope_nether:forgetfulness_soup=40 x48",
                        "kaleidoscope_nether:fruit_platter=34 x48",
                        "kaleidoscope_nether:garlic_oysters=71 x48",
                        "kaleidoscope_nether:ghast_kabob=20 x64",
                        "kaleidoscope_nether:ghast_pasta=68 x48",
                        "kaleidoscope_nether:ghast_pudding=85 x48",
                        "kaleidoscope_nether:gilded_barbaric_roast=54 x48",
                        "kaleidoscope_nether:golden_kabob=85 x48",
                        "kaleidoscope_nether:golden_roast=85 x48",
                        "kaleidoscope_nether:ham_yogurt=38 x48",
                        "kaleidoscope_nether:hoglin_tusk_braised_meat=38 x48",
                        "kaleidoscope_end:chorus_flower_cake=67 x48",
                        "kaleidoscope_end:chorus_flower_soup=14 x96",
                        "kaleidoscope_end:chorus_flower_tea=7 x96",
                        "kaleidoscope_end:chorus_pasta=34 x48",
                        "kaleidoscope_end:cooked_ender_dragon_meat=7 x96",
                        "kaleidoscope_end:dark_dragon_egg_stew=43 x48",
                        "kaleidoscope_end:dark_dragon_steak=45 x48",
                        "kaleidoscope_end:dragon_breath_chorus_soup=7 x96",
                        "kaleidoscope_end:dragon_breath_mixed_stew=85 x48",
                        "kaleidoscope_end:dragon_breath_popping_candy=11 x96",
                        "kaleidoscope_end:dragon_egg_custard=43 x48",
                        "kaleidoscope_end:dragon_egg_ice_cream=54 x48",
                        "kaleidoscope_end:dragon_head_with_sauce=30 x48",
                        "kaleidoscope_end:dragon_souffle=41 x48",
                        "kaleidoscope_end:end_caterpillar_sashimi=47 x48",
                        "kaleidoscope_end:end_salad=40 x48",
                        "kaleidoscope_end:ender_dragon_tea=7 x96",
                        "kaleidoscope_end:ender_mint_candy=8 x96",
                        "kaleidoscope_end:ender_mint_tea=7 x96",
                        "kaleidoscope_end:fried_dragon_egg=7 x96",
                        "kaleidoscope_flora:absolution=7 x96",
                        "kaleidoscope_flora:as_you_wish=7 x96",
                        "kaleidoscope_flora:autumn_serenade=7 x96",
                        "kaleidoscope_flora:breath_of_ancients=7 x96",
                        "kaleidoscope_flora:coronation=7 x96",
                        "kaleidoscope_flora:dew_flower_cake=7 x96",
                        "kaleidoscope_flora:echo_of_the_end=7 x96",
                        "kaleidoscope_flora:fire_waltz=7 x96",
                        "kaleidoscope_flora:first_bloom=7 x96",
                        "kaleidoscope_flora:fleeting_bloom=7 x96",
                        "kaleidoscope_flora:fleurs_du_mal=7 x96",
                        "kaleidoscope_flora:hanami_tale=7 x96",
                        "kaleidoscope_flora:loves_me_not=7 x96",
                        "kaleidoscope_flora:lullaby=7 x96",
                        "kaleidoscope_flora:may_kiss=7 x96",
                        "kaleidoscope_flora:prussian_leap=7 x96",
                        "kaleidoscope_flora:rosy_stride=7 x96",
                        "kaleidoscope_flora:spring_waltz=7 x96",
                        "bakeries:bottle_milk=5 x96",
                        "bakeries:brown_sugar_roll=7 x96",
                        "bakeries:butter_latte=33 x48",
                        "bakeries:cake_base=14 x96",
                        "bakeries:cheese_cocoa_toast=14 x96",
                        "bakeries:cooked_taro=7 x96",
                        "bakeries:cream_bingle_coffee=28 x48",
                        "bakeries:croissant=7 x96",
                        "bakeries:dirty_choco_croissant=17 x96",
                        "bakeries:egg_tart=7 x96",
                        "bakeries:flat_croissant=10 x96",
                        "bakeries:honey_butter_spread_country_bread=5 x96",
                        "bakeries:honey_butter_spread_toast=9 x96",
                        "bakeries:iced_latte=21 x64",
                        "bakeries:matcha_cake=14 x96",
                        "bakeries:matcha_latte=21 x64",
                        "bakeries:meat_floss_bread_roll=12 x96",
                        "bakeries:mooncake=7 x96",
                        "bakeries:pineapple_bun=7 x96",
                        "bakeries:pork_mooncake=7 x96",
                        "bakeries:pound_cake=14 x96",
                        "bakeries:red_velvet_cake=27 x64",
                        "bakeries:red_velvet_cake_base=14 x96",
                        "bakeries:rice_bread=7 x96",
                        "bakeries:round_bread=7 x96",
                        "bakeries:scone=7 x96",
                        "orients_hotpot_chronicles:cooked_beef_loin=7 x96",
                        "orients_hotpot_chronicles:cooked_beef_meatball=7 x96",
                        "orients_hotpot_chronicles:cooked_cilantro_beef_meatball=7 x96",
                        "orients_hotpot_chronicles:cooked_cilantro_fish_ball=7 x96",
                        "orients_hotpot_chronicles:cooked_cilantro_mutton_meatball=7 x96",
                        "orients_hotpot_chronicles:cooked_cilantro_pork_meatball=7 x96",
                        "orients_hotpot_chronicles:cooked_dried_tofu=7 x96",
                        "orients_hotpot_chronicles:cooked_enoki_mushroom=7 x96",
                        "orients_hotpot_chronicles:cooked_fish_ball=7 x96",
                        "orients_hotpot_chronicles:cooked_greens=7 x96",
                        "orients_hotpot_chronicles:cooked_mutton_meatball=7 x96",
                        "orients_hotpot_chronicles:cooked_peeled_shrimp=7 x96",
                        "orients_hotpot_chronicles:cooked_pork_meatball=7 x96",
                        "orients_hotpot_chronicles:cooked_shrimp=7 x96",
                        "orients_hotpot_chronicles:cooked_squid_roll=7 x96",
                        "orients_hotpot_chronicles:cooked_tofu_skin=7 x96",
                        "orients_hotpot_chronicles:cooked_wide_noodles=7 x96",
                        "bountifulfares:apple_cider_jar=9 x96",
                        "bountifulfares:apple_stew=36 x48",
                        "bountifulfares:arid_medley=40 x48",
                        "bountifulfares:bellflower_tea_bottle=14 x96",
                        "bountifulfares:coastal_medley=28 x48",
                        "bountifulfares:cocoa_cake=58 x48",
                        "bountifulfares:coconut_cake=53 x48",
                        "bountifulfares:coconut_crusted_cod=28 x48",
                        "bountifulfares:coconut_milk_bottle=7 x96",
                        "bountifulfares:coconut_stew=24 x64",
                        "bountifulfares:cooked_egg=6 x96",
                        "bountifulfares:crimson_chow=40 x48",
                        "bountifulfares:crusted_beef=44 x48",
                        "bountifulfares:elderberry_tart=24 x64",
                        "bountifulfares:fish_stew=36 x48",
                        "bountifulfares:forest_medley=45 x48",
                        "bountifulfares:glow_berry_tart=23 x64",
                        "bountifulfares:green_tea_bottle=14 x96",
                        "bountifulfares:hoary_cider_jar=7 x96",
                        "bountifulfares:hoary_pie=23 x64",
                        "flavor_immersed_daily:applejam=9 x96",
                        "flavor_immersed_daily:apricotjam=7 x96",
                        "flavor_immersed_daily:bananajam=7 x96",
                        "flavor_immersed_daily:blueberryjam=7 x96",
                        "flavor_immersed_daily:cafelatte=16 x96",
                        "flavor_immersed_daily:carambolajam=7 x96",
                        "flavor_immersed_daily:cherryjam=7 x96",
                        "flavor_immersed_daily:coconutjam=7 x96",
                        "flavor_immersed_daily:coffeeicecream=14 x96",
                        "flavor_immersed_daily:coffeepopsicle=11 x96",
                        "flavor_immersed_daily:cookedchickenpiece=7 x96",
                        "flavor_immersed_daily:crispyeggcake=7 x96",
                        "flavor_immersed_daily:dragonfruitjam=7 x96",
                        "flavor_immersed_daily:durianjam=7 x96",
                        "flavor_immersed_daily:eggnoodleswrappedinsyrup=14 x96",
                        "flavor_immersed_daily:eggtart=7 x96",
                        "flavor_immersed_daily:fried_tofu_skin_rolls=11 x96",
                        "flavor_immersed_daily:goldricecake=85 x48",
                        "flavor_immersed_daily:goldricecakemax=85 x48",
                        "flavor_immersed_daily:grapejam=7 x96",
                        "flavor_immersed_daily:greemplumjam=7 x96",
                        "flavor_immersed_daily:greengrapejam=7 x96",
                        "flavor_immersed_daily:gultinousricesasamepowder=7 x96",
                        "flavor_immersed_daily:hamburgerbread=43 x48",
                        "flavor_immersed_daily:hamimelonjam=7 x96",
                        "flavor_immersed_daily:hawthornjam=7 x96",
                        "flavor_immersed_daily:honeypeachjam=7 x96",
                        "flavor_immersed_daily:kiwifruitjam=7 x96",
                        "flavor_immersed_daily:lemonjam=7 x96",
                        "flavor_immersed_daily:loquatjam=7 x96",
                        "flavor_immersed_daily_argo:braised_duck_platter=50 x48",
                        "flavor_immersed_daily_argo:cooked_brown_crab_meat=20 x64",
                        "flavor_immersed_daily_argo:cooked_king_crab_leg=7 x96",
                        "flavor_immersed_daily_argo:cooked_lobster_meat=7 x96",
                        "flavor_immersed_daily_argo:cooked_squid_meat=7 x96",
                        "flavor_immersed_daily_argo:roasted_chestnut=7 x96",
                        "flavor_immersed_daily_argo:roasted_goose=11 x96",
                        "// ===== 3. 钓鱼 (fishing)：原版、捕星者全稀有度鱼种、东篱水产与水生升级（支持黄金鱼与体型溢价） =====",
                        "minecraft:cod=6 x128",
                        "minecraft:salmon=8 x128",
                        "minecraft:tropical_fish=12 x96",
                        "minecraft:pufferfish=14 x96",
                        "minecraft:ink_sac=5 x128",
                        "minecraft:nautilus_shell=42 x32",
                        "starcatcher:aurora=58 x20",
                        "starcatcher:azure_crystalback_minnow=58 x20",
                        "starcatcher:cerberay=58 x20",
                        "starcatcher:cloudfin=58 x20",
                        "starcatcher:deepslatefish=58 x20",
                        "starcatcher:joel=58 x20",
                        "starcatcher:king_of_the_frost=58 x20",
                        "starcatcher:oasis_sturgeon=58 x20",
                        "starcatcher:obsidian_eel=58 x20",
                        "starcatcher:scorched_bloodsucker=58 x20",
                        "starcatcher:shroomfish=58 x20",
                        "starcatcher:stonefish=58 x20",
                        "starcatcher:vesani=58 x20",
                        "starcatcher:voidbiter=58 x20",
                        "starcatcher:ward=58 x20",
                        "starcatcher:amethystback=42 x32",
                        "starcatcher:bigeye_tuna=42 x32",
                        "starcatcher:blue_ice_pike=42 x32",
                        "starcatcher:boreal=42 x32",
                        "starcatcher:bright_amethyst_snapper=42 x32",
                        "starcatcher:cactifish=42 x32",
                        "starcatcher:carpenjoe=42 x32",
                        "starcatcher:chorus_crab=42 x32",
                        "starcatcher:dark_amethyst_snapper=42 x32",
                        "starcatcher:eel_dynamo=42 x32",
                        "starcatcher:frostbit_jelly=42 x32",
                        "starcatcher:geode_eel=42 x32",
                        "starcatcher:lava_crab=42 x32",
                        "starcatcher:lightning_bass=42 x32",
                        "starcatcher:livid_bamboo=42 x32",
                        "starcatcher:lush_pike=42 x32",
                        "starcatcher:molten_deepslate_crab=42 x32",
                        "starcatcher:nebula_squid=42 x32",
                        "starcatcher:obsidian_crab=42 x32",
                        "starcatcher:purple_carp=42 x32",
                        "starcatcher:rose_siamese_fish=42 x32",
                        "starcatcher:sage_catfish=42 x32",
                        "starcatcher:sculkfish=42 x32",
                        "starcatcher:thunder_bass=42 x32",
                        "starcatcher:voidfin=42 x32",
                        "starcatcher:willow_bream=42 x32",
                        "starcatcher:agave_bream=30 x48",
                        "starcatcher:blue_crystal_fin=30 x48",
                        "starcatcher:blue_herring=30 x48",
                        "starcatcher:bluegigi=30 x48",
                        "starcatcher:charfish=30 x48",
                        "starcatcher:cinder_squid=30 x48",
                        "starcatcher:cogtopus=30 x48",
                        "starcatcher:crystalback_boreal=30 x48",
                        "starcatcher:drive_pike=30 x48",
                        "starcatcher:fossilized_angelfish=30 x48",
                        "starcatcher:glowing_dark=30 x48",
                        "starcatcher:glowstone_pufferfish=30 x48",
                        "starcatcher:lilac_minnow=30 x48",
                        "starcatcher:lily_snapper=30 x48",
                        "starcatcher:mechanical_brass_snail=30 x48",
                        "starcatcher:meka_agave_bream=30 x48",
                        "starcatcher:molten_shrimp=30 x48",
                        "starcatcher:mothfish=30 x48",
                        "starcatcher:pale_pinfish=30 x48",
                        "starcatcher:petal_bass=30 x48",
                        "starcatcher:pipehead=30 x48",
                        "starcatcher:ripple_catfish=30 x48",
                        "starcatcher:spacejelly=30 x48",
                        "starcatcher:sporefish=30 x48",
                        "starcatcher:sun_seeking_carp=30 x48",
                        "starcatcher:suneater=30 x48",
                        "starcatcher:sunflower_carp=30 x48",
                        "starcatcher:the_quarrish=30 x48",
                        "starcatcher:twilight_koi=30 x48",
                        "starcatcher:wintery_pike=30 x48",
                        "starcatcher:black_eel=21 x64",
                        "starcatcher:brassgill=21 x64",
                        "starcatcher:chorus_minnow=21 x64",
                        "starcatcher:coggill=21 x64",
                        "starcatcher:cryospine=21 x64",
                        "starcatcher:crystalback_minnow=21 x64",
                        "starcatcher:crystalback_trout=21 x64",
                        "starcatcher:downfall_bream=21 x64",
                        "starcatcher:dreamliner=21 x64",
                        "starcatcher:elderscale=21 x64",
                        "starcatcher:end_glow=21 x64",
                        "starcatcher:ghostly_pike=21 x64",
                        "starcatcher:icetooth_sturgeon=21 x64",
                        "starcatcher:ironjaw_herring=21 x64",
                        "starcatcher:magma_fish=21 x64",
                        "starcatcher:mechanical_snail=21 x64",
                        "starcatcher:mirage_carp=21 x64",
                        "starcatcher:morganite=21 x64",
                        "starcatcher:mossfin=21 x64",
                        "starcatcher:petaldrift_carp=21 x64",
                        "starcatcher:pinfish=21 x64",
                        "starcatcher:pyrotrout=21 x64",
                        "starcatcher:redscaled_tuna=21 x64",
                        "starcatcher:scalding_pike=21 x64",
                        "starcatcher:shadowfin=21 x64",
                        "starcatcher:silverfin_pike=21 x64",
                        "starcatcher:silverveil_perch=21 x64",
                        "starcatcher:sludge_catfish=21 x64",
                        "starcatcher:sunny_sturgeon=21 x64",
                        "starcatcher:willish=21 x64",
                        "starcatcher:yellowstone_fish=21 x64",
                        "starcatcher:aquamarine_pike=15 x80",
                        "starcatcher:blossomfish=15 x80",
                        "starcatcher:crystalback_sturgeon=15 x80",
                        "starcatcher:deepjaw_herring=15 x80",
                        "starcatcher:driftfin=15 x80",
                        "starcatcher:drifting_bream=15 x80",
                        "starcatcher:dripfin=15 x80",
                        "starcatcher:dusktail_snapper=15 x80",
                        "starcatcher:embergill=15 x80",
                        "starcatcher:frostgill_chub=15 x80",
                        "starcatcher:frostjaw_trout=15 x80",
                        "starcatcher:garnet_mackerel=15 x80",
                        "starcatcher:glimmergill=15 x80",
                        "starcatcher:glowstone_seeker=15 x80",
                        "starcatcher:gold_fan=15 x80",
                        "starcatcher:hollowbelly_darter=15 x80",
                        "starcatcher:mistback_chub=15 x80",
                        "starcatcher:obidontiee=15 x80",
                        "starcatcher:pale_carp=15 x80",
                        "starcatcher:peakdweller=15 x80",
                        "starcatcher:phillipsfish=15 x80",
                        "starcatcher:pink_koi=15 x80",
                        "starcatcher:rainfin=15 x80",
                        "starcatcher:rockgill=15 x80",
                        "starcatcher:sandtail=15 x80",
                        "starcatcher:scorchfish=15 x80",
                        "starcatcher:sea_bass=15 x80",
                        "starcatcher:valve=15 x80",
                        "starcatcher:vivid_moss=15 x80",
                        "starcatcher:whiteveil=15 x80",
                        "starcatcher:starcaught_fish=15 x80",
                        "flavor_immersed_daily_argo:raw_pleco=10 x80",
                        "flavor_immersed_daily_argo:raw_grass_carp=12 x80",
                        "flavor_immersed_daily_argo:raw_crucian_carp=12 x80",
                        "flavor_immersed_daily_argo:raw_silver_carp=13 x80",
                        "flavor_immersed_daily_argo:raw_redfin=13 x80",
                        "flavor_immersed_daily_argo:raw_tench=14 x80",
                        "flavor_immersed_daily_argo:raw_tilapia=14 x80",
                        "flavor_immersed_daily_argo:raw_bighead_carp=15 x80",
                        "flavor_immersed_daily_argo:raw_chinese_carp=15 x80",
                        "flavor_immersed_daily_argo:raw_goby=15 x80",
                        "flavor_immersed_daily_argo:raw_black_carp=16 x80",
                        "flavor_immersed_daily_argo:raw_loach=16 x80",
                        "flavor_immersed_daily_argo:raw_catfish=17 x80",
                        "flavor_immersed_daily_argo:raw_mandarin_fish=18 x80",
                        "flavor_immersed_daily_argo:raw_bass=20 x80",
                        "flavor_immersed_daily_argo:raw_black_fish=22 x80",
                        "flavor_immersed_daily_argo:raw_abalone=9 x80",
                        "flavor_immersed_daily_argo:raw_akiami_shrimp=9 x80",
                        "flavor_immersed_daily_argo:raw_anglerfish=14 x80",
                        "flavor_immersed_daily_argo:raw_anglerfish_liver=14 x80",
                        "flavor_immersed_daily_argo:raw_brown_crab=14 x80",
                        "flavor_immersed_daily_argo:raw_clam=9 x80",
                        "flavor_immersed_daily_argo:raw_crayfish=9 x80",
                        "flavor_immersed_daily_argo:raw_eel=14 x80",
                        "flavor_immersed_daily_argo:raw_eel_segment=14 x80",
                        "flavor_immersed_daily_argo:raw_fish_head=14 x80",
                        "flavor_immersed_daily_argo:raw_fish_liver=14 x80",
                        "flavor_immersed_daily_argo:raw_fish_roe=14 x80",
                        "flavor_immersed_daily_argo:raw_fish_skin=14 x80",
                        "upgrade_aquatic:pike=9 x128",
                        "upgrade_aquatic:perch=9 x128",
                        "upgrade_aquatic:lionfish=14 x96",
                        "kaleidoscope_chinesefood:yellow_croaker=14 x96",
                        "#starcatcher:legendary_starcaught_fishes=58 x20",
                        "#starcatcher:epic_starcaught_fishes=42 x32",
                        "#starcatcher:rare_starcaught_fishes=30 x48",
                        "#starcatcher:uncommon_starcaught_fishes=21 x64",
                        "#starcatcher:common_starcaught_fishes=15 x80",
                        "#minecraft:fishes=7 x128",
                        "// ===== 4. 战斗 (combat)：原版、灾变 Boss、暮色森林、铁魔法战利品与深海劫掠素材（已做防刷怪塔限额调优） =====",
                        "minecraft:rotten_flesh=3 x128",
                        "minecraft:bone=3 x128",
                        "minecraft:string=3 x128",
                        "minecraft:spider_eye=4 x128",
                        "minecraft:gunpowder=6 x96",
                        "minecraft:slime_ball=8 x64",
                        "minecraft:prismarine_shard=7 x64",
                        "minecraft:prismarine_crystals=10 x64",
                        "minecraft:magma_cream=10 x64",
                        "minecraft:phantom_membrane=14 x48",
                        "minecraft:ender_pearl=12 x32",
                        "minecraft:blaze_rod=14 x32",
                        "minecraft:breeze_rod=28 x32",
                        "minecraft:ghast_tear=28 x24",
                        "minecraft:echo_shard=45 x24",
                        "minecraft:shulker_shell=65 x16",
                        "minecraft:wither_skeleton_skull=52 x6",
                        "minecraft:heavy_core=380 x2",
                        "minecraft:nether_star=360 x3",
                        "cataclysm:koboleton_bone=18 x48",
                        "cataclysm:coral_chunk=22 x48",
                        "cataclysm:crystallized_coral_fragments=25 x48",
                        "cataclysm:void_shard=28 x48",
                        "cataclysm:amethyst_crab_meat=28 x32",
                        "cataclysm:blessed_amethyst_crab_meat=52 x24",
                        "cataclysm:dying_ember=14 x32",
                        "cataclysm:burning_ashes=64 x24",
                        "cataclysm:void_crystal=58 x24",
                        "cataclysm:black_steel_ingot=72 x24",
                        "cataclysm:ancient_metal_ingot=82 x24",
                        "cataclysm:lacrima=96 x16",
                        "cataclysm:monstrous_horn=125 x12",
                        "cataclysm:abyssal_sacrifice=175 x8",
                        "cataclysm:essence_of_the_storm=210 x8",
                        "cataclysm:enderite_ingot=260 x6",
                        "cataclysm:witherite_ingot=300 x6",
                        "cataclysm:ignitium_ingot=320 x6",
                        "cataclysm:cursium_ingot=320 x6",
                        "twilightforest:raven_feather=12 x64",
                        "twilightforest:torchberries=10 x64",
                        "twilightforest:borer_essence=15 x48",
                        "twilightforest:armor_shard=5 x64",
                        "twilightforest:armor_shard_cluster=48 x24",
                        "twilightforest:knightmetal_ingot=56 x24",
                        "twilightforest:arctic_fur=18 x48",
                        "twilightforest:ironwood_ingot=22 x48",
                        "twilightforest:steeleaf_ingot=28 x48",
                        "twilightforest:naga_scale=38 x32",
                        "twilightforest:carminite=36 x32",
                        "twilightforest:alpha_yeti_fur=58 x24",
                        "twilightforest:fiery_blood=72 x16",
                        "twilightforest:fiery_tears=72 x16",
                        "twilightforest:fiery_ingot=98 x16",
                        "twilightforest:naga_trophy=160 x4",
                        "twilightforest:lich_trophy=190 x4",
                        "twilightforest:minoshroom_trophy=190 x4",
                        "twilightforest:knight_phantom_trophy=200 x4",
                        "twilightforest:alpha_yeti_trophy=190 x4",
                        "twilightforest:snow_queen_trophy=230 x4",
                        "twilightforest:hydra_trophy=240 x4",
                        "twilightforest:ur_ghast_trophy=240 x4",
                        "irons_spellbooks:frozen_bone=12 x48",
                        "irons_spellbooks:hogskin=18 x48",
                        "irons_spellbooks:blood_vial=20 x48",
                        "irons_spellbooks:lightning_bottle=24 x32",
                        "irons_spellbooks:divine_pearl=28 x32",
                        "irons_spellbooks:dragonskin=68 x16",
                        "upgrade_aquatic:thrasher_tooth=30 x32",
                        "takesapillage:ravager_horn=68 x16",
                        "irons_artifice:blackpowder=10 x64",
                        "// ===== 5. 附魔 (enchanting) 与 药水 (brewing) =====",
                        "minecraft:enchanted_book=30 x24",
                        "minecraft:potion=15 x32",
                        "minecraft:splash_potion=19 x32",
                        "minecraft:lingering_potion=28 x24",
                        "minecraft:fermented_spider_eye=7 x48",
                        "minecraft:glistering_melon_slice=14 x48",
                        "minecraft:dragon_breath=36 x24",
                        "// ===== 6. 调酒师 (bartending)：森罗酒馆果酒、维度佳酿与世界名酒（按真实酿造配方成本定价 + 年份上限 1.68x） =====",
                        "kaleidoscope_tavern:sweet_berry_wine=10 x64",
                        "kaleidoscope_tavern:wine=11 x64",
                        "kaleidoscope_tavern:plum_wine=10 x64",
                        "kaleidoscope_tavern:honey_wine=42 x32",
                        "kaleidoscope_tavern:mojito=14 x48",
                        "kaleidoscope_tavern:rum=6 x64",
                        "kaleidoscope_tavern:bloody_mary=32 x32",
                        "kaleidoscope_tavern:sakura_wine=15 x48",
                        "kaleidoscope_tavern:brandy=20 x48",
                        "kaleidoscope_tavern:carignan=15 x48",
                        "kaleidoscope_tavern:depth_charge=24 x32",
                        "kaleidoscope_tavern:ice_wine=17 x48",
                        "kaleidoscope_tavern:champagne=15 x48",
                        "kaleidoscope_tavern:whiskey=7 x64",
                        "kaleidoscope_tavern:vodka=7 x64",
                        "kaleidoscope_world_liquor:sour_plum=10 x64",
                        "kaleidoscope_world_liquor:cola=32 x32",
                        "kaleidoscope_world_liquor:cool_tea=7 x64",
                        "kaleidoscope_world_liquor:bamboo_leaf_green_liquor=10 x64",
                        "kaleidoscope_world_liquor:gin_tonic=49 x32",
                        "kaleidoscope_world_liquor:highball=39 x32",
                        "kaleidoscope_world_liquor:absolut_vodka=8 x64",
                        "kaleidoscope_world_liquor:bacardi_carta_blanca=10 x64",
                        "kaleidoscope_world_liquor:bombay_sapphire_gin=11 x64",
                        "kaleidoscope_world_liquor:long_island_iced_tea=57 x32",
                        "kaleidoscope_world_liquor:around_the_world=39 x32",
                        "kaleidoscope_world_liquor:jack_daniel=13 x64",
                        "kaleidoscope_world_liquor:johnnie_walker=14 x48",
                        "kaleidoscope_world_liquor:dassai=6 x64",
                        "kaleidoscope_world_liquor:maotai=15 x48",
                        "kaleidoscope_world_liquor:lafite_1982=21 x48",
                        "kaleidoscope_tavern:glowflower_brew=15 x48",
                        "kaleidoscope_tavern:godfather=38 x32",
                        "kaleidoscope_tavern:grasshopper=19 x48",
                        "kaleidoscope_tavern:luminous_bride=20 x48",
                        "kaleidoscope_tavern:madame_shexiang=32 x32",
                        "kaleidoscope_tavern:miners_star=31 x32",
                        "kaleidoscope_tavern:molotov=16 x48",
                        "kaleidoscope_tavern:mother_snow=24 x32",
                        "kaleidoscope_tavern:nether_special=34 x32",
                        "kaleidoscope_tavern:polaris_sweet_white=24 x32",
                        "kaleidoscope_tavern:red_queen=8 x64",
                        "kaleidoscope_tavern:riesling_dry_white=27 x32",
                        "kaleidoscope_tavern:sauvignon_blanc_dry_white=22 x48",
                        "kaleidoscope_tavern:screwdriver=40 x32",
                        "kaleidoscope_tavern:sculk_special=39 x32",
                        "kaleidoscope_tavern:sherry=24 x32",
                        "kaleidoscope_tavern:sunset_glow=36 x32",
                        "kaleidoscope_tavern:white_lady=28 x32",
                        "kaleidoscope_world_liquor:jerk=27 x32",
                        "kaleidoscope_world_liquor:kwas_chlebowy=27 x32",
                        "kaleidoscope_world_liquor:pina_colada=10 x64",
                        "kaleidoscope_world_liquor:pine_colada=23 x48",
                        "kaleidoscope_world_liquor:shrimp_cocktail=39 x32",
                        "kaleidoscope_world_liquor:skyy_vodka=8 x64",
                        "kaleidoscope_world_liquor:smirnoff_red_vodka=14 x48",
                        "kaleidoscope_world_liquor:spiryt_vodka=13 x64",
                        "kaleidoscope_world_liquor:strongbow=13 x64",
                        "kaleidoscope_world_liquor:tonic_water=32 x32",
                        "kaleidoscope_dim_wine:abyssal_porter=72 x32",
                        "kaleidoscope_dim_wine:black_bone_lafite=72 x32",
                        "kaleidoscope_dim_wine:blaze_martell=28 x32",
                        "kaleidoscope_dim_wine:budweiser_beer=10 x64",
                        "kaleidoscope_dim_wine:cave_glowbrew=34 x32",
                        "kaleidoscope_dim_wine:chorus_cognac=21 x48",
                        "kaleidoscope_dim_wine:crimson_crescendo=31 x32",
                        "kaleidoscope_dim_wine:dawnlight_dew=43 x32",
                        "kaleidoscope_dim_wine:dead_end_spittle=72 x32",
                        "kaleidoscope_dim_wine:divine_embryo_port=72 x32",
                        "kaleidoscope_dim_wine:divine_offering_brew=22 x48",
                        "kaleidoscope_dim_wine:draconic_blood_wine=57 x32",
                        "kaleidoscope_dim_wine:druids_secret_brew=36 x32",
                        "kaleidoscope_dim_wine:embereye=56 x32",
                        "kaleidoscope_dim_wine:end_starlight=71 x32",
                        "kaleidoscope_dim_wine:ethereal_noble=31 x32",
                        "kaleidoscope_dim_wine:forgotten_margarita=72 x32",
                        "kaleidoscope_dim_wine:frostvein_beastblood=41 x32",
                        "kaleidoscope_dim_wine:giants_hymn=36 x32",
                        "kaleidoscope_dim_wine:gingerbread_warmer=22 x48",
                        "kaleidoscope_dim_wine:goblins_stash=36 x32",
                        "kaleidoscope_dim_wine:gravitite_drunk=22 x48",
                        "kaleidoscope_dim_wine:healing_elixir=25 x32",
                        "kaleidoscope_dim_wine:magma_royal_salute=72 x32",
                        "kaleidoscope_dim_wine:moorglow_birds_song=46 x32",
                        "kaleidoscope_dim_wine:perennial_frost=69 x32",
                        "kaleidoscope_dim_wine:phantom_dream_bubble=43 x32",
                        "kaleidoscope_dim_wine:rangers_satchel=36 x32",
                        "kaleidoscope_dim_wine:silent_sherry=32 x32",
                        "kaleidoscope_dim_wine:snakeskin_liqueur=72 x32",
                        "kaleidoscope_dim_wine:stagcall_monsoon=38 x32",
                        "kaleidoscope_dim_wine:swet_fizz=22 x48",
                        "kaleidoscope_dim_wine:unbound_skyborn=36 x32",
                        "kaleidoscope_dim_wine:wart_hennessy=49 x32",
                        "// ===== 7. 魔导师 (archmage)：铁魔法奥术源质、五阶墨水、法术卷轴、秘药与符文 =====",
                        "irons_spellbooks:arcane_essence=10 x80",
                        "irons_spellbooks:common_ink=3 x96",
                        "irons_spellbooks:uncommon_ink=15 x48",
                        "irons_spellbooks:rare_ink=65 x24",
                        "irons_spellbooks:epic_ink=275 x6",
                        "irons_spellbooks:legendary_ink=680 x1",
                        "irons_spellbooks:cinder_essence=55 x24",
                        "irons_spellbooks:scroll=32 x24",
                        "irons_spellbooks:arcane_ingot=88 x24",
                        "irons_spellbooks:shriving_stone=28 x24",
                        "irons_spellbooks:oakskin_elixir=24 x24",
                        "irons_spellbooks:invisibility_elixir=34 x24",
                        "irons_spellbooks:evasion_elixir=26 x24",
                        "irons_spellbooks:greater_oakskin_elixir=34 x16",
                        "irons_spellbooks:greater_invisibility_elixir=44 x16",
                        "irons_spellbooks:greater_evasion_elixir=56 x16",
                        "irons_spellbooks:greater_healing_potion=30 x24",
                        "irons_spellbooks:fire_rune=135 x8",
                        "irons_spellbooks:ice_rune=135 x8",
                        "irons_spellbooks:lightning_rune=135 x8",
                        "irons_spellbooks:ender_rune=135 x8",
                        "irons_spellbooks:holy_rune=135 x8",
                        "irons_spellbooks:blood_rune=170 x8",
                        "irons_spellbooks:evocation_rune=170 x8",
                        "irons_spellbooks:nature_rune=135 x8",
                        "irons_spellbooks:cooldown_rune=135 x8",
                        "irons_spellbooks:protection_rune=135 x8",
                        "irons_spellbooks:mithril_scrap=55 x12",
                        "irons_spellbooks:mithril_ingot=220 x4",
                        "irons_spellbooks:pyrium_ingot=460 x2",
                        "// ===== 8. 通用基础矿木材料（低限额散人托底；低阶矿物严格低于东篱商业电脑进货折算价防套利） =====",
                        "#minecraft:logs=3 x256",
                        "minecraft:coal=2 x192",
                        "minecraft:copper_ingot=2 x192",
                        "minecraft:redstone=2 x192",
                        "minecraft:quartz=2 x128",
                        "minecraft:lapis_lazuli=2 x128",
                        "minecraft:amethyst_shard=3 x128",
                        "minecraft:iron_ingot=4 x128",
                        "minecraft:clay_ball=2 x256",
                        "minecraft:brick=3 x192",
                        "minecraft:glow_ink_sac=8 x64",
                        "minecraft:obsidian=10 x64",
                        "minecraft:crying_obsidian=20 x48",
                        "minecraft:gold_ingot=14 x48",
                        "minecraft:emerald=20 x32",
                        "minecraft:diamond=70 x16",
                        "minecraft:netherite_scrap=100 x10"),
                        () -> "minecraft:wheat=4 x256", o -> o instanceof String);
        shippingLevelSlots = builder.comment("每个等级的槽位数（列表长度 = 等级数；默认 4 档：9/18/27/36 格）")
                .defineList("shippingLevelSlots", List.of("9", "18", "27", "36"), () -> "9", o -> o instanceof String);
        shippingUpgradeCosts = builder.comment("升级花费（第 1 项 = 1→2 级，依此类推，费用自动回流官方总钱箱）")
                .defineList("shippingUpgradeCosts", List.of("8000", "28000", "75000"), () -> "8000", o -> o instanceof String);
        shippingDailyLimits = builder.comment("每个等级的每日结算金额上限（现实时间 0 点刷新；0 = 不限）")
                .defineList("shippingDailyLimits", List.of("1000", "2200", "4500", "8000"), () -> "1000", o -> o instanceof String);
        shippingSettleTick = builder.comment("每天结算的游戏刻（0 = 早上 06:00；结算窗口见下一项）")
                .defineInRange("shippingSettleTick", 0, 0, 23999);
        shippingSettleWindow = builder.comment("（已不再限制结算：现在到点后第一次 tick 就结算，错过时间也会补结算；保留此项只为兼容旧配置）")
                .defineInRange("shippingSettleWindow", 180, 1, 12000);
        shippingResetTime = builder.comment("每日额度按【现实时间】在几点重置（HH:mm，默认 00:00 = 现实零点）")
                .define("shippingResetTime", "00:00");
        builder.pop();

        builder.push("mall");
        mallRestockEnabled = builder.comment("购物城是否定时补货（Eco 模式下做市货架仅在低于 10% 时微补至 10% 应急底仓，默认开启）")
                .define("mallRestockEnabled", true);
        mallRestockMode = builder.comment("补货时刻按哪种时间算：real = 现实时间，game = 游戏时间（其它值按 real）")
                .define("mallRestockMode", "real");
        mallRestockTime = builder.comment("补货时刻（HH:mm）。real：现实几点（默认 00:00）；game：游戏内时间（06:00 = 游戏 0 刻）")
                .define("mallRestockTime", "00:00");
        mallRefreshMode = builder.comment("购物城库存刷新方式：off=关闭 / real=按现实时间 / game=按游戏时间")
                .define("mallRefreshMode", "off");
        mallRefreshAmount = builder.comment("库存刷新周期：real 按小时、game 按游戏天数（默认 24 小时 / 1 天）")
                .defineInRange("mallRefreshAmount", 24, 1, 100000);
        mallStockMode = builder.comment("购物城库存模式：global = 全服共用一份库存（谁先买谁先得）"
                        + " / personal = 每个玩家各自一份库存（互不影响，刷新或补货时每人恢复）")
                .define("mallStockMode", "global");
        mallRestockAmount = builder.comment("补货数量：0 = 每件商品补回各自原量；>0 = 所有商品统一补到该数量")
                .defineInRange("mallRestockAmount", 0, 0, 1000000);
        builder.pop();

        builder.push("eco");
        ecoEnabled = builder.comment("是否启用阿尔比恩式官方做市商与闭环总钱箱机制").define("enabled", true);
        treasuryInitialReserve = builder.comment("新世界官方总钱箱初始启动储备星币")
                .defineInRange("treasuryInitialReserve", 500_000L, 0L, Long.MAX_VALUE);
        treasuryTargetReserve = builder.comment("官方总钱箱健康水位 1.00x 对应的目标储备星币")
                .defineInRange("treasuryTargetReserve", 500_000L, 1L, Long.MAX_VALUE);
        treasuryMinFloor = builder.comment("官方总钱箱最低保底安全线（低于此值暂停收购与外币兑汇出账，绝不吞物品）")
                .defineInRange("treasuryMinFloor", 20_000L, 0L, Long.MAX_VALUE);
        treasuryMinFactor = builder.comment("总钱箱见底时对收购价的最低阻尼倍率")
                .defineInRange("treasuryMinFactor", 0.50D, 0.10D, 1.00D);
        treasuryMaxFactor = builder.comment("总钱箱充裕时对收购价的最高上浮倍率")
                .defineInRange("treasuryMaxFactor", 1.10D, 1.00D, 2.00D);
        ecoAskFloor = builder.comment("购物城做市商品库存充裕时的售价下限倍率（相对基准价）")
                .defineInRange("askFloor", 0.75D, 0.10D, 0.99D);
        ecoAskCeiling = builder.comment("购物城做市商品断货边缘时的售价上限倍率（相对基准价）")
                .defineInRange("askCeiling", 1.85D, 1.05D, 5.00D);
        ecoBidFloor = builder.comment("官方收购在接近满仓前的最低收购倍率（满仓后直接停收）")
                .defineInRange("bidFloor", 0.35D, 0.05D, 0.99D);
        ecoBidCeiling = builder.comment("官方收购在零库存断货时的基础悬赏倍率")
                .defineInRange("bidCeiling", 1.35D, 1.00D, 3.00D);
        ecoMaxBidToAskRatio = builder.comment("防倒买倒卖硬锁红线：单件最高收购到手价永远不超过同商品商城入库后售价的该比例（默认 0.82）")
                .defineInRange("maxBidToAskRatio", 0.82D, 0.10D, 0.98D);
        ecoShippingToMallRatio = builder.comment("出货箱成交物品转入购物城做市库存的比例（默认 1.0 = 100% 实业供货）")
                .defineInRange("shippingToMallRatio", 1.00D, 0.00D, 1.00D);
        ecoBuybackToMallRatio = builder.comment("每日收购成交物品转入购物城做市库存的比例（默认 0.30，其余 70% 直接销毁泄洪）")
                .defineInRange("buybackToMallRatio", 0.30D, 0.00D, 1.00D);
        blackMarketDefaultCap = builder.comment("未在购物城上架的纯收购物品默认目标库存基数（达到 fullRatio 倍时满仓停收）")
                .defineInRange("blackMarketDefaultCap", 256, 8, 1_000_000);
        blackMarketWarehouseFullRatio = builder.comment("官方仓库满仓停收阈值倍数（例如 2.0 表示库存达到目标容量 200% 时直接停收）")
                .defineInRange("blackMarketWarehouseFullRatio", 2.00D, 1.00D, 10.00D);
        blackMarketDailyDecayRatio = builder.comment("官方黑市仓库每日自然消化库存比例（默认每天消耗 20%）")
                .defineInRange("blackMarketDailyDecayRatio", 0.20D, 0.00D, 1.00D);
        blackMarketScarcityDailyStep = builder.comment("某物品全天 0 库存断供时，次日自动增加的缺货求购加价幅度（默认每天 +5%）")
                .defineInRange("blackMarketScarcityDailyStep", 0.05D, 0.00D, 0.50D);
        blackMarketScarcityMaxBonus = builder.comment("长期缺货累积求购加价的最高上限（默认 +40%）")
                .defineInRange("blackMarketScarcityMaxBonus", 0.40D, 0.00D, 2.00D);
        ecoTreasurySinkReasonPrefixes = builder.comment("外部附属调用 EconomyApi.take 时视为系统净消耗并自动回流总钱箱的 reason 关键字（不匹配玩家炒股本金和发红包）")
                .defineList("treasurySinkReasonPrefixes",
                        List.of("lottery", "抽卡", "祈愿", "blind_tax", "遗迹盲仓门票", "印花税", "stock_fee", "星途纪行"),
                        () -> "lottery", o -> o instanceof String);
        ecoTreasuryRefundReasonPrefixes = builder.comment("外部附属调用 EconomyApi.add 时需从总钱箱冲正退回的 reason 关键字（如盲仓人数不足退票）")
                .defineList("treasuryRefundReasonPrefixes",
                        List.of("退回门票", "blind_refund"),
                        () -> "退回门票", o -> o instanceof String);
        builder.pop();

        builder.push("eco_exchange");
        exchangeAllowWithdraw = builder.comment("是否允许用星币逆向取款换回外部模组实体钱币（默认 false = 单向结汇防套利）")
                .define("exchangeAllowWithdraw", false);
        playerDailyExchangeCap = builder.comment("单名玩家每天通过外部实体钱币（如东篱五元纸币）兑换星币的基础上限（0 = 不限）")
                .defineInRange("playerDailyExchangeCap", 1_200L, 0L, Long.MAX_VALUE);
        globalDailyFxCap = builder.comment("全服每天用于外部实体钱币结汇的星币总额上限（0 = 不限）")
                .defineInRange("globalDailyFxCap", 18_000L, 0L, Long.MAX_VALUE);
        treasuryDailyFxShare = builder.comment("全服每日外币结汇池最多动用当前官方总钱箱余额的比例（默认 0.12 = 12%）")
                .defineInRange("treasuryDailyFxShare", 0.12D, 0.01D, 1.00D);
        builder.pop();

        builder.push("eco_professions");
        professionsEnabled = builder.comment("是否启用内置职业与每日专注点系统（详细职业与厨具绑定见 config/module_shop_eco/professions.json）")
                .define("professionsEnabled", true);
        dailyFocusRestore = builder.comment("每名玩家每天自动恢复的专注点数量")
                .defineInRange("dailyFocusRestore", 800, 0, 1_000_000);
        maxFocusCap = builder.comment("玩家专注点可累积的上限（默认 2400 = 最多攒 3 天护肝）")
                .defineInRange("maxFocusCap", 2400, 100, 10_000_000);
        baseFocusCostPerItem = builder.comment("Lv.1 新手精销单件本职物品的基础专注点消耗（随职业等级提升最高可省 75% 专注）")
                .defineInRange("baseFocusCostPerItem", 25.0D, 0.5D, 10_000.0D);
        maxProfessionPriceBonus = builder.comment("Lv.100 满级职业开启专注精销时的最高额外加价比例（默认 0.60 = +60%）")
                .defineInRange("maxProfessionPriceBonus", 0.60D, 0.0D, 5.0D);
        scarcityBonusMax = builder.comment("全服冷门紧缺职业自动享有的最高额外悬赏比例（默认 0.20 = +20%，热门职业保底 1.00x 不扣钱）")
                .defineInRange("scarcityBonusMax", 0.20D, 0.0D, 2.0D);
        qualityBonusEnabled = builder.comment("是否启用模组物品原生品质溢价与糊菜/黑暗料理拒收检测")
                .define("qualityBonusEnabled", true);
        combatGrinderKillThreshold = builder.comment("战斗职业防刷怪塔阈值：同一 3x3 区块每分钟击杀超过多少只普通怪后开始经验衰减")
                .defineInRange("combatGrinderKillThreshold", 25, 5, 10_000);
        builder.pop();

        // 同 IP 多账号登录限制已按需求移除：同一个 IP 可以正常登录多个账号，不再踢人。
    }

    // =========================================================== 出货箱：配置读取

    /** 出货箱等级数（至少 1 级）。 */
    public static int shippingLevels() {
        return Math.max(1, INSTANCE.shippingLevelSlots.get().size());
    }

    /** 第 level 级（1 起）的槽位数（超出列表长度时每升 1 级平滑递增 9 格直至 54 格上限）。 */
    public static int shippingSlotsFor(int level) {
        List<? extends String> list = INSTANCE.shippingLevelSlots.get();
        if (list.isEmpty()) {
            return Math.max(9, Math.min(54, Math.max(1, level) * 9));
        }
        int lv = Math.max(1, level);
        if (lv <= list.size()) {
            return Math.max(1, Math.min(54, parseIntSafe(list.get(lv - 1), 9)));
        }
        int lastSlots = Math.max(1, parseIntSafe(list.get(list.size() - 1), 27));
        int extraLevels = lv - list.size();
        return Math.max(1, Math.min(54, lastSlots + extraLevels * 9));
    }

    /** 从 level 升到 level+1 的花费（仅星币视角兼容）；已满级返回 -1。 */
    public static long shippingUpgradeCost(int level) {
        cn.autoforged.module_shop.data.PriceSpec spec = shippingUpgradeSpec(level);
        if (spec == null) {
            return -1L;
        }
        return spec.supportsStar() ? spec.starPrice : 0L;
    }

    public static cn.autoforged.module_shop.data.PriceSpec shippingUpgradeSpec(int level) {
        List<cn.autoforged.module_shop.data.PriceSpec> list = shippingUpgradeSpecList();
        int index = level - 1;
        if (index < 0 || index >= list.size()) {
            return null;
        }
        return list.get(index);
    }

    public static List<cn.autoforged.module_shop.data.PriceSpec> dailyRefreshSpecList() {
        return parseSpecList(INSTANCE.dailyRefreshCosts.get());
    }

    public static List<cn.autoforged.module_shop.data.PriceSpec> buybackRefreshSpecList() {
        return parseSpecList(INSTANCE.buybackRefreshCosts.get());
    }

    public static List<cn.autoforged.module_shop.data.PriceSpec> shippingUpgradeSpecList() {
        return parseSpecList(INSTANCE.shippingUpgradeCosts.get());
    }

    public static List<cn.autoforged.module_shop.data.PriceSpec> parseSpecList(List<? extends String> raw) {
        List<cn.autoforged.module_shop.data.PriceSpec> out = new ArrayList<>();
        if (raw == null) {
            return out;
        }
        for (String entry : raw) {
            cn.autoforged.module_shop.data.PriceSpec parsed = cn.autoforged.module_shop.data.PriceSpec.parseRuleText(entry);
            out.add(parsed == null ? cn.autoforged.module_shop.data.PriceSpec.ZERO : parsed);
        }
        return out;
    }

    public static long buybackAllBonus() {
        return Math.max(0L, INSTANCE.buybackAllBonus.get());
    }

    /** 第 level 级的每日结算上限（0 = 不限 → 返回 Long.MAX_VALUE）。 */
    public static long shippingDailyLimit(int level) {
        List<? extends String> list = INSTANCE.shippingDailyLimits.get();
        if (list.isEmpty()) {
            return Long.MAX_VALUE;
        }
        int index = Math.max(0, Math.min(list.size() - 1, level - 1));
        long value = parseLongSafe(list.get(index), 0L);
        return value <= 0L ? Long.MAX_VALUE : value;
    }

    private static int parseIntSafe(String text, int fallback) {
        try {
            return Integer.parseInt(text == null ? "" : text.trim());
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private static long parseLongSafe(String text, long fallback) {
        try {
            return Long.parseLong(text == null ? "" : text.trim());
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    // =========================================================== 时间格式（HH:mm）

    /** 「HH:mm」→ 一天中的分钟数；格式不对返回 -1。 */
    public static int parseClock(String text) {
        if (text == null) {
            return -1;
        }
        String value = text.trim();
        int colon = value.indexOf(':');
        if (colon <= 0 || colon >= value.length() - 1) {
            return -1;
        }
        try {
            int hour = Integer.parseInt(value.substring(0, colon).trim());
            int minute = Integer.parseInt(value.substring(colon + 1).trim());
            if (hour < 0 || hour > 23 || minute < 0 || minute > 59) {
                return -1;
            }
            return hour * 60 + minute;
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    /** 「HH:mm」→ 游戏刻（0 = 早上 06:00，1000 刻 = 1 小时）；格式不对返回 -1。 */
    public static int clockToDayTick(String text) {
        int minutes = parseClock(text);
        if (minutes < 0) {
            return -1;
        }
        // 游戏内 0 刻 = 06:00，所以要把「06:00」当零点重新折算
        int shifted = (minutes - 6 * 60 + 24 * 60) % (24 * 60);
        return (int) ((long) shifted * 1000L / 60L);
    }

    /** 游戏刻 → 「HH:mm」（用于把配置里的结算时刻显示成时间）。 */
    public static String dayTickToClock(int tick) {
        int minutes = Math.floorMod((int) (tick * 60L / 1000L) + 6 * 60, 24 * 60);
        return String.format("%02d:%02d", minutes / 60, minutes % 60);
    }

    /** 每日额度的现实重置时间（分钟数；配置格式不对时按 0 点处理）。 */
    public static int shippingResetMinutes() {
        int minutes = parseClock(INSTANCE.shippingResetTime.get());
        return Math.max(0, minutes);
    }

    /** 每日额度归属的「现实日期」：以重置时间为分界（例如重置时间 04:00，则凌晨 3 点仍算前一天）。 */
    public static long shippingQuotaDay() {
        return realDayWithReset(INSTANCE.shippingResetTime.get());
    }

    /** 以某个「HH:mm」为分界的现实日期（epochDay）：凌晨过了分界点才算新的一天。 */
    public static long realDayWithReset(String clock) {
        int minutes = Math.max(0, parseClock(clock));
        return java.time.LocalDateTime.now().minusMinutes(minutes).toLocalDate().toEpochDay();
    }

    // =========================================================== 购物城：定时补货

    /** 购物城是否开启定时补货。 */    public static boolean mallRestockEnabled() {
        return INSTANCE.mallRestockEnabled.get();
    }

    /** 补货时刻是否按**游戏时间**算（false = 现实时间）。 */
    public static boolean mallRestockGameTime() {
        String mode = INSTANCE.mallRestockMode.get();
        return mode != null && "game".equalsIgnoreCase(mode.trim());
    }

    /** 补货时刻（HH:mm，配置格式不对时按 00:00 处理）。 */
    public static String mallRestockTime() {
        String text = INSTANCE.mallRestockTime.get();
        return parseClock(text) < 0 ? "00:00" : text.trim();
    }

    /** 补货时刻对应的游戏刻（游戏时间口径用；0 = 早上 06:00）。 */
    public static int mallRestockTick() {
        return Math.max(0, clockToDayTick(mallRestockTime()));
    }

    /** 补货数量：0 = 补回各自原量；>0 = 统一补到该数量。 */
    public static int mallRestockAmount() {
        return Math.max(0, INSTANCE.mallRestockAmount.get());
    }

    /** 购物城补货归属的「现实日期」（现实时间口径）。 */
    public static long mallRestockRealDay() {
        return realDayWithReset(mallRestockTime());
    }

    /**
     * 生效的余额上限：配置里 {@code <= 0} 表示**不限**（返回 {@link Long#MAX_VALUE}）。
     * <p>
     * 所有涉及上限的地方都要走这里，否则「0 = 不限」会在入账 / 限价 / 发放等路径上被当成「一分钱都装不下」。
     * <p>
     * ⚠ {@code 1} 也按「不限」处理：早期版本的配置范围下限误写成 {@code 1L}，
     * NeoForge 会把配置文件里的 {@code maxBalance = 0}（不限）**纠正成 1 并写回文件**，
     * 于是"不限"变成"上限 1 星币"，管理员连 5 星币的商品都存不下来（报"价格无效"）。
     * 下限已改为 {@code 0L}，这里再兜一层，保证旧配置文件同样能正常工作。
     */
    public static long maxBalanceLimit() {
        long value = INSTANCE.maxBalance.get();
        return value <= 1L ? Long.MAX_VALUE : value;
    }

    /** 下发给客户端的余额上限：不限时下发 0（客户端遇到 0 就不显示「已满」）。 */
    public static long maxBalanceForClient() {
        long value = maxBalanceLimit();
        return value == Long.MAX_VALUE ? 0L : value;
    }

    // =========================================================== 购物城：库存周期刷新

    /** 库存刷新方式（归一化：off / real / game）。 */
    public static String mallRefreshMode() {
        String mode = INSTANCE.mallRefreshMode.get();
        if (mode == null) {
            return "off";
        }
        String clean = mode.trim().toLowerCase(java.util.Locale.ROOT);
        if ("real".equals(clean) || "game".equals(clean)) {
            return clean;
        }
        return "off";
    }

    public static int mallRefreshAmount() {
        return Math.max(1, INSTANCE.mallRefreshAmount.get());
    }

    public static boolean mallRefreshActive() {
        return !"off".equals(mallRefreshMode());
    }

    /** 购物城库存是不是「个人库存」（每个玩家各自一份；全服共用时返回 false）。 */
    public static boolean mallStockPersonal() {
        String mode = INSTANCE.mallStockMode.get();
        if (mode == null) {
            return false;
        }
        return "personal".equals(mode.trim().toLowerCase(java.util.Locale.ROOT));
    }

    /** 现实时间周期（毫秒）。 */
    public static long mallRefreshRealMillis() {
        return mallRefreshAmount() * 3_600_000L;
    }

    /** 游戏时间周期（刻；24000 刻 = 1 天）。 */
    public static long mallRefreshGameTicks() {
        return mallRefreshAmount() * 24_000L;
    }

    // =========================================================== 玩家求购（悬赏）

    public static boolean demandEnabled() {
        return INSTANCE.demandEnabled.get();
    }

    public static int demandSlots() {
        return Math.max(1, INSTANCE.demandSlots.get());
    }

    public static double demandFeePercent() {
        return Math.max(0.0D, Math.min(100.0D, INSTANCE.demandFeePercent.get()));
    }

    public static long demandMaxReward() {
        return Math.max(0L, INSTANCE.demandMaxReward.get());
    }

    public static int demandMaxListed() {
        return Math.max(1, INSTANCE.demandMaxListed.get());
    }

    /** 求购自动下架方式（归一化：{@code permanent} / {@code game} / {@code real}）。 */
    public static String demandExpireMode() {
        String mode = INSTANCE.demandExpireMode.get();
        if (mode == null) {
            return "permanent";
        }
        String clean = mode.trim().toLowerCase(java.util.Locale.ROOT);
        if ("game".equals(clean) || "real".equals(clean)) {
            return clean;
        }
        return "permanent";
    }

    public static int demandExpireAmount() {
        return Math.max(0, INSTANCE.demandExpireAmount.get());
    }

    /** 是否真的启用自动下架（永久 或 0 时长 = 不启用）。 */
    public static boolean demandExpireActive() {
        return !"permanent".equals(demandExpireMode()) && demandExpireAmount() > 0;
    }

    public static long demandExpireGameTicks() {
        return demandExpireAmount() * 24_000L;
    }

    public static long demandExpireRealMillis() {
        return demandExpireAmount() * 3_600_000L;
    }

    // =========================================================== 玩家摊位：自动下架

    /** 摊位自动下架方式（归一化后只会是 {@code permanent} / {@code game} / {@code real}）。 */
    public static String stallExpireMode() {
        String mode = INSTANCE.stallExpireMode.get();
        if (mode == null) {
            return "permanent";
        }
        String clean = mode.trim().toLowerCase(java.util.Locale.ROOT);
        if ("game".equals(clean) || "real".equals(clean)) {
            return clean;
        }
        return "permanent";
    }

    /** 限时时长原值（game 按天、real 按小时；0 也当永久）。 */
    public static int stallExpireAmount() {
        return Math.max(0, INSTANCE.stallExpireAmount.get());
    }

    /** 实际是否启用自动下架（永久 或 0 时长 = 不启用）。 */
    public static boolean stallExpireActive() {
        return !"permanent".equals(stallExpireMode()) && stallExpireAmount() > 0;
    }

    /** 限时长度 → **游戏刻**（game 模式用；24000 刻 = 1 天）。 */
    public static long stallExpireGameTicks() {
        return stallExpireAmount() * 24_000L;
    }

    /** 限时长度 → **现实毫秒**（real 模式用；3600_000 ms = 1 小时）。 */
    public static long stallExpireRealMillis() {
        return stallExpireAmount() * 3_600_000L;
    }

    public static double stallSaleFeePercent() {
        return Math.max(0.0D, Math.min(100.0D, INSTANCE.stallSaleFeePercent.get()));
    }

    /** 判断外部 API 扣款原因是否属于系统净消耗（需注入官方总钱箱）。 */
    public static boolean isTreasurySinkReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return false;
        }
        String lower = reason.trim().toLowerCase(java.util.Locale.ROOT);
        for (String prefix : INSTANCE.ecoTreasurySinkReasonPrefixes.get()) {
            if (prefix != null && !prefix.isBlank()) {
                String p = prefix.trim().toLowerCase(java.util.Locale.ROOT);
                if (lower.startsWith(p) || lower.contains(p)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** 判断外部 API 退款原因是否需要从官方总钱箱冲正（如盲仓流局退回门票）。 */
    public static boolean isTreasuryRefundReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return false;
        }
        String lower = reason.trim().toLowerCase(java.util.Locale.ROOT);
        for (String key : INSTANCE.ecoTreasuryRefundReasonPrefixes.get()) {
            if (key != null && !key.isBlank() && lower.contains(key.trim().toLowerCase(java.util.Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }
}
