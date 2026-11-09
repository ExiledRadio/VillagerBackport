package com.exiledradio.villagerbackport.bamboo;

import com.exiledradio.backportcore.waterlog.Waterloggable;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityFallingBlock;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Random;

/**
 * 1.14's scaffolding.
 *
 * <h2>Support</h2>
 * Every scaffold knows its <b>distance</b> from support, 0 to 7, recomputed whenever a neighbor
 * changes, exactly as 1.14 does it:
 * <ul>
 * <li>standing on a block with a solid top: 0;</li>
 * <li>standing on scaffolding: the same distance as the scaffold below, so a column carries its
 * base's distance all the way up;</li>
 * <li>otherwise one more than the nearest scaffold beside it.</li>
 * </ul>
 * A scaffold that works out to 7 cannot exist: it breaks and drops itself, and anything leaning on
 * it follows - so a run reaches six blocks out from a supported column and no further, and taking
 * out the bottom of a tower brings the whole tower down as items. One that was already 7 when
 * ticked (only possible when a piston or a mod put it there) falls as a falling block instead.
 *
 * <p>A scaffold hanging in the air with nothing under it shows the extra frame along its bottom
 * edge - the <b>bottom</b> state - and that frame is something to stand on.
 *
 * <h2>Moving through it</h2>
 * 1.14 has a "climbable" block tag and a collision shape that depends on the entity. 1.12 has
 * neither, but has the two pieces that make the same thing:
 * <ul>
 * <li><b>Collision per entity.</b> {@link #addCollisionBoxToList} is handed the entity, so the box
 * it adds can depend on where that entity is. Above the top of a scaffold and not sneaking, it is
 * solid underfoot. Anywhere else it has no collision at all - the sides walk straight through - apart
 * from the bottom frame of a hanging scaffold. This is 1.14's {@code getCollisionShape} line for
 * line.</li>
 * <li><b>Climbing</b> is a ladder: {@link #isLadder} says yes while the entity is not sneaking, so
 * falling inside a scaffold is slowed to a slide and walking into one does not hurt. Jumping inside
 * a scaffold climbs it, done in {@link #onEntityCollision} the way 1.14 does on a ladder when the
 * jump key is held.</li>
 * <li><b>Sneaking</b> drops through the top and descends at 1.14's climbing speed, without fall
 * damage. 1.12's own ladder rule would instead hold a sneaking player still, so a sneaking entity is
 * not on a ladder here and its descent is slowed in {@link #onEntityCollision}.</li>
 * </ul>
 * None of this touches how players move: it only answers the questions the game already asks every
 * block, so anything that rewrites player movement - Aqua Acrobatics, for one - is unaffected and
 * still sees an ordinary block.
 *
 * <h2>Water</h2>
 * Waterloggable as in current Java: placed into a water source it keeps it, a bucket fills or drains
 * it, and the water changes nothing else about it. Without Fluidlogged API it holds none, as before
 * 1.13; flowing water still cannot wash it away.
 *
 * <h2>Not 1.14</h2>
 * The outline is always a full block. 1.14 shows the full block only while you hold scaffolding,
 * and the frame otherwise, but 1.12's outline cannot see what is in your hand.
 */
public class BlockScaffolding extends Block implements Waterloggable {

    public static final PropertyInteger DISTANCE = PropertyInteger.create("distance", 0, 7);
    public static final PropertyBool BOTTOM = PropertyBool.create("bottom");

    /** A scaffold this far from support cannot stand. */
    public static final int MAX_DISTANCE = 7;

    /** 1.14 compares heights with this slack, in floats. */
    private static final double EPSILON = 1.0E-5F;

    /** The platform on top and the four legs: what stops you when you stand on it. */
    private static final AxisAlignedBB[] STABLE = {
            box(0, 14, 0, 16, 16, 16),
            box(0, 0, 0, 2, 16, 2),
            box(14, 0, 0, 16, 16, 2),
            box(0, 0, 14, 2, 16, 16),
            box(14, 0, 14, 16, 16, 16),
    };

    /** The extra frame along the bottom of a hanging scaffold. */
    private static final AxisAlignedBB BOTTOM_FRAME = box(0, 0, 0, 16, 2, 16);

    /** Ladder climbing speed in 1.14. */
    private static final double CLIMB_SPEED = 0.2D;

