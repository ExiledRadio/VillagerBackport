package com.exiledradio.villagerbackport.cat.client;

import com.exiledradio.villagerbackport.babies.client.BabyModels;
import com.exiledradio.villagerbackport.babies.client.model.BabyCatModel;
import com.exiledradio.villagerbackport.VillagerBackport;
import com.exiledradio.villagerbackport.cat.EntityCat;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 1.14's cat renderer: four-fifths size, the coat chosen by type, a dyed collar on tamed cats, and
 * the cat rolled onto its side as it lies down - shuffled a little further over when it is lying next
 * to a sleeping player, so it curls against them rather than inside them.
 */
@SideOnly(Side.CLIENT)
public class RenderCat extends RenderLiving<EntityCat> {

    private static final ResourceLocation[] TEXTURES = new ResourceLocation[EntityCat.TYPES.length];
    private static final ResourceLocation[] BABY_TEXTURES = new ResourceLocation[EntityCat.TYPES.length];

    static {
        for (int i = 0; i < EntityCat.TYPES.length; i++) {
            TEXTURES[i] = new ResourceLocation(VillagerBackport.MOD_ID, "textures/entity/cat/" + EntityCat.TYPES[i] + ".png");
            BABY_TEXTURES[i] = new ResourceLocation(VillagerBackport.MOD_ID, "textures/entity/cat/cat_" + EntityCat.TYPES[i] + "_baby.png");
        }
    }

    private final ModelCat adultModel;
    private final BabyCatModel babyModel = new BabyCatModel();

    public RenderCat(RenderManager manager) {
        super(manager, new ModelCat(0.0F), 0.4F);
        this.adultModel = (ModelCat) this.mainModel;
        this.addLayer(new CollarLayer(this));
    }

    /** @return true when this cat draws 26.1's baby model */
    static boolean newBaby(EntityCat cat) {
        return cat.isChild() && BabyModels.active();
    }

    @Override
    public void doRender(EntityCat cat, double x, double y, double z, float yaw, float partialTicks) {
        this.mainModel = newBaby(cat) ? this.babyModel : this.adultModel;
        super.doRender(cat, x, y, z, yaw, partialTicks);
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityCat cat) {
        int type = cat.getCatType();
        return (newBaby(cat) ? BABY_TEXTURES : TEXTURES)[type >= 0 && type < TEXTURES.length ? type : 0];
    }

    @Override
    protected void preRenderCallback(EntityCat cat, float partialTicks) {
        // 26.3 scales the adult cat's layer by 0.8 (AdultCatModel.CAT_TRANSFORMER), never the baby's.
        if (this.mainModel != this.babyModel) {
            GlStateManager.scale(0.8F, 0.8F, 0.8F);
        }
    }

    @Override
    protected void applyRotations(EntityCat cat, float ageInTicks, float rotationYaw, float partialTicks) {
        super.applyRotations(cat, ageInTicks, rotationYaw, partialTicks);
        float lie = cat.getLieDownAmount(partialTicks);
        if (lie > 0.0F) {
            GlStateManager.translate(0.4F * lie, 0.15F * lie, 0.1F * lie);
            GlStateManager.rotate(90.0F * lie, 0.0F, 0.0F, 1.0F);
            BlockPos at = new BlockPos(cat);
            for (EntityPlayer player : cat.world.getEntitiesWithinAABB(EntityPlayer.class, new AxisAlignedBB(at).grow(2.0D))) {
                if (player.isPlayerSleeping()) {
                    GlStateManager.translate(0.15F * lie, 0.0F, 0.0F);
                    break;
                }
            }
        }
    }

    /** 1.14's {@code CatCollarLayer}: the collar texture over a slightly larger model, dyed. */
    private static final class CollarLayer implements LayerRenderer<EntityCat> {
        private static final ResourceLocation COLLAR = new ResourceLocation(VillagerBackport.MOD_ID, "textures/entity/cat/cat_collar.png");
        private static final ResourceLocation BABY_COLLAR = new ResourceLocation(VillagerBackport.MOD_ID, "textures/entity/cat/cat_collar_baby.png");

        private final RenderCat renderer;
        private final ModelCat model = new ModelCat(0.01F);
        private final BabyCatModel babyModel = new BabyCatModel();

        CollarLayer(RenderCat renderer) {
            this.renderer = renderer;
        }

        @Override
        public void doRenderLayer(EntityCat cat, float limbSwing, float limbSwingAmount, float partialTicks,
                                  float ageInTicks, float netHeadYaw, float headPitch, float scale) {
            if (!cat.isTamed() || cat.isInvisible()) {
                return;
            }
            boolean baby = this.renderer.getMainModel() instanceof BabyCatModel;
            this.renderer.bindTexture(baby ? BABY_COLLAR : COLLAR);
            EnumDyeColor color = cat.getCollarColor();
            float[] rgb = color.getColorComponentValues();
            GlStateManager.color(rgb[0], rgb[1], rgb[2]);
            if (baby) {
                // CAT_BABY_COLLAR: the baby layer through MeshTransformer.scaling(1.01F).
                GlStateManager.pushMatrix();
                GlStateManager.translate(0.0F, 24.016F * (1.0F - 1.01F) * scale, 0.0F);
                GlStateManager.scale(1.01F, 1.01F, 1.01F);
                this.babyModel.setLivingAnimations(cat, limbSwing, limbSwingAmount, partialTicks);
                this.babyModel.render(cat, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
                GlStateManager.popMatrix();
                GlStateManager.color(1.0F, 1.0F, 1.0F);
                return;
            }
            this.model.setModelAttributes(this.renderer.getMainModel());
            this.model.setLivingAnimations(cat, limbSwing, limbSwingAmount, partialTicks);
            this.model.render(cat, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
            GlStateManager.color(1.0F, 1.0F, 1.0F);
        }

        @Override
        public boolean shouldCombineTextures() {
            return true;
        }
    }
}
