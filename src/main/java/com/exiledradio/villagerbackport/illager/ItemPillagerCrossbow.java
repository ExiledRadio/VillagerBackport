package com.exiledradio.villagerbackport.illager;

import com.exiledradio.villagerbackport.VillagerBackport;

import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;

/**
 * The crossbow pillagers carry when there is no real one: when {@code compat.crossbow} is OFF and no
 * other mod adds one.
 *
 * <p>It does nothing in a player's hand: it is a prop for the pillager to hold, with 1.14's model and
 * its loaded look, and a pillager never drops it. The real crossbow is
 * {@link com.exiledradio.villagerbackport.crossbow.ItemCrossbow}.
 */
public final class ItemPillagerCrossbow extends Item {

    public ItemPillagerCrossbow() {
        setMaxStackSize(1);
        // Drawn with an arrow in it while loaded, as 1.14's crossbow is.
        addPropertyOverride(new ResourceLocation(VillagerBackport.MOD_ID, "charged"),
                (stack, world, entity) -> Crossbows.isCharged(stack) ? 1.0F : 0.0F);
    }
}
