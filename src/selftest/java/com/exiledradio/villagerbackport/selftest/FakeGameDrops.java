package com.exiledradio.villagerbackport.selftest;

import com.exiledradio.villagerbackport.VillagerBackport;
import net.minecraft.item.Item;
import net.minecraft.item.ItemMap;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.io.File;

/**
 * Test only, never shipped: stands in for the two Game Drops Backport items this mod looks for by name, in a dev
 * environment without that mod - {@code gamedropsbackport:golden_dandelion} (present with its 26.1 name tag changes)
 * and one explorer map, {@code gamedropsbackport:abandoned_camp_map} (an {@link ItemMap}, as Game Drops' are). Only
 * when the file {@code fake-gd} exists in the run directory and the real mod is not loaded.
 */
@Mod.EventBusSubscriber(modid = VillagerBackport.MOD_ID)
public final class FakeGameDrops {

    static Item explorerMap;

    private FakeGameDrops() {
    }

    static boolean active() {
        return new File("fake-gd").exists() && !Loader.isModLoaded("gamedropsbackport");
    }

    @SubscribeEvent
    public static void items(RegistryEvent.Register<Item> event) {
        if (!active()) {
            return;
        }
        event.getRegistry().register(new Item().setRegistryName(new ResourceLocation("gamedropsbackport", "golden_dandelion")));
        event.getRegistry().register(new Item().setRegistryName(new ResourceLocation("gamedropsbackport", "sulfur_spike")));
        explorerMap = new ItemMap() {
        }.setRegistryName(new ResourceLocation("gamedropsbackport", "abandoned_camp_map"));
        event.getRegistry().register(explorerMap);
        VillagerBackport.LOGGER.info("SELFTEST fake Game Drops items registered");
    }
}
