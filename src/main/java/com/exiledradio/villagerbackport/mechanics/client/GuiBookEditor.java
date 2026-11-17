package com.exiledradio.villagerbackport.mechanics.client;

import io.netty.buffer.Unpooled;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.client.CPacketCustomPayload;
import net.minecraft.util.ChatAllowedCharacters;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
import java.util.List;

/**
 * The book and quill editor as 1.14 (18w43a) rewrote it ({@code BookEditScreen}): a cursor that moves with the arrow
 * keys, Home and End (Ctrl for the page) and the mouse; Shift and dragging select; Ctrl+A, C, X and V; Ctrl+Backspace
 * and Ctrl+Delete take a word; a double click selects a word and a third the page; Page Up and Page Down turn pages;
 * and a book holds 100 pages (1.12.2: 50).
 *
 * <p>It edits and signs exactly as 1.12.2's {@code GuiScreenBook} does, with the same {@code MC|BEdit} and
 * {@code MC|BSign} messages, so a server needs nothing to accept it. A page holds what fits in the 116 x 128 text
 * area, under 256 characters.
 */
@SideOnly(Side.CLIENT)
public class GuiBookEditor extends GuiScreen {

    private static final ResourceLocation BOOK = new ResourceLocation("textures/gui/book.png");
    private static final int MAX_PAGES = 100;
    // 1.12.2's page (its book texture and its reading screen): 116 wide from y 34. 1.14 moved it to 114 from y 32.
    private static final int TEXT_WIDTH = 116;
    private static final int TEXT_TOP = 34;
    private static final int TEXT_HEIGHT = 128;
    private static final int LINE_HEIGHT = 9;
    private static final int MAX_PAGE_LENGTH = 256;
    private static final int MAX_TITLE = 16;

    private final EntityPlayer player;
    private final ItemStack book;
    private final List<String> pages = new ArrayList<String>();
    private int page;
    private int cursor;
    private int anchor;
    private boolean modified;
    private boolean signing;
    private String title = "";
    private int frameTick;
    private long lastClick;
    private int lastClickIndex = -1;

    private GuiButton signButton;
    private GuiButton doneButton;
    private GuiButton finalizeButton;
    private GuiButton cancelButton;
    private PageButton nextButton;
    private PageButton previousButton;

    public GuiBookEditor(EntityPlayer player, ItemStack book) {
        this.player = player;
        this.book = book;
        NBTTagCompound tag = book.getTagCompound();
        if (tag != null) {
            NBTTagList list = tag.getTagList("pages", 8);
            for (int i = 0; i < list.tagCount() && i < MAX_PAGES; i++) {
                this.pages.add(list.getStringTagAt(i));
            }
        }
        if (this.pages.isEmpty()) {
            this.pages.add("");
        }
    }

