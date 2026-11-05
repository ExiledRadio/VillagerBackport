package com.exiledradio.villagerbackport.fox;

import com.google.common.base.Optional;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.EntityLookHelper;
import net.minecraft.entity.ai.EntityMoveHelper;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityChicken;
import net.minecraft.entity.passive.EntityRabbit;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The fox, after current Java's: a small, skittish taiga hunter that sleeps through the day.
 *
 * <h2>Day and night</h2>
 * By day a fox looks for shade - anywhere it cannot see the sky - and sleeps there, curled up, until
 * something it does not trust comes within 12 blocks (a non-sneaking player, a monster, an untamed
 * wolf, its prey), night falls, it is hurt, it gets wet or a thunderstorm starts. At night it hunts,
 * wanders and, near a village, strolls through it. Its ambient sound at night is sometimes a screech,
 * when no player is within 16 blocks.
 *
 * <h2>Hunting</h2>
 * Red foxes go after chickens and rabbits first; snow foxes put fish first, which 1.12.2 has none of as
 * mobs, so they hunt the same two. A fox stalks its prey to within 6 blocks, crouches, and once fully
 * crouched with a clear line pounces - a high leap that bites on landing within 2 blocks. Pouncing
 * into a snow layer buries its face in the snow for two seconds.
 *
 * <h2>The mouth</h2>
 * A fox carries one item in its mouth: 20% of foxes spawn with one (an emerald, egg, rabbit's foot or
 * hide, wheat, leather or a feather). It picks up any item it walks over while its mouth is empty, and
 * swaps what it holds for food - spitting the old item out - if it has not just eaten. Food in its
 * mouth is eaten after 30 seconds, 2 seconds' worth of crunching first. Whatever it holds drops when
 * it dies. Foxes walk to ripe sweet berry bushes within 12 blocks and pick them, taking a berry in the
 * mouth when it is empty and dropping the rest. Picking up items and picking bushes both need
 * {@code mobGriefing}, as in 1.14.
 *
 * <h2>Trust</h2>
 * Foxes flee non-sneaking players (16 blocks), untamed wolves and polar bears (8 blocks). Two foxes
 * bred with sweet berries have a cub that trusts the players who fed them - up to two - and it neither
 * flees those players nor wakes for them, and it attacks whatever hurts them.
 *
 * <h2>Sitting, crouching, pouncing...</h2>
 * The seven states are one byte of synced flags, as in 1.14, so the client's model can show each of
 * them without packets of its own.
 */
public class EntityFox extends EntityAnimal {

    private static final DataParameter<Integer> TYPE = EntityDataManager.createKey(EntityFox.class, DataSerializers.VARINT);
    private static final DataParameter<Byte> FLAGS = EntityDataManager.createKey(EntityFox.class, DataSerializers.BYTE);
    private static final DataParameter<Optional<UUID>> TRUSTED_0 =
            EntityDataManager.createKey(EntityFox.class, DataSerializers.OPTIONAL_UNIQUE_ID);
    private static final DataParameter<Optional<UUID>> TRUSTED_1 =
            EntityDataManager.createKey(EntityFox.class, DataSerializers.OPTIONAL_UNIQUE_ID);

    private static final int SITTING = 1;
    private static final int CROUCHING = 4;
    private static final int INTERESTED = 8;
    private static final int POUNCING = 16;
    private static final int SLEEPING = 32;
    private static final int FACEPLANTED = 64;
    private static final int DEFENDING = 128;

    /** Entity event: crumbs of the held food, while it is being eaten. */
    static final byte EATING_EVENT = 45;

    /** Red or snow: the color of the coat, and whether fish or land prey comes first. */
    public enum Type {
        RED("red"),
        SNOW("snow");

        private final String name;

        Type(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        static Type byName(String name) {
            return SNOW.name.equals(name) ? SNOW : RED;
        }

        static Type byId(int id) {
            return id == 1 ? SNOW : RED;
        }
    }

    private float interestedAngle;
    private float interestedAngleO;
    private float crouchAmount;
    private float crouchAmountO;
    private int ticksSinceEaten;

    /** Hunting chickens and rabbits; added at a priority that depends on the type. */
    private EntityAIBase landTarget;
    private boolean targetGoalsAdded;

