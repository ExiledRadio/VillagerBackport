package com.exiledradio.villagerbackport.building.client;

import com.exiledradio.villagerbackport.building.BlockWoodSign;
import com.exiledradio.villagerbackport.building.SignWood;
import com.exiledradio.villagerbackport.building.TileEntityWoodSign;

import net.minecraft.block.Block;
import net.minecraft.block.BlockStandingSign;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiUtilRenderComponents;
import net.minecraft.client.gui.inventory.GuiEditSign;
import net.minecraft.client.model.ModelSign;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;

/**
 * Draws the five new wood signs, with 1.14's textures.
 *
 * <h2>Why not vanilla's renderer with another texture</h2>
 * Vanilla's sign renderer decides whether a sign is standing by comparing its block to
 * {@code Blocks.STANDING_SIGN}, so it would draw every one of these as a wall sign, floating where
 * the post should be. This is that renderer with the comparison made an {@code instanceof} and the
 * texture read from the block's wood - otherwise line for line the same, text and all.
 *
 * <h2>The editing screen</h2>
 * {@link GuiEditSign} makes the same comparison when it turns the sign to face the player, so for a
 * standing sign it applies the wall rotation for the sign's metadata instead of the standing one.
 * Undoing that here is simpler than replacing the screen: when the screen draws a standing sign, this
 * rotates it by the wall angle the screen used, which leaves it facing front as an oak sign does.
 */
@SideOnly(Side.CLIENT)
public final class TileEntityWoodSignRenderer extends TileEntitySpecialRenderer<TileEntityWoodSign> {

    private static final ResourceLocation FALLBACK = new ResourceLocation("textures/entity/sign.png");

    private final ModelSign model = new ModelSign();

    @Override
    public void render(TileEntityWoodSign te, double x, double y, double z, float partialTicks, int destroyStage,
                       float alpha) {
        Block block = te.getBlockType();
        SignWood wood = BlockWoodSign.woodOf(block);
        GlStateManager.pushMatrix();

        if (block instanceof BlockStandingSign) {
            GlStateManager.translate((float) x + 0.5F, (float) y + 0.5F, (float) z + 0.5F);
            float angle = inEditScreen(x, y, z) ? wallAngle(te.getBlockMetadata()) : te.getBlockMetadata() * 360 / 16.0F;
            GlStateManager.rotate(-angle, 0.0F, 1.0F, 0.0F);
            this.model.signStick.showModel = true;
        } else {
            GlStateManager.translate((float) x + 0.5F, (float) y + 0.5F, (float) z + 0.5F);
            GlStateManager.rotate(-wallAngle(te.getBlockMetadata()), 0.0F, 1.0F, 0.0F);
            GlStateManager.translate(0.0F, -0.3125F, -0.4375F);
            this.model.signStick.showModel = false;
        }

        if (destroyStage >= 0) {
            bindTexture(DESTROY_STAGES[destroyStage]);
            GlStateManager.matrixMode(5890);
            GlStateManager.pushMatrix();
            GlStateManager.scale(4.0F, 2.0F, 1.0F);
            GlStateManager.translate(0.0625F, 0.0625F, 0.0625F);
            GlStateManager.matrixMode(5888);
        } else {
            bindTexture(wood != null ? wood.texture : FALLBACK);
        }

        GlStateManager.enableRescaleNormal();
        GlStateManager.pushMatrix();
        GlStateManager.scale(0.6666667F, -0.6666667F, -0.6666667F);
        this.model.renderSign();
        GlStateManager.popMatrix();

        FontRenderer font = getFontRenderer();
        GlStateManager.translate(0.0F, 0.33333334F, 0.046666667F);
        GlStateManager.scale(0.010416667F, -0.010416667F, 0.010416667F);
        GlStateManager.glNormal3f(0.0F, 0.0F, -0.010416667F);
        GlStateManager.depthMask(false);

        if (destroyStage < 0) {
            int color = 0;
            for (int line = 0; line < te.signText.length; ++line) {
                ITextComponent text = te.signText[line];
                if (text == null) {
                    continue;
                }
                List<ITextComponent> split = GuiUtilRenderComponents.splitText(text, 90, font, false, true);
                String s = split != null && !split.isEmpty() ? split.get(0).getFormattedText() : "";
                if (line == te.lineBeingEdited) {
                    s = "> " + s + " <";
                }
                font.drawString(s, -font.getStringWidth(s) / 2, line * 10 - te.signText.length * 5, color);
            }
        }

        GlStateManager.depthMask(true);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.popMatrix();

        if (destroyStage >= 0) {
            GlStateManager.matrixMode(5890);
            GlStateManager.popMatrix();
            GlStateManager.matrixMode(5888);
        }
    }

    /** Vanilla's wall-sign angle for a facing's metadata: north 0, south 180, west 90, east -90. */
    private static float wallAngle(int meta) {
        switch (meta) {
            case 2:
                return 180.0F;
            case 4:
                return 90.0F;
            case 5:
                return -90.0F;
            default:
                return 0.0F;
        }
    }

    /**
     * @return true when the sign editing screen is drawing this sign
     *
     * <p>The screen always draws at this one fixed offset. A sign in the world behind the screen would
     * only be passed the same numbers with the camera at exactly those fractions of a block from it,
     * and even then the cost is that one frame's sign turned on its post.
     */
    private static boolean inEditScreen(double x, double y, double z) {
        return Minecraft.getMinecraft().currentScreen instanceof GuiEditSign
                && x == -0.5D && y == -0.75D && z == -0.5D;
    }
}
