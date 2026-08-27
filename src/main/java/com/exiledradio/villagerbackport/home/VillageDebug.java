package com.exiledradio.villagerbackport.home;

import com.exiledradio.villagerbackport.ModConfig;

import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

/**
 * Says out loud what village life is doing, for anyone trying to test it.
 *
 * <h2>Why this exists at all</h2>
 * Most of this mod can be checked by looking: a price changed, a villager took a job, a book was
 * rolled. Gossip, sleeping and golem spawning cannot. They turn on conditions that are invisible -
 * whether a villager slept in the last day, worked in the last day and a half, how many neighbours
 * qualify - and when nothing happens there is no way to tell which of those was the one that failed.
 *
 * <p>So this reports the near misses, not only the successes. "Two of three eligible" is the line
 * that tells you the village is one rested villager short of a golem; a silent village tells you
 * nothing at all.
 */
public final class VillageDebug {

    /** How far away a player has to be to stop hearing about it. */
    private static final double RANGE = 48.0D;

    /** How long the same line from the same villager is held back for. Half a minute. */
    private static final long REPEAT_TICKS = 600L;

    /** When each villager last said each kind of thing. */
    private static final java.util.Map<String, Long> SAID = new java.util.HashMap<String, Long>();

    private VillageDebug() {
    }

    public static boolean on() {
        return ModConfig.homes.debugMessages;
    }

    /**
     * Sends a line that repeats, no more often than once every {@link #REPEAT_TICKS}.
     *
     * <h2>Why some lines need holding back</h2>
     * A villager with no reachable bed reports that fact every time it looks, which is every five
     * seconds, for every villager in the village, all day long - and it is the same sentence each
     * time. The useful information is that the condition exists, not that it still exists five
     * seconds later, and a debug channel nobody can read is not a debug channel.
     *
     * @param key what makes this line the same line as the last one, so repeats can be recognised
     */
    public static void repeat(EntityVillager villager, String key, String message) {
        if (!on() || villager.world.isRemote) {
            return;
        }

        String id = villager.getEntityId() + "/" + key;
        long now = villager.world.getTotalWorldTime();
        Long last = SAID.get(id);

        if (last != null && now - last.longValue() < REPEAT_TICKS) {
            return;
        }

        SAID.put(id, Long.valueOf(now));
        say(villager, message);
    }

    /** Sends one line to every player near enough to be the one testing this. */
    public static void say(EntityVillager villager, String message) {
        if (!on() || villager.world.isRemote) {
            return;
        }

        TextComponentString line = new TextComponentString("[village] " + message);
        line.getStyle().setColor(TextFormatting.DARK_AQUA);

        for (EntityPlayer player : villager.world.playerEntities) {
            if (player.getDistanceSq(villager) <= RANGE * RANGE) {
                player.sendMessage(line);
            }
        }
    }
}
