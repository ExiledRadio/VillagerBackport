package com.exiledradio.villagerbackport.cat;

import com.exiledradio.villagerbackport.home.HomeSite;

import net.minecraft.entity.ai.EntityAIMoveToBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * A tamed cat wandering over to a bed and lying down on it. 1.14's {@code CatLieOnBedGoal}.
 *
 * <p>Any bed with room above it, within eight blocks; the cat lies there until it gets bored (1.12.2's
 * move-to-block timer, one to two minutes) or something else wants it. 1.14 looks for a bed every two
 * seconds rather than every ten to twenty, which is what {@link #shouldExecute} restores.
 *
 * <p>Uses this mod's own bed test ({@link HomeSite#isBed}), so modded beds count wherever they count for
 * villagers.
 */
public class EntityAICatLieOnBed extends EntityAIMoveToBlock {

    private static final int SEARCH_INTERVAL = 40;

    private final EntityCat cat;

    public EntityAICatLieOnBed(EntityCat cat, double speed, int range) {
        super(cat, speed, range);
        this.cat = cat;
    }

    @Override
    public boolean shouldExecute() {
        if (!this.cat.isTamed() || this.cat.isSitting() || this.cat.isLying()) {
            return false;
        }
        boolean found = super.shouldExecute();
        // The parent has just set its own 10-20 second wait; 1.14 waits two.
        if (this.runDelay > SEARCH_INTERVAL) {
            this.runDelay = SEARCH_INTERVAL;
        }
        return found;
    }

    @Override
    public void startExecuting() {
        super.startExecuting();
        this.cat.orderSit(false);
    }

    @Override
    public void resetTask() {
        super.resetTask();
        this.cat.setLying(false);
    }

    @Override
    public void updateTask() {
        super.updateTask();
        this.cat.orderSit(false);
        if (!this.getIsAboveDestination()) {
            this.cat.setLying(false);
        } else if (!this.cat.isLying()) {
            this.cat.setLying(true);
        }
    }

    @Override
    protected boolean shouldMoveTo(World world, BlockPos pos) {
        return world.isAirBlock(pos.up()) && HomeSite.isBed(world.getBlockState(pos));
    }
}
