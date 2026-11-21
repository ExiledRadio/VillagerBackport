package com.exiledradio.villagerbackport.babies.client;

import com.exiledradio.villagerbackport.ModConfig;
import com.exiledradio.villagerbackport.VillagerBackport;
import com.exiledradio.villagerbackport.babies.client.model.BabyVillagerModel;
import com.exiledradio.villagerbackport.babies.client.render.BabyHeadLayer;
import com.exiledradio.villagerbackport.babies.client.render.BabyRender;
import com.exiledradio.villagerbackport.babies.client.render.BabyWrapper;
import com.exiledradio.villagerbackport.babies.client.render.OverlayLayer;
import com.exiledradio.villagerbackport.client.VillagerSkin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RenderVillager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 26.1's baby villager: {@code BabyVillagerModel}, {@code villager_baby.png} and the biome's {@code villager/baby}
 * overlay (26.3's {@code VillagerProfessionLayer} gives babies their type and never a profession or badge), with
 * {@code CustomHeadLayer}'s worn blocks and skulls.
 *
 * <p>Wrapped around 1.12's villager renderer only when it is still vanilla's, so a mod that brought its own keeps
 * drawing babies its way; the 1.14 skin layers ({@code LayerInstaller}) stay on that renderer for adults.
 */
@SideOnly(Side.CLIENT)
public final class VillagerBabies {

    /** 26.3's villager types, each with a baby overlay. */
    private static final List<String> TYPES = Arrays.asList("desert", "jungle", "plains", "savanna", "snow", "swamp", "taiga");
    private static final Map<String, ResourceLocation> OVERLAYS = new HashMap<String, ResourceLocation>();

    private VillagerBabies() {
    }

    /** From client post-init, once the renderers exist. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void install() {
        RenderManager rm = Minecraft.getMinecraft().getRenderManager();
        Map<Class<? extends Entity>, Render<? extends Entity>> map = rm.entityRenderMap;
        Render<? extends Entity> current = map.get(EntityVillager.class);
        if (current == null || current.getClass() != RenderVillager.class) {
            VillagerBackport.LOGGER.info("Baby villagers: the villager renderer is {}, not vanilla's; its babies left to it",
                    current == null ? "none" : current.getClass().getName());
            return;
        }
        map.put(EntityVillager.class, new BabyWrapper((Render) current, render(rm)));
    }

    private static BabyRender<EntityVillager> render(RenderManager rm) {
        BabyVillagerModel model = new BabyVillagerModel();
        final ResourceLocation base = BabyRender.tex("villager/villager_baby");
        // VillagerRenderer halves the baby shadow again: 0.5 * 0.5, then 1.12's child half.
        BabyRender<EntityVillager> render = new BabyRender<EntityVillager>(rm, model, 0.25F, new BabyRender.Texture<EntityVillager>() {
            @Override
            public ResourceLocation get(EntityVillager villager) {
                return base;
            }
        }) {
            @Override
            public boolean handles(EntityVillager villager) {
                // The 1.14 look only: with 1.12.2's skins, or a modded villager in its own clothes, the old baby stays.
                return ModConfig.display.useModernVillagerSkins && VillagerSkin.dressesAsVanilla(villager);
            }
        };
        render.addLayer(new BabyHeadLayer(model, -0.1171875F, -0.07421875F, 1.0F));
        render.addLayer(new OverlayLayer<EntityVillager>(render, model, new OverlayLayer.Overlay<EntityVillager>() {
            @Override
            public ResourceLocation texture(EntityVillager villager) {
                return overlay(VillagerSkin.name(VillagerSkin.typeFor(villager)));
            }

            @Override
            public float[] color(EntityVillager villager, float partialTicks) {
                return new float[]{1.0F, 1.0F, 1.0F};
            }
        }, false));
        return render;
    }

    private static ResourceLocation overlay(String type) {
        String name = TYPES.contains(type) ? type : "plains";
        ResourceLocation overlay = OVERLAYS.get(name);
        if (overlay == null) {
            overlay = BabyRender.tex("villager/baby/" + name);
            OVERLAYS.put(name, overlay);
        }
        return overlay;
    }
}
