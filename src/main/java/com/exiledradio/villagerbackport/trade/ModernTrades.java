package com.exiledradio.villagerbackport.trade;

import com.exiledradio.backportcore.trade.TradeRegistry;

import net.minecraft.block.Block;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionType;
import net.minecraft.potion.PotionUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.village.MerchantRecipe;
import net.minecraft.village.MerchantRecipeList;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.storage.MapDecoration;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 1.14's trade tables, in 1.12.2's items.
 *
 * <h2>Source</h2>
 * {@code VillagerTrades} from the 1.14.4 sources, entry for entry and in the same order, with the
 * few later changes minecraft.wiki documents for current Java applied on top: the librarian's
 * bookshelf is 9 emeralds for one rather than 6 for three, an enchanted fishing rod is stocked 3
 * like every other piece of enchanted gear, and a master librarian sells candles rather than a name
 * tag when a mod that adds candles is installed (26.1), and no name tag at all with Game Drops Backport.
 * Enchanted books and gear are 26.1's with trades.modernEnchantedTrades ({@link EnchantedTrades}).
 *
 * <h2>Picking</h2>
 * Each level is a pool, and a villager reaching that level takes two different trades from it at
 * random - 1.14's {@code addTrades(offers, pool, 2)}. A pool of two or fewer is taken whole. That is
 * the whole reason two 1.14 farmers can sell different things, where 1.12.2's career lists give
 * every farmer the same trades in the same order.
 *
 * <h2>Items 1.12.2 does not have</h2>
 * A 1.14 item with a 1.12.2 equivalent uses it: cod is raw fish, white dye is bone meal, the stained
 * terracottas are one block's metadata. One with no equivalent at all - the crossbow, the campfire,
 * the lantern, sweet berries, the turtle scute - is looked up by name from the mods that commonly
 * add it, and drops out of its pool when none of them is installed, so the villager simply picks
 * from what is left. Nothing here needs another mod.
 *
 * <p>1.14's own white, black, brown and blue dyes, suspicious stew, leather horse armor and Globe
 * banner pattern come from this mod's flora package. With it switched off, a shepherd takes 1.12.2's
 * bone meal and the rest for those colors, the leatherworker's horse armor drops out of its pool,
 * and the cartographer sells one of this mod's other four patterns in place of the Globe.
 */
public final class ModernTrades {

    /** 1.14 takes this many trades from a level's pool. */
    private static final int TRADES_PER_LEVEL = 2;

    /** 1.14's stock for an ordinary trade, the {@code MerchantOffer} constructor's default. */
    private static final int STOCK = 12;

    /** Stock for enchanted tools, weapons and armour. */
    private static final int GEAR_STOCK = 3;

    /** The candles a master librarian sells, by name: Deeper Depths', then Caves &amp; Cliffs Backport's. */
    private static final String[] YELLOW_CANDLES = {"deeperdepths:yellow_candle", "cavesandcliffsbackport:yellow_candle"};
    private static final String[] RED_CANDLES = {"deeperdepths:red_candle", "cavesandcliffsbackport:red_candle"};

    /**
     * Present when Game Drops Backport's 26.1 name tag changes are (its babies stream registers the golden dandelion
     * with them): the master librarian then sells no name tag.
     */
    static final String[] GAME_DROPS_NAME_TAGS = {"gamedropsbackport:golden_dandelion"};

    /** Career key ("profession registry name/career name") to its five levels of pools. */
    private static final Map<String, Trade[][]> TABLES = new HashMap<String, Trade[][]>();

    static {
        build();
    }

    private ModernTrades() {
    }

    /** One entry in a level's pool. */
    interface Trade {

        /** @return false when the item this trade needs does not exist in this game */
        boolean available();

        /**
         * @param villager the villager, or null when another mod asks for an example of the trade
         *                 (JEI Villagers does, with a stand-in merchant)
         * @return the trade, or null if it cannot be made here (a map to a structure that does not exist)
         */
        @Nullable
        MerchantRecipe offer(@Nullable EntityVillager villager, Random random);
    }

    /** @return true if the career has a 1.14 table */
    public static boolean has(String careerKey) {
        return TABLES.containsKey(careerKey);
    }

    /** @return how many levels the career's table defines, or 0 if it has none */
    public static int levels(String careerKey) {
        Trade[][] tiers = TABLES.get(careerKey);
        return tiers == null ? 0 : tiers.length;
    }

    /**
     * @return the career's 1.14 table in the shape Forge's careers use, every pool entry as its own
     * trade, for mods that read careers to list what villagers sell. Null if the career has no table.
     */
    @Nullable
    public static List<List<EntityVillager.ITradeList>> asCareerTrades(String careerKey) {
        Trade[][] tiers = TABLES.get(careerKey);
        if (tiers == null) {
            return null;
        }
        List<List<EntityVillager.ITradeList>> out = new ArrayList<List<EntityVillager.ITradeList>>();
        for (int t = 0; t < tiers.length; t++) {
            List<EntityVillager.ITradeList> level = new ArrayList<EntityVillager.ITradeList>();
            for (final Trade trade : pool(careerKey, t + 1, tiers[t])) {
                if (!trade.available()) {
                    continue;
                }
                level.add(new EntityVillager.ITradeList() {
                    @Override
                    public void addMerchantRecipe(net.minecraft.entity.IMerchant merchant, MerchantRecipeList list, Random random) {
                        MerchantRecipe recipe = trade.offer(merchant instanceof EntityVillager ? (EntityVillager) merchant : null, random);
                        if (recipe != null) {
                            list.add(recipe);
                        }
                    }
                });
            }
            out.add(level);
        }
        return out;
    }

    /**
     * Adds one level's trades to a list, picking as 1.14 does.
     *
     * @param tier the level, 1 to 5
     * @return how many trades were added
     */
    public static int addTier(String careerKey, int tier, EntityVillager villager, MerchantRecipeList list, Random random) {
        Trade[][] tiers = TABLES.get(careerKey);
        if (tiers == null || tier < 1 || tier > tiers.length) {
            return 0;
        }

        List<Trade> pool = new ArrayList<Trade>();
        for (Trade trade : pool(careerKey, tier, tiers[tier - 1])) {
            if (trade.available()) {
                pool.add(trade);
            }
        }

        // Today's addOffersFromItemListings: draw entries at random without putting them back until two give a trade,
        // so an entry that gives none here (a map to a structure not found, another biome's trade) makes way for
        // another. A shuffle is the same draw. 1.14.4 drew two and kept whatever they gave.
        Collections.shuffle(pool, random);
        int added = 0;
        for (Trade trade : pool) {
            if (added >= TRADES_PER_LEVEL) {
                break;
            }
            MerchantRecipe recipe = trade.offer(villager, random);
            if (recipe != null) {
                list.add(recipe);
                added++;
            }
        }
        return added;
    }

