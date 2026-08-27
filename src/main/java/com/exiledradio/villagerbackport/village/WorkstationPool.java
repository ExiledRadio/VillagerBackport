package com.exiledradio.villagerbackport.village;

import com.exiledradio.villagerbackport.ModConfig;
import com.exiledradio.villagerbackport.job.JobSiteRegistry;

import net.minecraft.block.Block;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Which workstation a generated village building gets.
 *
 * <h2>Drawn from the job mapping, not a list of its own</h2>
 * The pool is every block {@link JobSiteRegistry} knows a career for, minus anything a pack has
 * excluded. Keeping the two together means a village cannot be built with a workstation nobody can
 * be employed at, and a pack that remaps a career gets villages built to match without touching a
 * second setting.
 *
 * <p>The bell is absent for the same reason it is absent from the job mapping: it is 1.14's meeting
 * point, not a job site. It gets its own building - see {@link VillageMeetingPoint}.
 */
public final class WorkstationPool {

    private WorkstationPool() {
    }

    /**
     * @return the blocks village generation may choose from, in the mapping's own order
     *
     * <p>Resolved on each call rather than cached: the mapping is reloaded when the config changes,
     * and generation is far too rare for the cost to matter.
     */
    public static List<Block> blocks() {
        Set<Block> excluded = new HashSet<Block>();

        for (String name : ModConfig.villages.excludedWorkstations) {
            Block block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(name.trim()));
            if (block != null) {
                excluded.add(block);
            }
        }

        List<Block> pool = new ArrayList<Block>();

        for (Block block : JobSiteRegistry.allSites()) {
            if (!excluded.contains(block)) {
                pool.add(block);
            }
        }

        return pool;
    }

    /**
     * @return the pool with each block repeated as many times as its weight
     *
     * <p>Weighting by repetition rather than by running totals: the lists are a dozen entries long
     * and picked from a handful of times per village, so the simple version is the right one.
     */
    public static List<Block> weighted() {
        Map<Block, Integer> weights = new HashMap<Block, Integer>();

        for (String entry : ModConfig.villages.workstationWeights) {
            String[] parts = entry.split("=", 2);
            if (parts.length != 2) {
                continue;
            }

            Block block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(parts[0].trim()));
            if (block == null) {
                continue;
            }

            try {
                weights.put(block, Math.max(1, Math.min(16, Integer.parseInt(parts[1].trim()))));
            } catch (NumberFormatException ignored) {
                // A malformed weight just leaves the block at its default.
            }
        }

        List<Block> pool = new ArrayList<Block>();

        for (Block block : blocks()) {
            Integer weight = weights.get(block);
            int times = weight == null ? 1 : weight;

            for (int i = 0; i < times; i++) {
                pool.add(block);
            }
        }

        return pool;
    }

    /**
     * @return a workstation for a building about to be laid out, or null if the pool is empty
     *
     * <p>Chosen when the piece is created rather than when it is built, because a village's layout is
     * decided in one go and then written to disk - the building may not actually be constructed until
     * a later session. The choice travels in the piece's own NBT, which is why this returns a block
     * to be recorded by name rather than being re-rolled at build time.
     */
    @Nullable
    public static Block pick(Random random) {
        return pick(random, null);
    }

    /**
     * @param besides a block the answer must not be, or null for no restriction
     *
     * <p>Used where the workstation being replaced is itself in the pool - swapping a cauldron for a
     * cauldron is a wasted roll, and at a glance looks like the swap simply not working.
     */
    @Nullable
    public static Block pick(Random random, @Nullable Block besides) {
        List<Block> pool = weighted();

        if (besides != null) {
            List<Block> without = new ArrayList<Block>();
            for (Block block : pool) {
                if (block != besides) {
                    without.add(block);
                }
            }
            pool = without;
        }

        return pool.isEmpty() ? null : pool.get(random.nextInt(pool.size()));
    }

    /** @return the block a recorded name refers to, or null if nothing by that name is installed. */
    @Nullable
    public static Block byName(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        return ForgeRegistries.BLOCKS.getValue(new ResourceLocation(name));
    }

    /** @return the name to record for a block, or an empty string for none. */
    public static String nameOf(@Nullable Block block) {
        ResourceLocation id = block == null ? null : block.getRegistryName();
        return id == null ? "" : id.toString();
    }
}
