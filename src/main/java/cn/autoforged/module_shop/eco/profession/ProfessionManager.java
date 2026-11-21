package cn.autoforged.module_shop.eco.profession;

import cn.autoforged.module_shop.Config;
import cn.autoforged.module_shop.api.event.ApiEvents;
import cn.autoforged.module_shop.api.event.ProfessionLevelUpEvent;
import cn.autoforged.module_shop.data.PlayerAccount;
import cn.autoforged.module_shop.data.PlayerProfessionProfile;
import cn.autoforged.module_shop.data.ShopSavedData;
import cn.autoforged.module_shop.eco.EcoPricing;
import cn.autoforged.module_shop.shop.ShopManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.TieredItem;

/**
 * 八大职业核心服务：产出物归属识别、专注点恢复与结算、冷门紧缺倍率核算、经验发放与升级通知。
 */
public final class ProfessionManager {
    private ProfessionManager() {
    }

    public static boolean isEnabled() {
        try {
            return Config.INSTANCE.professionsEnabled.get();
        } catch (RuntimeException notLoaded) {
            return true;
        }
    }

    public static List<ProfessionConfig.ProfessionSpec> activeSpecs() {
        if (!isEnabled()) {
            return List.of();
        }
        return new ArrayList<>(ProfessionConfig.get().activeMap().values());
    }

    public static ProfessionConfig.ProfessionSpec specOf(String professionId) {
        if (professionId == null || professionId.isEmpty()) {
            return null;
        }
        return ProfessionConfig.get().activeMap().get(professionId);
    }

    public static String displayNameZh(String professionId) {
        ProfessionConfig.ProfessionSpec s = specOf(professionId);
        return s != null && s.nameZh != null ? s.nameZh : professionId;
    }

    /** 解析某职业用于客户端显示的图标物品（若模组专属图标未安装，自动退回原版图标）。 */
    public static ItemStack iconStackOf(ProfessionConfig.ProfessionSpec spec) {
        if (spec == null) {
            return new ItemStack(Items.BOOK);
        }
        Item primary = resolveItemOrNull(spec.iconItem);
        if (primary != null && primary != Items.AIR) {
            return new ItemStack(primary);
        }
        Item fallback = resolveItemOrNull(spec.fallbackIconItem);
        if (fallback != null && fallback != Items.AIR) {
            return new ItemStack(fallback);
        }
        return new ItemStack(Items.BOOK);
    }

    private static Item resolveItemOrNull(String idStr) {
        if (idStr == null || idStr.isBlank()) {
            return null;
        }
        ResourceLocation rl = ResourceLocation.tryParse(idStr.trim());
        if (rl == null || !BuiltInRegistries.ITEM.containsKey(rl)) {
            return null;
        }
        return BuiltInRegistries.ITEM.get(rl);
    }

    /** 确保某玩家的每日专注点已按当前自然日刷新。 */
    public static void ensureFocusRefreshed(PlayerAccount account) {
        if (account == null || !isEnabled()) {
            return;
        }
        long day = Config.shippingQuotaDay();
        account.career.refreshFocusIfNeeded(
                day,
                Config.INSTANCE.dailyFocusRestore.get(),
                Config.INSTANCE.maxFocusCap.get()
        );
    }

    /** 查询玩家当前等级最高的激活职业（用于每日收购 4 本职 + 2 跨职抽池）。 */
    public static String topProfessionOf(PlayerAccount account) {
        if (account == null || !isEnabled()) {
            return "";
        }
        List<ProfessionConfig.ProfessionSpec> specs = activeSpecs();
        if (specs.isEmpty()) {
            return "";
        }
        String bestId = "";
        int bestLv = 1;
        long bestTotalXp = 0L;
        for (ProfessionConfig.ProfessionSpec spec : specs) {
            PlayerProfessionProfile.Progress p = account.career.professions.get(spec.id);
            if (p == null) {
                continue;
            }
            if (p.level > bestLv || (p.level == bestLv && p.totalXp > bestTotalXp)) {
                bestLv = p.level;
                bestTotalXp = p.totalXp;
                bestId = spec.id;
            }
        }
        return bestTotalXp > 0L ? bestId : "";
    }

