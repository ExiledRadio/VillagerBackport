package com.exiledradio.villagerbackport.cat;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIAvoidEntity;
import net.minecraft.entity.ai.EntityAIFollowOwner;
import net.minecraft.entity.ai.EntityAILeapAtTarget;
import net.minecraft.entity.ai.EntityAIMate;
import net.minecraft.entity.ai.EntityAIOcelotAttack;
import net.minecraft.entity.ai.EntityAISwimming;
import net.minecraft.entity.ai.EntityAITargetNonTamed;
import net.minecraft.entity.ai.EntityAIWanderAvoidWater;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityRabbit;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.World;
import net.minecraft.world.storage.loot.LootTableList;
import net.minecraftforge.oredict.OreDictionary;

import javax.annotation.Nullable;

/**
 * 1.14's cat.
 *
 * <h2>The animal</h2>
 * Ten hearts' worth of half-hearts (10 health), the ocelot's speed and bite, no fall damage. A stray
 * cat runs from players who come near and slinks after one holding raw cod or salmon, meowing
 * to beg; fed one, it is tamed one time in three, and sits. A tamed cat:
 * <ul>
 *   <li>follows its owner, and sits or stands when its owner uses an empty hand (or anything that is
 *       not food or dye) on it;</li>
 *   <li>is healed by fish, and breeds with another tamed cat on fish; a kitten takes one parent's
 *       coat and, if its parent is tamed, its owner and one parent's collar;</li>
 *   <li>has a collar - red to start with - that its owner can dye;</li>
 *   <li>sits on chests (which then cannot be opened), lit furnaces and the foot of beds, and lies down
 *       on beds;</li>
 *   <li>curls up on its owner's bed while they sleep, and in the morning has seven chances in ten of
 *       having brought them something ({@code gameplay/cat_morning_gift}).</li>
 * </ul>
 * Untamed cats hunt rabbits (1.14 adds baby turtles, which 1.12.2 has none of), and despawn like any
 * other passing animal unless they came with their village or witch hut.
 *
 * <h2>Coats</h2>
 * Eleven, numbered as 1.14 numbers them (its {@code CatType}), so a world, a template and
 * 1.14's own data agree: tabby, tuxedo ({@code black}), red, Siamese, British shorthair, calico,
 * Persian, ragdoll, white, Jellie, and black ({@code all_black}). The last is the witch's cat, and
 * otherwise turns up only on a full moon.
 *
 * <h2>Why not a subclass of the ocelot</h2>
 * It would have been less code - the model and half the goals are the ocelot's - but everything that
 * looks for ocelots would then find cats: other mods' taming tweaks, 1.12.2's own ocelot code reading
 * a skin field cats do not use, and this mod's {@link OcelotTrust}, which must leave cats alone.
 */
public class EntityCat extends EntityTameable {

    /** The coats, by 1.14's numbering; each is also a texture and an advancement criterion. */
    public static final String[] TYPES = {"tabby", "black", "red", "siamese", "british_shorthair", "calico",
            "persian", "ragdoll", "white", "jellie", "all_black"};

    /** The witch's cat. */
    public static final int ALL_BLACK = 10;

    public static final ResourceLocation MORNING_GIFT = LootTableList.register(
            new ResourceLocation("villagerbackport", "gameplay/cat_morning_gift"));

    private static final ResourceLocation LOOT = LootTableList.register(
            new ResourceLocation("villagerbackport", "entities/cat"));

