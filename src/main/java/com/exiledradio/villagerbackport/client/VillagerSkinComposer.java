package com.exiledradio.villagerbackport.client;

import com.exiledradio.villagerbackport.VillagerBackport;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.resources.IResource;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

/**
 * Flattens 1.14's stacked villager textures into a single one.
 *
 * <h2>Why not just draw them in order</h2>
 * The obvious way to reproduce a stack of textures is to render the model once per texture, which is
 * what 1.14 does and what this mod did first. On identical geometry that means several passes
 * competing for the same depth values, and every remedy for it has its own cost: without a depth
 * bias the passes flicker, and with one the bias leaks along box silhouettes as visible seams.
 *
 * <p>None of that is necessary. The textures are stacked the same way for every villager sharing a
 * biome, profession and rank, so the stacking can be done once - on the image rather than on the
 * screen. Compositing produces one ordinary texture and one ordinary render pass, which is not a
 * special case at all and cannot fight with anything.
 *
 * <p>It is also cheaper. Four passes over the model became one, for every villager, every frame.
 *
 * <h2>Caching</h2>
 * Keyed on the combination that produced it, so villagers sharing a look share a texture. The
 * theoretical maximum is seven biomes times fourteen professions times five ranks, and a real world
 * uses a handful. Each is a 64x64 image, so even the pathological case is a few megabytes.
 */
@SideOnly(Side.CLIENT)
public final class VillagerSkinComposer {

    private static final int SIZE = 64;

    private static final Map<String, ResourceLocation> CACHE = new HashMap<String, ResourceLocation>();

    private VillagerSkinComposer() {
    }

    /**
     * @return a single texture combining the given layers, or null if it could not be built
     *
     * <p>Layers are applied in order and may be null, which skips them - a villager with no career
     * yet has no profession outfit to wear, and gets the plain biome clothing 1.14 gives an
     * unemployed villager.
     */
    @Nullable
    public static ResourceLocation compose(String key, ResourceLocation... layers) {
        ResourceLocation cached = CACHE.get(key);
        if (cached != null) {
            return cached;
        }

        // Cached even on failure, as a null entry would be, so a missing texture is not retried on
        // every frame for every villager. Recorded as absent by leaving it out and returning null.
        ResourceLocation built = build(key, layers);
        if (built != null) {
            CACHE.put(key, built);
        }
        return built;
    }

    @Nullable
    private static ResourceLocation build(String key, ResourceLocation[] layers) {
        try {
            BufferedImage canvas = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = canvas.createGraphics();

            boolean drewAnything = false;
            for (ResourceLocation layer : layers) {
                if (layer == null) {
                    continue;
                }

                BufferedImage image = read(layer);
                if (image != null) {
                    // Default composite is source-over, which is exactly how the layers stack:
                    // transparent areas leave what is underneath showing.
                    graphics.drawImage(image, 0, 0, SIZE, SIZE, null);
                    drewAnything = true;
                }
            }

            graphics.dispose();

            if (!drewAnything) {
                return null;
            }

            ResourceLocation location = new ResourceLocation("villagerbackport", "composed/" + key);
            Minecraft.getMinecraft().getTextureManager().loadTexture(location, new DynamicTexture(canvas));
            return location;
        } catch (RuntimeException e) {
            VillagerBackport.LOGGER.error("Failed to compose villager skin '{}'", key, e);
            return null;
        }
    }

    @Nullable
    private static BufferedImage read(ResourceLocation location) {
        IResource resource = null;
        try {
            resource = Minecraft.getMinecraft().getResourceManager().getResource(location);
            return TextureUtil.readBufferedImage(resource.getInputStream());
        } catch (Exception e) {
            VillagerBackport.LOGGER.warn("Missing villager texture {}", location, e);
            return null;
        } finally {
            if (resource != null) {
                try {
                    resource.getInputStream().close();
                } catch (Exception ignored) {
                    // Nothing useful to do if the stream will not close.
                }
            }
        }
    }

    /**
     * Drops every composed texture, for when the resource packs change.
     *
     * <p>The sources are reloaded from a different pack set, so anything built from the old ones is
     * stale. The textures themselves are left registered with the texture manager, which replaces
     * them by name when they are next composed.
     */
    public static void clear() {
        CACHE.clear();
    }
}
