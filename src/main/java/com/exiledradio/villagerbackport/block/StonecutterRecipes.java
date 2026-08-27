package com.exiledradio.villagerbackport.block;

import com.exiledradio.villagerbackport.ModConfig;
import com.exiledradio.villagerbackport.VillagerBackport;

import net.minecraft.block.Block;
import net.minecraft.block.BlockFence;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.BlockWall;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.oredict.OreDictionary;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * What a stonecutter can turn each block into.
 *
 * <h2>Why these are derived rather than listed</h2>
 * 1.14 ships ninety-five stonecutting recipes as data files, naming every block by hand. Copying that
 * list would have produced a stonecutter that works on vanilla stone and nothing else - and in 1.12.2
 * not even all of that, since roughly half of 1.14's stone variants (granite stairs, andesite walls,
 * prismarine slabs) do not exist here to be cut into.
 *
 * <p>So the list is worked out from the crafting recipes the game already has. Any recipe that turns
 * some number of one stone block into stairs, slabs, walls, or another whole block of the same family
 * describes a shape a stonecutter should be able to cut. Reading those and inverting them produces
 * 1.12.2's own stone variants, and every modded one alongside them, without this mod knowing which
 * mods are installed - the same reasoning behind the composter's ore dictionary entries.
 *
 * <p>The practical difference: a pack with Chisel, Quark or Rustic gets its blocks in the stonecutter
 * for free, and gets them right, because each mod has already declared what its own blocks are worth.
 *
 * <h2>Counts</h2>
 * Follow 1.14: one stairs or wall per block, two slabs per block, and whole blocks at whatever ratio
 * the crafting recipe used. That makes stairs strictly cheaper to cut than to craft - six blocks for
 * four stairs becomes one for one - which is the entire point of the block in 1.14.
 *
 * <h2>What is refused, and why</h2>
 * Derivation is only safe while it cannot make something out of less than it took. So:
 *
 * <ul>
 *   <li>Inputs must be whole blocks of rock. A recipe taking slabs, stairs or walls is skipped, or
 *       cutting two slabs into a chiseled block would become one slab into a chiseled block - half the
 *       stone for the same result.
 *   <li>Whole-block outputs must divide exactly and never exceed their input. Nine of something into
 *       one of something else is a compression recipe, and reading it backwards would be a
 *       nine-for-one duplicator.
 *   <li>Slabs only double when the crafting recipe already did. A mod that trades one block for one
 *       slab keeps that rate.
 * </ul>
 *
 * <p>The handful of vanilla shapes this cannot reach - the chiseled variants, whose 1.12.2 recipes
 * take slabs - are listed explicitly below, at 1.14's own one-to-one rate.
 */
public final class StonecutterRecipes {

    /** Matches any metadata, for ore dictionary entries that did not name one. */
    private static final int ANY_META = OreDictionary.WILDCARD_VALUE;

    /** A whole-block recipe producing more than this per input is treated as a mistake. */
    private static final int MAX_BLOCK_YIELD = 4;

    private static final Map<CutterKind, StonecutterRecipes> TABLES =
            new EnumMap<CutterKind, StonecutterRecipes>(CutterKind.class);

    private final CutterKind kind;
    private final Map<Long, List<ItemStack>> RESULTS = new HashMap<Long, List<ItemStack>>();

    private boolean built;

    private StonecutterRecipes(CutterKind kind) {
        this.kind = kind;
    }

    /** @return the table for one of the saws, built on first use. */
    public static synchronized StonecutterRecipes of(CutterKind kind) {
        StonecutterRecipes table = TABLES.get(kind);

        if (table == null) {
            table = new StonecutterRecipes(kind);
            TABLES.put(kind, table);
        }

        return table;
    }

    /**
     * Throws the table away so it is rebuilt on next use.
     *
     * <p>Called when a world starts, because the recipe registry a client sees is the server's, sent
     * during login - a table built against the previous server's recipes would be wrong.
     */
    public static synchronized void invalidate() {
        for (StonecutterRecipes table : TABLES.values()) {
            table.forget();
        }
    }

