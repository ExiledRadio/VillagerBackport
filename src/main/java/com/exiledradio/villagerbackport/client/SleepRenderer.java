package com.exiledradio.villagerbackport.client;

import net.minecraft.block.BlockBed;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Draws a sleeping villager lying in its bed.
 *
 * <h2>Why this is a transform and not a pose</h2>
 * 1.14 gives every living entity a sleeping pose and a renderer that reads it. 1.12.2 has nothing of
 * the kind - the only thing in the game that lies down is the player, and that is written into the
 * player renderer specifically. Rather than replace the villager renderer wholesale, which would
 * fight every mod that decorates it, the model is simply tipped over before vanilla draws it.
 *
 * <p>Rotating about the villager's own position is what the two translations are for: GL transforms
 * compose, and rotating first would swing the villager around the world origin rather than turning
 * it on the spot. Translate to it, turn it, translate back, and vanilla's own drawing follows.
 */
@SideOnly(Side.CLIENT)
public final class SleepRenderer {

    /**
     * How far down the bed a sleeping villager is drawn.
     *
     * <p>Vanilla uses 1.8 for a player, in {@code EntityPlayer.setRenderOffsetForSleep}, because the
     * model is drawn from the block it sleeps in and would otherwise lie with its feet on the
     * pillow. A villager is shorter and differently proportioned, and 1.5 is where it lands right.
     */
    private static final double ALONG_BED = 1.5D;

    /**
     * Vanilla's own bed angles, from {@code EntityPlayer.getBedOrientationInDegrees}.
     *
     * <p>Not derived. The first attempt reasoned about which way to tip the model and produced a
     * villager lying across its bed - so these are the numbers the game already uses to lay a
     * player down, read out of the source rather than worked out again.
     */
    private static float angleOf(EnumFacing facing) {
        switch (facing) {
            case SOUTH:
                return 90.0F;
            case WEST:
                return 0.0F;
            case NORTH:
                return 270.0F;
            case EAST:
                return 180.0F;
            default:
                return 0.0F;
        }
    }

    @SubscribeEvent
    public void onRenderPre(RenderLivingEvent.Pre<EntityVillager> event) {
        if (!(event.getEntity() instanceof EntityVillager)) {
            return;
        }

        BlockPos bed = SleepCache.bedOf(event.getEntity().getEntityId());
        if (bed == null) {
            return;
        }

        EnumFacing facing = facingOf(event.getEntity().world.getBlockState(bed));

        GlStateManager.pushMatrix();

        // Vanilla shifts a sleeping body down the bed before drawing it, in
        // EntityPlayer.setRenderOffsetForSleep: renderOffsetX/Z = -1.8 along the bed's facing. A
        // player has fields for that and a villager does not, so the same shift is applied here.
        // Without it the model is drawn centred on the head block and ends up with its feet on the
        // pillow, which is exactly how it looked.
        // A baby is drawn at half scale but shifted by the same world-space distance, so the shift
        // that lays an adult correctly carries a child right off the end of the mattress. It needs
        // a block less.
        double along = event.getEntity().isChild() ? ALONG_BED - 1.0D : ALONG_BED;

        GlStateManager.translate(
                -along * facing.getDirectionVec().getX(),
                0.0D,
                -along * facing.getDirectionVec().getZ());

        GlStateManager.translate(event.getX(), event.getY(), event.getZ());

        // The three rotations RenderPlayer.applyRotations uses for a sleeping player, in its
        // order. They compose correctly only because the villager's facing is pinned to 180 while
        // it sleeps, which makes vanilla's own rotate(180 - yaw) the identity - see
        // EntityAISleep.lieStill.
        GlStateManager.rotate(angleOf(facing), 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(90.0F, 0.0F, 0.0F, 1.0F);
        GlStateManager.rotate(270.0F, 0.0F, 1.0F, 0.0F);

        GlStateManager.translate(-event.getX(), -event.getY(), -event.getZ());
    }

    @SubscribeEvent
    public void onRenderPost(RenderLivingEvent.Post<EntityVillager> event) {
        if (event.getEntity() instanceof EntityVillager
                && SleepCache.bedOf(event.getEntity().getEntityId()) != null) {
            GlStateManager.popMatrix();
        }
    }

    /** @return which way the bed points, or north if the block is no longer a bed. */
    private static EnumFacing facingOf(IBlockState state) {
        return state.getBlock() instanceof BlockBed
                ? state.getValue(BlockBed.FACING)
                : EnumFacing.NORTH;
    }
}
