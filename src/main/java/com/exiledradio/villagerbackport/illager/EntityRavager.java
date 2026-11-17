package com.exiledradio.villagerbackport.illager;

import com.exiledradio.villagerbackport.block.ModSounds;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLeaves;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIAttackMelee;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.ai.EntityAISwimming;
import net.minecraft.entity.ai.EntityAIWanderAvoidWater;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.monster.AbstractIllager;
import net.minecraft.entity.monster.EntityIronGolem;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.pathfinding.PathNavigate;
import net.minecraft.pathfinding.PathNavigateGround;
import net.minecraft.pathfinding.PathNodeType;
import net.minecraft.pathfinding.PathFinder;
import net.minecraft.pathfinding.WalkNodeProcessor;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;

/**
 * 1.14's ravager.
 *
 * <p>100 health, a 12-damage bite that throws its target back, and 1.14's shield rule: a bite
 * blocked by a shield stuns it half the time - it stands dazed for two seconds, then roars,
 * hurting and scattering everything within four blocks that is not an illager. It walks through
 * leaves as if they were not there and tears them down when they are in the way. Illagers ride it
 * on the later waves of a raid.
 */
public class EntityRavager extends EntityMob implements Celebrates {

    /** Client-visible states, sent as entity events the way 1.14 does. */
    private static final byte EVENT_ATTACK = 4;
    private static final byte EVENT_STUNNED = 39;

    private int attackTick;
    private int stunTick;
    private int roarTick;

    public EntityRavager(World world) {
        super(world);
        setSize(1.95F, 2.2F);
        this.stepHeight = 1.0F;
        this.experienceValue = 20;
    }