    public EntityFox(World world) {
        super(world);
        setSize(0.6F, 0.7F);
        // The look helper is private in 1.12.2, unlike the move helper; FML maps the SRG name in a
        // released game and the MCP one in a development run.
        net.minecraftforge.fml.common.ObfuscationReflectionHelper.setPrivateValue(net.minecraft.entity.EntityLiving.class,
                this, new LookHelper(this), "field_70749_g", "lookHelper");
        moveHelper = new MoveHelper(this);
        setCanPickUpLoot(true);
    }

    // ------------------------------------------------------------------------------------------ setup

    @Override
    protected void entityInit() {
        super.entityInit();
        dataManager.register(TRUSTED_0, Optional.<UUID>absent());
        dataManager.register(TRUSTED_1, Optional.<UUID>absent());
        dataManager.register(TYPE, 0);
        dataManager.register(FLAGS, (byte) 0);
    }

    /**
     * 1.14's goals, in 1.14's order. The hunting target goal is added separately (see
     * {@link #setTargetGoals}) because its priority depends on the type, which is not known yet.
     *
     * <p>This runs from the entity constructor, before this class's own fields are initialized, so it
     * must not depend on any of them.
     */
    @Override
    protected void initEntityAI() {
        tasks.addTask(0, new FoxGoals.Float(this));
        tasks.addTask(1, new FoxGoals.Faceplant(this));
        tasks.addTask(2, new FoxGoals.Panic(this, 2.2D));
        tasks.addTask(3, new FoxGoals.Avoid<EntityPlayer>(this, EntityPlayer.class, 16.0F, 1.6D, 1.4D));
        tasks.addTask(3, new FoxGoals.Avoid<net.minecraft.entity.passive.EntityWolf>(this,
                net.minecraft.entity.passive.EntityWolf.class, 8.0F, 1.6D, 1.4D));
        tasks.addTask(3, new FoxGoals.Avoid<net.minecraft.entity.monster.EntityPolarBear>(this,
                net.minecraft.entity.monster.EntityPolarBear.class, 8.0F, 1.6D, 1.4D));
        tasks.addTask(4, new FoxGoals.StalkPrey(this));
        tasks.addTask(5, new FoxGoals.Pounce(this));
        tasks.addTask(5, new FoxGoals.Breed(this, 1.0D));
        tasks.addTask(5, new FoxGoals.SeekShelter(this, 1.25D));
        tasks.addTask(6, new FoxGoals.Bite(this, 1.2D));
        tasks.addTask(6, new FoxGoals.Sleep(this));
        tasks.addTask(7, new FoxGoals.FollowParent(this, 1.25D));
        tasks.addTask(8, new FoxGoals.StrollThroughVillage(this));
        tasks.addTask(9, new FoxGoals.EatBerries(this, 1.2D, 12));
        tasks.addTask(9, new net.minecraft.entity.ai.EntityAILeapAtTarget(this, 0.4F));
        tasks.addTask(10, new net.minecraft.entity.ai.EntityAIWanderAvoidWater(this, 1.0D));
        tasks.addTask(10, new FoxGoals.SearchForItems(this));
        tasks.addTask(11, new FoxGoals.LookAtPlayer(this, 24.0F));
        tasks.addTask(12, new FoxGoals.PerchAndSearch(this));

        targetTasks.addTask(3, new FoxGoals.DefendTrusted(this));
    }

    /**
     * Adds the hunting goal at the type's priority: before anything else for a red fox, after fish for
     * a snow fox. Never called from a goal - only from spawning, loading and the entity tick before
     * any goal runs - since changing the goal list while it is being walked deletes the entity.
     */
    private void setTargetGoals() {
        if (landTarget == null) {
            landTarget = new net.minecraft.entity.ai.EntityAINearestAttackableTarget<EntityAnimal>(this, EntityAnimal.class,
                    10, false, false, prey -> prey instanceof EntityChicken || prey instanceof EntityRabbit);
        }
        targetTasks.removeTask(landTarget);
        targetTasks.addTask(getFoxType() == Type.RED ? 4 : 6, landTarget);
        targetGoalsAdded = true;
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.3D);
        getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(10.0D);
        getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(32.0D);
        getAttributeMap().registerAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(2.0D);
    }

    @Override
    @Nullable
    public IEntityLivingData onInitialSpawn(DifficultyInstance difficulty, @Nullable IEntityLivingData data) {
        Type type = FoxContent.isSnowy(world.getBiome(new BlockPos(this))) ? Type.SNOW : Type.RED;
        boolean cub = false;
        if (data instanceof GroupData) {
            GroupData group = (GroupData) data;
            type = group.type;
            if (group.members >= 2) {
                cub = true;
            } else {
                group.members++;
            }
        } else {
            GroupData group = new GroupData(type);
            group.members++;
            data = group;
        }
        setFoxType(type);
        if (cub) {
            setGrowingAge(-24000);
        }
        setTargetGoals();
        setEquipmentBasedOnDifficulty(difficulty);
        return super.onInitialSpawn(difficulty, data);
    }

