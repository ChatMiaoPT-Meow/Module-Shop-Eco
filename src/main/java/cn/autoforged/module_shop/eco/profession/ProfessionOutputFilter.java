package cn.autoforged.module_shop.eco.profession;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 纯 Java 职业分类、防刷菜单校验与产出白名单辅助（无 MC 启动环境依赖，便于单元测试直接覆盖）：
 * <ol>
 *   <li>容器菜单准入：仅允许已绑定的真实厨具菜单开启烹饪追踪，屏蔽冰箱/市场/无人机/电脑等储物与商店界面；</li>
 *   <li>实体归属校验：剥离召唤物、宠物、女仆、东篱外卖顾客/骑手、观赏鸟、魔法召唤熊/马等误判；</li>
 *   <li>方块成熟度护栏：屏蔽树苗、木盆、置地菜肴、海带藤蔓、盐田等误用 stage/age/has_fruit 的非农作物方块；</li>
 *   <li>产出白名单与高水位线：拦截餐具/空盘/刀具/建材/教程书/钞票/刷怪蛋，放行合法熟食、海鲜渔获与宴席盘。</li>
 * </ol>
 */
final class ProfessionOutputFilter {

    private static final Set<String> NON_COOKING_MENU_TOKENS = Set.of(
            "fridge", "freezer", "chest", "shulker", "backpack", "pouch", "storage",
            "cabinet", "cupboard", "basket", "rack", "market", "shop", "purchasing",
            "cash_register", "atm", "receipt", "computer", "uav", "fishing", "steelyard",
            "saucestation", "sauce_station", "stovescreenhandler", "cropconversion",
            "raddles", "chickencoop", "chicken_coop", "incubator", "sewing",
            "uncrafting", "weaponfusion", "ministrostiy", "frameworkbench",
            "crownest", "enchantment", "brewingstand", "furnace", "smoker", "blastfurnace"
    );

    private static final Set<String> SUMMON_PET_NPC_CLASS_TOKENS = Set.of(
            "magicsummon", "summoned", "foodorder", "entitymaid", "imaid",
            "smileyball", "piggyfurnace", "questram", "quest_ram", "loyalzombie"
    );

    private static final Set<String> NON_LIVESTOCK_PATH_TOKENS = Set.of(
            "customer", "rider", "maid", "summon", "pet", "fox", "wolf", "cat",
            "parrot", "bee", "frog", "tadpole", "axolotl", "turtle", "panda",
            "polar_bear", "ocelot", "sniffer", "armadillo", "squirrel", "penguin",
            "raven", "crow", "mouse", "smiley", "piggyfurnace", "quest_ram"
    );

    private static final Set<String> NON_CROP_BLOCK_CLASS_TOKENS = Set.of(
            "woodbasin", "basinblock", "bighook", "eggbreaking", "appraisalmachine",
            "fridge", "placeablefood", "multistagefood", "multistageinteractive",
            "entityfooddish", "cookedwholesheep", "colablock", "juiceblock",
            "saltpan", "saltpile", "dormantore", "forestryfire", "cakeroll",
            "cakeblock", "bcakeblock", "pieblock", "feastblock", "foodbiteblock"
    );

    private static final Set<String> FOOD_LIFESTYLE_NAMESPACES = Set.of(
            "fidworkblock", "flavor_immersed_daily", "flavor_immersed_daily_argo",
            "bakeries", "bountifulfares", "orients_hotpot_chronicles",
            "kaleidoscope_cookery", "kaleidoscope_chinesefood", "kaleidoscope_nether",
            "kaleidoscope_end", "farmersdelight", "baishi_pantry"
    );