    /**
     * 精准识别某件物品归属哪个职业（具体程度优先级：精确物品ID > 原生类别特征 > 物品标签 > 模组命名空间通配）。
     * 保证例如森罗厨房的生番茄归「农牧」、炒好的糖醋里脊归「厨师」；东篱的鲈鱼归「钓鱼」、鸭蛋归「农牧」。
     */
    public static String matchProfessionForOutput(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !isEnabled()) {
            return "";
        }
        Map<String, ProfessionConfig.ProfessionSpec> active = ProfessionConfig.get().activeMap();
        if (active.isEmpty()) {
            return "";
        }

        ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        String fullId = key == null ? "" : key.toString().toLowerCase(Locale.ROOT);
        String ns = key == null ? "" : key.getNamespace().toLowerCase(Locale.ROOT);
        String path = key == null ? "" : key.getPath().toLowerCase(Locale.ROOT);

        // 第 1 优先级：professions.json 中配置的精确物品 ID
        for (ProfessionConfig.ProfessionSpec spec : active.values()) {
            if (spec.outputMatchers == null) continue;
            for (String m : spec.outputMatchers) {
                if (m != null && !m.startsWith("#") && !m.endsWith(":*")
                        && m.trim().equalsIgnoreCase(fullId)) {
                    return spec.id;
                }
            }
        }

        // 非商业产出守卫：屏蔽刷怪蛋、钞票、农具/渔具、电脑/无人机、餐具/厨具/建材等（第 1 优先级精确白名单仍可放行）
        if (stack.getItem() instanceof SpawnEggItem
                || ProfessionOutputFilter.isExcludedNonOutputItem(ns, path)
                || (isLifestyleToolOrGear(stack.getItem()) && isLifestyleNamespace(ns))) {
            return "";
        }

        // 第 2 优先级：强语义物品类别细分（防止命名空间通配误伤同模组下的生熟/鱼农分界）
        if (stack.getItem() instanceof EnchantedBookItem && active.containsKey(ProfessionConfig.ENCHANTING)) {
            return ProfessionConfig.ENCHANTING;
        }
        if (stack.getItem() instanceof PotionItem && "minecraft".equals(ns) && active.containsKey(ProfessionConfig.BREWING)) {
            return ProfessionConfig.BREWING;
        }
        if ((stack.is(ItemTags.FISHES)
                || "starcatcher".equals(ns)
                || ("flavor_immersed_daily_argo".equals(ns) && ProfessionOutputFilter.isFidaSeafoodOutputPath(path)))
                && active.containsKey(ProfessionConfig.FISHING)) {
            return ProfessionConfig.FISHING;
        }
        if ("flavor_immersed_daily_argo".equals(ns) && ProfessionOutputFilter.isFidaCookedDishPath(path)
                && active.containsKey(ProfessionConfig.COOKING)) {
            return ProfessionConfig.COOKING;
        }
        if ("twilightforest".equals(ns) && isTwilightCookedFoodPath(path)
                && active.containsKey(ProfessionConfig.COOKING)) {
            return ProfessionConfig.COOKING;
        }
        if (("kaleidoscope_cookery".equals(ns) || "kaleidoscope_chinesefood".equals(ns)
                || "kaleidoscope_nether".equals(ns) || "kaleidoscope_end".equals(ns)
                || "kaleidoscope_tavern".equals(ns) || "kaleidoscope_dim_wine".equals(ns)
                || "baishi_pantry".equals(ns) || "orients_hotpot_chronicles".equals(ns)
                || "bakeries".equals(ns) || "bountifulfares".equals(ns)
                || "farmersdelight".equals(ns) || "twilightforest".equals(ns)
                || "flavor_immersed_daily".equals(ns) || "flavor_immersed_daily_argo".equals(ns))
                && isRawCropOrSeed(stack, ns, path) && active.containsKey(ProfessionConfig.FARMING)) {
            return ProfessionConfig.FARMING;
        }

