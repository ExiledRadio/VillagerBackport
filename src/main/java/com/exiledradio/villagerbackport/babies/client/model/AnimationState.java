package com.exiledradio.villagerbackport.babies.client.model;

/** 26.3's {@code AnimationState}: the tick an animation started, or stopped. */
public final class AnimationState {

    private static final int STOPPED = Integer.MIN_VALUE;
    private int startTick = STOPPED;

    public void start(int tickCount) {
        startTick = tickCount;
    }

    public void startIfStopped(int tickCount) {
        if (!isStarted()) {
            start(tickCount);
        }
    }

    public void stop() {
        startTick = STOPPED;
    }

    public boolean isStarted() {
        return startTick != STOPPED;
    }

    public long getTimeInMillis(float ageInTicks) {
        return (long) ((ageInTicks - startTick) * 50.0F);
    }
}
