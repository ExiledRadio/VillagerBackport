package com.exiledradio.villagerbackport.item;

import com.exiledradio.villagerbackport.block.Names;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.BannerPattern;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;

import java.util.List;

/**
 * A banner pattern, carried as an item so a loom can read it.
 *
 * <h2>What this replaces</h2>
 * 1.12.2 applies the creeper, skull, flower and Mojang patterns by putting the thing itself into a
 * crafting grid - a creeper head, a wither skeleton skull, an oxeye daisy, an enchanted golden
 * apple. 1.14 moved all four behind an item crafted from that thing and a sheet of paper, which is
 * what the loom's third slot takes.
 *
 * <p>The point of the change is that the item is <em>not</em> consumed: one banner pattern applies
 * its design to as many banners as a player likes, so the head or the apple is spent once rather
 * than once per banner. Wearing out was never the interesting part of it.
 *
 * <p>Each of these stacks to one, as 1.14's do, and reads "Banner Pattern" with the design named
 * underneath - so four items that would otherwise be indistinguishable are told apart by the second
 * line rather than the first.
 */
public class ItemBannerPattern extends Item {

    private final BannerPattern pattern;
    private final String fallbackDescription;

    /**
     * @param pattern             the design this applies
     * @param fallbackDescription 1.14's own name for the design, used when this mod's language file
     *                            is not loaded - see {@link Names} for why that happens
     */
    public ItemBannerPattern(BannerPattern pattern, String fallbackDescription) {
        this.pattern = pattern;
        this.fallbackDescription = fallbackDescription;

        setMaxStackSize(1);
    }

    public BannerPattern getPattern() {
        return this.pattern;
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        return Names.translateOr(getTranslationKey(stack) + ".name", "Banner Pattern");
    }

    /** The design's name, in grey below the item's own - which is the only thing telling these apart. */
    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip,
                               ITooltipFlag flag) {
        tooltip.add(TextFormatting.GRAY + Names.translateOr(
                getTranslationKey(stack) + ".desc", this.fallbackDescription));
    }
}
