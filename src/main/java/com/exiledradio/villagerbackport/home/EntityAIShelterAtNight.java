package com.exiledradio.villagerbackport.home;

import com.exiledradio.villagerbackport.ModConfig;

import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;

/**
 * A villager with no bed waits out the night indoors.
 *
 * <p>The game's villager without a bed goes inside a house at night and stays there until morning,
 * and walks out again only if it notices a free bed - which {@link HomeClaims} keeps looking for, so
 * the moment one is claimed this gives way to {@link EntityAISleep}.
 *
 * <p>"Inside" is read as anywhere the sky cannot be seen: under a roof. A villager already under one
 * simply stays where it is. One out in the open looks for a roofed spot nearby, goes there and
 * stays. If there is nowhere roofed in reach it is left to do as it likes.
 */
public final class EntityAIShelterAtNight extends EntityAIBase {

    /** How far a villager will look for somewhere roofed. */
    private static final int SEARCH = 16;
    private static final int SEARCH_HEIGHT = 6;
    private static final int TRIES = 40;

    private final EntityVillager villager;

    @Nullable
    private BlockPos shelter;

    public EntityAIShelterAtNight(EntityVillager villager) {
        this.villager = villager;

        // Movement and looking, like sleep: a villager waiting indoors is not wandering about.
        this.setMutexBits(3);
    }

    @Override
    public boolean shouldExecute() {
        if (!applies()) {
            return false;
        }

        BlockPos here = new BlockPos(this.villager);
        this.shelter = isRoofed(this.villager.world, here) ? here : findRoofed();
        return this.shelter != null;
    }

    private boolean applies() {
        return ModConfig.homes.enabled && ModConfig.homes.sleepAtNight
                && this.villager.getCustomer() == null
                && EntityAISleep.isBedtime(this.villager)
                && HomeSite.validated(this.villager) == null
                && !PanicWatch.isPanicking(this.villager);
    }

    @Override
    public void startExecuting() {
        if (!this.shelter.equals(new BlockPos(this.villager))) {
            this.villager.getNavigator().tryMoveToXYZ(
                    this.shelter.getX() + 0.5D, this.shelter.getY(), this.shelter.getZ() + 0.5D, 0.5D);
        }
    }

    @Override
    public boolean shouldContinueExecuting() {
        // Still night, still no bed. Holding the movement mutex all the while is what keeps the
        // villager from wandering back out once it is in.
        return applies();
    }

    @Override
    public void updateTask() {
        // Arrived, or stopped short under some other roof: either way it is in, so it waits.
        if (isRoofed(this.villager.world, new BlockPos(this.villager))) {
            this.villager.getNavigator().clearPath();
        } else if (this.villager.getNavigator().noPath() && this.shelter != null) {
            this.villager.getNavigator().tryMoveToXYZ(
                    this.shelter.getX() + 0.5D, this.shelter.getY(), this.shelter.getZ() + 0.5D, 0.5D);
        }
    }

    @Override
    public void resetTask() {
        this.shelter = null;
    }

    @Nullable
    private BlockPos findRoofed() {
        World world = this.villager.world;
        BlockPos origin = new BlockPos(this.villager);

        for (int i = 0; i < TRIES; i++) {
            BlockPos probe = origin.add(
                    this.villager.getRNG().nextInt(SEARCH * 2 + 1) - SEARCH,
                    this.villager.getRNG().nextInt(SEARCH_HEIGHT * 2 + 1) - SEARCH_HEIGHT,
                    this.villager.getRNG().nextInt(SEARCH * 2 + 1) - SEARCH);

            if (isRoofed(world, probe) && canStand(world, probe) && reaches(probe)) {
                return probe;
            }
        }

        return null;
    }

    /** A path that actually gets there, not one that stops at the nearest wall. */
    private boolean reaches(BlockPos target) {
        net.minecraft.pathfinding.Path path = this.villager.getNavigator().getPathToPos(target);
        net.minecraft.pathfinding.PathPoint end = path == null ? null : path.getFinalPathPoint();
        return end != null && Math.abs(end.x - target.getX()) <= 1
                && Math.abs(end.y - target.getY()) <= 1 && Math.abs(end.z - target.getZ()) <= 1;
    }

    private static boolean isRoofed(World world, BlockPos pos) {
        return world.isBlockLoaded(pos) && !world.canSeeSky(pos);
    }

    private static boolean canStand(World world, BlockPos pos) {
        return world.getBlockState(pos.down()).isSideSolid(world, pos.down(), net.minecraft.util.EnumFacing.UP)
                && !world.getBlockState(pos).getMaterial().blocksMovement()
                && !world.getBlockState(pos.up()).getMaterial().blocksMovement();
    }
}