    /**
     * @return a level's pool: this mod's table, then whatever other mods added to it through Backport
     * Core's {@code TradeRegistry}
     */
    private static List<Trade> pool(String careerKey, int tier, Trade[] own) {
        List<Trade> pool = new ArrayList<Trade>(Arrays.asList(own));
        for (final TradeRegistry.VillagerTrade added : TradeRegistry.villagerTrades(careerKey, tier)) {
            pool.add(new Trade() {
                @Override
                public boolean available() {
                    return added.available();
                }

                @Nullable
                @Override
                public MerchantRecipe offer(@Nullable EntityVillager villager, Random random) {
                    return added.offer(villager, random);
                }
            });
        }
        return pool;
    }

    // ---------------------------------------------------------------------------------------------
    // The tables
    // ---------------------------------------------------------------------------------------------

    private static void build() {
        table("minecraft:farmer", "farmer",
                level(buy(Items.WHEAT, 20, 16), buy(Items.POTATO, 26, 16), buy(Items.CARROT, 22, 16),
                        buy(Items.BEETROOT, 15, 16), sell(Items.BREAD, 1, 6, 16)),
                level(buy(Blocks.PUMPKIN, 6, 12), sell(Items.PUMPKIN_PIE, 1, 4, STOCK),
                        sell(Items.APPLE, 1, 4, 16)),
                level(sell(Items.COOKIE, 3, 18, STOCK), buy(Blocks.MELON_BLOCK, 4, 12)),
                level(sell(Items.CAKE, 1, 1, STOCK), stew("night_vision", 100), stew("jump_boost", 160),
                        stew("weakness", 140), stew("blindness", 120), stew("poison", 280),
                        stew("saturation", 7)),
                level(sell(Items.GOLDEN_CARROT, 3, 3, STOCK), sell(Items.SPECKLED_MELON, 4, 3, STOCK)));

        table("minecraft:farmer", "fisherman",
                level(buy(Items.STRING, 20, 16), buy(Items.COAL, 10, 16),
                        convert(stack(Items.FISH, 6, 0), 1, stack(Items.COOKED_FISH, 6, 0), 16),
                        optionalSell(3, 1, 16, "updateaquaticbackport:cod_bucket", "futuremc:cod_bucket")),
                level(buy(stack(Items.FISH, 15, 0), 16),
                        convert(stack(Items.FISH, 6, 1), 1, stack(Items.COOKED_FISH, 6, 1), 16),
                        optionalSell(2, 1, STOCK, "villagerbackport:campfire", "futuremc:campfire")),
                level(buy(stack(Items.FISH, 13, 1), 16), enchanted(Items.FISHING_ROD, 3)),
                level(buy(stack(Items.FISH, 6, 2), 12)),
                level(buy(stack(Items.FISH, 4, 3), 12), boatForVillage()));

        table("minecraft:farmer", "shepherd",
                level(buy(wool(EnumDyeColor.WHITE, 18), 16), buy(wool(EnumDyeColor.BROWN, 18), 16),
                        buy(wool(EnumDyeColor.BLACK, 18), 16), buy(wool(EnumDyeColor.GRAY, 18), 16),
                        sell(Items.SHEARS, 2, 1, STOCK)),
                concat(level(buyDye(EnumDyeColor.WHITE, 12, 16), buy(dye(EnumDyeColor.GRAY, 12), 16),
                                buyDye(EnumDyeColor.BLACK, 12, 16), buy(dye(EnumDyeColor.LIGHT_BLUE, 12), 16),
                                buy(dye(EnumDyeColor.LIME, 12), 16)),
                        everyColour(Blocks.WOOL, 1, 1, 16),
                        everyColour(Blocks.CARPET, 1, 4, 16)),
                concat(level(buy(dye(EnumDyeColor.YELLOW, 12), 16), buy(dye(EnumDyeColor.SILVER, 12), 16),
                                buy(dye(EnumDyeColor.ORANGE, 12), 16), buy(dye(EnumDyeColor.RED, 12), 16),
                                buy(dye(EnumDyeColor.PINK, 12), 16)),
                        beds()),
                concat(level(buyDye(EnumDyeColor.BROWN, 12, 16), buy(dye(EnumDyeColor.PURPLE, 12), 16),
                                buyDye(EnumDyeColor.BLUE, 12, 16), buy(dye(EnumDyeColor.GREEN, 12), 16),
                                buy(dye(EnumDyeColor.MAGENTA, 12), 16), buy(dye(EnumDyeColor.CYAN, 12), 16)),
                        banners()),
                level(sell(Items.PAINTING, 2, 3, STOCK)));

        table("minecraft:farmer", "fletcher",
                level(buy(Items.STICK, 32, 16), sell(Items.ARROW, 1, 16, STOCK),
                        convert(new ItemStack(Blocks.GRAVEL, 10), 1, new ItemStack(Items.FLINT, 10), 12)),
                level(buy(Items.FLINT, 26, 12), sell(Items.BOW, 2, 1, STOCK)),
                level(buy(Items.STRING, 14, 16), optionalSell(3, 1, STOCK, "villagerbackport:crossbow", "futuremc:crossbow", "crossbows:crossbow")),
                level(buy(Items.FEATHER, 24, 16), enchanted(Items.BOW, 2)),
                level(buy(new ItemStack(Blocks.TRIPWIRE_HOOK, 8), 12),
                        optionalEnchanted(3, "villagerbackport:crossbow", "futuremc:crossbow", "crossbows:crossbow"),
                        tippedArrows()));

        table("minecraft:librarian", "librarian",
                level(buy(Items.PAPER, 24, 16), enchantedBook(), sell(new ItemStack(Blocks.BOOKSHELF), 9, 1, STOCK)),
                level(buy(Items.BOOK, 4, 12), enchantedBook(), optionalSell(1, 1, STOCK, "villagerbackport:lantern", "futuremc:lantern")),
                level(buy(dye(EnumDyeColor.BLACK, 5), 12), enchantedBook(), sell(new ItemStack(Blocks.GLASS), 1, 4, STOCK)),
                level(buy(Items.WRITABLE_BOOK, 2, 12), enchantedBook(), sell(Items.CLOCK, 5, 1, STOCK),
                        sell(Items.COMPASS, 4, 1, STOCK)),
                // 26.1's master librarian: a yellow and a red candle for 3 emeralds each, and no name tag (26.1 made
                // name tags craftable and gave them to the wandering trader). Without a mod that adds candles the
                // name tag stays, unless Game Drops Backport has made that 26.1 change (its name tag recipe and
                // trader trade come with its golden dandelion), when there is nothing left to sell.
                level(optionalSell(3, 1, STOCK, YELLOW_CANDLES),
                        optionalSell(3, 1, STOCK, RED_CANDLES),
                        unless(sell(Items.NAME_TAG, 20, 1, STOCK), concat(YELLOW_CANDLES, RED_CANDLES, GAME_DROPS_NAME_TAGS))));

        // 1.14's table, or with trades.modernCartographer current Java's (1.21.5, VillagerTrades' CARTOGRAPHER): maps by
        // the villager's biome, the trial chambers map, banners by biome, the mansion map at master.
        table("minecraft:librarian", "cartographer",
                concat(legacy(buy(Items.PAPER, 24, 16), sell(Items.MAP, 7, 1, STOCK)),
                        modern(buy(Items.PAPER, 24, STOCK), sell(Items.MAP, 7, 1, STOCK))),
                concat(legacy(buy(new ItemStack(Blocks.GLASS_PANE, 11), 16), map(13, "Monument", MapDecoration.Type.MONUMENT)),
                        modern(buy(new ItemStack(Blocks.GLASS_PANE, 11), STOCK),
                                biomeMap("minecraft:village_taiga", "village_taiga", "taiga_village", "swamp", "snow", "plains"),
                                biomeMap("minecraft:swamp_hut", "explorer_swamp", "swamp_hut", "taiga", "snow", "jungle"),
                                biomeMap("minecraft:village_snowy", "village_snowy", "snowy_village", "taiga", "swamp"),
                                biomeMap("minecraft:village_savanna", "village_savanna", "savanna_village", "plains", "jungle", "desert"),
                                biomeMap("minecraft:village_plains", "village_plains", "plains_village", "taiga", "snow", "savanna", "desert"),
                                biomeMap("minecraft:jungle_pyramid", "explorer_jungle", "jungle_temple", "swamp", "savanna", "desert"),
                                biomeMap("minecraft:village_desert", "village_desert", "desert_village", "savanna", "jungle"))),
                concat(legacy(buy(Items.COMPASS, 1, 12), map(14, "Mansion", MapDecoration.Type.MANSION)),
                        modern(buy(Items.COMPASS, 1, STOCK), map(13, "Monument", MapDecoration.Type.MONUMENT),
                                locatedMap(12, "minecraft:trial_chambers", "trial_chambers", "trial_chambers"))),
                concat(legacy(concat(level(sell(Items.ITEM_FRAME, 7, 1, STOCK)), banners())),
                        modern(concat(level(sell(Items.ITEM_FRAME, 7, 1, STOCK)), biomeBanners()))),
                concat(legacy(bannerPattern()), modern(bannerPattern(), map(14, "Mansion", MapDecoration.Type.MANSION))));

        table("minecraft:priest", "cleric",
                level(buy(Items.ROTTEN_FLESH, 32, 16), sell(Items.REDSTONE, 1, 2, STOCK)),
                level(buy(Items.GOLD_INGOT, 3, 12), sell(dye(EnumDyeColor.BLUE, 1), 1, STOCK)),
                level(buy(Items.RABBIT_FOOT, 2, 12), sell(new ItemStack(Blocks.GLOWSTONE), 4, 1, STOCK)),
                level(optionalBuy(4, 12, "updateaquaticbackport:scute", "oe:turtle_scute"), buy(Items.GLASS_BOTTLE, 9, 12),
                        sell(Items.ENDER_PEARL, 5, 1, STOCK)),
                level(buy(Items.NETHER_WART, 22, 12), sell(Items.EXPERIENCE_BOTTLE, 3, 1, STOCK)));

        table("minecraft:smith", "armor",
                level(buy(Items.COAL, 15, 16), sell(Items.IRON_LEGGINGS, 7, 1, STOCK), sell(Items.IRON_BOOTS, 4, 1, STOCK),
                        sell(Items.IRON_HELMET, 5, 1, STOCK), sell(Items.IRON_CHESTPLATE, 9, 1, STOCK)),
                level(buy(Items.IRON_INGOT, 4, 12), bell(), sell(Items.CHAINMAIL_BOOTS, 1, 1, STOCK),
                        sell(Items.CHAINMAIL_LEGGINGS, 3, 1, STOCK)),
                level(buy(Items.LAVA_BUCKET, 1, 12), buy(Items.DIAMOND, 1, 12), sell(Items.CHAINMAIL_HELMET, 1, 1, STOCK),
                        sell(Items.CHAINMAIL_CHESTPLATE, 4, 1, STOCK), sell(Items.SHIELD, 5, 1, STOCK)),
                level(enchanted(Items.DIAMOND_LEGGINGS, 14), enchanted(Items.DIAMOND_BOOTS, 8)),
                level(enchanted(Items.DIAMOND_HELMET, 8), enchanted(Items.DIAMOND_CHESTPLATE, 16)));

        table("minecraft:smith", "weapon",
                level(buy(Items.COAL, 15, 16), sell(Items.IRON_AXE, 3, 1, STOCK), enchanted(Items.IRON_SWORD, 2)),
                level(buy(Items.IRON_INGOT, 4, 12), bell()),
                level(buy(Items.FLINT, 24, 12)),
                level(buy(Items.DIAMOND, 1, 12), enchanted(Items.DIAMOND_AXE, 12)),
                level(enchanted(Items.DIAMOND_SWORD, 8)));

        table("minecraft:smith", "tool",
                level(buy(Items.COAL, 15, 16), sell(Items.STONE_AXE, 1, 1, STOCK), sell(Items.STONE_SHOVEL, 1, 1, STOCK),
                        sell(Items.STONE_PICKAXE, 1, 1, STOCK), sell(Items.STONE_HOE, 1, 1, STOCK)),
                level(buy(Items.IRON_INGOT, 4, 12), bell()),
                level(buy(Items.FLINT, 30, 12), enchanted(Items.IRON_AXE, 1), enchanted(Items.IRON_SHOVEL, 2),
                        enchanted(Items.IRON_PICKAXE, 3), sell(Items.DIAMOND_HOE, 4, 1, GEAR_STOCK)),
                level(buy(Items.DIAMOND, 1, 12), enchanted(Items.DIAMOND_AXE, 12), enchanted(Items.DIAMOND_SHOVEL, 5)),
                level(enchanted(Items.DIAMOND_PICKAXE, 13)));

        table("minecraft:smith", "mason",
                level(buy(Items.CLAY_BALL, 10, 16), sell(Items.BRICK, 1, 10, 16)),
                level(buy(new ItemStack(Blocks.STONE, 20), 16), sell(new ItemStack(Blocks.STONEBRICK, 1, 3), 1, 4, 16)),
                level(buy(new ItemStack(Blocks.STONE, 16, 1), 16), buy(new ItemStack(Blocks.STONE, 16, 5), 16),
                        buy(new ItemStack(Blocks.STONE, 16, 3), 16), sell(new ItemStack(Blocks.STONE, 1, 6), 1, 4, 16),
                        sell(new ItemStack(Blocks.STONE, 1, 4), 1, 4, 16), sell(new ItemStack(Blocks.STONE, 1, 2), 1, 4, 16),
                        // 1.17's dripstone block, with Caves & Cliffs Backport.
                        optionalSell(1, 4, 16, "cavesandcliffsbackport:dripstone_block")),
                concat(level(buy(Items.QUARTZ, 12, 12)), everyColour(Blocks.STAINED_HARDENED_CLAY, 1, 1, STOCK),
                        glazedTerracotta()),
                level(sell(new ItemStack(Blocks.QUARTZ_BLOCK, 1, 2), 1, 1, STOCK), sell(new ItemStack(Blocks.QUARTZ_BLOCK), 1, 1, STOCK)));

        table("minecraft:butcher", "butcher",
                level(buy(Items.CHICKEN, 14, 16), buy(Items.PORKCHOP, 7, 16), buy(Items.RABBIT, 4, 16),
                        sell(Items.RABBIT_STEW, 1, 1, STOCK)),
                level(buy(Items.COAL, 15, 16), sell(Items.COOKED_PORKCHOP, 1, 5, 16), sell(Items.COOKED_CHICKEN, 1, 8, 16)),
                level(buy(Items.MUTTON, 7, 16), buy(Items.BEEF, 10, 16)),
                level(optionalBuy(10, 12, "updateaquaticbackport:dried_kelp_block", "oe:dried_kelp_block")),
                level(sweetBerriesBuy(10, 12)));

        table("minecraft:butcher", "leather",
                level(buy(Items.LEATHER, 6, 16), dyed(Items.LEATHER_LEGGINGS, 3), dyed(Items.LEATHER_CHESTPLATE, 7)),
                level(buy(Items.FLINT, 26, 12), dyed(Items.LEATHER_HELMET, 5), dyed(Items.LEATHER_BOOTS, 4)),
                level(buy(Items.RABBIT_HIDE, 9, 12), dyed(Items.LEATHER_CHESTPLATE, 7)),
                level(optionalBuy(4, 12, "updateaquaticbackport:scute", "oe:turtle_scute"), dyedHorseArmor(6)),
                level(sell(Items.SADDLE, 6, 1, STOCK), dyed(Items.LEATHER_HELMET, 5)));
    }

