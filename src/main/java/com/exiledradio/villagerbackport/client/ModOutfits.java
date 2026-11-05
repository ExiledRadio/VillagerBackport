package com.exiledradio.villagerbackport.client;

import com.exiledradio.villagerbackport.ModConfig;
import com.exiledradio.villagerbackport.VillagerBackport;

import net.minecraftforge.fml.common.registry.VillagerRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * {@code display.modOutfits}: which 1.14 outfit another mod's villagers wear.
 *
 * <p>Another mod's profession comes with a texture of its own, a whole 1.12.2-style villager, and
 * there is no telling which part of it is the clothes. So by default such villagers keep it, with
 * 1.14's rank badge drawn on. A pack that would rather see them in 1.14's clothes names the closest
 * outfit here, by profession or by a single career:
 * <pre>
 *   somemod:chef=butcher            every career of the profession somemod:chef
 *   minecraft:farmer/beekeeper=farmer   one career, here one a mod added to a vanilla profession
 * </pre>
 * A mapped villager is dressed exactly as a vanilla one: biome clothing, the outfit, its hat and the
 * badge. {@code none} gives the biome clothing alone.
 */
@SideOnly(Side.CLIENT)
final class ModOutfits {

    /** The value meaning "biome clothing only". */
    static final String NONE = "none";

    /** 1.14's outfits, by the names its textures use. */
    static final Set<String> OUTFITS = new HashSet<String>(Arrays.asList("armorer", "butcher", "cartographer",
            "cleric", "farmer", "fisherman", "fletcher", "leatherworker", "librarian", "mason", "nitwit", "shepherd",
            "toolsmith", "weaponsmith", NONE));

    private static String[] parsedFrom;
    private static final Map<String, String> MAP = new HashMap<String, String>();

    private ModOutfits() {
    }

    /**
     * @return the 1.14 outfit name this career is mapped to, {@link #NONE}, or null if the setting
     * does not mention it
     */
    @Nullable
    static String outfitFor(VillagerRegistry.VillagerProfession profession,
                            @Nullable VillagerRegistry.VillagerCareer career) {
        parse();
        if (profession == null || profession.getRegistryName() == null) {
            return null;
        }
        String key = profession.getRegistryName().toString();
        if (career != null) {
            String byCareer = MAP.get(key + "/" + career.getName().toLowerCase(Locale.ROOT));
            if (byCareer != null) {
                return byCareer;
            }
        }
        return MAP.get(key);
    }

    /** @return true if anything at all is mapped for this profession, by itself or by a career */
    static boolean mentions(VillagerRegistry.VillagerProfession profession) {
        parse();
        if (profession == null || profession.getRegistryName() == null) {
            return false;
        }
        String key = profession.getRegistryName().toString();
        for (String entry : MAP.keySet()) {
            if (entry.equals(key) || entry.startsWith(key + "/")) {
                return true;
            }
        }
        return false;
    }

    /** Re-reads the setting whenever it has been changed, in the file or the config screen. */
    private static synchronized void parse() {
        String[] lines = ModConfig.display.modOutfits;
        if (lines == parsedFrom) {
            return;
        }
        parsedFrom = lines;
        MAP.clear();
        for (String line : lines) {
            int equals = line.lastIndexOf('=');
            if (equals <= 0) {
                continue;
            }
            String key = line.substring(0, equals).trim().toLowerCase(Locale.ROOT);
            String outfit = line.substring(equals + 1).trim().toLowerCase(Locale.ROOT);
            if (!OUTFITS.contains(outfit)) {
                VillagerBackport.LOGGER.warn("display.modOutfits: '{}' is not one of 1.14's outfits ({}); ignoring '{}'.",
                        outfit, OUTFITS, line);
                continue;
            }
            MAP.put(key, outfit);
        }
        // A changed mapping changes the composed skins; the old ones are stale.
        VillagerSkinComposer.clear();
    }
}
