package com.exiledradio.villagerbackport.trade;

import com.exiledradio.villagerbackport.ModConfig;

import com.exiledradio.villagerbackport.data.VillagerTradeData;

import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;

import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * What a villager thinks of each player, and what that is worth at the till.
 *
 * <h2>1.14's reputation, in the villager's own data</h2>
 * 1.14 keeps this in a {@code GossipManager} per villager: a map of player to a map of
 * {@link GossipType} to how much of it has accrued. Reputation for a player is the weighted sum,
 * and a villager offers a discount proportional to it. Everything the player does around villagers
 * feeds it - trading, hurting one, killing one, curing one of zombiehood - and it wears off daily
 * and spreads between villagers who talk to each other.
 *
 * <p>Stored here in the same compound the rest of this mod's villager data lives in, keyed by player
 * UUID and gossip name, which is close enough to 1.14's own save shape to read at a glance:
 * <pre>
 *   Gossip: { "&lt;player-uuid&gt;": { trading: 8, major_positive: 20 } }
 * </pre>
 *
 * <p>Only the villager holds it, and it dies with the villager - which is 1.14's behaviour and the
 * reason curing one zombie villager does not make a whole village cheap on its own. It spreads
 * because villagers repeat it to each other, and only the part worth repeating.
 */
public final class Gossip {

    private static final String ROOT = "villagerbackport";
    private static final String GOSSIP = "Gossip";
    private static final String LAST_DECAY = "LastGossipDecay";

    private static final int TAG_COMPOUND = 10;
    private static final int TAG_ANY_NUMBER = 99;

    /** 1.14 decays gossip once a day, measured in game time rather than by the clock. */
    private static final long DECAY_INTERVAL = 24000L;

    /** 1.14 hands over at most this much of any one opinion per conversation. */
    private static final int SHARE_LIMIT = 10;

    private Gossip() {
    }

    private static NBTTagCompound root(EntityVillager villager) {
        return VillagerTradeData.root(villager);
    }

    private static NBTTagCompound all(EntityVillager villager) {
        NBTTagCompound tag = root(villager);
        if (!tag.hasKey(GOSSIP, TAG_COMPOUND)) {
            tag.setTag(GOSSIP, new NBTTagCompound());
        }
        return tag.getCompoundTag(GOSSIP);
    }

    private static NBTTagCompound about(EntityVillager villager, UUID player) {
        NBTTagCompound gossip = all(villager);
        String key = player.toString();

        if (!gossip.hasKey(key, TAG_COMPOUND)) {
            gossip.setTag(key, new NBTTagCompound());
        }

        return gossip.getCompoundTag(key);
    }

    /**
     * Adds to one kind of opinion, up to that kind's ceiling.
     *
     * <p>1.14's {@code add}: the value is capped at the type's maximum rather than the addition
     * being refused, so a player who keeps trading stays at the cap rather than drifting past it.
     */
    public static void add(EntityVillager villager, UUID player, GossipType type, int amount) {
        if (!ModConfig.pricing.gossipEnabled || amount <= 0) {
            return;
        }

        NBTTagCompound mine = about(villager, player);
        int updated = Math.min(mine.getInteger(type.id) + amount, type.max);

        mine.setInteger(type.id, updated);
    }

    /**
     * @return how well this villager thinks of the player, as 1.14's weighted total
     *
     * <p>Signed: positive earns a discount, negative costs extra.
     */
    public static int reputation(EntityVillager villager, EntityPlayer player) {
        if (!ModConfig.pricing.gossipEnabled) {
            return 0;
        }

        NBTTagCompound mine = about(villager, player.getUniqueID());
        int total = 0;

        for (GossipType type : GossipType.values()) {
            total += mine.getInteger(type.id) * type.weight;
        }

        return total;
    }

    /**
     * @return what this player's standing is worth on a trade - negative for a discount, positive
     * for the surcharge a villager puts on someone it has reason to dislike
     *
     * <h2>Both directions, and why</h2>
     * 1.14's {@code updateSpecialPrices} is a single line - {@code -floor(reputation *
     * priceMultiplier)} - and it is signed on purpose. Reputation of -50 comes back as
     * {@code -floor(-2.5)}, which is {@code +3}: three more emeralds, because the villager watched
     * you kill somebody.
     *
     * <p>The {@code Math.max(0, ...)} that stops prices falling below base applies to the demand
     * term alone and never to this one. Demand is about what a village is short of; this is about
     * what a villager thinks of you, and it moves the price both ways.
     *
     * <p>At the default multiplier of 0.05 that is one item per twenty points. Trading to the cap is
     * worth a single emerald off; curing a zombie villager is 125 points and worth six; watching you
     * murder a neighbour is -125 and worth six the other way.
     *
     * <p>1.14 also discounts for Hero of the Village here. That effect is part of raids, which
     * 1.12.2 has none of, so there is nothing to read and the branch simply does not exist.
     */
    public static int specialPriceFor(EntityVillager villager, EntityPlayer player) {
        int reputation = reputation(villager, player);
        if (reputation == 0) {
            return 0;
        }

        return -(int) Math.floor(reputation * ModConfig.pricing.priceMultiplier);
    }

