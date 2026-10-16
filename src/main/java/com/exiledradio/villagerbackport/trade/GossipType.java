package com.exiledradio.villagerbackport.trade;

import javax.annotation.Nullable;

/**
 * The kinds of opinion a villager holds about a player, ported from 1.14's {@code GossipType}.
 *
 * <p>The numbers are 1.14's, with the cure caps the game has had since:
 * <pre>
 *   MAJOR_NEGATIVE("major_negative", -5, 100, 10, 10)
 *   MINOR_NEGATIVE("minor_negative", -1, 200, 20, 20)
 *   MINOR_POSITIVE("minor_positive",  1,  25,  1,  5)   (the game now caps this at 25; 1.14 had 200)
 *   MAJOR_POSITIVE("major_positive",  5,  20,  0, 100)   (the game now caps this at 20; 1.14 had 100)
 *   TRADING       ("trading",         1,  25,  2, 20)
 * </pre>
 *
 * <p>Reading across: how much a point of it is worth, how much of it can pile up, how much of it
 * wears off each day, and how much is lost each time it is repeated to another villager.
 *
 * <p>The shape of the table is what gives the system its character. Killing a villager is worth five
 * times what hurting one is and takes ten days to wear off; trading is worth a single point, caps
 * out fast and fades in a fortnight; and being cured of zombiehood is the only thing here that never
 * fades at all, because {@code MAJOR_POSITIVE} decays by zero.
 */
public enum GossipType {

    MAJOR_NEGATIVE("major_negative", -5, 100, 10, 10),
    MINOR_NEGATIVE("minor_negative", -1, 200, 20, 20),
    // Capped at 25, the amount one cure gives, for the same reason as MAJOR_POSITIVE below.
    MINOR_POSITIVE("minor_positive", 1, 25, 1, 5),
    // Capped at 20, the amount one cure gives, so curing the same villager again adds nothing. It
    // was 100 in 1.14, which let a villager be cured over and over for an ever bigger discount.
    MAJOR_POSITIVE("major_positive", 5, 20, 0, 100),
    TRADING("trading", 1, 25, 2, 20);

    /** The name this is stored under, matching 1.14's so the saved shape is recognisable. */
    public final String id;

    /** What one point of this opinion is worth when reputation is totted up. */
    public final int weight;

    /** How much of this opinion can accumulate. */
    public final int max;

    /** How much wears off per day. */
    public final int decay;

    /** How much is lost each time this is passed on to another villager. */
    public final int shareFloor;

    GossipType(String id, int weight, int max, int decay, int shareFloor) {
        this.id = id;
        this.weight = weight;
        this.max = max;
        this.decay = decay;
        this.shareFloor = shareFloor;
    }

    @Nullable
    public static GossipType byId(String id) {
        for (GossipType type : values()) {
            if (type.id.equals(id)) {
                return type;
            }
        }
        return null;
    }
}
