package com.exiledradio.villagerbackport.home;

import net.minecraft.entity.monster.EntityWitch;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.EntityStruckByLightningEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * A witch made from a villager struck by lightning never despawns, as in the game.
 *
 * <p>1.12.2 spawns that witch as an ordinary monster, so it wanders off and despawns like any other.
 * There is no event for the conversion itself, only for the strike, so the strike is noted and the
 * witch that appears on that same tick, where the villager was standing, is the one it became.
 */
public final class Persistence {

    /** Per dimension, where and when a villager was last struck. */
    private static final Map<Integer, Strike> STRUCK = new HashMap<Integer, Strike>();

    @SubscribeEvent
    public void onLightning(EntityStruckByLightningEvent event) {
        if (event.getEntity().world.isRemote || !(event.getEntity() instanceof EntityVillager)) {
            return;
        }

        EntityVillager villager = (EntityVillager) event.getEntity();
        STRUCK.put(Integer.valueOf(villager.world.provider.getDimension()),
                new Strike(villager.world.getTotalWorldTime(), villager.getPositionVector()));
    }

    @SubscribeEvent
    public void onJoin(EntityJoinWorldEvent event) {
        if (event.getWorld().isRemote || !(event.getEntity() instanceof EntityWitch)) {
            return;
        }

        Strike strike = STRUCK.get(Integer.valueOf(event.getWorld().provider.getDimension()));
        EntityWitch witch = (EntityWitch) event.getEntity();

        if (strike != null && strike.tick == event.getWorld().getTotalWorldTime()
                && strike.where.squareDistanceTo(witch.getPositionVector()) < 1.0D) {
            witch.enablePersistence();
        }
    }

    private static final class Strike {
        final long tick;
        final Vec3d where;

        Strike(long tick, Vec3d where) {
            this.tick = tick;
            this.where = where;
        }
    }
}
