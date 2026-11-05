package com.exiledradio.villagerbackport.trader.client;

import com.exiledradio.villagerbackport.VillagerBackport;
import com.exiledradio.villagerbackport.trader.EntityTraderLlama;
import com.exiledradio.villagerbackport.trader.EntityWanderingTrader;
import com.exiledradio.villagerbackport.trader.TraderContent;

import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

/**
 * The trader's client side, in a class only a client loads.
 *
 * <p>Renderers are registered from {@link ModelRegistryEvent}, which a client fires during
 * pre-initialization, while {@code RenderingRegistry} still takes factories - so the mod class and
 * {@code ClientSetup} need no changes, and a dedicated server never sees a client type.
 */
@Mod.EventBusSubscriber(modid = VillagerBackport.MOD_ID, value = Side.CLIENT)
public final class TraderClient {

    private TraderClient() {
    }

    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        if (!TraderContent.enabled()) {
            return;
        }
        RenderingRegistry.registerEntityRenderingHandler(EntityWanderingTrader.class, RenderWanderingTrader::new);
        RenderingRegistry.registerEntityRenderingHandler(EntityTraderLlama.class, RenderTraderLlama::new);
    }
}
