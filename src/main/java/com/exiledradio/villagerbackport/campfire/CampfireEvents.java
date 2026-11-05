package com.exiledradio.villagerbackport.campfire;

import com.exiledradio.villagerbackport.VillagerBackport;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.entity.projectile.EntityFireball;
import net.minecraft.entity.projectile.EntityPotion;
import net.minecraft.init.PotionTypes;
import net.minecraft.potion.PotionUtils;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * What reaches a campfire from outside it: projectiles.
 *
 * <h2>Projectiles</h2>
 * <ul>
 * <li>A <b>burning arrow</b> lights an unlit campfire it hits (1.14's {@code onProjectileHit}).</li>
 * <li>A <b>small fireball</b> - a blaze's, or a fire charge from a dispenser - does too, as in current
 * Java, but only a player's or with {@code mobGriefing} on.</li>
 * <li>A <b>splash or lingering water bottle</b> puts out a lit campfire where it lands: the block it
 * hit, the space in front of the face it hit, and the four spaces around that - the same spots 1.12.2's
 * own water bottle already checks for fire.</li>
 * </ul>
 * All three are events rather than overrides, because the projectiles are vanilla's.
 *
 * <h2>Hay bales</h2>
 * Nothing to do: 1.12.2's own {@code BlockHay.onFallenUpon} already multiplies fall damage by 0.2,
 * exactly as 1.14's {@code HayBlock.fallOn} does. An earlier draft
 * applied the cut again from {@code LivingFallEvent} and so cut it twice; the test that caught it now
 * checks vanilla's number instead.
 */
@Mod.EventBusSubscriber(modid = VillagerBackport.MOD_ID)
public final class CampfireEvents {

    private CampfireEvents() {
    }

    @SubscribeEvent
    public static void onArrow(ProjectileImpactEvent.Arrow event) {
        EntityArrow arrow = event.getArrow();
        RayTraceResult hit = event.getRayTraceResult();
        if (arrow.world.isRemote || !arrow.isBurning() || hit.typeOfHit != RayTraceResult.Type.BLOCK) {
            return;
        }
        light(arrow.world, hit.getBlockPos());
    }

    @SubscribeEvent
    public static void onFireball(ProjectileImpactEvent.Fireball event) {
        EntityFireball ball = event.getFireball();
        RayTraceResult hit = event.getRayTraceResult();
        if (ball.world.isRemote || hit.typeOfHit != RayTraceResult.Type.BLOCK
                || !(ball instanceof net.minecraft.entity.projectile.EntitySmallFireball)) {
            return;
        }
        if (!(ball.shootingEntity instanceof EntityPlayer)
                && !ball.world.getGameRules().getBoolean("mobGriefing")) {
            return;
        }
        light(ball.world, hit.getBlockPos());
    }

    @SubscribeEvent
    public static void onThrowable(ProjectileImpactEvent.Throwable event) {
        if (!(event.getThrowable() instanceof EntityPotion)) {
            return;
        }
        EntityPotion potion = (EntityPotion) event.getThrowable();
        RayTraceResult hit = event.getRayTraceResult();
        if (potion.world.isRemote || hit.typeOfHit != RayTraceResult.Type.BLOCK
                || PotionUtils.getPotionFromItem(potion.getPotion()) != PotionTypes.WATER
                || !PotionUtils.getEffectsFromStack(potion.getPotion()).isEmpty()) {
            return;
        }
        World world = potion.world;
        BlockPos front = hit.getBlockPos().offset(hit.sideHit);
        douse(world, hit.getBlockPos());
        douse(world, front);
        for (EnumFacing side : EnumFacing.Plane.HORIZONTAL) {
            douse(world, front.offset(side));
        }
    }

    private static void light(World world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        if (state.getBlock() instanceof BlockCampfire) {
            BlockCampfire.ignite(world, pos, state);
        }
    }

    private static void douse(World world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        if (BlockCampfire.isLit(state)) {
            BlockCampfire.extinguish(world, pos, state, true);
        }
    }
}
