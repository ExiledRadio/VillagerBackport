package com.exiledradio.villagerbackport.compat;

import com.exiledradio.villagerbackport.ModConfig;
import com.exiledradio.villagerbackport.VillagerBackport;
import com.exiledradio.villagerbackport.data.VillagerTradeData;
import com.exiledradio.villagerbackport.trade.Gossip;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import javax.annotation.Nullable;
import java.lang.reflect.Method;

/**
 * Keeps a villager ordinary on its way through zombification and back.
 *
 * <h2>Why</h2>
 * Curing is something players do on purpose, over and over, for the discount. In a pack that turns
 * random mobs into something worse, every zombification is then a fresh roll: one in a few hundred
 * comes back an Infernal Mobs elite that explodes, a Scaling Health blight or a Champion, standing
 * in the middle of the player's base. And Dregora's EagleMixins rolls every librarian that appears
 * for a one-in-ten chance of becoming a Sussyberian or Mentalberian, which refuse to trade at all -
 * a cured librarian appears as a brand new entity, so it is rolled again on every cure.
 *
 * <p>None of those mods has an API for "leave this one alone", but each one marks a mob it has
 * already dealt with, and a mob that looks dealt with is skipped:
 * <ul>
 *   <li><b>Infernal Mobs</b> - RLTweaker patches its spawn roll to skip any mob carrying
 *       {@code InfernalMobsInitialized}. Without RLTweaker there is no such check, so an elite made
 *       anyway is taken apart again before anything has ticked; see {@link #undoInfernal}.</li>
 *   <li><b>Scaling Health</b> - processes a mob once, on its first update, and records that by
 *       setting {@code scalinghealth:difficulty}. Set beforehand, there is no bonus health and no
 *       blight roll.</li>
 *   <li><b>Champions</b> - rolls a rank only for a mob that has none. Given Champions' own empty
 *       rank first, it never rolls.</li>
 *   <li><b>EagleMixins</b> - rolls a librarian only once, and records it as {@code BerianCheck}.</li>
 * </ul>
 * All of these are written before the other mods see the mob: the callers run at the highest
 * priority of {@link EntityJoinWorldEvent}, and all four mods act at normal priority or later.
 */
public final class ConvertedVillagers {

    /** RLTweaker's flag for "Infernal Mobs has already rolled this mob". */
    private static final String INFERNAL_DONE = "InfernalMobsInitialized";
    /** Where Infernal Mobs keeps an elite's modifiers. */
    private static final String INFERNAL_MODIFIERS = "InfernalMobsMod";
    /** Scaling Health's record of the difficulty a mob was processed at; zero means not yet. */
    private static final String SCALING_HEALTH_DONE = "scalinghealth:difficulty";

    private static final String BERIAN_CHECKED = "BerianCheck";
    private static final String[] BERIANS = {"Sussyberian", "Mentalberian"};

    /** Set on a villager that came out of a cure, so it can be recognised on later loads. */
    private static final String CURED = "Cured";

    private static boolean reflected;
    @Nullable private static Method championshipOf;
    @Nullable private static Method championRank;
    @Nullable private static Method setChampionRank;
    @Nullable private static Method emptyRank;
    @Nullable private static Method infernalIsRare;
    @Nullable private static Method infernalRemove;

    /**
     * Marks a zombie villager made from a villager, or a villager cured out of one, as already
     * dealt with by every mod that would otherwise give it something special.
     *
     * <p>Call before those mods see it join the world.
     */
    public static void keepOrdinary(EntityLiving entity) {
        if (!ModConfig.compat.convertedStayOrdinary || entity.world.isRemote) {
            return;
        }

        NBTTagCompound data = entity.getEntityData();
        data.setBoolean(INFERNAL_DONE, true);
        if (data.getShort(SCALING_HEALTH_DONE) == 0) {
            data.setShort(SCALING_HEALTH_DONE, (short) 1);
        }
        noChampion(entity);

        if (entity instanceof EntityVillager) {
            VillagerTradeData.root((EntityVillager) entity).setBoolean(CURED, true);
            notBerian((EntityVillager) entity);
        }
    }

    /**
     * Makes sure EagleMixins never turns this villager into a Sussyberian or Mentalberian, and
     * turns it back if it already is one.
     *
     * <p>A berian is an ordinary villager with a tag and a name - it keeps its profession and its
     * trades underneath - so taking both away gives back exactly the villager it was.
     */
    public static void notBerian(EntityVillager villager) {
        NBTTagCompound data = villager.getEntityData();
        data.setBoolean(BERIAN_CHECKED, true);
        for (String berian : BERIANS) {
            data.removeTag(berian);
            if (berian.equals(villager.getCustomNameTag())) {
                villager.setCustomNameTag("");
            }
        }
    }