    // ------------------------------------------------------------------------------------------------
    // Screen
    // ------------------------------------------------------------------------------------------------

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        this.buttonList.clear();
        this.signButton = this.addButton(new GuiButton(3, this.width / 2 - 100, 196, 98, 20, I18n.format("book.signButton")));
        this.doneButton = this.addButton(new GuiButton(0, this.width / 2 + 2, 196, 98, 20, I18n.format("gui.done")));
        this.finalizeButton = this.addButton(new GuiButton(5, this.width / 2 - 100, 196, 98, 20, I18n.format("book.finalizeButton")));
        this.cancelButton = this.addButton(new GuiButton(4, this.width / 2 + 2, 196, 98, 20, I18n.format("gui.cancel")));
        int left = (this.width - 192) / 2;
        this.nextButton = this.addButton(new PageButton(1, left + 120, 156, true));
        this.previousButton = this.addButton(new PageButton(2, left + 38, 156, false));
        this.updateButtons();
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        ++this.frameTick;
    }

    private void updateButtons() {
        this.previousButton.visible = !this.signing && this.page > 0;
        this.nextButton.visible = !this.signing && (this.page < this.pages.size() - 1 || this.pages.size() < MAX_PAGES);
        this.doneButton.visible = !this.signing;
        this.signButton.visible = !this.signing;
        this.cancelButton.visible = this.signing;
        this.finalizeButton.visible = this.signing;
        this.finalizeButton.enabled = !this.title.trim().isEmpty();
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (!button.enabled) {
            return;
        }
        switch (button.id) {
            case 0:
                this.mc.displayGuiScreen(null);
                this.send(false);
                break;
            case 3:
                this.signing = true;
                break;
            case 4:
                this.signing = false;
                break;
            case 5:
                this.send(true);
                this.mc.displayGuiScreen(null);
                break;
            case 1:
                this.nextPage();
                break;
            case 2:
                this.previousPage();
                break;
            default:
                break;
        }
        this.updateButtons();
    }

    private void nextPage() {
        if (this.page < this.pages.size() - 1) {
            ++this.page;
        } else if (this.pages.size() < MAX_PAGES) {
            this.pages.add("");
            ++this.page;
            this.modified = true;
        }
        this.cursor = this.anchor = this.text().length();
        this.updateButtons();
    }

    private void previousPage() {
        if (this.page > 0) {
            --this.page;
        }
        this.cursor = this.anchor = this.text().length();
        this.updateButtons();
    }

    /** 1.12.2's {@code sendBookToServer}: trailing empty pages dropped, then {@code MC|BEdit} or {@code MC|BSign}. */
    private void send(boolean sign) {
        if (!this.modified && !sign) {
            return;
        }
        while (this.pages.size() > 1 && this.pages.get(this.pages.size() - 1).isEmpty()) {
            this.pages.remove(this.pages.size() - 1);
        }
        NBTTagList list = new NBTTagList();
        for (String p : this.pages) {
            list.appendTag(new NBTTagString(p));
        }
        this.book.setTagInfo("pages", list);
        if (sign) {
            this.book.setTagInfo("author", new NBTTagString(this.player.getName()));
            this.book.setTagInfo("title", new NBTTagString(this.title.trim()));
        }
        PacketBuffer buffer = new PacketBuffer(Unpooled.buffer());
        buffer.writeItemStack(this.book);
        if (this.mc.getConnection() != null) {
            this.mc.getConnection().sendPacket(new CPacketCustomPayload(sign ? "MC|BSign" : "MC|BEdit", buffer));
        }
    }

    // ------------------------------------------------------------------------------------------------
    // Text
    // ------------------------------------------------------------------------------------------------

    private String text() {
        return this.pages.get(this.page);
    }

    private void setText(String text) {
        this.pages.set(this.page, text);
        this.modified = true;
    }

    /** @return true if the text fits a page: under 256 characters and 14 lines (1.14's 128 / 9) */
    private boolean fits(String text) {
        return text.length() < MAX_PAGE_LENGTH && lines(text).size() * LINE_HEIGHT <= TEXT_HEIGHT;
    }

    /**
     * The page cut into lines, as [start, end) character ranges: at each newline (left out of the line), and where the
     * next character would pass 116 pixels, after the last space if the line has one.
     */
    List<int[]> lines(String text) {
        List<int[]> out = new ArrayList<int[]>();
        int start = 0;
        int lastSpace = -1;
        int width = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\n') {
                out.add(new int[] {start, i});
                start = i + 1;
                lastSpace = -1;
                width = 0;
                continue;
            }
            int w = this.fontRenderer.getCharWidth(c);
            if (width + w > TEXT_WIDTH && i > start) {
                int end = lastSpace >= start ? lastSpace + 1 : i;
                out.add(new int[] {start, end});
                start = end;
                lastSpace = -1;
                width = this.fontRenderer.getStringWidth(text.substring(start, i));
            }
            if (c == ' ') {
                lastSpace = i;
            }
            width += w;
        }
        out.add(new int[] {start, text.length()});
        return out;
    }

    private int selectionStart() {
        return Math.min(this.cursor, this.anchor);
    }

    private int selectionEnd() {
        return Math.max(this.cursor, this.anchor);
    }

    private String selected() {
        return this.text().substring(this.selectionStart(), this.selectionEnd());
    }

    /** Replaces the selection (or inserts at the cursor), as far as the page has room. */
    private void insert(String what) {
        String text = this.text();
        int from = this.selectionStart();
        int to = this.selectionEnd();
        String filtered = ChatAllowedCharacters.filterAllowedCharacters(what.replace("\r", ""));
        // Newlines are allowed in a book (filterAllowedCharacters keeps none, so put them back).
        if (what.indexOf('\n') >= 0) {
            StringBuilder b = new StringBuilder();
            for (char c : what.replace("\r", "").toCharArray()) {
                if (c == '\n' || ChatAllowedCharacters.isAllowedCharacter(c)) {
                    b.append(c);
                }
            }
            filtered = b.toString();
        }
        String result = text.substring(0, from) + filtered + text.substring(to);
        while (!filtered.isEmpty() && !this.fits(result)) {
            filtered = filtered.substring(0, filtered.length() - 1);
            result = text.substring(0, from) + filtered + text.substring(to);
        }
        if (filtered.isEmpty() && from == to) {
            return;
        }
        this.setText(result);
        this.cursor = this.anchor = from + filtered.length();
    }

    private void delete(int amount) {
        String text = this.text();
        if (this.cursor != this.anchor) {
            this.insert("");
            return;
        }
        int to = Math.max(0, Math.min(text.length(), this.cursor + amount));
        int from = Math.min(this.cursor, to);
        int end = Math.max(this.cursor, to);
        this.setText(text.substring(0, from) + text.substring(end));
        this.cursor = this.anchor = from;
    }

    /** 1.14's {@code getWordPosition}: the next word boundary in that direction. */
    private int word(int from, int dir) {
        String text = this.text();
        int i = from;
        if (dir < 0) {
            while (i > 0 && Character.isWhitespace(text.charAt(i - 1))) {
                i--;
            }
            while (i > 0 && !Character.isWhitespace(text.charAt(i - 1))) {
                i--;
            }
        } else {
            while (i < text.length() && !Character.isWhitespace(text.charAt(i))) {
                i++;
            }
            while (i < text.length() && Character.isWhitespace(text.charAt(i))) {
                i++;
            }
        }
        return i;
    }

    private void moveTo(int index, boolean select) {
        this.cursor = Math.max(0, Math.min(this.text().length(), index));
        if (!select) {
            this.anchor = this.cursor;
        }
    }

    /** @return the line holding a character index (a line's end belongs to it unless the next line starts there) */
    private int lineOf(List<int[]> lines, int index) {
        for (int i = 0; i < lines.size(); i++) {
            int[] l = lines.get(i);
            boolean last = i == lines.size() - 1;
            if (index >= l[0] && (index < l[1] || index == l[1] && (last || lines.get(i + 1)[0] != index))) {
                return i;
            }
        }
        return lines.size() - 1;
    }

    /** @return the character index nearest a pixel x on a line */
    private int indexAt(String text, int[] line, int x) {
        int best = line[0];
        for (int i = line[0]; i <= line[1]; i++) {
            int w = this.fontRenderer.getStringWidth(text.substring(line[0], i));
            if (w <= x + 1) {
                best = i;
            }
        }
        if (best == line[1] && best > line[0] && best < text.length() && text.charAt(best - 1) == ' ' && line[1] != text.length()) {
            best--; // stay on this line rather than the start of the next
        }
        return best;
    }

    private int xOf(String text, int[] line, int index) {
        return this.fontRenderer.getStringWidth(text.substring(line[0], Math.min(index, line[1])));
    }

    private void moveVertically(int lines, boolean select) {
        String text = this.text();
        List<int[]> all = this.lines(text);
        int line = this.lineOf(all, this.cursor);
        int x = this.xOf(text, all.get(line), this.cursor);
        int target = line + lines;
        if (target < 0) {
            this.moveTo(0, select);
        } else if (target >= all.size()) {
            this.moveTo(text.length(), select);
        } else {
            this.moveTo(this.indexAt(text, all.get(target), x), select);
        }
    }

    // ------------------------------------------------------------------------------------------------
    // Input
    // ------------------------------------------------------------------------------------------------

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws java.io.IOException {
        if (this.signing) {
            this.keyInTitle(typedChar, keyCode);
            return;
        }
        if (keyCode == Keyboard.KEY_ESCAPE) {
            super.keyTyped(typedChar, keyCode);
            return;
        }
        boolean shift = isShiftKeyDown();
        boolean ctrl = isCtrlKeyDown();
        String text = this.text();
        if (isKeyComboCtrlA(keyCode)) {
            this.anchor = 0;
            this.cursor = text.length();
        } else if (isKeyComboCtrlC(keyCode)) {
            setClipboardString(this.selected());
        } else if (isKeyComboCtrlX(keyCode)) {
            setClipboardString(this.selected());
            this.insert("");
        } else if (isKeyComboCtrlV(keyCode)) {
            this.insert(getClipboardString());
        } else {
            switch (keyCode) {
                case Keyboard.KEY_BACK:
                    this.delete(ctrl ? this.word(this.cursor, -1) - this.cursor : -1);
                    break;
                case Keyboard.KEY_DELETE:
                    this.delete(ctrl ? this.word(this.cursor, 1) - this.cursor : 1);
                    break;
                case Keyboard.KEY_RETURN:
                case Keyboard.KEY_NUMPADENTER:
                    this.insert("\n");
                    break;
                case Keyboard.KEY_LEFT:
                    this.moveTo(ctrl ? this.word(this.cursor, -1) : !shift && this.cursor != this.anchor ? this.selectionStart() : this.cursor - 1, shift);
                    break;
                case Keyboard.KEY_RIGHT:
                    this.moveTo(ctrl ? this.word(this.cursor, 1) : !shift && this.cursor != this.anchor ? this.selectionEnd() : this.cursor + 1, shift);
                    break;
                case Keyboard.KEY_UP:
                    this.moveVertically(-1, shift);
                    break;
                case Keyboard.KEY_DOWN:
                    this.moveVertically(1, shift);
                    break;
                case Keyboard.KEY_HOME: {
                    List<int[]> lines = this.lines(text);
                    this.moveTo(ctrl ? 0 : lines.get(this.lineOf(lines, this.cursor))[0], shift);
                    break;
                }
                case Keyboard.KEY_END: {
                    List<int[]> lines = this.lines(text);
                    this.moveTo(ctrl ? text.length() : lines.get(this.lineOf(lines, this.cursor))[1], shift);
                    break;
                }
                case Keyboard.KEY_PRIOR:
                    this.previousPage();
                    break;
                case Keyboard.KEY_NEXT:
                    this.nextPage();
                    break;
                default:
                    if (ChatAllowedCharacters.isAllowedCharacter(typedChar)) {
                        this.insert(Character.toString(typedChar));
                    }
                    break;
            }
        }
        this.updateButtons();
    }

    private void keyInTitle(char typedChar, int keyCode) {
        switch (keyCode) {
            case Keyboard.KEY_ESCAPE:
                this.mc.displayGuiScreen(null);
                return;
            case Keyboard.KEY_BACK:
                if (!this.title.isEmpty()) {
                    this.title = this.title.substring(0, this.title.length() - 1);
                }
                break;
            case Keyboard.KEY_RETURN:
            case Keyboard.KEY_NUMPADENTER:
                if (!this.title.trim().isEmpty()) {
                    this.send(true);
                    this.mc.displayGuiScreen(null);
                    return;
                }
                break;
            default:
                if (this.title.length() < MAX_TITLE && ChatAllowedCharacters.isAllowedCharacter(typedChar)) {
                    this.title += typedChar;
                    this.modified = true;
                }
                break;
        }
        this.updateButtons();
    }

    /** @return the character index under the mouse, or -1 outside the text */
    private int indexAtMouse(int mouseX, int mouseY) {
        int left = (this.width - 192) / 2 + 36;
        int top = TEXT_TOP;
        String text = this.text();
        List<int[]> lines = this.lines(text);
        int line = (mouseY - top) / LINE_HEIGHT;
        if (mouseY < top) {
            return 0;
        }
        if (line >= lines.size()) {
            return text.length();
        }
        return this.indexAt(text, lines.get(line), mouseX - left);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws java.io.IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (this.signing || mouseButton != 0) {
            return;
        }
        int left = (this.width - 192) / 2 + 36;
        if (mouseX < left - 4 || mouseX > left + TEXT_WIDTH + 4 || mouseY < TEXT_TOP - 4 || mouseY > TEXT_TOP + TEXT_HEIGHT + 2) {
            return;
        }
        int index = this.indexAtMouse(mouseX, mouseY);
        long now = net.minecraft.client.Minecraft.getSystemTime();
        if (index == this.lastClickIndex && now - this.lastClick < 250L) {
            if (this.cursor == this.anchor) {
                // Double click: the word.
                String text = this.text();
                int from = index;
                int to = index;
                while (from > 0 && !Character.isWhitespace(text.charAt(from - 1))) {
                    from--;
                }
                while (to < text.length() && !Character.isWhitespace(text.charAt(to))) {
                    to++;
                }
                this.anchor = from;
                this.cursor = to;
            } else {
                // A third click: the page.
                this.anchor = 0;
                this.cursor = this.text().length();
            }
        } else {
            this.moveTo(index, isShiftKeyDown());
        }
        this.lastClick = now;
        this.lastClickIndex = index;
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        if (!this.signing && clickedMouseButton == 0) {
            this.moveTo(this.indexAtMouse(mouseX, mouseY), true);
        }
    }

    // ------------------------------------------------------------------------------------------------
    // Drawing
    // ------------------------------------------------------------------------------------------------

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(BOOK);
        int left = (this.width - 192) / 2;
        this.drawTexturedModalRect(left, 2, 0, 0, 192, 192);
        if (this.signing) {
            this.drawTitlePage(left);
        } else {
            String indicator = I18n.format("book.pageIndicator", this.page + 1, this.pages.size());
            this.fontRenderer.drawString(indicator, left - this.fontRenderer.getStringWidth(indicator) + 192 - 44, 18, 0);
            this.drawPage(left + 36, TEXT_TOP);
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawTitlePage(int left) {
        boolean blink = this.frameTick / 6 % 2 == 0;
        String shown = this.title + (blink ? TextFormatting.BLACK + "_" : TextFormatting.GRAY + "_");
        String edit = I18n.format("book.editTitle");
        this.fontRenderer.drawString(edit, left + 36 + (TEXT_WIDTH - this.fontRenderer.getStringWidth(edit)) / 2, 34, 0);
        this.fontRenderer.drawString(shown, left + 36 + (TEXT_WIDTH - this.fontRenderer.getStringWidth(shown)) / 2, 50, 0);
        String by = I18n.format("book.byAuthor", this.player.getName());
        this.fontRenderer.drawString(TextFormatting.DARK_GRAY + by, left + 36 + (TEXT_WIDTH - this.fontRenderer.getStringWidth(by)) / 2, 60, 0);
        this.fontRenderer.drawSplitString(I18n.format("book.finalizeWarning"), left + 36, 82, TEXT_WIDTH, 0);
    }

    private void drawPage(int x, int y) {
        String text = this.text();
        List<int[]> lines = this.lines(text);
        int from = this.selectionStart();
        int to = this.selectionEnd();
        for (int i = 0; i < lines.size(); i++) {
            int[] l = lines.get(i);
            int ly = y + i * LINE_HEIGHT;
            String s = text.substring(l[0], l[1]);
            if (from != to && to > l[0] && from < l[1]) {
                int sx = x + this.xOf(text, l, Math.max(from, l[0]));
                int ex = x + this.xOf(text, l, Math.min(to, l[1]));
                drawRect(sx, ly - 1, Math.max(ex, sx + 1), ly + LINE_HEIGHT - 1, 0xFF3390FF);
            }
            this.fontRenderer.drawString(s, x, ly, 0);
        }
        // 1.14's cursor: an underscore at the very end of the page, a thin bar inside the text; blinking.
        if (this.frameTick / 6 % 2 == 0 && from == to) {
            int line = this.lineOf(lines, this.cursor);
            int[] l = lines.get(line);
            int cx = x + this.xOf(text, l, this.cursor);
            int cy = y + line * LINE_HEIGHT;
            if (this.cursor >= text.length()) {
                this.fontRenderer.drawString("_", cx, cy, 0);
            } else {
                drawRect(cx, cy - 1, cx + 1, cy + LINE_HEIGHT, 0xFF000000);
            }
        }
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /** For the client self-test: the open page's text, the page count and the selection. */
    public String pageText() {
        return this.text();
    }

    public int pageCount() {
        return this.pages.size();
    }

    public int selectionFrom() {
        return this.selectionStart();
    }

    public int selectionTo() {
        return this.selectionEnd();
    }

    /** For the client self-test: a key press, as LWJGL would deliver it. */
    public void press(char typedChar, int keyCode) throws java.io.IOException {
        this.keyTyped(typedChar, keyCode);
    }

    /** For the client self-test: a left click on a character of the open page, by its line and pixel. */
    public void clickText(int line, int x) throws java.io.IOException {
        this.mouseClicked((this.width - 192) / 2 + 36 + x, TEXT_TOP + line * LINE_HEIGHT + 1, 0);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    /** The book's page arrows, from its own texture, as 1.12.2's private {@code NextPageButton}. */
    static final class PageButton extends GuiButton {
        private final boolean forward;

        PageButton(int id, int x, int y, boolean forward) {
            super(id, x, y, 23, 13, "");
            this.forward = forward;
        }

        @Override
        public void drawButton(net.minecraft.client.Minecraft mc, int mouseX, int mouseY, float partialTicks) {
            if (!this.visible) {
                return;
            }
            boolean hover = mouseX >= this.x && mouseY >= this.y && mouseX < this.x + this.width && mouseY < this.y + this.height;
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            mc.getTextureManager().bindTexture(BOOK);
            this.drawTexturedModalRect(this.x, this.y, hover ? 23 : 0, this.forward ? 192 : 205, 23, 13);
        }
    }
}