    private static void table(String profession, String career, Trade[]... levels) {
        TABLES.put(profession + "/" + career, levels);
    }

    private static Trade[] level(Trade... trades) {
        return trades;
    }

    private static Trade[] concat(Trade[]... parts) {
        List<Trade> all = new ArrayList<Trade>();
        for (Trade[] part : parts) {
            Collections.addAll(all, part);
        }
        return all.toArray(new Trade[all.size()]);
    }

    // ---------------------------------------------------------------------------------------------
    // Entry kinds, named after 1.14's
    // ---------------------------------------------------------------------------------------------

    private static ItemStack stack(Item item, int count, int meta) {
        return new ItemStack(item, count, meta);
    }

    private static ItemStack wool(EnumDyeColor colour, int count) {
        return new ItemStack(Blocks.WOOL, count, colour.getMetadata());
    }

    private static ItemStack dye(EnumDyeColor colour, int count) {
        return new ItemStack(Items.DYE, count, colour.getDyeDamage());
    }

    /** Fixed stacks, always available. */
    private abstract static class Fixed implements Trade {
        @Override
        public boolean available() {
            return true;
        }
    }

    /** {@code EmeraldForItemsTrade}: the player hands over goods for one emerald. */
    private static Trade buy(Item item, int count, int stock) {
        return buy(new ItemStack(item, count), stock);
    }

