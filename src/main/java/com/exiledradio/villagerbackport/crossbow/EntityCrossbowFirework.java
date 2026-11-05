package com.exiledradio.villagerbackport.crossbow;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IProjectile;
import net.minecraft.entity.MoverType;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSourceIndirect;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * A firework rocket fired from a crossbow: 1.14's rocket "shot at an angle".
 *
 * <p>1.12.2's rocket can only rise, speeding up as it goes, and passes through anything in its way;
 * so this is a rocket of its own. It flies straight at the speed it was shot, with no gravity, for a
 * normal rocket's lifetime, and goes off when it hits something or its time is up. The blast is
 * vanilla's: 5 damage plus 2 for each firework star, falling off over five blocks, to everything in
 * line of sight - the shooter included - and credited to the shooter.
 */
public class EntityCrossbowFirework extends Entity implements IProjectile {

    private static final DataParameter<ItemStack> ROCKET = EntityDataManager.createKey(EntityCrossbowFirework.class, DataSerializers.ITEM_STACK);

    private int age;
    private int lifetime;
    @Nullable
    private EntityLivingBase shooter;
    @Nullable
    private UUID shooterId;

    public EntityCrossbowFirework(World world) {
        super(world);
        setSize(0.25F, 0.25F);
    }

    public EntityCrossbowFirework(World world, ItemStack rocket, EntityLivingBase shooter) {
        this(world);
        this.shooter = shooter;
        this.shooterId = shooter.getUniqueID();
        setPosition(shooter.posX, shooter.posY + shooter.getEyeHeight() - 0.15D, shooter.posZ);
        ItemStack copy = rocket.copy();
        copy.setCount(1);
        this.dataManager.set(ROCKET, copy);

        int flight = 1;
        NBTTagCompound fireworks = copy.getSubCompound("Fireworks");
        if (fireworks != null) {
            flight += fireworks.getByte("Flight");
        }
        this.lifetime = 10 * flight + this.rand.nextInt(6) + this.rand.nextInt(7);
    }

    @Override
    protected void entityInit() {
        this.dataManager.register(ROCKET, ItemStack.EMPTY);
    }

    public ItemStack getRocket() {
        ItemStack rocket = this.dataManager.get(ROCKET);
        return rocket.isEmpty() ? new ItemStack(Items.FIREWORKS) : rocket;
    }

