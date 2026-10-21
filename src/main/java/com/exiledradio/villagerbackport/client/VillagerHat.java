package com.exiledradio.villagerbackport.client;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.model.ModelVillager;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * The two boxes 1.14 added to the villager's head, which the 1.12.2 model does not have.
 *
 * <h2>Why villagers were bareheaded</h2>
 * 1.14's textures draw every hat - the farmer's straw hat, the librarian's cap, the snowy and desert
 * head wraps - on a second, slightly larger head box at texture offset 32,0, and wide brims on a flat
 * 16x16 sheet at 30,47. The 1.12.2 {@link ModelVillager} has neither box, so those pixels were in the
 * composed texture all along with nothing to draw them on.
 *
 * <p>The boxes are 1.14's {@code VillagerModel} ones, exactly: the hat on the head, the brim on the hat,
 * laid flat. They follow the head through the model's own pose, so nodding and head-shaking carry the
 * hat with them.
 */
@SideOnly(Side.CLIENT)
final class VillagerHat {

    /** Holds the boxes; ModelRenderer must belong to a model, and texture size is read from it. */
    private static final ModelBase OWNER = new ModelBase() {
    };

    private static final ModelRenderer HAT;

    static {
        OWNER.textureWidth = 64;
        OWNER.textureHeight = 64;

        HAT = new ModelRenderer(OWNER).setTextureSize(64, 64);
        HAT.setTextureOffset(32, 0).addBox(-4.0F, -10.0F, -4.0F, 8, 10, 8, 0.5F);

        ModelRenderer rim = new ModelRenderer(OWNER).setTextureSize(64, 64);
        rim.setTextureOffset(30, 47).addBox(-8.0F, -8.0F, -6.0F, 16, 16, 1, 0.0F);
        rim.rotateAngleX = (float) -Math.PI / 2.0F;
        HAT.addChild(rim);
    }

    private VillagerHat() {
    }

    /**
     * Draws the hat on the head of a model that has just been rendered, with its texture still bound.
     *
     * <p>Does nothing for a model that is not the vanilla villager's: one another mod supplies has
     * its own idea of where the head is.
     */
    static void render(ModelBase model, float scale) {
        if (!(model instanceof ModelVillager)) {
            return;
        }
        ModelRenderer head = ((ModelVillager) model).villagerHead;
        if (head == null || !head.showModel || head.isHidden) {
            return;
        }
        HAT.rotationPointX = head.rotationPointX;
        HAT.rotationPointY = head.rotationPointY;
        HAT.rotationPointZ = head.rotationPointZ;
        HAT.rotateAngleX = head.rotateAngleX;
        HAT.rotateAngleY = head.rotateAngleY;
        HAT.rotateAngleZ = head.rotateAngleZ;
        HAT.offsetX = head.offsetX;
        HAT.offsetY = head.offsetY;
        HAT.offsetZ = head.offsetZ;
        HAT.render(scale);
    }
}
