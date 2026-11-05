package com.exiledradio.villagerbackport.building;

import com.exiledradio.villagerbackport.ModConfig;
import com.exiledradio.villagerbackport.ModCreativeTab;
import com.exiledradio.villagerbackport.VillagerBackport;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemSlab;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.ShapedRecipes;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.registries.IForgeRegistry;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Registers 1.14's building blocks from {@link BuildingTable}: blocks, items, recipes, smelting and
 * ore dictionary names, plus the five wood signs.
 *
 * <h2>One path for all of them</h2>
 * Every row of the table goes through the same few lines below, so a row cannot end up with a block
 * but no item, or an item but no recipe. Stonecutter entries are not registered here at all: the
 * stonecutter reads the crafting recipes this class registers and works its cuts out from them (see
 * {@code StonecutterRecipes}), which is also how other mods' slabs reach it.
 *
 * <h2>Recipes in code</h2>
 * Registered from the table rather than shipped as about sixty JSON files, for the same reason the
 * blocks are: a JSON file per recipe is a JSON file to forget. They are ordinary shaped recipes, so
 * JEI, crafting books and recipe-reading mods see them like any other.
 *
 * <h2>Seven vanilla recipes change</h2>
 * 1.14 split seven of 1.12.2's recipes, and leaving them alone would leave two recipes on one
 * crafting pattern, of which the game only ever uses the first registered - vanilla's. See
 * {@code building.vanillaRecipeChanges} for the list. They are narrowed in place, keeping their
 * names, so recipe-book advancements and pack scripts that name them still find them.
 *
 * <h2>Switched off</h2>
 * With {@code building.enabled} off nothing here is registered, every field stays empty and villages
 * keep their 1.12.2 stand-ins.
 */
@Mod.EventBusSubscriber(modid = VillagerBackport.MOD_ID)
public final class BuildingContent {

    /** Group names for the recipe book, matching 1.14's where it has one. */
    private static final String SIGN_GROUP = "sign";

    /** The full blocks by name, so a row can be built from one. */
    private static final Map<String, Block> FULL = new LinkedHashMap<String, Block>();

    /** Each row's blocks, by shape. Doubles are not here; see {@link #DOUBLES}. */
    private static final Map<String, Map<BuildingTable.Shape, Block>> SHAPED =
            new LinkedHashMap<String, Map<BuildingTable.Shape, Block>>();

    /** Half slab to its double, for the item that joins them. */
    private static final Map<Block, Block> DOUBLES = new LinkedHashMap<Block, Block>();

    private static final Map<SignWood, BlockWoodSign.Standing> STANDING = new EnumMap<SignWood, BlockWoodSign.Standing>(SignWood.class);
    private static final Map<SignWood, BlockWoodSign.Wall> WALL = new EnumMap<SignWood, BlockWoodSign.Wall>(SignWood.class);
    private static final Map<SignWood, ItemWoodSign> SIGN_ITEMS = new EnumMap<SignWood, ItemWoodSign>(SignWood.class);

    /** Every block with an item of its own, in table order, for item registration and models. */
    private static final List<Block> WITH_ITEMS = new ArrayList<Block>();

    private BuildingContent() {
    }

    /** @return true if this mod adds the building blocks in this game - fixed for the whole session */
    public static boolean enabled() {
        return ModConfig.building.enabled;
    }

    // ------------------------------------------------------------------------------------------
    // Lookups for the rest of the mod
    // ------------------------------------------------------------------------------------------

    /** @return the standing sign of a wood; only call once blocks are registered and enabled */
    static BlockWoodSign.Standing standingSign(SignWood wood) {
        return STANDING.get(wood);
    }

    static BlockWoodSign.Wall wallSign(SignWood wood) {
        return WALL.get(wood);
    }

    /** @return the sign item of a wood, or air before items are registered */
    static Item signItem(SignWood wood) {
        Item item = SIGN_ITEMS.get(wood);
        return item != null ? item : Items.AIR;
    }

    /** @return every block with an item, for the client's model registration */
    public static List<Block> blocksWithItems() {
        return Collections.unmodifiableList(WITH_ITEMS);
    }

    /** @return every slab block, half and double, for the client's state mapper */
    public static List<Block> slabs() {
        List<Block> out = new ArrayList<Block>();
        for (Map.Entry<Block, Block> entry : DOUBLES.entrySet()) {
            out.add(entry.getKey());
            out.add(entry.getValue());
        }
        return out;
    }