    private static final DataParameter<Integer> TYPE = EntityDataManager.createKey(EntityCat.class, DataSerializers.VARINT);
    private static final DataParameter<Boolean> LYING = EntityDataManager.createKey(EntityCat.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Boolean> RELAXED = EntityDataManager.createKey(EntityCat.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Integer> COLLAR = EntityDataManager.createKey(EntityCat.class, DataSerializers.VARINT);

    /** The pose blends, 0 to 1, kept on both sides from the synced flags; the model reads them. */
    private float lieDown;
    private float lieDownO;
    private float lieDownTail;
    private float lieDownTailO;
    private float relax;
    private float relaxO;

    private EntityAICatTempt temptGoal;

    /** See {@link #orderSit}. */
    private boolean orderedToSit;

    public EntityCat(World world) {
        super(world);
        this.setSize(0.6F, 0.7F);
    }

    // ---------------------------------------------------------------------------------------------
    // Goals - 1.14's list and priorities
    // ---------------------------------------------------------------------------------------------

    @Override
    protected void initEntityAI() {
        this.aiSit = new net.minecraft.entity.ai.EntityAISit(this);
        this.temptGoal = new EntityAICatTempt(this, 0.6D, true);

        this.tasks.addTask(1, new EntityAISwimming(this));
        this.tasks.addTask(1, new EntityAICatRelaxOnOwner(this));
        this.tasks.addTask(2, this.aiSit);
        this.tasks.addTask(3, this.temptGoal);
        this.tasks.addTask(4, new AvoidPlayers(this));
        this.tasks.addTask(5, new EntityAICatLieOnBed(this, 1.1D, 8));
        this.tasks.addTask(6, new EntityAIFollowOwner(this, 1.0D, 10.0F, 5.0F));
        this.tasks.addTask(7, new EntityAICatSitOnBlock(this, 0.8D));
        this.tasks.addTask(8, new EntityAILeapAtTarget(this, 0.3F));
        this.tasks.addTask(9, new EntityAIOcelotAttack(this));
        this.tasks.addTask(10, new EntityAIMate(this, 0.8D));
        this.tasks.addTask(11, new EntityAIWanderAvoidWater(this, 0.8D, 1.0000001E-5F));
        this.tasks.addTask(12, new EntityAIWatchClosest(this, EntityPlayer.class, 10.0F));

        this.targetTasks.addTask(1, new EntityAITargetNonTamed<EntityRabbit>(this, EntityRabbit.class, false, null));
    }

    /**
     * 1.14 swaps this goal in and out as a cat is tamed. Here it stays and asks instead: editing the
     * goal list from code a goal can reach (taming happens inside an interaction a goal may be
     * mid-way through) is how entities get deleted by a ConcurrentModificationException.
     */
    private static final class AvoidPlayers extends EntityAIAvoidEntity<EntityPlayer> {
        private final EntityCat cat;

        AvoidPlayers(EntityCat cat) {
            super(cat, EntityPlayer.class, 16.0F, 0.8D, 1.33D);
            this.cat = cat;
        }

        @Override
        public boolean shouldExecute() {
            return !this.cat.isTamed() && super.shouldExecute();
        }

        @Override
        public boolean shouldContinueExecuting() {
            return !this.cat.isTamed() && super.shouldContinueExecuting();
        }
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(10.0D);
        this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.30000001192092896D);
        this.getAttributeMap().registerAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(3.0D);
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        this.dataManager.register(TYPE, 1);
        this.dataManager.register(LYING, false);
        this.dataManager.register(RELAXED, false);
        this.dataManager.register(COLLAR, EnumDyeColor.RED.getMetadata());
    }

    // ---------------------------------------------------------------------------------------------
    // State
    // ---------------------------------------------------------------------------------------------

    public int getCatType() {
        return this.dataManager.get(TYPE);
    }

    public void setCatType(int type) {
        if (type < 0 || type >= TYPES.length) {
            type = this.rand.nextInt(10);
        }
        this.dataManager.set(TYPE, type);
    }

    public String getTypeName() {
        return TYPES[MathHelper.clamp(getCatType(), 0, TYPES.length - 1)];
    }

    public boolean isLying() {
        return this.dataManager.get(LYING);
    }

    public void setLying(boolean lying) {
        this.dataManager.set(LYING, lying);
    }

    /** 1.14's "relax state one": the head tilted down, while it settles on its owner's bed. */
    public boolean isRelaxed() {
        return this.dataManager.get(RELAXED);
    }

    public void setRelaxed(boolean relaxed) {
        this.dataManager.set(RELAXED, relaxed);
    }

    public EnumDyeColor getCollarColor() {
        return EnumDyeColor.byMetadata(this.dataManager.get(COLLAR));
    }

    public void setCollarColor(EnumDyeColor color) {
        this.dataManager.set(COLLAR, color.getMetadata());
    }

    /**
     * Gives or withdraws the order to sit. 1.12.2's sit goal keeps its order private, and the sitting
     * flag is only the pose - which a cat on a chest has without having been told to sit - so the
     * order is mirrored here for the goals that need to tell the two apart.
     */
    void orderSit(boolean sitting) {
        this.orderedToSit = sitting;
        if (this.aiSit != null) {
            this.aiSit.setSitting(sitting);
        }
    }

    boolean isOrderedToSit() {
        return this.orderedToSit;
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound compound) {
        super.writeEntityToNBT(compound);
        compound.setInteger("CatType", getCatType());
        compound.setByte("CollarColor", (byte) getCollarColor().getMetadata());
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound compound) {
        super.readEntityFromNBT(compound);
        this.orderedToSit = compound.getBoolean("Sitting");
        setCatType(compound.getInteger("CatType"));
        if (compound.hasKey("CollarColor", 99)) {
            setCollarColor(EnumDyeColor.byMetadata(compound.getByte("CollarColor")));
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Ticking
    // ---------------------------------------------------------------------------------------------

    /** 1.14's {@code customServerAiStep}: creeping while tempted, running while it flees. */
    @Override
    public void updateAITasks() {
        if (this.getMoveHelper().isUpdating()) {
            double speed = this.getMoveHelper().getSpeed();
            this.setSneaking(speed == 0.6D);
            this.setSprinting(speed == 1.33D);
        } else {
            this.setSneaking(false);
            this.setSprinting(false);
        }
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (!this.world.isRemote && this.temptGoal != null && this.temptGoal.isRunning() && !this.isTamed()
                && this.ticksExisted % 100 == 0 && CatContent.begForFood != null) {
            this.playSound(CatContent.begForFood, 1.0F, 1.0F);
        }

        if ((isLying() || isRelaxed()) && this.ticksExisted % 5 == 0) {
            this.playSound(SoundEvents.ENTITY_CAT_PURR, 0.6F + 0.4F * (this.rand.nextFloat() - this.rand.nextFloat()), 1.0F);
        }

        this.lieDownO = this.lieDown;
        this.lieDownTailO = this.lieDownTail;
        if (isLying()) {
            this.lieDown = Math.min(1.0F, this.lieDown + 0.15F);
            this.lieDownTail = Math.min(1.0F, this.lieDownTail + 0.08F);
        } else {
            this.lieDown = Math.max(0.0F, this.lieDown - 0.22F);
            this.lieDownTail = Math.max(0.0F, this.lieDownTail - 0.13F);
        }

        this.relaxO = this.relax;
        if (isRelaxed()) {
            this.relax = Math.min(1.0F, this.relax + 0.1F);
        } else {
            this.relax = Math.max(0.0F, this.relax - 0.13F);
        }
    }

    public float getLieDownAmount(float partialTicks) {
        return this.lieDownO + (this.lieDown - this.lieDownO) * partialTicks;
    }

    public float getLieDownAmountTail(float partialTicks) {
        return this.lieDownTailO + (this.lieDownTail - this.lieDownTailO) * partialTicks;
    }

    public float getRelaxAmount(float partialTicks) {
        return this.relaxO + (this.relax - this.relaxO) * partialTicks;
    }

    // ---------------------------------------------------------------------------------------------
    // Sounds
    // ---------------------------------------------------------------------------------------------

    @Override
    @Nullable
    protected SoundEvent getAmbientSound() {
        if (this.isTamed()) {
            if (this.isInLove()) {
                return SoundEvents.ENTITY_CAT_PURR;
            }
            return this.rand.nextInt(4) == 0 ? SoundEvents.ENTITY_CAT_PURREOW : SoundEvents.ENTITY_CAT_AMBIENT;
        }
        return CatContent.strayAmbient;
    }

    @Override
    public int getTalkInterval() {
        return 120;
    }

    public void hiss() {
        this.playSound(SoundEvents.ENTITY_CAT_HISS, this.getSoundVolume(), this.getSoundPitch());
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENTITY_CAT_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_CAT_DEATH;
    }

    @Override
    protected float getSoundVolume() {
        return 0.4F;
    }

    // ---------------------------------------------------------------------------------------------
    // Body
    // ---------------------------------------------------------------------------------------------

    @Override
    public void fall(float distance, float damageMultiplier) {
        // Cats land on their feet.
    }

    @Override
    public boolean attackEntityAsMob(Entity target) {
        return target.attackEntityFrom(DamageSource.causeMobDamage(this), 3.0F);
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        if (this.isEntityInvulnerable(source)) {
            return false;
        }
        orderSit(false);
        return super.attackEntityFrom(source, amount);
    }

    @Override
    public float getEyeHeight() {
        return this.height * 0.5F;
    }

    @Override
    @Nullable
    protected ResourceLocation getLootTable() {
        return LOOT;
    }

    @Override
    protected boolean canDespawn() {
        return !this.isTamed() && this.ticksExisted > 2400;
    }

    // ---------------------------------------------------------------------------------------------
    // Interaction
    // ---------------------------------------------------------------------------------------------

    @Override
    public boolean isBreedingItem(ItemStack stack) {
        return CatContent.isFish(stack);
    }

    /** 1.14's {@code usePlayerItem}: eating a fish has its own sound. */
    @Override
    protected void consumeItemFromStack(EntityPlayer player, ItemStack stack) {
        if (isBreedingItem(stack) && CatContent.eat != null) {
            this.playSound(CatContent.eat, 1.0F, 1.0F);
        }
        super.consumeItemFromStack(player, stack);
    }

    @Override
    public boolean processInteract(EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);

        if (this.isTamed()) {
            // By id rather than isOwner, which needs the owner loaded in this world as an entity.
            if (player.getUniqueID().equals(this.getOwnerId())) {
                EnumDyeColor dye = dyeColorOf(stack);
                if (dye != null) {
                    if (dye != getCollarColor()) {
                        setCollarColor(dye);
                        if (!player.capabilities.isCreativeMode) {
                            stack.shrink(1);
                        }
                        return true;
                    }
                } else if (isBreedingItem(stack)) {
                    if (this.getHealth() < this.getMaxHealth() && stack.getItem() instanceof ItemFood) {
                        this.consumeItemFromStack(player, stack);
                        this.heal(((ItemFood) stack.getItem()).getHealAmount(stack));
                        return true;
                    }
                } else if (!this.world.isRemote) {
                    orderSit(!isOrderedToSit());
                }
            }
        } else if (isBreedingItem(stack)) {
            this.consumeItemFromStack(player, stack);
            if (!this.world.isRemote) {
                if (this.rand.nextInt(3) == 0 && !net.minecraftforge.event.ForgeEventFactory.onAnimalTame(this, player)) {
                    tameBy(player);
                    orderSit(true);
                    this.setSitting(true);
                    this.world.setEntityState(this, (byte) 7);
                } else {
                    this.world.setEntityState(this, (byte) 6);
                }
            }
            return true;
        }

        return super.processInteract(player, hand);
    }

    /** Taming, with the advancements 1.14 hangs off it. */
    void tameBy(EntityPlayer player) {
        // setTamedBy fires vanilla's tame_animal trigger itself.
        this.setTamedBy(player);
        this.navigator.clearPath();
        this.setAttackTarget(null);
        if (player instanceof EntityPlayerMP) {
            CatAdvancements.tamed((EntityPlayerMP) player, this);
        }
    }

    /**
     * Sets the owner without the advancement - for conversions and kittens. 1.12.2's
     * {@code setTamedBy} also fires the tame trigger, so this goes round it.
     */
    void setOwnerQuietly(java.util.UUID owner) {
        this.setTamed(true);
        this.setOwnerId(owner);
    }

    /** @return the dye this stack is - vanilla's or any mod's, by ore name - or null */
    @Nullable
    static EnumDyeColor dyeColorOf(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        if (stack.getItem() == Items.DYE) {
            return EnumDyeColor.byDyeDamage(stack.getMetadata());
        }
        int[] ids = OreDictionary.getOreIDs(stack);
        for (EnumDyeColor color : EnumDyeColor.values()) {
            String name = "dye" + oreSuffix(color);
            int id = OreDictionary.getOreID(name);
            for (int has : ids) {
                if (has == id) {
                    return color;
                }
            }
        }
        return null;
    }

    /** {@code light_blue} to {@code LightBlue}, the ore dictionary's spelling. */
    private static String oreSuffix(EnumDyeColor color) {
        StringBuilder out = new StringBuilder();
        for (String part : color.getName().split("_")) {
            out.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        String name = out.toString();
        // 1.12.2 calls light gray "silver"; the ore dictionary calls it LightGray.
        return "Silver".equals(name) ? "LightGray" : name;
    }

    // ---------------------------------------------------------------------------------------------
    // Breeding and spawning
    // ---------------------------------------------------------------------------------------------

    @Override
    public boolean canMateWith(EntityAnimal other) {
        if (!this.isTamed() || other == this || !(other instanceof EntityCat)) {
            return false;
        }
        EntityCat cat = (EntityCat) other;
        return cat.isTamed() && this.isInLove() && cat.isInLove();
    }

    @Override
    public EntityCat createChild(EntityAgeable mate) {
        EntityCat child = new EntityCat(this.world);
        if (mate instanceof EntityCat) {
            EntityCat other = (EntityCat) mate;
            child.setCatType(this.rand.nextBoolean() ? this.getCatType() : other.getCatType());
            if (this.isTamed() && this.getOwnerId() != null) {
                child.setOwnerQuietly(this.getOwnerId());
                child.setCollarColor(this.rand.nextBoolean() ? this.getCollarColor() : other.getCollarColor());
            }
        }
        return child;
    }

    /**
     * A random coat - black only under a full moon - and the witch's black cat if this is a witch
     * hut. Spawn eggs come here too, so an egg used in a witch hut gives a black cat, as in 1.14.
     */
    @Override
    @Nullable
    public IEntityLivingData onInitialSpawn(DifficultyInstance difficulty, @Nullable IEntityLivingData data) {
        data = super.onInitialSpawn(difficulty, data);
        if (this.world.getCurrentMoonPhaseFactor() > 0.9F) {
            setCatType(this.rand.nextInt(11));
        } else {
            setCatType(this.rand.nextInt(10));
        }
        if (CatSpawner.isWitchHut(this.world, new BlockPos(this))) {
            setCatType(ALL_BLACK);
            this.enablePersistence();
        }
        return data;
    }

    /**
     * A cat for one of 1.14's village pieces: the piece's coat, stray, and staying put - 1.14 saves
     * them with {@code PersistenceRequired}. Null when this mod adds no cats.
     */
    @Nullable
    public static EntityCat fromTemplate(World world, int type) {
        if (!CatContent.enabled()) {
            return null;
        }
        EntityCat cat = new EntityCat(world);
        cat.setCatType(type);
        cat.enablePersistence();
        return cat;
    }
}
