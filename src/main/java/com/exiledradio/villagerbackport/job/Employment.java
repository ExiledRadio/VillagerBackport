package com.exiledradio.villagerbackport.job;

import com.exiledradio.villagerbackport.ModConfig;
import com.exiledradio.villagerbackport.VillagerBackport;
import com.exiledradio.villagerbackport.compat.VillagerAccess;
import com.exiledradio.villagerbackport.data.VillagerTradeData;

import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.village.MerchantRecipeList;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.common.registry.VillagerRegistry;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Villagers take their job from a workstation, and lose it if they never used one.
 *
 * <h2>1.14's rules</h2>
 * Two brain tasks, and they are short enough to state exactly:
 *
 * <ul>
 *   <li>{@code AssignProfessionTask} - a villager with no profession that has claimed a job site
 *       takes the profession that site belongs to.</li>
 *   <li>{@code ChangeJobTask} - a villager with no job site loses its profession, but only if
 *       {@code xp == 0 && level <= 1}. Any trading at all and the job is permanent.</li>
 * </ul>
 *
 * <p>That second condition is what makes the system fair: breaking a lectern next to a librarian you
 * have traded with does not undo your work, but a villager you never used goes back on the market.
 *
 * <h2>Standing in for the NONE profession</h2>
 * 1.14 has a profession meaning "unemployed". 1.12.2 has no such thing - every villager has one of
 * six, and the closest in spirit is the nitwit, which has no trades at all. So unemployment here is
 * the nitwit profession with a career of zero.
 *
 * <p>Career zero is doing real work in that pair. It is already 1.12.2's own value for "has not
 * picked a career", the texture layer already reads it as "no profession outfit" and so renders the
 * plain biome clothing 1.14 gives an unemployed villager, and the workstation registry already reads
 * it as "no workstation to seek". The unemployed state needed no new concept.
 */
public final class Employment {

    /** Marks a villager this mod has already made a decision about. */
    private static final String ROOT = "villagerbackport";
    private static final String SEEN = "EmploymentChecked";

    /**
     * Marks a real nitwit, as opposed to an unemployed villager wearing the nitwit profession.
     *
     * <p>Both are the nitwit profession in 1.12.2, and the career field cannot tell them apart:
     * opening any nitwit's screen has vanilla give it the nitwit career. So the difference is kept
     * here, decided once when the villager is first seen.
     */
    private static final String REAL_NITWIT = "Nitwit";

    private static final int TAG_COMPOUND = 10;

    /** 1.12.2's nitwit, which has no trades and stands in for unemployment. */
    private static final ResourceLocation NITWIT = new ResourceLocation("minecraft:nitwit");

    private Employment() {
    }

    private static NBTTagCompound root(EntityVillager villager) {
        return VillagerTradeData.root(villager);
    }

    /**
     * @return true if this villager has no job and could take one.
     *
     * <p>A real nitwit is not unemployed. In 1.14 it is a profession of its own that never takes a
     * job, never loses one, and never counts towards a golem - see {@link #isNitwit}.
     */
    public static boolean isUnemployed(EntityVillager villager) {
        if (isNitwit(villager)) {
            return false;
        }

        return VillagerAccess.getCareerId(villager) <= 0 || hasNitwitProfession(villager);
    }

    /**
     * @return true if this villager is a nitwit: 1.14's villager that never works.
     *
     * <p>1.14's {@code AssignProfessionTask} only hands a job to a villager whose profession is
     * {@code NONE}, so a nitwit never takes one; its golem test excludes {@code NITWIT} by name; and
     * it has no trades, so it never gains the experience a rank needs.
     */
    public static boolean isNitwit(EntityVillager villager) {
        return root(villager).getBoolean(REAL_NITWIT);
    }

    private static boolean hasNitwitProfession(EntityVillager villager) {
        VillagerRegistry.VillagerProfession profession = villager.getProfessionForge();
        return profession != null && NITWIT.equals(profession.getRegistryName());
    }

