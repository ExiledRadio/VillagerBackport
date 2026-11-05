package com.exiledradio.villagerbackport.bamboo.client;

import com.exiledradio.villagerbackport.VillagerBackport;
import com.exiledradio.villagerbackport.bamboo.BambooContent;
import com.exiledradio.villagerbackport.bamboo.EntityPanda;

import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * The client half of the bamboo stream: item models and the panda's renderer.
 *
 * <p>Kept in a class of its own under {@code client/} for the reason given on
 * {@link com.exiledradio.villagerbackport.client.ClientSetup}: a server must never be asked to load
 * any of these types. FML subscribes this class on the client only.
 */
@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(modid = VillagerBackport.MOD_ID, value = Side.CLIENT)
public final class BambooClient {

    private BambooClient() {
    }

    /** Called from {@code ClientSetup.init}, during pre-initialization. */
    public static void init() {
        if (BambooContent.enabled()) {
            RenderingRegistry.registerEntityRenderingHandler(EntityPanda.class, RenderPanda::new);
        }
    }

    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        model(BambooContent.bambooItem);
        model(BambooContent.scaffoldingItem);
    }

    private static void model(Item item) {
        if (item != null) {
            ModelLoader.setCustomModelResourceLocation(item, 0, new ModelResourceLocation(item.getRegistryName(), "inventory"));
        }
    }
}
