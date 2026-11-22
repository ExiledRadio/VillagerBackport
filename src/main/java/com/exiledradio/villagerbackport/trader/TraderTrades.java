package com.exiledradio.villagerbackport.trader;

import com.exiledradio.backportcore.trade.TradeRegistry;
import com.exiledradio.villagerbackport.trade.TradeVersion;

import net.minecraft.block.BlockDirt;
import net.minecraft.block.BlockFlower;
import net.minecraft.block.BlockPlanks;
import net.minecraft.block.BlockTallGrass;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.PotionTypes;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionType;
import net.minecraft.potion.PotionUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraft.village.MerchantRecipe;
import net.minecraft.village.MerchantRecipeList;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The wandering trader's trade table, current Java's, in 1.12.2's items.
 *
 * <h2>Source</h2>
 * minecraft.wiki's "Wandering Trader" page for current Java, which reworked the table after 1.14:
 * every trader now has nine trades, two from a pool of things it <em>buys</em> (hay, water, baked
 * potatoes, milk), two from a pool of "special" goods (packed and blue ice, logs, podzol, gunpowder, an
 * invisibility potion, an enchanted iron pickaxe) and five from the long list of ordinary goods -
 * flowers, seeds, saplings, dyes, coral, sand and the like. Each trade is picked with equal chance
 * within its pool and no trade is picked twice. 1.14.4 itself had only the last two pools (five
 * ordinary, one special), which is why its traders never bought anything.
 *
 * <p>Stock is the wiki's per-trade figure and never comes back: a trader does not restock, and once
 * a trade is used up it stays crossed out until the trader leaves.
 *
 * <h2>Items 1.12.2 lacks</h2>
 * The same rule {@code ModernTrades} uses for villagers. An item 1.12.2 has under another name is
 * used directly (fern is tall grass, podzol is dirt, the tulips are one flower's metadata, white dye is
 * bone meal unless something adds a real one). An item with no 1.12.2 equivalent - blue ice, kelp,
 * sea pickles, coral blocks, cornflowers, fish buckets, the nautilus shell - is looked up by name
 * from the mods that commonly add it, and drops out of its pool when none is installed, so the
 * trader picks from what is left. Sea pickles are Update Aquatic Backport's, the family mod they fit
 * best, and are sold only when it is installed. Items later versions added are sold by their owners' names.
 *
 * <h2>Versions</h2>
 * The table is the newest installed backport's ({@link TradeVersion}): 1.14's layout and prices up to 1.20, with
 * 1.17's cave blocks and 1.20's cherry sapling from those versions; 1.21's rework (the buying pool, two specials,
 * logs, new prices) from Tricky Trials on; 26.x's additions with Game Drops.
 */
public final class TraderTrades {

    /** How many trades each pool contributes, in the order the wiki lists them. */
    private static final int BUYING_PICKS = 2;
    private static final int SPECIAL_PICKS = 2;
    private static final int ORDINARY_PICKS = 5;

    private static final List<Trade> BUYING = new ArrayList<Trade>();
    private static final List<Trade> SPECIAL = new ArrayList<Trade>();
    private static final List<Trade> ORDINARY = new ArrayList<Trade>();

    static {
        build();
    }

    private TraderTrades() {
    }

    /** One entry in a pool. */
    interface Trade {

        /** @return false when the item this trade needs does not exist in this game */
        boolean available();

        /** @return the trade, or null if it cannot be made here */
        @Nullable
        MerchantRecipe offer(Random random);
    }

    /**
     * A fresh trader's trades: two bought, two special, five ordinary, in that order on screen.
     *
     * <p>Picking is 1.14's {@code addTrades}: distinct entries at random from the pool's available
     * ones, all of them when the pool is no bigger than the count.
     */
    public static MerchantRecipeList roll(Random random) {
        MerchantRecipeList list = new MerchantRecipeList();
        // 1.14 to 1.20: five ordinary and one special, nothing bought. 1.21 added the buying pool and a second special.
        boolean modern = TradeVersion.current().atLeast(TradeVersion.V1_21);
        if (modern) {
            pick(list, pool(BUYING, TradeRegistry.TraderPool.BUYING), BUYING_PICKS, random);
        }
        pick(list, pool(SPECIAL, TradeRegistry.TraderPool.SPECIAL), modern ? SPECIAL_PICKS : 1, random);
        pick(list, pool(ORDINARY, TradeRegistry.TraderPool.ORDINARY), ORDINARY_PICKS, random);
        return list;
    }

