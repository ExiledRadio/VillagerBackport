package com.exiledradio.villagerbackport.bamboo;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

/**
 * Scaffolding as an item: 1.14's {@code ScaffoldingBlockItem}.
 *
 * <p>Using it on a scaffold does not put the new one against the face you clicked. It extends the
 * structure instead:
 * <ul>
 * <li>clicking the <b>top</b> of a scaffold extends it outward, in the direction you face, past
 * any scaffolds already there - up to seven blocks out;</li>
 * <li>clicking a <b>side or the bottom</b> builds the column up to its top, however tall it is;</li>
 * <li><b>sneaking</b>, it goes the way the clicked face points, through any scaffolds in the way.</li>
 * </ul>
 * Anywhere else it places like any block, as long as the spot is within reach of support.
 */
public class ItemScaffolding extends net.minecraft.item.ItemBlock {

    /** How many horizontal steps the extension may search. */
    private static final int REACH = 7;

    public ItemScaffolding(Block block) {
        super(block);
    }

    /**
     * The client asks this before it even sends the click to the server. Against a scaffold the
     * answer depends on where the extension ends up, which {@link #onItemUse} works out, so the
     * click is always let through.
     */
    @Override
    public boolean canPlaceBlockOnSide(World world, BlockPos pos, EnumFacing side, EntityPlayer player,
                                       net.minecraft.item.ItemStack stack) {
        return world.getBlockState(pos).getBlock() == this.block
                || super.canPlaceBlockOnSide(world, pos, side, player, stack);
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing,
                                      float hitX, float hitY, float hitZ) {
        IBlockState clicked = world.getBlockState(pos);
        if (clicked.getBlock() != this.block) {
            return super.onItemUse(player, world, pos, hand, facing, hitX, hitY, hitZ);
        }

        EnumFacing direction;
        if (player.isSneaking()) {
            direction = facing;
        } else {
            direction = facing == EnumFacing.UP ? player.getHorizontalFacing() : EnumFacing.UP;
        }

        BlockPos target = pos.offset(direction);
        int steps = 0;
        while (steps < REACH) {
            if (world.isOutsideBuildHeight(target)) {
                if (!world.isRemote && target.getY() >= world.getHeight()) {
                    TextComponentTranslation message = new TextComponentTranslation("build.tooHigh", world.getHeight());
                    message.getStyle().setColor(TextFormatting.RED);
                    player.sendStatusMessage(message, true);
                }
                break;
            }
            IBlockState state = world.getBlockState(target);
            if (state.getBlock() != this.block) {
                if (state.getBlock().isReplaceable(world, target)) {
                    // Placed as though the face of the block before it had been clicked.
                    return super.onItemUse(player, world, target, hand, direction, hitX, hitY, hitZ);
                }
                break;
            }
            target = target.offset(direction);
            if (direction.getAxis().isHorizontal()) {
                steps++;
            }
        }
        return EnumActionResult.FAIL;
    }
}
