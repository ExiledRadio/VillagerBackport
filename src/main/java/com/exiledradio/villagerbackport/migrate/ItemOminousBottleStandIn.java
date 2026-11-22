package com.exiledradio.villagerbackport.migrate;

import com.exiledradio.backportcore.migrate.StandInItem;
import com.exiledradio.villagerbackport.VillagerBackport;

import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/**
 * Ominous bottles that Village &amp; Pillage 2.2.0 to 2.7.3 made, in a game without Tricky Trials Backport (or
 * Deeper Depths), which owns ominous bottles now.
 *
 * <p>Registered under the old item's name, {@code villagerbackport:ominous_bottle}, only while no other bottle is
 * there, so every stack a world holds loads as this and keeps its count and level (metadata 0-4). With Tricky Trials
 * added later, the name is missing again and {@link OminousBottles} points it at Tricky Trials' bottle. It is named
 * and drawn as an ominous bottle and does nothing else; nothing makes one except loading such a world.
 */
public class ItemOminousBottleStandIn extends StandInItem {

    ItemOminousBottleStandIn() {
        super(VillagerBackport.MOD_ID, "ominous_bottle");
        // "Ominous Bottle", already in every language file.
        this.setTranslationKey(VillagerBackport.MOD_ID + ".ominous_bottle");
        this.setMaxStackSize(64);
    }

    @Override
    public EnumRarity getRarity(ItemStack stack) {
        return EnumRarity.UNCOMMON;
    }

    /** @return the bottle that took over, at the same level, or nothing while there is none */
    @Override
    public ItemStack real(int damage, int count) {
        Item real = OminousBottles.bottle();
        return real == null || real == this ? ItemStack.EMPTY : new ItemStack(real, count, damage);
    }
}
