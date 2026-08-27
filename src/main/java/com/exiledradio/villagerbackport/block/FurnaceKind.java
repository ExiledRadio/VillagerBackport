package com.exiledradio.villagerbackport.block;

import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraftforge.oredict.OreDictionary;

/**
 * The two specialised furnaces, and what each one will accept.
 *
 * <h2>What makes them different from a furnace</h2>
 * Both cook in half the time and take only part of what a furnace does. That trade is the whole
 * design: a smoker is faster for food and useless for anything else, so it is worth building
 * alongside a furnace rather than instead of one.
 *
 * <h2>Deciding what qualifies</h2>
 * 1.14 answers this with item tags - {@code minecraft:smelts_to_food} and an ore list - which 1.12.2
 * has no equivalent of. Rather than transcribe a list of item ids that would go stale the moment a
 * mod adds an ore, each kind asks a question about the item:
 *
 * <ul>
 *   <li>a smoker takes anything whose smelted result is food;</li>
 *   <li>a blast furnace takes anything registered as an ore, or that smelts into an ingot.</li>
 * </ul>
 *
 * <p>Both are answered from the furnace recipe list and the ore dictionary, so a modded ore is
 * accepted the moment its mod registers it properly - with no list here to maintain.
 */
public enum FurnaceKind {

    /** Cooks food, and nothing else. */
    SMOKER {
        @Override
        public boolean accepts(ItemStack stack) {
            return smeltResultOf(stack).getItem() instanceof ItemFood;
        }
    },

    /** Smelts ores and metals, and nothing else. */
    BLAST_FURNACE {
        @Override
        public boolean accepts(ItemStack stack) {
            if (hasOrePrefix(stack, "ore")) {
                return true;
            }

            // Some things smelt into metal without being ores - raw chunks and crushed dusts, which
            // several mods add. Judging by the result catches those without naming any of them.
            ItemStack result = smeltResultOf(stack);
            return !result.isEmpty() && (hasOrePrefix(result, "ingot") || hasOrePrefix(result, "gem"));
        }
    };

    /** Ticks to cook one item. Half a furnace's 200, which is the point of these blocks. */
    public static final int COOK_TIME = 100;

    /** @return true if this furnace will take the item. */
    public abstract boolean accepts(ItemStack stack);

    /**
     * @return what the item smelts into, or empty if it does not smelt at all.
     *
     * <p>Asking the recipe list rather than keeping our own means anything a mod adds a furnace
     * recipe for is considered on the same terms as vanilla's.
     */
    static ItemStack smeltResultOf(ItemStack stack) {
        return stack.isEmpty() ? ItemStack.EMPTY : FurnaceRecipes.instance().getSmeltingResult(stack);
    }

    /**
     * @return true if any of the item's ore dictionary names starts with the given prefix.
     *
     * <p>Prefix rather than exact match because the dictionary names by category - {@code oreIron},
     * {@code ingotCopper} - so the prefix is the category and the rest is the material.
     */
    static boolean hasOrePrefix(ItemStack stack, String prefix) {
        for (int id : OreDictionary.getOreIDs(stack)) {
            if (OreDictionary.getOreName(id).startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}
