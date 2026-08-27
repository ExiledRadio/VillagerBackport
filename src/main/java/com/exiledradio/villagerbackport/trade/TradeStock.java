package com.exiledradio.villagerbackport.trade;

import com.exiledradio.villagerbackport.ModConfig;

import net.minecraft.init.Items;
import net.minecraft.village.MerchantRecipe;

/**
 * Raises how many times a trade can be used to 1.14's stock levels.
 *
 * <h2>Why</h2>
 * 1.12.2's {@link MerchantRecipe} defaults to <b>7</b> uses - the three-argument constructor passes
 * {@code maxTradeUses = 7}, and almost every vanilla trade takes that default. 1.14 is far more
 * generous: {@code EmeraldForItemsTrade} allows <b>16</b>, and {@code ItemsForEmeraldsTrade}
 * defaults to <b>12</b>.
 *
 * <p>That gap matters once experience is in play. The experience values and the level thresholds are
 * both 1.14's, and 1.14 calibrated them against 1.14's stock. Paying those amounts over less than
 * half the trades means a villager earns well under half the experience per restock, and with
 * restocking capped at twice a day a villager can sell out with the next level still out of reach.
 *
 * <p>Raising the stock fixes that at the source rather than inflating what each trade pays: the
 * per-trade experience stays exactly 1.14's, and the number of trades behind it does too.
 *
 * <h2>Only ever upward</h2>
 * A trade already allowing more than the target keeps what it has. Another mod that deliberately
 * made a trade scarce - a rare or powerful item meant to be limited - has made a decision worth
 * respecting, and there is no version of "matching 1.14" that justifies overriding it.
 */
public final class TradeStock {

    private TradeStock() {
    }

    /**
     * Brings one trade up to the configured stock level.
     *
     * <p>Idempotent. {@code increaseMaxTradeUses} is the only way 1.12.2 offers to change the
     * figure - there is no setter - so the increment is computed from the gap, and a trade already
     * at or above the target is left alone. That also makes it safe to call every poll.
     */
    public static void normalise(MerchantRecipe recipe) {
        int target = targetFor(recipe);
        if (target <= 0) {
            return;
        }

        int current = recipe.getMaxTradeUses();
        if (current < target) {
            recipe.increaseMaxTradeUses(target - current);
        }
    }

    /**
     * @return the stock this trade should allow, or 0 to leave it untouched.
     *
     * <p>The split follows 1.14's two trade shapes. The player paying emeralds means they are buying
     * from the villager, which is {@code ItemsForEmeraldsTrade} territory and allows 12. Otherwise
     * the villager is buying raw goods, which is {@code EmeraldForItemsTrade} and allows 16. Same
     * test used to pick the experience table.
     */
    private static int targetFor(MerchantRecipe recipe) {
        boolean playerBuying = recipe.getItemToBuy().getItem() == Items.EMERALD;
        return playerBuying ? ModConfig.pricing.buyingTradeStock : ModConfig.pricing.sellingTradeStock;
    }
}