    private void forget() {
        synchronized (this.RESULTS) {
            this.RESULTS.clear();
            this.built = false;
        }
    }

    /**
     * @return everything the given stack can be cut into, in a fixed order
     *
     * <p>Order matters more than it looks: the screen sends back which entry was clicked as an index,
     * so the client and the server have to agree on what is at each position. Sorting by registry name
     * guarantees that even if the two walked the recipe registry differently.
     */
    public List<ItemStack> resultsFor(ItemStack input) {
        if (input.isEmpty()) {
            return Collections.emptyList();
        }

        build();

        synchronized (RESULTS) {
            List<ItemStack> exact = RESULTS.get(key(input.getItem(), input.getMetadata()));
            if (exact != null) {
                return exact;
            }

            List<ItemStack> any = RESULTS.get(key(input.getItem(), ANY_META));
            return any != null ? any : Collections.<ItemStack>emptyList();
        }
    }

    public boolean isInput(ItemStack stack) {
        return !resultsFor(stack).isEmpty();
    }

    private void build() {
        synchronized (RESULTS) {
            if (built) {
                return;
            }

            // Set before the work, not after: anything that queries during the build gets an empty
            // answer rather than recursing into another build.
            built = true;

            long start = System.currentTimeMillis();
            Map<Long, Set<StackKey>> gathered = new HashMap<Long, Set<StackKey>>();

            if (this.kind.derive()) {
                deriveFromCrafting(gathered);
            }

            addConfigured(gathered);
            removeBlocked(gathered);

            for (Map.Entry<Long, Set<StackKey>> entry : gathered.entrySet()) {
                List<StackKey> keys = new ArrayList<StackKey>(entry.getValue());
                Collections.sort(keys, StackKey.BY_NAME);

                List<ItemStack> stacks = new ArrayList<ItemStack>(keys.size());
                for (StackKey key : keys) {
                    stacks.add(key.toStack());
                }
                RESULTS.put(entry.getKey(), Collections.unmodifiableList(stacks));
            }

            VillagerBackport.LOGGER.info("Built {} {} cutter inputs in {} ms.",
                    RESULTS.size(), this.kind.name().toLowerCase(java.util.Locale.ROOT),
                    System.currentTimeMillis() - start);
        }
    }

    /**
     * Reads every crafting recipe and keeps the ones that describe cutting a stone block to shape.
     *
     * <p>A recipe qualifies when every one of its ingredient slots accepts the same item - which is
     * what "made entirely out of one block" looks like once a recipe is reduced to its ingredients,
     * whether it was written as a shape or a shapeless pile.
     */
    private void deriveFromCrafting(Map<Long, Set<StackKey>> out) {
        int recipes = 0;

        for (IRecipe recipe : ForgeRegistries.RECIPES) {
            try {
                if (readRecipe(recipe, out)) {
                    recipes++;
                }
            } catch (Throwable t) {
                // Dynamic and generated recipes can refuse to describe themselves outside of a
                // crafting grid. Skipping one costs a few entries; letting it escape would take the
                // whole table down with it.
                VillagerBackport.LOGGER.debug("Skipped recipe {} while building the cutter list: {}",
                        recipe.getRegistryName(), t.toString());
            }
        }

        VillagerBackport.LOGGER.info("Derived {} cuts from {} crafting recipes.",
                this.kind.name().toLowerCase(java.util.Locale.ROOT), recipes);
    }

