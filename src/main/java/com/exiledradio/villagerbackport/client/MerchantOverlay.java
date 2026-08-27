package com.exiledradio.villagerbackport.client;

import com.exiledradio.villagerbackport.ModConfig;
import com.exiledradio.villagerbackport.VillagerBackport;
import com.exiledradio.villagerbackport.trade.VillagerLevel;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiMerchant;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.IMerchant;
import net.minecraft.inventory.Container;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.village.MerchantRecipe;
import net.minecraft.village.MerchantRecipeList;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.lang.reflect.Field;

/**
 * Draws a villager's level and experience above the vanilla trade screen.
 *
 * <h2>Why an overlay rather than a custom screen</h2>
 * 1.14 shows the level as part of a redesigned merchant screen. Rebuilding that here would mean a
 * custom {@code Container} and {@code GuiScreen}, and opening ours instead of vanilla's - which is
 * the one part of this feature with real compatibility cost. FermiumMixins patches
 * {@code ContainerMerchant} (its {@code ContainerMerchant_DropsMixin}), and any mod that opens or
 * decorates a merchant screen expects the vanilla classes. Replacing them means their work silently
 * stops applying to villagers.
 *
 * <p>Drawing on top of the vanilla screen avoids all of it. Vanilla's container and screen stay
 * exactly as they are, every other mod's patches and hooks keep working, and this mod contributes
 * pixels and nothing else. The cost is that the level display sits outside the window rather than
 * being integrated into it, and that trades cannot be visually locked behind a level - a container
 * change would be needed for that, and it is not worth the conflict surface.
 *
 * <p>The overlay is drawn <em>above</em> the window rather than inside it. The area inside is shared
 * with vanilla's widgets and with anything another mod chooses to draw; the strip above is not.
 */
@SideOnly(Side.CLIENT)
public final class MerchantOverlay {

    /** Height of the experience bar in pixels. */
    private static final int BAR_HEIGHT = 3;

    /** Gap between the bottom of our overlay and the top of the trade window. */
    private static final int BAR_GAP = 2;

    private static final int COLOUR_TEXT = 0xFFFFFF;
    private static final int COLOUR_BAR_BACKGROUND = 0xFF3E3E3E;
    private static final int COLOUR_BAR_FILL = 0xFF80FF20;
    private static final int COLOUR_BAR_FULL = 0xFF39C0FF;

    /**
     * Experience for the villager whose screen is open, or -1 when we have not been told.
     *
     * <p>Static because there is only ever one trade screen open at a time, and because the render
     * event gives us no way to carry state alongside the screen it hands us.
     */
    private static int currentXp = -1;

    /** Level in effect, which lags {@link #currentXp} until the screen closes. */
    private static int currentLevel = VillagerLevel.MIN_LEVEL;

    @Nullable
    private static Field guiLeftField;

    @Nullable
    private static Field guiTopField;

    private static boolean fieldsResolved;

    /** Experience each trade is worth, in trade-list order. */
    private static int[] tradeXp = new int[0];

    /** What each trade cost before demand moved it, or zero where it has not moved. */
    private static int[] basePrices = new int[0];

    /** Called from the network handler on the client thread when a villager's data arrives. */
    public static void acceptData(int xp, int level, int[] perTradeXp, int[] perTradeUses,
                                  int[] perTradeBasePrice) {
        currentXp = xp;
        currentLevel = level;
        tradeXp = perTradeXp == null ? new int[0] : perTradeXp;
        basePrices = perTradeBasePrice == null ? new int[0] : perTradeBasePrice;
        applyUses(perTradeUses);
    }

    /**
     * Corrects the client's idea of how used up each trade is.
     *
     * <p>The client maintains its own use counts, incrementing them as it predicts each trade, and
     * nothing in vanilla tells it when the server's figures moved independently - the trade list is
     * sent once when the screen opens and never again. A restock zeroes the real counts silently, so
     * the screen could show a trade as sold out while it still worked, or the reverse, and rapid
     * trading drifted further apart the longer a screen stayed open.
     *
     * <p>{@code MerchantRecipe} has no setter for the count, so each one is round-tripped through
     * its own NBT - the same trick used server-side to reset uses on a restock.
     */
    private static void applyUses(int[] uses) {
        if (uses == null || uses.length == 0) {
            return;
        }

        GuiScreen screen = Minecraft.getMinecraft().currentScreen;
        if (!(screen instanceof GuiVillagerMerchant)) {
            return;
        }

        MerchantRecipeList recipes = ((GuiVillagerMerchant) screen).recipes();
        if (recipes == null) {
            return;
        }

        int count = Math.min(uses.length, recipes.size());
        for (int i = 0; i < count; i++) {
            MerchantRecipe recipe = recipes.get(i);
            if (recipe.getToolUses() == uses[i]) {
                continue;
            }

            NBTTagCompound tag = recipe.writeToTags();
            tag.setInteger("uses", uses[i]);
            recipe.readFromTags(tag);
        }
    }

