package com.exiledradio.villagerbackport.cat;

import com.exiledradio.villagerbackport.home.HomeSite;

import net.minecraft.block.BlockBed;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.ai.EntityAIMoveToBlock;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * A tamed cat going to sit on a chest, a lit furnace or the foot of a bed. 1.14's
 * {@code CatSitOnBlockGoal}.
 *
 * <p>A chest only while nobody has it open. A cat sitting on a chest stops it opening
 * ({@link CatFear#onChestOpen}), as in 1.14.
 */
public class EntityAICatSitOnBlock extends EntityAIMoveToBlock {

    private final EntityCat cat;

    public EntityAICatSitOnBlock(EntityCat cat, double speed) {
        super(cat, speed, 8);
        this.cat = cat;
    }

    @Override
    public boolean shouldExecute() {
        return this.cat.isTamed() && !this.cat.isSitting() && super.shouldExecute();
    }

    @Override
    public void startExecuting() {
        super.startExecuting();
        this.cat.orderSit(false);
    }

    @Override
    public void resetTask() {
        super.resetTask();
        // Interrupted by an order to sit, it must not stand the cat back up.
        if (!this.cat.isOrderedToSit()) {
            this.cat.setSitting(false);
        }
    }

    @Override
    public void updateTask() {
        super.updateTask();
        this.cat.orderSit(false);
        if (!this.getIsAboveDestination()) {
            this.cat.setSitting(false);
        } else if (!this.cat.isSitting()) {
            this.cat.setSitting(true);
        }
    }

    @Override
    protected boolean shouldMoveTo(World world, BlockPos pos) {
        if (!world.isAirBlock(pos.up())) {
            return false;
        }
        IBlockState state = world.getBlockState(pos);
        if (state.getBlock() == Blocks.CHEST) {
            TileEntity tile = world.getTileEntity(pos);
            return tile instanceof TileEntityChest && ((TileEntityChest) tile).numPlayersUsing < 1;
        }
        if (state.getBlock() == Blocks.LIT_FURNACE) {
            return true;
        }
        return HomeSite.isBed(state) && state.getBlock() instanceof BlockBed
                && state.getValue(BlockBed.PART) != BlockBed.EnumPartType.HEAD;
    }
}
