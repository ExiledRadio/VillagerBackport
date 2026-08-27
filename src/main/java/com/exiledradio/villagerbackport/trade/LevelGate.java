package com.exiledradio.villagerbackport.trade;

import com.exiledradio.villagerbackport.ModConfig;
import com.exiledradio.villagerbackport.compat.VillagerAccess;
import com.exiledradio.villagerbackport.data.VillagerTradeData;

import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.PotionEffect;
import net.minecraft.village.MerchantRecipeList;

/**
 * Applies villager level-ups on 1.14's terms: after the trade screen closes, and only when the
 * villager has actually earned one.
 *
 * <h2>What 1.12.2 gets wrong</h2>
 * Both versions share the same reset block in {@code updateAITasks} - count down from 40, then
 * unlock a tier of trades and apply a regeneration effect. What differs is when it is armed:
 *
 * <pre>
 * 1.14  onTrade:     if (canLevelUp())                      { timeUntilReset = 40; flag = true; }
 * 1.12  useRecipe:   if (toolUses == 1 || rand(5) == 0)      { timeUntilReset = 40; flag = true; }
 * </pre>
 *
 * <p>So in 1.14 the block is a <em>level-up</em>, and the regeneration particles are its cue. In
 * 1.12.2 it is an ordinary post-trade event that also happens to hand out a tier of trades, which
 * is why particles appeared after a first trade that earned nothing.
 *
 * <p>Both versions also guard the block with "not currently trading", which is the deferral: the
 * countdown cannot expire while the screen is open, so the level-up lands once the player walks
 * away. That behaviour is kept here.
 *
 * <h2>How this works</h2>
 * Vanilla's reset is cancelled outright, and the level-up is applied deliberately when the villager
 * has both earned it and closed the screen. The applied level is stored separately from experience,
 * so experience can keep accruing - and the bar can sit full - while the level itself waits.
 */
public final class LevelGate {

    /** Duration of the level-up regeneration effect, matching both versions' 200 ticks. */
    private static final int CELEBRATION_TICKS = 200;

    /**
     * Ticks between closing the screen and the level-up landing. Both versions set
     * {@code timeUntilReset = 40} and only count it down once trading has stopped.
     */
    private static final int LEVEL_UP_DELAY_TICKS = 40;

    /** Entity status for a burst of VILLAGER_HAPPY particles, the same value in both versions. */
    private static final byte LEVEL_UP_PARTICLE_STATUS = 14;

    private LevelGate() {
    }

    /**
     * Keeps a villager's level and unlocked trades in step with its experience.
     *
     * <p>Called from the villager tick.
     */
    public static void apply(EntityVillager villager) {
        // Always cancel vanilla's reset, even with level gating switched off. It is the thing that
        // fires regeneration particles after trades that earned nothing, and its tier unlock would
        // fight the level-up applied below.
        VillagerAccess.cancelPendingReset(villager);

        int applied = VillagerTradeData.getAppliedLevel(villager);

        if (applied < 0) {
            // First sight. Adopt whatever the villager already has rather than levelling it from
            // scratch, so villagers that predate this mod keep the trades they came with.
            VillagerTradeData.setAppliedLevel(villager, adoptedLevel(villager));
            return;
        }

        // Repair a level adopted by an earlier version, which took vanilla's counter at face value
        // and so stranded long-lived villagers above Master. Left alone they are stuck there.
        if (applied > VillagerLevel.MAX_LEVEL) {
            applied = VillagerLevel.MAX_LEVEL;
            VillagerTradeData.setAppliedLevel(villager, applied);
        }

        // 1.14 will not let a level-up land while the screen is open - its reset block is guarded by
        // "not currently trading". Holding off here is what lets the bar fill and stay full until
        // the player closes the screen, with the rank and the new trades arriving together after.
        if (villager.getCustomer() != null || !canLevelUp(villager, applied)) {
            VillagerTradeData.clearLevelUpAt(villager);
            return;
        }

        // Then wait out vanilla's own countdown before it lands. Both versions set timeUntilReset to
        // 40 and only decrement it once the screen is closed, so a level-up arrives about two
        // seconds after walking away rather than the instant the screen shuts. That pause is what
        // makes the particles read as the villager reacting rather than as part of closing the
        // window, and without it the whole thing happened too fast to notice.
        long now = villager.world.getTotalWorldTime();
        long scheduled = VillagerTradeData.getLevelUpAt(villager);

        if (scheduled < 0) {
            VillagerTradeData.setLevelUpAt(villager, now + LEVEL_UP_DELAY_TICKS);
            return;
        }

        // Guard against a world whose time ran backwards, which would otherwise strand the level-up
        // until the clock caught back up.
        if (now < scheduled && now >= scheduled - LEVEL_UP_DELAY_TICKS) {
            return;
        }

        VillagerTradeData.clearLevelUpAt(villager);
        levelUp(villager, applied + 1);
    }

