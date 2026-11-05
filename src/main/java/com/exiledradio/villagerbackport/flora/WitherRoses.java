package com.exiledradio.villagerbackport.flora;

import com.exiledradio.villagerbackport.ModConfig;
import com.exiledradio.villagerbackport.VillagerBackport;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.boss.EntityWither;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * The Wither leaving a wither rose where it kills something.
 *
 * <p>1.14's {@code LivingEntity.createWitherRose}, run from {@code onDeath}: when the killer is the
 * Wither - by its skulls or its touch - a rose is planted in the block the victim died in if that
 * block is empty and a wither rose could stand there, and mob griefing is allowed for the Wither.
 * Otherwise it drops as an item. Every living thing counts, players and villagers included, and the
 * rose comes whether or not the victim drops loot.
 *
 * <p>Lowest priority and ignoring cancelled deaths, so a mod that saves the victim (a totem, a
 * revive) wins and no rose is left for a death that did not happen.
 */
@Mod.EventBusSubscriber(modid = VillagerBackport.MOD_ID)
public final class WitherRoses {

    private WitherRoses() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDeath(LivingDeathEvent event) {
        EntityLivingBase victim = event.getEntityLiving();
        World world = victim.world;
        if (world.isRemote || !FloraContent.enabled() || !ModConfig.flora.witherKillsPlantRoses
                || FloraContent.witherRose == null) {
            return;
        }

        Entity killer = event.getSource().getTrueSource();
        if (!(killer instanceof EntityWither)) {
            killer = victim.getAttackingEntity();
        }
        if (!(killer instanceof EntityWither)) {
            return;
        }

        BlockPos pos = new BlockPos(victim.posX, victim.posY, victim.posZ);
        if (ForgeEventFactory.getMobGriefingEvent(world, killer)
                && world.isAirBlock(pos) && FloraContent.witherRose.canPlaceBlockAt(world, pos)) {
            world.setBlockState(pos, FloraContent.witherRose.getDefaultState(), 3);
            return;
        }

        EntityItem item = new EntityItem(world, victim.posX, victim.posY, victim.posZ,
                new ItemStack(FloraContent.witherRose));
        world.spawnEntity(item);
    }
}
