package com.exiledradio.villagerbackport.woodset;

import com.exiledradio.backportcore.waterlog.Waterloggable;
import com.exiledradio.backportcore.waterlog.Waterlogging;

import net.minecraft.block.Block;
import net.minecraft.block.IGrowable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.Random;

/**
 * Current Java's sea pickle: one to four in a block, waterloggable, glowing only in water.
 *
 * <h2>What carries over</h2>
 * <ul>
 *   <li>{@code pickles} 1 to 4. Right-clicking a pickle block holding a sea pickle adds one, up to four,
 *       and breaking drops one item per pickle. 1.14's outline boxes per count.</li>
 *   <li>Light {@code 3 + 3 * pickles} - 6, 9, 12, 15 - when waterlogged, none when not.</li>
 *   <li>Stands on any block with a collision box (1.14 asks that the block below projects a non-empty
 *       collision shape upward), and pops off when that block goes.</li>
 *   <li>No collision of its own, broken instantly, slime sounds, green on maps, destroyed by pistons.</li>
 *   <li>Smelts to lime dye, which 1.12.2 already has (dye metadata 10).</li>
 * </ul>
 *
 * <h2>Water</h2>
 * The water is real: Fluidlogged API keeps it in the pickle's block, through Backport Core's
 * {@link Waterloggable}. Placed into a water source - on the sea floor, say - a pickle keeps the water, a
 * bucket fills or drains it, and it never breaks when the water goes, as in current Java. Light and the
 * live or dead model follow that water ({@link #getLightValue(IBlockState, IBlockAccess, BlockPos)},
 * {@link #getActualState}).
 *
 * <p>The {@code waterlogged} property stays, with its metadata bit (4), because worlds saved by 2.4.0 and
 * earlier stored it: back then a pickle had no water of its own and the property said whether water was
 * beside it. Nothing sets it now. A pickle stored with it true is given a real water source once - when
 * its chunk first loads ({@link Waterlogging#migrateStoredProperty}, called by {@link WoodsetContent}), or
 * at its first update if it got there some other way, such as a structure file or a command - and the
 * property is cleared. {@link #getActualState} reports the real water, for the models.
 *
 * <p>Flowing water does not wash a dry pickle away: the material blocks liquids, as a waterloggable block
 * does in current Java, while having no collision box.
 *
 * <h2>Bone meal</h2>
 * As in current Java: bone meal on a waterlogged pickle sitting on a living coral block makes it four
 * pickles and spreads more across the coral blocks around it. 1.12.2 has no coral of its own, so the
 * coral blocks are looked up by name ({@link #CORAL_BLOCKS}); without them bone meal does nothing, as it
 * does on a pickle on any other block in current Java.
 */
public class BlockSeaPickle extends Block implements Waterloggable, IGrowable {

    public static final PropertyInteger PICKLES = PropertyInteger.create("pickles", 1, 4);

    /** Stored by 2.4.0 and earlier; never set now. See the class notes. */
    public static final PropertyBool WATERLOGGED = PropertyBool.create("waterlogged");

    /** Living coral blocks a waterlogged pickle grows on with bone meal: current Java's {@code #coral_blocks}. */
    static final ResourceLocation[] CORAL_BLOCKS = {
            new ResourceLocation("updateaquaticbackport", "tube_coral_block"),
            new ResourceLocation("updateaquaticbackport", "brain_coral_block"),
            new ResourceLocation("updateaquaticbackport", "bubble_coral_block"),
            new ResourceLocation("updateaquaticbackport", "fire_coral_block"),
            new ResourceLocation("updateaquaticbackport", "horn_coral_block"),
    };

