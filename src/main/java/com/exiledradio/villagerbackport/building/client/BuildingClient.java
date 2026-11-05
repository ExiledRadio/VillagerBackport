package com.exiledradio.villagerbackport.building.client;

import com.exiledradio.villagerbackport.VillagerBackport;
import com.exiledradio.villagerbackport.building.BlockBuildingSlab;
import com.exiledradio.villagerbackport.building.BuildingContent;
import com.exiledradio.villagerbackport.building.ItemWoodSign;
import com.exiledradio.villagerbackport.building.TileEntityWoodSign;

import net.minecraft.block.Block;
import net.minecraft.block.BlockStandingSign;
import net.minecraft.block.BlockWall;
import net.minecraft.block.BlockWallSign;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.statemap.StateMap;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * The building blocks' models and the wood signs' renderer.
 *
 * <p>Its own class, subscribed on the client only, for the reason {@code ClientSetup} explains: a
 * server verifying {@link BuildingContent} must never meet a client type in it.
 *
 * <h2>State mappers</h2>
 * Each mapper drops a property the blockstate files have no use for, so the files stay what 1.14's
 * look like:
 * <ul>
 *   <li>slabs: the one-value {@code variant} {@link BlockBuildingSlab} needs for {@code ItemSlab};</li>
 *   <li>walls: vanilla's cobblestone/mossy {@code variant}, which these never change;</li>
 *   <li>signs: their rotation and facing, since a sign is drawn by its renderer and its block model
 *       is only there to name the particle texture.</li>
 * </ul>
 * Every block gets an explicit mapping, never a lookup over the model registry's keys, which
 * VintageFix bakes lazily.
 */
@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(modid = VillagerBackport.MOD_ID, value = Side.CLIENT)
public final class BuildingClient {

    private BuildingClient() {
    }

    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        if (!BuildingContent.enabled()) {
            return;
        }

        for (Block slab : BuildingContent.slabs()) {
            ModelLoader.setCustomStateMapper(slab, new StateMap.Builder().ignore(BlockBuildingSlab.VARIANT).build());
        }
        for (Block block : BuildingContent.blocksWithItems()) {
            if (block instanceof BlockWall) {
                ModelLoader.setCustomStateMapper(block, new StateMap.Builder().ignore(BlockWall.VARIANT).build());
            }
            ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(block), 0,
                    new ModelResourceLocation(block.getRegistryName(), "inventory"));
        }

        for (Block sign : BuildingContent.standingSigns().values()) {
            ModelLoader.setCustomStateMapper(sign, new StateMap.Builder().ignore(BlockStandingSign.ROTATION).build());
        }
        for (Block sign : BuildingContent.wallSigns().values()) {
            ModelLoader.setCustomStateMapper(sign, new StateMap.Builder().ignore(BlockWallSign.FACING).build());
        }
        for (ItemWoodSign item : BuildingContent.signItems().values()) {
            ModelLoader.setCustomModelResourceLocation(item, 0,
                    new ModelResourceLocation(item.getRegistryName(), "inventory"));
        }

        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityWoodSign.class, new TileEntityWoodSignRenderer());
    }
}
