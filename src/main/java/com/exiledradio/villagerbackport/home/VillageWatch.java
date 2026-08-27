package com.exiledradio.villagerbackport.home;

import com.exiledradio.villagerbackport.ModConfig;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.village.Village;
import net.minecraft.world.World;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Announces villages as the game works out that it has one.
 *
 * <h2>Why this is a poll and not an event</h2>
 * 1.12.2 grows villages quietly. {@code VillageCollection} decides on its own schedule that a
 * cluster of doors is a village, adjusts its centre and radius as more are found, and merges or
 * drops them without telling anybody - there is no event for any of it. So the list is read
 * periodically and compared with what was there last time.
 *
 * <p>Worth reporting because so much depends on it and none of it is visible. Whether a village
 * exists at all decides breeding and golem spawning, and a player building a village has no way to
 * tell whether the game agrees with them that they have built one.
 */
public final class VillageWatch {

    /** How often the list is read. Villages form over minutes, so this can be lazy. */
    private static final int CHECK_INTERVAL = 200;

    /** How far a player can be from a new village and still be told about it. */
    private static final double TELL_RANGE = 128.0D;

    /** Centres already announced, per dimension, so each village is reported once. */
    private static final Map<Integer, Set<BlockPos>> KNOWN = new HashMap<Integer, Set<BlockPos>>();

    private int ticks;

    @SubscribeEvent
    public void onServerTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.world.isRemote) {
            return;
        }

        if (!ModConfig.homes.enabled || !ModConfig.homes.debugMessages) {
            return;
        }

        if (++this.ticks % CHECK_INTERVAL != 0) {
            return;
        }

        check(event.world);
    }

    private void check(World world) {
        if (world.getVillageCollection() == null) {
            return;
        }

        Set<BlockPos> known = knownIn(world);

        for (Village village : world.getVillageCollection().getVillageList()) {
            BlockPos centre = village.getCenter();

            if (known.add(centre)) {
                announce(world, village, centre);
            }
        }
    }

    /**
     * Says what the game thinks it has found.
     *
     * <p>The bed count comes from this mod rather than from the village, because 1.12.2's village
     * has no idea what a bed is - it counts doors. Reporting both together is the point: a village
     * of twenty doors and two beds explains a great deal about why nothing is breeding.
     */
    private void announce(World world, Village village, BlockPos centre) {
        int beds = VillageOutline.bedsAround(world, centre, village.getVillageRadius()).size();

        String message = "village found at " + centre.getX() + ", " + centre.getY() + ", "
                + centre.getZ() + " - radius " + village.getVillageRadius()
                + ", " + village.getNumVillagers() + " villagers, "
                + village.getNumVillageDoors() + " doors, " + beds + " beds";

        TextComponentString line = new TextComponentString("[village] " + message);
        line.getStyle().setColor(TextFormatting.DARK_AQUA);

        for (EntityPlayer player : world.playerEntities) {
            if (player.getDistanceSq(centre) <= TELL_RANGE * TELL_RANGE) {
                player.sendMessage(line);
            }
        }
    }

    private static Set<BlockPos> knownIn(World world) {
        int dimension = world.provider.getDimension();
        Set<BlockPos> known = KNOWN.get(dimension);

        if (known == null) {
            known = new HashSet<BlockPos>();
            KNOWN.put(dimension, known);
        }

        return known;
    }

    /** Villages are found again from scratch on the next load, so nothing should outlive the world. */
    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        if (!event.getWorld().isRemote) {
            KNOWN.remove(event.getWorld().provider.getDimension());
        }
    }
}
