package com.exiledradio.villagerbackport.flora;

import com.exiledradio.villagerbackport.ModConfig;
import com.exiledradio.villagerbackport.VillagerBackport;
import com.exiledradio.villagerbackport.compat.FutureMc;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.oredict.ShapelessOreRecipe;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.IForgeRegistryModifiable;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Living alongside Future MC, which adds the same three flowers, the same four dyes and suspicious
 * stew.
 *
 * <p>Follows {@code compat.futureMcDuplicates}, the setting that already decides between the two
 * mods' workstations:
 * <ul>
 * <li><b>KEEP_OURS</b> (the default): Future MC's copies leave the creative menu and JEI, and lose
 * their crafting recipes. Each gains a one-for-one recipe into this mod's copy, so a Future MC
 * flower picked in the wild, or a dye already in a chest, is never stranded. Future MC's flowers
 * still generate - its worldgen has no switch this mod can reach - and they work anywhere ours
 * do as stew ingredients and mooshroom food.</li>
 * <li><b>KEEP_FUTURE_MC</b>: the reverse. This mod's flowers, dyes and stew leave the menus, their
 * recipes are not registered, its flowers stop generating and bone meal stops growing them, and
 * each gains a recipe into Future MC's copy. Brown mooshrooms, leather horse armor, the Globe
 * pattern and sign dyeing stay, since Future MC has none of them; a brown mooshroom's stew is
 * still this mod's.</li>
 * <li><b>SHOW_BOTH</b>: nothing is hidden.</li>
 * </ul>
 * Without Future MC none of this does anything. Its items are found by name - {@code futuremc:dye}
 * meta 0 to 3 is white, blue, brown and black - so this needs nothing from it at compile time.
 */
@Mod.EventBusSubscriber(modid = VillagerBackport.MOD_ID)
public final class FloraFutureMc {

    private FloraFutureMc() {
    }

    /** @return true if this mod's flowers, dyes and stew are the hidden half */
    public static boolean oursHidden() {
        return FutureMc.isLoaded() && ModConfig.compat.futureMcDuplicates == FutureMc.Duplicates.KEEP_FUTURE_MC
                && !pairs().isEmpty();
    }

    /** @return true if Future MC's copies are the hidden half */
    public static boolean theirsHidden() {
        return FutureMc.isLoaded() && ModConfig.compat.futureMcDuplicates == FutureMc.Duplicates.KEEP_OURS
                && !pairs().isEmpty();
    }

    /** @return {ours, theirs} for every item both mods have; empty without Future MC */
    public static List<ItemStack[]> pairs() {
        if (!FloraContent.enabled() || !FutureMc.isLoaded() || FloraContent.cornflower == null) {
            return Collections.emptyList();
        }

        List<ItemStack[]> pairs = new ArrayList<ItemStack[]>();
        pair(pairs, new ItemStack(FloraContent.cornflower), block("cornflower"));
        pair(pairs, new ItemStack(FloraContent.lilyOfTheValley), block("lily_of_the_valley"));
        pair(pairs, new ItemStack(FloraContent.witherRose), block("wither_rose"));
        pair(pairs, new ItemStack(FloraContent.whiteDye), item("dye", 0));
        pair(pairs, new ItemStack(FloraContent.blueDye), item("dye", 1));
        pair(pairs, new ItemStack(FloraContent.brownDye), item("dye", 2));
        pair(pairs, new ItemStack(FloraContent.blackDye), item("dye", 3));
        pair(pairs, new ItemStack(FloraContent.suspiciousStew), item("suspicious_stew", 0));
        return pairs;
    }

    private static void pair(List<ItemStack[]> pairs, ItemStack ours, @Nullable ItemStack theirs) {
        if (theirs != null && !ours.isEmpty()) {
            pairs.add(new ItemStack[]{ours, theirs});
        }
    }

    @Nullable
    private static ItemStack block(String name) {
        Block block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(FutureMc.MOD_ID, name));
        if (block == null || block == Blocks.AIR) {
            return null;
        }
        Item item = Item.getItemFromBlock(block);
        return item == Items.AIR ? null : new ItemStack(item);
    }

    @Nullable
    private static ItemStack item(String name, int meta) {
        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(FutureMc.MOD_ID, name));
        return item == null || item == Items.AIR ? null : new ItemStack(item, 1, meta);
    }

    /** @return the stacks to keep out of JEI */
    public static List<ItemStack> hiddenStacks() {
        List<ItemStack> hidden = new ArrayList<ItemStack>();
        int side = oursHidden() ? 0 : theirsHidden() ? 1 : -1;
        if (side >= 0) {
            for (ItemStack[] pair : pairs()) {
                hidden.add(pair[side]);
            }
        }
        return hidden;
    }

    /**
     * Hides one half of each pair from the creative menu, removes the hidden half's recipes and adds
     * the conversion recipes.
     *
     * <p>Lowest priority: every mod's items and recipes are registered by now, this mod's included.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRecipes(RegistryEvent.Register<IRecipe> event) {
        boolean ours = oursHidden();
        if (!ours && !theirsHidden()) {
            return;
        }

        IForgeRegistry<IRecipe> registry = event.getRegistry();
        List<ItemStack> hidden = hiddenStacks();
        for (ItemStack stack : hidden) {
            stack.getItem().setCreativeTab(null);
        }

        if (registry instanceof IForgeRegistryModifiable) {
            List<ResourceLocation> doomed = new ArrayList<ResourceLocation>();
            for (IRecipe recipe : registry) {
                ItemStack output = recipe.getRecipeOutput();
                if (output.isEmpty() || !VillagerBackport.MOD_ID.equals(recipe.getRegistryName().getNamespace())
                        && !FutureMc.MOD_ID.equals(recipe.getRegistryName().getNamespace())) {
                    continue;
                }
                for (ItemStack stack : hidden) {
                    if (output.getItem() == stack.getItem()
                            && (!stack.getHasSubtypes() || output.getMetadata() == stack.getMetadata())) {
                        doomed.add(recipe.getRegistryName());
                        break;
                    }
                }
            }
            for (ResourceLocation name : doomed) {
                ((IForgeRegistryModifiable<IRecipe>) registry).remove(name);
            }
            VillagerBackport.LOGGER.info("Future MC is installed; hid {} of the duplicated flowers, dyes and stew "
                    + "and removed {} recipes for them.", hidden.size(), doomed.size());
        }

        int index = 0;
        for (ItemStack[] pair : pairs()) {
            ItemStack from = ours ? pair[0] : pair[1];
            ItemStack to = ours ? pair[1] : pair[0];
            if (from.getItem() == FloraContent.suspiciousStew || to.getItem() == FloraContent.suspiciousStew) {
                // A stew's effect is in its NBT, and the two mods do not agree on the layout.
                continue;
            }
            ShapelessOreRecipe convert = new ShapelessOreRecipe(null, to.copy(), from.copy());
            convert.setRegistryName(new ResourceLocation(VillagerBackport.MOD_ID, "future_mc_conversion_" + index++));
            registry.register(convert);
        }
    }
}
