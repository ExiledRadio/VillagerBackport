package com.exiledradio.villagerbackport.cat;

import com.exiledradio.villagerbackport.home.HomeSite;

import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.loot.LootContext;
import net.minecraft.world.storage.loot.LootTable;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.List;

/**
 * A tamed cat curling up on the bed its owner is sleeping in, and its morning gift. 1.14's
 * {@code CatRelaxOnOwnerGoal}.
 *
 * <h2>The night</h2>
 * When its owner is asleep within ten blocks, the cat makes for the foot of their bed, unless another
 * cat is already settled there. Close to them it first looks at them with its head lowered, then
 * after a little under a second lies down. It holds no mutex - it runs alongside whatever else the
 * cat is doing, as in 1.14.
 *
 * <h2>The morning</h2>
 * When its owner gets up, if they slept the night through (a sleep timer at its full 100) and it is
 * now dawn - 1.14 checks the sun is between 0.77 and 0.8 of the way round - the cat has seven chances in
 * ten of leaving them a present: it hops to a random spot within five blocks and drops one roll of
 * {@code gameplay/cat_morning_gift}.
 *
 * <p>The table is 1.14's but for its phantom membrane, which 1.12.2 lacks. Its weight (2 in 62) is
 * kept here instead: when it comes up and some mod has a phantom membrane, that is the gift; when no
 * mod does, the table is rolled as usual.
 *
 * <p>The gift is decided in {@link #resetTask}, which runs whenever this goal ends; that is safe to
 * rely on because the goal has no mutex, so nothing can stop it without ending it.
 */
public class EntityAICatRelaxOnOwner extends EntityAIBase {

    private static final int MEMBRANE_WEIGHT = 2;
    private static final int TABLE_WEIGHT = 62;

    @Nullable
    private static Item membrane;
    private static boolean membraneLooked;

    private final EntityCat cat;
    @Nullable
    private EntityPlayer owner;
    @Nullable
    private BlockPos goalPos;
    private int onBedTicks;

    public EntityAICatRelaxOnOwner(EntityCat cat) {
        this.cat = cat;
        this.setMutexBits(0);
    }

    @Override
    public boolean shouldExecute() {
        if (!this.cat.isTamed() || this.cat.isSitting()) {
            return false;
        }
        EntityLivingBase found = this.cat.getOwner();
        if (!(found instanceof EntityPlayer)) {
            return false;
        }
        this.owner = (EntityPlayer) found;
        if (!this.owner.isPlayerSleeping() || this.cat.getDistanceSq(this.owner) > 100.0D) {
            return false;
        }

        BlockPos bed = new BlockPos(this.owner);
        IBlockState state = this.cat.world.getBlockState(bed);
        if (!HomeSite.isBed(state) || !state.getPropertyKeys().contains(BlockHorizontal.FACING)) {
            return false;
        }
        // The foot of the bed: the head faces the way FACING points.
        EnumFacing facing = state.getValue(BlockHorizontal.FACING);
        this.goalPos = bed.offset(facing.getOpposite());
        return !spaceIsOccupied();
    }

    /** Another cat already lying or settling at the foot of this bed. */
    private boolean spaceIsOccupied() {
        List<EntityCat> cats = this.cat.world.getEntitiesWithinAABB(EntityCat.class, new AxisAlignedBB(this.goalPos).grow(2.0D));
        for (EntityCat other : cats) {
            if (other != this.cat && (other.isLying() || other.isRelaxed())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean shouldContinueExecuting() {
        return this.cat.isTamed() && !this.cat.isSitting() && this.owner != null && this.owner.isPlayerSleeping()
                && this.goalPos != null && !spaceIsOccupied();
    }

    @Override
    public void startExecuting() {
        if (this.goalPos != null) {
            this.cat.orderSit(false);
            this.cat.getNavigator().tryMoveToXYZ(this.goalPos.getX(), this.goalPos.getY(), this.goalPos.getZ(), 1.1D);
        }
    }

    @Override
    public void updateTask() {
        if (this.owner == null || this.goalPos == null) {
            return;
        }
        this.cat.orderSit(false);
        this.cat.getNavigator().tryMoveToXYZ(this.goalPos.getX(), this.goalPos.getY(), this.goalPos.getZ(), 1.1D);
        if (this.cat.getDistanceSq(this.owner) < 2.5D) {
            this.onBedTicks++;
            if (this.onBedTicks > 16) {
                this.cat.setLying(true);
                this.cat.setRelaxed(false);
            } else {
                this.cat.getLookHelper().setLookPositionWithEntity(this.owner, 45.0F, 45.0F);
                this.cat.setRelaxed(true);
            }
        } else {
            this.cat.setLying(false);
        }
    }

    @Override
    public void resetTask() {
        this.cat.setLying(false);
        float sun = this.cat.world.getCelestialAngle(1.0F);
        if (this.owner != null && this.owner.getSleepTimer() >= 100 && sun > 0.77F && sun < 0.8F
                && this.cat.world.rand.nextFloat() < 0.7F) {
            giveMorningGift();
        }
        this.onBedTicks = 0;
        this.cat.setRelaxed(false);
        this.cat.getNavigator().clearPath();
    }

    /** Public so the headless test can call it directly; the goal is the only caller in play. */
    public void giveMorningGift() {
        giveMorningGift(this.cat);
    }

    static void giveMorningGift(EntityCat cat) {
        if (!(cat.world instanceof WorldServer)) {
            return;
        }
        WorldServer world = (WorldServer) cat.world;
        BlockPos at = new BlockPos(cat);
        cat.attemptTeleport(at.getX() + cat.getRNG().nextInt(11) - 5, at.getY() + cat.getRNG().nextInt(5) - 2,
                at.getZ() + cat.getRNG().nextInt(11) - 5);
        at = new BlockPos(cat);

        Item phantomMembrane = membrane();
        if (phantomMembrane != null && cat.getRNG().nextInt(TABLE_WEIGHT) < MEMBRANE_WEIGHT) {
            drop(cat, at, new ItemStack(phantomMembrane));
            return;
        }

        LootTable table = world.getLootTableManager().getLootTableFromLocation(EntityCat.MORNING_GIFT);
        LootContext context = new LootContext.Builder(world).withLootedEntity(cat).build();
        for (ItemStack stack : table.generateLootForPools(cat.getRNG(), context)) {
            drop(cat, at, stack);
        }
    }

    private static void drop(EntityCat cat, BlockPos at, ItemStack stack) {
        float yaw = cat.renderYawOffset * 0.017453292F;
        cat.world.spawnEntity(new EntityItem(cat.world, at.getX() - MathHelper.sin(yaw), at.getY(),
                at.getZ() + MathHelper.cos(yaw), stack));
    }

    /** Any mod's phantom membrane - Future MC's, or another backport's - found once. */
    @Nullable
    private static Item membrane() {
        if (!membraneLooked) {
            membraneLooked = true;
            for (ResourceLocation id : ForgeRegistries.ITEMS.getKeys()) {
                if ("phantom_membrane".equals(id.getPath())) {
                    membrane = ForgeRegistries.ITEMS.getValue(id);
                    break;
                }
            }
        }
        return membrane;
    }
}
