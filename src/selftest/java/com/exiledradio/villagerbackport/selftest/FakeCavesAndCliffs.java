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
 * Test only, never shipped: stands in for the Caves &amp; Cliffs Backport item a villager trades by name
 * ({@code cavesandcliffsbackport:dripstone_block}, the mason's), in a dev environment without that mod. Only when the
 * file {@code fake-cc} exists in the run directory and the real mod is not loaded.
 */
@Mod.EventBusSubscriber(modid = VillagerBackport.MOD_ID)
public final class FakeCavesAndCliffs {

    /** The mason's dripstone block, then what the wandering trader sells. */
    static final String[] ITEMS = {"dripstone_block", "small_dripleaf", "rooted_dirt", "moss_block"};

    private FakeCavesAndCliffs() {
    }

    static boolean active() {
        return new File("fake-cc").exists() && !Loader.isModLoaded("cavesandcliffsbackport");
    }

    @SubscribeEvent
    public static void items(RegistryEvent.Register<Item> event) {
        if (active()) {
            for (String name : ITEMS) {
                event.getRegistry().register(new Item().setRegistryName(new ResourceLocation("cavesandcliffsbackport", name)));
            }
            VillagerBackport.LOGGER.info("SELFTEST fake Caves & Cliffs items registered");
        }
    }
}
