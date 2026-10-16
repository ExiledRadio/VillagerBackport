package com.exiledradio.villagerbackport.trade;

import com.exiledradio.villagerbackport.ModConfig;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.village.MerchantRecipe;

import java.util.HashMap;
import java.util.Map;

/**
 * Sets how many times a trade can be used to the stock 1.14 gives the same trade.
 *
 * <h2>Why</h2>
 * 1.12.2's {@link MerchantRecipe} defaults to <b>7</b> uses - the three-argument constructor passes
 * {@code maxTradeUses = 7}, and almost every vanilla trade takes that default. 1.14 gives every trade
 * its own figure in {@code VillagerTrades}, and they are mostly far more generous.
 *
 * <p>That gap matters once experience is in play. The experience values and the level thresholds are
 * both 1.14's, and 1.14 calibrated them against 1.14's stock. Paying those amounts over less than
 * half the trades means a villager earns well under half the experience per restock, and with
 * restocking capped at twice a day a villager can sell out with the next level still out of reach.
 *
 * <h2>Per trade, from 1.14's table</h2>
 * The figure is not one number per direction. Of the trades a villager buys from the player, the
 * everyday ones - crops, wool, string, meat, paper - take 16, while the valuable ones - iron, gold,
 * diamonds, books, flint - take 12. Of the trades it sells, enchanted tools, weapons and armour take
 * only 3, a diamond hoe 3, and a handful of food and building blocks 16; everything else is 12. Those
 * figures are copied here keyed by item, and a trade found in the table is set to exactly that,
 * downwards as well as up - an enchanted diamond sword with twelve in stock is not 1.14's sword.
 *
 * <h2>Trades 1.14 has no figure for</h2>
 * Another mod's trades, and the odd 1.12.2 trade with no 1.14 equivalent, fall back to the
 * configured per-direction figure and are only ever raised to it. A mod that deliberately made a
 * trade scarce has made a decision worth respecting, and there is no version of "matching 1.14" that
 * justifies overriding a trade 1.14 never had.
 */
public final class TradeStock {

    /** 1.14's {@code EnchantedItemForEmeraldsTrade} stock, for every enchanted tool, weapon and armour. */
    private static final int ENCHANTED_GEAR = 3;

    /** What the villager takes, for trades where the player sells to it. */
    private static final Map<Item, Integer> SELLING = new HashMap<Item, Integer>();

    /** What the villager hands over, for trades where the player buys from it. */
    private static final Map<Item, Integer> BUYING = new HashMap<Item, Integer>();

    static {
        // EmeraldForItemsTrade at 16.
        sells(16, Items.WHEAT, Items.POTATO, Items.CARROT, Items.BEETROOT, Items.STRING, Items.COAL,
                Items.STICK, Items.FEATHER, Items.PAPER, Items.ROTTEN_FLESH, Items.CHICKEN,
                Items.PORKCHOP, Items.RABBIT, Items.MUTTON, Items.BEEF, Items.LEATHER, Items.CLAY_BALL);
        sells(16, Blocks.WOOL, Blocks.GLASS_PANE, Blocks.STONE);

        // EmeraldForItemsTrade at 12.
        sells(12, Items.FLINT, Items.BOOK, Items.WRITABLE_BOOK, Items.COMPASS, Items.GOLD_INGOT,
                Items.RABBIT_FOOT, Items.GLASS_BOTTLE, Items.NETHER_WART, Items.IRON_INGOT,
                Items.LAVA_BUCKET, Items.DIAMOND, Items.RABBIT_HIDE, Items.QUARTZ);
        sells(12, Blocks.PUMPKIN, Blocks.MELON_BLOCK, Blocks.TRIPWIRE_HOOK);

        // ItemsForEmeraldsTrade at 16. Everything not listed is 12, the constructor's default.
        buys(16, Items.BREAD, Items.APPLE, Items.COOKED_FISH, Items.COOKED_PORKCHOP,
                Items.COOKED_CHICKEN, Items.BRICK);
        buys(16, Blocks.WOOL, Blocks.CARPET, Blocks.STONEBRICK, Blocks.STONE);

        // The one unenchanted trade 1.14 limits to 3.
        buys(ENCHANTED_GEAR, Items.DIAMOND_HOE);
    }

    private TradeStock() {
    }

    /**
     * Brings one trade to its stock level.
     *
     * <p>Idempotent, so safe to call every poll. {@code increaseMaxTradeUses} is the only way 1.12.2
     * offers to change the figure - there is no setter - so the change is computed from the gap.
     * Vanilla's own reset raises the figure a little whenever it fires; re-applying here every poll
     * is what holds a 1.14 trade at 1.14's number regardless.
     */
    public static void normalise(MerchantRecipe recipe) {
        boolean playerBuying = PriceEngine.pricedStack(recipe).getItem() == Items.EMERALD;
        int fallback = playerBuying ? ModConfig.pricing.buyingTradeStock : ModConfig.pricing.sellingTradeStock;

        // Zero switches stock handling off for that direction entirely, table and all.
        if (fallback <= 0) {
            return;
        }

        int current = recipe.getMaxTradeUses();
        int known = fromTable(recipe, playerBuying);

        if (known > 0) {
            if (current != known) {
                recipe.increaseMaxTradeUses(known - current);
            }
        } else if (current < fallback) {
            recipe.increaseMaxTradeUses(fallback - current);
        }
    }

    /** @return 1.14's stock for this trade, or 0 if 1.14 has no matching trade */
    private static int fromTable(MerchantRecipe recipe, boolean playerBuying) {
        if (playerBuying) {
            ItemStack sold = recipe.getItemToSell();

            // EnchantedItemForEmeraldsTrade. Enchanted books are their own trade, at 12.
            if (sold.isItemEnchanted() && sold.getItem() != Items.ENCHANTED_BOOK) {
                return ENCHANTED_GEAR;
            }

            Integer stock = BUYING.get(sold.getItem());
            return stock == null ? 0 : stock.intValue();
        }

        ItemStack taken = recipe.getItemToBuy();

        // Two items share an id with metadata telling them apart, and 1.14 stocks them differently.
        if (taken.getItem() == Items.FISH) {
            // Cod and salmon 16; clownfish and pufferfish, 1.14's tropical fish and pufferfish, 12.
            return taken.getMetadata() <= 1 ? 16 : 12;
        }
        if (taken.getItem() == Items.DYE) {
            // Metadata 0 is the ink sac, 12 from a librarian. The other dyes are a shepherd's 16.
            return taken.getMetadata() == 0 ? 12 : 16;
        }

        Integer stock = SELLING.get(taken.getItem());
        return stock == null ? 0 : stock.intValue();
    }

    private static void sells(int stock, Item... items) {
        for (Item item : items) {
            SELLING.put(item, Integer.valueOf(stock));
        }
    }

    private static void sells(int stock, Block... blocks) {
        for (Block block : blocks) {
            SELLING.put(Item.getItemFromBlock(block), Integer.valueOf(stock));
        }
    }

    private static void buys(int stock, Item... items) {
        for (Item item : items) {
            BUYING.put(item, Integer.valueOf(stock));
        }
    }

    private static void buys(int stock, Block... blocks) {
        for (Block block : blocks) {
            BUYING.put(Item.getItemFromBlock(block), Integer.valueOf(stock));
        }
    }
}
