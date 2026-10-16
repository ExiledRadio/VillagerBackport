package com.exiledradio.villagerbackport.home;

import com.exiledradio.villagerbackport.data.VillagerTradeData;

import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.init.Items;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * A villager's food, and whether it has enough to breed.
 *
 * <h2>The game's rule</h2>
 * Each food a villager carries is worth some nutrition: bread 4, and a carrot, a potato or a
 * beetroot 1 each. A villager is willing to breed when the food in its inventory plus whatever it
 * has left over from last time comes to 12 or more. Breeding eats food until there are at least 12
 * unspent points, then uses up 12 of them - whether or not a baby comes of it.
 *
 * <p>1.12.2's rule was different in every respect: one stack of 3 bread or 12 carrots or potatoes,
 * no mixing, beetroot never counted, and trading could make a villager willing with no food at all.
 */
public final class VillagerFood {

    private static final String LEFT_OVER = "FoodLevel";

    /** The nutrition a villager needs to be willing, and what one breeding uses. */
    public static final int TO_BREED = 12;

    private static final Map<Item, Integer> NUTRITION = new HashMap<Item, Integer>();

    static {
        NUTRITION.put(Items.BREAD, Integer.valueOf(4));
        NUTRITION.put(Items.CARROT, Integer.valueOf(1));
        NUTRITION.put(Items.POTATO, Integer.valueOf(1));
        NUTRITION.put(Items.BEETROOT, Integer.valueOf(1));
    }

    private VillagerFood() {
    }

    /** @return true if this villager has the food to breed */
    public static boolean isWilling(EntityVillager villager) {
        return leftOver(villager) + inInventory(villager) >= TO_BREED;
    }

    /** Eats until there are 12 unspent points, then uses 12 of them. */
    public static void eatForBreeding(EntityVillager villager) {
        int level = leftOver(villager);
        InventoryBasic inventory = villager.getVillagerInventory();

        for (int slot = 0; slot < inventory.getSizeInventory() && level < TO_BREED; slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            Integer value = NUTRITION.get(stack.getItem());
            if (value == null) {
                continue;
            }

            while (!stack.isEmpty() && level < TO_BREED) {
                stack.shrink(1);
                level += value.intValue();
            }

            if (stack.isEmpty()) {
                inventory.setInventorySlotContents(slot, ItemStack.EMPTY);
            }
        }

        VillagerTradeData.root(villager).setInteger(LEFT_OVER, Math.max(0, level - TO_BREED));
    }

    private static int leftOver(EntityVillager villager) {
        return VillagerTradeData.root(villager).getInteger(LEFT_OVER);
    }

    private static int inInventory(EntityVillager villager) {
        InventoryBasic inventory = villager.getVillagerInventory();
        int total = 0;

        for (int slot = 0; slot < inventory.getSizeInventory(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            Integer value = NUTRITION.get(stack.getItem());
            if (value != null) {
                total += value.intValue() * stack.getCount();
            }
        }

        return total;
    }
}
