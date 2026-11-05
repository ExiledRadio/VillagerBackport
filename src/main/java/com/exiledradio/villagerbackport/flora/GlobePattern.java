package com.exiledradio.villagerbackport.flora;

import com.exiledradio.villagerbackport.ModConfig;
import com.exiledradio.villagerbackport.VillagerBackport;

import net.minecraft.tileentity.BannerPattern;
import net.minecraftforge.common.util.EnumHelper;

import javax.annotation.Nullable;

/**
 * 1.14's fifth banner pattern, the Globe, which 1.12.2's {@link BannerPattern} does not have.
 *
 * <h2>How it gets in</h2>
 * Banner patterns are an enum, so the design is added to it with Forge's {@link EnumHelper}, the
 * same way mods add horse armor or enchantment types. The new constant is a pattern in every respect
 * the game cares about: a banner stores it by its hash in NBT, and the client draws it from
 * {@code textures/entity/banner/globe.png} and {@code textures/entity/shield/globe.png}, both of
 * which this mod ships under the {@code minecraft} domain, where 1.12.2's renderer looks.
 *
 * <p>It has no crafting-grid shape and no grid item, so 1.12.2's banner recipes skip it - which is
 * correct, because in 1.14 it is only ever applied at a loom from its pattern item.
 *
 * <h2>Future MC</h2>
 * Future MC adds its own Globe the same way, under the name {@code GLOBE} with the hash
 * {@code glo}. Two Globes would be two loom entries and two textures for one design, so when one is
 * already there - Future MC loads first, being earlier in the alphabet - this reuses it. Otherwise
 * it adds one with 1.14's own hash, {@code glb}.
 *
 * <h2>When</h2>
 * {@link com.exiledradio.villagerbackport.block.BannerPatterns} builds the loom's list from the
 * enum once, when that class first loads, so the Globe has to exist by then; it asks for it from its
 * own static initializer, and item registration asks too. Whichever comes first adds it. Both sides
 * of a connection run the same code at the same point, so they agree on the list.
 */
public final class GlobePattern {

    /** 1.14's hash for the Globe. */
    private static final String HASH = "glb";

    private static boolean resolved;

    @Nullable
    private static BannerPattern globe;

    private GlobePattern() {
    }

    /**
     * @return the Globe pattern, adding it to the enum on first call; null when stream F is off
     * and no other mod has added one
     */
    @Nullable
    public static synchronized BannerPattern globe() {
        if (resolved) {
            return globe;
        }
        resolved = true;

        globe = existing();
        if (globe == null && ModConfig.flora.enabled) {
            try {
                globe = EnumHelper.addEnum(BannerPattern.class, "GLOBE",
                        new Class<?>[]{String.class, String.class}, "globe", HASH);
            } catch (RuntimeException e) {
                VillagerBackport.LOGGER.error("Could not add the Globe banner pattern; it will be missing.", e);
                globe = null;
            }
        }
        return globe;
    }

    /** A Globe another mod already added: by constant name, or by 1.14's texture name. */
    @Nullable
    private static BannerPattern existing() {
        for (BannerPattern pattern : BannerPattern.values()) {
            if ("GLOBE".equals(pattern.name()) || HASH.equals(pattern.getHashname())) {
                return pattern;
            }
        }
        return null;
    }
}
