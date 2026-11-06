package com.exiledradio.villagerbackport.migrate;

import com.exiledradio.villagerbackport.ModConfig;
import com.exiledradio.villagerbackport.VillagerBackport;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Items;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.NibbleArray;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.world.ChunkDataEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Worlds that had Future MC, loaded without it.
 *
 * <p>Left alone, every Future MC block in such a world turns to air - a base's stripped logs, its
 * walls, its barrels and what was in them - and every Future MC item vanishes. This turns them into
 * this mod's instead, the first time each chunk loads:
 * <ul>
 *   <li><b>Blocks</b>: Forge reports Future MC's block names as missing, with the numbers the world
 *   saved them under. Those numbers are remembered with the world. Each chunk, as it is read and
 *   before anything sees it, has every block saved under one of them replaced by this mod's
 *   counterpart, in the state Future MC saved it in - read from the recorded table, so facing, axis
 *   and the rest come out exactly as they were (see {@link FutureMcTable}).</li>
 *   <li><b>Contents</b>: a converted block's saved contents - barrel, furnace, lectern, campfire,
 *   sign - are put into this mod's block in the same step, before the chunk joins the world.</li>
 *   <li><b>Items</b>: Future MC's item names are pointed at this mod's items, so stacks anywhere in
 *   the world - chests, players, the ground - load as this mod's.</li>
 * </ul>
 * Each chunk is marked once it has been through this, so blocks placed later that happen to reuse
 * one of the old numbers are never touched. Future MC's blocks with no counterpart here still turn to
 * air, as they would without this mod. Off with {@code compat.convertRemovedFutureMc}.
 */
@Mod.EventBusSubscriber(modid = VillagerBackport.MOD_ID)
public final class FutureMcRemoved {

    private static final String FLAG = "villagerbackport:futuremcGeneration";

    /** Old block number to Future MC block name, from this load's missing names. */
    private static final Map<Integer, String> fresh = new HashMap<Integer, String>();

    /** The numbers to convert in the world now running, and which removal of Future MC they belong to. */
    private static volatile Map<Integer, String> ids = Collections.emptyMap();
    private static volatile int generation;

    private static final Queue<Drop> drops = new ConcurrentLinkedQueue<Drop>();
    private static final AtomicInteger blocks = new AtomicInteger();
    private static final AtomicInteger chunks = new AtomicInteger();
    private static final AtomicInteger contents = new AtomicInteger();

    private FutureMcRemoved() {
    }

    private static boolean active() {
        return ModConfig.compat.convertRemovedFutureMc && !Loader.isModLoaded(FutureMcTable.FUTURE_MC);
    }

    @SubscribeEvent
    public static void missingBlocks(RegistryEvent.MissingMappings<Block> event) {
        if (!active()) {
            return;
        }
        for (RegistryEvent.MissingMappings.Mapping<Block> mapping : event.getAllMappings()) {
            String path = mapping.key.getPath();
            if (FutureMcTable.FUTURE_MC.equals(mapping.key.getNamespace()) && FutureMcTable.block(mapping.key) != null
                    && FutureMcTable.recorded(path)) {
                fresh.put(mapping.id, path);
                // Handled here, chunk by chunk; Forge has nothing to do for it.
                mapping.ignore();
            }
        }
        if (!fresh.isEmpty()) {
        }
    }

    @SubscribeEvent
    public static void missingItems(RegistryEvent.MissingMappings<Item> event) {
        if (!active()) {
            return;
        }
        int remapped = 0;
        for (RegistryEvent.MissingMappings.Mapping<Item> mapping : event.getAllMappings()) {
            Item target = itemFor(mapping.key);
            if (target != null) {
                mapping.remap(target);
                remapped++;
            }
        }
        if (remapped > 0) {
            VillagerBackport.LOGGER.info("This world had Future MC: {} of its items now load as this mod's.", remapped);
        }
    }

    /** @return the item a Future MC item name should load as, whatever its damage, or null */
    private static Item itemFor(ResourceLocation key) {
        if (!FutureMcTable.FUTURE_MC.equals(key.getNamespace())) {
            return null;
        }
        // The dyes (one item, four colors) and the bark woods (1.12.2's logs keep the wood in the
        // damage value): a stand-in that keeps what it was and becomes it in a player's inventory.
        Item standIn = ItemFutureMcStandIn.BY_FUTURE_MC_NAME.get(key.getPath());
        if (standIn != null) {
            return standIn;
        }
        FutureMcTable.Target block = FutureMcTable.block(key);
        if (block != null) {
            Item item = Item.getItemFromBlock(block.block);
            if (item != Items.AIR) {
                return item;
            }
        }
        FutureMcTable.ItemTarget item = FutureMcTable.item(key, 0);
        return item == null ? null : item.item;
    }