    /** @return every trade the three pools can produce in this game, one example each - for listings */
    public static List<MerchantRecipe> examples(Random random) {
        List<MerchantRecipe> out = new ArrayList<MerchantRecipe>();
        for (List<Trade> pool : new List[] {pool(BUYING, TradeRegistry.TraderPool.BUYING),
                pool(SPECIAL, TradeRegistry.TraderPool.SPECIAL), pool(ORDINARY, TradeRegistry.TraderPool.ORDINARY)}) {
            for (Trade trade : pool) {
                if (trade.available()) {
                    MerchantRecipe recipe = trade.offer(random);
                    if (recipe != null) {
                        out.add(recipe);
                    }
                }
            }
        }
        return out;
    }

    /** @return this mod's pool, then whatever other mods added to it through Backport Core's TradeRegistry */
    private static List<Trade> pool(List<Trade> own, TradeRegistry.TraderPool which) {
        List<TradeRegistry.TraderTrade> added = TradeRegistry.traderTrades(which);
        if (added.isEmpty()) {
            return own;
        }
        List<Trade> pool = new ArrayList<Trade>(own);
        for (final TradeRegistry.TraderTrade trade : added) {
            pool.add(new Trade() {
                @Override
                public boolean available() {
                    return trade.available();
                }

                @Nullable
                @Override
                public MerchantRecipe offer(Random random) {
                    return trade.offer(random);
                }
            });
        }
        return pool;
    }

    private static void pick(MerchantRecipeList list, List<Trade> pool, int count, Random random) {
        List<Trade> open = new ArrayList<Trade>();
        for (Trade trade : pool) {
            if (trade.available()) {
                open.add(trade);
            }
        }
        int taken = 0;
        while (taken < count && !open.isEmpty()) {
            MerchantRecipe recipe = open.remove(random.nextInt(open.size())).offer(random);
            if (recipe != null) {
                list.add(recipe);
                taken++;
            }
        }
    }

    // ---------------------------------------------------------------------------------------------

