package com.exiledradio.villagerbackport.restock;

import com.exiledradio.villagerbackport.VillagerBackport;
import com.exiledradio.villagerbackport.compat.FutureMc;
import com.exiledradio.villagerbackport.block.ModSounds;
import com.exiledradio.villagerbackport.job.JobSite;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nullable;

/**
 * The noise a villager makes when it works at its job site.
 *
 * <h2>Where this comes from</h2>
 * 1.14 plays a work sound when a villager uses its workstation, one per profession
 * ({@code entity.villager.work_*}): an armorer's blast furnace crackles, a farmer's composter fills,
 * a fletcher whittles at the fletching table, a toolsmith hammers at the smithing table. This mod
 * ships those same sound events with 1.14's own files, volumes and pitches.
 *
 * <p>Picked by the villager's career, as 1.14 picks it by profession. A career 1.14 does not have -
 * another mod's - gets its workstation's own sound if it is one of this mod's, or else the noise the
 * block makes when struck, which is always of the right material.
 */
public final class WorkSounds {

    private WorkSounds() {
    }

    /** Plays the work sound for wherever this villager is standing to work. */
    static void playFor(EntityVillager villager) {
        BlockPos pos = JobSite.validated(villager);
        if (pos == null) {
            return;
        }

        IBlockState state = villager.world.getBlockState(pos);
        SoundEvent sound = forCareer(villager);
        if (sound == null) {
            sound = forBlock(state, villager, pos);
        }

        if (sound != null) {
            villager.world.playSound(null, pos, sound, SoundCategory.BLOCKS, 1.0F, 1.0F);
        }
    }

    /**
     * 1.12.2's career names, which abbreviate three, to 1.14's professions. 1.14 picks the work sound
     * by profession, not by block: a farmer sounds like a composter filling wherever it works.
     */
    private static final java.util.Map<String, String> PROFESSIONS = new java.util.HashMap<String, String>();

    static {
        for (String same : new String[]{"farmer", "fisherman", "shepherd", "fletcher", "librarian", "cartographer",
                "cleric", "butcher", com.exiledradio.villagerbackport.job.MasonCareer.NAME}) {
            PROFESSIONS.put(same, same);
        }
        PROFESSIONS.put("armor", "armorer");
        PROFESSIONS.put("weapon", "weaponsmith");
        PROFESSIONS.put("tool", "toolsmith");
        PROFESSIONS.put("leather", "leatherworker");
    }

    /** @return 1.14's work sound for this villager's career, or null for a career 1.14 does not have */
    @Nullable
    private static SoundEvent forCareer(EntityVillager villager) {
        String name = modernProfession(villager);
        return name == null ? null : ModSounds.WORK.get(name);
    }

    /**
     * @return the 1.14 profession this villager's career is - {@code "armorer"}, {@code "mason"} -
     *         or null for no career, or another mod's career 1.14 does not have
     */
    @Nullable
    public static String modernProfession(EntityVillager villager) {
        net.minecraftforge.fml.common.registry.VillagerRegistry.VillagerCareer career = careerOf(villager);
        return career == null ? null : PROFESSIONS.get(career.getName());
    }

    /** @return this villager's career, or null if it has none */
    @Nullable
    public static net.minecraftforge.fml.common.registry.VillagerRegistry.VillagerCareer careerOf(
            EntityVillager villager) {
        try {
            net.minecraftforge.fml.common.registry.VillagerRegistry.VillagerProfession profession =
                    villager.getProfessionForge();
            int career = com.exiledradio.villagerbackport.compat.VillagerAccess.getCareerId(villager);
            if (profession == null || career <= 0) {
                return null;
            }
            return profession.getCareer(career - 1);
        } catch (RuntimeException e) {
            // An out-of-range career, which a mod reassigning professions can cause.
            return null;
        }
    }

    /** Another mod's career: the workstation's own sound, or its material's. */
    @Nullable
    private static SoundEvent forBlock(IBlockState state, EntityVillager villager, BlockPos pos) {
        ResourceLocation name = state.getBlock().getRegistryName();

        // Future MC names its workstations exactly as this mod and 1.14 do, so its loom can sound like
        // a loom without this knowing anything more about it.
        if (name != null && (VillagerBackport.MOD_ID.equals(name.getNamespace())
                || FutureMc.MOD_ID.equals(name.getNamespace()))) {
            SoundEvent ours = ofOurs(name.getPath());
            if (ours != null) {
                return ours;
            }
        }

        return state.getBlock().getSoundType(state, villager.world, pos, villager).getHitSound();
    }

    /**
     * @return 1.14's own sound for one of this mod's workstations, or null for one that has none
     *
     * <p>Matched on the registered name rather than by holding references to the blocks, so a
     * workstation this mod does not define - a vanilla cauldron, another mod's block someone mapped
     * to a career - simply falls through to the material sound rather than needing a case here.
     */
    @Nullable
    private static SoundEvent ofOurs(String name) {
        if ("lectern".equals(name)) {
            return ModSounds.bookPageTurn;
        }
        if ("grindstone".equals(name)) {
            return ModSounds.grindstoneUse;
        }
        if ("cartography_table".equals(name)) {
            return ModSounds.cartographyTableUse;
        }
        if ("loom".equals(name)) {
            return ModSounds.loomSelectPattern;
        }
        if ("stonecutter".equals(name)) {
            return ModSounds.stonecutterUse;
        }

        return null;
    }
}
