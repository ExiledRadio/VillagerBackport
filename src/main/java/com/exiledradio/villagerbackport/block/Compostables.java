package com.exiledradio.villagerbackport.block;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.exiledradio.villagerbackport.ModConfig;
import com.exiledradio.villagerbackport.VillagerBackport;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.oredict.OreDictionary;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * What a composter accepts, and how likely each thing is to raise its level.
 *
 * <h2>1.14's table, minus what 1.12.2 does not have</h2>
 * The chances are 1.14's own, banded at 30%, 50%, 65%, 85% and 100% - roughly "how much plant is in
 * it". Around a dozen of 1.14's entries are items that did not exist in 1.12.2 at all: sweet
 * berries, kelp, seagrass, bamboo, sea pickles, and the flowers added alongside them. Those are
 * simply absent rather than substituted, since inventing an equivalent would be guessing.
 *
 * <p>Several entries are metadata variants here where 1.14 gives each its own item - saplings,
 * leaves, flowers and tall plants are all one block with a damage value - so matching is done on
 * item and metadata rather than item alone.
 */
public final class Compostables {

    /** Any item, regardless of metadata. */
    private static final int ANY_META = -1;

    private static final Map<Long, Float> CHANCES = new HashMap<Long, Float>();

    static {
        // 30% - leaves, saplings and seeds.
        add(0.3F, Blocks.LEAVES, ANY_META);
        add(0.3F, Blocks.LEAVES2, ANY_META);
        add(0.3F, Blocks.SAPLING, ANY_META);
        add(0.3F, Items.BEETROOT_SEEDS);
        add(0.3F, Items.MELON_SEEDS);
        add(0.3F, Items.PUMPKIN_SEEDS);
        add(0.3F, Items.WHEAT_SEEDS);
        add(0.3F, Blocks.TALLGRASS, 1);   // grass

        // 50% - larger plant matter.
        add(0.5F, Blocks.CACTUS, ANY_META);
        add(0.5F, Blocks.REEDS, ANY_META);
        add(0.5F, Items.REEDS);
        add(0.5F, Blocks.VINE, ANY_META);
        add(0.5F, Items.MELON);           // melon slice
        add(0.5F, Blocks.DOUBLE_PLANT, 2);  // tall grass
        add(0.5F, Blocks.DOUBLE_PLANT, 3);  // large fern

        // 65% - crops, flowers and mushrooms.
        add(0.65F, Blocks.WATERLILY, ANY_META);
        add(0.65F, Blocks.PUMPKIN, ANY_META);
        add(0.65F, Blocks.LIT_PUMPKIN, ANY_META);
        add(0.65F, Blocks.MELON_BLOCK, ANY_META);
        add(0.65F, Items.APPLE);
        add(0.65F, Items.BEETROOT);
        add(0.65F, Items.CARROT);
        add(0.65F, Items.DYE, 3);         // cocoa beans
        add(0.65F, Items.POTATO);
        add(0.65F, Items.WHEAT);
        add(0.65F, Blocks.BROWN_MUSHROOM, ANY_META);
        add(0.65F, Blocks.RED_MUSHROOM, ANY_META);
        add(0.65F, Blocks.YELLOW_FLOWER, ANY_META);
        add(0.65F, Blocks.RED_FLOWER, ANY_META);
        add(0.65F, Blocks.TALLGRASS, 2);  // fern
        add(0.65F, Blocks.DOUBLE_PLANT, 0);  // sunflower
        add(0.65F, Blocks.DOUBLE_PLANT, 1);  // lilac
        add(0.65F, Blocks.DOUBLE_PLANT, 4);  // rose bush
        add(0.65F, Blocks.DOUBLE_PLANT, 5);  // peony

        // 85% - processed or bundled plant matter.
        add(0.85F, Blocks.HAY_BLOCK, ANY_META);
        add(0.85F, Blocks.BROWN_MUSHROOM_BLOCK, ANY_META);
        add(0.85F, Blocks.RED_MUSHROOM_BLOCK, ANY_META);
        add(0.85F, Items.BREAD);
        add(0.85F, Items.BAKED_POTATO);
        add(0.85F, Items.COOKIE);

        // 100% - always works.
        add(1.0F, Items.CAKE);
        add(1.0F, Items.PUMPKIN_PIE);
    }

    /**
     * Ore dictionary entries added by configuration, checked when an item is not in the table above.
     *
     * <p>Kept separate rather than expanded into the table because ore dictionary membership is not
     * fixed at load: mods register entries throughout startup, and a few do so lazily. Matching
     * against the dictionary when asked means a late registration still counts.
     */
    private static final List<OreEntry> ORE_ENTRIES = new ArrayList<OreEntry>();