    private static void build() {
        final TradeVersion v14 = TradeVersion.V1_14;
        final TradeVersion v17 = TradeVersion.V1_17;
        final TradeVersion v20 = TradeVersion.V1_20;
        final TradeVersion v21 = TradeVersion.V1_21;
        final TradeVersion v26 = TradeVersion.V26;

        // Buying (1.21): the trader pays one to three emeralds, two of each.
        BUYING.add(from(v21, buy(new ItemStack(Blocks.HAY_BLOCK), 1, 2)));
        BUYING.add(from(v21, buy(potion(PotionTypes.WATER), 1, 2)));
        BUYING.add(from(v21, buy(new ItemStack(Items.BAKED_POTATO, 4), 1, 2)));
        BUYING.add(from(v21, buy(new ItemStack(Items.MILK_BUCKET), 2, 2)));
        BUYING.add(from(v21, buy(new ItemStack(Items.WATER_BUCKET), 2, 2)));
        BUYING.add(from(v21, buy(new ItemStack(Items.FERMENTED_SPIDER_EYE), 3, 2)));

        // Special. 1.14 to 1.20: the fish buckets, packed and blue ice, gunpowder and podzol. 1.21: ice, gunpowder,
        // podzol, logs, the enchanted pickaxe and the long invisibility potion.
        String[] tropical = {"updateaquaticbackport:tropical_fish_bucket", "futuremc:tropical_fish_bucket"};
        String[] puffer = {"updateaquaticbackport:pufferfish_bucket", "futuremc:pufferfish_bucket"};
        String[] blueIce = {"updateaquaticbackport:blue_ice", "villagerbackport:blue_ice", "oe:blue_ice", "futuremc:blue_ice"};
        SPECIAL.add(between(v14, v21, optionalSell(5, 1, 4, tropical)));
        SPECIAL.add(between(v14, v21, optionalSell(5, 1, 4, puffer)));
        SPECIAL.add(between(v14, v21, sell(new ItemStack(Blocks.PACKED_ICE), 3, 6)));
        SPECIAL.add(from(v21, sell(new ItemStack(Blocks.PACKED_ICE), 1, 6)));
        SPECIAL.add(optionalSell(6, 1, 6, blueIce));
        SPECIAL.add(between(v14, v21, sell(new ItemStack(Items.GUNPOWDER), 1, 8)));
        SPECIAL.add(from(v21, sell(new ItemStack(Items.GUNPOWDER, 4), 1, 2)));
        SPECIAL.add(sell(new ItemStack(Blocks.DIRT, 3, BlockDirt.DirtType.PODZOL.getMetadata()), 3, 6));
        for (BlockPlanks.EnumType wood : BlockPlanks.EnumType.values()) {
            SPECIAL.add(from(v21, sell(log(wood, 8), 1, 4)));
        }
        // Later woods' logs by their owners' names (poplar is Game Drops' own, through Backport Core).
        SPECIAL.add(from(v21, optionalSell(1, 8, 4, "trailsandtalesbackport:cherry_log")));
        SPECIAL.add(from(v21, optionalSell(1, 8, 4, "wildupdatebackport:mangrove_log")));
        SPECIAL.add(from(v21, optionalSell(1, 8, 4, "trickytrialsbackport:pale_oak_log")));
        SPECIAL.add(from(v21, enchantedPickaxe()));
        SPECIAL.add(from(v21, sell(potion(PotionTypes.LONG_INVISIBILITY), 5, 1)));

        // Ordinary.
        ORDINARY.add(from(v21, optionalSell(3, 1, 4, tropical)));
        ORDINARY.add(from(v21, optionalSell(3, 1, 4, puffer)));
        // Sea pickles are Update Aquatic Backport's: the trader sells them only when it is installed.
        ORDINARY.add(optionalSell(2, 1, 5, "updateaquaticbackport:sea_pickle"));
        ORDINARY.add(sell(new ItemStack(Items.SLIME_BALL), 4, 5));
        ORDINARY.add(sell(new ItemStack(Blocks.GLOWSTONE), 2, 5));
        ORDINARY.add(optionalSell(5, 1, 5, "updateaquaticbackport:nautilus_shell", "oe:nautilus_shell", "futuremc:nautilus_shell"));
        ORDINARY.add(sell(new ItemStack(Blocks.TALLGRASS, 1, BlockTallGrass.EnumType.FERN.getMeta()), 1, 12));
        ORDINARY.add(sell(new ItemStack(Items.REEDS), 1, 8));
        ORDINARY.add(sell(new ItemStack(Blocks.PUMPKIN), 1, 4));
        ORDINARY.add(optionalSell(3, 1, 12, "updateaquaticbackport:kelp", "oe:kelp", "futuremc:kelp"));
        ORDINARY.add(sell(new ItemStack(Blocks.CACTUS), 3, 8));
        ORDINARY.add(sell(new ItemStack(Blocks.YELLOW_FLOWER), 1, 12));
        for (BlockFlower.EnumFlowerType flower : new BlockFlower.EnumFlowerType[] {
                BlockFlower.EnumFlowerType.POPPY, BlockFlower.EnumFlowerType.ALLIUM,
                BlockFlower.EnumFlowerType.HOUSTONIA, BlockFlower.EnumFlowerType.RED_TULIP,
                BlockFlower.EnumFlowerType.ORANGE_TULIP, BlockFlower.EnumFlowerType.WHITE_TULIP,
                BlockFlower.EnumFlowerType.PINK_TULIP, BlockFlower.EnumFlowerType.OXEYE_DAISY}) {
            ORDINARY.add(sell(new ItemStack(Blocks.RED_FLOWER, 1, flower.getMeta()), 1, 12));
        }
        ORDINARY.add(sell(new ItemStack(Blocks.RED_FLOWER, 1, BlockFlower.EnumFlowerType.BLUE_ORCHID.getMeta()), 1, 8));
        ORDINARY.add(optionalSell(1, 1, 12, "villagerbackport:cornflower", "futuremc:cornflower"));
        ORDINARY.add(optionalSell(1, 1, 7, "villagerbackport:lily_of_the_valley", "futuremc:lily_of_the_valley"));
        ORDINARY.add(from(v21, optionalSell(1, 1, 7, "trickytrialsbackport:open_eyeblossom")));
        ORDINARY.add(from(v21, optionalSell(1, 1, 12, "trickytrialsbackport:wildflowers")));
        ORDINARY.add(from(v21, optionalSell(1, 1, 12, "trickytrialsbackport:tall_dry_grass")));
        ORDINARY.add(sell(new ItemStack(Items.WHEAT_SEEDS), 1, 12));
        ORDINARY.add(sell(new ItemStack(Items.BEETROOT_SEEDS), 1, 12));
        ORDINARY.add(sell(new ItemStack(Items.PUMPKIN_SEEDS), 1, 12));
        ORDINARY.add(sell(new ItemStack(Items.MELON_SEEDS), 1, 12));
        for (BlockPlanks.EnumType wood : BlockPlanks.EnumType.values()) {
            ORDINARY.add(sell(new ItemStack(Blocks.SAPLING, 1, wood.getMetadata()), 5, 8));
        }
        // The mangrove propagule (1.19) is Wild Update's own, through Backport Core.
        ORDINARY.add(from(v20, optionalSell(5, 1, 8, "trailsandtalesbackport:cherry_sapling")));
        ORDINARY.add(from(v21, optionalSell(5, 1, 8, "trickytrialsbackport:pale_oak_sapling")));

        // Dyes, three for an emerald. The four 1.14 split off from other items are taken from a mod
        // that adds them when there is one, and are 1.12.2's bone meal, ink sac, cocoa beans and lapis
        // otherwise - as the villagers' tables do.
        for (EnumDyeColor color : EnumDyeColor.values()) {
            ORDINARY.add(dye(color));
        }

        // 1.13's five coral blocks: Update Aquatic Backport's, or Oceanic Expanse's, which names them by color (blue is
        // tube, pink is brain, purple is bubble, red is fire, yellow is horn).
        String[] corals = {"tube", "brain", "bubble", "fire", "horn"};
        String[] oeColors = {"blue", "pink", "purple", "red", "yellow"};
        for (int i = 0; i < corals.length; i++) {
            ORDINARY.add(optionalSell(3, 1, 8, "updateaquaticbackport:" + corals[i] + "_coral_block", "oe:" + oeColors[i] + "_coral_block"));
        }

        // 1.14 to 1.20 sold vines and mushrooms one at a time and lily pads in twos; 1.21 in threes and fives.
        ORDINARY.add(between(v14, v21, sell(new ItemStack(Blocks.VINE), 1, 12)));
        ORDINARY.add(from(v21, sell(new ItemStack(Blocks.VINE, 3), 1, 4)));
        ORDINARY.add(from(v21, optionalSell(1, 3, 4, "trickytrialsbackport:pale_hanging_moss")));
        ORDINARY.add(between(v14, v21, sell(new ItemStack(Blocks.BROWN_MUSHROOM), 1, 12)));
        ORDINARY.add(between(v14, v21, sell(new ItemStack(Blocks.RED_MUSHROOM), 1, 12)));
        ORDINARY.add(from(v21, sell(new ItemStack(Blocks.BROWN_MUSHROOM, 3), 1, 4)));
        ORDINARY.add(from(v21, sell(new ItemStack(Blocks.RED_MUSHROOM, 3), 1, 4)));
        ORDINARY.add(between(v14, v21, sell(new ItemStack(Blocks.WATERLILY, 2), 1, 5)));
        ORDINARY.add(from(v21, sell(new ItemStack(Blocks.WATERLILY, 5), 1, 2)));
        ORDINARY.add(sell(new ItemStack(Blocks.SAND, 8), 1, 8));
        ORDINARY.add(sell(new ItemStack(Blocks.SAND, 4, 1), 1, 6));

        // 1.17's cave blocks, Caves & Cliffs Backport's (its pointed dripstone it adds itself, through Backport Core).
        ORDINARY.add(from(v17, optionalSell(1, 2, 5, "cavesandcliffsbackport:small_dripleaf")));
        ORDINARY.add(from(v17, optionalSell(1, 2, 5, "cavesandcliffsbackport:rooted_dirt")));
        ORDINARY.add(from(v17, optionalSell(1, 2, 5, "cavesandcliffsbackport:moss_block")));
        ORDINARY.add(from(v21, optionalSell(1, 2, 5, "trickytrialsbackport:pale_moss_block")));
        ORDINARY.add(from(v21, optionalSell(3, 1, 12, "trickytrialsbackport:firefly_bush")));
        // 26.2's sulfur spikes. Game Drops adds the golden dandelion, name tag, poplar and shelf mushroom itself.
        ORDINARY.add(from(v26, optionalSell(1, 2, 5, "gamedropsbackport:sulfur_spike")));
    }

