package com.exiledradio.villagerbackport.job;

import com.exiledradio.villagerbackport.ModConfig;
import com.exiledradio.villagerbackport.VillagerBackport;

import net.minecraft.entity.ai.EntityAITasks;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.pathfinding.PathNavigateGround;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Gives every villager the go-to-work behaviour as it enters the world.
 *
 * <h2>Adding rather than replacing</h2>
 * {@code EntityLiving.tasks} is public, so the goal can simply be appended - no reflection, and no
 * touching what is already there. That matters in a large pack: villagers arrive with AI from
 * vanilla and from every mod that has had an opinion about them, and this contributes one more goal
 * without disturbing the order of the rest.
 *
 * <p>The priority is deliberately low. Fleeing zombies, trading with a player and breeding all
 * matter more than walking to a lectern, and a villager that ignored a player to go to work would be
 * worse than one that never worked at all.
 */
public final class JobSiteAttacher {

    /**
     * Priority for the work goal. Vanilla villagers use 0 through 9, with the low numbers on urgent
     * things - swimming, avoiding zombies, trading. This sits below all of that.
     */
    private static final int PRIORITY = 8;

    /**
     * Where going to bed sits among a villager's other business.
     *
     * <p>Ahead of work at 8 and of wandering, behind the flee-from-monster goals at 1. A villager
     * should abandon its workstation at dusk and abandon its bed for a zombie.
     */
    private static final int SLEEP_PRIORITY = 3;

    /** Where vanilla puts mating, and where its replacement goes. */
    private static final int MATE_PRIORITY = 6;

    @SubscribeEvent
    public void onEntityJoinWorld(EntityJoinWorldEvent event) {
        if (event.getWorld().isRemote || !(event.getEntity() instanceof EntityVillager)) {
            return;
        }

        if (!ModConfig.jobs.enabled) {
            return;
        }

        EntityVillager villager = (EntityVillager) event.getEntity();

        try {
            if (!hasWorkGoal(villager)) {
                villager.tasks.addTask(PRIORITY, new EntityAIWorkAtSite(villager));
            }

            // Above going to work and below fleeing, which is the order 1.14 puts them in: a
            // villager stops working to go to bed, and gets out of bed for a zombie.
            if (ModConfig.homes.enabled && ModConfig.homes.sleepAtNight && !hasSleepGoal(villager)) {
                villager.tasks.addTask(SLEEP_PRIORITY,
                        new com.exiledradio.villagerbackport.home.EntityAISleep(villager));
                removeMoveIndoors(villager);
            }

            if (ModConfig.homes.enabled && ModConfig.homes.bedsForBreeding
                    && !hasBreedGoal(villager)) {
                replaceMatingGoal(villager);
            }

            // Older versions raised this permanently, which made every path a villager computed -
            // wandering, fleeing, going indoors - search a far larger volume. See WorkPathing.
            WorkPathing.removeStale(villager);
            allowDoors(villager);
        } catch (RuntimeException e) {
            // A villager that never walks to work still trades; the gate fails open for it.
            VillagerBackport.LOGGER.error("Could not add the work goal to a villager.", e);
        }
    }

    /**
     * Makes sure a closed door counts as something a villager can walk through.
     *
     * <h2>Why this is set rather than assumed</h2>
     * The pathfinder only treats a closed wooden door as walkable when the navigator has <em>both</em>
     * flags: it may enter doors, and it may open them. Only the first is on by default. Villagers set
     * the second themselves, so in vanilla this is already true - but it is one line in a constructor
     * that any mod is free to reset, and a pack the size of RLCraft has plenty of candidates.
     *
     * <p>When it is not set the effect is quietly severe, because the door does not merely cost more
     * to walk through - it is impassable, so no path into the building exists at all. The villager
     * gets a path that stops in the street and stands there, which is exactly what a workstation
     * being "unreachable through a wall" looked like. Measured here: with the door shut a path ended
     * one block short of it, and the same path completed the moment the door was opened by hand.
     *
     * <p>Setting it costs nothing when it is already true, so it is set unconditionally rather than
     * detected.
     */
    private void allowDoors(EntityVillager villager) {
        if (villager.getNavigator() instanceof PathNavigateGround) {
            PathNavigateGround navigator = (PathNavigateGround) villager.getNavigator();
            navigator.setEnterDoors(true);
            navigator.setBreakDoors(true);
        }
    }

