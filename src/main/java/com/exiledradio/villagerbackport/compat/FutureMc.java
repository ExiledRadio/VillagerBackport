package com.exiledradio.villagerbackport.compat;

import com.exiledradio.villagerbackport.ModConfig;
import com.exiledradio.villagerbackport.VillagerBackport;

import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.IForgeRegistryModifiable;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Living alongside Future MC, which back-ports twelve of the same blocks.
 *
 * <h2>The problem</h2>
 * Both mods add a barrel, a smoker, a loom and so on, under the same names and with 1.14's own
 * recipes. Installed together that is two of everything in the item list, two recipes fighting over
 * each crafting grid - only one of which can win - and villagers that walk past a Future MC barrel
 * because it is not the barrel this mod knows.
 *
 * <h2>What this does about it</h2>
 * <ul>
 * <li><b>Both sets employ villagers.</b> Future MC's blocks are added to the workstation mapping
 * beside this mod's own, so a fisherman takes either barrel.</li>
 * <li><b>One of each pair is hidden.</b> The hidden block keeps existing, so anything already placed
 * in a world carries on working and still drops itself. It loses its crafting recipe, its place in
 * the creative menu and its entry in JEI, and villages stop being built with it.</li>
 * </ul>
 *
 * <h2>Nothing here needs Future MC</h2>
 * Every block is looked up by name. Without Future MC - or with one of its blocks turned off in its
 * own config - the lookup comes back empty and that pair is simply not a pair.
 *
 * <h2>The smithing table</h2>
 * Follows the same setting as the rest. It used to be Future MC's always, while this mod's was
 * decorative. Now this mod's does the netherite upgrade too, and reads Future MC's own smithing
 * recipes as one of its sources (see {@code SmithingUpgrades}), so keeping it loses nothing and adds
 * every other netherite mod's gear. When it is the one kept, Future MC's JEI smithing page is hidden
 * as well, since its table can no longer be crafted.
 */
@Mod.EventBusSubscriber(modid = VillagerBackport.MOD_ID)
public final class FutureMc {

    public static final String MOD_ID = "futuremc";

    /** Which half of each duplicated pair stays visible. */
    public enum Duplicates {
        KEEP_OURS,
        KEEP_FUTURE_MC,
        SHOW_BOTH
    }

    /**
     * Every block both mods add, by the name they share, with the career it employs.
     *
     * <p>The bell employs nobody - it is the meeting point - so it is a duplicate to hide and not a
     * workstation to map.
     */
    private static final String[][] SHARED = {
            {"barrel", "fisherman"},
            {"composter", "farmer"},
            {"cartography_table", "cartographer"},
            {"fletching_table", "fletcher"},
            {"smithing_table", "tool"},
            {"loom", "shepherd"},
            {"blast_furnace", "armor"},
            {"smoker", "butcher"},
            {"grindstone", "weapon"},
            {"stonecutter", "mason"},
            {"bell", null},
            {"lantern", null},
    };

    /** Hidden block to the visible one standing in for it. Built once the registries are full. */
    private static Map<Block, Block> hidden;

    private FutureMc() {
    }

    public static boolean isLoaded() {
        return Loader.isModLoaded(MOD_ID);
    }

    /**
     * @return workstation entries for Future MC's blocks, in the config's own 'block=career' form
     *
     * <p>Added in code rather than to the config's defaults, because a default only reaches an
     * install that has never written the file - and the people who need this are the ones who
     * already have both mods.
     */
    public static List<String> mappings() {
        if (!ModConfig.compat.futureMcWorkstations || !isLoaded()) {
            return Collections.emptyList();
        }

        List<String> entries = new ArrayList<String>();

        for (String[] shared : SHARED) {
            if (shared[1] != null) {
                entries.add(MOD_ID + ":" + shared[0] + "=" + shared[1]);
            }
        }

        return entries;
    }

    /** @return true if this block is the hidden half of a duplicated pair. */
    public static boolean isHidden(Block block) {
        return hidden().containsKey(block);
    }

    /** @return the block to build with in place of this one: its visible twin, or itself. */
    public static Block preferred(Block block) {
        Block twin = hidden().get(block);
        return twin == null ? block : twin;
    }

