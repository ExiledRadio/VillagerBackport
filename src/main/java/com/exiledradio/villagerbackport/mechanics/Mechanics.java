package com.exiledradio.villagerbackport.mechanics;

import com.exiledradio.backportcore.mechanics.BlockRules;
import com.exiledradio.backportcore.pose.PoseRules;
import com.exiledradio.backportcore.pose.Poses;
import com.exiledradio.villagerbackport.ModConfig;
import com.exiledradio.villagerbackport.VillagerBackport;
import com.exiledradio.villagerbackport.raid.RaidView;
import com.exiledradio.villagerbackport.raid.Raids;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityMinecartTNT;
import net.minecraft.entity.item.EntityTNTPrimed;
import net.minecraft.entity.monster.AbstractIllager;
import net.minecraft.entity.monster.AbstractSkeleton;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.entity.monster.EntityGuardian;
import net.minecraft.entity.monster.EntitySkeleton;
import net.minecraft.entity.monster.EntityVindicator;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.passive.EntityWolf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.pathfinding.PathNavigate;
import net.minecraft.pathfinding.PathNavigateGround;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.event.furnace.FurnaceFuelBurnTimeEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.ExplosionEvent;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * 1.14's changes to things 1.12.2 already has (and 1.18's illagers), each behind its setting in
 * {@link ModConfig.Mechanics}. Everything here is an event or a task added to a mob; the three that need a patch (the
 * crouch's eyes, map icons and torches on panes) are Backport Core's, switched on from {@link #preInit}.
 */
public final class Mechanics {

    /** 1.14's {@code leaves} loot table: the stick entry's {@code table_bonus} chances by Fortune level. */
    static final float[] STICK_CHANCES = {0.02F, 0.022222222F, 0.025F, 0.033333335F, 0.1F};

    /** TNT explosions running right now (between Detonate and the end of the tick), by the positions they break. */
    private static final Map<Explosion, Set<BlockPos>> TNT = new IdentityHashMap<Explosion, Set<BlockPos>>();

    private static Method canDespawn;

    private Mechanics() {
    }

    /** Pre-init, both sides. */
    public static void preInit() {
        MinecraftForge.EVENT_BUS.register(new Mechanics());
        MechanicsSounds.init();
        CrouchSync.init();
        CrouchSync.register();
        ModernSiege.init();
        // Backport Core's pose engine: 1.14's shorter crouch.
        Poses.register(new PoseRules() {
            @Override
            public boolean enabled() {
                return crouch();
            }

            @Override
            public boolean shortCrouch() {
                return crouch();
            }
        });
        // Backport Core's patch P35.
        BlockRules.torchesOnPanes(() -> ModConfig.mechanics.torchesOnPanes);
        Cartography.preInit();
    }

    /** Init, both sides, once sounds are registered. */
    public static void init() {
        if (ModConfig.mechanics.parrotsImitateRaiders) {
            MechanicsSounds.registerImitations();
        }
    }

    /** @return the short crouch's switch: the server's, which a client is told when it joins (see CrouchSync) */
    public static boolean crouch() {
        return CrouchSync.active();
    }

    // ------------------------------------------------------------------------------------------------
    // Illagers, zombies, guardians: tasks and targets
    // ------------------------------------------------------------------------------------------------

    @SubscribeEvent
    public void onJoin(EntityJoinWorldEvent event) {
        Entity entity = event.getEntity();
        if (event.getWorld().isRemote || !(entity instanceof EntityLiving)) {
            return;
        }
        EntityLiving mob = (EntityLiving) entity;
        if (entity instanceof AbstractIllager) {
            // Vindicators, evokers and illusioners (and any other mod's AbstractIllager): 1.18's babies, 1.14's
            // invisibility.
            Targeting.restrict(mob, EntityVillager.class, (looker, target) -> Targeting.sparesBaby(target) && Targeting.noticed(looker, target));
        } else if (entity instanceof EntityZombie) {
            Targeting.restrict(mob, EntityVillager.class, Targeting::noticed);
        } else if (entity instanceof EntityGuardian) {
            Targeting.restrict(mob, EntityLivingBase.class, Targeting::noticed);
        }
        if (entity instanceof EntityVindicator && !hasTask(mob, EntityAIRaidBreakDoor.class)) {
            mob.tasks.addTask(2, new EntityAIRaidBreakDoor(mob));
        }
    }

    private static boolean hasTask(EntityLiving mob, Class<?> type) {
        for (net.minecraft.entity.ai.EntityAITasks.EntityAITaskEntry e : mob.tasks.taskEntries) {
            if (type.isInstance(e.action)) {
                return true;
            }
        }
        return false;
    }

    /** @return true if the mob is in a raid that is still going: 1.14's {@code hasActiveRaid} */
    static boolean inActiveRaid(EntityLiving mob) {
        RaidView view = Raids.view();
        if (!view.inRaid(mob)) {
            return false;
        }
        RaidView.Raid raid = view.raidAt(mob.world, new BlockPos(mob));
        return raid == null || raid.isActive();
    }

    /**
     * 1.14's {@code VindicatorEntity.updateAITasks}: while it stands where a raid is, its path may run through wooden
     * doors (which the door goal then breaks). Here only on the difficulties the door can break on.
     */
    @SubscribeEvent
    public void onLivingUpdate(LivingEvent.LivingUpdateEvent event) {
        EntityLivingBase entity = event.getEntityLiving();
        if (!(entity instanceof EntityVindicator) || entity.world.isRemote || entity.ticksExisted % 10 != 0) {
            return;
        }
        EntityVindicator vindicator = (EntityVindicator) entity;
        PathNavigate nav = vindicator.getNavigator();
        if (!(nav instanceof PathNavigateGround)) {
            return;
        }
        boolean doors = ModConfig.mechanics.vindicatorsBreakDoors && EntityAIRaidBreakDoor.validDifficulty(vindicator)
                && inActiveRaid(vindicator);
        PathNavigateGround ground = (PathNavigateGround) nav;
        if (ground.getEnterDoors() != doors) {
            ground.setEnterDoors(doors);
            ground.setBreakDoors(doors);
        }
    }

    // ------------------------------------------------------------------------------------------------
    // Explosions: TNT drops everything
    // ------------------------------------------------------------------------------------------------

    /**
     * 1.14 (19w11a): blocks broken by TNT and TNT minecarts all drop ({@code tnt_explosion_drop_decay} false). Lowest
     * priority, so the list is what other mods left. The chance itself is set as each block drops, below.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onDetonate(ExplosionEvent.Detonate event) {
        if (event.getWorld().isRemote || !ModConfig.mechanics.tntDropsAll) {
            return;
        }
        Entity exploder = exploder(event.getExplosion());
        if (exploder instanceof EntityTNTPrimed || exploder instanceof EntityMinecartTNT) {
            Set<BlockPos> positions = Collections.newSetFromMap(new java.util.HashMap<BlockPos, Boolean>());
            positions.addAll(event.getAffectedBlocks());
            TNT.put(event.getExplosion(), positions);
        }
    }

    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !TNT.isEmpty()) {
            TNT.clear();
        }
    }

    static Entity exploder(Explosion explosion) {
        return ObfuscationReflectionHelper.getPrivateValue(Explosion.class, explosion, "field_77283_e");
    }

    static float size(Explosion explosion) {
        return ObfuscationReflectionHelper.<Float, Explosion>getPrivateValue(Explosion.class, explosion, "field_77280_f");
    }

    /** Lowest priority: after any mod that changes the drops, before they are rolled against the chance. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onHarvestTnt(BlockEvent.HarvestDropsEvent event) {
        if (TNT.isEmpty() || event.getHarvester() != null) {
            return;
        }
        for (Map.Entry<Explosion, Set<BlockPos>> e : TNT.entrySet()) {
            // Explosion.doExplosionB rolls each block's drops against 1 / size.
            if (e.getValue().contains(event.getPos()) && event.getDropChance() == 1.0F / size(e.getKey())) {
                event.setDropChance(1.0F);
                return;
            }
        }
    }

    // ------------------------------------------------------------------------------------------------
    // Leaves drop sticks
    // ------------------------------------------------------------------------------------------------

    /**
     * 1.14 (18w43a): oak, spruce, birch, jungle, acacia and dark oak leaves drop 1 or 2 sticks, 2% of the time, more with
     * Fortune ({@link #STICK_CHANCES}), on decay too, never when sheared (1.12.2's shears take the leaves before any
     * drops) or broken with Silk Touch. Other mods' leaves keep their own drops.
     */
    @SubscribeEvent
    public void onHarvestLeaves(BlockEvent.HarvestDropsEvent event) {
        if (!ModConfig.mechanics.leavesDropSticks || event.isSilkTouching() || event.getWorld().isRemote) {
            return;
        }
        Block block = event.getState().getBlock();
        if (block != Blocks.LEAVES && block != Blocks.LEAVES2) {
            return;
        }
        if (event.getHarvester() != null && event.getHarvester().getHeldItemMainhand().getItem() instanceof net.minecraft.item.ItemShears) {
            return;
        }
        Random rand = event.getWorld().rand;
        float chance = STICK_CHANCES[Math.max(0, Math.min(event.getFortuneLevel(), STICK_CHANCES.length - 1))];
        if (rand.nextFloat() < chance) {
            event.getDrops().add(new ItemStack(Items.STICK, 1 + rand.nextInt(2)));
        }
    }

    // ------------------------------------------------------------------------------------------------
    // Deaths: discs, wolves
    // ------------------------------------------------------------------------------------------------

    /**
     * 1.14: a creeper killed by any skeleton drops a disc ({@code AbstractSkeletonEntity}). 1.12.2's creeper checks for
     * {@code EntitySkeleton} only, so strays and wither skeletons gave nothing; those get it here.
     */
    @SubscribeEvent
    public void onDrops(LivingDropsEvent event) {
        EntityLivingBase dead = event.getEntityLiving();
        if (!ModConfig.mechanics.anySkeletonDisc || !(dead instanceof EntityCreeper) || dead.world.isRemote
                || !dead.world.getGameRules().getBoolean("doMobLoot")) {
            return;
        }
        Entity killer = event.getSource().getTrueSource();
        if (killer instanceof AbstractSkeleton && !(killer instanceof EntitySkeleton)) {
            int first = Item.getIdFromItem(Items.RECORD_13);
            int last = Item.getIdFromItem(Items.RECORD_WAIT);
            Item disc = Item.getItemById(first + dead.world.rand.nextInt(last - first + 1));
            event.getDrops().add(new EntityItem(dead.world, dead.posX, dead.posY, dead.posZ, new ItemStack(disc)));
        }
    }

    /**
     * 1.14 (19w11a): a hit from a tamed wolf counts as its owner's: {@code LivingEntity.hurt} sets
     * {@code lastHurtByPlayer} to the owner, where 1.12.2 sets {@code attackingPlayer} to null. So a mob a wolf kills
     * drops what only player kills drop.
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onDeath(LivingDeathEvent event) {
        EntityLivingBase dead = event.getEntityLiving();
        if (!ModConfig.mechanics.wolfKillsCount || dead.world.isRemote || dead instanceof EntityPlayer) {
            return;
        }
        Entity killer = event.getSource().getTrueSource();
        if (killer instanceof EntityWolf && ((EntityWolf) killer).isTamed()) {
            EntityLivingBase owner = ((EntityWolf) killer).getOwner();
            if (owner instanceof EntityPlayer && attackingPlayer(dead) == null) {
                setAttackingPlayer(dead, (EntityPlayer) owner);
            }
        }
    }

    static EntityPlayer attackingPlayer(EntityLivingBase entity) {
        return ObfuscationReflectionHelper.getPrivateValue(EntityLivingBase.class, entity, "field_70717_bb");
    }

    static void setAttackingPlayer(EntityLivingBase entity, EntityPlayer player) {
        ObfuscationReflectionHelper.setPrivateValue(EntityLivingBase.class, entity, player, "field_70717_bb");
        ObfuscationReflectionHelper.setPrivateValue(EntityLivingBase.class, entity, 100, "field_70718_bc");
    }

    // ------------------------------------------------------------------------------------------------
    // Fuel, chorus flowers, spawning
    // ------------------------------------------------------------------------------------------------

    /** 1.14 (18w43a): a dead bush burns for 100 ticks, half an item. */
    @SubscribeEvent
    public void onFuel(FurnaceFuelBurnTimeEvent event) {
        if (ModConfig.mechanics.deadBushFuel && event.getBurnTime() < 0
                && event.getItemStack().getItem() == Item.getItemFromBlock(Blocks.DEADBUSH)) {
            event.setBurnTime(100);
        }
    }

    /** 1.14 (18w43a): an arrow breaks a chorus flower it hits, and the flower drops ({@code ChorusFlowerBlock.onProjectileHit}). */
    @SubscribeEvent
    public void onArrow(ProjectileImpactEvent.Arrow event) {
        RayTraceResult hit = event.getRayTraceResult();
        World world = event.getArrow().world;
        if (!ModConfig.mechanics.arrowsBreakChorusFlowers || world.isRemote || hit == null
                || hit.typeOfHit != RayTraceResult.Type.BLOCK) {
            return;
        }
        BlockPos pos = hit.getBlockPos();
        if (world.getBlockState(pos).getBlock() == Blocks.CHORUS_FLOWER
                && net.minecraftforge.event.ForgeEventFactory.getMobGriefingEvent(world, event.getArrow())) {
            world.destroyBlock(pos, false);
            Block.spawnAsEntity(world, pos, new ItemStack(Blocks.CHORUS_FLOWER));
        }
    }

    /**
     * 1.14: {@code NaturalSpawner} refuses a mob that would despawn the moment it spawned, more than 128 blocks from
     * every player ({@code removeWhenFarAway}). 1.12.2 spawns it and {@code despawnEntity} removes it the next tick.
     */
    @SubscribeEvent
    public void onCheckSpawn(LivingSpawnEvent.CheckSpawn event) {
        if (!ModConfig.mechanics.refuseInstantDespawns || event.isSpawner() || event.getWorld().isRemote
                || !(event.getEntityLiving() instanceof EntityLiving) || event.getResult() != Event.Result.DEFAULT) {
            return;
        }
        EntityLiving mob = (EntityLiving) event.getEntityLiving();
        if (mob.isNoDespawnRequired() || !canDespawn(mob)) {
            return;
        }
        EntityPlayer nearest = event.getWorld().getClosestPlayer(event.getX(), event.getY(), event.getZ(), -1.0D, false);
        if (nearest != null && nearest.getDistanceSq(event.getX(), event.getY(), event.getZ()) > 16384.0D) {
            event.setResult(Event.Result.DENY);
        }
    }

    static boolean canDespawn(EntityLiving mob) {
        try {
            if (canDespawn == null) {
                canDespawn = ObfuscationReflectionHelper.findMethod(EntityLiving.class, "func_70692_ba", boolean.class);
            }
            return (Boolean) canDespawn.invoke(mob);
        } catch (ReflectiveOperationException | RuntimeException e) {
            VillagerBackport.LOGGER.warn("Cannot ask {} whether it despawns", mob, e);
            return false;
        }
    }
}
