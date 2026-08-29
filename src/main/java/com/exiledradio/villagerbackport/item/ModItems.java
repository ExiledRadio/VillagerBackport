package com.exiledradio.villagerbackport.item;

import com.exiledradio.villagerbackport.ModCreativeTab;
import com.exiledradio.villagerbackport.VillagerBackport;

import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.tileentity.BannerPattern;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.List;

/**
 * Items this mod adds that are not the item form of a block.
 *
 * <p>Only the four banner patterns so far. Globe is the fifth in 1.14 and is not here: 1.12.2's
 * {@link BannerPattern} has no globe design to point at, and the only source of one in 1.14 is the
 * wandering trader - so an item for it would name a pattern that cannot be drawn and could not be
 * come by in any case. See {@link com.exiledradio.villagerbackport.block.BannerPatterns}.
 */
@Mod.EventBusSubscriber(modid = VillagerBackport.MOD_ID)
public final class ModItems {

    private static final List<Item> ITEMS = new ArrayList<Item>();

    public static ItemBannerPattern creeperBannerPattern;
    public static ItemBannerPattern skullBannerPattern;
    public static ItemBannerPattern flowerBannerPattern;
    public static ItemBannerPattern mojangBannerPattern;

    private ModItems() {
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        // The descriptions are 1.14's own, which are the designs' names rather than the patterns'
        // - "Thing" for the Mojang logo is deliberate on their part, and kept.
        creeperBannerPattern = add(event, "creeper_banner_pattern",
                new ItemBannerPattern(BannerPattern.CREEPER, "Creeper Charge"));
        skullBannerPattern = add(event, "skull_banner_pattern",
                new ItemBannerPattern(BannerPattern.SKULL, "Skull Charge"));
        flowerBannerPattern = add(event, "flower_banner_pattern",
                new ItemBannerPattern(BannerPattern.FLOWER, "Flower Charge"));
        mojangBannerPattern = add(event, "mojang_banner_pattern",
                new ItemBannerPattern(BannerPattern.MOJANG, "Thing"));
    }

    private static <T extends Item> T add(RegistryEvent.Register<Item> event, String name, T item) {
        item.setRegistryName(new ResourceLocation(VillagerBackport.MOD_ID, name));
        item.setTranslationKey(VillagerBackport.MOD_ID + "." + name);
        item.setCreativeTab(ModCreativeTab.TAB);

        event.getRegistry().register(item);
        ITEMS.add(item);
        return item;
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public static void registerModels(ModelRegistryEvent event) {
        for (Item item : ITEMS) {
            ModelLoader.setCustomModelResourceLocation(item, 0,
                    new ModelResourceLocation(item.getRegistryName(), "inventory"));
        }
    }
}
