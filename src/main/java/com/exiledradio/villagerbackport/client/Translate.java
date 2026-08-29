package com.exiledradio.villagerbackport.client;

import net.minecraft.client.resources.I18n;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Localised strings with a built-in English fallback.
 *
 * <h2>Why this exists</h2>
 * {@link I18n#format} returns the key unchanged when it cannot resolve it, which is how untranslated
 * text ends up rendered as {@code gui.villagerbackport.level.3} on screen. That happened for three
 * releases even though the lang file was present, correctly named and at the right path in the
 * built jar.
 *
 * <p>Rather than leave the interface showing raw keys, every lookup falls back to a hardcoded
 * English string when the key does not resolve. Translations still work wherever the lang file loads
 * normally - this only takes over when it does not.
 *
 * <p>The cause was a missing {@code pack.mcmeta}, and is fixed; see
 * {@link com.exiledradio.villagerbackport.block.Names} for the chain. This is kept as a safety net
 * rather than removed, since a pack can break resource loading in other ways.
 */
@SideOnly(Side.CLIENT)
public final class Translate {

    private Translate() {
    }

    /**
     * @param key      translation key
     * @param fallback English text to use if the key does not resolve
     */
    public static String of(String key, String fallback) {
        String translated = I18n.format(key);
        return key.equals(translated) ? fallback : translated;
    }

    /** As {@link #of(String, String)}, with format arguments applied to whichever string is used. */
    public static String of(String key, String fallbackFormat, Object... args) {
        String translated = I18n.format(key, args);
        if (!translated.contains(key)) {
            return translated;
        }
        return String.format(fallbackFormat, args);
    }

    /**
     * Turns an unresolved villager name into a readable one.
     *
     * <p>A villager's name is the translation of {@code entity.Villager.<career>}, built server-side
     * and resolved on arrival. A career this mod adds has no vanilla translation, and in a pack where
     * this mod's language file is not loaded there is no translation at all - either way the trade
     * screen ends up captioned {@code entity.Villager.mason}.
     *
     * <p>Anything still carrying that prefix is turned into a title-cased career name, so a new
     * career reads correctly without needing an entry anywhere.
     */
    public static String villagerName(String name) {
        final String prefix = "entity.Villager.";
        if (name == null || !name.startsWith(prefix)) {
            return name;
        }

        String career = name.substring(prefix.length());
        if (career.isEmpty()) {
            return name;
        }

        return Character.toUpperCase(career.charAt(0)) + career.substring(1);
    }

    /** Villager rank name for a level, 1 to 5. */
    public static String levelName(int level) {
        return of("gui.villagerbackport.level." + level, defaultLevelName(level));
    }

    private static String defaultLevelName(int level) {
        switch (level) {
            case 1:
                return "Novice";
            case 2:
                return "Apprentice";
            case 3:
                return "Journeyman";
            case 4:
                return "Expert";
            case 5:
                return "Master";
            default:
                return "Novice";
        }
    }
}
