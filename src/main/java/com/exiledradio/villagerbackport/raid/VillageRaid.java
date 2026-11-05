package com.exiledradio.villagerbackport.raid;

import com.exiledradio.villagerbackport.ModConfig;
import com.exiledradio.villagerbackport.VillagerBackport;
import com.exiledradio.villagerbackport.block.ModSounds;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Enchantments;
import net.minecraft.init.Items;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.network.play.server.SPacketSoundEffect;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.BossInfo;
import net.minecraft.world.BossInfoServer;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.WorldEntitySpawner;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * One raid on one village: 1.14's {@code Raid}, ported.
 *
 * <p>The timings, the wave table, the spawn rings, the boss bar and the outcome rules are 1.14's.
 * What changes is who the raiders are: any mob can be one (see {@link RaiderData}), and the wave
 * table names its members by registry name, so a pillager is whichever mod's pillager is installed.
 */
public final class VillageRaid implements RaidView.Raid {

    enum Status {
        ONGOING, VICTORY, LOSS, STOPPED;

        static Status of(String name) {
            for (Status status : values()) {
                if (status.name().equalsIgnoreCase(name)) {
                    return status;
                }
            }
            return ONGOING;
        }
    }

    /** 1.14's {@code WaveMember}: how many of each, by wave, with index 0 unused. */
    enum Member {
        VINDICATOR(new int[]{0, 0, 2, 0, 1, 4, 2, 5}),
        EVOKER(new int[]{0, 0, 0, 0, 0, 1, 1, 2}),
        PILLAGER(new int[]{0, 4, 3, 3, 4, 4, 4, 2}),
        WITCH(new int[]{0, 0, 0, 0, 3, 0, 0, 1}),
        RAVAGER(new int[]{0, 0, 0, 1, 0, 1, 0, 2});

        final int[] counts;

        Member(int[] counts) {
            this.counts = counts;
        }
    }

    public static final int MAX_LEVEL = 5;
    private static final int PRE_RAID_TICKS = 300;
    private static final long TIMEOUT = 48000L;

    /** Raiders this far (112 blocks) from the centre have wandered off and leave the raid. */
    private static final double LEAVE_DISTANCE_SQ = 12544.0D;

    private final WorldServer world;
    private final int id;
    private final int numGroups;

    private final Map<Integer, Set<EntityLiving>> raiders = new HashMap<Integer, Set<EntityLiving>>();
    private final Map<Integer, EntityLiving> leaders = new HashMap<Integer, EntityLiving>();
    private final Set<UUID> heroes = new HashSet<UUID>();
    private final Map<UUID, Integer> idleStrikes = new HashMap<UUID, Integer>();

    private final BossInfoServer bossInfo;
    private final Random random = new Random();

    private BlockPos center;
    private boolean started;
    private boolean active;
    private long ticksActive;
    private int badOmenLevel;
    private int groupsSpawned;
    private int preRaidTicks;
    private int postRaidTicks;
    private int celebrationTicks;
    private float totalHealth;
    private Status status;

    @Nullable
    private BlockPos waveSpawnPos;

    VillageRaid(int id, WorldServer world, BlockPos center) {
        this.id = id;
        this.world = world;
        this.center = center;
        this.active = true;
        this.preRaidTicks = PRE_RAID_TICKS;
        this.numGroups = wavesFor(world.getDifficulty());
        this.status = Status.ONGOING;
        this.bossInfo = newBossInfo();
        this.bossInfo.setPercent(0.0F);
    }

    VillageRaid(WorldServer world, NBTTagCompound nbt) {
        this.world = world;
        this.id = nbt.getInteger("Id");
        this.started = nbt.getBoolean("Started");
        this.active = nbt.getBoolean("Active");
        this.ticksActive = nbt.getLong("TicksActive");
        this.badOmenLevel = nbt.getInteger("BadOmenLevel");
        this.groupsSpawned = nbt.getInteger("GroupsSpawned");
        this.preRaidTicks = nbt.getInteger("PreRaidTicks");
        this.postRaidTicks = nbt.getInteger("PostRaidTicks");
        this.totalHealth = nbt.getFloat("TotalHealth");
        this.center = new BlockPos(nbt.getInteger("CX"), nbt.getInteger("CY"), nbt.getInteger("CZ"));
        this.numGroups = nbt.getInteger("NumGroups");
        this.status = Status.of(nbt.getString("Status"));
        NBTTagList list = nbt.getTagList("HeroesOfTheVillage", 10);
        for (int i = 0; i < list.tagCount(); i++) {
            this.heroes.add(NBTUtil.getUUIDFromTag(list.getCompoundTagAt(i)));
        }
        this.bossInfo = newBossInfo();
    }

