package com.exiledradio.villagerbackport.restock;

import com.exiledradio.villagerbackport.VillagerBackport;
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
 * 1.14 plays a work sound when a villager uses its workstation, and it is per profession: a
 * librarian turns a page, a toolsmith runs a grindstone, a cartographer draws. Those are separate
 * sound assets 1.12.2 has no equivalent of, but the sounds themselves are the workstation's - and
 * this mod already ships 1.14's workstation sounds, because it ships the workstations.
 *
 * <p>So the sound is taken from the block the villager is working at rather than from its
 * profession. It arrives at the same answer by the shorter route: the villager at the lectern makes
 * the lectern's noise, and a profession this mod never gave a workstation to cannot reach here at
 * all.
 *
 * <p>Anything without a sound of its own falls back to the noise the block makes when it is struck,
 * which every block defines and which is always made of the right material - a wooden tap at a
 * fletching table, stone at a mason's, metal at a cauldron.
 */
final class WorkSounds {

    private WorkSounds() {
    }

    /** Plays the work sound for wherever this villager is standing to work. */
    static void playFor(EntityVillager villager) {
        BlockPos pos = JobSite.validated(villager);
        if (pos == null) {
            return;
        }

        IBlockState state = villager.world.getBlockState(pos);
        SoundEvent sound = forBlock(state, villager, pos);

        if (sound != null) {
            villager.world.playSound(null, pos, sound, SoundCategory.BLOCKS, 1.0F, 1.0F);
        }
    }

    @Nullable
    private static SoundEvent forBlock(IBlockState state, EntityVillager villager, BlockPos pos) {
        ResourceLocation name = state.getBlock().getRegistryName();

        if (name != null && VillagerBackport.MOD_ID.equals(name.getNamespace())) {
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

        // Not the fletching table. It is a stonecutter in what it does and a wooden bench in what it
        // is, and a saw biting stone is the wrong noise for arrows - so it falls through and gets
        // the wood of its own material instead.

        return null;
    }
}
