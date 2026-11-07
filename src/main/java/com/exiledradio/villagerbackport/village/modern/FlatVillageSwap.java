package com.exiledradio.villagerbackport.village.modern;

import com.exiledradio.backportcore.terrain.FlatTerrain;
import com.exiledradio.villagerbackport.VillagerBackport;

import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraft.world.gen.structure.MapGenStructure;
import net.minecraft.world.gen.structure.MapGenVillage;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Map;

/**
 * Swaps a superflat world's village generator for this mod's before it makes any chunk.
 *
 * <p>{@code ChunkGeneratorFlat} builds its village generator straight into its own map of structures,
 * without the {@code InitMapGenEvent} every other generator posts, so {@link MapGenModernVillage}'s
 * usual swap never sees it. It is swapped here instead, keeping the preset's own village size and
 * spacing. Reading the map, and why this has to happen as the spawn search begins, is Backport Core's
 * {@link FlatTerrain}.
 */
public final class FlatVillageSwap {

    /** A new world, before its spawn search generates anything. */
    @SubscribeEvent
    public void onCreateSpawn(WorldEvent.CreateSpawnPosition event) {
        swap(event.getWorld());
    }

    /** A world that already has its spawn. Does nothing if the spawn search already swapped it. */
    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event) {
        swap(event.getWorld());
    }

    private static void swap(World world) {
        if (!(world instanceof WorldServer)) {
            return;
        }
        IChunkGenerator generator = ((WorldServer) world).getChunkProvider().chunkGenerator;
        if (!FlatTerrain.handles(generator)) {
            return;
        }
        // A preset without "village" has no village generator, and gets none.
        Map<String, MapGenStructure> structures = FlatTerrain.structures(generator);
        if (structures == null) {
            VillagerBackport.LOGGER.warn("A superflat world's structures could not be read, so its villages stay 1.12.2's.");
            return;
        }
        MapGenStructure villages = structures.get("Village");
        if (villages != null && villages.getClass() == MapGenVillage.class) {
            structures.put("Village", new MapGenModernVillage((MapGenVillage) villages));
            VillagerBackport.LOGGER.info("Superflat world: building this mod's villages.");
        }
    }
}
