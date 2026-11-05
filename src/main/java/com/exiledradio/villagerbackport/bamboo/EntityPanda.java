package com.exiledradio.villagerbackport.bamboo;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIAttackMelee;
import net.minecraft.entity.ai.EntityAIAvoidEntity;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.EntityAIFollowParent;
import net.minecraft.entity.ai.EntityAIHurtByTarget;
import net.minecraft.entity.ai.EntityAILookIdle;
import net.minecraft.entity.ai.EntityAIMate;
import net.minecraft.entity.ai.EntityAIPanic;
import net.minecraft.entity.ai.EntityAISwimming;
import net.minecraft.entity.ai.EntityAITempt;
import net.minecraft.entity.ai.EntityAIWanderAvoidWater;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.ai.EntityMoveHelper;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemMonsterPlacer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * 1.14's panda, behavior for behavior, from its bytecode.
 *
 * <h2>Personality</h2>
 * Two genes ({@link PandaGene}) make one of seven personalities, which change a lot:
 * <ul>
 * <li><b>Lazy</b> pandas move at half speed, lie on their backs, and sit longer.</li>
 * <li><b>Worried</b> pandas keep away from players (8 blocks) and monsters (4), and cower in a
 * thunderstorm, unable to be fed or to do anything else.</li>
 * <li><b>Playful</b> pandas roll over far more often - about every three seconds when free.</li>
 * <li><b>Aggressive</b> pandas keep attacking whatever hit them, and join in when another panda is
 * hit. Every other panda bites back once and lets it go.</li>
 * <li><b>Weak</b> pandas have half the health (10) and their cubs sneeze far more often.</li>
 * <li><b>Brown</b> pandas are only a color.</li>
 * </ul>
 *
 * <h2>Genes</h2>
 * A wild panda draws both genes at random. A cub takes one gene from each parent - from each parent
 * one of its two, at random - with a 1 in 32 chance that either is replaced by a random one.
 *
 * <h2>Food and breeding</h2>
 * Bamboo is food; cake too, but only if they find it. Pandas pick up bamboo and cake lying within
 * six blocks, sit down and eat it. Handed bamboo, an adult goes into love mode, and a cub grows a
 * tenth of the way up. Breeding needs bamboo growing near the pair (within 7 blocks across, 2 up):
 * without it a panda trying to breed shakes its head, bleats at the nearest player and gives up for
 * thirty seconds. 1.14 also sets that player as its target; here it only looks at them, which is the
 * behavior the wiki describes - see {@link BreedGoal}.
 *
 * <h2>Cubs</h2>
 * Cubs roll and sneeze. A sneeze knocks every adult panda within 10 blocks into a jump and, one time
 * in 700, leaves a slime ball.
 */
public class EntityPanda extends EntityAnimal {

    private static final DataParameter<Integer> UNHAPPY_COUNTER =
            EntityDataManager.createKey(EntityPanda.class, DataSerializers.VARINT);
    private static final DataParameter<Integer> SNEEZE_COUNTER =
            EntityDataManager.createKey(EntityPanda.class, DataSerializers.VARINT);
    private static final DataParameter<Integer> EAT_COUNTER =
            EntityDataManager.createKey(EntityPanda.class, DataSerializers.VARINT);
    private static final DataParameter<Byte> MAIN_GENE =
            EntityDataManager.createKey(EntityPanda.class, DataSerializers.BYTE);
    private static final DataParameter<Byte> HIDDEN_GENE =
            EntityDataManager.createKey(EntityPanda.class, DataSerializers.BYTE);
    private static final DataParameter<Byte> FLAGS =
            EntityDataManager.createKey(EntityPanda.class, DataSerializers.BYTE);

    private static final int FLAG_SNEEZING = 2;
    private static final int FLAG_ROLLING = 4;
    private static final int FLAG_SITTING = 8;
    private static final int FLAG_ON_BACK = 16;

    /** Bamboo or cake lying on the ground, ready to be picked up. */
    static final Predicate<EntityItem> PANDA_ITEMS = item -> {
        ItemStack stack = item.getItem();
        return (BambooContent.isBamboo(stack) || stack.getItem() == Items.CAKE)
                && item.isEntityAlive() && !item.cannotPickup();
    };

