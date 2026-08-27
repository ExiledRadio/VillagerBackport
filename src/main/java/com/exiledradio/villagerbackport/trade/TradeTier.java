package com.exiledradio.villagerbackport.trade;

import com.exiledradio.villagerbackport.compat.VillagerAccess;

import net.minecraft.entity.passive.EntityVillager;
import net.minecraftforge.fml.common.registry.VillagerRegistry;

import java.util.List;

/**
 * Works out which tier a trade belongs to, from its position in the villager's trade list.
 *
 * <h2>Why position rather than career level</h2>
 * The obvious source looks like {@code EntityVillager.careerLevel}, and it is wrong.
 * {@code populateBuyingList()} increments it on every call and only afterwards asks the career for
 * that level's trades - and it is called on every restock, whether or not any trades are left to
 * unlock. So it counts restocks, not progress, and climbs indefinitely. Using it as a tier made
 * every trade on a well-used villager read as tier five, which is what made the experience preview
 * fill the whole bar regardless of what was selected.
 *
 * <p>What is reliable is order. {@code populateBuyingList()} appends each tier's trades to the end
 * of the list as they unlock, so the list is grouped by tier from the start. Walking the career's
 * own tier definitions and counting how many trades each contributes gives the boundaries, and a
 * trade's index says which group it falls in.
 *
 * <p>This reads the career definitions live from {@link VillagerRegistry}, so it works for
 * professions added by other mods without knowing anything about them.
 */
public final class TradeTier {

    /** Upper bound on tiers to walk, guarding against a career that never returns null. */
    private static final int MAX_TIERS_SCANNED = 32;

    private TradeTier() {
    }

    /**
     * @param villager    the villager
     * @param recipeIndex position of the trade in the villager's buying list
     * @return the tier, 1 to 5
     */
    public static int forIndex(EntityVillager villager, int recipeIndex) {
        if (recipeIndex < 0) {
            return 1;
        }

        VillagerRegistry.VillagerCareer career = careerOf(villager);
        if (career == null) {
            return 1;
        }

        int cumulative = 0;
        for (int tier = 0; tier < MAX_TIERS_SCANNED; tier++) {
            List<EntityVillager.ITradeList> trades = career.getTrades(tier);
            if (trades == null) {
                break;
            }

            cumulative += trades.size();
            if (recipeIndex < cumulative) {
                return Math.min(VillagerLevel.MAX_LEVEL, tier + 1);
            }
        }

        // Past everything the career defines. That means a trade another mod appended directly to
        // the list rather than registering through the career system - RLCraft Villager Tomes does
        // exactly this with player-taught enchanted books. Treated as tier one, because guessing
        // low costs a slightly stingy experience reward while guessing high would hand out the
        // maximum for every trade a mod adds.
        return 1;
    }

    private static VillagerRegistry.VillagerCareer careerOf(EntityVillager villager) {
        try {
            VillagerRegistry.VillagerProfession profession = villager.getProfessionForge();
            if (profession == null) {
                return null;
            }

            // Career ids are stored one-based, with 0 meaning the villager has not been assigned
            // one yet - which happens before its trade list is first built.
            int id = VillagerAccess.getCareerId(villager);
            if (id <= 0) {
                return null;
            }

            return profession.getCareer(id - 1);
        } catch (RuntimeException e) {
            // getCareer throws on an out-of-range id, which a mod reassigning professions can
            // produce. Falling back to tier one is preferable to interrupting the trade screen.
            return null;
        }
    }
}
