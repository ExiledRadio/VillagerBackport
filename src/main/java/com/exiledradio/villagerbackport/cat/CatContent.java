package com.exiledradio.villagerbackport.cat;

import com.exiledradio.villagerbackport.ModConfig;
import com.exiledradio.villagerbackport.VillagerBackport;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityEntryBuilder;

/**
 * 1.14's cats: registration, and the switch that decides whether any of it happens.
 *
 * <h2>The split</h2>
 * In 1.12.2 a cat is a tamed ocelot. 1.14 made them two animals: the {@link EntityCat cat}, with
 * eleven coats, which lives in villages and witch huts and is tamed with raw fish; and the ocelot,
 * which stays in the jungle and can no longer be tamed at all, only won over (its "trust", see
 * {@link OcelotTrust}). Old worlds' tamed ocelots become cats when they load, as 1.14's own world
 * upgrade made them.
 *
 * <h2>What lives where</h2>
 * <ul>
 *   <li>{@link EntityCat} and its goals - the animal itself.</li>
 *   <li>{@link OcelotTrust} - the 1.14 ocelot, done to 1.12.2's own ocelot with events, and the
 *       conversion of tamed ones.</li>
 *   <li>{@link CatSpawner} - stray cats in villages, black cats at witch huts.</li>
 *   <li>{@link CatFear} - creepers and phantoms keeping away from cats; cats on chests.</li>
 *   <li>{@link CatAdvancements} - <i>A Complete Catalogue</i>.</li>
 * </ul>
 *
 * <p>Everything is skipped with {@code cats.enabled} off: no entity is registered, and the event
 * handlers are never put on the bus. The entity id never changes once a world has used it.
 */
@Mod.EventBusSubscriber(modid = VillagerBackport.MOD_ID)
public final class CatContent {

    /** Network id within this mod; see docs/V14_COMPLETION_PLAN.md for who owns which. */
    private static final int CAT_ID = 107;

    /** 1.14's cat spawn egg colors. */
    private static final int EGG_PRIMARY = 15714446;
    private static final int EGG_SECONDARY = 9794134;

    /** The sounds 1.12.2 lacks. The cat's meows, purrs and hisses are vanilla's own. */
    public static SoundEvent begForFood;
    public static SoundEvent eat;
    public static SoundEvent strayAmbient;
    public static SoundEvent ocelotAmbient;
    public static SoundEvent ocelotHurt;
    public static SoundEvent ocelotDeath;

    private CatContent() {
    }

    /** @return true if this mod adds cats in this game - fixed for the session (cats.enabled) */
    public static boolean enabled() {
        return ModConfig.cats.enabled;
    }

    /**
     * @return true if this is food to a cat or an ocelot: raw cod or raw salmon. 1.12.2 keeps both as
     *         metadata of one fish item, alongside clownfish and pufferfish, which are not.
     */
    public static boolean isFish(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (stack.getItem() == Items.FISH) {
            int meta = stack.getMetadata();
            return meta == 0 || meta == 1;
        }
        // Mods that add 1.13's separate fish items (raw only - a cooked one is not cat food).
        ResourceLocation id = stack.getItem().getRegistryName();
        return id != null && ("cod".equals(id.getPath()) || "salmon".equals(id.getPath()));
    }

    @SubscribeEvent
    public static void registerEntities(RegistryEvent.Register<EntityEntry> event) {
        if (!enabled()) {
            VillagerBackport.LOGGER.info("Cats are off (cats.enabled); ocelots stay as 1.12.2 has them.");
            return;
        }
        event.getRegistry().register(EntityEntryBuilder.create().entity(EntityCat.class)
                .id(new ResourceLocation(VillagerBackport.MOD_ID, "cat"), CAT_ID)
                .name(VillagerBackport.MOD_ID + ".cat").egg(EGG_PRIMARY, EGG_SECONDARY)
                .tracker(80, 3, true).build());

        // Registered here rather than from a static block: the entity registry event is the first
        // point where the config is known to be loaded and the bus is ready.
        MinecraftForge.EVENT_BUS.register(new OcelotTrust());
        MinecraftForge.EVENT_BUS.register(new CatSpawner());
        MinecraftForge.EVENT_BUS.register(new CatFear());
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void registerSounds(RegistryEvent.Register<SoundEvent> event) {
        if (!enabled()) {
            return;
        }
        begForFood = sound(event, "entity.cat.beg_for_food");
        eat = sound(event, "entity.cat.eat");
        strayAmbient = sound(event, "entity.cat.stray_ambient");
        ocelotAmbient = sound(event, "entity.ocelot.ambient");
        ocelotHurt = sound(event, "entity.ocelot.hurt");
        ocelotDeath = sound(event, "entity.ocelot.death");
    }

    private static SoundEvent sound(RegistryEvent.Register<SoundEvent> event, String name) {
        ResourceLocation id = new ResourceLocation(VillagerBackport.MOD_ID, name);
        SoundEvent sound = new SoundEvent(id).setRegistryName(id);
        event.getRegistry().register(sound);
        return sound;
    }
}
