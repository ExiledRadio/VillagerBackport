package com.exiledradio.villagerbackport.illager;

import com.exiledradio.villagerbackport.ModCreativeTab;
import com.exiledradio.villagerbackport.VillagerBackport;

import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityEntryBuilder;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * The pillager, the ravager, and the two items that come with them.
 *
 * <p>Registered whatever raid system is in use, and never renamed or removed: a world that has had
 * them must always find them. Neither mob spawns naturally - only raids, patrols and outposts send
 * them - but both have spawn eggs.
 */
@Mod.EventBusSubscriber(modid = VillagerBackport.MOD_ID)
public final class IllagerContent {

    /** Network ids within this mod. */
    private static final int PILLAGER_ID = 100;
    private static final int RAVAGER_ID = 101;

    public static Item pillagerCrossbow;
    public static ItemOminousBottle ominousBottle;

    private IllagerContent() {
    }

    @SubscribeEvent
    public static void registerEntities(RegistryEvent.Register<EntityEntry> event) {
        event.getRegistry().registerAll(
                EntityEntryBuilder.create().entity(EntityPillager.class)
                        .id(new ResourceLocation(VillagerBackport.MOD_ID, "pillager"), PILLAGER_ID)
                        .name(VillagerBackport.MOD_ID + ".pillager").egg(5451574, 9804699).tracker(64, 3, true).build(),
                EntityEntryBuilder.create().entity(EntityRavager.class)
                        .id(new ResourceLocation(VillagerBackport.MOD_ID, "ravager"), RAVAGER_ID)
                        .name(VillagerBackport.MOD_ID + ".ravager").egg(7697520, 5984329).tracker(64, 3, true).build());
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        // The pillagers' stand-in, for when there is no real crossbow (compat.crossbow OFF and no
        // crossbow mod): no recipe, no creative tab, never dropped.
        pillagerCrossbow = new ItemPillagerCrossbow();
        pillagerCrossbow.setRegistryName(new ResourceLocation(VillagerBackport.MOD_ID, "pillager_crossbow"));
        pillagerCrossbow.setTranslationKey(VillagerBackport.MOD_ID + ".pillager_crossbow");

        ominousBottle = new ItemOminousBottle();
        ominousBottle.setRegistryName(new ResourceLocation(VillagerBackport.MOD_ID, "ominous_bottle"));
        ominousBottle.setTranslationKey(VillagerBackport.MOD_ID + ".ominous_bottle");
        ominousBottle.setCreativeTab(ModCreativeTab.TAB);

        event.getRegistry().registerAll(pillagerCrossbow, ominousBottle);
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public static void registerModels(ModelRegistryEvent event) {
        ModelLoader.setCustomModelResourceLocation(pillagerCrossbow, 0,
                new ModelResourceLocation(pillagerCrossbow.getRegistryName(), "inventory"));
        for (int level = 0; level < ItemOminousBottle.LEVELS; level++) {
            ModelLoader.setCustomModelResourceLocation(ominousBottle, level,
                    new ModelResourceLocation(ominousBottle.getRegistryName(), "inventory"));
        }
    }
}