    /**
     * @return the level to start a villager we have never seen before on
     *
     * <h2>Why vanilla's counter cannot be used as-is</h2>
     * {@code careerLevel} is not a tier. {@code populateBuyingList} increments it on every restock
     * whether or not anything unlocked, so a villager that has been restocking since world creation
     * reports numbers far past the five levels that exist - one in a test world had reached 17.
     *
     * <p>Adopting that verbatim put the villager above Master, where {@code canLevelUp} is false
     * forever: no further tiers, no experience bar drawn, and no route back down however much it
     * was traded with. Clamping to the real range keeps it inside the ladder, and taking whatever
     * its experience has already earned means the clamp can never demote a villager below the level
     * it can prove it deserves.
     */
    private static int adoptedLevel(EntityVillager villager) {
        int career = Math.min(VillagerAccess.getCareerLevel(villager), VillagerLevel.MAX_LEVEL);
        int earned = VillagerLevel.levelFor(VillagerTradeData.getXp(villager));

        return Math.max(VillagerLevel.MIN_LEVEL, Math.max(career, earned));
    }

    /** 1.14's {@code canLevelUp}: below the cap, and enough experience for the next threshold. */
    private static boolean canLevelUp(EntityVillager villager, int applied) {
        return VillagerLevel.canLevelUp(applied)
                && VillagerTradeData.getXp(villager) >= VillagerLevel.xpForNextLevel(applied);
    }

    /**
     * Advances one level, unlocks that tier's trades, and plays the cue.
     *
     * <p>One level at a time even when enough experience arrived at once to cross two thresholds -
     * a bulk trade should not skip a tier of trades. The next tick picks up the next level.
     */
    private static void levelUp(EntityVillager villager, int level) {
        if (ModConfig.pricing.levelGatedTrades) {
            // Counted before the unlock so only what the new tier added gets announced, rather than
            // every book the villager has ever offered.
            MerchantRecipeList before = VillagerAccess.getBuyingList(villager);
            int had = before == null ? 0 : before.size();

            VillagerAccess.unlockTier(villager, level);
            BookAnnouncer.announce(villager, VillagerAccess.getBuyingList(villager), had);
        }

        VillagerTradeData.setAppliedLevel(villager, level);

        // The regeneration effect is the level-up cue both versions apply directly.
        villager.addPotionEffect(new PotionEffect(MobEffects.REGENERATION, CELEBRATION_TICKS, 0));

        // The green sparkle is a separate signal, sent as an entity status rather than an effect.
        // 1.14 fires it from its own branch of updateAITasks; both versions map status 14 to
        // VILLAGER_HAPPY particles, so one call gets the same burst here. Without it only the
        // regeneration swirls showed, which is a much weaker cue than 1.14's.
        villager.world.setEntityState(villager, LEVEL_UP_PARTICLE_STATUS);

        // Nearby clients need the new rank so the badge changes with the particles rather than the
        // next time someone walks away and back.
        LevelSyncHandler.broadcastLevelUp(villager, level);
    }
}