    /**
     * Puts a villager the mod has not seen before into the unemployed state.
     *
     * <p>Only ever done once per villager, and only to one that has never traded. A villager already
     * carrying trades or experience is one the player has invested in, and taking its job away
     * because the mod was installed afterwards would be indefensible.
     */
    public static void initialise(EntityVillager villager) {
        if (!ModConfig.jobs.professionsFromWorkstations) {
            return;
        }

        NBTTagCompound tag = root(villager);
        if (tag.getBoolean(SEEN)) {
            return;
        }
        tag.setBoolean(SEEN, true);

        // A grown villager that arrived as a nitwit stays one. Villages are built with a few nitwits
        // among the unemployed, and 1.12.2's village generation rolls nitwit as one of its
        // professions, so keeping the roll is where a village's nitwits come from - and from curing
        // a zombie villager that was one.
        //
        // Never for a child: babies are always born unemployed, and 1.12.2 rolls its newborns a
        // random profession that has to be undone, not honoured. Nor for a villager a player made
        // from an egg or a bare /summon, which is always unemployed - see SpawnSource.
        if (!villager.isChild() && hasNitwitProfession(villager) && !SpawnSource.isPlayerMade(villager)) {
            becomeNitwit(villager);
            return;
        }

        // Anything already earned means this villager predates the mod and keeps what it has.
        if (VillagerTradeData.getXp(villager) > 0 || VillagerAccess.getBuyingList(villager) != null) {
            return;
        }

        unassign(villager);
    }

    /**
     * Gives a villager the job belonging to a workstation.
     *
     * @param career the career name the workstation maps to
     * @return true if the job was taken
     */
    public static boolean assign(EntityVillager villager, String career) {
        VillagerRegistry.VillagerProfession profession = professionFor(career);
        if (profession == null) {
            return false;
        }

        int careerId = careerIdFor(profession, career);
        if (careerId < 0) {
            return false;
        }

        villager.setProfession(profession);

        // Career ids are stored one-based, with zero meaning none.
        VillagerAccess.setCareerId(villager, careerId + 1);

        // Level one, not zero, and the first tier of trades is built here rather than left to
        // vanilla. This is not tidiness - it is the only way to keep the career that was just
        // assigned. populateBuyingList only advances an existing career:
        //
        //   if (careerId != 0 && careerLevel != 0) { ++careerLevel; }
        //   else { careerId = getRandomCareer(rand) + 1; careerLevel = 1; }
        //
        // Reaching it with a career level of zero takes the second branch, which throws away the
        // career and rolls a new one from the same profession. A villager sent to a cauldron came
        // back a butcher about half the time, because butcher and leatherworker share a profession.
        // There is no value of careerLevel that both survives that test and produces tier one, so
        // tier one is built directly and vanilla is only ever asked for tier two onward.
        VillagerAccess.setCareerLevel(villager, 1);
        buildFirstTier(villager, profession, careerId);
        VillagerTradeData.setAppliedLevel(villager, 1);

        com.exiledradio.villagerbackport.trade.LevelSyncHandler.broadcastJobChange(villager);

        VillagerBackport.LOGGER.debug("Villager took the '{}' job.", career);
        return true;
    }

    /**
     * Builds the trades a new hire starts with, without going through vanilla.
     *
     * <p>Tier one of the trade table new villagers use - 1.14's, or the career's own registered
     * trades, which is what {@code populateBuyingList} would add minus the career re-roll that makes
     * it unusable here.
     */
    private static void buildFirstTier(EntityVillager villager,
                                       VillagerRegistry.VillagerProfession profession, int careerId) {
        // From whichever table new trades come from - see TradeTables.
        MerchantRecipeList list = com.exiledradio.villagerbackport.trade.TradeTables.buildFirstTier(villager);

        VillagerAccess.setBuyingList(villager, list);

        // The moment a reroll produces its answer: a fresh set of trades, rolled just now.
        com.exiledradio.villagerbackport.trade.BookAnnouncer.announce(villager, list, 0);
    }

