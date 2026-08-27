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

    private static final int TAG_COMPOUND = 10;

    /** 1.12.2's nitwit, which has no trades and stands in for unemployment. */
    private static final ResourceLocation NITWIT = new ResourceLocation("minecraft:nitwit");

    private Employment() {
    }

    private static NBTTagCompound root(EntityVillager villager) {
        return VillagerTradeData.root(villager);
    }

    /** @return true if this villager has no job. */
    public static boolean isUnemployed(EntityVillager villager) {
        return VillagerAccess.getCareerId(villager) <= 0 || isNitwit(villager);
    }

    private static boolean isNitwit(EntityVillager villager) {
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
     * <p>The career's own tier-one entries, asked to add themselves to a fresh list - exactly what
     * {@code populateBuyingList} would do, minus the career re-roll that makes it unusable here.
     */
    private static void buildFirstTier(EntityVillager villager,
                                       VillagerRegistry.VillagerProfession profession, int careerId) {
        MerchantRecipeList list = new MerchantRecipeList();

        try {
            List<EntityVillager.ITradeList> trades =
                    VillagerAccess.getCareers(profession).get(careerId).getTrades(0);

            if (trades != null) {
                for (EntityVillager.ITradeList trade : trades) {
                    trade.addMerchantRecipe(villager, list, villager.getRNG());
                }
            }
        } catch (RuntimeException e) {
            VillagerBackport.LOGGER.warn("Could not build starting trades for a new villager job.", e);
        }

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
    public static void keepChildUnemployed(EntityVillager villager) {
        if (!ModConfig.jobs.professionsFromWorkstations || isUnemployed(villager)) {
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
        if (!ModConfig.jobs.professionsFromWorkstations || isUnemployed(villager)) {
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