    /** A spawned group shares one type, and every fox after the first two is a cub. */
    static final class GroupData implements IEntityLivingData {
        final Type type;
        int members;

        GroupData(Type type) {
            this.type = type;
        }
    }

    /** One fox in five spawns holding something. */
    @Override
    protected void setEquipmentBasedOnDifficulty(DifficultyInstance difficulty) {
        if (rand.nextFloat() >= 0.2F) {
            return;
        }
        float roll = rand.nextFloat();
        ItemStack stack;
        if (roll < 0.05F) {
            stack = new ItemStack(Items.EMERALD);
        } else if (roll < 0.2F) {
            stack = new ItemStack(Items.EGG);
        } else if (roll < 0.4F) {
            stack = new ItemStack(rand.nextBoolean() ? Items.RABBIT_FOOT : Items.RABBIT_HIDE);
        } else if (roll < 0.6F) {
            stack = new ItemStack(Items.WHEAT);
        } else if (roll < 0.8F) {
            stack = new ItemStack(Items.LEATHER);
        } else {
            stack = new ItemStack(Items.FEATHER);
        }
        setItemStackToSlot(EntityEquipmentSlot.MAINHAND, stack);
    }

    /**
     * Current Java's ground: grass, snow, podzol, coarse dirt (and, since 1.12.2 makes them one block,
     * plain dirt), in light above 8.
     */
    @Override
    public boolean getCanSpawnHere() {
        int x = MathHelper.floor(posX);
        int y = MathHelper.floor(getEntityBoundingBox().minY);
        int z = MathHelper.floor(posZ);
        BlockPos pos = new BlockPos(x, y, z);
        Block below = world.getBlockState(pos.down()).getBlock();
        boolean ground = below == net.minecraft.init.Blocks.GRASS || below == net.minecraft.init.Blocks.SNOW
                || below == net.minecraft.init.Blocks.SNOW_LAYER || below == net.minecraft.init.Blocks.DIRT;
        return ground && world.getLight(pos) > 8 && getBlockPathWeight(pos) >= 0.0F
                && world.getBlockState(pos.down()).canEntitySpawn(this);
    }

    // -------------------------------------------------------------------------------------- ticking

    @Override
    public void onUpdate() {
        if (!world.isRemote && !targetGoalsAdded) {
            // A fox loaded or summoned without onInitialSpawn: set the goals before any of them runs.
            setTargetGoals();
        }
        super.onUpdate();

        if (!world.isRemote) {
            boolean wet = isInWater();
            if (wet || getAttackTarget() != null || world.isThundering()) {
                wakeUp();
            }
            if (wet || isSleeping()) {
                setSitting(false);
            }
            if (isFaceplanted() && world.rand.nextFloat() < 0.2F) {
                BlockPos at = new BlockPos(this);
                IBlockState state = world.getBlockState(at);
                world.playEvent(2001, at, Block.getStateId(state));
            }
        }

        interestedAngleO = interestedAngle;
        interestedAngle += ((isInterested() ? 1.0F : 0.0F) - interestedAngle) * 0.4F;
        crouchAmountO = crouchAmount;
        if (isCrouching()) {
            crouchAmount = Math.min(3.0F, crouchAmount + 0.2F);
        } else {
            crouchAmount = 0.0F;
        }
    }

    @Override
    public void onLivingUpdate() {
        if (!world.isRemote && isEntityAlive() && isServerWorld()) {
            ++ticksSinceEaten;
            ItemStack held = getHeldItemMainhand();
            if (canEat(held)) {
                if (ticksSinceEaten > 600) {
                    ItemStack left = held.onItemUseFinish(world, this);
                    if (!left.isEmpty()) {
                        setItemStackToSlot(EntityEquipmentSlot.MAINHAND, left);
                    }
                    ticksSinceEaten = 0;
                } else if (ticksSinceEaten > 560 && rand.nextFloat() < 0.1F) {
                    playSound(FoxContent.sound("eat"), 1.0F, 1.0F);
                    world.setEntityState(this, EATING_EVENT);
                }
            }
            EntityLivingBase target = getAttackTarget();
            if (target == null || !target.isEntityAlive()) {
                setCrouching(false);
                setInterested(false);
            }
        }
        if (isSleeping() || isMovementBlocked()) {
            isJumping = false;
            moveStrafing = 0.0F;
            moveForward = 0.0F;
        }
        super.onLivingUpdate();
        if (isDefending() && rand.nextFloat() < 0.05F) {
            playSound(FoxContent.sound("aggro"), 1.0F, 1.0F);
        }
    }

