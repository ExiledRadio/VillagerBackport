package com.exiledradio.villagerbackport.trade;

import com.exiledradio.villagerbackport.ModConfig;
import com.exiledradio.villagerbackport.VillagerBackport;
import com.exiledradio.villagerbackport.compat.VillagerAccess;
import com.exiledradio.villagerbackport.data.VillagerTradeData;

import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.village.MerchantRecipe;
import net.minecraft.village.MerchantRecipeList;
import net.minecraftforge.fml.common.registry.VillagerRegistry;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Which trade table a villager's trades come from, and building them from it.
 *
 * <h2>Two tables</h2>
 * <b>Modern</b> is 1.14's, from {@link ModernTrades}: each level a pool, two trades drawn from it.
 * <b>Legacy</b> is whatever the career registered with Forge - 1.12.2's own trades, plus anything
 * another mod added to a career. {@code trades.table} picks which one new villagers get.
 *
 * <h2>A villager keeps the table it was built with</h2>
 * Each villager records the table its list came from. A villager anyone has traded with keeps it for
 * good, and keeps unlocking from it as it levels, so switching the setting never takes away or
 * reshuffles trades a player has worked for. A villager nobody has traded with has nothing to lose,
 * so it is rebuilt from the configured table the next time it is looked at - which is how villages
 * that existed before this update get the new trades.
 *
 * <h2>Careers 1.14 never had</h2>
 * A career with no 1.14 table, which means one another mod added, uses its own registered trades
 * whatever the setting says.
 */
public final class TradeTables {

    /** Records which table built the villager's current trade list. */
    private static final String TABLE = "TradeTable";

    /** Trade-list size after each level was added, for villagers built from the modern table. */
    private static final String TIER_ENDS = "TierEnds";

    public enum Table {
        MODERN,
        LEGACY
    }

    private TradeTables() {
    }

    /** @return the career key {@link ModernTrades} is indexed by, or null if the villager has no career */
    @Nullable
    public static String careerKey(EntityVillager villager) {
        VillagerRegistry.VillagerCareer career = careerOf(villager);
        if (career == null) {
            return null;
        }
        VillagerRegistry.VillagerProfession profession = villager.getProfessionForge();
        return profession.getRegistryName() + "/" + career.getName();
    }

    /** @return the table the villager's list was built from, or null if nothing has recorded one */
    @Nullable
    public static Table recorded(EntityVillager villager) {
        NBTTagCompound root = VillagerTradeData.root(villager);
        if (!root.hasKey(TABLE)) {
            return null;
        }
        return "modern".equals(root.getString(TABLE)) ? Table.MODERN : Table.LEGACY;
    }

    /** @return the table a villager building trades now should use */
    private static Table forNewTrades(EntityVillager villager) {
        String key = careerKey(villager);
        if (ModConfig.trades.table == Table.MODERN && key != null && ModernTrades.has(key)) {
            return Table.MODERN;
        }
        return Table.LEGACY;
    }

    private static void record(EntityVillager villager, Table table) {
        VillagerTradeData.root(villager).setString(TABLE, table == Table.MODERN ? "modern" : "legacy");
    }

    /**
     * Builds a new hire's first level of trades, from whichever table it should have.
     *
     * @return the new list
     */
    public static MerchantRecipeList buildFirstTier(EntityVillager villager) {
        MerchantRecipeList list = new MerchantRecipeList();
        Table table = forNewTrades(villager);

        if (table == Table.MODERN) {
            ModernTrades.addTier(careerKey(villager), 1, villager, list, villager.getRNG());
            VillagerTradeData.root(villager).setIntArray(TIER_ENDS, new int[]{list.size()});
        } else {
            VillagerTradeData.root(villager).removeTag(TIER_ENDS);
            addLegacyTier(villager, 1, list);
        }

        record(villager, table);
        return list;
    }

    /**
     * Appends one level's trades, from the table the villager's list came from.
     *
     * @param tier the level being reached, 2 to 5
     */
    public static void unlockTier(EntityVillager villager, int tier) {
        Table table = recorded(villager);
        String key = careerKey(villager);

        if (table == Table.MODERN && key != null && ModernTrades.has(key)) {
            MerchantRecipeList list = VillagerAccess.getBuyingList(villager);
            if (list == null) {
                return;
            }

            ModernTrades.addTier(key, tier, villager, list, villager.getRNG());

            // Kept in step with vanilla's counter, which other code still reads as a progress hint.
            VillagerAccess.setCareerLevel(villager, tier);

            int[] ends = VillagerTradeData.root(villager).getIntArray(TIER_ENDS);
            int[] grown = new int[Math.max(ends.length, tier)];
            System.arraycopy(ends, 0, grown, 0, Math.min(ends.length, grown.length));
            for (int i = ends.length; i < tier - 1; i++) {
                grown[i] = i == 0 ? 0 : grown[i - 1];
            }
            grown[tier - 1] = list.size();
            VillagerTradeData.root(villager).setIntArray(TIER_ENDS, grown);
            return;
        }

        VillagerAccess.unlockTier(villager, tier);
    }

