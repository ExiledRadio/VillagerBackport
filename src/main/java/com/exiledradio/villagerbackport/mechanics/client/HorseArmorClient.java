package com.exiledradio.villagerbackport.mechanics.client;

import com.exiledradio.villagerbackport.ModConfig;
import com.exiledradio.villagerbackport.VillagerBackport;

import net.minecraft.entity.passive.HorseArmorType;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.Map;

/**
 * 1.14 (19w08a) draws horse armor as a layer over the horse ({@code HorseArmorLayer}) where 1.12.2 bakes it into the
 * horse's texture: so the armor no longer turns red with the horse when it is hurt, and an invisible horse's armor
 * still shows.
 *
 * <p>1.12.2 bakes the texture each armor type names; with {@link ModConfig.Mechanics#horseArmorLayer} on at start-up,
 * iron, gold and diamond name none here (their texture is kept for the layer), so the baked texture is the bare
 * horse and {@code flora.client.LayerLeatherHorseArmor} draws the armor, as it already draws leather. The textures
 * stay 1.12.2's: 1.14's are laid out for 1.14's horse model.
 */
@SideOnly(Side.CLIENT)
public final class HorseArmorClient {

    private static final String TEXTURE_NAME = "field_188586_e";
    private static final Map<HorseArmorType, ResourceLocation> TEXTURES = new EnumMap<HorseArmorType, ResourceLocation>(HorseArmorType.class);
    private static boolean active;

    private HorseArmorClient() {
    }

    /** Client pre-init. A change to the setting takes effect on restart: horses cache their baked textures. */
    public static void init() {
        if (!ModConfig.mechanics.horseArmorLayer) {
            return;
        }
        try {
            for (HorseArmorType type : new HorseArmorType[] {HorseArmorType.IRON, HorseArmorType.GOLD, HorseArmorType.DIAMOND}) {
                String name = ObfuscationReflectionHelper.getPrivateValue(HorseArmorType.class, type, TEXTURE_NAME);
                if (name == null) {
                    continue;
                }
                TEXTURES.put(type, new ResourceLocation(name));
                ObfuscationReflectionHelper.setPrivateValue(HorseArmorType.class, type, null, TEXTURE_NAME);
            }
            active = true;
        } catch (RuntimeException e) {
            // Put back whatever was taken, so armor is never lost from sight.
            for (Map.Entry<HorseArmorType, ResourceLocation> e2 : TEXTURES.entrySet()) {
                ObfuscationReflectionHelper.setPrivateValue(HorseArmorType.class, e2.getKey(), e2.getValue().toString(), TEXTURE_NAME);
            }
            TEXTURES.clear();
            VillagerBackport.LOGGER.warn("Horse armor stays baked into the horse's texture (1.12.2's way)", e);
        }
    }

    /** @return true when vanilla's horse armor is drawn as a layer (and the layer must be attached) */
    public static boolean active() {
        return active;
    }

    /** @return the layer texture of a vanilla horse armor, or null for none */
    @Nullable
    public static ResourceLocation texture(ItemStack armor) {
        if (!active || armor.isEmpty()) {
            return null;
        }
        return TEXTURES.get(armor.getItem().getHorseArmorType(armor));
    }
}
