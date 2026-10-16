package com.exiledradio.villagerbackport.trade;

import com.exiledradio.villagerbackport.ModConfig;
import com.exiledradio.villagerbackport.data.VillagerTradeData;

import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.village.MerchantRecipe;
import net.minecraft.village.MerchantRecipeList;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

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
            int base = storedBase(villager, recipe, TradeKey.of(villager, recipe));

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
        ItemStack buy = pricedStack(recipe);
        if (buy.isEmpty() || storedBase(villager, recipe, key) >= 0) {
            return;
        }

        // Whatever it costs right now is its base price - including any adjustment another mod made
        // when it built the trade, which we want to preserve rather than normalise away.
        storeBase(villager, recipe, key, buy.getCount());
        startDemand(villager, recipe, key);
    }

    /**
     * Gives a trade seen for the first time the game's starting demand: minus twice its stock.
     *
     * <p>So a new trade has to sell out on several days running before its price first goes up,
     * rather than after the first. Each sold-out restock adds one stock's worth of demand and each
     * untouched one takes one away, so a trade with 12 in stock starts 24 below the point where
     * prices begin to rise.
     *
     * <p>Only ever for a trade with no demand recorded. One already in a world keeps what it has.
     */
    private static void startDemand(EntityVillager villager, MerchantRecipe recipe, String key) {
        if (!VillagerTradeData.hasDemand(villager, key)) {
            VillagerTradeData.setDemand(villager, key, -2 * recipe.getMaxTradeUses());
        }
    }

    /**
     * @return the stack whose count is this trade's price
     *
     * <h2>Usually the first, and when it is not</h2>
     * 1.14 only ever adjusts an offer's first cost, and every 1.14 offer with two costs puts the
     * emeralds first: an enchanted book is {@code emeralds + book}. 1.12.2 builds the same trade
     * the other way round, {@code book + emeralds}, so pricing "the first stack" here charged a
     * player who had upset a librarian eight books instead of more emeralds.
     *
     * <p>So where the emeralds are the second cost and not the first, the emeralds are the price.
     * Every other trade is priced on its first stack, as before.
     */
    public static ItemStack pricedStack(MerchantRecipe recipe) {
        return pricesSecond(recipe) ? recipe.getSecondItemToBuy() : recipe.getItemToBuy();
    }

    /** @return the cost stack that is not the price, which may be empty */
    public static ItemStack unpricedStack(MerchantRecipe recipe) {
        return pricesSecond(recipe) ? recipe.getItemToBuy() : recipe.getSecondItemToBuy();
    }

    private static boolean pricesSecond(MerchantRecipe recipe) {
        ItemStack second = recipe.getSecondItemToBuy();

        return !second.isEmpty()
                && second.getItem() == Items.EMERALD
                && recipe.getItemToBuy().getItem() != Items.EMERALD;
    }

    /**
     * @return the recorded base count of the stack {@link #pricedStack} names, or -1
     *
     * <p>Also where villagers saved before second-stack pricing are put right. For such a trade the
     * figure under the old name is the base of its <em>first</em> stack - the book count - so it is
     * written back onto that stack, undoing any surcharge still sitting there, and then dropped.
     */
    private static int storedBase(EntityVillager villager, MerchantRecipe recipe, String key) {
        if (!pricesSecond(recipe)) {
            return VillagerTradeData.getBaseCount(villager, key);
        }

        int stale = VillagerTradeData.getBaseCount(villager, key);
        if (stale >= 0) {
            ItemStack first = recipe.getItemToBuy();
            first.setCount(MathHelper.clamp(stale, 1, first.getMaxStackSize()));
            VillagerTradeData.clearBaseCount(villager, key);
        }

        return VillagerTradeData.getSecondBaseCount(villager, key);
    }

    private static void storeBase(EntityVillager villager, MerchantRecipe recipe, String key, int base) {
        if (pricesSecond(recipe)) {
            VillagerTradeData.setSecondBaseCount(villager, key, base);
        } else {
            VillagerTradeData.setBaseCount(villager, key, base);
        }
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
        ItemStack buy = pricedStack(recipe);
        if (buy.isEmpty()) {
            return;
        }

        int base = storedBase(villager, recipe, key);
        if (base < 0) {
            // Not yet recorded - the villager has never been polled at rest. Treat the current
            // price as the base rather than skipping, so a trade is never left unpriced.
            base = buy.getCount();
            storeBase(villager, recipe, key, base);
            startDemand(villager, recipe, key);
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
        ItemStack buy = pricedStack(recipe);
        if (buy.isEmpty()) {
            return;
        }

        int base = storedBase(villager, recipe, key);
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
     * @return how sharply demand and reputation should move this trade's price
     *
     * <h2>1.14 has two of these, not one</h2>
     * Every offer carries its own multiplier, and vanilla only ever uses two values: 0.05 for the
     * ordinary trades and 0.2 for the premium ones. Applying one figure to everything, as this once
     * did, made exactly the trades players care about the least responsive.
     *
     * <h2>Which trades are premium</h2>
     * 1.12.2 recipes carry no such field, so the tier is looked up from what is being sold, against
     * which of 1.14's trades pass 0.2:
     * <ul>
     *   <li>every enchanted book, and every explorer map;</li>
     *   <li>enchanted gear, with two exceptions 1.14 builds through the constructor that defaults
     *       to 0.05 - the weaponsmith's enchanted iron sword and the fletcher's enchanted bow;</li>
     *   <li>and some gear sold plain: iron and chainmail armour, shields, the iron axe, the stone
     *       tools, the diamond hoe, saddles and leather armour.</li>
     * </ul>
     * Everything else is ordinary, including every trade the player sells into - those all pay
     * emeralds, and no 1.14 emerald-paying trade is premium.
     */
    public static double multiplierFor(MerchantRecipe recipe) {
        return isPremium(recipe.getItemToSell())
                ? ModConfig.pricing.premiumPriceMultiplier
                : ModConfig.pricing.priceMultiplier;
    }

    private static boolean isPremium(ItemStack sold) {
        if (sold.isEmpty()) {
            return false;
        }

        Item item = sold.getItem();

        if (item == Items.ENCHANTED_BOOK || item == Items.FILLED_MAP) {
            return true;
        }

        if (sold.isItemEnchanted()) {
            return item != Items.IRON_SWORD && item != Items.BOW;
        }

        return PREMIUM_PLAIN.contains(item);
    }

    /** Unenchanted items 1.14 sells at 0.2. Its bell is one too, but 1.12.2 has no bell to sell. */
    private static final Set<Item> PREMIUM_PLAIN = new HashSet<Item>(Arrays.asList(
            Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS,
            Items.CHAINMAIL_HELMET, Items.CHAINMAIL_CHESTPLATE, Items.CHAINMAIL_LEGGINGS,
            Items.CHAINMAIL_BOOTS, Items.SHIELD, Items.IRON_AXE, Items.STONE_AXE, Items.STONE_SHOVEL,
            Items.STONE_PICKAXE, Items.STONE_HOE, Items.DIAMOND_HOE, Items.SADDLE,
            Items.LEATHER_HELMET, Items.LEATHER_CHESTPLATE, Items.LEATHER_LEGGINGS,
            Items.LEATHER_BOOTS));

    /** Where demand stops when no ceiling is configured. Far past anything a stack-size price needs. */
    private static final int SAFETY_LIMIT = 1000000;

    /**
     * 1.14's {@code updateDemand()}, run once per restock per trade.
     *
     * <p>Reads as: a trade used more than half its stock gains demand, a trade used less loses it.
     * A trade sold out completely ({@code uses == maxUses}) gains its full stock as demand, and a
     * trade untouched since the last restock loses the same. Over time this tracks what a village's
     * players actually buy.
     *
     * <p>Unbounded by default, as 1.14 has it: the price is clamped by stack size, and that is the
     * only ceiling 1.14 has. A configured ceiling, if set, bounds both directions. The counter
     * itself cannot realistically overflow - it moves by at most one stock per restock - but it is
     * still held inside a million either way, so a corrupted save cannot wrap it round.
     *
     * @param uses    the trade's use count for the period now ending
     * @param maxUses the trade's stock per period
     */
    public static int updatedDemand(int demand, int uses, int maxUses) {
        int updated = demand + uses - (maxUses - uses);
        int ceiling = ModConfig.pricing.maxDemand > 0 ? ModConfig.pricing.maxDemand : SAFETY_LIMIT;
        return MathHelper.clamp(updated, -ceiling, ceiling);
    }
}