    NBTTagCompound write(NBTTagCompound nbt) {
        nbt.setInteger("Id", this.id);
        nbt.setBoolean("Started", this.started);
        nbt.setBoolean("Active", this.active);
        nbt.setLong("TicksActive", this.ticksActive);
        nbt.setInteger("BadOmenLevel", this.badOmenLevel);
        nbt.setInteger("GroupsSpawned", this.groupsSpawned);
        nbt.setInteger("PreRaidTicks", this.preRaidTicks);
        nbt.setInteger("PostRaidTicks", this.postRaidTicks);
        nbt.setFloat("TotalHealth", this.totalHealth);
        nbt.setInteger("NumGroups", this.numGroups);
        nbt.setString("Status", this.status.name().toLowerCase(Locale.ROOT));
        nbt.setInteger("CX", this.center.getX());
        nbt.setInteger("CY", this.center.getY());
        nbt.setInteger("CZ", this.center.getZ());
        NBTTagList list = new NBTTagList();
        for (UUID hero : this.heroes) {
            list.appendTag(NBTUtil.createUUIDTag(hero));
        }
        nbt.setTag("HeroesOfTheVillage", list);
        return nbt;
    }

    private static BossInfoServer newBossInfo() {
        return new BossInfoServer(raidName(), BossInfo.Color.RED, BossInfo.Overlay.NOTCHED_10);
    }

    private static ITextComponent raidName() {
        return new TextComponentTranslation("event.villagerbackport.raid");
    }

    private static ITextComponent withSuffix(ITextComponent suffix) {
        return raidName().appendSibling(new TextComponentString(" - ")).appendSibling(suffix);
    }

    public int getId() {
        return this.id;
    }

    public BlockPos getCenter() {
        return this.center;
    }

    public int getBadOmenLevel() {
        return this.badOmenLevel;
    }

    public int getGroupsSpawned() {
        return this.groupsSpawned;
    }

    public int getNumGroups() {
        return this.numGroups;
    }

    // --- RaidView.Raid ---------------------------------------------------------------------------

    @Override
    public boolean isActive() {
        return this.active;
    }

    @Override
    public boolean isVictory() {
        return this.status == Status.VICTORY;
    }

    @Override
    public boolean isLoss() {
        return this.status == Status.LOSS;
    }

    @Override
    public boolean isStopped() {
        return this.status == Status.STOPPED;
    }

    @Override
    public boolean hasFirstWaveSpawned() {
        return this.groupsSpawned > 0;
    }

    public boolean isOver() {
        return isVictory() || isLoss();
    }

    /** 1.14's {@code isBetweenWaves}: a wave beaten, the next not yet here. */
    public boolean isBetweenWaves() {
        return hasFirstWaveSpawned() && getRaiderCount() == 0 && this.preRaidTicks > 0;
    }

    public boolean isStarted() {
        return this.started;
    }

    // --- level --------------------------------------------------------------------------------

    /**
     * Adds an omen to the raid: its levels go on top of the raid's own, up to five.
     *
     * @param omenLevel the omen's amplifier plus one
     */
    void absorbOmen(int omenLevel) {
        this.badOmenLevel = MathHelper.clamp(this.badOmenLevel + omenLevel, 0, MAX_LEVEL);
    }

    /** 1.14's chance of enchanted gear, by raid level. */
    float enchantOdds() {
        switch (this.badOmenLevel) {
            case 2:
                return 0.1F;
            case 3:
                return 0.25F;
            case 4:
                return 0.5F;
            case 5:
                return 0.75F;
            default:
                return 0.0F;
        }
    }

