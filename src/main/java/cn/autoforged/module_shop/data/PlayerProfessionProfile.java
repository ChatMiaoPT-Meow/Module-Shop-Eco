package cn.autoforged.module_shop.data;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;

/**
 * 玩家的八大职业永久熟练度、每日专注点与近 7 日核验实业营收档案（挂在 {@link PlayerAccount} 下持久化）。
 */
public final class PlayerProfessionProfile {
    public static final int MAX_LEVEL = 100;

    /** 当前可用专注点（保留小数精度，避免老手单件 6.25 专注点被取整吞掉）。 */
    public double focusPoints = -1.0D;
    /** 上次自动恢复专注点的自然日（epochDay）。 */
    public long lastFocusRestoreDay = Long.MIN_VALUE;
    /** 出货/出售时是否启用专注点享受职业加价（玩家可在界面一键开关）。 */
    public boolean focusSellEnabled = true;

    /** 各职业永久等级与经验（key = professionId）。 */
    public final Map<String, Progress> professions = new LinkedHashMap<>();

    /**
     * 按游戏日记录的各职业官方核验真实出货营收（gameDay -> (professionId -> starCoins)）。
     * 只保留最近 14 个游戏日，用于核算近 7 日实业 IPO 资格与财报，存档体积极小。
     */
    public final Map<Long, Map<String, Long>> dailyVerifiedRevenue = new LinkedHashMap<>();

    public static final class Progress {
        public int level = 1;
        public long xp = 0L;
        public long totalXp = 0L;

        public CompoundTag save() {
            CompoundTag t = new CompoundTag();
            t.putInt("lv", level);
            t.putLong("xp", xp);
            t.putLong("totalXp", totalXp);
            return t;
        }

        public static Progress load(CompoundTag t) {
            Progress p = new Progress();
            if (t != null) {
                int maxLv = Math.max(effectiveMaxLevel(), 500);
                p.level = Math.max(1, Math.min(maxLv, t.getInt("lv")));
                p.xp = Math.max(0L, t.getLong("xp"));
                p.totalXp = Math.max(0L, t.getLong("totalXp"));
            }
            return p;
        }
    }

    /** 当前生效的最大职业等级（未加载自定义配置时回退默认 {@link #MAX_LEVEL}）。 */
    public static int effectiveMaxLevel() {
        cn.autoforged.module_shop.eco.profession.ProfessionConfig cfg =
                cn.autoforged.module_shop.eco.profession.ProfessionConfig.getIfLoaded();
        return cfg != null ? cfg.resolvedMaxLevel() : MAX_LEVEL;
    }

    /** 升到下一级所需经验：优先按管理员配置的每级经验表，未配置则走平滑递增公式。 */
    public static long xpRequiredForNextLevel(int currentLevel) {
        return xpRequiredForNextLevel(currentLevel,
                cn.autoforged.module_shop.eco.profession.ProfessionConfig.getIfLoaded());
    }

    public static long xpRequiredForNextLevel(int currentLevel,
                                              cn.autoforged.module_shop.eco.profession.ProfessionConfig cfg) {
        if (cfg != null) {
            return cfg.xpRequiredForNextLevel(currentLevel);
        }
        if (currentLevel >= MAX_LEVEL) {
            return 0L;
        }
        int lv = Math.max(1, currentLevel);
        return 50L + (long) lv * 10L + (long) lv * lv / 2L;
    }

    public Progress getOrCreate(String professionId) {
        return professions.computeIfAbsent(professionId, k -> new Progress());
    }

    public int getLevel(String professionId) {
        Progress p = professions.get(professionId);
        int maxLv = effectiveMaxLevel();
        return p == null ? 1 : Math.max(1, Math.min(maxLv, p.level));
    }

    public long getXp(String professionId) {
        Progress p = professions.get(professionId);
        return p == null ? 0L : p.xp;
    }

    /** 增加职业经验并自动处理连升多级，返回升级后的等级。 */
    public int addXp(String professionId, long amount) {
        return addXp(professionId, amount,
                cn.autoforged.module_shop.eco.profession.ProfessionConfig.getIfLoaded());
    }

    public int addXp(String professionId, long amount,
                     cn.autoforged.module_shop.eco.profession.ProfessionConfig cfg) {
        int maxLv = cfg != null ? cfg.resolvedMaxLevel() : MAX_LEVEL;
        if (professionId == null || professionId.isBlank() || amount <= 0L) {
            Progress p = professions.get(professionId);
            return p == null ? 1 : Math.max(1, Math.min(maxLv, p.level));
        }
        Progress p = getOrCreate(professionId);
        p.totalXp = Math.min(Long.MAX_VALUE - amount, p.totalXp) + amount;
        if (p.level >= maxLv) {
            p.level = maxLv;
            p.xp = 0L;
            return maxLv;
        }
        p.xp += amount;
        while (p.level < maxLv) {
            long req = xpRequiredForNextLevel(p.level, cfg);
            if (req <= 0L || p.xp < req) {
                break;
            }
            p.xp -= req;
            p.level++;
        }
        if (p.level >= maxLv) {
            p.level = maxLv;
            p.xp = 0L;
        }
        return p.level;
    }

