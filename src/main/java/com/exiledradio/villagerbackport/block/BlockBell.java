package com.exiledradio.villagerbackport.block;


import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;


/**
 * A village bell: ring it and the raiders in earshot light up, and villagers run home.
 *
 * <h2>What it does</h2>
 * 1.14's bell: nearby <em>raiders</em> glow, so a village under attack can see where the attackers
 * are, and villagers within 32 blocks go home - see {@link TileEntityBell} and
 * {@code raid.BellHearing}. Raiders are 1.14's: 1.12.2's own illagers and witches, and any mod's
 * pillagers and ravagers. With no raider in reach it only rings, as in 1.14.
 *
 * <h2>1.14's timing, kept</h2>
 * The ring is immediate; the resonance and the glow follow a moment later. That gap is what makes a
 * bell feel like a bell rather than a button, so it is preserved with a scheduled block update -
 * which also means the block needs no tile entity of its own.
 *
 * <h2>How it hangs</h2>
 * 1.14's four mountings, with its models: on a floor stand, hanging from a ceiling, from one wall,
 * or between two. Placed like 1.14's - on top of a block it stands, under one it hangs, against a
 * wall it hangs from that wall, or between two if there is one opposite. 1.14's villages hang about
 * a third of their bells from beams. Only the frame differs; the bell itself is drawn the same way
 * whatever holds it, by {@code TileEntityBellRenderer}. Bells placed before this existed are floor
 * bells, which is what their saved state reads as.
 */
public class BlockBell extends BlockWorkstationShaped {

    /** 1.14's {@code BellAttachment}, in its order: the saved metadata's top two bits. */
    public enum Attachment implements net.minecraft.util.IStringSerializable {
        FLOOR("floor"), CEILING("ceiling"), SINGLE_WALL("single_wall"), DOUBLE_WALL("double_wall");

        private final String name;

        Attachment(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return this.name;
        }
    }

    public static final net.minecraft.block.properties.PropertyEnum<Attachment> ATTACHMENT =
            net.minecraft.block.properties.PropertyEnum.create("attachment", Attachment.class);

    /** 1.14's outlines, as near as one box each gets: the stand, and the bell with its bar. */
    private static final AxisAlignedBB FLOOR_NS = new AxisAlignedBB(0, 0, 4 / 16D, 1, 1, 12 / 16D);
    private static final AxisAlignedBB FLOOR_EW = new AxisAlignedBB(4 / 16D, 0, 0, 12 / 16D, 1, 1);
    private static final AxisAlignedBB HANGING = new AxisAlignedBB(4 / 16D, 4 / 16D, 4 / 16D, 12 / 16D, 1, 12 / 16D);
    private static final AxisAlignedBB WALL_NS = new AxisAlignedBB(4 / 16D, 4 / 16D, 0, 12 / 16D, 1, 1);
    private static final AxisAlignedBB WALL_EW = new AxisAlignedBB(0, 4 / 16D, 4 / 16D, 1, 1, 12 / 16D);

    public BlockBell(Material material, SoundType sound, float hardness,
                     AxisAlignedBB shape, boolean cutout) {
        super(material, sound, hardness, shape, cutout);
    }

    @Override
    protected net.minecraft.block.state.BlockStateContainer createBlockState() {
        return new net.minecraft.block.state.BlockStateContainer(this, FACING, ATTACHMENT);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3))
                .withProperty(ATTACHMENT, Attachment.values()[(meta >> 2) & 3]);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(FACING).getHorizontalIndex() | state.getValue(ATTACHMENT).ordinal() << 2;
    }

    /** 1.14's placement: by the face clicked. */
    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing clicked, float hitX, float hitY,
                                            float hitZ, int meta, net.minecraft.entity.EntityLivingBase placer) {
        IBlockState floor = super.getStateForPlacement(world, pos, clicked, hitX, hitY, hitZ, meta, placer)
                .withProperty(ATTACHMENT, Attachment.FLOOR);
        if (clicked == EnumFacing.UP) {
            return floor;
        }
        if (clicked == EnumFacing.DOWN) {
            return floor.withProperty(ATTACHMENT, Attachment.CEILING);
        }
        // Against a wall: hung from it, facing away, or between two if there is one opposite.
        EnumFacing facing = clicked.getOpposite();
        BlockPos across = pos.offset(clicked);
        boolean opposite = world.getBlockState(across).getBlockFaceShape(world, across, clicked.getOpposite())
                == net.minecraft.block.state.BlockFaceShape.SOLID;
        return getDefaultState().withProperty(FACING, facing)
                .withProperty(ATTACHMENT, opposite ? Attachment.DOUBLE_WALL : Attachment.SINGLE_WALL);
    }

    @Override
    @SuppressWarnings("deprecation")
    public AxisAlignedBB getBoundingBox(IBlockState state, net.minecraft.world.IBlockAccess source, BlockPos pos) {
        boolean northSouth = state.getValue(FACING).getAxis() == EnumFacing.Axis.Z;
        switch (state.getValue(ATTACHMENT)) {
            case CEILING:
                return HANGING;
            case SINGLE_WALL:
            case DOUBLE_WALL:
                return northSouth ? WALL_NS : WALL_EW;
            default:
                return northSouth ? FLOOR_NS : FLOOR_EW;
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public IBlockState withRotation(IBlockState state, net.minecraft.util.Rotation rotation) {
        return state.withProperty(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Override
    public net.minecraft.tileentity.TileEntity createTileEntity(World world, IBlockState state) {
        return new TileEntityBell();
    }

    /**
     * Passes block events on to the tile entity, which is what makes the swing reach clients.
     *
     * <p>Vanilla routes them this way for chest lids and note blocks too: the server posts an event,
     * every client watching the block receives it, and each starts the animation locally.
     */
    @Override
    public boolean eventReceived(IBlockState state, World world, BlockPos pos, int id, int param) {
        super.eventReceived(state, world, pos, id, param);

        net.minecraft.tileentity.TileEntity tile = world.getTileEntity(pos);
        return tile != null && tile.receiveClientEvent(id, param);
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player,
                                    EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (!world.isRemote) {
            ring(world, pos, facing);
        }
        return true;
    }

    /**
     * A villager rings the bell, as 1.14's {@code RingBellTask} has them do when a raid is coming:
     * the same as a player striking it, from the north.
     */
    public void ringByVillager(World world, BlockPos pos) {
        if (!world.isRemote) {
            ring(world, pos, EnumFacing.NORTH);
        }
    }

    /**
     * @param struck the side the bell was hit on, which decides which way it swings
     */
    private void ring(World world, BlockPos pos, EnumFacing struck) {
        world.playSound(null, pos, ModSounds.bellUse, SoundCategory.BLOCKS, 2.0F, 1.0F);

        // A vertical hit has no axis to swing along, so treat it as a strike from the north.
        EnumFacing swingAxis = struck.getAxis() == EnumFacing.Axis.Y ? EnumFacing.NORTH : struck;
        world.addBlockEvent(pos, this, TileEntityBell.EVENT_RING, swingAxis.getIndex());

        com.exiledradio.villagerbackport.raid.BellHearing.ring(world, pos);
        // Raiders in earshot are given away by the tile, on 1.14's timing - see TileEntityBell.
    }
}
