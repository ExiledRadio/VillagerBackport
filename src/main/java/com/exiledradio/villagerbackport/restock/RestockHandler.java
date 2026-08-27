package com.exiledradio.villagerbackport.restock;

import com.exiledradio.villagerbackport.ModConfig;
import com.exiledradio.villagerbackport.compat.VillagerAccess;
import com.exiledradio.villagerbackport.data.VillagerTradeData;
import com.exiledradio.villagerbackport.job.Employment;
import com.exiledradio.villagerbackport.job.JobSite;
import com.exiledradio.villagerbackport.job.JobSiteWork;
import com.exiledradio.villagerbackport.trade.Gossip;
import com.exiledradio.villagerbackport.trade.LevelGate;
import com.exiledradio.villagerbackport.trade.TradeKey;
import com.exiledradio.villagerbackport.trade.TradeTracker;

import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.village.MerchantRecipe;
import net.minecraft.village.MerchantRecipeList;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Set;

/**
 * Ports 1.14's villager restock rules onto 1.12.2's villagers.
 *
 * <h2>Why 1.12.2 villagers lock up</h2>
 * Vanilla 1.12.2 only ever arms a restock as a side effect of a successful trade. In
 * {@code EntityVillager.useRecipe} the countdown is set behind
 * {@code if (recipe.getToolUses() == 1 || rand.nextInt(5) == 0)} - so after the first use of a
 * trade, every further restock depends on a one-in-five roll. If a villager's trades all reach
 * {@code toolUses >= maxTradeUses} without that roll coming up, there is no usable trade left,
 * {@code useRecipe} is never called again, the countdown is never armed, and the villager is
 * bricked permanently. There is no timer anywhere in 1.12.2 that can rescue it.
 *
 * <h2>What 1.14 does instead</h2>
 * 1.14 moves the decision out of the trade handler and onto a clock, which is what makes it
 * robust: {@code canRestock()} is {@code restocksToday < 2 && gameTime > lastRestock + 2400},
 * and restocking sets every offer's uses back to zero rather than raising its cap. No roll, no
 * dependency on the player trading again, and a hard limit of two restocks per day so it cannot
 * be farmed. This class implements exactly that predicate against 1.12.2's
 * {@link MerchantRecipe}, whose {@code toolUses}/{@code maxTradeUses} pair is a clean one-to-one
 * match for 1.14's {@code uses}/{@code maxUses}.
 *
 * <h2>Compatibility</h2>
 * This runs off {@link LivingEvent.LivingUpdateEvent} and edits the recipe list the villager
 * already owns. It does not subclass {@link EntityVillager}, replace entities on spawn, patch
 * bytecode, or register anything into {@code VillagerRegistry}. That matters in a large pack:
 * mods add villager content by registering professions and careers, and their trades land in the
 * same {@code buyingList} this reads, so they get restocking for free without this mod ever
 * naming them.
 */
public final class RestockHandler {

    /** For the arrival path, which has a villager but no handler. */
    private static final RestockHandler INSTANCE = new RestockHandler();

    /**
     * How often to evaluate a villager, in ticks. 1.14 checks restock eligibility inside the brain
     * task that walks a villager to its job site, so it is naturally infrequent - the check only
     * happens when the villager gets there.
     *
     * <p>Here the two are separate: the work goal handles walking, and this handles the decision, so
     * the rate is set by hand. Once a second is far more often than a restock can actually fire -
     * the cooldown alone is two minutes - and keeps the per-tick cost off a pack that may have
     * hundreds of villagers loaded.
     */
    private static final int CHECK_INTERVAL_TICKS = 20;

    /**
     * How often to evaluate a villager that has a player trading with it. Two ticks is a tenth of a
     * second, fast enough that the experience bar moves as the trade completes rather than visibly
     * catching up afterwards.
     */
    private static final int TRADING_INTERVAL_TICKS = 2;

    /**
     * How often a villager repeats what it thinks to a neighbour.
     *
     * <p>A minute apart. 1.14 does this at the meeting point around midday, so it is rare there too,
     * and it wants to be rare here: every pass is an entity lookup, and gossip that spread on every
     * tick would be all over a village in seconds rather than making its way round over a day.
     */
    private static final int GOSSIP_INTERVAL_TICKS = 1200;

