package cn.autoforged.module_shop.shop;

/**
 * 出货箱结算时钟解析器（纯 Java）：正常昼夜循环按 {@code dayTime} 结算，
 * 检测到永昼/时间停滞/循环重置白天时自动平滑回退到按总运行刻 {@code gameTime} 每 24,000 刻（20 分钟）定时结算。
 */
public final class ShippingSettleClock {
    public static final long TICKS_PER_DAY = 24_000L;
    public static final long STALL_THRESHOLD_TICKS = 100L;
    public static final long RECOVER_THRESHOLD_TICKS = 100L;

    public record Sample(long clockTick, long cycleDay, long timeOfDay,
                         boolean fallbackGameTime, boolean modeChanged) {
    }

    private boolean initialized;
    private boolean fallbackGameTime;
    private boolean loopLocked;
    private long lastDayTime;
    private long lastGameTime;
    private long maxObservedDayCycle = Long.MIN_VALUE;
    private long stalledTicks;
    private long recoveringTicks;
    private int backwardResetsWithoutDayProgress;

    public synchronized void reset() {
        this.initialized = false;
        this.fallbackGameTime = false;
        this.loopLocked = false;
        this.lastDayTime = 0L;
        this.lastGameTime = 0L;
        this.maxObservedDayCycle = Long.MIN_VALUE;
        this.stalledTicks = 0L;
        this.recoveringTicks = 0L;
        this.backwardResetsWithoutDayProgress = 0;
    }

    public synchronized boolean isFallbackGameTime() {
        return this.fallbackGameTime;
    }

    public synchronized Sample sample(boolean daylightCycleEnabled, long dayTime, long gameTime) {
        boolean prevFallback = this.fallbackGameTime;
        long rawDayCycle = Math.floorDiv(dayTime, TICKS_PER_DAY);

        if (!this.initialized) {
            this.initialized = true;
            this.lastDayTime = dayTime;
            this.lastGameTime = gameTime;
            this.maxObservedDayCycle = rawDayCycle;
            this.fallbackGameTime = !daylightCycleEnabled;
            this.loopLocked = false;
            this.stalledTicks = 0L;
            this.recoveringTicks = 0L;
            this.backwardResetsWithoutDayProgress = 0;
            return buildSample(dayTime, gameTime, this.fallbackGameTime, this.fallbackGameTime != prevFallback);
        }

        long dg = Math.max(0L, gameTime - this.lastGameTime);
        long dd = dayTime - this.lastDayTime;

        if (!daylightCycleEnabled) {
            this.fallbackGameTime = true;
            this.stalledTicks = STALL_THRESHOLD_TICKS;
            this.recoveringTicks = 0L;
        } else {
            if (rawDayCycle > this.maxObservedDayCycle) {
                this.maxObservedDayCycle = rawDayCycle;
                this.backwardResetsWithoutDayProgress = 0;
                this.loopLocked = false;
            }
            if (dg > 0L) {
                if (dd == 0L) {
                    this.stalledTicks += dg;
                    this.recoveringTicks = 0L;
                    if (this.stalledTicks >= STALL_THRESHOLD_TICKS) {
                        this.fallbackGameTime = true;
                    }
                } else if (dd < 0L) {
                    this.stalledTicks = 0L;
                    this.recoveringTicks = 0L;
                    if (rawDayCycle <= this.maxObservedDayCycle) {
                        this.backwardResetsWithoutDayProgress++;
                        if (this.backwardResetsWithoutDayProgress >= 2) {
                            this.fallbackGameTime = true;
                            this.loopLocked = true;
                        }
                    }
                } else {
                    this.stalledTicks = 0L;
                    if (this.fallbackGameTime && !this.loopLocked) {
                        this.recoveringTicks += dg;
                        if (this.recoveringTicks >= RECOVER_THRESHOLD_TICKS) {
                            this.fallbackGameTime = false;
                            this.recoveringTicks = 0L;
                        }
                    }
                }
            }
        }

        this.lastDayTime = dayTime;
        this.lastGameTime = gameTime;
        return buildSample(dayTime, gameTime, this.fallbackGameTime, this.fallbackGameTime != prevFallback);
    }

    private static Sample buildSample(long dayTime, long gameTime, boolean fallback, boolean changed) {
        long tick = fallback ? gameTime : dayTime;
        long cycleDay = Math.floorDiv(tick, TICKS_PER_DAY);
        long timeOfDay = Math.floorMod(tick, TICKS_PER_DAY);
        return new Sample(tick, cycleDay, timeOfDay, fallback, changed);
    }

    public static long remainingTicks(long clockTick, long settleStartTick) {
        long start = Math.floorMod(settleStartTick, TICKS_PER_DAY);
        long timeOfDay = Math.floorMod(clockTick, TICKS_PER_DAY);
        return timeOfDay <= start ? start - timeOfDay : TICKS_PER_DAY - timeOfDay + start;
    }
}
