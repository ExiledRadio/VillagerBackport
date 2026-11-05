package com.exiledradio.villagerbackport.bamboo;

import net.minecraft.block.Block;
import net.minecraft.block.IGrowable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.Random;

/**
 * 1.14's bamboo stalk.
 *
 * <h2>The three properties</h2>
 * <ul>
 * <li><b>age</b> is the stalk's thickness, not its growth: 0 is the thin stalk a fresh shoot
 * grows into, 1 the thick one. A stalk turns thick once it is three blocks tall, and every block
 * grown above a thick one is thick too. A thin block below a thick one thickens to match.</li>
 * <li><b>leaves</b> - none, small or large - follows the top of the stalk as it grows: the top two
 * blocks carry large leaves, the one below them small ones, and everything lower is bare.</li>
 * <li><b>stage</b> 1 means this block has stopped growing. The stalk stops somewhere between 12
 * and 16 blocks: from the 12th block on each new block has a quarter chance of being the last, and
 * the 16th always is.</li>
 * </ul>
 * All twelve combinations fit in the 16 metadata values with room to spare.
 *
 * <h2>Growing</h2>
 * Exactly 1.14's rules, read from its bytecode: a third of random ticks add a block on top, if
 * there is air there with light 9 or more. Bone meal adds one or two at once. The stalk grows on
 * 1.14's bamboo soils (see {@link #isSoil}) and on itself.
 *
 * <h2>Breaking</h2>
 * Hardness 1, mined fastest with an axe, and broken instantly by a sword. Like sugar cane, the
 * blocks above a broken one have nothing to stand on and break too.
 *
 * <h2>Shape</h2>
 * Every block is nudged off-center by up to a quarter of a block, by position, as tall grass is -
 * which is what makes a grove look planted by nothing. The outline is 6 or 10 pixels across
 * depending on the leaves; the part that stops you is a 3-pixel pole.
 */
public class BlockBamboo extends Block implements IGrowable {

    /** Thickness: 0 thin, 1 thick. Named as 1.14 names it. */
    public static final PropertyInteger AGE = PropertyInteger.create("age", 0, 1);
    public static final PropertyEnum<Leaves> LEAVES = PropertyEnum.create("leaves", Leaves.class);
    /** 1 once this block will grow no further. */
    public static final PropertyInteger STAGE = PropertyInteger.create("stage", 0, 1);

    /** The tallest a stalk ever grows. */
    public static final int MAX_HEIGHT = 16;

    private static final AxisAlignedBB SMALL_SHAPE = new AxisAlignedBB(5 / 16D, 0, 5 / 16D, 11 / 16D, 1, 11 / 16D);
    private static final AxisAlignedBB LARGE_SHAPE = new AxisAlignedBB(3 / 16D, 0, 3 / 16D, 13 / 16D, 1, 13 / 16D);
    private static final AxisAlignedBB COLLISION_SHAPE = new AxisAlignedBB(6.5 / 16D, 0, 6.5 / 16D, 9.5 / 16D, 1, 9.5 / 16D);

    /**
     * 1.14's {@code Material.BAMBOO}: blocks movement (so water cannot wash it away and mobs path
     * around it), burns, and is broken rather than shoved by a piston.
     */
    static final Material MATERIAL = new Material(MapColor.FOLIAGE) {
        {
            setNoPushMobility();
        }

        @Override
        public boolean isOpaque() {
            return false;
        }

        @Override
        public boolean blocksLight() {
            return false;
        }

        @Override
        public boolean getCanBurn() {
            return true;
        }
    };

    public enum Leaves implements IStringSerializable {
        NONE("none"),
        SMALL("small"),
        LARGE("large");

        private final String name;

        Leaves(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }
    }

    public BlockBamboo() {
        super(MATERIAL);
        setHardness(1.0F);
        setResistance(1.0F);
        setLightOpacity(0);
        setTickRandomly(true);
        setHarvestLevel("axe", 0);
        setDefaultState(blockState.getBaseState().withProperty(AGE, 0).withProperty(LEAVES, Leaves.NONE)
                .withProperty(STAGE, 0));
    }

    // ---- soil -------------------------------------------------------------------------------

