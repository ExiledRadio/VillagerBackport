package com.exiledradio.villagerbackport.block;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.translation.I18n;

/**
 * Readable names when the language file is not loaded.
 *
 * <h2>Why this keeps coming up</h2>
 * This mod's language file is present in the jar, correctly named and keyed, and other resources
 * beside it load without trouble - the block textures and models all render. Only the translations
 * are skipped, which has already surfaced twice: as block names reading
 * {@code tile.villagerbackport.stonecutter.name}, and as a barrel titled
 * {@code container.villagerbackport.barrel}.
 *
 * <p>The likeliest explanation is how the two are fetched. A texture is looked up by explicit path,
 * whereas translations are gathered by walking every resource domain the game knows about - so a mod
 * absent from that list loses its translations while keeping everything else. What removes it in a
 * pack of this size has not been identified.
 *
 * <p>Rather than patch each place separately as it appears, everything user-facing goes through here.
 * A translation is used when one exists, so this is invisible on an installation where the language
 * file loads normally - which includes a plain 1.12.2 install.
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
