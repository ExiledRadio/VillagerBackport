package com.exiledradio.villagerbackport.flora.client;

import com.exiledradio.villagerbackport.VillagerBackport;
import com.exiledradio.villagerbackport.flora.FloraContent;
import com.exiledradio.villagerbackport.flora.ItemLeatherHorseArmor;
import com.exiledradio.villagerbackport.flora.Mooshrooms;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.color.IItemColor;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.entity.passive.EntityMooshroom;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraftforge.client.event.ColorHandlerEvent;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.lang.reflect.Field;
import java.util.List;

/**
 * Stream F's client side: item models, the leather horse armor's tint, the dyed sign renderer, the
 * brown mooshroom's look, and the layer that draws leather horse armor on horses.
 *
 * <p>A class of its own under {@code client/}, subscribed only on a client, so a dedicated server
 * never loads any of the client types it names.
 *
 * <h2>Brown mooshrooms</h2>
 * Whatever renderer draws mooshrooms - vanilla's, or one another mod registered - keeps drawing the
 * red ones. A brown one is drawn by {@link RenderBrownMooshroom} instead: the event that would start
 * the usual renderer is cancelled and this one is run in its place, with the same position, so
 * nothing about red mooshrooms or other mods' renderers is replaced.
 *
 * <h2>Leather horse armor</h2>
 * Its layer is appended to the live horse renderer on the first horse drawn, the way this mod's
 * villager layers are (see {@code client.LayerInstaller}): whatever renderer is in use by then is
 * the real one.
 */
@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(modid = VillagerBackport.MOD_ID, value = Side.CLIENT)
public final class FloraClient {

    private static RenderBrownMooshroom brownMooshroom;
    private static boolean drawingBrown;
    private static boolean horseLayerInstalled;

    private FloraClient() {
    }

    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        if (!FloraContent.enabled()) {
            return;
        }
        for (Item item : FloraContent.items()) {
            ModelLoader.setCustomModelResourceLocation(item, 0,
                    new ModelResourceLocation(item.getRegistryName(), "inventory"));
        }

        // Replaces 1.12.2's sign renderer, which it extends: an undyed sign is drawn by vanilla's code.
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntitySign.class, new TileEntityDyedSignRenderer());
    }

    @SubscribeEvent
    public static void registerItemColors(ColorHandlerEvent.Item event) {
        if (FloraContent.leatherHorseArmor == null) {
            return;
        }
        event.getItemColors().registerItemColorHandler(new IItemColor() {
            @Override
            public int colorMultiplier(ItemStack stack, int tintIndex) {
                return tintIndex == 0 ? ItemLeatherHorseArmor.getColor(stack) : -1;
            }
        }, FloraContent.leatherHorseArmor);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderLiving(RenderLivingEvent.Pre<?> event) {
        EntityLivingBase entity = event.getEntity();

        if (entity instanceof EntityHorse && !horseLayerInstalled) {
            horseLayerInstalled = true;
            installHorseLayer(Minecraft.getMinecraft().getRenderManager().getEntityRenderObject(entity));
            return;
        }

        if (!(entity instanceof EntityMooshroom) || drawingBrown || !Mooshrooms.isBrown(entity)
                || !FloraContent.enabled() || event.isCanceled()) {
            return;
        }

        if (brownMooshroom == null) {
            brownMooshroom = new RenderBrownMooshroom(Minecraft.getMinecraft().getRenderManager());
        }

        event.setCanceled(true);
        float partialTicks = event.getPartialRenderTick();
        float yaw = entity.prevRotationYaw + (entity.rotationYaw - entity.prevRotationYaw) * partialTicks;
        drawingBrown = true;
        try {
            brownMooshroom.doRender((EntityMooshroom) entity, event.getX(), event.getY(), event.getZ(), yaw, partialTicks);
        } finally {
            drawingBrown = false;
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void installHorseLayer(Render<?> render) {
        if (!(render instanceof RenderLivingBase) || FloraContent.leatherHorseArmor == null) {
            return;
        }
        try {
            Field field = ReflectionHelper.findField(RenderLivingBase.class, "layerRenderers", "field_177097_h");
            field.setAccessible(true);
            List<LayerRenderer<?>> layers = (List<LayerRenderer<?>>) field.get(render);
            layers.add((LayerRenderer) new LayerLeatherHorseArmor((RenderLivingBase<?>) render));
            VillagerBackport.LOGGER.info("Leather horse armor layer attached to {}.", render.getClass().getName());
        } catch (Exception e) {
            VillagerBackport.LOGGER.error("Could not attach the leather horse armor layer; it will not be drawn.", e);
        }
    }

    /** Called with what the server says about a mooshroom's color. */
    public static void acceptMooshroom(int entityId, boolean brown) {
        if (Minecraft.getMinecraft().world == null) {
            return;
        }
        net.minecraft.entity.Entity entity = Minecraft.getMinecraft().world.getEntityByID(entityId);
        if (entity instanceof EntityMooshroom) {
            Mooshrooms.setBrownClient(entity, brown);
        }
    }
}
