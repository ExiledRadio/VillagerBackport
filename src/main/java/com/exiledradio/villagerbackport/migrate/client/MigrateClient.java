package com.exiledradio.villagerbackport.migrate.client;

import com.exiledradio.villagerbackport.VillagerBackport;
import com.exiledradio.villagerbackport.migrate.ItemFutureMcStandIn;

import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.Side;

/**
 * Draws each stand-in for a removed Future MC item as what it stands for, kept sea pickles as sea pickles and old
 * ominous bottles as ominous bottles.
 */
@Mod.EventBusSubscriber(modid = VillagerBackport.MOD_ID, value = Side.CLIENT)
public final class MigrateClient {

    private MigrateClient() {
    }

    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        Item pickle = com.exiledradio.villagerbackport.migrate.UpdateAquaticPickles.standIn();
        if (pickle != null) {
            ModelLoader.setCustomModelResourceLocation(pickle, 0,
                    new ModelResourceLocation(VillagerBackport.MOD_ID + ":sea_pickle", "inventory"));
        }
        Item bottle = com.exiledradio.villagerbackport.migrate.OminousBottles.standIn();
        if (bottle != null) {
            for (int level = 0; level < com.exiledradio.villagerbackport.migrate.OminousBottles.LEVELS; level++) {
                ModelLoader.setCustomModelResourceLocation(bottle, level,
                        new ModelResourceLocation(VillagerBackport.MOD_ID + ":ominous_bottle", "inventory"));
            }
        }
        for (Item item : ForgeRegistries.ITEMS) {
            if (!(item instanceof ItemFutureMcStandIn)) {
                continue;
            }
            ItemFutureMcStandIn standIn = (ItemFutureMcStandIn) item;
            int metas = "dye".equals(standIn.futureMcName()) ? 4 : 1;
            for (int meta = 0; meta < metas; meta++) {
                ItemStack real = ItemFutureMcStandIn.real(standIn.futureMcName(), meta, 1);
                if (real.isEmpty() || real.getItem().getRegistryName() == null) {
                    continue;
                }
                // This mod's dyes are drawn by their own name; 1.12.2's logs by their wood's ("spruce_log").
                String model = real.getItem() instanceof net.minecraft.item.ItemBlock
                        ? "minecraft:" + standIn.futureMcName().replace("_wood", "_log")
                        : real.getItem().getRegistryName().toString();
                ModelLoader.setCustomModelResourceLocation(standIn, meta, new ModelResourceLocation(model, "inventory"));
            }
        }
    }
}
