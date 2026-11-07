package com.exiledradio.villagerbackport.compat;

import com.exiledradio.backportcore.duplicate.DuplicateBlocks;
import com.exiledradio.villagerbackport.ModConfig;
import com.exiledradio.villagerbackport.VillagerBackport;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.Item;
import net.minecraftforge.fml.common.Loader;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
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
 *
 * <h2>Backport Core</h2>
 * The hiding itself - creative menu, recipes, smelting, the visible twin for builders - is Backport
 * Core's {@link DuplicateBlocks}; this class tells it this mod's pairs and how
 * {@code compat.futureMcDuplicates} is set, and keeps the methods the rest of this mod calls.
 */
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
            {"campfire", null},
    };

    private FutureMc() {
    }

    public static boolean isLoaded() {
        return Loader.isModLoaded(MOD_ID);
    }

    /** Tells Backport Core this mod's pairs. From pre-init. */
    public static void register() {
        DuplicateBlocks.register(VillagerBackport.MOD_ID, MOD_ID, FutureMc::keep, FutureMc::pairs);
        // Sweet berries are an item without a block item, so they are not in the block pairs.
        DuplicateBlocks.registerItems(com.exiledradio.villagerbackport.fox.FoxContent::hiddenItems);
    }

    private static DuplicateBlocks.Keep keep() {
        switch (ModConfig.compat.futureMcDuplicates) {
            case KEEP_FUTURE_MC:
                return DuplicateBlocks.Keep.THEIRS;
            case SHOW_BOTH:
                return DuplicateBlocks.Keep.BOTH;
            default:
                return DuplicateBlocks.Keep.OURS;
        }
    }

    /** {our name, Future MC's name} for every block both mods add. */
    private static List<String[]> pairs() {
        // The workstations share names outright; the 1.14 building blocks mostly do, but Future
        // MC calls its standing signs "spruce_standing_sign" where 1.14 says "spruce_sign".
        List<String[]> names = new ArrayList<String[]>();
        for (String[] shared : SHARED) {
            names.add(new String[] {shared[0], shared[0]});
        }
        if (com.exiledradio.villagerbackport.building.BuildingContent.enabled()) {
            names.addAll(com.exiledradio.villagerbackport.building.BuildingContent.futureMcPairs());
        }
        // Stripped logs and wood, the five wood trapdoors and blue ice, all under 1.14's names.
        if (com.exiledradio.villagerbackport.woodset.WoodsetContent.enabled()) {
            names.addAll(com.exiledradio.villagerbackport.woodset.WoodsetContent.futureMcPairs());
        }
        return names;
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
        return DuplicateBlocks.isHidden(block);
    }

    /** @return the block to build with in place of this one: its visible twin, or itself. */
    public static Block preferred(Block block) {
        return DuplicateBlocks.preferred(block);
    }

    /**
     * @return this state as its visible twin, or the state itself if it is not hidden
     *
     * <p>Future MC uses 1.14's property names - facing, lit, open, hanging, attachment - so each
     * property carries across by name. {@code extra} supplies any the hidden block lacks: the 1.14
     * properties a structure file was written with, such as the grindstone's {@code face}.
     */
    public static IBlockState preferred(IBlockState state, @Nullable Map<String, String> extra) {
        return DuplicateBlocks.preferred(state, extra);
    }

    /** @return the item form of every hidden block, for anything that lists items. */
    public static Set<Item> hiddenItems() {
        return DuplicateBlocks.hiddenItems();
    }

    /** Again at the end of this mod's post-init, for recipes a mod added after the registry event. */
    public static void removeLateRecipes() {
        DuplicateBlocks.removeLateRecipes();
    }
}
