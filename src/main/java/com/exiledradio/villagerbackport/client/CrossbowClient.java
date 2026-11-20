package com.exiledradio.villagerbackport.client;

import com.exiledradio.villagerbackport.crossbow.EntityCrossbowArrow;
import com.exiledradio.villagerbackport.crossbow.EntityCrossbowFirework;
import com.exiledradio.villagerbackport.crossbow.EntityCrossbowSpectralArrow;
import com.exiledradio.villagerbackport.crossbow.ItemCrossbow;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.entity.RenderSnowball;
import net.minecraft.client.renderer.entity.RenderSpectralArrow;
import net.minecraft.client.renderer.entity.RenderTippedArrow;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumHandSide;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.RenderSpecificHandEvent;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * The crossbow on a client: its projectiles' renderers, and how a player holds it loaded.
 *
 * <h2>The pose</h2>
 * 1.14 holds a loaded crossbow up to the shoulder, aimed. 1.12.2's player model has no such pose and
 * picks its own arm poses on every frame, so the nearest it has - the bow's draw, both arms raised
 * along the line of sight - is put on the model after it has chosen and before it draws.
 * {@code RenderLivingEvent.Pre} fires at exactly that point. Armour copies the model's poses, so it
 * follows. Drawing the crossbow already looks like this, as it uses the bow's pose.
 *
 * <h2>In first person</h2>
 * 1.12.2 would hold it as a bow: drawn with the bow's pull, then flat in the hand. 1.14 has a
 * crossbow branch of its own - turned across the body while loading, swung in to the centre once
 * loaded - and only draws the other hand when it would not be in the way. Forge's
 * {@code RenderSpecificHandEvent} lets a hand be drawn in place of the game's, so that branch is
 * copied here, with 1.14.4's numbers, around 1.12.2's own item drawing.
 */
@SideOnly(Side.CLIENT)
public final class CrossbowClient {

    static void registerRenderers() {
        RenderingRegistry.registerEntityRenderingHandler(EntityCrossbowArrow.class, RenderTippedArrow::new);
        RenderingRegistry.registerEntityRenderingHandler(EntityCrossbowSpectralArrow.class, RenderSpectralArrow::new);
        RenderingRegistry.registerEntityRenderingHandler(EntityCrossbowFirework.class,
                manager -> new RenderSnowball<EntityCrossbowFirework>(manager, Items.FIREWORKS,
                        Minecraft.getMinecraft().getRenderItem()) {
                    @Override
                    public ItemStack getStackToRender(EntityCrossbowFirework entity) {
                        return entity.getRocket();
                    }
                });
    }

    /** The crossbow is held close to the camera, where 1.12.2's per-pixel item edges show seams; close them. */
    @SubscribeEvent
    public void onModelBake(net.minecraftforge.client.event.ModelBakeEvent event) {
        ItemCrossbow item = com.exiledradio.villagerbackport.crossbow.CrossbowContent.crossbow;
        if (item == null || item.getRegistryName() == null) {
            return;
        }
        net.minecraft.client.renderer.block.model.ModelResourceLocation key =
                new net.minecraft.client.renderer.block.model.ModelResourceLocation(item.getRegistryName(), "inventory");
        net.minecraft.client.renderer.block.model.IBakedModel model = event.getModelRegistry().getObject(key);
        if (model != null && !(model instanceof SeamlessItemModel)) {
            event.getModelRegistry().putObject(key, new SeamlessItemModel(model));
        }
    }

