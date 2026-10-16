package com.exiledradio.villagerbackport.trade;

import com.exiledradio.villagerbackport.ModConfig;

import com.exiledradio.villagerbackport.data.VillagerTradeData;

import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

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

    private static final String LAST_GOSSIP = "LastGossip";

    /** 1.14's minute between any one villager's conversations. */
    private static final long GOSSIP_COOLDOWN = 1200L;

    /** How many opinions 1.14 draws from a partner per meeting. */
    private static final int DRAWS_PER_MEETING = 10;

    /** 1.14 decays gossip once a day, measured in game time rather than by the clock. */
    private static final long DECAY_INTERVAL = 24000L;

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
     * @return what a reputation is worth on one trade - negative for a discount, positive for the
     * surcharge a villager puts on someone it has reason to dislike
     *
     * <h2>Both directions, and why</h2>
     * 1.14's {@code updateSpecialPrices} is a single line per offer - {@code -floor(reputation *
     * offer.getPriceMultiplier())} - and it is signed on purpose. The {@code Math.max(0, ...)} that
     * stops prices falling below base applies to the demand term alone and never to this one.
     *
     * <h2>Per trade, not per villager</h2>
     * The multiplier is the offer's own, the same one demand uses, so standing counts four times as
     * much on a premium trade as on an ordinary one. Watching you murder a neighbour is -125: seven
     * more wheat at 0.05, twenty-five more emeralds for an enchanted book at 0.2. Curing a villager
     * is +125 and worth the same the other way.
     *
     * <p>Float arithmetic, as 1.14 has it, and not for show: in doubles {@code -125 * 0.2} can land
     * a hair under -25 and floor to -26.
     *
     * <p>1.14 also discounts for Hero of the Village here. That effect is part of raids, which
     * 1.12.2 has none of, so there is nothing to read and the branch simply does not exist.
     *
     * @param multiplier the trade's price multiplier - see {@link PriceEngine#multiplierFor}
     */
    public static int specialPriceFor(int reputation, double multiplier) {
        if (reputation == 0) {
            return 0;
        }

        return -MathHelper.floor((float) reputation * (float) multiplier);
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

                // 1.14 drops an opinion once what is left of it would be under 2, not at nothing.
                if (left >= 2) {
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
     * Two villagers meeting and swapping news, 1.14's {@code ShareItemsTask} gossip.
     *
     * <h2>When</h2>
     * 1.14 villagers gossip when one walks up to another - within {@code sqrt 5}, a little over two
     * blocks - and only if neither has gossiped in the last minute. Both are then marked, so a pair
     * that just talked does not talk again straight away, and neither does either of them with
     * anyone else. Here it is checked on every poll against whoever is standing that close, which is
     * the same rule without 1.14's walking-up step.
     *
     * <h2>What is said</h2>
     * The villager running the check hears from its partner, as in 1.14 - one direction per meeting.
     * It is not told everything: 1.14 draws ten times from the partner's opinions, weighted by how
     * much each is worth, and passes on only what was drawn. A strong grudge is almost certain to
     * come up; a passing remark about one trade may not. See {@link #hear} for how much of each
     * survives the retelling.
     *
     * <h2>The golem</h2>
     * 1.14 asks for a golem from the same place, needing five eligible villagers rather than the
     * three a frightened one needs, and it asks whether or not there was anything to say. Villagers
     * standing about talking is how a peaceful village gets its golem.
     */
    public static void meet(EntityVillager villager, Random random) {
        if (!ModConfig.pricing.gossipEnabled || !ModConfig.pricing.gossipSpreads) {
            return;
        }

        long now = villager.world.getTotalWorldTime();
        if (!readyToTalk(villager, now)) {
            return;
        }

        EntityVillager partner = partnerFor(villager, now, random);
        if (partner == null) {
            return;
        }

        root(villager).setLong(LAST_GOSSIP, now);
        root(partner).setLong(LAST_GOSSIP, now);

        hearFrom(villager, partner, random);

        com.exiledradio.villagerbackport.home.VillageDebug.say(villager, "gossip: exchanged with a neighbour");

        com.exiledradio.villagerbackport.home.GolemSpawner.tryToSpawn(
                villager, com.exiledradio.villagerbackport.home.GolemSpawner.GOSSIP_REQUIREMENT);
    }

    /**
     * 1.14's {@code transferFrom}: ten weighted draws, with replacement, from the teller's opinions.
     *
     * <p>Each opinion's weight is what it is worth on a price, {@code |value * weight|}, so a killing
     * at 25 major-negative (worth 125) is drawn far more often than two points of trading. An
     * opinion drawn more than once is still only passed on once.
     */
    private static void hearFrom(EntityVillager listener, EntityVillager teller, Random random) {
        List<UUID> players = new ArrayList<UUID>();
        List<GossipType> types = new ArrayList<GossipType>();
        List<Integer> values = new ArrayList<Integer>();

        NBTTagCompound told = all(teller);
        for (String player : told.getKeySet()) {
            UUID id = uuidOf(player);
            if (id == null) {
                continue;
            }

            NBTTagCompound held = told.getCompoundTag(player);
            for (GossipType type : GossipType.values()) {
                int value = held.getInteger(type.id);
                if (value != 0) {
                    players.add(id);
                    types.add(type);
                    values.add(Integer.valueOf(value));
                }
            }
        }

        if (players.isEmpty()) {
            return;
        }

        int[] upTo = new int[players.size()];
        int total = 0;
        for (int k = 0; k < players.size(); k++) {
            total += Math.abs(values.get(k).intValue() * types.get(k).weight);
            upTo[k] = total - 1;
        }

        Set<Integer> drawn = new HashSet<Integer>();
        for (int draw = 0; draw < DRAWS_PER_MEETING; draw++) {
            int pick = Arrays.binarySearch(upTo, random.nextInt(total));
            drawn.add(Integer.valueOf(pick < 0 ? -pick - 1 : pick));
        }

        for (Integer k : drawn) {
            GossipType type = types.get(k.intValue());
            int worth = values.get(k.intValue()).intValue() - type.shareFloor;

            if (worth >= 2) {
                hear(listener, players.get(k.intValue()), type, worth);
            }
        }
    }

    private static boolean readyToTalk(EntityVillager villager, long now) {
        NBTTagCompound tag = root(villager);
        if (!tag.hasKey(LAST_GOSSIP, TAG_ANY_NUMBER)) {
            return true;
        }

        long last = tag.getLong(LAST_GOSSIP);

        // Earlier than now as well, for a world whose time was set back.
        return now < last || now >= last + GOSSIP_COOLDOWN;
    }

    @Nullable
    private static EntityVillager partnerFor(EntityVillager villager, long now, Random random) {
        double range = ModConfig.pricing.gossipRange;
        AxisAlignedBB box = villager.getEntityBoundingBox().grow(range);

        List<EntityVillager> willing = new ArrayList<EntityVillager>();
        for (EntityVillager other : villager.world.getEntitiesWithinAABB(EntityVillager.class, box)) {
            if (other != villager && !other.isChild()
                    && other.getDistanceSq(villager) <= range * range
                    && readyToTalk(other, now)
                    && villager.canEntityBeSeen(other)) {
                willing.add(other);
            }
        }

        return willing.isEmpty() ? null : willing.get(random.nextInt(willing.size()));
    }

    /**
     * Drops everything this villager holds against one player, leaving any goodwill alone.
     *
     * @return true if there was a grudge to drop
     */
    public static boolean forgive(EntityVillager villager, UUID player) {
        NBTTagCompound tag = root(villager);
        if (!tag.hasKey(GOSSIP, TAG_COMPOUND)) {
            return false;
        }

        NBTTagCompound gossip = tag.getCompoundTag(GOSSIP);
        String key = player.toString();
        if (!gossip.hasKey(key, TAG_COMPOUND)) {
            return false;
        }

        NBTTagCompound mine = gossip.getCompoundTag(key);
        boolean held = false;

        for (GossipType type : GossipType.values()) {
            if (type.weight < 0 && mine.hasKey(type.id)) {
                mine.removeTag(type.id);
                held = true;
            }
        }

        if (mine.getSize() == 0) {
            gossip.removeTag(key);
        }

        return held;
    }

    /** Takes on a retold opinion if it is stronger than the one already held. */
    private static void hear(EntityVillager villager, UUID player, GossipType type, int amount) {
        NBTTagCompound mine = about(villager, player);

        if (amount > mine.getInteger(type.id)) {
            mine.setInteger(type.id, Math.min(amount, type.max));
        }
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
