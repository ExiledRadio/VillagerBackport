package com.exiledradio.villagerbackport.job;

import com.exiledradio.villagerbackport.VillagerBackport;

import net.minecraft.entity.IMerchant;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.village.MerchantRecipe;
import net.minecraft.village.MerchantRecipeList;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.common.registry.VillagerRegistry;

import java.util.Random;

/**
 * The mason, 1.14's stonecutter villager, which 1.12.2 has no equivalent of.
 *
 * <h2>Why a career and not a profession</h2>
 * 1.12.2 files what 1.14 calls a profession one level down, as a career. Twelve of the thirteen line
 * up already; the mason is the one that does not exist. Adding it as a career under the existing
 * smith profession means every part of this mod keeps working unchanged - the outfit lookup, the
 * texture suppressor and the trade tier logic all key on career names and all recognise it
 * immediately. A new profession would have to be taught to each of them separately, and would render
 * without the biome clothing every other villager wears.
 *
 * <p>The smith is the natural home: it already covers the armorer, weaponsmith and toolsmith, and a
 * mason is the same kind of tradesman.
 *
 * <h2>Trades</h2>
 * Taken from 1.14's own {@code VillagerTrades} rather than transcribed, with 1.12.2's item forms -
 * the stone variants are metadata on one block here, where 1.14 gives each its own.
 *
 * <p>1.14 offers a villager at most two trades per level and picks them at random from a longer
 * list; 1.12.2 adds every entry it is given. Tiers with a wide choice - the stone variants, and the
 * thirty-two terracottas - are therefore wrapped in {@link OneOf} so a mason ends up with a couple
 * of trades per tier rather than dozens.
 */
public final class MasonCareer {

    public static final String NAME = "mason";

    /** Metadata for the stone variants, which are one block with subtypes in 1.12.2. */
    private static final int GRANITE = 1;
    private static final int POLISHED_GRANITE = 2;
    private static final int DIORITE = 3;
    private static final int POLISHED_DIORITE = 4;
    private static final int ANDESITE = 5;
    private static final int POLISHED_ANDESITE = 6;

    /** Chiseled stone brick, likewise a metadata variant here. */
    private static final int CHISELED_STONE_BRICK = 3;

    /** 1.12.2 gives each glazed terracotta colour its own block rather than a metadata value. */
    private static final net.minecraft.block.Block[] GLAZED = {
            Blocks.WHITE_GLAZED_TERRACOTTA, Blocks.ORANGE_GLAZED_TERRACOTTA,
            Blocks.MAGENTA_GLAZED_TERRACOTTA, Blocks.LIGHT_BLUE_GLAZED_TERRACOTTA,
            Blocks.YELLOW_GLAZED_TERRACOTTA, Blocks.LIME_GLAZED_TERRACOTTA,
            Blocks.PINK_GLAZED_TERRACOTTA, Blocks.GRAY_GLAZED_TERRACOTTA,
            Blocks.SILVER_GLAZED_TERRACOTTA, Blocks.CYAN_GLAZED_TERRACOTTA,
            Blocks.PURPLE_GLAZED_TERRACOTTA, Blocks.BLUE_GLAZED_TERRACOTTA,
            Blocks.BROWN_GLAZED_TERRACOTTA, Blocks.GREEN_GLAZED_TERRACOTTA,
            Blocks.RED_GLAZED_TERRACOTTA, Blocks.BLACK_GLAZED_TERRACOTTA,
    };

    private static boolean registered;

    private MasonCareer() {
    }

