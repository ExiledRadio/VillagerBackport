package com.exiledradio.villagerbackport.client;

import com.exiledradio.villagerbackport.block.BannerPatterns;
import com.exiledradio.villagerbackport.block.ContainerLoom;
import com.exiledradio.villagerbackport.block.ModSounds;
import com.exiledradio.villagerbackport.block.Names;

import com.google.common.collect.Lists;

import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.BannerTextures;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.BannerPattern;
import net.minecraft.tileentity.TileEntityBanner;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.List;

/**
 * The loom screen: a banner and a dye on the left, every design in the middle, the result on the
 * right.
 *
 * <h2>Where the little banners come from</h2>
 * Each design is shown as a miniature banner rather than an icon, and there is no icon to show -
 * banner patterns exist only as textures composited at runtime. So each one is built the way a real
 * banner is: a grey base with the design in white, handed to vanilla's banner texture cache, and
 * drawn scaled down to five by ten pixels. That is 1.14's approach and its two colours.
 *
 * <p>Thirty-four of those would be a visible stall if they were all built at once, so one is built
 * per tick and drawn once it exists - which is why the grid fills in over the first second or so.
 * 1.14 does the same, for the same reason.
 *
 * <h2>The three states of the middle panel</h2>
 * The list of designs appears only when there is a banner and a dye and no pattern item; a pattern
 * item replaces the whole list with the single design it carries; and a banner already holding six
 * patterns shows neither, only a marker over the output slot. All three are 1.14's rules.
 */
@SideOnly(Side.CLIENT)
public class GuiLoom extends GuiContainer {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation("villagerbackport", "textures/gui/loom.png");

    /** The two colours a preview is drawn in: a grey banner with the design picked out in white. */
    private static final EnumDyeColor PREVIEW_BASE = EnumDyeColor.GRAY;
    private static final EnumDyeColor PREVIEW_PATTERN = EnumDyeColor.WHITE;

    private static final int COLUMNS = 4;
    private static final int ROWS = 4;
    private static final int VISIBLE = COLUMNS * ROWS;
    private static final int CELL = 14;

    private static final int GRID_X = 60;
    private static final int GRID_Y = 13;

    private static final int SCROLL_X = 119;
    private static final int SCROLL_WIDTH = 12;
    private static final int SCROLL_HEIGHT = 15;

    /** The full height of the scrollbar's track, and how far the handle travels inside it. */
    private static final int SCROLL_TRACK = 56;
    private static final int SCROLL_TRAVEL = SCROLL_TRACK - SCROLL_HEIGHT;

    /** Where the handle's two states sit in the texture, live and greyed out. */
    private static final int SCROLL_TEXTURE_X = 232;

    /** A banner texture is a 64 by 64 sheet with the cloth itself at (1,1), twenty by forty. */
    private static final int BANNER_U = 1;
    private static final int BANNER_V = 1;
    private static final int BANNER_WIDTH = 20;
    private static final int BANNER_HEIGHT = 40;
    private static final float BANNER_SHEET = 64.0F;

    /** How many rows the whole list of designs comes to. */
    private static final int TOTAL_ROWS = (BannerPatterns.FREE_COUNT + COLUMNS - 1) / COLUMNS;

    /** Rows that do not fit, which is how far the scrollbar has to move. */
    private static final int HIDDEN_ROWS = Math.max(TOTAL_ROWS - ROWS, 1);

    private final ContainerLoom loom;
    private final InventoryPlayer playerInventory;

    private final ResourceLocation[] previews = new ResourceLocation[BannerPatterns.count()];
    private ResourceLocation resultTexture;

    /** The next preview to build, one per tick. Starts past {@code BASE}, which is never shown. */
    private int nextPreview = 1;

    private ItemStack lastBanner = ItemStack.EMPTY;
    private ItemStack lastDye = ItemStack.EMPTY;
    private ItemStack lastPattern = ItemStack.EMPTY;

    private boolean showList;
    private boolean showSingle;
    private boolean bannerFull;

    private float scroll;
    private int firstVisible = 1;
    private boolean dragging;

    public GuiLoom(InventoryPlayer playerInventory, ContainerLoom container) {
        super(container);

        this.loom = container;
        this.playerInventory = playerInventory;

        container.setListener(new Runnable() {
            @Override
            public void run() {
                GuiLoom.this.onContentsChanged();
            }
        });

        onContentsChanged();
    }

