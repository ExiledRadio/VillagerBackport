package com.exiledradio.villagerbackport.woodset;

import com.exiledradio.backportcore.creative.CreativePlacement;
import com.exiledradio.villagerbackport.ModConfig;
import com.exiledradio.villagerbackport.VillagerBackport;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.ShapedRecipes;
import net.minecraft.item.crafting.ShapelessRecipes;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.furnace.FurnaceFuelBurnTimeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.registries.IForgeRegistry;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Registers the last blocks 1.14's villages are built from that 1.12.2 lacks: stripped logs and
 * stripped wood, wood trapdoors, buttons and pressure plates, blue ice and sea pickles.
 *
 * <h2>Data-driven like {@code building}</h2>
 * Every block comes from a loop over {@link Wood} or one line below, and goes through the same
 * {@link #register} call, so none can end up with a block but no item, a name but no creative-tab
 * entry, or a block the client has no model for (the client registers models from
 * {@link #blocks()}). The blockstates, models and language entries are written by
 * {@code tools/woodset/generate.py} from the same names, and {@code tools/woodset/check_blockstates.py}
 * proves that every state each block can be in has a variant, a model and a texture.
 *
 * <h2>Recipes in code</h2>
 * As in {@code building}: ordinary shaped and shapeless recipes registered from the loops, so JEI and
 * the recipe book see them like any JSON recipe, without twenty-five JSON files to keep in step.
 *
 * <h2>Three vanilla recipes change</h2>
 * 1.12.2 makes its one trapdoor, button and pressure plate from any planks. Left alone, six spruce
 * planks would match vanilla's trapdoor recipe first, and the spruce trapdoor could never be crafted.
 * So, as 1.14 did, vanilla's three are narrowed to oak planks. In place - each keeps its name, its
 * place in the registry and its recipe-book unlock - never removed: see {@link #narrowToOak}.
 *
 * <h2>Switched off</h2>
 * With {@code villageBlocks.enabled} off nothing here is registered, every lookup returns null, axes
 * strip nothing, vanilla's recipes are untouched, and villages use another mod's blocks or 1.12.2
 * stand-ins as they did before this package existed.
 */
@Mod.EventBusSubscriber(modid = VillagerBackport.MOD_ID)
public final class WoodsetContent {

    /** 1.14's burn time for a wooden button. Trapdoors, plates and logs get 300 from their material. */
    private static final int BUTTON_BURN_TIME = 100;

    private static final Map<Wood, BlockStrippedLog> STRIPPED_LOGS = new EnumMap<Wood, BlockStrippedLog>(Wood.class);
    private static final Map<Wood, BlockStrippedLog> STRIPPED_WOOD = new EnumMap<Wood, BlockStrippedLog>(Wood.class);
    private static final Map<Wood, BlockWoodRedstone.Trapdoor> TRAPDOORS = new EnumMap<Wood, BlockWoodRedstone.Trapdoor>(Wood.class);
    private static final Map<Wood, BlockWoodRedstone.Button> BUTTONS = new EnumMap<Wood, BlockWoodRedstone.Button>(Wood.class);
    private static final Map<Wood, BlockWoodRedstone.PressurePlate> PLATES = new EnumMap<Wood, BlockWoodRedstone.PressurePlate>(Wood.class);

    private static Block blueIce;
    private static Block seaPickle;

    /** Every block this package registers, all with an item, in creative-tab order. */
    private static final List<Block> BLOCKS = new ArrayList<Block>();

    /** 1.14's {@code item.axe.strip}, played by {@link AxeStripping}. */
    public static SoundEvent axeStrip;

    private WoodsetContent() {
    }

    /** @return true if this mod adds these blocks in this game - fixed for the whole session */
    public static boolean enabled() {
        return ModConfig.villageBlocks.enabled;
    }

    // ------------------------------------------------------------------------------------------
    // Lookups for the rest of the mod
    // ------------------------------------------------------------------------------------------

    /** @return the stripped log of a wood, or null when switched off */
    @Nullable
    public static BlockStrippedLog strippedLog(Wood wood) {
        return STRIPPED_LOGS.get(wood);
    }

    /** @return the stripped wood (all-sides stripped) of a wood, or null when switched off */
    @Nullable
    public static BlockStrippedLog strippedWood(Wood wood) {
        return STRIPPED_WOOD.get(wood);
    }

    @Nullable
    public static Block blueIce() {
        return blueIce;
    }

    @Nullable
    public static Block seaPickle() {
        return seaPickle;
    }

    /** @return every block, each with an item, for the client's model registration and tests */
    public static List<Block> blocks() {
        return Collections.unmodifiableList(BLOCKS);
    }

    /**
     * Blocks this mod shares with Future MC, as {ours, theirs}, for {@code FutureMc}'s duplicate
     * hiding.
     *
     * <p>Future MC 0.2.21 registers stripped logs and wood for all six woods, trapdoors for the five,
     * and blue ice, all under 1.14's names and properties. It has no wood buttons or pressure plates
     * (only crimson and warped) and no sea pickle, so those are not pairs. It also leaves its stripped
     * logs and blue ice out when Oceanic Expanse is installed; then those pairs are simply missing,
     * which {@code FutureMc} already allows for.
     */
    public static List<String[]> futureMcPairs() {
        List<String[]> pairs = new ArrayList<String[]>();
        for (Wood wood : Wood.values()) {
            pairs.add(new String[] {"stripped_" + wood.name + "_log", "stripped_" + wood.name + "_log"});
            pairs.add(new String[] {"stripped_" + wood.name + "_wood", "stripped_" + wood.name + "_wood"});
            if (wood.hasOwnRedstone()) {
                pairs.add(new String[] {wood.name + "_trapdoor", wood.name + "_trapdoor"});
            }
        }
        pairs.add(new String[] {"blue_ice", "blue_ice"});
        return pairs;
    }

    // ------------------------------------------------------------------------------------------
    // Registration
    // ------------------------------------------------------------------------------------------

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        if (!enabled()) {
            VillagerBackport.LOGGER.info("1.14 village blocks are off (villageBlocks.enabled).");
            return;
        }
        IForgeRegistry<Block> registry = event.getRegistry();

        for (Wood wood : Wood.values()) {
            BlockStrippedLog log = new BlockStrippedLog(wood, false);
            register(registry, "stripped_" + wood.name + "_log", log, "axe");
            STRIPPED_LOGS.put(wood, log);
        }
        for (Wood wood : Wood.values()) {
            BlockStrippedLog bark = new BlockStrippedLog(wood, true);
            register(registry, "stripped_" + wood.name + "_wood", bark, "axe");
            STRIPPED_WOOD.put(wood, bark);
        }
        for (Wood wood : Wood.values()) {
            if (!wood.hasOwnRedstone()) {
                continue;
            }
            BlockWoodRedstone.Trapdoor trapdoor = new BlockWoodRedstone.Trapdoor(wood);
            register(registry, wood.name + "_trapdoor", trapdoor, "axe");
            TRAPDOORS.put(wood, trapdoor);
        }
        for (Wood wood : Wood.values()) {
            if (!wood.hasOwnRedstone()) {
                continue;
            }
            BlockWoodRedstone.Button button = new BlockWoodRedstone.Button(wood);
            register(registry, wood.name + "_button", button, "axe");
            BUTTONS.put(wood, button);
        }
        for (Wood wood : Wood.values()) {
            if (!wood.hasOwnRedstone()) {
                continue;
            }
            BlockWoodRedstone.PressurePlate plate = new BlockWoodRedstone.PressurePlate(wood);
            register(registry, wood.name + "_pressure_plate", plate, "axe");
            PLATES.put(wood, plate);
        }

        blueIce = new BlockBlueIce();
        register(registry, "blue_ice", blueIce, "pickaxe");
        seaPickle = new BlockSeaPickle();
        register(registry, "sea_pickle", seaPickle, null);
        // 2.4.0 and earlier stored a pickle's water as a property; each saved chunk moves it into real
        // water once, as it loads.
        com.exiledradio.backportcore.waterlog.Waterlogging.migrateStoredProperty(seaPickle, BlockSeaPickle.WATERLOGGED);

        // Vanilla's logs' numbers: catch fire as easily, burn away as fast.
        for (Wood wood : Wood.values()) {
            Blocks.FIRE.setFireInfo(STRIPPED_LOGS.get(wood), 5, 5);
            Blocks.FIRE.setFireInfo(STRIPPED_WOOD.get(wood), 5, 5);
        }

        VillagerBackport.LOGGER.info("Registered {} 1.14 village blocks.", BLOCKS.size());
    }

    private static void register(IForgeRegistry<Block> registry, String name, Block block, @Nullable String tool) {
        block.setRegistryName(new ResourceLocation(VillagerBackport.MOD_ID, name));
        block.setTranslationKey(VillagerBackport.MOD_ID + "." + name);
        // Vanilla's tab, next to the vanilla block of the same name (Backport Core's creative placement).
        CreativePlacement.place(block);
        if (tool != null) {
            // Vanilla's own get these from Forge by material; ours have to ask.
            block.setHarvestLevel(tool, 0);
        }
        registry.register(block);
        BLOCKS.add(block);
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        if (!enabled()) {
            return;
        }
        IForgeRegistry<Item> registry = event.getRegistry();
        for (Block block : BLOCKS) {
            ItemBlock item = new ItemBlock(block);
            item.setRegistryName(block.getRegistryName());
            registry.register(item);
        }

        for (Wood wood : Wood.values()) {
            OreDictionary.registerOre("logWood", STRIPPED_LOGS.get(wood));
            OreDictionary.registerOre("logWood", STRIPPED_WOOD.get(wood));
        }

        // 1.14's composter takes sea pickles at 65%, the same as a flower.
        com.exiledradio.villagerbackport.block.Compostables.register(0.65F, Item.getItemFromBlock(seaPickle));
    }

    @SubscribeEvent
    public static void registerSounds(RegistryEvent.Register<SoundEvent> event) {
        if (!enabled()) {
            return;
        }
        ResourceLocation id = new ResourceLocation(VillagerBackport.MOD_ID, "item.axe.strip");
        axeStrip = new SoundEvent(id).setRegistryName(id);
        event.getRegistry().register(axeStrip);
    }

    /** 1.14's 100-tick wooden button. Vanilla only knows its own oak button by name. */
    @SubscribeEvent
    public static void fuel(FurnaceFuelBurnTimeEvent event) {
        if (!enabled() || event.getItemStack().isEmpty()) {
            return;
        }
        Block block = Block.getBlockFromItem(event.getItemStack().getItem());
        if (block instanceof BlockWoodRedstone.Button && BUTTONS.containsValue(block)) {
            event.setBurnTime(BUTTON_BURN_TIME);
        }
    }

    // ------------------------------------------------------------------------------------------
    // Recipes
    // ------------------------------------------------------------------------------------------

    /**
     * The crafting recipes, smelting, and the three vanilla recipe changes.
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

        for (Wood wood : Wood.values()) {
            ItemStack planks = new ItemStack(Blocks.PLANKS, 4, wood.planksMeta());
            Ingredient log = Ingredient.fromStacks(new ItemStack(STRIPPED_LOGS.get(wood)));
            // 1.14 makes planks from anything in a wood's logs tag, stripped or not.
            shapeless(registry, wood.name + "_planks_from_stripped_log", "planks", planks.copy(), log);
            shapeless(registry, wood.name + "_planks_from_stripped_wood", "planks", planks.copy(),
                    Ingredient.fromStacks(new ItemStack(STRIPPED_WOOD.get(wood))));
            shaped(registry, "stripped_" + wood.name + "_wood", "bark", new ItemStack(STRIPPED_WOOD.get(wood), 3), 2, 2,
                    log, log, log, log);

            if (wood.hasOwnRedstone()) {
                Ingredient p = Ingredient.fromStacks(new ItemStack(Blocks.PLANKS, 1, wood.planksMeta()));
                shaped(registry, wood.name + "_trapdoor", "wooden_trapdoor", new ItemStack(TRAPDOORS.get(wood), 2), 3, 2,
                        p, p, p, p, p, p);
                shapeless(registry, wood.name + "_button", "wooden_button", new ItemStack(BUTTONS.get(wood)), p);
                shaped(registry, wood.name + "_pressure_plate", "wooden_pressure_plate", new ItemStack(PLATES.get(wood)), 2, 1,
                        p, p);
            }
        }

        Ingredient packed = Ingredient.fromStacks(new ItemStack(Blocks.PACKED_ICE));
        shaped(registry, "blue_ice", "", new ItemStack(blueIce), 3, 3,
                packed, packed, packed, packed, packed, packed, packed, packed, packed);

        registerSmelting();
        narrowToOak(registry);
    }

    private static void shaped(IForgeRegistry<IRecipe> registry, String name, String group, ItemStack result,
                               int width, int height, Ingredient... slots) {
        ShapedRecipes recipe = new ShapedRecipes(group, width, height, NonNullList.from(Ingredient.EMPTY, slots), result);
        recipe.setRegistryName(new ResourceLocation(VillagerBackport.MOD_ID, "woodset/" + name));
        registry.register(recipe);
    }

    private static void shapeless(IForgeRegistry<IRecipe> registry, String name, String group, ItemStack result,
                                  Ingredient... slots) {
        ShapelessRecipes recipe = new ShapelessRecipes(group, result, NonNullList.from(Ingredient.EMPTY, slots));
        recipe.setRegistryName(new ResourceLocation(VillagerBackport.MOD_ID, "woodset/" + name));
        registry.register(recipe);
    }

    /** Charcoal from every stripped log and wood, as from logs; lime dye from sea pickles, as 1.14. */
    private static void registerSmelting() {
        FurnaceRecipes furnace = FurnaceRecipes.instance();
        ItemStack charcoal = new ItemStack(Items.COAL, 1, 1);
        for (Wood wood : Wood.values()) {
            furnace.addSmeltingRecipeForBlock(STRIPPED_LOGS.get(wood), charcoal.copy(), 0.15F);
            furnace.addSmeltingRecipeForBlock(STRIPPED_WOOD.get(wood), charcoal.copy(), 0.15F);
        }
        // 1.12.2 already has lime dye: dye metadata 10. 1.14's flora additions are other colors.
        furnace.addSmeltingRecipeForBlock(seaPickle, new ItemStack(Items.DYE, 1, 10), 0.1F);
    }

    /**
     * Narrows vanilla's trapdoor, button and pressure plate recipes to oak planks, so the other five
     * woods' own recipes are reachable.
     *
     * <h2>Edited in place, never removed</h2>
     * Exactly as {@code building} narrows the sign recipe: a shaped recipe's ingredients are a mutable
     * list behind a final field, so only what a slot accepts changes, and the recipe keeps its name
     * and its recipe-book advancement. A recipe another mod has replaced with something that is not a
     * plain shaped recipe is left alone and named in the log.
     */
    private static void narrowToOak(IForgeRegistry<IRecipe> registry) {
        List<String> changed = new ArrayList<String>();
        ItemStack oak = new ItemStack(Blocks.PLANKS, 1, Wood.OAK.planksMeta());
        Ingredient oakOnly = Ingredient.fromStacks(oak);
        for (String name : new String[] {"trapdoor", "wooden_button", "wooden_pressure_plate"}) {
            IRecipe recipe = registry.getValue(new ResourceLocation("minecraft", name));
            if (!(recipe instanceof ShapedRecipes)) {
                VillagerBackport.LOGGER.warn("minecraft:{} is not vanilla's recipe; left as it is.", name);
                continue;
            }
            NonNullList<Ingredient> slots = ((ShapedRecipes) recipe).recipeItems;
            boolean any = false;
            for (int i = 0; i < slots.size(); i++) {
                if (slots.get(i) != Ingredient.EMPTY && slots.get(i).apply(oak)) {
                    slots.set(i, oakOnly);
                    any = true;
                }
            }
            if (any) {
                changed.add(name);
            }
        }
        VillagerBackport.LOGGER.info("Narrowed vanilla recipes to oak planks the way 1.14 did: {}", changed);
    }
}
