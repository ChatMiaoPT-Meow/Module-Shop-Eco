package cn.autoforged.module_shop.eco.profession;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 八大职业配置文件模型（{@code config/module_shop_eco/professions.json}）。
 *
 * <p>面向对外公开发布设计：
 * <ul>
 *   <li>前 6 大职业（战斗、附魔、药水、农牧、厨师、钓鱼）纯原版即可完整游玩，默认 {@code "always"} 开启；</li>
 *   <li>【调酒师】与【魔导师】默认设为 {@code "auto"} 智能显隐——检测到服内安装了对应酒水或魔法模组时自动亮出，纯原版服自动隐藏不违和；腐竹也可手动改为 {@code "true"} 或 {@code "false"}。</li>
 * </ul>
 */
public final class ProfessionConfig {
    private static final Logger LOG = LoggerFactory.getLogger("module_shop_eco/professions");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    public static final String COMBAT = "combat";
    public static final String ENCHANTING = "enchanting";
    public static final String BREWING = "brewing";
    public static final String FARMING = "farming";
    public static final String COOKING = "cooking";
    public static final String FISHING = "fishing";
    public static final String BARTENDING = "bartending";
    public static final String ARCHMAGE = "archmage";

    public static final class ProfessionSpec {
        public String id;
        public String nameZh;
        public String nameEn;
        public String descZh;
        public String descEn;
        public String iconItem;
        public String fallbackIconItem;
        /** "always" / "true" = 强制开启；"false" = 强制关闭；"auto" = 检测到 autoDetectMods 任一已安装时开启。 */
        public String enableMode = "always";
        public List<String> autoDetectMods = new ArrayList<>();
        /** 归属该职业的产出物匹配规则（支持 物品ID、#标签、命名空间:*）。 */
        public List<String> outputMatchers = new ArrayList<>();

        public ProfessionSpec() {
        }

        public ProfessionSpec(String id, String nameZh, String nameEn, String descZh, String descEn,
                              String iconItem, String fallbackIconItem, String enableMode,
                              List<String> autoDetectMods, List<String> outputMatchers) {
            this.id = id;
            this.nameZh = nameZh;
            this.nameEn = nameEn;
            this.descZh = descZh;
            this.descEn = descEn;
            this.iconItem = iconItem;
            this.fallbackIconItem = fallbackIconItem;
            this.enableMode = enableMode;
            this.autoDetectMods = new ArrayList<>(autoDetectMods);
            this.outputMatchers = new ArrayList<>(outputMatchers);
        }

        public boolean isEnabledRuntime() {
            if (enableMode == null || enableMode.isBlank() || "always".equalsIgnoreCase(enableMode)
                    || "true".equalsIgnoreCase(enableMode)) {
                return true;
            }
            if ("false".equalsIgnoreCase(enableMode) || "off".equalsIgnoreCase(enableMode)) {
                return false;
            }
            if ("auto".equalsIgnoreCase(enableMode)) {
                try {
                    ModList modList = ModList.get();
                    if (modList == null) {
                        return false;
                    }
                    for (String modId : autoDetectMods) {
                        if (modId != null && !modId.isBlank() && modList.isLoaded(modId.trim())) {
                            return true;
                        }
                    }
                } catch (Throwable ignored) {
                }
                return false;
            }
            return true;
        }
    }

    public List<ProfessionSpec> professions = new ArrayList<>();
    /** 厨师绑定的做饭方块列表（支持 完整方块ID 或 命名空间:*，支持游戏内 /shop eco bind_cook 一键增删）。 */
    public List<String> boundCookingBlocks = new ArrayList<>();
    /** 调酒师绑定的酿酒/冰柜/榨汁方块列表。 */
    public List<String> boundBartendingBlocks = new ArrayList<>();

    private static volatile ProfessionConfig instance;

    public static synchronized ProfessionConfig get() {
        if (instance == null) {
            reload();
        }
        return instance;
    }

