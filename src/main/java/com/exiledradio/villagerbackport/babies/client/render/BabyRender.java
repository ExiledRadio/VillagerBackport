package com.exiledradio.villagerbackport.babies.client.render;

import com.exiledradio.villagerbackport.VillagerBackport;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.EntityLiving;
import net.minecraft.util.ResourceLocation;

/**
 * A baby's renderer: 1.12's {@link RenderLiving} pipeline (rotations, hurt tint, layers, name tag, leash, events)
 * around a 26.3 baby model, with no extra scale, as 26.3's renderers draw babies.
 */
public class BabyRender<T extends EntityLiving> extends RenderLiving<T> {

    /** Chooses a baby's texture. */
    public interface Texture<T> {
        ResourceLocation get(T entity);
    }

    private final Texture<T> texture;

    /**
     * @param shadow 26.3's shadow radius for the adult; 1.12 halves it for children as 26.3 does
     */
    public BabyRender(RenderManager manager, ModelBase model, float shadow, Texture<T> texture) {
        super(manager, model, shadow);
        this.texture = texture;
    }

    /** @return {@code villagerbackport:textures/entity/<path>.png} */
    public static ResourceLocation tex(String path) {
        return new ResourceLocation(VillagerBackport.MOD_ID, "textures/entity/" + path + ".png");
    }

    public ResourceLocation texture(T entity) {
        return texture.get(entity);
    }

    @Override
    protected ResourceLocation getEntityTexture(T entity) {
        return texture.get(entity);
    }

    /** Whether this renderer can draw the entity as 26.3 does; otherwise vanilla's draws it. */
    public boolean handles(T entity) {
        return true;
    }
}