    /**
     * 每日恢复专注点（支持跨多天离线自动按离线天数补满至 maxCap 上限，最多攒 3 天不亏！）。
     */
    public void refreshFocusIfNeeded(long currentDay, int dailyRestore, int maxCap) {
        double cap = Math.max(0, maxCap);
        if (this.focusPoints < 0.0D || this.lastFocusRestoreDay == Long.MIN_VALUE) {
            // 新玩家首次初始化直接给一天启动专注点
            this.focusPoints = Math.min(cap, Math.max(0, dailyRestore));
            this.lastFocusRestoreDay = currentDay;
            return;
        }
        if (currentDay > this.lastFocusRestoreDay) {
            long daysElapsed = Math.min(30L, currentDay - this.lastFocusRestoreDay);
            this.focusPoints = Math.min(cap, this.focusPoints + (double) daysElapsed * Math.max(0, dailyRestore));
            this.lastFocusRestoreDay = currentDay;
        } else if (this.focusPoints > cap) {
            this.focusPoints = cap;
        }
    }

    /** 记录一笔官方核验的真实本职出货营收（供股市 IPO 与全服紧缺度统计使用）。 */
    public void recordVerifiedRevenue(long gameDay, String professionId, long starCoins) {
        if (professionId == null || professionId.isBlank() || starCoins <= 0L) {
            return;
        }
        Map<String, Long> byProf = dailyVerifiedRevenue.computeIfAbsent(gameDay, k -> new HashMap<>());
        byProf.merge(professionId, starCoins, (a, b) -> Math.min(Long.MAX_VALUE - a, b) + a);
        // 清理 14 个游戏日之前的旧记录
        dailyVerifiedRevenue.keySet().removeIf(d -> gameDay - d > 14L);
    }

    /** 汇总过去 windowDays（如 7）个游戏日内某职业的核验真实出货额。 */
    public long sumVerifiedRevenue(long currentGameDay, String professionId, int windowDays) {
        if (professionId == null || professionId.isBlank()) {
            return 0L;
        }
        long sum = 0L;
        for (Map.Entry<Long, Map<String, Long>> e : dailyVerifiedRevenue.entrySet()) {
            long day = e.getKey();
            if (currentGameDay - day >= 0L && currentGameDay - day < Math.max(1, windowDays)) {
                long v = e.getValue().getOrDefault(professionId, 0L);
                sum = Math.min(Long.MAX_VALUE - sum, v) + sum;
            }
        }
        return sum;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putDouble("focus", focusPoints);
        tag.putLong("focusDay", lastFocusRestoreDay);
        tag.putBoolean("focusOn", focusSellEnabled);

        CompoundTag profTag = new CompoundTag();
        for (Map.Entry<String, Progress> e : professions.entrySet()) {
            profTag.put(e.getKey(), e.getValue().save());
        }
        tag.put("profs", profTag);

        CompoundTag revTag = new CompoundTag();
        for (Map.Entry<Long, Map<String, Long>> dayEntry : dailyVerifiedRevenue.entrySet()) {
            CompoundTag oneDay = new CompoundTag();
            for (Map.Entry<String, Long> pe : dayEntry.getValue().entrySet()) {
                if (pe.getValue() != null && pe.getValue() > 0L) {
                    oneDay.putLong(pe.getKey(), pe.getValue());
                }
            }
            if (!oneDay.isEmpty()) {
                revTag.put(Long.toString(dayEntry.getKey()), oneDay);
            }
        }
        tag.put("verifiedRev", revTag);
        return tag;
    }

    public static PlayerProfessionProfile load(CompoundTag tag) {
        PlayerProfessionProfile profile = new PlayerProfessionProfile();
        if (tag == null || tag.isEmpty()) {
            return profile;
        }
        profile.focusPoints = tag.contains("focus") ? tag.getDouble("focus") : -1.0D;
        profile.lastFocusRestoreDay = tag.contains("focusDay") ? tag.getLong("focusDay") : Long.MIN_VALUE;
        profile.focusSellEnabled = !tag.contains("focusOn") || tag.getBoolean("focusOn");

        CompoundTag profTag = tag.getCompound("profs");
        for (String key : profTag.getAllKeys()) {
            profile.professions.put(key, Progress.load(profTag.getCompound(key)));
        }
        CompoundTag revTag = tag.getCompound("verifiedRev");
        for (String dayStr : revTag.getAllKeys()) {
            try {
                long day = Long.parseLong(dayStr);
                CompoundTag oneDay = revTag.getCompound(dayStr);
                Map<String, Long> map = new HashMap<>();
                for (String pk : oneDay.getAllKeys()) {
                    long val = oneDay.getLong(pk);
                    if (val > 0L) {
                        map.put(pk, val);
                    }
                }
                if (!map.isEmpty()) {
                    profile.dailyVerifiedRevenue.put(day, map);
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return profile;
    }
}