    /** Ticks in a Minecraft day, used to convert world time into a day number. */
    private static final long TICKS_PER_DAY = 24000L;

    @SubscribeEvent
    public void onLivingUpdate(LivingEvent.LivingUpdateEvent event) {
        if (!(event.getEntityLiving() instanceof EntityVillager)) {
            return;
        }

        EntityVillager villager = (EntityVillager) event.getEntityLiving();

        // Trades only exist server-side; the client gets a snapshot over the wire. Doing this
        // work on the client would desync the two copies of the recipe list.
        if (villager.world.isRemote || !VillagerAccess.isAvailable()) {
            return;
        }

        // Note there is deliberately no "everything is disabled, do nothing" shortcut here. The poll
        // is also what puts demand surcharges back to base prices, so it has to keep running after
        // the features are switched off - otherwise turning pricing off would freeze every trade at
        // whatever it last cost. Once prices are back at base the work is a no-op, and it is
        // throttled to once a second per villager regardless.

        // Babies have no trades, and vanilla builds their list only once they grow up. They do get
        // a profession though, which they should not have until they are old enough to work.
        if (villager.isChild()) {
            if ((villager.ticksExisted + villager.getEntityId()) % CHECK_INTERVAL_TICKS == 0) {
                Employment.keepChildUnemployed(villager);
            }
            return;
        }

        // Spread the checks across ticks instead of hitting every villager on the same one. The
        // entity id offset means a village full of villagers evaluates a few per tick rather
        // than all at once every twentieth tick.
        //
        // A villager with someone trading is checked far more often. That is where completed trades
        // are noticed and the experience total pushed to the player's screen, and at the normal
        // interval the progress bar would lag up to a second behind the trade that moved it. Only
        // one villager can have a customer at a time, so the extra work is negligible.
        int interval = villager.getCustomer() != null ? TRADING_INTERVAL_TICKS : CHECK_INTERVAL_TICKS;
        if ((villager.ticksExisted + villager.getEntityId()) % interval != 0) {
            return;
        }

        tick(villager);
    }

    /**
     * Runs a villager's evaluation now, rather than waiting for its next scheduled one.
     *
     * <p>Called the moment a villager reaches its workstation. Without it, arriving and restocking
     * are separate events up to a second apart, and in that second the goal that walked the villager
     * there has already finished - releasing it to whatever wants to move it next. The villager gets
     * walked off, notices its trades are still sold out, and comes back: a round trip per second
     * until a poll happens to catch it standing still. Doing the work on arrival closes that gap.
     */
    public static void workNow(EntityVillager villager) {
        if (!villager.world.isRemote && VillagerAccess.isAvailable() && !villager.isChild()) {
            INSTANCE.tick(villager);
        }
    }

    /**
     * One evaluation of a villager: account for any trades completed since we last looked, then
     * decide whether to restock.
     *
     * <p>The two halves are ordered deliberately. Polling first means the experience and demand
     * bookkeeping is up to date with the use counts <em>before</em> a restock zeroes them, so a
     * villager that is restocked on the same tick a trade completes still gets credit for it.
     */
    private void tick(EntityVillager villager) {
        // Employment is checked before trades, because it decides whether there should be any. A
        // villager that just lost its job has its list cleared here and nothing left to poll.
        Employment.initialise(villager);
        checkJobLoss(villager);

        MerchantRecipeList recipes = VillagerAccess.getBuyingList(villager);
        if (recipes == null || recipes.isEmpty()) {
            return;
        }

        // Opinions wear off and spread whether or not anything is being bought - see Gossip.
        Gossip.decayIfDue(villager);
        if ((villager.ticksExisted + villager.getEntityId()) % GOSSIP_INTERVAL_TICKS == 0) {
            Gossip.shareWith(villager, villager.getRNG());
        }

        repairDemand(villager, recipes);

        Set<String> liveKeys = TradeTracker.poll(villager, recipes);

        // After polling, so a level reached by the trades just counted unlocks its tier immediately
        // rather than a tick later.
        LevelGate.apply(villager);

        if (ModConfig.restock.enabled) {
            tryRestock(villager, recipes, liveKeys);
        }
    }

