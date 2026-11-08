package com.exiledradio.villagerbackport.migrate;

import com.exiledradio.backportcore.structure.ModernPalette;
import com.exiledradio.backportcore.waterlog.Waterlogging;
import com.exiledradio.villagerbackport.VillagerBackport;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import javax.annotation.Nullable;

/**
 * Sea pickles from worlds saved by Village &amp; Pillage 2.3.0 to 2.5.0.
 *
 * <p>Sea pickles belong to Update Aquatic Backport (Backport Core's ownership table: when two backports
 * need a block, it belongs to the one it fits best), and this mod no longer adds them. Worlds still hold
 * {@code villagerbackport:sea_pickle}, as placed blocks, wet and dry, and as items, and none of them may
 * be lost:
 * <ul>
 *   <li><b>With Update Aquatic installed</b>, the old name is pointed at its sea pickle, block and item
 *   (Forge's {@code MissingMappings}). Both pickles have the same states and metadata, so this is a
 *   rename: each keeps its count, and the water Fluidlogged API keeps beside it stays. A 2.3.0 or 2.4.0
 *   pickle's stored {@code waterlogged} flag becomes real water as its chunk loads, through Update
 *   Aquatic's own migration.</li>
 *   <li><b>Without it</b>, {@link BlockSeaPickleKept} and {@link ItemSeaPickleStandIn} take the old
 *   name, keep the count and the water, and become Update Aquatic's the day it is added.</li>
 * </ul>
 * Whether Update Aquatic's pickle is there is read from the registry itself, at the lowest priority of
 * the registry events, so a version of it that leaves its pickle out still gets the kept ones.
 */
@Mod.EventBusSubscriber(modid = VillagerBackport.MOD_ID)
public final class UpdateAquaticPickles {

    public static final String UPDATE_AQUATIC = "updateaquaticbackport";

    public static final ResourceLocation OLD_PICKLE = new ResourceLocation(VillagerBackport.MOD_ID, "sea_pickle");
    public static final ResourceLocation UA_PICKLE = new ResourceLocation(UPDATE_AQUATIC, "sea_pickle");

    private static Block kept;
    private static Item standIn;

    private UpdateAquaticPickles() {
    }

    /** @return the kept sea pickle block, or null when Update Aquatic provides sea pickles */
    @Nullable
    public static Block kept() {
        return kept;
    }

    /** @return the sea pickle stand-in item, or null when Update Aquatic provides sea pickles */
    @Nullable
    public static Item standIn() {
        return standIn;
    }

    // ------------------------------------------------------------------------------------------
    // Without Update Aquatic: the kept pickle and its stand-in item, under the old name
    // ------------------------------------------------------------------------------------------

    /** Lowest priority, so every other mod's blocks are in first. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        if (event.getRegistry().containsKey(UA_PICKLE)) {
            return;
        }
        BlockSeaPickleKept block = new BlockSeaPickleKept();
        block.setRegistryName(OLD_PICKLE);
        block.setTranslationKey(VillagerBackport.MOD_ID + ".sea_pickle");
        event.getRegistry().register(block);
        kept = block;
        // As 2.5.0 did for its own pickle: a 2.3.0 or 2.4.0 pickle's stored flag becomes real water once.
        Waterlogging.migrateStoredProperty(block, BlockSeaPickleKept.WATERLOGGED);
        // Backport Core reads this mod's blocks for today's structure files; a structure's sea pickle
        // must never become the kept one. Without Update Aquatic it leaves the position as it is.
        ModernPalette.map("minecraft:sea_pickle", props -> (IBlockState) null);
        VillagerBackport.LOGGER.info("Update Aquatic Backport's sea pickle is missing; sea pickles from older worlds are kept as they are until it is added.");
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void registerItems(RegistryEvent.Register<Item> event) {
        if (kept == null || event.getRegistry().containsKey(UA_PICKLE)) {
            return;
        }
        standIn = new ItemSeaPickleStandIn();
        standIn.setRegistryName(OLD_PICKLE);
        event.getRegistry().register(standIn);
    }

    // ------------------------------------------------------------------------------------------
    // With Update Aquatic: the old name, missing, is its sea pickle
    // ------------------------------------------------------------------------------------------

    @SubscribeEvent
    public static void missingBlocks(RegistryEvent.MissingMappings<Block> event) {
        for (RegistryEvent.MissingMappings.Mapping<Block> mapping : event.getAllMappings()) {
            if (!OLD_PICKLE.equals(mapping.key)) {
                continue;
            }
            Block target = block(UA_PICKLE);
            if (target != null) {
                VillagerBackport.LOGGER.info("{} from an older world is now {}.", mapping.key, target.getRegistryName());
                mapping.remap(target);
            } else {
                VillagerBackport.LOGGER.warn("{} is missing and nothing can take it over; placed sea pickles will be lost.", mapping.key);
            }
        }
    }

    @SubscribeEvent
    public static void missingItems(RegistryEvent.MissingMappings<Item> event) {
        for (RegistryEvent.MissingMappings.Mapping<Item> mapping : event.getAllMappings()) {
            if (OLD_PICKLE.equals(mapping.key)) {
                Item target = item(UA_PICKLE);
                if (target != null) {
                    mapping.remap(target);
                }
            }
        }
    }

    @Nullable
    private static Block block(ResourceLocation name) {
        Block block = ForgeRegistries.BLOCKS.containsKey(name) ? ForgeRegistries.BLOCKS.getValue(name) : null;
        return block == null || block == Blocks.AIR ? null : block;
    }

    @Nullable
    private static Item item(ResourceLocation name) {
        Item item = ForgeRegistries.ITEMS.containsKey(name) ? ForgeRegistries.ITEMS.getValue(name) : null;
        return item == null || item == Items.AIR ? null : item;
    }
}
