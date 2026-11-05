package com.exiledradio.villagerbackport.crossbow;

import com.exiledradio.villagerbackport.VillagerBackport;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnumEnchantmentType;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.util.ResourceLocation;

/**
 * Quick Charge, Multishot and Piercing. What each does is in {@link ItemCrossbow}; this is only their
 * place on the enchanting table: rarity, levels, and an enchanting cost of {@code base + perLevel *
 * (level - 1)} up to 50. Multishot and Piercing cannot go on the same crossbow.
 */
final class CrossbowEnchantment extends Enchantment {

    private final int maxLevel;
    private final int base;
    private final int perLevel;

    CrossbowEnchantment(String name, Rarity rarity, EnumEnchantmentType type, int maxLevel, int base, int perLevel) {
        super(rarity, type, new EntityEquipmentSlot[]{EntityEquipmentSlot.MAINHAND});
        this.maxLevel = maxLevel;
        this.base = base;
        this.perLevel = perLevel;
        setRegistryName(new ResourceLocation(VillagerBackport.MOD_ID, name));
        setName(VillagerBackport.MOD_ID + "." + name);
    }

    @Override
    public int getMinEnchantability(int level) {
        return this.base + this.perLevel * (level - 1);
    }

    @Override
    public int getMaxEnchantability(int level) {
        return 50;
    }

    @Override
    public int getMaxLevel() {
        return this.maxLevel;
    }

    @Override
    protected boolean canApplyTogether(Enchantment other) {
        boolean exclusive = (this == CrossbowContent.multishot && other == CrossbowContent.piercing)
                || (this == CrossbowContent.piercing && other == CrossbowContent.multishot);
        return !exclusive && super.canApplyTogether(other);
    }
}
