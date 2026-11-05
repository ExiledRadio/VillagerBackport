package com.exiledradio.villagerbackport.raid;

import com.exiledradio.villagerbackport.ModConfig;
import com.exiledradio.villagerbackport.VillagerBackport;
import com.exiledradio.villagerbackport.home.HomeSite;
import com.exiledradio.villagerbackport.job.Employment;
import com.exiledradio.villagerbackport.restock.WorkSounds;

import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.loot.LootContext;
import net.minecraft.world.storage.loot.LootTable;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

/**
 * A villager giving a Hero of the Village something: 1.14's {@code GiveHeroGiftsTask}.
 *
 * <p>A hero in sight gets walked up to and, once within five blocks and a second has passed, thrown
 * a gift - what its profession's loot table says, a poppy from a baby, wheat seeds from a villager
 * with no job. Then nothing more for 30 seconds to five minutes and a half, as in 1.14.
 *
 * <p>With Raids Backport running, its villagers already give gifts. This only gives them for the
 * careers it has none for - the mason, which is this mod's - so nobody is given two.
 */
public final class EntityAIGiveHeroGifts extends EntityAIBase {

    /** 1.14's nearest-visible-player sensor reaches 16 blocks. */
    private static final double SIGHT = 16.0D;

    /** 1.14 hands over the gift within five blocks, a second after setting off. */
    private static final double GIVE_WITHIN = 5.0D;
    private static final long GIVE_AFTER = 20L;

    /** The first gift waits 30 seconds, as 1.14's cooldown starts at 600. */
    private static final long FIRST_COOLDOWN = 600L;

    private final EntityVillager villager;

    private long nextGift = -1L;
    private long startedAt;
    private boolean done;

    @Nullable
    private EntityPlayer hero;

    public EntityAIGiveHeroGifts(EntityVillager villager) {
        this.villager = villager;
        this.setMutexBits(3);
    }

    @Override
    public boolean shouldExecute() {
        if (!ModConfig.raids.heroGifts || this.villager.getCustomer() != null
                || HomeSite.isSleeping(this.villager) || !givesHere()) {
            return false;
        }

        long now = this.villager.world.getTotalWorldTime();
        if (this.nextGift < 0L) {
            this.nextGift = now + FIRST_COOLDOWN;
        }
        if (now < this.nextGift) {
            return false;
        }

        this.hero = nearestHero();
        return this.hero != null;
    }

    @Override
    public void startExecuting() {
        this.startedAt = this.villager.world.getTotalWorldTime();
        this.done = false;
    }

    @Override
    public boolean shouldContinueExecuting() {
        return !this.done && this.hero != null && this.hero.isEntityAlive()
                && Raids.heroLevel(this.hero) >= 0 && this.villager.getDistanceSq(this.hero) <= SIGHT * SIGHT;
    }

    @Override
    public void updateTask() {
        if (this.hero == null) {
            return;
        }

        this.villager.getLookHelper().setLookPositionWithEntity(this.hero, 30.0F, 30.0F);

        if (this.villager.getDistanceSq(this.hero) <= GIVE_WITHIN * GIVE_WITHIN) {
            this.villager.getNavigator().clearPath();
            if (this.villager.world.getTotalWorldTime() - this.startedAt > GIVE_AFTER) {
                for (ItemStack gift : gifts()) {
                    throwAt(gift, this.hero);
                }
                this.done = true;
            }
        } else if (this.villager.getNavigator().noPath()) {
            this.villager.getNavigator().tryMoveToEntityLiving(this.hero, 0.5D);
        }
    }

    @Override
    public void resetTask() {
        // 1.14's next cooldown: 30 seconds plus up to five minutes, whether or not a gift was given.
        this.nextGift = this.villager.world.getTotalWorldTime() + 600L + this.villager.getRNG().nextInt(6001);
        this.hero = null;
        this.villager.getNavigator().clearPath();
    }

    /** @return false where another mod's gifts already cover this villager */
    private boolean givesHere() {
        if (!Raids.usingRaidsBackport()) {
            return true;
        }
        // Raids Backport handles babies and the jobless itself; the gap is careers it has no table for.
        return !this.villager.isChild() && !Employment.isUnemployed(this.villager)
                && !Raids.othersGiveGiftsFor(WorkSounds.careerOf(this.villager));
    }

    @Nullable
    private EntityPlayer nearestHero() {
        EntityPlayer best = null;
        double bestDistance = SIGHT * SIGHT;

        for (EntityPlayer player : this.villager.world.playerEntities) {
            if (player.isSpectator() || !player.isEntityAlive() || Raids.heroLevel(player) < 0) {
                continue;
            }
            double distance = this.villager.getDistanceSq(player);
            if (distance <= bestDistance && this.villager.canEntityBeSeen(player)) {
                best = player;
                bestDistance = distance;
            }
        }
        return best;
    }

    private List<ItemStack> gifts() {
        if (this.villager.isChild()) {
            return Collections.singletonList(new ItemStack(Blocks.RED_FLOWER, 1, 0));
        }

        String profession = Employment.isUnemployed(this.villager) ? null : WorkSounds.modernProfession(this.villager);
        if (profession == null || !(this.villager.world instanceof WorldServer)) {
            return Collections.singletonList(new ItemStack(Items.WHEAT_SEEDS));
        }

        WorldServer world = (WorldServer) this.villager.world;
        LootTable table = world.getLootTableManager().getLootTableFromLocation(new ResourceLocation(
                VillagerBackport.MOD_ID, "gameplay/hero_of_the_village/" + profession + "_gift"));
        LootContext context = new LootContext.Builder(world).withLootedEntity(this.villager).build();
        return table.generateLootForPools(this.villager.getRNG(), context);
    }

    /** 1.14's {@code BrainUtil.throwItemAt}: from just below the eyes, a gentle lob at the target. */
    private void throwAt(ItemStack stack, EntityPlayer target) {
        if (stack.isEmpty()) {
            return;
        }

        double y = this.villager.posY - 0.3D + this.villager.getEyeHeight();
        EntityItem item = new EntityItem(this.villager.world, this.villager.posX, y, this.villager.posZ, stack);

        Vec3d toward = new Vec3d(target.posX - this.villager.posX, target.posY - this.villager.posY,
                target.posZ - this.villager.posZ).normalize().scale(0.3D);
        item.motionX = toward.x;
        item.motionY = toward.y;
        item.motionZ = toward.z;
        item.setDefaultPickupDelay();

        this.villager.world.spawnEntity(item);
    }
}
