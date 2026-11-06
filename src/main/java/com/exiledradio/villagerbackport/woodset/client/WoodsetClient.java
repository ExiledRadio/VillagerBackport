package com.exiledradio.villagerbackport.woodset.client;

import com.exiledradio.villagerbackport.VillagerBackport;
import com.exiledradio.villagerbackport.woodset.WoodsetContent;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * The village blocks' item models.
 *
 * <p>Its own class, subscribed on the client only, so a dedicated server verifying
 * {@link WoodsetContent} never meets a client type.
 *
 * <h2>No state mappers</h2>
 * Every block uses the default mapping: each blockstate file lists every combination of every
 * property, in vanilla's own format, exactly as {@code tools/woodset/check_blockstates.py} checks.
 * Each block gets an explicit item model, never a lookup over the model registry's keys, which
 * VintageFix bakes lazily.
 */
@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(modid = VillagerBackport.MOD_ID, value = Side.CLIENT)
public final class WoodsetClient {

    private WoodsetClient() {
    }

    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        if (!WoodsetContent.enabled()) {
            return;
        }
        for (Block block : WoodsetContent.blocks()) {
            ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(block), 0,
                    new ModelResourceLocation(block.getRegistryName(), "inventory"));
        }
    }
}
