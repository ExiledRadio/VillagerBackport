package com.exiledradio.villagerbackport.raid;

import com.exiledradio.villagerbackport.VillagerBackport;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.village.MerchantRecipe;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.registry.VillagerRegistry;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Raids Backport's raids, read by reflection.
 *
 * <h2>Why reflection</h2>
 * The mod is optional, so nothing here may name one of its classes where the JVM would have to load
 * it. Every member is looked up once, by name, the first time this view is built, and if any of
 * them is missing - a renamed method in a later version - the whole view is refused and villagers
 * fall back to living without raids. A half-working bridge would be worse than none.
 *
 * <p>Checked against Raids Backport 1.1.5b.
 */
final class RaidsBackportRaids implements RaidView {

    static final String MOD_ID = "raids";

    private final Method getData;
    private final Method getRaidAt;
    private final Method hasActiveRaid;
    private final Method isActive;
    private final Method isVictory;
    private final Method isLoss;
    private final Method isStopped;
    private final Method hasFirstWaveSpawned;

    /** Raids Backport's discount, mixed into every trade. Null if this version has none. */
    @Nullable
    private final Method setDiscountedPrice;

    /** Its villager gift table, for finding the careers it gives nothing for. Null if absent. */
    @Nullable
    private final Object giftConfig;
    @Nullable
    private final Method possibleGifts;

    private RaidsBackportRaids() throws ReflectiveOperationException {
        Class<?> data = Class.forName("net.smileycorp.raids.common.raid.WorldDataRaids");
        Class<?> raid = Class.forName("net.smileycorp.raids.common.raid.Raid");
        Class<?> handler = Class.forName("net.smileycorp.raids.common.raid.RaidHandler");

        this.getData = data.getMethod("getData", WorldServer.class);
        this.getRaidAt = data.getMethod("getRaidAt", BlockPos.class);
        this.hasActiveRaid = handler.getMethod("hasActiveRaid", Entity.class);
        this.isActive = raid.getMethod("isActive");
        this.isVictory = raid.getMethod("isVictory");
        this.isLoss = raid.getMethod("isLoss");
        this.isStopped = raid.getMethod("isStopped");
        this.hasFirstWaveSpawned = raid.getMethod("hasFirstWaveSpawned");

        Method discount = null;
        try {
            discount = Class.forName("net.smileycorp.raids.common.interfaces.ITradeDiscount")
                    .getMethod("setDiscountedPrice", int.class);
        } catch (ReflectiveOperationException e) {
            VillagerBackport.LOGGER.warn("Raids Backport's trade discount was not found; "
                    + "its hero discount will not be taken off trades.");
        }
        this.setDiscountedPrice = discount;

        Object gifts = null;
        Method possible = null;
        try {
            Class<?> config = Class.forName("net.smileycorp.raids.config.VillagerGiftsConfig");
            Field instance = config.getField("INSTANCE");
            gifts = instance.get(null);
            possible = config.getMethod("getPossibleGifts", VillagerRegistry.VillagerCareer.class);
        } catch (ReflectiveOperationException e) {
            VillagerBackport.LOGGER.warn("Raids Backport's villager gifts were not found; "
                    + "this mod will not fill in the careers it gives nothing for.");
        }
        this.giftConfig = gifts;
        this.possibleGifts = possible;
    }

    /** @return the view, or null if this version of Raids Backport does not fit it */
    @Nullable
    static RaidsBackportRaids create() {
        try {
            return new RaidsBackportRaids();
        } catch (ReflectiveOperationException | LinkageError e) {
            VillagerBackport.LOGGER.error("Raids Backport is installed but its raids could not be read; "
                    + "villagers will not react to them.", e);
            return null;
        }
    }

    @Nullable
    @Override
    public Raid raidAt(World world, BlockPos pos) {
        if (!(world instanceof WorldServer)) {
            return null;
        }

        try {
            Object data = this.getData.invoke(null, world);
            Object raid = data == null ? null : this.getRaidAt.invoke(data, pos);
            return raid == null ? null : new Wrapped(raid);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    @Override
    public boolean inRaid(Entity entity) {
        try {
            return (Boolean) this.hasActiveRaid.invoke(null, entity);
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }

    /**
     * Takes Raids Backport's discount off a trade, so the hero discount is counted once - here,
     * through this mod's prices.
     */
    void clearDiscount(MerchantRecipe recipe) {
        if (this.setDiscountedPrice == null) {
            return;
        }

        try {
            this.setDiscountedPrice.invoke(recipe, 0);
        } catch (ReflectiveOperationException | ClassCastException e) {
            // A trade the mixin did not reach; it carries no discount to clear.
        }
    }

    /** @return true if Raids Backport has gifts of its own for this career */
    boolean givesGiftsFor(VillagerRegistry.VillagerCareer career) {
        if (this.possibleGifts == null || this.giftConfig == null) {
            // Not known, so assume it does: two gifts are worse than none from this mod.
            return true;
        }

        try {
            List<?> gifts = (List<?>) this.possibleGifts.invoke(this.giftConfig, career);
            return gifts != null && !gifts.isEmpty();
        } catch (ReflectiveOperationException e) {
            return true;
        }
    }

    private final class Wrapped implements Raid {

        private final Object raid;

        Wrapped(Object raid) {
            this.raid = raid;
        }

        @Override
        public boolean isActive() {
            return ask(isActive);
        }

        @Override
        public boolean isVictory() {
            return ask(isVictory);
        }

        @Override
        public boolean isLoss() {
            return ask(isLoss);
        }

        @Override
        public boolean isStopped() {
            return ask(isStopped);
        }

        @Override
        public boolean hasFirstWaveSpawned() {
            return ask(hasFirstWaveSpawned);
        }

        private boolean ask(Method method) {
            try {
                return (Boolean) method.invoke(this.raid);
            } catch (ReflectiveOperationException e) {
                return false;
            }
        }
    }
}
