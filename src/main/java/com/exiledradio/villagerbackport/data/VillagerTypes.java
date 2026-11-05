package com.exiledradio.villagerbackport.data;

import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeDesert;
import net.minecraft.world.biome.BiomeJungle;
import net.minecraft.world.biome.BiomeSavanna;
import net.minecraft.world.biome.BiomeSnow;
import net.minecraft.world.biome.BiomeSwamp;
import net.minecraft.world.biome.BiomeTaiga;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Random;

/**
 * A villager's type: which of 1.14's seven biome outfits it wears, for life.
 *
 * <h2>Fixed, not read from where it stands</h2>
 * 1.14 gives a villager its type when it spawns, from the biome it spawns in, and keeps it: a desert
 * villager led into a taiga is still a desert villager. This mod first read the biome under the
 * villager every frame instead, so a villager changed clothes as it walked across a biome border.
 *
 * <p>The type is stored with the rest of this mod's villager data the first time a villager enters
 * the world - spawned, generated with a village, or loaded from an older save, which takes the biome
 * it stands in then. The client learns it alongside the career; see {@code PacketVillagerLevel}.
 *
 * <h2>Babies</h2>
 * 1.14's rule: half the time the biome the baby is born in, otherwise one parent's type, each parent
 * equally likely.
 *
 * <h2>Zombies</h2>
 * Nothing to do. A zombified villager carries this mod's data across and back again (see
 * {@code Zombification}), so a cured villager wears what it wore before. One that was only ever a
 * zombie takes the biome it is cured in.
 */
public final class VillagerTypes {

    /** 1.14's types, in its order. The index is what goes over the network. */
    public static final String[] NAMES = {"plains", "desert", "jungle", "savanna", "snow", "swamp", "taiga"};

    private static final String KEY = "Type";

    /** @return this villager's type, settling it from the biome it stands in if it has none yet */
    public static String of(EntityVillager villager) {
        NBTTagCompound root = VillagerTradeData.root(villager);
        String type = root.getString(KEY);
        if (indexOf(type) < 0) {
            type = forBiome(villager.world.getBiome(new BlockPos(villager)));
            root.setString(KEY, type);
        }
        return type;
    }

    /** @return the index of this villager's type, for the network */
    public static int indexOf(EntityVillager villager) {
        return indexOf(of(villager));
    }

    public static void set(EntityVillager villager, String type) {
        if (indexOf(type) >= 0) {
            VillagerTradeData.root(villager).setString(KEY, type);
        }
    }

    /** 1.14's rule for a baby: the biome it is born in half the time, otherwise a parent's type. */
    public static String forBaby(EntityVillager parent, EntityVillager mate, Random random) {
        double roll = random.nextDouble();
        if (roll < 0.5D) {
            return forBiome(parent.world.getBiome(new BlockPos(parent)));
        }
        return roll < 0.75D ? of(parent) : of(mate);
    }

    public static int indexOf(String type) {
        for (int i = 0; i < NAMES.length; i++) {
            if (NAMES[i].equals(type)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Classifies a biome into one of 1.14's seven villager types.
     *
     * <p>Done by biome class and temperature rather than by name, so biomes from other mods - and
     * Dregora and Biomes O' Plenty add a great many - land somewhere sensible instead of all
     * defaulting to plains.
     */
    public static String forBiome(Biome biome) {
        if (biome instanceof BiomeDesert) {
            return "desert";
        }
        if (biome instanceof BiomeJungle) {
            return "jungle";
        }
        if (biome instanceof BiomeSavanna) {
            return "savanna";
        }
        if (biome instanceof BiomeSwamp) {
            return "swamp";
        }
        if (biome instanceof BiomeSnow || biome.isSnowyBiome()) {
            return "snow";
        }
        if (biome instanceof BiomeTaiga) {
            return "taiga";
        }

        // The game's list puts every badlands biome with the desert and every windswept hills with
        // the taiga, and neither is a subclass of those here.
        if (biome instanceof net.minecraft.world.biome.BiomeMesa) {
            return "desert";
        }
        if (biome instanceof net.minecraft.world.biome.BiomeHills) {
            return "taiga";
        }

        // Any other vanilla biome is plains, as the game has it - the Nether and stony shore
        // included, which the climate reading below would have dressed as desert and taiga.
        if (biome.getRegistryName() != null && "minecraft".equals(biome.getRegistryName().getNamespace())) {
            return "plains";
        }

        // Modded biomes rarely extend the vanilla classes, so fall back to climate. The game would
        // call all of them plains, which in a pack made of modded biomes would be every villager.
        float temperature = biome.getDefaultTemperature();
        if (temperature <= 0.05F) {
            return "snow";
        }
        if (temperature <= 0.3F) {
            return "taiga";
        }
        if (temperature >= 1.5F && biome.getRainfall() < 0.3F) {
            return "desert";
        }
        if (temperature >= 1.0F && biome.getRainfall() >= 0.8F) {
            return "jungle";
        }
        if (temperature >= 1.0F) {
            return "savanna";
        }

        return "plains";
    }

    /** Settles the type of every villager as it enters a server world, before anyone can see it. */
    @SubscribeEvent
    public void onJoin(EntityJoinWorldEvent event) {
        Entity entity = event.getEntity();
        if (!event.getWorld().isRemote && entity instanceof EntityVillager) {
            of((EntityVillager) entity);
        }
    }
}
