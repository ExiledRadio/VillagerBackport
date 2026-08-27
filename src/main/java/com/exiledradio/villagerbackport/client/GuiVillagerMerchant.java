package com.exiledradio.villagerbackport.client;

import com.exiledradio.villagerbackport.ModConfig;
import com.exiledradio.villagerbackport.trade.VillagerLevel;

import io.netty.buffer.Unpooled;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.IMerchant;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerMerchant;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.client.CPacketCustomPayload;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.village.MerchantRecipe;
import net.minecraft.village.MerchantRecipeList;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.Locale;

/**
 * A rebuild of 1.14's merchant screen for 1.12.2.
 *
 * <h2>How this replaces the vanilla screen</h2>
 * It does not replace the container. {@link MerchantOverlay} swaps this screen in for
 * {@code GuiMerchant} as it opens, handing over the <em>same</em> {@link ContainerMerchant} instance
 * - already bound to the server's window id - and the same client-side {@code NpcMerchant} holding
 * the trade list. Vanilla's container class is untouched, so FermiumMixins' patch on it and any
 * other mod hooking the merchant container keep working exactly as before. Trade selection goes out
 * on vanilla's own {@code MC|TrSel} channel, so the server needs no changes at all.
 *
 * <p>What does change is where the slots are: 1.14 moves the player inventory 100px right to make
 * room for the trade list, which {@link SlotLayout} applies to the client's copy of the container.
 *
 * <h2>Layout</h2>
 * Every coordinate here is taken from 1.14.4's {@code MerchantScreen}. The window is 276x166 as in
 * both versions, drawn from 1.14's own 512x256 texture:
 * <ul>
 *   <li>seven trade rows down the left, 89x20 each, starting at (5, 18) and stepping 20px</li>
 *   <li>a scrollbar at x=94 when there are more than seven trades</li>
 *   <li>the experience bar at (136, 16), 102x5</li>
 *   <li>trade slots at (136,37), (162,37) and (220,37)</li>
 * </ul>
 *
 * <h2>Why Mouse Tweaks is told to keep out</h2>
 * Mouse Tweaks drives slots by calling {@code PlayerControllerMP.windowClick} itself rather than
 * going through {@code GuiContainer.handleMouseClick}, so a screen cannot see, order or refuse what
 * it does. On this screen that is fatal: its right-button tweak quick-moves whatever slot the cursor
 * passes over, and on a merchant screen the slot next to the result is the trade's payment. Holding
 * shift and the right button empties the payment slot again and again instead of trading, and the
 * refill dutifully reloads it - a loop that looks exactly like trades being refused.
 *
 * <p>It would also fight the trade list, which uses the wheel for scrolling.
 *
 * <p>1.14's merchant screen has no such helper, so ignoring it here is both the fix and the more
 * faithful behaviour. Every other screen in the game is untouched.
 */
@yalter.mousetweaks.api.MouseTweaksIgnore
@SideOnly(Side.CLIENT)
public class GuiVillagerMerchant extends GuiContainer {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation("villagerbackport", "textures/gui/merchant.png");

    /** The texture is wider than the 256 square GUIs usually use, so sizes are passed explicitly. */
    private static final float TEX_W = 512.0F;
    private static final float TEX_H = 256.0F;

    /** Trade rows visible at once. More than this and the list scrolls. */
    private static final int VISIBLE_ROWS = 7;

    private static final int ROW_HEIGHT = 20;
    private static final int ROW_X = 5;
    private static final int ROW_Y = 18;
    private static final int ROW_WIDTH = 89;

    /** Container slot indices, fixed by {@code ContainerMerchant}'s construction order. */
    private static final int SLOT_INPUT_A = 0;
    private static final int SLOT_INPUT_B = 1;
    private static final int SLOT_INVENTORY_END = 39;


    private final IMerchant merchant;

    /** Index into the full trade list, not into the visible rows. */
    private int selectedRecipe;

    /** How many rows the list is scrolled down by. */
    private int scrollOffset;

    private boolean scrolling;


