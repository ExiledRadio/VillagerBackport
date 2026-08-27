package com.exiledradio.villagerbackport.trade;

/**
 * 1.14's villager level curve.
 *
 * <p>Levels run 1 (Novice) to 5 (Master). The thresholds are 1.14's own, from
 * {@code VillagerData}'s {@code {0, 10, 70, 150, 250}}: 10 experience to reach level 2, 70 for
 * level 3, 150 for level 4, 250 for level 5.
 *
 * <p>1.12.2 has a parallel concept already - {@code EntityVillager.careerLevel}, which counts up
 * every time the villager restocks and gates which tier of trades {@code populateBuyingList()}
 * adds. It is not the same thing. Career level is incremented by the act of restocking regardless
 * of how much trading happened, so it measures elapsed restocks rather than the player's investment
 * in the villager. This class tracks the 1.14 quantity - experience actually earned by trading -
 * and keeps it separate from career level so that vanilla's trade-tier progression continues to
 * work untouched while the level shown to the player means what it means in 1.14.
 */
public final class VillagerLevel {

    /** Cumulative experience required to have reached each level, indexed from level 1. */
    private static final int[] THRESHOLDS = {0, 10, 70, 150, 250};

    public static final int MIN_LEVEL = 1;
    public static final int MAX_LEVEL = 5;

    private VillagerLevel() {
    }

    /** @return the level, 1 to 5, that the given total experience corresponds to. */
    public static int levelFor(int xp) {
        int level = MIN_LEVEL;
        for (int i = 1; i < THRESHOLDS.length; i++) {
            if (xp >= THRESHOLDS[i]) {
                level = i + 1;
            }
        }
        return level;
    }

    /**
     * @return total experience needed to reach the given level, or 0 if it is out of range.
     * Matches 1.14's {@code VillagerData.func_221133_b}.
     */
    public static int xpForLevel(int level) {
        if (level < MIN_LEVEL || level > MAX_LEVEL) {
            return 0;
        }
        return THRESHOLDS[level - 1];
    }

    /**
     * @return experience needed to reach the next level, or 0 at max level.
     *
     * <p>The XP bar in the merchant screen is drawn from this and {@link #xpForLevel(int)}, so it
     * exists now even though nothing renders it until the GUI phase.
     */
    public static int xpForNextLevel(int level) {
        if (level < MIN_LEVEL || level >= MAX_LEVEL) {
            return 0;
        }
        return THRESHOLDS[level];
    }

    public static boolean canLevelUp(int level) {
        return level >= MIN_LEVEL && level < MAX_LEVEL;
    }
}
