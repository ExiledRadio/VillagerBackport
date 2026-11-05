package com.exiledradio.villagerbackport.trader;

import com.exiledradio.villagerbackport.ModConfig;
import com.exiledradio.villagerbackport.VillagerBackport;

import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.ai.EntityAITasks;
import net.minecraft.entity.monster.EntityEvoker;
import net.minecraft.entity.monster.EntityIllusionIllager;
import net.minecraft.entity.monster.EntityPigZombie;
import net.minecraft.entity.monster.EntityVindicator;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityEntryBuilder;

/**
 * The wandering trader and its trader llamas: registration, sounds, and the mobs that hunt it.
 *
 * <h2>Switched off</h2>
 * With {@code trader.enabled} off nothing here is registered - no entities, no sounds, no spawn eggs -
 * and {@link TraderSpawner} never runs. A world that had traders then loses them on load, with
 * Forge's usual "unknown entity" warning, which is what turning a mob off has to mean. Nothing else in
 * the mod refers to these classes, so the rest of it is unaffected either way.
 *
 * <h2>Future MC</h2>
 * Future MC (0.2.21, the last 1.12.2 release) has no wandering trader, so unlike the crossbow there is
 * nothing to step aside for.
 *
 * <h2>Who hunts it</h2>
 * In 1.14 the trader is an {@code AbstractVillager}, and every mob that goes after villagers names
 * that class rather than {@code Villager}: zombies (and husks, zombie villagers and drowned with
 * them), pillagers, vindicators, evokers, illusioners and ravagers. 1.12.2 has no shared villager
 * class to aim at - its mobs name {@code EntityVillager} - so each of them is given a second,
 * identical target task for the trader as it joins the world, at the priority and with the
 * line-of-sight rule its villager task already has. Zombie pigmen are a zombie subclass with their
 * own targets, and are left alone as 1.14 leaves them.
 */
@Mod.EventBusSubscriber(modid = VillagerBackport.MOD_ID)
public final class TraderContent {

    /** Network ids within this mod; the crossbow's projectiles end at 104. */
    private static final int TRADER_ID = 105;
    private static final int LLAMA_ID = 106;

    /** 1.14's spawn egg colors. */
    private static final int TRADER_EGG_BASE = 0x456296;
    private static final int TRADER_EGG_SPOTS = 0xEAA430;

    public static SoundEvent ambient;
    public static SoundEvent death;
    public static SoundEvent hurt;
    public static SoundEvent no;
    public static SoundEvent yes;
    public static SoundEvent trade;
    public static SoundEvent drinkMilk;
    public static SoundEvent drinkPotion;
    public static SoundEvent disappeared;
    public static SoundEvent reappeared;

    private TraderContent() {
    }

    /** @return true if this mod adds the wandering trader in this game - fixed for the whole session */
    public static boolean enabled() {
        return ModConfig.trader.enabled;
    }

    @SubscribeEvent
    public static void registerEntities(RegistryEvent.Register<EntityEntry> event) {
        if (!enabled()) {
            VillagerBackport.LOGGER.info("The wandering trader is off (trader.enabled); not registering it.");
            return;
        }
        event.getRegistry().registerAll(
                EntityEntryBuilder.create().entity(EntityWanderingTrader.class)
                        .id(new ResourceLocation(VillagerBackport.MOD_ID, "wandering_trader"), TRADER_ID)
                        .name(VillagerBackport.MOD_ID + ".wandering_trader")
                        .egg(TRADER_EGG_BASE, TRADER_EGG_SPOTS).tracker(80, 3, true).build(),
                // The trader llama's egg is the trader's with the colors swapped, as in 1.14.
                EntityEntryBuilder.create().entity(EntityTraderLlama.class)
                        .id(new ResourceLocation(VillagerBackport.MOD_ID, "trader_llama"), LLAMA_ID)
                        .name(VillagerBackport.MOD_ID + ".trader_llama")
                        .egg(TRADER_EGG_SPOTS, TRADER_EGG_BASE).tracker(80, 3, true).build());
    }

    @SubscribeEvent
    public static void registerSounds(RegistryEvent.Register<SoundEvent> event) {
        if (!enabled()) {
            return;
        }
        ambient = sound(event, "ambient");
        death = sound(event, "death");
        hurt = sound(event, "hurt");
        no = sound(event, "no");
        yes = sound(event, "yes");
        trade = sound(event, "trade");
        drinkMilk = sound(event, "drink_milk");
        drinkPotion = sound(event, "drink_potion");
        disappeared = sound(event, "disappeared");
        reappeared = sound(event, "reappeared");
    }

    private static SoundEvent sound(RegistryEvent.Register<SoundEvent> event, String name) {
        // The registry name and the sounds.json key have to match; that is what ties the event to the
        // files listed there.
        ResourceLocation id = new ResourceLocation(VillagerBackport.MOD_ID, "entity.wandering_trader." + name);
        SoundEvent sound = new SoundEvent(id).setRegistryName(id);
        event.getRegistry().register(sound);
        return sound;
    }

    /**
     * Gives villager hunters a target task for the trader too.
     *
     * <p>Runs as the hunter joins, before it has ticked, so its task list is not being iterated - the
     * one moment it is safe to add to. A mob that joins again (a chunk reloading it) is a new object
     * with a fresh task list; the duplicate check covers anything that re-fires the event on the
     * same one.
     */
    @SubscribeEvent
    public static void onJoin(EntityJoinWorldEvent event) {
        if (!enabled() || event.getWorld().isRemote || !(event.getEntity() instanceof EntityCreature)) {
            return;
        }
        EntityCreature mob = (EntityCreature) event.getEntity();

        int priority;
        boolean checkSight;
        if (mob instanceof EntityZombie && !(mob instanceof EntityPigZombie)) {
            priority = 3;
            checkSight = false;
        } else if (mob instanceof com.exiledradio.villagerbackport.illager.EntityPillager) {
            priority = 3;
            checkSight = false;
        } else if (mob instanceof com.exiledradio.villagerbackport.illager.EntityRavager) {
            priority = 4;
            checkSight = true;
        } else if (mob instanceof EntityVindicator) {
            priority = 3;
            checkSight = true;
        } else if (mob instanceof EntityEvoker || mob instanceof EntityIllusionIllager) {
            priority = 3;
            checkSight = false;
        } else {
            return;
        }

        for (EntityAITasks.EntityAITaskEntry entry : mob.targetTasks.taskEntries) {
            if (entry.action instanceof HuntTrader) {
                return;
            }
        }
        mob.targetTasks.addTask(priority, new HuntTrader(mob, checkSight));
    }

    /** A villager hunter's target task, aimed at the trader. Its own class so it can be recognized. */
    public static final class HuntTrader extends EntityAINearestAttackableTarget<EntityWanderingTrader> {
        HuntTrader(EntityCreature mob, boolean checkSight) {
            super(mob, EntityWanderingTrader.class, checkSight);
        }
    }
}
