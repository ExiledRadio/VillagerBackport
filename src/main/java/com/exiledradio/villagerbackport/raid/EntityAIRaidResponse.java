package com.exiledradio.villagerbackport.raid;

import com.exiledradio.villagerbackport.ModConfig;
import com.exiledradio.villagerbackport.block.BlockBell;
import com.exiledradio.villagerbackport.home.EntityAIShelterAtNight;
import com.exiledradio.villagerbackport.home.HomeSite;
import com.exiledradio.villagerbackport.job.WorkPathing;
import com.exiledradio.villagerbackport.job.WorkstationIndex;

import net.minecraft.block.Block;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.RandomPositionGenerator;
import net.minecraft.entity.item.EntityFireworkRocket;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import javax.annotation.Nullable;

/**
 * A villager living through a raid, or hiding from a bell: 1.14's PRE_RAID, RAID and HIDE
 * activities, as one goal.
 *
 * <ul>
 *   <li><b>Hide</b> - the bell was rung, with no raid on. Go to a bed within 32 blocks and
 *       stay there for fifteen seconds ({@code HideFromRaidOnBellRingTask}, {@code ExpireHidingTask}).</li>
 *   <li><b>Pre-raid</b> - a raid has begun but no raider has arrived. Gather at the bell, a little
 *       quicker than walking, and ring it now and then when standing by it ({@code RingBellTask}).</li>
 *   <li><b>Raid</b> - the raiders are here. Go to a bed within 24 blocks and stay
 *       ({@code FindHidingPlaceDuringRaidTask}).</li>
 *   <li><b>Victory</b> - come out under the open sky, jump about and set off fireworks
 *       ({@code GoOutsideAfterRaidTask}, {@code CelebrateRaidVictoryTask}).</li>
 * </ul>
 *
 * <p>Below panicking and above sleep: a villager runs from a pillager in its path, and a raid gets
 * it out of bed. A villager with no bed in reach hides under the nearest roof instead, as it does at
 * night.
 *
 * <p>Nothing is counted in {@link #shouldExecute}: whether to act is read from the raid and the
 * bell each time, so a villager this goal is not asked about loses nothing.
 */
public final class EntityAIRaidResponse extends EntityAIBase {

    private enum Mode {
        HIDE, PRE_RAID, RAID, VICTORY
    }

    /** 1.14's hiding radii: 32 for a bell in peacetime, 24 during a raid. */
    private static final int HIDE_REACH = 32;
    private static final int RAID_HIDE_REACH = 24;

    /** Close enough to the hiding place to count as there: 1.14's 2 blocks, plus one. */
    private static final double AT_HIDING_PLACE = 3.0D;

    /** 1.14's {@code ExpireHidingTask(15, 3)}: fifteen seconds at the hiding place and it is over. */
    private static final int HIDE_FOR = 15 * 20;

    /** 1.14's pre-raid gathering: within 2 blocks of the bell, half as fast again. */
    private static final double BELL_NEAR = 2.0D;
    private static final double RING_FROM = 3.0D;

    private static final double WALK = 0.5D;

    private final EntityVillager villager;

    @Nullable
    private Mode mode;
    @Nullable
    private BlockPos target;
    private int hiddenTicks;
    private long nextRepath;

    private static final long RAID_CHECK_EVERY = 10L;
    @Nullable
    private RaidView.Raid raid;
    private long nextRaidCheck;

    public EntityAIRaidResponse(EntityVillager villager) {
        this.villager = villager;
        this.setMutexBits(3);
    }

    @Override
    public boolean shouldExecute() {
        this.mode = currentMode();
        if (this.mode == null) {
            return false;
        }

        this.target = targetFor(this.mode);
        return this.target != null || this.mode == Mode.VICTORY;
    }

    @Override
    public boolean shouldContinueExecuting() {
        Mode now = currentMode();
        if (now != this.mode) {
            return false;
        }
        if (this.mode == Mode.HIDE && this.hiddenTicks >= HIDE_FOR) {
            BellHearing.forget(this.villager);
            return false;
        }
        return true;
    }

    @Override
    public void startExecuting() {
        this.hiddenTicks = 0;
        this.nextRepath = 0L;
    }

    @Override
    public void resetTask() {
        this.mode = null;
        this.target = null;
        this.villager.getNavigator().clearPath();
    }

