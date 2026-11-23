package cn.autoforged.module_shop.client.screen.shop;

import cn.autoforged.module_shop.client.ClickSound;
import cn.autoforged.module_shop.client.ClientShopData;
import cn.autoforged.module_shop.client.screen.IOStyle;
import cn.autoforged.module_shop.client.ui.render.Draw;
import cn.autoforged.module_shop.client.ui.render.Text;
import cn.autoforged.module_shop.client.ui.v2.ListTable;
import cn.autoforged.module_shop.client.ui.v2.V2;
import cn.autoforged.module_shop.client.ui.widget.SkiaTextField;
import io.github.humbleui.skija.Canvas;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.network.chat.Component;

/**
 * 职业专精与专注点面板（UI v2）：
 * 顶部工具栏（标题 + 专注精销开关） + 概览条（可用专注点 / 主力职业 / 官方总钱箱 / 今日钱箱流转 / 近7日核验营收）
 * + 动态激活职业列表（图标 · 职业名与紧缺标 · 等级进度条 · 专注加价 · 单件专注消耗 · 近7日实业出货 · 经验途径说明）。
 */
public final class ProfessionPage implements ShopPage {
    private static final float OVERVIEW_H = 40.0F;
    private static String query = "";
    private SkiaTextField search;
    private List<ClientShopData.ProfessionView> shown = List.of();
    private static final ListTable TABLE = new ListTable();
    private float[] focusBtnRect;
    private float[] configBtnRect;

    @Override
    public String id() {
        return "profession";
    }

    @Override
    public void init(ShopHost host) {
        search = new SkiaTextField(host.screen(), host.font(), 48)
                .placeholder(tr("gui.module_shop.search.placeholder"))
                .clearable(true)
                .fontSize(V2.FS_S)
                .onChange(t -> {
                    query = t == null ? "" : t;
                    TABLE.scrollToTop();
                });
        search.setValue(query);
        host.registerInput(search.editBox());
    }

    private static String tr(String key, Object... args) {
        return Component.translatable(key, args).getString();
    }

    private static boolean over(PageFrame f, float x, float y, float w, float h) {
        return f.mouseX() >= x && f.mouseX() < x + w && f.mouseY() >= y && f.mouseY() < y + h;
    }

    @Override
    public void render(Canvas c, PageFrame f) {
        float x = f.x();
        float y = f.y();
        if (!ClientShopData.ecoProfEnabled) {
            if (search != null) {
                search.visible(false);
            }
            V2.empty(c, tr("gui.module_shop.profession.disabled"), x, y, f.w(), f.h());
            return;
        }
        List<ClientShopData.ProfessionView> all = ClientShopData.professions;
        List<ClientShopData.ProfessionView> nextShown = new ArrayList<>();
        for (ClientShopData.ProfessionView pv : all) {
            if (query.isBlank() || PageKit.matchesQuery(query, pv.localizedName(), pv.id, pv.localizedDesc())) {
                nextShown.add(pv);
            }
        }
        shown = nextShown;
        drawToolbar(c, f);
        drawOverview(c, x, y + V2.BAR, f.w());
        float top = y + V2.BAR + OVERVIEW_H;
        float bottom = y + f.h() - V2.FOOT;
        drawTable(c, f, x, top, f.w(), bottom - top, all.isEmpty());
        String footLeft = query.isBlank()
                ? tr("gui.module_shop.v2.profession.count", all.size())
                : tr("gui.module_shop.v2.showing", TABLE.firstVisible(), TABLE.lastVisible(), shown.size());
        V2.foot(c, x, bottom, f.w(), footLeft, tr("gui.module_shop.v2.profession.hint"));
    }

