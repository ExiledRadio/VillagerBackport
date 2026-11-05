package com.exiledradio.villagerbackport.flora.client;

import com.exiledradio.villagerbackport.flora.SignColor;

import net.minecraft.block.Block;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiUtilRenderComponents;
import net.minecraft.client.model.ModelSign;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntitySignRenderer;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;

/**
 * 1.12.2's sign renderer with the text in the sign's dye color.
 *
 * <p>An undyed sign - and any sign while it is being broken - is handed straight to vanilla's
 * renderer, so for every sign nobody has dyed nothing at all changes. A dyed one is drawn by a copy
 * of vanilla's {@code render} whose only difference is the color passed to {@code drawString}, which
 * vanilla hard-codes to black. See {@link SignColor} for the colors.
 */
@SideOnly(Side.CLIENT)
public class TileEntityDyedSignRenderer extends TileEntitySignRenderer {

    private static final ResourceLocation SIGN_TEXTURE = new ResourceLocation("textures/entity/sign.png");

    private final ModelSign model = new ModelSign();

    @Override
    public void render(TileEntitySign te, double x, double y, double z, float partialTicks, int destroyStage,
                       float alpha) {
        int color = SignColor.textColor(te);
        if (color == 0 || destroyStage >= 0) {
            super.render(te, x, y, z, partialTicks, destroyStage, alpha);
            return;
        }

        Block block = te.getBlockType();
        GlStateManager.pushMatrix();

        if (block == Blocks.STANDING_SIGN) {
            GlStateManager.translate((float) x + 0.5F, (float) y + 0.5F, (float) z + 0.5F);
            float rotation = (float) (te.getBlockMetadata() * 360) / 16.0F;
            GlStateManager.rotate(-rotation, 0.0F, 1.0F, 0.0F);
            this.model.signStick.showModel = true;
        } else {
            int meta = te.getBlockMetadata();
            float rotation = 0.0F;
            if (meta == 2) {
                rotation = 180.0F;
            }
            if (meta == 4) {
                rotation = 90.0F;
            }
            if (meta == 5) {
                rotation = -90.0F;
            }
            GlStateManager.translate((float) x + 0.5F, (float) y + 0.5F, (float) z + 0.5F);
            GlStateManager.rotate(-rotation, 0.0F, 1.0F, 0.0F);
            GlStateManager.translate(0.0F, -0.3125F, -0.4375F);
            this.model.signStick.showModel = false;
        }

        this.bindTexture(SIGN_TEXTURE);
        GlStateManager.enableRescaleNormal();
        GlStateManager.pushMatrix();
        GlStateManager.scale(0.6666667F, -0.6666667F, -0.6666667F);
        this.model.renderSign();
        GlStateManager.popMatrix();

        FontRenderer font = this.getFontRenderer();
        GlStateManager.translate(0.0F, 0.33333334F, 0.046666667F);
        GlStateManager.scale(0.010416667F, -0.010416667F, 0.010416667F);
        GlStateManager.glNormal3f(0.0F, 0.0F, -0.010416667F);
        GlStateManager.depthMask(false);

        for (int line = 0; line < te.signText.length; line++) {
            if (te.signText[line] == null) {
                continue;
            }
            ITextComponent text = te.signText[line];
            List<ITextComponent> split = GuiUtilRenderComponents.splitText(text, 90, font, false, true);
            String s = split != null && !split.isEmpty() ? split.get(0).getFormattedText() : "";
            if (line == te.lineBeingEdited) {
                s = "> " + s + " <";
            }
            font.drawString(s, -font.getStringWidth(s) / 2, line * 10 - te.signText.length * 5, color);
        }

        GlStateManager.depthMask(true);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.popMatrix();
    }
}
