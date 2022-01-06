package com.github.kangmoo.telegram.bot.scheduler;

import java.util.concurrent.TimeUnit;

/**
 * @author kangmoo Heo
 */
public abstract class IntervalTaskUnit implements Runnable {
    protected int interval;
    protected TimeUnit timeUnit = TimeUnit.MILLISECONDS;

    public IntervalTaskUnit(int interval) {
        this.interval = interval;
    }

    public IntervalTaskUnit(int interval, TimeUnit timeUnit) {
        this.interval = interval;
        this.timeUnit = timeUnit;
    }

    public int getInterval() {
        return interval;
    }

    public void setInterval(int interval) {
        this.interval = interval;
    }
}
