package com.exiledradio.villagerbackport.client;

import com.exiledradio.villagerbackport.block.BlockLectern;
import com.exiledradio.villagerbackport.block.TileEntityLectern;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.model.ModelBook;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * The book lying open on a lectern.
 *
 * <p>Drawn rather than modelled because it is not always there: a block model would need a second
 * variant for every facing, and would still be a flat approximation of an open book. Vanilla already
 * ships the model - it is the one that floats above an enchanting table - and its texture, so this is
 * the geometry 1.14 uses, placed the way 1.14 places it.
 */
@SideOnly(Side.CLIENT)
public class TileEntityLecternRenderer extends TileEntitySpecialRenderer<TileEntityLectern> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation("textures/entity/enchanting_table_book.png");

    /** How far open the book lies, and how the two flipping pages sit. 1.14's numbers. */
    private static final float SPREAD = 1.2F;
    private static final float FLIP_LEFT = 0.1F;
    private static final float FLIP_RIGHT = 0.9F;

    /** Matches the slope of the stand's top face. */
    private static final float TILT = 67.5F;

    private final ModelBook model = new ModelBook();

    @Override
    public void render(TileEntityLectern lectern, double x, double y, double z,
                       float partialTicks, int destroyStage, float alpha) {
        IBlockState state = lectern.getWorld().getBlockState(lectern.getPos());

        if (!(state.getBlock() instanceof BlockLectern)
                || !state.getValue(BlockLectern.HAS_BOOK).booleanValue()) {
            return;
        }

        GlStateManager.pushMatrix();
        GlStateManager.translate((float) x + 0.5F, (float) y + 1.0F + 0.0625F, (float) z + 0.5F);

        // Turned a quarter past the block's facing, so the book faces whoever is standing in front
        // of the stand rather than lying along it.
        float angle = state.getValue(BlockLectern.FACING).rotateY().getHorizontalAngle();
        GlStateManager.rotate(-angle, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(TILT, 0.0F, 0.0F, 1.0F);
        GlStateManager.translate(0.0F, -0.125F, 0.0F);

        bindTexture(TEXTURE);
        GlStateManager.enableCull();

        // No animation: the enchanting table's copy hovers and riffles, a lectern's lies still.
        this.model.render(null, 0.0F, FLIP_LEFT, FLIP_RIGHT, SPREAD, 0.0F, 0.0625F);

        GlStateManager.popMatrix();
    }
}
