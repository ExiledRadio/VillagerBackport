package com.exiledradio.villagerbackport.block;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.translation.I18n;

/**
 * Readable names when the language file is not loaded.
 *
 * <h2>Why this used to happen</h2>
 * The jar shipped without a {@code pack.mcmeta}, so {@code FMLFileResourcePack} substituted a dummy
 * one declaring {@code pack_format: 2}. {@code FMLClientHandler.addModAsResource} wraps any pack
 * declaring exactly 2 in {@link net.minecraft.client.resources.LegacyV2Adapter}, whose
 * {@code fudgePath} rewrites every path under {@code lang/} ending in {@code .lang} by uppercasing
 * the region code. So each request for {@code lang/en_us.lang} was quietly turned into
 * {@code lang/en_US.lang}, which a zip lookup does not find.
 *
 * <p>That is why textures and models were fine while only translations went missing: nothing else
 * goes through that rewrite. A {@code pack.mcmeta} declaring format 3, which is 1.12.2's own, keeps
 * the adapter out of the way and is why the uppercase file name so many 1.12.2 tutorials recommend
 * also works.
 *
 * <p>The fallbacks stay. They cost nothing where translations load, and they are what kept the mod
 * readable through three releases of not knowing this. A translation is used whenever one exists.
 *
 * <p>Deliberately server-safe: this uses the common {@code I18n}, not the client-only one, because
 * container titles are built server-side.
 */
public final class Names {

    private Names() {
    }

    /**
     * @return the translation for a key, or the given fallback if there is none
     *
     * <p>{@code translateToLocal} hands back the key unchanged when it cannot resolve it, which is
     * what makes the failure detectable at all.
     */
    public static String translateOr(String key, String fallback) {
        String translated = I18n.translateToLocal(key);
        return key.equals(translated) ? fallback : translated.trim();
    }

    /**
     * @return a display name built from a registry name, so {@code cartography_table} reads as
     * "Cartography Table"
     *
     * <p>Derived rather than listed, so a block added later cannot end up without one.
     */
    public static String titleCase(ResourceLocation registryName) {
        return registryName == null ? "Workstation" : titleCase(registryName.getPath());
    }

    public static String titleCase(String path) {
        StringBuilder out = new StringBuilder();

        for (String word : path.split("_")) {
            if (word.isEmpty()) {
                continue;
            }
            if (out.length() > 0) {
                out.append(' ');
            }
            out.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }

        return out.length() == 0 ? "Workstation" : out.toString();
    }
}