    private void drawToolbar(Canvas c, PageFrame f) {
        float x = f.x();
        float cy = f.y() + V2.BAR / 2;
        float by = cy - V2.CTRL / 2;
        Draw.hairline(c, x, f.y() + V2.BAR - 0.5F, f.w(), IOStyle.BORDER_DIM);
        float tw = V2.bold(c, tr("tab.module_shop.profession"), V2.FS_T, IOStyle.TEXT, x + 8, cy, 96, Text.Align.LEFT);

        float right = x + f.w() - 8;
        configBtnRect = null;
        if (ClientShopData.canAdmin) {
            String cfgText = cn.autoforged.module_shop.client.ClientHomeCurrencyState.tr(
                    "gui.module_shop.profession.config_btn", "专精配置");
            float cw = V2.buttonWidth(cfgText);
            right -= cw;
            V2.button(c, cfgText, V2.Btn.NORMAL, right, by, cw, V2.CTRL, over(f, right, by, cw, V2.CTRL) ? 1 : 0);
            configBtnRect = new float[]{right, by, cw, V2.CTRL};
            right -= 5;
        }
        String toggleText = tr(ClientShopData.focusSellEnabled
                ? "gui.module_shop.v2.profession.focus_on"
                : "gui.module_shop.v2.profession.focus_off");
        float bw = V2.buttonWidth(toggleText);
        right -= bw;
        V2.button(c, toggleText, ClientShopData.focusSellEnabled ? V2.Btn.PRIMARY : V2.Btn.NORMAL,
                right, by, bw, V2.CTRL, over(f, right, by, bw, V2.CTRL) ? 1 : 0);
        focusBtnRect = new float[]{right, by, bw, V2.CTRL};

        float sx = x + 8 + tw + 10;
        float avail = right - 8 - sx;
        float afterSearch = sx;
        if (search != null) {
            if (avail >= 52.0F) {
                float sw = Math.min(145.0F, Math.max(52.0F, avail >= 220.0F ? 116.0F : avail));
                search.visible(true).bounds(sx, by, sw, V2.CTRL);
                search.render(c, f.mouseX(), f.mouseY(), f.dtMs());
                afterSearch = sx + sw + 8.0F;
            } else {
                search.visible(false);
            }
        }
        float infoW = right - 8 - afterSearch;
        if (infoW >= 60.0F) {
            V2.text(c, tr("gui.module_shop.v2.profession.daily_restore", ClientShopData.focusDaily, ClientShopData.focusMax),
                    V2.FS_XS, IOStyle.TEXT_2, afterSearch, cy, infoW, Text.Align.RIGHT);
        }
    }

    private void drawOverview(Canvas c, float x, float y, float w) {
        Draw.fill(c, x, y, w, OVERVIEW_H, 0, Draw.alpha(IOStyle.CARD, 0.6F));
        Draw.hairline(c, x, y + OVERVIEW_H - 0.5F, w, IOStyle.BORDER_DIM);

        String topJobName = "—";
        long totalWeekRev = 0L;
        int bestLv = 0;
        for (ClientShopData.ProfessionView pv : ClientShopData.professions) {
            totalWeekRev += pv.weekRev;
            if (pv.level > bestLv) {
                bestLv = pv.level;
                topJobName = pv.localizedName() + " Lv." + pv.level;
            }
        }
        int healthPct = (int) Math.round(ClientShopData.treasuryHealth * 100.0D);

        String[][] cells = {
                {tr("gui.module_shop.v2.profession.focus_now"),
                        ClientShopData.focusPoints + " / " + ClientShopData.focusMax, ""},
                {tr("gui.module_shop.v2.profession.top_job"), topJobName,
                        tr("gui.module_shop.v2.profession.no_lock")},
                {tr("gui.module_shop.v2.profession.treasury"), V2.money(ClientShopData.treasuryBalance),
                        tr("gui.module_shop.v2.profession.health", healthPct)},
                {tr("gui.module_shop.v2.profession.treasury_flow"),
                        "+" + V2.money(ClientShopData.treasuryInToday) + " / -" + V2.money(ClientShopData.treasuryOutToday),
                        tr("gui.module_shop.v2.profession.today_flow")},
                {tr("gui.module_shop.v2.profession.week_rev"), V2.money(totalWeekRev),
                        tr("gui.module_shop.v2.profession.ipo_ready")}
        };

        float cw = w / cells.length;
        for (int i = 0; i < cells.length; i++) {
            float cx = x + i * cw;
            if (i > 0) {
                Draw.fill(c, cx, y + 7, 0.5F, OVERVIEW_H - 14, 0, IOStyle.BORDER_DIM);
            }
            V2.text(c, cells[i][0], V2.FS_XS, IOStyle.TEXT_3, cx + 9, y + 10, cw - 14, Text.Align.LEFT);
            int valColor = i == 0 ? IOStyle.BLUE : (i == 2 ? IOStyle.ORANGE : IOStyle.TEXT);
            V2.bold(c, cells[i][1], V2.FS, valColor, cx + 9, y + 21, cw - 14, Text.Align.LEFT);
            if (i == 0) {
                float ratio = ClientShopData.focusPoints / (float) Math.max(1, ClientShopData.focusMax);
                V2.bar(c, cx + 9, y + 31, cw - 22, ratio, IOStyle.BLUE);
            } else if (!cells[i][2].isEmpty()) {
                V2.text(c, cells[i][2], V2.FS_XS, IOStyle.TEXT_3, cx + 9, y + 31, cw - 14, Text.Align.LEFT);
            }
        }
    }