    @SubscribeEvent
    public static void onWorldLoad(WorldEvent.Load event) {
        World world = event.getWorld();
        if (world.isRemote || world.provider.getDimension() != 0) {
            return;
        }
        Saved saved = (Saved) world.getMapStorage().getOrLoadData(Saved.class, Saved.NAME);
        if (saved == null) {
            saved = new Saved(Saved.NAME);
            world.getMapStorage().setData(Saved.NAME, saved);
        }
        // A world that was loaded once without Future MC, before this mod could convert it, keeps
        // Future MC's block names as placeholders that Forge restores on every load without
        // reporting them missing again. Their numbers are just as good.
        if (active()) {
            for (Block block : net.minecraftforge.fml.common.registry.ForgeRegistries.BLOCKS) {
                ResourceLocation name = block.getRegistryName();
                if (name != null && FutureMcTable.FUTURE_MC.equals(name.getNamespace())
                        && FutureMcTable.block(name) != null && FutureMcTable.recorded(name.getPath())) {
                    fresh.put(Block.getIdFromBlock(block), name.getPath());
                }
            }
        }
        if (!fresh.isEmpty() && !fresh.equals(saved.ids)) {
            // A new removal: these numbers replace any from an earlier one, and every chunk is
            // looked at again, since Future MC may have been put back and taken out since.
            saved.ids = new HashMap<Integer, String>(fresh);
            saved.generation++;
            saved.markDirty();
        }
        fresh.clear();
        ids = active() ? Collections.unmodifiableMap(new HashMap<Integer, String>(saved.ids)) : Collections.<Integer, String>emptyMap();
        generation = saved.generation;
        if (!ids.isEmpty()) {
            VillagerBackport.LOGGER.info("This world had Future MC: {} of its blocks become this mod's in each chunk the first time it loads.",
                    ids.size());
        }
        blocks.set(0);
        chunks.set(0);
        contents.set(0);
    }

    @SubscribeEvent
    public static void onWorldUnload(WorldEvent.Unload event) {
        if (!event.getWorld().isRemote && event.getWorld().provider.getDimension() == 0) {
            if (chunks.get() > 0) {
                VillagerBackport.LOGGER.info("Converted {} Future MC blocks in {} chunks this session, and moved {} items into them.",
                        blocks.get(), chunks.get(), contents.get());
            }
            ids = Collections.emptyMap();
            drops.clear();
        }
    }

    /**
     * Runs on the server thread once the chunk is read, before it joins the world: only the chunk
     * and its saved data are touched.
     */
    @SubscribeEvent
    public static void onChunkLoad(ChunkDataEvent.Load event) {
        Map<Integer, String> map = ids;
        if (map.isEmpty() || event.getWorld() == null || event.getWorld().isRemote) {
            return;
        }
        NBTTagCompound level = event.getData().getCompoundTag("Level");
        if (level.getInteger(FLAG) >= generation) {
            return;
        }
        Chunk chunk = event.getChunk();
        Set<BlockPos> converted = new HashSet<BlockPos>();
        NBTTagList sections = level.getTagList("Sections", 10);
        for (int s = 0; s < sections.tagCount(); s++) {
            NBTTagCompound section = sections.getCompoundTagAt(s);
            int sy = section.getByte("Y");
            if (sy < 0 || sy >= 16) {
                continue;
            }
            byte[] raw = section.getByteArray("Blocks");
            NibbleArray data = new NibbleArray(section.getByteArray("Data"));
            NibbleArray add = section.hasKey("Add", 7) ? new NibbleArray(section.getByteArray("Add")) : null;
            for (int i = 0; i < raw.length; i++) {
                int id = (raw[i] & 255) | (add == null ? 0 : add.getFromIndex(i) << 8);
                String path = map.get(id);
                if (path == null) {
                    continue;
                }
                IBlockState state = stateFor(path, data.getFromIndex(i));
                if (state == null) {
                    continue;
                }
                ExtendedBlockStorage storage = chunk.getBlockStorageArray()[sy];
                if (storage == Chunk.NULL_BLOCK_STORAGE) {
                    storage = new ExtendedBlockStorage(sy << 4, event.getWorld().provider.hasSkyLight());
                    chunk.getBlockStorageArray()[sy] = storage;
                }
                int x = i & 15;
                int y = (i >> 8) & 15;
                int z = (i >> 4) & 15;
                storage.set(x, y, z, state);
                converted.add(new BlockPos((chunk.x << 4) + x, (sy << 4) + y, (chunk.z << 4) + z));
            }
        }
        if (converted.isEmpty()) {
            return;
        }

        // Future MC's saved block contents. Forge has already read the chunk's block contents by the
        // time this runs, and skipped these, having nothing to read them with; this mod's own block
        // gets a tile entity filled from them now, before the chunk joins the world.
        NBTTagList tiles = level.getTagList("TileEntities", 10);
        int dimension = event.getWorld().provider.getDimension();
        for (int t = 0; t < tiles.tagCount(); t++) {
            NBTTagCompound tile = tiles.getCompoundTagAt(t);
            BlockPos pos = new BlockPos(tile.getInteger("x"), tile.getInteger("y"), tile.getInteger("z"));
            // The block here was Future MC's, so whatever was saved with it was too.
            if (!converted.contains(pos)) {
                continue;
            }
            IBlockState state = chunk.getBlockState(pos);
            TileEntity ours = state.getBlock().hasTileEntity(state) ? state.getBlock().createTileEntity(event.getWorld(), state) : null;
            List<ItemStack> leftovers = new ArrayList<ItemStack>();
            if (ours != null) {
                ours.setPos(pos);
            }
            contents.addAndGet(FutureMcConvert.fill(ours, tile, leftovers));
            if (ours != null) {
                chunk.addTileEntity(pos, ours);
            }
            for (ItemStack stack : leftovers) {
                drops.add(new Drop(dimension, pos, stack));
            }
        }
        chunk.markDirty();
        blocks.addAndGet(converted.size());
        chunks.incrementAndGet();
    }

