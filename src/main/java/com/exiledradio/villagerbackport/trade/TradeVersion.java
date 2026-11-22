package com.exiledradio.villagerbackport.trade;

import com.exiledradio.backportcore.presentation.GameVersion;
import com.exiledradio.backportcore.presentation.Presentation;

import javax.annotation.Nullable;

/**
 * Which version's trade tables villagers and the wandering trader use: the newest installed backport's
 * ({@link Presentation#installedNewest()}, whatever the look settings).
 *
 * <ul>
 *   <li>{@link #V1_14}: this mod alone (and Update Aquatic, Buzzy Bees, Nether Update: 1.15 and 1.16 changed no
 *       trade).</li>
 *   <li>{@link #V1_17}: Caves &amp; Cliffs (1.17 to 1.18).</li>
 *   <li>{@link #V1_19}: Wild Update.</li>
 *   <li>{@link #V1_20}: Trails &amp; Tales.</li>
 *   <li>{@link #V1_21}: Tricky Trials (1.21 to 1.21.11).</li>
 *   <li>{@link #V26}: Game Drops (26.1 to 26.3).</li>
 * </ul>
 * Each table is the previous one with that version's changes (decompiled {@code VillagerTrades} of 1.14.4, 1.17.1,
 * 1.19.2, 1.20.1, 1.21.11 and 26.3, compared): a later backport adds a constant and its rows. A villager keeps the
 * trades it already has; a new level, a new villager and a new trader take the current table.
 */
public enum TradeVersion {
    V1_14("1.14"),
    V1_17("1.17"),
    V1_19("1.19"),
    V1_20("1.20"),
    V1_21("1.21"),
    V26("26.1");

    private final GameVersion from;

    /** Test only: the version to use in place of the installed one, or null. */
    @Nullable
    private static TradeVersion override;

    TradeVersion(String from) {
        this.from = GameVersion.parse(from);
    }

    /** @return the table for the newest installed backport */
    public static TradeVersion current() {
        if (override != null) {
            return override;
        }
        return of(Presentation.installedNewest());
    }

    /** @return the table for a game version: the newest constant at or below it */
    public static TradeVersion of(GameVersion version) {
        TradeVersion out = V1_14;
        for (TradeVersion v : values()) {
            if (version.isAtLeast(v.from)) {
                out = v;
            }
        }
        return out;
    }

    public boolean atLeast(TradeVersion other) {
        return ordinal() >= other.ordinal();
    }

    /** Test only: uses {@code version} in place of the installed one; null goes back to the installed one. */
    public static void override(@Nullable TradeVersion version) {
        override = version;
    }
}
