package com.exiledradio.villagerbackport.flora;

import com.exiledradio.villagerbackport.VillagerBackport;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;
import net.minecraftforge.registries.IForgeRegistry;

/**
 * Stream F's crafting recipes, registered in code rather than as JSON so that they disappear with
 * {@code flora.enabled} and so the stew recipes can carry each flower's effect in their output.
 *
 * <h2>The dyes</h2>
 * 1.14's eight one-item recipes: bone meal or a lily of the valley to white dye, an ink sac or a
 * wither rose to black, cocoa beans to brown, lapis lazuli or a cornflower to blue. Each gives one
 * dye. They ask for the exact item, not its ore dictionary name - {@code dyeWhite} includes white
 * dye itself, and a recipe turning white dye into white dye is a loop JEI would list forever.
 *
 * <p>Nothing turns the new dyes back into bone meal or the others, as in 1.14.
 *
 * <h2>Suspicious stew</h2>
 * One shapeless recipe per flower - bowl, red mushroom, brown mushroom and the flower - each with
 * that flower's stew as its output, so JEI shows every one and which effect it gives. 1.14 does this
 * with a single special recipe that hides the effect; listing them is the one deliberate departure,
 * because a 1.12.2 pack leans on JEI to discover recipes at all.
 *
 * <h2>Leather horse armor</h2>
 * 1.14's shape, an H of leather. Plus the two special recipes that teach the grid about the new dyes
 * and the new armor: {@link LeatherHorseArmorDyeRecipe} and {@link ModernDyeRecipe}.
 */
final class FloraRecipes {

    private FloraRecipes() {
    }

    static void register(IForgeRegistry<IRecipe> registry) {
        dye(registry, "white_dye_from_bone_meal", new ItemStack(Items.DYE, 1, EnumDyeColor.WHITE.getDyeDamage()),
                FloraContent.whiteDye);
        dye(registry, "white_dye_from_lily_of_the_valley", new ItemStack(FloraContent.lilyOfTheValley),
                FloraContent.whiteDye);
        dye(registry, "black_dye_from_ink_sac", new ItemStack(Items.DYE, 1, EnumDyeColor.BLACK.getDyeDamage()),
                FloraContent.blackDye);
        dye(registry, "black_dye_from_wither_rose", new ItemStack(FloraContent.witherRose), FloraContent.blackDye);
        dye(registry, "brown_dye_from_cocoa_beans", new ItemStack(Items.DYE, 1, EnumDyeColor.BROWN.getDyeDamage()),
                FloraContent.brownDye);
        dye(registry, "blue_dye_from_lapis_lazuli", new ItemStack(Items.DYE, 1, EnumDyeColor.BLUE.getDyeDamage()),
                FloraContent.blueDye);
        dye(registry, "blue_dye_from_cornflower", new ItemStack(FloraContent.cornflower), FloraContent.blueDye);

        int index = 0;
        for (StewEffects.Entry entry : StewEffects.table()) {
            ResourceLocation name = new ResourceLocation(VillagerBackport.MOD_ID, "suspicious_stew_" + index++);
            ShapelessOreRecipe stew = new ShapelessOreRecipe(new ResourceLocation(VillagerBackport.MOD_ID, "suspicious_stew"),
                    ItemSuspiciousStew.create(entry.effect, entry.ticks),
                    new ItemStack(Items.BOWL), new ItemStack(Blocks.RED_MUSHROOM), new ItemStack(Blocks.BROWN_MUSHROOM),
                    entry.flower.copy());
            stew.setRegistryName(name);
            registry.register(stew);
        }

        ShapedOreRecipe armor = new ShapedOreRecipe(null, new ItemStack(FloraContent.leatherHorseArmor),
                "L L", "LLL", "L L", 'L', "leather");
        armor.setRegistryName(new ResourceLocation(VillagerBackport.MOD_ID, "leather_horse_armor"));
        registry.register(armor);

        registry.register(new LeatherHorseArmorDyeRecipe()
                .setRegistryName(new ResourceLocation(VillagerBackport.MOD_ID, "leather_horse_armor_dyeing")));
        registry.register(new ModernDyeRecipe()
                .setRegistryName(new ResourceLocation(VillagerBackport.MOD_ID, "modern_dyes")));
    }

    private static void dye(IForgeRegistry<IRecipe> registry, String name, ItemStack from, ItemModernDye to) {
        ShapelessOreRecipe recipe = new ShapelessOreRecipe(
                new ResourceLocation(VillagerBackport.MOD_ID, to.getRegistryName().getPath()), new ItemStack(to), from);
        recipe.setRegistryName(new ResourceLocation(VillagerBackport.MOD_ID, name));
        registry.register(recipe);
    }
}
