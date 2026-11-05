package com.exiledradio.villagerbackport.client;

import com.exiledradio.villagerbackport.compat.VillagerAccess;

import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.registry.VillagerRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * Picks which 1.14 textures make up a villager's appearance.
 *
 * <h2>How 1.14 builds a villager</h2>
 * Four textures stacked, not one: a plain body, a biome outfit, a profession outfit, and the rank
 * badge. 1.12.2 has a single flat texture per profession, which is why villagers still looked old
 * once the badge alone was ported.
 *
 * <h2>Profession versus career</h2>
 * 1.12.2 splits the same idea across two levels - six professions, each with careers underneath -
 * and it is the <em>career</em> that lines up with a 1.14 profession. A 1.12.2 "farmer" profession
 * covers farmer, fisherman, shepherd and fletcher, which are four separate looks in 1.14. So the
 * mapping is keyed on career name rather than profession, which is also why the vanilla renderer
 * could never do this on its own: it picks its texture per profession.
 *
 * <p>Career names are read from the registry rather than assumed by index, so a career another mod
 * inserted cannot shift everything after it onto the wrong outfit.
 *
 * <h2>Unrecognised villagers keep their own look</h2>
 * A career with no 1.14 counterpart - anything another mod registered - returns null and is left
 * entirely alone. Ice and Fire registers its own villagers, and dressing them in a vanilla outfit
 * because they happen to sit under a vanilla profession would be worse than leaving them be.
 */
@SideOnly(Side.CLIENT)
public final class VillagerSkin {

    private static final String ROOT = "villagerbackport";

    /** 1.14's plain villager body, drawn under everything else. */
    public static final ResourceLocation BASE =
            new ResourceLocation(ROOT, "textures/entity/villager/villager.png");

    /** 1.12.2 career name to the matching 1.14 profession texture. */
    private static final Map<String, ResourceLocation> PROFESSIONS = new HashMap<String, ResourceLocation>();

    static {
        // Names as registered in VillagerRegistry's vanilla careers. Three of them are abbreviated
        // there - "armor", "weapon" and "tool" - where 1.14 spells them out.
        profession("farmer", "farmer");
        profession("fisherman", "fisherman");
        profession("shepherd", "shepherd");
        profession("fletcher", "fletcher");
        profession("librarian", "librarian");
        profession("cartographer", "cartographer");
        profession("cleric", "cleric");
        profession("armor", "armorer");
        profession("weapon", "weaponsmith");
        profession("tool", "toolsmith");
        profession("butcher", "butcher");
        profession("leather", "leatherworker");
        profession("nitwit", "nitwit");

        // Added by this mod, since 1.12.2 has no mason of its own.
        profession("mason", "mason");
    }

    /** Registry names of the six professions 1.12.2 ships with. */
    private static final java.util.Set<String> VANILLA_PROFESSIONS =
            new java.util.HashSet<String>(java.util.Arrays.asList(
                    "minecraft:farmer", "minecraft:librarian", "minecraft:priest",
                    "minecraft:smith", "minecraft:butcher", "minecraft:nitwit"));

    /**
     * The outfit for a career another mod added to a vanilla profession, by that profession: the
     * 1.12.2 texture it would have worn was the profession's.
     */
    private static final Map<String, String> VANILLA_DEFAULTS = new HashMap<String, String>();

    static {
        VANILLA_DEFAULTS.put("minecraft:farmer", "farmer");
        VANILLA_DEFAULTS.put("minecraft:librarian", "librarian");
        VANILLA_DEFAULTS.put("minecraft:priest", "cleric");
        VANILLA_DEFAULTS.put("minecraft:smith", "toolsmith");
        VANILLA_DEFAULTS.put("minecraft:butcher", "butcher");
        VANILLA_DEFAULTS.put("minecraft:nitwit", "nitwit");
    }

    /** Rank badge textures, indexed by level 1 through 5. */
    private static final ResourceLocation[] BADGES = {
            badge("stone"), badge("iron"), badge("gold"), badge("emerald"), badge("diamond")
    };

    private VillagerSkin() {
    }

    private static ResourceLocation badge(String name) {
        return new ResourceLocation(ROOT, "textures/entity/villager/level/" + name + ".png");
    }

    /** @return the rank badge for a level, or null when the level is outside the known range. */
    @Nullable
    public static ResourceLocation badgeFor(int level) {
        return level >= 1 && level <= BADGES.length ? BADGES[level - 1] : null;
    }