    /**
     * Takes a villager's job away if it has lost its workstation and never earned anything.
     *
     * <p>1.14's rule exactly: no experience and still at the first level. Trading with a villager
     * even once makes its profession permanent - the workstation then only controls whether it can
     * restock, not whether it keeps its trades.
     */
    private void checkJobLoss(EntityVillager villager) {
        if (JobSite.validated(villager) != null || !Employment.shouldLoseJob(villager)) {
            return;
        }

        Employment.unassign(villager);
    }

    /**
     * Runs 1.14's restock decision for one villager.
     *
     * <p>Order matters here and mirrors 1.14's {@code shouldRestock()}: the daily counter is
     * rolled over <em>before</em> eligibility is tested, so a villager that comes back into a
     * loaded chunk on a new day is immediately eligible rather than having to wait out a check
     * cycle first.
     */
    private void tryRestock(EntityVillager villager, MerchantRecipeList recipes, Set<String> liveKeys) {
        // Stay out of the way while vanilla's own 40-tick reset countdown is running, so the two
        // never restock the same villager on the same tick.
        //
        // Note that reset no longer unlocks a tier: LevelGate clears the flag that would have, so
        // tiers follow the experience level instead. What is left of vanilla's reset is the
        // countdown itself and its regeneration effect.
        if (VillagerAccess.isVanillaResetPending(villager)) {
            return;
        }

        // Never while the trade screen is open - see RestockRules.couldRestockNow. Checked here as
        // well as there because this is the path that actually restocks, and the poll runs ten times
        // more often for a villager with a customer than for one without.
        if (villager.getCustomer() != null) {
            return;
        }

        long gameTime = villager.world.getTotalWorldTime();

        rollOverDay(villager, recipes, gameTime);

        if (!RestockRules.hasAllowance(villager, gameTime) || !RestockRules.hasExhaustedTrade(recipes)) {
            return;
        }

        // 1.14 restocks from inside the task that walks a villager to its workstation, so being
        // there is a precondition rather than a check. Same rule, stated explicitly.
        if (!JobSiteWork.canRestock(villager)) {
            return;
        }

        if (!RestockRules.isWorkingHours(villager.world)) {
            return;
        }

        restock(villager, recipes, gameTime);

        // Only prune on a restock. Doing it every poll would fight vanilla's career level-ups,
        // which append a tier of trades between our readings - a key absent from one poll may be
        // back in the next.
        VillagerTradeData.pruneTrades(villager, liveKeys);
    }

    /**
     * Resets the daily restock allowance when a new day has started.
     *
     * <p>1.14 checks this two ways and so do we, because either one alone has a hole. Comparing
     * day numbers from world time handles the normal case, but world time can be frozen
     * ({@code doDaylightCycle false}) or moved backwards by a command, and then the day number
     * never advances and villagers would stop restocking for good. The elapsed-game-time check
     * covers that: game time is monotonic and unaffected by both. A villager that was unloaded
     * across a day boundary is caught by the day-number branch when it loads back in.
     */
    private void rollOverDay(EntityVillager villager, MerchantRecipeList recipes, long gameTime) {
        long lastRestock = VillagerTradeData.getLastRestock(villager);
        long worldTime = villager.world.getWorldTime();
        long lastDayCheck = VillagerTradeData.getLastDayCheck(villager);

        boolean newDay = lastRestock != Long.MIN_VALUE
                && gameTime > lastRestock + ModConfig.restock.dailyResetTicks;

        if (lastDayCheck >= 0) {
            newDay |= (worldTime / TICKS_PER_DAY) > (lastDayCheck / TICKS_PER_DAY);
        }

        VillagerTradeData.setLastDayCheck(villager, worldTime);

        // Settling unused restocks is destructive, so it happens on the day actually turning over
        // and nowhere else. The condition above is not that edge: it stays true on every check from
        // the moment half a day has passed until the next restock, which had the settling run once a
        // second - clearing every trade's stock and shedding two stocks' worth of demand each time.
        long day = worldTime / TICKS_PER_DAY;
        long settled = VillagerTradeData.getLastCatchUpDay(villager);

        if (settled != day) {
            VillagerTradeData.setLastCatchUpDay(villager, day);

            // Never on first sight: a villager loaded today has not been idle for a day, it has
            // simply never been asked before.
            if (settled >= 0) {
                catchUpDemand(villager, recipes);
                VillagerTradeData.setRestocksToday(villager, 0);
            }
        }

        if (newDay) {
            VillagerTradeData.setRestocksToday(villager, 0);
        }
    }

