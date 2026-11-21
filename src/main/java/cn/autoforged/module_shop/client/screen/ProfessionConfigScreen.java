package cn.autoforged.module_shop.client.screen;

import cn.autoforged.module_shop.client.ClientHomeCurrencyState;
import cn.autoforged.module_shop.client.ClientShopData;
import cn.autoforged.module_shop.client.ui.AtomUi;
import cn.autoforged.module_shop.client.ui.render.Draw;
import cn.autoforged.module_shop.client.ui.render.Text;
import cn.autoforged.module_shop.client.ui.widget.Button;
import cn.autoforged.module_shop.client.ui.widget.ScrollController;
import cn.autoforged.module_shop.client.ui.widget.SkiaTextField;
import cn.autoforged.module_shop.eco.profession.ProfessionConfig;
import cn.autoforged.module_shop.network.payload.ServerboundShopActionPayload;
import io.github.humbleui.skija.Canvas;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

/**
 * 管理员「职业专精配置」可视化管理界面（继承 {@link SkiaDialog}，附带原版回退界面）：
 * <ul>
 *     <li>顶部：最大等级、满级加价上限(%)、起步加价(%)、每日恢复专注、专注上限、单件基础专注、冷门悬赏上限(%)；</li>
 *     <li>左侧可滚动等级表：列出 Lv.1 ~ Lv.Max，显示每一级的升级所需经验（XP）与加价限制（%）；</li>
 *     <li>右侧当前等级编辑器：实时编辑该级升级所需经验（XP）与加价限制（%），并支持一键「按公式重算全表」与「从此级线性填充至满级」；</li>
 *     <li>保存后发送 {@code admin_profession_config_save}，服务端立即写入 {@code professions.json} + {@code Config} 并全服热同步。</li>
 * </ul>
 */
public class ProfessionConfigScreen extends SkiaDialog {
    private static final float MAX_W = 440.0F;
    private static final float MAX_H = 266.0F;

    private int maxLevel;
    private double starterBonusPct;
    private double maxBonusPct;
    private int dailyFocus;
    private int maxFocus;
    private double baseFocusCost;
    private double scarcityPct;
    private long xpBase;
    private long xpLinear;
    private double xpQuad;

    /** 索引 i 对应 Lv.(i+1) -> Lv.(i+2) 所需经验（长度 = maxLevel - 1）。 */
    private final List<Long> levelXp = new ArrayList<>();
    /** 索引 i 对应 Lv.(i+1) 的加价限制百分比（如 12.5 表示 +12.5%，长度 = maxLevel）。 */
    private final List<Double> levelBonusPct = new ArrayList<>();

    private int selectedLevel = 1;
    private boolean syncingFields;
    private String errorText = "";
    private String statusText = "";

    private final ScrollController listScroll = new ScrollController();

    private SkiaTextField maxLevelField;
    private SkiaTextField starterBonusField;
    private SkiaTextField maxBonusField;
    private SkiaTextField dailyFocusField;
    private SkiaTextField maxFocusField;
    private SkiaTextField baseFocusField;
    private SkiaTextField scarcityField;

    private SkiaTextField selXpField;
    private SkiaTextField selBonusField;

    // Vanilla fallback boxes
    private EditBox fbMaxLevelBox;
    private EditBox fbMaxBonusBox;
    private EditBox fbDailyFocusBox;
    private EditBox fbMaxFocusBox;
    private EditBox fbBaseFocusBox;

