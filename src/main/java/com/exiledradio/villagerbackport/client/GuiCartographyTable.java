package com.exiledradio.villagerbackport.client;

import com.exiledradio.villagerbackport.block.ContainerCartographyTable;
import com.exiledradio.villagerbackport.block.Names;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.storage.MapData;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;

/**
 * The cartography table screen.
 *
 * <p>Most of this is the preview to the right of the slots: the map is drawn on a sheet of parchment
 * whose shape changes with what is in the modifier slot - one sheet normally, two overlapping when
 * copying, and a larger sheet with the map shrunk into the middle of it when zooming out. It is the
 * clearest part of 1.14's table, since it shows what you are about to get before you pay for it.
 *
 * <p>1.14's third layout, for locking a map behind a glass pane, is absent along with the operation
 * itself - see the note on {@link ContainerCartographyTable}.
 */
@SideOnly(Side.CLIENT)
public class GuiCartographyTable extends GuiContainer {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation("villagerbackport", "textures/gui/cartography_table.png");

    /** 1.14's widest map. Paper on one of these does nothing, and the screen says so. */
    private static final int MAX_SCALE = 4;

    private final InventoryPlayer playerInventory;

    public GuiCartographyTable(InventoryPlayer playerInventory, Container container) {
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
        String title = Names.translateOr("container.villagerbackport.cartography_table",
                "Cartography Table");

        this.fontRenderer.drawString(title, 8, 4, 4210752);
        this.fontRenderer.drawString(this.playerInventory.getDisplayName().getUnformattedText(),
                8, this.ySize - 96 + 2, 4210752);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(TEXTURE);
        drawTexturedModalRect(this.guiLeft, this.guiTop, 0, 0, this.xSize, this.ySize);

        Item modifier = this.inventorySlots.getSlot(ContainerCartographyTable.SLOT_MODIFIER)
                .getStack().getItem();
        boolean copying = modifier == Items.MAP;
        boolean zooming = modifier == Items.PAPER;

        MapData map = mapData();
        boolean refused = false;

        // Paper on a map that cannot get any wider produces nothing, so the crossed-out marker beside
        // the result slot is drawn for the same reason the grindstone draws its own.
        if (zooming && map != null && map.scale >= MAX_SCALE) {
            refused = true;
            drawTexturedModalRect(this.guiLeft + 35, this.guiTop + 31, 226, 132, 28, 21);
        }

        drawPreview(map, copying, zooming && !refused);
    }

    @Nullable
    private MapData mapData() {
        ItemStack stack = this.inventorySlots.getSlot(ContainerCartographyTable.SLOT_MAP).getStack();

        return stack.getItem() == Items.FILLED_MAP
                ? Items.FILLED_MAP.getMapData(stack, this.mc.world)
                : null;
    }

    /**
     * Draws the parchment, and the map on it.
     *
     * <p>The offsets and scales are 1.14's. Zooming shrinks the map inside a full sheet, which is what
     * zooming out looks like; copying draws two sheets offset from each other, the second nudged
     * forward so it reads as being on top rather than fighting with the first.
     */
    private void drawPreview(@Nullable MapData map, boolean copying, boolean zooming) {
        int x = this.guiLeft;
        int y = this.guiTop;

        if (zooming) {
            drawTexturedModalRect(x + 67, y + 13, 176, 66, 66, 66);
            drawMap(map, x + 85, y + 31, 0.226F);
        } else if (copying) {
            drawTexturedModalRect(x + 83, y + 13, 176, 132, 50, 66);
            drawMap(map, x + 86, y + 16, 0.34F);

            this.mc.getTextureManager().bindTexture(TEXTURE);
            GlStateManager.pushMatrix();
            GlStateManager.translate(0.0F, 0.0F, 1.0F);
            drawTexturedModalRect(x + 67, y + 29, 176, 132, 50, 66);
            drawMap(map, x + 70, y + 32, 0.34F);
            GlStateManager.popMatrix();
        } else {
            drawTexturedModalRect(x + 67, y + 13, 176, 0, 66, 66);
            drawMap(map, x + 71, y + 17, 0.45F);
        }
    }

    /**
     * Draws the map itself, using the same renderer that draws one held in hand.
     *
     * <p>That renderer binds its own texture, so the background has to be bound again before anything
     * else is drawn from it - which is why the caller above rebinds between the two sheets.
     */
    private void drawMap(@Nullable MapData map, int x, int y, float scale) {
        if (map == null) {
            return;
        }

        GlStateManager.pushMatrix();
        GlStateManager.translate((float) x, (float) y, 1.0F);
        GlStateManager.scale(scale, scale, 1.0F);

        this.mc.entityRenderer.getMapItemRenderer().renderMap(map, true);

        GlStateManager.popMatrix();
        this.mc.getTextureManager().bindTexture(TEXTURE);
    }
}
