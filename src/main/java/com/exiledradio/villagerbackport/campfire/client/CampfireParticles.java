package com.exiledradio.villagerbackport.campfire.client;

import com.exiledradio.villagerbackport.campfire.BlockCampfire;
import com.exiledradio.villagerbackport.campfire.PacketCampfireDowse;
import com.exiledradio.villagerbackport.campfire.TileEntityCampfire;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Random;

/**
 * The campfire's smoke, ported from 1.14.4's {@code CampfireBlock.makeParticles} and
 * {@code CampfireTileEntity.makeParticles}.
 *
 * <p>Every tick a lit campfire has an 11% chance of putting up two or three plumes, each from a random
 * spot within a third of a block of the center, between one and three blocks up, rising at 0.07 a
 * tick. Each item cooking on it gives off four wisps of ordinary smoke one tick in five, from its own
 * spot on the logs.
 *
 * <p>The methods take only common types, so the tile entity and the packet - both loaded by a
 * dedicated server - can call them without the server ever loading this class.
 */
@SideOnly(Side.CLIENT)
public final class CampfireParticles {

    /** How far each cooking spot sits from the center along its side, in blocks (1.14's 0.3125). */
    private static final double SPOT = 0.3125D;

    private CampfireParticles() {
    }

    /** Called every client tick from a lit campfire's tile entity. */
    public static void tick(World world, BlockPos pos, IBlockState state, NonNullList<ItemStack> items) {
        Random rand = world.rand;
        boolean signal = state.getValue(BlockCampfire.SIGNAL_FIRE).booleanValue();
        if (rand.nextFloat() < 0.11F) {
            for (int i = 0; i < rand.nextInt(2) + 2; i++) {
                smoke(world, pos, signal, false);
            }
        }

        EnumFacing facing = state.getValue(BlockCampfire.FACING);
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).isEmpty() || rand.nextFloat() >= 0.2F) {
                continue;
            }
            EnumFacing side = TileEntityCampfire.slotDirection(facing, i);
            EnumFacing across = side.rotateY();
            double x = pos.getX() + 0.5D - side.getXOffset() * SPOT + across.getXOffset() * SPOT;
            double y = pos.getY() + 0.5D;
            double z = pos.getZ() + 0.5D - side.getZOffset() * SPOT + across.getZOffset() * SPOT;
            for (int k = 0; k < 4; k++) {
                world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, x, y, z, 0.0D, 5.0E-4D, 0.0D);
            }
        }
    }

    /** The twenty-plume burst of a campfire doused with water. */
    public static void dowse(PacketCampfireDowse message) {
        World world = Minecraft.getMinecraft().world;
        if (world == null) {
            return;
        }
        for (int i = 0; i < 20; i++) {
            smoke(world, message.pos(), message.signal(), true);
        }
    }

    /**
     * 1.14's {@code CampfireBlock.makeParticles}: one plume, and with {@code wisps} a low puff of
     * ordinary smoke as well.
     */
    static void smoke(World world, BlockPos pos, boolean signal, boolean wisps) {
        Random rand = world.rand;
        TextureAtlasSprite sprite = CampfireClient.SMOKE[rand.nextInt(CampfireClient.SMOKE.length)];
        if (sprite != null) {
            double x = pos.getX() + 0.5D + rand.nextDouble() / 3.0D * (rand.nextBoolean() ? 1 : -1);
            double y = pos.getY() + rand.nextDouble() + rand.nextDouble();
            double z = pos.getZ() + 0.5D + rand.nextDouble() / 3.0D * (rand.nextBoolean() ? 1 : -1);
            Minecraft.getMinecraft().effectRenderer.addEffect(
                    new ParticleCampfireSmoke(world, x, y, z, 0.0D, 0.07D, 0.0D, signal, sprite));
        }
        if (wisps) {
            world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL,
                    pos.getX() + 0.25D + rand.nextDouble() / 2.0D * (rand.nextBoolean() ? 1 : -1),
                    pos.getY() + 0.4D,
                    pos.getZ() + 0.25D + rand.nextDouble() / 2.0D * (rand.nextBoolean() ? 1 : -1),
                    0.0D, 0.005D, 0.0D);
        }
    }
}
