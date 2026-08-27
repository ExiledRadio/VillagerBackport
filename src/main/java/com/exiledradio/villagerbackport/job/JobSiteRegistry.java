package com.exiledradio.villagerbackport.job;

import com.exiledradio.villagerbackport.ModConfig;
import com.exiledradio.villagerbackport.VillagerBackport;
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
import java.util.Map;

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
 * The defaults cover the workstations 1.12.2 already has: brewing stands and cauldrons. The rest of
 * 1.14's are blocks this mod has yet to add, and a career with no workstation registered is not
 * gated at all - it restocks freely. That matters, because gating a career on a block that does not
 * exist would recreate the permanently-stuck villager this mod set out to fix.
 *
 * <p>The mapping is configurable, so a pack can point a career at any block it likes.
 */
public final class JobSiteRegistry {

    /** Career name to the block its villagers work at. */
    private static final Map<String, Block> SITES = new LinkedHashMap<String, Block>();

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

        for (String entry : ModConfig.jobs.workstations) {
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

            SITES.put(parts[1].trim(), block);
            CAREERS_BY_BLOCK.put(block, parts[1].trim());
        }

        loaded = true;
        VillagerBackport.LOGGER.info("Loaded {} villager workstation mappings.", SITES.size());
    }

    /**
     * @return the block this villager works at, or null if its career has none registered
     *
     * <p>Null is the ungated case and is deliberately common right now - most of 1.14's workstations
     * are blocks this mod has not added yet.
     */
    @Nullable
    public static Block siteFor(EntityVillager villager) {
        if (!loaded) {
            load();
        }

        String career = careerName(villager);
        return career == null ? null : SITES.get(career);
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
        Block wanted = siteFor(villager);
        return wanted != null && world.getBlockState(pos).getBlock() == wanted;
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
