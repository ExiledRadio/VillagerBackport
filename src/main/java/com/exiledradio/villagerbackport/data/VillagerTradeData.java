package com.exiledradio.villagerbackport.data;

import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.nbt.NBTTagCompound;

import java.util.Set;

/**
 * The per-villager bookkeeping 1.14 keeps on {@code VillagerEntity} and 1.12.2 has nowhere to put.
 *
 * <p>1.14 stores {@code LastRestock}, {@code RestocksToday} and {@code Xp} as fields written
 * straight into the entity's NBT, and keeps {@code demand} on each {@code MerchantOffer}. We cannot
 * add fields to {@link EntityVillager} or to {@code MerchantRecipe}, so all of it lives in the
 * entity's Forge data compound - the tag Forge already persists under {@code ForgeData} for every
 * entity, saved and loaded automatically with no capability to register and no serializer to write.
 *
 * <p>Using ForgeData rather than a capability is a deliberate choice. A capability is the more
 * idiomatic home for this, but it means registering a type, a provider and a storage implementation,
 * and attaching it to an entity class that thirty other mods in a large pack already touch.
 * ForgeData has none of that surface: it is a plain compound tag, nested under this mod's own key,
 * that no other mod has a reason to read. When the XP bar needs this data client-side it will be
 * sent in a packet of our own; nothing here has to change for that.
 *
 * <p>Layout:
 * <pre>
 * ForgeData/villagerbackport/
 *   LastRestock   long   game time of last restock
 *   RestocksToday int    daily allowance spent
 *   LastDayCheck  long   world time at last day-rollover check
 *   Xp            int    trading experience, drives level
 *   AppliedLevel  int    the level actually in effect; lags Xp until the screen closes
 *   LevelUpAt     long   game time a pending level-up lands, absent when none is due
 *   Trades/&lt;key&gt;/  per-trade state, keyed by TradeKey
 *     Base        int    the trade's original buy-stack count, before any price adjustment
 *     Demand      int    1.14's demand value
 *     Uses        int    last observed toolUses, for detecting completed trades
 * </pre>
 */
public final class VillagerTradeData {

    /** Our private compound inside the entity's ForgeData tag. */
    private static final String ROOT = "villagerbackport";

    private static final String LAST_RESTOCK = "LastRestock";
    private static final String RESTOCKS_TODAY = "RestocksToday";
    private static final String LAST_DAY_CHECK = "LastDayCheck";
    private static final String LAST_CATCH_UP = "LastCatchUpDay";
    private static final String DEMAND_REPAIRED = "DemandRepaired";
    private static final String XP = "Xp";
    private static final String APPLIED_LEVEL = "AppliedLevel";
    private static final String LEVEL_UP_AT = "LevelUpAt";
    private static final String TRADES = "Trades";

    private static final String TRADE_BASE = "Base";
    private static final String TRADE_DEMAND = "Demand";
    private static final String TRADE_USES = "Uses";

    /** NBT type ids, used with hasKey to distinguish "absent" from "present and zero". */
    private static final int TAG_COMPOUND = 10;
    private static final int TAG_ANY_NUMBER = 99;

    private VillagerTradeData() {
    }

    /**
     * @return this mod's compound on the given villager, creating it on first use.
     *
     * <p>{@code getEntityData()} returns the live ForgeData tag, so mutating the compound we put
     * inside it writes through to the entity without a further set call.
     */
    /**
     * @return this mod's compound on the villager, creating it if this is the first write
     *
     * <p>Shared rather than reimplemented. Four classes across three packages keep state on the same
     * villager, and each used to carry its own copy of this - agreeing by coincidence rather than by
     * construction, with four places to get the tag name or the type id wrong.
     */
    public static NBTTagCompound root(EntityVillager villager) {
        NBTTagCompound forgeData = villager.getEntityData();
        if (!forgeData.hasKey(ROOT, TAG_COMPOUND)) {
            forgeData.setTag(ROOT, new NBTTagCompound());
        }
        return forgeData.getCompoundTag(ROOT);
    }

