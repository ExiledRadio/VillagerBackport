package com.exiledradio.villagerbackport.block;

import com.exiledradio.villagerbackport.GuiHandler;
import com.exiledradio.villagerbackport.VillagerBackport;

import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A grindstone: takes the enchantments off an item and hands back some experience.
 *
 * <p>The block does almost nothing itself - it opens a screen, and the container does the work. It
 * has no tile entity because it holds nothing between uses: whatever is in it when the screen closes
 * goes back to the player, exactly as a crafting table or anvil behaves.
 *
 * <h2>Floor, wall and ceiling</h2>
 * As 1.14's {@code GrindstoneBlock}, a {@code HorizontalFaceBlock}: it stands on the floor, hangs
 * from the ceiling or sticks out of a wall, by the way you are looking as you place it - mostly down
 * for the floor, mostly up for the ceiling, otherwise on the wall in front of you. Like 1.14's it
 * needs nothing to hold it up.
 *
 * <p>Metadata keeps the facing in its low two bits, as it was before the grindstone had a face, so
 * every grindstone already in a world is a floor one, as it was.
 */
public class BlockGrindstone extends BlockWorkstationShaped {

    /** 1.14's {@code AttachFace}. */
    public enum Face implements IStringSerializable {
        FLOOR("floor"),
        WALL("wall"),
        CEILING("ceiling");

        private final String name;

        Face(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return this.name;
        }
    }

    public static final PropertyEnum<Face> FACE = PropertyEnum.create("face", Face.class);

    // 1.14's shape with the wheel along the facing axis, turned onto the wall it hangs from.
    private static final AxisAlignedBB WALL_NORTH_SOUTH = new AxisAlignedBB(2 / 16.0, 2 / 16.0, 0, 14 / 16.0, 14 / 16.0, 1);
    private static final AxisAlignedBB WALL_EAST_WEST = new AxisAlignedBB(0, 2 / 16.0, 2 / 16.0, 1, 14 / 16.0, 14 / 16.0);

    public BlockGrindstone(Material material, SoundType sound, float hardness,
                           AxisAlignedBB shape, boolean cutout) {
        super(material, sound, hardness, shape, cutout);
        setDefaultState(this.blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH).withProperty(FACE, Face.FLOOR));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING, FACE);
    }

    /** 1.14's nearest looking direction: whichever axis the player's look leans along most. */
    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
                                            float hitX, float hitY, float hitZ, int meta,
                                            EntityLivingBase placer) {
        Vec3d look = placer.getLook(1.0F);
        double y = Math.abs(look.y);
        if (y > Math.abs(look.x) && y > Math.abs(look.z)) {
            // Floor ones keep facing the player, as they always have here; the wheel looks the same
            // either way round.
            return getDefaultState().withProperty(FACE, look.y > 0 ? Face.CEILING : Face.FLOOR)
                    .withProperty(FACING, placer.getHorizontalFacing().getOpposite());
        }
        return getDefaultState().withProperty(FACE, Face.WALL)
                .withProperty(FACING, placer.getHorizontalFacing().getOpposite());
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        int face = (meta >> 2) & 3;
        return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3))
                .withProperty(FACE, face < Face.values().length ? Face.values()[face] : Face.FLOOR);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(FACING).getHorizontalIndex() | state.getValue(FACE).ordinal() << 2;
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        if (state.getValue(FACE) == Face.WALL) {
            return state.getValue(FACING).getAxis() == EnumFacing.Axis.Z ? WALL_NORTH_SOUTH : WALL_EAST_WEST;
        }
        return super.getBoundingBox(state, source, pos);
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player,
                                    EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (!world.isRemote) {
            player.openGui(VillagerBackport.instance, GuiHandler.GRINDSTONE, world,
                    pos.getX(), pos.getY(), pos.getZ());
        }
        return true;
    }
}
