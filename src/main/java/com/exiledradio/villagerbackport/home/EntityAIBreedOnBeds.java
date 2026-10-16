package com.exiledradio.villagerbackport.home;

import com.exiledradio.villagerbackport.ModConfig;
import com.exiledradio.villagerbackport.job.Employment;
import com.exiledradio.villagerbackport.restock.RestockRules;

import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.BabyEntitySpawnEvent;

/**
 * Two villagers having a baby, the way the game does it.
 *
 * <h2>Who</h2>
 * Two grown villagers near each other, both with a bed of their own and the food to be willing -
 * see {@link VillagerFood} - and neither on its five-minute cooldown. Employed villagers do not breed during work hours;
 * nitwits and the unemployed can, at any time they are up and about.
 *
 * <h2>How</h2>
 * They spend about fourteen to sixteen seconds together, hearts showing now and then. Then both eat
 * the food breeding costs, whatever happens next. A baby needs a free bed within 48 blocks that the
 * parent can find a path to and that has two empty blocks above it; with one, the baby is born and
 * given that bed. Without one, both parents show angry particles and no baby comes.
 *
 * <p>This replaces 1.12.2's own mating goal, which counts doors and gives up with no village, so a
 * breeder built of beds and food without doors never bred at all.
 */
public final class EntityAIBreedOnBeds extends EntityAIBase {

    /** The game's courtship: 275 ticks, plus up to 50 more. */
    private static final int COURTSHIP = 275;
    private static final int COURTSHIP_SPREAD = 50;

    /** How close the pair has to stand, as the game has it: within the square root of 5. */
    private static final double CLOSE_ENOUGH_SQ = 5.0D;

    private static final double SEARCH_RANGE = 8.0D;

    private static final byte HEARTS = 12;
    private static final byte ANGRY = 13;

    private final EntityVillager villager;
    private final World world;

    private EntityVillager mate;
    private long endsAt;
    private boolean done;

    public EntityAIBreedOnBeds(EntityVillager villager) {
        this.villager = villager;
        this.world = villager.world;

        // Movement and looking, the same pair vanilla's mating task reserves.
        this.setMutexBits(3);
    }

    @Override
    public boolean shouldExecute() {
        if (!ModConfig.homes.enabled || !ModConfig.homes.bedsForBreeding) {
            return false;
        }

        // Vanilla's rate. Breeding is meant to be something that happens eventually, not on cue.
        if (this.villager.getRNG().nextInt(500) != 0 || !ready(this.villager)) {
            return false;
        }

        for (EntityVillager other : this.world.getEntitiesWithinAABB(EntityVillager.class,
                this.villager.getEntityBoundingBox().grow(SEARCH_RANGE, 3.0D, SEARCH_RANGE))) {
            if (other != this.villager && ready(other) && this.villager.canEntityBeSeen(other)) {
                this.mate = other;
                return true;
            }
        }

        return false;
    }

    /** Grown, off cooldown, fed, awake, not at work, and with a bed of its own. */
    private static boolean ready(EntityVillager villager) {
        return villager.isEntityAlive()
                && villager.getGrowingAge() == 0
                && villager.getCustomer() == null
                && !HomeSite.isSleeping(villager)
                && !EntityAISleep.isBedtime(villager)
                && !PanicWatch.isPanicking(villager)
                && !atWork(villager)
                && HomeSite.validated(villager) != null
                && VillagerFood.isWilling(villager);
    }

    /** Employed villagers keep their work hours for work. */
    private static boolean atWork(EntityVillager villager) {
        return !Employment.isUnemployed(villager) && !Employment.isNitwit(villager)
                && RestockRules.isWorkingHours(villager.world);
    }

    @Override
    public void startExecuting() {
        this.done = false;
        this.endsAt = this.world.getTotalWorldTime() + COURTSHIP
                + this.villager.getRNG().nextInt(COURTSHIP_SPREAD);
        this.villager.setMating(true);
        this.mate.setMating(true);
    }

    @Override
    public boolean shouldContinueExecuting() {
        return !this.done && this.mate != null && this.mate.isEntityAlive()
                && this.world.getTotalWorldTime() <= this.endsAt
                && ready(this.villager) && ready(this.mate);
    }

    @Override
    public void updateTask() {
        this.villager.getLookHelper().setLookPositionWithEntity(this.mate, 10.0F, 30.0F);

        if (this.villager.getDistanceSq(this.mate) > CLOSE_ENOUGH_SQ) {
            this.villager.getNavigator().tryMoveToEntityLiving(this.mate, 0.5D);
            return;
        }

        if (this.world.getTotalWorldTime() >= this.endsAt) {
            this.done = true;
            breed();
        } else if (this.villager.getRNG().nextInt(35) == 0) {
            this.world.setEntityState(this.villager, HEARTS);
            this.world.setEntityState(this.mate, HEARTS);
        }
    }

    @Override
    public void resetTask() {
        this.villager.setMating(false);
        if (this.mate != null) {
            this.mate.setMating(false);
        }
        this.mate = null;
    }

    private void breed() {
        // Eaten first, whatever comes of it.
        VillagerFood.eatForBreeding(this.villager);
        VillagerFood.eatForBreeding(this.mate);

        BlockPos bed = HomeClaims.freeBedFor(this.villager, true);
        if (bed == null) {
            this.world.setEntityState(this.villager, ANGRY);
            this.world.setEntityState(this.mate, ANGRY);
            VillageDebug.say(this.villager, "breeding: no free bed it could reach, so no baby");
            return;
        }

        EntityAgeable child = this.villager.createChild(this.mate);
        if (child == null) {
            return;
        }

        BabyEntitySpawnEvent event = new BabyEntitySpawnEvent(this.villager, this.mate, child);
        if (MinecraftForge.EVENT_BUS.post(event) || event.getChild() == null) {
            return;
        }

        this.villager.setGrowingAge(6000);
        this.mate.setGrowingAge(6000);

        child = event.getChild();
        child.setGrowingAge(-24000);
        child.setLocationAndAngles(this.villager.posX, this.villager.posY, this.villager.posZ, 0.0F, 0.0F);

        this.world.spawnEntity(child);
        this.world.setEntityState(child, HEARTS);

        // The bed that made the birth possible is the baby's.
        if (child instanceof EntityVillager) {
            HomeSite.set((EntityVillager) child, bed);
        }

        VillageDebug.say(this.villager, "breeding: a child was born");
    }
}
