package com.exiledradio.villagerbackport.fox;

import com.exiledradio.backportcore.creative.CreativePlacement;
import com.exiledradio.villagerbackport.ModConfig;
import com.exiledradio.villagerbackport.VillagerBackport;
import com.exiledradio.villagerbackport.compat.FutureMc;

import net.minecraft.block.Block;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityEntryBuilder;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.oredict.OreDictionary;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Foxes and sweet berries: what is registered, and whose copy is in use when Future MC adds them too.
 *
 * <h2>Registered whenever {@code foxes.enabled} is on</h2>
 * The bush, the berries and the fox are always registered with the switch on, Future MC or not, so a
 * world never loses them because a mod was added or a setting changed. What the duplicate setting
 * changes is only which copy is <i>live</i>:
 *
 * <h2>Living alongside Future MC</h2>
 * Future MC adds sweet berries and their bush (and, in some versions, a fox) under the same names.
 * {@code compat.futureMcDuplicates} - the same setting that decides between the two mods' barrels -
 * picks one:
 * <ul>
 * <li><b>KEEP_OURS</b> (default): this mod's bushes generate and its foxes spawn. Future MC's berries
 * are hidden from JEI and the creative menu, and its fox, if it has one, no longer spawns naturally.
 * Its own bushes still generate, since that is its world generator; switch them off in Future MC's
 * config ({@code berry bush > Spawn Rate}) to have only these.</li>
 * <li><b>KEEP_FUTURE_MC</b>: this mod's bushes stop generating and its foxes stop spawning, and its
 * berries are hidden. Trades and village fields use Future MC's berries.</li>
 * <li><b>SHOW_BOTH</b>: both, side by side.</li>
 * </ul>
 * Foxes eat, breed on and pick either mod's berries in every mode.
 */
@Mod.EventBusSubscriber(modid = VillagerBackport.MOD_ID)
public final class FoxContent {

    /** Network id within this mod; the plan in docs/V14_COMPLETION_PLAN.md gives foxes 108. */
    private static final int FOX_ID = 108;

    /** 1.14's spawn egg colors. */
    private static final int EGG_BASE = 14005919;
    private static final int EGG_SPOTS = 13396256;

    @Nullable
    public static BlockSweetBerryBush bush;
    @Nullable
    public static ItemSweetBerries sweetBerries;

    private static final Map<String, SoundEvent> SOUNDS = new HashMap<String, SoundEvent>();

    /** Every biome foxes were added to, and whether its foxes are snow foxes. Filled in {@link #init}. */
    private static final Map<Biome, Boolean> FOX_BIOMES = new HashMap<Biome, Boolean>();

    private FoxContent() {
    }

    /** @return true if this mod's foxes and berries are registered in this game - fixed for the session */
    public static boolean registered() {
        return ModConfig.foxes.enabled;
    }

    /**
     * @return true if this mod's copies are the ones in use: its bushes generate, its foxes spawn and
     * its berries are listed
     */
    public static boolean live() {
        return registered() && !(ModConfig.compat.futureMcDuplicates == FutureMc.Duplicates.KEEP_FUTURE_MC
                && futureMcBerries() != null);
    }

    /** @return Future MC's berries, or null when it is not installed or has them switched off */
    @Nullable
    public static Item futureMcBerries() {
        return item(FutureMc.MOD_ID, "sweet_berries");
    }

    /**
     * @return the sweet berries everything else should use - trades, village fields - or null when no
     * mod adds any
     */
    @Nullable
    public static Item berries() {
        if (live()) {
            return sweetBerries;
        }
        Item theirs = futureMcBerries();
        return theirs != null ? theirs : sweetBerries;
    }