    @Override
    public void updateTask() {
        if (this.mode == null) {
            return;
        }

        switch (this.mode) {
            case HIDE:
            case RAID:
                hide();
                break;
            case PRE_RAID:
                gatherAtBell();
                break;
            case VICTORY:
                celebrate();
                break;
        }
    }

    @Nullable
    private Mode currentMode() {
        RaidView.Raid raid = ModConfig.raids.raidBehaviour ? raid() : null;

        if (raid != null && !raid.isStopped()) {
            if (raid.isVictory()) {
                return Mode.VICTORY;
            }
            if (raid.isLoss() || !raid.isActive()) {
                return null;
            }
            return raid.hasFirstWaveSpawned() ? Mode.RAID : Mode.PRE_RAID;
        }

        // 1.14 only hides from a bell when there is no raid: during one, the raid decides.
        if (ModConfig.raids.hideOnBell && BellHearing.heardRecently(this.villager)) {
            return Mode.HIDE;
        }
        return null;
    }

    /**
     * The raid this villager is in, looked up at most every ten ticks. Which raid covers a spot
     * changes on the scale of minutes; its state is still read fresh each time.
     */
    @Nullable
    private RaidView.Raid raid() {
        long now = this.villager.world.getTotalWorldTime();
        if (now >= this.nextRaidCheck) {
            this.nextRaidCheck = now + RAID_CHECK_EVERY;
            this.raid = Raids.view().raidAt(this.villager.world, new BlockPos(this.villager));
        }
        return this.raid;
    }

    @Nullable
    private BlockPos targetFor(Mode mode) {
        switch (mode) {
            case HIDE:
                return hidingPlace(HIDE_REACH);
            case RAID:
                return hidingPlace(RAID_HIDE_REACH);
            case PRE_RAID:
                return nearestBell();
            default:
                return null;
        }
    }

    /** Hiding: get there, then stay put. Time only counts once there, as in 1.14. */
    private void hide() {
        if (this.target == null) {
            return;
        }

        if (this.villager.getDistanceSqToCenter(this.target) <= AT_HIDING_PLACE * AT_HIDING_PLACE) {
            this.villager.getNavigator().clearPath();
            this.hiddenTicks++;
            return;
        }

        if (!repathTo(this.target, WALK * 1.4D)) {
            return;
        }

        // The bed cannot be reached from here. Under a roof is hidden enough - it is what 1.14's
        // villagers settle for when they have no bed - and otherwise any roof within reach will do.
        BlockPos here = new BlockPos(this.villager);
        BlockPos instead = EntityAIShelterAtNight.isRoofed(this.villager.world, here)
                ? here : EntityAIShelterAtNight.findRoofed(this.villager);
        if (instead != null) {
            this.target = instead;
        }
    }

    private void gatherAtBell() {
        if (this.target == null) {
            return;
        }

        double distance = Math.sqrt(this.villager.getDistanceSqToCenter(this.target));

        // 1.14's RingBellTask: one chance in twenty a tick, and only within three blocks.
        if (distance <= RING_FROM && this.villager.getRNG().nextFloat() > 0.95F) {
            Block block = this.villager.world.getBlockState(this.target).getBlock();
            if (block instanceof BlockBell) {
                ((BlockBell) block).ringByVillager(this.villager.world, this.target);
            }
        }

        if (distance > BELL_NEAR) {
            repathTo(this.target, WALK * 1.5D);
        }
    }

    private void celebrate() {
        World world = this.villager.world;

        if (!underOpenSky()) {
            // GoOutsideAfterRaidTask: somewhere the sky is open, close by.
            if (this.villager.getNavigator().noPath() && world.getTotalWorldTime() >= this.nextRepath) {
                this.nextRepath = world.getTotalWorldTime() + 40L;
                Vec3d out = RandomPositionGenerator.findRandomTarget(this.villager, 10, 7);
                if (out != null && world.canSeeSky(new BlockPos(out))) {
                    this.villager.getNavigator().tryMoveToXYZ(out.x, out.y, out.z, WALK);
                }
            }
            return;
        }

        if (this.villager.getRNG().nextInt(100) == 0) {
            // 1.14 plays its own celebration sound; the nearest 1.12.2 has is the villager's "yes".
            this.villager.playSound(SoundEvents.ENTITY_VILLAGER_YES, 1.0F,
                    (this.villager.getRNG().nextFloat() - this.villager.getRNG().nextFloat()) * 0.2F + 1.0F);
            if (this.villager.onGround) {
                this.villager.getJumpHelper().setJumping();
            }
        }

        // Raids Backport sets off its own villagers' fireworks; two sets would be twice as many.
        if (!Raids.usingRaidsBackport() && this.villager.getRNG().nextInt(200) == 0) {
            launchFirework();
        }
    }

