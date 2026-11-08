package com.exiledradio.villagerbackport.migrate;

import com.exiledradio.backportcore.waterlog.Waterloggable;
import com.exiledradio.backportcore.waterlog.Waterlogging;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;

/**
 * A sea pickle that Village &amp; Pillage 2.3.0 to 2.5.0 placed, in a game without Update Aquatic
 * Backport, which owns sea pickles now.
 *
 * <p>Kept as it was saved, under its old name {@code villagerbackport:sea_pickle} and with the old
 * metadata (pickle count in bits 0-1, the stored {@code waterlogged} flag in bit 2), so that adding
 * Update Aquatic later turns every one into its sea pickle by a plain rename
 * ({@link UpdateAquaticPickles}): same count, same metadata, same water.
 *
 * <p>The water is real, as it was in 2.5.0: the block is {@link Waterloggable}, so Fluidlogged API keeps
 * the water a pickle holds, and a pickle saved by 2.3.0 or 2.4.0 with only the stored flag gets real
 * water once, as its chunk loads ({@link Waterlogging#migrateStoredProperty}, asked for by
 * {@link UpdateAquaticPickles}). It looks and lights as before, glowing only in water, and pops off as
 * its pickles when the block under it goes. Nothing else: it can't be placed, added to, grown or
 * smelted, and breaking it gives the pickles back as {@link ItemSeaPickleStandIn}s.
 *
 * <p>Registered only when Update Aquatic's sea pickle isn't, never in a creative tab, and never placed
 * by villages.
 */
public class BlockSeaPickleKept extends Block implements Waterloggable {

    public static final PropertyInteger PICKLES = PropertyInteger.create("pickles", 1, 4);

    /** Stored by 2.4.0 and earlier; never set now. */
    public static final PropertyBool WATERLOGGED = PropertyBool.create("waterlogged");

    /** 2.3.0's: no collision, but liquids treat it as a wall rather than washing it away. */
    private static final Material MATERIAL = new Material(MapColor.GREEN) {
        {
            this.setNoPushMobility();
        }

        @Override
        public boolean isSolid() {
            return false;
        }

        @Override
        public boolean blocksLight() {
            return false;
        }

        @Override
        public boolean isOpaque() {
            return false;
        }
    };

    /** 1.14.4's {@code SeaPickleBlock} shapes, by pickle count. */
    private static final AxisAlignedBB[] SHAPES = {
            new AxisAlignedBB(6 / 16.0, 0, 6 / 16.0, 10 / 16.0, 6 / 16.0, 10 / 16.0),
            new AxisAlignedBB(3 / 16.0, 0, 3 / 16.0, 13 / 16.0, 6 / 16.0, 13 / 16.0),
            new AxisAlignedBB(2 / 16.0, 0, 2 / 16.0, 14 / 16.0, 6 / 16.0, 14 / 16.0),
            new AxisAlignedBB(2 / 16.0, 0, 2 / 16.0, 14 / 16.0, 7 / 16.0, 14 / 16.0),
    };

    public BlockSeaPickleKept() {
        super(MATERIAL, MapColor.GREEN);
        this.setHardness(0.0F);
        this.setSoundType(SoundType.SLIME);
        this.setDefaultState(this.blockState.getBaseState().withProperty(PICKLES, 1).withProperty(WATERLOGGED, false));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, new IProperty[] {PICKLES, WATERLOGGED});
    }

    @Override
    @SuppressWarnings("deprecation")
    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState().withProperty(PICKLES, (meta & 3) + 1).withProperty(WATERLOGGED, (meta & 4) != 0);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return (state.getValue(PICKLES) - 1) | (state.getValue(WATERLOGGED) ? 4 : 0);
    }

    /** @return true if the pickle holds water, counting a stored flag its chunk has not converted yet */
    static boolean isWet(IBlockState state, IBlockAccess world, BlockPos pos) {
        return state.getValue(WATERLOGGED) || Waterlogging.hasWater(world, pos);
    }

    /** The real water, for the live and dead models. */
    @Override
    @SuppressWarnings("deprecation")
    public IBlockState getActualState(IBlockState state, IBlockAccess world, BlockPos pos) {
        return state.withProperty(WATERLOGGED, isWet(state, world, pos));
    }

    /** Light follows the water. */
    @Override
    public void onWaterChanged(World world, BlockPos pos, IBlockState state, boolean hasWater) {
        world.checkLight(pos);
    }

    /** Pops off, as its pickles, when the block under it goes; nothing else changes it. */
    @Override
    @SuppressWarnings("deprecation")
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block neighbor, BlockPos from) {
        if (world.isRemote || !from.equals(pos.down())) {
            return;
        }
        IBlockState below = world.getBlockState(from);
        if (below.getCollisionBoundingBox(world, from) == NULL_AABB) {
            this.dropBlockAsItem(world, pos, state, 0);
            world.setBlockToAir(pos);
        }
    }

    @Override
    public boolean canPlaceBlockAt(World world, BlockPos pos) {
        return false;
    }

    @Override
    public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos, IBlockState state, int fortune) {
        Item item = UpdateAquaticPickles.standIn();
        if (item != null) {
            drops.add(new ItemStack(item, state.getValue(PICKLES)));
        }
    }

    @Override
    public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world, BlockPos pos, EntityPlayer player) {
        Item item = UpdateAquaticPickles.standIn();
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    /** Current Java's light for this many pickles while waterlogged: 6, 9, 12 or 15. */
    private static int light(int pickles) {
        return 3 + 3 * pickles;
    }

    @Override
    @SuppressWarnings("deprecation")
    public int getLightValue(IBlockState state) {
        return state.getValue(WATERLOGGED) ? light(state.getValue(PICKLES)) : 0;
    }

    @Override
    public int getLightValue(IBlockState state, IBlockAccess world, BlockPos pos) {
        IBlockState here = world.getBlockState(pos);
        if (here.getBlock() != this) {
            return getLightValue(state);
        }
        return isWet(here, world, pos) ? light(here.getValue(PICKLES)) : 0;
    }

    @Override
    @SuppressWarnings("deprecation")
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return SHAPES[state.getValue(PICKLES) - 1];
    }

    @Nullable
    @Override
    @SuppressWarnings("deprecation")
    public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        return NULL_AABB;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos, EnumFacing face) {
        return BlockFaceShape.UNDEFINED;
    }

    @Override
    public boolean isPassable(IBlockAccess world, BlockPos pos) {
        return true;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT;
    }
}
