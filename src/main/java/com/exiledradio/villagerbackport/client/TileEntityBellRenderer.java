package com.exiledradio.villagerbackport.client;

import com.exiledradio.villagerbackport.block.TileEntityBell;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Draws the bell body, and swings it when rung.
 *
 * <h2>Why the body is not in the block model</h2>
 * It was, as static geometry, because a block model is simpler than a renderer and the bell had
 * nothing to animate. Making it swing changes that: a block model cannot move, so the body has to be
 * drawn in code. It is removed from the model at the same time, or it would appear twice - once
 * hanging still and once swinging through it.
 *
 * <h2>The model and the swing</h2>
 * Both taken from 1.14's {@code BellModel} and its renderer rather than approximated: a 6x7x6 body
 * and an 8x2x8 lip on a 32x32 sheet, and a swing of
 * {@code sin(t / PI) / (4 + t / 3)} - a sine that decays as the bell settles, applied about the axis
 * across the direction it was struck from.
 */
@SideOnly(Side.CLIENT)
public class TileEntityBellRenderer extends TileEntitySpecialRenderer<TileEntityBell> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation("villagerbackport", "textures/blocks/bell_body.png");

    private final BellModel model = new BellModel();

    @Override
    public void render(TileEntityBell bell, double x, double y, double z,
                       float partialTicks, int destroyStage, float alpha) {
        float swing = bell.isRinging() ? bell.ringTicks() + partialTicks : 0.0F;

        // Decaying sine: quick at first, settling toward nothing. Zero while at rest, so a still
        // bell hangs exactly straight rather than a hair off.
        float amount = swing > 0.0F
                ? MathHelper.sin(swing / (float) Math.PI) / (4.0F + swing / 3.0F)
                : 0.0F;

        // A bell struck from the north swings north-south, one struck from the east swings
        // east-west - so the axis is the one the strike came along.
        EnumFacing struck = bell.struckFrom();
        float rotateX = 0.0F;
        float rotateZ = 0.0F;

        switch (struck) {
            case NORTH: rotateX = -amount; break;
            case SOUTH: rotateX = amount; break;
            case EAST: rotateZ = -amount; break;
            case WEST: rotateZ = amount; break;
            default: break;
        }

        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        bindTexture(TEXTURE);

        this.model.render(rotateX, rotateZ);

        GlStateManager.popMatrix();
    }

    /**
     * 1.14's bell, box for box.
     *
     * <p>The lip is a child of the body so it inherits the swing without being rotated separately -
     * the two are one object that happens to be built from two boxes.
     */
    private static class BellModel extends ModelBase {

        private final ModelRenderer body;

        BellModel() {
            this.textureWidth = 32;
            this.textureHeight = 32;

            this.body = new ModelRenderer(this, 0, 0);
            this.body.addBox(-3.0F, -6.0F, -3.0F, 6, 7, 6);
            this.body.setRotationPoint(8.0F, 12.0F, 8.0F);

            ModelRenderer lip = new ModelRenderer(this, 0, 13);
            lip.addBox(4.0F, 4.0F, 4.0F, 8, 2, 8);
            lip.setRotationPoint(-8.0F, -12.0F, -8.0F);

            this.body.addChild(lip);
        }

        void render(float rotateX, float rotateZ) {
            this.body.rotateAngleX = rotateX;
            this.body.rotateAngleZ = rotateZ;
            this.body.render(0.0625F);
        }
    }
}
