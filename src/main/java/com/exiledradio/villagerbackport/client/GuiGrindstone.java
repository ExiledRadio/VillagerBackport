package com.exiledradio.villagerbackport.client;

import com.exiledradio.villagerbackport.block.ContainerGrindstone;
import com.exiledradio.villagerbackport.block.Names;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * The grindstone screen, using 1.14's own background.
 *
 * <p>The crossed-out marker beside the result slot is part of that background and is drawn only when
 * something is in the inputs but nothing will come of it - which is how a player finds out that two
 * different items, or a plain unenchanted one, produce nothing.
 */
@SideOnly(Side.CLIENT)
public class GuiGrindstone extends GuiContainer {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation("villagerbackport", "textures/gui/grindstone.png");

    private final InventoryPlayer playerInventory;

    public GuiGrindstone(InventoryPlayer playerInventory, ContainerGrindstone container) {
        super(container);
        this.playerInventory = playerInventory;
    }

    /** Container screens have to render their own tooltips - see the note on the furnace screen. */
    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        renderHoveredToolTip(mouseX, mouseY);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        String title = Names.translateOr("container.villagerbackport.grindstone", "Repair & Disenchant");

        this.fontRenderer.drawString(title,
                this.xSize / 2 - this.fontRenderer.getStringWidth(title) / 2, 4, 4210752);
        this.fontRenderer.drawString(this.playerInventory.getDisplayName().getUnformattedText(),
                8, this.ySize - 96 + 2, 4210752);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(TEXTURE);
        drawTexturedModalRect(this.guiLeft, this.guiTop, 0, 0, this.xSize, this.ySize);

        if (hasInput() && !this.inventorySlots.getSlot(ContainerGrindstone.SLOT_RESULT).getHasStack()) {
            drawTexturedModalRect(this.guiLeft + 92, this.guiTop + 31, 176, 0, 28, 21);
        }
    }

    private boolean hasInput() {
        return this.inventorySlots.getSlot(ContainerGrindstone.SLOT_TOP).getHasStack()
                || this.inventorySlots.getSlot(ContainerGrindstone.SLOT_BOTTOM).getHasStack();
    }
}
