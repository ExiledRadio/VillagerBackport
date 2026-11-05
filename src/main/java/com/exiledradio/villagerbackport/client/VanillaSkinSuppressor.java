package com.exiledradio.villagerbackport.client;

import com.exiledradio.villagerbackport.ModConfig;
import com.exiledradio.villagerbackport.VillagerBackport;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.common.registry.VillagerRegistry;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * Stops the vanilla villager texture being drawn underneath the 1.14 one.
 *
 * <h2>Why it was being drawn at all</h2>
 * The 1.14 appearance is added as a render layer, because overriding
 * {@code RenderVillager.getEntityTexture} would mean replacing the renderer and discarding MoBends,
 * iChunUtil and Classy Hats along with it. A layer draws <em>in addition to</em> the renderer's own
 * pass, so the old texture was still being rendered - covered up, but competing for the same depth
 * values the whole time. That competition is what produced the flicker, and the depth bias used to
 * settle it is what produced the seams.
 *
 * <p>Covering it up was treating the symptom. Removing it is the fix.
 *
 * <h2>How</h2>
 * The renderer asks the profession which texture to use, and the profession simply holds a
 * {@code ResourceLocation} in a field. Pointing that at a fully transparent image means the
 * renderer's pass draws nothing: entity rendering runs with alpha testing on, so transparent
 * fragments are discarded before they reach the depth buffer. Nothing is written, so nothing can be
 * fought over, and the composed texture is the only thing drawn.
 *
 * <h2>Other mods' professions</h2>
 * Only the six vanilla professions are blanked for good. Another mod's is blanked only while the
 * renderer this mod's layer sits on is drawing it - see {@link #blankWhileDrawing} - because a mod
 * may draw its villagers with a renderer of its own, and there a blank texture would mean an
 * invisible villager.
 *
 * <p>The zombie texture is untouched. Curing and zombie villager rendering go through
 * {@code getZombieSkin}, which this mod has no replacement for.
 */
@SideOnly(Side.CLIENT)
public final class VanillaSkinSuppressor {

    /** A fully transparent image, so the renderer's own pass has nothing to draw. */
    private static final ResourceLocation BLANK =
            new ResourceLocation("villagerbackport", "blank_villager_skin");

    private static boolean applied;

    /** Each suppressed profession's own texture, for drawing when a 1.14 skin cannot be built. */
    private static final java.util.Map<VillagerRegistry.VillagerProfession, ResourceLocation> ORIGINALS =
            new java.util.HashMap<VillagerRegistry.VillagerProfession, ResourceLocation>();

    private VanillaSkinSuppressor() {
    }

    /**
     * Blanks the texture on every vanilla villager profession.
     *
     * <p>Runs once, and only when the modern skins are actually going to be drawn - with them turned
     * off, the vanilla textures are the ones doing the work and must be left alone.
     */
    public static void apply() {
        if (applied || !ModConfig.display.useModernVillagerSkins) {
            return;
        }
        applied = true;

        try {
            registerBlankTexture();

            Field texture = ReflectionHelper.findField(
                    VillagerRegistry.VillagerProfession.class, "texture", "texture");
            unfinalise(texture);

            textureField = texture;
            int count = 0;
            for (VillagerRegistry.VillagerProfession profession : ForgeRegistries.VILLAGER_PROFESSIONS) {
                ORIGINALS.put(profession, profession.getSkin());
                if (!VillagerSkin.isVanilla(profession)) {
                    // Another mod's: blanked only while the renderer this mod draws on is drawing it.
                    continue;
                }

                texture.set(profession, BLANK);
                count++;
            }

            VillagerBackport.LOGGER.info("Suppressed the vanilla texture on {} villager professions.", count);
        } catch (Exception e) {
            // Not fatal. The composed skin still renders on top; the old texture is simply still
            // underneath it, which is how this looked before and is far better than no villagers.
            VillagerBackport.LOGGER.error(
                    "Could not suppress the vanilla villager textures. The 1.14 skins will still be "
                            + "drawn, but over the old ones rather than instead of them.", e);
        }
    }

    /** The profession's texture field, once {@link #apply} has opened it. */
    private static Field textureField;

    /** Another mod's profession blanked for the villager being drawn now, or null. */
    @javax.annotation.Nullable
    private static VillagerRegistry.VillagerProfession blanked;

    /**
     * Blanks another mod's profession texture for one villager's draw, so the layer can draw its
     * own version - the mod's texture with the rank badge, or a 1.14 outfit - with nothing under it.
     *
     * <h2>Why only for the draw</h2>
     * Some mods draw their villagers with a renderer of their own, which this mod adds no layer to.
     * A texture blanked for good would leave those invisible. Blanked only while the vanilla
     * renderer this mod's layer sits on is drawing, and put back straight after, nothing else ever
     * sees it.
     */
    public static void blankWhileDrawing(@javax.annotation.Nullable VillagerRegistry.VillagerProfession profession) {
        restore();
        if (textureField == null || profession == null || VillagerSkin.isVanilla(profession)
                || !ORIGINALS.containsKey(profession)) {
            return;
        }
        try {
            textureField.set(profession, BLANK);
            blanked = profession;
        } catch (Exception e) {
            // Left as it was: the mod's own texture shows, as before.
        }
    }

    /** Puts back whatever {@link #blankWhileDrawing} took away. */
    public static void restore() {
        if (blanked == null) {
            return;
        }
        try {
            textureField.set(blanked, ORIGINALS.get(blanked));
        } catch (Exception ignored) {
            // The field was writable a moment ago; nothing more to do.
        }
        blanked = null;
    }

    /**
     * @return the profession's own 1.12.2 texture as it was before being blanked, or null if this
     * profession was never seen
     */
    @javax.annotation.Nullable
    public static ResourceLocation original(@javax.annotation.Nullable VillagerRegistry.VillagerProfession profession) {
        return profession == null ? null : ORIGINALS.get(profession);
    }

    /**
     * Registers a 1x1 transparent texture under our own name.
     *
     * <p>Built in memory rather than shipped as a file, so there is no way for a resource pack to
     * replace it with something opaque and turn every villager into a blank slate.
     */
    private static void registerBlankTexture() {
        BufferedImage blank = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        blank.setRGB(0, 0, 0x00000000);
        Minecraft.getMinecraft().getTextureManager().loadTexture(BLANK, new DynamicTexture(blank));
    }

    /** Clears the FINAL modifier so the field can be written. Java 8, which is all 1.12.2 runs. */
    private static void unfinalise(Field field) throws Exception {
        field.setAccessible(true);
        Field modifiers = Field.class.getDeclaredField("modifiers");
        modifiers.setAccessible(true);
        modifiers.setInt(field, field.getModifiers() & ~Modifier.FINAL);
    }
}