    /** Set when the panda's attacker fed it bamboo: it forgives them. */
    private boolean gotBamboo;
    /** Set when a panda that is not aggressive has bitten back once. */
    private boolean didBite;
    /** Ticks into the current roll, 0 when not rolling. Read by the renderer. */
    public int rollCounter;
    private Vec3d rollDelta = Vec3d.ZERO;
    /** Who an unhappy panda is glaring at. */
    @Nullable
    private EntityPlayer unhappyAt;

    private float sitAmount;
    private float sitAmountO;
    private float onBackAmount;
    private float onBackAmountO;
    private float rollAmount;
    private float rollAmountO;

    public EntityPanda(World world) {
        super(world);
        setSize(1.3F, 1.25F);
        this.moveHelper = new MoveHelper(this);
        setCanPickUpLoot(true);
    }

    // ---- synced state -----------------------------------------------------------------------

    @Override
    protected void entityInit() {
        super.entityInit();
        dataManager.register(UNHAPPY_COUNTER, 0);
        dataManager.register(SNEEZE_COUNTER, 0);
        dataManager.register(MAIN_GENE, (byte) 0);
        dataManager.register(HIDDEN_GENE, (byte) 0);
        dataManager.register(FLAGS, (byte) 0);
        dataManager.register(EAT_COUNTER, 0);
    }

    private boolean getPandaFlag(int flag) {
        return (dataManager.get(FLAGS) & flag) != 0;
    }

    private void setPandaFlag(int flag, boolean on) {
        byte flags = dataManager.get(FLAGS);
        dataManager.set(FLAGS, (byte) (on ? flags | flag : flags & ~flag));
    }

    public int getUnhappyCounter() {
        return dataManager.get(UNHAPPY_COUNTER);
    }

    public void setUnhappyCounter(int ticks) {
        dataManager.set(UNHAPPY_COUNTER, ticks);
    }

    public boolean isSneezing() {
        return getPandaFlag(FLAG_SNEEZING);
    }

    public void sneeze(boolean sneezing) {
        setPandaFlag(FLAG_SNEEZING, sneezing);
        if (!sneezing) {
            setSneezeCounter(0);
        }
    }

    public int getSneezeCounter() {
        return dataManager.get(SNEEZE_COUNTER);
    }

    public void setSneezeCounter(int ticks) {
        dataManager.set(SNEEZE_COUNTER, ticks);
    }

    public boolean isSitting() {
        return getPandaFlag(FLAG_SITTING);
    }

    public void sit(boolean sitting) {
        setPandaFlag(FLAG_SITTING, sitting);
    }

    public boolean isOnBack() {
        return getPandaFlag(FLAG_ON_BACK);
    }

    public void setOnBack(boolean onBack) {
        setPandaFlag(FLAG_ON_BACK, onBack);
    }

    public boolean isRolling() {
        return getPandaFlag(FLAG_ROLLING);
    }

    public void roll(boolean rolling) {
        setPandaFlag(FLAG_ROLLING, rolling);
    }

    public boolean isEating() {
        return dataManager.get(EAT_COUNTER) > 0;
    }

    public void eat(boolean eating) {
        dataManager.set(EAT_COUNTER, eating ? 1 : 0);
    }

    private int getEatCounter() {
        return dataManager.get(EAT_COUNTER);
    }

    private void setEatCounter(int ticks) {
        dataManager.set(EAT_COUNTER, ticks);
    }

    // ---- genes ------------------------------------------------------------------------------

    public PandaGene getMainGene() {
        return PandaGene.byId(dataManager.get(MAIN_GENE));
    }

    public void setMainGene(PandaGene gene) {
        dataManager.set(MAIN_GENE, (byte) gene.getId());
    }

    public PandaGene getHiddenGene() {
        return PandaGene.byId(dataManager.get(HIDDEN_GENE));
    }

    public void setHiddenGene(PandaGene gene) {
        dataManager.set(HIDDEN_GENE, (byte) gene.getId());
    }

    /** @return the personality the two genes make */
    public PandaGene getPersonality() {
        return PandaGene.personality(getMainGene(), getHiddenGene());
    }

    public boolean isLazy() {
        return getPersonality() == PandaGene.LAZY;
    }

    public boolean isWorried() {
        return getPersonality() == PandaGene.WORRIED;
    }

    public boolean isPlayful() {
        return getPersonality() == PandaGene.PLAYFUL;
    }

    public boolean isWeak() {
        return getPersonality() == PandaGene.WEAK;
    }

    public boolean isAggressive() {
        return getPersonality() == PandaGene.AGGRESSIVE;
    }

    /** A worried panda in a thunderstorm: cowering, and good for nothing else. */
    public boolean isScared() {
        return isWorried() && world.isThundering();
    }

