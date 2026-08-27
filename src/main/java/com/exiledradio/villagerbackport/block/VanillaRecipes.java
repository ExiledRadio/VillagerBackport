package com.exiledradio.villagerbackport.block;

import com.exiledradio.villagerbackport.ModConfig;
import com.exiledradio.villagerbackport.VillagerBackport;

import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.IForgeRegistryModifiable;

/**
 * Vanilla recipes this mod takes away, when a workstation is configured to be the only route.
 *
 * <p>Only one so far: applying a banner pattern in a crafting table, which 1.14 removed when the
 * loom arrived. It is off by default - see {@code workstations.loomOnlyBannerPatterns} for why
 * taking something away is the opt-in half of that.
 *
 * <h2>Why it is safe to remove here</h2>
 * Vanilla's own recipes are registered from {@code Bootstrap}, long before any mod's registry event
 * runs, so by the time this fires the entry is already in place and this is simply a later edit to
 * the same registry. Nothing is registered in its place: a missing recipe is a recipe that does not
 * match, which is exactly the intent.
 *
 * <p>Duplicating a banner is a separate recipe and is left alone, because 1.14 left it alone too.
 */
@Mod.EventBusSubscriber(modid = VillagerBackport.MOD_ID)
public final class VanillaRecipes {

    /** Vanilla's own name for it - registered without an underscore, unlike most of them. */
    private static final ResourceLocation BANNER_ADD_PATTERN =
            new ResourceLocation("minecraft", "banneraddpattern");

    private VanillaRecipes() {
    }

    @SubscribeEvent
    public static void removeRecipes(RegistryEvent.Register<IRecipe> event) {
        if (!ModConfig.workstations.loomOnlyBannerPatterns) {
            return;
        }

        IForgeRegistry<IRecipe> registry = event.getRegistry();

        // Every registry Forge builds is modifiable, but the interface does not say so - so this
        // asks rather than assumes, and leaves the recipe in place if the answer is ever no.
        if (!(registry instanceof IForgeRegistryModifiable)) {
            VillagerBackport.LOGGER.warn(
                    "Cannot remove the crafting-table banner pattern recipe: the recipe registry is not modifiable.");
            return;
        }

        IRecipe removed = ((IForgeRegistryModifiable<IRecipe>) registry).remove(BANNER_ADD_PATTERN);

        if (removed == null) {
            VillagerBackport.LOGGER.warn(
                    "Cannot remove the crafting-table banner pattern recipe: {} is not registered.",
                    BANNER_ADD_PATTERN);
        } else {
            VillagerBackport.LOGGER.info(
                    "Banner patterns are now applied at a loom only; removed {}.", BANNER_ADD_PATTERN);
        }
    }
}