    /**
     * Wears opinions down once a day, dropping any that reach nothing.
     *
     * <p>Called from the villager's own tick. 1.14 does the same on a 24000 tick timer held on the
     * villager, which means an unloaded villager does not decay while nobody is watching - time
     * passing is not what fades a memory here, the villager living through it is.
     */
    public static void decayIfDue(EntityVillager villager) {
        if (!ModConfig.pricing.gossipEnabled) {
            return;
        }

        NBTTagCompound tag = root(villager);
        long now = villager.world.getTotalWorldTime();
        long last = tag.hasKey(LAST_DECAY, TAG_ANY_NUMBER) ? tag.getLong(LAST_DECAY) : 0L;

        if (last == 0L) {
            tag.setLong(LAST_DECAY, now);
            return;
        }

        if (now < last + DECAY_INTERVAL) {
            return;
        }

        tag.setLong(LAST_DECAY, now);
        decay(villager);
    }

    private static void decay(EntityVillager villager) {
        NBTTagCompound gossip = all(villager);

        for (String player : gossip.getKeySet().toArray(new String[0])) {
            NBTTagCompound mine = gossip.getCompoundTag(player);

            for (GossipType type : GossipType.values()) {
                int left = mine.getInteger(type.id) - type.decay;

                if (left > 0) {
                    mine.setInteger(type.id, left);
                } else {
                    mine.removeTag(type.id);
                }
            }

            // A player nobody holds any opinion about is not worth a line in the save.
            if (mine.getSize() == 0) {
                gossip.removeTag(player);
            }
        }
    }

    /**
     * Repeats what is worth repeating to one nearby villager.
     *
     * <h2>What travels and what does not</h2>
     * 1.14 hands over the amount <em>above</em> the type's share floor, capped at ten, and only when
     * at least two points would move. That is what stops one grudge propagating around a village at
     * full strength forever: a killing is worth 100 to the villager who saw it and at most 10 to the
     * next one along, and trading barely travels at all.
     *
     * <p>1.14 does this in the meeting-point gossip activity, where villagers gather at midday. There
     * is no such activity here, so it happens on the villager's own tick against whoever is standing
     * nearby - the same outcome by the shorter route, and rare enough not to be worth a schedule.
     */
    public static void shareWith(EntityVillager villager, Random random) {
        if (!ModConfig.pricing.gossipEnabled || !ModConfig.pricing.gossipSpreads) {
            return;
        }

        NBTTagCompound mine = all(villager);
        if (mine.getSize() == 0) {
            return;
        }

        EntityVillager other = neighbourOf(villager, random);
        if (other == null) {
            return;
        }

        // 1.14 asks for a golem from the same place two villagers exchange gossip, needing five
        // eligible villagers rather than the three a frightened one needs. Villagers standing about
        // talking is how a peaceful village gets its golem.
        com.exiledradio.villagerbackport.home.VillageDebug.say(villager, "gossip: exchanged with a neighbour");

        com.exiledradio.villagerbackport.home.GolemSpawner.tryToSpawn(
                villager, com.exiledradio.villagerbackport.home.GolemSpawner.GOSSIP_REQUIREMENT);

        for (String player : mine.getKeySet()) {
            NBTTagCompound held = mine.getCompoundTag(player);
            UUID id = uuidOf(player);

            if (id == null) {
                continue;
            }

            for (GossipType type : GossipType.values()) {
                int worth = Math.min(held.getInteger(type.id) - type.shareFloor, SHARE_LIMIT);

                if (worth >= 2) {
                    add(other, id, type, worth);
                }
            }
        }
    }

    private static EntityVillager neighbourOf(EntityVillager villager, Random random) {
        AxisAlignedBB box = villager.getEntityBoundingBox().grow(ModConfig.pricing.gossipRange);
        List<EntityVillager> nearby = villager.world.getEntitiesWithinAABB(EntityVillager.class, box);

        nearby.remove(villager);

        return nearby.isEmpty() ? null : nearby.get(random.nextInt(nearby.size()));
    }

    private static UUID uuidOf(String text) {
        try {
            return UUID.fromString(text);
        } catch (IllegalArgumentException e) {
            // Not ours to interpret - leave whatever wrote it alone rather than throwing per tick.
            return null;
        }
    }

    /** @return true if this villager holds any opinion at all, so the cheap case stays cheap. */
    public static boolean isEmpty(EntityVillager villager) {
        NBTTagCompound tag = root(villager);
        if (!tag.hasKey(GOSSIP, TAG_COMPOUND)) {
            return true;
        }

        Set<String> players = tag.getCompoundTag(GOSSIP).getKeySet();
        return players.isEmpty();
    }
}
