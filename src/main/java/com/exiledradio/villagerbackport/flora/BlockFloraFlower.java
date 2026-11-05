package com.exiledradio.villagerbackport.flora;

import net.minecraft.block.BlockBush;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * A one-block-tall flower: the cornflower and the lily of the valley, and the base of the wither rose.
 *
 * <h2>Why not {@code BlockFlower}</h2>
 * 1.12.2's {@code BlockFlower} is two blocks, yellow and red, that carry every flower as a metadata
 * value of one enum - and that enum is a fixed list of nine. There is no slot in it for a tenth
 * flower, so these are plain {@link BlockBush}es that behave as 1.12.2's flowers do: they stand on
 * grass, dirt and farmland, break when the block beneath them goes, are knocked off by water, and
 * sit at a small random offset within their block so a meadow of them does not look gridded.
 *
 * <p>The shape is 1.14's: six pixels wide and ten tall, centered, moved by that same offset.
 *
 * <h2>What else treats them as flowers</h2>
 * Biomes plant them from bone meal through Forge's {@code Biome.addFlower} (see
 * {@link FloraWorldGen}), flower pots take them through {@link BlockFloraPot}, mooshrooms and the
 * stew recipe through {@link StewEffects}, and composters at 1.14's 65%.
 */
public class BlockFloraFlower extends BlockBush {

    /** 1.14's flower shape: x and z 5 to 11, y 0 to 10, in sixteenths. */
    private static final AxisAlignedBB SHAPE =
            new AxisAlignedBB(0.3125D, 0.0D, 0.3125D, 0.6875D, 0.625D, 0.6875D);

    public BlockFloraFlower() {
        setSoundType(SoundType.PLANT);
        setHardness(0.0F);
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return SHAPE.offset(state.getOffset(source, pos));
    }

    /** Flowers sit off-center in their block, as 1.12.2's own do. */
    @Override
    public EnumOffsetType getOffsetType() {
        return EnumOffsetType.XZ;
    }
}
