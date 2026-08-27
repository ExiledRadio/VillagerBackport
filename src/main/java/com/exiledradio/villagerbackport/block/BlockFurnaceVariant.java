package com.exiledradio.villagerbackport.block;

import com.exiledradio.villagerbackport.GuiHandler;
import com.exiledradio.villagerbackport.VillagerBackport;

import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Random;

/**
 * A smoker or blast furnace: half the cook time, a fraction of the recipes.
 *
 * <h2>Lit state</h2>
 * Vanilla registers a furnace twice - once lit, once not - as two separate blocks, swapping one for
 * the other and carrying the tile entity across by hand. That was a workaround for a limitation
 * 1.12.2 does not have. A {@code lit} property does the same job without a second registry entry, a
 * second item to hide from the creative menu, or the swap code that has to preserve state.
 */
public class BlockFurnaceVariant extends BlockWorkstationFacing {

    public static final PropertyBool LIT = PropertyBool.create("lit");

    private final FurnaceKind kind;

    public BlockFurnaceVariant(Material material, SoundType sound, float hardness, FurnaceKind kind) {
        super(material, sound, hardness);
        this.kind = kind;
        setDefaultState(this.blockState.getBaseState()
                .withProperty(FACING, EnumFacing.NORTH)
                .withProperty(LIT, Boolean.FALSE));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING, LIT);
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return this.kind == FurnaceKind.SMOKER
                ? new TileEntityFurnaceVariant.Smoker()
                : new TileEntityFurnaceVariant.Blast();
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player,
                                    EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (!world.isRemote && world.getTileEntity(pos) instanceof TileEntityFurnaceVariant) {
            player.openGui(VillagerBackport.instance, GuiHandler.FURNACE, world,
                    pos.getX(), pos.getY(), pos.getZ());
        }
        return true;
    }

    /** Lit furnaces glow, as vanilla's do. */
    @Override
    public int getLightValue(IBlockState state) {
        return state.getValue(LIT) ? 13 : 0;
    }

    /**
     * Smoke and flame from the front while it is burning.
     *
     * <p>Offset onto the facing side rather than the block centre, so it reads as coming out of the
     * opening - which is the only reason these blocks have a front at all.
     */
    @Override
    public void randomDisplayTick(IBlockState state, World world, BlockPos pos, Random rand) {
        if (!state.getValue(LIT)) {
            return;
        }

        EnumFacing facing = state.getValue(FACING);
        double y = pos.getY() + rand.nextDouble() * 6.0D / 16.0D;
        double along = rand.nextDouble() * 0.6D - 0.3D;
        double edge = 0.52D;

        boolean onX = facing.getAxis() == EnumFacing.Axis.X;
        double x = pos.getX() + 0.5D + (onX ? facing.getXOffset() * edge : along);
        double z = pos.getZ() + 0.5D + (onX ? along : facing.getZOffset() * edge);

        world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, x, y, z, 0.0D, 0.0D, 0.0D);
        world.spawnParticle(EnumParticleTypes.FLAME, x, y, z, 0.0D, 0.0D, 0.0D);
    }

    /** Contents spill when broken, as a furnace's do. */
    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof TileEntityFurnaceVariant) {
            InventoryHelper.dropInventoryItems(world, pos, (TileEntityFurnaceVariant) tile);
            world.updateComparatorOutputLevel(pos, this);
        }
        super.breakBlock(world, pos, state);
    }

    @Override
    public boolean hasComparatorInputOverride(IBlockState state) {
        return true;
    }

    @Override
    public int getComparatorInputOverride(IBlockState state, World world, BlockPos pos) {
        TileEntity tile = world.getTileEntity(pos);
        return tile instanceof net.minecraft.inventory.IInventory
                ? net.minecraft.inventory.Container.calcRedstoneFromInventory(
                        (net.minecraft.inventory.IInventory) tile)
                : 0;
    }

    // ---------------------------------------------------------------- metadata

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState()
                .withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3))
                .withProperty(LIT, (meta & 4) != 0);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(FACING).getHorizontalIndex() | (state.getValue(LIT) ? 4 : 0);
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
                                            float hitX, float hitY, float hitZ, int meta,
                                            EntityLivingBase placer) {
        return getDefaultState()
                .withProperty(FACING, placer.getHorizontalFacing().getOpposite())
                .withProperty(LIT, Boolean.FALSE);
    }
}
