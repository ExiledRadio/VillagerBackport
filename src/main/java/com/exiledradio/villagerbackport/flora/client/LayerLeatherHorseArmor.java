package com.exiledradio.villagerbackport.flora.client;

import com.exiledradio.villagerbackport.VillagerBackport;
import com.exiledradio.villagerbackport.flora.ItemLeatherHorseArmor;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.item.ItemStack;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.lang.reflect.Field;

/**
 * Draws leather horse armor over a horse, in its dye color.
 *
 * <p>1.12.2 bakes horse armor into the horse's texture, which cannot be tinted, so leather horse
 * armor opts out of that (see {@link ItemLeatherHorseArmor#getHorseArmorTexture}) and is drawn here
 * instead: the horse's own model drawn a second time with a grey armor texture, colored by the dye.
 * The texture is 1.12.2's iron horse armor redrawn in grey, so it fits 1.12.2's horse model exactly.
 *
 * <p>The armor a horse wears reaches the client through Forge's {@code HORSE_ARMOR_STACK} entry in
 * the horse's synced data, which has no public getter, so it is read through that field.
 */
@SideOnly(Side.CLIENT)
public class LayerLeatherHorseArmor implements LayerRenderer<EntityLivingBase> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(VillagerBackport.MOD_ID, "textures/entity/horse/armor/horse_armor_leather.png");

    @Nullable
    private static DataParameter<ItemStack> armorStack;
    private static boolean looked;

    private final RenderLivingBase<?> renderer;

    public LayerLeatherHorseArmor(RenderLivingBase<?> renderer) {
        this.renderer = renderer;
    }

    @Override
    public void doRenderLayer(EntityLivingBase entity, float limbSwing, float limbSwingAmount, float partialTicks,
                              float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        // 1.14 draws horse armor as a layer, so an invisible horse's armor shows (mechanics.horseArmorLayer).
        boolean modern = com.exiledradio.villagerbackport.mechanics.client.HorseArmorClient.active();
        if (!(entity instanceof EntityHorse) || entity.isInvisible() && !modern) {
            return;
        }

        ItemStack armor = armor((EntityHorse) entity);
        if (!(armor.getItem() instanceof ItemLeatherHorseArmor)) {
            // Iron, gold and diamond, which mechanics.client.HorseArmorClient took out of the baked texture.
            ResourceLocation texture = com.exiledradio.villagerbackport.mechanics.client.HorseArmorClient.texture(armor);
            if (texture != null) {
                this.renderer.bindTexture(texture);
                GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                this.renderer.getMainModel().render(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
            }
            return;
        }

        int color = ItemLeatherHorseArmor.getColor(armor);
        this.renderer.bindTexture(TEXTURE);
        GlStateManager.color((color >> 16 & 255) / 255.0F, (color >> 8 & 255) / 255.0F, (color & 255) / 255.0F, 1.0F);
        this.renderer.getMainModel().render(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    @SuppressWarnings("unchecked")
    private static ItemStack armor(EntityHorse horse) {
        if (!looked) {
            looked = true;
            try {
                Field field = ReflectionHelper.findField(EntityHorse.class, "HORSE_ARMOR_STACK");
                field.setAccessible(true);
                armorStack = (DataParameter<ItemStack>) field.get(null);
            } catch (Exception e) {
                VillagerBackport.LOGGER.error("Could not find the horse armor data entry; leather horse armor will not be drawn.", e);
            }
        }
        return armorStack == null ? ItemStack.EMPTY : horse.getDataManager().get(armorStack);
    }

    @Override
    public boolean shouldCombineTextures() {
        return false;
    }
}
