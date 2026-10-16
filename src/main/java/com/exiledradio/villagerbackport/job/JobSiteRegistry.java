package com.exiledradio.villagerbackport.job;

import com.exiledradio.villagerbackport.ModConfig;
import com.exiledradio.villagerbackport.VillagerBackport;
import com.exiledradio.villagerbackport.compat.FutureMc;
import com.exiledradio.villagerbackport.compat.VillagerAccess;

import net.minecraft.block.Block;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.common.registry.VillagerRegistry;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Which block a villager of a given career goes to work at.
 *
 * <h2>What 1.14 does</h2>
 * Every profession has a point-of-interest block - a librarian works at a lectern, a fisherman at a
 * barrel - and a villager that cannot reach its workstation cannot restock. That is the whole reason
 * workstations matter: they turn restocking from something that happens on a timer into something a
 * village has to be built for.
 *
 * <p>1.12.2 has no point-of-interest system at all, so the mapping lives here and is checked
 * directly against the world.
 *
 * <h2>Keyed on career</h2>
 * As with textures and trade tiers, it is 1.12.2's <em>career</em> that lines up with a 1.14
 * profession - one 1.12.2 profession covers up to four of them, and a farmer and a fisherman do not
 * share a workstation.
 *
 * <h2>Only what exists</h2>
 * An entry naming a block that is not installed is skipped, and a career left with no workstation
 * at all is not gated - it restocks freely. That matters, because gating a career on a block that
 * does not exist would recreate the permanently-stuck villager this mod set out to fix.
 *
 * <h2>More than one block per career</h2>
 * A career may be listed against any number of blocks, and a villager of that career works at
 * whichever of them it reaches first. A block still offers exactly one career, because an
 * unemployed villager arriving at it has to become something. This is what lets another mod's
 * barrel employ a fisherman alongside this mod's own - see {@link FutureMc}.
 *
 * <p>The mapping is configurable, so a pack can point a career at any blocks it likes.
 */
public final class JobSiteRegistry {

    /** Career name to the blocks its villagers work at. */
    private static final Map<String, Set<Block>> SITES = new LinkedHashMap<String, Set<Block>>();

    /** The same mapping the other way round, for working out what job a block offers. */
    private static final Map<Block, String> CAREERS_BY_BLOCK = new LinkedHashMap<Block, String>();

    private static boolean loaded;

    private JobSiteRegistry() {
    }

    /**
     * Parses the configured mapping. Safe to call repeatedly; re-reads on a config change.
     */
    public static void load() {
        SITES.clear();
        CAREERS_BY_BLOCK.clear();

        List<String> entries = new ArrayList<String>();
        Collections.addAll(entries, ModConfig.jobs.workstations);

        // After the configured ones, so a pack that has already mapped one of these blocks to a
        // career of its own choosing keeps its answer.
        entries.addAll(FutureMc.mappings());

        for (String entry : entries) {
            String[] parts = entry.split("=", 2);
            if (parts.length != 2) {
                VillagerBackport.LOGGER.warn(
                        "Ignoring malformed workstation entry '{}'; expected 'modid:block=career'.", entry);
                continue;
            }

            Block block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(parts[0].trim()));
            if (block == null) {
                // Expected and harmless: a pack may map a block from a mod that is not installed.
                VillagerBackport.LOGGER.info(
                        "Workstation block '{}' is not present; the '{}' career will not be gated.",
                        parts[0].trim(), parts[1].trim());
                continue;
            }

            String career = parts[1].trim();

            // A block offers one career. The first line to name it wins, and a later one is not
            // added to the second career either - a villager working at a block that would have
            // given it a different job is a contradiction better refused than half-kept.
            String existing = CAREERS_BY_BLOCK.get(block);
            if (existing != null) {
                if (!existing.equals(career)) {
                    VillagerBackport.LOGGER.warn(
                            "Ignoring workstation entry '{}'; that block already belongs to '{}'.",
                            entry, existing);
                }
                continue;
            }

            Set<Block> blocks = SITES.get(career);
            if (blocks == null) {
                blocks = new LinkedHashSet<Block>();
                SITES.put(career, blocks);
            }

            blocks.add(block);
            CAREERS_BY_BLOCK.put(block, career);
        }

        loaded = true;
        VillagerBackport.LOGGER.info("Loaded {} villager workstations for {} careers.",
                CAREERS_BY_BLOCK.size(), SITES.size());
    }

    /**
     * @return the blocks this villager works at, empty if its career has none registered
     *
     * <p>Empty is the ungated case: a career nobody mapped, or one whose blocks are all missing.
     */
    public static Set<Block> sitesFor(EntityVillager villager) {
        if (!loaded) {
            load();
        }

        String career = careerName(villager);
        Set<Block> blocks = career == null ? null : SITES.get(career);
        return blocks == null ? Collections.<Block>emptySet() : blocks;
    }

    /** @return true if this villager's career has at least one workstation to go to. */
    public static boolean hasSite(EntityVillager villager) {
        return !sitesFor(villager).isEmpty();
    }

    /**
     * @return the career this block offers, or null if it is not a workstation.
     *
     * <p>Used when an unemployed villager claims a block, to work out what job it just took.
     */
    @Nullable
    public static String careerForBlock(Block block) {
        if (!loaded) {
            load();
        }
        return CAREERS_BY_BLOCK.get(block);
    }

    /** @return true if any career works at this block. */
    public static boolean isWorkstation(Block block) {
        return careerForBlock(block) != null;
    }

    /**
     * @return every block some career works at, in the order the mapping lists them
     *
     * <p>This is what village generation draws its workstations from, so the blocks a village is
     * built with and the blocks a villager will take a job at are the same set by construction -
     * generating a workstation nobody can be employed at is not a mistake this can make. A pack that
     * remaps a career therefore also changes what its villages are built with, which is the
     * behaviour worth having.
     */
    public static Collection<Block> allSites() {
        if (!loaded) {
            load();
        }
        return Collections.unmodifiableCollection(new ArrayList<Block>(CAREERS_BY_BLOCK.keySet()));
    }

    /** @return true if the block at this position is the villager's workstation. */
    public static boolean isSiteFor(EntityVillager villager, World world, BlockPos pos) {
        return sitesFor(villager).contains(world.getBlockState(pos).getBlock());
    }

    /**
     * @return the villager's career name, or null before it has picked one.
     *
     * <p>A villager has no career until its trade list is first built, which means an untraded
     * villager has no workstation to seek. That is the correct behaviour rather than a gap: it has
     * nothing to restock either.
     */
    @Nullable
    private static String careerName(EntityVillager villager) {
        try {
            VillagerRegistry.VillagerProfession profession = villager.getProfessionForge();
            int careerId = VillagerAccess.getCareerId(villager);

            if (profession == null || careerId <= 0) {
                return null;
            }

            VillagerRegistry.VillagerCareer career = profession.getCareer(careerId - 1);
            return career == null ? null : career.getName();
        } catch (RuntimeException e) {
            return null;
        }
    }
}
