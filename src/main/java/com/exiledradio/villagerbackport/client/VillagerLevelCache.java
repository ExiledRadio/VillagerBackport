package com.exiledradio.villagerbackport.client;

import com.exiledradio.villagerbackport.trade.VillagerLevel;

import net.minecraft.entity.passive.EntityVillager;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.HashMap;
import java.util.Map;

/**
 * What the client knows about nearby villagers' levels, so their badges can be drawn.
 *
 * <h2>Why a cache rather than entity data</h2>
 * The level lives in the villager's Forge data on the server, which is never sent to clients. The
 * usual way to publish something like this is a {@code DataParameter}, but those have to be declared
 * in the entity class's constructor and {@link EntityVillager} is not ours to change - registering
 * one from outside would desync every client that does not have the mod. So the server sends the
 * value directly and it is kept here, keyed by entity id.
 *
 * <p>Populated when a player starts tracking a villager and refreshed when one levels up. Villagers
 * level rarely, so entries are written far less often than they are read.
 *
 * <h2>Lifetime</h2>
 * Instances exist only to receive the world-unload event; all state is static.
 * Cleared when the client leaves a world. Entity ids are only unique within a session, so carrying
 * them across would eventually paint a badge on whatever entity inherited the id. Individual entries
 * are not removed when a villager goes out of range: the id stays valid for the session, and a stale
 * entry costs two ints rather than a wrong badge - the next update corrects it if the level changed
 * while out of sight.
 */
@SideOnly(Side.CLIENT)
public final class VillagerLevelCache {

    private static final Map<Integer, Integer> LEVELS = new HashMap<Integer, Integer>();

    /**
     * Careers, which decide the profession outfit.
     *
     * <p>The client cannot work this out for itself: {@code EntityVillager} publishes its profession
     * through a data parameter but not its career, and one profession covers up to four 1.14 outfits.
     */
    private static final Map<Integer, Integer> CAREERS = new HashMap<Integer, Integer>();

    /** Called on the client thread when a level arrives from the server. */
    public static void accept(int entityId, int level, int careerId) {
        LEVELS.put(entityId, level);
        CAREERS.put(entityId, careerId);
    }

    /**
     * @return the villager's career, or -1 if the server has not said.
     *
     * <p>Zero is a real answer meaning unemployed, so "not known yet" has to be distinguishable
     * from it - a villager awaiting its first update should not be drawn as having lost its job.
     */
    public static int career(EntityVillager villager) {
        Integer career = CAREERS.get(villager.getEntityId());
        return career == null ? -1 : career;
    }

    /**
     * @return the villager's level, defaulting to the lowest rank when nothing is known yet.
     *
     * <p>Defaulting rather than returning "unknown" keeps the renderer simple: a villager whose
     * level has not arrived yet wears the novice badge for a moment instead of flickering between
     * having one and not.
     */
    public static int get(EntityVillager villager) {
        Integer level = LEVELS.get(villager.getEntityId());
        return level == null ? VillagerLevel.MIN_LEVEL : level;
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        if (event.getWorld().isRemote) {
            LEVELS.clear();
            CAREERS.clear();
        }
    }
}