    /** @return true if this recipe contributed anything. */
    private boolean readRecipe(IRecipe recipe, Map<Long, Set<StackKey>> out) {
        ItemStack result = recipe.getRecipeOutput();
        if (result.isEmpty() || result.getCount() <= 0) {
            return false;
        }

        Block product = Block.getBlockFromItem(result.getItem());
        if (product == Blocks.AIR) {
            return false;
        }

        List<Ingredient> slots = filledSlots(recipe.getIngredients());
        if (slots.isEmpty()) {
            return false;
        }

        int yield = yieldFor(product, result.getCount(), slots.size());
        if (yield <= 0) {
            return false;
        }

        boolean added = false;
        for (ItemStack candidate : slots.get(0).getMatchingStacks()) {
            if (!isCuttable(candidate) || sameItem(candidate, result)) {
                continue;
            }

            // Every other slot has to take this too, or the recipe is a mixture rather than one block
            // cut to shape - two ingredients means two materials, and a stonecutter takes one.
            if (!allAccept(slots, candidate)) {
                return false;
            }

            ItemStack cut = new ItemStack(result.getItem(), yield, result.getMetadata());
            if (result.hasTagCompound()) {
                cut.setTagCompound(result.getTagCompound().copy());
            }

            added |= add(out, candidate, cut);
        }

        return added;
    }

    private static List<Ingredient> filledSlots(NonNullList<Ingredient> ingredients) {
        List<Ingredient> slots = new ArrayList<Ingredient>();

        for (Ingredient ingredient : ingredients) {
            if (ingredient != Ingredient.EMPTY && ingredient.getMatchingStacks().length > 0) {
                slots.add(ingredient);
            }
        }

        return slots;
    }

    private static boolean allAccept(List<Ingredient> slots, ItemStack candidate) {
        for (int i = 1; i < slots.size(); i++) {
            if (!slots.get(i).apply(candidate)) {
                return false;
            }
        }
        return true;
    }

    /**
     * @return how many of the product one input block cuts into, or 0 if this is not a shape recipe
     *
     * @param made how many the crafting recipe produced
     * @param from how many blocks it consumed
     */
    private static int yieldFor(Block product, int made, int from) {
        if (product instanceof BlockSlab) {
            // A double slab is a whole block wearing a slab's class, and is not what anyone means by
            // cutting one.
            if (((BlockSlab) product).isDouble()) {
                return 0;
            }
            return made >= from * 2 ? 2 : 1;
        }

        if (product instanceof BlockStairs || product instanceof BlockWall) {
            return 1;
        }

        // Anything else is a whole block, and whole blocks have to break even.
        if (made < from || made % from != 0) {
            return 0;
        }

        int yield = made / from;
        return yield <= MAX_BLOCK_YIELD ? yield : 0;
    }

    /** @return true if this is a whole block of rock, and so something a saw could work on. */
    private boolean isCuttable(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        Block block = Block.getBlockFromItem(stack.getItem());
        if (block == Blocks.AIR) {
            return false;
        }

        if (block instanceof BlockSlab || block instanceof BlockStairs
                || block instanceof BlockWall || block instanceof BlockFence) {
            return false;
        }

        // Only whole blocks. A fence gate is made of wood and a saw is not going to cut one up.
        return block.getDefaultState().isFullCube()
                && block.getDefaultState().getMaterial() == this.kind.material;
    }

    private static boolean sameItem(ItemStack a, ItemStack b) {
        return a.getItem() == b.getItem() && a.getMetadata() == b.getMetadata();
    }

    /**
     * The extras from configuration, which is also where the shapes derivation cannot see live.
     *
     * <p>1.12.2 makes each chiseled block out of slabs, so the rule that inputs must be whole blocks -
     * the rule that stops the stonecutter halving the cost of everything made from slabs - excludes
     * them. They ship as configuration defaults rather than as code, since a pack that changes those
     * recipes should be able to change these to match.
     */
    private void addConfigured(Map<Long, Set<StackKey>> out) {
        for (String entry : this.kind.extras()) {
            String[] parts = entry.split("=", 2);
            if (parts.length != 2) {
                VillagerBackport.LOGGER.warn(
                        "Ignoring malformed cutter recipe '{}'; expected 'input=output'.", entry);
                continue;
            }

            ItemStack input = parse(parts[0].trim(), 1);
            ItemStack output = parseWithCount(parts[1].trim());

            if (!input.isEmpty() && !output.isEmpty()) {
                add(out, input, output);
            }
        }
    }