    public static Map<SignWood, BlockWoodSign.Standing> standingSigns() {
        return Collections.unmodifiableMap(STANDING);
    }

    public static Map<SignWood, BlockWoodSign.Wall> wallSigns() {
        return Collections.unmodifiableMap(WALL);
    }

    public static Map<SignWood, ItemWoodSign> signItems() {
        return Collections.unmodifiableMap(SIGN_ITEMS);
    }

    /**
     * Blocks this mod shares with Future MC under different names, as {ours, theirs}.
     *
     * <p>Future MC 0.2.21 registers all twelve of 1.14's new walls and the four smooth blocks under
     * the same names as this mod; those are paired by {@code FutureMc} directly. Its signs are named
     * {@code spruce_standing_sign} where 1.14 says {@code spruce_sign}. Its slabs and stairs have
     * models in its jar but never register, so there is nothing to pair them with.
     */
    public static List<String[]> futureMcPairs() {
        List<String[]> pairs = new ArrayList<String[]>();
        for (BuildingTable.Row row : BuildingTable.ROWS) {
            if (row.has(BuildingTable.Shape.WALL)) {
                pairs.add(new String[] {row.name + "_wall", row.name + "_wall"});
            }
        }
        for (BuildingTable.Full full : BuildingTable.FULL_BLOCKS) {
            pairs.add(new String[] {full.name, full.name});
        }
        for (SignWood wood : SignWood.values()) {
            pairs.add(new String[] {wood.name + "_sign", wood.name + "_standing_sign"});
            pairs.add(new String[] {wood.name + "_wall_sign", wood.name + "_wall_sign"});
        }
        return pairs;
    }

    // ------------------------------------------------------------------------------------------
    // Registration
    // ------------------------------------------------------------------------------------------

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        if (!enabled()) {
            VillagerBackport.LOGGER.info("1.14 building blocks are off (building.enabled).");
            return;
        }
        IForgeRegistry<Block> registry = event.getRegistry();

        for (BuildingTable.Full full : BuildingTable.FULL_BLOCKS) {
            Block block = new BlockSmoothStone(full.tint);
            register(registry, full.name, block, true);
            FULL.put(VillagerBackport.MOD_ID + ":" + full.name, block);
        }

        for (BuildingTable.Row row : BuildingTable.ROWS) {
            IBlockState base = baseState(registry, row);
            if (base == null) {
                VillagerBackport.LOGGER.error("Building block row '{}' names a missing base block {}; skipped.",
                        row.name, row.base);
                continue;
            }
            Map<BuildingTable.Shape, Block> shapes = new EnumMap<BuildingTable.Shape, Block>(BuildingTable.Shape.class);

            if (row.has(BuildingTable.Shape.SLAB)) {
                BlockBuildingSlab.Half half = new BlockBuildingSlab.Half(base.getBlock(), base);
                BlockBuildingSlab.Double full = new BlockBuildingSlab.Double(base.getBlock(), base, half);
                register(registry, row.name + "_slab", half, true);
                register(registry, row.name + "_double_slab", full, false);
                DOUBLES.put(half, full);
                shapes.put(BuildingTable.Shape.SLAB, half);
            }
            if (row.has(BuildingTable.Shape.STAIRS)) {
                Block stairs = new BlockBuildingStairs(base);
                register(registry, row.name + "_stairs", stairs, true);
                shapes.put(BuildingTable.Shape.STAIRS, stairs);
            }
            if (row.has(BuildingTable.Shape.WALL)) {
                Block wall = new BlockBuildingWall(base.getBlock());
                register(registry, row.name + "_wall", wall, true);
                shapes.put(BuildingTable.Shape.WALL, wall);
            }
            SHAPED.put(row.name, shapes);
        }

        for (SignWood wood : SignWood.values()) {
            BlockWoodSign.Standing standing = new BlockWoodSign.Standing(wood);
            BlockWoodSign.Wall wall = new BlockWoodSign.Wall(wood);
            register(registry, wood.name + "_sign", standing, false);
            register(registry, wood.name + "_wall_sign", wall, false);
            // Neither block is in a creative tab: the item is, as with vanilla's sign.
            standing.setCreativeTab(null);
            wall.setCreativeTab(null);
            STANDING.put(wood, standing);
            WALL.put(wood, wall);
        }