    private static Trade buy(Block block, int count, int stock) {
        return buy(new ItemStack(block, count), stock);
    }

    private static Trade buy(final ItemStack wanted, final int stock) {
        return new Fixed() {
            @Override
            public MerchantRecipe offer(EntityVillager villager, Random random) {
                return recipe(wanted.copy(), ItemStack.EMPTY, new ItemStack(Items.EMERALD), stock);
            }
        };
    }

    /** {@code ItemsForEmeraldsTrade}: emeralds for goods. */
    private static Trade sell(Item item, int emeralds, int count, int stock) {
        return sell(new ItemStack(item, count), emeralds, stock);
    }

    private static Trade sell(ItemStack item, int emeralds, int count, int stock) {
        ItemStack sized = item.copy();
        sized.setCount(count);
        return sell(sized, emeralds, stock);
    }

    private static Trade sell(final ItemStack sold, final int emeralds, final int stock) {
        return new Fixed() {
            @Override
            public MerchantRecipe offer(EntityVillager villager, Random random) {
                return recipe(new ItemStack(Items.EMERALD, emeralds), ItemStack.EMPTY, sold.copy(), stock);
            }
        };
    }

    /** {@code ItemsForEmeraldsAndItemsTrade}: goods plus emeralds for other goods. */
    private static Trade convert(final ItemStack given, final int emeralds, final ItemStack result, final int stock) {
        return new Fixed() {
            @Override
            public MerchantRecipe offer(EntityVillager villager, Random random) {
                return recipe(given.copy(), new ItemStack(Items.EMERALD, emeralds), result.copy(), stock);
            }
        };
    }

