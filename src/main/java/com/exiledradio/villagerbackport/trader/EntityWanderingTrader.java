package com.exiledradio.villagerbackport.trader;

import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.IMerchant;
import net.minecraft.entity.INpc;
import net.minecraft.entity.ai.EntityAIAvoidEntity;
import net.minecraft.entity.ai.EntityAIMoveTowardsRestriction;
import net.minecraft.entity.ai.EntityAIPanic;
import net.minecraft.entity.ai.EntityAISwimming;
import net.minecraft.entity.ai.EntityAIWanderAvoidWater;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.ai.EntityAIWatchClosest2;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.monster.EntityEvoker;
import net.minecraft.entity.monster.EntityIllusionIllager;
import net.minecraft.entity.monster.EntityVex;
import net.minecraft.entity.monster.EntityVindicator;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.init.PotionTypes;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.potion.PotionUtils;
import net.minecraft.stats.StatList;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.village.MerchantRecipe;
import net.minecraft.village.MerchantRecipeList;
import net.minecraft.world.World;

import javax.annotation.Nullable;

/**
 * 1.14's wandering trader.
 *
 * <h2>What it is</h2>
 * A merchant with no profession, no levels and no village. It turns up near a player (see
 * {@link TraderSpawner}) with two {@link EntityTraderLlama}s on leads, sells a random nine trades from
 * {@link TraderTrades}, and leaves after forty minutes. It never restocks: a trade used up stays used
 * up.
 *
 * <h2>Why it is not an EntityVillager</h2>
 * 1.14 gives villagers and the trader a shared parent, {@code AbstractVillager}, and 1.12.2 has no such
 * class. Subclassing {@code EntityVillager} instead would hand the trader everything this mod and
 * vanilla attach to villagers - careers, beds, workstations, gossip, restocking, breeding, zombie
 * conversion - every piece of which would then have to be told to leave it alone. So it is an
 * {@link EntityAgeable} (1.14's {@code AgableMob}, the parent {@code AbstractVillager} itself has) that
 * implements {@link IMerchant} directly, and {@link TraderContent} points the villager hunters at it
 * separately.
 *
 * <h2>The trade screen</h2>
 * {@code EntityPlayerMP.displayVillagerTradeGui} takes any {@link IMerchant}, and the client opens the
 * same screen whatever is on the other end - which this mod already swaps for its 1.14-style one. The
 * trader sends no experience figures, so that screen shows no level and no experience bar, exactly as
 * 1.14's does for the trader. Nothing new crosses the network.
 *
 * <h2>Goals</h2>
 * 1.14.4's {@code WanderingTrader.registerGoals}, priority for priority:
 * <pre>
 *   0  swim
 *   0  drink an invisibility potion: at night, while visible        ({@link TraderGoals.UseItem})
 *   0  drink milk: by day, while invisible                          ({@link TraderGoals.UseItem})
 *   1  trade with the player                                        ({@link TraderGoals.TradeWithPlayer})
 *   1  avoid zombies 8, evokers 12, vindicators 8, vexes 8, pillagers 15, illusioners 12  (speed 0.5)
 *   1  panic when hurt (0.5)
 *   1  look at the customer                                         ({@link TraderGoals.LookAtCustomer})
 *   2  walk to the wander target, stopping within 2 blocks          ({@link TraderGoals.WanderToTarget})
 *   4  stay within 16 blocks of it once there (home distance)
 *   8  wander about (0.35)
 *   9  look at a nearby player (3 blocks)
 *  10  look at nearby mobs (8 blocks)
 * </pre>
 *
 * <h2>Leaving</h2>
 * {@code DespawnDelay} counts down by one each tick it is loaded and nobody is trading with it, and at
 * zero it is simply removed. A spawned trader starts at {@code trader.despawnTicks} (1.14: 48000); one
 * from a spawn egg or a command starts at 0, which means it never leaves. Nothing else despawns it:
 * {@link #canDespawn()} is false, so distance and a name tag make no difference, as in 1.14.
 */
public class EntityWanderingTrader extends EntityAgeable implements IMerchant, INpc {

    private static final String TAG_OFFERS = "Offers";
    private static final String TAG_DESPAWN = "DespawnDelay";
    private static final String TAG_TARGET = "WanderTarget";
    private static final String TAG_HOME = "TraderHome";
    private static final String TAG_HOME_RADIUS = "TraderHomeRadius";

    @Nullable
    private MerchantRecipeList offers;

    @Nullable
    private EntityPlayer customer;

    /** Where it was sent; cleared once it has arrived or been distracted from getting there. */
    @Nullable
    private BlockPos wanderTarget;

    private int despawnDelay;

    public EntityWanderingTrader(World world) {
        super(world);
        setSize(0.6F, 1.95F);
    }