    /**
     * @return this state as its visible twin, or the state itself if it is not hidden
     *
     * <p>Future MC uses 1.14's property names - facing, lit, open, hanging, attachment - so each
     * property carries across by name. {@code extra} supplies any the hidden block lacks: the 1.14
     * properties a structure file was written with, such as the grindstone's {@code face}.
     */
    public static IBlockState preferred(IBlockState state, @Nullable Map<String, String> extra) {
        Block twin = hidden().get(state.getBlock());
        if (twin == null) {
            return state;
        }
        IBlockState out = twin.getDefaultState();
        Set<String> set = new java.util.HashSet<String>();
        for (Map.Entry<IProperty<?>, Comparable<?>> entry : state.getProperties().entrySet()) {
            String name = entry.getKey().getName();
            out = with(out, name, propertyValue(entry.getKey(), entry.getValue()));
            set.add(name);
        }
        if (extra != null) {
            for (Map.Entry<String, String> entry : extra.entrySet()) {
                if (!set.contains(entry.getKey())) {
                    out = with(out, entry.getKey(), entry.getValue());
                }
            }
        }
        return out;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static String propertyValue(IProperty property, Comparable value) {
        return property.getName(value);
    }

    private static IBlockState with(IBlockState state, String name, String value) {
        for (IProperty<?> property : state.getPropertyKeys()) {
            if (property.getName().equals(name)) {
                return withValue(state, property, value);
            }
        }
        return state;
    }

    private static <T extends Comparable<T>> IBlockState withValue(IBlockState state, IProperty<T> property,
                                                                   String value) {
        com.google.common.base.Optional<T> parsed = property.parseValue(value);
        return parsed.isPresent() ? state.withProperty(property, parsed.get()) : state;
    }

    /** @return the item form of every hidden block, for anything that lists items. */
    public static Set<Item> hiddenItems() {
        Set<Item> items = new LinkedHashSet<Item>();

        for (Block block : hidden().keySet()) {
            Item item = Item.getItemFromBlock(block);
            if (item != null && item != net.minecraft.init.Items.AIR) {
                items.add(item);
            }
        }

        return items;
    }

    private static Map<Block, Block> hidden() {
        if (hidden != null) {
            return hidden;
        }

        Map<Block, Block> pairs = new HashMap<Block, Block>();
        Duplicates mode = ModConfig.compat.futureMcDuplicates;

        if (mode != Duplicates.SHOW_BOTH && isLoaded()) {
            for (String[] shared : SHARED) {
                Block ours = ForgeRegistries.BLOCKS.getValue(
                        new ResourceLocation(VillagerBackport.MOD_ID, shared[0]));
                Block theirs = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(MOD_ID, shared[0]));

                // Only a pair if both halves exist. Future MC can switch each of its blocks off.
                if (ours == null || theirs == null
                        || ours == net.minecraft.init.Blocks.AIR || theirs == net.minecraft.init.Blocks.AIR) {
                    continue;
                }

                if (mode == Duplicates.KEEP_FUTURE_MC) {
                    pairs.put(ours, theirs);
                } else {
                    pairs.put(theirs, ours);
                }
            }
        }

        hidden = pairs;
        return hidden;
    }

    /**
     * Takes the hidden blocks out of the creative menu.
     *
     * <p>Called from init, by which point every mod's blocks are registered and none of them has
     * been listed anywhere yet.
     */
    public static void hideFromCreative() {
        for (Block block : hidden().keySet()) {
            block.setCreativeTab(null);
        }

        if (!hidden().isEmpty()) {
            VillagerBackport.LOGGER.info(
                    "Future MC is installed; hid {} duplicated blocks ({}).",
                    hidden().size(), ModConfig.compat.futureMcDuplicates);
        }
    }

    /**
     * Removes the crafting recipes that make a hidden block.
     *
     * <p>This is what settles the conflict rather than just the look of it: both mods use 1.14's
     * recipes, so each pair is two recipes with identical ingredients, and the grid can only ever
     * produce one of them. Left alone, which one is down to registration order.
     *
     * <p>Lowest priority so every mod registering recipes in this event has done so. Recipes loaded
     * from JSON are already in by the time the event fires at all.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void removeRecipes(RegistryEvent.Register<IRecipe> event) {
        removeRecipes(event.getRegistry());
    }

    /** Again at the end of loading, for recipes a mod added later than the registry event. */
    public static void removeLateRecipes() {
        removeRecipes(ForgeRegistries.RECIPES);
    }

    private static void removeRecipes(IForgeRegistry<IRecipe> registry) {
        Set<Item> items = hiddenItems();

        if (items.isEmpty() || !(registry instanceof IForgeRegistryModifiable)) {
            return;
        }

        List<ResourceLocation> doomed = new ArrayList<ResourceLocation>();

        for (IRecipe recipe : registry) {
            ItemStack output = recipe.getRecipeOutput();

            if (!output.isEmpty() && items.contains(output.getItem())) {
                doomed.add(recipe.getRegistryName());
            }
        }

        for (ResourceLocation name : doomed) {
            ((IForgeRegistryModifiable<IRecipe>) registry).remove(name);
        }

        if (!doomed.isEmpty()) {
            VillagerBackport.LOGGER.info("Removed {} recipes for hidden duplicates: {}", doomed.size(), doomed);
        }
    }
}
