package com.exiledradio.villagerbackport.trade;

import com.exiledradio.villagerbackport.ModConfig;

import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.init.Items;
import net.minecraft.village.MerchantRecipe;
import net.minecraft.village.MerchantRecipeList;

/**
 * How much experience a trade is worth, using 1.14's values.
 *
 * <h2>The two tables</h2>
 * 1.14 sets experience per trade, and reading the values out of {@code VillagerTrades} shows they
 * follow a strict pattern on two axes - the tier the trade unlocks at, and which way it goes:
 *
 * <table>
 *   <tr><th>Tier</th><th>Selling to the villager</th><th>Buying from the villager</th></tr>
 *   <tr><td>1</td><td>2</td><td>1</td></tr>
 *   <tr><td>2</td><td>10</td><td>5</td></tr>
 *   <tr><td>3</td><td>20</td><td>10</td></tr>
 *   <tr><td>4</td><td>30</td><td>15</td></tr>
 *   <tr><td>5</td><td>30</td><td>30</td></tr>
 * </table>
 *
 * <p>These are per-trade amounts, and 1.14 calibrated them against 1.14's stock levels - 16 uses for
 * a trade selling to a villager, 12 for one buying from it. 1.12.2 defaults to 7, which would pay
 * well under half the experience per restock. {@link TradeStock} closes that gap by raising the
 * stock rather than inflating what each trade pays here.
 *
 * <p>Trades where the villager hands over emeralds - {@code EmeraldForItemsTrade}, the player
 * selling raw materials - are worth roughly double the ones where the player spends emeralds.
 *
 * <p>This matters more than it first looks. The level thresholds are 10, 70, 150 and 250, so each
 * level costs progressively more, and a flat experience value per trade would make later levels
 * crawl. Scaling the reward with the tier is what keeps the progress bar moving at a similar rate
 * throughout - and it is why the white "this trade is worth this much" marker on the bar visibly
 * changes width as a villager levels up.
 */
public final class TradeXp {

    /** Player gives goods, villager gives emeralds. */
    private static final int[] SELLING = {2, 10, 20, 30, 30};

    /** Player gives emeralds, villager gives goods. */
    private static final int[] BUYING = {1, 5, 10, 15, 30};

    private TradeXp() {
    }

    /**
     * @return experience per trade, in trade-list order, for the client's progress bar.
     *
     * <p>Order matters: the client holds the same list and indexes into this by the trade it has
     * selected, so the two have to line up.
     */
    public static int[] forList(EntityVillager villager, MerchantRecipeList recipes) {
        if (recipes == null) {
            return new int[0];
        }

        int[] values = new int[recipes.size()];
        for (int i = 0; i < recipes.size(); i++) {
            values[i] = forTrade(recipes.get(i), TradeTier.forIndex(villager, i));
        }
        return values;
    }

    /** @return each trade's current use count, in trade-list order, for the client's lock display. */
    public static int[] usesList(MerchantRecipeList recipes) {
        if (recipes == null) {
            return new int[0];
        }

        int[] values = new int[recipes.size()];
        for (int i = 0; i < recipes.size(); i++) {
            values[i] = recipes.get(i).getToolUses();
        }
        return values;
    }

    /**
     * @param recipe the trade
     * @param tier   the tier the trade unlocked at, 1 to 5
     * @return experience granted for completing it once
     */
    public static int forTrade(MerchantRecipe recipe, int tier) {
        if (!recipe.getRewardsExp()) {
            return 0;
        }

        int index = Math.max(1, Math.min(VillagerLevel.MAX_LEVEL, tier)) - 1;

        // The player paying emeralds means they are buying from the villager. This is the same test
        // vanilla uses in EntityVillager.useRecipe to decide whether the villager gains wealth.
        int[] table = recipe.getItemToBuy().getItem() == Items.EMERALD ? BUYING : SELLING;

        int base = table[index];
        return Math.max(0, Math.round((float) base * (float) ModConfig.pricing.xpMultiplier));
    }
}