    private void removeBlocked(Map<Long, Set<StackKey>> out) {
        for (String entry : this.kind.blocked()) {
            ItemStack blocked = parse(entry.trim(), 1);

            if (!blocked.isEmpty()) {
                out.remove(key(blocked.getItem(), blocked.getMetadata()));
            }
        }
    }

    /** Parses {@code modid:name[:meta]} with an optional trailing {@code *count}. */
    private static ItemStack parseWithCount(String spec) {
        int star = spec.indexOf('*');
        if (star < 0) {
            return parse(spec, 1);
        }

        int count;
        try {
            count = Integer.parseInt(spec.substring(star + 1).trim());
        } catch (NumberFormatException e) {
            VillagerBackport.LOGGER.warn("Stonecutter output '{}' has an unreadable count.", spec);
            return ItemStack.EMPTY;
        }

        return parse(spec.substring(0, star).trim(), count);
    }

    private static ItemStack parse(String spec, int count) {
        String[] parts = spec.split(":");
        if (parts.length < 2) {
            VillagerBackport.LOGGER.warn("Stonecutter entry '{}' is not a valid item name.", spec);
            return ItemStack.EMPTY;
        }

        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(parts[0], parts[1]));
        if (item == null) {
            // Expected: packs list things from mods they do not always have installed.
            return ItemStack.EMPTY;
        }

        int meta = 0;
        if (parts.length > 2) {
            try {
                meta = Integer.parseInt(parts[2]);
            } catch (NumberFormatException e) {
                VillagerBackport.LOGGER.warn("Stonecutter entry '{}' has an unreadable metadata value.", spec);
                return ItemStack.EMPTY;
            }
        }

        return new ItemStack(item, count, meta);
    }

    private static boolean add(Map<Long, Set<StackKey>> out, ItemStack input, ItemStack output) {
        if (input.isEmpty() || output.isEmpty()) {
            return false;
        }

        long key = key(input.getItem(), input.getMetadata());
        Set<StackKey> results = out.get(key);

        if (results == null) {
            results = new LinkedHashSet<StackKey>();
            out.put(key, results);
        }

        return results.add(new StackKey(output));
    }

    private static long key(Item item, int meta) {
        return ((long) Item.getIdFromItem(item) << 32) | (meta & 0xFFFFFFFFL);
    }

    /**
     * A result, reduced to the three things that make it distinct.
     *
     * <p>{@link ItemStack} has no equality or hashing of its own, so it cannot be put in a set - and a
     * set is what is wanted here, since several crafting recipes often describe the same cut.
     */
    private static final class StackKey {

        static final Comparator<StackKey> BY_NAME = new Comparator<StackKey>() {
            @Override
            public int compare(StackKey a, StackKey b) {
                ResourceLocation left = a.item.getRegistryName();
                ResourceLocation right = b.item.getRegistryName();

                int byName = String.valueOf(left).compareTo(String.valueOf(right));
                if (byName != 0) {
                    return byName;
                }
                return a.meta != b.meta ? a.meta - b.meta : a.count - b.count;
            }
        };

        final Item item;
        final int meta;
        final int count;

        StackKey(ItemStack stack) {
            this.item = stack.getItem();
            this.meta = stack.getMetadata();
            this.count = stack.getCount();
        }

        ItemStack toStack() {
            return new ItemStack(this.item, this.count, this.meta);
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof StackKey)) {
                return false;
            }

            StackKey that = (StackKey) other;
            return this.item == that.item && this.meta == that.meta && this.count == that.count;
        }

        @Override
        public int hashCode() {
            return (Item.getIdFromItem(this.item) * 31 + this.meta) * 31 + this.count;
        }
    }
}
