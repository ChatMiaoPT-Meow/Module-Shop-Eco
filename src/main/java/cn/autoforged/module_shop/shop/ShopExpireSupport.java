package cn.autoforged.module_shop.shop;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 摊位 / 求购下架时间配置、退件拆堆、活动日定时校验与 TOML 原子回写的纯逻辑辅助类。
 */
public final class ShopExpireSupport {
    public static final int MAX_EXPIRE_AMOUNT = 100_000;

    private ShopExpireSupport() {
    }

    public static String normalizeExpireMode(String raw) {
        if (raw == null) {
            return "permanent";
        }
        String clean = raw.trim().toLowerCase(Locale.ROOT);
        if ("game".equals(clean) || "real".equals(clean)) {
            return clean;
        }
        return "permanent";
    }

    public static boolean isExpireActive(String mode, int amount) {
        return !"permanent".equals(normalizeExpireMode(mode)) && amount > 0;
    }

    public static Integer parseExpireAmount(String mode, String rawAmount, int fallbackAmount) {
        String cleanMode = normalizeExpireMode(mode);
        String trimmed = rawAmount == null ? "" : rawAmount.trim();
        if ("permanent".equals(cleanMode) && trimmed.isEmpty()) {
            return Math.max(0, Math.min(MAX_EXPIRE_AMOUNT, fallbackAmount));
        }
        int parsed;
        try {
            parsed = Integer.parseInt(trimmed);
        } catch (NumberFormatException ex) {
            return null;
        }
        if (parsed < 0 || parsed > MAX_EXPIRE_AMOUNT) {
            return null;
        }
        return parsed;
    }

    public static boolean shouldResetExpireTimestamps(String oldMode, int oldAmount, String newMode, int newAmount) {
        if (!isExpireActive(newMode, newAmount)) {
            return false;
        }
        if (!isExpireActive(oldMode, oldAmount)) {
            return true;
        }
        return !normalizeExpireMode(oldMode).equals(normalizeExpireMode(newMode));
    }

    public static boolean shouldAdvanceDailyDay(long currentDay, long lastDay) {
        return currentDay > lastDay;
    }

    public static boolean canGrantActivityDayAtHour(int scheduledHour, int nowHour, boolean catchUp, boolean forOffline) {
        if (scheduledHour < 0) {
            return true;
        }
        if (nowHour < scheduledHour) {
            return false;
        }
        return nowHour == scheduledHour || catchUp || forOffline;
    }

    public static boolean canGrantActivityOfflineAtHour(int scheduledHour, int nowHour) {
        return scheduledHour < 0 || nowHour >= scheduledHour;
    }

    public static int[] splitStackCounts(int totalCount, int maxStackSize) {
        if (totalCount <= 0) {
            return new int[0];
        }
        int per = Math.max(1, maxStackSize);
        int chunks = (totalCount + per - 1) / per;
        int[] out = new int[chunks];
        int left = totalCount;
        for (int i = 0; i < chunks; i++) {
            int take = Math.min(per, left);
            out[i] = take;
            left -= take;
        }
        return out;
    }

    public static List<String> patchTomlSectionKeys(List<String> lines, String sectionName,
            Map<String, String> formattedValues) {
        List<String> result = new ArrayList<>(lines == null ? List.of() : lines);
        if (sectionName == null || sectionName.isBlank() || formattedValues == null || formattedValues.isEmpty()) {
            return result;
        }
        String targetHeader = "[" + sectionName.trim() + "]";
        Map<String, String> remaining = new LinkedHashMap<>(formattedValues);
        boolean inTarget = false;
        boolean sectionFound = false;
        int sectionEndIdx = result.size();
        String sectionIndent = "\t";

        for (int i = 0; i < result.size(); i++) {
            String rawLine = result.get(i);
            String trimmed = rawLine.trim();
            if (trimmed.startsWith("[") && trimmed.endsWith("]") && !trimmed.startsWith("[[")) {
                if (inTarget) {
                    sectionEndIdx = i;
                    inTarget = false;
                    break;
                }
                if (targetHeader.equals(trimmed)) {
                    inTarget = true;
                    sectionFound = true;
                }
                continue;
            }
            if (!inTarget || trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            int eq = trimmed.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            String key = trimmed.substring(0, eq).trim();
            int keyStart = rawLine.indexOf(key);
            if (keyStart >= 0) {
                sectionIndent = rawLine.substring(0, keyStart);
            }
            if (remaining.containsKey(key)) {
                String value = remaining.remove(key);
                result.set(i, sectionIndent + key + " = " + value);
            }
        }

        if (sectionFound && !remaining.isEmpty()) {
            List<String> inserts = new ArrayList<>(remaining.size());
            for (Map.Entry<String, String> entry : remaining.entrySet()) {
                inserts.add(sectionIndent + entry.getKey() + " = " + entry.getValue());
            }
            result.addAll(sectionEndIdx, inserts);
        } else if (!sectionFound) {
            if (!result.isEmpty() && !result.get(result.size() - 1).isEmpty()) {
                result.add("");
            }
            result.add(targetHeader);
            for (Map.Entry<String, String> entry : remaining.entrySet()) {
                result.add("\t" + entry.getKey() + " = " + entry.getValue());
            }
        }
        return result;
    }

    public static boolean writeTomlSectionAtomic(Path tomlPath, String sectionName,
            Map<String, String> formattedValues) {
        if (tomlPath == null || !Files.exists(tomlPath)) {
            return false;
        }
        Path parent = tomlPath.getParent();
        Path temp = null;
        try {
            List<String> lines = Files.readAllLines(tomlPath, StandardCharsets.UTF_8);
            List<String> patched = patchTomlSectionKeys(lines, sectionName, formattedValues);
            String body = String.join(System.lineSeparator(), patched) + System.lineSeparator();
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            if (parent != null) {
                temp = Files.createTempFile(parent, "module_shop-cfg-", ".tmp");
                Files.write(temp, bytes);
                try {
                    Files.move(temp, tomlPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                } catch (IOException ex) {
                    Files.move(temp, tomlPath, StandardCopyOption.REPLACE_EXISTING);
                }
                temp = null;
            } else {
                Files.write(tomlPath, bytes);
            }
            return true;
        } catch (Throwable ignored) {
            return false;
        } finally {
            if (temp != null) {
                try {
                    Files.deleteIfExists(temp);
                } catch (IOException ignored) {
                }
            }
        }
    }
}