    /** The trade, from that version's table on. */
    private static Trade from(TradeVersion since, Trade trade) {
        return between(since, null, trade);
    }

    /** The trade in the tables of {@code since} up to, not including, {@code until} (null: every later one). */
    private static Trade between(final TradeVersion since, @Nullable final TradeVersion until, final Trade trade) {
        return new Trade() {
            @Override
            public boolean available() {
                TradeVersion now = TradeVersion.current();
                return now.atLeast(since) && (until == null || !now.atLeast(until)) && trade.available();
            }

            @Override
            public MerchantRecipe offer(Random random) {
                return trade.offer(random);
            }
        };
    }

    // ---------------------------------------------------------------------------------------------

    private static ItemStack potion(PotionType type) {
        return PotionUtils.addPotionToItemStack(new ItemStack(Items.POTIONITEM), type);
    }

    /** 1.12.2 keeps oak to jungle on one log block and acacia and dark oak on a second. */
    private static ItemStack log(BlockPlanks.EnumType wood, int count) {
        int meta = wood.getMetadata();
        return meta < 4 ? new ItemStack(Blocks.LOG, count, meta) : new ItemStack(Blocks.LOG2, count, meta - 4);
    }

    private static Trade dye(final EnumDyeColor color) {
        final String modern;
        switch (color) {
            case WHITE:
            case BLACK:
            case BROWN:
            case BLUE:
                modern = "villagerbackport:" + color.getName() + "_dye";
                break;
            default:
                modern = null;
        }
        return new Trade() {
            @Override
            public boolean available() {
                return true;
            }

            @Override
            public MerchantRecipe offer(Random random) {
                Item item = modern == null ? null : lookup(modern);
                ItemStack stack = item != null ? new ItemStack(item, 3)
                        : new ItemStack(Items.DYE, 3, color.getDyeDamage());
                return recipe(new ItemStack(Items.EMERALD), stack, 12);
            }
        };
    }