    /**
     * @return the tier a trade at this position unlocked at, or 0 if the villager's own record cannot
     * say, in which case the caller works it out from the career's registered trades
     */
    public static int tierOf(EntityVillager villager, int index) {
        if (recorded(villager) != Table.MODERN) {
            return 0;
        }
        int[] ends = VillagerTradeData.root(villager).getIntArray(TIER_ENDS);
        if (ends.length == 0) {
            return 0;
        }
        for (int i = 0; i < ends.length; i++) {
            if (index < ends[i]) {
                return i + 1;
            }
        }
        // Past every level: a trade another mod appended, read as tier one as TradeTier does.
        return 1;
    }

    /**
     * Settles which table an existing villager's trades belong to, rebuilding them if nobody has
     * traded with it yet and they came from the other table.
     *
     * <p>Called from the villager tick. Cheap once settled: one tag read and one comparison.
     */
    public static void adopt(EntityVillager villager) {
        MerchantRecipeList list = VillagerAccess.getBuyingList(villager);
        if (list == null || list.isEmpty() || villager.getCustomer() != null) {
            return;
        }

        Table recorded = recorded(villager);
        if (recorded == null) {
            // Built before this was tracked, or by vanilla's own spawn path: 1.12.2's career lists.
            recorded = Table.LEGACY;
            record(villager, recorded);
        }

        Table wanted = forNewTrades(villager);
        if (recorded == wanted || !ModConfig.trades.rebuildUntradedVillagers || hasTraded(villager, list)) {
            return;
        }

        // A legacy list holding more than its career's own trades has been added to by something
        // else - another mod teaching it a trade, say. That is the player's doing as surely as a
        // trade is, so it is left exactly as it is.
        if (recorded == Table.LEGACY && list.size() != legacyCount(villager)) {
            return;
        }

        int level = Math.max(1, Math.min(VillagerLevel.MAX_LEVEL, VillagerTradeData.getAppliedLevel(villager)));
        MerchantRecipeList rebuilt = buildFirstTier(villager);
        VillagerAccess.setBuyingList(villager, rebuilt);
        VillagerAccess.setCareerLevel(villager, 1);

        for (int tier = 2; tier <= level; tier++) {
            if (wanted == Table.MODERN) {
                unlockTier(villager, tier);
            } else {
                addLegacyTier(villager, tier, rebuilt);
                VillagerAccess.setCareerLevel(villager, tier);
            }
        }

        VillagerBackport.LOGGER.debug("Rebuilt an untraded villager's trades from the {} table.", wanted);
    }

    /** @return true if a player has ever completed a trade with this villager */
    private static boolean hasTraded(EntityVillager villager, MerchantRecipeList list) {
        if (VillagerTradeData.getXp(villager) > 0) {
            return true;
        }
        for (MerchantRecipe recipe : list) {
            if (recipe.getToolUses() > 0) {
                return true;
            }
        }
        return false;
    }

    /** @return how many trades the career's registered tiers give up to the villager's level */
    private static int legacyCount(EntityVillager villager) {
        VillagerRegistry.VillagerCareer career = careerOf(villager);
        if (career == null) {
            return -1;
        }
        int level = Math.max(1, VillagerTradeData.getAppliedLevel(villager));
        int count = 0;
        for (int tier = 0; tier < level; tier++) {
            List<EntityVillager.ITradeList> trades = career.getTrades(tier);
            if (trades == null) {
                break;
            }
            count += trades.size();
        }
        return count;
    }

    /** Asks the career's registered trades for one level, straight into the list. */
    private static void addLegacyTier(EntityVillager villager, int tier, MerchantRecipeList list) {
        VillagerRegistry.VillagerCareer career = careerOf(villager);
        if (career == null) {
            return;
        }
        try {
            List<EntityVillager.ITradeList> trades = career.getTrades(tier - 1);
            if (trades != null) {
                for (EntityVillager.ITradeList trade : trades) {
                    trade.addMerchantRecipe(villager, list, villager.getRNG());
                }
            }
        } catch (RuntimeException e) {
            VillagerBackport.LOGGER.warn("Could not build level {} trades for a villager.", tier, e);
        }
    }

    @Nullable
    private static VillagerRegistry.VillagerCareer careerOf(EntityVillager villager) {
        try {
            VillagerRegistry.VillagerProfession profession = villager.getProfessionForge();
            int id = VillagerAccess.getCareerId(villager);
            if (profession == null || id <= 0) {
                return null;
            }
            List<VillagerRegistry.VillagerCareer> careers = VillagerAccess.getCareers(profession);
            return id - 1 < careers.size() ? careers.get(id - 1) : null;
        } catch (RuntimeException e) {
            return null;
        }
    }
}
