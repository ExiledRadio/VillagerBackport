package com.exiledradio.villagerbackport.client;

import com.exiledradio.villagerbackport.block.ContainerLectern;
import com.exiledradio.villagerbackport.network.NetworkHandler;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiUtilRenderComponents;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.network.play.client.CPacketCloseWindow;
import net.minecraft.util.ChatAllowedCharacters;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import org.lwjgl.input.Keyboard;

import javax.annotation.Nullable;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * Reading a book on a lectern.
 *
 * <h2>Why this is not the vanilla book screen</h2>
 * Vanilla's {@code GuiScreenBook} keeps its page number, its parsed text and its buttons private, and
 * turning a page has to reach the server here - a lectern's page is shared, and pulses redstone. So
 * reaching into it would have meant reflection into four private fields and a screen other mods
 * commonly replace. This draws the same book from the same texture instead, and owns its page.
 *
 * <p>Whose page it is matters: the number lives on the lectern, not on the reader, so two people at
 * the same lectern turn the same pages, and a comparator sees it happen. That is the difference
 * between a lectern and a book held in hand.
 */
@SideOnly(Side.CLIENT)
public class GuiLectern extends GuiScreen {

    private static final ResourceLocation BOOK = new ResourceLocation("textures/gui/book.png");

    private static final int BOOK_WIDTH = 192;
    private static final int BOOK_HEIGHT = 192;

    /** Where the text sits inside the book, and how wide a line may be. */
    private static final int TEXT_LEFT = 36;
    private static final int TEXT_TOP = 32;
    private static final int TEXT_WIDTH = 116;

    /** Vanilla's ceiling on how long a draft may get. */
    private static final int MAX_PAGES = 50;

    private static final int BUTTON_DONE = 0;
    private static final int BUTTON_NEXT = 1;
    private static final int BUTTON_PREVIOUS = 2;
    private static final int BUTTON_TAKE = 3;

    private final ContainerLectern container;

    private PageButton nextPage;
    private PageButton previousPage;

    private NBTTagList pages;
    private int pageCount;

    /** The book the pages above were read out of, so a different one is noticed. */
    private ItemStack readFrom = ItemStack.EMPTY;

    /** The page whose text is in {@link #lines}, so it is only re-parsed when it changes. */
    private int cachedPage = -1;
    private List<ITextComponent> lines = Collections.emptyList();

    /** True when what is on the stand is a draft, and this player is allowed to write in it. */
    private boolean writable;

    /** True when there are keystrokes the lectern has not been told about yet. */
    private boolean modified;

    /** Ticks the screen has been open, which is only used to blink the cursor. */
    private int ticks;

    public GuiLectern(ContainerLectern container) {
        this.container = container;
    }

    @Override
    public void initGui() {
        // What GuiContainer does for its own screens, and what makes the window id FML sends land on
        // the right container - so the button presses below reach this lectern and not the player's
        // own inventory.
        this.mc.player.openContainer = this.container;

        // So holding a key repeats it, as it does in every other text field.
        Keyboard.enableRepeatEvents(true);

        readBook();

        int left = (this.width - BOOK_WIDTH) / 2;

        this.nextPage = addButton(new PageButton(BUTTON_NEXT, left + 120, 156, true));
        this.previousPage = addButton(new PageButton(BUTTON_PREVIOUS, left + 38, 156, false));

        addButton(new GuiButton(BUTTON_DONE, this.width / 2 - 100, 196, 98, 20,
                I18n.format("gui.done")));
        addButton(new GuiButton(BUTTON_TAKE, this.width / 2 + 2, 196, 98, 20,
                Translate.of("lectern.take_book", "Take Book")));

        updateButtons();
    }

    /**
     * Tells the server the window is finished with, which vanilla only does for slot screens.
     *
     * <h2>Not {@code player.closeScreen()}</h2>
     * That is the obvious call and it is a loop: {@code EntityPlayerSP.closeScreen} ends in
     * {@code displayGuiScreen(null)}, which calls this method, which calls it again. {@code
     * GuiContainer} gets away with using it because it calls it from the escape key rather than from
     * here - by the time this runs, the screen is already going.
     *
     * <p>So the packet is sent directly instead, which is all that call would have added. The guard
     * covers the case where the server closed the window first - taking the book does that - since it
     * clears {@code openContainer} before the screen goes, and a second close would be for a window
     * that no longer exists.
     */
    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        Keyboard.enableRepeatEvents(false);