    @SubscribeEvent
    public void onRenderHand(RenderSpecificHandEvent event) {
        EntityPlayerSP player = Minecraft.getMinecraft().player;
        if (player == null || !shown(player, event.getHand())) {
            event.setCanceled(true);
            return;
        }
        ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof ItemCrossbow)) {
            return;
        }
        event.setCanceled(true);
        EnumHand hand = event.getHand();
        EnumHandSide side = hand == EnumHand.MAIN_HAND ? player.getPrimaryHand() : player.getPrimaryHand().opposite();
        int i = side == EnumHandSide.RIGHT ? 1 : -1;
        float swing = event.getSwingProgress();
        float equip = event.getEquipProgress();

        // 1.14's use lasts the charge time and three ticks more, then runs on past zero until let go,
        // and only the first part is drawn as loading: a drawn crossbow rests in the hand. 1.12.2 would
        // end a use at zero, so this mod's lasts far longer and the count 1.14 would see is worked out.
        int used = stack.getMaxItemUseDuration() - player.getItemInUseCount();
        boolean loading = used < ItemCrossbow.chargeTicks(stack) + 3;

        GlStateManager.pushMatrix();
        if (player.isHandActive() && loading && player.getActiveHand() == hand) {
            // Loading: turned across the body, pulled back a little as it draws.
            transformSide(i, equip);
            GlStateManager.translate(i * -0.4785682F, -0.094387F, 0.05731531F);
            GlStateManager.rotate(-11.935F, 1.0F, 0.0F, 0.0F);
            GlStateManager.rotate(i * 65.3F, 0.0F, 1.0F, 0.0F);
            GlStateManager.rotate(i * -9.785F, 0.0F, 0.0F, 1.0F);
            float drawn = stack.getMaxItemUseDuration() - (player.getItemInUseCount() - event.getPartialTicks() + 1.0F);
            float pull = Math.min(drawn / Math.max(1, ItemCrossbow.chargeTicks(stack)), 1.0F);
            if (pull > 0.1F) {
                float shake = MathHelper.sin((drawn - 0.1F) * 1.3F) * (pull - 0.1F);
                GlStateManager.translate(0.0F, shake * 0.004F, 0.0F);
            }
            GlStateManager.translate(0.0F, 0.0F, pull * 0.04F);
            GlStateManager.scale(1.0F, 1.0F, 1.0F + pull * 0.2F);
            GlStateManager.rotate(i * 45.0F, 0.0F, -1.0F, 0.0F);
        } else {
            float x = -0.4F * MathHelper.sin(MathHelper.sqrt(swing) * (float) Math.PI);
            float y = 0.2F * MathHelper.sin(MathHelper.sqrt(swing) * ((float) Math.PI * 2.0F));
            float z = -0.2F * MathHelper.sin(swing * (float) Math.PI);
            GlStateManager.translate(i * x, y, z);
            transformSide(i, equip);
            transformSwing(i, swing);
            if (ItemCrossbow.isCharged(stack) && swing < 0.001F) {
                // Loaded: swung in to aim down the middle of the screen.
                GlStateManager.translate(i * -0.641864F, 0.0F, 0.0F);
                GlStateManager.rotate(i * 10.0F, 0.0F, 1.0F, 0.0F);
            }
        }
        boolean right = side == EnumHandSide.RIGHT;
        Minecraft.getMinecraft().getItemRenderer().renderItemSide(player, stack,
                right ? ItemCameraTransforms.TransformType.FIRST_PERSON_RIGHT_HAND
                        : ItemCameraTransforms.TransformType.FIRST_PERSON_LEFT_HAND, !right);
        GlStateManager.popMatrix();
    }

    /**
     * 1.14's choice of hands: while loading only the crossbow's hand is drawn, and a loaded crossbow
     * hides the other hand - from the off hand, only when the main hand is empty.
     */
    private static boolean shown(EntityPlayerSP player, EnumHand hand) {
        if (player.isHandActive()) {
            return !(player.getActiveItemStack().getItem() instanceof ItemCrossbow) || player.getActiveHand() == hand;
        }
        ItemStack main = player.getHeldItemMainhand();
        ItemStack off = player.getHeldItemOffhand();
        if (main.getItem() instanceof ItemCrossbow && ItemCrossbow.isCharged(main)) {
            return hand == EnumHand.MAIN_HAND;
        }
        if (off.getItem() instanceof ItemCrossbow && ItemCrossbow.isCharged(off)) {
            return (hand == EnumHand.MAIN_HAND) == !main.isEmpty();
        }
        return true;
    }

    /** {@code ItemRenderer.transformSideFirstPerson}, private in 1.12.2. */
    private static void transformSide(int i, float equip) {
        GlStateManager.translate(i * 0.56F, -0.52F + equip * -0.6F, -0.72F);
    }

    /** {@code ItemRenderer.transformFirstPerson}, private in 1.12.2. */
    private static void transformSwing(int i, float swing) {
        float f = MathHelper.sin(swing * swing * (float) Math.PI);
        GlStateManager.rotate(i * (45.0F + f * -20.0F), 0.0F, 1.0F, 0.0F);
        float f1 = MathHelper.sin(MathHelper.sqrt(swing) * (float) Math.PI);
        GlStateManager.rotate(i * f1 * -20.0F, 0.0F, 0.0F, 1.0F);
        GlStateManager.rotate(f1 * -80.0F, 1.0F, 0.0F, 0.0F);
        GlStateManager.rotate(i * -45.0F, 0.0F, 1.0F, 0.0F);
    }

    @SubscribeEvent
    public void onRenderLiving(RenderLivingEvent.Pre<EntityLivingBase> event) {
        EntityLivingBase entity = event.getEntity();
        if (!(entity instanceof EntityPlayer) || !(event.getRenderer().getMainModel() instanceof ModelBiped)) {
            return;
        }
        ModelBiped model = (ModelBiped) event.getRenderer().getMainModel();
        for (EnumHand hand : EnumHand.values()) {
            ItemStack held = entity.getHeldItem(hand);
            if (!(held.getItem() instanceof ItemCrossbow) || !ItemCrossbow.isCharged(held)) {
                continue;
            }
            EnumHandSide side = hand == EnumHand.MAIN_HAND ? entity.getPrimaryHand() : entity.getPrimaryHand().opposite();
            if (side == EnumHandSide.RIGHT) {
                model.rightArmPose = ModelBiped.ArmPose.BOW_AND_ARROW;
            } else {
                model.leftArmPose = ModelBiped.ArmPose.BOW_AND_ARROW;
            }
        }
    }
}
