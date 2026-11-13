# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

模块商店（Module Shop）：Minecraft 1.21.1 / NeoForge 21.1.248 的服务端权威经济模组（每日商店、收购、购物城、拍卖、摊位、求购、交易、钱包、邮箱、兑换、签到等）。客户端界面用 AtomLib（Skija）绘制。文档与代码注释均为中文。

## 常用命令

```powershell
.\gradlew.bat build                    # 产物：build/libs/module_shop-<ver>.jar 与 module_shop-<ver>-api.jar
.\gradlew.bat runClient                # 开发客户端（AtomLib 以 localRuntime 加载）
.\gradlew.bat runServer                # 开发服务端（--nogui）
.\gradlew.bat verifyAtomLibInput       # 校验 AtomLib jar 的 SHA-256（check 已依赖它）
.\gradlew.bat build -PkubejsJar=<kubejs jar>   # 额外编译 src/kubejs-bridge（脚本全局 ModuleShop）
pwsh tools\check-ui.ps1                # 界面一致性检查（见下）
```

- 需要 JDK 21。没有单元测试，验证靠「编译通过 + 游戏内验收」（清单：`docs/ingame-checklist.md`、`docs/atomlib-qa-checklist.md`）。
- 首次构建会把 AtomLib 发布包（版本见 `atomlib_version`，当前 0.4.0；运行期范围 `[0.1.0-beta,)`，只能用 beta3 已有接口）下载到 `.atomlib/`（约 100MB，SHA-256 固定在 `gradle.properties`），也可用 `-PatomlibMavenRepo=<目录>` / `ATOMLIB_MAVEN_REPO` 指定。逻辑在 `gradle/atomlib.gradle`。
- 开启了 configuration cache：Gradle 脚本里执行阶段不能调用脚本定义的方法（`atomlib.gradle` 里有内联写法的先例）。
- `tools/deploy.ps1` 的路径写死在原作者机器上；它的核心规则是**游戏运行时绝不覆盖客户端 jar**（热替换会导致 NoClassDefFoundError）。

## 架构

### 客户端 ↔ 服务端：只有两个包
- `network/payload/ServerboundShopActionPayload`：客户端发 `action` 字符串 + `CompoundTag`；服务端 `ShopManager.handleAction` 里一个大 switch 分发（`shop/ShopManager.java`，约 7000 行）。
- `ClientboundShopSyncPayload`：服务端把快照（NBT，`NbtTransport`/`ChunkAssembler` 分块）推给客户端，存入 `client/ClientShopData`。
- 每次服务端操作后客户端 `ShopScreen` 会被**重建**；需要跨重建保留的 UI 状态（滚动位置、展开项）放静态字段或 `ClientShopData`。
- 持久化在 `data/ShopSavedData`（各 `*Entry` 记录类 + `ShopNbt`）；配置在 `Config`（`config/module_shop-common.toml`，必须是无 BOM 的 UTF-8）。

### 客户端界面（新旧并存）
- `client/screen/ShopScreen.java`（约 6600 行）是外壳：侧栏、标题栏、资料块、`TAB_IDS` 页签表、旧版 `GuiGraphics` 绘制、`addRegion(..., "action", ...)` 命中区与 `onAction` 分发。
- `client/screen/shop/*Page`：每个页签一个 `ShopPage`，用 Skija 画内容区；业务动作一律走 `ShopHost.action` 回到 ShopScreen 原有分发，保证行为与旧界面一致。物品通过 `host.items()` 登记，Skija 绘制后统一画原版物品层。
- 次级界面继承 `client/screen/SkiaDialog`；容器界面只换底板绘制（`SkiaContainerBackground`，在 `ContainerScreenEvent.Render.Background` 最低优先级绘制）。
- **回退**：`AtomUi.available()` 为 false（非 Windows、AtomLib 缺失）时，每个界面都走同类里保留的旧绘制分支。改界面时两条路径都要顾及；删除旧代码属于迁移计划阶段 7，尚未决定。
- 迁移方案、进度与约定：`docs/atomlib-migration.md`。

### AtomLib 薄适配层 `client/ui/`
- 所有 `com.atom.lib.*` / Skija 调用只能出现在 `client` 包（Dist.CLIENT）。专服不装 AtomLib 必须能启动；`neoforge.mods.toml` 中 atomlib 依赖为 `side="CLIENT"`。
- `AtomUi`（初始化/可用性/字体）、`render/`（`SkiaFrame` 帧编排、`Draw`、`Text`、`StyledText` § 颜色码、`ItemOverlay` 物品层）、`widget/`（`UiComponent`、`Button`、`ScrollController`、`SkiaTextField`（隐藏原版 EditBox 承载输入/IME）、`Motion`）、`input/ImePosition`（IMM32 候选窗定位）、`ShopDesign`（皮肤色板 → AtomLib `DesignHost`）。
- 部分组件移植自 AtomChat（MIT），源文件里保留了出处注释，不要删。

### 公共 API（版本独立于模组版本，当前 1.2.0）
- `api/` 包：`ModuleShopApi` 入口，实现在 `api/internal/ApiImpl`。同一主版本内**只增不改**，`ApiVersion` 与 `docs/公共API-使用文档.md` 需同步更新。
- `api/client/`：附属模组扩展点 `ShopModules`（`registerPage` 让附属页面画在内容区，经 `AddonPageAdapter` 接入页面路由；内置 id 优先）与 `ShopTheme`（皮肤取色）。
- `apiJar` 任务只打包 `api/**`（排除 `api/internal`）以及 UI 套件：`client/ui/AtomUi`、`render/Draw*`、`Text*`、`SkiaFrame*`、`widget/**`。这些类对附属是公开契约，改签名等于破坏 API。附属示例：同级目录 `../Module-Shop-RedPacket`。

## 改动时的检查点
- 新增页签/动作/菜单后跑 `tools\check-ui.ps1`：它检查 `TAB_IDS` 每项都有 `case "<id>" ->` 分支、中英语言键 `tab.module_shop.<id>` 齐全且 `zh_cn.json` 与 `en_us.json` 键数一致、每个 `addRegion` action 都有 case、`ModMenus` 每个菜单都在 `ClientEvents` 注册了界面（新菜单需在脚本的 `$menuConsts` 补映射）。
- 资源模板（`src/main/templates`）含中文，`ProcessResources` 已设 UTF-8；Windows 默认 GBK 下不要绕开它。
- 版本号在 `gradle.properties` 的 `mod_version`；发布时同步 `CHANGELOG.md` 与 README 中的 jar 名。
