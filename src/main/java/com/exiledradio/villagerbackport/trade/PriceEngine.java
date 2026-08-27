package com.exiledradio.villagerbackport.trade;

import com.exiledradio.villagerbackport.ModConfig;
import com.exiledradio.villagerbackport.data.VillagerTradeData;

import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.village.MerchantRecipe;
import net.minecraft.village.MerchantRecipeList;

/**
 * 1.14's supply-and-demand pricing, applied to 1.12.2 trades.
 *
 * <h2>The formula</h2>
 * 1.14 computes a trade's buy price in {@code MerchantOffer.getBuyingStack()} as
 * <pre>
 *   int i = baseCount;
 *   int j = Math.max(0, floor(i * demand * priceMultiplier));
 *   count = clamp(i + j + specialPrice, 1, maxStackSize);
 * </pre>
 * and updates demand on restock in {@code updateDemand()} as
 * <pre>
 *   demand = demand + uses - (maxUses - uses);
 * </pre>
 *
 * <p>The {@code Math.max(0, ...)} is easy to miss and important: demand can go negative for a trade
 * nobody uses, but it never discounts below the base price. Demand only ever makes things more
 * expensive or leaves them alone. Discounts in 1.14 come from {@code specialPrice}, which is driven
 * by gossip, curing a zombie villager, and Hero of the Village - a separate system, and a later
 * phase here.
 *
 * <h2>Why the price is written into the recipe</h2>
 * 1.12.2's {@code ContainerMerchant} and {@code InventoryMerchant} match the player's input against
 * {@code MerchantRecipe.getItemToBuy()} directly, and the client renders the same stack. There is no
 * hook between "the recipe" and "the price shown and charged", so a price that is not physically in
 * the recipe would neither display nor be enforced. We therefore rewrite the count on the buy stack
 * itself, computing it from a base count recorded in {@link VillagerTradeData} rather than from the
 * stack's current value.
 *
 * <h2>Why the surcharge is not left there</h2>
 * A villager's trades are saved into its NBT, so a surcharge sitting in the recipe becomes part of
 * the world save. That has two consequences worth avoiding: turning demand pricing off would leave
 * prices frozen wherever they last landed, and uninstalling the mod would leave them inflated
 * permanently with nothing left to undo them.
 *
 * <p>So the surcharge is only present while a player is actually trading - written on interact just
 * before the trade screen opens, and taken back off once the villager has no customer. At rest,
 * which is when a villager is saved, its recipes hold their base prices. Nothing is lost by not
 * persisting the surcharge: demand itself is stored separately, and the price is recomputed from it
 * the next time someone opens the screen.
 */
public final class PriceEngine {

    private PriceEngine() {
    }

    /**
     * @return each trade's base price, in trade-list order, for the client's price display
     *
     * <p>Zero for a trade whose base has never been recorded, which means nothing has priced it.
     *
     * <h2>Why the comparison is left to the client</h2>
     * This once sent the base only where it differed from what the recipe currently cost, which
     * seemed tidier and was wrong: the packet is sent as the player interacts, and the surcharge is
     * written into the recipe <em>after</em> that. So at the moment of sending, the price always
     * still equals the base, and the screen was told nothing had moved every single time it opened.
     * It only ever appeared to work after a trade, because the second packet goes out once the
     * surcharge is in place.
     *
     * <p>The client holds the priced recipe list and can compare for itself, which needs no
     * assumption about what has happened to the recipe by the time this runs.
     */
    public static int[] baseList(EntityVillager villager, MerchantRecipeList recipes) {
        if (recipes == null) {
            return new int[0];
        }

        int[] values = new int[recipes.size()];

        for (int i = 0; i < recipes.size(); i++) {
            MerchantRecipe recipe = recipes.get(i);
            int base = VillagerTradeData.getBaseCount(villager, TradeKey.of(villager, recipe));

            values[i] = Math.max(0, base);
        }

        return values;
    }

    /**
     * Records a trade's base price if it has not been seen before.
     *
     * <p>Deliberately called only when the villager has no customer, which is exactly when its
     * recipes are guaranteed to be holding base prices rather than a surcharge. Capturing the base
     * at that moment means a stray surcharge - left behind by, say, a world saved while a player had
     * the trade screen open - cannot be mistaken for the trade's real price and baked in permanently.
     */
    public static void recordBaseIfAbsent(EntityVillager villager, MerchantRecipe recipe, String key) {
        ItemStack buy = recipe.getItemToBuy();
        if (buy.isEmpty() || VillagerTradeData.getBaseCount(villager, key) >= 0) {
            return;
        }

        // Whatever it costs right now is its base price - including any adjustment another mod made
        // when it built the trade, which we want to preserve rather than normalise away.
        VillagerTradeData.setBaseCount(villager, key, buy.getCount());
    }

    /**
     * Applies the demand-adjusted price for a single trade.
     *
     * <p>Idempotent: running it twice with the same demand produces the same price, because the
     * calculation always starts from the recorded base count.
     */
    public static void applyPrice(EntityVillager villager, MerchantRecipe recipe, String key) {
        applyPrice(villager, recipe, key, 0);
    }

