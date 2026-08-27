package com.exiledradio.villagerbackport.trade;

import com.exiledradio.villagerbackport.ModConfig;
import com.exiledradio.villagerbackport.data.VillagerTradeData;
import com.exiledradio.villagerbackport.network.NetworkHandler;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.village.MerchantRecipe;
import net.minecraft.village.MerchantRecipeList;

import java.util.HashSet;
import java.util.Set;

/**
 * Notices completed trades and keeps per-trade state in step with them.
 *
 * <h2>Why polling and not an event</h2>
 * 1.14 does this work inside {@code VillagerEntity.onTrade}, called the moment a trade completes.
 * 1.12.2 has an equivalent method - {@code EntityVillager.useRecipe} - but Forge fires no event
 * from it, and it is not overridable without subclassing the entity or patching the class, both of
 * which this mod avoids on purpose.
 *
 * <p>What 1.12.2 does give us is the evidence: {@code useRecipe} calls
 * {@code recipe.incrementToolUses()}, so a trade's use count is a running total of how many times a
 * player has used it. Recording that count each time we look at a villager and comparing it against
 * the previous reading tells us exactly how many trades happened in between, without needing to be
 * present when they happened. It is a poll rather than a callback, but it cannot miss a trade: the
 * count is cumulative, so any trades completed between two readings show up in the difference.
 *
 * <p>The one case needing care is restock, which sets uses back to zero. A naive comparison would
 * then read as a large negative delta. {@link #resetTracking} is called as part of restocking so
 * the next reading starts from a correct baseline.
 */
public final class TradeTracker {

    private TradeTracker() {
    }

    /**
     * Reads the villager's trade list, awards experience for any trades completed since the last
     * reading, and applies current demand pricing.
     *
     * <h2>Bulk trading</h2>
     * Experience is deliberately settled in one go rather than trade by trade. Because a trade's use
     * count is cumulative, emptying a stack into a villager shows up as a single large difference on
     * the next reading, which becomes one addition to the total and one packet to the player - no
     * matter whether it was one trade or forty. Reacting to each individual trade would mean a
     * write and a packet per exchange, which is what makes bulk trading stutter.
     *
     * <p>This only affects the total. The white marker on the experience bar stays sized to a single
     * trade, because that is what it means - what the next exchange is worth, not what a stack of
     * them would be.
     *
     * @return the set of live trade keys, for pruning stale stored state
     */
    public static Set<String> poll(EntityVillager villager, MerchantRecipeList recipes) {
        // Keeps the payment slot loaded while a player is trading - see PacketRefillTrade.topUp.
        if (villager.getCustomer() instanceof EntityPlayerMP) {
            com.exiledradio.villagerbackport.network.PacketRefillTrade.Handler
                    .topUp((EntityPlayerMP) villager.getCustomer());
        }

        Set<String> liveKeys = new HashSet<String>();
        int xpGained = 0;

        for (int index = 0; index < recipes.size(); index++) {
            MerchantRecipe recipe = recipes.get(index);
            String key = TradeKey.of(villager, recipe);
            liveKeys.add(key);

            // Before reading uses, so a trade brought up to 1.14's stock is not treated as sold out
            // on the same pass that raised its ceiling.
            TradeStock.normalise(recipe);

            int uses = recipe.getToolUses();
            int lastSeen = VillagerTradeData.getLastSeenUses(villager, key);

            // A negative delta means something reset the count behind our back - most likely
            // vanilla's own restock path, which we deliberately leave running. Re-baseline rather
            // than trying to interpret it.
            int delta = uses - lastSeen;
            if (delta > 0) {

                xpGained += countedTrades(recipe, lastSeen, delta)
                        * TradeXp.forTrade(recipe, TradeTier.forIndex(villager, index));
            }

            if (delta != 0) {
                VillagerTradeData.setLastSeenUses(villager, key, uses);
            }

            // Prices are only carried in the recipe while someone is trading - see PricingHandler.
            // With no customer the recipe should be sitting at its base price, so this is both where
            // we learn what that base is and where we put back any surcharge left over from the last
            // trading session. Doing it here rather than on GUI close also covers the cases a close
            // event would miss: a disconnect mid-trade, or a chunk unloading with the screen open.
            if (villager.getCustomer() == null) {
                PriceEngine.recordBaseIfAbsent(villager, recipe, key);
                PriceEngine.restoreBasePrice(villager, recipe, key);
            }
        }

        if (xpGained > 0) {
            VillagerTradeData.setXp(villager, VillagerTradeData.getXp(villager) + xpGained);
            pushToCustomer(villager, recipes);

            // 1.14 raises TRADING gossip by 2 for the player it just traded with, from the same tick
            // that fires the happy particles. It caps at 25, so trading your way into a villager's
            // good books is worth about one emerald off and no more - being liked is a courtesy
            // here, not a business model.
            EntityPlayer customer = villager.getCustomer();
            if (customer != null) {
                Gossip.add(villager, customer.getUniqueID(), GossipType.TRADING, 2);
            }
        }

        return liveKeys;
    }

