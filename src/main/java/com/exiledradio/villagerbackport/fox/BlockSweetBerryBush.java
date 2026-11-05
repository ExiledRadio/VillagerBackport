package com.exiledradio.villagerbackport.fox;

import net.minecraft.block.BlockBush;
import net.minecraft.block.IGrowable;
import net.minecraft.block.SoundType;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.pathfinding.PathNodeType;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;

import javax.annotation.Nullable;
import java.util.Random;

/**
 * The sweet berry bush: a four-stage plant that is picked rather than broken, and that pricks
 * anything walking through it.
 *
 * <h2>Growing</h2>
 * Each random tick a bush below its last stage grows one stage with a one-in-five chance, if the
 * block above it has a light level of 9 or more - 1.14's numbers, and Forge's crop-growth events
 * fire around it as for any crop, so growth-tweaking mods see it. Bone meal always adds one stage.
 *
 * <h2>Picking</h2>
 * Using a bush at stage 2 (a few berries) gives one or two berries, and at stage 3 (fully ripe) two or
 * three, and puts it back to stage 1 - the bush stays. Bone meal on an unripe bush is let through to
 * grow it instead of picking. Breaking a ripe bush drops the same berries (plus Fortune), and an
 * unripe one nothing; the bush itself is never dropped, and is replanted from its berries.
 *
 * <h2>Thorns</h2>
 * A living thing inside a bush is slowed almost to a crawl, and, once the bush is past its first
 * stage, takes half a heart each tick it is actually moving. Foxes are the exception: they are never
 * slowed or hurt, and path straight through bushes while every other mob routes around them.
 *
 * <p>1.14 slows by scaling the movement of the tick that entity is in and then dropping its velocity.
 * 1.12.2 has no such hook, so the velocity is dropped after each move instead: the next tick moves
 * only by that tick's own push, which is the same crawl with no momentum to carry through.
 */
public class BlockSweetBerryBush extends BlockBush implements IGrowable {

    public static final int MAX_AGE = 3;
    public static final PropertyInteger AGE = PropertyInteger.create("age", 0, MAX_AGE);

    /** 1.14's shapes: a sprig at stage 0, almost a full block after. */
    private static final AxisAlignedBB SAPLING = new AxisAlignedBB(3 / 16D, 0, 3 / 16D, 13 / 16D, 8 / 16D, 13 / 16D);
    private static final AxisAlignedBB GROWING = new AxisAlignedBB(1 / 16D, 0, 1 / 16D, 15 / 16D, 1, 15 / 16D);

    /**
     * The damage the thorns deal. Its death message is 1.14's ("was poked to death by a sweet berry
     * bush"), under {@code death.attack.sweetBerryBush}.
     */
    public static final DamageSource THORNS = new DamageSource("sweetBerryBush");

    public BlockSweetBerryBush() {
        super(net.minecraft.block.material.Material.PLANTS);
        setDefaultState(blockState.getBaseState().withProperty(AGE, 0));
        setTickRandomly(true);
        setSoundType(new SoundType(1.0F, 1.0F, FoxContent.sound("bushBreak"), SoundType.PLANT.getStepSound(),
                FoxContent.sound("bushPlace"), SoundType.PLANT.getHitSound(), SoundType.PLANT.getFallSound()));
    }