    /**
     * Turns a Sussyberian or Mentalberian that came out of a cure back into an ordinary villager.
     *
     * <p>Those were made by the cure, before this class kept them from being. One counts as cured if
     * a player ever cured it - the gossip a cure leaves never fades - or if it carries trading
     * experience, which a berian cannot earn. Berians that generated as berians are Dregora's own
     * and are left alone.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onVillagerJoin(EntityJoinWorldEvent event) {
        if (event.getWorld().isRemote || !ModConfig.compat.convertedStayOrdinary
                || !(event.getEntity() instanceof EntityVillager)) {
            return;
        }

        EntityVillager villager = (EntityVillager) event.getEntity();
        if (!isBerian(villager)) {
            return;
        }

        if (VillagerTradeData.root(villager).getBoolean(CURED) || Gossip.wasCured(villager)
                || VillagerTradeData.getXp(villager) > 0) {
            notBerian(villager);
            VillagerBackport.LOGGER.info("Turned a cured berian at {} back into an ordinary villager.",
                    villager.getPosition());
        }
    }

    /**
     * Takes back an Infernal Mobs elite made of a converted villager anyway.
     *
     * <p>Only needed without RLTweaker, whose patch is what makes Infernal Mobs honour
     * {@code InfernalMobsInitialized}. This runs after every other mod has seen the mob join and
     * before it has ticked, so the elite has not yet had its health raised or used a modifier;
     * dropping it from Infernal Mobs' list and its saved modifiers is all there is to undo.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void afterJoin(EntityJoinWorldEvent event) {
        if (event.getWorld().isRemote || !ModConfig.compat.convertedStayOrdinary
                || !(event.getEntity() instanceof EntityLivingBase)) {
            return;
        }

        EntityLivingBase entity = (EntityLivingBase) event.getEntity();
        if (!entity.getEntityData().getBoolean(INFERNAL_DONE) || !isConverted(entity)) {
            return;
        }

        reflect();
        if (infernalIsRare == null || infernalRemove == null) {
            return;
        }
        try {
            if ((Boolean) infernalIsRare.invoke(null, entity)) {
                infernalRemove.invoke(null, entity);
                entity.getEntityData().removeTag(INFERNAL_MODIFIERS);
            }
        } catch (Exception e) {
            VillagerBackport.LOGGER.debug("Could not take an Infernal Mobs elite back apart.", e);
        }
    }

    private static boolean isConverted(EntityLivingBase entity) {
        if (entity instanceof EntityVillager) {
            return VillagerTradeData.root((EntityVillager) entity).getBoolean(CURED);
        }
        return com.exiledradio.villagerbackport.trade.Zombification.isCarrying(entity);
    }

    private static boolean isBerian(EntityVillager villager) {
        NBTTagCompound data = villager.getEntityData();
        for (String berian : BERIANS) {
            if (data.getBoolean(berian)) {
                return true;
            }
        }
        return false;
    }

    /** Gives the mob Champions' empty rank, unless it already has a rank of its own. */
    private static void noChampion(EntityLiving entity) {
        reflect();
        if (championshipOf == null || championRank == null || setChampionRank == null || emptyRank == null) {
            return;
        }
        try {
            Object championship = championshipOf.invoke(null, entity);
            if (championship != null && championRank.invoke(championship) == null) {
                setChampionRank.invoke(championship, emptyRank.invoke(null));
            }
        } catch (Exception e) {
            VillagerBackport.LOGGER.debug("Could not keep a converted villager from becoming a Champion.", e);
        }
    }

    /** Looks up the other mods' methods once; any that are not installed stay null. */
    private static synchronized void reflect() {
        if (reflected) {
            return;
        }
        reflected = true;

        try {
            Class<?> capability = Class.forName("c4.champions.common.capability.CapabilityChampionship");
            Class<?> championship = Class.forName("c4.champions.common.capability.IChampionship");
            Class<?> rank = Class.forName("c4.champions.common.rank.Rank");
            Class<?> ranks = Class.forName("c4.champions.common.rank.RankManager");
            championshipOf = capability.getMethod("getChampionship", EntityLiving.class);
            championRank = championship.getMethod("getRank");
            setChampionRank = championship.getMethod("setRank", rank);
            emptyRank = ranks.getMethod("getEmptyRank");
        } catch (ClassNotFoundException e) {
            // Champions is not installed.
        } catch (Exception e) {
            VillagerBackport.LOGGER.warn("Champions is installed but could not be read; converted villagers may "
                    + "still become Champions.", e);
            championshipOf = null;
        }

        try {
            Class<?> core = Class.forName("atomicstryker.infernalmobs.common.InfernalMobsCore");
            infernalIsRare = core.getMethod("getIsRareEntity", EntityLivingBase.class);
            infernalRemove = core.getMethod("removeEntFromElites", EntityLivingBase.class);
        } catch (ClassNotFoundException e) {
            // Infernal Mobs is not installed.
        } catch (Exception e) {
            VillagerBackport.LOGGER.warn("Infernal Mobs is installed but could not be read; without RLTweaker, "
                    + "converted villagers may still become elites.", e);
            infernalIsRare = null;
        }
    }

}