    void stop() {
        this.active = false;
        for (EntityPlayerMP player : new ArrayList<EntityPlayerMP>(this.bossInfo.getPlayers())) {
            this.bossInfo.removePlayer(player);
        }
        this.status = Status.STOPPED;
    }

    // --- tick ---------------------------------------------------------------------------------

    void tick() {
        if (isStopped()) {
            return;
        }

        if (this.status != Status.ONGOING) {
            tickOutcome();
            return;
        }

        boolean wasActive = this.active;
        this.active = this.world.isBlockLoaded(this.center);
        if (this.world.getDifficulty() == EnumDifficulty.PEACEFUL) {
            stop();
            return;
        }
        if (wasActive != this.active) {
            this.bossInfo.setVisible(this.active);
        }
        if (!this.active) {
            return;
        }

        if (!RaidVillages.isVillage(this.world, this.center)) {
            moveCenterToVillage();
        }
        if (!RaidVillages.isVillage(this.world, this.center)) {
            if (this.groupsSpawned > 0) {
                this.status = Status.LOSS;
            } else {
                stop();
            }
            return;
        }

        if (++this.ticksActive >= TIMEOUT) {
            stop();
            return;
        }

        int count = getRaiderCount();
        if (count == 0 && hasMoreWaves()) {
            if (this.preRaidTicks <= 0) {
                if (this.groupsSpawned > 0) {
                    this.preRaidTicks = PRE_RAID_TICKS;
                    this.bossInfo.setName(raidName());
                    return;
                }
            } else {
                boolean chosen = this.waveSpawnPos != null;
                boolean choose = !chosen && this.preRaidTicks % 5 == 0;
                if (chosen && !this.world.isBlockLoaded(this.waveSpawnPos)) {
                    choose = true;
                }
                if (choose) {
                    this.waveSpawnPos = findSpawnSpot(this.preRaidTicks < 100 ? 1 : 0, 3);
                }
                if (this.preRaidTicks == PRE_RAID_TICKS || this.preRaidTicks % 20 == 0) {
                    updateBossInfoVisibility();
                }
                this.preRaidTicks--;
                this.bossInfo.setPercent(MathHelper.clamp((PRE_RAID_TICKS - this.preRaidTicks) / (float) PRE_RAID_TICKS, 0.0F, 1.0F));
            }
        }

        if (this.ticksActive % 20L == 0L) {
            updateBossInfoVisibility();
            dropStrays();
            count = getRaiderCount();
            if (count > 0 && count <= 2) {
                this.bossInfo.setName(withSuffix(new TextComponentTranslation(
                        "event.villagerbackport.raid.raiders_remaining", count)));
            } else {
                this.bossInfo.setName(raidName());
            }
        }

        boolean horn = false;
        int failures = 0;
        while (shouldSpawnWave()) {
            BlockPos at = this.waveSpawnPos != null ? this.waveSpawnPos : findSpawnSpot(failures, 20);
            if (at != null) {
                this.started = true;
                spawnNextWave(at);
                if (!horn) {
                    playHorn(at);
                    horn = true;
                }
            } else {
                failures++;
            }
            if (failures > 3) {
                stop();
                break;
            }
        }

        if (this.started && !hasMoreWaves() && getRaiderCount() == 0) {
            if (this.postRaidTicks < 40) {
                this.postRaidTicks++;
            } else {
                victory();
            }
        }

        RaidManager.get(this.world).markDirty();
    }

    private void victory() {
        this.status = Status.VICTORY;
        for (UUID uuid : this.heroes) {
            Entity entity = this.world.getEntityFromUuid(uuid);
            if (entity instanceof EntityLivingBase && !(entity instanceof EntityPlayerMP && ((EntityPlayerMP) entity).isSpectator())) {
                ((EntityLivingBase) entity).addPotionEffect(new PotionEffect(ModEffects.heroOfTheVillage,
                        48000, Math.max(0, this.badOmenLevel - 1), false, false));
            }
        }
        VillagerBackport.LOGGER.info("Raid {} at {} was won by {} hero(es).", this.id, this.center, this.heroes.size());
    }

