package cn.autoforged.module_shop.command;

import cn.autoforged.module_shop.Config;
import cn.autoforged.module_shop.config.ShopTemplateStore;
import cn.autoforged.module_shop.data.BlackMarketBucket;
import cn.autoforged.module_shop.data.PlayerAccount;
import cn.autoforged.module_shop.data.ShopSavedData;
import cn.autoforged.module_shop.eco.ClearingHouse;
import cn.autoforged.module_shop.eco.profession.ProfessionConfig;
import cn.autoforged.module_shop.eco.profession.ProfessionManager;
import cn.autoforged.module_shop.shop.GrantManager;
import cn.autoforged.module_shop.shop.BillExporter;
import cn.autoforged.module_shop.shop.ShopManager;
import cn.autoforged.module_shop.shop.TemplateService;
import cn.autoforged.module_shop.shop.TradeManager;
import cn.autoforged.module_shop.shop.auth.AdminAuthService;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public final class ShopCommands {
    private ShopCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(build());
    }

    private static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("shop")
                .executes(ctx -> {
                    ShopManager.sendSnapshotOpen(ctx.getSource().getPlayerOrException(), "home");
                    return 1;
                })
                // 拍卖行管理：/shop auction list|history|anomalies|audit|cancel <id>|end <id>
                .then(Commands.literal("auction")
                        .requires(source -> source.hasPermission(2))
                        .executes(ctx -> {
                            if (!checkPlayerCommandUnlocked(ctx.getSource())) {
                                return 0;
                            }
                            ShopManager.sendSnapshotOpen(ctx.getSource().getPlayerOrException(), "auction");
                            return 1;
                        })
                        .then(Commands.literal("list").executes(ctx -> checkPlayerCommandUnlocked(ctx.getSource())
                                ? AuctionCommands.list(ctx.getSource()) : 0))
                        .then(Commands.literal("history")
                                .executes(ctx -> checkPlayerCommandUnlocked(ctx.getSource())
                                        ? AuctionCommands.history(ctx.getSource(), 1) : 0)
                                .then(Commands.argument("page", IntegerArgumentType.integer(1))
                                        .executes(ctx -> checkPlayerCommandUnlocked(ctx.getSource())
                                                ? AuctionCommands.history(ctx.getSource(),
                                                        IntegerArgumentType.getInteger(ctx, "page")) : 0)))
                        .then(Commands.literal("anomalies").executes(ctx -> checkPlayerCommandUnlocked(ctx.getSource())
                                ? AuctionCommands.anomalies(ctx.getSource()) : 0))
                        .then(Commands.literal("audit").executes(ctx -> checkPlayerCommandUnlocked(ctx.getSource())
                                ? AuctionCommands.audit(ctx.getSource()) : 0))
                        .then(Commands.literal("cancel")
                                .then(Commands.argument("id", LongArgumentType.longArg(1))
                                        .executes(ctx -> checkPlayerCommandUnlocked(ctx.getSource())
                                                ? AuctionCommands.force(ctx.getSource(),
                                                        LongArgumentType.getLong(ctx, "id"), true) : 0)))
                        .then(Commands.literal("end")
                                .then(Commands.argument("id", LongArgumentType.longArg(1))
                                        .executes(ctx -> checkPlayerCommandUnlocked(ctx.getSource())
                                                ? AuctionCommands.force(ctx.getSource(),
                                                        LongArgumentType.getLong(ctx, "id"), false) : 0))))
                .then(Commands.literal("mailbox").executes(ctx -> {
                    ShopManager.sendSnapshotOpen(ctx.getSource().getPlayerOrException(), "mail");
                    return 1;
                }))
                .then(Commands.literal("stall").executes(ctx -> {
                    ShopManager.sendSnapshotOpen(ctx.getSource().getPlayerOrException(), "stall");
                    return 1;
                }))
                .then(Commands.literal("eco")
                        .executes(ctx -> {
                            ShopManager.sendSnapshotOpen(ctx.getSource().getPlayerOrException(), "profession");
                            return 1;
                        })
                        .then(Commands.literal("status")
                                .requires(source -> source.hasPermission(2))
                                .executes(ctx -> checkPlayerCommandUnlocked(ctx.getSource()) ? ecoStatus(ctx.getSource()) : 0))
                        .then(Commands.literal("bind_cook")
                                .requires(source -> source.hasPermission(2))
                                .executes(ctx -> checkPlayerCommandUnlocked(ctx.getSource()) ? ecoBindCook(ctx.getSource()) : 0))
                        .then(Commands.literal("apply_preset")
                                .requires(source -> source.hasPermission(2))
                                .executes(ctx -> checkPlayerCommandUnlocked(ctx.getSource()) ? ecoApplyPreset(ctx.getSource()) : 0))
                        .then(Commands.literal("treasury")
                                .requires(source -> source.hasPermission(2))
                                .then(Commands.literal("add")
                                        .then(Commands.argument("amount", LongArgumentType.longArg())
                                                .executes(ctx -> checkPlayerCommandUnlocked(ctx.getSource())
                                                        ? ecoTreasuryAdd(ctx.getSource(), LongArgumentType.getLong(ctx, "amount")) : 0)))
                                .then(Commands.literal("set")
                                        .then(Commands.argument("amount", LongArgumentType.longArg(0L))
                                                .executes(ctx -> checkPlayerCommandUnlocked(ctx.getSource())
                                                        ? ecoTreasurySet(ctx.getSource(), LongArgumentType.getLong(ctx, "amount")) : 0))))
                        .then(Commands.literal("focus")
                                .requires(source -> source.hasPermission(2))
                                .then(Commands.literal("set")
                                        .then(Commands.argument("target", EntityArgument.player())
                                                .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                                        .executes(ctx -> checkPlayerCommandUnlocked(ctx.getSource())
                                                                ? ecoFocusSet(ctx.getSource(),
                                                                        EntityArgument.getPlayer(ctx, "target"),
                                                                        IntegerArgumentType.getInteger(ctx, "amount")) : 0)))))
                        .then(Commands.literal("prof")
                                .requires(source -> source.hasPermission(2))
                                .then(Commands.literal("addxp")
                                        .then(Commands.argument("target", EntityArgument.player())
                                                .then(Commands.argument("profId", StringArgumentType.word())
                                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                                                ProfessionManager.activeSpecs().stream().map(s -> s.id),
                                                                builder))
                                                        .then(Commands.argument("amount", LongArgumentType.longArg(1L))
                                                                .executes(ctx -> checkPlayerCommandUnlocked(ctx.getSource())
                                                                        ? ecoProfAddXp(ctx.getSource(),
                                                                                EntityArgument.getPlayer(ctx, "target"),
                                                                                StringArgumentType.getString(ctx, "profId"),
                                                                                LongArgumentType.getLong(ctx, "amount")) : 0)))))))
                .then(Commands.literal("auth")
                        .executes(ctx -> authCommand(ctx.getSource()))
                        .then(Commands.literal("status")
                                .requires(source -> source.getEntity() == null)
                                .executes(ctx -> printAuthStatus(ctx.getSource())))
                        .then(Commands.literal("disable")
                                .requires(source -> source.getEntity() == null)
                                .executes(ctx -> consoleDisableAuth(ctx.getSource()))))
                .then(Commands.literal("admin")
                        .requires(source -> source.hasPermission(2))
                        .executes(ctx -> {
                            if (!checkPlayerCommandUnlocked(ctx.getSource())) {
                                return 0;
                            }
                            ShopManager.sendSnapshotOpen(ctx.getSource().getPlayerOrException(), "admin");
                            return 1;
                        })
                        // 需求 12：指定某玩家刷新其每日商店货物与收购商品种类。
                        .then(Commands.literal("refresh")
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(ctx -> {
                                            if (!checkPlayerCommandUnlocked(ctx.getSource())) {
                                                return 0;
                                            }
                                            ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
                                            ShopManager.refreshDaily(target);
                                            ctx.getSource().sendSuccess(() -> Component.translatable(
                                                    "msg.module_shop.refreshed", target.getGameProfile().getName())
                                                    .withStyle(ChatFormatting.GREEN), true);
                                            return 1;
                                        })))
                        // 管理员发钱：不填目标 = 所有已知玩家（在线 + 历史离线），填目标则只发这些玩家。
                        .then(Commands.literal("money")
                                // /shop admin money set <玩家> <金额>：把余额**直接设为**该值（0 = 清空）。
                                .then(Commands.literal("set")
                                        .then(Commands.argument("target", StringArgumentType.word())
                                                .then(Commands.argument("amount", LongArgumentType.longArg(0L))
                                                        .executes(ctx -> checkPlayerCommandUnlocked(ctx.getSource())
                                                                ? setMoney(ctx.getSource(),
                                                                        StringArgumentType.getString(ctx, "target"),
                                                                        LongArgumentType.getLong(ctx, "amount")) : 0))))
                                .then(Commands.argument("amount", LongArgumentType.longArg(1L))
                                        .executes(ctx -> checkPlayerCommandUnlocked(ctx.getSource())
                                                ? giveMoneyAll(ctx.getSource(),
                                                        LongArgumentType.getLong(ctx, "amount")) : 0)
                                        // 目标写玩家名（在线/离线都行，离线按账户表名字匹配），也可以写 all 或 @a 表示全体。
                                        .then(Commands.argument("targets", StringArgumentType.greedyString())
                                                .executes(ctx -> checkPlayerCommandUnlocked(ctx.getSource())
                                                        ? giveMoneyNames(ctx.getSource(),
                                                                StringArgumentType.getString(ctx, "targets"),
                                                                LongArgumentType.getLong(ctx, "amount")) : 0))))
                        // 管理员发放物品：打开发放界面（放入物品 + 勾选玩家/全体 → 发送，物品在邮箱领取）。
                        .then(Commands.literal("send")
                                .executes(ctx -> {
                                    if (!checkPlayerCommandUnlocked(ctx.getSource())) {
                                        return 0;
                                    }
                                    GrantManager.open(ctx.getSource().getPlayerOrException());
                                    return 1;
                                }))
                        // 管理员查看某玩家的交易流水：/shop admin history <玩家> [页码]
                        .then(Commands.literal("history")
                                .then(Commands.argument("target", StringArgumentType.word())
                                        .executes(ctx -> checkPlayerCommandUnlocked(ctx.getSource())
                                                ? history(ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "target"), 1) : 0)
                                        .then(Commands.argument("page", IntegerArgumentType.integer(1))
                                                .executes(ctx -> checkPlayerCommandUnlocked(ctx.getSource())
                                                        ? history(ctx.getSource(),
                                                                StringArgumentType.getString(ctx, "target"),
                                                                IntegerArgumentType.getInteger(ctx, "page")) : 0)))))
                        // 同 IP 多账号登录限制已按需求移除（不再有 /shop admin ip 子命令）。
                // 账单导出（管理员）：/shop bill <玩家> [起日] [止日]  或  /shop bill all
                .then(Commands.literal("bill")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("all")
                                .executes(ctx -> checkPlayerCommandUnlocked(ctx.getSource())
                                        ? billAll(ctx.getSource(), null, null) : 0)
                                .then(Commands.argument("from", StringArgumentType.word())
                                        .executes(ctx -> checkPlayerCommandUnlocked(ctx.getSource())
                                                ? billAll(ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "from"), null) : 0)
                                        .then(Commands.argument("to", StringArgumentType.word())
                                                .executes(ctx -> checkPlayerCommandUnlocked(ctx.getSource())
                                                        ? billAll(ctx.getSource(),
                                                                StringArgumentType.getString(ctx, "from"),
                                                                StringArgumentType.getString(ctx, "to")) : 0))))
                        .then(Commands.argument("target", StringArgumentType.word())
                                .executes(ctx -> checkPlayerCommandUnlocked(ctx.getSource())
                                        ? billPlayer(ctx.getSource(),
                                                StringArgumentType.getString(ctx, "target"), null, null) : 0)
                                .then(Commands.argument("from", StringArgumentType.word())
                                        .executes(ctx -> checkPlayerCommandUnlocked(ctx.getSource())
                                                ? billPlayer(ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "target"),
                                                        StringArgumentType.getString(ctx, "from"), null) : 0)
                                        .then(Commands.argument("to", StringArgumentType.word())
                                                .executes(ctx -> checkPlayerCommandUnlocked(ctx.getSource())
                                                        ? billPlayer(ctx.getSource(),
                                                                StringArgumentType.getString(ctx, "target"),
                                                                StringArgumentType.getString(ctx, "from"),
                                                                StringArgumentType.getString(ctx, "to")) : 0)))))
                // 需求 3：点击交易邀请提示后执行，把自己的交易界面打开。
                .then(Commands.literal("trade")
                        .then(Commands.literal("open").executes(ctx -> {
                            TradeManager.openFor(ctx.getSource().getPlayerOrException());
                            return 1;
                        })))
                .then(Commands.literal("reload")
                        .requires(source -> source.hasPermission(2))
                        .executes(ctx -> {
                            if (!checkPlayerCommandUnlocked(ctx.getSource())) {
                                return 0;
                            }
                            ShopManager.reload(ctx.getSource().getServer());
                            ctx.getSource().sendSuccess(() -> Component.translatable("msg.module_shop.reloaded")
                                    .withStyle(ChatFormatting.GREEN), true);
                            return 1;
                        }))
                .then(Commands.literal("wallet")
                        .then(Commands.literal("balance").executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            ctx.getSource().sendSuccess(() -> Component.translatable(
                                    "msg.module_shop.balance",
                                    ShopManager.balanceOf(player),
                                    ShopManager.currencyName(ctx.getSource().getServer())), false);
                            return 1;
                        }))
                        .then(Commands.literal("pay")
                                .then(Commands.argument("target", EntityArgument.player())
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                                .executes(ctx -> pay(ctx.getSource(), EntityArgument.getPlayer(ctx, "target"),
                                                        IntegerArgumentType.getInteger(ctx, "amount"))))))
                        .then(Commands.literal("give")
                                .requires(source -> source.hasPermission(2))
                                .then(Commands.argument("target", EntityArgument.player())
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                                .executes(ctx -> checkPlayerCommandUnlocked(ctx.getSource())
                                                        ? give(ctx.getSource(), EntityArgument.getPlayer(ctx, "target"),
                                                                IntegerArgumentType.getInteger(ctx, "amount")) : 0))))
                        // 外部账目：其它插件 / 模组扣的或加的钱也写进玩家账单（只记账，不动本模组余额）。
                        // 用法：/shop wallet log <玩家名> <金额(负=扣款)> [事由] [来源]
                        .then(Commands.literal("log")
                                .requires(source -> source.hasPermission(2))
                                .then(Commands.argument("target", StringArgumentType.word())
                                        .then(Commands.argument("amount", LongArgumentType.longArg())
                                                .executes(ctx -> checkPlayerCommandUnlocked(ctx.getSource())
                                                        ? logExternal(ctx.getSource(),
                                                                StringArgumentType.getString(ctx, "target"),
                                                                LongArgumentType.getLong(ctx, "amount"), "", "") : 0)
                                                .then(Commands.argument("reason", StringArgumentType.greedyString())
                                                        .executes(ctx -> checkPlayerCommandUnlocked(ctx.getSource())
                                                                ? logExternal(ctx.getSource(),
                                                                        StringArgumentType.getString(ctx, "target"),
                                                                        LongArgumentType.getLong(ctx, "amount"),
                                                                        StringArgumentType.getString(ctx, "reason"), "") : 0))))));
    }

    /**
     * 玩家执行管理员指令时必须校验密码锁；控制台 / RCON / 命令方块 / FTB Quests / KubeJS（{@code getEntity() == null}）直接放行。
     */
    private static boolean checkPlayerCommandUnlocked(CommandSourceStack source) {
        if (source.getEntity() instanceof ServerPlayer player) {
            if (!AdminAuthService.canEditAdmin(player)) {
                source.sendFailure(Component.translatable("msg.module_shop.auth_required")
                        .withStyle(ChatFormatting.RED));
                ShopManager.promptAdminAuth(player);
                return false;
            }
        }
        return true;
    }

    private static int authCommand(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            return printAuthStatus(source);
        }
        if (!player.hasPermissions(2)) {
            source.sendFailure(Component.translatableWithFallback(
                    "msg.module_shop.no_permission", "权限不足"));
            return 0;
        }
        if (!AdminAuthService.isLockEnabled()) {
            source.sendSuccess(() -> Component.translatableWithFallback(
                    "msg.module_shop.auth_not_enabled", "当前未启用管理员密码锁")
                    .withStyle(ChatFormatting.GRAY), false);
            return 1;
        }
        if (AdminAuthService.isUnlocked(player)) {
            source.sendSuccess(() -> Component.translatable("msg.module_shop.auth_ok")
                    .withStyle(ChatFormatting.GREEN), false);
            return 1;
        }
        ShopManager.promptAdminAuth(player);
        return 1;
    }

    private static int printAuthStatus(CommandSourceStack source) {
        boolean enabled = AdminAuthService.isLockEnabled();
        int timeout = AdminAuthService.sessionTimeoutMinutes();
        int sessions = AdminAuthService.activeSessionCount();
        source.sendSuccess(() -> Component.literal("[ModuleShop] 密码锁: "
                + (enabled ? "开启" : "关闭")
                + ", 会话超时: " + (timeout <= 0 ? "直到下线" : timeout + " 分钟")
                + ", 当前已解锁会话数: " + sessions)
                .withStyle(enabled ? ChatFormatting.YELLOW : ChatFormatting.GREEN), false);
        return 1;
    }

    private static int consoleDisableAuth(CommandSourceStack source) {
        AdminAuthService.consoleDisable();
        ShopManager.refreshAllClients(source.getServer(), ShopManager.data(source.getServer()));
        source.sendSuccess(() -> Component.literal("[ModuleShop] 已从服务端控制台关闭管理员密码锁并同步在线管理员状态。")
                .withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    /** /shop wallet log <玩家> <金额> [事由]：其它插件 / 模组把扣款或入账写进该玩家账单（只记账）。 */
    private static int logExternal(CommandSourceStack source, String target, long amount, String reason,
            String sourceName) {
        if (amount == 0L) {
            source.sendFailure(Component.translatable("msg.module_shop.bill_log_zero"));
            return 0;
        }
        String plugin = sourceName == null || sourceName.isBlank() ? "外部插件" : sourceName;
        if (ShopManager.recordExternal(source.getServer(), target, amount, reason, plugin)) {
            source.sendSuccess(() -> Component.translatable("msg.module_shop.bill_log_ok",
                    target, amount).withStyle(ChatFormatting.GREEN), true);
            return 1;
        }
        source.sendFailure(Component.translatable("msg.module_shop.grant_unknown", target));
        return 0;
    }

    private static int pay(CommandSourceStack source, ServerPlayer target, int amount) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        CompoundTag tag = new CompoundTag();
        tag.putString("action", "do_transfer");
        tag.putString("target", target.getGameProfile().getName());
        tag.putString("amount", Integer.toString(amount));
        ShopManager.handleAction(player, tag);
        return 1;
    }

    private static int give(CommandSourceStack source, ServerPlayer target, int amount) {
        ShopManager.adminGive(target, amount);
        source.sendSuccess(() -> Component.translatable("msg.module_shop.gave", amount, target.getGameProfile().getName())
                .withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    /** /shop admin money <金额>：给所有已知玩家（在线 + 历史离线）各发 amount 星币。 */
    private static int giveMoneyAll(CommandSourceStack source, long amount) {
        if (rejectHugeAmount(source, amount)) {
            return 0;
        }
        GrantManager.MoneyResult result = GrantManager.giveMoney(source.getServer(),
                GrantManager.allKnownTargets(source.getServer()), amount);
        source.sendSuccess(() -> Component.translatable("msg.module_shop.grant_money_all",
                result.total(), amount, result.offline()).withStyle(ChatFormatting.GREEN), true);
        if (result.clamped() > 0) {
            source.sendSuccess(() -> Component.translatable("msg.module_shop.grant_clamped", result.clamped())
                    .withStyle(ChatFormatting.GOLD), false);
        }
        return result.total();
    }

    /** /shop admin money <金额> <目标...>：目标为玩家名（在线/离线均可，离线按账户表名字匹配），all / @a 表示全体。 */
    private static int giveMoneyNames(CommandSourceStack source, String text, long amount) {
        if (rejectHugeAmount(source, amount)) {
            return 0;
        }
        GrantManager.ResolveResult resolved = GrantManager.resolveTargets(source.getServer(), text);
        if (!resolved.unknown().isEmpty()) {
            source.sendFailure(Component.translatable("msg.module_shop.grant_unknown",
                    String.join("、", resolved.unknown())));
        }
        if (resolved.ids().isEmpty()) {
            if (resolved.unknown().isEmpty()) {
                source.sendFailure(Component.translatable("msg.module_shop.grant_no_target"));
            }
            return 0;
        }
        GrantManager.MoneyResult result = GrantManager.giveMoney(source.getServer(), resolved.ids(), amount);
        source.sendSuccess(() -> Component.translatable("msg.module_shop.grant_money_targets",
                result.total(), amount).withStyle(ChatFormatting.GREEN), true);
        if (result.clamped() > 0) {
            source.sendSuccess(() -> Component.translatable("msg.module_shop.grant_clamped", result.clamped())
                    .withStyle(ChatFormatting.GOLD), false);
        }
        return result.total();
    }

    /** /shop admin money set <玩家> <金额>：把余额直接设为该值（0 = 清空），在线/离线都可。 */
    private static int setMoney(CommandSourceStack source, String target, long amount) {
        String operator = source.getEntity() instanceof net.minecraft.world.entity.player.Player player
                ? player.getGameProfile().getName() : "控制台";
        if (!ShopManager.adminSetBalanceByName(source.getServer(), target, amount, operator)) {
            source.sendFailure(Component.translatable("msg.module_shop.grant_unknown", target));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("msg.module_shop.admin_set_balance_ok",
                target, amount).withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    /**
     * 拒绝超过余额上限的发放金额。
     * 单个玩家最多只能装下 maxBalance，更大的数字没有意义；提前拒绝比“发一半”更直观
     * （入账侧也做了饱和保护，避免 before + amount 溢出把余额写成负数）。
     */
    private static boolean rejectHugeAmount(CommandSourceStack source, long amount) {
        long max = Config.maxBalanceLimit();
        if (amount > max) {
            source.sendFailure(Component.translatable("msg.module_shop.grant_amount_too_large", max));
            return true;
        }
        return false;
    }

    /** /shop admin history <玩家> [页码]：查看某玩家的交易流水（控制台/RCON 也能用）。 */
    private static int history(CommandSourceStack source, String target, int page) {
        List<Component> lines = ShopManager.historyLines(source.getServer(), target, page);
        if (lines.isEmpty()) {
            source.sendFailure(Component.translatable("msg.module_shop.grant_unknown", target));
            return 0;
        }
        for (Component line : lines) {
            source.sendSuccess(() -> line, false);
        }
        return lines.size();
    }

    /** /shop bill <玩家> [起日] [止日]：把该玩家的流水导出成 CSV（服务端目录 config/module_shop_eco/bills）。 */
    private static int billPlayer(CommandSourceStack source, String target, String from, String to) {
        try {
            BillExporter.Result result = BillExporter.exportPlayer(source.getServer(), target,
                    BillExporter.parseDay(from), BillExporter.parseDay(to));
            if (result.path() == null) {
                source.sendFailure(Component.literal(result.message()));
                return 0;
            }
            source.sendSuccess(() -> Component.literal("账单已导出：" + result.message()
                    + " → " + result.path().toAbsolutePath()), true);
            return Math.max(1, result.rows());
        } catch (Exception ex) {
            source.sendFailure(Component.literal("导出失败：" + ex.getMessage()));
            return 0;
        }
    }

    /** /shop bill all [起日] [止日]：导出全服流水 + 玩家间对账表。 */
    private static int billAll(CommandSourceStack source, String from, String to) {
        try {
            BillExporter.Result result = BillExporter.exportAll(source.getServer(),
                    BillExporter.parseDay(from), BillExporter.parseDay(to));
            source.sendSuccess(() -> Component.literal("全服账单已导出：" + result.message()
                    + " → " + result.path().toAbsolutePath()), true);
            return Math.max(1, result.rows());
        } catch (Exception ex) {
            source.sendFailure(Component.literal("导出失败：" + ex.getMessage()));
            return 0;
        }
    }

    /** /shop eco status：查看官方总钱箱、景气健康系数、当日收支、外币池额度与黑市仓库状态。 */
    private static int ecoStatus(CommandSourceStack source) {
        ShopSavedData data = ShopManager.data(source.getServer());
        long quotaDay = Config.shippingQuotaDay();
        data.treasury.rollDayIfNeeded(quotaDay);

        double health = data.treasury.healthFactor(
                Config.INSTANCE.treasuryMinFactor.get(),
                Config.INSTANCE.treasuryMaxFactor.get()
        );
        long minFloor = Config.INSTANCE.treasuryMinFloor.get();
        long spendable = data.treasury.spendableAboveFloor(minFloor);
        long fxRoom = ClearingHouse.computeGlobalExchangeRoom(data, quotaDay);

        double fullRatio = Config.INSTANCE.blackMarketWarehouseFullRatio.get();
        int totalBuckets = data.blackMarket.size();
        int fullBuckets = 0;
        for (BlackMarketBucket b : data.blackMarket.values()) {
            if (b != null && b.isWarehouseFull(fullRatio)) {
                fullBuckets++;
            }
        }
        int finalFullBuckets = fullBuckets;

        source.sendSuccess(() -> Component.literal("=== [Module-Shop Eco 经济总览] ===")
                .withStyle(ChatFormatting.GOLD), false);
        source.sendSuccess(() -> Component.literal(String.format(Locale.ROOT,
                "• 官方总钱箱余额：%d 星币（锚定储备 %d · 安全保底线 %d · 可支配 %d）",
                data.treasury.balance, data.treasury.targetReserve, minFloor, spendable))
                .withStyle(ChatFormatting.YELLOW), false);
        source.sendSuccess(() -> Component.literal(String.format(Locale.ROOT,
                "• 收购景气健康系数：%.1f%%  |  今日进账：+%d  |  今日兑付支出：-%d",
                health * 100.0D, data.treasury.inflowToday, data.treasury.outflowToday))
                .withStyle(ChatFormatting.GREEN), false);
        source.sendSuccess(() -> Component.literal(String.format(Locale.ROOT,
                "• 外币池今日已兑付：%d 星币（剩余全服可兑额度：%s）",
                data.treasury.fxPaidToday,
                fxRoom >= Long.MAX_VALUE / 2 ? "不限" : Long.toString(fxRoom)))
                .withStyle(ChatFormatting.AQUA), false);
        source.sendSuccess(() -> Component.literal(String.format(Locale.ROOT,
                "• 黑市库存桶数量：%d 个（其中已满仓停收：%d 个）",
                totalBuckets, finalFullBuckets))
                .withStyle(ChatFormatting.GRAY), false);
        return 1;
    }

    /** /shop eco bind_cook：将准星对准的方块一键绑定或解绑为厨师做饭方块。 */
    private static int ecoBindCook(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        HitResult hit = player.pick(6.0D, 0.0F, false);
        if (hit.getType() != HitResult.Type.BLOCK || !(hit instanceof BlockHitResult blockHit)) {
            source.sendFailure(Component.literal("请将准星对准 6 格以内的目标做饭方块再执行此命令"));
            return 0;
        }
        BlockState state = player.serverLevel().getBlockState(blockHit.getBlockPos());
        if (state.isAir()) {
            source.sendFailure(Component.literal("准星对准的位置是空气，请对准实体做饭方块"));
            return 0;
        }
        ResourceLocation key = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        String blockId = key.toString();
        String blockName = state.getBlock().getName().getString();
        boolean bound = ProfessionConfig.toggleCookingBlock(blockId);
        if (bound) {
            source.sendSuccess(() -> Component.literal("✔ 已绑定厨师做饭方块：" + blockName
                    + " (" + blockId + ")，已保存至 professions.json").withStyle(ChatFormatting.GREEN), true);
        } else {
            source.sendSuccess(() -> Component.literal("✦ 已解除厨师做饭方块绑定：" + blockName
                    + " (" + blockId + ")，已保存至 professions.json").withStyle(ChatFormatting.YELLOW), true);
        }
        return 1;
    }

    /** /shop eco treasury add <amount>：增减官方总钱箱余额。 */
    private static int ecoTreasuryAdd(CommandSourceStack source, long amount) {
        ShopSavedData data = ShopManager.data(source.getServer());
        long quotaDay = Config.shippingQuotaDay();
        if (amount > 0L) {
            data.treasury.inflow(amount, quotaDay);
        } else if (amount < 0L) {
            data.treasury.rollDayIfNeeded(quotaDay);
            long dec = amount == Long.MIN_VALUE ? data.treasury.balance : Math.min(data.treasury.balance, -amount);
            data.treasury.balance -= dec;
        }
        data.setDirty();
        long newBal = data.treasury.balance;
        source.sendSuccess(() -> Component.literal("已调整官方总钱箱（" + (amount >= 0 ? "+" + amount : amount)
                + "），当前总钱箱余额：" + newBal + " 星币").withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    /** /shop eco treasury set <amount>：直接设定官方总钱箱余额。 */
    private static int ecoTreasurySet(CommandSourceStack source, long amount) {
        ShopSavedData data = ShopManager.data(source.getServer());
        data.treasury.rollDayIfNeeded(Config.shippingQuotaDay());
        data.treasury.balance = Math.max(0L, amount);
        data.setDirty();
        long newBal = data.treasury.balance;
        source.sendSuccess(() -> Component.literal("已将官方总钱箱余额直接设为：" + newBal + " 星币")
                .withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    /** /shop eco focus set <target> <amount>：设置指定在线玩家的可用专注点。 */
    private static int ecoFocusSet(CommandSourceStack source, ServerPlayer target, int amount) {
        ShopSavedData data = ShopManager.data(source.getServer());
        PlayerAccount account = data.account(target.getUUID());
        ProfessionManager.ensureFocusRefreshed(account);
        int maxCap = Config.INSTANCE.maxFocusCap.get();
        int clamped = Math.max(0, Math.min(maxCap, amount));
        account.career.focusPoints = clamped;
        account.career.lastFocusRestoreDay = Config.shippingQuotaDay();
        data.setDirty();
        ShopManager.sendSnapshot(target, null, true);
        source.sendSuccess(() -> Component.literal("已将 " + target.getGameProfile().getName()
                + " 的专注点设为 " + clamped + " / " + maxCap
                + (amount > maxCap ? "（已按上限截断）" : "")).withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    /** /shop eco prof addxp <target> <profId> <amount>：给指定玩家发放职业经验。 */
    private static int ecoProfAddXp(CommandSourceStack source, ServerPlayer target, String profId, long amount) {
        ProfessionConfig.ProfessionSpec spec = ProfessionManager.specOf(profId);
        if (spec == null) {
            String validIds = String.join(", ", ProfessionManager.activeSpecs().stream().map(s -> s.id).toList());
            source.sendFailure(Component.literal("未知或未启用的职业 ID：" + profId
                    + "（当前可用：" + validIds + "）"));
            return 0;
        }
        int newLv = ProfessionManager.grantXp(target, profId, amount, "admin");
        ShopManager.sendSnapshot(target, null, true);
        source.sendSuccess(() -> Component.literal("已向 " + target.getGameProfile().getName()
                + " 发放【" + spec.nameZh + "】经验 +" + amount + "，当前等级 Lv." + newLv)
                .withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    /** /shop eco apply_preset：一键把 config/module_shop_eco/ 默认模板（.mshop/.json.gz/.json）应用到当前存档并重载出货规则。 */
    private static int ecoApplyPreset(CommandSourceStack source) {
        java.nio.file.Path configDir = net.minecraftforge.fml.loading.FMLPaths.CONFIGDIR.get();
        java.nio.file.Path file = ShopTemplateStore.resolveExistingDefaultFile(configDir);
        if (file == null) {
            file = ShopTemplateStore.defaultFile(configDir);
        }
        ShopTemplateStore.ReadResult read = ShopTemplateStore.read(file);
        if (!read.ok()) {
            source.sendFailure(Component.literal("读取预设模板失败（" + read.error() + "）：" + file.toAbsolutePath()));
            return 0;
        }
        ShopSavedData data = ShopManager.data(source.getServer());
        TemplateService.ApplyResult res = TemplateService.apply(
                read.template(), data, TemplateService.Mode.REPLACE);
        data.templateInitDone = true;
        data.setDirty();
        ShopManager.reload(source.getServer());
        source.sendSuccess(() -> Component.literal("✔ 已应用商店预设模板「" + read.template().name
                + "」：成功载入 " + res.applied() + " 项（未安装模组自动跳过 " + res.invalid() + " 项），并已同步全服！")
                .withStyle(ChatFormatting.GREEN), true);
        return 1;
    }
}
