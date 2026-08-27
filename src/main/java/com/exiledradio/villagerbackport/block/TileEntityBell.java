package com.exiledradio.villagerbackport.block;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;

/**
 * A bell's swing.
 *
 * <h2>State that only the client needs</h2>
 * None of this is saved or sent as tile entity data. A swing lasts two and a half seconds and means
 * nothing afterwards, so persisting it would be pointless - a bell rung as the world saved should
 * not still be swinging when it loads.
 *
 * <p>The ring is announced with a block event instead: the server calls
 * {@code world.addBlockEvent}, which every client watching the block receives and turns into a
 * swing locally. That is how 1.14 does it and how vanilla drives chest lids and note blocks - one
 * tiny packet, no synchronised state, and clients out of range simply never hear about it.
 */
public class TileEntityBell extends TileEntity implements ITickable {

    /** Ticks a swing lasts before the bell settles, matching 1.14. */
    public static final int SWING_TICKS = 50;

    /** Block event id for "the bell was rung", paired with the struck side. */
    public static final int EVENT_RING = 1;

    private boolean ringing;
    private int ringTicks;
    private EnumFacing struckFrom = EnumFacing.NORTH;

    public boolean isRinging() {
        return this.ringing;
    }

    public int ringTicks() {
        return this.ringTicks;
    }

    public EnumFacing struckFrom() {
        return this.struckFrom;
    }

    /**
     * Starts a swing, or restarts one already under way.
     *
     * <p>Restarting rather than ignoring the second ring is deliberate: striking a swinging bell
     * again should set it going afresh, not be swallowed.
     */
    @Override
    public boolean receiveClientEvent(int id, int param) {
        if (id != EVENT_RING) {
            return super.receiveClientEvent(id, param);
        }

        this.struckFrom = EnumFacing.byIndex(param);
        this.ringTicks = 0;
        this.ringing = true;
        return true;
    }

    @Override
    public void update() {
        if (!this.ringing) {
            return;
        }

        this.ringTicks++;
        if (this.ringTicks >= SWING_TICKS) {
            this.ringing = false;
            this.ringTicks = 0;
        }
    }

    /**
     * Keeps the tile entity across block state changes, so a bell does not lose its swing to an
     * unrelated update - the same guard the barrel and furnaces needed.
     */
    @Override
    public boolean shouldRefresh(net.minecraft.world.World world, net.minecraft.util.math.BlockPos pos,
                                 net.minecraft.block.state.IBlockState oldState,
                                 net.minecraft.block.state.IBlockState newState) {
        return oldState.getBlock() != newState.getBlock();
    }

    /**
     * The bell body is drawn beyond the block it sits in, so the renderer has to keep working when
     * the block itself is off screen.
     */
    @Override
    public net.minecraft.util.math.AxisAlignedBB getRenderBoundingBox() {
        return new net.minecraft.util.math.AxisAlignedBB(this.pos).grow(1.0D);
    }
}
