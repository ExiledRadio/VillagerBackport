package com.exiledradio.villagerbackport.flora;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Blocks;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Random;

/**
 * The wither rose: a black flower that withers whatever walks through it.
 *
 * <h2>Contact</h2>
 * Any living thing standing in the block gets Wither for two seconds (1.14's 40 ticks), renewed for
 * as long as it stays - so it takes damage while inside and for a moment after leaving, as the wiki
 * describes. Not on Peaceful, and not to the three mobs the wiki names as immune - the Wither,
 * wither skeletons and the ender dragon - nor to anything else that refuses the effect, nor to a
 * creative or spectator player, who is invulnerable to it as in 1.14.
 *
 * <h2>Where it can stand</h2>
 * Wherever other flowers can, and also on netherrack and soul sand, which is where it is most often
 * planted - the Wither is usually fought in the Nether or near one.
 *
 * <h2>Where it comes from</h2>
 * It does not generate. A mob the Wither kills leaves one where it died (see {@link WitherRoses}),
 * and it does not grow from bone meal. Smoke drifts up from its stem.
 */
public class BlockWitherRose extends BlockFloraFlower {

    /** 1.14's Wither duration on contact, in ticks. */
    private static final int WITHER_TICKS = 40;

    @Override
    public void onEntityCollision(World world, BlockPos pos, IBlockState state, Entity entity) {
        if (world.isRemote || world.getDifficulty() == EnumDifficulty.PEACEFUL
                || !(entity instanceof EntityLivingBase)) {
            return;
        }

        EntityLivingBase living = (EntityLivingBase) entity;
        if (!isImmune(living) && !living.isEntityInvulnerable(DamageSource.WITHER)) {
            living.addPotionEffect(new PotionEffect(MobEffects.WITHER, WITHER_TICKS));
        }
    }

    /**
     * The wiki's three immune mobs. 1.14 gives wither skeletons a potion immunity 1.12.2's do not
     * have, so they, the Wither and the dragon are named here.
     */
    private static boolean isImmune(EntityLivingBase living) {
        return living instanceof net.minecraft.entity.monster.EntityWitherSkeleton
                || living instanceof net.minecraft.entity.boss.EntityWither
                || living instanceof net.minecraft.entity.boss.EntityDragon
                || !living.isPotionApplicable(new PotionEffect(MobEffects.WITHER, WITHER_TICKS));
    }

    @Override
    protected boolean canSustainBush(IBlockState state) {
        return super.canSustainBush(state) || isNetherSoil(state.getBlock());
    }

    @Override
    public boolean canPlaceBlockAt(World world, BlockPos pos) {
        return super.canPlaceBlockAt(world, pos)
                || (world.getBlockState(pos).getBlock().isReplaceable(world, pos)
                && isNetherSoil(world.getBlockState(pos.down()).getBlock()));
    }

    @Override
    public boolean canBlockStay(World world, BlockPos pos, IBlockState state) {
        return super.canBlockStay(world, pos, state) || isNetherSoil(world.getBlockState(pos.down()).getBlock());
    }

    private static boolean isNetherSoil(Block block) {
        return block == Blocks.NETHERRACK || block == Blocks.SOUL_SAND;
    }

    /** 1.14's smoke: up to three puffs a tick, from around the stem. */
    @Override
    @SideOnly(Side.CLIENT)
    public void randomDisplayTick(IBlockState state, World world, BlockPos pos, Random rand) {
        net.minecraft.util.math.Vec3d offset = state.getOffset(world, pos);
        double x = pos.getX() + 0.5D + offset.x;
        double z = pos.getZ() + 0.5D + offset.z;

        for (int i = 0; i < 3; i++) {
            if (rand.nextBoolean()) {
                world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL,
                        x + rand.nextFloat() / 5.0D, pos.getY() + (0.5D - rand.nextFloat()),
                        z + rand.nextFloat() / 5.0D, 0.0D, 0.0D, 0.0D);
            }
        }
    }
}
