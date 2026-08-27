package com.exiledradio.villagerbackport.job;

import com.exiledradio.villagerbackport.ModConfig;

import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.pathfinding.Path;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nullable;

import java.util.UUID;

/**
 * Lets a villager path further than usual, but only while it is pathing to work.
 *
 * <h2>Why the reach has to be borrowed rather than kept</h2>
 * A navigator will not look for a path further than the entity's follow range, so reaching a
 * workstation across a large village means raising it. Raising it and leaving it raised is the
 * obvious way to do that and it is a bad mistake: follow range is what
 * {@code PathNavigate.getPathSearchRange} returns for <em>every</em> path the entity asks for.
 *
 * <p>So a villager left with a permanently large follow range does not merely walk further to work -
 * it searches a far larger volume every time it wanders, heads indoors, flees a zombie or follows a
 * golem, none of which needed the extra reach. Tripling it triples the cost of all of that, and in a
 * village with a lot of villagers the result is exactly what it sounds like: everything moves in
 * jerks.
 *
 * <p>Borrowing it for the one call that needs it costs a map write and a recalculation, which is
 * nothing beside a pathfind, and leaves every other goal searching the distance it was written for.
 */
public final class WorkPathing {

    /** Identifies the modifier so it can be found again and taken off. */
    private static final UUID REACH = UUID.fromString("7b1d5a4e-9c2f-4a30-8d61-2f9b0c7e4a11");

    private WorkPathing() {
    }

    /** @return a path to the workstation, computed with the longer reach */
    @Nullable
    public static Path pathTo(EntityVillager villager, BlockPos pos) {
        IAttributeInstance range = borrow(villager);

        try {
            return villager.getNavigator().getPathToPos(pos);
        } finally {
            give(range);
        }
    }

    /** Starts the villager walking to the workstation, with the longer reach for the path itself. */
    public static void walkTo(EntityVillager villager, BlockPos pos, double speed) {
        IAttributeInstance range = borrow(villager);

        try {
            villager.getNavigator().tryMoveToXYZ(
                    pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, speed);
        } finally {
            give(range);
        }
    }

    /**
     * Takes off a modifier left behind by an older version of this mod.
     *
     * <p>Attributes are saved with the entity, so a villager that met the version which applied this
     * permanently is still carrying it - and would go on searching the larger volume forever, on a
     * world where nothing was doing that deliberately any more.
     */
    public static void removeStale(EntityVillager villager) {
        IAttributeInstance range = villager.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE);

        if (range != null && range.getModifier(REACH) != null) {
            range.removeModifier(REACH);
        }
    }

    /**
     * @return how far a path for this villager will actually be searched
     *
     * <p>Not the same as reading the attribute: the extra reach is only on the villager while a path
     * is being worked out, so anything asking outside that window sees the base figure and would
     * wrongly conclude the villager cannot path that far.
     */
    public static double effectiveRange(EntityVillager villager) {
        IAttributeInstance range = villager.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE);
        double base = range == null ? 16.0D : range.getAttributeValue();

        if (!ModConfig.jobs.extendPathfindingRange) {
            return base;
        }

        return Math.max(base, ModConfig.jobs.searchRadius + 8.0D);
    }

    @Nullable
    private static IAttributeInstance borrow(EntityVillager villager) {
        if (!ModConfig.jobs.extendPathfindingRange) {
            return null;
        }

        IAttributeInstance range = villager.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE);
        if (range == null || range.getModifier(REACH) != null) {
            return null;
        }

        double extra = ModConfig.jobs.searchRadius + 8.0D - range.getBaseValue();
        if (extra <= 0.0D) {
            return null;
        }

        range.applyModifier(new AttributeModifier(REACH, "villagerbackport.workReach", extra, 0));
        return range;
    }

    private static void give(@Nullable IAttributeInstance range) {
        if (range != null) {
            range.removeModifier(REACH);
        }
    }
}