    /**
     * Keeps a child villager out of work.
     *
     * <p>1.12.2 gives every villager a profession the moment it spawns, babies included, so a
     * village nursery is full of toddlers dressed as blacksmiths. 1.14 does not: a baby has no
     * profession at all, and picks one up when it grows up and finds a workstation - which is
     * already how this mod employs adults, so the outfit is the only thing that needs correcting.
     *
     * <p>Checked rather than done once, because a baby can be handed a profession after it is born -
     * by breeding, by another mod, or by vanilla's own spawn path - and the job it is not old enough
     * to do should not survive that.
     */
    /**
     * Makes this villager a real nitwit: the nitwit outfit, no job, no trades, and no badge.
     *
     * <p>The career is set to the nitwit's own so the client is sent something to dress it in. With
     * a career of zero it would be drawn exactly like an unemployed villager, which is the one thing
     * 1.14 makes a point of not doing.
     */
    private static void becomeNitwit(EntityVillager villager) {
        root(villager).setBoolean(REAL_NITWIT, true);

        if (VillagerAccess.getCareerId(villager) <= 0) {
            VillagerAccess.setCareerId(villager, 1);
        }
        VillagerAccess.setCareerLevel(villager, 1);
        VillagerTradeData.setAppliedLevel(villager, 1);

        com.exiledradio.villagerbackport.trade.LevelSyncHandler.broadcastJobChange(villager);
    }

    /**
     * Keeps a child out of work, and settles it as unemployed for good.
     *
     * <p>1.12.2 rolls every newborn a random profession, one time in six a nitwit. Marking the child
     * as seen here matters as much as taking the job away: {@link #initialise} only ever runs on grown
     * villagers, so a child left unmarked was first looked at the day it grew up - when its nitwit roll
     * made it look like a villager that had arrived as one, and it was kept a nitwit. Bred villagers
     * are never nitwits in the game.
     *
     * <p>A child that already is a real nitwit - a cured baby zombie villager that was one - stays one.
     */
    public static void keepChildUnemployed(EntityVillager villager) {
        if (!ModConfig.jobs.professionsFromWorkstations) {
            return;
        }

        root(villager).setBoolean(SEEN, true);

        if (isNitwit(villager)) {
            return;
        }

        if (isUnemployed(villager) && VillagerAccess.getCareerId(villager) <= 0) {
            return;
        }

        unassign(villager);
    }

    /**
     * Takes a villager's job away, returning it to the unemployed state.
     *
     * <p>Its trades go with the job. They belong to a career it no longer has, and leaving them
     * would produce a nitwit still selling books.
     */
    public static void unassign(EntityVillager villager) {
        VillagerRegistry.VillagerProfession nitwit = ForgeRegistries.VILLAGER_PROFESSIONS.getValue(NITWIT);
        if (nitwit == null) {
            return;
        }

        villager.setProfession(nitwit);
        root(villager).removeTag(REAL_NITWIT);
        VillagerAccess.setCareerId(villager, 0);
        VillagerAccess.setCareerLevel(villager, 0);
        VillagerAccess.clearBuyingList(villager);
        VillagerTradeData.setAppliedLevel(villager, 1);

        com.exiledradio.villagerbackport.trade.LevelSyncHandler.broadcastJobChange(villager);
    }

    /**
     * @return true if this villager should lose its job now that it has no workstation.
     *
     * <p>1.14's exact condition: no experience and still at the first level. A villager that has been
     * traded with even once keeps its profession permanently and simply cannot restock until it
     * finds another workstation.
     */
    public static boolean shouldLoseJob(EntityVillager villager) {
        // A nitwit has no job to lose.
        if (!ModConfig.jobs.professionsFromWorkstations || isUnemployed(villager) || isNitwit(villager)) {
            return false;
        }

        if (VillagerTradeData.getXp(villager) > 0) {
            return false;
        }

        return VillagerTradeData.getAppliedLevel(villager) <= 1;
    }

    @Nullable
    private static VillagerRegistry.VillagerProfession professionFor(String career) {
        for (VillagerRegistry.VillagerProfession profession : ForgeRegistries.VILLAGER_PROFESSIONS) {
            if (careerIdFor(profession, career) >= 0) {
                return profession;
            }
        }
        return null;
    }

    /** @return the career's index within its profession, or -1 if this profession has no such career. */
    private static int careerIdFor(VillagerRegistry.VillagerProfession profession, String career) {
        List<VillagerRegistry.VillagerCareer> careers = VillagerAccess.getCareers(profession);
        for (int i = 0; i < careers.size(); i++) {
            if (careers.get(i).getName().equals(career)) {
                return i;
            }
        }
        return -1;
    }
}
