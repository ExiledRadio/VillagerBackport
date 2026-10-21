package com.exiledradio.villagerbackport.client;

import com.exiledradio.villagerbackport.block.ContainerSmithingTable;
import com.exiledradio.villagerbackport.block.Names;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * The smithing table screen, on 1.16's own background.
 *
 * <p>As at the grindstone, the red cross is part of the background and shows when something is in
 * the inputs but they make nothing.
 */
@SideOnly(Side.CLIENT)
public class GuiSmithingTable extends GuiContainer {

    public static final ResourceLocation TEXTURE =
            new ResourceLocation("villagerbackport", "textures/gui/smithing.png");

    private final InventoryPlayer playerInventory;

    public GuiSmithingTable(InventoryPlayer playerInventory, ContainerSmithingTable container) {
        super(container);
        this.playerInventory = playerInventory;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        renderHoveredToolTip(mouseX, mouseY);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        // 1.16 puts the title beside the hammer rather than centred.
        String title = Names.translateOr("container.villagerbackport.smithing_table", "Upgrade Gear");
        this.fontRenderer.drawString(title, 60, 18, 4210752);
        this.fontRenderer.drawString(this.playerInventory.getDisplayName().getUnformattedText(),
                8, this.ySize - 96 + 2, 4210752);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(TEXTURE);
        drawTexturedModalRect(this.guiLeft, this.guiTop, 0, 0, this.xSize, this.ySize);

        boolean input = this.inventorySlots.getSlot(ContainerSmithingTable.SLOT_BASE).getHasStack()
                || this.inventorySlots.getSlot(ContainerSmithingTable.SLOT_ADDITION).getHasStack();
        if (input && !this.inventorySlots.getSlot(ContainerSmithingTable.SLOT_RESULT).getHasStack()) {
            drawTexturedModalRect(this.guiLeft + 99, this.guiTop + 45, this.xSize, 0, 28, 21);
        }
    }
}
