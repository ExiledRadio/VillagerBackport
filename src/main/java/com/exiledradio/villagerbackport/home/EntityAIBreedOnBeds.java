package com.exiledradio.villagerbackport.home;

import com.exiledradio.villagerbackport.ModConfig;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.BabyEntitySpawnEvent;

/**
 * Villagers breeding because there is a bed spare, rather than because there are enough doors.
 *
 * <h2>Why vanilla's task had to be replaced rather than nudged</h2>
 * 1.12.2 counts doors, and it does the counting inside {@code EntityAIVillagerMate}:
 *
 * <pre>
 *   village = world.getVillageCollection().getNearestVillage(pos, 0);
 *   if (village == null) return false;
 *   int i = (int)(village.getNumVillageDoors() * 0.35D);
 *   return village.getNumVillagers() &lt; i;
 * </pre>
 *
 * <p>Every line of that is fatal to a village built the way 1.14 taught people to build one. A
 * breeder made of beds and food has no doors, so there is no village at all, so
 * {@code getNearestVillage} returns null and mating never even begins. No amount of gating births
 * afterwards can help, because the courtship never starts - which is why the first attempt at this,
 * a check on the baby, did nothing for anyone following a modern tutorial.
 *
 * <p>So the whole task is replaced. This is vanilla's, line for line - the same one-in-five-hundred
 * rate, the same eight-block search for a partner, the same three hundred tick courtship, the same
 * birth - with the door test swapped for 1.14's question: is there a bed free for the child.
 *
 * <p>Willingness is left exactly as 1.12.2 has it. Food is still what makes a villager willing, and
 * {@code getIsWillingToMate} still decides, so bread and carrots work the way every player expects.
 */
public final class EntityAIBreedOnBeds extends EntityAIBase {

    /** Vanilla's courtship length, in ticks. */
    private static final int COURTSHIP = 300;

    /** How far apart two villagers may be and still find each other, as vanilla has it. */
    private static final double SEARCH_RANGE = 8.0D;

    private final EntityVillager villager;
    private final World world;

    private EntityVillager mate;
    private int matingTimeout;

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

        if (this.villager.getGrowingAge() != 0) {
            return false;
        }

        // Vanilla's rate. Breeding is meant to be something that happens eventually, not on cue.
        if (this.villager.getRNG().nextInt(500) != 0) {
            return false;
        }

        if (!BedBreeding.hasFreeBed(this.villager)) {
            return false;
        }

        if (!this.villager.getIsWillingToMate(true)) {
            return false;
        }

        Entity found = this.world.findNearestEntityWithinAABB(EntityVillager.class,
                this.villager.getEntityBoundingBox().grow(SEARCH_RANGE, 3.0D, SEARCH_RANGE),
                this.villager);

        if (!(found instanceof EntityVillager)) {
            return false;
        }

        this.mate = (EntityVillager) found;
        return this.mate.getGrowingAge() == 0 && this.mate.getIsWillingToMate(true);
    }

    @Override
    public void startExecuting() {
        this.matingTimeout = COURTSHIP;
        this.villager.setMating(true);
    }

    @Override
    public boolean shouldContinueExecuting() {
        return this.matingTimeout >= 0
                && BedBreeding.hasFreeBed(this.villager)
                && this.villager.getGrowingAge() == 0
                && this.villager.getIsWillingToMate(false);
    }

    @Override
    public void updateTask() {
        --this.matingTimeout;

        this.villager.getLookHelper().setLookPositionWithEntity(this.mate, 10.0F, 30.0F);

        if (this.villager.getDistanceSq(this.mate) > 2.25D) {
            this.villager.getNavigator().tryMoveToEntityLiving(this.mate, 0.25D);
        } else if (this.matingTimeout == 0 && this.mate.isMating()) {
            giveBirth();
        }

        // The hearts, which vanilla throws at the same rate.
        if (this.villager.getRNG().nextInt(35) == 0) {
            this.world.setEntityState(this.villager, (byte) 12);
        }
    }

    @Override
    public void resetTask() {
        this.mate = null;
        this.villager.setMating(false);
    }

    /**
     * Brings the child into the world, exactly as vanilla does.
     *
     * <p>Including the Forge event, which other mods listen to and can cancel - a birth this mod
     * arranges should be as interruptible as one the game arranges itself.
     */
    private void giveBirth() {
        EntityAgeable child = this.villager.createChild(this.mate);

        this.mate.setGrowingAge(6000);
        this.villager.setGrowingAge(6000);
        this.mate.setIsWillingToMate(false);
        this.villager.setIsWillingToMate(false);

        BabyEntitySpawnEvent event = new BabyEntitySpawnEvent(this.villager, this.mate, child);

        if (MinecraftForge.EVENT_BUS.post(event) || event.getChild() == null) {
            return;
        }

        child = event.getChild();
        child.setGrowingAge(-24000);
        child.setLocationAndAngles(this.villager.posX, this.villager.posY, this.villager.posZ,
                0.0F, 0.0F);

        this.world.spawnEntity(child);
        this.world.setEntityState(child, (byte) 12);

        VillageDebug.say(this.villager, "breeding: a child was born");
    }
}
