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
 * Getting a villager out of a bed you want.
 *
 * <h2>Why this has to intercept rather than add</h2>
 * A sleeping villager stands in the same block as its bed, so a right-click aimed at the bed hits
 * the villager instead - and a right-click on a villager opens the trade screen. Both things then
 * happen at once: the shop opens and the sleeper is bundled out from under it.
 *
 * <p>So the interaction is claimed outright while a villager is asleep. Waking it is the whole of
 * what the click does, and the trade screen stays shut. That is the useful reading of the gesture:
 * nobody right-clicks a bed at midnight hoping to buy something.
 *
 * <p>Runs at the highest priority so it is decided before anything else looks at the click -
 * including this mod's own pricing, which sits at the lowest and skips cancelled interactions.
 */
public final class BedInteraction {

    /**
     * Right-clicking a sleeping villager wakes it rather than trading with it.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getWorld().isRemote || !ModConfig.homes.enabled) {
            return;
        }

        if (!(event.getTarget() instanceof EntityVillager)) {
            return;
        }

        EntityVillager villager = (EntityVillager) event.getTarget();

        if (HomeSite.isSleeping(villager)) {
            HomeSite.disturb(villager);
            VillageDebug.say(villager, "sleep: turned out of bed");

            event.setCanceled(true);
        }
    }

    /**
     * Right-clicking the bed itself wakes whoever is in it.
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

        HomeSite.disturb(sleeper);
        VillageDebug.say(sleeper, "sleep: turned out of bed");

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