    /** Victory or defeat stays on the bar for 30 seconds, then the raid is forgotten. */
    private void tickOutcome() {
        if (++this.celebrationTicks >= 600) {
            stop();
            return;
        }
        if (this.celebrationTicks % 20 == 0) {
            updateBossInfoVisibility();
            this.bossInfo.setVisible(true);
            if (isVictory()) {
                this.bossInfo.setPercent(0.0F);
                this.bossInfo.setName(withSuffix(new TextComponentTranslation("event.villagerbackport.raid.victory")));
            } else {
                this.bossInfo.setName(withSuffix(new TextComponentTranslation("event.villagerbackport.raid.defeat")));
            }
        }
    }

    /** The village moved, or its centre emptied: find the nearest village section within two. */
    private void moveCenterToVillage() {
        int sx = this.center.getX() >> 4;
        int sy = this.center.getY() >> 4;
        int sz = this.center.getZ() >> 4;
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;

        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    BlockPos mid = new BlockPos(((sx + dx) << 4) + 8, ((sy + dy) << 4) + 8, ((sz + dz) << 4) + 8);
                    if (mid.getY() >= 0 && mid.getY() < 256 && RaidVillages.isVillage(this.world, mid)) {
                        double distance = mid.distanceSq(this.center);
                        if (distance < bestDistance) {
                            best = mid;
                            bestDistance = distance;
                        }
                    }
                }
            }
        }
        if (best != null) {
            this.center = best;
        }
    }

    private boolean hasBonusWave() {
        return this.badOmenLevel > 1;
    }

    /** 1.14's {@code shouldSpawnGroup}: waves remain, counting the bonus one. */
    private boolean hasMoreWaves() {
        return hasBonusWave() ? this.groupsSpawned <= this.numGroups : this.groupsSpawned < this.numGroups;
    }

    private boolean isFinalWaveDone() {
        return this.groupsSpawned == this.numGroups;
    }

    private boolean shouldSpawnBonusWave() {
        return isFinalWaveDone() && getRaiderCount() == 0 && hasBonusWave();
    }

    private boolean shouldSpawnWave() {
        return this.preRaidTicks == 0 && (this.groupsSpawned < this.numGroups || shouldSpawnBonusWave())
                && getRaiderCount() == 0;
    }

    // --- raiders ------------------------------------------------------------------------------

    public int getRaiderCount() {
        int count = 0;
        for (Set<EntityLiving> wave : this.raiders.values()) {
            count += wave.size();
        }
        return count;
    }

    /**
     * Adds a raider to this raid, or puts a reloaded one back in its place.
     *
     * @param countHealth whether its health counts towards the bar - not for one being reloaded
     */
    void joinRaid(int wave, EntityLiving raider, boolean countHealth) {
        Set<EntityLiving> members = this.raiders.get(wave);
        if (members == null) {
            members = new HashSet<EntityLiving>();
            this.raiders.put(wave, members);
        }

        Iterator<EntityLiving> it = members.iterator();
        while (it.hasNext()) {
            if (it.next().getUniqueID().equals(raider.getUniqueID())) {
                it.remove();
            }
        }
        members.add(raider);
        RaiderData.join(raider, this.id, wave);
        RaiderGoals.attach(raider);

        if (countHealth) {
            this.totalHealth += raider.getHealth();
        }
        updateBarPercentage();
        RaidManager.get(this.world).markDirty();
    }

    void leaveRaid(EntityLiving raider, boolean dropHealth) {
        Set<EntityLiving> members = this.raiders.get(RaiderData.wave(raider));
        if (members != null && members.remove(raider)) {
            if (dropHealth) {
                this.totalHealth -= raider.getHealth();
            }
            updateBarPercentage();
            RaidManager.get(this.world).markDirty();
        }
        RaiderData.leave(raider);
        this.idleStrikes.remove(raider.getUniqueID());
    }

    void addHero(Entity hero) {
        this.heroes.add(hero.getUniqueID());
    }

    void removeLeader(int wave) {
        this.leaders.remove(wave);
    }

    /**
     * 1.14's clean-up of raiders that are no longer really in it: unloaded for good, wandered more
     * than 112 blocks off, gone to another dimension, or idle outside the village - no player within
     * 32 blocks for two minutes - for thirty seconds more.
     *
     * <p>One change from 1.14: a raider with something to fight is never idle. 1.14's idle clock only
     * measures how long since a player was near, so pillagers busy at the far end of a village from
     * the player were dropped while still shooting villagers, and the bar counted them as gone. Each
     * drop is logged with its reason.
     */
    private void dropStrays() {
        Map<EntityLiving, String> gone = new HashMap<EntityLiving, String>();
        for (Set<EntityLiving> wave : this.raiders.values()) {
            for (EntityLiving raider : wave) {
                BlockPos pos = new BlockPos(raider);
                if (raider.isDead) {
                    gone.put(raider, "dead");
                } else if (raider.dimension != this.world.provider.getDimension()) {
                    gone.put(raider, "changed dimension");
                } else if (this.center.distanceSq(pos) >= LEAVE_DISTANCE_SQ) {
                    gone.put(raider, "more than 112 blocks from the centre");
                } else if (raider.ticksExisted > 600) {
                    if (this.world.getEntityFromUuid(raider.getUniqueID()) == null) {
                        gone.put(raider, "no longer loaded");
                    } else if (raider.getAttackTarget() == null && raider.getIdleTime() > 2400
                            && !RaidVillages.isVillage(this.world, pos)) {
                        Integer strikes = this.idleStrikes.get(raider.getUniqueID());
                        int now = strikes == null ? 1 : strikes + 1;
                        this.idleStrikes.put(raider.getUniqueID(), now);
                        if (now >= 30) {
                            gone.put(raider, "idle outside the village");
                        }
                    }
                }
            }
        }
        for (Map.Entry<EntityLiving, String> entry : gone.entrySet()) {
            VillagerBackport.LOGGER.debug("Raider {} ({}) left raid {}: {}.", entry.getKey().getEntityId(),
                    entry.getKey().getName(), this.id, entry.getValue());
            leaveRaid(entry.getKey(), true);
        }
    }

    private void updateBarPercentage() {
        float health = 0.0F;
        for (Set<EntityLiving> wave : this.raiders.values()) {
            for (EntityLiving raider : wave) {
                health += raider.getHealth();
            }
        }
        this.bossInfo.setPercent(this.totalHealth <= 0.0F ? 0.0F : MathHelper.clamp(health / this.totalHealth, 0.0F, 1.0F));
    }

    /** The bar is shown to every living player this raid covers. */
    private void updateBossInfoVisibility() {
        Set<EntityPlayerMP> showing = new HashSet<EntityPlayerMP>(this.bossInfo.getPlayers());
        List<EntityPlayerMP> covered = new ArrayList<EntityPlayerMP>();

        for (net.minecraft.entity.player.EntityPlayer player : this.world.playerEntities) {
            if (player instanceof EntityPlayerMP && player.isEntityAlive()
                    && RaidManager.get(this.world).findRaid(new BlockPos(player)) == this) {
                covered.add((EntityPlayerMP) player);
            }
        }
        for (EntityPlayerMP player : covered) {
            if (!showing.contains(player)) {
                this.bossInfo.addPlayer(player);
            }
        }
        for (EntityPlayerMP player : showing) {
            if (!covered.contains(player)) {
                this.bossInfo.removePlayer(player);
            }
        }
    }

    // --- waves --------------------------------------------------------------------------------

    private void spawnNextWave(BlockPos at) {
        boolean leaderChosen = false;
        int wave = this.groupsSpawned + 1;
        this.totalHealth = 0.0F;
        DifficultyInstance difficulty = this.world.getDifficultyForLocation(at);
        boolean bonus = shouldSpawnBonusWave();

        for (Member member : Member.values()) {
            // Waves past 1.14's seventh repeat its last.
            int row = Math.min(bonus ? this.numGroups : wave, member.counts.length - 1);
            int count = member.counts[row] + extraCount(member, wave, difficulty, bonus);

            int ridden = 0;
            for (int i = 0; i < count; i++) {
                EntityLiving raider = create(member);
                if (raider == null) {
                    continue;
                }
                if (!leaderChosen && canBeLeader(member)) {
                    setLeader(wave, raider);
                    leaderChosen = true;
                }
                spawn(wave, raider, at, difficulty);

                if (member == Member.RAVAGER) {
                    // 1.14's riders: a pillager on normal's last wave, an evoker and then
                    // vindicators from hard's last wave on.
                    Member riderKind = null;
                    if (wave == wavesFor(EnumDifficulty.NORMAL)) {
                        riderKind = Member.PILLAGER;
                    } else if (wave >= wavesFor(EnumDifficulty.HARD)) {
                        riderKind = ridden == 0 ? Member.EVOKER : Member.VINDICATOR;
                    }
                    ridden++;
                    EntityLiving rider = riderKind == null ? null : create(riderKind);
                    if (rider != null) {
                        spawn(wave, rider, at, difficulty);
                        rider.startRiding(raider);
                    }
                }
            }
        }

        this.waveSpawnPos = null;
        this.groupsSpawned++;
        updateBarPercentage();
        RaidManager.get(this.world).markDirty();
        VillagerBackport.LOGGER.debug("Raid {} wave {} of {} spawned at {} ({} raiders).", this.id, wave,
                this.numGroups, at, getRaiderCount());
    }

    private void spawn(int wave, EntityLiving raider, BlockPos at, DifficultyInstance difficulty) {
        raider.setPosition(at.getX() + 0.5D, at.getY() + 1.0D, at.getZ() + 0.5D);
        raider.onInitialSpawn(difficulty, null);
        applyWaveBonus(raider, wave);
        raider.onGround = true;
        this.world.spawnEntity(raider);
        joinRaid(wave, raider, true);
    }

    /**
     * 1.14's per-wave gear: the vindicator's axe, sharper on the later waves of a strong raid, and
     * this mod's pillager's multishot crossbow.
     */
    private void applyWaveBonus(EntityLiving raider, int wave) {
        if (raider instanceof com.exiledradio.villagerbackport.illager.EntityPillager) {
            ((com.exiledradio.villagerbackport.illager.EntityPillager) raider).applyRaidBonus(wave,
                    wavesFor(EnumDifficulty.EASY), wavesFor(EnumDifficulty.NORMAL), enchantOdds());
            return;
        }
        if (pathOf(raider).contains("vindicat")) {
            ItemStack axe = new ItemStack(Items.IRON_AXE);
            if (this.random.nextFloat() <= enchantOdds()) {
                Map<net.minecraft.enchantment.Enchantment, Integer> enchants =
                        new HashMap<net.minecraft.enchantment.Enchantment, Integer>();
                enchants.put(Enchantments.SHARPNESS, wave > wavesFor(EnumDifficulty.NORMAL) ? 2 : 1);
                EnchantmentHelper.setEnchantments(enchants, axe);
            }
            raider.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, axe);
        }
    }

    private void setLeader(int wave, EntityLiving raider) {
        this.leaders.put(wave, raider);
        RaiderData.setLeader(raider, true);
        raider.setItemStackToSlot(EntityEquipmentSlot.HEAD, OminousBanner.create());
        raider.setDropChance(EntityEquipmentSlot.HEAD, 2.0F);
    }

    /** 1.14's {@code getPotentialBonusSpawns}: a few more on normal and hard, ravagers on bonus waves. */
    private int extraCount(Member member, int wave, DifficultyInstance difficulty, boolean bonus) {
        EnumDifficulty level = this.world.getDifficulty();
        boolean easy = level == EnumDifficulty.EASY;
        boolean normal = level == EnumDifficulty.NORMAL;
        int max;
        switch (member) {
            case WITCH:
                if (easy || wave <= 2 || wave == 4) {
                    return 0;
                }
                max = 1;
                break;
            case PILLAGER:
            case VINDICATOR:
                max = easy ? this.random.nextInt(2) : normal ? 1 : 2;
                break;
            case RAVAGER:
                max = !easy && bonus ? 1 : 0;
                break;
            default:
                return 0;
        }
        return max > 0 ? this.random.nextInt(max + 1) : 0;
    }

    private static boolean canBeLeader(Member member) {
        return member != Member.WITCH && member != Member.RAVAGER;
    }

    /** @return a new raider of this kind, from the first installed mob the config names, or null */
    @Nullable
    private EntityLiving create(Member member) {
        for (String name : entitiesFor(member)) {
            ResourceLocation id = new ResourceLocation(name.trim());
            if (!ForgeRegistries.ENTITIES.containsKey(id)) {
                continue;
            }
            Entity entity = EntityList.createEntityByIDFromName(id, this.world);
            if (entity instanceof EntityLiving) {
                return (EntityLiving) entity;
            }
        }
        return null;
    }

    private static String[] entitiesFor(Member member) {
        switch (member) {
            case VINDICATOR:
                return ModConfig.raids.vindicators;
            case EVOKER:
                return ModConfig.raids.evokers;
            case PILLAGER:
                return ModConfig.raids.pillagers;
            case WITCH:
                return ModConfig.raids.witches;
            default:
                return ModConfig.raids.ravagers;
        }
    }

    private static String pathOf(Entity entity) {
        ResourceLocation name = EntityList.getKey(entity);
        return name == null ? "" : name.getPath();
    }

    /**
     * 1.14's spawn ring: 64 blocks out at first, 32 if that fails, then anywhere near the centre;
     * outside the village for the first two, on loaded ground a mob can stand on.
     */
    @Nullable
    private BlockPos findSpawnSpot(int proximity, int tries) {
        int ring = proximity == 0 ? 2 : 2 - proximity;
        for (int i = 0; i < tries; i++) {
            float angle = this.world.rand.nextFloat() * ((float) Math.PI * 2F);
            int x = this.center.getX() + MathHelper.floor(MathHelper.cos(angle) * 32.0F * ring) + this.world.rand.nextInt(5);
            int z = this.center.getZ() + MathHelper.floor(MathHelper.sin(angle) * 32.0F * ring) + this.world.rand.nextInt(5);
            BlockPos probe = new BlockPos(x, 0, z);
            if (!this.world.isBlockLoaded(probe)) {
                continue;
            }
            BlockPos pos = this.world.getHeight(probe);

            if ((!RaidVillages.isVillage(this.world, pos) || proximity >= 2)
                    && this.world.isAreaLoaded(pos, 10)
                    && WorldEntitySpawner.canCreatureTypeSpawnAtLocation(EntityLiving.SpawnPlacementType.ON_GROUND, this.world, pos)) {
                return pos;
            }
        }
        return null;
    }

    /** The horn, sent to each player from 13 blocks away in the wave's direction, as 1.14 does. */
    private void playHorn(BlockPos at) {
        for (net.minecraft.entity.player.EntityPlayer player : this.world.playerEntities) {
            if (!(player instanceof EntityPlayerMP)) {
                continue;
            }
            double dx = at.getX() - player.posX;
            double dz = at.getZ() - player.posZ;
            float distance = MathHelper.sqrt(dx * dx + dz * dz);
            if (distance <= 0.0F) {
                continue;
            }
            if (distance <= 64.0F || RaidVillages.isVillage(this.world, new BlockPos(player))) {
                double x = player.posX + 13.0F / distance * dx;
                double z = player.posZ + 13.0F / distance * dz;
                ((EntityPlayerMP) player).connection.sendPacket(new SPacketSoundEffect(ModSounds.raidHorn,
                        SoundCategory.NEUTRAL, x, player.posY, z, 64.0F, 1.0F));
            }
        }
    }

    /** 1.14's wave counts: three, five or seven. */
    static int wavesFor(EnumDifficulty difficulty) {
        switch (difficulty) {
            case EASY:
                return ModConfig.raids.easyWaves;
            case NORMAL:
                return ModConfig.raids.normalWaves;
            case HARD:
                return ModConfig.raids.hardWaves;
            default:
                return 0;
        }
    }

    /** Every raider currently counted, for commands and the bell. */
    List<EntityLiving> allRaiders() {
        List<EntityLiving> all = new ArrayList<EntityLiving>();
        for (Set<EntityLiving> wave : this.raiders.values()) {
            all.addAll(wave);
        }
        return Collections.unmodifiableList(all);
    }
}
