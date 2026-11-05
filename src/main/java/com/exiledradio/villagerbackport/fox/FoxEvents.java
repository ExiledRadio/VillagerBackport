package com.exiledradio.villagerbackport.fox;

import com.exiledradio.villagerbackport.VillagerBackport;

import net.minecraft.entity.Entity;
import net.minecraft.init.SoundEvents;
import net.minecraftforge.event.entity.PlaySoundAtEntityEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import javax.annotation.Nullable;

/**
 * The few things foxes and berries need from game-wide events.
 */
@Mod.EventBusSubscriber(modid = VillagerBackport.MOD_ID)
public final class FoxEvents {

    /**
     * The entity a bush is hurting right now, for the length of that one call.
     *
     * <p>1.14 gives a player pricked by a bush its own scratchy hurt sound. 1.12.2 picks a player's hurt
     * sound without a hook, and plays it with no entity attached, so the only way to know a given
     * {@code entity.player.hurt} is the bush's is that it is played during the bush's damage call -
     * which runs on one thread, start to finish, before anything else can play a sound.
     */
    @Nullable
    private static Entity pricking;

    private static boolean spawnsChecked;

    private FoxEvents() {
    }

    /** Deals a bush's damage, with 1.14's hurt sound for players. */
    static void hurtByThorns(Entity entity) {
        pricking = entity;
        try {
            entity.attackEntityFrom(BlockSweetBerryBush.THORNS, 1.0F);
        } finally {
            pricking = null;
        }
    }

    @SubscribeEvent
    public static void onSound(PlaySoundAtEntityEvent event) {
        if (pricking != null && event.getSound() == SoundEvents.ENTITY_PLAYER_HURT) {
            event.setSound(FoxContent.sound("thorns"));
        }
    }

    @SubscribeEvent
    public static void onWorldLoad(WorldEvent.Load event) {
        if (!spawnsChecked && !event.getWorld().isRemote) {
            spawnsChecked = true;
            FoxContent.removeDuplicateSpawns();
        }
    }
}