    /**
     * The motion to leave behind so the next tick's move is exactly 1.14's 0.15-a-tick descent.
     * 1.12 applies gravity ({@code -0.08}, then {@code *0.98}) between this block seeing the
     * entity and that entity moving again: {@code (v - 0.08) * 0.98 = -0.15}.
     */
    private static final double SNEAK_DESCENT = -0.15D / 0.98D + 0.08D;

    /** {@code EntityLivingBase.isJumping}: the jump key for players, the jump intent for mobs. */
    @Nullable
    private static final Field IS_JUMPING = jumpingField();

    /**
     * 1.14's {@code Material.MISCELLANEOUS}, sand-colored on maps. It blocks movement as far as
     * water and falling blocks are concerned - so flowing water cannot wash scaffolding away - while
     * {@link #isPassable} lets mobs plan paths through it. A water source it holds is Fluidlogged
     * API's, through Backport Core ({@link Waterloggable}), as current Java waterlogs scaffolding.
     */
    static final Material MATERIAL = new Material(MapColor.SAND) {
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

        @Override
        public boolean getCanBurn() {
            return true;
        }
    };

    public BlockScaffolding() {
        super(MATERIAL);
        setHardness(0.0F);
        setLightOpacity(0);
        setDefaultState(blockState.getBaseState().withProperty(DISTANCE, MAX_DISTANCE).withProperty(BOTTOM, false));
    }

    private static AxisAlignedBB box(double x1, double y1, double z1, double x2, double y2, double z2) {
        return new AxisAlignedBB(x1 / 16D, y1 / 16D, z1 / 16D, x2 / 16D, y2 / 16D, z2 / 16D);
    }

    @Nullable
    private static Field jumpingField() {
        try {
            return ObfuscationReflectionHelper.findField(EntityLivingBase.class, "field_70703_bu");
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static boolean isJumping(EntityLivingBase entity) {
        if (IS_JUMPING == null) {
            return false;
        }
        try {
            return IS_JUMPING.getBoolean(entity);
        } catch (IllegalAccessException e) {
            return false;
        }
    }

    // ---- support ----------------------------------------------------------------------------

    /** @return how far {@code pos} would be from support, 0 to 7, as 1.14 counts it */
    public static int getDistance(IBlockAccess world, BlockPos pos) {
        BlockPos belowPos = pos.down();
        IBlockState below = world.getBlockState(belowPos);
        int distance = MAX_DISTANCE;

        if (below.getBlock() == BambooContent.scaffolding) {
            distance = below.getValue(DISTANCE);
        } else if (below.isSideSolid(world, belowPos, EnumFacing.UP)
                || below.getBlockFaceShape(world, belowPos, EnumFacing.UP) == BlockFaceShape.SOLID) {
            return 0;
        }

        for (EnumFacing facing : EnumFacing.Plane.HORIZONTAL) {
            IBlockState side = world.getBlockState(pos.offset(facing));
            if (side.getBlock() == BambooContent.scaffolding) {
                distance = Math.min(distance, side.getValue(DISTANCE) + 1);
                if (distance == 1) {
                    break;
                }
            }
        }

        return distance;
    }

    /** A scaffold off the ground with no scaffold below it shows its bottom frame. */
    private boolean isBottom(IBlockAccess world, BlockPos pos, int distance) {
        return distance > 0 && world.getBlockState(pos.down()).getBlock() != this;
    }

    @Override
    public boolean canPlaceBlockAt(World world, BlockPos pos) {
        return super.canPlaceBlockAt(world, pos) && getDistance(world, pos) < MAX_DISTANCE;
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY,
                                            float hitZ, int meta, EntityLivingBase placer, EnumHand hand) {
        int distance = getDistance(world, pos);
        return getDefaultState().withProperty(DISTANCE, distance).withProperty(BOTTOM, isBottom(world, pos, distance));
    }

    @Override
    public void onBlockAdded(World world, BlockPos pos, IBlockState state) {
        if (!world.isRemote) {
            world.scheduleUpdate(pos, this, 1);
        }
    }

    @Override
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block, BlockPos fromPos) {
        if (!world.isRemote) {
            world.scheduleUpdate(pos, this, 1);
        }
    }

    /** 1.14's {@code tick}: re-count the distance, then settle, break or fall. */
    @Override
    public void updateTick(World world, BlockPos pos, IBlockState state, Random rand) {
        int distance = getDistance(world, pos);
        IBlockState updated = state.withProperty(DISTANCE, distance).withProperty(BOTTOM, isBottom(world, pos, distance));

        if (distance == MAX_DISTANCE) {
            if (state.getValue(DISTANCE) == MAX_DISTANCE) {
                world.spawnEntity(new EntityFallingBlock(world, pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, updated));
            } else {
                world.destroyBlock(pos, true);
            }
        } else if (state != updated) {
            world.setBlockState(pos, updated, 3);
        }
    }

    // ---- moving through it ------------------------------------------------------------------

    /**
     * 1.14's {@code getCollisionShape}, per entity: solid on top for anyone standing above it and
     * not sneaking, the bottom frame of a hanging scaffold for anyone not below it, and otherwise
     * nothing.
     */
    @Override
    public void addCollisionBoxToList(IBlockState state, World world, BlockPos pos, AxisAlignedBB entityBox,
                                      List<AxisAlignedBB> collidingBoxes, @Nullable Entity entity,
                                      boolean isActualState) {
        if (entity == null) {
            return;
        }
        double feet = entity.getEntityBoundingBox().minY;

        if (feet > pos.getY() + 1 - EPSILON && !entity.isSneaking()) {
            for (AxisAlignedBB box : STABLE) {
                addCollisionBoxToList(pos, entityBox, collidingBoxes, box);
            }
        } else if (state.getValue(DISTANCE) != 0 && state.getValue(BOTTOM) && feet > pos.getY() - EPSILON) {
            addCollisionBoxToList(pos, entityBox, collidingBoxes, BOTTOM_FRAME);
        }
    }

    /** No box without an entity: placing scaffolding where you stand is allowed, as in 1.14. */
    @Nullable
    @Override
    public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        return NULL_AABB;
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return FULL_BLOCK_AABB;
    }

