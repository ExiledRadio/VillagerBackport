package com.exiledradio.villagerbackport.bamboo;

import com.exiledradio.villagerbackport.ModConfig;
import com.exiledradio.villagerbackport.VillagerBackport;
import com.exiledradio.villagerbackport.compat.FutureMc;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.IForgeRegistryModifiable;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Living alongside Future MC, which adds bamboo, scaffolding and pandas of its own.
 *
 * <p>Follows {@code compat.futureMcDuplicates}, the setting that already decides between the two
 * mods' workstations:
 * <ul>
 * <li><b>Bamboo and scaffolding</b>: the hidden mod's two items lose their recipes and leave the
 * creative menu and JEI. They keep existing and working, so anything already placed still stands
 * and still drops.</li>
 * <li><b>Pandas</b>: with this mod's kept, Future MC's pandas stop spawning naturally; with Future
 * MC's kept, this mod's stop spawning in ordinary jungles. This mod's still live in bamboo jungles
 * either way, since Future MC has no bamboo jungle.</li>
 * <li><b>Jungle bamboo</b>: Future MC already scatters its bamboo through jungles, so this mod's
 * light jungle bamboo stands aside whenever Future MC is installed, whichever copy is kept.</li>
 * </ul>
 * Pandas eat, and breed on, either mod's bamboo.
 */
public final class BambooFutureMc {

    private static final String[] SHARED = {"bamboo", "scaffolding"};

    private BambooFutureMc() {
    }

    private static boolean present() {
        return FutureMc.isLoaded();
    }

    private static FutureMc.Duplicates mode() {
        return ModConfig.compat.futureMcDuplicates;
    }

    /** @return true when Future MC is generating its own bamboo in jungles */
    static boolean generatesJungleBamboo() {
        return present() && futureMcItem("bamboo") != null;
    }

    /** @return true if this mod's pandas should be left out of ordinary jungles */
    static boolean leaveJunglesToFutureMc() {
        return present() && mode() == FutureMc.Duplicates.KEEP_FUTURE_MC;
    }

    /** @return Future MC's bamboo item, which pandas also eat, or null */
    @Nullable
    static Item futureMcBamboo() {
        return present() ? futureMcItem("bamboo") : null;
    }

    @Nullable
    private static Item futureMcItem(String name) {
        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(FutureMc.MOD_ID, name));
        return item == null || item == net.minecraft.init.Items.AIR ? null : item;
    }

    /** @return the bamboo and scaffolding items hidden by {@code compat.futureMcDuplicates} */
    public static Set<Item> hiddenItems() {
        if (!present() || mode() == FutureMc.Duplicates.SHOW_BOTH) {
            return Collections.emptySet();
        }
        Set<Item> hidden = new LinkedHashSet<Item>();
        for (String name : SHARED) {
            Item ours = ForgeRegistries.ITEMS.getValue(new ResourceLocation(VillagerBackport.MOD_ID, name));
            Item theirs = futureMcItem(name);
            if (ours == null || theirs == null || ours == net.minecraft.init.Items.AIR) {
                continue;
            }
            hidden.add(mode() == FutureMc.Duplicates.KEEP_FUTURE_MC ? ours : theirs);
        }
        return hidden;
    }

    /**
     * Takes the hidden items out of the creative menu and removes their recipes. Called from the
     * recipe registry event at its lowest priority, by which point every item exists and every JSON
     * recipe is loaded.
     */
    static void hide(IForgeRegistry<IRecipe> recipes) {
        Set<Item> hidden = hiddenItems();
        if (hidden.isEmpty()) {
            return;
        }
        for (Item item : hidden) {
            item.setCreativeTab(null);
        }
        if (recipes instanceof IForgeRegistryModifiable) {
            List<ResourceLocation> doomed = new ArrayList<ResourceLocation>();
            for (IRecipe recipe : recipes) {
                ItemStack output = recipe.getRecipeOutput();
                if (!output.isEmpty() && hidden.contains(output.getItem())) {
                    doomed.add(recipe.getRegistryName());
                }
            }
            for (ResourceLocation name : doomed) {
                ((IForgeRegistryModifiable<IRecipe>) recipes).remove(name);
            }
        }
        VillagerBackport.LOGGER.info("Future MC is installed; hid {} duplicated bamboo items ({}).",
                hidden.size(), mode());
    }

    /**
     * With this mod's pandas kept, stops Future MC's spawning naturally. Run once the first world
     * loads, after every mod has added its spawns.
     */
    @SuppressWarnings("unchecked")
    static void removeFutureMcPandaSpawns() {
        if (!present() || mode() != FutureMc.Duplicates.KEEP_OURS) {
            return;
        }
        EntityEntry entry = ForgeRegistries.ENTITIES.getValue(new ResourceLocation(FutureMc.MOD_ID, "panda"));
        if (entry == null || !EntityLiving.class.isAssignableFrom(entry.getEntityClass())) {
            return;
        }
        Class<? extends Entity> type = entry.getEntityClass();
        List<Biome> biomes = new ArrayList<Biome>(BiomeDictionary.getBiomes(BiomeDictionary.Type.JUNGLE));
        for (Biome biome : ForgeRegistries.BIOMES) {
            if (!biomes.contains(biome)) {
                biomes.add(biome);
            }
        }
        EntityRegistry.removeSpawn((Class<? extends EntityLiving>) type, EnumCreatureType.CREATURE,
                biomes.toArray(new Biome[0]));
        VillagerBackport.LOGGER.info("Future MC is installed; its pandas no longer spawn naturally (compat.futureMcDuplicates).");
    }
}
