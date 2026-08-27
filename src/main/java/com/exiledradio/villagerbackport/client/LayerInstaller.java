package com.exiledradio.villagerbackport.client;

import com.exiledradio.villagerbackport.VillagerBackport;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.lang.reflect.Field;
import java.util.List;

/**
 * Attaches the level badge layer to whatever villager renderer is actually in use.
 *
 * <h2>Why this is done by hand</h2>
 * {@code RenderLivingBase.addLayer} is protected, so a layer can normally only be added from inside
 * a renderer - which would mean shipping our own and registering it in place of the existing one.
 * That is exactly what should not happen here: MoBends, iChunUtil and Classy Hats all supply or
 * decorate villager renderers, and replacing the registered renderer would silently discard whoever
 * registered before us. Appending to the live renderer's layer list instead leaves every one of them
 * intact and adds a single extra pass.
 *
 * <h2>Why on first render rather than at load</h2>
 * Renderers are built during client startup and can be replaced by other mods afterwards, so there
 * is no load-time moment that is reliably after everyone else. Waiting until a villager is actually
 * drawn sidesteps the ordering question entirely: whatever renderer is in use by then is the real
 * one. The work happens once and the handler is a no-op from then on.
 */
@SideOnly(Side.CLIENT)
public final class LayerInstaller {

    private static boolean installed;

    /**
     * Runs at the lowest priority so any mod that swaps the renderer during this event has already
     * done so, and the layer lands on the renderer that will actually draw.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onRenderLiving(RenderLivingEvent.Pre<?> event) {
        if (installed || !(event.getEntity() instanceof EntityVillager)) {
            return;
        }

        // Set before attempting, so a failure is not retried on every villager on every frame.
        installed = true;

        try {
            install(Minecraft.getMinecraft().getRenderManager()
                    .getEntityClassRenderObject(EntityVillager.class));
        } catch (RuntimeException e) {
            VillagerBackport.LOGGER.error(
                    "Could not attach the villager level badge layer. Trading is unaffected; "
                            + "villagers simply will not show their rank.", e);
        }
    }

    @SuppressWarnings("unchecked")
    private void install(Render<?> render) {
        if (!(render instanceof RenderLivingBase)) {
            VillagerBackport.LOGGER.warn(
                    "The villager renderer is a {}, which has no layers to add to. Skipping the "
                            + "level badge.", render == null ? "null" : render.getClass().getName());
            return;
        }

        RenderLivingBase<?> living = (RenderLivingBase<?>) render;

        Field field = ReflectionHelper.findField(RenderLivingBase.class, "layerRenderers", "field_177097_h");
        field.setAccessible(true);

        List<LayerRenderer<?>> layers;
        try {
            layers = (List<LayerRenderer<?>>) field.get(living);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        // One layer, not two. The rank badge is composited into the same texture as the outfit, so
        // there is no second pass to order against the first.
        layers.add((LayerRenderer) new LayerVillagerSkin(living));

        // Only now that a villager is actually being rendered is it safe to blank the vanilla
        // textures: the texture manager is up and every mod has finished registering professions.
        VanillaSkinSuppressor.apply();

        VillagerBackport.LOGGER.info("Villager skin layer attached to {}.", living.getClass().getName());
    }
}
