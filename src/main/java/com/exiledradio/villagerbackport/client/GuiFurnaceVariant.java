package com.exiledradio.villagerbackport.client;

import com.exiledradio.villagerbackport.block.ContainerFurnaceVariant;
import com.exiledradio.villagerbackport.block.FurnaceKind;
import com.exiledradio.villagerbackport.block.TileEntityFurnaceVariant;

import com.exiledradio.backportcore.recipe.book.SmeltingBook;
import com.exiledradio.backportcore.recipe.book.SmeltingBooks;
import com.exiledradio.backportcore.recipe.book.client.BookScreen;
import com.exiledradio.backportcore.recipe.book.client.GuiFurnaceBook;
import com.exiledradio.backportcore.recipe.book.client.SmeltingBookGui;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiButtonImage;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Slot;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.IOException;

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
 *
 * <h2>The recipe book</h2>
 * 1.14's blast furnace and smoker each have their own book (Backport Core's, registered by {@code CookingBooks}):
 * the green button at guiLeft + 20 and the book beside the screen, as Core's furnace screen lays them out.
 */
@SideOnly(Side.CLIENT)
public class GuiFurnaceVariant extends GuiContainer implements BookScreen {

    private static final int BOOK_BUTTON = 10;

    private static final ResourceLocation SMOKER =
            new ResourceLocation("villagerbackport", "textures/gui/smoker.png");
    private static final ResourceLocation BLAST_FURNACE =
            new ResourceLocation("villagerbackport", "textures/gui/blast_furnace.png");

    private final InventoryPlayer playerInventory;
    private final TileEntityFurnaceVariant furnace;
    /** Null when Backport Core has no book for this furnace. */
    private final SmeltingBookGui book;
    private GuiButtonImage recipeButton;
    private boolean widthTooNarrow;

    public GuiFurnaceVariant(InventoryPlayer playerInventory, TileEntityFurnaceVariant furnace) {
        super(new ContainerFurnaceVariant(playerInventory, furnace));
        this.playerInventory = playerInventory;
        this.furnace = furnace;
        SmeltingBook b = SmeltingBooks.forContainer(this.inventorySlots);
        this.book = b != null ? new SmeltingBookGui(b) : null;
    }

    @Override
    public SmeltingBookGui smeltingBook() {
        return this.book;
    }

    @Override
    public void initGui() {
        super.initGui();
        if (this.book == null) {
            return;
        }
        this.widthTooNarrow = this.width < 379;
        this.book.init(this.width, this.height, this.mc, this.widthTooNarrow, this.inventorySlots);
        this.guiLeft = this.book.updateScreenPosition(this.widthTooNarrow, this.width, this.xSize);
        this.recipeButton = new GuiButtonImage(BOOK_BUTTON, this.guiLeft + 20, this.height / 2 - 49, 20, 18, 0, 0, 19,
                GuiFurnaceBook.RECIPE_BUTTON);
        this.buttonList.add(this.recipeButton);
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        if (this.book != null) {
            this.book.tick();
        }
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
        if (this.book != null && this.book.isVisible() && this.widthTooNarrow) {
            drawGuiContainerBackgroundLayer(partialTicks, mouseX, mouseY);
            this.book.render(mouseX, mouseY, partialTicks);
        } else {
            super.drawScreen(mouseX, mouseY, partialTicks);
            if (this.book != null) {
                this.book.render(mouseX, mouseY, partialTicks);
                this.book.renderGhostRecipe(this.guiLeft, this.guiTop, true, partialTicks);
            }
        }
        renderHoveredToolTip(mouseX, mouseY);
        if (this.book != null) {
            this.book.renderTooltip(this.guiLeft, this.guiTop, mouseX, mouseY);
        }
    }

    @Override
    protected boolean isPointInRegion(int rectX, int rectY, int rectWidth, int rectHeight, int pointX, int pointY) {
        return (this.book == null || !this.widthTooNarrow || !this.book.isVisible())
                && super.isPointInRegion(rectX, rectY, rectWidth, rectHeight, pointX, pointY);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (this.book == null || !this.book.mouseClicked(mouseX, mouseY, mouseButton)) {
            if (this.book == null || !this.widthTooNarrow || !this.book.isVisible()) {
                super.mouseClicked(mouseX, mouseY, mouseButton);
            }
        }
    }

    @Override
    protected boolean hasClickedOutside(int mouseX, int mouseY, int guiLeft, int guiTop) {
        boolean outside = mouseX < guiLeft || mouseY < guiTop || mouseX >= guiLeft + this.xSize || mouseY >= guiTop + this.ySize;
        return this.book == null ? outside
                : this.book.hasClickedOutside(mouseX, mouseY, this.guiLeft, this.guiTop, this.xSize, this.ySize) && outside;
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (this.book != null && button.id == BOOK_BUTTON && button == this.recipeButton) {
            this.book.initVisuals(this.widthTooNarrow);
            this.book.toggleVisibility();
            this.guiLeft = this.book.updateScreenPosition(this.widthTooNarrow, this.width, this.xSize);
            this.recipeButton.setPosition(this.guiLeft + 20, this.height / 2 - 49);
        } else {
            super.actionPerformed(button);
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (this.book == null || !this.book.keyPressed(typedChar, keyCode)) {
            super.keyTyped(typedChar, keyCode);
        }
    }

    @Override
    protected void handleMouseClick(Slot slotIn, int slotId, int mouseButton, ClickType type) {
        super.handleMouseClick(slotIn, slotId, mouseButton, type);
        if (this.book != null) {
            this.book.slotClicked(slotIn);
        }
    }

    @Override
    public void onGuiClosed() {
        if (this.book != null) {
            this.book.removed();
        }
        super.onGuiClosed();
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