    private static IBlockState stateFor(String path, int meta) {
        FutureMcTable.Target target = FutureMcTable.block(new ResourceLocation(FutureMcTable.FUTURE_MC, path));
        Map<String, String> props = FutureMcTable.recordedState(path, meta);
        return target == null || props == null ? null : FutureMcConvert.state(target, props).state;
    }

    @SubscribeEvent
    public static void onChunkSave(ChunkDataEvent.Save event) {
        if (!ids.isEmpty()) {
            event.getData().getCompoundTag("Level").setInteger(FLAG, generation);
        }
    }

    /** Anything a converted block couldn't hold, dropped where it stood once the chunk is in the world. */
    @SubscribeEvent
    public static void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.world.isRemote || drops.isEmpty()) {
            return;
        }
        int dimension = event.world.provider.getDimension();
        Iterator<Drop> it = drops.iterator();
        while (it.hasNext()) {
            Drop drop = it.next();
            if (drop.dimension != dimension || !event.world.isBlockLoaded(drop.pos)) {
                continue;
            }
            it.remove();
            InventoryHelper.spawnItemStack(event.world, drop.pos.getX() + 0.5, drop.pos.getY() + 0.5, drop.pos.getZ() + 0.5, drop.stack);
        }
    }

    private static final class Drop {
        final int dimension;
        final BlockPos pos;
        final ItemStack stack;

        Drop(int dimension, BlockPos pos, ItemStack stack) {
            this.dimension = dimension;
            this.pos = pos;
            this.stack = stack;
        }
    }

    /** The old Future MC block numbers, kept with the world. */
    public static final class Saved extends WorldSavedData {
        static final String NAME = "villagerbackport_futuremc";

        Map<Integer, String> ids = new HashMap<Integer, String>();
        int generation;

        public Saved(String name) {
            super(name);
        }

        @Override
        public void readFromNBT(NBTTagCompound tag) {
            this.generation = tag.getInteger("Generation");
            this.ids = new HashMap<Integer, String>();
            NBTTagList list = tag.getTagList("Ids", 10);
            for (int i = 0; i < list.tagCount(); i++) {
                NBTTagCompound entry = list.getCompoundTagAt(i);
                this.ids.put(entry.getInteger("Id"), entry.getString("Name"));
            }
        }

        @Override
        public NBTTagCompound writeToNBT(NBTTagCompound tag) {
            tag.setInteger("Generation", this.generation);
            NBTTagList list = new NBTTagList();
            for (Map.Entry<Integer, String> entry : this.ids.entrySet()) {
                NBTTagCompound e = new NBTTagCompound();
                e.setInteger("Id", entry.getKey());
                e.setString("Name", entry.getValue());
                list.appendTag(e);
            }
            tag.setTag("Ids", list);
            return tag;
        }
    }
}