    /**
     * {@code EnchantedItemForEmeraldsTrade}: an item enchanted at a level from 5 to 19, priced at the
     * base plus that level, capped at a stack.
     */
    private static Trade enchanted(final Item item, final int basePrice) {
        return new Fixed() {
            @Override
            public MerchantRecipe offer(EntityVillager villager, Random random) {
                return enchantedOffer(item, basePrice, random);
            }
        };
    }

    @Nullable
    private static MerchantRecipe enchantedOffer(Item item, int basePrice, Random random) {
        return EnchantedTrades.item(item, basePrice, GEAR_STOCK, random);
    }

    /**
     * {@code EnchantedBookForEmeraldsTrade}, or with trades.modernEnchantedTrades 26.1's enchanted book trade
     * ({@link EnchantedTrades#book}). 1.12.2's own librarian trade is 1.14's formula.
     */
    private static Trade enchantedBook() {
        final EntityVillager.ITradeList vanilla = new EntityVillager.ListEnchantedBookForEmeralds();
        return new Fixed() {
            @Override
            public MerchantRecipe offer(EntityVillager villager, Random random) {
                if (com.exiledradio.villagerbackport.ModConfig.trades.modernEnchantedTrades) {
                    return EnchantedTrades.book(STOCK, random);
                }
                return single(vanilla, villager, random);
            }
        };
    }

    /** {@code EmeraldForMapTrade}: emeralds and a compass for a map to the nearest such structure. */
    private static Trade map(final int emeralds, final String structure, final MapDecoration.Type type) {
        return new Fixed() {
            @Override
            public MerchantRecipe offer(EntityVillager villager, Random random) {
                if (villager == null) {
                    return null;
                }
                net.minecraft.world.World world = villager.world;
                BlockPos target = world.findNearestStructure(structure, new BlockPos(villager), true);
                if (target == null) {
                    return null;
                }
                ItemStack map = net.minecraft.item.ItemMap.setupNewMap(world, target.getX(), target.getZ(), (byte) 2, true, true);
                net.minecraft.item.ItemMap.renderBiomePreviewMap(world, map);
                net.minecraft.world.storage.MapData.addTargetDecoration(map, target, "+", type);
                map.setTranslatableName("filled_map." + structure.toLowerCase(java.util.Locale.ROOT));
                return recipe(new ItemStack(Items.EMERALD, emeralds), new ItemStack(Items.COMPASS), map, STOCK);
            }
        };
    }

    // ---------------------------------------------------------------------------------------------
    // Current Java's cartographer (1.21.5)
    // ---------------------------------------------------------------------------------------------

    /** @return the trades, each offered only while trades.modernCartographer is off (1.14's table) */
    private static Trade[] legacy(Trade... trades) {
        return switched(false, trades);
    }

    /** @return the trades, each offered only while trades.modernCartographer is on */
    private static Trade[] modern(Trade... trades) {
        return switched(true, trades);
    }

    private static Trade[] switched(final boolean modern, Trade[] trades) {
        Trade[] out = new Trade[trades.length];
        for (int i = 0; i < trades.length; i++) {
            final Trade trade = trades[i];
            out[i] = new Trade() {
                @Override
                public boolean available() {
                    return com.exiledradio.villagerbackport.ModConfig.trades.modernCartographer == modern && trade.available();
                }

                @Nullable
                @Override
                public MerchantRecipe offer(@Nullable EntityVillager villager, Random random) {
                    return trade.offer(villager, random);
                }
            };
        }
        return out;
    }

    /**
     * Today's {@code TypeSpecificTrade.oneTradeInBiomes(TreasureMapForEmeralds(8, ...))}: 8 emeralds and a compass for
     * a map to the nearest such structure, sold only by cartographers of the listed biomes.
     */
    private static Trade biomeMap(final String structure, final String name, final String icon, final String... types) {
        final Trade map = locatedMap(8, structure, name, icon);
        return new Trade() {
            @Override
            public boolean available() {
                return map.available();
            }

            @Nullable
            @Override
            public MerchantRecipe offer(@Nullable EntityVillager villager, Random random) {
                if (villager == null || !Arrays.asList(types).contains(com.exiledradio.villagerbackport.data.VillagerTypes.of(villager))) {
                    return null;
                }
                return map.offer(villager, random);
            }
        };
    }

    /**
     * Today's {@code TreasureMapForEmeralds} for a structure found through Backport Core's {@code /locate} registry
     * (this mod's villages, Core's temples, Tricky Trials Backport's trial chambers): offered only when an installed
     * mod has registered the structure, and only if one is found within today's 100 regions.
     */
    private static Trade locatedMap(final int emeralds, final String structure, final String name, final String icon) {
        return new Trade() {
            @Override
            public boolean available() {
                return com.exiledradio.backportcore.locate.StructureLocate.known(structure);
            }

            @Nullable
            @Override
            public MerchantRecipe offer(@Nullable EntityVillager villager, Random random) {
                if (villager == null || !(villager.world instanceof net.minecraft.world.WorldServer)) {
                    return null;
                }
                net.minecraft.world.WorldServer world = (net.minecraft.world.WorldServer) villager.world;
                com.exiledradio.backportcore.locate.StructureLocate.Found found;
                try {
                    found = com.exiledradio.backportcore.locate.StructureLocate.nearest(world, new BlockPos(villager), structure);
                } catch (com.exiledradio.backportcore.locate.StructureLocate.Unavailable e) {
                    return null;
                }
                if (found == null) {
                    return null;
                }
                BlockPos target = found.pos;
                ItemStack map = net.minecraft.item.ItemMap.setupNewMap(world, target.getX(), target.getZ(), (byte) 2, true, true);
                net.minecraft.item.ItemMap.renderBiomePreviewMap(world, map);
                net.minecraft.world.storage.MapData.addTargetDecoration(map, target, "+",
                        com.exiledradio.villagerbackport.mechanics.Cartography.icon(icon));
                map.setTranslatableName("filled_map." + name);
                return recipe(new ItemStack(Items.EMERALD, emeralds), new ItemStack(Items.COMPASS), map, STOCK);
            }
        };
    }

