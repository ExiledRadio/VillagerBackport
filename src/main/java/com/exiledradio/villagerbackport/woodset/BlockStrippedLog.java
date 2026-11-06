package com.exiledradio.villagerbackport.woodset;

import net.minecraft.block.BlockLog;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * A stripped log or stripped wood, 1.14's {@code stripped_<wood>_log} and {@code stripped_<wood>_wood}.
 *
 * <h2>A log, as far as anything else can tell</h2>
 * Both extend vanilla's {@link BlockLog}, so they are wood to every check that asks - leaves next to
 * one do not decay ({@code canSustainLeaves}), tree-felling and lumber mods that look for
 * {@code instanceof BlockLog} or {@code isWood} treat them as logs, and placement, rotation in
 * structures and leaf decay on breaking all come from the same code as an oak log's. 1.14 counts
 * stripped logs as logs too: they are in its {@code logs} tag.
 *
 * <h2>One property, the same as a log's</h2>
 * {@link BlockLog#LOG_AXIS}, named {@code axis} and spelled {@code x}, {@code y}, {@code z} like 1.14's,
 * so a structure file's {@code axis=x} carries straight across. Its fourth value, {@code none}, is how
 * 1.12.2 draws bark on all six faces; here it draws a stripped log as stripped wood. Nothing in survival
 * reaches it (placing sets the axis from the face clicked, and stripping vanilla's bark gives the
 * stripped wood block); it is only there because the property has it.
 *
 * <h2>Stripped wood has an axis too</h2>
 * As in 1.14 and Future MC. All six faces show the same texture so it changes nothing you can see,
 * but a village's {@code stripped_spruce_wood[axis=x]} keeps its axis, and Future MC's twin takes it
 * by name.
 *
 * <h2>Not bark</h2>
 * 1.14's unstripped {@code oak_wood} is not added: 1.12.2 already has it as a vanilla log with
 * {@code axis=none}, which is what this mod's structures place for it.
 */
public class BlockStrippedLog extends BlockLog {

    public final Wood wood;

    /** True for {@code stripped_<wood>_wood}, false for {@code stripped_<wood>_log}. */
    public final boolean bark;

    public BlockStrippedLog(Wood wood, boolean bark) {
        this.wood = wood;
        this.bark = bark;
        this.setDefaultState(this.blockState.getBaseState().withProperty(LOG_AXIS, EnumAxis.Y));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, new IProperty[] {LOG_AXIS});
    }

    /** Vanilla logs' layout: axis in bits 2-3. Bits 0-1 are the variant there and unused here. */
    @Override
    @SuppressWarnings("deprecation")
    public IBlockState getStateFromMeta(int meta) {
        EnumAxis axis;
        switch (meta & 12) {
            case 4:
                axis = EnumAxis.X;
                break;
            case 8:
                axis = EnumAxis.Z;
                break;
            case 12:
                axis = EnumAxis.NONE;
                break;
            default:
                axis = EnumAxis.Y;
                break;
        }
        return this.getDefaultState().withProperty(LOG_AXIS, axis);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        switch (state.getValue(LOG_AXIS)) {
            case X:
                return 4;
            case Z:
                return 8;
            case NONE:
                return 12;
            default:
                return 0;
        }
    }

    /** 1.14 colors a stripped log on a map as its planks, on every face. */
    @Override
    @SuppressWarnings("deprecation")
    public MapColor getMapColor(IBlockState state, IBlockAccess world, BlockPos pos) {
        return this.wood.planks.getMapColor();
    }
}