    private static NBTTagCompound trades(EntityVillager villager) {
        NBTTagCompound root = root(villager);
        if (!root.hasKey(TRADES, TAG_COMPOUND)) {
            root.setTag(TRADES, new NBTTagCompound());
        }
        return root.getCompoundTag(TRADES);
    }

    private static NBTTagCompound trade(EntityVillager villager, String key) {
        NBTTagCompound trades = trades(villager);
        if (!trades.hasKey(key, TAG_COMPOUND)) {
            trades.setTag(key, new NBTTagCompound());
        }
        return trades.getCompoundTag(key);
    }

    // ---------------------------------------------------------------- restock

    /**
     * @return the game time of the last restock, or {@link Long#MIN_VALUE} for a villager that has
     * never restocked.
     *
     * <p>The sentinel matters. Returning 0 for a fresh villager would mean "restocked at world
     * creation", which on an old world is far enough in the past to read as eligible - correct by
     * accident. On a brand new world it would read as "restocked this second" and lock a villager
     * out of its first restock for two minutes. MIN_VALUE is unambiguously "never", so the first
     * restock is always allowed.
     */
    public static long getLastRestock(EntityVillager villager) {
        NBTTagCompound tag = root(villager);
        return tag.hasKey(LAST_RESTOCK, TAG_ANY_NUMBER) ? tag.getLong(LAST_RESTOCK) : Long.MIN_VALUE;
    }

    public static void setLastRestock(EntityVillager villager, long gameTime) {
        root(villager).setLong(LAST_RESTOCK, gameTime);
    }

    public static int getRestocksToday(EntityVillager villager) {
        return root(villager).getInteger(RESTOCKS_TODAY);
    }

    public static void setRestocksToday(EntityVillager villager, int count) {
        root(villager).setInteger(RESTOCKS_TODAY, count);
    }

    /** @return the last observed world time, or -1 if we have never checked this villager. */
    public static long getLastDayCheck(EntityVillager villager) {
        NBTTagCompound tag = root(villager);
        return tag.hasKey(LAST_DAY_CHECK, TAG_ANY_NUMBER) ? tag.getLong(LAST_DAY_CHECK) : -1L;
    }

    public static void setLastDayCheck(EntityVillager villager, long worldTime) {
        root(villager).setLong(LAST_DAY_CHECK, worldTime);
    }

    /**
     * @return the day number this villager last settled its unused restocks on, or -1 for never
     *
     * <p>Deliberately a day number rather than a flag. Settling demand is destructive - it clears
     * stock and sheds demand - so it must happen on the edge of a day turning and not once per check
     * for as long as the day is still new.
     */
    public static long getLastCatchUpDay(EntityVillager villager) {
        NBTTagCompound tag = root(villager);
        return tag.hasKey(LAST_CATCH_UP, TAG_ANY_NUMBER) ? tag.getLong(LAST_CATCH_UP) : -1L;
    }

    public static void setLastCatchUpDay(EntityVillager villager, long day) {
        root(villager).setLong(LAST_CATCH_UP, day);
    }

    /** @return true if this villager's demand has been checked over after the 0.49 catch-up bug. */
    public static boolean isDemandRepaired(EntityVillager villager) {
        return root(villager).getBoolean(DEMAND_REPAIRED);
    }

    public static void setDemandRepaired(EntityVillager villager) {
        root(villager).setBoolean(DEMAND_REPAIRED, true);
    }

    // --------------------------------------------------------------------- xp

    public static int getXp(EntityVillager villager) {
        return root(villager).getInteger(XP);
    }

    public static void setXp(EntityVillager villager, int xp) {
        root(villager).setInteger(XP, Math.max(0, xp));
    }