    @Override
    protected void initEntityAI() {
        this.tasks.addTask(0, new EntityAISwimming(this));
        this.tasks.addTask(0, new TraderGoals.UseItem(this,
                PotionUtils.addPotionToItemStack(new ItemStack(Items.POTIONITEM), PotionTypes.INVISIBILITY),
                TraderGoals.UseItem.Sound.DISAPPEARED, true));
        this.tasks.addTask(0, new TraderGoals.UseItem(this, new ItemStack(Items.MILK_BUCKET),
                TraderGoals.UseItem.Sound.REAPPEARED, false));
        this.tasks.addTask(1, new TraderGoals.TradeWithPlayer(this));
        this.tasks.addTask(1, new EntityAIAvoidEntity<EntityZombie>(this, EntityZombie.class, 8.0F, 0.5D, 0.5D));
        this.tasks.addTask(1, new EntityAIAvoidEntity<EntityEvoker>(this, EntityEvoker.class, 12.0F, 0.5D, 0.5D));
        this.tasks.addTask(1, new EntityAIAvoidEntity<EntityVindicator>(this, EntityVindicator.class, 8.0F, 0.5D, 0.5D));
        this.tasks.addTask(1, new EntityAIAvoidEntity<EntityVex>(this, EntityVex.class, 8.0F, 0.5D, 0.5D));
        this.tasks.addTask(1, new EntityAIAvoidEntity<com.exiledradio.villagerbackport.illager.EntityPillager>(
                this, com.exiledradio.villagerbackport.illager.EntityPillager.class, 15.0F, 0.5D, 0.5D));
        this.tasks.addTask(1, new EntityAIAvoidEntity<EntityIllusionIllager>(this, EntityIllusionIllager.class, 12.0F, 0.5D, 0.5D));
        this.tasks.addTask(1, new EntityAIPanic(this, 0.5D));
        this.tasks.addTask(1, new TraderGoals.LookAtCustomer(this));
        this.tasks.addTask(2, new TraderGoals.WanderToTarget(this, 2.0D, 0.35D));
        this.tasks.addTask(4, new EntityAIMoveTowardsRestriction(this, 1.0D));
        this.tasks.addTask(8, new EntityAIWanderAvoidWater(this, 0.35D));
        this.tasks.addTask(9, new EntityAIWatchClosest2(this, EntityPlayer.class, 3.0F, 1.0F));
        this.tasks.addTask(10, new EntityAIWatchClosest(this, EntityLiving.class, 8.0F));
    }

    // --- spawning and leaving --------------------------------------------------------------------

    /** Sets the ticks until it leaves. 0 means never. */
    public void setDespawnDelay(int ticks) {
        this.despawnDelay = ticks;
    }

    public int getDespawnDelay() {
        return this.despawnDelay;
    }

    public void setWanderTarget(@Nullable BlockPos target) {
        this.wanderTarget = target;
    }

    @Nullable
    public BlockPos getWanderTarget() {
        return this.wanderTarget;
    }

