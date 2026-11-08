package com.exiledradio.villagerbackport.migrate;

import com.exiledradio.backportcore.migrate.StandInItem;
import com.exiledradio.villagerbackport.VillagerBackport;

import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

/**
 * Sea pickle items that Village &amp; Pillage 2.3.0 to 2.5.0 made, in a game without Update Aquatic
 * Backport, which owns sea pickles now.
 *
 * <p>Registered under the old item's name, {@code villagerbackport:sea_pickle}, only while Update
 * Aquatic's sea pickle is missing, so every stack a world holds (chests, players, the ground) loads as
 * this and keeps its count. With Update Aquatic added later, the name is missing again and
 * {@link UpdateAquaticPickles} points it at Update Aquatic's pickle, so the stacks become real pickles
 * wherever they are. It is named and drawn as a sea pickle and does nothing else; nothing makes one
 * except loading such a world or breaking a {@link BlockSeaPickleKept}.
 */
public class ItemSeaPickleStandIn extends StandInItem {

    ItemSeaPickleStandIn() {
        super(VillagerBackport.MOD_ID, "sea_pickle");
        // The kept block's name, "Sea Pickle", already in every language file.
        this.setTranslationKey(VillagerBackport.MOD_ID + ".sea_pickle");
    }

    /** {@code tile.villagerbackport.sea_pickle}, the old block's name, rather than an item key of its own. */
    @Override
    public String getTranslationKey(ItemStack stack) {
        return "tile." + VillagerBackport.MOD_ID + ".sea_pickle";
    }

    @Override
    public String getTranslationKey() {
        return "tile." + VillagerBackport.MOD_ID + ".sea_pickle";
    }

    /** @return Update Aquatic's sea pickles when it is installed, otherwise nothing */
    @Override
    public ItemStack real(int damage, int count) {
        Item real = ForgeRegistries.ITEMS.getValue(UpdateAquaticPickles.UA_PICKLE);
        return real == null || real == Items.AIR || real == this ? ItemStack.EMPTY : new ItemStack(real, count);
    }
}
