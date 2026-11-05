package com.exiledradio.villagerbackport.cat.client;

import com.exiledradio.villagerbackport.VillagerBackport;
import com.exiledradio.villagerbackport.cat.CatContent;
import com.exiledradio.villagerbackport.cat.EntityCat;

import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * The cat's client half, in a class only a client loads (see
 * {@link com.exiledradio.villagerbackport.client.ClientSetup} for why that matters).
 *
 * <p>The renderer is registered from the model event, which a client fires during pre-initialization,
 * before Minecraft builds its entity renderers from the factories registered so far.
 */
@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(modid = VillagerBackport.MOD_ID, value = Side.CLIENT)
public final class CatClient {

    private CatClient() {
    }

    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        if (CatContent.enabled()) {
            RenderingRegistry.registerEntityRenderingHandler(EntityCat.class, RenderCat::new);
        }
    }
}
