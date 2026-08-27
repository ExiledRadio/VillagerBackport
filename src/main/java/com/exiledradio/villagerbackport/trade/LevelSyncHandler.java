package com.exiledradio.villagerbackport.trade;

import com.exiledradio.villagerbackport.ModConfig;
import com.exiledradio.villagerbackport.compat.VillagerAccess;
import com.exiledradio.villagerbackport.data.VillagerTradeData;
import com.exiledradio.villagerbackport.job.Employment;
import com.exiledradio.villagerbackport.network.NetworkHandler;

import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Publishes villager levels to the clients that can see them, for the rank badge.
 *
 * <p>The level is server-side state with no route to the client of its own - it lives in the
 * villager's Forge data, which is never transmitted. The trade screen gets it in its own packet, but
 * the badge has to be drawn on every villager in view, so it needs the value before anyone opens a
 * screen.
 *
 * <p>Two moments cover that: when a player starts tracking a villager, and when one levels up.
 * Tracking begins as a player approaches and is exactly when the client first needs to draw it;
 * level-ups are rare. Nothing here is periodic.
 */
public final class LevelSyncHandler {

    /**
     * Range in blocks for a level-up broadcast. Comfortably beyond entity tracking distance, so
     * everyone who can see the villager is told, without reaching players who cannot.
     */
    private static final double LEVEL_UP_BROADCAST_RANGE = 96.0D;

    /**
     * Sends a villager's level to a player who has just come within range of it.
     */
    @SubscribeEvent
    public void onStartTracking(PlayerEvent.StartTracking event) {
        if (!(event.getTarget() instanceof EntityVillager)) {
            return;
        }

        if (!(event.getEntityPlayer() instanceof EntityPlayerMP) || !ModConfig.display.showLevelBadge) {
            return;
        }

        EntityVillager villager = (EntityVillager) event.getTarget();
        NetworkHandler.sendVillagerLevel((EntityPlayerMP) event.getEntityPlayer(), villager,
                levelOf(villager), careerOf(villager));
    }

    /**
     * Tells everyone nearby that a villager's rank changed.
     *
     * <p>Called from {@link LevelGate} at the moment the level lands, so the badge changes in step
     * with the particles rather than the next time a player walks away and back.
     */
    public static void broadcastLevelUp(EntityVillager villager, int level) {
        broadcast(villager, level);
    }

    /**
     * Tells everyone nearby that a villager's job changed.
     *
     * <p>The outfit is chosen from the career, so taking or losing a job has to reach the client or
     * a villager would keep wearing the clothes of a job it no longer has until someone walked away
     * and back.
     */
    public static void broadcastJobChange(EntityVillager villager) {
        broadcast(villager, levelOf(villager));
    }

    private static void broadcast(EntityVillager villager, int level) {
        if (!ModConfig.display.showLevelBadge) {
            return;
        }

        NetworkHandler.sendVillagerLevelNearby(villager, level,
                careerOf(villager), LEVEL_UP_BROADCAST_RANGE);
    }

    /**
     * @return the career to publish, or 0 for an unemployed villager.
     *
     * <p>Not simply the career field. Opening an unemployed villager's screen makes vanilla assign
     * it a nitwit career, so the raw field stops reading as zero while the villager is still very
     * much out of work. Sending zero keeps the client's view honest: no outfit, no rank badge.
     */
    private static int careerOf(EntityVillager villager) {
        return Employment.isUnemployed(villager) ? 0 : VillagerAccess.getCareerId(villager);
    }

    /**
     * @return the villager's level, falling back to the experience-derived value for one the tick
     * has not reached yet and so has no applied level recorded.
     */
    private static int levelOf(EntityVillager villager) {
        int applied = VillagerTradeData.getAppliedLevel(villager);
        return applied > 0 ? applied : VillagerLevel.levelFor(VillagerTradeData.getXp(villager));
    }
}