    public ProfessionConfigScreen() {
        super(Component.literal(tr("gui.module_shop.prof_cfg.title", "职业专精配置管理")));
        this.maxLevel = Math.max(2, Math.min(500, ClientShopData.profMaxLevel));
        this.starterBonusPct = Math.max(0.0D, Math.min(500.0D, ClientShopData.profStarterBonus * 100.0D));
        this.maxBonusPct = Math.max(this.starterBonusPct, Math.min(500.0D, ClientShopData.profMaxBonus * 100.0D));
        this.dailyFocus = Math.max(0, ClientShopData.focusDaily);
        this.maxFocus = Math.max(100, ClientShopData.focusMax);
        this.baseFocusCost = Math.max(0.5D, ClientShopData.profBaseFocusCost);
        this.scarcityPct = Math.max(0.0D, Math.min(200.0D, ClientShopData.profScarcityBonusMax * 100.0D));
        this.xpBase = Math.max(0L, ClientShopData.profXpBase);
        this.xpLinear = Math.max(0L, ClientShopData.profXpLinear);
        this.xpQuad = Math.max(0.0D, ClientShopData.profXpQuad);

        for (int lv = 1; lv < this.maxLevel; lv++) {
            long xp = (lv - 1 < ClientShopData.profLevelXp.length && ClientShopData.profLevelXp[lv - 1] > 0L)
                    ? ClientShopData.profLevelXp[lv - 1]
                    : ProfessionConfig.formulaXp(lv, this.xpBase, this.xpLinear, this.xpQuad);
            this.levelXp.add(xp);
        }
        for (int lv = 1; lv <= this.maxLevel; lv++) {
            double pct;
            if (lv - 1 < ClientShopData.profLevelBonusBps.length && ClientShopData.profLevelBonusBps[lv - 1] >= 0) {
                pct = ClientShopData.profLevelBonusBps[lv - 1] / 100.0D;
            } else {
                pct = this.starterBonusPct + (this.maxBonusPct - this.starterBonusPct)
                        * ((lv - 1) / (double) Math.max(1, this.maxLevel - 1));
            }
            this.levelBonusPct.add(round2(pct));
        }
    }

    private static String tr(String key, String fallback, Object... args) {
        return ClientHomeCurrencyState.tr(key, fallback, args);
    }

    private static double round2(double v) {
        return Math.round(v * 100.0D) / 100.0D;
    }

