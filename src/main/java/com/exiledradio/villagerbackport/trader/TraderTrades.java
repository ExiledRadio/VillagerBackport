package com.exiledradio.villagerbackport.trader;

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
 * trader picks from what is left. Things only much later versions added (cherry and mangrove wood,
 * moss, dripleaf, dripstone, wildflowers, firefly bushes) are left out entirely.
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
        pick(list, BUYING, BUYING_PICKS, random);
        pick(list, SPECIAL, SPECIAL_PICKS, random);
        pick(list, ORDINARY, ORDINARY_PICKS, random);
        return list;
    }

    /** @return every trade the three pools can produce in this game, one example each - for listings */
    public static List<MerchantRecipe> examples(Random random) {
        List<MerchantRecipe> out = new ArrayList<MerchantRecipe>();
        for (List<Trade> pool : new List[] {BUYING, SPECIAL, ORDINARY}) {
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
        // Buying: the trader pays one to three emeralds, two of each.
        BUYING.add(buy(new ItemStack(Blocks.HAY_BLOCK), 1, 2));
        BUYING.add(buy(potion(PotionTypes.WATER), 1, 2));
        BUYING.add(buy(new ItemStack(Items.BAKED_POTATO, 4), 1, 2));
        BUYING.add(buy(new ItemStack(Items.MILK_BUCKET), 2, 2));
        BUYING.add(buy(new ItemStack(Items.WATER_BUCKET), 2, 2));
        BUYING.add(buy(new ItemStack(Items.FERMENTED_SPIDER_EYE), 3, 2));

        // Special.
        SPECIAL.add(sell(new ItemStack(Blocks.PACKED_ICE), 1, 6));
        SPECIAL.add(sell(new ItemStack(Items.GUNPOWDER, 4), 1, 2));
        for (BlockPlanks.EnumType wood : BlockPlanks.EnumType.values()) {
            SPECIAL.add(sell(log(wood, 8), 1, 4));
        }
        SPECIAL.add(sell(new ItemStack(Blocks.DIRT, 3, BlockDirt.DirtType.PODZOL.getMetadata()), 3, 6));
        SPECIAL.add(sell(potion(PotionTypes.INVISIBILITY), 5, 1));
        SPECIAL.add(optionalSell(6, 1, 6, "oe:blue_ice", "futuremc:blue_ice"));
        SPECIAL.add(enchantedPickaxe());

        // Ordinary: one emerald for one, twelve in stock.
        ORDINARY.add(sell(new ItemStack(Blocks.TALLGRASS, 1, BlockTallGrass.EnumType.FERN.getMeta()), 1, 12));
        ORDINARY.add(sell(new ItemStack(Blocks.YELLOW_FLOWER), 1, 12));
        for (BlockFlower.EnumFlowerType flower : new BlockFlower.EnumFlowerType[] {
                BlockFlower.EnumFlowerType.POPPY, BlockFlower.EnumFlowerType.ALLIUM,
                BlockFlower.EnumFlowerType.HOUSTONIA, BlockFlower.EnumFlowerType.RED_TULIP,
                BlockFlower.EnumFlowerType.ORANGE_TULIP, BlockFlower.EnumFlowerType.WHITE_TULIP,
                BlockFlower.EnumFlowerType.PINK_TULIP, BlockFlower.EnumFlowerType.OXEYE_DAISY}) {
            ORDINARY.add(sell(new ItemStack(Blocks.RED_FLOWER, 1, flower.getMeta()), 1, 12));
        }
        ORDINARY.add(optionalSell(1, 1, 12, "villagerbackport:cornflower", "futuremc:cornflower"));
        ORDINARY.add(sell(new ItemStack(Items.WHEAT_SEEDS), 1, 12));
        ORDINARY.add(sell(new ItemStack(Items.BEETROOT_SEEDS), 1, 12));
        ORDINARY.add(sell(new ItemStack(Items.PUMPKIN_SEEDS), 1, 12));
        ORDINARY.add(sell(new ItemStack(Items.MELON_SEEDS), 1, 12));

        ORDINARY.add(sell(new ItemStack(Items.REEDS), 1, 8));
        ORDINARY.add(sell(new ItemStack(Blocks.RED_FLOWER, 1, BlockFlower.EnumFlowerType.BLUE_ORCHID.getMeta()), 1, 8));
        ORDINARY.add(optionalSell(1, 1, 7, "villagerbackport:lily_of_the_valley", "futuremc:lily_of_the_valley"));
        ORDINARY.add(sell(new ItemStack(Blocks.PUMPKIN), 1, 4));
        ORDINARY.add(sell(new ItemStack(Items.NAME_TAG), 1, 5));

        // Dyes, three for an emerald. The four 1.14 split off from other items are taken from a mod
        // that adds them when there is one, and are 1.12.2's bone meal, ink sac, cocoa beans and lapis
        // otherwise - as the villagers' tables do.
        for (EnumDyeColor color : EnumDyeColor.values()) {
            ORDINARY.add(dye(color));
        }

        ORDINARY.add(sell(new ItemStack(Blocks.VINE, 3), 1, 4));
        ORDINARY.add(sell(new ItemStack(Blocks.BROWN_MUSHROOM, 3), 1, 4));
        ORDINARY.add(sell(new ItemStack(Blocks.RED_MUSHROOM, 3), 1, 4));
        ORDINARY.add(sell(new ItemStack(Blocks.WATERLILY, 5), 1, 2));
        ORDINARY.add(sell(new ItemStack(Blocks.SAND, 8), 1, 8));
        ORDINARY.add(sell(new ItemStack(Blocks.SAND, 4, 1), 1, 6));

        ORDINARY.add(optionalSell(2, 1, 5, "oe:sea_pickle", "futuremc:sea_pickle"));
        ORDINARY.add(sell(new ItemStack(Blocks.GLOWSTONE), 3, 5));
        ORDINARY.add(optionalSell(3, 1, 4, "futuremc:tropical_fish_bucket"));
        ORDINARY.add(optionalSell(3, 1, 4, "futuremc:pufferfish_bucket"));
        ORDINARY.add(optionalSell(3, 1, 12, "oe:kelp", "futuremc:kelp"));
        ORDINARY.add(sell(new ItemStack(Blocks.CACTUS), 3, 8));
        // 1.13's five coral blocks; Oceanic Expanse names them by color (blue is tube, pink is brain,
        // purple is bubble, red is fire, yellow is horn).
        for (String coral : new String[] {"blue", "pink", "purple", "red", "yellow"}) {
            ORDINARY.add(optionalSell(3, 1, 8, "oe:" + coral + "_coral_block"));
        }

        ORDINARY.add(sell(new ItemStack(Items.SLIME_BALL), 5, 5));
        ORDINARY.add(optionalSell(5, 1, 5, "oe:nautilus_shell", "futuremc:nautilus_shell"));
        for (BlockPlanks.EnumType wood : BlockPlanks.EnumType.values()) {
            ORDINARY.add(sell(new ItemStack(Blocks.SAPLING, 1, wood.getMetadata()), 5, 8));
        }
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