    /** Builds one design preview per tick, so opening the screen does not stall building all of them. */
    @Override
    public void updateScreen() {
        super.updateScreen();

        if (this.nextPreview >= this.previews.length) {
            return;
        }

        BannerPattern pattern = BannerPatterns.byIndex(this.nextPreview);
        String id = "b" + PREVIEW_BASE.getDyeDamage()
                + pattern.getHashname() + PREVIEW_PATTERN.getDyeDamage();

        this.previews[this.nextPreview] = BannerTextures.BANNER_DESIGNS.getResourceLocation(id,
                Lists.newArrayList(BannerPattern.BASE, pattern),
                Lists.newArrayList(PREVIEW_BASE, PREVIEW_PATTERN));

        this.nextPreview++;
    }

    /**
     * Rebuilds the result banner and works out which of the three middle-panel states applies.
     *
     * <p>Called whenever any of the four slots changes, which on this side means whenever the server
     * sends their contents - the container's inventories report every write.
     */
    private void onContentsChanged() {
        ItemStack result = this.loom.getResultSlot().getStack();

        if (result.isEmpty()) {
            this.resultTexture = null;
        } else {
            // A throwaway tile entity is the only way to reach the compositing: it is what turns the
            // banner's pattern list into the cache key and the layers vanilla draws from.
            TileEntityBanner banner = new TileEntityBanner();
            banner.setItemValues(result, false);

            this.resultTexture = BannerTextures.BANNER_DESIGNS.getResourceLocation(
                    banner.getPatternResourceLocation(), banner.getPatternList(), banner.getColorList());
        }

        ItemStack bannerStack = this.loom.getBannerSlot().getStack();
        ItemStack dyeStack = this.loom.getDyeSlot().getStack();
        ItemStack patternStack = this.loom.getPatternSlot().getStack();

        this.bannerFull = !bannerStack.isEmpty()
                && TileEntityBanner.getPatterns(bannerStack) >= BannerPatterns.MAX_PATTERNS;

        if (this.bannerFull) {
            this.resultTexture = null;
        }

        // Only recomputed when the ingredients themselves change, so that spending one banner out of
        // a stack does not make the panel flicker between states.
        if (!ItemStack.areItemStacksEqual(bannerStack, this.lastBanner)
                || !ItemStack.areItemStacksEqual(dyeStack, this.lastDye)
                || !ItemStack.areItemStacksEqual(patternStack, this.lastPattern)) {

            this.showList = !bannerStack.isEmpty() && !dyeStack.isEmpty()
                    && patternStack.isEmpty() && !this.bannerFull;
            this.showSingle = !this.bannerFull && !patternStack.isEmpty()
                    && !bannerStack.isEmpty() && !dyeStack.isEmpty();
        }

        this.lastBanner = bannerStack.copy();
        this.lastDye = dyeStack.copy();
        this.lastPattern = patternStack.copy();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        renderHoveredToolTip(mouseX, mouseY);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        String title = Names.translateOr("container.villagerbackport.loom", "Loom");

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

        drawSlotHint(this.loom.getBannerSlot(), 0);
        drawSlotHint(this.loom.getDyeSlot(), 16);
        drawSlotHint(this.loom.getPatternSlot(), 32);

        // The handle has a greyed-out twin beside it in the texture, used when there is no list.
        int handle = (int) (SCROLL_TRAVEL * this.scroll);
        drawTexturedModalRect(left + SCROLL_X, top + GRID_Y + handle,
                SCROLL_TEXTURE_X + (this.showList ? 0 : SCROLL_WIDTH), 0,
                SCROLL_WIDTH, SCROLL_HEIGHT);

        drawResult(left, top);
        drawDesigns(left, top, mouseX, mouseY);

        this.mc.getTextureManager().bindTexture(TEXTURE);
    }

    /** The faint outline in an empty ingredient slot showing what belongs there. */
    private void drawSlotHint(Slot slot, int offset) {
        if (!slot.getHasStack()) {
            drawTexturedModalRect(this.guiLeft + slot.xPos, this.guiTop + slot.yPos,
                    this.xSize + offset, 0, 16, 16);
        }
    }

    /** The finished banner, full size beside the output slot - or a marker if it can take no more. */
    private void drawResult(int left, int top) {
        if (this.bannerFull) {
            Slot result = this.loom.getResultSlot();
            drawTexturedModalRect(left + result.xPos - 2, top + result.yPos - 2,
                    this.xSize, 17, 17, 16);
            return;
        }

        if (this.resultTexture == null) {
            return;
        }

        this.mc.getTextureManager().bindTexture(this.resultTexture);
        drawScaledCustomSizeModalRect(left + 141, top + 8, BANNER_U, BANNER_V,
                BANNER_WIDTH, BANNER_HEIGHT, BANNER_WIDTH, BANNER_HEIGHT,
                BANNER_SHEET, BANNER_SHEET);
    }

