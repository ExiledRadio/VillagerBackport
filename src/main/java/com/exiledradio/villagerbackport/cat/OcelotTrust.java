package com.exiledradio.villagerbackport.cat;

import com.exiledradio.villagerbackport.ModConfig;
import com.exiledradio.villagerbackport.VillagerBackport;

import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.EntityAIAvoidEntity;
import net.minecraft.entity.ai.EntityAITasks;
import net.minecraft.entity.passive.EntityOcelot;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumActionResult;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.PlaySoundAtEntityEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 1.14's ocelot, made out of 1.12.2's with events, and the move of tamed ocelots to cats.
 *
 * <h2>Trust instead of taming</h2>
 * A 1.14 ocelot cannot be tamed. Fed raw cod or salmon from within three blocks, it comes to trust the
 * player one time in three (hearts; smoke otherwise). A trusting ocelot stops running from players, no
 * longer despawns, and from then on fish makes it breed, as fish makes any animal breed. Trust is the
 * ocelot's, not one player's, and is kept in its entity data ({@value #TRUSTING}) - 1.14's
 * {@code Trusting} tag, under this mod's name so nothing else mistakes it for its own.
 *
 * <p>Untrusting ocelots run from every player within 16 blocks. 1.12.2 only gives its ocelots that
 * goal once {@code setTamed} has been called on them, which happens when one loads from disk but not
 * when one first spawns; so the goal is put on every ocelot as it joins the world, and asks whether
 * the ocelot trusts anyone rather than being removed when it starts to - the goal list is never
 * edited after that.
 *
 * <p>1.14's ocelots also make their own noises. 1.12.2's wild ocelot is silent and borrows the cat's
 * hurt and death sounds, so those are swapped as they are played, and an ambient call is added about
 * once every 45 seconds, 1.14's interval.
 *
 * <h2>Old tamed ocelots</h2>
 * A tamed ocelot is a cat that predates the split. When one joins the world it is swapped for a
 * {@link EntityCat} with its owner, name, age, health, sitting and coat - 1.12.2's tuxedo, red and
 * Siamese skins are 1.14's coats 1 to 3, the same numbers, which is how 1.14's own world upgrade did it.
 *
 * <p>The swap waits for the end of the server tick. The join event can fire while a chunk is loading,
 * and cancelling it there - the obvious way to replace an entity - leaves the old one registered with
 * the chunk but not the world: invisible, solid, and saved with the chunk for ever. Letting it join
 * and then killing it the ordinary way avoids that, and doing both halves at the same moment means a
 * server stopping in between loses nothing.
 *
 * <p>Only {@link EntityOcelot} itself is touched, never another mod's subclass of it.
 */
public final class OcelotTrust {

    static final String TRUSTING = "villagerbackport:Trusting";

    /** Tamed ocelots waiting for the end of the tick to become cats. */
    private static final List<EntityOcelot> PENDING = new ArrayList<EntityOcelot>();

    /** 1.14's {@code getAmbientSoundInterval} for ocelots. */
    private static final int AMBIENT_INTERVAL = 900;

    public static boolean isTrusting(EntityOcelot ocelot) {
        return ocelot.getEntityData().getBoolean(TRUSTING);
    }

    static void setTrusting(EntityOcelot ocelot, boolean trusting) {
        ocelot.getEntityData().setBoolean(TRUSTING, trusting);
    }

    private static boolean isPlainOcelot(Entity entity) {
        return entity != null && entity.getClass() == EntityOcelot.class;
    }

    // ---------------------------------------------------------------------------------------------
    // Joining: conversion, and the flee goal
    // ---------------------------------------------------------------------------------------------

    @SubscribeEvent
    public void onJoin(EntityJoinWorldEvent event) {
        if (event.getWorld().isRemote || !isPlainOcelot(event.getEntity())) {
            return;
        }
        EntityOcelot ocelot = (EntityOcelot) event.getEntity();

        if (ocelot.isTamed()) {
            if (ModConfig.cats.convertTamedOcelots) {
                synchronized (PENDING) {
                    PENDING.add(ocelot);
                }
            }
            return;
        }

        if (ModConfig.cats.modernOcelots) {
            installFleeGoal(ocelot);
        }
    }

    /** Swaps 1.12.2's flee goal, if it has one, for one that asks about trust. */
    private static void installFleeGoal(EntityOcelot ocelot) {
        Iterator<EntityAITasks.EntityAITaskEntry> entries = ocelot.tasks.taskEntries.iterator();
        while (entries.hasNext()) {
            EntityAITasks.EntityAITaskEntry entry = entries.next();
            if (entry.action instanceof AvoidPlayersUntilTrusted) {
                return;
            }
            // 1.12.2's own: the ocelot's only avoid goal is the one that flees players.
            if (entry.action.getClass() == EntityAIAvoidEntity.class) {
                if (entry.using) {
                    entry.action.resetTask();
                }
                entries.remove();
            }
        }
        ocelot.tasks.addTask(4, new AvoidPlayersUntilTrusted(ocelot));
    }

    /** 1.14's {@code OcelotAvoidEntityGoal}: players, 16 blocks, only while it trusts no one. */
    static final class AvoidPlayersUntilTrusted extends EntityAIAvoidEntity<EntityPlayer> {
        private final EntityOcelot ocelot;

        AvoidPlayersUntilTrusted(EntityOcelot ocelot) {
            super(ocelot, EntityPlayer.class, 16.0F, 0.8D, 1.33D);
            this.ocelot = ocelot;
        }

        @Override
        public boolean shouldExecute() {
            return !isTrusting(this.ocelot) && super.shouldExecute();
        }

        @Override
        public boolean shouldContinueExecuting() {
            return !isTrusting(this.ocelot) && super.shouldContinueExecuting();
        }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        List<EntityOcelot> batch;
        synchronized (PENDING) {
            if (PENDING.isEmpty()) {
                return;
            }
            batch = new ArrayList<EntityOcelot>(PENDING);
            PENDING.clear();
        }
        for (EntityOcelot ocelot : batch) {
            if (!ocelot.isDead && ocelot.isTamed()) {
                convert(ocelot);
            }
        }
    }

    /** @return the cat that replaced it */
    static EntityCat convert(EntityOcelot ocelot) {
        World world = ocelot.world;
        EntityCat cat = new EntityCat(world);
        cat.copyLocationAndAnglesFrom(ocelot);
        cat.renderYawOffset = ocelot.renderYawOffset;
        cat.rotationYawHead = ocelot.rotationYawHead;

        int skin = ocelot.getTameSkin();
        // 1.12.2: 0 wild, 1 tuxedo, 2 red, 3 Siamese; 1.14 keeps 1-3. A tamed one with the wild coat
        // (only possible through commands or other mods) becomes a tabby, the nearest thing.
        cat.setCatType(skin >= 1 && skin <= 3 ? skin : 0);
        cat.setCollarColor(EnumDyeColor.RED);

        if (ocelot.getOwnerId() != null) {
            cat.setOwnerQuietly(ocelot.getOwnerId());
        }
        cat.orderSit(ocelot.isSitting());
        cat.setSitting(ocelot.isSitting());
        cat.setGrowingAge(ocelot.getGrowingAge());
        if (ocelot.hasCustomName()) {
            cat.setCustomNameTag(ocelot.getCustomNameTag());
            cat.setAlwaysRenderNameTag(ocelot.getAlwaysRenderNameTag());
        }
        if (ocelot.isNoDespawnRequired()) {
            cat.enablePersistence();
        }
        cat.setEntityInvulnerable(ocelot.getIsInvulnerable());
        cat.setHealth(Math.min(cat.getMaxHealth(), ocelot.getHealth()));

        // Other mods' data (and Forge capabilities' entity data) travels with it.
        NBTTagCompound forgeData = ocelot.getEntityData();
        if (!forgeData.isEmpty()) {
            cat.getEntityData().merge(forgeData);
        }

        ocelot.setDead();
        world.spawnEntity(cat);
        VillagerBackport.LOGGER.info("A tamed ocelot at {} {} {} is now a {} cat.",
                (int) cat.posX, (int) cat.posY, (int) cat.posZ, cat.getTypeName());
        return cat;
    }

    // ---------------------------------------------------------------------------------------------
    // Feeding
    // ---------------------------------------------------------------------------------------------

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (!ModConfig.cats.modernOcelots || !isPlainOcelot(event.getTarget())) {
            return;
        }
        EntityOcelot ocelot = (EntityOcelot) event.getTarget();
        if (ocelot.isTamed()) {
            return;
        }
        EntityPlayer player = event.getEntityPlayer();
        ItemStack stack = player.getHeldItem(event.getHand());
        if (!CatContent.isFish(stack)) {
            return;
        }

        // From here 1.12.2's own handling (taming) must never run; the server decides what happens
        // and the client takes its word for the fish. The client has already sent its click on.
        event.setCanceled(true);
        event.setCancellationResult(EnumActionResult.SUCCESS);
        if (event.getWorld().isRemote) {
            return;
        }
        feed(ocelot, player, stack);
    }

    /** What 1.14's ocelot does with fish. Package-visible for the headless test. */
    static void feed(EntityOcelot ocelot, EntityPlayer player, ItemStack stack) {
        if (!isTrusting(ocelot)) {
            if (player.getDistanceSq(ocelot) >= 9.0D) {
                return;
            }
            eat(ocelot, player, stack);
            if (ocelot.getRNG().nextInt(3) == 0) {
                setTrusting(ocelot, true);
                ocelot.enablePersistence();
                ocelot.world.setEntityState(ocelot, (byte) 7);
            } else {
                ocelot.world.setEntityState(ocelot, (byte) 6);
            }
            return;
        }

        // Trusted: fish is just food, as for any animal - grow a kitten up, or breed.
        if (ocelot.isChild()) {
            eat(ocelot, player, stack);
            ocelot.ageUp((int) ((float) (-ocelot.getGrowingAge() / 20) * 0.1F), true);
        } else if (ocelot.getGrowingAge() == 0 && !ocelot.isInLove()) {
            eat(ocelot, player, stack);
            ocelot.setInLove(player);
        }
    }

    private static void eat(EntityOcelot ocelot, EntityPlayer player, ItemStack stack) {
        if (CatContent.eat != null) {
            ocelot.playSound(CatContent.eat, 1.0F, 1.0F);
        }
        if (!player.capabilities.isCreativeMode) {
            stack.shrink(1);
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Sounds
    // ---------------------------------------------------------------------------------------------

    @SubscribeEvent
    public void onSound(PlaySoundAtEntityEvent event) {
        if (!ModConfig.cats.modernOcelots || !isPlainOcelot(event.getEntity()) || ((EntityOcelot) event.getEntity()).isTamed()) {
            return;
        }
        if (event.getSound() == SoundEvents.ENTITY_CAT_HURT && CatContent.ocelotHurt != null) {
            event.setSound(CatContent.ocelotHurt);
        } else if (event.getSound() == SoundEvents.ENTITY_CAT_DEATH && CatContent.ocelotDeath != null) {
            event.setSound(CatContent.ocelotDeath);
        }
    }

    @SubscribeEvent
    public void onLivingUpdate(LivingEvent.LivingUpdateEvent event) {
        Entity entity = event.getEntityLiving();
        if (entity.world.isRemote || !ModConfig.cats.modernOcelots || !isPlainOcelot(entity)) {
            return;
        }
        EntityOcelot ocelot = (EntityOcelot) entity;
        if (!ocelot.isTamed() && ocelot.isEntityAlive() && CatContent.ocelotAmbient != null
                && ocelot.getRNG().nextInt(AMBIENT_INTERVAL) == 0) {
            ocelot.playSound(CatContent.ocelotAmbient, 0.4F,
                    (ocelot.getRNG().nextFloat() - ocelot.getRNG().nextFloat()) * 0.2F + 1.0F);
        }
    }
}
