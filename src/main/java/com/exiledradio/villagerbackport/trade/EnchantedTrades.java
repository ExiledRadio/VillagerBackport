package com.exiledradio.villagerbackport.trade;

import com.exiledradio.villagerbackport.ModConfig;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentData;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemEnchantedBook;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.village.MerchantRecipe;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * The enchanted trades, priced as 26.1 prices them.
 *
 * <h2>26.1</h2>
 * 26.1 moved villager trades into data ({@code VillagerTrade}) and with them the enchanting cost: the
 * {@code enchant_randomly} and {@code enchant_with_levels} functions that enchant the item leave an
 * {@code additional_trade_cost} on it, which the trade adds to what it wants ({@code TradeCost.toItemCost}, clamped
 * to 0 to a stack), doubled when the book holds one of {@code #double_trade_price}. In 26.3's
 * {@code VillagerTrades}:
 * <ul>
 *   <li>The librarian's book ({@code enchantedBook}): wants 0 emeralds plus the cost, then a book; one enchantment of
 *       {@code #tradeable} (every non-treasure enchantment, both curses, frost walker and mending; not soul speed,
 *       swift sneak or wind burst) at a level from its minimum to its maximum; cost
 *       {@code 2 + random(5 + level * 10) + 3 * level}; doubled for {@code #treasure}. A book that rolls nothing is
 *       discarded ({@code discardItemIfItsNot}).</li>
 *   <li>Enchanted gear ({@code enchantedItem}): {@code enchant_with_levels} at 5 to 19 from {@code #on_traded_equipment}
 *       (the non-treasure enchantments), the cost being that level, added to the trade's emeralds. An item that comes
 *       out with no enchantment is discarded, so the villager draws another trade.</li>
 * </ul>
 * The numbers are 1.14's; what is 26.1's is the pool, the order of the costs (emeralds, then the book) and the
 * discarding. In 1.12.2's terms {@code #treasure} is {@link Enchantment#isTreasureEnchantment()} and
 * {@code #tradeable} is every enchantment allowed on books but the three 26.x keeps off them, by name in any mod.
 */
public final class EnchantedTrades {

    /** 26.3's {@code #treasure} members that are not {@code #tradeable}, by registry path. */
    private static final Set<String> UNTRADEABLE = new HashSet<String>(Arrays.asList("soul_speed", "swift_sneak", "wind_burst"));

    /** {@code TradeCost.toItemCost}'s ceiling: an emerald stack. */
    private static final int MAX_COST = 64;

    private EnchantedTrades() {
    }

    /** @return {@code #tradeable}: every registered enchantment a book may hold but soul speed, swift sneak and wind burst */
    public static List<Enchantment> tradeable() {
        List<Enchantment> out = new ArrayList<Enchantment>();
        for (Enchantment e : ForgeRegistries.ENCHANTMENTS.getValuesCollection()) {
            if (e.getRegistryName() != null && e.isAllowedOnBooks() && !UNTRADEABLE.contains(e.getRegistryName().getPath())) {
                out.add(e);
            }
        }
        return out;
    }

    /** @return 26.1's enchanting cost of a book holding the enchantment at the level, before the stack clamp */
    public static int bookCost(Enchantment enchantment, int level, Random random) {
        int cost = 2 + random.nextInt(5 + level * 10) + 3 * level;
        return enchantment.isTreasureEnchantment() ? cost * 2 : cost;
    }

    /** 26.3's librarian enchanted book trade: emeralds and a book for a book of one tradeable enchantment. */
    @Nullable
    public static MerchantRecipe book(int stock, Random random) {
        List<Enchantment> pool = tradeable();
        if (pool.isEmpty()) {
            return null;
        }
        Enchantment enchantment = pool.get(random.nextInt(pool.size()));
        int level = MathHelper.getInt(random, enchantment.getMinLevel(), enchantment.getMaxLevel());
        ItemStack sold = ItemEnchantedBook.getEnchantedItemStack(new EnchantmentData(enchantment, level));
        int cost = MathHelper.clamp(bookCost(enchantment, level, random), 0, MAX_COST);
        if (cost < 1) {
            return null;
        }
        return new MerchantRecipe(new ItemStack(Items.EMERALD, cost), new ItemStack(Items.BOOK), sold, 0, stock);
    }

    /**
     * {@code EnchantedItemForEmeraldsTrade} (1.14) and 26.3's {@code enchantedItem}: the item enchanted at a level of 5
     * to 19, priced at the base plus that level, at most a stack.
     *
     * @return the trade, or null where trades.modernEnchantedTrades is on and the item rolled no enchantment
     */
    @Nullable
    public static MerchantRecipe item(Item item, int basePrice, int stock, Random random) {
        int level = 5 + random.nextInt(15);
        ItemStack result = EnchantmentHelper.addRandomEnchantment(random, new ItemStack(item), level, false);
        if (ModConfig.trades.modernEnchantedTrades && !result.isItemEnchanted()) {
            return null;
        }
        return new MerchantRecipe(new ItemStack(Items.EMERALD, Math.min(basePrice + level, MAX_COST)), ItemStack.EMPTY,
                result, 0, stock);
    }
}
