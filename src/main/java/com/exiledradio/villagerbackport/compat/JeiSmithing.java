package com.exiledradio.villagerbackport.compat;

import com.exiledradio.villagerbackport.VillagerBackport;
import com.exiledradio.villagerbackport.block.ContainerSmithingTable;
import com.exiledradio.villagerbackport.block.Names;
import com.exiledradio.villagerbackport.block.SmithingUpgrades;
import com.exiledradio.villagerbackport.client.GuiSmithingTable;

import mezz.jei.api.IGuiHelper;
import mezz.jei.api.gui.IDrawable;
import mezz.jei.api.gui.IGuiItemStackGroup;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.ingredients.VanillaTypes;
import mezz.jei.api.recipe.IRecipeCategory;
import mezz.jei.api.recipe.IRecipeWrapper;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The smithing table's netherite upgrades in JEI, laid out as the table's own slot row.
 *
 * <p>Only shown when this mod's table is the one in use. When it replaces Future MC's, Future MC's
 * own smithing page is hidden - see {@link JeiPlugin}.
 */
final class JeiSmithing {

    static final String UID = VillagerBackport.MOD_ID + ".smithing";

    private JeiSmithing() {
    }

    /** Future MC's smithing category, as its JEI plugin registers it. */
    static final String FUTURE_MC_UID = "container.jei.futuremc.smithing.name";

    static boolean futureMcTableHidden() {
        Block theirs = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(FutureMc.MOD_ID, "smithing_table"));
        return theirs != null && FutureMc.isHidden(theirs);
    }

    static Block table() {
        return ForgeRegistries.BLOCKS.getValue(new ResourceLocation(VillagerBackport.MOD_ID, "smithing_table"));
    }

    /** @return true if the category should be shown at all */
    static boolean shown() {
        Block table = table();
        return table != null && !FutureMc.isHidden(table) && SmithingUpgrades.isActive();
    }

    static List<Wrapper> recipes() {
        List<Wrapper> recipes = new ArrayList<Wrapper>();
        for (SmithingUpgrades.Upgrade upgrade : SmithingUpgrades.list()) {
            recipes.add(new Wrapper(upgrade));
        }
        return recipes;
    }

    static final class Category implements IRecipeCategory<Wrapper> {

        private final IDrawable background;
        private final IDrawable icon;

        Category(IGuiHelper gui) {
            // The slot row of the table's own screen: base, plus, addition, arrow, result.
            this.background = gui.createDrawable(GuiSmithingTable.TEXTURE, 26, 46, 126, 18);
            this.icon = gui.createDrawableIngredient(new ItemStack(table()));
        }

        @Override
        public String getUid() {
            return UID;
        }

        @Override
        public String getTitle() {
            return Names.translateOr("jei.villagerbackport.smithing", "Smithing");
        }

        @Override
        public String getModName() {
            return VillagerBackport.MOD_NAME;
        }

        @Override
        public IDrawable getBackground() {
            return this.background;
        }

        @Override
        public IDrawable getIcon() {
            return this.icon;
        }

        @Override
        public void setRecipe(IRecipeLayout layout, Wrapper recipe, IIngredients ingredients) {
            IGuiItemStackGroup stacks = layout.getItemStacks();
            stacks.init(ContainerSmithingTable.SLOT_BASE, true, 0, 0);
            stacks.init(ContainerSmithingTable.SLOT_ADDITION, true, 49, 0);
            stacks.init(ContainerSmithingTable.SLOT_RESULT, false, 107, 0);
            stacks.set(ingredients);
        }
    }

    static final class Wrapper implements IRecipeWrapper {

        private final SmithingUpgrades.Upgrade upgrade;

        Wrapper(SmithingUpgrades.Upgrade upgrade) {
            this.upgrade = upgrade;
        }

        @Override
        public void getIngredients(IIngredients ingredients) {
            List<List<ItemStack>> inputs = new ArrayList<List<ItemStack>>();
            inputs.add(Collections.singletonList(new ItemStack(this.upgrade.base)));
            inputs.add(this.upgrade.additions());
            ingredients.setInputLists(VanillaTypes.ITEM, inputs);
            ingredients.setOutput(VanillaTypes.ITEM, this.upgrade.result());
        }
    }
}
