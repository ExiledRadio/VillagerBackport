package com.exiledradio.villagerbackport.babies.client.render;

import com.exiledradio.villagerbackport.babies.client.model.HeadedModel;
import com.mojang.authlib.GameProfile;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.client.renderer.tileentity.TileEntitySkullRenderer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Items;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.tileentity.TileEntitySkull;
import net.minecraft.util.EnumFacing;

import org.apache.commons.lang3.StringUtils;

import java.util.UUID;

/**
 * Blocks and skulls worn on the head: 1.12's {@code LayerCustomHead} placed by 26.3's {@code CustomHeadLayer}
 * (its {@code Transforms}, and no extra baby shrinking: the baby head is already baby sized).
 */
public final class BabyHeadLayer implements LayerRenderer<EntityLivingBase> {

    private final HeadedModel model;
    private final float yOffset;
    private final float skullYOffset;
    private final float horizontalScale;

    /** {@code CustomHeadLayer.Transforms}: (0, 0, 1) by default, villagers (-0.1171875, -0.07421875, 1). */
    public BabyHeadLayer(HeadedModel model, float yOffset, float skullYOffset, float horizontalScale) {
        this.model = model;
        this.yOffset = yOffset;
        this.skullYOffset = skullYOffset;
        this.horizontalScale = horizontalScale;
    }

    @Override
    public void doRenderLayer(EntityLivingBase entity, float limbSwing, float limbSwingAmount, float partialTicks,
            float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        ItemStack stack = entity.getItemStackFromSlot(EntityEquipmentSlot.HEAD);
        if (stack.isEmpty()) {
            return;
        }
        Item item = stack.getItem();
        boolean skull = item == Items.SKULL;
        if (!skull && item instanceof ItemArmor && ((ItemArmor) item).getEquipmentSlot() == EntityEquipmentSlot.HEAD) {
            return;
        }
        GlStateManager.pushMatrix();
        GlStateManager.scale(horizontalScale, 1.0F, horizontalScale);
        model.getHead().translateAndRotate(0.0625F);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        if (skull) {
            GlStateManager.translate(0.0F, skullYOffset, 0.0F);
            GlStateManager.scale(1.1875F, -1.1875F, -1.1875F);
            TileEntitySkullRenderer.instance.renderSkull(-0.5F, 0.0F, -0.5F, EnumFacing.UP, 180.0F, stack.getMetadata(),
                    profile(stack), -1, limbSwing);
        } else {
            GlStateManager.translate(0.0F, -0.25F + yOffset, 0.0F);
            GlStateManager.rotate(180.0F, 0.0F, 1.0F, 0.0F);
            GlStateManager.scale(0.625F, -0.625F, -0.625F);
            Minecraft.getMinecraft().getItemRenderer().renderItem(entity, stack, ItemCameraTransforms.TransformType.HEAD);
        }
        GlStateManager.popMatrix();
    }

    /** The skull owner, as 1.12's {@code LayerCustomHead} resolves it. */
    private static GameProfile profile(ItemStack stack) {
        if (!stack.hasTagCompound()) {
            return null;
        }
        NBTTagCompound tag = stack.getTagCompound();
        if (tag.hasKey("SkullOwner", 10)) {
            return NBTUtil.readGameProfileFromNBT(tag.getCompoundTag("SkullOwner"));
        }
        if (tag.hasKey("SkullOwner", 8)) {
            String name = tag.getString("SkullOwner");
            if (!StringUtils.isBlank(name)) {
                GameProfile profile = TileEntitySkull.updateGameProfile(new GameProfile((UUID) null, name));
                tag.setTag("SkullOwner", NBTUtil.writeGameProfile(new NBTTagCompound(), profile));
                return profile;
            }
        }
        return null;
    }

    @Override
    public boolean shouldCombineTextures() {
        return false;
    }
}