    /**
     * Not solid, not light-blocking, but blocking liquids: water and lava treat a block whose material
     * blocks movement as a wall (see {@code BlockDynamicLiquid.isBlocked}) instead of washing it away.
     */
    static final Material MATERIAL = new Material(MapColor.GREEN) {
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

    public BlockSeaPickle() {
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

    // ------------------------------------------------------------------------------------------
    // Water
    // ------------------------------------------------------------------------------------------

    /** @return true if the pickle is waterlogged, counting a stored flag not yet moved into real water */
    static boolean isWet(IBlockState state, IBlockAccess world, BlockPos pos) {
        return state.getValue(WATERLOGGED) || Waterlogging.hasWater(world, pos);
    }

    /** Placed dry: Fluidlogged API keeps the water of a source it is placed into. */
    @Override
    @SuppressWarnings("deprecation")
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY,
                                            float hitZ, int meta, EntityLivingBase placer) {
        return this.getDefaultState();
    }

    /**
     * Turns a stored {@code waterlogged=true} into real water, once: a 2.4.0 world's flag that the
     * chunk-load conversion has not reached yet, a structure file's state, a command's.
     *
     * @return the state now at the position
     */
    static IBlockState moveStoredWater(World world, BlockPos pos, IBlockState state) {
        if (world.isRemote || !state.getValue(WATERLOGGED)) {
            return state;
        }
        IBlockState dry = state.withProperty(WATERLOGGED, false);
        world.setBlockState(pos, dry, 2);
        Waterlogging.setWater(world, pos, true, 3);
        world.checkLight(pos);
        return dry;
    }

    @Override
    public void onBlockAdded(World world, BlockPos pos, IBlockState state) {
        moveStoredWater(world, pos, state);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block neighbor, BlockPos from) {
        if (world.isRemote) {
            return;
        }
        if (!canStayOn(world, pos.down())) {
            this.dropBlockAsItem(world, pos, state, 0);
            world.setBlockToAir(pos);
            return;
        }
        moveStoredWater(world, pos, state);
    }

    @Override
    public void updateTick(World world, BlockPos pos, IBlockState state, Random rand) {
        moveStoredWater(world, pos, state);
    }

    /** Light follows the water: relight whenever it comes or goes. */
    @Override
    public void onWaterChanged(World world, BlockPos pos, IBlockState state, boolean hasWater) {
        world.checkLight(pos);
    }

    /** The real water, for the dead and live models; the stored value is only for older worlds. */
    @Override
    @SuppressWarnings("deprecation")
    public IBlockState getActualState(IBlockState state, IBlockAccess world, BlockPos pos) {
        return state.withProperty(WATERLOGGED, isWet(state, world, pos));
    }

    // ------------------------------------------------------------------------------------------
    // Placing and adding
    // ------------------------------------------------------------------------------------------

    /** @return true if the block at {@code below} has collision a pickle can stand on */
    @SuppressWarnings("deprecation")
    static boolean canStayOn(World world, BlockPos below) {
        IBlockState state = world.getBlockState(below);
        return state.getCollisionBoundingBox(world, below) != NULL_AABB;
    }

    @Override
    public boolean canPlaceBlockAt(World world, BlockPos pos) {
        return super.canPlaceBlockAt(world, pos) && canStayOn(world, pos.down());
    }

    /**
     * Right-clicking with a sea pickle adds one, as 1.14 does by letting the pickle item replace the
     * block it clicks. Done here rather than in the item so it needs no item class of its own. The
     * water stays: it is the same block.
     */
    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand,
                                    EnumFacing facing, float hitX, float hitY, float hitZ) {
        ItemStack held = player.getHeldItem(hand);
        if (held.isEmpty() || held.getItem() != Item.getItemFromBlock(this) || state.getValue(PICKLES) >= 4) {
            return false;
        }
        if (!world.isRemote) {
            IBlockState now = moveStoredWater(world, pos, state);
            world.setBlockState(pos, now.withProperty(PICKLES, now.getValue(PICKLES) + 1), 3);
            world.checkLight(pos);
            SoundType sound = this.getSoundType(state, world, pos, player);
            world.playSound(null, pos, sound.getPlaceSound(), SoundCategory.BLOCKS,
                    (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
            if (!player.capabilities.isCreativeMode) {
                held.shrink(1);
            }
        }
        return true;
    }

    // ------------------------------------------------------------------------------------------
    // Light, shape, drops
    // ------------------------------------------------------------------------------------------

    /** @return current Java's light for this many pickles while waterlogged: 6, 9, 12 or 15 */
    static int light(int pickles) {
        return 3 + 3 * pickles;
    }

    /** Without a position there is no water to ask about, so only a stored flag counts. */
    @Override
    @SuppressWarnings("deprecation")
    public int getLightValue(IBlockState state) {
        return state.getValue(WATERLOGGED) ? light(state.getValue(PICKLES)) : 0;
    }

    /** Lit only while waterlogged, as in current Java. */
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

    @Override
    public int quantityDropped(IBlockState state, int fortune, Random random) {
        return state.getValue(PICKLES);
    }

    // ------------------------------------------------------------------------------------------
    // Bone meal, from current Java's SeaPickleBlock
    // ------------------------------------------------------------------------------------------

    /** @return true if the block is a living coral block, by name: 1.12.2 has none of its own */
    static boolean isCoralBlock(IBlockState state) {
        ResourceLocation name = state.getBlock().getRegistryName();
        if (name == null) {
            return false;
        }
        for (ResourceLocation coral : CORAL_BLOCKS) {
            if (coral.equals(name)) {
                return true;
            }
        }
        return false;
    }

    /** Only a waterlogged pickle on a living coral block, as {@code isValidBonemealTarget}. */
    @Override
    public boolean canGrow(World world, BlockPos pos, IBlockState state, boolean isClient) {
        return isWet(state, world, pos) && isCoralBlock(world.getBlockState(pos.down()));
    }

    @Override
    public boolean canUseBonemeal(World world, Random rand, BlockPos pos, IBlockState state) {
        return true;
    }

    /**
     * {@code performBonemeal}: in a diamond five blocks across, in the layer below the pickle and its
     * own, each water block over a coral block has a one in six chance of one to four pickles; then this
     * pickle becomes four.
     */
    @Override
    public void grow(World world, Random rand, BlockPos pos, IBlockState state) {
        int width = 1;
        int offset = 0;
        int x0 = pos.getX() - 2;
        int step = 0;
        for (int dx = 0; dx < 5; dx++) {
            for (int dz = 0; dz < width; dz++) {
                int top = 2 + pos.getY() - 1;
                for (int y = top - 2; y < top; y++) {
                    BlockPos at = new BlockPos(x0 + dx, y, pos.getZ() - offset + dz);
                    if (rand.nextInt(6) == 0 && isWaterBlock(world.getBlockState(at))
                            && isCoralBlock(world.getBlockState(at.down()))) {
                        Waterlogging.placeWithWater(world, at,
                                this.getDefaultState().withProperty(PICKLES, rand.nextInt(4) + 1), 3);
                    }
                }
            }
            if (step < 2) {
                width += 2;
                offset++;
            } else {
                width -= 2;
                offset--;
            }
            step++;
        }
        IBlockState now = moveStoredWater(world, pos, world.getBlockState(pos));
        if (now.getBlock() == this) {
            world.setBlockState(pos, now.withProperty(PICKLES, 4), 2);
            world.checkLight(pos);
        }
    }

    /** Current Java's {@code is(Blocks.WATER)}: a water block, source or flowing, not water kept in a block. */
    static boolean isWaterBlock(IBlockState state) {
        return state.getBlock() == Blocks.WATER || state.getBlock() == Blocks.FLOWING_WATER;
    }
}
