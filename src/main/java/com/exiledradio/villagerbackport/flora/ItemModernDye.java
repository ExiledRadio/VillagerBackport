package com.exiledradio.villagerbackport.flora;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntitySheep;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;

/**
 * One of 1.14's four new dyes: white, black, brown or blue.
 *
 * <p>In 1.12.2 those four colors are bone meal, the ink sac, cocoa beans and lapis lazuli, each of
 * which is also something else. 1.14 gave each color a plain dye of its own; these are those dyes.
 * The old items keep every use they had - see {@link FloraContent} for how the new ones reach every
 * recipe that wants a dye.
 *
 * <p>A dye item does three things outside the crafting grid, all handled to match 1.12.2's own
 * dyes: it dyes a sheep (here), a tamed wolf's collar ({@link DyeInteractions}, since the wolf
 * answers the click before any item is asked) and, new in 1.14, a sign's text ({@link SignDyeing}).
 */
public class ItemModernDye extends Item {

    private final EnumDyeColor color;

    public ItemModernDye(EnumDyeColor color) {
        this.color = color;
    }

    public EnumDyeColor getColor() {
        return this.color;
    }

    /** @return the ore dictionary name 1.12.2's dye of this color is under, such as {@code dyeWhite} */
    public String oreName() {
        switch (this.color) {
            case WHITE:
                return "dyeWhite";
            case BLACK:
                return "dyeBlack";
            case BROWN:
                return "dyeBrown";
            case BLUE:
                return "dyeBlue";
            default:
                throw new IllegalStateException("Not one of 1.14's new dyes: " + this.color);
        }
    }

    /** 1.12.2's {@code ItemDye.itemInteractionForEntity}: dyes an unsheared sheep that is another color. */
    @Override
    public boolean itemInteractionForEntity(ItemStack stack, EntityPlayer player, EntityLivingBase target,
                                            EnumHand hand) {
        if (!(target instanceof EntitySheep)) {
            return false;
        }

        EntitySheep sheep = (EntitySheep) target;
        if (!sheep.getSheared() && sheep.getFleeceColor() != this.color) {
            sheep.setFleeceColor(this.color);
            stack.shrink(1);
        }
        return true;
    }
}