    /** One gene from each parent, each picked from that parent's two, then a 1 in 32 mutation each. */
    void setGenesFromParents(EntityPanda first, @Nullable EntityPanda second) {
        if (second == null) {
            if (rand.nextBoolean()) {
                setMainGene(first.oneGeneAtRandom());
                setHiddenGene(PandaGene.random(rand));
            } else {
                setMainGene(PandaGene.random(rand));
                setHiddenGene(first.oneGeneAtRandom());
            }
        } else if (rand.nextBoolean()) {
            setMainGene(first.oneGeneAtRandom());
            setHiddenGene(second.oneGeneAtRandom());
        } else {
            setMainGene(second.oneGeneAtRandom());
            setHiddenGene(first.oneGeneAtRandom());
        }
        if (rand.nextInt(32) == 0) {
            setMainGene(PandaGene.random(rand));
        }
        if (rand.nextInt(32) == 0) {
            setHiddenGene(PandaGene.random(rand));
        }
    }

    private PandaGene oneGeneAtRandom() {
        return rand.nextBoolean() ? getMainGene() : getHiddenGene();
    }

    /** Weak pandas have 10 health and lazy ones walk at under half speed. */
    void applyPersonalityAttributes() {
        if (isWeak()) {
            getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(10.0D);
            if (getHealth() > 10.0F) {
                setHealth(10.0F);
            }
        }
        if (isLazy()) {
            getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.07D);
        }
    }

    // ---- setup ------------------------------------------------------------------------------

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(20.0D);
        getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.15D);
        getAttributeMap().registerAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(6.0D);
    }

    @Override
    protected void initEntityAI() {
        tasks.addTask(0, new EntityAISwimming(this));
        tasks.addTask(2, new PanicGoal(this, 2.0D));
        tasks.addTask(2, new BreedGoal(this, 1.0D));
        tasks.addTask(3, new AttackGoal(this, 1.2D, true));
        tasks.addTask(4, new EntityAITempt(this, 1.0D, false, temptItems()));
        tasks.addTask(6, new AvoidGoal<EntityPlayer>(this, EntityPlayer.class, 8.0F, 2.0D, 2.0D));
        tasks.addTask(6, new AvoidGoal<EntityMob>(this, EntityMob.class, 4.0F, 2.0D, 2.0D));
        tasks.addTask(7, new SitGoal(this));
        tasks.addTask(8, new LieOnBackGoal(this));
        tasks.addTask(8, new SneezeGoal(this));
        tasks.addTask(9, new LookAtPlayerGoal(this, EntityPlayer.class, 6.0F));
        tasks.addTask(10, new EntityAILookIdle(this));
        tasks.addTask(12, new RollGoal(this));
        tasks.addTask(13, new EntityAIFollowParent(this, 1.25D));
        tasks.addTask(14, new EntityAIWanderAvoidWater(this, 1.0D));
        targetTasks.addTask(1, new HurtByTargetGoal(this));
    }

    private static Set<Item> temptItems() {
        Set<Item> items = new HashSet<Item>();
        if (BambooContent.bambooItem != null) {
            items.add(BambooContent.bambooItem);
        }
        Item theirs = BambooFutureMc.futureMcBamboo();
        if (theirs != null) {
            items.add(theirs);
        }
        return items;
    }

    @Nullable
    @Override
    public IEntityLivingData onInitialSpawn(DifficultyInstance difficulty, @Nullable IEntityLivingData data) {
        data = super.onInitialSpawn(difficulty, data);
        setMainGene(PandaGene.random(rand));
        setHiddenGene(PandaGene.random(rand));
        applyPersonalityAttributes();
        // The first panda of a group is grown; each one after it is a cub one time in five.
        if (data instanceof GroupData) {
            if (rand.nextInt(5) == 0) {
                setGrowingAge(-24000);
            }
        } else {
            data = new GroupData();
        }
        return data;
    }

    @Override
    public EntityAgeable createChild(EntityAgeable mate) {
        EntityPanda cub = new EntityPanda(world);
        if (mate instanceof EntityPanda) {
            cub.setGenesFromParents(this, (EntityPanda) mate);
        }
        cub.applyPersonalityAttributes();
        return cub;
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        tag.setString("MainGene", getMainGene().getName());
        tag.setString("HiddenGene", getHiddenGene().getName());
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        setMainGene(PandaGene.byName(tag.getString("MainGene")));
        setHiddenGene(PandaGene.byName(tag.getString("HiddenGene")));
    }

    // ---- ticking ----------------------------------------------------------------------------

    @Override
    public void onUpdate() {
        super.onUpdate();

        if (isWorried()) {
            if (world.isThundering() && !isInWater()) {
                sit(true);
                eat(false);
            } else if (!isEating()) {
                sit(false);
            }
        }

        if (getAttackTarget() == null) {
            gotBamboo = false;
            didBite = false;
        }

        if (getUnhappyCounter() > 0) {
            if (unhappyAt != null && unhappyAt.isEntityAlive()) {
                faceEntity(unhappyAt, 90.0F, 90.0F);
            }
            if (!world.isRemote && (getUnhappyCounter() == 29 || getUnhappyCounter() == 14)) {
                playSound(BambooContent.pandaCantBreed, 1.0F, 1.0F);
            }
            setUnhappyCounter(getUnhappyCounter() - 1);
        } else {
            unhappyAt = null;
        }

        if (isSneezing()) {
            setSneezeCounter(getSneezeCounter() + 1);
            if (getSneezeCounter() > 20) {
                sneeze(false);
                afterSneeze();
            } else if (getSneezeCounter() == 1 && !world.isRemote) {
                playSound(BambooContent.pandaPreSneeze, 1.0F, 1.0F);
            }
        }

        if (isRolling()) {
            handleRoll();
        } else {
            rollCounter = 0;
        }

        if (isSitting()) {
            rotationPitch = 0.0F;
        }

        updateSitAmount();
        handleEating();
        updateOnBackAmount();
        updateRollAmount();
    }

    /**
     * Starts eating whatever it holds a moment after sitting down with it, and finishes some time
     * after four seconds - eating it if it is food, otherwise just putting it down again.
     */
    private void handleEating() {
        ItemStack held = getHeldItemMainhand();
        if (!isEating() && isSitting() && !isScared() && !held.isEmpty() && rand.nextInt(80) == 1) {
            eat(true);
        } else if (held.isEmpty() || !isSitting()) {
            eat(false);
        }

        if (isEating()) {
            addEatingEffects();
            if (!world.isRemote && getEatCounter() > 80 && rand.nextInt(20) == 1) {
                if (getEatCounter() > 100 && isFoodOrCake(getHeldItemMainhand())) {
                    setItemStackToSlot(EntityEquipmentSlot.MAINHAND, ItemStack.EMPTY);
                    sit(false);
                }
                eat(false);
                return;
            }
            setEatCounter(getEatCounter() + 1);
        }
    }

    private void addEatingEffects() {
        if (getEatCounter() % 5 != 0) {
            return;
        }
        if (!world.isRemote) {
            playSound(BambooContent.pandaEat, 0.5F + 0.5F * rand.nextInt(2),
                    (rand.nextFloat() - rand.nextFloat()) * 0.2F + 1.0F);
            return;
        }
        ItemStack held = getHeldItemMainhand();
        if (held.isEmpty()) {
            return;
        }
        for (int i = 0; i < 6; i++) {
            Vec3d speed = new Vec3d((rand.nextFloat() - 0.5D) * 0.1D, Math.random() * 0.1D + 0.1D,
                    (rand.nextFloat() - 0.5D) * 0.1D)
                    .rotatePitch(-rotationPitch * 0.017453292F)
                    .rotateYaw(-rotationYaw * 0.017453292F);
            double y = -rand.nextFloat() * 0.6D - 0.3D;
            Vec3d at = new Vec3d((rand.nextFloat() - 0.5D) * 0.8D, y, 1.0D + (rand.nextFloat() - 0.5D) * 0.4D)
                    .rotateYaw(-renderYawOffset * 0.017453292F)
                    .add(posX, posY + getEyeHeight() + 1.0D, posZ);
            world.spawnParticle(EnumParticleTypes.ITEM_CRACK, at.x, at.y, at.z, speed.x, speed.y + 0.05D, speed.z,
                    Item.getIdFromItem(held.getItem()), held.getMetadata());
        }
    }

    private void updateSitAmount() {
        sitAmountO = sitAmount;
        sitAmount = isSitting() ? Math.min(1.0F, sitAmount + 0.15F) : Math.max(0.0F, sitAmount - 0.19F);
    }

    private void updateOnBackAmount() {
        onBackAmountO = onBackAmount;
        onBackAmount = isOnBack() ? Math.min(1.0F, onBackAmount + 0.15F) : Math.max(0.0F, onBackAmount - 0.19F);
    }

    private void updateRollAmount() {
        rollAmountO = rollAmount;
        rollAmount = isRolling() ? Math.min(1.0F, rollAmount + 0.15F) : Math.max(0.0F, rollAmount - 0.19F);
    }

    public float getSitAmount(float partialTicks) {
        return sitAmountO + (sitAmount - sitAmountO) * partialTicks;
    }

    public float getLieOnBackAmount(float partialTicks) {
        return onBackAmountO + (onBackAmount - onBackAmountO) * partialTicks;
    }

    public float getRollAmount(float partialTicks) {
        return rollAmountO + (rollAmount - rollAmountO) * partialTicks;
    }

    /** A roll lasts 32 ticks: a lunge forward and three hops as it goes over. */
    private void handleRoll() {
        rollCounter++;
        if (rollCounter > 32) {
            roll(false);
            return;
        }
        if (world.isRemote) {
            return;
        }
        if (rollCounter == 1) {
            float yaw = rotationYaw * 0.017453292F;
            float push = isChild() ? 0.1F : 0.2F;
            rollDelta = new Vec3d(motionX + -MathHelper.sin(yaw) * push, 0.0D, motionZ + MathHelper.cos(yaw) * push);
            motionX = rollDelta.x;
            motionY = 0.27D;
            motionZ = rollDelta.z;
        } else if (rollCounter == 7 || rollCounter == 15 || rollCounter == 23) {
            motionX = 0.0D;
            motionY = onGround ? 0.27D : motionY;
            motionZ = 0.0D;
        } else {
            motionX = rollDelta.x;
            motionZ = rollDelta.z;
        }
        velocityChanged = true;
    }

    /** The sneeze itself: a puff, a sound, startled adults, and now and then a slime ball. */
    private void afterSneeze() {
        if (world.isRemote) {
            double reach = (width + 1.0F) * 0.5D;
            float body = renderYawOffset * 0.017453292F;
            world.spawnParticle(EnumParticleTypes.CLOUD, posX - reach * MathHelper.sin(body),
                    posY + getEyeHeight() - 0.1D, posZ + reach * MathHelper.cos(body), motionX, 0.0D, motionZ);
            return;
        }
        playSound(BambooContent.pandaSneeze, 1.0F, 1.0F);
        for (EntityPanda panda : world.getEntitiesWithinAABB(EntityPanda.class, getEntityBoundingBox().grow(10.0D))) {
            if (!panda.isChild() && panda.onGround && !panda.isInWater() && panda.canPerformAction()) {
                panda.jump();
            }
        }
        if (rand.nextInt(700) == 0 && world.getGameRules().getBoolean("doMobLoot")) {
            dropItem(Items.SLIME_BALL, 1);
        }
    }

    /** @return true when it is doing nothing in particular, and free to start something */
    public boolean canPerformAction() {
        return !isOnBack() && !isScared() && !isEating() && !isRolling() && !isSitting();
    }

    private void tryToSit() {
        if (!isInWater()) {
            moveForward = 0.0F;
            getNavigator().clearPath();
            sit(true);
        }
    }

    // ---- items ------------------------------------------------------------------------------

    @Override
    public boolean isBreedingItem(ItemStack stack) {
        return BambooContent.isBamboo(stack);
    }

    private boolean isFoodOrCake(ItemStack stack) {
        return isBreedingItem(stack) || stack.getItem() == Items.CAKE;
    }

    /** Picks up bamboo or cake when its paws are free, and always drops it again on death. */
    @Override
    protected void updateEquipmentIfNeeded(EntityItem item) {
        if (getItemStackFromSlot(EntityEquipmentSlot.MAINHAND).isEmpty() && PANDA_ITEMS.test(item)) {
            ItemStack stack = item.getItem();
            setItemStackToSlot(EntityEquipmentSlot.MAINHAND, stack);
            inventoryHandsDropChances[EntityEquipmentSlot.MAINHAND.getIndex()] = 2.0F;
            onItemPickup(item, stack.getCount());
            item.setDead();
        }
    }

    @Override
    public boolean processInteract(EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (stack.getItem() instanceof ItemMonsterPlacer) {
            return super.processInteract(player, hand);
        }
        if (isScared()) {
            return false;
        }
        if (isOnBack()) {
            setOnBack(false);
            return true;
        }
        if (!isBreedingItem(stack)) {
            return false;
        }

        if (getAttackTarget() != null) {
            gotBamboo = true;
        }
        if (isChild()) {
            consumeItemFromStack(player, stack);
            ageUp((int) ((float) (-getGrowingAge() / 20) * 0.1F), true);
        } else if (!world.isRemote && getGrowingAge() == 0 && !isInLove()) {
            consumeItemFromStack(player, stack);
            setInLove(player);
        } else {
            if (world.isRemote || isSitting() || isInWater()) {
                return false;
            }
            tryToSit();
            eat(true);
            ItemStack held = getItemStackFromSlot(EntityEquipmentSlot.MAINHAND);
            if (!held.isEmpty() && !player.capabilities.isCreativeMode) {
                entityDropItem(held, 0.0F);
            }
            ItemStack one = stack.copy();
            one.setCount(1);
            setItemStackToSlot(EntityEquipmentSlot.MAINHAND, one);
            consumeItemFromStack(player, stack);
        }
        return true;
    }

    // ---- combat -----------------------------------------------------------------------------

    @Override
    public boolean attackEntityAsMob(Entity target) {
        playSound(BambooContent.pandaBite, 1.0F, 1.0F);
        if (!isAggressive()) {
            didBite = true;
        }
        float damage = (float) getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).getAttributeValue();
        boolean hit = target.attackEntityFrom(DamageSource.causeMobDamage(this), damage);
        if (hit) {
            applyEnchantments(this, target);
        }
        return hit;
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        if (!world.isRemote && !isEntityInvulnerable(source)) {
            sit(false);
        }
        return super.attackEntityFrom(source, amount);
    }

    // ---- sounds, drops ----------------------------------------------------------------------

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        if (isAggressive()) {
            return BambooContent.pandaAggressiveAmbient;
        }
        return isWorried() ? BambooContent.pandaWorriedAmbient : BambooContent.pandaAmbient;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return BambooContent.pandaHurt;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return BambooContent.pandaDeath;
    }

    @Override
    protected void playStepSound(BlockPos pos, Block block) {
        playSound(BambooContent.pandaStep, 0.15F, 1.0F);
    }

    @Nullable
    @Override
    protected ResourceLocation getLootTable() {
        return BambooContent.PANDA_LOOT;
    }

    // ---- AI ---------------------------------------------------------------------------------

    /** Marks the second and later pandas of a spawned group, so some of them can be cubs. */
    private static final class GroupData implements IEntityLivingData {
    }

    /** Stands still while busy: sitting, eating, rolling, lying back or cowering. */
    private static final class MoveHelper extends EntityMoveHelper {
        private final EntityPanda panda;

        MoveHelper(EntityPanda panda) {
            super(panda);
            this.panda = panda;
        }

        @Override
        public void onUpdateMoveHelper() {
            if (panda.canPerformAction()) {
                super.onUpdateMoveHelper();
            }
        }
    }

    /** Only fire makes a panda run - toward water if there is some. */
    private static final class PanicGoal extends EntityAIPanic {
        private final EntityPanda panda;

        PanicGoal(EntityPanda panda, double speed) {
            super(panda, speed);
            this.panda = panda;
        }

        @Override
        public boolean shouldExecute() {
            return panda.isBurning() && super.shouldExecute();
        }

        @Override
        public boolean shouldContinueExecuting() {
            if (panda.isSitting()) {
                panda.getNavigator().clearPath();
                return false;
            }
            return super.shouldContinueExecuting();
        }
    }

    /**
     * Breeding, but only beside growing bamboo.
     *
     * <p>Without bamboo, at most once every 30 seconds, the panda turns unhappy for 32 ticks: it
     * shakes its head, bleats twice and stares at the nearest player within 8 blocks. 1.14 makes
     * that player its attack target, which would set the bite goal on them; the wiki describes only
     * the head-shaking, so this panda glares and does not bite.
     */
    private static final class BreedGoal extends EntityAIMate {
        private final EntityPanda panda;
        private int unhappyCooldown;

        BreedGoal(EntityPanda panda, double speed) {
            super(panda, speed);
            this.panda = panda;
        }

        @Override
        public boolean shouldExecute() {
            if (!super.shouldExecute() || panda.getUnhappyCounter() != 0) {
                return false;
            }
            if (canFindBamboo()) {
                return true;
            }
            if (unhappyCooldown <= panda.ticksExisted) {
                panda.setUnhappyCounter(32);
                unhappyCooldown = panda.ticksExisted + 600;
                panda.unhappyAt = panda.world.getClosestPlayerToEntity(panda, 8.0D);
            }
            return false;
        }

        /** Bamboo stalk within 7 blocks across and up to 2 blocks above the panda's feet. */
        private boolean canFindBamboo() {
            BlockPos origin = new BlockPos(panda);
            BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
            for (int y = 0; y < 3; y++) {
                for (int x = -7; x <= 7; x++) {
                    for (int z = -7; z <= 7; z++) {
                        cursor.setPos(origin.getX() + x, origin.getY() + y, origin.getZ() + z);
                        if (isBambooBlock(panda.world.getBlockState(cursor).getBlock())) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }

        private static boolean isBambooBlock(Block block) {
            if (block == BambooContent.bamboo) {
                return true;
            }
            ResourceLocation name = block.getRegistryName();
            return name != null && "futuremc".equals(name.getNamespace()) && "bamboo".equals(name.getPath());
        }
    }

    private static final class AttackGoal extends EntityAIAttackMelee {
        private final EntityPanda panda;

        AttackGoal(EntityPanda panda, double speed, boolean memory) {
            super(panda, speed, memory);
            this.panda = panda;
        }

        @Override
        public boolean shouldExecute() {
            return panda.canPerformAction() && super.shouldExecute();
        }
    }

    /** Only worried pandas keep their distance. */
    private static final class AvoidGoal<T extends Entity> extends EntityAIAvoidEntity<T> {
        private final EntityPanda panda;

        AvoidGoal(EntityPanda panda, Class<T> type, float distance, double farSpeed, double nearSpeed) {
            super(panda, type, entity -> !(entity instanceof EntityPlayer) || !((EntityPlayer) entity).isSpectator(),
                    distance, farSpeed, nearSpeed);
            this.panda = panda;
        }

        @Override
        public boolean shouldExecute() {
            return panda.isWorried() && panda.canPerformAction() && super.shouldExecute();
        }
    }

    /**
     * Sitting down to eat: walks over to bamboo or cake lying within eight blocks, picks it up (as
     * any mob picks up loot), sits and holds it. Gets up again after a while - lazy pandas stay down
     * longer - putting down anything it did not eat, and waits 10 to 160 seconds (lazy: 10 to 60)
     * before doing it again.
     */
    private static final class SitGoal extends EntityAIBase {
        private final EntityPanda panda;
        private int cooldown;

        SitGoal(EntityPanda panda) {
            this.panda = panda;
            setMutexBits(1);
        }

        @Override
        public boolean shouldExecute() {
            if (cooldown > panda.ticksExisted || panda.isChild() || panda.isInWater() || !panda.canPerformAction()
                    || panda.getUnhappyCounter() > 0) {
                return false;
            }
            List<EntityItem> items = panda.world.getEntitiesWithinAABB(EntityItem.class,
                    panda.getEntityBoundingBox().grow(6.0D), PANDA_ITEMS::test);
            return !items.isEmpty() || !panda.getHeldItemMainhand().isEmpty();
        }

        @Override
        public boolean shouldContinueExecuting() {
            if (panda.isInWater() || (!panda.isLazy() && panda.rand.nextInt(600) == 1)) {
                return false;
            }
            return panda.rand.nextInt(2000) != 1;
        }

        @Override
        public void updateTask() {
            if (!panda.isSitting() && !panda.getHeldItemMainhand().isEmpty()) {
                panda.tryToSit();
            }
        }

        @Override
        public void startExecuting() {
            List<EntityItem> items = panda.world.getEntitiesWithinAABB(EntityItem.class,
                    panda.getEntityBoundingBox().grow(8.0D), PANDA_ITEMS::test);
            if (!items.isEmpty() && panda.getHeldItemMainhand().isEmpty()) {
                panda.getNavigator().tryMoveToEntityLiving(items.get(0), 1.2D);
            } else if (!panda.getHeldItemMainhand().isEmpty()) {
                panda.tryToSit();
            }
            cooldown = 0;
        }

        @Override
        public void resetTask() {
            ItemStack held = panda.getHeldItemMainhand();
            if (!held.isEmpty()) {
                panda.entityDropItem(held, 0.0F);
                panda.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, ItemStack.EMPTY);
                int seconds = panda.isLazy() ? panda.rand.nextInt(50) + 10 : panda.rand.nextInt(150) + 10;
                cooldown = panda.ticksExisted + seconds * 20;
            }
            panda.sit(false);
        }
    }

    /** Lazy pandas roll onto their backs now and then. A player's click turns them back over. */
    private static final class LieOnBackGoal extends EntityAIBase {
        private final EntityPanda panda;
        private int cooldown;

        LieOnBackGoal(EntityPanda panda) {
            this.panda = panda;
        }

        @Override
        public boolean shouldExecute() {
            return cooldown < panda.ticksExisted && panda.isLazy() && panda.canPerformAction()
                    && panda.rand.nextInt(400) == 1;
        }

        @Override
        public boolean shouldContinueExecuting() {
            if (panda.isInWater() || (!panda.isLazy() && panda.rand.nextInt(600) == 1)) {
                return false;
            }
            return panda.rand.nextInt(2000) != 1;
        }

        @Override
        public void startExecuting() {
            panda.setOnBack(true);
            cooldown = 0;
        }

        @Override
        public void resetTask() {
            panda.setOnBack(false);
            cooldown = panda.ticksExisted + 200;
        }
    }

    /** Cubs sneeze: one in 6000 ticks, or one in 500 for a weak cub. */
    private static final class SneezeGoal extends EntityAIBase {
        private final EntityPanda panda;

        SneezeGoal(EntityPanda panda) {
            this.panda = panda;
        }

        @Override
        public boolean shouldExecute() {
            if (!panda.isChild() || !panda.canPerformAction()) {
                return false;
            }
            if (panda.isWeak() && panda.rand.nextInt(500) == 1) {
                return true;
            }
            return panda.rand.nextInt(6000) == 1;
        }

        @Override
        public boolean shouldContinueExecuting() {
            return false;
        }

        @Override
        public void startExecuting() {
            panda.sneeze(true);
        }
    }

    private static final class LookAtPlayerGoal extends EntityAIWatchClosest {
        private final EntityPanda panda;

        LookAtPlayerGoal(EntityPanda panda, Class<? extends Entity> type, float distance) {
            super(panda, type, distance);
            this.panda = panda;
        }

        @Override
        public boolean shouldExecute() {
            return panda.canPerformAction() && super.shouldExecute();
        }
    }

    /**
     * Rolling over: cubs and playful pandas, on the ground and free. Always when the block ahead
     * and below is air - so they tumble off ledges - otherwise one tick in 60 for a playful panda
     * and one in 500 for a cub.
     */
    private static final class RollGoal extends EntityAIBase {
        private final EntityPanda panda;

        RollGoal(EntityPanda panda) {
            this.panda = panda;
            setMutexBits(7);
        }

        @Override
        public boolean shouldExecute() {
            if ((!panda.isChild() && !panda.isPlayful()) || !panda.onGround || !panda.canPerformAction()) {
                return false;
            }
            float yaw = panda.rotationYaw * 0.017453292F;
            float ahead = -MathHelper.sin(yaw);
            float side = MathHelper.cos(yaw);
            int dx = Math.abs(ahead) > 0.5D ? (int) (ahead / Math.abs(ahead)) : 0;
            int dz = Math.abs(side) > 0.5D ? (int) (side / Math.abs(side)) : 0;
            if (panda.world.isAirBlock(new BlockPos(panda).add(dx, -1, dz))) {
                return true;
            }
            if (panda.isPlayful() && panda.rand.nextInt(60) == 1) {
                return true;
            }
            return panda.rand.nextInt(500) == 1;
        }

        @Override
        public boolean shouldContinueExecuting() {
            return false;
        }

        @Override
        public void startExecuting() {
            panda.roll(true);
        }

        @Override
        public boolean isInterruptible() {
            return false;
        }
    }

    /**
     * Fights back when hit. Aggressive pandas call nearby aggressive pandas in and keep at it; the
     * rest bite once and stop, and any panda stops if its attacker feeds it bamboo.
     */
    private static final class HurtByTargetGoal extends EntityAIHurtByTarget {
        private final EntityPanda panda;

        HurtByTargetGoal(EntityPanda panda) {
            super(panda, true);
            this.panda = panda;
        }

        @Override
        public boolean shouldContinueExecuting() {
            if (panda.gotBamboo || panda.didBite) {
                panda.setAttackTarget(null);
                return false;
            }
            return super.shouldContinueExecuting();
        }

        @Override
        protected void setEntityAttackTarget(EntityCreature other, EntityLivingBase target) {
            if (other instanceof EntityPanda && ((EntityPanda) other).isAggressive()) {
                other.setAttackTarget(target);
            }
        }
    }
}