    /** Today's expert cartographer banners: 2 emeralds each, each color sold in the biomes listed (VillagerTrades). */
    private static Trade[] biomeBanners() {
        Object[][] colors = {
                {EnumDyeColor.BLUE, "snow", "taiga"}, {EnumDyeColor.WHITE, "snow", "plains"},
                {EnumDyeColor.RED, "snow", "savanna"}, {EnumDyeColor.GREEN, "desert", "savanna", "jungle"},
                {EnumDyeColor.LIME, "desert", "taiga"}, {EnumDyeColor.PURPLE, "taiga", "swamp"},
                {EnumDyeColor.CYAN, "desert", "snow"}, {EnumDyeColor.YELLOW, "plains", "jungle"},
                {EnumDyeColor.ORANGE, "savanna", "desert"}, {EnumDyeColor.BROWN, "plains", "jungle"},
                {EnumDyeColor.MAGENTA, "savanna"}, {EnumDyeColor.LIGHT_BLUE, "snow", "swamp"},
                {EnumDyeColor.PINK, "taiga", "plains"}, {EnumDyeColor.GRAY, "desert"}, {EnumDyeColor.BLACK, "swamp"}};
        Trade[] trades = new Trade[colors.length];
        for (int i = 0; i < colors.length; i++) {
            EnumDyeColor color = (EnumDyeColor) colors[i][0];
            final List<Object> types = Arrays.asList(colors[i]).subList(1, colors[i].length);
            final Trade banner = sell(new ItemStack(Items.BANNER, 1, color.getDyeDamage()), 2, STOCK);
            trades[i] = new Fixed() {
                @Nullable
                @Override
                public MerchantRecipe offer(@Nullable EntityVillager villager, Random random) {
                    return villager != null && types.contains(com.exiledradio.villagerbackport.data.VillagerTypes.of(villager))
                            ? banner.offer(villager, random) : null;
                }
            };
        }
        return trades;
    }

    /** Runs a vanilla trade and returns what it added, or null if it added nothing. */
    @Nullable
    private static MerchantRecipe single(EntityVillager.ITradeList trade, EntityVillager villager, Random random) {
        MerchantRecipeList scratch = new MerchantRecipeList();
        trade.addMerchantRecipe(villager, scratch, random);
        if (scratch.isEmpty()) {
            return null;
        }
        MerchantRecipe recipe = scratch.get(0);
        return recipe(recipe.getItemToBuy(), recipe.getSecondItemToBuy(), recipe.getItemToSell(), STOCK);
    }

    /** {@code DyedArmorForEmeraldsTrade}: leather armour in one to three dyes mixed. */
    private static Trade dyed(final Item armour, final int emeralds) {
        return new Fixed() {
            @Override
            public MerchantRecipe offer(EntityVillager villager, Random random) {
                List<EnumDyeColor> dyes = new ArrayList<EnumDyeColor>();
                dyes.add(EnumDyeColor.byMetadata(random.nextInt(16)));
                if (random.nextFloat() > 0.7F) {
                    dyes.add(EnumDyeColor.byMetadata(random.nextInt(16)));
                }
                if (random.nextFloat() > 0.8F) {
                    dyes.add(EnumDyeColor.byMetadata(random.nextInt(16)));
                }

                ItemStack result = new ItemStack(armour);
                if (armour instanceof ItemArmor) {
                    ((ItemArmor) armour).setColor(result, mix(dyes));
                }
                return recipe(new ItemStack(Items.EMERALD, emeralds), ItemStack.EMPTY, result, STOCK);
            }
        };
    }

    /** The crafting grid's dye mix, as 1.12.2's {@code RecipesArmorDyes} and 1.14 both do it. */
    private static int mix(List<EnumDyeColor> dyes) {
        int[] sum = new int[3];
        int brightest = 0;

        for (EnumDyeColor dye : dyes) {
            float[] rgb = dye.getColorComponentValues();
            int r = (int) (rgb[0] * 255.0F);
            int g = (int) (rgb[1] * 255.0F);
            int b = (int) (rgb[2] * 255.0F);
            brightest += Math.max(r, Math.max(g, b));
            sum[0] += r;
            sum[1] += g;
            sum[2] += b;
        }

        int count = dyes.size();
        int r = sum[0] / count;
        int g = sum[1] / count;
        int b = sum[2] / count;
        float average = (float) brightest / (float) count;
        float peak = (float) Math.max(r, Math.max(g, b));

        r = (int) ((float) r * average / peak);
        g = (int) ((float) g * average / peak);
        b = (int) ((float) b * average / peak);
        return (r << 16) + (g << 8) + b;
    }

    /** {@code ItemWithPotionForEmeraldsAndItemsTrade}: five arrows and two emeralds for five tipped. */
    private static Trade tippedArrows() {
        return new Fixed() {
            @Override
            public MerchantRecipe offer(EntityVillager villager, Random random) {
                List<PotionType> potions = new ArrayList<PotionType>();
                for (PotionType type : ForgeRegistries.POTION_TYPES) {
                    ResourceLocation name = type.getRegistryName();
                    // 1.14 takes brewable potions with an effect. Vanilla's own set is that, minus
                    // luck, which has an effect but no brewing recipe; another mod's potions are not
                    // 1.14's to offer.
                    if (name != null && "minecraft".equals(name.getNamespace()) && !"luck".equals(name.getPath())
                            && !type.getEffects().isEmpty()) {
                        potions.add(type);
                    }
                }
                if (potions.isEmpty()) {
                    return null;
                }
                PotionType potion = potions.get(random.nextInt(potions.size()));
                ItemStack result = PotionUtils.addPotionToItemStack(new ItemStack(Items.TIPPED_ARROW, 5), potion);
                return recipe(new ItemStack(Items.EMERALD, 2), new ItemStack(Items.ARROW, 5), result, STOCK);
            }
        };
    }