    /**
     * An iron pickaxe enchanted at level 5 to 19, for one emerald plus one per level - 1.14's {@code
     * EnchantedItemForEmeralds} with a base price of 1. One in stock.
     */
    private static Trade enchantedPickaxe() {
        return new Trade() {
            @Override
            public boolean available() {
                return true;
            }

            @Override
            public MerchantRecipe offer(Random random) {
                int level = 5 + random.nextInt(15);
                ItemStack pickaxe = EnchantmentHelper.addRandomEnchantment(random,
                        new ItemStack(Items.IRON_PICKAXE), level, false);
                return recipe(new ItemStack(Items.EMERALD, Math.min(1 + level, 64)), pickaxe, 1);
            }
        };
    }

    /** The trader pays {@code emeralds} for {@code wanted}. */
    private static Trade buy(final ItemStack wanted, final int emeralds, final int stock) {
        return new Trade() {
            @Override
            public boolean available() {
                return true;
            }

            @Override
            public MerchantRecipe offer(Random random) {
                return recipe(wanted.copy(), new ItemStack(Items.EMERALD, emeralds), stock);
            }
        };
    }

    /** The trader sells {@code sold} for {@code emeralds}. */
    private static Trade sell(final ItemStack sold, final int emeralds, final int stock) {
        return new Trade() {
            @Override
            public boolean available() {
                return true;
            }

            @Override
            public MerchantRecipe offer(Random random) {
                return recipe(new ItemStack(Items.EMERALD, emeralds), sold.copy(), stock);
            }
        };
    }

    /** A sale of an item looked up by name: the first of the candidates that exists, or no trade. */
    private static Trade optionalSell(final int emeralds, final int count, final int stock, final String... candidates) {
        return new Trade() {
            @Override
            public boolean available() {
                return item() != null;
            }

            @Override
            public MerchantRecipe offer(Random random) {
                Item item = item();
                return item == null ? null
                        : recipe(new ItemStack(Items.EMERALD, emeralds), new ItemStack(item, count), stock);
            }

            @Nullable
            private Item item() {
                for (String name : candidates) {
                    Item item = lookup(name);
                    if (item != null) {
                        return item;
                    }
                }
                return null;
            }
        };
    }

    @Nullable
    private static Item lookup(String name) {
        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(name));
        return item == null || item == Items.AIR ? null : item;
    }

    private static MerchantRecipe recipe(ItemStack cost, ItemStack result, int stock) {
        // uses 0, maxUses = stock. Nothing ever resets the uses: a trader does not restock.
        return new MerchantRecipe(cost, ItemStack.EMPTY, result, 0, stock);
    }
}
