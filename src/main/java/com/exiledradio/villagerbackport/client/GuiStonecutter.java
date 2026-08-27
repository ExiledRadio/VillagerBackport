package com.exiledradio.villagerbackport.client;

import com.exiledradio.villagerbackport.block.ContainerStonecutter;
import com.exiledradio.villagerbackport.block.Names;

import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.List;

/**
 * The stonecutter screen: the input on the left, every shape it could become in the middle, and the
 * chosen one on the right.
 *
 * <p>Twelve shapes are visible at once in a four-by-three grid, with the rest reached by the
 * scrollbar or the mouse wheel. Modded stone tends to produce longer lists than vanilla's - a pack
 * with Chisel can put dozens of variants behind one block - so the scrolling matters more here than
 * it does in 1.14.
 */
@SideOnly(Side.CLIENT)
public class GuiStonecutter extends GuiContainer {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation("villagerbackport", "textures/gui/stonecutter.png");

    private static final int COLUMNS = 4;
    private static final int ROWS = 3;
    private static final int VISIBLE = COLUMNS * ROWS;

    /** Where the grid of shapes starts, relative to the top left of the screen. */
    private static final int GRID_X = 52;
    private static final int GRID_Y = 14;
    private static final int CELL_WIDTH = 16;
    private static final int CELL_HEIGHT = 18;

    private static final int SCROLL_X = 119;
    private static final int SCROLL_Y = 15;
    private static final int SCROLL_WIDTH = 12;
    private static final int SCROLL_HEIGHT = 15;

    /** How far the handle travels: the track's height less the handle's own. */
    private static final int SCROLL_TRAVEL = 41;

    private final ContainerStonecutter stonecutter;
    private final InventoryPlayer playerInventory;
    private final String titleKey;
    private final String titleFallback;

    private float scroll;
    private int firstVisible;
    private boolean dragging;

    public GuiStonecutter(InventoryPlayer playerInventory, ContainerStonecutter container,
                          String titleKey, String titleFallback) {
        super(container);

        this.stonecutter = container;
        this.playerInventory = playerInventory;
        this.titleKey = titleKey;
        this.titleFallback = titleFallback;

        container.setListener(new Runnable() {
            @Override
            public void run() {
                GuiStonecutter.this.onInputChanged();
            }
        });
    }

    /** Putting a different block in starts the list again from the top. */
    private void onInputChanged() {
        this.scroll = 0.0F;
        this.firstVisible = 0;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        renderHoveredToolTip(mouseX, mouseY);

        // Not something 1.14 does, but a grid of small grey blocks is hard to read without it - and
        // with modded stone in the list the names are the only way to tell two variants apart.
        int hovered = cellAt(mouseX, mouseY);
        if (hovered >= 0) {
            renderToolTip(this.stonecutter.getResults().get(hovered), mouseX, mouseY);
        }
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        String title = Names.translateOr(this.titleKey, this.titleFallback);

        this.fontRenderer.drawString(title, 8, 4, 4210752);
        this.fontRenderer.drawString(this.playerInventory.getDisplayName().getUnformattedText(),
                8, this.ySize - 94, 4210752);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(TEXTURE);

        int left = this.guiLeft;
        int top = this.guiTop;
        drawTexturedModalRect(left, top, 0, 0, this.xSize, this.ySize);

        // The handle has a greyed-out twin sitting beside it in the texture, used when there is
        // nothing to scroll.
        int handle = (int) (SCROLL_TRAVEL * this.scroll);
        drawTexturedModalRect(left + SCROLL_X, top + SCROLL_Y + handle,
                this.xSize + (canScroll() ? 0 : SCROLL_WIDTH), 0, SCROLL_WIDTH, SCROLL_HEIGHT);

        drawCells(mouseX, mouseY);
        drawShapes();
    }