    private boolean canEat(ItemStack stack) {
        return stack.getItem() instanceof ItemFood && getAttackTarget() == null && onGround && !isSleeping();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void handleStatusUpdate(byte id) {
        if (id != EATING_EVENT) {
            super.handleStatusUpdate(id);
            return;
        }
        ItemStack held = getHeldItemMainhand();
        if (held.isEmpty()) {
            return;
        }
        for (int i = 0; i < 8; i++) {
            Vec3d speed = new Vec3d((rand.nextFloat() - 0.5D) * 0.1D, Math.random() * 0.1D + 0.1D, 0.0D)
                    .rotatePitch(-rotationPitch * 0.017453292F).rotateYaw(-rotationYaw * 0.017453292F);
            Vec3d look = getLookVec();
            world.spawnParticle(EnumParticleTypes.ITEM_CRACK, posX + look.x / 2.0D, posY, posZ + look.z / 2.0D,
                    speed.x, speed.y + 0.05D, speed.z, net.minecraft.item.Item.getIdFromItem(held.getItem()),
                    held.getMetadata());
        }
    }

    // ------------------------------------------------------------------------------ the mouth

    /** Whether this item is worth taking: anything when the mouth is empty, food over non-food otherwise. */
    boolean canHoldItem(ItemStack stack) {
        ItemStack held = getHeldItemMainhand();
        return held.isEmpty()
                || ticksSinceEaten > 0 && stack.getItem() instanceof ItemFood && !(held.getItem() instanceof ItemFood);
    }

    @Override
    protected void updateEquipmentIfNeeded(EntityItem itemEntity) {
        ItemStack stack = itemEntity.getItem();
        if (!canHoldItem(stack)) {
            return;
        }
        int count = stack.getCount();
        if (count > 1) {
            dropLoose(stack.splitStack(count - 1));
        }
        spitOut(getHeldItemMainhand());
        setItemStackToSlot(EntityEquipmentSlot.MAINHAND, stack.splitStack(1));
        inventoryHandsDropChances[EntityEquipmentSlot.MAINHAND.getIndex()] = 2.0F;
        onItemPickup(itemEntity, 1);
        itemEntity.setDead();
        ticksSinceEaten = 0;
    }

    /** Puts a stack from a bush straight into the mouth. */
    void takeInMouth(ItemStack stack) {
        setItemStackToSlot(EntityEquipmentSlot.MAINHAND, stack);
    }

    /** Spits the held item out in front, where it cannot be picked up again for two seconds. */
    private void spitOut(ItemStack stack) {
        if (stack.isEmpty() || world.isRemote) {
            return;
        }
        Vec3d look = getLookVec();
        EntityItem item = new EntityItem(world, posX + look.x, posY + 1.0D, posZ + look.z, stack);
        item.setPickupDelay(40);
        playSound(FoxContent.sound("spit"), 1.0F, 1.0F);
        world.spawnEntity(item);
    }

    private void dropLoose(ItemStack stack) {
        world.spawnEntity(new EntityItem(world, posX, posY, posZ, stack));
    }

    /** Whatever is in the mouth always drops on death, however it got there. */
    @Override
    protected void dropEquipment(boolean wasRecentlyHit, int lootingModifier) {
        ItemStack held = getHeldItemMainhand();
        if (!held.isEmpty()) {
            entityDropItem(held, 0.0F);
            setItemStackToSlot(EntityEquipmentSlot.MAINHAND, ItemStack.EMPTY);
        }
        super.dropEquipment(wasRecentlyHit, lootingModifier);
    }

    // ------------------------------------------------------------------------------ breeding

    @Override
    public boolean isBreedingItem(ItemStack stack) {
        return FoxContent.isBerries(stack);
    }

    @Override
    protected void consumeItemFromStack(EntityPlayer player, ItemStack stack) {
        if (isBreedingItem(stack)) {
            playSound(FoxContent.sound("eat"), 1.0F, 1.0F);
        }
        super.consumeItemFromStack(player, stack);
    }

    @Override
    public EntityFox createChild(EntityAgeable mate) {
        EntityFox cub = new EntityFox(world);
        Type type = rand.nextBoolean() || !(mate instanceof EntityFox) ? getFoxType() : ((EntityFox) mate).getFoxType();
        cub.setFoxType(type);
        cub.setTargetGoals();
        return cub;
    }

    // ------------------------------------------------------------------------------ trust

    List<UUID> getTrustedUUIDs() {
        List<UUID> list = new ArrayList<UUID>(2);
        if (dataManager.get(TRUSTED_0).isPresent()) {
            list.add(dataManager.get(TRUSTED_0).get());
        }
        if (dataManager.get(TRUSTED_1).isPresent()) {
            list.add(dataManager.get(TRUSTED_1).get());
        }
        return list;
    }

    void addTrustedUUID(@Nullable UUID uuid) {
        if (dataManager.get(TRUSTED_0).isPresent()) {
            dataManager.set(TRUSTED_1, Optional.fromNullable(uuid));
        } else {
            dataManager.set(TRUSTED_0, Optional.fromNullable(uuid));
        }
    }

    public boolean trusts(UUID uuid) {
        return getTrustedUUIDs().contains(uuid);
    }

    // ------------------------------------------------------------------------------ fighting

    @Override
    public boolean attackEntityAsMob(Entity target) {
        boolean hit = target.attackEntityFrom(DamageSource.causeMobDamage(this),
                (float) getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).getAttributeValue());
        if (hit) {
            applyEnchantments(this, target);
        }
        return hit;
    }

