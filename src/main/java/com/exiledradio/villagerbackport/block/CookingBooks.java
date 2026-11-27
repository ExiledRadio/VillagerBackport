package com.exiledradio.villagerbackport.block;

import com.exiledradio.backportcore.recipe.book.SmeltingBook;
import com.exiledradio.backportcore.recipe.book.SmeltingBooks;
import com.exiledradio.villagerbackport.campfire.CampfireRecipes;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 1.14's recipe books for this mod's cooking blocks, kept by Backport Core ({@link SmeltingBooks}), as the furnace's is.
 *
 * <ul>
 *   <li><b>Blast furnace</b>: the ores and metals it takes ({@link FurnaceKind#BLAST_FURNACE}), tabs Blocks (iron ore)
 *   and Miscellaneous (iron shovel and golden leggings), "Showing blastable".</li>
 *   <li><b>Smoker</b>: the food it takes, one tab Food (porkchop), "Showing smokable".</li>
 *   <li><b>Campfire</b>: no screen in 1.14 either; its recipes unlock as you pick up their food and toast with the
 *   campfire, as 1.14's {@code campfire_cooking} recipes do.</li>
 * </ul>
 */
public final class CookingBooks {

    private CookingBooks() {
    }

    /** From init, once the blocks exist. Both sides. */
    public static void init() {
        Block blast = Block.getBlockFromName("villagerbackport:blast_furnace");
        if (blast != null) {
            SmeltingBooks.register(new SmeltingBook(new ResourceLocation("minecraft", "blast_furnace"),
                    c -> c != null && c.getClass() == ContainerFurnaceVariant.class
                            && ((ContainerFurnaceVariant) c).kind() == FurnaceKind.BLAST_FURNACE,
                    0, 1, 2, () -> furnaceRecipesFor(FurnaceKind.BLAST_FURNACE), TileEntityFurnace::isItemFuel,
                    "gui.recipebook.toggleRecipes.blastable", new ItemStack(blast),
                    new SmeltingBook.Tab(s -> s.getItem() instanceof ItemBlock, new ItemStack(Blocks.IRON_ORE)),
                    new SmeltingBook.Tab(s -> true, new ItemStack(Items.IRON_SHOVEL), new ItemStack(Items.GOLDEN_LEGGINGS))));
        }
        Block smoker = Block.getBlockFromName("villagerbackport:smoker");
        if (smoker != null) {
            SmeltingBooks.register(new SmeltingBook(new ResourceLocation("minecraft", "smoker"),
                    c -> c != null && c.getClass() == ContainerFurnaceVariant.class
                            && ((ContainerFurnaceVariant) c).kind() == FurnaceKind.SMOKER,
                    0, 1, 2, () -> furnaceRecipesFor(FurnaceKind.SMOKER), TileEntityFurnace::isItemFuel,
                    "gui.recipebook.toggleRecipes.smokable", new ItemStack(smoker),
                    new SmeltingBook.Tab(s -> true, new ItemStack(Items.PORKCHOP))));
        }
        Block campfire = Block.getBlockFromName("villagerbackport:campfire");
        if (campfire != null) {
            SmeltingBooks.register(new SmeltingBook(new ResourceLocation("minecraft", "campfire"), c -> false, 0, 1, 2,
                    CampfireRecipes::recipes, s -> false, "gui.recipebook.toggleRecipes.smeltable", new ItemStack(campfire)));
        }
    }

    /** The furnace recipes whose input the block takes (what its input slot accepts). */
    private static Map<ItemStack, ItemStack> furnaceRecipesFor(FurnaceKind kind) {
        Map<ItemStack, ItemStack> out = new LinkedHashMap<ItemStack, ItemStack>();
        for (Map.Entry<ItemStack, ItemStack> e : FurnaceRecipes.instance().getSmeltingList().entrySet()) {
            ItemStack in = e.getKey();
            if (in != null && !in.isEmpty() && e.getValue() != null && !e.getValue().isEmpty() && kind.accepts(in)) {
                out.put(e.getKey(), e.getValue());
            }
        }
        return out;
    }
}
