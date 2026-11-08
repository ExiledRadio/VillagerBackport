package com.exiledradio.villagerbackport.village.modern;

import com.exiledradio.backportcore.locate.StructureLocate;
import com.exiledradio.backportcore.structure.ScatteredPlacement;

import com.exiledradio.villagerbackport.ModConfig;

import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.gen.ChunkGeneratorOverworld;
import net.minecraft.world.gen.IChunkGenerator;

import java.util.Locale;

/**
 * This mod's structures for Backport Core's {@code /locate structure}, under today's ids: the five village types
 * ({@code minecraft:village_plains} ...; tag {@code #minecraft:village}) and {@code minecraft:pillager_outpost}.
 *
 * <p>Villages are found on the grid {@link MapGenModernVillage} places them on, whichever terrain it stands in for
 * (vanilla's, superflat, Open Terrain Generator, Biomes O' Plenty, RTG, Caves Not Cliffs), each start named by the
 * type its biome gives it. Where 1.14 villages are not built (villages.style LEGACY, or terrain they cannot be
 * planned on) the types say why, and {@code #minecraft:village} falls back to Core's 1.12.2 village search, so the
 * world's own villages are still found. {@code /locate Village} keeps working the same way, unlisted.
 */
public final class VillageLocate {

    /** 1.14's village grid salt ({@code VillageFeature}), which is 1.12.2's. */
    static final int VILLAGE_SALT = 10387312;
    static final int SEPARATION = 8;

    private VillageLocate() {
    }

    /** @return today's id of a 1.14 village type */
    public static String id(ModernVillageStart.Type type) {
        return "minecraft:village_" + type.name().toLowerCase(Locale.ROOT);
    }

    public static void register() {
        ModernVillageStart.Type[] types = ModernVillageStart.Type.values();
        String[] ids = new String[types.length];
        for (int i = 0; i < types.length; i++) {
            ids[i] = id(types[i]);
        }
        StructureLocate.placement("minecraft:villages", world -> {
            MapGenModernVillage villages = MapGenModernVillage.of(world);
            if (villages == null) {
                if (world.provider.getDimension() != 0 || !world.getWorldInfo().isMapFeaturesEnabled()) {
                    return null;
                }
                throw whyNoModernVillages(world);
            }
            return new StructureLocate.Grid(new ScatteredPlacement(villages.spacing(), SEPARATION, VILLAGE_SALT), (x, z) -> {
                ModernVillageStart.Type type = villages.startTypeAt(world, x, z);
                return type == null ? null : id(type);
            });
        }, ids);
        StructureLocate.tag("minecraft:village", ids);

        StructureLocate.placement("minecraft:pillager_outposts", world -> {
            MapGenOutpost outposts = MapGenOutpost.of(world);
            if (outposts == null) {
                if (world.provider.getDimension() != 0 || !world.getWorldInfo().isMapFeaturesEnabled()) {
                    return null;
                }
                throw new StructureLocate.Unavailable("commands.villagerbackport.locate.nooutposts");
            }
            return new StructureLocate.Grid(new ScatteredPlacement(outposts.spacing(), SEPARATION, MapGenOutpost.salt()),
                    (x, z) -> outposts.startsAt(world, x, z) ? "minecraft:pillager_outpost" : null);
        }, "minecraft:pillager_outpost");
    }

    /**
     * Says why this world has no 1.14 villages to locate, so a player in a world with its own terrain generator
     * (Dregora's OTG, say) learns that is the reason rather than suspecting the mod.
     */
    static StructureLocate.Unavailable whyNoModernVillages(WorldServer world) {
        if (ModConfig.villages.style != MapGenModernVillage.Style.MODERN) {
            return new StructureLocate.Unavailable("commands.villagerbackport.locate.legacy");
        }
        IChunkGenerator generator = world.getChunkProvider().chunkGenerator;
        if (generator != null && !(generator instanceof ChunkGeneratorOverworld)) {
            return new StructureLocate.Unavailable("commands.villagerbackport.locate.othergen", ownerOf(generator.getClass(), world));
        }
        return new StructureLocate.Unavailable("commands.villagerbackport.locate.nomodern");
    }

    /**
     * @return the name of the mod a generator comes from, or for one of vanilla's own (a superflat world, say) the
     *         world type's name
     */
    private static String ownerOf(Class<?> type, World world) {
        try {
            java.io.File source = ModVillagePieces.jarOf(type);
            for (net.minecraftforge.fml.common.ModContainer mod : net.minecraftforge.fml.common.Loader.instance().getModList()) {
                if (mod.getSource() != null && source.equals(mod.getSource().getCanonicalFile())) {
                    return mod.getName();
                }
            }
        } catch (Exception | LinkageError e) {
            // Fall through to the world type's name.
        }
        return '"' + world.getWorldType().getName() + '"';
    }
}