    /** @return experience for the villager whose screen is open, or -1 if unknown. */
    public static int currentXp() {
        return currentXp;
    }

    /**
     * @return the level in effect for the villager whose screen is open.
     *
     * <p>Sent by the server rather than worked out from experience, because the two deliberately
     * disagree while the screen is open: experience earned now does not become a level until the
     * player closes the screen, so deriving it here would show a rank the villager has not reached.
     */
    public static int currentLevel() {
        return Math.max(VillagerLevel.MIN_LEVEL, currentLevel);
    }

    /** @return experience the trade at the given index grants, or 0 if not known. */
    public static int tradeXp(int index) {
        return index >= 0 && index < tradeXp.length ? tradeXp[index] : 0;
    }

    /** @return what the trade at the given index cost before demand moved it, or 0 if unmoved. */
    public static int basePrice(int index) {
        return index >= 0 && index < basePrices.length ? basePrices[index] : 0;
    }

    /**
     * Drops stored data when the player leaves the trade screen.
     *
     * <p>Without this, the numbers from the last villager would still be sitting there the next time
     * a merchant screen opened - briefly showing another villager's level before its own data
     * arrived. Opening the merchant screen itself must not clear, because the data packet is sent on
     * interact and therefore arrives before the screen does.
     */
    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) {
        GuiScreen gui = event.getGui();

        // Both the vanilla screen and our replacement count as "still trading" - the replacement is
        // not a GuiMerchant, so without naming it here the swap below would wipe the data it needs.
        if (!(gui instanceof GuiMerchant) && !(gui instanceof GuiVillagerMerchant)) {
            currentXp = -1;
        }
    }

    /**
     * Swaps the vanilla trade screen for the 1.14-style one, once the trade list has arrived.
     *
     * <h2>Why this waits instead of swapping as the screen opens</h2>
     * The client only accepts a villager's trade list if the screen it is looking at is a
     * {@code GuiMerchant}. {@code NetHandlerPlayClient.handleCustomPayload} gates the {@code
     * MC|TrList} channel behind {@code guiscreen instanceof GuiMerchant}, and our replacement is a
     * plain {@code GuiContainer}. Swapping during {@code GuiOpenEvent} - before that packet is
     * handled - meant the trade list was read off the wire and then thrown away, which is why the
     * trade list rendered empty.
     *
     * <p>So vanilla's screen is allowed to open and receive the list, and the swap happens after.
     * Waiting is safe because the trade list is sent exactly once, from
     * {@code EntityPlayerMP.displayVillagerTradeGui}, and never resent. Nothing after this point
     * needs the screen to be a {@code GuiMerchant}.
     *
     * <h2>Why this runs on the render tick rather than the game tick</h2>
     * {@code Minecraft.runGameLoop} drains queued packet handlers every frame, but only calls
     * {@code runTick} twenty times a second:
     * <pre>
     *   1172  scheduledExecutables    // packet handlers - the screen opens here, every frame
     *   1188  this.runTick()          // ClientTickEvent - only 20x/sec
     *   1207  onRenderTickStart       // every frame
     *   1209  updateCameraAndRender   // the draw
     * </pre>
     * Swapping on the game tick therefore let several frames render the vanilla screen first at any
     * framerate above 20, which showed up as a visible flash. The render tick fires every frame,
     * after the packet handlers and before the draw, which closes that window entirely.
     */
    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.START || !ModConfig.display.useModernTradeScreen) {
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        if (!(mc.currentScreen instanceof GuiMerchant) || !SlotLayout.isAvailable() || mc.player == null) {
            return;
        }

        try {
            GuiMerchant vanilla = (GuiMerchant) mc.currentScreen;
            IMerchant merchant = vanilla.getMerchant();
            if (merchant == null) {
                return;
            }

            // Hold off until the trade list has actually landed. Both packets are drained in the
            // same batch above, so this is normally true on the very first frame - but if they ever
            // split across frames, waiting means showing the vanilla screen briefly rather than
            // swapping to a screen whose trade list was discarded.
            MerchantRecipeList recipes = merchant.getRecipes(mc.player);
            if (recipes == null || recipes.isEmpty()) {
                return;
            }

            // The existing container is handed straight over. It is already registered with the
            // server under a window id, so building a fresh one would leave the server talking to a
            // window the client no longer has.
            Container container = vanilla.inventorySlots;
            SlotLayout.apply(container);

            mc.displayGuiScreen(new GuiVillagerMerchant(container, merchant, mc.player.inventory));
        } catch (RuntimeException e) {
            VillagerBackport.LOGGER.error(
                    "Failed to open the 1.14-style trade screen; keeping the vanilla one.", e);
        }
    }

    @SubscribeEvent
    public void onDrawScreen(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (!ModConfig.display.showVillagerLevel || currentXp < 0) {
            return;
        }

        GuiScreen screen = event.getGui();

        // Only the vanilla screen needs this. The 1.14-style replacement is a GuiContainer rather
        // than a GuiMerchant and draws level and experience itself, in their proper places, so it
        // never matches here.
        if (!(screen instanceof GuiMerchant)) {
            return;
        }

        Integer guiLeft = readField(guiLeftField(), screen);
        Integer guiTop = readField(guiTopField(), screen);
        if (guiLeft == null || guiTop == null) {
            return;
        }

        draw(guiLeft, guiTop);
    }

    private void draw(int guiLeft, int guiTop) {
        Minecraft mc = Minecraft.getMinecraft();

        int level = VillagerLevel.levelFor(currentXp);
        boolean maxed = !VillagerLevel.canLevelUp(level);

        String label = Translate.levelName(level);
        String progress = maxed
                ? Translate.of("gui.villagerbackport.xp.max", "%s XP", currentXp)
                : Translate.of("gui.villagerbackport.xp.progress", "%s/%s XP",
                        currentXp, VillagerLevel.xpForNextLevel(level));

        String text = label + "  " + progress;

        int barWidth = mc.fontRenderer.getStringWidth(text);
        int textY = guiTop - (mc.fontRenderer.FONT_HEIGHT + BAR_HEIGHT + BAR_GAP);
        int barY = textY + mc.fontRenderer.FONT_HEIGHT;

        mc.fontRenderer.drawStringWithShadow(text, guiLeft, textY, COLOUR_TEXT);

        Gui.drawRect(guiLeft, barY, guiLeft + barWidth, barY + BAR_HEIGHT, COLOUR_BAR_BACKGROUND);

        int filled = maxed ? barWidth : filledWidth(barWidth, level);
        if (filled > 0) {
            Gui.drawRect(guiLeft, barY, guiLeft + filled, barY + BAR_HEIGHT,
                    maxed ? COLOUR_BAR_FULL : COLOUR_BAR_FILL);
        }
    }

    /**
     * @return how much of the bar to fill, as progress through the current level rather than
     * progress toward the total.
     *
     * <p>Measuring within the level is what makes the bar useful: the thresholds are 10, 70, 150 and
     * 250, so a bar scaled to the total would barely move through the early levels and then crawl.
     */
    private int filledWidth(int barWidth, int level) {
        int levelStart = VillagerLevel.xpForLevel(level);
        int levelEnd = VillagerLevel.xpForNextLevel(level);

        int span = levelEnd - levelStart;
        if (span <= 0) {
            return 0;
        }

        float fraction = (float) (currentXp - levelStart) / (float) span;
        return Math.round(Math.max(0.0F, Math.min(1.0F, fraction)) * barWidth);
    }

    @Nullable
    private static Integer readField(@Nullable Field field, GuiScreen screen) {
        if (field == null) {
            return null;
        }
        try {
            return field.getInt(screen);
        } catch (IllegalAccessException e) {
            return null;
        }
    }

    @Nullable
    private static Field guiLeftField() {
        resolveFields();
        return guiLeftField;
    }

    @Nullable
    private static Field guiTopField() {
        resolveFields();
        return guiTopField;
    }

    /**
     * {@code guiLeft} and {@code guiTop} are protected on {@code GuiContainer}, so an outside
     * observer cannot read them directly. Resolved once and cached; if the lookup fails the overlay
     * simply does not draw rather than throwing every frame.
     */
    private static void resolveFields() {
        if (fieldsResolved) {
            return;
        }
        fieldsResolved = true;

        try {
            guiLeftField = ReflectionHelper.findField(
                    net.minecraft.client.gui.inventory.GuiContainer.class, "guiLeft", "field_147003_i");
            guiLeftField.setAccessible(true);

            guiTopField = ReflectionHelper.findField(
                    net.minecraft.client.gui.inventory.GuiContainer.class, "guiTop", "field_147009_r");
            guiTopField.setAccessible(true);
        } catch (ReflectionHelper.UnableToFindFieldException e) {
            VillagerBackport.LOGGER.error(
                    "Could not resolve GuiContainer's layout fields; the villager level overlay will "
                            + "not be drawn. Trading itself is unaffected.", e);
            guiLeftField = null;
            guiTopField = null;
        }
    }
}