        if (this.mc.player == null || this.mc.player.openContainer != this.container) {
            return;
        }

        commit(true);

        if (this.mc.getConnection() != null) {
            this.mc.getConnection().sendPacket(new CPacketCloseWindow(this.container.windowId));
        }

        this.mc.player.openContainer = this.mc.player.inventoryContainer;
    }

    /** The world keeps going while someone reads, as it does in 1.14. */
    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    /**
     * Reads the pages out of whatever is on the lectern.
     *
     * <p>Called again whenever the book changes rather than only on opening, because the lectern's
     * contents arrive as tile entity data and may not have landed by the time the screen does - and
     * because someone else can swap the book while this one is open.
     */
    private void readBook() {
        ItemStack book = this.container.getBook();
        NBTTagCompound tag = book.getTagCompound();

        this.readFrom = book;
        this.writable = book.getItem() == Items.WRITABLE_BOOK && this.mc.player.capabilities.allowEdit;

        // Copied rather than used in place: the list belongs to the item stack held by the client's
        // copy of the lectern, and typing into that directly would change what is on the stand
        // without the server ever agreeing to it.
        this.pages = tag != null ? tag.getTagList("pages", 8).copy() : new NBTTagList();

        // A draft always has a page to write on, even if it was placed blank.
        if (this.writable && this.pages.tagCount() == 0) {
            this.pages.appendTag(new NBTTagString(""));
        }

        this.pageCount = Math.max(this.pages.tagCount(), 1);
        this.cachedPage = -1;
    }

    /**
     * Sends anything typed since the last time, if there is any.
     *
     * <p>Called before every page turn as well as on the way out, so the lectern's copy is current
     * before anything else is asked of it - the page it is being told to turn to may be one that only
     * exists because of the text in this message.
     */
    private void commit() {
        commit(false);
    }

    /**
     * @param trim drop empty pages off the end first
     *
     * <p>Only done on the way out. Trimming while the book is open would delete the page that was
     * just added to write on, before a single character had been typed into it.
     */
    private void commit(boolean trim) {
        if (!this.modified) {
            return;
        }

        if (trim) {
            while (this.pages.tagCount() > 1
                    && this.pages.getStringTagAt(this.pages.tagCount() - 1).isEmpty()) {
                this.pages.removeTag(this.pages.tagCount() - 1);
            }
        }

        this.modified = false;
        NetworkHandler.sendLecternEdit(this.container.getLecternPos(), this.pages);
    }

    private int currentPage() {
        return MathHelper.clamp(this.container.getPage(), 0, this.pageCount - 1);
    }

    private void updateButtons() {
        int page = currentPage();

        // On a draft the forward arrow stays lit at the end of the book, where it adds a page - which
        // is the only way to write past the first one.
        this.nextPage.visible = page < this.pageCount - 1
                || (this.writable && this.pageCount < MAX_PAGES);
        this.previousPage.visible = page > 0;
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (!button.enabled) {
            return;
        }

        switch (button.id) {
            case BUTTON_DONE:
                this.mc.displayGuiScreen(null);
                break;

            case BUTTON_NEXT:
                if (currentPage() >= this.pageCount - 1) {
                    addPage();
                }
                press(ContainerLectern.BUTTON_NEXT_PAGE);
                break;

            case BUTTON_PREVIOUS:
                press(ContainerLectern.BUTTON_PREVIOUS_PAGE);
                break;

            case BUTTON_TAKE:
                press(ContainerLectern.BUTTON_TAKE_BOOK);
                break;

            default:
                break;
        }
    }

    /**
     * Sends a button press over vanilla's own channel.
     *
     * <p>Applied locally first so the page turns under the cursor rather than after the round trip;
     * the server's answer arrives as a window property and replaces it either way.
     */
    private void press(int id) {
        commit();

        this.container.enchantItem(this.mc.player, id);
        this.mc.playerController.sendEnchantPacket(this.container.windowId, id);
        updateButtons();
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        this.ticks++;
    }

    /** @return the text of the open page, as typed */
    private String pageText() {
        int page = currentPage();
        return page < this.pages.tagCount() ? this.pages.getStringTagAt(page) : "";
    }

    private void setPageText(String text) {
        int page = currentPage();
        if (page >= this.pages.tagCount()) {
            return;
        }

        this.pages.set(page, new NBTTagString(text));
        this.modified = true;
        this.cachedPage = -1;
    }

    private void addPage() {
        if (!this.writable || this.pages.tagCount() >= MAX_PAGES) {
            return;
        }

        this.pages.appendTag(new NBTTagString(""));
        this.pageCount = this.pages.tagCount();
        this.modified = true;
    }

    /**
     * Adds typed text to the open page, if it still fits.
     *
     * <p>Both limits are vanilla's: a page holds what will draw inside the book and no more than 256
     * characters. Without them text runs off the bottom of the page and is invisible but still saved.
     */
    private void insert(String text) {
        String updated = pageText() + text;

        int height = this.fontRenderer.getWordWrappedHeight(
                updated + TextFormatting.BLACK + "_", TEXT_WIDTH + 2);

        if (height <= 128 && updated.length() < 256) {
            setPageText(updated);
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        super.keyTyped(typedChar, keyCode);

        if (!this.writable) {
            return;
        }

        if (isKeyComboCtrlV(keyCode)) {
            insert(getClipboardString());
            return;
        }

        switch (keyCode) {
            case Keyboard.KEY_BACK: {
                String text = pageText();
                if (!text.isEmpty()) {
                    setPageText(text.substring(0, text.length() - 1));
                }
                break;
            }

            case Keyboard.KEY_RETURN:
            case Keyboard.KEY_NUMPADENTER:
                insert("\n");
                break;

            default:
                if (ChatAllowedCharacters.isAllowedCharacter(typedChar)) {
                    insert(Character.toString(typedChar));
                }
                break;
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(BOOK);

        int left = (this.width - BOOK_WIDTH) / 2;
        drawTexturedModalRect(left, 2, 0, 0, BOOK_WIDTH, BOOK_HEIGHT);

        // Only picked up while nothing is waiting to be sent, or the lectern's older copy would
        // arrive mid-sentence and take back what was just typed.
        if (!this.modified && this.container.getBook() != this.readFrom) {
            readBook();
        }

        int page = currentPage();
        if (page != this.cachedPage) {
            this.cachedPage = page;
            this.lines = this.writable ? Collections.<ITextComponent>emptyList() : parse(page);
            updateButtons();
        }

        String indicator = I18n.format("book.pageIndicator", page + 1, this.pageCount);
        this.fontRenderer.drawString(indicator,
                left - this.fontRenderer.getStringWidth(indicator) + BOOK_WIDTH - 44, 18, 0);

        if (this.writable) {
            drawDraft(left);
        } else {
            drawWritten(left, mouseX, mouseY);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    /**
     * Draws a draft being written on: the raw text, and a cursor that blinks at the end of it.
     *
     * <p>Nothing is parsed here. A draft's pages are the characters someone typed, and showing them
     * as anything else would mean the text changed the moment the book was signed.
     */
    private void drawDraft(int left) {
        String text = pageText();

        // Roughly three blinks a second, matching every other text field in the game.
        if (this.ticks / 6 % 2 == 0) {
            text = text + "_";
        }

        this.fontRenderer.drawSplitString(text, left + TEXT_LEFT, TEXT_TOP + 2, TEXT_WIDTH, 0);
    }

    /** Draws a signed book: parsed, formatted, and with its links live. */
    private void drawWritten(int left, int mouseX, int mouseY) {
        int visible = Math.min(128 / this.fontRenderer.FONT_HEIGHT, this.lines.size());

        for (int i = 0; i < visible; i++) {
            this.fontRenderer.drawString(this.lines.get(i).getFormattedText(),
                    left + TEXT_LEFT, TEXT_TOP + i * this.fontRenderer.FONT_HEIGHT, 0);
        }

        ITextComponent hovered = componentAt(mouseX, mouseY);
        if (hovered != null) {
            handleComponentHover(hovered, mouseX, mouseY);
        }
    }

    /**
     * @return the page's text, wrapped to the width of the book
     *
     * <p>A written book's pages are JSON components, which carry colour, formatting and links. A
     * draft's are plain strings, and are shown as they are - read-only, since this is someone else's
     * lectern and not a writing desk.
     */
    private List<ITextComponent> parse(int page) {
        if (page < 0 || page >= this.pages.tagCount()) {
            return Collections.emptyList();
        }

        String raw = this.pages.getStringTagAt(page);
        ITextComponent component;

        try {
            component = ITextComponent.Serializer.jsonToComponent(raw);
            if (component == null) {
                component = new TextComponentString(raw);
            }
        } catch (Exception e) {
            // A draft book, or a written one whose text was written by something that did not read
            // the format. Either way the string itself is what to show.
            component = new TextComponentString(raw);
        }

        return GuiUtilRenderComponents.splitText(component, TEXT_WIDTH, this.fontRenderer, true, true);
    }

    /** @return the piece of text under the cursor, so links can be hovered and clicked */
    @Nullable
    private ITextComponent componentAt(int mouseX, int mouseY) {
        if (this.lines.isEmpty()) {
            return null;
        }

        int x = MathHelper.floor(mouseX - (float) ((this.width - BOOK_WIDTH) / 2) - TEXT_LEFT);
        int y = MathHelper.floor(mouseY - 2.0F - 16.0F - 16.0F);

        if (x < 0 || y < 0 || x > TEXT_WIDTH) {
            return null;
        }

        int line = y / this.fontRenderer.FONT_HEIGHT;
        if (line < 0 || line >= this.lines.size()
                || line >= 128 / this.fontRenderer.FONT_HEIGHT) {
            return null;
        }

        int width = 0;
        for (ITextComponent part : this.lines.get(line)) {
            if (!(part instanceof TextComponentString)) {
                continue;
            }

            width += this.fontRenderer.getStringWidth(((TextComponentString) part).getText());
            if (width > x) {
                return part;
            }
        }

        return null;
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (mouseButton == 0) {
            ITextComponent clicked = componentAt(mouseX, mouseY);
            if (clicked != null && handleComponentClick(clicked)) {
                return;
            }
        }

        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    /**
     * Follows a link in the text.
     *
     * <p>A page jump is the interesting one: it is the same operation as pressing the arrow, so it
     * goes to the lectern rather than being applied locally, and pulses redstone like any other page
     * turn. Everything else - web links, commands - is handled by vanilla.
     */
    @Override
    public boolean handleComponentClick(ITextComponent component) {
        ClickEvent event = component.getStyle().getClickEvent();
        if (event == null) {
            return false;
        }

        if (event.getAction() == ClickEvent.Action.CHANGE_PAGE) {
            try {
                int wanted = Integer.parseInt(event.getValue()) - 1;

                if (wanted >= 0 && wanted < this.pageCount && wanted != currentPage()) {
                    press(ContainerLectern.BUTTON_GOTO_PAGE + wanted);
                    return true;
                }
            } catch (NumberFormatException e) {
                // A malformed page link; nothing to jump to.
            }

            return false;
        }

        boolean handled = super.handleComponentClick(component);
        if (handled && event.getAction() == ClickEvent.Action.RUN_COMMAND) {
            this.mc.displayGuiScreen(null);
        }

        return handled;
    }

    /** The two arrows, which live in the book texture below the page itself. */
    @SideOnly(Side.CLIENT)
    private static class PageButton extends GuiButton {

        private static final int WIDTH = 23;
        private static final int HEIGHT = 13;

        private final boolean forward;

        PageButton(int id, int x, int y, boolean forward) {
            super(id, x, y, WIDTH, HEIGHT, "");
            this.forward = forward;
        }

        @Override
        public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
            if (!this.visible) {
                return;
            }

            boolean over = mouseX >= this.x && mouseY >= this.y
                    && mouseX < this.x + this.width && mouseY < this.y + this.height;

            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            mc.getTextureManager().bindTexture(BOOK);

            drawTexturedModalRect(this.x, this.y,
                    over ? WIDTH : 0, this.forward ? 192 : 192 + HEIGHT, WIDTH, HEIGHT);
        }
    }
}