    private void drawTable(Canvas c, PageFrame f, float x, float y, float w, float h, boolean totalEmpty) {
        float[][] cols = V2.columnsMin(x, w, 10, 6,
                new float[]{14, 104, 118, 64, 70, 78, 0},
                new float[]{14, 72, 86, 48, 52, 58, 0});
        String[] heads = {
                "",
                tr("gui.module_shop.v2.col.prof_name"),
                tr("gui.module_shop.v2.col.prof_level"),
                tr("gui.module_shop.v2.col.prof_bonus"),
                tr("gui.module_shop.v2.col.prof_focus_cost"),
                tr("gui.module_shop.v2.col.prof_week_rev"),
                tr("gui.module_shop.v2.col.prof_desc")
        };
        Text.Align[] al = {
                Text.Align.LEFT, Text.Align.LEFT, Text.Align.LEFT,
                Text.Align.RIGHT, Text.Align.RIGHT, Text.Align.RIGHT, Text.Align.LEFT
        };
        TABLE.layout(x, y, w, h, cols, heads, al, null);
        if (shown.isEmpty()) {
            V2.tableHead(c, x, y, w, cols, heads, al, -1, false);
            V2.empty(c, tr(totalEmpty ? "gui.module_shop.profession.disabled" : "gui.module_shop.search.empty"),
                    x, y + V2.HEAD, w, h - V2.HEAD);
            return;
        }
        List<ClientShopData.ProfessionView> rows = shown;
        TABLE.render(c, f.host().items(), f.mouseX(), f.mouseY(), f.dtMs(), rows.size(), (cv, row, ry, hover) -> {
            ClientShopData.ProfessionView pv = rows.get(row);
            float cy = ry + V2.ROW / 2;
            f.host().items().add(pv.icon, cols[0][0], ry + (V2.ROW - V2.ICON) / 2, V2.ICON, false);

            String scarceStr = pv.scarcityPct >= 2.0D
                    ? tr("gui.module_shop.v2.profession.scarce", Math.round(pv.scarcityPct)) : null;
            float tagReserve = scarceStr != null ? (V2.tagWidth(scarceStr) + 4.0F) : 0.0F;
            float nameMax = Math.max(16.0F, cols[1][1] - tagReserve);
            float nw = V2.bold(cv, pv.localizedName(), V2.FS, IOStyle.TEXT, cols[1][0], cy, nameMax, Text.Align.LEFT);
            if (scarceStr != null) {
                V2.tag(cv, scarceStr, IOStyle.ORANGE, cols[1][0] + Math.min(nw, nameMax) + 4, cy);
            }

            // 等级 + 经验条
            String lvText = "Lv." + pv.level;
            float lvMaxW = Math.min(Math.max(28.0F, V2.width(lvText, V2.FS_S, true) + 1.0F), cols[2][1] * 0.48F);
            float lvw = V2.bold(cv, lvText, V2.FS_S, IOStyle.BLUE, cols[2][0], cy, lvMaxW, Text.Align.LEFT);
            float barX = cols[2][0] + Math.min(lvw, lvMaxW) + 4;
            float barW = Math.max(20.0F, cols[2][1] - Math.min(lvw, lvMaxW) - 8);
            float ratio = pv.nextXp <= 0 ? 1.0F : Math.min(1.0F, pv.xp / (float) pv.nextXp);
            V2.bar(cv, barX, cy + 3.5F, barW, ratio, pv.level >= ClientShopData.profMaxLevel ? IOStyle.ORANGE : IOStyle.BLUE);
            String xpStr = pv.nextXp <= 0 ? "MAX" : (pv.xp + "/" + pv.nextXp);
            V2.text(cv, xpStr, V2.FS_XS, IOStyle.TEXT_3, barX, cy - 3.5F, barW + 0.5F, Text.Align.RIGHT);

            // 专注加价
            V2.bold(cv, String.format(Locale.ROOT, "+%.1f%%", pv.bonusPct), V2.FS,
                    ClientShopData.focusSellEnabled ? IOStyle.GREEN : IOStyle.TEXT_3,
                    cols[3][0], cy, cols[3][1] + 0.5F, Text.Align.RIGHT);

            // 单件消耗专注
            V2.text(cv, String.format(Locale.ROOT, "%.1f", pv.focusCost), V2.FS_S, IOStyle.TEXT_2,
                    cols[4][0], cy, cols[4][1] + 0.5F, Text.Align.RIGHT);

            // 近 7 日出货营收
            V2.text(cv, pv.weekRev > 0 ? V2.money(pv.weekRev) : "—", V2.FS_S,
                    pv.weekRev > 0 ? IOStyle.ORANGE : IOStyle.TEXT_3,
                    cols[5][0], cy, cols[5][1] + 0.5F, Text.Align.RIGHT);

            // 说明文字（避让滚动条并支持悬停完整提示）
            float descGutter = TABLE.scroll().canScroll() ? 12.0F : 0.0F;
            V2.text(cv, pv.localizedDesc(), V2.FS_XS, IOStyle.TEXT_2, cols[6][0], cy,
                    Math.max(16.0F, cols[6][1] - descGutter), Text.Align.LEFT);
            if (hover > 0.5F && !pv.localizedDesc().isEmpty()) {
                f.host().textTooltip(Component.literal(pv.localizedName() + "：" + pv.localizedDesc()));
            }
        });
    }