    // ------------------------------------------------------------------------------------- state

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, AGE);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(AGE, Math.min(meta, MAX_AGE));
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(AGE);
    }

    @Override
    @SuppressWarnings("deprecation")
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        int age = state.getValue(AGE);
        return age == 0 ? SAPLING : age < MAX_AGE ? GROWING : FULL_BLOCK_AABB;
    }

    // -------------------------------------------------------------------------------------- growth

    @Override
    public void updateTick(World world, BlockPos pos, IBlockState state, Random rand) {
        super.updateTick(world, pos, state, rand);
        if (!world.isAreaLoaded(pos, 1)) {
            return;
        }
        int age = state.getValue(AGE);
        if (age < MAX_AGE && world.getLightFromNeighbors(pos.up()) >= 9
                && ForgeHooks.onCropsGrowPre(world, pos, state, rand.nextInt(5) == 0)) {
            world.setBlockState(pos, state.withProperty(AGE, age + 1), 2);
            ForgeHooks.onCropsGrowPost(world, pos, state, world.getBlockState(pos));
        }
    }

    @Override
    public boolean canGrow(World world, BlockPos pos, IBlockState state, boolean isClient) {
        return state.getValue(AGE) < MAX_AGE;
    }

    @Override
    public boolean canUseBonemeal(World world, Random rand, BlockPos pos, IBlockState state) {
        return true;
    }

    @Override
    public void grow(World world, Random rand, BlockPos pos, IBlockState state) {
        world.setBlockState(pos, state.withProperty(AGE, Math.min(MAX_AGE, state.getValue(AGE) + 1)), 2);
    }

    // ------------------------------------------------------------------------------------- picking

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand,
                                    EnumFacing facing, float hitX, float hitY, float hitZ) {
        int age = state.getValue(AGE);
        boolean ripe = age == MAX_AGE;
        ItemStack held = player.getHeldItem(hand);

        // Bone meal (dye metadata 15) on an unripe bush grows it rather than picking it.
        if (!ripe && held.getItem() == Items.DYE && held.getMetadata() == 15) {
            return false;
        }
        if (age <= 1) {
            return false;
        }
        if (!world.isRemote) {
            int count = 1 + world.rand.nextInt(2) + (ripe ? 1 : 0);
            spawnAsEntity(world, pos, new ItemStack(FoxContent.sweetBerries, count));
            world.playSound(null, pos, FoxContent.sound("pick"), SoundCategory.BLOCKS, 1.0F,
                    0.8F + world.rand.nextFloat() * 0.4F);
            world.setBlockState(pos, state.withProperty(AGE, 1), 2);
        }
        return true;
    }

    /**
     * Picks a bush the way a fox does: some berries straight to {@code mouth} if it is empty, the rest
     * on the ground.
     *
     * @return what is left for the mouth to take, possibly empty
     */
    public static ItemStack pickForFox(World world, BlockPos pos, IBlockState state, boolean mouthEmpty) {
        int age = state.getValue(AGE);
        if (age < 2) {
            return ItemStack.EMPTY;
        }
        int count = 1 + world.rand.nextInt(2) + (age == MAX_AGE ? 1 : 0);
        ItemStack mouth = ItemStack.EMPTY;
        if (mouthEmpty) {
            mouth = new ItemStack(FoxContent.sweetBerries);
            count--;
        }
        if (count > 0) {
            spawnAsEntity(world, pos, new ItemStack(FoxContent.sweetBerries, count));
        }
        world.playSound(null, pos, FoxContent.sound("pick"), SoundCategory.BLOCKS, 1.0F, 1.0F);
        world.setBlockState(pos, state.withProperty(AGE, 1), 2);
        return mouth;
    }

    // -------------------------------------------------------------------------------------- drops

    /** 1.14's loot table: 2-3 berries when ripe, 1-2 at stage 2, Fortune adding 0 to its level. */
    @Override
    public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos, IBlockState state, int fortune) {
        int age = state.getValue(AGE);
        if (age < 2) {
            return;
        }
        Random rand = world instanceof World ? ((World) world).rand : RANDOM;
        int count = (age == MAX_AGE ? 2 : 1) + rand.nextInt(2);
        if (fortune > 0) {
            count += rand.nextInt(fortune + 1);
        }
        drops.add(new ItemStack(FoxContent.sweetBerries, count));
    }

    @Override
    public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world, BlockPos pos, EntityPlayer player) {
        return new ItemStack(FoxContent.sweetBerries);
    }

    // ------------------------------------------------------------------------------------- thorns

    @Override
    public void onEntityCollision(World world, BlockPos pos, IBlockState state, Entity entity) {
        if (!(entity instanceof EntityLivingBase) || entity instanceof EntityFox) {
            return;
        }
        // Where 1.14 compares with xOld, 1.12.2's equivalent is prevPos: lastTickPos is already
        // overwritten with the new position by the time a mob's move reaches its block collisions.
        boolean moved = entity.prevPosX != entity.posX || entity.prevPosZ != entity.posZ;
        double dx = Math.abs(entity.posX - entity.prevPosX);
        double dz = Math.abs(entity.posZ - entity.prevPosZ);

        entity.motionX = 0.0D;
        entity.motionY = 0.0D;
        entity.motionZ = 0.0D;
        entity.fallDistance = 0.0F;

        if (!world.isRemote && state.getValue(AGE) > 0 && moved && (dx >= 0.003D || dz >= 0.003D)) {
            FoxEvents.hurtByThorns(entity);
        }
    }

    /**
     * Every mob but a fox treats a bush as somewhere that hurts (impassable) and the ground next to one
     * as risky; a fox treats it as open ground.
     */
    @Nullable
    @Override
    public PathNodeType getAiPathNodeType(IBlockState state, IBlockAccess world, BlockPos pos, @Nullable EntityLiving entity) {
        return entity instanceof EntityFox ? null : PathNodeType.DAMAGE_OTHER;
    }

    // ---------------------------------------------------------------------------------- placement

    /** The bush grows on grass, dirt, podzol, coarse dirt and farmland, as {@link BlockBush} allows. */
    @Override
    public boolean canPlaceBlockAt(World world, BlockPos pos) {
        IBlockState here = world.getBlockState(pos);
        return here.getBlock().isReplaceable(world, pos) && canSustainBush(world.getBlockState(pos.down()));
    }

    /** @return true if a bush can stand on this block: grass, any dirt, or farmland */
    public boolean canGrowOn(IBlockState ground) {
        return canSustainBush(ground);
    }

    @Override
    public EnumOffsetType getOffsetType() {
        return EnumOffsetType.NONE;
    }
}