    /** @return the bush that goes with {@link #berries()}, or null */
    @Nullable
    public static Block berryBush() {
        if (live()) {
            return bush;
        }
        Block theirs = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(FutureMc.MOD_ID, "sweet_berry_bush"));
        return theirs != null && theirs != Blocks.AIR ? theirs : bush;
    }

    /** @return true for either mod's sweet berries, or anything in the ore dictionary as them */
    public static boolean isBerries(net.minecraft.item.ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        Item item = stack.getItem();
        if (item == sweetBerries || (item != null && item == futureMcBerries())) {
            return true;
        }
        for (int id : OreDictionary.getOreIDs(stack)) {
            if ("cropSweetberry".equals(OreDictionary.getOreName(id))) {
                return true;
            }
        }
        return false;
    }

    /** @return items JEI and the creative menu should not show: the hidden half of each duplicate */
    public static Set<Item> hiddenItems() {
        Item theirs = futureMcBerries();
        if (!registered() || theirs == null) {
            return Collections.emptySet();
        }
        switch (ModConfig.compat.futureMcDuplicates) {
            case KEEP_OURS:
                return Collections.singleton(theirs);
            case KEEP_FUTURE_MC:
                return Collections.<Item>singleton(sweetBerries);
            default:
                return Collections.emptySet();
        }
    }

    /** @return the sound under this short name: fox.ambient as "ambient", the bush's as "bushBreak", ... */
    public static SoundEvent sound(String name) {
        SoundEvent sound = SOUNDS.get(name);
        if (sound == null) {
            throw new IllegalStateException("No fox sound '" + name + "'");
        }
        return sound;
    }

    // ------------------------------------------------------------------------------- registration

    @SubscribeEvent
    public static void registerSounds(RegistryEvent.Register<SoundEvent> event) {
        if (!registered()) {
            return;
        }
        String[][] sounds = {
                {"ambient", "entity.fox.ambient"}, {"hurt", "entity.fox.hurt"}, {"death", "entity.fox.death"},
                {"aggro", "entity.fox.aggro"}, {"sniff", "entity.fox.sniff"}, {"bite", "entity.fox.bite"},
                {"eat", "entity.fox.eat"}, {"screech", "entity.fox.screech"}, {"sleep", "entity.fox.sleep"},
                {"spit", "entity.fox.spit"}, {"bushBreak", "block.sweet_berry_bush.break"},
                {"bushPlace", "block.sweet_berry_bush.place"}, {"thorns", "entity.player.hurt_sweet_berry_bush"},
                {"pick", "item.sweet_berries.pick_from_bush"},
        };
        for (String[] sound : sounds) {
            // The bush's two were made early (see ensureSounds); register those same objects.
            SoundEvent made = SOUNDS.get(sound[0]);
            if (made == null) {
                made = early(sound[1]);
                SOUNDS.put(sound[0], made);
            }
            event.getRegistry().register(made);
        }
    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        if (!registered()) {
            return;
        }
        // Sound events come later than blocks, so the bush's sound type is filled in from names now
        // and swapped for the registered events below.
        ensureSounds();
        bush = new BlockSweetBerryBush();
        bush.setRegistryName(new ResourceLocation(VillagerBackport.MOD_ID, "sweet_berry_bush"));
        bush.setTranslationKey(VillagerBackport.MOD_ID + ".sweet_berry_bush");
        event.getRegistry().register(bush);
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        if (!registered()) {
            return;
        }
        sweetBerries = new ItemSweetBerries();
        sweetBerries.setRegistryName(new ResourceLocation(VillagerBackport.MOD_ID, "sweet_berries"));
        sweetBerries.setTranslationKey(VillagerBackport.MOD_ID + ".sweet_berries");
        CreativePlacement.place(sweetBerries);
        event.getRegistry().register(sweetBerries);
    }

    @SubscribeEvent
    public static void registerEntities(RegistryEvent.Register<EntityEntry> event) {
        if (!registered()) {
            return;
        }
        event.getRegistry().register(EntityEntryBuilder.create().entity(EntityFox.class)
                .id(new ResourceLocation(VillagerBackport.MOD_ID, "fox"), FOX_ID)
                .name(VillagerBackport.MOD_ID + ".fox").egg(EGG_BASE, EGG_SPOTS).tracker(80, 3, true).build());
    }

    /**
     * Block registration runs before sound registration, but the bush wants its sounds in its
     * constructor. Creating the events early and registering the same objects later is fine: a
     * {@link SoundEvent} is only a name until the sound registry holds it.
     */
    private static void ensureSounds() {
        if (!SOUNDS.isEmpty()) {
            return;
        }
        SOUNDS.put("bushBreak", early("block.sweet_berry_bush.break"));
        SOUNDS.put("bushPlace", early("block.sweet_berry_bush.place"));
    }

    private static SoundEvent early(String path) {
        ResourceLocation name = new ResourceLocation(VillagerBackport.MOD_ID, path);
        return new SoundEvent(name).setRegistryName(name);
    }

    // ---------------------------------------------------------------------------------- lifecycle

    /** From the mod's init: ore names, the composter, spawns and world generation. */
    public static void init() {
        if (!registered()) {
            return;
        }
        OreDictionary.registerOre("cropSweetberry", sweetBerries);
        OreDictionary.registerOre("listAllberry", sweetBerries);
        com.exiledradio.villagerbackport.block.Compostables.register(0.3F, sweetBerries);
        Item theirs = futureMcBerries();
        if (theirs != null) {
            com.exiledradio.villagerbackport.block.Compostables.register(0.3F, theirs);
        }

        for (Item hidden : hiddenItems()) {
            hidden.setCreativeTab(null);
        }

        if (!live()) {
            VillagerBackport.LOGGER.info("Future MC's sweet berries are kept; this mod's bushes and foxes stay "
                    + "registered but do not generate or spawn (compat.futureMcDuplicates).");
            return;
        }
        GameRegistry.registerWorldGenerator(new BerryBushGenerator(), 10);
        addSpawns();
    }

    private static void addSpawns() {
        Set<String> extra = new HashSet<String>(Arrays.asList(ModConfig.foxes.extraFoxBiomes));
        Set<String> blacklist = new HashSet<String>(Arrays.asList(ModConfig.foxes.foxBiomeBlacklist));
        List<Biome> red = new ArrayList<Biome>();
        List<Biome> snow = new ArrayList<Biome>();

        for (Biome biome : ForgeRegistries.BIOMES) {
            String name = String.valueOf(biome.getRegistryName());
            if (blacklist.contains(name)) {
                continue;
            }
            if (extra.contains(name) || BiomeDictionary.hasType(biome, BiomeDictionary.Type.CONIFEROUS)) {
                boolean snowy = isSnowy(biome);
                FOX_BIOMES.put(biome, snowy);
                (snowy ? snow : red).add(biome);
            }
        }

        int min = ModConfig.foxes.minGroup;
        int max = Math.max(min, ModConfig.foxes.maxGroup);
        if (ModConfig.foxes.spawnWeight > 0 && !FOX_BIOMES.isEmpty()) {
            Set<Biome> all = new LinkedHashSet<Biome>(red);
            all.addAll(snow);
            EntityRegistry.addSpawn(EntityFox.class, ModConfig.foxes.spawnWeight, min, max, EnumCreatureType.CREATURE,
                    all.toArray(new Biome[0]));
        }
        VillagerBackport.LOGGER.info("Foxes spawn in {} biomes ({} with snow foxes).", FOX_BIOMES.size(), snow.size());
    }

    /** @return true if foxes born in this biome are snow foxes */
    public static boolean isSnowy(Biome biome) {
        return biome.isSnowyBiome() || BiomeDictionary.hasType(biome, BiomeDictionary.Type.SNOWY);
    }

    /**
     * Stops another mod's fox spawning naturally, when this mod's is the one kept.
     *
     * <p>Run at the first world load rather than at init, because Future MC adds its spawns in its own
     * init, which may come after this mod's.
     */
    static void removeDuplicateSpawns() {
        if (!live() || ModConfig.compat.futureMcDuplicates != FutureMc.Duplicates.KEEP_OURS) {
            return;
        }
        EntityEntry theirs = ForgeRegistries.ENTITIES.getValue(new ResourceLocation(FutureMc.MOD_ID, "fox"));
        if (theirs == null || theirs.getEntityClass() == null) {
            return;
        }
        @SuppressWarnings("unchecked")
        Class<? extends net.minecraft.entity.EntityLiving> type =
                (Class<? extends net.minecraft.entity.EntityLiving>) theirs.getEntityClass();
        EntityRegistry.removeSpawn(type, EnumCreatureType.CREATURE,
                ForgeRegistries.BIOMES.getValuesCollection().toArray(new Biome[0]));
        VillagerBackport.LOGGER.info("Future MC's fox no longer spawns naturally; this mod's is kept "
                + "(compat.futureMcDuplicates).");
    }

    @Nullable
    private static Item item(String mod, String path) {
        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(mod, path));
        return item == null || item == net.minecraft.init.Items.AIR ? null : item;
    }
}