    /**
     * {@code EmeraldForVillageTypeItemTrade}: one boat of the village's wood for an emerald.
     *
     * <p>1.14 reads the villager's type, fixed by the biome it was born in. 1.12.2 villagers have no
     * type, so the biome it stands in when it reaches Master decides instead - for a villager that
     * has lived in its village all along, the same answer.
     */
    private static Trade boatForVillage() {
        return new Fixed() {
            @Override
            public MerchantRecipe offer(EntityVillager villager, Random random) {
                Item boat = Items.BOAT;
                if (villager == null) {
                    return recipe(new ItemStack(boat), ItemStack.EMPTY, new ItemStack(Items.EMERALD), STOCK);
                }
                Biome biome = villager.world.getBiome(new BlockPos(villager));
                if (BiomeDictionary.hasType(biome, BiomeDictionary.Type.SWAMP)) {
                    boat = Items.DARK_OAK_BOAT;
                } else if (BiomeDictionary.hasType(biome, BiomeDictionary.Type.SAVANNA)) {
                    boat = Items.ACACIA_BOAT;
                } else if (BiomeDictionary.hasType(biome, BiomeDictionary.Type.JUNGLE)
                        || BiomeDictionary.hasType(biome, BiomeDictionary.Type.SANDY)) {
                    boat = Items.JUNGLE_BOAT;
                } else if (BiomeDictionary.hasType(biome, BiomeDictionary.Type.SNOWY)
                        || BiomeDictionary.hasType(biome, BiomeDictionary.Type.CONIFEROUS)) {
                    boat = Items.SPRUCE_BOAT;
                }
                return recipe(new ItemStack(boat), ItemStack.EMPTY, new ItemStack(Items.EMERALD), STOCK);
            }
        };
    }

    /** One entry per colour, each its own pool entry, as 1.14 lists them. */
    private static Trade[] everyColour(Block block, int emeralds, int count, int stock) {
        Trade[] trades = new Trade[16];
        for (EnumDyeColor colour : EnumDyeColor.values()) {
            trades[colour.getMetadata()] = sell(new ItemStack(block, count, colour.getMetadata()), emeralds, stock);
        }
        return trades;
    }

    private static Trade[] glazedTerracotta() {
        Block[] blocks = {Blocks.WHITE_GLAZED_TERRACOTTA, Blocks.ORANGE_GLAZED_TERRACOTTA,
                Blocks.MAGENTA_GLAZED_TERRACOTTA, Blocks.LIGHT_BLUE_GLAZED_TERRACOTTA,
                Blocks.YELLOW_GLAZED_TERRACOTTA, Blocks.LIME_GLAZED_TERRACOTTA, Blocks.PINK_GLAZED_TERRACOTTA,
                Blocks.GRAY_GLAZED_TERRACOTTA, Blocks.SILVER_GLAZED_TERRACOTTA, Blocks.CYAN_GLAZED_TERRACOTTA,
                Blocks.PURPLE_GLAZED_TERRACOTTA, Blocks.BLUE_GLAZED_TERRACOTTA, Blocks.BROWN_GLAZED_TERRACOTTA,
                Blocks.GREEN_GLAZED_TERRACOTTA, Blocks.RED_GLAZED_TERRACOTTA, Blocks.BLACK_GLAZED_TERRACOTTA};
        Trade[] trades = new Trade[blocks.length];
        for (int i = 0; i < blocks.length; i++) {
            trades[i] = sell(new ItemStack(blocks[i]), 1, STOCK);
        }
        return trades;
    }

    /** Beds in sixteen colours. The bed item's metadata is its colour. */
    private static Trade[] beds() {
        Trade[] trades = new Trade[16];
        for (EnumDyeColor colour : EnumDyeColor.values()) {
            trades[colour.getMetadata()] = sell(new ItemStack(Items.BED, 1, colour.getMetadata()), 3, STOCK);
        }
        return trades;
    }

    /** Banners in sixteen colours. The banner item's metadata is the dye damage of its base. */
    private static Trade[] banners() {
        Trade[] trades = new Trade[16];
        for (EnumDyeColor colour : EnumDyeColor.values()) {
            trades[colour.getMetadata()] = sell(new ItemStack(Items.BANNER, 1, colour.getDyeDamage()), 3, STOCK);
        }
        return trades;
    }

    /** The armourer's, weaponsmith's and toolsmith's bell, which is this mod's own block. */
    private static Trade bell() {
        return optionalSell(36, 1, STOCK, "villagerbackport:bell");
    }

    /**
     * The master cartographer's Globe banner pattern, 8 emeralds. With the flora package switched off
     * there is no Globe, and one of this mod's four other patterns stands in, so a master is never
     * left with nothing to sell.
     */
    private static Trade bannerPattern() {
        return new Trade() {
            @Override
            public boolean available() {
                return com.exiledradio.villagerbackport.flora.FloraContent.globeBannerPattern != null
                        || com.exiledradio.villagerbackport.item.ModItems.mojangBannerPattern != null;
            }

            @Override
            public MerchantRecipe offer(EntityVillager villager, Random random) {
                Item globe = com.exiledradio.villagerbackport.flora.FloraContent.globeBannerPattern;
                if (globe != null) {
                    return recipe(new ItemStack(Items.EMERALD, 8), ItemStack.EMPTY, new ItemStack(globe), STOCK);
                }
                Item[] patterns = {com.exiledradio.villagerbackport.item.ModItems.creeperBannerPattern,
                        com.exiledradio.villagerbackport.item.ModItems.skullBannerPattern,
                        com.exiledradio.villagerbackport.item.ModItems.flowerBannerPattern,
                        com.exiledradio.villagerbackport.item.ModItems.mojangBannerPattern};
                Item pattern = patterns[random.nextInt(patterns.length)];
                return pattern == null ? null
                        : recipe(new ItemStack(Items.EMERALD, 8), ItemStack.EMPTY, new ItemStack(pattern), STOCK);
            }
        };
    }

    /**
     * {@code EmeraldForItemsTrade} for one of the four colors 1.14 gave a dye item of its own: that
     * item when the flora package is on, 1.12.2's bone meal, ink sac, cocoa beans or lapis otherwise.
     * Looked up when offered, since the table is built before items exist.
     */
    private static Trade buyDye(final EnumDyeColor colour, final int count, final int stock) {
        return new Fixed() {
            @Override
            public MerchantRecipe offer(EntityVillager villager, Random random) {
                Item modern = modernDye(colour);
                ItemStack wanted = modern != null ? new ItemStack(modern, count) : dye(colour, count);
                return recipe(wanted, ItemStack.EMPTY, new ItemStack(Items.EMERALD), stock);
            }
        };
    }

