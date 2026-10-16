package com.exiledradio.villagerbackport.trade;

import net.minecraft.block.Block;
import net.minecraft.enchantment.EnchantmentHelper;
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
 * tag when a mod that adds candles is installed.
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
 * <p>Where 1.12.2 has neither the item nor a stand-in, the level can come up short: the leather
 * horse armour has no 1.12.2 counterpart, and the cartographer's globe banner pattern is replaced
 * with one of this mod's four banner patterns, since a master with nothing to sell is worse than a
 * near miss.
 */
public final class ModernTrades {

    /** 1.14 takes this many trades from a level's pool. */
    private static final int TRADES_PER_LEVEL = 2;

    /** 1.14's stock for an ordinary trade, the {@code MerchantOffer} constructor's default. */
    private static final int STOCK = 12;

    /** Stock for enchanted tools, weapons and armour. */
    private static final int GEAR_STOCK = 3;

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

        /** @return the trade, or null if it cannot be made here (a map to a structure that does not exist) */
        @Nullable
        MerchantRecipe offer(EntityVillager villager, Random random);
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
        for (Trade trade : tiers[tier - 1]) {
            if (trade.available()) {
                pool.add(trade);
            }
        }

        // 1.14 draws indices until it has two different ones, which is a uniform pick of two; a
        // shuffle is the same thing without the loop.
        if (pool.size() > TRADES_PER_LEVEL) {
            Collections.shuffle(pool, random);
            pool = pool.subList(0, TRADES_PER_LEVEL);
        }

