package com.exiledradio.villagerbackport.campfire.client;

import com.exiledradio.villagerbackport.VillagerBackport;
import com.exiledradio.villagerbackport.campfire.BlockCampfire;
import com.exiledradio.villagerbackport.campfire.CampfireContent;
import com.exiledradio.villagerbackport.campfire.TileEntityCampfire;

import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.statemap.StateMap;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * The campfire's client setup: its models, the renderer for the food on it, and the smoke textures.
 *
 * <p>Subscribed for the client side only, so a dedicated server never loads it. Everything is done
 * from the model event rather than from {@code ClientSetup}, which keeps this stream's client code in
 * its own package.
 *
 * <h2>Smoke textures</h2>
 * 1.14's twelve {@code big_smoke} frames are stitched into the block atlas, which is the one texture
 * sheet 1.12.2's particle manager will draw a mod's particle from (layer 1). Each plume picks one frame
 * at random and keeps it, as 1.14's {@code pickSprite} does.
 */
@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(modid = VillagerBackport.MOD_ID, value = Side.CLIENT)
public final class CampfireClient {

    static final TextureAtlasSprite[] SMOKE = new TextureAtlasSprite[12];

    private CampfireClient() {
    }

    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        BlockCampfire campfire = CampfireContent.campfire;
        if (campfire == null) {
            return;
        }
        // The hay underneath changes the smoke, not the logs.
        ModelLoader.setCustomStateMapper(campfire, new StateMap.Builder().ignore(BlockCampfire.SIGNAL_FIRE).build());
        ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(campfire), 0,
                new ModelResourceLocation(campfire.getRegistryName(), "inventory"));
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityCampfire.class, new TileEntityCampfireRenderer());
    }

    @SubscribeEvent
    public static void stitch(TextureStitchEvent.Pre event) {
        if (CampfireContent.campfire == null) {
            return;
        }
        for (int i = 0; i < SMOKE.length; i++) {
            SMOKE[i] = event.getMap().registerSprite(
                    new ResourceLocation(VillagerBackport.MOD_ID, "particle/big_smoke_" + i));
        }
    }
}
