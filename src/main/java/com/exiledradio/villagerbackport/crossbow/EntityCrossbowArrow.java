package com.exiledradio.villagerbackport.crossbow;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.projectile.EntityTippedArrow;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import javax.annotation.Nullable;

/** An arrow or tipped arrow from a crossbow: vanilla's, able to pierce. See {@link Piercing}. */
public class EntityCrossbowArrow extends EntityTippedArrow implements Piercing.Arrow {

    private final Piercing piercing = new Piercing(this);

    public EntityCrossbowArrow(World world) {
        super(world);
    }

    public EntityCrossbowArrow(World world, EntityLivingBase shooter) {
        super(world, shooter);
    }

    @Override
    public Piercing piercing() {
        return this.piercing;
    }

    @Override
    public void onUpdate() {
        this.piercing.tick(this.inGround);
        super.onUpdate();
    }

    @Override
    protected void onHit(RayTraceResult result) {
        this.piercing.onHit(result);
    }

    @Override
    public void hit(RayTraceResult result) {
        super.onHit(result);
    }

    @Nullable
    @Override
    protected Entity findEntityOnPath(Vec3d start, Vec3d end) {
        return this.piercing.level() > 0 ? this.piercing.findEntityOnPath(start, end) : super.findEntityOnPath(start, end);
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound compound) {
        super.writeEntityToNBT(compound);
        this.piercing.write(compound);
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound compound) {
        super.readEntityFromNBT(compound);
        this.piercing.read(compound);
    }
}
