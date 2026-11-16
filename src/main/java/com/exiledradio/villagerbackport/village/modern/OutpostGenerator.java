package com.exiledradio.villagerbackport.village.modern;

import com.exiledradio.villagerbackport.ModConfig;
import com.exiledradio.villagerbackport.VillagerBackport;
import com.exiledradio.villagerbackport.illager.EntityPillager;

import net.minecraft.entity.EnumCreatureType;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraft.world.gen.structure.MapGenStructureIO;
import net.minecraft.world.storage.loot.LootEntryItem;
import net.minecraft.world.storage.loot.LootPool;
import net.minecraft.world.storage.loot.conditions.LootCondition;
import net.minecraft.world.storage.loot.functions.LootFunction;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.IWorldGenerator;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.common.registry.GameRegistry;

import java.util.List;
import java.util.Random;

/**
 * Builds pillager outposts and keeps them manned.
 *
 * <ul>
 *   <li>Generation: from the populate step - see {@link MapGenOutpost} for why.</li>
 *   <li>Spawning: inside an outpost, and twelve blocks round it, the only monster that spawns is the
 *       pillager, at any hour, as 1.14's {@code getSpawnList} has it.</li>
 *   <li>Loot: 1.14's chest, with the crossbow in {@code raids.crossbows} that players can have:
 *       this mod's own, or another mod's when that steps it aside.</li>
 * </ul>
 */
public final class OutpostGenerator implements IWorldGenerator {

    private static final ResourceLocation CHEST = new ResourceLocation(VillagerBackport.MOD_ID, "chests/pillager_outpost");

    /** Low, so outposts are in place before anything decorates the chunk around them. */
    private static final int WEIGHT = 1;

    private OutpostGenerator() {
    }

    public static void register() {
        MapGenStructureIO.registerStructure(OutpostStart.class, "VBP_PillagerOutpost");
        GameRegistry.registerWorldGenerator(new OutpostGenerator(), WEIGHT);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new Events());
        // 1.18 terrain (Caves & Cliffs Backport) shapes itself around outposts, as 1.18's outposts' beard_thin.
        com.exiledradio.backportcore.structure.TerrainAdaptation.register(
                com.exiledradio.backportcore.structure.Structures.PILLAGER_OUTPOST,
                com.exiledradio.backportcore.structure.TerrainAdaptation.Kind.BEARD_THIN, (world, chunkX, chunkZ, out) -> {
                    MapGenOutpost outposts = MapGenOutpost.of(world);
                    if (outposts != null) {
                        outposts.startsNear(world, chunkX, chunkZ, out);
                    }
                });
    }

    @Override
    public void generate(Random random, int chunkX, int chunkZ, World world, IChunkGenerator generator,
                         IChunkProvider provider) {
        MapGenOutpost outposts = MapGenOutpost.of(world);
        if (outposts == null) {
            return;
        }
        try {
            // Plans every outpost within eight chunks; the primer is never touched by a structure.
            outposts.generate(world, chunkX, chunkZ, null);
            if (outposts.generateStructure(world, random, new ChunkPos(chunkX, chunkZ))) {
                // 1.14 snows over the outpost (freeze_top_layer, after the structures); 1.12 snowed before this ran.
                com.exiledradio.backportcore.structure.FreezeTopLayer.afterBuilding(world, outposts, chunkX, chunkZ);
            }
        } catch (RuntimeException e) {
            VillagerBackport.LOGGER.error("Could not build a pillager outpost at chunk {}, {}.", chunkX, chunkZ, e);
        }
    }

    public static final class Events {

        /**
         * The one entry, shared. The spawner asks for the list twice - once to choose, once to check
         * its choice is still allowed - and checks by identity, so a fresh entry each time would never
         * be allowed to spawn.
         */
        private static final Biome.SpawnListEntry PILLAGERS = new Biome.SpawnListEntry(EntityPillager.class, 1, 1, 1);

        @SubscribeEvent
        public void onPotentialSpawns(WorldEvent.PotentialSpawns event) {
            if (event.getType() != EnumCreatureType.MONSTER) {
                return;
            }
            MapGenOutpost outposts = MapGenOutpost.of(event.getWorld());
            if (outposts != null && outposts.isPositionInStructure(event.getWorld(), event.getPos())) {
                List<Biome.SpawnListEntry> list = event.getList();
                list.clear();
                list.add(PILLAGERS);
            }
        }

        @SubscribeEvent
        public void onLootLoad(LootTableLoadEvent event) {
            if (!CHEST.equals(event.getName())) {
                return;
            }
            Item crossbow = installedCrossbow();
            LootPool pool = event.getTable().getPool("pool0");
            if (crossbow != null && pool != null) {
                pool.addEntry(new LootEntryItem(crossbow, 1, 0, new LootFunction[0], new LootCondition[0], "crossbow"));
            }
        }

        /** @return the first crossbow in {@code raids.crossbows} that players can have, or null */
        private static Item installedCrossbow() {
            if (com.exiledradio.villagerbackport.crossbow.CrossbowContent.crossbow != null) {
                return com.exiledradio.villagerbackport.crossbow.CrossbowContent.crossbow;
            }
            for (String name : ModConfig.raids.crossbows) {
                Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(name.trim()));
                if (item != null && item != Items.AIR
                        && item != com.exiledradio.villagerbackport.illager.IllagerContent.pillagerCrossbow) {
                    return item;
                }
            }
            return null;
        }
    }
}
