package cn.autoforged.module_shop.eco.profession;

import java.util.Map;

/**
 * 纯 Java 职业产出白名单与高水位线结算辅助：
 * 将餐具/空杯/装饰方块排除在烹饪与调酒经验外，并防止容器进出反复刷经验。
 */
final class ProfessionOutputFilter {

    private ProfessionOutputFilter() {}

    static boolean isEligibleCookedDishForXp(String ns, String path, boolean hasFoodComponent) {
        if (ns == null || path == null || path.isEmpty()) {
            return false;
        }
        if ("orients_hotpot_chronicles".equals(ns)) {
            return hasFoodComponent || "tomato_soup_bowl".equals(path) || "three_fresh_soup_bowl".equals(path);
        }
        if ("kaleidoscope_tavern".equals(ns)) {
            return false;
        }
        return !isNonFoodCookingUtensilOrDecor(ns, path);
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
        return path.equals("chopsticks") || path.endsWith("_chopsticks")
                || path.equals("cup") || path.endsWith("_cup") || path.equals("teacup")
                || path.equals("plate") || path.endsWith("_plate") || path.equals("plate_holder")
                || path.equals("pot") || path.endsWith("_pot") || path.equals("hotpot")
                || path.equals("hot_pot") || path.endsWith("_hotpot") || path.endsWith("_hot_pot")
                || path.equals("skillet") || path.endsWith("_skillet")
                || path.equals("wok") || path.equals("pan") || path.endsWith("_pan")
                || path.equals("steamer") || path.endsWith("_steamer")
                || path.endsWith("_board") || path.equals("knife") || path.endsWith("_knife")
                || path.equals("spatula") || path.equals("ladle") || path.equals("whisk")
                || path.equals("rolling_pin") || path.equals("strainer")
                || path.equals("stove") || path.endsWith("_stove")
                || path.equals("oven") || path.endsWith("_oven")
                || path.equals("holder") || path.endsWith("_holder")
                || path.equals("table") || path.endsWith("_table")
                || path.equals("cabinet") || path.endsWith("_cabinet")
                || path.equals("rack") || path.endsWith("_rack")
                || path.equals("counter") || path.endsWith("_counter")
                || path.equals("baking_tray") || path.equals("wooden_tray")
                || path.equals("wooden_bowl") || path.equals("glass_bottle")
                || path.equals("water_bottle") || path.equals("xp_bottle");
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
