package com.exiledradio.villagerbackport.babies.client.render;

import com.exiledradio.villagerbackport.babies.client.BabyModels;

import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.lang.reflect.Method;

/**
 * Stands in for vanilla's renderer of one mob: babies go to the 26.3 baby renderer while
 * {@link BabyModels#active()} (read every frame), everything else to vanilla's renderer untouched.
 */
public final class BabyWrapper<T extends EntityLiving> extends Render<T> {

    private static Method getEntityTexture;

    private final Render<T> vanilla;
    private final BabyRender<T> baby;

    public BabyWrapper(Render<T> vanilla, BabyRender<T> baby) {
        super(vanilla.getRenderManager());
        this.vanilla = vanilla;
        this.baby = baby;
    }

    public Render<T> vanilla() {
        return vanilla;
    }

    private boolean useBaby(T entity) {
        return BabyModels.active() && entity.isChild() && baby.handles(entity);
    }

    private Render<T> pick(T entity) {
        return useBaby(entity) ? baby : vanilla;
    }

    @Override
    public void doRender(T entity, double x, double y, double z, float entityYaw, float partialTicks) {
        pick(entity).doRender(entity, x, y, z, entityYaw, partialTicks);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void doRenderShadowAndFire(Entity entity, double x, double y, double z, float yaw, float partialTicks) {
        pick((T) entity).doRenderShadowAndFire(entity, x, y, z, yaw, partialTicks);
    }

    @Override
    public boolean shouldRender(T entity, ICamera camera, double camX, double camY, double camZ) {
        return vanilla.shouldRender(entity, camera, camX, camY, camZ);
    }

    @Override
    public void setRenderOutlines(boolean renderOutlines) {
        super.setRenderOutlines(renderOutlines);
        vanilla.setRenderOutlines(renderOutlines);
        baby.setRenderOutlines(renderOutlines);
    }

    @Override
    public boolean isMultipass() {
        return vanilla.isMultipass();
    }

    @Override
    public void renderMultipass(T entity, double x, double y, double z, float entityYaw, float partialTicks) {
        if (!useBaby(entity)) {
            vanilla.renderMultipass(entity, x, y, z, entityYaw, partialTicks);
        }
    }

    @Override
    protected ResourceLocation getEntityTexture(T entity) {
        if (useBaby(entity)) {
            return baby.texture(entity);
        }
        try {
            if (getEntityTexture == null) {
                getEntityTexture = ReflectionHelper.findMethod(Render.class, "getEntityTexture", "func_110775_a", Entity.class);
            }
            return (ResourceLocation) getEntityTexture.invoke(vanilla, entity);
        } catch (Exception e) {
            return null;
        }
    }
}
