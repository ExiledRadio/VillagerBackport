package com.exiledradio.villagerbackport.illager;

import com.exiledradio.villagerbackport.block.ModSounds;
import com.exiledradio.villagerbackport.crossbow.CrossbowContent;
import com.exiledradio.villagerbackport.crossbow.ItemCrossbow;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.EnumCreatureAttribute;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.IRangedAttackMob;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.ai.EntityAISwimming;
import net.minecraft.entity.ai.EntityAIWanderAvoidWater;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.monster.AbstractIllager;
import net.minecraft.entity.monster.EntityIronGolem;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.entity.projectile.EntityTippedArrow;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.init.Items;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;

/**
 * 1.14's pillager: an illager with a crossbow.
 *
 * <p>24 health, 1.14's speed and senses, and its crossbow attack: walk in to eight blocks, load for
 * a second and a quarter, wait one to two seconds aimed, fire, repeat. On the later waves of a
 * strong raid it may carry a multishot crossbow (three arrows at once) that loads faster, as in
 * 1.14. With this mod's crossbow those are its real enchantments, and so is anything else the
 * crossbow has (Piercing, from a difficult spawn); another mod's crossbow knows nothing of this mod's
 * enchantments, so for one of those the raid's are kept on the pillager itself.
 */
public class EntityPillager extends AbstractIllager implements IRangedAttackMob, Celebrates {

    private static final DataParameter<Boolean> CHARGING = EntityDataManager.createKey(EntityPillager.class, DataSerializers.BOOLEAN);

    /** 1.14's {@code CrossbowItem.getChargeTime}: 25 ticks, five fewer per Quick Charge level. */
    private static final int BASE_CHARGE_TICKS = 25;

    private int quickCharge;
    private boolean multishot;

    /** Client side only: how long the current load has gone on, for the arm pose. */
    private int clientChargeTicks;

    /** 1.14's {@code PatrollerEntity}: where the patrol is headed, and who leads it. */
    @javax.annotation.Nullable
    private net.minecraft.util.math.BlockPos patrolTarget;
    private boolean patrolLeader;
    private boolean patrolling;

    public EntityPillager(World world) {
        super(world);
        setSize(0.6F, 1.95F);
    }