    /**
     * @return true if this profession is one of 1.12.2's own, and so safe to restyle.
     *
     * <p>Professions from other mods are left entirely alone - both here and in the suppressor -
     * because there is no 1.14 equivalent to dress them in and blanking them would leave them
     * invisible.
     */
    public static boolean isVanilla(VillagerRegistry.VillagerProfession profession) {
        return profession != null && profession.getRegistryName() != null
                && VANILLA_PROFESSIONS.contains(profession.getRegistryName().toString());
    }

    /** @return true if this villager wears the nitwit profession, which 1.14 never badges. */
    public static boolean isNitwit(EntityVillager villager) {
        try {
            VillagerRegistry.VillagerProfession profession = villager.getProfessionForge();
            return profession != null && profession.getRegistryName() != null
                    && "minecraft:nitwit".equals(profession.getRegistryName().toString());
        } catch (RuntimeException e) {
            return false;
        }
    }

    public static boolean isVanillaProfession(EntityVillager villager) {
        try {
            return isVanilla(villager.getProfessionForge());
        } catch (RuntimeException e) {
            return false;
        }
    }

    /** @return the last path segment of a texture, for building a cache key. */
    public static String name(@Nullable ResourceLocation location) {
        if (location == null) {
            return "none";
        }
        String path = location.getPath();
        int slash = path.lastIndexOf('/');
        int dot = path.lastIndexOf('.');
        return path.substring(slash + 1, dot < slash ? path.length() : dot);
    }

    /**
     * @return true if this villager is dressed in 1.14's clothes: a vanilla profession, or another
     * mod's that {@code display.modOutfits} maps
     */
    public static boolean dressesAsVanilla(EntityVillager villager) {
        try {
            VillagerRegistry.VillagerProfession profession = villager.getProfessionForge();
            if (isVanilla(profession)) {
                return true;
            }
            if (profession == null || !ModOutfits.mentions(profession)) {
                return false;
            }
            int careerId = VillagerLevelCache.career(villager);
            VillagerRegistry.VillagerCareer career = careerId > 0 ? profession.getCareer(careerId - 1) : null;
            return ModOutfits.outfitFor(profession, career) != null;
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static ResourceLocation outfit(String name) {
        return new ResourceLocation(ROOT, "textures/entity/villager/profession/" + name + ".png");
    }

    private static void profession(String career, String texture) {
        PROFESSIONS.put(career, new ResourceLocation(ROOT, "textures/entity/villager/profession/" + texture + ".png"));
    }

    /**
     * @return the 1.14 profession outfit for this villager, or null if it has no counterpart and
     * should be left with whatever texture it already had.
     */
    @Nullable
    public static ResourceLocation professionFor(EntityVillager villager) {
        try {
            VillagerRegistry.VillagerProfession profession = villager.getProfessionForge();
            if (profession == null) {
                return null;
            }

            // Read from what the server sent, not from the entity: careerId is never synced, so on
            // the client the field is always zero and every villager would look unemployed.
            int careerId = VillagerLevelCache.career(villager);
            if (careerId <= 0) {
                return null;
            }

            VillagerRegistry.VillagerCareer career = profession.getCareer(careerId - 1);
            if (career == null) {
                return null;
            }
            String mapped = ModOutfits.outfitFor(profession, career);
            if (mapped != null) {
                return ModOutfits.NONE.equals(mapped) ? null : outfit(mapped);
            }
            if (!isVanilla(profession)) {
                return null;
            }
            ResourceLocation own = PROFESSIONS.get(career.getName());
            if (own != null) {
                return own;
            }
            // A career another mod added to a vanilla profession: that profession's outfit.
            String fallback = VANILLA_DEFAULTS.get(String.valueOf(profession.getRegistryName()));
            return fallback == null ? null : outfit(fallback);
        } catch (RuntimeException e) {
            // getCareer throws on an out-of-range id, which a mod reassigning professions can cause.
            return null;
        }
    }

    /**
     * @return the biome outfit to wear: the type the server settled when the villager spawned.
     *
     * <p>Kept for life, as in 1.14 - see {@code VillagerTypes}. Until the server's word arrives the
     * biome it stands in stands in for it, which is what the type was settled from anyway.
     */
    public static ResourceLocation typeFor(EntityVillager villager) {
        String type = VillagerLevelCache.type(villager);
        if (type == null) {
            type = com.exiledradio.villagerbackport.data.VillagerTypes.forBiome(
                    villager.world.getBiome(new BlockPos(villager)));
        }
        return type(type);
    }

    private static ResourceLocation type(String name) {
        return new ResourceLocation(ROOT, "textures/entity/villager/type/" + name + ".png");
    }
}