    @Override
    public boolean mouseClicked(PageFrame f, double mx, double my, int button) {
        if (search != null && search.isVisible() && search.mouseClicked(mx, my, button)) {
            return true;
        }
        if (search != null && search.isFocused()) {
            search.blur();
        }
        if (button != 0) {
            return false;
        }
        if (configBtnRect != null && mx >= configBtnRect[0] && mx < configBtnRect[0] + configBtnRect[2]
                && my >= configBtnRect[1] && my < configBtnRect[1] + configBtnRect[3]) {
            ClickSound.play();
            f.host().action("profession_config", null);
            return true;
        }
        if (focusBtnRect != null && mx >= focusBtnRect[0] && mx < focusBtnRect[0] + focusBtnRect[2]
                && my >= focusBtnRect[1] && my < focusBtnRect[1] + focusBtnRect[3]) {
            ClickSound.play();
            f.host().action("eco_toggle_focus", null);
            return true;
        }
        if (TABLE.pressBar(mx, my)) {
            return true;
        }
        return TABLE.click(mx, my) != null;
    }

    @Override
    public boolean mouseReleased(PageFrame f, double mx, double my, int button) {
        if (search != null) {
            search.mouseReleased(mx, my, button);
        }
        return TABLE.releaseBar();
    }

    @Override
    public boolean mouseDragged(PageFrame f, double mx, double my, int button, double dx, double dy) {
        if (search != null && search.mouseDragged(mx, my, button, dx, dy)) {
            return true;
        }
        return TABLE.dragBar(my);
    }

    @Override
    public void clickedOutside() {
        if (search != null && search.isFocused()) {
            search.blur();
        }
    }

    @Override
    public void cancelInput() {
        if (search != null) {
            search.visible(false);
        }
        TABLE.releaseBar();
    }

    @Override
    public boolean mouseScrolled(PageFrame f, double mx, double my, double scrollY) {
        return TABLE.wheel(mx, my, scrollY, false);
    }
}
