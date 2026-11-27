package com.exiledradio.villagerbackport.selftest;

import com.exiledradio.backportcore.recipe.book.SmeltingBook;
import com.exiledradio.backportcore.recipe.book.SmeltingBooks;
import com.exiledradio.villagerbackport.block.FurnaceKind;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;

/**
 * A furnace recipe with an empty input (some mods register one) no longer crashes the blast furnace
 * and smoker books: Forge's ore dictionary throws on an empty stack.
 */
public final class CookingBooksSelfTest implements SelfTest {

    @Override
    public String name() {
        return "cookingbooks";
    }

    @Override
    public void run(Check check) {
        check.ok(!FurnaceKind.BLAST_FURNACE.accepts(ItemStack.EMPTY), "blast furnace refuses an empty stack");
        check.ok(!FurnaceKind.SMOKER.accepts(ItemStack.EMPTY), "smoker refuses an empty stack");

        ItemStack emptyIn = ItemStack.EMPTY;
        FurnaceRecipes.instance().getSmeltingList().put(emptyIn, new ItemStack(Items.IRON_INGOT));
        try {
            SmeltingBooks.invalidate();
            int books = 0;
            for (SmeltingBook book : SmeltingBooks.all()) {
                try {
                    int n = SmeltingBooks.recipes(book).size();
                    check.ok(n > 0, "book " + book.id + " built with an empty-input recipe present (" + n + ")");
                } catch (RuntimeException e) {
                    check.fail("book " + book.id + " threw " + e);
                }
                books++;
            }
            check.ok(books >= 3, "blast furnace, smoker and campfire books registered (" + books + ")");
        } finally {
            FurnaceRecipes.instance().getSmeltingList().remove(emptyIn);
            SmeltingBooks.invalidate();
        }
    }
}