    @Nullable
    private static Item modernDye(EnumDyeColor colour) {
        switch (colour) {
            case WHITE:
                return com.exiledradio.villagerbackport.flora.FloraContent.whiteDye;
            case BLACK:
                return com.exiledradio.villagerbackport.flora.FloraContent.blackDye;
            case BROWN:
                return com.exiledradio.villagerbackport.flora.FloraContent.brownDye;
            case BLUE:
                return com.exiledradio.villagerbackport.flora.FloraContent.blueDye;
            default:
                return null;
        }
    }

    /**
     * {@code SuspiciousStewForEmeraldTrade}: one emerald for a stew with a hidden effect. Each of the
     * six is its own entry in the expert farmer's pool, with 1.14's trade durations (which are not the
     * flowers' durations). Falls back to Future MC's stew, plain, when the flora package is off.
     */
    private static Trade stew(final String effect, final int ticks) {
        return new Trade() {
            @Override
            public boolean available() {
                return com.exiledradio.villagerbackport.flora.FloraContent.suspiciousStew != null
                        || ForgeRegistries.ITEMS.containsKey(new ResourceLocation("futuremc:suspicious_stew"));
            }

            @Override
            public MerchantRecipe offer(EntityVillager villager, Random random) {
                ItemStack stew;
                if (com.exiledradio.villagerbackport.flora.FloraContent.suspiciousStew != null) {
                    stew = com.exiledradio.villagerbackport.flora.ItemSuspiciousStew.create(
                            net.minecraft.potion.Potion.getPotionFromResourceLocation(effect), ticks);
                } else {
                    Item theirs = ForgeRegistries.ITEMS.getValue(new ResourceLocation("futuremc:suspicious_stew"));
                    if (theirs == null) {
                        return null;
                    }
                    stew = new ItemStack(theirs);
                }
                return recipe(new ItemStack(Items.EMERALD), ItemStack.EMPTY, stew, STOCK);
            }
        };
    }

    /** {@code DyedArmorForEmeraldsTrade} for leather horse armor, which only the flora package adds. */
    private static Trade dyedHorseArmor(final int emeralds) {
        return new Trade() {
            @Override
            public boolean available() {
                return com.exiledradio.villagerbackport.flora.FloraContent.leatherHorseArmor != null;
            }

            @Override
            public MerchantRecipe offer(EntityVillager villager, Random random) {
                List<EnumDyeColor> dyes = new ArrayList<EnumDyeColor>();
                dyes.add(EnumDyeColor.byMetadata(random.nextInt(16)));
                if (random.nextFloat() > 0.7F) {
                    dyes.add(EnumDyeColor.byMetadata(random.nextInt(16)));
                }
                if (random.nextFloat() > 0.8F) {
                    dyes.add(EnumDyeColor.byMetadata(random.nextInt(16)));
                }
                ItemStack result = new ItemStack(com.exiledradio.villagerbackport.flora.FloraContent.leatherHorseArmor);
                com.exiledradio.villagerbackport.flora.ItemLeatherHorseArmor.setColor(result, mix(dyes));
                return recipe(new ItemStack(Items.EMERALD, emeralds), ItemStack.EMPTY, result, STOCK);
            }
        };
    }

    // ---------------------------------------------------------------------------------------------
    // Items another mod may add
    // ---------------------------------------------------------------------------------------------

    /** A trade on an item looked up by name, the first of the candidates that exists. */
    private abstract static class Optional implements Trade {
        private final String[] candidates;

        Optional(String... candidates) {
            this.candidates = candidates;
        }

        @Nullable
        Item item() {
            for (String name : this.candidates) {
                Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(name));
                if (item != null && item != Items.AIR) {
                    return item;
                }
            }
            return null;
        }

        @Override
        public boolean available() {
            return item() != null;
        }
    }

    private static String[] concat(String[]... parts) {
        List<String> out = new ArrayList<String>();
        for (String[] part : parts) {
            out.addAll(Arrays.asList(part));
        }
        return out.toArray(new String[0]);
    }

    /** A trade offered only when none of the named items exist - the fallback for a missing one. */
    private static Trade unless(final Trade trade, final String... absent) {
        return new Trade() {
            @Override
            public boolean available() {
                for (String name : absent) {
                    Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(name));
                    if (item != null && item != Items.AIR) {
                        return false;
                    }
                }
                return trade.available();
            }

            @Override
            public MerchantRecipe offer(EntityVillager villager, Random random) {
                return trade.offer(villager, random);
            }
        };
    }

    private static Trade optionalSell(final int emeralds, final int count, final int stock, String... candidates) {
        return new Optional(candidates) {
            @Override
            public MerchantRecipe offer(EntityVillager villager, Random random) {
                Item item = item();
                return item == null ? null
                        : recipe(new ItemStack(Items.EMERALD, emeralds), ItemStack.EMPTY, new ItemStack(item, count), stock);
            }
        };
    }

    private static Trade optionalBuy(final int count, final int stock, String... candidates) {
        return new Optional(candidates) {
            @Override
            public MerchantRecipe offer(EntityVillager villager, Random random) {
                Item item = item();
                return item == null ? null
                        : recipe(new ItemStack(item, count), ItemStack.EMPTY, new ItemStack(Items.EMERALD), stock);
            }
        };
    }

    /**
     * Buying sweet berries: this mod's own, or Future MC's when that is the copy kept - whichever
     * {@code FoxContent.berries()} says is in use, so a villager never asks for the hidden one.
     */
    private static Trade sweetBerriesBuy(final int count, final int stock) {
        return new Optional() {
            @Override
            Item item() {
                return com.exiledradio.villagerbackport.fox.FoxContent.berries();
            }

            @Override
            public MerchantRecipe offer(EntityVillager villager, Random random) {
                Item item = item();
                return item == null ? null
                        : recipe(new ItemStack(item, count), ItemStack.EMPTY, new ItemStack(Items.EMERALD), stock);
            }
        };
    }

    private static Trade optionalEnchanted(final int basePrice, String... candidates) {
        return new Optional(candidates) {
            @Override
            public MerchantRecipe offer(EntityVillager villager, Random random) {
                Item item = item();
                return item == null ? null : enchantedOffer(item, basePrice, random);
            }
        };
    }

    // ---------------------------------------------------------------------------------------------

    private static MerchantRecipe recipe(ItemStack first, ItemStack second, ItemStack result, int stock) {
        // uses 0, maxUses = stock. TradeStock keeps the figure in step afterwards.
        return new MerchantRecipe(first, second, result, 0, stock);
    }
}
