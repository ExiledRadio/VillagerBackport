package com.exiledradio.villagerbackport.fox.client;

import com.exiledradio.villagerbackport.VillagerBackport;
import com.exiledradio.villagerbackport.fox.EntityFox;
import com.exiledradio.villagerbackport.fox.FoxContent;

import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * The fox's renderer and the berries' item model, registered from a client-only class so that a
 * dedicated server never loads a type it does not have.
 *
 * <p>The model event fires before the render manager is built, which is when Forge turns the
 * registered factories into renderers, so registering the fox's here is in time.
 */
@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(modid = VillagerBackport.MOD_ID, value = Side.CLIENT)
public final class FoxClient {

    private FoxClient() {
    }

    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        if (FoxContent.sweetBerries == null) {
            return;
        }
        ModelLoader.setCustomModelResourceLocation(FoxContent.sweetBerries, 0,
                new ModelResourceLocation(FoxContent.sweetBerries.getRegistryName(), "inventory"));
        RenderingRegistry.registerEntityRenderingHandler(EntityFox.class, RenderFox::new);
    }
}
