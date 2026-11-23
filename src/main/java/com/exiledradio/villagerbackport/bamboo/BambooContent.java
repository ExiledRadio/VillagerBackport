package com.exiledradio.villagerbackport.bamboo;

import com.exiledradio.backportcore.creative.CreativePlacement;
import com.exiledradio.villagerbackport.ModConfig;
import com.exiledradio.villagerbackport.VillagerBackport;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.storage.loot.LootTableList;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.furnace.FurnaceFuelBurnTimeEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityEntryBuilder;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Bamboo, scaffolding, the bamboo jungle and pandas: everything in this package, registered.
 *
 * <p>All of it is switched by {@code bamboo.enabled}. Turned off, nothing here is registered and
 * every field below stays null. The bamboo jungle has its own switch, {@code bamboo.bambooJungle}.
 *
 * <h2>Sound types</h2>
 * Blocks are registered before sounds, and a {@link SoundType} holds its sounds from the moment it
 * is made, so the three sound types are built when the sounds are and the blocks ask for them each
 * time rather than being handed one at construction.
 *
 * <h2>Future MC</h2>
 * Which mod's bamboo and scaffolding stay visible, and whose pandas spawn, follows
 * {@code compat.futureMcDuplicates}: see {@link BambooFutureMc}.
 */
@Mod.EventBusSubscriber(modid = VillagerBackport.MOD_ID)
public final class BambooContent {

    /** Network id within this mod; see docs/V14_COMPLETION_PLAN.md. */
    private static final int PANDA_ID = 109;

    /** Bamboo and scaffolding both burn for 50 ticks, a quarter of an item, as in 1.14. */
    private static final int BURN_TIME = 50;

    public static final ResourceLocation PANDA_LOOT = new ResourceLocation(VillagerBackport.MOD_ID, "entities/panda");

    @Nullable
    public static BlockBamboo bamboo;
    @Nullable
    public static BlockBambooSapling bambooSapling;
    @Nullable
    public static BlockScaffolding scaffolding;
    @Nullable
    public static Item bambooItem;
    @Nullable
    public static Item scaffoldingItem;
    @Nullable
    public static Biome bambooJungle;
    @Nullable
    public static Biome bambooJungleHills;

    @Nullable
    static SoundType bambooSound;
    @Nullable
    static SoundType bambooSaplingSound;
    @Nullable
    static SoundType scaffoldingSound;

    public static SoundEvent pandaAmbient;
    public static SoundEvent pandaAggressiveAmbient;
    public static SoundEvent pandaWorriedAmbient;
    public static SoundEvent pandaPreSneeze;
    public static SoundEvent pandaSneeze;
    public static SoundEvent pandaBite;
    public static SoundEvent pandaCantBreed;
    public static SoundEvent pandaDeath;
    public static SoundEvent pandaHurt;
    public static SoundEvent pandaEat;
    public static SoundEvent pandaStep;

    private static boolean futureMcSpawnsChecked;

    private BambooContent() {
    }

    /** @return true if this mod adds bamboo, scaffolding and pandas - fixed for the whole session */
    public static boolean enabled() {
        return ModConfig.bamboo.enabled;
    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        if (!enabled()) {
            return;
        }
        bamboo = named(new BlockBamboo(), "bamboo");
        bambooSapling = named(new BlockBambooSapling(), "bamboo_sapling");
        scaffolding = named(new BlockScaffolding(), "scaffolding");
        event.getRegistry().registerAll(bamboo, bambooSapling, scaffolding);
    }

    private static <T extends Block> T named(T block, String name) {
        block.setRegistryName(new ResourceLocation(VillagerBackport.MOD_ID, name));
        block.setTranslationKey(VillagerBackport.MOD_ID + "." + name);
        return block;
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        if (!enabled()) {
            return;
        }
        // The bamboo item is the stalk's own item: placing it plants a shoot (see
        // BlockBamboo.getStateForPlacement), and the shoot and stalk both drop it.
        bambooItem = new ItemBlock(bamboo);
        bambooItem.setRegistryName(bamboo.getRegistryName());
        // Vanilla's tabs, by the vanilla block of the same name (Backport Core's creative placement).
        CreativePlacement.place(bamboo);

        scaffoldingItem = new ItemScaffolding(scaffolding);
        scaffoldingItem.setRegistryName(scaffolding.getRegistryName());
        CreativePlacement.place(scaffolding);

        event.getRegistry().registerAll(bambooItem, scaffoldingItem);

        // 1.14's fire spread: both catch and burn like leaves.
        Blocks.FIRE.setFireInfo(bamboo, 60, 60);
        Blocks.FIRE.setFireInfo(bambooSapling, 60, 60);
        Blocks.FIRE.setFireInfo(scaffolding, 60, 60);
    }

