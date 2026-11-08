package com.exiledradio.villagerbackport.compat;

import com.exiledradio.villagerbackport.VillagerBackport;
import com.exiledradio.villagerbackport.job.Employment;
import com.exiledradio.villagerbackport.trade.TradeTables;

import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.village.MerchantRecipeList;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Keeps Village Names from giving this mod's villagers 1.12.2 jobs and trades.
 *
 * <h2>What it did</h2>
 * Every time a villager joins the world (spawning, and every chunk load after), Village Names checks
 * its trade list and, if the list is empty, runs vanilla's {@code populateBuyingList}. In 1.14 a villager
 * without a job has no trades, so this mod keeps those lists empty on purpose. Vanilla's method then
 * gave such a villager a random 1.12.2 career (when it had none) and that career's trades: a jobless
 * villager selling arrows, or a nitwit selling beef, and one that could look employed to everything
 * after it.
 *
 * <p>So for a villager this mod keeps out of work, its career, level and empty list are noted before any
 * other mod sees it join, and put back after every other mod has. Villagers with jobs are left alone:
 * their lists are not empty, so Village Names does not touch them, and their trades are this mod's.
 * Only active when Village Names is installed.
 */
public final class VillageNamesCompat {

    public static final String MOD_ID = "villagenames";

    /** A jobless villager as it was before the other mods saw it join: career id and career level. */
    private final Map<EntityVillager, int[]> before = new WeakHashMap<EntityVillager, int[]>();

    public static void register() {
        if (Loader.isModLoaded(MOD_ID)) {
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new VillageNamesCompat());
            VillagerBackport.LOGGER.info("Village Names found: jobless villagers keep 1.14's empty trade list.");
        }
    }

    /**
     * HIGH, not HIGHEST: this mod's own HIGHEST handlers run first, so a cured villager already has its
     * old trades back (Zombification) and is not mistaken for a jobless one. Village Names acts at NORMAL.
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public void beforeOthers(EntityJoinWorldEvent event) {
        if (event.getWorld().isRemote || !(event.getEntity() instanceof EntityVillager)) {
            return;
        }
        EntityVillager villager = (EntityVillager) event.getEntity();
        if (!jobless(villager)) {
            return;
        }
        MerchantRecipeList list = VillagerAccess.getBuyingList(villager);
        if (list == null || list.isEmpty()) {
            this.before.put(villager, new int[]{VillagerAccess.getCareerId(villager), VillagerAccess.getCareerLevel(villager)});
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public void afterOthers(EntityJoinWorldEvent event) {
        if (event.getWorld().isRemote || !(event.getEntity() instanceof EntityVillager)) {
            return;
        }
        EntityVillager villager = (EntityVillager) event.getEntity();
        int[] was = this.before.remove(villager);
        if (was == null) {
            return;
        }
        MerchantRecipeList list = VillagerAccess.getBuyingList(villager);
        boolean filled = list != null && !list.isEmpty();
        if (filled || VillagerAccess.getCareerId(villager) != was[0]) {
            VillagerAccess.setBuyingList(villager, new MerchantRecipeList());
            VillagerAccess.setCareerId(villager, was[0]);
            VillagerAccess.setCareerLevel(villager, was[1]);
        }
    }

    /** @return true for a villager this mod keeps without trades: out of work, a nitwit, or a child */
    private static boolean jobless(EntityVillager villager) {
        return villager.isChild() || Employment.isUnemployed(villager) || Employment.isNitwit(villager)
                || TradeTables.careerKey(villager) == null;
    }
}