        // 第 3 优先级：#标签 规则
        for (ProfessionConfig.ProfessionSpec spec : active.values()) {
            if (spec.outputMatchers == null) continue;
            for (String m : spec.outputMatchers) {
                if (m != null && m.startsWith("#")) {
                    ResourceLocation tagId = ResourceLocation.tryParse(m.substring(1).trim());
                    if (tagId != null && stack.is(TagKey.create(Registries.ITEM, tagId))) {
                        return spec.id;
                    }
                }
            }
        }

        // 第 4 优先级：命名空间:* 规则
        for (ProfessionConfig.ProfessionSpec spec : active.values()) {
            if (spec.outputMatchers == null) continue;
            for (String m : spec.outputMatchers) {
                if (m != null && m.endsWith(":*")) {
                    String wantNs = m.substring(0, m.length() - 2).trim().toLowerCase(Locale.ROOT);
                    if (wantNs.equals(ns)) {
                        return spec.id;
                    }
                }
            }
        }

        // 第 5 优先级：通用熟食兜底（有较高饱食度的非生肉食物归厨师）
        FoodProperties food = stack.isEdible() ? stack.getFoodProperties(null) : null;
        if (food != null && food.getNutrition() >= 5 && active.containsKey(ProfessionConfig.COOKING)) {
            return ProfessionConfig.COOKING;
        }
        return "";
    }

    private static boolean isLifestyleToolOrGear(Item item) {
        return item instanceof TieredItem || item instanceof ArmorItem
                || item instanceof ShearsItem || item instanceof FishingRodItem;
    }

    private static boolean isLifestyleNamespace(String ns) {
        return "flavor_immersed_daily".equals(ns) || "flavor_immersed_daily_argo".equals(ns)
                || "fidworkblock".equals(ns) || "bakeries".equals(ns) || "bountifulfares".equals(ns)
                || "orients_hotpot_chronicles".equals(ns) || "kaleidoscope_cookery".equals(ns)
                || "kaleidoscope_chinesefood".equals(ns) || "kaleidoscope_nether".equals(ns)
                || "kaleidoscope_end".equals(ns) || "kaleidoscope_tavern".equals(ns)
                || "farmersdelight".equals(ns) || "guaniao".equals(ns)
                || "kaleidoscope_flora".equals(ns) || "wilderflowers".equals(ns);
    }

    private static boolean isTwilightCookedFoodPath(String path) {
        return path.equals("cooked_venison") || path.equals("cooked_meef")
                || path.equals("meef_stroganoff") || path.equals("maze_wafer")
                || path.equals("experiment_115") || path.equals("berry_medley")
                || path.equals("hydra_chop")
                || path.endsWith("_jerky");
    }

    private static boolean isRawCropOrSeed(ItemStack stack, String ns, String path) {
        if ("flavor_immersed_daily".equals(ns) && path.startsWith("raw")) {
            return true;
        }
        if (path.contains("seed") || path.equals("tomato") || path.equals("chili") || path.equals("red_chili")
                || path.equals("green_chili") || path.equals("chili_pepper") || path.equals("lettuce")
                || path.equals("rice") || path.equals("paddy") || path.equals("cabbage")
                || path.equals("onion") || path.equals("sand_onion") || path.equals("scallion")
                || path.equals("garlic") || path.equals("ginger") || path.equals("tea_leaves")
                || path.equals("lemon") || path.equals("orange") || path.equals("plum")
                || path.equals("smoked_plum") || path.equals("walnut") || path.equals("passion_fruit")
                || path.equals("elderberries") || path.equals("lapisberries") || path.equals("tea_berries")
                || path.equals("hoary_apple") || path.equals("maize") || path.equals("leek")
                || path.equals("eggplant") || path.equals("olive")
                || path.equals("raw_coffee_bean") || path.equals("coffee_bean") || path.equals("taro")
                || path.equals("basil") || path.equals("gai_lan") || path.equals("ginseng")
                || path.equals("goji_berry") || path.equals("longan") || path.equals("osmanthus")
                || path.equals("watermelon_radish") || path.equals("cilantro") || path.equals("greens")
                || path.equals("lotus_root") || path.equals("peanut") || path.equals("soybean")
                || path.equals("zhe_er_gen") || path.equals("soul_pepper") || path.equals("crimson_fruit")
                || path.equals("warped_fruit") || path.equals("dream_berry") || path.equals("ender_mint")
                || path.equals("chorus_petal") || path.equals("grape") || path.equals("green_grape")
                || path.equals("ice_grape") || path.equals("gold_grape") || path.equals("crimson_grape")
                || path.equals("warped_grape") || path.equals("dreamfruit") || path.equals("frostheart_fruit")
                || path.equals("hop") || path.equals("raw_venison") || path.equals("raw_meef")
                || path.equals("boarkchop") || path.equals("blackberry") || path.equals("blueberry")) {
            return true;
        }
        return stack.is(TagKey.create(Registries.ITEM, new ResourceLocation("forge", "crops")))
                || stack.is(TagKey.create(Registries.ITEM, new ResourceLocation("forge", "seeds")))
                || stack.is(TagKey.create(Registries.ITEM, new ResourceLocation("forge", "fruits")))
                || stack.is(TagKey.create(Registries.ITEM, new ResourceLocation("forge", "raw_meats")))
                || stack.is(TagKey.create(Registries.ITEM, new ResourceLocation("forge", "eggs")))
                || stack.is(TagKey.create(Registries.ITEM, new ResourceLocation("c", "crops")))
                || stack.is(TagKey.create(Registries.ITEM, new ResourceLocation("c", "seeds")))
                || stack.is(TagKey.create(Registries.ITEM, new ResourceLocation("c", "fruit")))
                || stack.is(TagKey.create(Registries.ITEM, new ResourceLocation("c", "fruits")))
                || stack.is(TagKey.create(Registries.ITEM, new ResourceLocation("c", "raw_meats")))
                || stack.is(TagKey.create(Registries.ITEM, new ResourceLocation("c", "eggs")));
    }

    /**
     * 计算某职业当前在全服的「冷门紧缺悬赏倍率」（1.00 ~ 1 + scarcityBonusMax）。
     * 统计近 3 个游戏日全服各激活职业的真实出货额，低于平均值的冷门行业自动给最高 +25% 奖励，热门行业锁死 1.00 不扣钱。
     */
    public static double computeScarcityMultiplier(ShopSavedData data, long currentGameDay, String professionId) {
        if (data == null || professionId == null || professionId.isEmpty() || !isEnabled()) {
            return 1.0D;
        }
        List<ProfessionConfig.ProfessionSpec> active = activeSpecs();
        if (active.size() <= 1) {
            return 1.0D;
        }
        Map<String, Long> totals = new HashMap<>();
        long grandTotal = 0L;
        for (ProfessionConfig.ProfessionSpec spec : active) {
            totals.put(spec.id, 0L);
        }
        for (PlayerAccount acct : data.accounts().values()) {
            for (ProfessionConfig.ProfessionSpec spec : active) {
                long rev = acct.career.sumVerifiedRevenue(currentGameDay, spec.id, 3);
                if (rev > 0L) {
                    totals.merge(spec.id, rev, Long::sum);
                    grandTotal += rev;
                }
            }
        }
        if (grandTotal < 1000L) {
            return 1.0D; // 新服开荒期数据不足时不偏置
        }
        double avg = (double) grandTotal / (double) active.size();
        double myTotal = totals.getOrDefault(professionId, 0L);
        if (myTotal >= avg) {
            return 1.0D; // 热门行业绝不惩罚，保底 1.00x
        }
        double maxBonus = Config.INSTANCE.scarcityBonusMax.get();
        double deficitRatio = (avg - myTotal) / (avg + 500.0D);
        double bonus = Math.min(maxBonus, Math.max(0.0D, deficitRatio * maxBonus));
        return 1.0D + bonus;
    }

    /**
     * 预览某玩家当前出售某物品可拿到的职业倍率（不扣减专注点，仅用于界面实时报价预览）。
     */
    public static double previewProfessionMultiplier(ShopSavedData data, PlayerAccount account,
                                                     long currentGameDay, ItemStack stack) {
        if (account == null || !isEnabled()) {
            return 1.0D;
        }
        String profId = matchProfessionForOutput(stack);
        if (profId.isEmpty()) {
            return 1.0D;
        }
        ensureFocusRefreshed(account);
        double scarcity = computeScarcityMultiplier(data, currentGameDay, profId);
        if (!account.career.focusSellEnabled) {
            return scarcity;
        }
        int lv = account.career.getLevel(profId);
        double cost = EcoPricing.computeFocusCostPerItem(lv, Config.INSTANCE.baseFocusCostPerItem.get());
        if (account.career.focusPoints + 1e-6D < cost) {
            return scarcity;
        }
        return EcoPricing.computeProfessionFocusMultiplier(lv, Config.INSTANCE.maxProfessionPriceBonus.get(), scarcity);
    }

    /**
     * 向在线真人玩家发放职业经验（带动作栏温和进度提示与升级庆祝音效/事件广播）。
     */
    public static int grantXp(ServerPlayer player, String professionId, long amount, String actionHint) {
        if (player == null || amount <= 0L || !isEnabled()) {
            return 1;
        }
        ProfessionConfig.ProfessionSpec spec = specOf(professionId);
        if (spec == null) {
            return 1;
        }
        MinecraftServer server = player.server;
        ShopSavedData data = ShopManager.data(server);
        PlayerAccount account = data.account(player.getUUID());
        ensureFocusRefreshed(account);

        int oldLevel = account.career.getLevel(professionId);
        int newLevel = account.career.addXp(professionId, amount);
        data.setDirty();

        long curXp = account.career.getXp(professionId);
        long nextReq = PlayerProfessionProfile.xpRequiredForNextLevel(newLevel);
        String progressText = newLevel >= PlayerProfessionProfile.effectiveMaxLevel()
                ? "MAX"
                : (curXp + "/" + nextReq);

        player.displayClientMessage(
                Component.literal("✦ [" + spec.nameZh + " Lv." + newLevel + "] +" + amount + " 经验 (" + progressText + ")")
                        .withStyle(ChatFormatting.GOLD),
                true
        );

        if (newLevel > oldLevel) {
            player.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.85F, 1.15F);
            double bonusPct = (EcoPricing.computeProfessionFocusMultiplier(
                    newLevel, Config.INSTANCE.maxProfessionPriceBonus.get(), 1.0D) - 1.0D) * 100.0D;
            double focusCost = EcoPricing.computeFocusCostPerItem(newLevel, Config.INSTANCE.baseFocusCostPerItem.get());
            player.sendSystemMessage(Component.literal(String.format(
                    Locale.ROOT,
                    "★ 恭喜！【%s】提升至 Lv.%d！专注精销加成升至 +%.1f%%，单件专注消耗降至 %.1f 点！",
                    spec.nameZh, newLevel, bonusPct, focusCost
            )).withStyle(ChatFormatting.GREEN));
            ApiEvents.post(new ProfessionLevelUpEvent(player.getUUID(), professionId, oldLevel, newLevel));
        }
        return newLevel;
    }

    /** 供离线/API 发放职业经验。 */
    public static int grantXpByUuid(MinecraftServer server, UUID playerId, String professionId, long amount) {
        if (server == null || playerId == null || amount <= 0L || !isEnabled()) {
            return 1;
        }
        ServerPlayer online = server.getPlayerList().getPlayer(playerId);
        if (online != null) {
            return grantXp(online, professionId, amount, "API");
        }
        ProfessionConfig.ProfessionSpec spec = specOf(professionId);
        if (spec == null) {
            return 1;
        }
        ShopSavedData data = ShopManager.data(server);
        PlayerAccount account = data.account(playerId);
        int oldLv = account.career.getLevel(professionId);
        int newLv = account.career.addXp(professionId, amount);
        data.setDirty();
        if (newLv > oldLv) {
            ApiEvents.post(new ProfessionLevelUpEvent(playerId, professionId, oldLv, newLv));
        }
        return newLv;
    }
}
