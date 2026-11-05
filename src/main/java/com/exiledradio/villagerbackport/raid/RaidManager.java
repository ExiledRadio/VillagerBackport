package com.exiledradio.villagerbackport.raid;

import com.exiledradio.villagerbackport.VillagerBackport;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Every raid in one dimension: 1.14's {@code RaidManager}, saved with the world.
 *
 * <p>Saved per dimension, as 1.14 keeps a manager per world. The raids' raiders are not saved here:
 * each raider remembers its own raid (see {@link RaiderData}) and rejoins when its chunk loads.
 */
public final class RaidManager extends WorldSavedData {

    private static final String NAME = VillagerBackport.MOD_ID + "_raids";

    /** 1.14 finds the raid covering a position within 96 blocks of its centre. */
    private static final double FIND_RADIUS_SQ = 9216.0D;

    /** The game rule 1.14 added, kept by name so players can type what they know. */
    public static final String DISABLE_RAIDS = "disableRaids";

    private final Map<Integer, VillageRaid> byId = new LinkedHashMap<Integer, VillageRaid>();

    @Nullable
    private WorldServer world;

    /** Raids read back before the world was attached, waiting for it. */
    @Nullable
    private NBTTagList pending;

    private int nextId = 1;
    private int tick;

    public RaidManager(String name) {
        super(name);
    }

    public static RaidManager get(WorldServer world) {
        MapStorage storage = world.getPerWorldStorage();
        RaidManager manager = (RaidManager) storage.getOrLoadData(RaidManager.class, NAME);
        if (manager == null) {
            manager = new RaidManager(NAME);
            storage.setData(NAME, manager);
        }
        manager.attach(world);
        return manager;
    }

    private void attach(WorldServer world) {
        if (this.world == world) {
            return;
        }
        this.world = world;
        if (this.pending != null) {
            for (int i = 0; i < this.pending.tagCount(); i++) {
                VillageRaid raid = new VillageRaid(world, this.pending.getCompoundTagAt(i));
                this.byId.put(raid.getId(), raid);
            }
            this.pending = null;
        }
    }

    void tick() {
        this.tick++;
        boolean disabled = this.world != null && this.world.getGameRules().getBoolean(DISABLE_RAIDS);

        Iterator<VillageRaid> it = this.byId.values().iterator();
        while (it.hasNext()) {
            VillageRaid raid = it.next();
            if (disabled) {
                raid.stop();
            }
            if (raid.isStopped()) {
                it.remove();
                markDirty();
            } else {
                raid.tick();
            }
        }

        if (this.tick % 200 == 0) {
            markDirty();
        }
    }

    @Nullable
    public VillageRaid get(int id) {
        return this.byId.get(id);
    }

    public Collection<VillageRaid> all() {
        return this.byId.values();
    }

    /** @return the raid nearest this position within 96 blocks, or null */
    @Nullable
    public VillageRaid findRaid(BlockPos pos) {
        VillageRaid best = null;
        double bestDistance = FIND_RADIUS_SQ;
        for (VillageRaid raid : this.byId.values()) {
            double distance = raid.getCenter().distanceSq(pos);
            if (raid.isActive() && distance < bestDistance) {
                best = raid;
                bestDistance = distance;
            }
        }
        return best;
    }

    /**
     * Starts a raid on the village at this position, or makes the one already there stronger:
     * 1.14's {@code badOmenTick}, run when a Raid Omen ends.
     *
     * @param omenLevel the omen's amplifier plus one
     * @return the raid, or null if raids are off here or it is already at full strength
     */
    @Nullable
    public VillageRaid startOrExtend(BlockPos at, int omenLevel) {
        if (this.world == null || this.world.getGameRules().getBoolean(DISABLE_RAIDS)
                || this.world.provider.getDimensionType() == net.minecraft.world.DimensionType.NETHER) {
            return null;
        }

        BlockPos centre = RaidVillages.centreFor(this.world, at);
        VillageRaid raid = findRaid(centre);
        if (raid != null && raid.isOver()) {
            // A raid already won or lost is only showing its result: a new omen is a new raid, and
            // the old one makes way so villagers see only the raid that is coming.
            raid.stop();
            raid = null;
        }
        if (raid == null) {
            raid = new VillageRaid(this.nextId++, this.world, centre);
        }

        if (raid.isStarted() && raid.getBadOmenLevel() >= VillageRaid.MAX_LEVEL) {
            return null;
        }

        if (!this.byId.containsKey(raid.getId())) {
            this.byId.put(raid.getId(), raid);
            VillagerBackport.LOGGER.info("Raid {} begins at {}.", raid.getId(), centre);
        }
        raid.absorbOmen(omenLevel);
        markDirty();
        return raid;
    }

    /** A raider loaded from disk finds its raid again, or forgets it if that raid is over. */
    void rejoin(EntityLiving raider) {
        int id = RaiderData.raidId(raider);
        if (id == 0) {
            return;
        }
        VillageRaid raid = this.byId.get(id);
        if (raid == null || raid.isStopped()) {
            RaiderData.leave(raider);
            return;
        }
        raid.joinRaid(RaiderData.wave(raider), raider, false);
    }

    @Nullable
    public VillageRaid raidOf(Entity entity) {
        if (!(entity instanceof EntityLiving)) {
            return null;
        }
        int id = RaiderData.raidId((EntityLiving) entity);
        return id == 0 ? null : this.byId.get(id);
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        this.nextId = Math.max(1, nbt.getInteger("NextAvailableID"));
        this.tick = nbt.getInteger("Tick");
        this.pending = nbt.getTagList("Raids", 10);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        nbt.setInteger("NextAvailableID", this.nextId);
        nbt.setInteger("Tick", this.tick);
        NBTTagList list = new NBTTagList();
        for (VillageRaid raid : this.byId.values()) {
            list.appendTag(raid.write(new NBTTagCompound()));
        }
        if (this.pending != null) {
            for (int i = 0; i < this.pending.tagCount(); i++) {
                list.appendTag(this.pending.getCompoundTagAt(i));
            }
        }
        nbt.setTag("Raids", list);
        return nbt;
    }

    /** The view villagers see this manager's raids through. */
    static final RaidView VIEW = new RaidView() {
        @Nullable
        @Override
        public Raid raidAt(World world, BlockPos pos) {
            return world instanceof WorldServer ? get((WorldServer) world).findRaid(pos) : null;
        }

        @Override
        public boolean inRaid(Entity entity) {
            return entity.world instanceof WorldServer && get((WorldServer) entity.world).raidOf(entity) != null;
        }
    };
}
