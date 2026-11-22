package com.exiledradio.villagerbackport.mechanics.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

import java.io.IOException;

/**
 * The book's signing screen as 1.21.6 split it out of the editor ({@code BookSignScreen}, as in 26.3): the title in a
 * borderless box centered on the page (15 characters), "by" the player, the warning, and Sign and Close / Cancel. Cancel
 * goes back to the editor and keeps the title; Escape closes both without saving; Enter signs once the box holds text.
 * Signing is the editor's {@code MC|BSign}.
 */
@SideOnly(Side.CLIENT)
public class GuiBookSign extends GuiScreen {

    private static final ResourceLocation BOOK = new ResourceLocation("textures/gui/book.png");
    private static final int TEXT_WIDTH = 114;
    private static final int MAX_TITLE = 15;

    private final GuiBookEditor editor;
    private final String owner;
    private String titleValue = "";
    private GuiTextField titleBox;
    private GuiButton finalizeButton;
    private long focusedTime;
    private long lastClick;
    private int lastClickIndex = -1;

    GuiBookSign(GuiBookEditor editor, String owner) {
        this.editor = editor;
        this.owner = owner;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        this.buttonList.clear();
        this.finalizeButton = this.addButton(new GuiButton(0, this.width / 2 - 100, 196, 98, 20, I18n.format("book.finalizeButton")));
        this.addButton(new GuiButton(1, this.width / 2 + 2, 196, 98, 20, I18n.format("gui.cancel")));
        this.titleBox = new GuiTextField(2, this.fontRenderer, (this.width - TEXT_WIDTH) / 2 - 3, 50, TEXT_WIDTH, 20);
        this.titleBox.setMaxStringLength(MAX_TITLE);
        this.titleBox.setEnableBackgroundDrawing(false);
        this.titleBox.setFocused(true);
        this.titleBox.setText(this.titleValue);
        this.focusedTime = Minecraft.getSystemTime();
        this.updateButtons();
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    private void updateButtons() {
        this.finalizeButton.enabled = !this.titleBox.getText().trim().isEmpty();
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (!button.enabled) {
            return;
        }
        if (button.id == 0) {
            this.editor.sign(this.titleBox.getText());
            this.mc.displayGuiScreen(null);
        } else if (button.id == 1) {
            this.titleValue = this.titleBox.getText();
            this.mc.displayGuiScreen(this.editor);
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if ((keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) && !this.titleBox.getText().isEmpty()) {
            this.editor.sign(this.titleBox.getText());
            this.mc.displayGuiScreen(null);
            return;
        }
        if (keyCode == Keyboard.KEY_ESCAPE) {
            super.keyTyped(typedChar, keyCode);
            return;
        }
        if (this.titleBox.textboxKeyTyped(typedChar, keyCode)) {
            this.focusedTime = Minecraft.getSystemTime();
        }
        this.updateButtons();
    }

    // ------------------------------------------------------------------------------------------------
    // Mouse: 26.3's EditBox, on the centered text
    // ------------------------------------------------------------------------------------------------

    private int textX() {
        return this.titleBox.x + (TEXT_WIDTH - this.fontRenderer.getStringWidth(this.titleBox.getText())) / 2;
    }

    private boolean overBox(int mouseX, int mouseY) {
        return mouseX >= this.titleBox.x && mouseX < this.titleBox.x + TEXT_WIDTH && mouseY >= this.titleBox.y && mouseY < this.titleBox.y + 20;
    }

    private int indexAt(int mouseX) {
        return this.fontRenderer.trimStringToWidth(this.titleBox.getText(), Math.max(0, mouseX - this.textX())).length();
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (mouseButton != 0 || !this.overBox(mouseX, mouseY)) {
            return;
        }
        int index = this.indexAt(mouseX);
        long now = Minecraft.getSystemTime();
        if (index == this.lastClickIndex && now - this.lastClick < 250L) {
            // Double click: the word.
            String text = this.titleBox.getText();
            int from = index;
            int to = index;
            while (from > 0 && text.charAt(from - 1) != ' ') {
                from--;
            }
            while (to < text.length() && text.charAt(to) != ' ') {
                to++;
            }
            this.titleBox.setCursorPosition(from);
            this.titleBox.setSelectionPos(to);
        } else if (isShiftKeyDown()) {
            this.titleBox.setSelectionPos(index);
        } else {
            this.titleBox.setCursorPosition(index);
        }
        this.lastClick = now;
        this.lastClickIndex = index;
        this.focusedTime = now;
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        if (clickedMouseButton == 0 && this.overBox(mouseX, mouseY)) {
            this.titleBox.setSelectionPos(this.indexAt(mouseX));
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
        String edit = I18n.format("book.editTitle");
        this.fontRenderer.drawString(edit, left + 36 + (TEXT_WIDTH - this.fontRenderer.getStringWidth(edit)) / 2, 34, 0);
        this.drawTitle();
        String by = TextFormatting.DARK_GRAY + I18n.format("book.byAuthor", this.owner);
        this.fontRenderer.drawString(by, left + 36 + (TEXT_WIDTH - this.fontRenderer.getStringWidth(by)) / 2, 60, 0);
        this.fontRenderer.drawSplitString(I18n.format("book.finalizeWarning"), left + 36, 82, TEXT_WIDTH, 0);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    /**
     * 26.3's EditBox, borderless and centered: black text at the box's top, no shadow; a 1-pixel bar inside the text or
     * an underscore after it while there is room, blinking every 300 ms; the selection inverted blue.
     */
    private void drawTitle() {
        String text = this.titleBox.getText();
        int x = this.textX();
        int y = this.titleBox.y;
        int cursor = this.titleBox.getSelectionEnd();
        int anchor = this.titleBox.getCursorPosition();
        this.fontRenderer.drawString(text, x, y, 0);
        boolean append = cursor >= text.length() && text.length() < MAX_TITLE;
        int cx = x + this.fontRenderer.getStringWidth(text.substring(0, cursor)) + (append && !text.isEmpty() ? 1 : 0);
        if (anchor != cursor) {
            int ax = x + this.fontRenderer.getStringWidth(text.substring(0, anchor));
            int bx = x + this.fontRenderer.getStringWidth(text.substring(0, cursor));
            invert(Math.min(ax, bx), y - 1, Math.max(ax, bx), y + 1 + 9);
        }
        if ((Minecraft.getSystemTime() - this.focusedTime) / 300L % 2L == 0L) {
            if (append) {
                this.fontRenderer.drawString("_", cx, y, 0);
            } else {
                drawRect(cx, y - 1, cx + 1, y + 9, 0xFF000000);
            }
        }
    }

    /** 1.12.2's GuiTextField selection: blue, OR_REVERSE (26.3's inverted text highlight). */
    private static void invert(int x0, int y0, int x1, int y1) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        GlStateManager.color(0.0F, 0.0F, 255.0F, 255.0F);
        GlStateManager.disableTexture2D();
        GlStateManager.enableColorLogic();
        GlStateManager.colorLogicOp(GlStateManager.LogicOp.OR_REVERSE);
        buffer.begin(7, DefaultVertexFormats.POSITION);
        buffer.pos(x0, y1, 0.0D).endVertex();
        buffer.pos(x1, y1, 0.0D).endVertex();
        buffer.pos(x1, y0, 0.0D).endVertex();
        buffer.pos(x0, y0, 0.0D).endVertex();
        tessellator.draw();
        GlStateManager.disableColorLogic();
        GlStateManager.enableTexture2D();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /** For the client self-test: the title typed so far and whether Sign and Close can be pressed. */
    public String titleText() {
        return this.titleBox.getText();
    }

    public boolean canFinalize() {
        return this.finalizeButton.enabled;
    }

    /** For the client self-test: a key press, as LWJGL would deliver it, and the two buttons. */
    public void press(char typedChar, int keyCode) throws IOException {
        this.keyTyped(typedChar, keyCode);
    }

    public void pressFinalize() {
        this.actionPerformed(this.finalizeButton);
    }

    public void pressCancel() {
        this.actionPerformed(this.buttonList.get(1));
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