        int added = 0;
        for (Trade trade : pool) {
            MerchantRecipe recipe = trade.offer(villager, random);
            if (recipe != null) {
                list.add(recipe);
                added++;
            }
        }
        return added;
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
                level(sell(Items.CAKE, 1, 1, STOCK), optionalSell(1, 1, STOCK, "futuremc:suspicious_stew")),
                level(sell(Items.GOLDEN_CARROT, 3, 3, STOCK), sell(Items.SPECKLED_MELON, 4, 3, STOCK)));

        table("minecraft:farmer", "fisherman",
                level(buy(Items.STRING, 20, 16), buy(Items.COAL, 10, 16),
                        convert(stack(Items.FISH, 6, 0), 1, stack(Items.COOKED_FISH, 6, 0), 16),
                        optionalSell(3, 1, 16, "futuremc:cod_bucket")),
                level(buy(stack(Items.FISH, 15, 0), 16),
                        convert(stack(Items.FISH, 6, 1), 1, stack(Items.COOKED_FISH, 6, 1), 16),
                        optionalSell(2, 1, STOCK, "futuremc:campfire")),
                level(buy(stack(Items.FISH, 13, 1), 16), enchanted(Items.FISHING_ROD, 3)),
                level(buy(stack(Items.FISH, 6, 2), 12)),
                level(buy(stack(Items.FISH, 4, 3), 12), boatForVillage()));

        table("minecraft:farmer", "shepherd",
                level(buy(wool(EnumDyeColor.WHITE, 18), 16), buy(wool(EnumDyeColor.BROWN, 18), 16),
                        buy(wool(EnumDyeColor.BLACK, 18), 16), buy(wool(EnumDyeColor.GRAY, 18), 16),
                        sell(Items.SHEARS, 2, 1, STOCK)),
                concat(level(buy(dye(EnumDyeColor.WHITE, 12), 16), buy(dye(EnumDyeColor.GRAY, 12), 16),
                                buy(dye(EnumDyeColor.BLACK, 12), 16), buy(dye(EnumDyeColor.LIGHT_BLUE, 12), 16),
                                buy(dye(EnumDyeColor.LIME, 12), 16)),
                        everyColour(Blocks.WOOL, 1, 1, 16),
                        everyColour(Blocks.CARPET, 1, 4, 16)),
                concat(level(buy(dye(EnumDyeColor.YELLOW, 12), 16), buy(dye(EnumDyeColor.SILVER, 12), 16),
                                buy(dye(EnumDyeColor.ORANGE, 12), 16), buy(dye(EnumDyeColor.RED, 12), 16),
                                buy(dye(EnumDyeColor.PINK, 12), 16)),
                        beds()),
                concat(level(buy(dye(EnumDyeColor.BROWN, 12), 16), buy(dye(EnumDyeColor.PURPLE, 12), 16),
                                buy(dye(EnumDyeColor.BLUE, 12), 16), buy(dye(EnumDyeColor.GREEN, 12), 16),
                                buy(dye(EnumDyeColor.MAGENTA, 12), 16), buy(dye(EnumDyeColor.CYAN, 12), 16)),
                        banners()),
                level(sell(Items.PAINTING, 2, 3, STOCK)));

        table("minecraft:farmer", "fletcher",
                level(buy(Items.STICK, 32, 16), sell(Items.ARROW, 1, 16, STOCK),
                        convert(new ItemStack(Blocks.GRAVEL, 10), 1, new ItemStack(Items.FLINT, 10), 12)),
                level(buy(Items.FLINT, 26, 12), sell(Items.BOW, 2, 1, STOCK)),
                level(buy(Items.STRING, 14, 16), optionalSell(3, 1, STOCK, "futuremc:crossbow", "crossbows:crossbow")),
                level(buy(Items.FEATHER, 24, 16), enchanted(Items.BOW, 2)),
                level(buy(new ItemStack(Blocks.TRIPWIRE_HOOK, 8), 12),
                        optionalEnchanted(3, "futuremc:crossbow", "crossbows:crossbow"),
                        tippedArrows()));

        table("minecraft:librarian", "librarian",
                level(buy(Items.PAPER, 24, 16), enchantedBook(), sell(new ItemStack(Blocks.BOOKSHELF), 9, 1, STOCK)),
                level(buy(Items.BOOK, 4, 12), enchantedBook(), optionalSell(1, 1, STOCK, "villagerbackport:lantern", "futuremc:lantern")),
                level(buy(dye(EnumDyeColor.BLACK, 5), 12), enchantedBook(), sell(new ItemStack(Blocks.GLASS), 1, 4, STOCK)),
                level(buy(Items.WRITABLE_BOOK, 2, 12), enchantedBook(), sell(Items.CLOCK, 5, 1, STOCK),
                        sell(Items.COMPASS, 4, 1, STOCK)),
                // Current Java's master librarian sells candles, which replaced 1.14's name tag in 1.17.
                // Without a mod that adds candles, the name tag stays.
                level(optionalSell(3, 1, STOCK, "deeperdepths:yellow_candle"),
                        optionalSell(3, 1, STOCK, "deeperdepths:red_candle"),
                        unless(sell(Items.NAME_TAG, 20, 1, STOCK), "deeperdepths:yellow_candle", "deeperdepths:red_candle")));

        table("minecraft:librarian", "cartographer",
                level(buy(Items.PAPER, 24, 16), sell(Items.MAP, 7, 1, STOCK)),
                level(buy(new ItemStack(Blocks.GLASS_PANE, 11), 16), map(13, "Monument", MapDecoration.Type.MONUMENT)),
                level(buy(Items.COMPASS, 1, 12), map(14, "Mansion", MapDecoration.Type.MANSION)),
                concat(level(sell(Items.ITEM_FRAME, 7, 1, STOCK)), banners()),
                level(bannerPattern()));

        table("minecraft:priest", "cleric",
                level(buy(Items.ROTTEN_FLESH, 32, 16), sell(Items.REDSTONE, 1, 2, STOCK)),
                level(buy(Items.GOLD_INGOT, 3, 12), sell(dye(EnumDyeColor.BLUE, 1), 1, STOCK)),
                level(buy(Items.RABBIT_FOOT, 2, 12), sell(new ItemStack(Blocks.GLOWSTONE), 4, 1, STOCK)),
                level(optionalBuy(4, 12, "oe:turtle_scute"), buy(Items.GLASS_BOTTLE, 9, 12),
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
                        sell(new ItemStack(Blocks.STONE, 1, 4), 1, 4, 16), sell(new ItemStack(Blocks.STONE, 1, 2), 1, 4, 16)),
                concat(level(buy(Items.QUARTZ, 12, 12)), everyColour(Blocks.STAINED_HARDENED_CLAY, 1, 1, STOCK),
                        glazedTerracotta()),
                level(sell(new ItemStack(Blocks.QUARTZ_BLOCK, 1, 2), 1, 1, STOCK), sell(new ItemStack(Blocks.QUARTZ_BLOCK), 1, 1, STOCK)));

        table("minecraft:butcher", "butcher",
                level(buy(Items.CHICKEN, 14, 16), buy(Items.PORKCHOP, 7, 16), buy(Items.RABBIT, 4, 16),
                        sell(Items.RABBIT_STEW, 1, 1, STOCK)),
                level(buy(Items.COAL, 15, 16), sell(Items.COOKED_PORKCHOP, 1, 5, 16), sell(Items.COOKED_CHICKEN, 1, 8, 16)),
                level(buy(Items.MUTTON, 7, 16), buy(Items.BEEF, 10, 16)),
                level(optionalBuy(10, 12, "oe:dried_kelp_block")),
                level(optionalBuy(10, 12, "futuremc:sweet_berries")));

        table("minecraft:butcher", "leather",
                level(buy(Items.LEATHER, 6, 16), dyed(Items.LEATHER_LEGGINGS, 3), dyed(Items.LEATHER_CHESTPLATE, 7)),
                level(buy(Items.FLINT, 26, 12), dyed(Items.LEATHER_HELMET, 5), dyed(Items.LEATHER_BOOTS, 4)),
                level(buy(Items.RABBIT_HIDE, 9, 12), dyed(Items.LEATHER_CHESTPLATE, 7)),
                level(optionalBuy(4, 12, "oe:turtle_scute")),
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

    private static MerchantRecipe enchantedOffer(Item item, int basePrice, Random random) {
        int level = 5 + random.nextInt(15);
        ItemStack result = EnchantmentHelper.addRandomEnchantment(random, new ItemStack(item), level, false);
        return recipe(new ItemStack(Items.EMERALD, Math.min(basePrice + level, 64)), ItemStack.EMPTY, result, GEAR_STOCK);
    }

    /** {@code EnchantedBookForEmeraldsTrade}. 1.12.2's own librarian trade is the same formula. */
    private static Trade enchantedBook() {
        final EntityVillager.ITradeList vanilla = new EntityVillager.ListEnchantedBookForEmeralds();
        return new Fixed() {
            @Override
            public MerchantRecipe offer(EntityVillager villager, Random random) {
                return single(vanilla, villager, random);
            }
        };
    }

    /** {@code EmeraldForMapTrade}: emeralds and a compass for a map to the nearest such structure. */
    private static Trade map(final int emeralds, final String structure, final MapDecoration.Type type) {
        return new Fixed() {
            @Override
            public MerchantRecipe offer(EntityVillager villager, Random random) {
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
                Biome biome = villager.world.getBiome(new BlockPos(villager));
                Item boat = Items.BOAT;
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

    /** Stands in for the globe banner pattern, which 1.12.2 has no pattern for. */
    private static Trade bannerPattern() {
        return new Trade() {
            @Override
            public boolean available() {
                return com.exiledradio.villagerbackport.item.ModItems.mojangBannerPattern != null;
            }

            @Override
            public MerchantRecipe offer(EntityVillager villager, Random random) {
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
