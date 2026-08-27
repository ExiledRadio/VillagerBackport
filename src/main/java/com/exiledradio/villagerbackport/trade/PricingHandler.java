package com.exiledradio.villagerbackport.trade;

import com.exiledradio.villagerbackport.ModConfig;
import com.exiledradio.villagerbackport.compat.VillagerAccess;
import com.exiledradio.villagerbackport.data.VillagerTradeData;
import com.exiledradio.villagerbackport.network.NetworkHandler;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.village.MerchantRecipe;
import net.minecraft.village.MerchantRecipeList;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Writes demand-adjusted prices onto a villager's trades at the moment a player opens them.
 *
 * <h2>Timing</h2>
 * {@code EntityPlayer.interactOn} fires Forge's interact hook before calling
 * {@code processInitialInteract}, which is what opens the trade screen:
 * <pre>
 *   EnumActionResult cancelResult = ForgeHooks.onInteractEntity(this, entityToInteractOn, hand);
 *   if (cancelResult != null) return cancelResult;
 *   ...
 *   if (entityToInteractOn.processInitialInteract(this, hand))
 * </pre>
 * So by the time vanilla builds the recipe list to send to the client, the prices we set here are
 * already in place - the player never sees a price change under them, and the container validates
 * trades against the same adjusted stack it displayed.
 *
 * <p>Taking the surcharge back off is deliberately <em>not</em> done from a GUI-close event. It
 * happens on the next routine poll once the villager has no customer, which covers closing the
 * screen normally along with the cases a close event would miss: disconnecting mid-trade, or the
 * chunk unloading while the screen is open.
 *
 * <h2>Compatibility</h2>
 * The handler observes the event and never cancels it or changes its result, so it cannot stop
 * another mod's interaction from running. It uses {@link EventPriority#LOWEST} so that any mod which
 * <em>does</em> cancel the interaction - a protection mod, say - has already done so, and we skip
 * pricing a trade screen that is never going to open.
 */
public final class PricingHandler {

    /**
     * Drops the trade a player had loaded once their screen shuts.
     *
     * <p>The refill remembers which trade to keep topping up, and that note belongs to the screen
     * rather than to the player - left behind it would outlive the villager, the session and the
     * player themselves.
     */
    @SubscribeEvent
    public void onContainerClosed(PlayerContainerEvent.Close event) {
        if (event.getEntityPlayer() instanceof EntityPlayerMP) {
            com.exiledradio.villagerbackport.network.PacketRefillTrade.Handler
                    .forget((EntityPlayerMP) event.getEntityPlayer());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof EntityVillager)) {
            return;
        }

        // Prices belong to the server's copy of the trades; the client receives the result.
        if (event.getWorld().isRemote || event.isCanceled()) {
            return;
        }

        if (!VillagerAccess.isAvailable()) {
            return;
        }

        EntityVillager villager = (EntityVillager) event.getTarget();

        // A villager already serving someone else will not open a second screen, and its recipes
        // are mid-session with prices applied. Leave them alone.
        if (villager.getCustomer() != null || villager.isChild()) {
            return;
        }

        MerchantRecipeList recipes = VillagerAccess.getBuyingList(villager);

        // Send the level data before pricing, and independently of it. The two features are
        // separately switchable, and the client needs the experience figures whether or not prices
        // are being adjusted.
        if (ModConfig.display.showVillagerLevel && event.getEntityPlayer() instanceof EntityPlayerMP) {
            NetworkHandler.sendVillagerData(
                    (EntityPlayerMP) event.getEntityPlayer(),
                    VillagerTradeData.getXp(villager),
                    displayLevel(villager),
                    TradeXp.forList(villager, recipes),
                    TradeXp.usesList(recipes),
                    PriceEngine.baseList(villager, recipes));
        }

        if (!ModConfig.pricing.demandPricing || recipes == null) {
            // A null list means vanilla has not built one yet. It will when the screen opens, and
            // our next poll records the base prices; pricing starts from the interaction after this.
            return;
        }

        // What this villager thinks of this player, priced in. 1.14 works this out in
        // updateSpecialPrices immediately before setting the customer, for exactly this reason: the
        // discount belongs to the player who opened the screen, not to the trade.
        int standing = Gossip.specialPriceFor(villager, event.getEntityPlayer());

        for (MerchantRecipe recipe : recipes) {
            PriceEngine.applyPrice(villager, recipe, TradeKey.of(villager, recipe), standing);
        }
    }

    /**
     * @return the level to show on the trade screen.
     *
     * <p>The level actually in effect, not the one the villager's experience would imply. The two
     * disagree on purpose while a screen is open: experience earned now does not become a level
     * until the player closes it, so sending the derived value would show a rank the villager has
     * not taken yet.
     *
     * <p>Falls back to the derived level for a villager the tick has not reached, which has no
     * applied level recorded yet.
     */
    private int displayLevel(EntityVillager villager) {
        int applied = VillagerTradeData.getAppliedLevel(villager);
        return applied > 0 ? applied : VillagerLevel.levelFor(VillagerTradeData.getXp(villager));
    }
}