    /**
     * @return the level currently in effect, or -1 for a villager not yet seen.
     *
     * <p>Deliberately separate from experience. Experience accrues the moment a trade completes, but
     * the level it earns is not applied until the player closes the trade screen - so this is what
     * the rank and the unlocked trades follow, while experience runs ahead of it.
     *
     * <p>Also distinct from the villager's career level, which vanilla increments on every restock
     * whether or not anything unlocked. The -1 case marks a villager seen for the first time, which
     * adopts its existing trades rather than levelling from scratch.
     */
    public static int getAppliedLevel(EntityVillager villager) {
        NBTTagCompound tag = root(villager);
        return tag.hasKey(APPLIED_LEVEL, TAG_ANY_NUMBER) ? tag.getInteger(APPLIED_LEVEL) : -1;
    }

    public static void setAppliedLevel(EntityVillager villager, int tier) {
        root(villager).setInteger(APPLIED_LEVEL, tier);
    }

    /**
     * @return the game time a pending level-up is due to land, or -1 if none is scheduled.
     *
     * <p>A level-up waits out a short countdown after the trade screen closes rather than landing
     * the instant it shuts, which is what both vanilla versions do with {@code timeUntilReset}.
     * Persisted so the pause survives the chunk unloading in between.
     */
    public static long getLevelUpAt(EntityVillager villager) {
        NBTTagCompound tag = root(villager);
        return tag.hasKey(LEVEL_UP_AT, TAG_ANY_NUMBER) ? tag.getLong(LEVEL_UP_AT) : -1L;
    }

    public static void setLevelUpAt(EntityVillager villager, long gameTime) {
        root(villager).setLong(LEVEL_UP_AT, gameTime);
    }

    /** Cancels a scheduled level-up, for when the villager is no longer eligible for one. */
    public static void clearLevelUpAt(EntityVillager villager) {
        root(villager).removeTag(LEVEL_UP_AT);
    }

    // ------------------------------------------------------------ per-trade

    /**
     * @return the trade's recorded base buy count, or -1 if we have not seen this trade before.
     *
     * <p>Recording the base is what makes demand pricing idempotent. The adjusted price is always
     * computed from this value, never from the trade's current count - otherwise each adjustment
     * would compound on the last and prices would run away within a few restocks.
     */
    public static int getBaseCount(EntityVillager villager, String key) {
        NBTTagCompound tag = trade(villager, key);
        return tag.hasKey(TRADE_BASE, TAG_ANY_NUMBER) ? tag.getInteger(TRADE_BASE) : -1;
    }

    public static void setBaseCount(EntityVillager villager, String key, int baseCount) {
        trade(villager, key).setInteger(TRADE_BASE, baseCount);
    }

    public static int getDemand(EntityVillager villager, String key) {
        return trade(villager, key).getInteger(TRADE_DEMAND);
    }

    public static void setDemand(EntityVillager villager, String key, int demand) {
        trade(villager, key).setInteger(TRADE_DEMAND, demand);
    }

    public static int getLastSeenUses(EntityVillager villager, String key) {
        return trade(villager, key).getInteger(TRADE_USES);
    }

    public static void setLastSeenUses(EntityVillager villager, String key, int uses) {
        trade(villager, key).setInteger(TRADE_USES, uses);
    }

    /**
     * Drops stored state for trades the villager no longer offers.
     *
     * <p>Without this the Trades compound only ever grows. A villager's trade list is normally
     * small and stable, but it is not guaranteed to be: vanilla appends a tier of trades on every
     * career level-up, and a mod that generates trades with varying NBT - RLCraft Villager Tomes
     * hands villagers player-chosen enchanted books - can produce a different key each time. Pruning
     * on restock keeps the saved data proportional to what the villager actually offers.
     *
     * @param liveKeys the keys of the trades currently in the villager's buying list
     */
    public static void pruneTrades(EntityVillager villager, Set<String> liveKeys) {
        NBTTagCompound trades = trades(villager);
        for (String key : trades.getKeySet().toArray(new String[0])) {
            if (!liveKeys.contains(key)) {
                trades.removeTag(key);
            }
        }
    }
}
