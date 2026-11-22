package com.exiledradio.villagerbackport.selftest;

import com.exiledradio.villagerbackport.VillagerBackport;

import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.io.File;

/**
 * Test only, never shipped: stands in for Tricky Trials Backport's ominous bottle ({@code trickytrialsbackport:ominous_bottle},
 * metadata 0-4) in this mod's dev environment, which doesn't have that mod. Only when the file {@code fake-tt} exists in
 * the run directory and the real mod is not loaded.
 */
@Mod.EventBusSubscriber(modid = VillagerBackport.MOD_ID)
public final class FakeTrickyTrials {

    static Item bottle;

    private FakeTrickyTrials() {
    }

    static boolean active() {
        return new File("fake-tt").exists() && !Loader.isModLoaded("trickytrialsbackport");
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        if (!active()) {
            return;
        }
        bottle = new Item().setHasSubtypes(true).setRegistryName(new ResourceLocation("trickytrialsbackport", "ominous_bottle"));
        event.getRegistry().register(bottle);
        VillagerBackport.LOGGER.info("SELFTEST fake Tricky Trials ominous bottle registered");
    }
}
