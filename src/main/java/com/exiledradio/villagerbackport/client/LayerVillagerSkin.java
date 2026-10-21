package com.exiledradio.villagerbackport.client;

import com.exiledradio.villagerbackport.ModConfig;
import com.exiledradio.villagerbackport.trade.VillagerLevel;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Draws a villager in 1.14's appearance: body, biome outfit, profession outfit and rank badge.
 *
 * <h2>One pass, not four</h2>
 * 1.14 stacks four textures by rendering the model once per texture. Doing that here meant four
 * passes over identical geometry, which fight for the same depth values - flickering without a depth
 * bias, and showing seams along every box silhouette with one.
 *
 * <p>{@link VillagerSkinComposer} flattens the four into a single image instead, so this is one
 * ordinary render pass with one ordinary texture. There is nothing left to fight, and the model is
 * rendered a quarter as often.
 *
 * <h2>Why this still cannot be the renderer's own texture</h2>
 * {@code RenderVillager.getEntityTexture} returns {@code profession.getSkin()}, which holds one
 * texture per profession - and a 1.12.2 profession covers up to four 1.14 outfits, so it cannot tell
 * a fisherman from a fletcher. It also knows nothing of biome or rank. Overriding the method means
 * replacing the renderer, which would discard MoBends, iChunUtil and Classy Hats.
 *
 * <p>So the composed texture is drawn as a layer, and {@link VanillaSkinSuppressor} stops the
 * renderer drawing its own underneath.
 */
@SideOnly(Side.CLIENT)
public class LayerVillagerSkin implements LayerRenderer<EntityVillager> {

    private final RenderLivingBase<?> renderer;

    public LayerVillagerSkin(RenderLivingBase<?> renderer) {
        this.renderer = renderer;
    }

    @Override
    public void doRenderLayer(EntityVillager villager, float limbSwing, float limbSwingAmount,
                              float partialTicks, float ageInTicks, float netHeadYaw,
                              float headPitch, float scale) {
        if (!ModConfig.display.useModernVillagerSkins || villager.isInvisible()) {
            return;
        }

        ResourceLocation skin = skinFor(villager);
        boolean modern = skin != null;
        if (skin == null && VillagerSkin.isVanillaProfession(villager)) {
            // The 1.14 skin could not be built - a texture missing from a resource pack, or the jar
            // replaced underneath a running game. The vanilla skin underneath has been blanked, so
            // drawing nothing here would leave only the villager's shadow. Its own 1.12.2 texture
            // is drawn instead.
            skin = VanillaSkinSuppressor.original(villager.getProfessionForge());
        }
        if (skin == null) {
            return;
        }

        ModelBase model = this.renderer.getMainModel();
        if (model == null) {
            return;
        }

        this.renderer.bindTexture(skin);
        // 1.14's head shake. The 1.12.2 model sets the head's pitch as it renders, so the shake's
        // bowed head is handed in as the pitch; it never touches the roll, which is set beforehand.
        net.minecraft.client.model.ModelRenderer head = model instanceof net.minecraft.client.model.ModelVillager
                ? ((net.minecraft.client.model.ModelVillager) model).villagerHead : null;
        if (head != null) {
            head.rotateAngleZ = HeadShake.roll(villager, ageInTicks);
            headPitch = HeadShake.pitch(villager, headPitch);
        }
        model.render(villager, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
        // The 1.12.2 model has nowhere to put a hat, so the hat is a box of its own on the head.
        // Only on a 1.14 skin: the 1.12.2 fallback was never drawn for one.
        if (modern) {
            VillagerHat.render(model, scale);
        }
        if (head != null) {
            head.rotateAngleZ = 0.0F;
        }
    }

    /**
     * @return the composed texture for this villager, or null to leave it alone entirely
     *
     * <p>Null only for villagers whose profession has no 1.14 counterpart - ones another mod
     * registered. Those keep their own texture, which {@link VanillaSkinSuppressor} deliberately
     * leaves intact for exactly this reason.
     */
    private ResourceLocation skinFor(EntityVillager villager) {
        if (!VillagerSkin.isVanillaProfession(villager)) {
            return null;
        }

        // A villager that has not picked a career yet - which is every villager until its trade list
        // is first built - has no profession outfit. 1.14 dresses an unemployed villager in plain
        // biome clothing, and doing the same means it is never left with nothing to render.
        ResourceLocation profession = VillagerSkin.professionFor(villager);
        ResourceLocation type = VillagerSkin.typeFor(villager);

        // No badge without a job. The badge is a rank within a profession, so wearing one while
        // unemployed reads as a villager that has a trade when it has none - and 1.14 never shows
        // one on an unemployed villager either.
        //
        // Nor on a nitwit, which wears its own outfit but has no rank to show. 1.14.4 drew one on
        // nitwits anyway; later versions stopped, and a nitwit with a stone badge reads as a novice
        // in something rather than as the villager who does nothing.
        int level = VillagerLevelCache.get(villager);
        boolean employed = VillagerLevelCache.career(villager) > 0;

        ResourceLocation badge = ModConfig.display.showLevelBadge && employed && !villager.isChild()
                && !VillagerSkin.isNitwit(villager)
                ? VillagerSkin.badgeFor(level)
                : null;

        // A biome hat under a profession's is hidden, as 1.14 does - a snowy farmer wears the straw
        // hat rather than both at once.
        boolean typeHat = VillagerSkinComposer.typeHatShows(type, profession);

        String key = VillagerSkin.name(type) + "_" + VillagerSkin.name(profession)
                + "_" + (badge == null ? "none" : VillagerSkin.name(badge)) + (typeHat ? "" : "_hatless");

        return VillagerSkinComposer.compose(key, typeHat ? null : type,
                VillagerSkin.BASE, type, profession, badge);
    }

    /**
     * @return false, so the composed texture is used as-is rather than being merged with the base.
     */
    @Override
    public boolean shouldCombineTextures() {
        return false;
    }
}