    @Override
    protected void initEntityAI() {
        this.tasks.addTask(0, new EntityAISwimming(this));
        this.tasks.addTask(4, new AttackGoal(this));
        this.tasks.addTask(5, new EntityAIWanderAvoidWater(this, 0.4D));
        this.tasks.addTask(6, new EntityAIWatchClosest(this, EntityPlayer.class, 6.0F));
        this.tasks.addTask(10, new EntityAIWatchClosest(this, EntityLiving.class, 8.0F));
        this.targetTasks.addTask(2, new EntityAIRaiderHurtByTarget(this));
        this.targetTasks.addTask(3, new EntityAINearestAttackableTarget<EntityPlayer>(this, EntityPlayer.class, true));
        // 1.18-pre8: ravagers leave baby villagers alone (RaidUtils' predicate in today's Ravager); 1.14's invisibility.
        this.targetTasks.addTask(4, new EntityAINearestAttackableTarget<EntityVillager>(this, EntityVillager.class, 10, true, false,
                v -> com.exiledradio.villagerbackport.mechanics.Targeting.sparesBaby(v)
                        && com.exiledradio.villagerbackport.mechanics.Targeting.noticed(this, v)));
        this.targetTasks.addTask(4, new EntityAINearestAttackableTarget<EntityIronGolem>(this, EntityIronGolem.class, true));
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(100.0D);
        getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.3D);
        getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE).setBaseValue(0.5D);
        getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(12.0D);
        getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(32.0D);
    }

    @Override
    protected PathNavigate createNavigator(World world) {
        return new Navigator(this, world);
    }

    @Override
    public int getHorizontalFaceSpeed() {
        return 45;
    }

    @Override
    public double getMountedYOffset() {
        return 2.1D;
    }

    /**
     * Seats a rider on the ravager's back rather than standing on it.
     *
     * <p>1.14 lowers every raider 0.45 below its feet when it rides ({@code PatrollerEntity.getYOffset}).
     * This mod's pillager does that itself; 1.12.2's own vindicators and evokers are lowered here.
     */
    @Override
    public void updatePassenger(Entity passenger) {
        if (isPassenger(passenger)) {
            double seat = passenger instanceof AbstractIllager && !(passenger instanceof EntityPillager) ? SEATED_DROP : 0.0D;
            passenger.setPosition(this.posX, this.posY + getMountedYOffset() + passenger.getYOffset() - seat, this.posZ);
        }
    }

    private static final double SEATED_DROP = 0.45D;

    @Nullable
    @Override
    public Entity getControllingPassenger() {
        return getPassengers().isEmpty() ? null : getPassengers().get(0);
    }

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (!isEntityAlive()) {
            return;
        }

        // 1.14 stops it dead while it bites, reels or roars, and eases it up to a charge otherwise.
        net.minecraft.entity.ai.attributes.IAttributeInstance speed = getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED);
        if (isMovementBlocked()) {
            speed.setBaseValue(0.0D);
        } else {
            double wanted = getAttackTarget() != null ? 0.35D : 0.3D;
            speed.setBaseValue(speed.getBaseValue() + (wanted - speed.getBaseValue()) * 0.1D);
        }

        if (this.collidedHorizontally && !this.world.isRemote && ForgeEventFactory.getMobGriefingEvent(this.world, this)) {
            boolean broke = false;
            AxisAlignedBB box = getEntityBoundingBox().grow(0.2D);
            for (BlockPos pos : BlockPos.getAllInBoxMutable(MathHelper.floor(box.minX), MathHelper.floor(box.minY),
                    MathHelper.floor(box.minZ), MathHelper.floor(box.maxX), MathHelper.floor(box.maxY), MathHelper.floor(box.maxZ))) {
                IBlockState state = this.world.getBlockState(pos);
                if (state.getBlock() instanceof BlockLeaves) {
                    broke = this.world.destroyBlock(pos, true) || broke;
                }
            }
            if (!broke && this.onGround) {
                jump();
            }
        }

        if (this.roarTick > 0 && --this.roarTick == 10) {
            roar();
        }
        if (this.attackTick > 0) {
            this.attackTick--;
        }
        if (this.stunTick > 0) {
            this.stunTick--;
            stunParticles();
            if (this.stunTick == 0) {
                playSound(ModSounds.ravagerRoar, 1.0F, 1.0F);
                this.roarTick = 20;
            }
        }
    }

    private void stunParticles() {
        if (this.world.isRemote && this.rand.nextInt(6) == 0) {
            double x = this.posX - this.width * Math.sin(this.renderYawOffset * 0.017453292F) + (this.rand.nextDouble() * 0.6D - 0.3D);
            double y = this.posY + this.height - 0.3D;
            double z = this.posZ + this.width * Math.cos(this.renderYawOffset * 0.017453292F) + (this.rand.nextDouble() * 0.6D - 0.3D);
            this.world.spawnParticle(EnumParticleTypes.SPELL_MOB, x, y, z, 0.498D, 0.514D, 0.573D);
        }
    }

    @Override
    protected boolean isMovementBlocked() {
        return super.isMovementBlocked() || this.attackTick > 0 || this.stunTick > 0 || this.roarTick > 0;
    }

    @Override
    public boolean canEntityBeSeen(Entity entity) {
        return this.stunTick <= 0 && this.roarTick <= 0 && super.canEntityBeSeen(entity);
    }

    /** The roar: 6 damage to everything close that is not an illager, and everything close thrown back. */
    private void roar() {
        if (!isEntityAlive()) {
            return;
        }
        if (!this.world.isRemote) {
            for (EntityLivingBase entity : this.world.getEntitiesWithinAABB(EntityLivingBase.class,
                    getEntityBoundingBox().grow(4.0D), e -> e != null && e.isEntityAlive() && !(e instanceof EntityRavager))) {
                if (!(entity instanceof AbstractIllager)) {
                    entity.attackEntityFrom(DamageSource.causeMobDamage(this), 6.0F);
                }
                launch(entity);
            }
        } else {
            Vec3d centre = new Vec3d((getEntityBoundingBox().minX + getEntityBoundingBox().maxX) / 2.0D,
                    (getEntityBoundingBox().minY + getEntityBoundingBox().maxY) / 2.0D,
                    (getEntityBoundingBox().minZ + getEntityBoundingBox().maxZ) / 2.0D);
            for (int i = 0; i < 40; i++) {
                this.world.spawnParticle(EnumParticleTypes.EXPLOSION_NORMAL, centre.x, centre.y, centre.z,
                        this.rand.nextGaussian() * 0.2D, this.rand.nextGaussian() * 0.2D, this.rand.nextGaussian() * 0.2D);
            }
        }
    }

    /**
     * Throws an entity away from the ravager: 1.14's shove, four over the distance, so the closest
     * fly furthest.
     *
     * <p>Capped. 1.14 divides by the squared distance with almost no floor, so anything standing
     * nearly inside the ravager - an illager pressed against its flank - was thrown at hundreds of
     * blocks a tick. The cap is what 1.14 gives something two blocks away; everything further keeps
     * 1.14's push exactly. Its own rider is never thrown.
     */
    private void launch(Entity entity) {
        if (entity.getRidingEntity() == this) {
            return;
        }
        double dx = entity.posX - this.posX;
        double dz = entity.posZ - this.posZ;
        double distance = Math.sqrt(dx * dx + dz * dz);
        if (distance < 1.0E-4D) {
            return;
        }
        double speed = Math.min(4.0D / distance, MAX_LAUNCH);
        entity.addVelocity(dx / distance * speed, 0.2D, dz / distance * speed);
        entity.velocityChanged = true;
    }

    /** 1.14's push at two blocks. */
    private static final double MAX_LAUNCH = 2.0D;

    /**
     * The bite. A target holding up a shield towards it either stuns it or is thrown, as 1.14's
     * {@code blockedByShield} - 1.12.2 has no hook for the moment a shield blocks, so the check is
     * made here, the same way the shield decides.
     */
    @Override
    public boolean attackEntityAsMob(Entity target) {
        this.attackTick = 10;
        this.world.setEntityState(this, EVENT_ATTACK);
        playSound(ModSounds.ravagerAttack, 1.0F, 1.0F);

        if (target instanceof EntityLivingBase && blocksWithShield((EntityLivingBase) target) && this.roarTick == 0) {
            if (this.rand.nextDouble() < 0.5D) {
                this.stunTick = 40;
                playSound(ModSounds.ravagerStunned, 1.0F, 1.0F);
                this.world.setEntityState(this, EVENT_STUNNED);
                target.applyEntityCollision(this);
            } else {
                launch(target);
            }
        }

        boolean hit = super.attackEntityAsMob(target);
        if (hit && target instanceof EntityLivingBase) {
            // 1.14's attack knockback of 1.5, which 1.12.2 has no attribute for.
            ((EntityLivingBase) target).knockBack(this, 0.75F, MathHelper.sin(this.rotationYaw * 0.017453292F),
                    -MathHelper.cos(this.rotationYaw * 0.017453292F));
        }
        return hit;
    }

    /** The shield's own test: raised, and facing the attacker. */
    private boolean blocksWithShield(EntityLivingBase target) {
        if (!target.isActiveItemStackBlocking()) {
            return false;
        }
        Vec3d look = target.getLook(1.0F);
        Vec3d towards = new Vec3d(this.posX - target.posX, 0.0D, this.posZ - target.posZ).normalize();
        return towards.dotProduct(new Vec3d(look.x, 0.0D, look.z)) > 0.0D;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void handleStatusUpdate(byte id) {
        if (id == EVENT_ATTACK) {
            this.attackTick = 10;
        } else if (id == EVENT_STUNNED) {
            this.stunTick = 40;
        } else {
            super.handleStatusUpdate(id);
        }
    }

    public int attackTick() {
        return this.attackTick;
    }

    public int stunTick() {
        return this.stunTick;
    }

    public int roarTick() {
        return this.roarTick;
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound compound) {
        super.writeEntityToNBT(compound);
        compound.setInteger("AttackTick", this.attackTick);
        compound.setInteger("StunTick", this.stunTick);
        compound.setInteger("RoarTick", this.roarTick);
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound compound) {
        super.readEntityFromNBT(compound);
        this.attackTick = compound.getInteger("AttackTick");
        this.stunTick = compound.getInteger("StunTick");
        this.roarTick = compound.getInteger("RoarTick");
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.ravagerAmbient;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.ravagerHurt;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.ravagerDeath;
    }

    @Override
    protected void playStepSound(BlockPos pos, Block block) {
        playSound(ModSounds.ravagerStep, 0.15F, 1.0F);
    }

    @Override
    public SoundEvent celebrateSound() {
        return ModSounds.ravagerCelebrate;
    }

    /** 1.14's ravager drops a saddle. */
    @Override
    protected void dropFewItems(boolean wasRecentlyHit, int looting) {
        dropItem(Items.SADDLE, 1);
    }

    @Override
    public boolean isNotColliding() {
        return !this.world.containsAnyLiquid(getEntityBoundingBox()) && super.isNotColliding();
    }

    /** 1.14's attack reach, which its width makes far longer than an ordinary mob's. */
    static final class AttackGoal extends EntityAIAttackMelee {
        private final EntityRavager ravager;

        AttackGoal(EntityRavager ravager) {
            super(ravager, 1.0D, true);
            this.ravager = ravager;
        }

        @Override
        protected double getAttackReachSqr(EntityLivingBase target) {
            float f = this.ravager.width - 0.1F;
            return f * 2.0F * f * 2.0F + target.width;
        }
    }

    /** Leaves are open ground to a ravager. */
    static final class Navigator extends PathNavigateGround {
        Navigator(EntityLiving entity, World world) {
            super(entity, world);
        }

        @Override
        protected PathFinder getPathFinder() {
            this.nodeProcessor = new Processor();
            this.nodeProcessor.setCanEnterDoors(true);
            return new PathFinder(this.nodeProcessor);
        }
    }

    static final class Processor extends WalkNodeProcessor {
        @Override
        public PathNodeType getPathNodeType(IBlockAccess world, int x, int y, int z) {
            // 1.12.2 has no leaves node type - leaves are just solid - so they are looked for.
            if (world.getBlockState(new BlockPos(x, y, z)).getBlock() instanceof BlockLeaves) {
                return PathNodeType.OPEN;
            }
            return super.getPathNodeType(world, x, y, z);
        }
    }
}
