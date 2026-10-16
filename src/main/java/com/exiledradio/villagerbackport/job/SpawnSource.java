package com.exiledradio.villagerbackport.job;

import com.exiledradio.villagerbackport.data.VillagerTradeData;

import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.init.Items;
import net.minecraft.item.ItemMonsterPlacer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * Notices villagers a player made with a spawn egg or a bare {@code /summon}.
 *
 * <h2>Why it matters</h2>
 * Those villagers are always unemployed in the current game until they claim a job site. 1.12.2
 * instead rolls them a random profession as they appear, one time in six a nitwit - and a nitwit
 * never takes a job. So without this, a player placing villagers from eggs got a nitwit every so
 * often, which the current game never does. Nitwits come from villages, and from curing zombie
 * villagers that were nitwits.
 *
 * <p>Neither path has an event of its own, so the moment is caught from the outside: the egg use or
 * the command is noted, and a villager joining that same world on that same tick is the one it made.
 * A {@code /summon} that gives the villager data of its own skips the random roll, so whatever
 * profession it names is the player's choice and is left alone.
 */
public final class SpawnSource {

    private static final String PLAYER_MADE = "PlayerMade";

    private static final ResourceLocation VILLAGER = new ResourceLocation("minecraft", "villager");

    /** Per dimension, the tick a player last made a villager on. */
    private static final Map<Integer, Long> MADE_AT = new HashMap<Integer, Long>();

    /** @return true if a player made this villager with an egg or a bare /summon */
    public static boolean isPlayerMade(EntityVillager villager) {
        return VillagerTradeData.root(villager).getBoolean(PLAYER_MADE);
    }

    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        noteEgg(event.getWorld(), event.getItemStack());
    }

    @SubscribeEvent
    public void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        noteEgg(event.getWorld(), event.getItemStack());
    }

    private static void noteEgg(World world, ItemStack stack) {
        if (!world.isRemote && stack.getItem() == Items.SPAWN_EGG
                && VILLAGER.equals(ItemMonsterPlacer.getNamedIdFrom(stack))) {
            note(world);
        }
    }

    @SubscribeEvent
    public void onCommand(CommandEvent event) {
        String[] args = event.getParameters();
        World world = event.getSender().getEntityWorld();

        // summon <entity> [x y z] [data]: five or more arguments carry data of their own.
        if (world == null || world.isRemote || !"summon".equals(event.getCommand().getName())
                || args.length < 1 || args.length > 4) {
            return;
        }

        if (VILLAGER.equals(new ResourceLocation(args[0]))) {
            note(world);
        }
    }

    private static void note(World world) {
        MADE_AT.put(Integer.valueOf(world.provider.getDimension()), Long.valueOf(world.getTotalWorldTime()));
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onJoin(EntityJoinWorldEvent event) {
        if (event.getWorld().isRemote || !(event.getEntity() instanceof EntityVillager)) {
            return;
        }

        Long tick = MADE_AT.get(Integer.valueOf(event.getWorld().provider.getDimension()));
        if (tick != null && tick.longValue() == event.getWorld().getTotalWorldTime()) {
            VillagerTradeData.root((EntityVillager) event.getEntity()).setBoolean(PLAYER_MADE, true);
        }
    }
}