    /**
     * @return how many of the trades seen since the last reading should actually count.
     *
     * <p>A trade can only be made as many times as it has stock left. Vanilla enforces that by
     * refusing to produce a result once {@code toolUses} reaches {@code maxTradeUses}, so a player
     * emptying a full stack into a trade with five uses remaining gets five trades, not however many
     * the stack would have paid for. Experience has to follow the same ceiling, or bulk trading
     * would credit trades that never happened.
     *
     * <p>Vanilla normally stops the count at the cap on its own and this changes nothing. It matters
     * when something else has pushed the use count past the maximum - another mod granting extra
     * trades, or a restock landing mid-sequence - where taking the raw difference would hand out
     * experience for trades beyond what the villager allowed.
     */
    private static int countedTrades(MerchantRecipe recipe, int lastSeen, int delta) {
        int remaining = recipe.getMaxTradeUses() - lastSeen;
        return Math.max(0, Math.min(delta, remaining));
    }

    /**
     * Sends the villager's updated experience to whoever is trading with it.
     *
     * <p>Without this the progress bar only ever reflects what the villager was worth at the moment
     * the screen opened. Completing a trade moved the real total on the server, but the client was
     * never told, so the green fill sat still and the white preview stayed where it was until the
     * screen was closed and reopened.
     *
     * <p>Only sent when the total actually changed and only to the one player looking at it, so the
     * cost is a handful of bytes per completed trade rather than anything periodic.
     */
    private static void pushToCustomer(EntityVillager villager, MerchantRecipeList recipes) {
        if (!ModConfig.display.showVillagerLevel) {
            return;
        }

        EntityPlayer customer = villager.getCustomer();
        if (customer instanceof EntityPlayerMP) {
            NetworkHandler.sendVillagerData(
                    (EntityPlayerMP) customer,
                    VillagerTradeData.getXp(villager),
                    Math.max(VillagerLevel.MIN_LEVEL, VillagerTradeData.getAppliedLevel(villager)),
                    TradeXp.forList(villager, recipes),
                    TradeXp.usesList(recipes),
                    PriceEngine.baseList(villager, recipes));
        }
    }

    /**
     * Rolls demand forward for every trade and re-baselines use tracking. Called as part of a
     * restock, mirroring 1.14 where {@code updateDemand()} runs on each offer inside
     * {@code restock()}.
     *
     * <p>Order matters: demand has to be computed from the use counts of the period that is ending,
     * so this must run <em>before</em> the caller resets those counts to zero.
     */
    public static void updateDemandAndReset(EntityVillager villager, MerchantRecipeList recipes) {
        for (MerchantRecipe recipe : recipes) {
            String key = TradeKey.of(villager, recipe);

            if (ModConfig.pricing.demandPricing) {
                int demand = VillagerTradeData.getDemand(villager, key);
                int updated = PriceEngine.updatedDemand(demand, recipe.getToolUses(), recipe.getMaxTradeUses());
                VillagerTradeData.setDemand(villager, key, updated);
            }

            // The caller is about to zero the recipe's uses, so our baseline must go to zero too.
            VillagerTradeData.setLastSeenUses(villager, key, 0);
        }
    }

    /** Re-baselines tracking for one trade without touching demand. */
    public static void resetTracking(EntityVillager villager, MerchantRecipe recipe) {
        VillagerTradeData.setLastSeenUses(villager, TradeKey.of(villager, recipe), recipe.getToolUses());
    }
}