    /** Losing its target ends a fox's defense of its player. */
    @Override
    public void setAttackTarget(@Nullable EntityLivingBase target) {
        if (isDefending() && target == null) {
            setDefending(false);
        }
        super.setAttackTarget(target);
    }

    /** Foxes take fall damage from 5 blocks rather than 3. */
    @Override
    public void fall(float distance, float damageMultiplier) {
        super.fall(Math.max(0.0F, distance - 2.0F), damageMultiplier);
    }

    // ------------------------------------------------------------------------------ sounds

    @Override
    protected SoundEvent getAmbientSound() {
        if (isSleeping()) {
            return FoxContent.sound("sleep");
        }
        if (!world.isDaytime() && rand.nextFloat() < 0.1F
                && world.getEntitiesWithinAABB(EntityPlayer.class, getEntityBoundingBox().grow(16.0D),
                p -> p != null && !p.isSpectator()).isEmpty()) {
            return FoxContent.sound("screech");
        }
        return FoxContent.sound("ambient");
    }

    /** The screech carries: twice the volume of the other sounds. */
    @Override
    public void playLivingSound() {
        SoundEvent sound = getAmbientSound();
        if (sound == FoxContent.sound("screech")) {
            playSound(sound, 2.0F, getSoundPitch());
        } else if (sound != null) {
            playSound(sound, getSoundVolume(), getSoundPitch());
        }
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return FoxContent.sound("hurt");
    }

    @Override
    protected SoundEvent getDeathSound() {
        return FoxContent.sound("death");
    }

    @Override
    public float getEyeHeight() {
        return isChild() ? height * 0.85F : 0.4F;
    }

    // ------------------------------------------------------------------------------ saving

