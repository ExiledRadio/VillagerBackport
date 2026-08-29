package com.exiledradio.villagerbackport;

import com.exiledradio.villagerbackport.block.Names;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * The creative tab every block and item this mod adds appears under.
 *
 * <h2>Why a tab of its own</h2>
 * The workstations were scattered through Decorations and the banner patterns through Miscellaneous,
 * which are the tabs vanilla would have put them in and are also two of the fuller pages in the game.
 * Sixteen entries added to somebody's pack is easier to find gathered in one place, and a pack that
 * would rather they were somewhere else can move them with a resource pack or a tweaker either way.
 *
 * <h2>The icon</h2>
 * Looked up from the registry rather than held from registration, because the tab is constructed
 * while this class loads, which is before any block exists. Asked for once and cached by
 * {@code CreativeTabs} itself, so there is no lookup per frame.
 *
 * <p>The fallback matters. An icon of nothing leaves the creative screen drawing an empty slot
 * where the tab should be, and an emerald is at least on the nose for this mod.
 *
 * <h2>The label</h2>
 * Resolved here rather than left to the creative screen, for the reason described on {@link Names}:
 * in a pack where this mod's language file is not loaded, the tab draws as
 * {@code itemGroup.villagerbackport}. Every other name this mod shows already goes through that
 * fallback; the tab was the one string reaching a vanilla lookup directly.
 *
 * <p>Returning text where a key is expected is safe both ways round. The creative screen passes
 * whatever comes back to {@code I18n.format}, which hands an unrecognised string straight back.
 */
public final class ModCreativeTab extends CreativeTabs {

    public static final ModCreativeTab TAB = new ModCreativeTab();

    /** The block whose item stands for the whole tab. */
    private static final String ICON = "lectern";

    /** Shown when the language file has not loaded. Matches the entry in en_us.lang. */
    private static final String LABEL = "1.14 Villager Backport";

    private ModCreativeTab() {
        // Becomes the itemGroup.villagerbackport translation key.
        super(VillagerBackport.MOD_ID);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public String getTranslationKey() {
        return Names.translateOr(super.getTranslationKey(), LABEL);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public ItemStack createIcon() {
        Item item = ForgeRegistries.ITEMS.getValue(
                new ResourceLocation(VillagerBackport.MOD_ID, ICON));

        return item == null ? new ItemStack(Items.EMERALD) : new ItemStack(item);
    }
}