    private void drawDesigns(int left, int top, int mouseX, int mouseY) {
        if (this.showList) {
            int last = this.firstVisible + VISIBLE;

            for (int i = this.firstVisible; i < last && i <= BannerPatterns.FREE_COUNT; i++) {
                int cell = i - this.firstVisible;
                int x = left + GRID_X + cell % COLUMNS * CELL;
                int y = top + GRID_Y + cell / COLUMNS * CELL;

                // Rebound each time because the previous iteration left a banner texture bound.
                this.mc.getTextureManager().bindTexture(TEXTURE);

                int v = this.ySize;
                if (i == this.loom.getSelected()) {
                    v += CELL;
                } else if (mouseX >= x && mouseY >= y && mouseX < x + CELL && mouseY < y + CELL) {
                    v += CELL * 2;
                }

                drawTexturedModalRect(x, y, 0, v, CELL, CELL);
                drawPreview(i, x + 4, y + 2);
            }
        } else if (this.showSingle) {
            int x = left + GRID_X;
            int y = top + GRID_Y;

            this.mc.getTextureManager().bindTexture(TEXTURE);
            drawTexturedModalRect(x, y, 0, this.ySize, CELL, CELL);
            drawPreview(this.loom.getSelected(), x + 4, y + 2);
        }
    }

    /** One design, drawn as a banner shrunk to a quarter size. Nothing yet if it is still being built. */
    private void drawPreview(int index, int x, int y) {
        ResourceLocation preview = index >= 0 && index < this.previews.length
                ? this.previews[index] : null;

        if (preview == null) {
            return;
        }

        this.mc.getTextureManager().bindTexture(preview);
        drawScaledCustomSizeModalRect(x, y, BANNER_U, BANNER_V, BANNER_WIDTH, BANNER_HEIGHT,
                5, 10, BANNER_SHEET, BANNER_SHEET);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        this.dragging = false;

        if (this.showList) {
            int last = this.firstVisible + VISIBLE;

            for (int i = this.firstVisible; i < last; i++) {
                int cell = i - this.firstVisible;
                int x = this.guiLeft + GRID_X + cell % COLUMNS * CELL;
                int y = this.guiTop + GRID_Y + cell / COLUMNS * CELL;

                if (mouseX < x || mouseY < y || mouseX >= x + CELL || mouseY >= y + CELL) {
                    continue;
                }

                // Applied here as well as sent, so the choice highlights and the preview updates
                // immediately rather than after the round trip. The server's answer replaces both a
                // moment later either way.
                if (this.loom.enchantItem(this.mc.player, i)) {
                    this.mc.getSoundHandler().playSound(
                            PositionedSoundRecord.getMasterRecord(ModSounds.loomSelectPattern, 1.0F));
                    this.mc.playerController.sendEnchantPacket(this.loom.windowId, i);
                    return;
                }
            }

            int trackX = this.guiLeft + SCROLL_X;
            int trackY = this.guiTop + 9;

            if (mouseX >= trackX && mouseX < trackX + SCROLL_WIDTH
                    && mouseY >= trackY && mouseY < trackY + SCROLL_TRACK) {
                this.dragging = true;
            }
        }

        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int mouseButton, long timeSinceClick) {
        if (this.dragging && this.showList) {
            // The handle is grabbed by its middle, so the pointer is offset by half its height.
            int top = this.guiTop + GRID_Y;
            setScroll((mouseY - top - SCROLL_HEIGHT / 2.0F) / SCROLL_TRAVEL);
        } else {
            super.mouseClickMove(mouseX, mouseY, mouseButton, timeSinceClick);
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();

        int wheel = Mouse.getEventDWheel();
        if (wheel != 0 && this.showList) {
            setScroll(this.scroll - Integer.signum(wheel) / (float) HIDDEN_ROWS);
        }
    }

    /** Moves the list, which always lands on a whole row - the designs never sit half-shown. */
    private void setScroll(float value) {
        this.scroll = MathHelper.clamp(value, 0.0F, 1.0F);
        this.firstVisible = 1 + (int) (this.scroll * HIDDEN_ROWS + 0.5D) * COLUMNS;
    }
}