    @SubscribeEvent
    public static void registerSounds(RegistryEvent.Register<SoundEvent> event) {
        if (!enabled()) {
            return;
        }
        SoundEvent bambooBreak = sound(event, "block.bamboo.break");
        SoundEvent bambooFall = sound(event, "block.bamboo.fall");
        SoundEvent bambooHit = sound(event, "block.bamboo.hit");
        SoundEvent bambooPlace = sound(event, "block.bamboo.place");
        SoundEvent bambooStep = sound(event, "block.bamboo.step");
        SoundEvent saplingBreak = sound(event, "block.bamboo_sapling.break");
        SoundEvent saplingHit = sound(event, "block.bamboo_sapling.hit");
        SoundEvent saplingPlace = sound(event, "block.bamboo_sapling.place");
        // 1.14's SoundType arguments: volume, pitch, break, step, place, hit, fall.
        bambooSound = new SoundType(1.0F, 1.0F, bambooBreak, bambooStep, bambooPlace, bambooHit, bambooFall);
        bambooSaplingSound = new SoundType(1.0F, 1.0F, saplingBreak, bambooStep, saplingPlace, saplingHit, bambooFall);
        scaffoldingSound = new SoundType(1.0F, 1.0F, sound(event, "block.scaffolding.break"),
                sound(event, "block.scaffolding.step"), sound(event, "block.scaffolding.place"),
                sound(event, "block.scaffolding.hit"), sound(event, "block.scaffolding.fall"));

        pandaAmbient = sound(event, "entity.panda.ambient");
        pandaAggressiveAmbient = sound(event, "entity.panda.aggressive_ambient");
        pandaWorriedAmbient = sound(event, "entity.panda.worried_ambient");
        pandaPreSneeze = sound(event, "entity.panda.pre_sneeze");
        pandaSneeze = sound(event, "entity.panda.sneeze");
        pandaBite = sound(event, "entity.panda.bite");
        pandaCantBreed = sound(event, "entity.panda.cant_breed");
        pandaDeath = sound(event, "entity.panda.death");
        pandaHurt = sound(event, "entity.panda.hurt");
        pandaEat = sound(event, "entity.panda.eat");
        pandaStep = sound(event, "entity.panda.step");
    }

    private static SoundEvent sound(RegistryEvent.Register<SoundEvent> event, String name) {
        ResourceLocation id = new ResourceLocation(VillagerBackport.MOD_ID, name);
        SoundEvent sound = new SoundEvent(id).setRegistryName(id);
        event.getRegistry().register(sound);
        return sound;
    }

    @SubscribeEvent
    public static void registerBiomes(RegistryEvent.Register<Biome> event) {
        if (!enabled()) {
            return;
        }
        // Worldgen hooks that are not registry events, set up once here: this event comes during
        // pre-initialization, before any world exists.
        MinecraftForge.EVENT_BUS.register(JungleBamboo.class);
        if (!ModConfig.bamboo.bambooJungle) {
            return;
        }

        // 1.14's numbers: a jungle's climate and terrain, and hills raised as jungle hills are.
        bambooJungle = new BiomeBambooJungle(new Biome.BiomeProperties("Bamboo Jungle")
                .setTemperature(0.95F).setRainfall(0.9F).setBaseHeight(0.1F).setHeightVariation(0.2F));
        bambooJungle.setRegistryName(new ResourceLocation(VillagerBackport.MOD_ID, "bamboo_jungle"));
        bambooJungleHills = new BiomeBambooJungle(new Biome.BiomeProperties("Bamboo Jungle Hills")
                .setTemperature(0.95F).setRainfall(0.9F).setBaseHeight(0.45F).setHeightVariation(0.3F));
        bambooJungleHills.setRegistryName(new ResourceLocation(VillagerBackport.MOD_ID, "bamboo_jungle_hills"));
        event.getRegistry().registerAll(bambooJungle, bambooJungleHills);
        rtgTerrain(bambooJungle, "JUNGLE");
        rtgTerrain(bambooJungleHills, "JUNGLE_HILLS");

        BiomeDictionary.addTypes(bambooJungle, BiomeDictionary.Type.JUNGLE, BiomeDictionary.Type.HOT,
                BiomeDictionary.Type.WET, BiomeDictionary.Type.DENSE);
        BiomeDictionary.addTypes(bambooJungleHills, BiomeDictionary.Type.JUNGLE, BiomeDictionary.Type.HOT,
                BiomeDictionary.Type.WET, BiomeDictionary.Type.DENSE,
                BiomeDictionary.Type.HILLS);

        MinecraftForge.TERRAIN_GEN_BUS.register(BambooJungleLayers.class);
    }

