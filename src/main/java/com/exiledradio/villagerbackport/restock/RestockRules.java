package com.exiledradio.villagerbackport.restock;

import com.exiledradio.villagerbackport.ModConfig;
import com.exiledradio.villagerbackport.data.VillagerTradeData;

import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.village.MerchantRecipe;
import net.minecraft.village.MerchantRecipeList;
import net.minecraft.world.World;

/**
 * When a villager is allowed to restock.
 *
 * <h2>Why this is not simply part of the restock itself</h2>
 * Because two things need the answer, and they used to disagree. Restocking is done by
 * {@link RestockHandler} when a villager is at its workstation; walking it there is a separate AI
 * goal, and that goal asked a different and much weaker question - "is anything sold out?" - with no
 * regard for whether a restock could actually follow.
 *
 * <p>What that produces is a villager that walks to its workstation, cannot restock, gets released
 * by the goal, is picked up by whatever wants to move it next, wanders off, notices its trades are
 * still sold out and walks back. Over and over, at walking-to-work speed, until an allowance
 * happens to come round. Asking one question from both places is what stops that.
 */
public final class RestockRules {

    /** Ticks in a Minecraft day, for turning world time into a time of day. */
    private static final long TICKS_PER_DAY = 24000L;

    private RestockRules() {
    }

    /**
     * @return true if a restock could happen right now, leaving aside where the villager is standing
     *
     * <p>Everything a villager cannot change by walking somewhere: the clock, the daily allowance
     * and the cooldown. What it can change - being at its workstation - is deliberately not asked
     * here, because this is the question asked <em>before</em> setting off.
     */
    public static boolean couldRestockNow(EntityVillager villager) {
        if (!ModConfig.restock.enabled) {
            return false;
        }

        // Not while somebody is looking at the shop. 1.14 cannot restock a villager that has a
        // customer because restocking happens in a work task and a villager being traded with is not
        // running one - it is stood still, facing the player, for as long as the screen is open.
        //
        // Doing it anyway refills the shelves under the player's hands mid-session: trades that were
        // sold out a moment ago silently work again, stock counts jump, and a daily allowance is
        // spent on a villager that was going to be restocked properly the moment the player walked
        // away. It also lets one player hold a villager open and drain it past its daily limit.
        if (villager.getCustomer() != null) {
            return false;
        }

        return isWorkingHours(villager.world)
                && hasAllowance(villager, villager.world.getTotalWorldTime());
    }

    /**
     * @return true if it is a time of day a villager would be at work
     *
     * <h2>Why a clock is part of restocking at all</h2>
     * 1.14 never asks whether a villager may restock outside working hours, because the question can
     * only come up inside {@code WorkAtPoi} - a task in the WORK activity, which the villager
     * schedule turns on at 2000 and off at 9000. Everything either side of that is idling, gathering
     * at the meeting point, or asleep.
     *
     * <p>Leaving that out is what made "twice a day" wrong rather than merely approximate. The daily
     * allowance comes back 12000 ticks after the last restock, which is half a day - so a villager
     * standing at its workstation with nothing to stop it simply spends two more restocks every half
     * day, all night included. The cap was never the limit in 1.14; the working day was.
     */
    public static boolean isWorkingHours(World world) {
        if (!ModConfig.restock.workHoursOnly) {
            return true;
        }

        long timeOfDay = world.getWorldTime() % TICKS_PER_DAY;
        int start = ModConfig.restock.workStartTime;
        int end = ModConfig.restock.workEndTime;

        if (start <= end) {
            return timeOfDay >= start && timeOfDay < end;
        }

        // A window configured to run through midnight.
        return timeOfDay >= start || timeOfDay < end;
    }

    /**
     * 1.14's {@code allowedToRestock()}: allowance left today, and the cooldown elapsed.
     *
     * <p>The cooldown is what stops a player standing at a villager and draining it indefinitely;
     * the daily cap is what stops the same over a longer window. The day's first restock is exempt
     * from the cooldown, as 1.14's {@code numberOfRestocksToday == 0} branch has it - nothing has
     * been drained yet for the two minutes to be protecting against.
     */
    public static boolean hasAllowance(EntityVillager villager, long gameTime) {
        int spent = VillagerTradeData.getRestocksToday(villager);

        if (spent >= ModConfig.restock.maxRestocksPerDay) {
            return false;
        }

        if (spent == 0) {
            return true;
        }

        long lastRestock = VillagerTradeData.getLastRestock(villager);

        // Never restocked - always eligible. See the sentinel note on getLastRestock.
        if (lastRestock == Long.MIN_VALUE) {
            return true;
        }

        // Guard against a world whose time ran backwards (a /time set, or a restored backup).
        // Without this the villager would be locked out until game time caught back up.
        if (gameTime < lastRestock) {
            return true;
        }

        return gameTime > lastRestock + ModConfig.restock.cooldownTicks;
    }

    /**
     * @return true if any trade is used up, matching 1.14's check for an offer with
     * {@code uses >= maxUses}.
     *
     * <p>1.12.2 spells the same condition {@link MerchantRecipe#isRecipeDisabled()}. Restocking a
     * villager whose trades are all still available would burn a daily allowance for nothing, so
     * this gate is what makes the two-per-day budget mean something.
     */
    public static boolean hasExhaustedTrade(MerchantRecipeList recipes) {
        if (recipes == null) {
            return false;
        }

        for (MerchantRecipe recipe : recipes) {
            if (recipe.isRecipeDisabled()) {
                return true;
            }
        }

        return false;
    }
}