    @Override
    protected void initEntityAI() {
        super.initEntityAI();
        this.tasks.addTask(0, new EntityAISwimming(this));
        this.tasks.addTask(3, new EntityAICrossbowAttack(this, 1.0D, 8.0F));
        this.tasks.addTask(4, new EntityAIPatrol(this, 0.7D, 0.595D));
        this.tasks.addTask(8, new EntityAIWanderAvoidWater(this, 0.6D));
        this.tasks.addTask(9, new EntityAIWatchClosest(this, EntityPlayer.class, 15.0F, 1.0F));
        this.tasks.addTask(10, new EntityAIWatchClosest(this, EntityLiving.class, 15.0F));
        this.targetTasks.addTask(1, new EntityAIRaiderHurtByTarget(this));
        this.targetTasks.addTask(2, new EntityAINearestAttackableTarget<EntityPlayer>(this, EntityPlayer.class, true));
        this.targetTasks.addTask(3, new EntityAINearestAttackableTarget<EntityVillager>(this, EntityVillager.class, false));
        this.targetTasks.addTask(3, new EntityAINearestAttackableTarget<EntityIronGolem>(this, EntityIronGolem.class, true));
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.35D);
        getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(24.0D);
        getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(5.0D);
        getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(32.0D);
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        this.dataManager.register(CHARGING, false);
    }

    public boolean isCharging() {
        return this.dataManager.get(CHARGING);
    }

    void setCharging(boolean charging) {
        this.dataManager.set(CHARGING, charging);
    }

    /** Ticks a load takes, with 1.14's Quick Charge. */
    int chargeTicks() {
        int quick = Math.max(this.quickCharge, CrossbowContent.level(CrossbowContent.quickCharge, getHeldItemMainhand()));
        return Math.max(0, BASE_CHARGE_TICKS - 5 * quick);
    }

    private boolean multishot() {
        return this.multishot || CrossbowContent.level(CrossbowContent.multishot, getHeldItemMainhand()) > 0;
    }

    /** @return how far into a load the pillager is, in ticks - what 1.14's arm pose follows */
    @SideOnly(Side.CLIENT)
    public int clientChargeTicks() {
        return this.clientChargeTicks;
    }

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (this.world.isRemote) {
            this.clientChargeTicks = isCharging() ? this.clientChargeTicks + 1 : 0;
        }
    }

    public boolean isHoldingCrossbow() {
        return Crossbows.isCrossbow(getHeldItemMainhand());
    }

    @Override
    @SideOnly(Side.CLIENT)
    public AbstractIllager.IllagerArmPose getArmPose() {
        // The crossbow poses are drawn by this mod's model from isCharging and isHoldingCrossbow;
        // vanilla's enum has no crossbow entries, so it is told only what it understands.
        return isAggressive(1) ? AbstractIllager.IllagerArmPose.ATTACKING : AbstractIllager.IllagerArmPose.CROSSED;
    }

    @Override
    public void setSwingingArms(boolean swingingArms) {
        setAggressive(1, swingingArms);
    }

    @Nullable
    @Override
    public IEntityLivingData onInitialSpawn(DifficultyInstance difficulty, @Nullable IEntityLivingData data) {
        IEntityLivingData result = super.onInitialSpawn(difficulty, data);
        setEquipmentBasedOnDifficulty(difficulty);
        setEnchantmentBasedOnDifficulty(difficulty);
        return result;
    }

    @Override
    protected void setEquipmentBasedOnDifficulty(DifficultyInstance difficulty) {
        ItemStack crossbow = Crossbows.newCrossbow();
        setItemStackToSlot(EntityEquipmentSlot.MAINHAND, crossbow);
        // This mod's own crossbow is the pillager's alone: it never drops.
        setDropChance(EntityEquipmentSlot.MAINHAND, Crossbows.isOurs(crossbow) ? 0.0F : 0.085F);
    }

    /**
     * 1.14's raid gear: on a strong raid, a chance of a multishot crossbow that loads faster on the
     * later waves.
     */
    public void applyRaidBonus(int wave, int easyWaves, int normalWaves, float enchantOdds) {
        if (this.rand.nextFloat() > enchantOdds) {
            return;
        }
        int quick = wave > normalWaves ? 2 : wave > easyWaves ? 1 : 0;
        ItemStack held = getHeldItemMainhand();
        if (CrossbowContent.crossbow != null && held.getItem() == CrossbowContent.crossbow) {
            java.util.Map<net.minecraft.enchantment.Enchantment, Integer> enchants = EnchantmentHelper.getEnchantments(held);
            enchants.put(CrossbowContent.multishot, 1);
            enchants.remove(CrossbowContent.piercing);
            if (quick > 0) {
                enchants.put(CrossbowContent.quickCharge, quick);
            }
            EnchantmentHelper.setEnchantments(enchants, held);
            return;
        }
        this.multishot = true;
        this.quickCharge = quick;
    }

    @Override
    public boolean isOnSameTeam(Entity other) {
        if (super.isOnSameTeam(other)) {
            return true;
        }
        return other instanceof EntityLivingBase
                && ((EntityLivingBase) other).getCreatureAttribute() == EnumCreatureAttribute.ILLAGER
                && getTeam() == null && other.getTeam() == null;
    }

    /**
     * Fires: one bolt, or three spread ten degrees apart with multishot, at 1.14's speed and with its
     * spread, which tightens with difficulty.
     */
    @Override
    public void attackEntityWithRangedAttack(EntityLivingBase target, float distanceFactor) {
        if (!isHoldingCrossbow()) {
            return;
        }

        float inaccuracy = 14 - this.world.getDifficulty().getId() * 4;
        float[] angles = multishot() ? new float[]{0.0F, -10.0F, 10.0F} : new float[]{0.0F};
        for (float angle : angles) {
            shoot(target, angle, inaccuracy);
        }

        playSound(ModSounds.crossbowShoot, 1.0F, 1.0F / (getRNG().nextFloat() * 0.4F + 0.8F));
        this.idleTime = 0;
    }

    private void shoot(EntityLivingBase target, float angle, float inaccuracy) {
        // This mod's crossbow makes the arrow, so its Piercing goes with it.
        EntityArrow arrow = CrossbowContent.crossbow != null && getHeldItemMainhand().getItem() == CrossbowContent.crossbow
                ? ItemCrossbow.arrow(this.world, this, getHeldItemMainhand(), new ItemStack(Items.ARROW))
                : new EntityTippedArrow(this.world, this);
        arrow.setIsCritical(true);

        double dx = target.posX - this.posX;
        double dz = target.posZ - this.posZ;
        double horizontal = MathHelper.sqrt(dx * dx + dz * dz);
        double dy = target.getEntityBoundingBox().minY + target.height / 3.0F - arrow.posY + horizontal * 0.2D;

        // Multishot's side bolts, turned about the vertical as 1.14's crossbow spreads them.
        double radians = Math.toRadians(angle);
        double rx = dx * Math.cos(radians) - dz * Math.sin(radians);
        double rz = dx * Math.sin(radians) + dz * Math.cos(radians);

        arrow.shoot(rx, dy, rz, 1.6F, inaccuracy);
        if (angle != 0.0F) {
            arrow.pickupStatus = EntityArrow.PickupStatus.CREATIVE_ONLY;
        }
        this.world.spawnEntity(arrow);
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound compound) {
        super.writeEntityToNBT(compound);
        compound.setBoolean("Multishot", this.multishot);
        compound.setInteger("QuickCharge", this.quickCharge);
        if (this.patrolTarget != null) {
            compound.setTag("PatrolTarget", net.minecraft.nbt.NBTUtil.createPosTag(this.patrolTarget));
        }
        compound.setBoolean("PatrolLeader", this.patrolLeader);
        compound.setBoolean("Patrolling", this.patrolling);
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound compound) {
        super.readEntityFromNBT(compound);
        this.multishot = compound.getBoolean("Multishot");
        this.quickCharge = compound.getInteger("QuickCharge");
        if (compound.hasKey("PatrolTarget")) {
            this.patrolTarget = net.minecraft.nbt.NBTUtil.getPosFromTag(compound.getCompoundTag("PatrolTarget"));
        }
        this.patrolLeader = compound.getBoolean("PatrolLeader");
        this.patrolling = compound.getBoolean("Patrolling");
    }

    // --- patrols: 1.14's PatrollerEntity -------------------------------------------------------------

    /** 1.14 seats every raider this much lower than its feet, so it sits on what it rides. */
    @Override
    public double getYOffset() {
        return -0.45D;
    }

    /**
     * Makes this pillager a patrol's captain: the ominous banner on its head, always dropped, and a
     * destination somewhere within 500 blocks.
     */
    public void becomePatrolLeader() {
        this.patrolLeader = true;
        this.patrolling = true;
        setItemStackToSlot(EntityEquipmentSlot.HEAD, com.exiledradio.villagerbackport.raid.OminousBanner.create());
        setDropChance(EntityEquipmentSlot.HEAD, 2.0F);
        resetPatrolTarget();
    }

    public void joinPatrol() {
        this.patrolling = true;
    }

    public boolean isPatrolLeader() {
        return this.patrolLeader;
    }

    boolean isPatrolling() {
        return this.patrolling;
    }

    @javax.annotation.Nullable
    net.minecraft.util.math.BlockPos patrolTarget() {
        return this.patrolTarget;
    }

    void setPatrolTarget(net.minecraft.util.math.BlockPos target) {
        this.patrolTarget = target;
        this.patrolling = true;
    }

    /** 1.14's next leg: anywhere within 500 blocks either way. */
    void resetPatrolTarget() {
        this.patrolTarget = new net.minecraft.util.math.BlockPos(this).add(-500 + this.rand.nextInt(1000), 0,
                -500 + this.rand.nextInt(1000));
        this.patrolling = true;
    }

    /**
     * A patrol stays together as long as anyone is near it: 1.14 only lets a patrolling pillager go
     * once no player is within 128 blocks. Others despawn as any monster does.
     */
    @Override
    protected void despawnEntity() {
        if (!this.patrolling) {
            super.despawnEntity();
            return;
        }
        net.minecraftforge.fml.common.eventhandler.Event.Result result = net.minecraftforge.event.ForgeEventFactory.canEntityDespawn(this);
        if (result == net.minecraftforge.fml.common.eventhandler.Event.Result.DENY) {
            this.idleTime = 0;
            return;
        }
        if (this.world.getClosestPlayerToEntity(this, 128.0D) == null) {
            setDead();
        }
    }

    /**
     * 1.14's pillager spawn rule: not under bright artificial light, on something solid, not on
     * peaceful - and unlike other monsters, in daylight too, which is why an outpost is always manned.
     */
    @Override
    public boolean getCanSpawnHere() {
        net.minecraft.util.math.BlockPos pos = new net.minecraft.util.math.BlockPos(this);
        net.minecraft.block.state.IBlockState below = this.world.getBlockState(pos.down());
        return this.world.getDifficulty() != net.minecraft.world.EnumDifficulty.PEACEFUL
                && this.world.getLightFor(net.minecraft.world.EnumSkyBlock.BLOCK, pos) <= 8
                && below.canEntitySpawn(this);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.pillagerAmbient;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.pillagerHurt;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.pillagerDeath;
    }

    /** 1.14's pillager drops nothing of its own; its crossbow and banner are equipment. */
    @Nullable
    @Override
    protected ResourceLocation getLootTable() {
        return null;
    }

    @Override
    public int getMaxSpawnedInChunk() {
        return 1;
    }

    @Override
    public SoundEvent celebrateSound() {
        return ModSounds.pillagerCelebrate;
    }
}