    /** The button backgrounds: plain, chosen, or hovered, stacked below the screen in the texture. */
    private void drawCells(int mouseX, int mouseY) {
        int last = Math.min(this.firstVisible + VISIBLE, this.stonecutter.getResults().size());

        for (int i = this.firstVisible; i < last; i++) {
            int cell = i - this.firstVisible;
            int x = this.guiLeft + GRID_X + cell % COLUMNS * CELL_WIDTH;
            int y = this.guiTop + GRID_Y + cell / COLUMNS * CELL_HEIGHT;

            int v = this.ySize;
            if (i == this.stonecutter.getSelected()) {
                v += CELL_HEIGHT;
            } else if (mouseX >= x && mouseY >= y + 2 && mouseX < x + CELL_WIDTH
                    && mouseY < y + 2 + CELL_HEIGHT) {
                v += CELL_HEIGHT * 2;
            }

            drawTexturedModalRect(x, y + 1, 0, v, CELL_WIDTH, CELL_HEIGHT);
        }
    }

    private void drawShapes() {
        List<ItemStack> results = this.stonecutter.getResults();
        int last = Math.min(this.firstVisible + VISIBLE, results.size());

        RenderHelper.enableGUIStandardItemLighting();

        for (int i = this.firstVisible; i < last; i++) {
            int cell = i - this.firstVisible;
            int x = this.guiLeft + GRID_X + cell % COLUMNS * CELL_WIDTH;
            int y = this.guiTop + GRID_Y + cell / COLUMNS * CELL_HEIGHT + 2;

            this.itemRender.renderItemAndEffectIntoGUI(results.get(i), x, y);
        }

        RenderHelper.disableStandardItemLighting();
        this.mc.getTextureManager().bindTexture(TEXTURE);
    }

    /** @return the index of the shape under the cursor, or -1 */
    private int cellAt(int mouseX, int mouseY) {
        if (!this.stonecutter.hasChoices()) {
            return -1;
        }

        int last = Math.min(this.firstVisible + VISIBLE, this.stonecutter.getResults().size());

        for (int i = this.firstVisible; i < last; i++) {
            int cell = i - this.firstVisible;
            int x = this.guiLeft + GRID_X + cell % COLUMNS * CELL_WIDTH;
            int y = this.guiTop + GRID_Y + cell / COLUMNS * CELL_HEIGHT;

            if (mouseX >= x && mouseY >= y && mouseX < x + CELL_WIDTH && mouseY < y + CELL_HEIGHT) {
                return i;
            }
        }

        return -1;
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        this.dragging = false;

        int clicked = cellAt(mouseX, mouseY);
        if (clicked >= 0) {
            // Applied here as well as sent, so the choice highlights immediately rather than after
            // the round trip. The server's answer replaces it a moment later either way.
            this.stonecutter.enchantItem(this.mc.player, clicked);
            this.mc.playerController.sendEnchantPacket(this.stonecutter.windowId, clicked);
            this.mc.getSoundHandler().playSound(
                    PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            return;
        }

        if (this.stonecutter.hasChoices() && mouseX >= this.guiLeft + SCROLL_X && mouseX < this.guiLeft + SCROLL_X + SCROLL_WIDTH
                && mouseY >= this.guiTop + GRID_Y && mouseY < this.guiTop + GRID_Y + 54) {
            this.dragging = true;
        }

        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int mouseButton, long timeSinceClick) {
        if (this.dragging && canScroll()) {
            int top = this.guiTop + GRID_Y;

            // The handle is grabbed by its middle, so the pointer is offset by half its height.
            this.scroll = (mouseY - top - SCROLL_HEIGHT / 2.0F) / (54.0F - SCROLL_HEIGHT);
            setScroll(this.scroll);
        } else {
            super.mouseClickMove(mouseX, mouseY, mouseButton, timeSinceClick);
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();

        int wheel = Mouse.getEventDWheel();
        if (wheel != 0 && canScroll()) {
            int rows = hiddenRows();
            setScroll(this.scroll - Integer.signum(wheel) / (float) rows);
        }
    }

    private void setScroll(float value) {
        this.scroll = MathHelper.clamp(value, 0.0F, 1.0F);
        this.firstVisible = (int) (this.scroll * hiddenRows() + 0.5D) * COLUMNS;
    }

    private boolean canScroll() {
        return this.stonecutter.hasChoices() && this.stonecutter.getResults().size() > VISIBLE;
    }

    /** @return how many rows sit below the three that fit */
    private int hiddenRows() {
        int rows = (this.stonecutter.getResults().size() + COLUMNS - 1) / COLUMNS - ROWS;
        return Math.max(rows, 1);
    }
}
