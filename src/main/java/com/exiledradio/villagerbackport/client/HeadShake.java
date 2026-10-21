package com.exiledradio.villagerbackport.client;

import com.exiledradio.villagerbackport.network.PacketVillagerShake;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * 1.14's "no": the villager bows its head and shakes it side to side for two seconds.
 *
 * <p>The numbers are 1.14's {@code VillagerModel}: while shaking, the head's pitch is held at 0.4
 * radians and it rolls by {@code 0.3 sin(0.45 * ageInTicks)}. Kept per villager as the tick the shake
 * ends, by the villager's own clock, so it lasts 40 ticks whatever the frame rate.
 */
@SideOnly(Side.CLIENT)
public final class HeadShake {

    /** 1.14's {@code setShakeHeadTicks(40)}. */
    private static final int TICKS = 40;

    /** 1.14's held head pitch while shaking, in degrees as the model is handed it. */
    private static final float PITCH = (float) Math.toDegrees(0.4D);

    private static final Map<EntityVillager, Integer> ENDS = new WeakHashMap<EntityVillager, Integer>();

    private HeadShake() {
    }

    public static void accept(PacketVillagerShake message) {
        if (Minecraft.getMinecraft().world == null) {
            return;
        }
        Entity entity = Minecraft.getMinecraft().world.getEntityByID(message.getEntityId());
        if (entity instanceof EntityVillager) {
            ENDS.put((EntityVillager) entity, Integer.valueOf(entity.ticksExisted + TICKS));
        }
    }

    public static boolean isShaking(EntityVillager villager) {
        Integer end = ENDS.get(villager);
        if (end == null) {
            return false;
        }
        if (villager.ticksExisted >= end.intValue()) {
            ENDS.remove(villager);
            return false;
        }
        return true;
    }

    /** @return the head pitch to draw with: 1.14's bowed head while shaking, the real one otherwise */
    public static float pitch(EntityVillager villager, float headPitch) {
        return isShaking(villager) ? PITCH : headPitch;
    }

    /** @return the head's roll, in radians */
    public static float roll(EntityVillager villager, float ageInTicks) {
        return isShaking(villager) ? 0.3F * MathHelper.sin(0.45F * ageInTicks) : 0.0F;
    }
}