        GameRegistry.registerTileEntity(TileEntityWoodSign.class,
                new ResourceLocation(VillagerBackport.MOD_ID, "wood_sign"));

        VillagerBackport.LOGGER.info("Registered {} building blocks and {} wood signs.",
                WITH_ITEMS.size() + DOUBLES.size(), STANDING.size());
    }

    private static void register(IForgeRegistry<Block> registry, String name, Block block, boolean hasItem) {
        block.setRegistryName(new ResourceLocation(VillagerBackport.MOD_ID, name));
        block.setTranslationKey(VillagerBackport.MOD_ID + "." + name);
        block.setCreativeTab(ModCreativeTab.TAB);
        if (block.getDefaultState().getMaterial() == net.minecraft.block.material.Material.ROCK) {
            // Stone needs a pickaxe to drop; vanilla's own get this from Forge, ours have to ask.
            block.setHarvestLevel("pickaxe", 0);
        }
        registry.register(block);
        if (hasItem) {
            WITH_ITEMS.add(block);
        }
    }

    @Nullable
    @SuppressWarnings("deprecation")
    private static IBlockState baseState(IForgeRegistry<Block> registry, BuildingTable.Row row) {
        Block block = FULL.get(row.base);
        if (block == null) {
            block = registry.getValue(new ResourceLocation(row.base));
        }
        if (block == null || block == Blocks.AIR) {
            return null;
        }
        return block.getStateFromMeta(row.baseMeta);
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        if (!enabled()) {
            return;
        }
        IForgeRegistry<Item> registry = event.getRegistry();

        for (Block block : WITH_ITEMS) {
            ItemBlock item;
            Block full = DOUBLES.get(block);
            if (full != null) {
                item = new ItemSlab(block, (BlockBuildingSlab) block, (BlockBuildingSlab) full);
            } else {
                item = new ItemBlock(block);
            }
            item.setRegistryName(block.getRegistryName());
            registry.register(item);
        }

        for (SignWood wood : SignWood.values()) {
            ItemWoodSign item = new ItemWoodSign(wood);
            item.setRegistryName(new ResourceLocation(VillagerBackport.MOD_ID, wood.name + "_sign"));
            item.setTranslationKey(VillagerBackport.MOD_ID + "." + wood.name + "_sign");
            item.setCreativeTab(ModCreativeTab.TAB);
            registry.register(item);
            SIGN_ITEMS.put(wood, item);
        }

        registerOres();
    }

    /**
     * Ore dictionary names, following the conventions Forge set for vanilla's own entries.
     *
     * <p>The smooth blocks join the families they belong to - smooth sandstone is {@code sandstone}
     * to any recipe that asks for sandstone, as vanilla's three sandstones already are. The shapes get
     * the generic {@code slab}, {@code stair} and {@code wall} names packs use for "any one of these".
     */
    private static void registerOres() {
        Block smoothStone = FULL.get(VillagerBackport.MOD_ID + ":smooth_stone");
        OreDictionary.registerOre("stoneSmooth", smoothStone);
        OreDictionary.registerOre("sandstone", FULL.get(VillagerBackport.MOD_ID + ":smooth_sandstone"));
        OreDictionary.registerOre("sandstone", FULL.get(VillagerBackport.MOD_ID + ":smooth_red_sandstone"));
        OreDictionary.registerOre("blockQuartz", FULL.get(VillagerBackport.MOD_ID + ":smooth_quartz"));

        for (Map<BuildingTable.Shape, Block> shapes : SHAPED.values()) {
            for (Map.Entry<BuildingTable.Shape, Block> entry : shapes.entrySet()) {
                switch (entry.getKey()) {
                    case SLAB:
                        OreDictionary.registerOre("slab", entry.getValue());
                        break;
                    case STAIRS:
                        OreDictionary.registerOre("stair", entry.getValue());
                        break;
                    default:
                        OreDictionary.registerOre("wall", entry.getValue());
                        break;
                }
            }
        }
        for (ItemWoodSign sign : SIGN_ITEMS.values()) {
            OreDictionary.registerOre("sign", sign);
        }
    }

    /**
     * The crafting recipes, smelting, and the vanilla recipe changes.
     *
     * <p>Normal priority, so it runs before {@code FutureMc}'s lowest-priority pass that removes
     * recipes for hidden duplicates - which then sees these like any other mod's.
     */
    @SubscribeEvent
    public static void registerRecipes(RegistryEvent.Register<IRecipe> event) {
        if (!enabled()) {
            return;
        }
        IForgeRegistry<IRecipe> registry = event.getRegistry();

        for (BuildingTable.Row row : BuildingTable.ROWS) {
            Map<BuildingTable.Shape, Block> shapes = SHAPED.get(row.name);
            if (shapes == null) {
                continue;
            }
            ItemStack base = baseStack(row);
            Ingredient in = Ingredient.fromStacks(base);

            Block slab = shapes.get(BuildingTable.Shape.SLAB);
            if (slab != null) {
                shaped(registry, row.name + "_slab", "", new ItemStack(slab, 6), 3, 1, in, in, in);
            }
            Block stairs = shapes.get(BuildingTable.Shape.STAIRS);
            if (stairs != null) {
                shaped(registry, row.name + "_stairs", "", new ItemStack(stairs, 4), 3, 3,
                        in, Ingredient.EMPTY, Ingredient.EMPTY,
                        in, in, Ingredient.EMPTY,
                        in, in, in);
            }
            Block wall = shapes.get(BuildingTable.Shape.WALL);
            if (wall != null) {
                shaped(registry, row.name + "_wall", "", new ItemStack(wall, 6), 3, 2, in, in, in, in, in, in);
            }
        }

        Ingredient stick = Ingredient.fromItem(Items.STICK);
        for (SignWood wood : SignWood.values()) {
            Ingredient planks = Ingredient.fromStacks(new ItemStack(Blocks.PLANKS, 1, wood.planksMeta));
            shaped(registry, wood.name + "_sign", SIGN_GROUP, new ItemStack(SIGN_ITEMS.get(wood), 3), 3, 3,
                    planks, planks, planks, planks, planks, planks, Ingredient.EMPTY, stick, Ingredient.EMPTY);
        }

        registerSmelting();

        if (ModConfig.building.vanillaRecipeChanges) {
            changeVanillaRecipes(registry);
        }
    }

    private static ItemStack baseStack(BuildingTable.Row row) {
        Block full = FULL.get(row.base);
        if (full != null) {
            return new ItemStack(full);
        }
        Block block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(row.base));
        return new ItemStack(block, 1, row.baseMeta);
    }

    private static void shaped(IForgeRegistry<IRecipe> registry, String name, String group, ItemStack result,
                               int width, int height, Ingredient... slots) {
        NonNullList<Ingredient> ingredients = NonNullList.from(Ingredient.EMPTY, slots);
        ShapedRecipes recipe = new ShapedRecipes(group, width, height, ingredients, result);
        recipe.setRegistryName(new ResourceLocation(VillagerBackport.MOD_ID, "building/" + name));
        registry.register(recipe);
    }

    /**
     * 1.14's smelting additions: the four smooth blocks, and cracked stone bricks.
     *
     * <p>Each takes only the plain form of its source - 1.14 smelts sandstone into smooth sandstone,
     * not chiseled or cut sandstone - so the input is pinned to metadata 0.
     */
    private static void registerSmelting() {
        FurnaceRecipes furnace = FurnaceRecipes.instance();
        for (BuildingTable.Full full : BuildingTable.FULL_BLOCKS) {
            Block from = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(full.smeltedFrom));
            Block to = FULL.get(VillagerBackport.MOD_ID + ":" + full.name);
            if (from != null && from != Blocks.AIR && to != null) {
                furnace.addSmeltingRecipe(new ItemStack(from, 1, full.smeltedMeta), new ItemStack(to), 0.1F);
            }
        }
        // Not new as a block, only as a recipe: 1.12.2 has cracked stone bricks but no way to make them.
        furnace.addSmeltingRecipe(new ItemStack(Blocks.STONEBRICK, 1, 0), new ItemStack(Blocks.STONEBRICK, 1, 2), 0.1F);
    }

    /**
     * Narrows the vanilla recipes 1.14 split, so each new block's recipe is reachable.
     *
     * <h2>Edited in place</h2>
     * The ingredient list of a shaped recipe is a mutable list behind a final field, so each recipe
     * keeps its own name, its place in the registry and its recipe-book advancement, and only what
     * one of its slots accepts changes. Removing and re-registering them was tried first, and cost
     * vanilla's recipe-unlocking advancements, which name the recipes they unlock.
     *
     * <p>A recipe another mod has already replaced with something that is not a plain shaped recipe
     * is left alone and named in the log; the pack has its own idea about it.
     */
    private static void changeVanillaRecipes(IForgeRegistry<IRecipe> registry) {
        List<String> changed = new ArrayList<String>();
        Block smoothStone = FULL.get(VillagerBackport.MOD_ID + ":smooth_stone");

        // Oak only: the other five woods make their own sign.
        narrow(registry, "sign", new ItemStack(Blocks.PLANKS, 1, 0),
                Ingredient.fromStacks(new ItemStack(Blocks.PLANKS, 1, 0)), changed);
        // 1.12.2's "Stone Slab" is 1.14's smooth stone slab, made from smooth stone; stone makes
        // 1.14's new plain stone slab.
        if (smoothStone != null) {
            narrow(registry, "stone_slab", new ItemStack(Blocks.STONE, 1, 0),
                    Ingredient.fromStacks(new ItemStack(smoothStone)), changed);
        }
        // Plain and chiseled only: cut sandstone makes the cut sandstone slab.
        narrow(registry, "sandstone_slab", new ItemStack(Blocks.SANDSTONE, 1, 0),
                Ingredient.fromStacks(new ItemStack(Blocks.SANDSTONE, 1, 0), new ItemStack(Blocks.SANDSTONE, 1, 1)), changed);
        narrow(registry, "red_sandstone_slab", new ItemStack(Blocks.RED_SANDSTONE, 1, 0),
                Ingredient.fromStacks(new ItemStack(Blocks.RED_SANDSTONE, 1, 0), new ItemStack(Blocks.RED_SANDSTONE, 1, 1)), changed);
        // Plain stone bricks only: mossy stone bricks make their own slab and stairs.
        Ingredient bricks = Ingredient.fromStacks(new ItemStack(Blocks.STONEBRICK, 1, 0));
        narrow(registry, "stone_brick_slab", new ItemStack(Blocks.STONEBRICK, 1, 0), bricks, changed);
        narrow(registry, "stone_brick_stairs", new ItemStack(Blocks.STONEBRICK, 1, 0), bricks, changed);

        // Six nether brick blocks is now the nether brick wall. 1.14 made the fence out of four
        // blocks with two nether brick items up the middle, the same shape as every wooden fence.
        IRecipe fence = registry.getValue(new ResourceLocation("minecraft", "nether_brick_fence"));
        if (fence instanceof ShapedRecipes && ((ShapedRecipes) fence).recipeWidth == 3
                && ((ShapedRecipes) fence).recipeHeight == 2) {
            NonNullList<Ingredient> slots = ((ShapedRecipes) fence).recipeItems;
            Ingredient item = Ingredient.fromItem(Items.NETHERBRICK);
            slots.set(1, item);
            slots.set(4, item);
            changed.add("nether_brick_fence");
        } else {
            VillagerBackport.LOGGER.warn("minecraft:nether_brick_fence is not vanilla's recipe; left as it is.");
        }

        VillagerBackport.LOGGER.info("Changed vanilla recipes the way 1.14 did: {}", changed);
    }

    /**
     * Replaces every slot of a vanilla shaped recipe that accepts {@code accepts} with
     * {@code ingredient}.
     */
    private static void narrow(IForgeRegistry<IRecipe> registry, String name, ItemStack accepts, Ingredient ingredient,
                               List<String> changed) {
        IRecipe recipe = registry.getValue(new ResourceLocation("minecraft", name));
        if (!(recipe instanceof ShapedRecipes)) {
            VillagerBackport.LOGGER.warn("minecraft:{} is not vanilla's recipe; left as it is.", name);
            return;
        }
        NonNullList<Ingredient> slots = ((ShapedRecipes) recipe).recipeItems;
        boolean any = false;
        for (int i = 0; i < slots.size(); i++) {
            if (slots.get(i) != Ingredient.EMPTY && slots.get(i).apply(accepts)) {
                slots.set(i, ingredient);
                any = true;
            }
        }
        if (any) {
            changed.add(name);
        }
    }
}