    private static boolean extrasLoaded;

    private Compostables() {
    }

    /**
     * Reads the configured extras. Safe to call repeatedly; re-reads on a config change.
     *
     * <p>This is how other mods' plants get in without being named. An entry like
     * {@code oredict:treeLeaves=0.3} covers every mod that registers its leaves properly - Dynamic
     * Trees, Better Nether and Rustic all do - which is far more robust than a list of item ids that
     * would need extending for each new mod and would break when one renames something.
     */
    public static void loadExtras() {
        ORE_ENTRIES.clear();
        extrasLoaded = true;

        int items = 0;
        for (String entry : ModConfig.workstations.extraCompostables) {
            String[] parts = entry.split("=", 2);
            if (parts.length != 2) {
                VillagerBackport.LOGGER.warn(
                        "Ignoring malformed compostable '{}'; expected 'what=chance'.", entry);
                continue;
            }

            float chance;
            try {
                chance = Float.parseFloat(parts[1].trim());
            } catch (NumberFormatException e) {
                VillagerBackport.LOGGER.warn("Compostable '{}' has an unreadable chance.", entry);
                continue;
            }

            if (chance <= 0.0F) {
                continue;
            }

            String what = parts[0].trim();
            if (what.startsWith("oredict:")) {
                ORE_ENTRIES.add(new OreEntry(what.substring("oredict:".length()), chance));
            } else if (addConfigured(what, chance)) {
                items++;
            }
        }

        VillagerBackport.LOGGER.info("Loaded {} ore dictionary and {} item compostables from config.",
                ORE_ENTRIES.size(), items);
    }

    /** @return true if the item exists and was added. */
    private static boolean addConfigured(String spec, float chance) {
        String[] parts = spec.split(":");
        if (parts.length < 2) {
            VillagerBackport.LOGGER.warn("Compostable '{}' is not a valid item name.", spec);
            return false;
        }

        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(parts[0], parts[1]));
        if (item == null) {
            // Expected: a pack may list items from mods that are not installed.
            return false;
        }

        int meta = ANY_META;
        if (parts.length > 2) {
            try {
                meta = Integer.parseInt(parts[2]);
            } catch (NumberFormatException e) {
                VillagerBackport.LOGGER.warn("Compostable '{}' has an unreadable metadata value.", spec);
                return false;
            }
        }

        add(chance, item, meta);
        return true;
    }

    /** @return the configured chance for any ore dictionary name this stack belongs to. */
    private static float oreChanceFor(ItemStack stack) {
        if (!extrasLoaded) {
            loadExtras();
        }

        float best = 0.0F;
        for (int id : OreDictionary.getOreIDs(stack)) {
            String name = OreDictionary.getOreName(id);
            for (OreEntry entry : ORE_ENTRIES) {
                // Highest wins, so a specific entry can raise a general one rather than fight it.
                if (entry.name.equals(name) && entry.chance > best) {
                    best = entry.chance;
                }
            }
        }
        return best;
    }

    private static class OreEntry {
        final String name;
        final float chance;

        OreEntry(String name, float chance) {
            this.name = name;
            this.chance = chance;
        }
    }

    private static void add(float chance, Item item) {
        add(chance, item, ANY_META);
    }

    private static void add(float chance, Block block, int meta) {
        add(chance, Item.getItemFromBlock(block), meta);
    }

    private static void add(float chance, Item item, int meta) {
        if (item != null && item != Items.AIR) {
            CHANCES.put(key(item, meta), chance);
        }
    }

    /** Item and metadata packed into one value, so both can be looked up at once. */
    private static long key(Item item, int meta) {
        return ((long) Item.getIdFromItem(item) << 32) | (meta & 0xFFFFFFFFL);
    }

    /**
     * @return the chance this stack raises the composter's level, or 0 if it cannot be composted
     *
     * <p>An exact metadata match wins over a wildcard, so a specific variant can be given its own
     * chance while the rest of the block keeps a general one.
     */
    public static float chanceFor(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0.0F;
        }

        Float exact = CHANCES.get(key(stack.getItem(), stack.getMetadata()));
        if (exact != null) {
            return exact;
        }

        Float wildcard = CHANCES.get(key(stack.getItem(), ANY_META));
        if (wildcard != null) {
            return wildcard;
        }

        // Nothing named it directly, so fall back to what it is - which is how other mods' plants
        // qualify without this mod listing them.
        return oreChanceFor(stack);
    }

    public static boolean isCompostable(ItemStack stack) {
        return chanceFor(stack) > 0.0F;
    }
}