    private static String fmtPct(double pct) {
        if (Math.abs(pct - Math.rint(pct)) < 1e-6D) {
            return Long.toString(Math.round(pct));
        }
        return String.format(Locale.ROOT, "%.2f", pct).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    @Override
    public void onClose() {
        Nav.back();
    }

    @Override
    protected boolean onEnter() {
        return false;
    }

    private void resizeTables(int newMaxLevel) {
        int clamped = Math.max(2, Math.min(500, newMaxLevel));
        if (clamped == this.maxLevel) {
            return;
        }
        this.maxLevel = clamped;
        while (this.levelXp.size() < this.maxLevel - 1) {
            int lv = this.levelXp.size() + 1;
            this.levelXp.add(ProfessionConfig.formulaXp(lv, this.xpBase, this.xpLinear, this.xpQuad));
        }
        while (this.levelXp.size() > this.maxLevel - 1) {
            this.levelXp.remove(this.levelXp.size() - 1);
        }
        while (this.levelBonusPct.size() < this.maxLevel) {
            int lv = this.levelBonusPct.size() + 1;
            double pct = this.starterBonusPct + (this.maxBonusPct - this.starterBonusPct)
                    * ((lv - 1) / (double) Math.max(1, this.maxLevel - 1));
            this.levelBonusPct.add(round2(pct));
        }
        while (this.levelBonusPct.size() > this.maxLevel) {
            this.levelBonusPct.remove(this.levelBonusPct.size() - 1);
        }
        this.selectedLevel = Math.max(1, Math.min(this.maxLevel, this.selectedLevel));
        syncSelectedFields();
    }

    private void recalculateAllByFormula() {
        this.levelXp.clear();
        for (int lv = 1; lv < this.maxLevel; lv++) {
            this.levelXp.add(ProfessionConfig.formulaXp(lv, this.xpBase, this.xpLinear, this.xpQuad));
        }
        this.levelBonusPct.clear();
        for (int lv = 1; lv <= this.maxLevel; lv++) {
            double pct = this.starterBonusPct + (this.maxBonusPct - this.starterBonusPct)
                    * ((lv - 1) / (double) Math.max(1, this.maxLevel - 1));
            this.levelBonusPct.add(round2(pct));
        }
        this.errorText = "";
        this.statusText = tr("gui.module_shop.prof_cfg.recalc_done", "已按当前最大等级与加价上下限重算全表");
        syncSelectedFields();
    }

    private void fillBonusLinearFromSelected() {
        int startIdx = Math.max(0, Math.min(this.maxLevel - 1, this.selectedLevel - 1));
        double startPct = this.levelBonusPct.get(startIdx);
        double endPct = this.maxBonusPct;
        int span = Math.max(1, (this.maxLevel - 1) - startIdx);
        for (int i = startIdx; i < this.maxLevel; i++) {
            double ratio = (i - startIdx) / (double) span;
            this.levelBonusPct.set(i, round2(startPct + (endPct - startPct) * ratio));
        }
        this.errorText = "";
        this.statusText = tr("gui.module_shop.prof_cfg.fill_done",
                "已从 Lv.%s 线性填充加价限制至 Lv.%s（+%.1f%%）", this.selectedLevel, this.maxLevel, endPct);
        syncSelectedFields();
    }

    private void selectLevel(int lv) {
        this.selectedLevel = Math.max(1, Math.min(this.maxLevel, lv));
        syncSelectedFields();
    }

    private void syncSelectedFields() {
        if (this.selXpField == null || this.selBonusField == null) {
            return;
        }
        this.syncingFields = true;
        try {
            boolean isMax = this.selectedLevel >= this.maxLevel;
            this.selXpField.enabled(!isMax);
            if (isMax) {
                this.selXpField.setValue("");
            } else {
                long xp = this.levelXp.get(this.selectedLevel - 1);
                this.selXpField.setValue(Long.toString(xp));
            }
            double pct = this.levelBonusPct.get(this.selectedLevel - 1);
            this.selBonusField.setValue(fmtPct(pct));
        } finally {
            this.syncingFields = false;
        }
    }

    @Override
    protected void initSkia() {
        this.maxLevelField = field(4, "100").fontSize(Text.SMALL);
        this.starterBonusField = field(7, "5").fontSize(Text.SMALL);
        this.maxBonusField = field(7, "60").fontSize(Text.SMALL);
        this.dailyFocusField = field(7, "800").fontSize(Text.SMALL);
        this.maxFocusField = field(8, "2400").fontSize(Text.SMALL);
        this.baseFocusField = field(7, "25").fontSize(Text.SMALL);
        this.scarcityField = field(6, "20").fontSize(Text.SMALL);
        this.selXpField = field(12, "60").fontSize(Text.SMALL);
        this.selBonusField = field(7, "5").fontSize(Text.SMALL);

        this.maxLevelField.editBox().setFilter(s -> s.isEmpty() || s.matches("[0-9]{0,3}"));
        this.starterBonusField.editBox().setFilter(s -> s.isEmpty() || s.matches("[0-9]{0,3}(\\.[0-9]{0,2})?"));
        this.maxBonusField.editBox().setFilter(s -> s.isEmpty() || s.matches("[0-9]{0,3}(\\.[0-9]{0,2})?"));
        this.dailyFocusField.editBox().setFilter(s -> s.isEmpty() || s.matches("[0-9]{0,7}"));
        this.maxFocusField.editBox().setFilter(s -> s.isEmpty() || s.matches("[0-9]{0,8}"));
        this.baseFocusField.editBox().setFilter(s -> s.isEmpty() || s.matches("[0-9]{0,5}(\\.[0-9]{0,2})?"));
        this.scarcityField.editBox().setFilter(s -> s.isEmpty() || s.matches("[0-9]{0,3}(\\.[0-9]{0,2})?"));
        this.selXpField.editBox().setFilter(s -> s.isEmpty() || s.matches("[0-9]{0,12}"));
        this.selBonusField.editBox().setFilter(s -> s.isEmpty() || s.matches("[0-9]{0,3}(\\.[0-9]{0,2})?"));

        this.syncingFields = true;
        try {
            this.maxLevelField.setValue(Integer.toString(this.maxLevel));
            this.starterBonusField.setValue(fmtPct(this.starterBonusPct));
            this.maxBonusField.setValue(fmtPct(this.maxBonusPct));
            this.dailyFocusField.setValue(Integer.toString(this.dailyFocus));
            this.maxFocusField.setValue(Integer.toString(this.maxFocus));
            this.baseFocusField.setValue(fmtPct(this.baseFocusCost));
            this.scarcityField.setValue(fmtPct(this.scarcityPct));
        } finally {
            this.syncingFields = false;
        }

        this.maxLevelField.onChange(val -> {
            if (this.syncingFields || val == null || val.isBlank()) {
                return;
            }
            try {
                int lv = Integer.parseInt(val.trim());
                if (lv >= 2 && lv <= 500) {
                    resizeTables(lv);
                    this.errorText = "";
                }
            } catch (NumberFormatException ignored) {
            }
        });
        this.starterBonusField.onChange(val -> {
            if (this.syncingFields || val == null || val.isBlank()) {
                return;
            }
            try {
                double v = Double.parseDouble(val.trim());
                if (v >= 0.0D && v <= 500.0D) {
                    this.starterBonusPct = round2(v);
                    this.errorText = "";
                }
            } catch (NumberFormatException ignored) {
            }
        });
        this.maxBonusField.onChange(val -> {
            if (this.syncingFields || val == null || val.isBlank()) {
                return;
            }
            try {
                double v = Double.parseDouble(val.trim());
                if (v >= 0.0D && v <= 500.0D) {
                    this.maxBonusPct = round2(v);
                    this.errorText = "";
                }
            } catch (NumberFormatException ignored) {
            }
        });
        this.dailyFocusField.onChange(val -> {
            if (this.syncingFields || val == null || val.isBlank()) {
                return;
            }
            try {
                this.dailyFocus = Math.max(0, Math.min(1_000_000, Integer.parseInt(val.trim())));
                this.errorText = "";
            } catch (NumberFormatException ignored) {
            }
        });
        this.maxFocusField.onChange(val -> {
            if (this.syncingFields || val == null || val.isBlank()) {
                return;
            }
            try {
                this.maxFocus = Math.max(100, Math.min(10_000_000, Integer.parseInt(val.trim())));
                this.errorText = "";
            } catch (NumberFormatException ignored) {
            }
        });
        this.baseFocusField.onChange(val -> {
            if (this.syncingFields || val == null || val.isBlank()) {
                return;
            }
            try {
                double v = Double.parseDouble(val.trim());
                if (v >= 0.5D && v <= 10_000.0D) {
                    this.baseFocusCost = v;
                    this.errorText = "";
                }
            } catch (NumberFormatException ignored) {
            }
        });
        this.scarcityField.onChange(val -> {
            if (this.syncingFields || val == null || val.isBlank()) {
                return;
            }
            try {
                double v = Double.parseDouble(val.trim());
                if (v >= 0.0D && v <= 200.0D) {
                    this.scarcityPct = round2(v);
                    this.errorText = "";
                }
            } catch (NumberFormatException ignored) {
            }
        });
        this.selXpField.onChange(val -> {
            if (this.syncingFields || this.selectedLevel >= this.maxLevel || val == null || val.isBlank()) {
                return;
            }
            try {
                long xp = Long.parseLong(val.trim());
                if (xp > 0L) {
                    this.levelXp.set(this.selectedLevel - 1, xp);
                    this.errorText = "";
                }
            } catch (NumberFormatException ignored) {
            }
        });
        this.selBonusField.onChange(val -> {
            if (this.syncingFields || val == null || val.isBlank()) {
                return;
            }
            try {
                double pct = Double.parseDouble(val.trim());
                if (pct >= 0.0D && pct <= 500.0D) {
                    this.levelBonusPct.set(this.selectedLevel - 1, round2(pct));
                    this.errorText = "";
                }
            } catch (NumberFormatException ignored) {
            }
        });

        syncSelectedFields();
    }

    private void saveConfig() {
        if (!skia()) {
            try {
                int lv = Integer.parseInt(this.fbMaxLevelBox.getValue().trim());
                double maxB = Double.parseDouble(this.fbMaxBonusBox.getValue().trim());
                int dFoc = Integer.parseInt(this.fbDailyFocusBox.getValue().trim());
                int mFoc = Integer.parseInt(this.fbMaxFocusBox.getValue().trim());
                double bFoc = Double.parseDouble(this.fbBaseFocusBox.getValue().trim());
                if (lv < 2 || lv > 500 || maxB < 0.0D || maxB > 500.0D || bFoc < 0.5D) {
                    this.errorText = tr("gui.module_shop.prof_cfg.err_range", "参数超出允许范围");
                    return;
                }
                resizeTables(lv);
                this.maxBonusPct = round2(maxB);
                this.dailyFocus = Math.max(0, dFoc);
                this.maxFocus = Math.max(100, mFoc);
                this.baseFocusCost = bFoc;
            } catch (NumberFormatException ex) {
                this.errorText = tr("gui.module_shop.prof_cfg.err_num", "请输入合法的数字");
                return;
            }
        }
        if (this.maxLevel < 2 || this.maxLevel > 500) {
            this.errorText = tr("gui.module_shop.prof_cfg.err_max_lv", "最大等级须在 2 ~ 500 之间");
            return;
        }
        if (this.maxBonusPct < this.starterBonusPct) {
            this.maxBonusPct = this.starterBonusPct;
        }
        long[] xpArr = new long[this.maxLevel - 1];
        for (int i = 0; i < xpArr.length; i++) {
            long xp = i < this.levelXp.size() ? this.levelXp.get(i) : 1L;
            if (xp <= 0L) {
                this.errorText = tr("gui.module_shop.prof_cfg.err_xp", "Lv.%s 升级所需经验必须大于 0", i + 1);
                selectLevel(i + 1);
                return;
            }
            xpArr[i] = xp;
        }
        int[] bonusBps = new int[this.maxLevel];
        for (int i = 0; i < bonusBps.length; i++) {
            double pct = i < this.levelBonusPct.size() ? this.levelBonusPct.get(i) : this.maxBonusPct;
            if (pct < 0.0D || pct > 500.0D) {
                this.errorText = tr("gui.module_shop.prof_cfg.err_bonus", "Lv.%s 加价限制须在 0%% ~ 500%% 之间", i + 1);
                selectLevel(i + 1);
                return;
            }
            bonusBps[i] = (int) Math.round(pct * 100.0D);
        }

        CompoundTag payload = new CompoundTag();
        payload.putString("action", "admin_profession_config_save");
        payload.putInt("maxLevel", this.maxLevel);
        payload.putDouble("starterBonus", this.starterBonusPct / 100.0D);
        payload.putDouble("maxBonus", this.maxBonusPct / 100.0D);
        payload.putInt("dailyFocus", this.dailyFocus);
        payload.putInt("maxFocus", this.maxFocus);
        payload.putDouble("baseFocusCost", this.baseFocusCost);
        payload.putDouble("scarcityBonusMax", this.scarcityPct / 100.0D);
        payload.putLong("xpBase", this.xpBase);
        payload.putLong("xpLinear", this.xpLinear);
        payload.putDouble("xpQuad", this.xpQuad);
        payload.putLongArray("levelXp", xpArr);
        payload.putIntArray("levelBonusBps", bonusBps);
        ServerboundShopActionPayload.send(payload);
        Nav.back();
    }

    // ------------------------------------------------------------------ Skija 主绘制

    @Override
    protected void paint(Canvas canvas) {
        float w = Math.max(320.0F, Math.min(MAX_W, this.width - 18.0F));
        float h = Math.max(230.0F, Math.min(MAX_H, this.height - 18.0F));
        float x = Math.round((this.width - w) / 2.0F);
        float y = Math.round((this.height - h) / 2.0F);

        panel(canvas, x, y, w, h, this.title.getString());
        float closeX = x + w - PAD - 8.0F;
        float closeY = y + HEADER_H * 0.5F;
        float chv = hover("close", closeX - 6.0F, closeY - 6.0F, 12.0F, 12.0F);
        Draw.cross(canvas, closeX, closeY, 3.0F, 1.1F, Draw.lerp(IOStyle.TEXT_2, IOStyle.RED, chv));
        hit(closeX - 6.0F, closeY - 6.0F, 12.0F, 12.0F, this::onClose, null);

        float innerX = x + PAD;
        float innerW = w - PAD * 2.0F;
        float topY = y + HEADER_H + 5.0F;

        // 顶部全局参数卡（2行紧凑布局）
        float topCardH = 52.0F;
        card(canvas, innerX, topY, innerW, topCardH);
        paintTopParams(canvas, innerX, topY, innerW, topCardH);

        float footerBtnH = 18.0F;
        float footerBtnY = y + h - PAD - footerBtnH;
        float splitTop = topY + topCardH + 6.0F;
        float splitBottom = footerBtnY - 12.0F;
        float splitH = Math.max(82.0F, splitBottom - splitTop);

        float leftW = Math.min(220.0F, Math.max(170.0F, innerW * 0.52F));
        float gap = 8.0F;
        float rightX = innerX + leftW + gap;
        float rightW = innerX + innerW - rightX;

        paintLevelList(canvas, innerX, splitTop, leftW, splitH);
        paintLevelEditor(canvas, rightX, splitTop, rightW, splitH);

        // 状态与提示
        if (!this.errorText.isEmpty()) {
            small(canvas, this.errorText, innerX, footerBtnY - 6.5F, innerW, IOStyle.RED);
        } else if (!this.statusText.isEmpty()) {
            small(canvas, this.statusText, innerX, footerBtnY - 6.5F, innerW, IOStyle.GREEN);
        } else {
            small(canvas, tr("gui.module_shop.prof_cfg.hint",
                            "点击左侧任意等级即可修改该级升级经验（XP）与加价限制（%），保存后全服即时生效"),
                    innerX, footerBtnY - 6.5F, innerW, IOStyle.TEXT_3);
        }

        float saveW = 104.0F;
        float cancelW = 66.0F;
        button(canvas, "cancel", tr("gui.module_shop.cancel", "取消"),
                innerX + innerW - saveW - 6.0F - cancelW, footerBtnY, cancelW, footerBtnH,
                Button.Kind.NORMAL, true, this::onClose);
        button(canvas, "save", tr("gui.module_shop.prof_cfg.save", "保存并全服生效"),
                innerX + innerW - saveW, footerBtnY, saveW, footerBtnH,
                Button.Kind.ACCENT, true, this::saveConfig);
    }

    private void paintTopParams(Canvas canvas, float x, float y, float w, float h) {
        float px = x + 6.0F;
        float pw = w - 12.0F;
        float gap = 6.0F;
        float row1Y = y + 4.0F;
        float row2Y = y + 28.0F;

        // 第 1 行：最大等级 | 起步加价% | 满级加价上限% | 冷门悬赏%
        float colW4 = (pw - gap * 3.0F) / 4.0F;
        placeParam(canvas, tr("gui.module_shop.prof_cfg.max_lv", "最大等级"), this.maxLevelField, px, row1Y, colW4);
        placeParam(canvas, tr("gui.module_shop.prof_cfg.starter_bonus", "起步加价(%)"), this.starterBonusField, px + (colW4 + gap), row1Y, colW4);
        placeParam(canvas, tr("gui.module_shop.prof_cfg.max_bonus", "满级加价上限(%)"), this.maxBonusField, px + (colW4 + gap) * 2.0F, row1Y, colW4);
        placeParam(canvas, tr("gui.module_shop.prof_cfg.scarcity", "冷门悬赏(%)"), this.scarcityField, px + (colW4 + gap) * 3.0F, row1Y, colW4);

        // 第 2 行：每日专注恢复 | 专注上限 | 单件基础专注 | 按公式重算全表按钮
        placeParam(canvas, tr("gui.module_shop.prof_cfg.daily_focus", "日恢复专注"), this.dailyFocusField, px, row2Y, colW4);
        placeParam(canvas, tr("gui.module_shop.prof_cfg.max_focus", "专注上限"), this.maxFocusField, px + (colW4 + gap), row2Y, colW4);
        placeParam(canvas, tr("gui.module_shop.prof_cfg.base_focus", "单件基础专注"), this.baseFocusField, px + (colW4 + gap) * 2.0F, row2Y, colW4);
        float btnX = px + (colW4 + gap) * 3.0F;
        button(canvas, "recalc", tr("gui.module_shop.prof_cfg.recalc_btn", "按公式重算全表"),
                btnX, row2Y + 7.5F, colW4, 14.0F, Button.Kind.NORMAL, true, this::recalculateAllByFormula);
    }

    private void placeParam(Canvas canvas, String label, SkiaTextField tf, float x, float y, float w) {
        Text.drawLine(canvas, label, AtomUi.REGULAR, 5.0F, IOStyle.TEXT_3, x, y + 3.5F, w, Text.Align.LEFT);
        if (tf != null) {
            tf.visible(true).bounds(x, y + 7.5F, w, 14.0F);
        }
    }

    private void paintLevelList(Canvas canvas, float x, float y, float w, float h) {
        float headH = 14.0F;
        small(canvas, tr("gui.module_shop.prof_cfg.level_list", "每级经验与加价限制表（Lv.1 ~ Lv.%s）", this.maxLevel),
                x, y + 6.5F, w, IOStyle.TEXT_2);
        float listY = y + headH + 2.0F;
        float listH = Math.max(40.0F, h - headH - 2.0F);
        card(canvas, x, listY, w, listH);

        float rowH = 16.0F;
        float rowGap = 2.0F;
        float padIn = 3.0F;
        float contentH = this.maxLevel * rowH + Math.max(0, this.maxLevel - 1) * rowGap + padIn * 2.0F;
        float oy = beginList(canvas, this.listScroll, x + 2.0F, listY + 2.0F, w - 4.0F, listH - 4.0F, contentH);
        float rx = x + padIn + 1.0F;
        float rw = w - padIn * 2.0F - 10.0F;

        for (int lv = 1; lv <= this.maxLevel; lv++) {
            float ry = oy + padIn + (lv - 1) * (rowH + rowGap);
            if (ry + rowH < listY || ry > listY + listH) {
                continue;
            }
            final int targetLv = lv;
            boolean sel = lv == this.selectedLevel;
            float hv = hover("lv_row:" + lv, rx, ry, rw, rowH);
            int bg = sel ? Draw.alpha(IOStyle.BLUE, 0.20F) : Draw.lerp(IOStyle.CARD, IOStyle.ROW_HOVER, hv);
            int border = sel ? IOStyle.BLUE : Draw.lerp(IOStyle.BORDER_DIM, IOStyle.BORDER_HOVER, hv);
            Draw.fill(canvas, rx, ry, rw, rowH, 2.5F, bg);
            Draw.stroke(canvas, rx, ry, rw, rowH, 2.5F, sel ? 1.0F : 0.6F, border);

            float cy = ry + rowH * 0.5F;
            Text.drawLine(canvas, "Lv." + lv, sel ? AtomUi.BOLD : AtomUi.REGULAR, Text.SMALL,
                    sel ? IOStyle.BLUE : IOStyle.TEXT, rx + 5.0F, cy, 34.0F, Text.Align.LEFT);

            String xpText = lv >= this.maxLevel
                    ? tr("gui.module_shop.prof_cfg.max_tag", "满级")
                    : tr("gui.module_shop.prof_cfg.xp_col", "需 %s XP", this.levelXp.get(lv - 1));
            Text.drawLine(canvas, xpText, AtomUi.REGULAR, 5.5F,
                    lv >= this.maxLevel ? IOStyle.ORANGE : IOStyle.TEXT_2,
                    rx + 42.0F, cy, Math.max(30.0F, rw - 92.0F), Text.Align.LEFT);

            String bonusText = String.format(Locale.ROOT, "+%.1f%%", this.levelBonusPct.get(lv - 1));
            Text.drawLine(canvas, bonusText, AtomUi.BOLD, Text.SMALL, IOStyle.GREEN,
                    rx + rw - 48.0F, cy, 43.0F, Text.Align.RIGHT);

            hit(rx, ry, rw, rowH, () -> selectLevel(targetLv), null);
        }
        endList(canvas);
    }

    private void paintLevelEditor(Canvas canvas, float x, float y, float w, float h) {
        float headH = 14.0F;
        small(canvas, tr("gui.module_shop.prof_cfg.editor_title", "当前编辑：Lv.%s", this.selectedLevel),
                x, y + 6.5F, w, IOStyle.ORANGE);
        float boxY = y + headH + 2.0F;
        float boxH = Math.max(40.0F, h - headH - 2.0F);
        card(canvas, x, boxY, w, boxH);

        float ix = x + 8.0F;
        float iw = Math.max(40.0F, w - 16.0F);
        float cy = boxY + 10.0F;

        boolean isMax = this.selectedLevel >= this.maxLevel;
        String xpLabel = isMax
                ? tr("gui.module_shop.prof_cfg.xp_max_label", "升至下一级所需经验（当前已是满级）")
                : tr("gui.module_shop.prof_cfg.xp_label", "Lv.%s → Lv.%s 升级所需经验 (XP)",
                        this.selectedLevel, this.selectedLevel + 1);
        small(canvas, xpLabel, ix, cy + 4.0F, iw, isMax ? IOStyle.TEXT_3 : IOStyle.TEXT_2);
        cy += 10.0F;
        if (this.selXpField != null) {
            this.selXpField.visible(true).bounds(ix, cy, iw, 16.0F);
        }
        cy += 24.0F;

        String bonusLabel = tr("gui.module_shop.prof_cfg.bonus_label", "Lv.%s 专注精销加价限制 (%)", this.selectedLevel);
        small(canvas, bonusLabel, ix, cy + 4.0F, iw, IOStyle.TEXT_2);
        cy += 10.0F;
        if (this.selBonusField != null) {
            this.selBonusField.visible(true).bounds(ix, cy, iw, 16.0F);
        }
        cy += 24.0F;

        // 实时预览本级专注消耗与倍率
        double progress = (this.selectedLevel - 1) / (double) Math.max(1, this.maxLevel - 1);
        double focusCost = Math.max(0.5D, this.baseFocusCost) / (1.0D + 2.97D * progress);
        double bonusPct = this.levelBonusPct.get(this.selectedLevel - 1);
        small(canvas, tr("gui.module_shop.prof_cfg.preview",
                        "本级预览：售价上限 +%.2f%% · 单件消耗 %.1f 专注", bonusPct, focusCost),
                ix, cy + 4.0F, iw, IOStyle.TEXT_3);
        cy += 14.0F;

        // 快捷微调行：上一级 / 下一级
        float navW = (iw - 6.0F) * 0.5F;
        button(canvas, "prev_lv", tr("gui.module_shop.prof_cfg.prev_lv", "◀ 上一级"),
                ix, cy, navW, 15.0F, Button.Kind.NORMAL, this.selectedLevel > 1,
                () -> selectLevel(this.selectedLevel - 1));
        button(canvas, "next_lv", tr("gui.module_shop.prof_cfg.next_lv", "下一级 ▶"),
                ix + navW + 6.0F, cy, navW, 15.0F, Button.Kind.NORMAL, this.selectedLevel < this.maxLevel,
                () -> selectLevel(this.selectedLevel + 1));
        cy += 19.0F;

        if (cy + 16.0F <= boxY + boxH - 4.0F) {
            button(canvas, "fill_linear", tr("gui.module_shop.prof_cfg.fill_linear", "从此级线性填充加价至满级"),
                    ix, cy, iw, 16.0F, Button.Kind.NORMAL, this.selectedLevel < this.maxLevel,
                    this::fillBonusLinearFromSelected);
        }
    }

    // ------------------------------------------------------------------ Vanilla Fallback

    @Override
    protected void init() {
        if (skiaInit()) {
            return;
        }
        int pw = 240;
        int ph = 200;
        int left = (this.width - pw) / 2;
        int top = (this.height - ph) / 2;
        this.fbMaxLevelBox = addFallbackBox(left + 120, top + 28, 100, Integer.toString(this.maxLevel));
        this.fbMaxBonusBox = addFallbackBox(left + 120, top + 54, 100, fmtPct(this.maxBonusPct));
        this.fbDailyFocusBox = addFallbackBox(left + 120, top + 80, 100, Integer.toString(this.dailyFocus));
        this.fbMaxFocusBox = addFallbackBox(left + 120, top + 106, 100, Integer.toString(this.maxFocus));
        this.fbBaseFocusBox = addFallbackBox(left + 120, top + 132, 100, fmtPct(this.baseFocusCost));

        this.addRenderableWidget(new ShopButton(left + 20, top + ph - 28, 92, 18,
                Component.literal(tr("gui.module_shop.cancel", "取消")), b -> onClose()));
        this.addRenderableWidget(new ShopButton(left + pw - 112, top + ph - 28, 92, 18,
                Component.literal(tr("gui.module_shop.prof_cfg.save", "保存并全服生效")), b -> saveConfig()));
    }

    private EditBox addFallbackBox(int x, int y, int w, String value) {
        EditBox b = new EditBox(this.font, x, y, w, 16, Component.empty());
        b.setValue(value);
        this.addRenderableWidget(b);
        return b;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (skiaRender(g, mouseX, mouseY, partialTick)) {
            return;
        }
        int pw = 240;
        int ph = 200;
        int left = (this.width - pw) / 2;
        int top = (this.height - ph) / 2;
        Bricks.panel(g, left, top, pw, ph);
        IOStyle.centerAt(g, this.font, this.title, left + pw / 2, top + 10, IOStyle.TEXT);
        g.drawString(this.font, tr("gui.module_shop.prof_cfg.max_lv", "最大等级"), left + 16, top + 32, IOStyle.TEXT_2, false);
        g.drawString(this.font, tr("gui.module_shop.prof_cfg.max_bonus", "满级加价上限(%)"), left + 16, top + 58, IOStyle.TEXT_2, false);
        g.drawString(this.font, tr("gui.module_shop.prof_cfg.daily_focus", "日恢复专注"), left + 16, top + 84, IOStyle.TEXT_2, false);
        g.drawString(this.font, tr("gui.module_shop.prof_cfg.max_focus", "专注上限"), left + 16, top + 110, IOStyle.TEXT_2, false);
        g.drawString(this.font, tr("gui.module_shop.prof_cfg.base_focus", "单件基础专注"), left + 16, top + 136, IOStyle.TEXT_2, false);
        if (!this.errorText.isEmpty()) {
            g.drawString(this.font, this.errorText, left + 16, top + 156, IOStyle.RED, false);
        }
        super.render(g, mouseX, mouseY, partialTick);
    }
}
