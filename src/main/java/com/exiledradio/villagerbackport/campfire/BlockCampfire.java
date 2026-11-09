package com.exiledradio.villagerbackport.campfire;

import com.exiledradio.backportcore.waterlog.Waterloggable;
import com.exiledradio.backportcore.waterlog.Waterlogging;

import net.minecraft.block.Block;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemFireball;
import net.minecraft.item.ItemFlintAndSteel;
import net.minecraft.item.ItemStack;
import net.minecraft.pathfinding.PathNodeType;
import net.minecraft.stats.StatList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.Mirror;
import net.minecraft.util.NonNullList;
import net.minecraft.util.Rotation;
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
 * 1.14's campfire.
 *
 * <h2>State</h2>
 * Three properties, all kept in metadata: {@code facing} (the way the logs lie, and which slot is
 * which), {@code lit}, and {@code signal_fire} - true while a hay bale sits directly underneath, which
 * sends the smoke up three times as high. 1.14's fourth, {@code waterlogged}, is Fluidlogged API's
 * water through Backport Core ({@link Waterloggable}), not a property: a campfire holds water as in
 * 1.14, and water arriving puts a lit one out with the burst of smoke. One placed into water is placed
 * out, and none can be lit while it holds water. Without Fluidlogged API it holds no water, as before
 * 1.13: it is placed lit even where water was, and a water bucket puts a lit one out.
 *
 * <h2>What it does, from minecraft.wiki (current Java)</h2>
 * <ul>
 * <li><b>Light 15</b> while lit, none when out.</li>
 * <li><b>Hurts what stands in it</b>: 1 point of fire damage, which the half-second hurt cooldown turns
 * into one hit every ten ticks. Only living things, only while lit, and not those immune to fire, with
 * Fire Resistance (the damage is fire damage, so 1.12.2 already skips it) or wearing Frost Walker boots.
 * Sneaking does <i>not</i> protect anyone: 1.14's snapshots briefly had that, and 19w04a took it out
 * before release. It never sets anything alight, so a mob killed in one drops raw meat.</li>
 * <li><b>Cooks</b> up to four foods at once, no fuel - see {@link TileEntityCampfire}. Food goes on lit
 * or unlit campfires alike, as in current Java (1.14.4 itself only took it while lit).</li>
 * <li><b>Lit</b> by flint and steel, a fire charge or a burning arrow, unless it holds water. Placed lit,
 * except into water.</li>
 * <li><b>Put out</b> by a shovel, water reaching it (a water bucket waterlogs it) or a splash water
 * bottle - never from below, and never by rain. Putting it out keeps what is cooking; it carries on
 * when relit.</li>
 * <li><b>Drops</b> two charcoal, or itself with Silk Touch, plus whatever was cooking.</li>
 * <li>Mobs path around a lit one rather than through it.</li>
 * </ul>
 *
 * <h2>Bees</h2>
 * A lit campfire under a beehive lets players take honey without angering the bees (1.15). This mod
 * has no bees, so nothing reads it yet: the Buzzy Bees backport should ask {@link #isSmokeSource} for
 * each block in the five below a hive, as 1.15's {@code CampfireBlock.isSmokeyPos} does.
 */
public class BlockCampfire extends Block implements ITileEntityProvider, Waterloggable {

    public static final PropertyDirection FACING = BlockHorizontal.FACING;
    public static final PropertyBool LIT = PropertyBool.create("lit");
    public static final PropertyBool SIGNAL_FIRE = PropertyBool.create("signal_fire");

    /** 1.14's shape: a slab seven sixteenths high. */
    private static final AxisAlignedBB SHAPE = new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, 7.0D / 16.0D, 1.0D);

    /** The block event 1.12.2 plays as a fire going out: a hiss, as 1.14's shovel and potion use. */
    static final int EXTINGUISH_EVENT = 1009;

    public BlockCampfire() {
        super(Material.WOOD, MapColor.OBSIDIAN);
        // 1.14: hardness 2, wood sounds, quickest with an axe but breakable by hand.
        setHardness(2.0F);
        setResistance(2.0F);
        setSoundType(SoundType.WOOD);
        setHarvestLevel("axe", 0);
        setDefaultState(this.blockState.getBaseState()
                .withProperty(FACING, EnumFacing.NORTH)
                .withProperty(LIT, Boolean.TRUE)
                .withProperty(SIGNAL_FIRE, Boolean.FALSE));
    }

    /**
     * @return true for a lit campfire - the hook for bees calming over smoke (1.15). See the class
     * notes; nothing in this mod calls it.
     */
    public static boolean isSmokeSource(IBlockState state) {
        return state.getBlock() instanceof BlockCampfire && state.getValue(LIT).booleanValue();
    }

    /** @return true if this state is one of this mod's campfires, lit */
    public static boolean isLit(IBlockState state) {
        return isSmokeSource(state);
    }

    // ---------------------------------------------------------------------------------------------
    // State
    // ---------------------------------------------------------------------------------------------

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING, LIT, SIGNAL_FIRE);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState()
                .withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3))
                .withProperty(LIT, Boolean.valueOf((meta & 4) != 0))
                .withProperty(SIGNAL_FIRE, Boolean.valueOf((meta & 8) != 0));
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        int meta = state.getValue(FACING).getHorizontalIndex();
        if (state.getValue(LIT).booleanValue()) {
            meta |= 4;
        }
        if (state.getValue(SIGNAL_FIRE).booleanValue()) {
            meta |= 8;
        }
        return meta;
    }

    /**
     * Placed lit, facing the way the player looks, a signal fire if there is hay underneath. Placed
     * into water it keeps the water and is placed out, as 1.14's {@code getStateForPlacement}. Without
     * Fluidlogged API nothing holds water, so it replaces the water and is placed lit, as before 1.13.
     */
    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY,
                                            float hitZ, int meta, EntityLivingBase placer) {
        return getDefaultState()
                .withProperty(FACING, placer.getHorizontalFacing())
                .withProperty(LIT, Boolean.valueOf(!(Waterlogging.isAvailable() && Waterlogging.hasWaterSource(world, pos))))
                .withProperty(SIGNAL_FIRE, Boolean.valueOf(isHay(world.getBlockState(pos.down()))));
    }

    /** 1.14's {@code updateShape} for the block below: hay placed or taken away under it. */
    @Override
    @SuppressWarnings("deprecation")
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block, BlockPos from) {
        if (!from.equals(pos.down())) {
            return;
        }
        boolean signal = isHay(world.getBlockState(from));
        if (signal != state.getValue(SIGNAL_FIRE).booleanValue()) {
            world.setBlockState(pos, state.withProperty(SIGNAL_FIRE, Boolean.valueOf(signal)), 3);
        }
    }

    static boolean isHay(IBlockState below) {
        return below.getBlock() == Blocks.HAY_BLOCK;
    }

    @Override
    @SuppressWarnings("deprecation")
    public IBlockState withRotation(IBlockState state, Rotation rotation) {
        return state.withProperty(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    @SuppressWarnings("deprecation")
    public IBlockState withMirror(IBlockState state, Mirror mirror) {
        return state.withRotation(mirror.toRotation(state.getValue(FACING)));
    }

    // ---------------------------------------------------------------------------------------------
    // Using it
    // ---------------------------------------------------------------------------------------------

    /**
     * Right-click: light it, put it out, or put food on it, depending on what is in the hand.
     *
     * <p>Lighting and putting out are handled here rather than by the items themselves because in
     * 1.12.2 flint and steel would set fire to the block in front. A lit campfire, or one holding
     * water, hands flint and steel back to the item, which lights the space next to it as 1.14's
     * does. With Fluidlogged API a water bucket is the bucket's own: it waterlogs the campfire, and the
     * water puts it out ({@link #onWaterChanged}). Without it the campfire can't hold water, so a water
     * bucket puts a lit one out here and is emptied, as 1.14 did before waterlogging (otherwise the
     * bucket would pour water beside it).
     */
    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand,
                                    EnumFacing side, float hitX, float hitY, float hitZ) {
        ItemStack held = player.getHeldItem(hand);
        boolean lit = state.getValue(LIT).booleanValue();
        boolean canLight = !lit && !Waterlogging.hasWater(world, pos);
        Item item = held.getItem();

        if (canLight && item instanceof ItemFlintAndSteel) {
            if (!world.isRemote) {
                world.playSound(null, pos, SoundEvents.ITEM_FLINTANDSTEEL_USE, SoundCategory.BLOCKS, 1.0F,
                        world.rand.nextFloat() * 0.4F + 0.8F);
                setLit(world, pos, state, true);
                held.damageItem(1, player);
            }
            return true;
        }
        if (canLight && item instanceof ItemFireball) {
            if (!world.isRemote) {
                world.playSound(null, pos, SoundEvents.ITEM_FIRECHARGE_USE, SoundCategory.BLOCKS, 1.0F,
                        (world.rand.nextFloat() - world.rand.nextFloat()) * 0.2F + 1.0F);
                setLit(world, pos, state, true);
                if (!player.capabilities.isCreativeMode) {
                    held.shrink(1);
                }
            }
            return true;
        }
        if (lit && side != EnumFacing.DOWN && item.getToolClasses(held).contains("shovel")) {
            if (!world.isRemote) {
                extinguish(world, pos, state, false);
                held.damageItem(1, player);
            }
            return true;
        }
        if (lit && item == Items.WATER_BUCKET && !Waterlogging.isAvailable()) {
            if (!world.isRemote) {
                extinguish(world, pos, state, true);
                if (!player.capabilities.isCreativeMode) {
                    player.setHeldItem(hand, new ItemStack(Items.BUCKET));
                }
                player.addStat(StatList.getObjectUseStats(Items.WATER_BUCKET));
            }
            return true;
        }

        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof TileEntityCampfire && CampfireRecipes.cooks(held)) {
            if (!world.isRemote && ((TileEntityCampfire) tile).placeFood(
                    player.capabilities.isCreativeMode ? held.copy() : held,
                    com.exiledradio.villagerbackport.ModConfig.campfire.cookTime)) {
                player.addStat(StatList.getObjectUseStats(item));
            }
            return true;
        }
        return false;
    }

    /** Lights or puts out, without the extinguishing effects. */
    static void setLit(World world, BlockPos pos, IBlockState state, boolean lit) {
        world.setBlockState(pos, state.withProperty(LIT, Boolean.valueOf(lit)), 11);
    }

    /**
     * Puts a lit campfire out: the hiss, the smoke burst, and the food kept for when it is relit.
     *
     * @param burst 1.14's waterlogging puff - twenty smoke plumes at once. The shovel has none.
     */
    public static void extinguish(World world, BlockPos pos, IBlockState state, boolean burst) {
        if (!isLit(state) || world.isRemote) {
            return;
        }
        world.playEvent(null, EXTINGUISH_EVENT, pos, 0);
        if (burst) {
            PacketCampfireDowse.send(world, pos, state.getValue(SIGNAL_FIRE).booleanValue());
        }
        setLit(world, pos, state, false);
        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof TileEntityCampfire) {
            ((TileEntityCampfire) tile).dowse();
        }
    }

    /** Lights an unlit campfire from outside: a burning arrow. Not one that holds water. */
    public static void ignite(World world, BlockPos pos, IBlockState state) {
        if (state.getBlock() instanceof BlockCampfire && !state.getValue(LIT).booleanValue() && !world.isRemote
                && !Waterlogging.hasWater(world, pos)) {
            setLit(world, pos, state, true);
        }
    }

    /**
     * Water arriving - a bucket, flowing water, a command - puts a lit campfire out with 1.14's
     * twenty-plume burst and the hiss, as its {@code receiveFluid} does. Draining it changes nothing:
     * it stays out until lit again.
     */
    @Override
    public void onWaterChanged(World world, BlockPos pos, IBlockState state, boolean hasWater) {
        if (hasWater && !world.isRemote) {
            extinguish(world, pos, world.getBlockState(pos), true);
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Contact damage
    // ---------------------------------------------------------------------------------------------

    @Override
    public void onEntityCollision(World world, BlockPos pos, IBlockState state, Entity entity) {
        if (!entity.isImmuneToFire() && state.getValue(LIT).booleanValue() && entity instanceof EntityLivingBase
                && !EnchantmentHelper.hasFrostWalkerEnchantment((EntityLivingBase) entity)) {
            entity.attackEntityFrom(DamageSource.IN_FIRE, 1.0F);
        }
    }

    @Nullable
    @Override
    public PathNodeType getAiPathNodeType(IBlockState state, IBlockAccess world, BlockPos pos,
                                          @Nullable EntityLiving entity) {
        return state.getValue(LIT).booleanValue() ? PathNodeType.DAMAGE_FIRE : null;
    }

    // ---------------------------------------------------------------------------------------------
    // Light, drops, tile
    // ---------------------------------------------------------------------------------------------

    @Override
    @SuppressWarnings("deprecation")
    public int getLightValue(IBlockState state) {
        return state.getValue(LIT).booleanValue() ? 15 : 0;
    }

    @Override
    public int getLightValue(IBlockState state, IBlockAccess world, BlockPos pos) {
        return getLightValue(state);
    }

    /** Two charcoal; Silk Touch drops the campfire itself, through {@link #canSilkHarvest}. */
    @Override
    public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos, IBlockState state,
                         int fortune) {
        drops.add(new ItemStack(Items.COAL, 2, 1));
    }

    @Override
    public boolean canSilkHarvest(World world, BlockPos pos, IBlockState state, EntityPlayer player) {
        return true;
    }

    @Override
    protected ItemStack getSilkTouchDrop(IBlockState state) {
        return new ItemStack(this);
    }

    @Override
    public ItemStack getPickBlock(IBlockState state, net.minecraft.util.math.RayTraceResult target, World world,
                                  BlockPos pos, EntityPlayer player) {
        return new ItemStack(this);
    }

    /** Whatever was cooking falls out when the campfire goes. */
    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof TileEntityCampfire) {
            for (ItemStack stack : ((TileEntityCampfire) tile).items()) {
                if (!stack.isEmpty()) {
                    InventoryHelper.spawnItemStack(world, pos.getX(), pos.getY(), pos.getZ(), stack);
                }
            }
            ((TileEntityCampfire) tile).items().clear();
        }
        super.breakBlock(world, pos, state);
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileEntityCampfire();
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    // ---------------------------------------------------------------------------------------------
    // Shape and rendering
    // ---------------------------------------------------------------------------------------------

    @Override
    @SuppressWarnings("deprecation")
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return SHAPE;
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
        return face == EnumFacing.DOWN ? BlockFaceShape.SOLID : BlockFaceShape.UNDEFINED;
    }

    @Override
    @SuppressWarnings("deprecation")
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.MODEL;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT;
    }

    /**
     * 1.14's {@code animateTick}: a crackle one tick in ten, and a spark of lava one in five. The
     * smoke itself comes from the tile entity's tick, as in 1.14.
     */
    @Override
    @SideOnly(Side.CLIENT)
    public void randomDisplayTick(IBlockState state, World world, BlockPos pos, Random rand) {
        if (!state.getValue(LIT).booleanValue()) {
            return;
        }
        if (rand.nextInt(10) == 0 && CampfireContent.crackle != null) {
            world.playSound(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, CampfireContent.crackle,
                    SoundCategory.BLOCKS, 0.5F + rand.nextFloat(), rand.nextFloat() * 0.7F + 0.6F, false);
        }
        if (rand.nextInt(5) == 0) {
            world.spawnParticle(net.minecraft.util.EnumParticleTypes.LAVA,
                    pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                    rand.nextFloat() / 2.0F, 5.0E-5D, rand.nextFloat() / 2.0F);
        }
    }
}