    /**
     * 1.14's {@code bamboo_plantable_on} tag: sand, red sand, gravel, dirt, coarse dirt, podzol,
     * grass and mycelium, and bamboo itself.
     *
     * <p>Any block made of grass is accepted too, so modded grass blocks take bamboo the way the
     * vanilla grass block does.
     */
    public static boolean isSoil(IBlockState state) {
        Block block = state.getBlock();
        return block == BambooContent.bamboo || block == BambooContent.bambooSapling
                || block == Blocks.SAND || block == Blocks.DIRT || block == Blocks.GRASS
                || block == Blocks.GRAVEL || block == Blocks.MYCELIUM
                || state.getMaterial() == Material.GRASS;
    }

    @Override
    public boolean canPlaceBlockAt(World world, BlockPos pos) {
        // 1.14 refuses to place bamboo into water, which 1.12 would otherwise count as replaceable.
        return super.canPlaceBlockAt(world, pos) && !world.getBlockState(pos).getMaterial().isLiquid()
                && isSoil(world.getBlockState(pos.down()));
    }

    public boolean canBlockStay(World world, BlockPos pos) {
        return isSoil(world.getBlockState(pos.down()));
    }

    /**
     * What placing bamboo here makes: a shoot on bare soil, or another block of stalk on top of a
     * stalk or shoot, as thick as the stalk below.
     */
    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY,
                                            float hitZ, int meta, EntityLivingBase placer, EnumHand hand) {
        IBlockState below = world.getBlockState(pos.down());
        if (below.getBlock() == BambooContent.bambooSapling) {
            return getDefaultState().withProperty(AGE, 0);
        }
        if (below.getBlock() == this) {
            return getDefaultState().withProperty(AGE, below.getValue(AGE) > 0 ? 1 : 0);
        }
        return BambooContent.bambooSapling.getDefaultState();
    }

    @Override
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block, BlockPos fromPos) {
        if (!canBlockStay(world, pos)) {
            world.scheduleUpdate(pos, this, 1);
            return;
        }
        // 1.14's updateShape: a thin block under a thick one thickens to match.
        if (fromPos.equals(pos.up())) {
            IBlockState above = world.getBlockState(fromPos);
            if (above.getBlock() == this && above.getValue(AGE) > state.getValue(AGE)) {
                world.setBlockState(pos, state.cycleProperty(AGE), 2);
            }
        }
    }

    /** Scheduled ticks only check the block can still stand; growth is the random tick. */
    @Override
    public void updateTick(World world, BlockPos pos, IBlockState state, Random rand) {
        if (!canBlockStay(world, pos)) {
            world.destroyBlock(pos, true);
        }
    }

    // ---- growth -----------------------------------------------------------------------------

    @Override
    public void randomTick(World world, BlockPos pos, IBlockState state, Random rand) {
        if (!canBlockStay(world, pos)) {
            world.destroyBlock(pos, true);
            return;
        }
        if (state.getValue(STAGE) != 0) {
            return;
        }
        if (rand.nextInt(3) == 0 && world.isAirBlock(pos.up()) && world.getLight(pos.up()) >= 9) {
            int height = heightBelow(world, pos) + 1;
            if (height < MAX_HEIGHT) {
                grow(state, world, pos, rand, height);
            }
        }
    }

    /**
     * Adds one block on top of the block at {@code pos}, which is {@code height} blocks tall
     * counting itself, and moves the leaves up with it.
     */
    protected void grow(IBlockState state, World world, BlockPos pos, Random rand, int height) {
        IBlockState below = world.getBlockState(pos.down());
        BlockPos below2Pos = pos.down(2);
        IBlockState below2 = world.getBlockState(below2Pos);
        Leaves leaves = Leaves.NONE;

        if (height >= 1) {
            if (below.getBlock() != this || below.getValue(LEAVES) == Leaves.NONE) {
                leaves = Leaves.SMALL;
            } else {
                leaves = Leaves.LARGE;
                if (below2.getBlock() == this) {
                    world.setBlockState(pos.down(), below.withProperty(LEAVES, Leaves.SMALL), 3);
                    world.setBlockState(below2Pos, below2.withProperty(LEAVES, Leaves.NONE), 3);
                }
            }
        }

        int age = state.getValue(AGE) == 1 || below2.getBlock() == this ? 1 : 0;
        int stage = (height >= 11 && rand.nextFloat() < 0.25F) || height == 15 ? 1 : 0;
        world.setBlockState(pos.up(), getDefaultState().withProperty(AGE, age).withProperty(LEAVES, leaves)
                .withProperty(STAGE, stage), 3);
    }

    /** @return how many blocks of bamboo stand directly above this one, up to 16 */
    protected int heightAbove(IBlockAccess world, BlockPos pos) {
        int i = 0;
        while (i < MAX_HEIGHT && world.getBlockState(pos.up(i + 1)).getBlock() == this) {
            i++;
        }
        return i;
    }

    /** @return how many blocks of bamboo stand directly below this one, up to 16 */
    protected int heightBelow(IBlockAccess world, BlockPos pos) {
        int i = 0;
        while (i < MAX_HEIGHT && world.getBlockState(pos.down(i + 1)).getBlock() == this) {
            i++;
        }
        return i;
    }

    @Override
    public boolean canGrow(World world, BlockPos pos, IBlockState state, boolean isClient) {
        int above = heightAbove(world, pos);
        int below = heightBelow(world, pos);
        return above + below + 1 < MAX_HEIGHT && world.getBlockState(pos.up(above)).getValue(STAGE) != 1;
    }

    @Override
    public boolean canUseBonemeal(World world, Random rand, BlockPos pos, IBlockState state) {
        return true;
    }

    /** One or two blocks on top of the whole stalk, wherever on it the bone meal was used. */
    @Override
    public void grow(World world, Random rand, BlockPos pos, IBlockState state) {
        int above = heightAbove(world, pos);
        int total = above + heightBelow(world, pos) + 1;
        int amount = 1 + rand.nextInt(2);

        for (int i = 0; i < amount; i++) {
            BlockPos top = pos.up(above);
            IBlockState topState = world.getBlockState(top);
            if (total >= MAX_HEIGHT || topState.getBlock() != this || topState.getValue(STAGE) == 1
                    || !world.isAirBlock(top.up())) {
                return;
            }
            grow(topState, world, top, rand, total);
            above++;
            total++;
        }
    }

    // ---- breaking ---------------------------------------------------------------------------

    /** A sword cuts bamboo in one hit, as in 1.14. */
    @Override
    public float getPlayerRelativeBlockHardness(IBlockState state, EntityPlayer player, World world, BlockPos pos) {
        if (player.getHeldItemMainhand().getItem() instanceof ItemSword) {
            return 1.0F;
        }
        return super.getPlayerRelativeBlockHardness(state, player, world, pos);
    }

    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return BambooContent.bambooItem;
    }

    @Override
    public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world, BlockPos pos,
                                  EntityPlayer player) {
        return new ItemStack(BambooContent.bambooItem);
    }

    @Override
    public SoundType getSoundType(IBlockState state, World world, BlockPos pos, @Nullable Entity entity) {
        return BambooContent.bambooSound != null ? BambooContent.bambooSound : SoundType.WOOD;
    }

    // ---- shape ------------------------------------------------------------------------------

    @Override
    public EnumOffsetType getOffsetType() {
        return EnumOffsetType.XZ;
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        AxisAlignedBB shape = state.getValue(LEAVES) == Leaves.LARGE ? LARGE_SHAPE : SMALL_SHAPE;
        return shape.offset(state.getOffset(source, pos));
    }

    @Nullable
    @Override
    public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        return COLLISION_SHAPE.offset(state.getOffset(world, pos));
    }

    @Override
    public boolean isPassable(IBlockAccess world, BlockPos pos) {
        return false;
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos, EnumFacing face) {
        return BlockFaceShape.UNDEFINED;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT;
    }

    // ---- state ------------------------------------------------------------------------------

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, AGE, LEAVES, STAGE);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        int leaves = (meta >> 1) & 3;
        return getDefaultState().withProperty(AGE, meta & 1)
                .withProperty(LEAVES, Leaves.values()[Math.min(leaves, 2)])
                .withProperty(STAGE, (meta >> 3) & 1);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(AGE) | state.getValue(LEAVES).ordinal() << 1 | state.getValue(STAGE) << 3;
    }

    /** Thick, bare and still growing: the body of a stalk, as worldgen builds it. */
    public IBlockState trunk() {
        return getDefaultState().withProperty(AGE, 1).withProperty(LEAVES, Leaves.NONE).withProperty(STAGE, 0);
    }

    @Override
    public EnumPushReaction getPushReaction(IBlockState state) {
        return EnumPushReaction.DESTROY;
    }
}