    private static final Set<String> FIDA_EXCLUDED_UTILITY_IDS = Set.of(
            "credit_card", "atm", "shopping_computer", "receipt_printer",
            "catering_business_license", "purchasing_box", "phone",
            "guide_book", "developer_guide_book", "fishing_handbook",
            "ranger_clipboard", "takeaway_bag", "carton", "seat_cushion",
            "uav", "handcart", "sickle", "harrow", "watering_can",
            "milker", "livestock_brush", "stock_whip", "poultry_catching_net",
            "insect_net", "fishing_net", "fish_net", "fishing_rod",
            "rod_pod", "fishing_box", "fish_protection", "fish_bowl",
            "fish_display_board", "fishing_floating_platform", "steelyard",
            "waterwheel", "water_pump", "water_pipe", "rotating_water_pipe",
            "sector_water_pipe", "water_oxygen_machine", "water_quality_detector",
            "water_level_gauge", "weather_instrument", "dosing_tube",
            "methane_tank", "septic_tank", "feeding_trough", "chicken_coop",
            "insect_incubator", "pet_nest", "sewing_machine", "scarecrow",
            "fire_extinguisher", "poison_bait_station", "whistle", "ranging_rope",
            "syringe", "syringe_dna", "flea_treatment", "herbicide", "jinkela",
            "organic_fertilizer", "poop", "marsh_gas", "activated_carbon_particles",
            "microbe_powder", "high_protein_substance", "oxyphylline",
            "protocatechuic_acid", "tree_paint", "insect_board", "straw_hat",
            "cardboard_honeycomb", "fertile_farmland", "river_silt",
            "salt_pan", "salt_pile"
    );

    private ProfessionOutputFilter() {}

    // =========================================================================
    // 1. 容器菜单准入控制（分类一：厨师界面防误判）
    // =========================================================================

    static boolean isAllowedModCookingMenu(String menuClassName) {
        if (menuClassName == null || menuClassName.isEmpty()) {
            return false;
        }
        String lower = menuClassName.toLowerCase(Locale.ROOT);
        for (String blocked : NON_COOKING_MENU_TOKENS) {
            if (lower.contains(blocked)) {
                return false;
            }
        }
        if (lower.equals("com.fidtest.screen.cookingmenu")
                || (lower.startsWith("com.fidtest.") && lower.contains("menu"))) {
            return true;
        }
        if (lower.equals("com.flavor_immersed_daily.screen.mooncakemoldmenu")
                || lower.equals("com.flavor_immersed_daily.screen.eggbreakingmachinemenu")) {
            return true;
        }
        if (lower.equals("cn.blockforge.generated.orientshotpotchronicles.hotpot.hotpotscreenhandler")) {
            return true;
        }
        if (lower.startsWith("com.renyigesai.bakeries.common.inventory.")) {
            return true;
        }
        if (lower.contains("bountifulfares")
                && (lower.contains("gristmill") || lower.contains("ferment") || lower.contains("cooking"))) {
            return true;
        }
        if (lower.contains("farmersdelight")
                && (lower.contains("cooking") || lower.contains("pot") || lower.contains("skillet"))) {
            return true;
        }
        if ((lower.contains("baishi") || lower.contains("kaleidoscope_cookery")) && lower.contains("menu")) {
            return true;
        }
        return (lower.contains("cookingpot") || lower.contains("cooking_pot")) && lower.contains("menu");
    }

    // =========================================================================
    // 2. 实体归属分类（分类二：顾客/女仆/召唤物/观赏鸟与家畜判定）
    // =========================================================================