    @SubscribeEvent
    public static void registerEntities(RegistryEvent.Register<EntityEntry> event) {
        if (!enabled()) {
            return;
        }
        EntityEntryBuilder<EntityPanda> panda = EntityEntryBuilder.<EntityPanda>create().entity(EntityPanda.class)
                .id(new ResourceLocation(VillagerBackport.MOD_ID, "panda"), PANDA_ID)
                .name(VillagerBackport.MOD_ID + ".panda").egg(15198183, 1776418).tracker(80, 3, true);

        // 1.14: rare in jungles and jungle hills, everywhere in bamboo jungles, in pairs or alone.
        if (ModConfig.bamboo.pandaJungleWeight > 0 && !BambooFutureMc.leaveJunglesToFutureMc()) {
            panda.spawn(EnumCreatureType.CREATURE, ModConfig.bamboo.pandaJungleWeight, 1, 2,
                    Biomes.JUNGLE, Biomes.JUNGLE_HILLS);
        }
        List<Biome> bambooJungles = new ArrayList<Biome>();
        if (bambooJungle != null) {
            bambooJungles.add(bambooJungle);
            bambooJungles.add(bambooJungleHills);
        }
        if (ModConfig.bamboo.pandaBambooJungleWeight > 0 && !bambooJungles.isEmpty()) {
            panda.spawn(EnumCreatureType.CREATURE, ModConfig.bamboo.pandaBambooJungleWeight, 1, 2, bambooJungles);
        }
        event.getRegistry().register(panda.build());
        LootTableList.register(PANDA_LOOT);
    }

    /** Lowest priority: every item and every JSON recipe is in by now. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void registerRecipes(RegistryEvent.Register<IRecipe> event) {
        if (enabled()) {
            BambooFutureMc.hide(event.getRegistry());
        }
    }

    @SubscribeEvent
    public static void onFuel(FurnaceFuelBurnTimeEvent event) {
        ItemStack stack = event.getItemStack();
        if (!stack.isEmpty() && stack.getItem() != null
                && (stack.getItem() == bambooItem || stack.getItem() == scaffoldingItem)) {
            event.setBurnTime(BURN_TIME);
        }
    }

    /** Future MC's pandas are dealt with once, when the first world loads and every mod is ready. */
    @SubscribeEvent
    public static void onWorldLoad(WorldEvent.Load event) {
        if (!futureMcSpawnsChecked && enabled() && !event.getWorld().isRemote) {
            futureMcSpawnsChecked = true;
            BambooFutureMc.removeFutureMcPandaSpawns();
        }
    }

    /** @return true for this mod's bamboo, or Future MC's, which pandas also eat */
    public static boolean isBamboo(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        Item item = stack.getItem();
        return item == bambooItem || (item != null && item == BambooFutureMc.futureMcBamboo());
    }

    /**
     * Realistic Terrain Generation builds only biomes registered with it, and makes any other one plains. Backport
     * Core's {@code RtgBiomes} (1.4.5) registers this biome on the terrain of the vanilla biome named here; reached by
     * name, so with an older Core the biome simply stays RTG's patch biome in RTG worlds.
     */
    private static void rtgTerrain(Biome biome, String template) {
        try {
            Class.forName("com.exiledradio.backportcore.terrain.RtgBiomes").getMethod("register", Biome.class, String.class)
                    .invoke(null, biome, template);
        } catch (ReflectiveOperationException | LinkageError e) {
            // A Backport Core without RtgBiomes.
        }
    }
}
