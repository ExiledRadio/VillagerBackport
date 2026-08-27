package com.exiledradio.villagerbackport.job;

import net.minecraft.block.BlockBed;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Where the workstations are, kept per chunk so a villager can be asked about a wide area cheaply.
 *
 * <h2>Why an index and not a search</h2>
 * A villager looking for work used to read the world block by block, which puts a hard ceiling on how
 * far it can see: the cost is the volume, so doubling the reach costs eight times as much. That
 * ceiling is what left villagers in a large village jobless - the nearest free workstation was simply
 * beyond what they could afford to look at.
 *
 * <p>1.14 has the same problem and solves it by not searching at all. Its {@code
 * PointOfInterestManager} records where every point of interest is as chunks are generated, and a
 * villager acquiring a job site queries that index rather than the world - which is exactly why 1.14
 * can reach 48 blocks without it costing anything. This is that idea, in the smallest form that
 * answers the one question asked of it: which workstations are in this chunk.
 *
 * <p>With that, reach costs nothing worth measuring. A search at 48 blocks looks at 49 chunks and
 * reads a short list from each, instead of reading a hundred thousand blocks.
 *
 * <h2>How it stays honest</h2>
 * A chunk is scanned the first time anybody asks about it and the answer kept. It is thrown away when
 * the chunk unloads, when a workstation is placed or broken in it, and in any case after a while - so
 * a workstation that appeared by some route this does not watch, a piston or another mod's doing, is
 * picked up within a minute rather than never.
 *
 * <p>Being wrong is survivable in both directions, which is what makes the caching safe. An entry
 * that lingers after its block is gone hands a villager a claim that {@link JobSite#validated} throws
 * out on the next tick, and it looks again. An entry not yet noticed only means a villager waits.
 *
 * <p>Deliberately not a record of <em>claims</em>: those stay on the villagers themselves, for the
 * reasons in {@link JobSite}. This is a cache of what the world contains, which is a fact the world
 * can be re-read for at any time, and so it never needs saving or migrating.
 */
public final class WorkstationIndex {

    /**
     * Ticks before a chunk is read again regardless.
     *
     * <p>Only a safety net: a workstation placed or broken throws its chunk's entry away at once, so
     * this exists for the rare block that changes by some other route. Reading a chunk is tens of
     * thousands of block lookups, and doing that to every chunk around every village every couple of
     * minutes is a real cost for a case that hardly ever happens.
     */
    private static final long REBUILD_AFTER = 12000L;

    private static final class Entry {

        final List<BlockPos> workstations;
        final List<BlockPos> beds;
        final long builtAt;

        Entry(List<BlockPos> workstations, List<BlockPos> beds, long builtAt) {
            this.workstations = workstations;
            this.beds = beds;
            this.builtAt = builtAt;
        }

        List<BlockPos> of(boolean bed) {
            return bed ? this.beds : this.workstations;
        }

        Entry with(boolean bed, List<BlockPos> replacement) {
            return bed
                    ? new Entry(this.workstations, replacement, this.builtAt)
                    : new Entry(replacement, this.beds, this.builtAt);
        }
    }

    /** Dimension to chunk to what is in it. */
    private static final Map<Integer, Map<Long, Entry>> WORLDS = new HashMap<Integer, Map<Long, Entry>>();

    private WorkstationIndex() {
    }

    private static Map<Long, Entry> chunksIn(World world) {
        int dimension = world.provider.getDimension();
        Map<Long, Entry> chunks = WORLDS.get(dimension);

        if (chunks == null) {
            chunks = new HashMap<Long, Entry>();
            WORLDS.put(dimension, chunks);
        }

        return chunks;
    }

    /**
     * @return the workstations in this chunk, or an empty list if it is not loaded
     *
     * <p>Never generates a chunk. Looking for work must not be a reason to build terrain.
     */
    public static List<BlockPos> workstationsIn(World world, int chunkX, int chunkZ) {
        return in(world, chunkX, chunkZ, false);
    }

    /**
     * @return the beds in this chunk, or an empty list if it is not loaded
     *
     * <p>Only the head of each bed. A bed is two blocks and one home, and counting both would give
     * a village twice the beds it has - which, with beds standing in for 1.14's population marker,
     * is twice the villagers and twice the golems.
     */
    public static List<BlockPos> bedsIn(World world, int chunkX, int chunkZ) {
        return in(world, chunkX, chunkZ, true);
    }

    private static List<BlockPos> in(World world, int chunkX, int chunkZ, boolean bed) {
        Chunk chunk = world.getChunkProvider().getLoadedChunk(chunkX, chunkZ);
        if (chunk == null) {
            return Collections.emptyList();
        }

        Map<Long, Entry> chunks = chunksIn(world);
        long key = ChunkPos.asLong(chunkX, chunkZ);
        Entry entry = chunks.get(key);
        long now = world.getTotalWorldTime();

        if (entry != null && now - entry.builtAt < REBUILD_AFTER) {
            return entry.of(bed);
        }

        // Both kinds come out of one pass. Reading a chunk is the expensive part and it is the same
        // read either way, so scanning twice to answer two questions would be paying double.
        entry = scan(chunk, now);
        chunks.put(key, entry);
        return entry.of(bed);
    }

    /** Reads a chunk once. Sections it has nothing in are skipped whole, which is most of a column. */
    private static Entry scan(Chunk chunk, long now) {
        List<BlockPos> found = new ArrayList<BlockPos>();
        List<BlockPos> beds = new ArrayList<BlockPos>();
        int baseX = chunk.x << 4;
        int baseZ = chunk.z << 4;

        ExtendedBlockStorage[] sections = chunk.getBlockStorageArray();

        for (int index = 0; index < sections.length; index++) {
            ExtendedBlockStorage section = sections[index];
            if (section == Chunk.NULL_BLOCK_STORAGE) {
                continue;
            }

            int baseY = index << 4;

            for (int y = 0; y < 16; y++) {
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        IBlockState state = section.get(x, y, z);

                        if (JobSiteRegistry.isWorkstation(state.getBlock())) {
                            found.add(new BlockPos(baseX + x, baseY + y, baseZ + z));
                        } else if (isBedHead(state)) {
                            beds.add(new BlockPos(baseX + x, baseY + y, baseZ + z));
                        }
                    }
                }
            }
        }

        return new Entry(found, beds, now);
    }

    /** @return true if this is the head half of a bed, the half a villager calls home. */
    static boolean isBedHead(IBlockState state) {
        return state.getBlock() instanceof BlockBed
                && state.getValue(BlockBed.PART) == BlockBed.EnumPartType.HEAD;
    }

    /**
     * @return where the head of this bed is, or null if the block is not part of one
     *
     * <h2>Why the foot has to be followed to the head</h2>
     * A bed is two blocks and the index records only the head, but the block a player <em>places</em>
     * is the foot - that is the one the item goes into, and the one the place event reports. Testing
     * the reported block for a head therefore said no to every bed ever laid, and the chunk's entry
     * was left as it was until the rebuild timer came round ten minutes later. From the outside: a
     * bed you just put down that no villager can see and no debug view will draw.
     *
     * <p>Following the foot round to the head fixes both halves of that, and works for breaking too -
     * either half breaks the whole bed, and the head is the record that has to go.
     */
    @Nullable
    static BlockPos bedHeadOf(IBlockState state, BlockPos pos) {
        if (!(state.getBlock() instanceof BlockBed)) {
            return null;
        }

        return state.getValue(BlockBed.PART) == BlockBed.EnumPartType.HEAD
                ? pos
                : pos.offset(state.getValue(BlockBed.FACING));
    }

    private static void forget(World world, int chunkX, int chunkZ) {
        chunksIn(world).remove(ChunkPos.asLong(chunkX, chunkZ));
    }

    /**
     * Applies a single known change to a chunk's entry, rather than re-reading the chunk.
     *
     * <h2>Why the chunk is not simply re-read</h2>
     * It was, and it is the reason a lectern broken and replaced in quick succession could be
     * ignored for the best part of ten minutes.
     *
     * <p>The block events say what changed, but they are cancellable, and a cancellable event fires
     * with the world in the state it would be rolled back to. {@code BreakEvent} in particular runs
     * while the block is still standing. Throwing the entry away and letting the next villager to
     * ask rebuild it therefore re-reads the world at exactly the wrong moment: the rebuilt entry
     * records the block as still there, or the newly placed one as not there yet, and that answer
     * is then cached until the rebuild interval comes round. Whether it happened at all depended on
     * whether a villager searched in the same tick, which is what made the wait unpredictable.
     *
     * <p>Applying the change the event describes needs no reading at all, so it cannot be caught out
     * by when it runs. The list is replaced rather than edited because callers iterate the one they
     * were handed.
     */
    private static void apply(World world, BlockPos pos, boolean present, boolean bed) {
        Map<Long, Entry> chunks = chunksIn(world);
        long key = ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4);

        Entry entry = chunks.get(key);
        if (entry == null || entry.of(bed).contains(pos) == present) {
            // Nothing cached to correct, or it already says the right thing.
            return;
        }

        List<BlockPos> updated = new ArrayList<BlockPos>(entry.of(bed));

        if (present) {
            updated.add(pos);
        } else {
            updated.remove(pos);
        }

        chunks.put(key, entry.with(bed, updated));
    }

    /** Drops everything the index knows, so a config change to the mapping takes effect at once. */
    public static void invalidateAll() {
        WORLDS.clear();
    }

    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event) {
        if (!event.getWorld().isRemote) {
            forget(event.getWorld(), event.getChunk().x, event.getChunk().z);
        }
    }

    @SubscribeEvent
    public static void onWorldUnload(WorldEvent.Unload event) {
        if (!event.getWorld().isRemote) {
            WORLDS.remove(event.getWorld().provider.getDimension());
        }
    }

    /**
     * Both handlers run last, so a placement or a break another mod goes on to cancel is not one we
     * ever hear about - Forge does not deliver a cancelled event unless asked to.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBlockPlaced(BlockEvent.PlaceEvent event) {
        if (event.getWorld().isRemote) {
            return;
        }

        IBlockState placed = event.getPlacedBlock();

        BlockPos bedHead = bedHeadOf(placed, event.getPos());

        if (bedHead != null) {
            apply(event.getWorld(), bedHead, true, true);
        } else if (JobSiteRegistry.isWorkstation(placed.getBlock())) {
            apply(event.getWorld(), event.getPos(), true, false);
            JobSiteClaims.onWorkstationPlaced(event.getWorld(), event.getPos());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBlockBroken(BlockEvent.BreakEvent event) {
        if (event.getWorld().isRemote) {
            return;
        }

        IBlockState broken = event.getState();

        BlockPos bedHead = bedHeadOf(broken, event.getPos());

        if (bedHead != null) {
            apply(event.getWorld(), bedHead, false, true);
        } else if (JobSiteRegistry.isWorkstation(broken.getBlock())) {
            apply(event.getWorld(), event.getPos(), false, false);
            JobSiteClaims.onWorkstationBroken(event.getWorld(), event.getPos());
        }
    }
}