    /** A ladder to anything not sneaking; see the class notes for why sneaking is not. */
    @Override
    public boolean isLadder(IBlockState state, IBlockAccess world, BlockPos pos, EntityLivingBase entity) {
        return !entity.isSneaking();
    }

    /**
     * Climbing and sneaking down, for an entity whose feet are in this scaffold - the block 1.12
     * and 1.14 both ask about when deciding whether you are on a ladder.
     *
     * <p>This runs inside the entity's move, before gravity: the same point at which 1.14 sets a
     * climbing entity's upward motion, so climbing is 1.14's speed exactly.
     */
    @Override
    public void onEntityCollision(World world, BlockPos pos, IBlockState state, Entity entity) {
        if (!(entity instanceof EntityLivingBase)) {
            return;
        }
        EntityLivingBase living = (EntityLivingBase) entity;
        BlockPos feet = new BlockPos(MathHelper.floor(entity.posX), MathHelper.floor(entity.getEntityBoundingBox().minY),
                MathHelper.floor(entity.posZ));
        if (!feet.equals(pos)) {
            return;
        }

        if (living.isSneaking()) {
            living.fallDistance = 0.0F;
            if (living.motionY < SNEAK_DESCENT) {
                living.motionY = SNEAK_DESCENT;
            }
        } else if (isJumping(living) && living.motionY < CLIMB_SPEED) {
            living.motionY = CLIMB_SPEED;
        }
    }

    // ---- everything else --------------------------------------------------------------------

    @Override
    public boolean isPassable(IBlockAccess world, BlockPos pos) {
        return true;
    }

    @Override
    public boolean isReplaceable(IBlockAccess world, BlockPos pos) {
        return false;
    }

    @Override
    public SoundType getSoundType(IBlockState state, World world, BlockPos pos, @Nullable Entity entity) {
        return BambooContent.scaffoldingSound != null ? BambooContent.scaffoldingSound : SoundType.WOOD;
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
    public boolean isTopSolid(IBlockState state) {
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

    @Override
    @SideOnly(Side.CLIENT)
    public boolean shouldSideBeRendered(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing side) {
        return true;
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, DISTANCE, BOTTOM);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(DISTANCE, meta & 7).withProperty(BOTTOM, (meta & 8) != 0);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(DISTANCE) | (state.getValue(BOTTOM) ? 8 : 0);
    }
}