    static boolean isSummonOrPetOrNpc(String ns, String path,
                                        boolean isTamable, boolean isOwnedNonHorse,
                                        boolean isTamedHorse, boolean isNpc,
                                        Iterable<String> classHierarchyNames) {
        if (isTamable || isOwnedNonHorse || isTamedHorse || isNpc) {
            return true;
        }
        if ("flavor_immersed_daily_argo".equals(ns) && path != null
                && ("customer".equals(path) || "delivery_rider".equals(path)
                || "uav".equals(path) || "handcart".equals(path) || "harvester".equals(path))) {
            return true;
        }
        if (classHierarchyNames != null) {
            for (String rawName : classHierarchyNames) {
                if (rawName == null) {
                    continue;
                }
                String lower = rawName.toLowerCase(Locale.ROOT);
                for (String token : SUMMON_PET_NPC_CLASS_TOKENS) {
                    if (lower.contains(token)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    static boolean isHostileBossCandidate(String ns, String path, float maxHealth,
                                          boolean isRaider, boolean isEnemy,
                                          boolean isFriendlyCategory, boolean isAnimal,
                                          boolean isNonEnemyGolem, boolean isSummonOrPetOrNpc) {
        if (isSummonOrPetOrNpc || isAnimal || isNonEnemyGolem) {
            return false;
        }
        if (path != null && path.contains("boss") && (!isFriendlyCategory || isEnemy)) {
            return true;
        }
        if (isRaider && maxHealth >= 80.0F) {
            return true;
        }
        return (isEnemy || !isFriendlyCategory) && maxHealth >= 150.0F;
    }

    static boolean isAdultButcherLivestockId(String ns, String path, boolean isAnimalInstance, boolean fidaLoaded) {
        if (ns == null || path == null || path.isEmpty()) {
            return false;
        }
        if ("flavor_immersed_daily_argo".equals(ns)) {
            return "duck".equals(path) || "goose".equals(path) || "turkey".equals(path)
                    || "pigeon".equals(path) || "quail".equals(path)
                    || "bullfrog".equals(path) || "bamboorat".equals(path) || "bamboo_rat".equals(path);
        }
        if ("guaniao".equals(ns) || "touhoulittlemaid".equals(ns) || "irons_spellbooks".equals(ns)
                || "cataclysm".equals(ns) || "blocky_music".equals(ns)
                || "brainlayers".equals(ns) || "settlements".equals(ns)) {
            return false;
        }
        if ("twilightforest".equals(ns)) {
            return "boar".equals(path) || "bighorn_sheep".equals(path) || "bighorn".equals(path)
                    || "deer".equals(path) || "dwarf_rabbit".equals(path);
        }
        if ("minecraft".equals(ns)) {
            if ("cow".equals(path) || "mooshroom".equals(path) || "sheep".equals(path)
                    || "pig".equals(path) || "chicken".equals(path)
                    || "rabbit".equals(path) || "goat".equals(path)) {
                return true;
            }
            return fidaLoaded && ("donkey".equals(path) || "camel".equals(path) || "llama".equals(path));
        }
        return isAnimalInstance && !isExcludedNonLivestockPath(path);
    }

    static boolean isBreedableLivestockId(String ns, String path, boolean isAnimalInstance) {
        if (ns == null || path == null || path.isEmpty()) {
            return false;
        }
        if ("minecraft".equals(ns) && ("horse".equals(path) || "donkey".equals(path)
                || "mule".equals(path) || "camel".equals(path) || "llama".equals(path))) {
            return true;
        }
        return isAdultButcherLivestockId(ns, path, isAnimalInstance, true);
    }

    private static boolean isExcludedNonLivestockPath(String path) {
        for (String token : NON_LIVESTOCK_PATH_TOKENS) {
            if (path.contains(token)) {
                return true;
            }
        }
        // 排除泛用观赏鸟类，但放行东篱可宰杀食用禽类（quail/pigeon/turkey/duck/goose）
        return path.contains("bird")
                && !path.contains("duck") && !path.contains("goose")
                && !path.contains("turkey") && !path.contains("pigeon") && !path.contains("quail");
    }

    // =========================================================================
    // 3. 方块成熟度护栏（分类三：树苗/木盆/置地菜肴/盐田防误判）
    // =========================================================================

    static boolean isNonCropStageOrFruitBlockName(String blockId, String className) {
        String idLower = blockId == null ? "" : blockId.toLowerCase(Locale.ROOT);
        String clsLower = className == null ? "" : className.toLowerCase(Locale.ROOT);
        if (clsLower.contains("sapling") || clsLower.contains("propagule")) {
            return true;
        }
        for (String token : NON_CROP_BLOCK_CLASS_TOKENS) {
            if (clsLower.contains(token)) {
                return true;
            }
        }
        return idLower.endsWith("_sapling")
                || "flavor_immersed_daily:woodbasin".equals(idLower)
                || "flavor_immersed_daily_argo:salt_pan".equals(idLower)
                || "flavor_immersed_daily_argo:salt_pile".equals(idLower);
    }

    // =========================================================================
    // 4. 职业产出与成品判定（分类四：餐具/工具/非产出过滤 + 东篱海鲜/菜肴）
    // =========================================================================

    static boolean isExcludedNonOutputItem(String ns, String path) {
        if (ns == null || path == null || path.isEmpty()) {
            return false;
        }
        if (path.endsWith("_spawn_egg")) {
            return true;
        }
        if ("flavor_immersed_daily_argo".equals(ns)) {
            return path.startsWith("money_")
                    || path.startsWith("uav_")
                    || path.startsWith("rod_")
                    || path.startsWith("reel_")
                    || path.startsWith("line_")
                    || path.startsWith("float_")
                    || path.startsWith("sinker_")
                    || path.startsWith("bait_")
                    || path.startsWith("blank_silk_")
                    || path.startsWith("feed_")
                    || FIDA_EXCLUDED_UTILITY_IDS.contains(path);
        }
        if ("fidworkblock".equals(ns)) {
            return !("dish".equals(path) || "soup".equals(path));
        }
        if (FOOD_LIFESTYLE_NAMESPACES.contains(ns)) {
            return isUtilityMachineFurnitureOrBuilding(ns, path);
        }
        return false;
    }

    static boolean isSeafoodPath(String path) {
        return path.contains("fish") || path.contains("crab") || path.contains("shrimp")
                || path.contains("prawn") || path.contains("crayfish") || path.contains("lobster")
                || path.contains("turtle") || path.contains("eel") || path.contains("carp")
                || path.contains("bass") || path.contains("tuna") || path.contains("salmon")
                || path.contains("squid") || path.contains("clam") || path.contains("oyster")
                || path.contains("perch") || path.contains("trout") || path.contains("abalone")
                || path.contains("scallop") || path.contains("sea_cucumber") || path.contains("sea_urchin")
                || path.contains("snail") || path.contains("jellyfish") || path.contains("spoon_worm")
                || path.contains("loach") || path.contains("grouper") || path.contains("hairtail")
                || path.contains("pomfret") || path.contains("saury") || path.contains("sardine")
                || path.contains("redfin") || path.contains("rectum") || path.contains("tench")
                || path.contains("tilapia") || path.contains("pleco") || path.contains("goby")
                || path.contains("halfbeak") || path.contains("mackerel") || path.contains("pike")
                || path.contains("croaker");
    }

    static boolean isFidaSeafoodOutputPath(String path) {
        if (path == null || path.isEmpty()) {
            return false;
        }
        if ((path.startsWith("raw_") || path.endsWith("_bucket")) && isSeafoodPath(path)) {
            return true;
        }
        return "shrimp_meat".equals(path) || "dried_shrimp".equals(path)
                || "crab_roe".equals(path) || "fish_scale".equals(path)
                || "fish_bone".equals(path) || "fish_liver_oil".equals(path)
                || "deep_sea_fish_liver_oil".equals(path);
    }

    static boolean isFidaCookedDishPath(String path) {
        if (path == null || path.isEmpty()) {
            return false;
        }
        return path.startsWith("cooked_") || path.startsWith("roast_") || path.startsWith("roasted_")
                || path.startsWith("steamed_") || path.startsWith("braised_")
                || path.startsWith("boiled_") || path.startsWith("fried_")
                || path.startsWith("stir_fried_") || path.startsWith("pan_fried_")
                || path.startsWith("deep_fried_") || path.startsWith("chili_fried_")
                || path.startsWith("pickled_") || path.startsWith("spicy_")
                || path.startsWith("garlic_vermicelli_") || path.startsWith("sea_urchin_")
                || path.endsWith("_soup") || path.endsWith("_jam")
                || path.endsWith("_juice") || path.endsWith("_icecream") || path.endsWith("_popsicle")
                || "peking_roast_duck".equals(path) || "pot_stew_goose".equals(path)
                || "eel_rice".equals(path) || "tuna_sushi".equals(path)
                || "donkey_feast".equals(path) || "donkey_meat_burger".equals(path)
                || "typhoon_shelter_prawn".equals(path) || "scalded_prawns".equals(path)
                || "scallion_sea_cucumber".equals(path) || "salt_pepper_mantis_shrimp".equals(path)
                || "spoon_worm_rice".equals(path) || "razor_clam_scrambled_eggs".equals(path)
                || "vinegar_jellyfish_head".equals(path) || "oil_soaked_snail_slices".equals(path)
                || "cold_fish_skin".equals(path) || "sugar_roasted_chestnut".equals(path)
                || "chestnut_cake".equals(path) || "oozing_salted_duck_egg".equals(path)
                || "foie_gras".equals(path) || "peixian_dog_meat".equals(path)
                || "mackerel_pancake_roll".equals(path) || "stinky_swordfish".equals(path)
                || "shrimp_paste".equals(path) || "oyster_sauce".equals(path);
    }

    static boolean isEligibleCookedDishForXp(String ns, String path, boolean hasFoodOrFoodBlock) {
        if (ns == null || path == null || path.isEmpty()) {
            return false;
        }
        if ("kaleidoscope_tavern".equals(ns) || "kaleidoscope_dim_wine".equals(ns)
                || "kaleidoscope_world_liquor".equals(ns) || "kaleidoscope_flora".equals(ns)
                || "guaniao".equals(ns) || "wilderflowers".equals(ns)) {
            return false;
        }
        if ("fidworkblock".equals(ns)) {
            return "dish".equals(path) || "soup".equals(path)
                    || (hasFoodOrFoodBlock && !isNonFoodCookingUtensilOrDecor(ns, path));
        }
        if ("orients_hotpot_chronicles".equals(ns)) {
            return !isNonFoodCookingUtensilOrDecor(ns, path)
                    && (hasFoodOrFoodBlock || "tomato_soup_bowl".equals(path) || "three_fresh_soup_bowl".equals(path));
        }
        if (isNonFoodCookingUtensilOrDecor(ns, path) || isExcludedNonOutputItem(ns, path)) {
            return false;
        }
        return hasFoodOrFoodBlock || isKnownPlaceableDishPath(ns, path);
    }

    static boolean isEligibleBartendingOutputForXp(String ns, String path) {
        return ns != null && path != null && !path.isEmpty()
                && !isNonOutputBartendingUtensilOrDecor(ns, path);
    }

    static boolean isNonFoodCookingUtensilOrDecor(String ns, String path) {
        if ("minecraft".equals(ns)
                && ("bowl".equals(path) || "glass_bottle".equals(path) || "bucket".equals(path))) {
            return true;
        }
        if (path.startsWith("empty_") || path.endsWith("_empty") || path.startsWith("raw_")) {
            return true;
        }
        if ("flavor_immersed_daily".equals(ns) && path.startsWith("raw")) {
            return true;
        }
        if (isUtilityMachineFurnitureOrBuilding(ns, path)) {
            return true;
        }
        if ((path.contains("mould") || path.contains("mold")) && !isBakedMouldBread(path)) {
            return true;
        }
        return path.endsWith("_dough") || path.contains("_dough_")
                || path.endsWith("_paste") || path.endsWith("_paste_bucket")
                || "flour".equals(path) || path.endsWith("_flour")
                || "flour_bag".equals(path) || "flour_sieve".equals(path)
                || "silicone_paper".equals(path) || "paper_cup".equals(path) || "drink_cup".equals(path)
                || "salt".equals(path)
                || (path.startsWith("salt_") && !path.equals("salt_croissant")
                && !path.startsWith("salt_pepper_") && !path.equals("salt_yolk"));
    }

    private static boolean isUtilityMachineFurnitureOrBuilding(String ns, String path) {
        if (path.equals("chopsticks") || path.endsWith("_chopsticks")
                || path.equals("cup") || path.endsWith("_cup") || path.equals("teacup")
                || path.equals("plate") || path.endsWith("_plate") || path.equals("plate_holder")
                || path.equals("pot") || path.endsWith("_pot") || path.equals("hotpot")
                || path.equals("hot_pot") || path.endsWith("_hotpot") || path.endsWith("_hot_pot")
                || path.equals("skillet") || path.endsWith("_skillet")
                || path.equals("wok") || path.equals("pan") || path.endsWith("_pan")
                || path.equals("steamer") || path.endsWith("_steamer")
                || path.endsWith("_board") || path.equals("choppingboard")
                || path.equals("fryingpan") || path.equals("pressurecooker")
                || path.startsWith("steambox") || path.equals("meatmincer")
                || path.equals("mortar") || path.equals("squeezingmachine")
                || path.equals("dryer") || path.equals("teapot")
                || path.equals("jar") || path.equals("tableware") || path.equals("startbook")
                || path.equals("knife") || path.endsWith("_knife") || path.endsWith("knife")
                || path.contains("scissors") || path.equals("mallet")
                || path.endsWith("_axe") || path.equals("wugang_axe")
                || path.contains("coarsecloth")
                || path.equals("spatula") || path.equals("ladle") || path.equals("whisk")
                || path.equals("rolling_pin") || path.equals("strainer")
                || path.equals("stove") || path.endsWith("_stove")
                || path.equals("oven") || path.endsWith("_oven")
                || path.equals("holder") || path.endsWith("_holder")
                || path.equals("table") || path.endsWith("_table")
                || path.equals("cabinet") || path.endsWith("_cabinet")
                || path.equals("cupboard") || path.endsWith("_cupboard")
                || path.equals("rack") || path.endsWith("_rack")
                || path.equals("counter") || path.endsWith("_counter")
                || path.equals("basket") || path.endsWith("_basket")
                || path.equals("baking_tray") || path.equals("wooden_tray")
                || path.equals("wooden_bowl") || path.equals("glass_bottle")
                || path.equals("water_bottle") || path.equals("xp_bottle")
                || path.endsWith("book") || path.endsWith("handbook") || path.equals("menu")
                || path.equals("cloth_apron") || path.equals("high_stool") || path.equals("bench")
                || path.equals("blender") || path.equals("toaster")
                || path.equals("fermentation_box") || path.equals("fermentation_vessel")
                || path.equals("gristmill") || path.equals("mix_block")
                || path.endsWith("_tank") || path.endsWith("_ore") || path.endsWith("_concrete")
                || path.contains("computer") || path.equals("magnetic_plate") || path.equals("luminous_light_sign")
                || path.equals("fridge") || path.equals("woodbasin") || path.equals("trellis")
                || path.equals("bighook") || path.equals("eggbreakingmachine") || path.equals("agriculturalappraisalmachine")
                || path.equals("raddles") || path.equals("stone_lion")
                || path.equals("wrestling_gun") || path.equals("fairy_sparkler")
                || path.endsWith("lantern") || ("flavor_immersed_daily".equals(ns) && path.endsWith("_light"))
                || path.contains("couplet") || path.contains("doorpaper")
                || path.contains("windowpaper") || path.contains("firecracker") || path.contains("fireworks")
                || path.endsWith("_planks") || path.endsWith("_log") || path.endsWith("_wood")
                || path.endsWith("_stairs") || path.endsWith("_slab") || path.endsWith("_fence")
                || path.endsWith("_fence_gate") || path.endsWith("_button") || path.endsWith("_pressure_plate")
                || path.endsWith("_door") || path.endsWith("_trapdoor") || path.endsWith("_wall")
                || path.endsWith("_chair") || ("flavor_immersed_daily".equals(ns) && path.endsWith("chair"))
                || path.endsWith("_sofa") || path.endsWith("_stool") || path.endsWith("_bench")
                || path.endsWith("_bed") || path.endsWith("sapling") || path.endsWith("_leaves")
                || path.contains("fruiting_leaves") || path.contains("straw_bed")) {
            return true;
        }
        return (path.contains("mould") || path.contains("mold")) && !isBakedMouldBread(path) && !path.contains("_paste") && !path.contains("_dough");
    }

    private static boolean isBakedMouldBread(String path) {
        return "mould_cake".equals(path)
                || "mould_toast".equals(path)
                || "mould_pound_cake".equals(path)
                || "mould_cheese_cocoa_toast".equals(path);
    }

    private static boolean isKnownPlaceableDishPath(String ns, String path) {
        if ("minecraft".equals(ns) && "cake".equals(path)) {
            return true;
        }
        if (("kaleidoscope_cookery".equals(ns) || "farmersdelight".equals(ns) || "kaleidoscope_chinesefood".equals(ns))
                && (path.endsWith("_block") || path.endsWith("_feast") || path.endsWith("_medley") || path.endsWith("_pie"))
                && !path.contains("salt") && !path.contains("oil") && !path.contains("straw") && !path.contains("coal")) {
            return true;
        }
        if ("flavor_immersed_daily_argo".equals(ns) && isFidaCookedDishPath(path)) {
            return true;
        }
        return path.endsWith("_cake") && !path.endsWith("_cake_base") && !path.contains("paste");
    }

    static boolean isNonOutputBartendingUtensilOrDecor(String ns, String path) {
        if ("minecraft".equals(ns)
                && ("glass_bottle".equals(path) || "bowl".equals(path) || "bucket".equals(path)
                || "water_bucket".equals(path) || "milk_bucket".equals(path))) {
            return true;
        }
        if (path.startsWith("empty_") || path.endsWith("_empty") || path.startsWith("string_lights_")) {
            return true;
        }
        return path.equals("vinegar") || path.equals("glassware") || path.equals("shaker")
                || path.endsWith("_shaker") || path.equals("barrel") || path.endsWith("_barrel")
                || path.equals("keg") || path.endsWith("_keg")
                || path.equals("tap") || path.endsWith("_tap")
                || path.equals("pressing_tub") || path.equals("apple_press") || path.equals("freezer")
                || path.equals("holder") || path.endsWith("_holder")
                || path.equals("cup") || path.endsWith("_cup")
                || path.equals("chopsticks") || path.endsWith("_chopsticks")
                || path.equals("plate") || path.endsWith("_plate")
                || path.equals("cabinet") || path.endsWith("_cabinet")
                || path.equals("bar_counter") || path.equals("table") || path.endsWith("_table")
                || path.equals("trellis") || path.equals("rack") || path.endsWith("_rack")
                || path.equals("chalkboard") || path.equals("brass_heart")
                || path.equals("water_bottle") || path.equals("xp_bottle") || path.equals("molotov")
                || path.endsWith("_sofa") || path.endsWith("_stool")
                || path.endsWith("_painting") || path.endsWith("_sandwich_board")
                || path.endsWith("_lamp") || path.endsWith("_incense");
    }

    /**
     * 高水位线递增计算烹饪容器内新增成品数量：放入旧菜再取出时基线不回落，避免反复刷菜单烹饪经验。
     */
    static int advanceHighWaterCookDelta(Map<String, Integer> highWaterBaseline, Map<String, Integer> now) {
        int gainedDishes = 0;
        for (Map.Entry<String, Integer> e : now.entrySet()) {
            String key = e.getKey();
            if (!key.startsWith("COOK|")) {
                continue;
            }
            int prev = highWaterBaseline.getOrDefault(key, 0);
            if (e.getValue() > prev) {
                gainedDishes += (e.getValue() - prev);
                highWaterBaseline.put(key, e.getValue());
            }
        }
        return gainedDishes;
    }
}