    public GuiVillagerMerchant(Container container, IMerchant merchant, InventoryPlayer playerInventory) {
        super(container);
        this.merchant = merchant;
        this.xSize = 276;
        this.ySize = 166;
    }

    @Override
    public void initGui() {
        super.initGui();

        // One invisible-until-needed button per visible row. Using real buttons rather than
        // hand-rolled hit testing keeps hover highlighting and click handling consistent with the
        // rest of the interface, which is what 1.14 does too.
        this.buttonList.clear();
        for (int row = 0; row < VISIBLE_ROWS; row++) {
            this.buttonList.add(new TradeRowButton(
                    row, this.guiLeft + ROW_X, this.guiTop + ROW_Y + row * ROW_HEIGHT));
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (!(button instanceof TradeRowButton)) {
            return;
        }

        select(((TradeRowButton) button).row + this.scrollOffset);
    }

    /**
     * Tells the server which trade is active.
     *
     * <p>Identical to what vanilla's screen sends, so the server's existing {@code MC|TrSel} handler
     * accepts it without any addition on that side. The local container is updated too so the result
     * slot reacts immediately rather than after the round trip.
     */
    private void select(int index) {
        MerchantRecipeList recipes = recipes();
        if (recipes == null || index < 0 || index >= recipes.size()) {
            return;
        }

        this.selectedRecipe = index;
        ((ContainerMerchant) this.inventorySlots).setCurrentRecipeIndex(index);

        PacketBuffer buffer = new PacketBuffer(Unpooled.buffer());
        buffer.writeInt(index);
        this.mc.getConnection().sendPacket(new CPacketCustomPayload("MC|TrSel", buffer));

        requestRefill(true);
    }

    /**
     * Moves the trade's cost items out of the player's inventory and into the input slots.
     *
     * <h2>Why this is done with click packets</h2>
     * 1.14 fills the slots server-side: selecting a trade calls into {@code MerchantContainer}, which
     * moves matching stacks out of the player inventory for you. 1.12.2's {@code ContainerMerchant}
     * has no equivalent - selecting a trade only sets an index - so picking a row left the slots
     * empty and the trade unfulfilled.
     *
     * <p>Rather than add a server-side message for this, the moves are performed as ordinary window
     * clicks through {@link net.minecraft.client.multiplayer.PlayerControllerMP#windowClick}. That
     * is the exact path a player clicking the slots by hand would take: it applies the change
     * locally and sends the same packet the server already validates. Nothing about the server's
     * view of the container changes, and an item the player does not actually have cannot be
     * conjured, because the server checks every one of these clicks as normal.
     *
     * <p>Slot indices come from {@code ContainerMerchant}: 0 and 1 are the trade inputs, 2 is the
     * result, and 3 to 38 are the player's inventory and hotbar.
     */
    /**
     * Asks the server to load the selected trade.
     *
     * @param replace true to swap out whatever the slots hold, which is what picking a trade from
     *                the list means. Topping up from the keyboard passes false, so a trade the
     *                player loaded by hand is never taken back off them.
     */
    private void requestRefill(boolean replace) {
        // Doing this while the player is dragging a stack would drop it somewhere unexpected.
        if (!this.mc.player.inventory.getItemStack().isEmpty()) {
            return;
        }

        com.exiledradio.villagerbackport.network.NetworkHandler.requestRefill(this.selectedRecipe, replace);
    }

    /**
     * Space refills the selected trade, the way it does in 1.14.
     *
     * <p>After a few exchanges the input slots run low; space tops them back up without having to
     * find and click the trade in the list again.
     */
    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == refillKey()) {
            MerchantRecipeList recipes = recipes();
            if (recipes != null && this.selectedRecipe >= 0 && this.selectedRecipe < recipes.size()) {
                // The same click the trade rows make when pressed. Refilling by keyboard otherwise
                // happens in silence, leaving no confirmation that anything was picked up.
                this.mc.getSoundHandler().playSound(
                        PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0F));

                requestRefill(false);
                return;
            }
        }

        super.keyTyped(typedChar, keyCode);
    }

    MerchantRecipeList recipes() {
        return this.merchant.getRecipes(this.mc.player);
    }

    // ------------------------------------------------------------------ input

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();

        int wheel = Mouse.getEventDWheel();
        if (wheel == 0) {
            return;
        }

        int maxScroll = maxScroll();
        if (maxScroll <= 0) {
            return;
        }

        this.scrollOffset = MathHelper.clamp(this.scrollOffset - Integer.signum(wheel), 0, maxScroll);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);

        if (maxScroll() > 0 && isOverScrollbar(mouseX, mouseY)) {
            this.scrolling = true;
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        super.mouseReleased(mouseX, mouseY, state);
        this.scrolling = false;
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        int maxScroll = maxScroll();

        if (this.scrolling && maxScroll > 0) {
            int trackTop = this.guiTop + ROW_Y;
            int trackHeight = VISIBLE_ROWS * ROW_HEIGHT - 27;
            float fraction = (float) (mouseY - trackTop - 13) / (float) trackHeight;
            this.scrollOffset = MathHelper.clamp(Math.round(fraction * maxScroll), 0, maxScroll);
        } else {
            super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
        }
    }

    private boolean isOverScrollbar(int mouseX, int mouseY) {
        int x = this.guiLeft + 94;
        int y = this.guiTop + ROW_Y;
        return mouseX >= x && mouseX < x + 6 && mouseY >= y && mouseY < y + VISIBLE_ROWS * ROW_HEIGHT;
    }

    private int maxScroll() {
        MerchantRecipeList recipes = recipes();
        return recipes == null ? 0 : Math.max(0, recipes.size() - VISIBLE_ROWS);
    }

    // ----------------------------------------------------------------- render

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(TEXTURE);
        drawModalRectWithCustomSizedTexture(
                this.guiLeft, this.guiTop, 0.0F, 0.0F, this.xSize, this.ySize, TEX_W, TEX_H);

        drawExperienceBar();
        drawScrollbar();
        drawOutOfStockMarker();
    }

    /**
     * 1.14's experience bar, at (136, 16) and 102 pixels wide.
     *
     * <p>Only drawn below max level - at Master there is no next threshold to fill toward, and 1.14
     * hides the bar entirely rather than showing a permanently full one.
     */
    private void drawExperienceBar() {
        int xp = MerchantOverlay.currentXp();
        if (xp < 0) {
            return;
        }

        // The level in effect, not the one the experience would imply. While the screen is open a
        // villager can hold enough experience for the next rank without having taken it yet, and the
        // bar is supposed to sit full through that rather than resetting early.
        int level = MerchantOverlay.currentLevel();
        if (!VillagerLevel.canLevelUp(level)) {
            return;
        }

        int x = this.guiLeft + 136;
        int y = this.guiTop + 16;

        this.mc.getTextureManager().bindTexture(TEXTURE);
        drawModalRectWithCustomSizedTexture(x, y, 0.0F, 186.0F, 102, 5, TEX_W, TEX_H);

        int levelStart = VillagerLevel.xpForLevel(level);
        int levelEnd = VillagerLevel.xpForNextLevel(level);
        int span = levelEnd - levelStart;
        if (span <= 0) {
            return;
        }

        // 1.14 scales against a 100px fill area inside the 102px bar, then draws one extra pixel.
        //
        // It computes this as `(float)(100 / span)` - integer division, so at levels 2 through 4
        // the scale collapses to 1 and the bar tops out at 60, 80 and 100 percent instead of
        // filling. That is a bug rather than a design, and copying it would mean shipping a bar
        // that never reaches the end, so the division is done in floating point here.
        float perXp = 100.0F / (float) span;

        // Clamped because experience can exceed the current level's threshold while the level-up
        // waits for the screen to close. The bar fills and holds there instead of overrunning.
        int filled = MathHelper.clamp(MathHelper.floor(perXp * (float) (xp - levelStart)), 0, 100);
        drawModalRectWithCustomSizedTexture(x, y, 0.0F, 191.0F, filled + 1, 5, TEX_W, TEX_H);

        drawPendingExperience(x, y, filled, perXp);
    }

    /**
     * The white segment showing what the selected trade is worth, drawn ahead of the green fill.
     *
     * <p>This is the part of 1.14's bar that makes it readable: before you trade, a white block
     * previews how much of the level that trade will advance you, and once the trade completes the
     * green catches up and swallows it.
     *
     * <p>1.14 reads the amount from the offer's own experience value. 1.12.2 recipes carry no such
     * number - only a flag for whether a trade grants experience at all - so the configured
     * per-trade amount is used, which is the same figure the server awards.
     */
    private void drawPendingExperience(int x, int y, int filled, float perXp) {
        // Only preview experience for a trade that could actually be completed right now.
        //
        // 1.14 does this through MerchantInventory: the pending amount is reset to zero whenever the
        // input slots are empty or do not match an offer, and only set once a real trade is lined up.
        //
        // The condition is the input slots, not the result slot, and the difference is visible. The
        // result slot empties for a moment every time a trade is taken and is refilled on the same
        // click if the inputs still cover another one - so keying the preview off it makes the
        // marker blink out and back on every single trade. Trading quickly turns that into a strobe
        // at the end of the bar, which is what a player sees as the bar jumping about. The inputs
        // stay put across a trade, so the preview stays put with them, and it is still empty when
        // the screen first opens with nothing lined up.
        if (inputEmpty(SLOT_INPUT_A) && inputEmpty(SLOT_INPUT_B)) {
            return;
        }

        MerchantRecipeList recipes = recipes();
        if (recipes == null || this.selectedRecipe < 0 || this.selectedRecipe >= recipes.size()) {
            return;
        }

        if (recipes.get(this.selectedRecipe).isRecipeDisabled()) {
            return;
        }

        // Sent by the server, which is the only side that knows which tier each trade came from.
        int pending = MerchantOverlay.tradeXp(this.selectedRecipe);
        if (pending <= 0) {
            return;
        }

        int width = Math.min(MathHelper.floor((float) pending * perXp), 100 - filled);
        if (width <= 0) {
            return;
        }

        // Inset by a pixel on both axes so it sits inside the bar's border rather than over it.
        drawModalRectWithCustomSizedTexture(
                x + filled + 1, y + 1, 2.0F, 182.0F, width, 3, TEX_W, TEX_H);
    }

    private void drawScrollbar() {
        int x = this.guiLeft + 94;
        int y = this.guiTop + ROW_Y;
        int maxScroll = maxScroll();

        this.mc.getTextureManager().bindTexture(TEXTURE);

        if (maxScroll <= 0) {
            // The greyed-out handle, at u=6 rather than u=0.
            drawModalRectWithCustomSizedTexture(x, y, 6.0F, 199.0F, 6, 27, TEX_W, TEX_H);
            return;
        }

        int track = VISIBLE_ROWS * ROW_HEIGHT - 27;
        int offset = Math.round((float) this.scrollOffset / (float) maxScroll * (float) track);
        drawModalRectWithCustomSizedTexture(x, y + offset, 0.0F, 199.0F, 6, 27, TEX_W, TEX_H);
    }

    /** The crossed-out marker 1.14 lays over the result slot when the selected trade is sold out. */
    private void drawOutOfStockMarker() {
        MerchantRecipeList recipes = recipes();
        if (recipes == null || this.selectedRecipe < 0 || this.selectedRecipe >= recipes.size()) {
            return;
        }

        if (!recipes.get(this.selectedRecipe).isRecipeDisabled()) {
            return;
        }

        GlStateManager.disableLighting();
        this.mc.getTextureManager().bindTexture(TEXTURE);
        drawModalRectWithCustomSizedTexture(
                this.guiLeft + 182, this.guiTop + 35, 311.0F, 0.0F, 28, 21, TEX_W, TEX_H);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        // Coordinates here are relative to the window, not the screen.
        String title = Translate.villagerName(this.merchant.getDisplayName().getFormattedText());
        int xp = MerchantOverlay.currentXp();

        if (xp >= 0) {
            String rank = "- " + Translate.levelName(MerchantOverlay.currentLevel());
            int titleWidth = this.fontRenderer.getStringWidth(title);
            int rankWidth = this.fontRenderer.getStringWidth(rank);
            int left = 49 + this.xSize / 2 - (titleWidth + rankWidth + 3) / 2;

            this.fontRenderer.drawString(title, left, 6, 4210752);
            this.fontRenderer.drawString(rank, left + titleWidth + 3, 6, 4210752);
        } else {
            int left = 49 + this.xSize / 2 - this.fontRenderer.getStringWidth(title) / 2;
            this.fontRenderer.drawString(title, left, 6, 4210752);
        }

        String trades = Translate.of("gui.villagerbackport.trades", "Trades");
        this.fontRenderer.drawString(trades, 53 - this.fontRenderer.getStringWidth(trades) / 2, 6, 4210752);

        this.fontRenderer.drawString(
                this.mc.player.inventory.getDisplayName().getUnformattedText(), 107, this.ySize - 94, 4210752);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);

        MerchantRecipeList recipes = recipes();
        if (recipes != null && !recipes.isEmpty()) {
            drawTradeRows(recipes);
        }

        // Slot tooltips first, then the trade list's. The two regions never overlap, so at most one
        // of them draws anything.
        this.renderHoveredToolTip(mouseX, mouseY);

        if (recipes != null && !recipes.isEmpty()) {
            drawTradeTooltip(recipes, mouseX, mouseY);
            drawOutOfStockTooltip(recipes, mouseX, mouseY);
        }

        drawExperienceTooltip(mouseX, mouseY);
    }

    /**
     * The figures behind the experience bar, on hover.
     *
     * <p>The bar alone cannot show what happens either side of a level-up. Experience earned past a
     * threshold keeps counting, but the bar is already full and has nowhere to put it, and once the
     * level lands that surplus is measured against a level six times wider - so a few trades' worth
     * of carried-over progress lands close enough to the left edge to read as a reset. The numbers
     * say plainly that it was kept.
     *
     * <p>Measured against the next threshold the villager has <em>not</em> reached rather than the
     * level currently in effect, so the target moves up the moment the experience is earned and the
     * total never appears to fall back.
     */
    private void drawExperienceTooltip(int mouseX, int mouseY) {
        int xp = MerchantOverlay.currentXp();
        if (xp < 0 || !VillagerLevel.canLevelUp(MerchantOverlay.currentLevel())) {
            return;
        }

        if (!isPointInRegion(136, 15, 102, 7, mouseX, mouseY)) {
            return;
        }

        int next = VillagerLevel.xpForNextLevel(VillagerLevel.levelFor(xp));

        String text = next > 0
                ? Translate.of("gui.villagerbackport.xp.progress", "%s/%s XP", xp, next)
                : Translate.of("gui.villagerbackport.xp.max", "%s XP", xp);

        drawHoveringText(text, mouseX, mouseY);
    }

    /**
     * Explains the crossed-out marker over the result slot.
     *
     * <p>Sold-out trades are otherwise a dead end with no indication of what to do about it. 1.14
     * shows this same line from the same region - a 22x21 box at (186, 35) - and it is the only
     * place the restock limit is communicated at all.
     */
    private void drawOutOfStockTooltip(MerchantRecipeList recipes, int mouseX, int mouseY) {
        if (this.selectedRecipe < 0 || this.selectedRecipe >= recipes.size()) {
            return;
        }

        if (!recipes.get(this.selectedRecipe).isRecipeDisabled()) {
            return;
        }

        if (isPointInRegion(186, 35, 22, 21, mouseX, mouseY)) {
            drawHoveringText(Translate.of("gui.villagerbackport.outofstock",
                    "Villagers restock up to two times per day."), mouseX, mouseY);
        }
    }

    /**
     * Tooltip for an item in the trade list.
     *
     * <p>The list is drawn by hand rather than being made of real inventory slots, so none of
     * vanilla's slot tooltip handling applies to it and hovering a trade showed nothing. That
     * matters most for enchanted books, where the item name alone is "Enchanted Book" and the
     * tooltip is the only thing that says which enchantment is actually for sale.
     */
    private void drawTradeTooltip(MerchantRecipeList recipes, int mouseX, int mouseY) {
        for (int row = 0; row < VISIBLE_ROWS; row++) {
            int index = row + this.scrollOffset;
            if (index >= recipes.size()) {
                return;
            }

            MerchantRecipe recipe = recipes.get(index);
            int y = this.guiTop + 19 + row * ROW_HEIGHT;

            ItemStack hovered = hoveredStack(recipe, y, mouseX, mouseY);
            if (!hovered.isEmpty()) {
                this.renderToolTip(hovered, mouseX, mouseY);
                return;
            }
        }
    }

    /** @return the trade item under the cursor on this row, or empty. */
    private ItemStack hoveredStack(MerchantRecipe recipe, int y, int mouseX, int mouseY) {
        if (mouseY < y || mouseY >= y + 16) {
            return ItemStack.EMPTY;
        }

        if (isOverIcon(mouseX, this.guiLeft + 10)) {
            return recipe.getItemToBuy();
        }
        if (isOverIcon(mouseX, this.guiLeft + 40)) {
            return recipe.getSecondItemToBuy();
        }
        if (isOverIcon(mouseX, this.guiLeft + 73)) {
            return recipe.getItemToSell();
        }

        return ItemStack.EMPTY;
    }

    private boolean isOverIcon(int mouseX, int iconX) {
        return mouseX >= iconX && mouseX < iconX + 16;
    }

    /**
     * Draws the item icons for each visible trade row.
     *
     * <p>Done after {@code super.drawScreen} rather than in the foreground layer so these sit above
     * the row buttons, matching how 1.14 renders offers after the rest of the screen.
     */
    private void drawTradeRows(MerchantRecipeList recipes) {
        GlStateManager.pushMatrix();
        RenderHelper.enableGUIStandardItemLighting();
        GlStateManager.enableRescaleNormal();
        GlStateManager.enableColorMaterial();
        GlStateManager.enableLighting();

        this.itemRender.zLevel = 100.0F;

        for (int row = 0; row < VISIBLE_ROWS; row++) {
            int index = row + this.scrollOffset;
            if (index >= recipes.size()) {
                break;
            }

            MerchantRecipe recipe = recipes.get(index);
            int y = this.guiTop + 19 + row * ROW_HEIGHT;

            drawCost(recipe.getItemToBuy(), MerchantOverlay.basePrice(index), this.guiLeft + 10, y);

            ItemStack second = recipe.getSecondItemToBuy();
            if (!second.isEmpty()) {
                drawStack(second, this.guiLeft + 40, y);
            }

            drawArrow(recipe, this.guiLeft + 60, y + 3);
            drawStack(recipe.getItemToSell(), this.guiLeft + 73, y);
        }

        this.itemRender.zLevel = 0.0F;

        GlStateManager.disableLighting();
        RenderHelper.disableStandardItemLighting();
        GlStateManager.popMatrix();
    }

    /**
     * @return the key that refills the selected trade
     *
     * <p>Configurable because it has to share a keyboard with whatever else is installed. Space is
     * 1.14's - there it presses the focused trade button - but it is also what Inventory Tweaks
     * binds its "move everything" shortcut to by default, and a shortcut that consumes the click
     * wins every time. Two mods cannot both have the key; this side of it can at least move.
     */
    private static int refillKey() {
        int key = Keyboard.getKeyIndex(ModConfig.display.refillKey.trim().toUpperCase(Locale.ROOT));
        return key == Keyboard.KEY_NONE ? Keyboard.KEY_SPACE : key;
    }

    /** @return true if that input slot is holding nothing. */
    private boolean inputEmpty(int slot) {
        return this.inventorySlots.getSlot(slot).getStack().isEmpty();
    }

    /**
     * The first cost slot, showing both prices when demand has moved one.
     *
     * <h2>1.14's layout</h2>
     * The price the trade started at is drawn in the slot with a red line struck through it, and
     * what it actually costs now is drawn immediately to its right. Without the struck-out original
     * there is nothing on screen to say a price moved at all - the trade simply looks expensive, and
     * a player has no way to tell a dear trade from one they have made dear by clearing it out.
     *
     * <p>The offset and the line come straight from 1.14: the adjusted stack sits 14 pixels along,
     * which is inside the gap before the second cost slot, and the line is a 9x2 sprite from the
     * merchant texture drawn across the middle of the original.
     */
    private void drawCost(ItemStack cost, int basePrice, int x, int y) {
        // Only ever drawn for a price that went up.
        //
        // Demand cannot make a trade cheaper than it started - 1.14 floors the surcharge at zero in
        // MerchantOffer.getCostA, so demand raises a price or leaves it alone, and a trade nobody
        // buys falls back towards its base rather than below it. Discounts in 1.14 come from
        // specialPrice, which gossip and Hero of the Village drive, and none of that exists here yet.
        //
        // So a recorded base above the current price is not a discount, it is a stale figure - two
        // trades of the same items share a key, and vanilla re-rolls prices when it unlocks a tier,
        // which can leave the base belonging to a trade the villager no longer offers. Drawing that
        // as a saving would be inventing one.
        if (basePrice <= 0 || basePrice == cost.getCount()) {
            drawStack(cost, x, y);
            return;
        }

        ItemStack original = cost.copy();
        original.setCount(basePrice);

        drawStack(original, x, y);
        drawStack(cost, x + 14, y);

        // Over the item, or the line lands underneath it and is never seen.
        GlStateManager.pushMatrix();
        GlStateManager.translate(0.0F, 0.0F, 400.0F);

        RenderHelper.disableStandardItemLighting();
        GlStateManager.enableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(TEXTURE);

        drawModalRectWithCustomSizedTexture(x + 7, y + 12, 0.0F, 176.0F, 9, 2, TEX_W, TEX_H);

        RenderHelper.enableGUIStandardItemLighting();
        GlStateManager.popMatrix();
    }

    private void drawStack(ItemStack stack, int x, int y) {
        this.itemRender.renderItemAndEffectIntoGUI(stack, x, y);
        this.itemRender.renderItemOverlays(this.fontRenderer, stack, x, y);
    }

    /** The arrow between cost and result, greyed when the trade is sold out. */
    private void drawArrow(MerchantRecipe recipe, int x, int y) {
        RenderHelper.disableStandardItemLighting();
        GlStateManager.enableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(TEXTURE);

        float u = recipe.isRecipeDisabled() ? 25.0F : 15.0F;
        drawModalRectWithCustomSizedTexture(x, y, u, 171.0F, 10, 9, TEX_W, TEX_H);

        RenderHelper.enableGUIStandardItemLighting();
    }

    /**
     * One selectable trade row.
     *
     * <p>Draws as a plain button with no label; the trade's items are rendered over the top. Rows
     * past the end of the trade list are hidden rather than removed, so scrolling does not have to
     * rebuild the button list.
     */
    private class TradeRowButton extends GuiButton {

        private final int row;

        TradeRowButton(int row, int x, int y) {
            super(row, x, y, ROW_WIDTH, ROW_HEIGHT, "");
            this.row = row;
        }

        @Override
        public void drawButton(net.minecraft.client.Minecraft mc, int mouseX, int mouseY, float partialTicks) {
            MerchantRecipeList recipes = GuiVillagerMerchant.this.recipes();
            this.visible = recipes != null && this.row + GuiVillagerMerchant.this.scrollOffset < recipes.size();

            if (this.visible) {
                super.drawButton(mc, mouseX, mouseY, partialTicks);
            }
        }
    }
}