    @Override
    public void shoot(double x, double y, double z, float velocity, float inaccuracy) {
        double length = MathHelper.sqrt(x * x + y * y + z * z);
        x = x / length + this.rand.nextGaussian() * 0.0075D * inaccuracy;
        y = y / length + this.rand.nextGaussian() * 0.0075D * inaccuracy;
        z = z / length + this.rand.nextGaussian() * 0.0075D * inaccuracy;
        this.motionX = x * velocity;
        this.motionY = y * velocity;
        this.motionZ = z * velocity;
        face();
        this.prevRotationYaw = this.rotationYaw;
        this.prevRotationPitch = this.rotationPitch;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void setVelocity(double x, double y, double z) {
        this.motionX = x;
        this.motionY = y;
        this.motionZ = z;
        if (this.prevRotationPitch == 0.0F && this.prevRotationYaw == 0.0F) {
            face();
            this.prevRotationYaw = this.rotationYaw;
            this.prevRotationPitch = this.rotationPitch;
        }
    }

    private void face() {
        float horizontal = MathHelper.sqrt(this.motionX * this.motionX + this.motionZ * this.motionZ);
        this.rotationYaw = (float) (MathHelper.atan2(this.motionX, this.motionZ) * (180D / Math.PI));
        this.rotationPitch = (float) (MathHelper.atan2(this.motionY, horizontal) * (180D / Math.PI));
    }

    @Override
    public void onUpdate() {
        this.lastTickPosX = this.posX;
        this.lastTickPosY = this.posY;
        this.lastTickPosZ = this.posZ;
        super.onUpdate();

        if (this.age == 0 && !isSilent()) {
            this.world.playSound(null, this.posX, this.posY, this.posZ, SoundEvents.ENTITY_FIREWORK_LAUNCH,
                    SoundCategory.AMBIENT, 3.0F, 1.0F);
        }
        this.age++;

        if (!this.world.isRemote) {
            RayTraceResult hit = findHit();
            if (hit != null && (hit.entityHit != null || hasStars())) {
                explode();
                return;
            }
        }

        move(MoverType.SELF, this.motionX, this.motionY, this.motionZ);
        face();

        if (this.world.isRemote) {
            this.world.spawnParticle(EnumParticleTypes.FIREWORKS_SPARK, this.posX, this.posY - 0.3D, this.posZ,
                    this.rand.nextGaussian() * 0.05D, -this.motionY * 0.5D, this.rand.nextGaussian() * 0.05D);
        } else if (this.age > this.lifetime) {
            explode();
        }
    }

    /** What lies between here and where it will be next tick: the nearest living thing, else a block. */
    @Nullable
    private RayTraceResult findHit() {
        Vec3d start = new Vec3d(this.posX, this.posY, this.posZ);
        Vec3d end = new Vec3d(this.posX + this.motionX, this.posY + this.motionY, this.posZ + this.motionZ);
        RayTraceResult block = this.world.rayTraceBlocks(start, end, false, true, false);
        if (block != null) {
            end = block.hitVec;
        }

        EntityLivingBase owner = shooter();
        Entity nearest = null;
        double best = 0.0D;
        for (Entity candidate : this.world.getEntitiesWithinAABBExcludingEntity(this,
                getEntityBoundingBox().expand(this.motionX, this.motionY, this.motionZ).grow(1.0D))) {
            if (candidate == owner || !candidate.canBeCollidedWith() || !candidate.isEntityAlive()
                    || (candidate instanceof net.minecraft.entity.player.EntityPlayer
                    && ((net.minecraft.entity.player.EntityPlayer) candidate).isSpectator())) {
                continue;
            }
            RayTraceResult hit = candidate.getEntityBoundingBox().grow(0.3D).calculateIntercept(start, end);
            if (hit != null) {
                double distance = start.squareDistanceTo(hit.hitVec);
                if (nearest == null || distance < best) {
                    nearest = candidate;
                    best = distance;
                }
            }
        }
        return nearest != null ? new RayTraceResult(nearest) : block;
    }

    private boolean hasStars() {
        NBTTagCompound fireworks = getRocket().getSubCompound("Fireworks");
        return fireworks != null && !fireworks.getTagList("Explosions", 10).isEmpty();
    }

    private void explode() {
        this.world.setEntityState(this, (byte) 17);
        dealExplosionDamage();
        setDead();
    }

    /** Vanilla's {@code dealExplosionDamage}, credited to the shooter. */
    private void dealExplosionDamage() {
        NBTTagCompound fireworks = getRocket().getSubCompound("Fireworks");
        NBTTagList stars = fireworks == null ? null : fireworks.getTagList("Explosions", 10);
        if (stars == null || stars.isEmpty()) {
            return;
        }
        float damage = 5 + stars.tagCount() * 2;
        DamageSource source = new EntityDamageSourceIndirect("fireworks", this, shooter()).setExplosion();
        Vec3d centre = new Vec3d(this.posX, this.posY, this.posZ);
        for (EntityLivingBase target : this.world.getEntitiesWithinAABB(EntityLivingBase.class, getEntityBoundingBox().grow(5.0D))) {
            if (getDistanceSq(target) > 25.0D) {
                continue;
            }
            boolean seen = false;
            for (int i = 0; i < 2 && !seen; i++) {
                RayTraceResult line = this.world.rayTraceBlocks(centre,
                        new Vec3d(target.posX, target.posY + target.height * 0.5D * i, target.posZ), false, true, false);
                seen = line == null || line.typeOfHit == RayTraceResult.Type.MISS;
            }
            if (seen) {
                target.attackEntityFrom(source, damage * (float) Math.sqrt((5.0D - getDistance(target)) / 5.0D));
            }
        }
    }

    @Nullable
    private EntityLivingBase shooter() {
        if (this.shooter == null && this.shooterId != null && this.world instanceof net.minecraft.world.WorldServer) {
            Entity found = ((net.minecraft.world.WorldServer) this.world).getEntityFromUuid(this.shooterId);
            if (found instanceof EntityLivingBase) {
                this.shooter = (EntityLivingBase) found;
            }
        }
        return this.shooter;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void handleStatusUpdate(byte id) {
        if (id == 17) {
            this.world.makeFireworks(this.posX, this.posY, this.posZ, this.motionX, this.motionY, this.motionZ,
                    getRocket().getSubCompound("Fireworks"));
        }
        super.handleStatusUpdate(id);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean isInRangeToRenderDist(double distance) {
        return distance < 4096.0D;
    }

    @Override
    public boolean canBeAttackedWithItem() {
        return false;
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound compound) {
        compound.setInteger("Life", this.age);
        compound.setInteger("LifeTime", this.lifetime);
        compound.setTag("FireworksItem", getRocket().writeToNBT(new NBTTagCompound()));
        if (this.shooterId != null) {
            compound.setUniqueId("Shooter", this.shooterId);
        }
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound compound) {
        this.age = compound.getInteger("Life");
        this.lifetime = compound.getInteger("LifeTime");
        this.dataManager.set(ROCKET, new ItemStack(compound.getCompoundTag("FireworksItem")));
        if (compound.hasUniqueId("Shooter")) {
            this.shooterId = compound.getUniqueId("Shooter");
        }
    }
}