    /**
     * Clears demand driven negative by the runaway catch-up in 0.49.
     *
     * <p>That bug settled a villager's unused restocks once a second instead of once a day, which
     * pushed every trade's demand into the floor within minutes. Negative demand is legitimate - it
     * is how 1.14 records a trade nobody wants - but a value that got there by accident would take
     * days of heavy trading to work off, and until it did, prices could not rise at all.
     *
     * <p>There is no way to recover what the value should have been, so it is put back to neutral
     * and left to build again honestly. Run once per villager.
     */
    private void repairDemand(EntityVillager villager, MerchantRecipeList recipes) {
        if (VillagerTradeData.isDemandRepaired(villager)) {
            return;
        }

        VillagerTradeData.setDemandRepaired(villager);

        for (MerchantRecipe recipe : recipes) {
            String key = TradeKey.of(villager, recipe);

            if (VillagerTradeData.getDemand(villager, key) < 0) {
                VillagerTradeData.setDemand(villager, key, 0);
            }
        }
    }

    /**
     * 1.14's {@code catchUpDemand()}: settle the restocks a villager never got round to using.
     *
     * <h2>Why demand has to move on a day nothing happened</h2>
     * Demand only rolls forward inside a restock, and a restock only happens when a trade has sold
     * out. So a villager nobody buys from never restocks, and without this its demand would be
     * frozen at whatever the last busy day left it at - a trade that was expensive once would stay
     * expensive for good, however long the village went quiet.
     *
     * <p>1.14 settles that on waking: whatever allowance went unspent yesterday is run through
     * {@code updateDemand} now, against use counts of zero. Each unused restock therefore sheds a
     * full stock's worth of demand, which is exactly what a restock would have done for a trade
     * nobody touched. Prices come down for a village that has been left alone, at the same rate they
     * would have come down had anyone been trading.
     *
     * <p>Uses are cleared first, as 1.14 does, so the demand being settled is the demand of an
     * untouched trade rather than of the last thing that happened to it.
     */
    private void catchUpDemand(EntityVillager villager, MerchantRecipeList recipes) {
        int unspent = ModConfig.restock.maxRestocksPerDay - VillagerTradeData.getRestocksToday(villager);
        if (unspent <= 0 || recipes == null) {
            return;
        }

        for (MerchantRecipe recipe : recipes) {
            VillagerAccess.resetUses(recipe);
        }

        for (int i = 0; i < unspent; i++) {
            TradeTracker.updateDemandAndReset(villager, recipes);
        }
    }

    /**
     * 1.14's {@code restock()}: clear every trade's use count and spend one daily allowance.
     *
     * <p>Resetting uses rather than raising the cap is the important half. Vanilla 1.12.2's own
     * reset calls {@code increaseMaxTradeUses(...)}, which ratchets the cap up a little every
     * time; over a long-lived world a heavily traded villager drifts toward unlimited stock.
     * Zeroing uses restores the trade to exactly its author's intended stock instead, so a trade
     * another mod declared with a deliberately small {@code maxTradeUses} stays scarce.
     */
    private void restock(EntityVillager villager, MerchantRecipeList recipes, long gameTime) {
        // Demand is computed from the use counts of the period now ending, so it has to be rolled
        // forward before those counts are cleared. This is the same order 1.14 uses inside
        // restock(): updateDemand() on every offer, then resetUses().
        TradeTracker.updateDemandAndReset(villager, recipes);

        for (MerchantRecipe recipe : recipes) {
            VillagerAccess.resetUses(recipe);
        }

        VillagerTradeData.setLastRestock(villager, gameTime);
        VillagerTradeData.setRestocksToday(villager, VillagerTradeData.getRestocksToday(villager) + 1);

        // 1.14 plays the villager's work sound as it uses the workstation, which is the only outward
        // sign that a restock has happened at all - without it the shop refills silently and the
        // only way to find out is to open the screen and look.
        if (ModConfig.restock.playWorkSound) {
            WorkSounds.playFor(villager);
        }
    }
}