    /**
     * Applies the price this player in particular is charged.
     *
     * @param discount what the player's standing with the villager is worth, as a negative number -
     *                 1.14's {@code specialPrice}, the only term in the whole calculation that can
     *                 bring a price below its base
     */
    public static void applyPrice(EntityVillager villager, MerchantRecipe recipe, String key,
                                  int discount) {
        ItemStack buy = recipe.getItemToBuy();
        if (buy.isEmpty()) {
            return;
        }

        int base = VillagerTradeData.getBaseCount(villager, key);
        if (base < 0) {
            // Not yet recorded - the villager has never been polled at rest. Treat the current
            // price as the base rather than skipping, so a trade is never left unpriced.
            base = buy.getCount();
            VillagerTradeData.setBaseCount(villager, key, base);
        }

        int demand = VillagerTradeData.getDemand(villager, key);
        buy.setCount(priceFor(base, demand, discount, buy.getMaxStackSize(), multiplierFor(recipe)));
    }

    /**
     * Puts a trade back to its base price, undoing {@link #applyPrice}.
     *
     * <p>A no-op when no base has been recorded, which means we have never adjusted this trade and
     * there is nothing to undo.
     */
    public static void restoreBasePrice(EntityVillager villager, MerchantRecipe recipe, String key) {
        ItemStack buy = recipe.getItemToBuy();
        if (buy.isEmpty()) {
            return;
        }

        int base = VillagerTradeData.getBaseCount(villager, key);
        if (base >= 0 && buy.getCount() != base) {
            buy.setCount(base);
        }
    }

    /**
     * The 1.14 price calculation, minus {@code specialPrice} (no discount sources exist yet).
     *
     * @param base         the trade's original buy-stack count
     * @param demand       accumulated demand, may be negative
     * @param maxStackSize the buy item's stack limit, the hard ceiling on price
     */
    public static int priceFor(int base, int demand, int maxStackSize) {
        return priceFor(base, demand, 0, maxStackSize);
    }

    /**
     * @param discount 1.14's {@code specialPrice}, negative or zero
     *
     * <p>Added inside the clamp exactly as 1.14 has it, so a discount can take a price down to one
     * item but never to nothing.
     */
    public static int priceFor(int base, int demand, int discount, int maxStackSize) {
        return priceFor(base, demand, discount, maxStackSize, ModConfig.pricing.priceMultiplier);
    }

    /** @param multiplier how sharply demand moves this particular trade - see {@link #multiplierFor}. */
    public static int priceFor(int base, int demand, int discount, int maxStackSize, double multiplier) {
        int surcharge = Math.max(0, MathHelper.floor((float) base * demand * multiplier));
        return MathHelper.clamp(base + surcharge + discount, 1, maxStackSize);
    }

    /**
     * @return how sharply demand should move this trade's price
     *
     * <h2>1.14 has two of these, not one</h2>
     * Every offer carries its own multiplier, and vanilla only ever uses two values: 0.05 for the
     * ordinary trades - selling a villager your raw materials, buying its everyday stock - and 0.2
     * for the premium ones. Counting them in {@code VillagerTrades}: seven at 0.05 and thirty-five
     * at 0.2. Enchanted books, enchanted tools and armour, maps, bells and saddles are all 0.2.
     *
     * <p>Applying one figure to everything, as this used to, makes exactly the trades players care
     * about the least responsive: a librarian's enchanted book climbed at a quarter of the rate
     * 1.14 would have moved it.
     *
     * <p>1.12.2 recipes carry no such field, so the tier is inferred from what is being sold.
     * Enchanted output - which is every enchanted book and every enchanted tool trade - and maps are
     * the premium cases; everything else is ordinary.
     */
    public static double multiplierFor(MerchantRecipe recipe) {
        ItemStack sold = recipe.getItemToSell();

        if (!sold.isEmpty()) {
            if (sold.getItem() == Items.ENCHANTED_BOOK
                    || sold.getItem() == Items.FILLED_MAP
                    || sold.isItemEnchanted()) {
                return ModConfig.pricing.premiumPriceMultiplier;
            }
        }

        return ModConfig.pricing.priceMultiplier;
    }

    /**
     * 1.14's {@code updateDemand()}, run once per restock per trade.
     *
     * <p>Reads as: a trade used more than half its stock gains demand, a trade used less loses it.
     * A trade sold out completely ({@code uses == maxUses}) gains its full stock as demand, and a
     * trade untouched since the last restock loses the same. Over time this tracks what a village's
     * players actually buy.
     *
     * <p>Demand is clamped to a configured ceiling. 1.14 leaves it unbounded because the price is
     * clamped by stack size anyway, but an unbounded counter in saved data that only ever grows for
     * a popular trade is worth keeping in a sane range.
     *
     * @param uses    the trade's use count for the period now ending
     * @param maxUses the trade's stock per period
     */
    public static int updatedDemand(int demand, int uses, int maxUses) {
        int updated = demand + uses - (maxUses - uses);
        return MathHelper.clamp(updated, -ModConfig.pricing.maxDemand, ModConfig.pricing.maxDemand);
    }
}
