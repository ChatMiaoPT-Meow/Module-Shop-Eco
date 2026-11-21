package cn.autoforged.module_shop.eco.quality;

import cn.autoforged.module_shop.Config;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.SuspiciousStewItem;
import net.minecraft.world.item.TippedArrowItem;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 零硬前置的全模组原生品质溢价与废品拒收评估器。
 *
 * <p>通过 MC 1.20.1 原生 {@link ItemStack#getTag()} 动态解析任意模组 NBT 数据，
 * 无需在编译期引用任何第三方玩法模组类，单装原版或混装任意整合包均 100% 安全。
 */
public final class QualityEvaluator {
    private static final Logger LOG = LoggerFactory.getLogger("module_shop_eco/quality");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    public record QualityResult(
            boolean rejected,
            double multiplier,
            long intrinsicBaseFloor,
            String badge
    ) {
        public static final QualityResult NORMAL = new QualityResult(false, 1.0D, 0L, "");

        public static QualityResult reject(String reasonBadge) {
            return new QualityResult(true, 0.0D, 0L, reasonBadge);
        }
    }

    public static final class QualityConfig {
        public List<String> rejectedItems = new ArrayList<>(List.of(
                "kaleidoscope_cookery:dark_cuisine",
                "kaleidoscope_cookery:suspicious_stir_fry",
                "kaleidoscope_cookery:mystery_tea",
                "kaleidoscope_tavern:mystery_cocktail",
                "kaleidoscope_tavern:vinegar",
                "bountifulfares:foul_flesh",
                "twilightforest:stale_bread"
        ));
        public double[] wineBrewLevelMultipliers = {1.00D, 1.10D, 1.22D, 1.35D, 1.50D, 1.68D};
        public double starcatcherGoldenMultiplier = 2.00D;
        public double starcatcherTopSizeMaxBonus = 0.45D;
        public double fidaWeightBaseline = 1.50D;
        public double fidaWeightSlope = 0.06D;
        public double fidaWeightMaxBonus = 0.50D;
        public double fidPerfectCookMultiplier = 1.25D;
        public double fidSeasoningBonusMultiplier = 1.10D;
        public double fidImperfectCookMultiplier = 0.75D;
        public boolean rejectFidBurntDish = true;
        public double maxTotalQualityMultiplier = 2.50D;
    }

    private static volatile QualityConfig config = new QualityConfig();
    private static volatile Set<String> rejectedSet = Set.of(
            "kaleidoscope_cookery:dark_cuisine",
            "kaleidoscope_cookery:suspicious_stir_fry",
            "kaleidoscope_cookery:mystery_tea",
            "kaleidoscope_tavern:mystery_cocktail",
            "kaleidoscope_tavern:vinegar",
            "bountifulfares:foul_flesh",
            "twilightforest:stale_bread"
    );
    private static volatile boolean loaded;

    // 铁魔法软反射缓存（未安装时保持 false，绝不抛异常）
    private static volatile boolean ironsChecked;
    private static Method ironsGetSpellMethod;
    private static Method ironsGetRarityMethod;

    private QualityEvaluator() {
    }

    public static synchronized void reloadConfig() {
        loaded = true;
        try {
            Path dir = FMLPaths.CONFIGDIR.get().resolve("module_shop_eco");
            Files.createDirectories(dir);
            Path file = dir.resolve("quality_rules.json");
            if (Files.isRegularFile(file)) {
                QualityConfig parsed = GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8), QualityConfig.class);
                if (parsed != null) {
                    config = parsed;
                }
            } else {
                Files.writeString(file, GSON.toJson(config), StandardCharsets.UTF_8);
            }
            Set<String> rej = new HashSet<>();
            if (config.rejectedItems != null) {
                for (String id : config.rejectedItems) {
                    if (id != null && !id.isBlank()) {
                        rej.add(id.trim().toLowerCase(Locale.ROOT));
                    }
                }
            }
            rejectedSet = Set.copyOf(rej);
        } catch (Throwable ex) {
            LOG.warn("[ModuleShop-Eco] 读取 quality_rules.json 失败，使用内置默认值：{}", ex.toString());
        }
    }

    private static void ensureLoaded() {
        if (!loaded) {
            reloadConfig();
        }
    }

    /** 快速判断某物品是否属于烧糊/黑暗料理等拒收废品（用于厨师/调酒防刷经验与商店拒收）。 */
    public static boolean isRejectedWaste(ItemStack stack) {
        return evaluate(stack).rejected();
    }

    /**
     * 每日收购智能分档匹配（客户端背包计数与服务端扣物品共用同一判定规则）：
     * <ul>
     *   <li>始终拒收烧糊菜、黑暗料理、失败鸡尾酒等废品；</li>
     *   <li>若收购单模板本身带有非默认数据组件，或属于药水/附魔书/药箭/迷之炖菜等同物品 ID 不同内容物类型，严格要求组件完全一致（严防拿水瓶冒充药水交差）；</li>
     *   <li>若收购单模板为普通白板物品，按物品 ID 匹配，但自动跳过铁砧改名、附魔或有耐久损耗的装备，防止误吞玩家随身装备。</li>
     * </ul>
     */
    public static boolean matchesBuybackCandidate(ItemStack template, ItemStack candidate) {
        if (template == null || template.isEmpty() || candidate == null || candidate.isEmpty()) {
            return false;
        }
        if (candidate.getItem() != template.getItem()) {
            return false;
        }
        if (isRejectedWaste(candidate)) {
            return false;
        }
        Item item = template.getItem();
        if (template.hasTag()
                || item instanceof PotionItem
                || item instanceof EnchantedBookItem
                || item instanceof TippedArrowItem
                || item instanceof SuspiciousStewItem) {
            return ItemStack.isSameItemSameTags(template, candidate);
        }
        if (candidate.hasCustomHoverName() || candidate.isDamaged()) {
            return false;
        }
        Map<Enchantment, Integer> ench = EnchantmentHelper.getEnchantments(candidate);
        if (!ench.isEmpty()) {
            return false;
        }
        return EnchantmentHelper.deserializeEnchantments(EnchantedBookItem.getEnchantments(candidate)).isEmpty();
    }

    public static QualityResult evaluate(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return QualityResult.NORMAL;
        }
        try {
            if (!Config.INSTANCE.qualityBonusEnabled.get()) {
                return QualityResult.NORMAL;
            }
        } catch (RuntimeException ignored) {
        }
        ensureLoaded();

        ResourceLocation itemKey = BuiltInRegistries.ITEM.getKey(stack.getItem());
        String itemId = itemKey == null ? "" : itemKey.toString();
        if (rejectedSet.contains(itemId)) {
            return QualityResult.reject("废品拒收");
        }

        // 0. 防刷底线校验：拒收普通水瓶/无效果基底药水、空餐盘、无词条空附魔书
        if (stack.getItem() instanceof PotionItem) {
            Potion potion = PotionUtils.getPotion(stack);
            boolean plainBase = potion == null || potion == Potions.EMPTY || potion == Potions.WATER
                    || potion == Potions.MUNDANE || potion == Potions.THICK || potion == Potions.AWKWARD;
            if (plainBase && PotionUtils.getMobEffects(stack).isEmpty()) {
                return QualityResult.reject("白水/基底药水拒收");
            }
        }
        if (stack.getItem() instanceof EnchantedBookItem) {
            if (EnchantedBookItem.getEnchantments(stack).isEmpty()) {
                return QualityResult.reject("空附魔书拒收");
            }
        }

        QualityConfig cfg = config;
        double mult = 1.0D;
        long floorPrice = 0L;
        List<String> badges = new ArrayList<>(2);

        // 1. 烟火凡人心（fidworkblock）火候与调味检测（原生存储于 ItemStack tag -> WokDish.StirCount & seasoning）
        CompoundTag root = stack.getTag();
        if (("fidworkblock:plate".equals(itemId) || "fidworkblock:long_plate".equals(itemId)
                || "fidworkblock:small_plate".equals(itemId))
                && (root == null || root.isEmpty())) {
            return QualityResult.reject("空盘拒收");
        }
        if (root != null && !root.isEmpty()) {
            if (root.contains("WokDish", Tag.TAG_COMPOUND)) {
                CompoundTag wok = root.getCompound("WokDish");
                if (wok.contains("StirCount", Tag.TAG_ANY_NUMERIC)) {
                    int stir = wok.getInt("StirCount");
                    if (stir >= 13 && cfg.rejectFidBurntDish) {
                        return QualityResult.reject("烧糊拒收");
                    } else if (stir >= 5 && stir <= 9) {
                        mult *= cfg.fidPerfectCookMultiplier;
                        badges.add("完美火候");
                    } else {
                        mult *= cfg.fidImperfectCookMultiplier;
                        badges.add(stir <= 4 ? "火候欠佳" : "略微过火");
                    }
                }
                if (!root.getString("seasoning").isEmpty()) {
                    mult *= cfg.fidSeasoningBonusMultiplier;
                    badges.add("调味加成");
                }
            }
        }

        // 2. 森罗酒馆 / 次元酒窖 / 世界名酒：酒桶陈酿等级 kaleidoscope_tavern:brew_level (1..6)
        Tag brewTag = encodeDynamicComponent(stack, "kaleidoscope_tavern", "brew_level");
        if (brewTag instanceof NumericTag num) {
            int level = Math.max(1, Math.min(6, num.getAsInt()));
            double[] table = cfg.wineBrewLevelMultipliers;
            if (table != null && table.length >= level) {
                double m = table[level - 1];
                if (m > 1.001D) {
                    mult *= m;
                    badges.add(level + "级陈酿");
                }
            }
        }

        // 3. 捕星者（Starcatcher）：starcatcher:caught_fish_info（golden 黄金鱼 & percentile 体型排位）
        Tag fishInfoTag = encodeDynamicComponent(stack, "starcatcher", "caught_fish_info");
        if (fishInfoTag instanceof CompoundTag fishCompound) {
            boolean golden = fishCompound.getBoolean("golden");
            if (golden) {
                mult *= cfg.starcatcherGoldenMultiplier;
                badges.add("黄金鱼");
            } else if (fishCompound.contains("percentile", Tag.TAG_ANY_NUMERIC)) {
                float pct = fishCompound.getFloat("percentile");
                if (pct >= 0.0F && pct < 50.0F) {
                    double sizeBonus = cfg.starcatcherTopSizeMaxBonus * ((50.0D - pct) / 50.0D);
                    if (sizeBonus > 0.02D) {
                        mult *= (1.0D + sizeBonus);
                        badges.add("大体型");
                    }
                }
            }
        }

        // 4. 东篱南山居（FIDA）：称重鱼获 weighed_weight 与称重活禽 captured_weight
        double weight = readDoubleComponent(stack, "flavor_immersed_daily_argo", "weighed_weight");
        if (weight <= 0.0D) {
            weight = readDoubleComponent(stack, "flavor_immersed_daily_argo", "captured_weight");
        }
        if (weight > cfg.fidaWeightBaseline) {
            double wBonus = Math.min(cfg.fidaWeightMaxBonus, (weight - cfg.fidaWeightBaseline) * cfg.fidaWeightSlope);
            if (wBonus > 0.02D) {
                mult *= (1.0D + wBonus);
                badges.add(String.format(Locale.ROOT, "%.1f斤优品", weight));
            }
        }

        // 5. 铁魔法（Iron's Spellbooks）：法术卷轴 irons_spellbooks:scroll 的法术等级与稀有度
        if ("irons_spellbooks:scroll".equals(itemId)) {
            Tag spellContainer = encodeDynamicComponent(stack, "irons_spellbooks", "ISB_Spells");
            if (spellContainer == null) {
                spellContainer = encodeDynamicComponent(stack, "irons_spellbooks", "spell_container");
            }
            if (spellContainer instanceof CompoundTag scTag) {
                ListTag list = scTag.contains("data", Tag.TAG_LIST)
                        ? scTag.getList("data", Tag.TAG_COMPOUND)
                        : (scTag.contains("spells", Tag.TAG_LIST) ? scTag.getList("spells", Tag.TAG_COMPOUND) : null);
                if (list != null && !list.isEmpty()) {
                    CompoundTag slot0 = list.getCompound(0);
                    String spellId = slot0.getString("id");
                    int spellLevel = Math.max(1, slot0.getInt("level"));
                    double rarityMult = resolveIronsSpellRarityMultiplier(spellId, spellLevel);
                    double levelMult = 1.0D + 0.18D * (spellLevel - 1);
                    double scrollMult = Math.min(cfg.maxTotalQualityMultiplier, rarityMult * levelMult);
                    mult *= scrollMult;
                    floorPrice = Math.max(floorPrice, Math.round(35L * scrollMult));
                    badges.add("Lv." + spellLevel + "卷轴");
                }
            }
        }

        // 6. 原版附魔书：StoredEnchantments 词条稀有度权重与等级
        ListTag storedList = EnchantedBookItem.getEnchantments(stack);
        if (!storedList.isEmpty()) {
            Map<Enchantment, Integer> stored = EnchantmentHelper.deserializeEnchantments(storedList);
            if (!stored.isEmpty()) {
                double enchScore = 0.0D;
                for (Map.Entry<Enchantment, Integer> entry : stored.entrySet()) {
                    Enchantment ench = entry.getKey();
                    if (ench == null) {
                        continue;
                    }
                    int lv = Math.max(1, entry.getValue() == null ? 1 : entry.getValue());
                    int weightVal = Math.max(1, ench.getRarity().getWeight());
                    int maxLv = Math.max(1, ench.getMaxLevel());
                    double rarityFactor = Math.min(5.0D, 10.0D / weightVal);
                    double lvRatio = (double) lv / (double) maxLv;
                    double treasure = ench.isTreasureOnly() ? 1.40D : 1.0D;
                    enchScore += rarityFactor * (0.55D + 0.75D * lvRatio) * treasure;
                }
                if (enchScore > 0.0D) {
                    double bookMult = Math.min(3.50D, Math.max(1.0D, 0.85D + enchScore * 0.35D));
                    mult *= bookMult;
                    floorPrice = Math.max(floorPrice, Math.round(40L * enchScore));
                    if (bookMult > 1.08D) {
                        badges.add("高阶附魔");
                    }
                }
            }
        }

        // 7. 公开发布通用兼容：Quality Food (quality_food:quality) 星级食材
        Tag qfTag = encodeDynamicComponent(stack, "quality_food", "quality");
        if (qfTag instanceof CompoundTag qf && qf.contains("level", Tag.TAG_ANY_NUMERIC)) {
            int qLv = qf.getInt("level");
            if (qLv == 1) {
                mult *= 1.15D;
                badges.add("铁星品质");
            } else if (qLv == 2) {
                mult *= 1.35D;
                badges.add("金星品质");
            } else if (qLv >= 3) {
                mult *= 1.65D;
                badges.add("钻星品质");
            }
        }

        mult = Math.min(Math.max(0.10D, mult), cfg.maxTotalQualityMultiplier);
        String badgeText = badges.isEmpty() ? "" : String.join("·", badges);
        return new QualityResult(false, mult, floorPrice, badgeText);
    }

    private static Tag encodeDynamicComponent(ItemStack stack, String namespace, String path) {
        if (stack == null || stack.isEmpty() || path == null || path.isEmpty()) {
            return null;
        }
        CompoundTag tag = stack.getTag();
        if (tag == null || tag.isEmpty()) {
            return null;
        }
        if (tag.contains(path)) {
            return tag.get(path);
        }
        if (namespace != null && !namespace.isEmpty()) {
            String namespacedKey = namespace + ":" + path;
            if (tag.contains(namespacedKey)) {
                return tag.get(namespacedKey);
            }
            if (tag.contains(namespace, Tag.TAG_COMPOUND)) {
                CompoundTag sub = tag.getCompound(namespace);
                if (sub.contains(path)) {
                    return sub.get(path);
                }
            }
        }
        return null;
    }

    private static double readDoubleComponent(ItemStack stack, String namespace, String path) {
        Tag t = encodeDynamicComponent(stack, namespace, path);
        if (t instanceof NumericTag num) {
            return num.getAsDouble();
        }
        return 0.0D;
    }

    private static double resolveIronsSpellRarityMultiplier(String spellId, int level) {
        if (spellId == null || spellId.isEmpty()) {
            return 1.0D;
        }
        try {
            if (!ironsChecked) {
                ironsChecked = true;
                if (ModList.get().isLoaded("irons_spellbooks")) {
                    Class<?> regCls = Class.forName("io.redspace.ironsspellbooks.api.registry.SpellRegistry");
                    ironsGetSpellMethod = regCls.getMethod("getSpell", ResourceLocation.class);
                    Class<?> spellCls = Class.forName("io.redspace.ironsspellbooks.api.spells.AbstractSpell");
                    ironsGetRarityMethod = spellCls.getMethod("getRarity", int.class);
                }
            }
            if (ironsGetSpellMethod != null && ironsGetRarityMethod != null) {
                Object spell = ironsGetSpellMethod.invoke(null, ResourceLocation.tryParse(spellId));
                if (spell != null) {
                    Object rarityEnum = ironsGetRarityMethod.invoke(spell, level);
                    if (rarityEnum instanceof Enum<?> e) {
                        return switch (e.name()) {
                            case "UNCOMMON" -> 1.35D;
                            case "RARE" -> 1.80D;
                            case "EPIC" -> 2.50D;
                            case "LEGENDARY" -> 3.60D;
                            default -> 1.00D;
                        };
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return 1.0D;
    }
}
