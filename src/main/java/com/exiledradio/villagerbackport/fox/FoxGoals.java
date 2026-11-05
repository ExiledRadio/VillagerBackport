package com.exiledradio.villagerbackport.fox;

import com.google.common.base.Predicate;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.passive.EntityChicken;
import net.minecraft.entity.passive.EntityRabbit;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.entity.passive.EntityWolf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.ai.EntityAIAttackMelee;
import net.minecraft.entity.ai.EntityAIAvoidEntity;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.EntityAIFollowParent;
import net.minecraft.entity.ai.EntityAIMoveThroughVillage;
import net.minecraft.entity.ai.EntityAIMoveToBlock;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.ai.EntityAIPanic;
import net.minecraft.entity.ai.EntityAISwimming;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.StatList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.village.Village;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.BabyEntitySpawnEvent;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/**
 * The fox's goals, one class each, after 1.14's {@code Fox} inner goals.
 *
 * <h2>Mutex bits</h2>
 * 1.12.2's goals exclude one another by bits where 1.14 uses flags: 1 is moving, 2 looking and 4
 * jumping or swimming. Each goal below carries the bits of the flags 1.14 gave it.
 *
 * <h2>Timers</h2>
 * Two goals count down inside {@code shouldExecute} as 1.14's do (sleep and shelter). That is only
 * safe because nothing that outranks them holds their bits for long - a goal blocked by a
 * higher-priority one is never asked, so its timer just pauses, which is the behavior wanted here.
 */
final class FoxGoals {

    private static final int MOVE = 1;
    private static final int LOOK = 2;
    private static final int JUMP = 4;

    private FoxGoals() {
    }

    // ------------------------------------------------------------------------------ shared tests

