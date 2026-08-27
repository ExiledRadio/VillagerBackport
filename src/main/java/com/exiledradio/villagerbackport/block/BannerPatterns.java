package com.exiledradio.villagerbackport.block;

import net.minecraft.tileentity.BannerPattern;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The order the loom presents banner patterns in, and where the free ones end.
 *
 * <h2>Why this is not just the enum</h2>
 * 1.14 addresses patterns by their position in {@link BannerPattern}, and the loom leans on that
 * ordering twice: the button a player clicks <em>is</em> the ordinal, and everything past a fixed
 * point in the list is a pattern that cannot be chosen freely. Both of those break if the ordering
 * is not the one the loom was written against.
 *
 * <p>1.12.2's enum is 1.14's with two differences: it has no globe, and the creeper sits in the
 * middle of the list rather than with the other special patterns at the end. 1.14 moved it precisely
 * so the split could be "the last five" - so this rebuilds that arrangement.
 *
 * <p>Which leaves the list identical to 1.14's in everything the loom shows: the same thirty-four
 * choices, in the same order, four to a row and nine rows deep, so the scrollbar behaves the same
 * way too. Only globe is absent, and globe never appeared in this list in 1.14 either - it is
 * reached through a pattern item, and the one that carries it is sold by the wandering trader, which
 * 1.12.2 does not have.
 *
 * <h2>Index zero</h2>
 * {@code BASE} occupies it, as in 1.14, and doubles as "nothing chosen" - which is what makes zero a
 * usable empty value over the wire without a separate sentinel.
 */
public final class BannerPatterns {

    /** Nothing chosen. {@code BASE}'s own position, which no loom button ever selects. */
    public static final int NONE = 0;

    /** How many patterns one banner may carry. 1.14's limit, and 1.12.2's crafting limit too. */
    public static final int MAX_PATTERNS = 6;

    /**
     * The patterns a pattern item is needed for, in 1.14's order.
     *
     * <p>Listed rather than derived: {@link BannerPattern#hasPatternItem()} looks like the right
     * question and is not, because curly border and bricks answer yes to it and are both freely
     * chosen at a loom. That is 1.12.2 describing how the pattern was <em>crafted</em>, which 1.14
     * stopped using.
     */
    private static final BannerPattern[] SPECIAL = {
            BannerPattern.CREEPER,
            BannerPattern.SKULL,
            BannerPattern.FLOWER,
            BannerPattern.MOJANG,
    };

    private static final BannerPattern[] ORDER;

    /** The last index a player may pick from the list. Everything above it needs a pattern item. */
    public static final int FREE_COUNT;

    static {
        List<BannerPattern> order = new ArrayList<BannerPattern>();
        List<BannerPattern> special = Arrays.asList(SPECIAL);

        order.add(BannerPattern.BASE);

        for (BannerPattern pattern : BannerPattern.values()) {
            if (pattern != BannerPattern.BASE && !special.contains(pattern)) {
                order.add(pattern);
            }
        }

        FREE_COUNT = order.size() - 1;
        order.addAll(special);

        ORDER = order.toArray(new BannerPattern[order.size()]);
    }

    private BannerPatterns() {
    }

    /** @return how many positions there are, {@code BASE} included */
    public static int count() {
        return ORDER.length;
    }

    /** @return the pattern at a position, or {@code BASE} for anything out of range */
    public static BannerPattern byIndex(int index) {
        return index >= 0 && index < ORDER.length ? ORDER[index] : BannerPattern.BASE;
    }

    /** @return where a pattern sits, or {@link #NONE} if it is not one this mod knows */
    public static int indexOf(BannerPattern pattern) {
        for (int i = 0; i < ORDER.length; i++) {
            if (ORDER[i] == pattern) {
                return i;
            }
        }

        return NONE;
    }

    /** @return true if this is one of the patterns the loom offers without a pattern item */
    public static boolean isFree(int index) {
        return index > NONE && index <= FREE_COUNT;
    }
}
