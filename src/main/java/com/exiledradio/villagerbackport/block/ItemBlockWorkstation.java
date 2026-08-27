package com.exiledradio.villagerbackport.block;

import net.minecraft.block.Block;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;

/**
 * A workstation's item form, named through {@link Names} so it stays readable when this mod's
 * language file is not loaded - see that class for why that happens.
 *
 * <p>The fallback is derived from the block's own registry name, so a block added later cannot end
 * up nameless.
 */
public class ItemBlockWorkstation extends ItemBlock {

    private final String fallbackName;

    public ItemBlockWorkstation(Block block) {
        super(block);
        this.fallbackName = Names.titleCase(block.getRegistryName());
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        return Names.translateOr(getTranslationKey(stack) + ".name", this.fallbackName);
    }
}
