package com.xucz.libscreenstream.utils;

/**
 * 描述：
 *
 * @author 创建人 ：xucz
 * @since 创建时间 ：2019-06-27
 */
public class FrameRateMeter {
    private static final long TIMETRAVEL = 1L;
    private static final long TIMETRAVEL_MS = 1000L;
    private static final long GET_TIMETRAVEL_MS = 2000L;
    private int times;
    private float lastFps;
    private long lastUpdateTime;
    private long now;

    public FrameRateMeter() {
        this.reSet();
    }

    public void count() {
        this.now = System.currentTimeMillis();
        if (this.lastUpdateTime == 0L) {
            this.lastUpdateTime = this.now;
        }

        if (this.now - this.lastUpdateTime > 1000L) {
            this.lastFps = (float) this.times / (float) (this.now - this.lastUpdateTime) * 1000.0F;
            this.lastUpdateTime = this.now;
            this.times = 0;
        }

        ++this.times;
    }

    public float getFps() {
        if (this.lastUpdateTime == 0L) {
            return 20.0F;
        } else {
            return System.currentTimeMillis() - this.lastUpdateTime > 2000L ? 0.0F : this.lastFps;
        }
    }

    public void reSet() {
        this.times = 0;
        this.lastFps = 0.0F;
        this.lastUpdateTime = 0L;
    }
}
