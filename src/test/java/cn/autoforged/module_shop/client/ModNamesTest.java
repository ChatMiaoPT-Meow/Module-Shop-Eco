package cn.autoforged.module_shop.client;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 模组名称自动汉化与热门 Mod 中文名包加载逻辑测试。
 */
class ModNamesTest {

    @Test
    @DisplayName("1. 内置热门 Mod 中文名包可正常解析且包含核心热门模组")
    void bundledPackLoadsAndContainsPopularMods() throws Exception {
        Map<String, String> parsed = new HashMap<>();
        Path resourceFile = Path.of("src/main/resources/assets/module_shop/mod_names_zh_cn.json");
        if (Files.isRegularFile(resourceFile)) {
            try (Reader reader = Files.newBufferedReader(resourceFile, StandardCharsets.UTF_8)) {
                ModNames.parseJsonMap(reader, parsed, "test-file");
            }
        } else {
            try (InputStream in = ModNames.class.getResourceAsStream("/assets/module_shop/mod_names_zh_cn.json")) {
                assertNotNull(in, "必须能在资源文件或 classpath 中找到 mod_names_zh_cn.json");
                try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                    ModNames.parseJsonMap(reader, parsed, "test-classpath");
                }
            }
        }

        assertTrue(parsed.size() >= 150, "内置热门模组词库应至少覆盖 150 个模组，当前: " + parsed.size());
        for (var entry : parsed.entrySet()) {
            assertFalse(entry.getKey().isBlank(), "modid 键不能为空");
            assertFalse(entry.getValue().isBlank(), "译名值不能为空: " + entry.getKey());
        }
        for (String expected : List.of(
                "minecraft", "create", "farmersdelight", "mekanism", "ae2",
                "irons_spellbooks", "kaleidoscope_cookery", "cataclysm", "jei",
                "ars_nouveau", "twilightforest", "module_shop")) {
            assertTrue(parsed.containsKey(expected), "缺少热门模组中文名: " + expected);
            assertTrue(ModNames.containsCjk(parsed.get(expected)), "中文名应包含汉字: " + expected + " -> " + parsed.get(expected));
        }
    }

    @Test
    @DisplayName("2. 译名解析优先级：外部覆盖 > 显式 lang > 含中文 ModMenu > 内置词库 > ModList 原名 > namespace")
    void resolutionPriorityChineseAndEnglish() {
        Map<String, String> external = new HashMap<>();
        Map<String, String> pack = new HashMap<>();
        pack.put("create", "机械动力");
        pack.put("ae2", "应用能源2");
        pack.put("only_pack", "仅词库模组");

        Map<String, String> raw = Map.of(
                "create", "Create",
                "ae2", "Applied Energistics 2",
                "modmenu_cn", "ModMenuRaw",
                "modmenu_en", "Some English Mod",
                "untranslated", "Untranslated Mod"
        );
        Map<String, String> i18n = new HashMap<>();
        i18n.put("gui.module_shop.mod_name.explicit_mod", "手动覆盖译名");
        i18n.put("modmenu.nameTranslation.modmenu_cn", "本地语言包汉化名");
        i18n.put("modmenu.nameTranslation.create", "Create (English ModMenu)");

        external.put("ae2", "玩家自定义AE2");

        assertEquals("玩家自定义AE2",
                ModNames.resolve("ae2", true, external, pack, raw::get, i18n::get));
        assertEquals("手动覆盖译名",
                ModNames.resolve("explicit_mod", true, external, pack, raw::get, i18n::get));
        assertEquals("本地语言包汉化名",
                ModNames.resolve("modmenu_cn", true, external, pack, raw::get, i18n::get));
        // 英文版 modmenu.nameTranslation 不应压过中文词库
        assertEquals("机械动力",
                ModNames.resolve("create", true, external, pack, raw::get, i18n::get));
        assertEquals("仅词库模组",
                ModNames.resolve("only_pack", true, external, pack, raw::get, i18n::get));
        assertEquals("Untranslated Mod",
                ModNames.resolve("untranslated", true, external, pack, raw::get, i18n::get));
        assertEquals("unknown_ns",
                ModNames.resolve("unknown_ns", true, external, pack, raw::get, i18n::get));

        // 英文客户端模式下不强行套用中文内置词库
        assertEquals("Create (English ModMenu)",
                ModNames.resolve("create", false, external, pack, raw::get, i18n::get));
        assertEquals("Applied Energistics 2",
                ModNames.resolve("ae2", false, Map.of(), pack, raw::get, i18n::get));
    }
}
