package com.exiledradio.villagerbackport.home;

import net.minecraft.block.Block;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.pathfinding.Path;
import net.minecraft.pathfinding.PathPoint;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * The current game's {@code InteractWithDoor}: while a villager is following a path, the wooden door
 * at the point it has just left or the point it is heading for is opened, and every door it opened or
 * walked through is shut again once it is past - unless another villager is coming through it.
 *
 * <h2>Why 1.12.2's door goals had to go</h2>
 * {@code EntityAIOpenDoor} only opens a door the villager has already walked into: it waits for a
 * horizontal collision and looks two path points ahead. {@code EntityAIRestrictOpenDoor} is worse at
 * night: near any village door it switches the villager's navigator to "cannot enter or open doors",
 * so a villager reaching its own front door at dusk could no longer path through it to its bed.
 *
 * <h2>Why every tick, and why the check for others</h2>
 * An earlier version opened the doors on a path once, when the path was taken, and shut them behind.
 * Two villagers heading home through one door then shut it on each other: the first through closed it
 * while the second stood in the doorway, and the second - its path unchanged - never opened it again
 * and stood pressed against it all night. The game asks about the two nearest path points on every
 * tick, which re-opens a door shut in a villager's face, and leaves open a door that another villager
 * within two blocks is passing through.
 *
 * <p>Uses no movement or look mutex, so it runs beside whatever goal is walking the villager, as the
 * game's runs in the core activity beside every other.
 */
public final class EntityAIInteractWithDoors extends EntityAIBase {

    /** The game's {@code SKIP_CLOSING_DOOR_IF_FURTHER_AWAY_THAN}: a door this far behind is forgotten. */
    private static final double FORGET_BEYOND = 3.0D;

    /** The game's {@code MAX_DISTANCE_TO_HOLD_DOOR_OPEN_FOR_OTHER_MOBS}. */
    private static final double HOLD_FOR_OTHERS_WITHIN = 2.0D;

    private final EntityVillager villager;

    /** Doors this villager opened or walked through and has not shut or forgotten yet. */
    private final Set<BlockPos> toClose = new HashSet<BlockPos>();

    public EntityAIInteractWithDoors(EntityVillager villager) {
        this.villager = villager;
        this.setMutexBits(0);
    }

    @Override
    public boolean shouldExecute() {
        Path path = this.villager.getNavigator().getPath();
        // The game's: a path that has not started or is done is left alone.
        return path != null && path.getCurrentPathIndex() > 0 && !path.isFinished();
    }

    @Override
    public boolean shouldContinueExecuting() {
        return shouldExecute();
    }

    @Override
    public void updateTask() {
        Path path = this.villager.getNavigator().getPath();
        if (path == null || path.getCurrentPathIndex() <= 0 || path.isFinished()) {
            return;
        }

        World world = this.villager.world;
        BlockPos previous = at(path, path.getCurrentPathIndex() - 1);
        BlockPos next = at(path, path.getCurrentPathIndex());

        // The door just walked through is remembered whether or not it had to be opened; the one ahead
        // only when this villager is the one opening it.
        IBlockState state = world.getBlockState(previous);
        if (isWoodenDoor(state)) {
            open(world, previous, state);
            this.toClose.add(previous);
        }
        state = world.getBlockState(next);
        if (isWoodenDoor(state) && !isOpen(world, next, state)) {
            open(world, next, state);
            this.toClose.add(next);
        }

        closeBehind(world, previous, next);
    }

    /** The game's {@code closeDoorsThatIHaveOpenedOrPassedThrough}. */
    private void closeBehind(World world, BlockPos previous, BlockPos next) {
        for (Iterator<BlockPos> it = this.toClose.iterator(); it.hasNext(); ) {
            BlockPos door = it.next();
            if (door.equals(previous) || door.equals(next)) {
                continue;
            }
            it.remove();

            if (this.villager.getDistanceSqToCenter(door) >= FORGET_BEYOND * FORGET_BEYOND) {
                continue;
            }
            IBlockState state = world.getBlockState(door);
            if (!isWoodenDoor(state) || !isOpen(world, door, state) || othersComingThrough(world, door)) {
                continue;
            }
            ((BlockDoor) state.getBlock()).toggleDoor(world, door, false);
        }
    }

    /** The game's {@code areOtherMobsComingThroughDoor}: another villager near it with the door next on its path. */
    private boolean othersComingThrough(World world, BlockPos door) {
        AxisAlignedBB near = new AxisAlignedBB(door).grow(HOLD_FOR_OTHERS_WITHIN);
        for (EntityVillager other : world.getEntitiesWithinAABB(EntityVillager.class, near)) {
            if (other == this.villager || other.getDistanceSqToCenter(door) >= HOLD_FOR_OTHERS_WITHIN * HOLD_FOR_OTHERS_WITHIN) {
                continue;
            }
            Path path = other.getNavigator().getPath();
            if (path == null || path.isFinished() || path.getCurrentPathIndex() <= 0) {
                continue;
            }
            if (door.equals(at(path, path.getCurrentPathIndex() - 1)) || door.equals(at(path, path.getCurrentPathIndex()))) {
                return true;
            }
        }
        return false;
    }

    private static void open(World world, BlockPos pos, IBlockState state) {
        if (!isOpen(world, pos, state)) {
            ((BlockDoor) state.getBlock()).toggleDoor(world, pos, true);
        }
    }

    /** Reads the open flag from the lower half, which is where 1.12.2 keeps it. */
    private static boolean isOpen(World world, BlockPos pos, IBlockState state) {
        IBlockState lower = state.getValue(BlockDoor.HALF) == BlockDoor.EnumDoorHalf.LOWER ? state : world.getBlockState(pos.down());
        return lower.getBlock() instanceof BlockDoor && lower.getValue(BlockDoor.OPEN);
    }

    @Nullable
    private static BlockPos at(Path path, int index) {
        if (index < 0 || index >= path.getCurrentPathLength()) {
            return null;
        }
        PathPoint p = path.getPathPointFromIndex(index);
        return new BlockPos(p.x, p.y, p.z);
    }

    /** @return true for the lower or upper half of a wooden door (the game's {@code WOODEN_DOORS} tag) */
    static boolean isWoodenDoor(@Nullable IBlockState state) {
        if (state == null) {
            return false;
        }
        Block block = state.getBlock();
        return block instanceof BlockDoor && state.getMaterial() == Material.WOOD;
    }
}
