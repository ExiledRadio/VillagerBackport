package com.exiledradio.villagerbackport.client;

import com.exiledradio.villagerbackport.network.PacketVillagerSleep;

import net.minecraft.util.math.BlockPos;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Which villagers the client believes are asleep, and in which bed.
 *
 * <p>The bed is worth keeping alongside the flag rather than working out from where the villager is
 * standing: the renderer needs the bed's facing to lay the villager along it rather than across it,
 * and a villager's own rotation says nothing about which way the bed points.
 */
@SideOnly(Side.CLIENT)
public final class SleepCache {

    private static final Map<Integer, BlockPos> SLEEPING = new HashMap<Integer, BlockPos>();

    /** Called on the client thread when a sleep change arrives from the server. */
    public static void accept(PacketVillagerSleep message) {
        if (message.isSleeping()) {
            SLEEPING.put(Integer.valueOf(message.getEntityId()),
                    new BlockPos(message.getBedX(), message.getBedY(), message.getBedZ()));
        } else {
            SLEEPING.remove(Integer.valueOf(message.getEntityId()));
        }
    }

    /** @return the bed this villager is asleep in, or null if it is awake. */
    @Nullable
    public static BlockPos bedOf(int entityId) {
        return SLEEPING.get(Integer.valueOf(entityId));
    }

    /**
     * Forgets everything when the world goes.
     *
     * <p>Entity ids are reused between worlds, so a stale entry would put some unrelated villager to
     * bed on the far side of a world change.
     */
    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        if (event.getWorld().isRemote) {
            SLEEPING.clear();
        }
    }
}