    @Override
    protected boolean canDespawn() {
        return false;
    }

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (!this.world.isRemote) {
            tickDespawn();
        }
    }

    /**
     * 1.14's {@code maybeDespawn}. On the entity tick rather than in a goal: a goal's checks can go
     * unasked while another goal holds the mutex, and a timer kept there would stall.
     */
    void tickDespawn() {
        if (this.despawnDelay > 0 && !isTrading() && --this.despawnDelay == 0) {
            setDead();
        }
    }

    @Nullable
    @Override
    public EntityAgeable createChild(EntityAgeable other) {
        return null;
    }

    // --- trading ---------------------------------------------------------------------------------

    public boolean isTrading() {
        return this.customer != null;
    }

    /**
     * 1.14's {@code mobInteract}: a name tag names it; otherwise, if it has anything to sell and is
     * free, the trade screen opens. A spawn egg of its own kind is left to the egg, which makes a new
     * trader (there are no baby traders), and sneaking does nothing, as with villagers.
     */
    @Override
    public boolean processInteract(EntityPlayer player, EnumHand hand) {
        ItemStack held = player.getHeldItem(hand);
        if (held.getItem() == Items.NAME_TAG) {
            held.interactWithEntity(player, this, hand);
            return true;
        }
        if (held.getItem() == Items.SPAWN_EGG || !isEntityAlive() || isTrading() || player.isSneaking()) {
            return super.processInteract(player, hand);
        }

        if (hand == EnumHand.MAIN_HAND) {
            player.addStat(StatList.TALKED_TO_VILLAGER);
        }
        if (getOffers().isEmpty()) {
            return super.processInteract(player, hand);
        }
        if (!this.world.isRemote) {
            setCustomer(player);
            player.displayVillagerTradeGui(this);
        }
        return true;
    }

    /** The trades, rolled the first time anyone asks. */
    public MerchantRecipeList getOffers() {
        if (this.offers == null) {
            this.offers = TraderTrades.roll(this.rand);
        }
        return this.offers;
    }

    @Override
    public void setCustomer(@Nullable EntityPlayer player) {
        this.customer = player;
    }

    @Nullable
    @Override
    public EntityPlayer getCustomer() {
        return this.customer;
    }

    @Nullable
    @Override
    public MerchantRecipeList getRecipes(EntityPlayer player) {
        return getOffers();
    }

    /** Client side only: the trade list that arrived over the network. Never called on the trader. */
    @Override
    public void setRecipes(@Nullable MerchantRecipeList recipes) {
    }

    /**
     * A trade went through: count it, say yes, and drop 1.14's experience orbs - three to six,
     * unconditionally, since the trader has no levels to feed.
     */
    @Override
    public void useRecipe(MerchantRecipe recipe) {
        recipe.incrementToolUses();
        this.livingSoundTime = -getTalkInterval();
        playSound(TraderContent.yes, getSoundVolume(), getSoundPitch());
        if (recipe.getRewardsExp()) {
            this.world.spawnEntity(new EntityXPOrb(this.world, this.posX, this.posY + 0.5D, this.posZ,
                    3 + this.rand.nextInt(4)));
        }
    }

    /** 1.14's {@code notifyTradeUpdated}: a yes or a no as the result slot fills or empties. */
    @Override
    public void verifySellingItem(ItemStack stack) {
        if (!this.world.isRemote && this.livingSoundTime > -getTalkInterval() + 20) {
            this.livingSoundTime = -getTalkInterval();
            playSound(stack.isEmpty() ? TraderContent.no : TraderContent.yes, getSoundVolume(), getSoundPitch());
        }
    }

    @Override
    public World getWorld() {
        return this.world;
    }

    @Override
    public BlockPos getPos() {
        return new BlockPos(this);
    }

    // --- drinking --------------------------------------------------------------------------------

    /**
     * Plays the trader's own drinking sounds while it drinks, where 1.12.2 would play the generic one.
     *
     * <p>1.14 gives mobs a {@code getDrinkingSound} to override. 1.12.2 hard-codes the generic drink in
     * here, so this takes over the drinking case and leaves eating to the original.
     */
    @Override
    protected void updateItemUse(ItemStack stack, int eatingParticleCount) {
        if (!stack.isEmpty() && isHandActive() && stack.getItemUseAction() == EnumAction.DRINK) {
            SoundEvent sound = stack.getItem() == Items.MILK_BUCKET ? TraderContent.drinkMilk : TraderContent.drinkPotion;
            playSound(sound, 0.5F, this.world.rand.nextFloat() * 0.1F + 0.9F);
            return;
        }
        super.updateItemUse(stack, eatingParticleCount);
    }

    // --- sounds ----------------------------------------------------------------------------------

    @Override
    protected SoundEvent getAmbientSound() {
        return isTrading() ? TraderContent.trade : TraderContent.ambient;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return TraderContent.hurt;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return TraderContent.death;
    }

    /**
     * No loot, as 1.14's empty loot table. What the trader is holding when it dies - the potion or
     * the milk, mid-drink - still drops at the usual 8.5% equipment chance.
     */
    @Nullable
    @Override
    protected ResourceLocation getLootTable() {
        return null;
    }

    // --- saving ----------------------------------------------------------------------------------

    @Override
    public void writeEntityToNBT(NBTTagCompound compound) {
        super.writeEntityToNBT(compound);
        if (this.offers != null) {
            compound.setTag(TAG_OFFERS, this.offers.getRecipiesAsTags());
        }
        compound.setInteger(TAG_DESPAWN, this.despawnDelay);
        if (this.wanderTarget != null) {
            compound.setTag(TAG_TARGET, NBTUtil.createPosTag(this.wanderTarget));
        }
        // 1.12.2 does not save a mob's home, and without it a trader reloaded after reaching its
        // spot would wander off for good. 1.14 saves the restriction with the mob.
        if (hasHome()) {
            compound.setTag(TAG_HOME, NBTUtil.createPosTag(getHomePosition()));
            compound.setInteger(TAG_HOME_RADIUS, (int) getMaximumHomeDistance());
        }
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound compound) {
        super.readEntityFromNBT(compound);
        if (compound.hasKey(TAG_OFFERS, 10)) {
            this.offers = new MerchantRecipeList(compound.getCompoundTag(TAG_OFFERS));
        }
        if (compound.hasKey(TAG_DESPAWN, 99)) {
            this.despawnDelay = compound.getInteger(TAG_DESPAWN);
        }
        if (compound.hasKey(TAG_TARGET, 10)) {
            this.wanderTarget = NBTUtil.getPosFromTag(compound.getCompoundTag(TAG_TARGET));
        }
        if (compound.hasKey(TAG_HOME, 10)) {
            setHomePosAndDistance(NBTUtil.getPosFromTag(compound.getCompoundTag(TAG_HOME)),
                    compound.getInteger(TAG_HOME_RADIUS));
        }
        // There are no baby traders; a summon that asked for one gets an adult.
        setGrowingAge(Math.max(0, getGrowingAge()));
    }
}