    @Override
    public void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        NBTTagList trusted = new NBTTagList();
        for (UUID uuid : getTrustedUUIDs()) {
            trusted.appendTag(NBTUtil.createUUIDTag(uuid));
        }
        tag.setTag("TrustedUUIDs", trusted);
        tag.setBoolean("Sleeping", isSleeping());
        tag.setString("Type", getFoxType().getName());
        tag.setBoolean("Sitting", isSitting());
        tag.setBoolean("Crouching", isCrouching());
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        NBTTagList trusted = tag.getTagList("TrustedUUIDs", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < trusted.tagCount() && i < 2; i++) {
            addTrustedUUID(NBTUtil.getUUIDFromTag(trusted.getCompoundTagAt(i)));
        }
        setSleeping(tag.getBoolean("Sleeping"));
        setFoxType(Type.byName(tag.getString("Type")));
        setSitting(tag.getBoolean("Sitting"));
        setCrouching(tag.getBoolean("Crouching"));
        setTargetGoals();
    }

    // ------------------------------------------------------------------------------ state

    public Type getFoxType() {
        return Type.byId(dataManager.get(TYPE));
    }

    void setFoxType(Type type) {
        dataManager.set(TYPE, type.ordinal());
    }

    private boolean foxFlag(int bit) {
        return (dataManager.get(FLAGS) & bit) != 0;
    }

    private void setFoxFlag(int bit, boolean on) {
        byte flags = dataManager.get(FLAGS);
        dataManager.set(FLAGS, (byte) (on ? flags | bit : flags & ~bit));
    }

    public boolean isSitting() {
        return foxFlag(SITTING);
    }

    void setSitting(boolean on) {
        setFoxFlag(SITTING, on);
    }

    public boolean isCrouching() {
        return foxFlag(CROUCHING);
    }

    void setCrouching(boolean on) {
        setFoxFlag(CROUCHING, on);
    }

    public boolean isInterested() {
        return foxFlag(INTERESTED);
    }

    void setInterested(boolean on) {
        setFoxFlag(INTERESTED, on);
    }

    public boolean isPouncing() {
        return foxFlag(POUNCING);
    }

    void setPouncing(boolean on) {
        setFoxFlag(POUNCING, on);
    }

    public boolean isSleeping() {
        return foxFlag(SLEEPING);
    }

    void setSleeping(boolean on) {
        setFoxFlag(SLEEPING, on);
    }

    public boolean isFaceplanted() {
        return foxFlag(FACEPLANTED);
    }

    void setFaceplanted(boolean on) {
        setFoxFlag(FACEPLANTED, on);
    }

    boolean isDefending() {
        return foxFlag(DEFENDING);
    }

    void setDefending(boolean on) {
        setFoxFlag(DEFENDING, on);
    }

    void wakeUp() {
        setSleeping(false);
    }

    /** Back to standing: every state off, as when a goal that used one ends. */
    void clearStates() {
        setInterested(false);
        setCrouching(false);
        setSitting(false);
        setSleeping(false);
        setDefending(false);
        setFaceplanted(false);
    }

    /** A sleeping, sitting or face-planted fox stays where it is. */
    boolean canMove() {
        return !isSleeping() && !isSitting() && !isFaceplanted();
    }

    boolean isFullyCrouched() {
        return crouchAmount == 3.0F;
    }

    boolean jumping() {
        return isJumping;
    }

    void resetCrouch() {
        crouchAmount = 0.0F;
        crouchAmountO = 0.0F;
    }

    // -------------------------------------------------------------------------- for the model

    /** The head's tilt while interested, in radians. */
    public float getHeadRollAngle(float partialTicks) {
        return (interestedAngleO + (interestedAngle - interestedAngleO) * partialTicks) * 0.11F * (float) Math.PI;
    }

    public float getCrouchAmount(float partialTicks) {
        return crouchAmountO + (crouchAmount - crouchAmountO) * partialTicks;
    }

    // -------------------------------------------------------------------------- helpers

    /** 1.14's look control: none while asleep; a pounce keeps its own pitch. */
    static final class LookHelper extends EntityLookHelper {
        private final EntityFox fox;

        LookHelper(EntityFox fox) {
            super(fox);
            this.fox = fox;
        }

        @Override
        public void onUpdateLook() {
            if (fox.isSleeping()) {
                return;
            }
            float pitch = fox.rotationPitch;
            super.onUpdateLook();
            if ((fox.isPouncing() || fox.isFaceplanted()) && !getIsLooking()) {
                fox.rotationPitch = pitch;
            }
        }
    }

    /** 1.14's move control: a fox that cannot move does not. */
    static final class MoveHelper extends EntityMoveHelper {
        private final EntityFox fox;

        MoveHelper(EntityFox fox) {
            super(fox);
            this.fox = fox;
        }

        @Override
        public void onUpdateMoveHelper() {
            if (fox.canMove()) {
                super.onUpdateMoveHelper();
            }
        }
    }

    /**
     * Whether nothing solid stands between this fox and its prey, checked at six points along the way
     * and three blocks up at each - the room a pounce's arc needs.
     */
    static boolean isPathClear(EntityFox fox, EntityLivingBase target) {
        double dz = target.posZ - fox.posZ;
        double dx = target.posX - fox.posX;
        double ratio = dz / dx;
        for (int i = 0; i < 6; i++) {
            double stepZ = ratio == 0.0D ? 0.0D : dz * (double) ((float) i / 6.0F);
            double stepX = ratio == 0.0D ? dx * (double) ((float) i / 6.0F) : stepZ / ratio;
            for (int up = 1; up < 4; up++) {
                BlockPos at = new BlockPos(fox.posX + stepX, fox.posY + up, fox.posZ + stepZ);
                if (!fox.world.getBlockState(at).getMaterial().isReplaceable()) {
                    return false;
                }
            }
        }
        return true;
    }
}