    /**
     * What wakes a fox, or keeps it awake: anything within 12 blocks that is not another fox, a tamed
     * pet, a player it trusts, a sneaking or sleeping one, or one in creative or spectator mode.
     */
    static boolean alertable(EntityFox fox, EntityLivingBase other) {
        if (other == fox || !other.isEntityAlive() || other instanceof EntityFox) {
            return false;
        }
        if (other instanceof EntityChicken || other instanceof EntityRabbit || other instanceof IMob) {
            return true;
        }
        if (other instanceof EntityTameable) {
            return !((EntityTameable) other).isTamed();
        }
        if (other instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) other;
            if (player.isSpectator() || player.isCreative() || player.isPlayerSleeping()) {
                return false;
            }
        }
        if (fox.trusts(other.getUniqueID())) {
            return false;
        }
        return !other.isSneaking();
    }

    static boolean anyAlertable(EntityFox fox) {
        List<EntityLivingBase> near = fox.world.getEntitiesWithinAABB(EntityLivingBase.class,
                fox.getEntityBoundingBox().grow(12.0D, 6.0D, 12.0D),
                other -> other != null && fox.getDistanceSq(other) <= 144.0D && alertable(fox, other));
        return !near.isEmpty();
    }

    /** Somewhere to sleep: out of the sky, on ground an animal would walk on. */
    static boolean hasShelter(EntityFox fox) {
        BlockPos pos = new BlockPos(fox.posX, fox.getEntityBoundingBox().minY, fox.posZ);
        return !fox.world.canSeeSky(pos) && fox.getBlockPathWeight(pos) >= 0.0F;
    }

    /** Prey worth stalking: chickens and rabbits. */
    static boolean stalkable(@Nullable Entity entity) {
        return entity instanceof EntityChicken || entity instanceof EntityRabbit;
    }

    // ------------------------------------------------------------------------------ priority 0-3

    /** Swims, and drops every pose on entering water. */
    static final class Float extends EntityAISwimming {
        private final EntityFox fox;

        Float(EntityFox fox) {
            super(fox);
            this.fox = fox;
        }

        @Override
        public void startExecuting() {
            super.startExecuting();
            fox.clearStates();
        }
    }

    /** Two seconds with its face in the snow after a pounce lands in a snow layer. */
    static final class Faceplant extends EntityAIBase {
        private final EntityFox fox;
        private int countdown;

        Faceplant(EntityFox fox) {
            this.fox = fox;
            setMutexBits(MOVE | LOOK | JUMP);
        }

        @Override
        public boolean shouldExecute() {
            return fox.isFaceplanted();
        }

        @Override
        public boolean shouldContinueExecuting() {
            return shouldExecute() && countdown > 0;
        }

        @Override
        public void startExecuting() {
            countdown = 40;
        }

        @Override
        public void resetTask() {
            fox.setFaceplanted(false);
        }

        @Override
        public void updateTask() {
            --countdown;
        }
    }

    static final class Panic extends EntityAIPanic {
        private final EntityFox fox;

        Panic(EntityFox fox, double speed) {
            super(fox, speed);
            this.fox = fox;
        }

        @Override
        public boolean shouldExecute() {
            return !fox.isDefending() && super.shouldExecute();
        }
    }

    /**
     * Running from players (that are not sneaking, not trusted, and not in creative or spectator),
     * untamed wolves and polar bears - unless the fox is defending its player.
     */
    static final class Avoid<T extends Entity> extends EntityAIAvoidEntity<T> {

        Avoid(final EntityFox fox, Class<T> type, float distance, double farSpeed, double nearSpeed) {
            super(fox, type, new Predicate<T>() {
                @Override
                public boolean apply(@Nullable T entity) {
                    if (entity == null || fox.isDefending()) {
                        return false;
                    }
                    if (entity instanceof EntityPlayer) {
                        EntityPlayer player = (EntityPlayer) entity;
                        return !player.isSneaking() && !player.isCreative() && !player.isSpectator()
                                && !fox.trusts(player.getUniqueID());
                    }
                    if (entity instanceof EntityWolf) {
                        return !((EntityWolf) entity).isTamed();
                    }
                    return true;
                }
            }, distance, farSpeed, nearSpeed);
        }
    }

    // ------------------------------------------------------------------------------ priority 4-5

    /** Walking up on prey, then crouching within 6 blocks, ready to pounce. */
    static final class StalkPrey extends EntityAIBase {
        private final EntityFox fox;

        StalkPrey(EntityFox fox) {
            this.fox = fox;
            setMutexBits(MOVE | LOOK);
        }

        @Override
        public boolean shouldExecute() {
            if (fox.isSleeping()) {
                return false;
            }
            EntityLivingBase target = fox.getAttackTarget();
            return target != null && target.isEntityAlive() && stalkable(target) && fox.getDistanceSq(target) > 36.0D
                    && !fox.isCrouching() && !fox.isInterested() && !fox.jumping();
        }

        @Override
        public void startExecuting() {
            fox.setSitting(false);
            fox.setFaceplanted(false);
        }

        @Override
        public void resetTask() {
            EntityLivingBase target = fox.getAttackTarget();
            if (target != null && EntityFox.isPathClear(fox, target)) {
                fox.setInterested(true);
                fox.setCrouching(true);
                fox.getNavigator().clearPath();
                fox.getLookHelper().setLookPositionWithEntity(target, fox.getHorizontalFaceSpeed(),
                        fox.getVerticalFaceSpeed());
            } else {
                fox.setInterested(false);
                fox.setCrouching(false);
            }
        }

        @Override
        public void updateTask() {
            EntityLivingBase target = fox.getAttackTarget();
            if (target == null) {
                return;
            }
            fox.getLookHelper().setLookPositionWithEntity(target, fox.getHorizontalFaceSpeed(), fox.getVerticalFaceSpeed());
            if (fox.getDistanceSq(target) <= 36.0D) {
                fox.setInterested(true);
                fox.setCrouching(true);
                fox.getNavigator().clearPath();
            } else {
                fox.getNavigator().tryMoveToEntityLiving(target, 1.5D);
            }
        }
    }

    /**
     * The pounce: from a full crouch with nothing overhead, a leap of 0.9 up and 0.8 across toward the
     * prey, biting it on the way down if it is within 2 blocks. Landing in a snow layer face first
     * ends it in a face-plant. Cannot be interrupted once airborne.
     */
    static final class Pounce extends EntityAIBase {
        private final EntityFox fox;

        Pounce(EntityFox fox) {
            this.fox = fox;
            setMutexBits(MOVE | JUMP);
        }

        @Override
        public boolean shouldExecute() {
            if (!fox.isFullyCrouched()) {
                return false;
            }
            EntityLivingBase target = fox.getAttackTarget();
            if (target == null || !target.isEntityAlive()) {
                return false;
            }
            boolean clear = EntityFox.isPathClear(fox, target);
            if (!clear) {
                fox.setCrouching(false);
                fox.setInterested(false);
            }
            return clear;
        }

        @Override
        public boolean shouldContinueExecuting() {
            EntityLivingBase target = fox.getAttackTarget();
            if (target == null || !target.isEntityAlive()) {
                return false;
            }
            double vy = fox.motionY;
            return (vy * vy >= 0.05D || Math.abs(fox.rotationPitch) >= 15.0F || !fox.onGround) && !fox.isFaceplanted();
        }

        @Override
        public boolean isInterruptible() {
            return false;
        }

        @Override
        public void startExecuting() {
            fox.setPouncing(true);
            fox.setInterested(false);
            EntityLivingBase target = fox.getAttackTarget();
            fox.getLookHelper().setLookPositionWithEntity(target, 60.0F, 30.0F);
            Vec3d dir = new Vec3d(target.posX - fox.posX, target.posY - fox.posY, target.posZ - fox.posZ).normalize();
            fox.motionX += dir.x * 0.8D;
            fox.motionY += 0.9D;
            fox.motionZ += dir.z * 0.8D;
            fox.getNavigator().clearPath();
        }

        @Override
        public void resetTask() {
            fox.setCrouching(false);
            fox.resetCrouch();
            fox.setInterested(false);
            fox.setPouncing(false);
        }

        @Override
        public void updateTask() {
            EntityLivingBase target = fox.getAttackTarget();
            if (target != null) {
                fox.getLookHelper().setLookPositionWithEntity(target, 60.0F, 30.0F);
            }
            if (!fox.isFaceplanted()) {
                double vy = fox.motionY;
                if (vy * vy < 0.03D && fox.rotationPitch != 0.0F) {
                    fox.rotationPitch += (0.0F - fox.rotationPitch) * 0.2F;
                } else {
                    double horizontal = Math.sqrt(fox.motionX * fox.motionX + fox.motionZ * fox.motionZ);
                    double length = Math.sqrt(horizontal * horizontal + vy * vy);
                    if (length > 0.0D) {
                        fox.rotationPitch = (float) (Math.signum(-vy) * Math.acos(horizontal / length) * 57.2957763671875D);
                    }
                }
            }
            if (target != null && fox.getDistance(target) <= 2.0F) {
                fox.attackEntityAsMob(target);
            } else if (fox.rotationPitch > 0.0F && fox.onGround && (float) fox.motionY != 0.0F
                    && fox.world.getBlockState(new BlockPos(fox)).getBlock() == Blocks.SNOW_LAYER) {
                fox.rotationPitch = 60.0F;
                fox.setAttackTarget(null);
                fox.setFaceplanted(true);
            }
        }
    }

    /**
     * Breeding on sweet berries, as {@code EntityAIMate} does, plus 1.14's trust: the cub trusts the
     * players who fed each parent.
     */
    static final class Breed extends EntityAIBase {
        private final EntityFox fox;
        private final double speed;
        private EntityFox mate;
        private int delay;

        Breed(EntityFox fox, double speed) {
            this.fox = fox;
            this.speed = speed;
            setMutexBits(MOVE | LOOK);
        }

        @Override
        public boolean shouldExecute() {
            if (!fox.isInLove()) {
                return false;
            }
            mate = nearestMate();
            return mate != null;
        }

        @Override
        public boolean shouldContinueExecuting() {
            return mate != null && mate.isEntityAlive() && mate.isInLove() && delay < 60;
        }

        @Override
        public void startExecuting() {
            fox.clearStates();
            mate.clearStates();
        }

        @Override
        public void resetTask() {
            mate = null;
            delay = 0;
        }

        @Override
        public void updateTask() {
            fox.getLookHelper().setLookPositionWithEntity(mate, 10.0F, fox.getVerticalFaceSpeed());
            fox.getNavigator().tryMoveToEntityLiving(mate, speed);
            ++delay;
            if (delay >= 60 && fox.getDistanceSq(mate) < 9.0D) {
                spawnCub();
            }
        }

        @Nullable
        private EntityFox nearestMate() {
            EntityFox best = null;
            double bestDistance = Double.MAX_VALUE;
            for (EntityFox other : fox.world.getEntitiesWithinAABB(EntityFox.class, fox.getEntityBoundingBox().grow(8.0D))) {
                double distance = fox.getDistanceSq(other);
                if (fox.canMateWith(other) && distance < bestDistance) {
                    best = other;
                    bestDistance = distance;
                }
            }
            return best;
        }

        private void spawnCub() {
            World world = fox.world;
            EntityFox cub = fox.createChild(mate);
            BabyEntitySpawnEvent event = new BabyEntitySpawnEvent(fox, mate, cub);
            boolean cancelled = MinecraftForge.EVENT_BUS.post(event);
            if (cancelled || event.getChild() == null) {
                fox.setGrowingAge(6000);
                mate.setGrowingAge(6000);
                fox.resetInLove();
                mate.resetInLove();
                return;
            }
            EntityPlayerMP first = fox.getLoveCause();
            EntityPlayerMP second = mate.getLoveCause();
            if (event.getChild() instanceof EntityFox) {
                cub = (EntityFox) event.getChild();
                if (first != null) {
                    cub.addTrustedUUID(first.getUniqueID());
                }
                if (second != null && second != first) {
                    cub.addTrustedUUID(second.getUniqueID());
                }
            }
            EntityPlayerMP breeder = first != null ? first : second;
            if (breeder != null) {
                breeder.addStat(StatList.ANIMALS_BRED);
                CriteriaTriggers.BRED_ANIMALS.trigger(breeder, fox, mate, event.getChild());
            }
            fox.setGrowingAge(6000);
            mate.setGrowingAge(6000);
            fox.resetInLove();
            mate.resetInLove();
            event.getChild().setGrowingAge(-24000);
            event.getChild().setLocationAndAngles(fox.posX, fox.posY, fox.posZ, 0.0F, 0.0F);
            world.spawnEntity(event.getChild());
            world.setEntityState(fox, (byte) 18);
            if (world.getGameRules().getBoolean("doMobLoot")) {
                world.spawnEntity(new EntityXPOrb(world, fox.posX, fox.posY, fox.posZ, fox.getRNG().nextInt(7) + 1));
            }
        }
    }

    /**
     * By day, a fox in the open looks for somewhere out of the sky within 10 blocks, every five seconds
     * - and at once in a thunderstorm. Not in a village, where foxes wander through at night instead.
     */
    static final class SeekShelter extends EntityAIBase {
        private final EntityFox fox;
        private final double speed;
        private int interval = 100;
        private double x;
        private double y;
        private double z;

        SeekShelter(EntityFox fox, double speed) {
            this.fox = fox;
            this.speed = speed;
            setMutexBits(MOVE);
        }

        @Override
        public boolean shouldExecute() {
            if (fox.isSleeping() || fox.getAttackTarget() != null) {
                return false;
            }
            World world = fox.world;
            BlockPos pos = new BlockPos(fox.posX, fox.getEntityBoundingBox().minY, fox.posZ);
            if (world.isThundering() && world.canSeeSky(pos)) {
                return findShelter(pos);
            }
            if (interval > 0) {
                --interval;
                return false;
            }
            interval = 100;
            return world.isDaytime() && world.canSeeSky(pos) && !inVillage(world, pos) && findShelter(pos);
        }

        @Override
        public boolean shouldContinueExecuting() {
            return !fox.getNavigator().noPath();
        }

        @Override
        public void startExecuting() {
            fox.clearStates();
            fox.getNavigator().tryMoveToXYZ(x, y, z, speed);
        }

        private boolean findShelter(BlockPos from) {
            for (int i = 0; i < 10; i++) {
                BlockPos at = from.add(fox.getRNG().nextInt(20) - 10, fox.getRNG().nextInt(6) - 3,
                        fox.getRNG().nextInt(20) - 10);
                if (!fox.world.canSeeSky(at) && fox.world.isAirBlock(at)
                        && fox.world.getBlockState(at.down()).isSideSolid(fox.world, at.down(), net.minecraft.util.EnumFacing.UP)) {
                    x = at.getX() + 0.5D;
                    y = at.getY();
                    z = at.getZ() + 0.5D;
                    return true;
                }
            }
            return false;
        }

        private static boolean inVillage(World world, BlockPos pos) {
            Village village = world.getVillageCollection().getNearestVillage(pos, 32);
            return village != null && village.isBlockPosWithinSqVillageRadius(pos);
        }
    }

    // ------------------------------------------------------------------------------ priority 6-8

    /** The bite. Not from a sitting, sleeping, crouching or face-planted fox. */
    static final class Bite extends EntityAIAttackMelee {
        private final EntityFox fox;

        Bite(EntityFox fox, double speed) {
            super(fox, speed, true);
            this.fox = fox;
        }

        @Override
        public boolean shouldExecute() {
            return !fox.isSitting() && !fox.isSleeping() && !fox.isCrouching() && !fox.isFaceplanted()
                    && super.shouldExecute();
        }

        @Override
        public void startExecuting() {
            fox.setInterested(false);
            super.startExecuting();
        }

        @Override
        protected void checkAndPerformAttack(EntityLivingBase enemy, double distanceSq) {
            if (distanceSq <= getAttackReachSqr(enemy) && attackTick <= 0) {
                attackTick = 20;
                fox.attackEntityAsMob(enemy);
                fox.playSound(FoxContent.sound("bite"), 1.0F, 1.0F);
            }
        }
    }

    /**
     * Sleeping through the day: once standing still, with no sky overhead and nothing alarming within
     * 12 blocks, after a random wait of up to 7 seconds.
     */
    static final class Sleep extends EntityAIBase {
        private final EntityFox fox;
        private int countdown;

        Sleep(EntityFox fox) {
            this.fox = fox;
            this.countdown = fox.getRNG().nextInt(140);
            setMutexBits(MOVE | LOOK | JUMP);
        }

        @Override
        public boolean shouldExecute() {
            if (fox.moveStrafing == 0.0F && fox.moveVertical == 0.0F && fox.moveForward == 0.0F) {
                return canSleep() || fox.isSleeping();
            }
            return false;
        }

        @Override
        public boolean shouldContinueExecuting() {
            return canSleep();
        }

        private boolean canSleep() {
            if (countdown > 0) {
                --countdown;
                return false;
            }
            return fox.world.isDaytime() && hasShelter(fox) && !anyAlertable(fox);
        }

        @Override
        public void resetTask() {
            countdown = fox.getRNG().nextInt(140);
            fox.clearStates();
        }

        @Override
        public void startExecuting() {
            fox.setSitting(false);
            fox.setCrouching(false);
            fox.setInterested(false);
            fox.setJumping(false);
            fox.setSleeping(true);
            fox.getNavigator().clearPath();
            fox.getMoveHelper().setMoveTo(fox.posX, fox.posY, fox.posZ, 0.0D);
        }
    }

    static final class FollowParent extends EntityAIFollowParent {
        private final EntityFox fox;

        FollowParent(EntityFox fox, double speed) {
            super(fox, speed);
            this.fox = fox;
        }

        @Override
        public boolean shouldExecute() {
            return !fox.isDefending() && super.shouldExecute();
        }

        @Override
        public boolean shouldContinueExecuting() {
            return !fox.isDefending() && super.shouldContinueExecuting();
        }

        @Override
        public void startExecuting() {
            fox.clearStates();
            super.startExecuting();
        }
    }

    /**
     * Wandering through a village at night. 1.14 tries now and then rather than every tick, and so
     * does this - the underlying 1.12.2 goal plans a path each time it is asked.
     */
    static final class StrollThroughVillage extends EntityAIMoveThroughVillage {
        private final EntityFox fox;

        StrollThroughVillage(EntityFox fox) {
            super(fox, 1.0D, true);
            this.fox = fox;
        }

        private boolean canFoxMove() {
            return fox.canMove() && !fox.isDefending() && fox.getAttackTarget() == null && !fox.isCrouching();
        }

        @Override
        public boolean shouldExecute() {
            return fox.getRNG().nextInt(200) == 0 && canFoxMove() && super.shouldExecute();
        }

        @Override
        public boolean shouldContinueExecuting() {
            return canFoxMove() && super.shouldContinueExecuting();
        }

        @Override
        public void startExecuting() {
            fox.clearStates();
            super.startExecuting();
        }
    }

    // ------------------------------------------------------------------------------ priority 9-12

    /**
     * Walking to a ripe sweet berry bush within 12 blocks, sniffing on the way, and picking it after
     * two seconds there. Either mod's bush will do.
     *
     * <p>1.12.2's goal targets the block a mob stands on, so the destination is the ground under the
     * bush and the bush is the block above it.
     */
    static final class EatBerries extends EntityAIMoveToBlock {
        private final EntityFox fox;
        private int ticksWaited;

        EatBerries(EntityFox fox, double speed, int range) {
            super(fox, speed, range);
            this.fox = fox;
        }

        @Override
        public boolean shouldExecute() {
            return !fox.isSleeping() && super.shouldExecute();
        }

        @Override
        public void startExecuting() {
            ticksWaited = 0;
            fox.setSitting(false);
            super.startExecuting();
        }

        @Override
        protected boolean shouldMoveTo(World world, BlockPos ground) {
            return ripeAge(world.getBlockState(ground.up())) >= 2;
        }

        @Override
        public void updateTask() {
            super.updateTask();
            if (getIsAboveDestination()) {
                if (ticksWaited >= 40) {
                    eat();
                } else {
                    ++ticksWaited;
                }
            } else if (fox.getRNG().nextFloat() < 0.05F) {
                fox.playSound(FoxContent.sound("sniff"), 1.0F, 1.0F);
            }
        }

        private void eat() {
            World world = fox.world;
            if (!net.minecraftforge.event.ForgeEventFactory.getMobGriefingEvent(world, fox)) {
                return;
            }
            BlockPos at = destinationBlock.up();
            IBlockState state = world.getBlockState(at);
            boolean mouthEmpty = fox.getHeldItemMainhand().isEmpty();
            ItemStack mouth;
            if (state.getBlock() == FoxContent.bush) {
                mouth = BlockSweetBerryBush.pickForFox(world, at, state, mouthEmpty);
            } else {
                mouth = pickOther(world, at, state, mouthEmpty);
            }
            if (!mouth.isEmpty()) {
                fox.takeInMouth(mouth);
            }
        }

        /** The same picking, for another mod's bush with an {@code age} of 0 to 3. */
        private ItemStack pickOther(World world, BlockPos at, IBlockState state, boolean mouthEmpty) {
            PropertyInteger age = ageProperty(state);
            Item berries = FoxContent.futureMcBerries();
            int value = ripeAge(state);
            if (age == null || berries == null || value < 2) {
                return ItemStack.EMPTY;
            }
            int count = 1 + world.rand.nextInt(2) + (value == 3 ? 1 : 0);
            ItemStack mouth = ItemStack.EMPTY;
            if (mouthEmpty) {
                mouth = new ItemStack(berries);
                count--;
            }
            if (count > 0) {
                Block.spawnAsEntity(world, at, new ItemStack(berries, count));
            }
            world.playSound(null, at, FoxContent.sound("pick"), SoundCategory.BLOCKS, 1.0F, 1.0F);
            world.setBlockState(at, state.withProperty(age, 1), 2);
            return mouth;
        }
    }

    private static final ResourceLocation FUTURE_MC_BUSH = new ResourceLocation("futuremc", "sweet_berry_bush");

    /** @return a sweet berry bush's age, from either mod, or -1 for anything else */
    static int ripeAge(IBlockState state) {
        Block block = state.getBlock();
        if (block == FoxContent.bush && block != null) {
            return state.getValue(BlockSweetBerryBush.AGE);
        }
        if (FUTURE_MC_BUSH.equals(block.getRegistryName())) {
            PropertyInteger age = ageProperty(state);
            return age == null ? -1 : state.getValue(age);
        }
        return -1;
    }

    @Nullable
    private static PropertyInteger ageProperty(IBlockState state) {
        for (IProperty<?> property : state.getPropertyKeys()) {
            if ("age".equals(property.getName()) && property instanceof PropertyInteger) {
                return (PropertyInteger) property;
            }
        }
        return null;
    }

    /**
     * Trotting over to an item within 8 blocks, now and then, when the mouth is empty and nothing is
     * chasing or being chased. Picking it up is the pick-up-loot check every mob has.
     */
    static final class SearchForItems extends EntityAIBase {
        private final EntityFox fox;

        SearchForItems(EntityFox fox) {
            this.fox = fox;
            setMutexBits(MOVE);
        }

        private List<EntityItem> items() {
            return fox.world.getEntitiesWithinAABB(EntityItem.class, fox.getEntityBoundingBox().grow(8.0D),
                    item -> item != null && !item.cannotPickup() && item.isEntityAlive());
        }

        @Override
        public boolean shouldExecute() {
            if (!fox.getHeldItemMainhand().isEmpty() || fox.getAttackTarget() != null || fox.getRevengeTarget() != null
                    || !fox.canMove() || fox.getRNG().nextInt(10) != 0) {
                return false;
            }
            return !items().isEmpty();
        }

        @Override
        public void startExecuting() {
            List<EntityItem> items = items();
            if (!items.isEmpty()) {
                fox.getNavigator().tryMoveToEntityLiving(items.get(0), 1.2D);
            }
        }

        @Override
        public void updateTask() {
            List<EntityItem> items = items();
            if (fox.getHeldItemMainhand().isEmpty() && !items.isEmpty()) {
                fox.getNavigator().tryMoveToEntityLiving(items.get(0), 1.2D);
            }
        }
    }

    static final class LookAtPlayer extends EntityAIWatchClosest {
        private final EntityFox fox;

        LookAtPlayer(EntityFox fox, float distance) {
            super(fox, EntityPlayer.class, distance);
            this.fox = fox;
        }

        @Override
        public boolean shouldExecute() {
            return super.shouldExecute() && !fox.isFaceplanted() && !fox.isInterested();
        }

        @Override
        public boolean shouldContinueExecuting() {
            return super.shouldContinueExecuting() && !fox.isFaceplanted() && !fox.isInterested();
        }
    }

    /** Sitting down for a few seconds now and then, looking around. */
    static final class PerchAndSearch extends EntityAIBase {
        private final EntityFox fox;
        private double relX;
        private double relZ;
        private int lookTime;
        private int looksRemaining;

        PerchAndSearch(EntityFox fox) {
            this.fox = fox;
            setMutexBits(MOVE | LOOK);
        }

        @Override
        public boolean shouldExecute() {
            return fox.getRevengeTarget() == null && fox.getRNG().nextFloat() < 0.02F && !fox.isSleeping()
                    && fox.getAttackTarget() == null && fox.getNavigator().noPath() && !anyAlertable(fox)
                    && !fox.isPouncing() && !fox.isCrouching();
        }

        @Override
        public boolean shouldContinueExecuting() {
            return looksRemaining > 0;
        }

        @Override
        public void startExecuting() {
            resetLook();
            looksRemaining = 2 + fox.getRNG().nextInt(3);
            fox.setSitting(true);
            fox.getNavigator().clearPath();
        }

        @Override
        public void resetTask() {
            fox.setSitting(false);
        }

        @Override
        public void updateTask() {
            --lookTime;
            if (lookTime <= 0) {
                --looksRemaining;
                resetLook();
            }
            fox.getLookHelper().setLookPosition(fox.posX + relX, fox.posY + fox.getEyeHeight(), fox.posZ + relZ,
                    fox.getHorizontalFaceSpeed(), fox.getVerticalFaceSpeed());
        }

        private void resetLook() {
            double angle = Math.PI * 2.0D * fox.getRNG().nextDouble();
            relX = Math.cos(angle);
            relZ = Math.sin(angle);
            lookTime = 80 + fox.getRNG().nextInt(20);
        }
    }

    // ------------------------------------------------------------------------------ targets

    /**
     * Going after whatever last hurt a trusted player (or any trusted mob), unless it is trusted itself.
     * Checked one tick in ten, and once per hit.
     *
     * <p>1.14 also asks that the attacker has hurt something lately. 1.12.2 only records that for
     * players - a mob's own attacks never set it - so here the hit on the trusted player is that
     * evidence, as it always is in 1.14 too.
     */
    static final class DefendTrusted extends EntityAINearestAttackableTarget<EntityLivingBase> {
        private final EntityFox fox;
        private EntityLivingBase trustedLastHurtBy;
        private EntityLivingBase trustedLastHurt;
        private int timestamp;

        DefendTrusted(final EntityFox fox) {
            super(fox, EntityLivingBase.class, 10, false, false, null);
            this.fox = fox;
        }

        private boolean worthFighting(EntityLivingBase entity) {
            return entity != fox && !fox.trusts(entity.getUniqueID());
        }

        @Override
        public boolean shouldExecute() {
            if (fox.getRNG().nextInt(10) != 0 || !(fox.world instanceof WorldServer)) {
                return false;
            }
            for (UUID uuid : fox.getTrustedUUIDs()) {
                Entity entity = ((WorldServer) fox.world).getEntityFromUuid(uuid);
                if (!(entity instanceof EntityLivingBase)) {
                    continue;
                }
                EntityLivingBase trusted = (EntityLivingBase) entity;
                trustedLastHurt = trusted;
                trustedLastHurtBy = trusted.getRevengeTarget();
                int time = trusted.getRevengeTimer();
                return time != timestamp && trustedLastHurtBy != null && worthFighting(trustedLastHurtBy)
                        && isSuitableTarget(trustedLastHurtBy, false);
            }
            return false;
        }

        @Override
        public void startExecuting() {
            targetEntity = trustedLastHurtBy;
            fox.setAttackTarget(trustedLastHurtBy);
            if (trustedLastHurt != null) {
                timestamp = trustedLastHurt.getRevengeTimer();
            }
            fox.playSound(FoxContent.sound("aggro"), 1.0F, 1.0F);
            fox.setDefending(true);
            fox.wakeUp();
            super.startExecuting();
        }
    }
}
