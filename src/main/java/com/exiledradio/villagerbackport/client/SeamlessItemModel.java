package com.exiledradio.villagerbackport.client;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import javax.vecmath.Matrix4f;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * A generated item model without seams.
 *
 * <p>1.12.2 builds a generated item's edges as one strip per pixel that only meet edge to edge, so held close to the
 * camera the background shows through between them. Modern versions draw those edges closed. Every edge strip here is
 * pushed out by {@link #EPS} in its own plane so neighbors overlap; front and back faces and every UV are unchanged.
 * The models the item's overrides pick (a crossbow's pulling and loaded states) are wrapped the same way.</p>
 */
@SideOnly(Side.CLIENT)
final class SeamlessItemModel implements IBakedModel {

    /** Model units (a block is 1, a texture pixel 1/16): about a thirtieth of a pixel. */
    static final float EPS = 0.002F;

    private final IBakedModel inner;
    private final List<BakedQuad> general;
    private final Map<EnumFacing, List<BakedQuad>> sided = new EnumMap<>(EnumFacing.class);
    private final ItemOverrideList overrides;

    SeamlessItemModel(IBakedModel inner) {
        this.inner = inner;
        this.general = widen(inner.getQuads(null, null, 0L));
        for (EnumFacing side : EnumFacing.values()) {
            this.sided.put(side, widen(inner.getQuads(null, side, 0L)));
        }
        final ItemOverrideList base = inner.getOverrides();
        final Map<IBakedModel, IBakedModel> wrapped = Collections.synchronizedMap(new IdentityHashMap<>());
        this.overrides = new ItemOverrideList(Collections.emptyList()) {
            @Override
            public IBakedModel handleItemState(IBakedModel model, ItemStack stack, @Nullable World world,
                                               @Nullable EntityLivingBase entity) {
                IBakedModel picked = base.handleItemState(SeamlessItemModel.this.inner, stack, world, entity);
                if (picked == null || picked == SeamlessItemModel.this.inner) {
                    return SeamlessItemModel.this;
                }
                return picked instanceof SeamlessItemModel ? picked : wrapped.computeIfAbsent(picked, SeamlessItemModel::new);
            }
        };
    }

    private static List<BakedQuad> widen(List<BakedQuad> quads) {
        List<BakedQuad> out = new ArrayList<>(quads.size());
        for (BakedQuad quad : quads) {
            out.add(widen(quad));
        }
        return out;
    }

    /** Pushes an edge strip's corners away from its center along the two axes in its plane. */
    static BakedQuad widen(BakedQuad quad) {
        EnumFacing face = quad.getFace();
        if (face == null || face.getAxis() == EnumFacing.Axis.Z) {
            return quad;
        }
        VertexFormat format = quad.getFormat();
        int stride = format.getIntegerSize();
        int[] data = quad.getVertexData().clone();
        if (data.length < stride * 4) {
            return quad;
        }
        int normal = face.getAxis().ordinal();
        float[] center = new float[3];
        for (int v = 0; v < 4; v++) {
            for (int a = 0; a < 3; a++) {
                center[a] += Float.intBitsToFloat(data[v * stride + a]) / 4.0F;
            }
        }
        for (int v = 0; v < 4; v++) {
            for (int a = 0; a < 3; a++) {
                if (a == normal) {
                    continue;
                }
                float p = Float.intBitsToFloat(data[v * stride + a]);
                if (p > center[a] + 1.0E-5F) {
                    p += EPS;
                } else if (p < center[a] - 1.0E-5F) {
                    p -= EPS;
                }
                data[v * stride + a] = Float.floatToRawIntBits(p);
            }
        }
        return new BakedQuad(data, quad.getTintIndex(), face, quad.getSprite(), quad.shouldApplyDiffuseLighting(), format);
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side, long rand) {
        return side == null ? this.general : this.sided.get(side);
    }

    @Override
    public Pair<? extends IBakedModel, Matrix4f> handlePerspective(ItemCameraTransforms.TransformType type) {
        Pair<? extends IBakedModel, Matrix4f> p = this.inner.handlePerspective(type);
        return p.getLeft() == this.inner ? Pair.of(this, p.getRight()) : p;
    }

    @Override
    public boolean isAmbientOcclusion() {
        return this.inner.isAmbientOcclusion();
    }

    @Override
    public boolean isGui3d() {
        return this.inner.isGui3d();
    }

    @Override
    public boolean isBuiltInRenderer() {
        return false;
    }

    @Override
    public TextureAtlasSprite getParticleTexture() {
        return this.inner.getParticleTexture();
    }

    @SuppressWarnings("deprecation")
    @Override
    public ItemCameraTransforms getItemCameraTransforms() {
        return this.inner.getItemCameraTransforms();
    }

    @Override
    public ItemOverrideList getOverrides() {
        return this.overrides;
    }
}
