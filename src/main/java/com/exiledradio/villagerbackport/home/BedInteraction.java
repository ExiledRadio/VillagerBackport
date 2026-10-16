package com.exiledradio.villagerbackport.home;

import com.exiledradio.villagerbackport.ModConfig;

import net.minecraft.block.BlockBed;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import javax.annotation.Nullable;

/**
 * Clicking on a sleeping villager, or on the bed it is sleeping in.
 *
 * <h2>Either click wakes it</h2>
 * A villager wakes when its bed is used - a right-click on the bed - or when it is right-clicked
 * itself, and goes back to sleep a moment later if the bed is still free. The wiki gives the first
 * on its Villager page and the second on its Bed page; both are honoured. The trade screen never
 * opens from a click on a sleeper.
 *
 * <p>Both clicks are still claimed outright here. A sleeping villager stands in the same block as its
 * bed, so a click aimed at the bed lands on the villager, and a click on a villager would otherwise
 * open its trade screen in the middle of the night.
 *
 * <p>Runs at the highest priority so it is decided before anything else looks at the click -
 * including this mod's own pricing, which sits at the lowest and skips cancelled interactions.
 */
public final class BedInteraction {

    /**
     * How long a villager woken by its bed being used stays up before trying again. The current
     * game's sleep task waits 40 ticks after it ends: long enough for the player to take the bed
     * with a second click, after which the villager finds it occupied and gives it up.
     */
    private static final int BED_USED_TICKS = 40;

    /** Right-clicking a sleeping villager wakes it, as the wiki's Bed page describes, and never trades. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getWorld().isRemote || !ModConfig.homes.enabled) {
            return;
        }

        if (event.getTarget() instanceof EntityVillager
                && HomeSite.isSleeping((EntityVillager) event.getTarget())) {
            EntityVillager sleeper = (EntityVillager) event.getTarget();
            HomeSite.disturb(sleeper, BED_USED_TICKS);
            VillageDebug.say(sleeper, "sleep: woken by a player");
            event.setCanceled(true);
        }
    }

    /**
     * Right-clicking a bed a villager is asleep in wakes the villager, without the player lying down.
     *
     * <p>The click can land on either half of the bed and the sleeper is recorded against the head,
     * so a click on the foot is followed round to the other end before asking who is in it.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getWorld().isRemote || !ModConfig.homes.enabled) {
            return;
        }

        BlockPos head = headOf(event.getWorld(), event.getPos());
        if (head == null) {
            return;
        }

        EntityVillager sleeper = HomeSite.sleeperIn(event.getWorld(), head);
        if (sleeper == null) {
            return;
        }

        HomeSite.disturb(sleeper, BED_USED_TICKS);
        VillageDebug.say(sleeper, "sleep: woken by its bed being used");

        event.setCanceled(true);
    }

    /** @return the head half of the bed at this position, or null if it is not a bed. */
    @Nullable
    private static BlockPos headOf(World world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);

        if (!(state.getBlock() instanceof BlockBed)) {
            return null;
        }

        if (state.getValue(BlockBed.PART) == BlockBed.EnumPartType.HEAD) {
            return pos;
        }

        return pos.offset(state.getValue(BlockBed.FACING));
    }
}
