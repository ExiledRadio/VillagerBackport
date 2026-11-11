package com.exiledradio.villagerbackport.lookfeel;

/**
 * 1.14.4's {@code MouseHandler.accumulatedScroll}: wheel movement is added up, a change of direction starts the
 * sum again, and each time it passes a whole notch that many notches are handed on and the remainder kept.
 * Plain arithmetic, so the server self-test can check it.
 */
public final class ScrollAccumulator {

    private double accumulated;

    /** @return the whole notches reached by adding {@code amount} (0 while the sum is below one) */
    public int add(double amount) {
        if (this.accumulated != 0.0 && Math.signum(amount) != Math.signum(this.accumulated)) {
            this.accumulated = 0.0;
        }
        this.accumulated += amount;
        int whole = (int) this.accumulated;
        this.accumulated -= whole;
        return whole;
    }

    /**
     * The size of one wheel notch in a mouse event. LWJGL 2 reports 120. Cleanroom's LWJGL 3 layer
     * ({@code org.lwjglx}) reports 1, so dividing its events by 120 took 120 notches to move one hotbar slot.
     */
    public static double notch() {
        return notch(ScrollAccumulator.class.getClassLoader().getResource("org/lwjglx/input/Mouse.class") != null);
    }

    /** @param lwjglx whether Cleanroom's LWJGL 3 layer is the game's input library */
    public static double notch(boolean lwjglx) {
        return lwjglx ? 1.0 : 120.0;
    }

    /** One wheel event's amount: the movement in notches, or its sign with Discrete Scrolling, times Scroll Sensitivity. */
    public static double amount(double notches) {
        return (LookAndFeel.DISCRETE_SCROLLING.get() ? Math.signum(notches) : notches) * LookAndFeel.SCROLL_SENSITIVITY.get();
    }
}
