package com.exiledradio.villagerbackport.fox.client;

import com.exiledradio.villagerbackport.babies.client.BabyModels;
import com.exiledradio.villagerbackport.babies.client.model.BabyFoxModel;
import com.exiledradio.villagerbackport.VillagerBackport;
import com.exiledradio.villagerbackport.fox.EntityFox;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Draws a fox: red or snow, with its eyes shut while asleep, tipped forward through a pounce, and
 * holding whatever is in its mouth.
 */
@SideOnly(Side.CLIENT)
public class RenderFox extends RenderLiving<EntityFox> {

    private static final ResourceLocation RED = texture("fox");
    private static final ResourceLocation RED_SLEEP = texture("fox_sleep");
    private static final ResourceLocation SNOW = texture("snow_fox");
    private static final ResourceLocation SNOW_SLEEP = texture("snow_fox_sleep");
    private static final ResourceLocation RED_BABY = texture("fox_baby");
    private static final ResourceLocation RED_SLEEP_BABY = texture("fox_sleep_baby");
    private static final ResourceLocation SNOW_BABY = texture("fox_snow_baby");
    private static final ResourceLocation SNOW_SLEEP_BABY = texture("fox_snow_sleep_baby");

    private final ModelFox adultModel;
    private final BabyFoxModel babyModel = new BabyFoxModel();

    public RenderFox(RenderManager manager) {
        super(manager, new ModelFox(), 0.4F);
        adultModel = (ModelFox) mainModel;
        addLayer(new LayerFoxHeldItem(this));
    }

    private static ResourceLocation texture(String name) {
        return new ResourceLocation(VillagerBackport.MOD_ID, "textures/entity/fox/" + name + ".png");
    }

    /** @return true when this fox draws 26.1's baby model */
    static boolean newBaby(EntityFox fox) {
        return fox.isChild() && BabyModels.active();
    }

    @Override
    public void doRender(EntityFox fox, double x, double y, double z, float yaw, float partialTicks) {
        mainModel = newBaby(fox) ? babyModel : adultModel;
        super.doRender(fox, x, y, z, yaw, partialTicks);
    }

    @Override
    protected void applyRotations(EntityFox fox, float ageInTicks, float rotationYaw, float partialTicks) {
        super.applyRotations(fox, ageInTicks, rotationYaw, partialTicks);
        if (fox.isPouncing() || fox.isFaceplanted()) {
            float pitch = fox.prevRotationPitch + (fox.rotationPitch - fox.prevRotationPitch) * partialTicks;
            GlStateManager.rotate(-pitch, 1.0F, 0.0F, 0.0F);
        }
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityFox fox) {
        if (newBaby(fox)) {
            if (fox.getFoxType() == EntityFox.Type.RED) {
                return fox.isSleeping() ? RED_SLEEP_BABY : RED_BABY;
            }
            return fox.isSleeping() ? SNOW_SLEEP_BABY : SNOW_BABY;
        }
        if (fox.getFoxType() == EntityFox.Type.RED) {
            return fox.isSleeping() ? RED_SLEEP : RED;
        }
        return fox.isSleeping() ? SNOW_SLEEP : SNOW;
    }
}
