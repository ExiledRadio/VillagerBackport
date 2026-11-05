package com.exiledradio.villagerbackport.client;

import com.exiledradio.villagerbackport.VillagerBackport;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBox;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.model.ModelVillager;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.lang.reflect.Field;

/**
 * Mirrors the villager's left arm, as 1.14's model does.
 *
 * <p>The folded arms are three boxes: right arm, left arm, and the forearms across the front. Both
 * arms read the same patch of the texture. 1.14 draws the left one mirrored, so whatever is on the
 * outside of one sleeve is on the outside of the other; 1.12.2's model does not, and an outfit drawn
 * for 1.14 shows its left sleeve inside out - the detail meant for the outer side facing the body.
 *
 * <p>The box is swapped for 1.14's in the model the renderer already uses, which keeps any other
 * mod's wrapping of the renderer intact. Only the plain villager model, recognised by its three arm
 * boxes in 1.12.2's places, is changed.
 */
@SideOnly(Side.CLIENT)
final class VillagerArms {

    private VillagerArms() {
    }

    static void mirrorLeftArm(ModelBase model) {
        if (!(model instanceof ModelVillager)) {
            return;
        }
        ModelRenderer arms = ((ModelVillager) model).villagerArms;
        if (arms == null || arms.cubeList.size() != 3) {
            return;
        }
        ModelBox left = arms.cubeList.get(1);
        if (left.posX1 != 4.0F || left.posY1 != -2.0F || left.posZ1 != -2.0F) {
            return;
        }

        arms.cubeList.set(1, new ModelBox(arms, 44, 22, 4.0F, -2.0F, -2.0F, 4, 8, 4, 0.0F, true));

        // A model already drawn has its boxes baked into a display list, which would keep the old
        // arm. Clearing the flag makes it bake again on the next draw.
        try {
            Field compiled = ReflectionHelper.findField(ModelRenderer.class, "compiled", "field_78812_q");
            compiled.setAccessible(true);
            compiled.setBoolean(arms, false);
        } catch (Exception e) {
            VillagerBackport.LOGGER.debug("Could not re-bake the villager's arms; the mirrored arm shows after a "
                    + "resource reload.", e);
        }
    }
}
