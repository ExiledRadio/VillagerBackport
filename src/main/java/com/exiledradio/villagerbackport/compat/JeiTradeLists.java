package com.exiledradio.villagerbackport.compat;

import com.exiledradio.villagerbackport.ModConfig;
import com.exiledradio.villagerbackport.VillagerBackport;
import com.exiledradio.villagerbackport.trade.ModernTrades;
import com.exiledradio.villagerbackport.trade.TradeTables;

import net.minecraft.entity.passive.EntityVillager;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.common.registry.VillagerRegistry;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.lang.reflect.Field;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Shows JEI Villagers the trades villagers actually sell.
 *
 * <h2>The problem</h2>
 * JEI Villagers lists a career's trades by reading the trade list the career registered with Forge,
 * once, while JEI starts. Villagers here sell from {@link ModernTrades} instead, so it listed
 * 1.12.2's trades - including enchanted books priced the old way - for villagers that will never
 * offer them.
 *
 * <h2>The fix</h2>
 * For the length of JEI's startup only, each career that has a 1.14 table gets that table in place
 * of its registered one; the moment JEI is done, the registered lists go back. JEI's startup happens
 * on the client as the game finishes loading, before any world - so no villager anywhere is building
 * trades while the swap is in place, and villagers still on 1.12.2's trades keep unlocking exactly
 * what they always would.
 *
 * <p>Only with {@code trades.table = MODERN}, and only when JEI Villagers is installed.
 */
public final class JeiTradeLists {

    private static final Map<VillagerRegistry.VillagerCareer, List<List<EntityVillager.ITradeList>>> SWAPPED =
            new IdentityHashMap<VillagerRegistry.VillagerCareer, List<List<EntityVillager.ITradeList>>>();

    private static Field trades;

    private JeiTradeLists() {
    }

    /**
     * The career's registered trades for one level, as the career registered them - looking through
     * the swap if JEI's startup is in progress. Everything in this mod that means "1.12.2's trades"
     * reads them through here, so a villager on the old table cannot pick up a 1.14 trade even if
     * JEI restarts (F3+T) while a world is open.
     *
     * @param level zero-based, as {@code VillagerCareer.getTrades} takes it
     */
    @javax.annotation.Nullable
    public static synchronized List<EntityVillager.ITradeList> registeredTrades(VillagerRegistry.VillagerCareer career, int level) {
        List<List<EntityVillager.ITradeList>> original = SWAPPED.get(career);
        if (original == null) {
            return career.getTrades(level);
        }
        return level >= 0 && level < original.size() ? original.get(level) : null;
    }

    /** Puts the 1.14 tables in place, for JEI Villagers to read. */
    public static synchronized void swapIn() {
        if (!SWAPPED.isEmpty() || !Loader.isModLoaded("jeivillagers")
                || ModConfig.trades.table != TradeTables.Table.MODERN) {
            return;
        }
        try {
            Field field = field();
            for (VillagerRegistry.VillagerProfession profession : ForgeRegistries.VILLAGER_PROFESSIONS) {
                for (VillagerRegistry.VillagerCareer career : VillagerAccess.getCareers(profession)) {
                    List<List<EntityVillager.ITradeList>> modern =
                            ModernTrades.asCareerTrades(profession.getRegistryName() + "/" + career.getName());
                    if (modern == null) {
                        continue;
                    }
                    @SuppressWarnings("unchecked")
                    List<List<EntityVillager.ITradeList>> original =
                            (List<List<EntityVillager.ITradeList>>) field.get(career);
                    SWAPPED.put(career, original);
                    field.set(career, modern);
                }
            }
        } catch (Exception e) {
            VillagerBackport.LOGGER.warn("Could not show 1.14's trades to JEI Villagers.", e);
            restore();
        }
    }

    /** Puts every career's registered trades back. Safe to call any number of times. */
    public static synchronized void restore() {
        if (SWAPPED.isEmpty()) {
            return;
        }
        try {
            Field field = field();
            for (Map.Entry<VillagerRegistry.VillagerCareer, List<List<EntityVillager.ITradeList>>> entry : SWAPPED.entrySet()) {
                field.set(entry.getKey(), entry.getValue());
            }
        } catch (Exception e) {
            VillagerBackport.LOGGER.error("Could not put the registered villager trades back after JEI.", e);
        }
        SWAPPED.clear();
    }

    private static Field field() {
        if (trades == null) {
            trades = ReflectionHelper.findField(VillagerRegistry.VillagerCareer.class, "trades");
            trades.setAccessible(true);
        }
        return trades;
    }
}
