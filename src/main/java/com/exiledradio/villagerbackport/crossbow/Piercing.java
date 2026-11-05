package com.exiledradio.villagerbackport.crossbow;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EntitySelectors;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.event.ForgeEventFactory;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The Piercing enchantment, for an arrow: it goes through as many targets as its level and hits the
 * one after. 1.12.2's arrow stops at the first thing it hurts, so this mod's arrow subclasses hand
 * their hits here first.
 *
 * <p>A target is only ever hit once by one arrow. That matters twice over: the arrow is still inside
 * the target it just went through on the next tick, and an arrow that bounced off armour would
 * otherwise come back for a second go.
 */
public final class Piercing {

    /** What an arrow subclass offers to have this work for it. */
    public interface Arrow {
        Piercing piercing();

        /** The arrow's own hit, without the piercing - its superclass's {@code onHit}. */
        void hit(RayTraceResult result);
    }

    private final EntityArrow arrow;
    private int level;
    private final Set<Integer> pierced = new HashSet<Integer>();
    /** Ticks in flight, kept here because the arrow's own count is private. */
    private int ticksInAir;

    Piercing(EntityArrow arrow) {
        this.arrow = arrow;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int level() {
        return this.level;
    }

    /** From the arrow's {@code onUpdate}, before its own: keeps count of the ticks in flight. */
    void tick(boolean inGround) {
        this.ticksInAir = inGround ? 0 : this.ticksInAir + 1;
    }

    /** From the arrow's {@code onHit}. */
    void onHit(RayTraceResult result) {
        Entity target = result.entityHit;
        if (this.level <= 0 || target == null) {
            ((Arrow) this.arrow).hit(result);
            return;
        }
        if (!this.pierced.add(target.getEntityId())) {
            return;
        }
        double mx = this.arrow.motionX;
        double my = this.arrow.motionY;
        double mz = this.arrow.motionZ;
        ((Arrow) this.arrow).hit(result);
        // It struck home and stopped: send it on, unless that was the last target it could pass.
        if (this.arrow.isDead && this.pierced.size() <= this.level) {
            this.arrow.isDead = false;
            this.arrow.motionX = mx;
            this.arrow.motionY = my;
            this.arrow.motionZ = mz;
            hitNextOnPath();
        }
    }

    /**
     * An arrow covers three blocks a tick, and stops looking once it has hit something; so one that has
     * just gone through a target would sail past the next one if it stood within the same tick's
     * flight. 1.14 keeps looking along the rest of the path, and so does this.
     */
    private void hitNextOnPath() {
        Vec3d start = new Vec3d(this.arrow.posX, this.arrow.posY, this.arrow.posZ);
        Vec3d end = start.add(this.arrow.motionX, this.arrow.motionY, this.arrow.motionZ);
        RayTraceResult block = this.arrow.world.rayTraceBlocks(start, end, false, true, false);
        if (block != null) {
            end = block.hitVec;
        }
        Entity next = findEntityOnPath(start, end);
        if (next == null) {
            return;
        }
        if (next instanceof EntityPlayer && this.arrow.shootingEntity instanceof EntityPlayer
                && !((EntityPlayer) this.arrow.shootingEntity).canAttackPlayer((EntityPlayer) next)) {
            return;
        }
        RayTraceResult hit = new RayTraceResult(next);
        if (!ForgeEventFactory.onProjectileImpact(this.arrow, hit)) {
            onHit(hit);
        }
    }

    /**
     * {@code EntityArrow.findEntityOnPath}, passing over anything already pierced. Without piercing
     * the arrow's own is used.
     */
    @Nullable
    Entity findEntityOnPath(Vec3d start, Vec3d end) {
        Entity nearest = null;
        double best = 0.0D;
        AxisAlignedBB sweep = this.arrow.getEntityBoundingBox()
                .expand(this.arrow.motionX, this.arrow.motionY, this.arrow.motionZ).grow(1.0D);
        List<Entity> candidates = this.arrow.world.getEntitiesInAABBexcluding(this.arrow, sweep,
                e -> EntitySelectors.NOT_SPECTATING.apply(e) && e.isEntityAlive() && e.canBeCollidedWith());
        for (Entity candidate : candidates) {
            if (this.pierced.contains(candidate.getEntityId())
                    || (candidate == this.arrow.shootingEntity && this.ticksInAir < 5)) {
                continue;
            }
            RayTraceResult hit = candidate.getEntityBoundingBox().grow(0.30000001192092896D).calculateIntercept(start, end);
            if (hit != null) {
                double distance = start.squareDistanceTo(hit.hitVec);
                if (nearest == null || distance < best) {
                    nearest = candidate;
                    best = distance;
                }
            }
        }
        return nearest;
    }

    void write(NBTTagCompound compound) {
        compound.setByte("PierceLevel", (byte) this.level);
    }

    void read(NBTTagCompound compound) {
        this.level = compound.getByte("PierceLevel");
    }
}
