# 变更记录

遵循 [语义化版本](https://semver.org/lang/zh-CN/)。模组版本与 API 版本独立维护（当前模组 `2.5.0`，公共 API `1.4.0`）。

## [2.5.1]

构建产物：`Module-Shop-Eco-2.5.1-for-NeoForge-1.21.1.jar` 与 `module_shop-2.5.1-api.jar`；PATCH 修复，对齐上游 `Ar-Zhe/Module-Shop` `1.1.2`，旧存档 / 配置直接兼容，保留历史全部版本 jar。

### 修复与对齐

- **合入上游 `1.1.2` 物品名 PUA 图标字符清洗**：`Text.sanitize()` 统一丢弃私有使用区（PUA）/ 变体选择符 / 零宽字符，并新增 `ItemNames.display()` 覆盖每日商店、每日收购、购物城、拍卖行、玩家摊位、求购、货币兑换、交易与弹窗，解决食物 / 营养模组物品名后的「□□」方块字符。
- **合入上游 `1.1.2` 配置管理入口收口**：移除商店设置底部按钮行重复的「配置管理」按钮，右上角「配置管理」统一经管理锁校验并走 `cfg_open` 链路（保留服务端 `cfg_open_ui` 兜底）。

## [2.5.0]

构建产物：`Module-Shop-Eco-2.5.0-for-NeoForge-1.21.1.jar` 与 `module_shop_eco-2.5.0-api.jar`；向后兼容旧存档、旧模板与全部已编译附属模组（公共 `AddonPage` 仅增 `default` 方法，保持 100% 二进制兼容），保留历史全部版本 jar。

### 修复与新增特性

1. **宿主与附属页面键盘输入 / 数字键 `1~9` 切页冲突修复**：
   - `AddonPage`（公共 API）与 `ShopPage` 新增 `default` 方法 `keyPressed` / `keyReleased` / `charTyped` / `isInputFocused`（与标准版 `Module-Shop 1.6.0` 签名 100% 对齐）；
   - `AddonPageAdapter` 完整透传上述事件；`ShopScreen` 在 `EditBox` 聚焦、页面消费按键或 `activePage().isInputFocused()` 为真时绝不再触发数字键 `1~9` 切页，同时补齐 `charTyped` 与 `keyReleased` 转发。
2. **购物城单品「每人永久限购（不随商城刷新补货）」**：
   - `MallItem` 新增 `permanentLimit` 字段与持久化（同步纳入 `ShopTemplate` 导出/导入）；管理端新增/批量添加/单品编辑界面可直接开启「每人永久限购（不随刷新重置）」，默认限购 1 次或沿用设定次数；
   - `ClearingHouse.restockMallEco` 与 `ShopManager.adminMallRefresh` 切换库存模式时绝不重置带该标记的商品；`MallPage` 表格与卡片视图清晰展示「永久限购 已购 X/N」与售罄置灰态。
3. **货币兑换上限游戏内配置（星币兑换 + 实体货币兑换双覆盖）**：
   - `ExchangeEntry` 新增单品每日兑换上限 `dailyLimit`（0 或负数 = 不限），持久化在存档与模板中；`ExchangePage` 管理端新增「上限设置」入口直接编辑单人日星币上限、全服日结汇上限与双向取款开关，并在界面实时展示剩余可兑额度；
   - `PhysicalCurrencyConfig` / `PhysicalCurrencyConfigScreen` / `PhysicalCurrencyExchangeScreen` 同步新增单人每日实体货币兑换面值上限（0 = 不限）与服务端超限拦截。
4. **职业专精可视化配置管理界面（`ProfessionConfigScreen`）**：
   - `ProfessionConfig` 新增最大等级（2~500）、每级升级所需经验表 `levelXpTable`、每级加价限制表 `levelBonusCaps` 及专注点参数，并由 `PlayerProfessionProfile` 与 `EcoPricing` 实时读取；
   - 管理员可在 `ProfessionPage` 与 `AdminPage` 打开「专精配置」界面直观逐级修改每级经验与加价限制（支持按公式重算全表与线性填充），保存后立即落盘 `professions.json` + `Config` 并热同步全服。
5. **出货箱突破 3 级固定上限，完整支持 4~5+ 任意等级与每日额度**：
   - 新增 `ShopSavedData.shippingMaxLevel()` 动态取升级表与槽位表上限；管理员在 `SystemCostsAdminScreen` 增删出货箱升级档位时，自动同步扩展 `shippingUpgradeCosts`、`shippingLevelSlots`（按 +9 格递增至 54 格）与 `shippingDailyLimits` 并落盘；
   - 解除 `adminShippingLimits` 硬编码循环上限，并支持 `ShopInputScreen` 动态渲染 `limit1..limitN`「等级 N 每日额度」标签，修改后即时广播同步全服。

## [2.4.1]

构建产物：`Module-Shop-Eco-2.4.1-for-NeoForge-1.21.1.jar` 与 `module_shop_eco-2.4.1-api.jar`（同步主仓 `1.5.1`；Eco 总钱箱 / 黑市 / 职业链路未改动）；PATCH 修复，旧存档 / 配置直接兼容，保留历史全部版本 jar。

### 修复

- **实体货币 + 星币并行时无法用星币发布求购**：「发布求购」界面（`DemandCreateScreen`）把点击转发给悬赏编辑器 `PriceSpecEditor` 后，又调用了 `setFocused(null)`，把刚交给「星币价格」输入框的 Screen 焦点立刻清空（连带 `EditBox.setFocused(false)`），键盘 / 字符事件再也送不进输入框，星币悬赏永远停在默认 `0`；而实体币数量默认就是 `1`，所以看起来「只能用实体货币求购」。现已移除该清空，只让搜索框 / 数量框失焦，星币框可正常点击、退格、输入。
- `PriceSpecEditor`：再次点击已选中的账本按钮（如默认的「仅星币」）时，直接把焦点交给对应输入框，便于立刻修改金额。
- 后端 `demandCreate` / `demandFulfill` / `demandCancel` 复核：`STAR_ONLY` 悬赏在并行模式下按星币账本校验余额、托管（含手续费）、交付打款、撤单退款，全链路无需改动；新增单测 `UpgradeRegressionTest.parallelCurrencyStarOnlyDemandEscrowUsesStarLedgerOnly` 与根因回归守卫 `demandCreateScreenKeepsPriceEditorFieldFocusAfterClick`。

## [2.4.0]

公共 API 仍为 `1.4.0`，旧存档、旧模板（`.json`）与现有 `module_shop_eco` 配置目录均可平滑兼容升级。本次同步合入主仓 `1.5.0` 全部 12 项重构、安全加固与 UX 优化，并针对 Eco 职业/总钱箱/黑市做市链路完成适配与加固：

### 整合包/配置分发与资源存储
* **多格式配置导出与 `.mshop` 打包**：`ShopTemplate` / `ShopTemplateStore` / `TemplateTransferManager` 与 `ConfigExportScreen` 支持 `.json`、`.json.gz`（GZIP 压缩 JSON）与 `.mshop`（含媒体资源的 ZIP 包）三种格式，并兼容读取 `1.20.1` 的 `tag_snbt` 与完整 `DataComponentPatch`（`components_snbt`）；支持按模块勾选导出范围（商店/收购/购物城/兑换/出货箱/签到/货币/实体货币/公告/布局/管理员密码锁哈希）；`/shop eco apply_preset` 同步支持自动解析 `.mshop`/`.json.gz`/`.json` 默认模板。
* **共用媒体存储与客户端磁盘缓存**：新增 `ShopMediaStore`（纯服务端字节头校验 PNG/JPEG/WebP，限制 `<= 1MB` / `<= 2048x2048`，SHA-256 原子落盘至 `config/module_shop_eco/media/`）与 `ClientMediaCache` / `ClientImageUploader`（Eco 独立客户端缓存目录 `cache/module_shop_eco/media/`，支持上传进度反馈与断网/分片重试），全程无头服务端安全零 Skija/AWT 依赖。
* **公告图文混排与品牌自定义**：新增 `AnnouncementBlock` 与 `AnnouncementBlockRenderer`（单条最多 40 个图文混排块，向后兼容旧版纯 `content`）；新增 `BrandSettingsScreen` 支持自定义商店标题与上传/更换品牌图标，自动持久化至 `SidebarLayout` 与 `config/module_shop_eco/brand_default.json` 并在全服实时热同步。

### 管理安全收口与数据精确校验
* **管理员密码锁（`PBKDF2WithHmacSHA256`）**：新增 `AdminAuthService`、`ClientAdminAuth`、`AdminAuthScreen` 与 `AdminSecurityScreen`，支持 120,000 次迭代哈希、`MessageDigest.isEqual` 常量时间校验、5 次错误指数冷却与滑动会话超时；全面收口 `ShopManager`（含 `GrantManager` / `AuctionService`）、`/shop admin` 与 `/shop eco` 管理子命令，控制台/RCON/命令方块/KubeJS 自动免密放行，新增 `/shop auth` 与控制台 `/shop auth status|disable`。
* **出货箱规则每日限额与 TACZ 判重修复**：`ShippingRules` 支持可选单日上限 `daily_max` 与服务端实时 `shipRuleUsed` 状态回推；`ShopManager.findOffer` / `adminMallImport` / `TemplateService` 切换为 `ItemStack.isSameItemSameComponents` 精准组件判重，修复 TACZ 枪械等带组件物品跨模板/批量导入互相覆盖的问题；同步保留 Eco `Offer.professionId` 与 `MallEntry` 做市字段（`base_price`、`supply_mode`、`floor_restock_pct`）。

### 全界面的滚动条交互、排版保护与检索体验
* **交互式滚动条拖拽与防截断排版**：`ScrollController` 全面升级为可见滑块与轨道按比例点击直达+即时拖拽（`THUMB_MIN=18`、`HOT_ZONE=10`、`pressBar` / `dragBar` / `releaseBar`），覆盖 `ListTable`、`CardGrid`、`ShellV2` 侧栏、全部内容页及公告/出货箱/配置/兑换码/玩家选择等弹窗；优化 `MallImportScreen`、`AvatarScreen`、`BillScreen`、`ShippingBinScreen`、`ConfigManagerScreen` 与 Eco 专属 `ProfessionPage` 的列宽动态让位、稀缺标签测宽与滚动条右侧避让余量。
* **可清空多词检索与 Regression Test**：全内容页（含 `ProfessionPage`）及二级弹窗接入 `.clearable(true)` 搜索框（`PageKit.matchesQuery` / `matchesItemQuery` 多词 AND 检索 + `gui.module_shop.search.empty` 空状态提示）；补充 `UpgradeRegressionTest` 全量回归测试。

## [2.3.0]

公共 API 仍为 `1.4.0`，旧存档、旧版 `physical_currency.json`（v1）与模板均可平滑兼容迁移；`physical_currency.json` 升级至 v2 格式并在首次保存后保留各阶稳定 `id`。

### 双独立并行账本（星币 ‖ 实体货币）全链路重构
* **星币与实体货币彻底分离**：移除旧版虚实混扣、`spendableBalance` 混算与 `SettleMode`，星币账本仅读写账户星币，实体货币账本仅在阶梯货币链内部进行破整找零与发放，两者绝不相互折抵或自动互转；`EcoPricing` 与官网总钱箱严格只作用于星币部分，`PHYS_ONLY` 交易与实体通道完全旁路市场做市锚点。
* **通用 `PriceSpec` 与全链路接入**：支持 `STAR_ONLY`（仅星币）、`PHYS_ONLY`（仅实体币）、`EITHER`（二选一双通道 `|`）与 `BOTH`（组合同付 `+`）；全面接入每日商店、购物城、每日收购、出货箱（含双账本预计收益、规则语法 `phys:<tier>*<n>` / `|` / `+` 与每日刷新/升级费）、玩家摊位（纯单账本挂单与同币种税率）、拍卖行（支持按任意阶实体货币起拍/加价/一口价/托管与同档手续费）、玩家求购、管理员发放、每日签到/补签/里程与活动日及兑换码。

### OP 四态配价与玩家市场实体币直显界面
* **OP 全场景可视化配价与系统费用台**：新增通用配价组件 `PriceSpecEditor`（OP 四态模式 + 实体币图标横条先选档位再填数量）与系统定额费用弹窗 `SystemCostsAdminScreen`（可视化配置每日商店刷新费、每日收购刷新费、出货箱升级费）；升级 `ShopInputScreen`、`GrantScreen`、`ScheduleRewardItemsScreen`、`RedeemRewardEditorScreen`、`MallImportScreen` 与 `BatchAddScreen`。
* **自由市场与全页面实体币直显**：`ShellV2` 顶栏/个人卡与 `ProfileHomePage` 并列直显双独立账本并支持 `EITHER` 快捷默认通道切换；`PriceDisplay` 在全部内容页与摊位/拍卖/求购弹窗中以无遮挡硬币图标 + 数量渲染双账本价格并修复 `EITHER` 可买性过滤与 `AtomChat` 打开主界面 / ESC 直退问题；同步完善中英双语词条。

## [2.2.1]

公共 API 仍为 `1.4.0`，旧存档与配置文件均可直接兼容升级。

### 物理端隔离加固
* **彻底解耦 `CurrencySettlement` 客户端钩子**：移除 `shop/currency/CurrencySettlement.java` 中的静态 `ClientCurrencyBridge` 内部类与所有 `client.*` 类引用，改为 `setClientBlockedHook(Predicate<ItemStack>)` 注册接口并由客户端 `ClientEvents` 注入 `ClientHomeCurrencyState` 判别逻辑，保证服务端（Dedicated Server）非 `client/` 包源码对客户端类零引用。

## [2.2.0]

公共 API 仍为 `1.4.0`，旧存档与配置文件均可直接兼容升级（`physical_currency.json` 默认关闭、不破坏既有经济）。

### 服务器个人主页 & 公告热更新
* **默认展示个人主页**：每次按 `[K]` 键或输入 `/shop` 打开商店默认落入「个人主页（`home`）」页签；保留左侧功能栏位并融入 3D 玩家全身模型跟随鼠标、玩家名片、LuckPerms 称号徽章（纯运行时反射软联动）、服务端在线时长、星币/实体货币资产总览以及原地显示连签天数的快速签到按钮。
* **服务器公告轮播与管理热更新**：新增 `announcements.json`（置顶优先 + `order` 升序），主页右栏支持置顶标签、6 秒自动轮播与悬停暂停；管理员可通过管理界面（`AnnouncementAdminScreen`）在线增删改并热更新同步至全服在线玩家。

### 管理可自定义分层实体货币系统（默认关闭）
* **阶梯面值链配置**：新增 `physical_currency.json`（默认 `enabled = false`），预设「1 泥土币 → 10 铁币 → 100 金币 → 1000 钻石币 → 5000 下界合金币」并支持管理端（`PhysicalCurrencyConfigScreen`）可视化增删、物品挑选、保留组件/NBT 校验与热保存；支持「优先实体混付」「纯实体货币」「优先虚拟混付」三种模式。
* **全链路破整找零与防刷闭环**：统一接入商店购买、每日收购、出货箱、玩家摊位、拍卖托管、求购悬赏、面对面交易、货币兑换与每日签到/补签/活动日；支持自动破整精确找零、背包满邮箱兜底、个人主页阶梯凑整/拆零互换（`PhysicalCurrencyExchangeScreen`），并拦截低价买币套利、收购/出货箱刷钱及拍卖/求购实体托管洗钱。

### 字体大小优化与全局字号调节
* **全局可调字号（`85% ~ 135%`）**：顶栏新增字号调节菜单（`UIFontScale`），`ShellV2`、个人主页与管理弹窗均支持随字号与最小逻辑窗口（`560×320`）自适应缩放防重叠。

## [2.1.0]

公共 API 仍为 `1.4.0`，旧存档、旧版 `exports/*.json` 与 `shop_config.json` 可直接兼容升级。

### 配置管理重做与体验优化

* **统一 AtomLib UI（含非 Skia 兜底）**：重构「配置管理」界面（`ConfigManagerScreen`）、「导入预览」二级确认界面（`TemplatePreviewScreen`），并新增「导出配置命名弹窗」（`ConfigExportScreen`）；统一仅保留「导入 / 导出 / 返回」三个按钮（移除列表删除项），点击「返回」或按 ESC 逐级回到上一层商店界面。
* **修复二级确认弹窗不弹出 bug**：修正 `ClientTemplateIO.showPreview` 界面类型判断失配问题，并解除 `Nav.DISMISSED` 冷却拦截，确保每次点击列表配置都能稳定弹出「替换导入 / 合并导入 / 取消」确认框。
* **扁平化目录与中文导出命名**：导出配置与默认模板统一存放在扁平目录 `config/module_shop_eco/`（同时兼容扫描旧版 `exports/*.json`，自动跳过 `backups/`、`professions.json`、`quality_rules.json` 与 `mod_names_zh_cn.json`）。导出弹窗支持中文名与实时文件名预览：留空保存为 `商店配置_<时间戳>.json`；输入 `shop_config` 直接写为默认模板 `shop_config.json` 并自动备份旧文件。
* **单机客户端落盘 vs 联机专用服直存同步**：单人游戏下导出直接写入客户端 `config/module_shop_eco/` 并原地刷新；联机专用服下导出直接保存在服务端配置目录，列表同时融合展示【服务端】与【本地】配置，管理员上传并确认导入本地配置时会同步保存到服务端目录。
* **一键导入打开文件夹 + 自动刷新**：点击「导入」按钮自动创建并打开本地配置文件夹，并每 20 刻轮询文件指纹变动自动刷新列表。

## [2.0.2]

公共 API 仍为 `1.4.0`，旧存档与旧配置可直接升级。

### 职业归属与防刷修复

* **容器菜单强校验**：厨师菜单追踪改为要求「5 刻内、10 格内真实右键已绑定厨具」且命中真实厨具菜单白名单，拦截冰箱、冰柜、无人机、钓鱼箱、市场、ATM、电脑、蘸料台、炉灶及熔炉族菜单，并将非玩家槽位中可手动放入的已有菜肴计入快照基线。
* **实体击杀与繁殖护栏**：农牧宰杀与繁殖改为明确禽畜白名单，拦截东篱外卖顾客/骑手、车万女仆、法术召唤物、驯服宠物/坐骑、NPC、观赏鸟、暮色任务羊及原版狐狸/青蛙/蜜蜂/美西螈等非家畜生物；战斗 Boss 判定补齐敌对校验。
* **方块成熟度防撞车**：排除木盆（`has_fruit`）、树苗/红树胎生苗/竹笋、甘蔗/仙人掌/竹竿、无果实属性藤蔓/海带、晒盐田与置地菜肴方块；成熟度提取优先判定果实布尔属性，右键采摘补齐 `isCropLikeBlock` 复核。
* **职业产出归类修正**：厨师成品经验强制要求具备食物属性、`WokDish` 盛盘数据或属于可放置菜肴/蛋糕/宴席方块，剔除刀具、餐盘、模具、家具、建材与生面团；`kaleidoscope_flora:*` 从厨师迁回农牧；排除钞票、无人机、ATM、刷怪蛋、钓具等杂项，补齐东篱鱼桶水产与熟食菜肴归属。

## [2.0.1]

公共 API 仍为 `1.4.0`，旧存档与旧配置可直接升级。

### 界面优化

* **列表 / 方块视图切换**：主界面右上角（换肤按钮左侧）新增视图切换开关，偏好持久化至客户端 `config/module_shop_ui_view.txt`。
* **统一紧凑方块卡片网格（`CardGrid`）**：为每日商店、每日收购、购物城、拍卖行、玩家摊位（大厅与我的商店）和求购广场 6 个前台交互页提供方块卡片模式，统一紧凑方格比例并保留原有列表视图逻辑、筛选分类、排序栏、官方参考价提示与管理员界面不变。

## [2.0.0] — Module-Shop Eco 独立闭环经济版

公共 API 升至 `1.4.0`（对 `1.2.x / 1.3.x` 附属保持 100% 二进制向后兼容）。世界存档物理隔离为 `module_shop_eco_data`，配置文件独立为 `module_shop_eco-common.toml` 与 `config/module_shop_eco/professions.json`。完整说明见 [ECO_2.0.0_构建验收报告与服主手册.md](ECO_2.0.0_构建验收报告与服主手册.md)。

### 新功能

* **官方闭环总钱箱（`ShopTreasury`）**：官店消费、刷新、补签、摊位/拍卖成交税、外部玩法净消耗统一回流总钱箱；出货箱、每日收购、签到、外币结汇统一从总钱箱拨付，触及安全保底线时自动截断收购量，剩余物品 100% 原样保留不吞货。
* **阿尔比恩式动态供需与防套利引擎（`EcoPricing` + `ClearingHouse`）**：购物城 `PLAYER_POOL` 货架逐件积分计价（零拆单漏洞）与 10% 应急微补货；黑市库存桶满仓（2.0x）直接停收防挂机机器倾销、每日自然消化 20%、断供每日 +5% 缺货悬赏；每日收购「4 本职 + 2 跨职」定向抽取与 70% 销毁泄洪；官方在售同款物品回收价 82% 防倒卖硬锁。
* **八大生活职业与每日专注点（公开发布全软联动）**：内置战斗、附魔、药水、农牧、厨师、钓鱼、调酒师（`auto` 智能显隐）、魔导师（`auto` 智能显隐）；严格校验在线真人亲手操作（拦截假人、女仆、无人机、漏斗）；每日恢复 1,000 专注点（上限 3,000 点），等级越高单件消耗专注越低；新增 UI v2 `ProfessionPage` 职业专精面板与 `/shop eco` 管理指令集（含准星一键绑厨具 `bind_cook`）。
* **全模组零硬依赖品质溢价与单向限额兑汇**：原生解析烟火凡人心火候、森罗厨房/酒馆陈酿年份与黑暗料理拒收、捕星者体型/黄金鱼、东篱南山居称重、铁魔法卷轴、附魔书与品质食物星级；货币兑换默认单向兑入星币并受个人日限额 + 国库外汇池双重保护。
* **公共 API 1.4.0**：新增 `TreasuryApi`、`ProfessionApi`（含近 7 日核验实业营收查询，供股市 IPO 对接）、`ShippingApi`、`ShippingSettledEvent`、`ProfessionLevelUpEvent`。

## [1.0.6]

公共 API 仍为 `1.2.0`，旧存档与旧配置可直接升级。

### 新功能

* 商店配置导出 / 导入 / 跨世界共享（上游 PR #11）：商品池、收购池、购物城货架、兑换规则、刷新权重与签到参数可导出为模板文件，
  在其他世界导入（服务端执行，需要 OP 权限）；新世界首次启动时可自动套用默认模板。详见 `docs/商店配置共享-设计与分析.md`。

### 前置库兼容

* 编译改用 AtomLib 0.4.0；运行期依赖范围仍为 `[0.1.0-beta,)`，0.1.0-beta 与 0.4.0 都能正常使用。
* AtomLib 以后更新若改掉了本模组用到的接口（运行中抛出 `LinkageError`），不再崩溃：本次游戏自动关闭 AtomLib 绘制、
  关掉当前界面并提示，重新打开后使用旧版界面。

### 修复

* 拍卖大厅顶栏在窄窗口下超出内容区；拍卖详情和上架界面的输入框、操作按钮宽度不再越过面板。

## [1.0.5]

本版本合并了「拍卖行升级」「玩家商店升级」「出货箱不出货」修复与「容器界面底板」修复。公共 API 仍为 `1.2.0`，旧存档与旧配置可直接升级。

### 界面修复

* 装了 ModernUI 等界面美化模组时，出货箱 / 物品发放 / 存入等容器界面的面板被一层暗色背景盖住、整块发灰：
  容器界面的底板改在 `ContainerScreenEvent.Render.Background`（最低优先级）里绘制。
* 附属模组的 AtomLib 界面在玩家还没打开过商店时弹出（例如红包详情）可能因 AtomLib 尚未初始化而崩溃：现在首次使用时自动初始化。

### 出货箱修复

* **倒计时结束后不出货**：原来只在 06:00 后 180 刻的窗口内结算，`/time add`、`/time set noon` 或服务器卡顿会整天错过；现在到点后的第一次 tick 就结算（每天仍只结算一次）。
* 游戏时间被调回（`/time set day`、`/time set 0` 等）后，箱子记录的上次结算日比今天大，会被一直当成「已结算」；现在检测到时间倒退会重置。
* 单人游戏切换存档时，上一个存档的「今天已结算」标记不会清空，同一游戏日的新存档整天不出货；现在服务器启动时重置。
* 额度 / 规则当日份数用完时会提示原因（物品仍留在箱子里），不再一声不吭。

### 玩家商店升级

#### 新功能

* 摊位页顶部分为「我的商店 / 商店大厅」两个视图，并保留原有的「查看摊位」入口。
* **商店大厅**：汇总全服摊位上有货的商品，可搜索、分类、按最新 / 价格排序、分页；点商品直接进入卖家摊位并预选这件商品。
* **摊位详情**：顶部显示店名、店主、营业状态和商品数；商品以物品格子卡片展示并分页；右侧是商品详情、购买数量（− / 输入 / + / 最大）、总价实时计算和购买按钮。
* **我的商店**：从背包选物品上架、补货（只收完全相同的物品）、改价（售罄的也能改）、下架；交易记录可只看摊位买卖。
* 新配置 `stallMaxStock`（单件库存上限，默认 64，原来写死为 64）。

#### 安全

* 购买、改价、补货、下架改为按商品 id 定位。原来按下标定位，卖家下架其它商品时可能导致**买错商品、扣错价格**。
* 购买时核对买家看到的单价；数量超过库存直接拒绝，不再悄悄少卖；每次请求带请求号去重；改价和下架也放进全局锁。

#### 修复

* 摊位交易流水备注显示为 `translation{...}` 乱码。
* 卖家流水备注写着「货款存邮箱」，实际货款已直接进钱包。
* 补货会把身上穿着的同款盔甲一起扣走（合并时排查发现）：补货只从主背包和副手取物品。

### 拍卖行升级

#### 新功能

* **拍卖大厅**：搜索（名称 / 物品 ID / 模组 / 附魔 / 卖家，多关键词）、9 种分类、7 种排序、自适应分页（滚轮翻页），
  每行显示当前价 / 最低下次出价 / 一口价 / 竞拍人数 / 倒计时 / 领先者，状态色条区分「我的 / 我领先 / 我被超过」。
* **详情页**：完整物品属性、竞价记录、出价、一口价（二次确认）、撤销、管理员强制结束 / 撤销。
* **独立上架界面**：从背包直接选物品，设置数量、起拍价、加价幅度、一口价、时长、是否允许竞价，实时显示手续费。
* **我的拍卖**：出售中 / 我的竞拍 / 已成交 / 已流拍 / 已撤销，可重新上架（只带出参数，物品须重新从背包选择）。
* **一口价**、**防截拍延时**（次数上限 + 总时长 2 倍封顶）、**上架费**（固定 + 百分比）、起拍价范围、全服 / 个人数量上限。
* **物品限制**：黑名单（ID / #标签 / 模组:*，潜影盒与收纳袋里的物品同样检查）、绑定物品、特殊 NBT、NBT 体积上限、禁止创造模式上架。
* **通知**：上架、被出价（30 秒合并一次）、被超价、成交、落选、流拍、撤销、即将结束提醒；红点与邮件沿用现有系统。
* **管理**：大厅「管理」界面（全部配置 + 运行统计 + 异常记录），`/shop auction list|history|anomalies|audit|cancel|end`。

#### 安全与可靠性

* 出价资金托管（`escrow`）+ 启动时 / 命令审计；请求号防重复提交；`expect` 防按过时价格出价；结算幂等、重启续结算。
* 上架改为一次调用完成「校验 → 扣费 → 取物品 → 入库」，物品不再暂存在内存里。
* 损坏的拍品数据原样保留，单条坏数据隔离保存，不影响整个存档读取；自增 id 落后时自动修正。
* 旧出价界面不再读客户端本地配置的加价幅度，改用服务端下发的最低出价。

#### 修复

* **邮箱已满时，离线玩家收到的物品邮件会直接消失**，在线玩家的物品会掉在地上：现在改为超额存入邮箱（对所有模块生效）。

#### 性能

* 拍卖列表不再随每次商店快照全量下发：按需拉取（带版本号）+ 增量推送，拍品编码缓存。

#### 兼容

* 旧拍卖存档、旧配置键全部保留；`antiSnipeExtensionSeconds` 新默认 30（已有配置文件里的 0 不会被改动）。公共 API 未变。

## [1.0.4]

### 新功能

* **商店外壳改用 AtomLib 绘制**：面板、侧栏、玩家信息块、标题栏与页面一起用 Skija 画，几何与点击区域不变。
* **22 个次级界面迁移到 AtomLib**：发放物品、兑换码编辑、上架 / 交易 / 拍卖出价、通知设置等弹窗与容器界面统一新画法；
  AtomLib 不可用时仍回退旧版界面。
* **动效**：侧栏选中块弹簧滑动、换页淡入上滑、按钮按下回弹、弹窗缩放弹出、开关滑块拉伸、悬停移入快移出慢、柔和投影。
* **界面皮肤**：右上角按钮打开皮肤菜单，可选日间、夜间与柔彩系列（樱糖 / 薄荷 / 雾蓝 / 奶茶 / 夜樱）。
  皮肤带渐变与柔光，切换时整个界面约 0.4 秒平滑过渡；选择保存在客户端 `config/module_shop_ui_theme.txt`。
* 主面板与弹窗改为半透明，透出背后的模糊画面。
* 「编辑布局」改用新画法，跟随皮肤配色。
* **公共 API 1.1.0 → 1.2.0（只增不改）**：`ShopTheme` 新增皮肤相关取色（`skin` / `lightness` / 各组渐变端点 / 柔光）
  与绘制方法 `dialogPanel` / `accentFill` / `contentFill`，附属页面与弹窗可以和主体皮肤保持一致。

### 修复

* 商品「品质」选项里选中的那一项（默认「普通」）整块透明、看起来像少了一个选项。
* 切换模块时左下角头像、侧栏红点跟着页面一起上浮。
* 侧栏通知角标改为圆角胶囊，日间主题下不再是深色数字。
* 货币兑换的物品格顶出卡片边缘。
* 日间主题下次级文字、边框、邮件标题里的亮黄 / 亮绿颜色代码看不清。
* 皮肤菜单被页面上的物品图标压住。

### 开发

* 新增《游戏内排查清单》（`docs/ingame-checklist.md`）。

## [1.0.3]

### 新功能

* **界面整体迁移到 AtomLib（Skija）**：12 个页面全部重绘；AtomLib 不可用（非 Windows）时自动回退旧版绘制。
* **附属模组扩展点 `ShopModules`（客户端）**：附属模组可以把自己的功能挂进商店左侧功能区。
  推荐 `registerPage`：附属页面像内置模块一样画在右侧内容区（`AddonPage` / `AddonFrame` / `AddonPageHost` / `ShopTheme`
  + UI 套件）；也可以用 `register` 注册弹出式界面。管理员可在「编辑布局」里拖动 / 移除附属条目。
  见《公共API-使用文档》第 10 节。公共 API 版本 1.0.0 → **1.1.0**（只增不改）。
* **公共 API 独立 jar**：构建时额外产出 `module_shop-<版本>-api.jar`（只含 `api` 包、不含 `api.internal`），
  附属模组对它编译即可，从构建层面隔离内部实现。
* 第一个使用该接口的附属：[module-shop: Red envelope（拼手气红包）](https://github.com/ChatMiaoPT-Meow/Module-Shop-RedPacket)，独立仓库 / 独立 jar。

### 修复（界面迁移过程中的 QA 问题）

* 购物城「最近购买」视图下，货架视图的按钮仍可被点中（误跳回货架 / 切收藏 / 翻页）：两个视图各自只保留自己的按钮。
* Skija 输入框背后的隐藏 EditBox 在不显示时仍能被点中并抢走焦点（数字键切页失效、打字悄悄改购物城搜索）：不显示时同步隐藏并失焦。
* 购物城右键星标不再触发批量购买；下拉只在选项上命中，点外面收起时搜索框一并失焦。
* 每日商店为空时不再显示「刷新商店」；长条布局的绘制 / 悬停 / 点击范围统一。
* 管理面板：签到模式展开模块下拉时日历不再响应悬停；出货箱错误规则的左右键动作与旧版一致。

### 变更

* 撤下早期的 AtomLib 试验实现（Yoga 布局壳层），改为「AtomLib 设计包 + 自有组件」路线；确认框与数量 / 金额输入框暂时恢复原版样式，后续用新组件重做。
* AtomLib 不再提交进仓库，构建时从 GitHub Release 下载并做 SHA-256 校验（`gradle/atomlib.gradle`）。
* 服务端逻辑（商店、数据、网络协议）与 1.0.2 相同，存档兼容。

## [未发布]

### 优化

* **全服快照广播合并**：`broadcastSnapshots` 改为打标记，在服务端 tick 末尾统一下发一次；
  同一 tick 内多次触发（摊位 / 求购到期、库存刷新、一次操作里连调）不再重复给每个玩家序列化 + 压缩整份快照。
* **广播内物品编码复用**：同一次广播里共享物品（购物城货架、求购、拍卖…）的 ItemStack 只走一次 Codec 编码。
* **购物城快照不再序列化个人已买记录**：`MallItem.saveForView` 以前先整表写出 `personalUsed` 再删掉。
* **邮件到期清理**从每秒一次降为每分钟一次（有效期按天计，精度足够）。
* **玩家上报包上限收紧**：服务端解析客户端上报时，压缩后 / 解压后上限从 64 MiB 降为 8 MiB；
  玩家下线时清理其未收齐的分片缓存。

## [1.0.2]

### 修复

* **普通玩家无法上架物品 / 拍卖上架**：`openDeposit(...)` 上被误加了一条「仅管理员」校验，
  导致摊位「上架物品」、拍卖「上架」、交易「放入物品」对普通玩家静默无响应（点了没反应）。
  现在只保留空值保护，玩家流程恢复正常；`admin_add` 等真正的管理操作仍各自校验权限。
## [1.0.1]

### 新功能

* **签到 / 交易 / 转账 变成正式模块**：侧栏最上方三个入口，页内提供「打开界面」按钮；签到设置（奖励、补签、累计档位、里程、累计加成、自动签到）全部收进「商店设置 → 选择模块 → 签到」。
* **改名系统**：编辑布局模式下可右键改名——左上角**商店主标题**、侧栏**模块名**、以及**货币名称**（右键余额，全服生效，留空恢复默认）。
* **货币名全局替换**：界面与聊天里写死的「星币」会自动跟随管理员设置的货币名（`CurrencyNames` 客户端包装层）。
* **通知红点**：每日商店 / 每日收购分别计数，不再互相点亮。
* **签到里程分轮**：每 7 天一轮，新一轮可重新领取 3/5/7 天里程奖励，断签重建也能再领。
* **购物城**：条目支持独立配置（见 `MallItem` 扩展）。

### 修复

* 余额悬停提示：去掉 `\n` 拼接造成的乱码，现在**只显示「点击查看账单明细」**（编辑布局时多一行右键提示）。
* 商店设置页与标题的右键改名此前不生效（条件写错），现在可用。
* 侧栏新增模块被追加到滚动区外、看起来「没加上」的问题。

### 工程

* 新增 `tools/check-ui.ps1`（模块/render/语言键/菜单界面一致性自检）与 `tools/deploy.ps1`（**游戏运行时不覆盖客户端 jar**，并自检四处哈希）。
## [1.0.0]

### 新增 · 公共 API（`cn.autoforged.module_shop.api`）

* `ModuleShopApi` 门面：`economy()` / `items()` / `mail()` / `history()` / `daily()` / `reward()` / `shop()` / `events()`
* 统一结果 `ApiResult` + 错误码 `ApiErrorCode`（不靠文案判断成败）
* 只读视图：`BalanceView` / `TransactionInfo` / `MailInfo` / `SignInView` / `ShopItemView` / `AuctionView` / `ActivityView`
* **幂等发奖** `RewardApi.grantOnce`：`requestId` 落存档，**重启后仍防重复**
* 事件 API（业务成功后通知、不可取消）：余额变更 / 邮件创建 / 奖励发放 / 签到完成 / 购买成交 / 出售成交 / 拍卖出价 / 求购发布 / 摊位上架
* 受校验的执行接口：`buyDaily` / `buyMall` / `sellBuyback` / `bid` / `createDemand` / `cancelDemand` / `stallList`（价格复核、限购、库存、在线、余额逐项校验，失败零副作用）
* **统一扣款入口** `ShopManager.debit(...)`：商店 14 处内联扣款改为走它，扣款也会发 `BalanceChangedEvent`

### 新增 · 集成

* **KubeJS 桥接**（可选构建）：脚本里直接用全局 `ModuleShop`（`-PkubejsJar=<jar>` 构建）
* **HTTP / REST**（可选、**默认关闭**）：9 个端点；Bearer token、默认只听 `127.0.0.1`、默认只读、每 IP 限速、审计日志、写操作强制 `requestId` 防重放

### 修复

* 求购交付：物品**没有投递给发布者**（钱扣了、东西消失）—— 现在在线入背包、离线进带附件的邮件
* 求购交付扣物品**非原子**：任一条失败会把已扣物品全部退回
* 商店界面「关不掉」：零点刷新/邮件推送会用快照把界面弹回来 —— 现在只有 `/shop` 这类主动请求才弹界面
* 求购「想要 %S ×%S」乱码、求购列表字号偏大、求购选物界面图标重叠（改 18px 行距 + 滚轮 + 裁剪）
* 出货箱重开吞物品、购物城二次购买失效、侧栏滚动、模块重命名等（详见 `docs/第八轮优化说明.md`）

### 工具

* `/shop bill <玩家|all> [起日] [止日]`：把交易流水导出成 CSV（含**玩家间对账**表，UTF-8 BOM，Excel 直接打开）

## [1.14.0] 及更早

见 `docs/更新日志-1.14到1.15.md` 与各轮 `第 N 轮需求实现说明.md`。
