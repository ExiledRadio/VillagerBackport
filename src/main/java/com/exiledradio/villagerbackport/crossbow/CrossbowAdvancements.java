package com.exiledradio.villagerbackport.crossbow;

import com.exiledradio.villagerbackport.VillagerBackport;

import net.minecraft.advancements.Advancement;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 1.14's four crossbow advancements. They are this mod's own, so 1.14's {@code shot_crossbow} and
 * {@code killed_by_crossbow} triggers are kept here instead: each advancement has one impossible
 * criterion, granted from code.
 *
 * <ul>
 *   <li><b>Ol' Betsy</b> - fire a crossbow.</li>
 *   <li><b>Who's the Pillager Now?</b> - kill a pillager with one.</li>
 *   <li><b>Two Birds, One Arrow</b> - kill two phantoms with one shot. 1.12.2 has no phantom, so
 *       this needs a mod that adds one (anything registered as {@code phantom}).</li>
 *   <li><b>Arbalistic</b> - kill five different kinds of mob with one shot.</li>
 * </ul>
 *
 * <p>"One shot" is everything one pull of the trigger let go - all three arrows of a Multishot,
 * however many each pierces, and a rocket's blast. Each projectile carries the number of the shot it
 * belongs to.
 */
public final class CrossbowAdvancements {

    private static final String SHOT = VillagerBackport.MOD_ID + ":CrossbowShot";
    private static final AtomicInteger SHOTS = new AtomicInteger();

    /** Each player's latest shot, and what it has killed. */
    private static final Map<UUID, Kills> KILLS = new HashMap<UUID, Kills>();

    private static final class Kills {
        final int shot;
        final Set<ResourceLocation> kinds = new HashSet<ResourceLocation>();
        int phantoms;

        Kills(int shot) {
            this.shot = shot;
        }
    }

    static int newShot() {
        return SHOTS.incrementAndGet();
    }

    static void tag(Entity projectile, int shot) {
        projectile.getEntityData().setInteger(SHOT, shot);
    }

    static void shot(EntityPlayerMP player) {
        grant(player, "ol_betsy");
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onDeath(LivingDeathEvent event) {
        if (event.isCanceled()) {
            return;
        }
        Entity projectile = event.getSource().getImmediateSource();
        Entity killer = event.getSource().getTrueSource();
        if (projectile == null || !(killer instanceof EntityPlayerMP) || !projectile.getEntityData().hasKey(SHOT)) {
            return;
        }
        EntityPlayerMP player = (EntityPlayerMP) killer;
        int shot = projectile.getEntityData().getInteger(SHOT);
        Kills kills = KILLS.get(player.getUniqueID());
        if (kills == null || kills.shot != shot) {
            kills = new Kills(shot);
            KILLS.put(player.getUniqueID(), kills);
        }

        ResourceLocation kind = EntityList.getKey(event.getEntityLiving());
        if (kind == null) {
            return;
        }
        kills.kinds.add(kind);
        if ("pillager".equals(kind.getPath())) {
            grant(player, "whos_the_pillager_now");
        }
        if ("phantom".equals(kind.getPath()) && ++kills.phantoms >= 2) {
            grant(player, "two_birds_one_arrow");
        }
        if (kills.kinds.size() >= 5) {
            grant(player, "arbalistic");
        }
    }

    private static void grant(EntityPlayerMP player, String name) {
        Advancement advancement = player.getServer().getAdvancementManager()
                .getAdvancement(new ResourceLocation(VillagerBackport.MOD_ID, "adventure/" + name));
        if (advancement != null) {
            player.getAdvancements().grantCriterion(advancement, name);
        }
    }
}
