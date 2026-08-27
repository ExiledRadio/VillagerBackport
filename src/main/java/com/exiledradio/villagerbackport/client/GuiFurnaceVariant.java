package com.exiledradio.villagerbackport.client;

import com.exiledradio.villagerbackport.block.ContainerFurnaceVariant;
import com.exiledradio.villagerbackport.block.FurnaceKind;
import com.exiledradio.villagerbackport.block.TileEntityFurnaceVariant;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * The smoker and blast furnace screen, using 1.14's own backgrounds.
 *
 * <h2>Why not just reuse the furnace screen</h2>
 * Mechanically it is a furnace screen - same three slots in the same places, same flame and same
 * progress arrow - and the container is vanilla's. Only the background differs, and 1.14 gives each
 * of these blocks its own so they read as distinct machines rather than reskinned furnaces.
 *
 * <p>Subclassing {@code GuiFurnace} would have been shorter, but its background drawing is a single
 * method with the texture baked in - overriding it means reproducing the whole thing anyway, and
 * inheriting a field pointing at the wrong texture is a trap for whoever reads it next.
 */
@SideOnly(Side.CLIENT)
public class GuiFurnaceVariant extends GuiContainer {

    private static final ResourceLocation SMOKER =
            new ResourceLocation("villagerbackport", "textures/gui/smoker.png");
    private static final ResourceLocation BLAST_FURNACE =
            new ResourceLocation("villagerbackport", "textures/gui/blast_furnace.png");

    private final InventoryPlayer playerInventory;
    private final TileEntityFurnaceVariant furnace;

    public GuiFurnaceVariant(InventoryPlayer playerInventory, TileEntityFurnaceVariant furnace) {
        super(new ContainerFurnaceVariant(playerInventory, furnace));
        this.playerInventory = playerInventory;
        this.furnace = furnace;
    }

    /**
     * Draws the screen, including the tooltip for whatever the cursor is over.
     *
     * <p>{@link GuiContainer} defines {@code renderHoveredToolTip} but never calls it - every screen
     * has to do so itself, and vanilla's own container screens all carry this same three-line
     * override. Without it the screen works perfectly and silently shows no item tooltips, which is
     * exactly how it went unnoticed.
     */
    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        renderHoveredToolTip(mouseX, mouseY);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        String title = this.furnace.getDisplayName().getUnformattedText();
        this.fontRenderer.drawString(title, this.xSize / 2 - this.fontRenderer.getStringWidth(title) / 2,
                6, 4210752);
        this.fontRenderer.drawString(this.playerInventory.getDisplayName().getUnformattedText(),
                8, this.ySize - 96 + 2, 4210752);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(
                this.furnace.kind() == FurnaceKind.SMOKER ? SMOKER : BLAST_FURNACE);

        drawTexturedModalRect(this.guiLeft, this.guiTop, 0, 0, this.xSize, this.ySize);

        // Flame, drawn from the bottom up so it burns down as fuel is used.
        if (isBurning()) {
            int height = burnLeftScaled(13);
            drawTexturedModalRect(this.guiLeft + 56, this.guiTop + 36 + 12 - height,
                    176, 12 - height, 14, height + 1);
        }

        // Progress arrow.
        int progress = cookProgressScaled(24);
        drawTexturedModalRect(this.guiLeft + 79, this.guiTop + 34, 176, 14, progress + 1, 16);
    }

    private boolean isBurning() {
        return this.furnace.getField(0) > 0;
    }

    /**
     * @return how much of the flame to draw
     *
     * <p>Falls back to a nominal full burn when the total is unknown. The server sends both numbers,
     * but the total can arrive as zero for a tick after the screen opens, and dividing by it would
     * make the flame flicker on the first frame.
     */
    private int burnLeftScaled(int pixels) {
        int total = this.furnace.getField(1);
        if (total == 0) {
            total = 200;
        }
        return this.furnace.getField(0) * pixels / total;
    }

    private int cookProgressScaled(int pixels) {
        int total = this.furnace.getField(3);
        return total == 0 ? 0 : this.furnace.getField(2) * pixels / total;
    }
}