    private boolean underOpenSky() {
        BlockPos here = new BlockPos(this.villager);
        return this.villager.world.canSeeSky(here);
    }

    /** 1.14's celebration rocket: one burst of a random colour, flight 0 to 2. */
    private void launchFirework() {
        EnumDyeColor colour = EnumDyeColor.values()[this.villager.getRNG().nextInt(EnumDyeColor.values().length)];

        NBTTagCompound explosion = new NBTTagCompound();
        // The firework colour table, as the firework recipe reads it: getColorValue() is client-only.
        explosion.setIntArray("Colors", new int[]{net.minecraft.item.ItemDye.DYE_COLORS[colour.getDyeDamage()]});
        explosion.setByte("Type", (byte) 1);

        NBTTagList explosions = new NBTTagList();
        explosions.appendTag(explosion);

        NBTTagCompound fireworks = new NBTTagCompound();
        fireworks.setByte("Flight", (byte) this.villager.getRNG().nextInt(3));
        fireworks.setTag("Explosions", explosions);

        ItemStack rocket = new ItemStack(Items.FIREWORKS);
        rocket.setTagInfo("Fireworks", fireworks);

        this.villager.world.spawnEntity(new EntityFireworkRocket(this.villager.world, this.villager.posX,
                this.villager.posY + this.villager.getEyeHeight(), this.villager.posZ, rocket));
    }

    /**
     * Sets off for the target again if the villager has stopped, at most once a second.
     *
     * @return true if a path was looked for and none reaches the target
     */
    private boolean repathTo(BlockPos pos, double speed) {
        long now = this.villager.world.getTotalWorldTime();
        if (!this.villager.getNavigator().noPath() || now < this.nextRepath) {
            return false;
        }
        this.nextRepath = now + 20L;
        return !WorkPathing.walkTo(this.villager, pos, speed);
    }

    /**
     * Where to hide: its own bed if that is in reach, else the nearest bed, else any roof.
     *
     * <p>1.14 picks among the beds in reach at random and only falls back to the villager's own. Its
     * own first keeps a village from all running for the one bed nearest the bell.
     */
    @Nullable
    private BlockPos hidingPlace(int reach) {
        BlockPos here = new BlockPos(this.villager);

        BlockPos home = HomeSite.validated(this.villager);
        if (home != null && home.distanceSq(here) <= (double) reach * reach) {
            return home;
        }

        BlockPos best = nearestIndexed(here, reach, true);
        if (best != null) {
            return best;
        }

        if (EntityAIShelterAtNight.isRoofed(this.villager.world, here)) {
            return here;
        }
        return EntityAIShelterAtNight.findRoofed(this.villager);
    }

    /** 1.14 gathers at the meeting point it knows of; here, the nearest bell within 48 blocks. */
    @Nullable
    private BlockPos nearestBell() {
        return nearestIndexed(new BlockPos(this.villager), 48, false);
    }

    @Nullable
    private BlockPos nearestIndexed(BlockPos here, int reach, boolean beds) {
        World world = this.villager.world;
        int chunks = (reach >> 4) + 1;
        BlockPos best = null;
        double bestDistance = (double) reach * reach;

        for (int dx = -chunks; dx <= chunks; dx++) {
            for (int dz = -chunks; dz <= chunks; dz++) {
                int cx = (here.getX() >> 4) + dx;
                int cz = (here.getZ() >> 4) + dz;
                for (BlockPos pos : beds ? WorkstationIndex.bedsIn(world, cx, cz) : WorkstationIndex.bellsIn(world, cx, cz)) {
                    double distance = pos.distanceSq(here);
                    if (distance < bestDistance) {
                        bestDistance = distance;
                        best = pos;
                    }
                }
            }
        }
        return best;
    }
}
