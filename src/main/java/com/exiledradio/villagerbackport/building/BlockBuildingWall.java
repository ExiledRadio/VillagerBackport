package com.exiledradio.villagerbackport.building;

import net.minecraft.block.Block;
import net.minecraft.block.BlockWall;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;

/**
 * One of 1.14's new walls.
 *
 * <h2>Why vanilla's wall, with its baggage</h2>
 * Extending {@link BlockWall} is what makes these connect: a wall joins anything whose face shape is
 * a thick middle pole, which is what every {@code BlockWall} reports, so these join vanilla's
 * cobblestone walls, each other and every modded wall built the same way, exactly as 1.14's walls
 * join each other. It is also how the stonecutter recognizes a wall when it reads the crafting
 * recipes.
 *
 * <p>The cost is vanilla's {@code variant} property, which is how the one cobblestone wall block
 * carries its mossy twin. {@code BlockWall}'s constructor sets it, so it cannot be removed; instead
 * it is pinned to its first value here and ignored by the client's state mapper, and nothing about
 * these blocks ever reads it.
 */
public final class BlockBuildingWall extends BlockWall implements com.exiledradio.backportcore.waterlog.Waterloggable {

    BlockBuildingWall(Block base) {
        super(base);
    }

    @Override
    @SuppressWarnings("deprecation")
    public String getLocalizedName() {
        // Vanilla's appends ".normal", the name of the cobblestone variant.
        return net.minecraft.util.text.translation.I18n.translateToLocal(getTranslationKey() + ".name");
    }

    @Override
    public void getSubBlocks(CreativeTabs tab, NonNullList<ItemStack> items) {
        items.add(new ItemStack(this));
    }

    @Override
    public int damageDropped(IBlockState state) {
        return 0;
    }

    @Override
    @SuppressWarnings("deprecation")
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState();
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return 0;
    }
}