    /**
     * Adds the career to the smith profession. Safe to call more than once.
     *
     * <p>Called after registries are populated, since it needs the vanilla profession to exist.
     */
    public static void register() {
        if (registered) {
            return;
        }

        VillagerRegistry.VillagerProfession smith =
                ForgeRegistries.VILLAGER_PROFESSIONS.getValue(new ResourceLocation("minecraft:smith"));

        if (smith == null) {
            VillagerBackport.LOGGER.warn(
                    "The vanilla smith profession is missing, so the mason career cannot be added. "
                            + "Stonecutters will not offer a job.");
            return;
        }

        // Already present - another mod, or a previous call. Adding it twice would throw.
        for (VillagerRegistry.VillagerCareer existing : com.exiledradio.villagerbackport.compat.VillagerAccess.getCareers(smith)) {
            if (NAME.equals(existing.getName())) {
                registered = true;
                return;
            }
        }

        try {
            new VillagerRegistry.VillagerCareer(smith, NAME)
                    .addTrade(1,
                            buy(new ItemStack(Items.CLAY_BALL), 10),
                            sell(new ItemStack(Items.BRICK), 10))
                    .addTrade(2,
                            buy(new ItemStack(Blocks.STONE), 20),
                            sell(new ItemStack(Blocks.STONEBRICK, 1, CHISELED_STONE_BRICK), 4))
                    .addTrade(3,
                            new OneOf(
                                    buy(new ItemStack(Blocks.STONE, 1, GRANITE), 16),
                                    buy(new ItemStack(Blocks.STONE, 1, ANDESITE), 16),
                                    buy(new ItemStack(Blocks.STONE, 1, DIORITE), 16)),
                            new OneOf(
                                    sell(new ItemStack(Blocks.STONE, 1, POLISHED_GRANITE), 4),
                                    sell(new ItemStack(Blocks.STONE, 1, POLISHED_ANDESITE), 4),
                                    sell(new ItemStack(Blocks.STONE, 1, POLISHED_DIORITE), 4)))
                    .addTrade(4,
                            buy(new ItemStack(Items.QUARTZ), 12),
                            terracotta(),
                            glazedTerracotta())
                    .addTrade(5,
                            sell(new ItemStack(Blocks.QUARTZ_BLOCK, 1, 2), 1),
                            sell(new ItemStack(Blocks.QUARTZ_BLOCK), 1));

            registered = true;
            VillagerBackport.LOGGER.info("Registered the mason career on the smith profession.");
        } catch (RuntimeException e) {
            VillagerBackport.LOGGER.error("Could not register the mason career.", e);
        }
    }

    /** One of the sixteen stained terracottas, which are metadata on a single block here. */
    private static EntityVillager.ITradeList terracotta() {
        EntityVillager.ITradeList[] options = new EntityVillager.ITradeList[16];
        for (int colour = 0; colour < options.length; colour++) {
            options[colour] = sell(new ItemStack(Blocks.STAINED_HARDENED_CLAY, 1, colour), 1);
        }
        return new OneOf(options);
    }

    private static EntityVillager.ITradeList glazedTerracotta() {
        EntityVillager.ITradeList[] options = new EntityVillager.ITradeList[GLAZED.length];
        for (int colour = 0; colour < options.length; colour++) {
            options[colour] = sell(new ItemStack(GLAZED[colour]), 1);
        }
        return new OneOf(options);
    }

    /**
     * The villager buys: the player hands over {@code count} of the stack for one emerald.
     *
     * <p>Vanilla's {@code EmeraldForItems} takes only an {@link net.minecraft.item.Item} and so
     * cannot express a metadata variant, which most of a mason's stone is.
     */
    private static EntityVillager.ITradeList buy(ItemStack stack, int count) {
        return new BuyStack(stack, count);
    }

    /** The villager sells: one emerald buys {@code count} of the stack. */
    private static EntityVillager.ITradeList sell(ItemStack stack, int count) {
        return new EntityVillager.ListItemForEmeralds(stack, new EntityVillager.PriceInfo(-count, -count));
    }

    /**
     * Buys a stack of a specific metadata variant for one emerald.
     */
    private static class BuyStack implements EntityVillager.ITradeList {

        private final ItemStack stack;
        private final int count;

        BuyStack(ItemStack stack, int count) {
            this.stack = stack;
            this.count = count;
        }

        @Override
        public void addMerchantRecipe(IMerchant merchant, MerchantRecipeList recipes, Random random) {
            ItemStack cost = this.stack.copy();
            cost.setCount(this.count);
            recipes.add(new MerchantRecipe(cost, new ItemStack(Items.EMERALD)));
        }
    }

    /**
     * Picks one of several trades at random.
     *
     * <p>1.12.2 adds every trade a career tier lists, where 1.14 picks a couple from a longer set.
     * Without this a mason would offer thirty-two terracotta trades at once.
     */
    private static class OneOf implements EntityVillager.ITradeList {

        private final EntityVillager.ITradeList[] options;

        OneOf(EntityVillager.ITradeList... options) {
            this.options = options;
        }

        @Override
        public void addMerchantRecipe(IMerchant merchant, MerchantRecipeList recipes, Random random) {
            this.options[random.nextInt(this.options.length)].addMerchantRecipe(merchant, recipes, random);
        }
    }
}
