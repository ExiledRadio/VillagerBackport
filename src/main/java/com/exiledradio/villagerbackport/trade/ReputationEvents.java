package com.exiledradio.villagerbackport.trade;

import com.exiledradio.villagerbackport.ModConfig;
import com.exiledradio.villagerbackport.VillagerBackport;

import net.minecraft.entity.Entity;
import net.minecraft.entity.monster.EntityZombieVillager;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import javax.annotation.Nullable;

import java.lang.reflect.Field;
import java.util.List;
import java.util.UUID;

/**
 * The things a player can do that a villager will remember.
 *
 * <p>1.14 routes these through {@code IReputationType} and hands them to every villager that ought
 * to know, which for a killing means the witnesses rather than the victim. The four events and their
 * weights are 1.14's:
 *
 * <pre>
 *   ZOMBIE_VILLAGER_CURED -&gt; MAJOR_POSITIVE +20 and MINOR_POSITIVE +25
 *   TRADE                 -&gt; TRADING        +2
 *   VILLAGER_HURT         -&gt; MINOR_NEGATIVE +25
 *   VILLAGER_KILLED       -&gt; MAJOR_NEGATIVE +25
 * </pre>
 *
 * <p>Trading is handled where trades are counted rather than here, because that is the only place
 * that knows a trade actually completed.
 */
public final class ReputationEvents {

    /** How far a villager has to be to have witnessed something, matching 1.14's reach. */
    private static final double WITNESS_RANGE = 16.0D;

    /**
     * {@code private UUID converstionStarter} - vanilla's spelling - on a converting zombie
     * villager. It is the player who fed it the golden apple, and the only record of who cured it.
     */
    @Nullable
    private static Field conversionStarter;

    private static boolean resolved;

    /**
     * A villager was hurt: it holds that against whoever did it.
     *
     * <p>Against the attacker only, and only the villager that was actually hit - 1.14 reports this
     * one from the victim rather than to the neighbourhood.
     */
    @SubscribeEvent
    public void onHurt(LivingHurtEvent event) {
        if (!(event.getEntityLiving() instanceof EntityVillager) || event.getEntityLiving().world.isRemote) {
            return;
        }

        EntityPlayer player = playerBehind(event.getSource().getTrueSource());
        if (player != null) {
            Gossip.add((EntityVillager) event.getEntityLiving(), player.getUniqueID(),
                    GossipType.MINOR_NEGATIVE, 25);
        }
    }

    /**
     * A villager was killed: the ones who saw it hold that against whoever did it.
     *
     * <p>Told to the witnesses rather than the victim, which is the only way it can matter - a dead
     * villager's opinion goes with it. This is 1.14's {@code tellWitnessesThatIWasMurdered}.
     */
    @SubscribeEvent
    public void onDeath(LivingDeathEvent event) {
        if (!(event.getEntityLiving() instanceof EntityVillager) || event.getEntityLiving().world.isRemote) {
            return;
        }

        EntityPlayer player = playerBehind(event.getSource().getTrueSource());
        if (player == null) {
            return;
        }

        EntityVillager victim = (EntityVillager) event.getEntityLiving();
        AxisAlignedBB box = victim.getEntityBoundingBox().grow(WITNESS_RANGE);

        for (EntityVillager witness : victim.world.getEntitiesWithinAABB(EntityVillager.class, box)) {
            if (witness != victim) {
                Gossip.add(witness, player.getUniqueID(), GossipType.MAJOR_NEGATIVE, 25);
            }
        }
    }

    /**
     * A zombie villager finished being cured: the villager it turned back into remembers who did it.
     *
     * <h2>Why this is caught here of all places</h2>
     * There is no event for it. 1.12.2 cures a zombie villager inside
     * {@code EntityZombieVillager.finishConversion}, which spawns the new villager and then kills the
     * zombie, and Forge has nothing to say about either half.
     *
     * <p>What that method does leave is a window: at the moment the new villager joins the world the
     * zombie is still standing in the same place, still holding the UUID of whoever fed it the golden
     * apple. So a villager appearing on top of a converting zombie villager is the cure, and the
     * zombie is asked who to thank on its way out.
     *
     * <p>This is the discount that matters. 1.14 grants both positives for a cure - 20 major and 25
     * minor - and major positive is the one gossip type that never decays, so a cured villager is
     * permanently cheaper for the player who cured it.
     */
    @SubscribeEvent
    public void onEntityJoin(EntityJoinWorldEvent event) {
        if (!ModConfig.pricing.gossipEnabled || event.getWorld().isRemote) {
            return;
        }

        if (!(event.getEntity() instanceof EntityVillager)) {
            return;
        }

        EntityVillager villager = (EntityVillager) event.getEntity();
        EntityZombieVillager zombie = Zombification.convertingUnder(villager);

        if (zombie == null) {
            return;
        }

        // The trades, level, experience and gossip it had before it was bitten, back where they
        // belong - and before the cure is credited, so the discount applies to the book it was
        // already selling rather than to a fresh roll.
        Zombification.restore(villager, zombie);

        UUID curer = curerOf(zombie);
        if (curer != null) {
            Gossip.add(villager, curer, GossipType.MAJOR_POSITIVE, 20);
            Gossip.add(villager, curer, GossipType.MINOR_POSITIVE, 25);
        }
    }

    /** @return who fed this zombie villager the golden apple, or null if that cannot be read. */
    @Nullable
    private static UUID curerOf(EntityZombieVillager zombie) {
        Field field = starterField();
        if (field == null) {
            return null;
        }

        try {
            return (UUID) field.get(zombie);
        } catch (IllegalAccessException e) {
            return null;
        }
    }

    @Nullable
    private static Field starterField() {
        if (resolved) {
            return conversionStarter;
        }

        resolved = true;

        try {
            conversionStarter = ReflectionHelper.findField(
                    EntityZombieVillager.class, "converstionStarter", "field_191992_by");
            conversionStarter.setAccessible(true);
        } catch (RuntimeException e) {
            VillagerBackport.LOGGER.warn(
                    "Could not read who cures a zombie villager; cure discounts are off.", e);
            conversionStarter = null;
        }

        return conversionStarter;
    }

    /** @return the player responsible for a source of damage, directly or through what they fired. */
    @Nullable
    private static EntityPlayer playerBehind(@Nullable Entity source) {
        return source instanceof EntityPlayer ? (EntityPlayer) source : null;
    }
}