    public static synchronized void reload() {
        ProfessionConfig defaults = createDefault();
        try {
            Path dir = FMLPaths.CONFIGDIR.get().resolve("module_shop_eco");
            Files.createDirectories(dir);
            Path file = dir.resolve("professions.json");
            if (Files.isRegularFile(file)) {
                ProfessionConfig parsed = GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8), ProfessionConfig.class);
                if (parsed != null && parsed.professions != null && !parsed.professions.isEmpty()) {
                    instance = parsed;
                    return;
                }
            } else {
                Files.writeString(file, GSON.toJson(defaults), StandardCharsets.UTF_8);
            }
        } catch (Throwable ex) {
            LOG.warn("[ModuleShop-Eco] 读取 professions.json 失败，使用内置默认职业配置：{}", ex.toString());
        }
        instance = defaults;
    }

    public static synchronized boolean saveCurrent() {
        if (instance == null) {
            return false;
        }
        try {
            Path dir = FMLPaths.CONFIGDIR.get().resolve("module_shop_eco");
            Files.createDirectories(dir);
            Path file = dir.resolve("professions.json");
            Files.writeString(file, GSON.toJson(instance), StandardCharsets.UTF_8);
            return true;
        } catch (Throwable ex) {
            LOG.warn("[ModuleShop-Eco] 保存 professions.json 失败：{}", ex.toString());
            return false;
        }
    }

    /** 切换某个方块 ID 在厨师绑定表中的状态（返回 true=已绑定，false=已解绑）。 */
    public static synchronized boolean toggleCookingBlock(String blockId) {
        if (blockId == null || blockId.isBlank()) {
            return false;
        }
        ProfessionConfig cfg = get();
        String norm = blockId.trim().toLowerCase(Locale.ROOT);
        if (cfg.boundCookingBlocks.removeIf(s -> s != null && s.trim().equalsIgnoreCase(norm))) {
            saveCurrent();
            return false;
        }
        cfg.boundCookingBlocks.add(norm);
        saveCurrent();
        return true;
    }

    public boolean isCookingBlockBound(String blockId) {
        return matchesBlockList(boundCookingBlocks, blockId);
    }

    public boolean isBartendingBlockBound(String blockId) {
        return matchesBlockList(boundBartendingBlocks, blockId);
    }

    private static boolean matchesBlockList(List<String> patterns, String blockId) {
        if (patterns == null || blockId == null || blockId.isEmpty()) {
            return false;
        }
        String lower = blockId.toLowerCase(Locale.ROOT);
        int colon = lower.indexOf(':');
        String ns = colon > 0 ? lower.substring(0, colon) : "";
        for (String p : patterns) {
            if (p == null || p.isBlank()) {
                continue;
            }
            String clean = p.trim().toLowerCase(Locale.ROOT);
            if (clean.endsWith(":*")) {
                String wantNs = clean.substring(0, clean.length() - 2);
                if (wantNs.equals(ns)) {
                    return true;
                }
            } else if (clean.equals(lower)) {
                return true;
            }
        }
        return false;
    }

    public Map<String, ProfessionSpec> activeMap() {
        Map<String, ProfessionSpec> map = new LinkedHashMap<>();
        for (ProfessionSpec spec : professions) {
            if (spec != null && spec.id != null && !spec.id.isBlank() && spec.isEnabledRuntime()) {
                map.put(spec.id, spec);
            }
        }
        return map;
    }

    public static ProfessionConfig createDefault() {
        ProfessionConfig cfg = new ProfessionConfig();

        cfg.boundCookingBlocks = new ArrayList<>(List.of(
                // 原版做饭方块
                "minecraft:smoker",
                "minecraft:campfire",
                "minecraft:soul_campfire",
                // 森罗物语：厨房 / 下界 / 末地（无界面右键盛菜与炉灶全系）
                "kaleidoscope_cookery:wok",
                "kaleidoscope_cookery:pot",
                "kaleidoscope_cookery:stove",
                "kaleidoscope_cookery:stockpot",
                "kaleidoscope_cookery:steamer",
                "kaleidoscope_cookery:shawarma_spit",
                "kaleidoscope_cookery:teapot",
                "kaleidoscope_cookery:millstone",
                "kaleidoscope_cookery:enamel_basin",
                "kaleidoscope_nether:nether_stove",
                "kaleidoscope_end:end_stove",
                // 烟火凡人心：工作方块（仅真实锅具/加工设备，排除餐具/摆盘/砧板）
                "fidworkblock:pot",
                "fidworkblock:wok",
                "fidworkblock:fryingpan",
                "fidworkblock:pressurecooker",
                "fidworkblock:steambox",
                "fidworkblock:stove",
                "fidworkblock:dryer",
                "fidworkblock:teapot",
                "fidworkblock:jar",
                "fidworkblock:soup",
                "fidworkblock:meatmincer",
                "fidworkblock:mortar",
                "fidworkblock:squeezingmachine",
                // 白氏药膳铺（暂未含实体方块，仅作兼容预留）
                "baishi_pantry:*",
                // 烘焙坊：仅真实烘焙加工设备（排除货架/餐盘/沙发/收银台）
                "bakeries:oven",
                "bakeries:toaster",
                "bakeries:blender",
                "bakeries:moka_pot",
                "bakeries:fermentation_box",
                "bakeries:fermentation_tank",
                "bakeries:cheese_tank",
                "bakeries:milk_tank",
                "bakeries:yeast_tank",
                "bakeries:mix_block",
                "bakeries:dough_crafting_table",
                // 东方火锅纪事：仅 12 口火锅本体（排除餐具架 plates、火锅桌、板凳、蘸料台、饮品桶等）
                "orients_hotpot_chronicles:copper_four_grid_hotpot",
                "orients_hotpot_chronicles:copper_nine_grid_hotpot",
                "orients_hotpot_chronicles:copper_round_yuanyang_hotpot",
                "orients_hotpot_chronicles:copper_split_yuanyang_hotpot",
                "orients_hotpot_chronicles:copper_square_yuanyang_hotpot",
                "orients_hotpot_chronicles:copper_taiji_yuanyang_hotpot",
                "orients_hotpot_chronicles:iron_four_grid_hotpot",
                "orients_hotpot_chronicles:iron_nine_grid_hotpot",
                "orients_hotpot_chronicles:iron_round_yuanyang_hotpot",
                "orients_hotpot_chronicles:iron_split_yuanyang_hotpot",
                "orients_hotpot_chronicles:iron_square_yuanyang_hotpot",
                "orients_hotpot_chronicles:iron_taiji_yuanyang_hotpot",
                "bountifulfares:fermentation_vessel",
                "bountifulfares:gristmill",
                // 公开发布热门美食模组软兼容（农夫乐事等）
                "farmersdelight:cooking_pot",
                "farmersdelight:skillet",
                "farmersdelight:stove"
        ));

        cfg.boundBartendingBlocks = new ArrayList<>(List.of(
                "kaleidoscope_tavern:barrel",
                "kaleidoscope_tavern:tap",
                "kaleidoscope_tavern:pressing_tub",
                "kaleidoscope_tavern:empty_glassware",
                "kaleidoscope_world_liquor:freezer",
                "kaleidoscope_world_liquor:bamboo_barrel",
                // 公开发布常见酿酒模组软兼容
                "vinery:fermentation_barrel",
                "vinery:apple_press",
                "brewinandchewin:keg"
        ));

        cfg.professions.add(new ProfessionSpec(
                COMBAT, "战斗", "Combat",
                "亲手击杀怪物与Boss获得经验，提升怪物战利品的官方收购价值",
                "Slay monsters and bosses for XP; increases monster loot sell value",
                "minecraft:diamond_sword", "minecraft:iron_sword", "always", List.of(),
                List.of(
                        "minecraft:rotten_flesh", "minecraft:bone", "minecraft:gunpowder", "minecraft:string",
                        "minecraft:spider_eye", "minecraft:ender_pearl", "minecraft:blaze_rod", "minecraft:ghast_tear",
                        "minecraft:slime_ball", "minecraft:magma_cream", "minecraft:phantom_membrane",
                        "minecraft:prismarine_shard", "minecraft:prismarine_crystals", "minecraft:shulker_shell",
                        "minecraft:nether_star", "minecraft:wither_skeleton_skull", "minecraft:breeze_rod",
                        "upgrade_aquatic:thrasher_tooth", "takesapillage:ravager_horn",
                        "cataclysm:*", "twilightforest:*", "irons_artifice:*"
                )
        ));

        cfg.professions.add(new ProfessionSpec(
                ENCHANTING, "附魔", "Enchanting",
                "使用附魔台消耗经验成功附魔获得经验，提升附魔书的官方收购价值",
                "Enchant items at an Enchanting Table for XP; increases Enchanted Book sell value",
                "minecraft:enchanting_table", "minecraft:enchanted_book", "always", List.of(),
                List.of("minecraft:enchanted_book")
        ));

        cfg.professions.add(new ProfessionSpec(
                BREWING, "药水", "Potions",
                "使用酿造台炼成药水获得经验，提升原版药水类的官方收购价值",
                "Brew fresh potions in a Brewing Stand for XP; increases potion sell value",
                "minecraft:brewing_stand", "minecraft:potion", "always", List.of(),
                List.of("minecraft:potion", "minecraft:splash_potion", "minecraft:lingering_potion")
        ));

        cfg.professions.add(new ProfessionSpec(
                FARMING, "农牧", "Farming & Ranching",
                "收获成熟农作物与饲养畜牧获得经验，提升基础作物和生鲜食材的收购价值",
                "Harvest mature crops and tend livestock for XP; increases raw crops & livestock sell value",
                "minecraft:golden_hoe", "minecraft:wheat", "always", List.of(),
                List.of(
                        "minecraft:wheat", "minecraft:carrot", "minecraft:potato", "minecraft:beetroot",
                        "minecraft:sugar_cane", "minecraft:pumpkin", "minecraft:melon_slice", "minecraft:melon",
                        "minecraft:sweet_berries", "minecraft:glow_berries", "minecraft:cocoa_beans",
                        "minecraft:apple", "minecraft:nether_wart", "minecraft:cactus", "minecraft:bamboo",
                        "minecraft:egg", "minecraft:milk_bucket", "minecraft:leather", "minecraft:rabbit_hide",
                        "minecraft:feather", "minecraft:honey_bottle", "minecraft:honeycomb",
                        "minecraft:beef", "minecraft:porkchop", "minecraft:chicken", "minecraft:mutton", "minecraft:rabbit",
                        "#minecraft:wool", "#c:crops", "#c:seeds", "#c:eggs",
                        "flavor_immersed_daily_argo:*", "guaniao:*", "wilderflowers:*"
                )
        ));

        cfg.professions.add(new ProfessionSpec(
                COOKING, "厨师", "Chef",
                "从绑定的做饭方块端出熟食成品获得经验，提升菜肴甜点与火锅熟食的收购价值",
                "Cook dishes in bound kitchen blocks for XP; increases cooked dishes & bakery sell value",
                "minecraft:smoker", "minecraft:cooked_beef", "always", List.of(),
                List.of(
                        "minecraft:cooked_beef", "minecraft:cooked_porkchop", "minecraft:cooked_chicken",
                        "minecraft:cooked_mutton", "minecraft:cooked_rabbit", "minecraft:cooked_cod",
                        "minecraft:cooked_salmon", "minecraft:baked_potato", "minecraft:bread",
                        "minecraft:pumpkin_pie", "minecraft:cake", "minecraft:cookie", "minecraft:golden_carrot",
                        "minecraft:rabbit_stew", "minecraft:mushroom_stew", "minecraft:beetroot_soup",
                        "upgrade_aquatic:cooked_pike", "upgrade_aquatic:cooked_perch", "upgrade_aquatic:cooked_lionfish",
                        "tlm_overture:sweet_berry_cake", "tlm_overture:sweet_berry_candy",
                        "kaleidoscope_cookery:*", "kaleidoscope_chinesefood:*", "kaleidoscope_nether:*",
                        "kaleidoscope_end:*", "kaleidoscope_flora:*", "baishi_pantry:*",
                        "fidworkblock:*", "flavor_immersed_daily:*", "bakeries:*",
                        "orients_hotpot_chronicles:*", "bountifulfares:*", "farmersdelight:*"
                )
        ));

        cfg.professions.add(new ProfessionSpec(
                FISHING, "钓鱼", "Fishing",
                "亲手垂钓起竿获得经验（支持原版/捕星者/东篱遛鱼），提升鱼类与海鲜的收购价值",
                "Catch fish with rods & minigames for XP; increases fish & seafood sell value",
                "minecraft:fishing_rod", "minecraft:cod", "always", List.of(),
                List.of(
                        "#minecraft:fishes", "minecraft:cod", "minecraft:salmon", "minecraft:tropical_fish",
                        "minecraft:pufferfish", "minecraft:nautilus_shell",
                        "upgrade_aquatic:pike", "upgrade_aquatic:perch", "upgrade_aquatic:lionfish",
                        "kaleidoscope_chinesefood:yellow_croaker",
                        "starcatcher:*"
                )
        ));

        cfg.professions.add(new ProfessionSpec(
                BARTENDING, "调酒师", "Bartender",
                "酒桶陈酿接酒与调酒壶摇酒获得经验，提升果酒、名酒与鸡尾酒的收购价值",
                "Age wines in barrels and shake cocktails for XP; increases wines & cocktails sell value",
                "kaleidoscope_tavern:barrel", "minecraft:honey_bottle", "auto",
                List.of("kaleidoscope_tavern", "kaleidoscope_dim_wine", "kaleidoscope_world_liquor",
                        "vinery", "brewery", "brewinandchewin"),
                List.of(
                        "kaleidoscope_tavern:*", "kaleidoscope_dim_wine:*", "kaleidoscope_world_liquor:*",
                        "vinery:*", "brewery:*", "brewinandchewin:*"
                )
        ));

        cfg.professions.add(new ProfessionSpec(
                ARCHMAGE, "魔导师", "Archmage",
                "抄写法术卷轴、奥术铁砧升阶与炼金锅熬墨获得经验，提升卷轴与奥术秘药的收购价值",
                "Inscribe spell scrolls, upgrade on Arcane Anvil & brew arcane inks for XP",
                "irons_spellbooks:scroll", "minecraft:writable_book", "auto",
                List.of("irons_spellbooks", "ars_nouveau"),
                List.of("irons_spellbooks:*", "ars_nouveau:*")
        ));

        return cfg;
    }
}