    /**
     * @return true if this villager already has the goal.
     *
     * <p>Entities can join the world more than once - dimension changes and chunk reloads both do
     * it - and adding the goal again each time would leave a villager running several copies of it.
     */
    /**
     * Takes away 1.12.2's own get-inside-at-night goal.
     *
     * <h2>Why it has to go</h2>
     * {@code EntityAIMoveIndoors} sits at priority 2 and runs at dusk, which is exactly when a
     * villager should be walking to its bed. It holds the movement mutex from a stronger position
     * than a sleeping goal can be given without also outranking fleeing from zombies, so the two
     * cannot share a villager: whichever way the priorities are set, one of them never runs.
     *
     * <p>1.14 has no such behaviour. Going indoors at night is what villagers did before they had
     * beds to go to, and the schedule replaced it - so this is not a compromise to make sleeping
     * work, it is the same removal Mojang made.
     *
     * <p>Only when sleeping is switched on. With it off a villager keeps every goal it was built
     * with and behaves exactly as 1.12.2 intends.
     */
    private void removeMoveIndoors(EntityVillager villager) {
        for (EntityAITasks.EntityAITaskEntry entry :
                new java.util.ArrayList<EntityAITasks.EntityAITaskEntry>(villager.tasks.taskEntries)) {

            if (entry.action instanceof net.minecraft.entity.ai.EntityAIMoveIndoors) {
                villager.tasks.removeTask(entry.action);
            }
        }
    }

    /**
     * Swaps 1.12.2's door-counting mating goal for one that counts beds.
     *
     * <h2>Why the vanilla one cannot stay</h2>
     * {@code EntityAIVillagerMate} asks the village collection for a village and gives up if there
     * is not one, and a village in 1.12.2 is made of doors. A breeder built the way 1.14 taught
     * people to build them - beds, food, no doors - therefore never breeds at all, no matter how
     * many beds are free, because the courtship never starts.
     *
     * <p>Both goals running would be worse than either: two tasks at the same priority competing for
     * the same pair of villagers, one of which can never succeed. So vanilla's is taken off.
     */
    private void replaceMatingGoal(EntityVillager villager) {
        for (EntityAITasks.EntityAITaskEntry entry :
                new java.util.ArrayList<EntityAITasks.EntityAITaskEntry>(villager.tasks.taskEntries)) {

            if (entry.action instanceof net.minecraft.entity.ai.EntityAIVillagerMate) {
                villager.tasks.removeTask(entry.action);
            }
        }

        villager.tasks.addTask(MATE_PRIORITY,
                new com.exiledradio.villagerbackport.home.EntityAIBreedOnBeds(villager));
    }

    private boolean hasBreedGoal(EntityVillager villager) {
        for (EntityAITasks.EntityAITaskEntry entry : villager.tasks.taskEntries) {
            if (entry.action instanceof com.exiledradio.villagerbackport.home.EntityAIBreedOnBeds) {
                return true;
            }
        }
        return false;
    }

    private boolean hasSleepGoal(EntityVillager villager) {
        for (EntityAITasks.EntityAITaskEntry entry : villager.tasks.taskEntries) {
            if (entry.action instanceof com.exiledradio.villagerbackport.home.EntityAISleep) {
                return true;
            }
        }
        return false;
    }

    private boolean hasWorkGoal(EntityVillager villager) {
        for (EntityAITasks.EntityAITaskEntry entry : villager.tasks.taskEntries) {
            if (entry.action instanceof EntityAIWorkAtSite) {
                return true;
            }
        }
        return false;
    }
}
