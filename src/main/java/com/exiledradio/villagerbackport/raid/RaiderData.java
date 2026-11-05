package com.exiledradio.villagerbackport.raid;

import com.exiledradio.villagerbackport.VillagerBackport;

import net.minecraft.entity.EntityLiving;
import net.minecraft.nbt.NBTTagCompound;

/**
 * What a raider remembers about its raid: 1.14's {@code RaidId}, {@code Wave} and leader flag.
 *
 * <p>Kept in the entity's Forge data, so any mob can be a raider - 1.12.2's own illagers and
 * witches, and any other mod's - without this mod owning its class. Saved with the entity, so a
 * raider whose chunk unloads mid-raid rejoins when it loads again, as in 1.14.
 */
public final class RaiderData {

    private static final String RAID = "RaidId";
    private static final String WAVE = "RaidWave";
    private static final String LEADER = "RaidLeader";

    private RaiderData() {
    }

    /** @return the raid id, or 0 for none - 1.14 numbers raids from 1 */
    public static int raidId(EntityLiving entity) {
        return root(entity).getInteger(RAID);
    }

    public static int wave(EntityLiving entity) {
        return root(entity).getInteger(WAVE);
    }

    public static boolean isLeader(EntityLiving entity) {
        return root(entity).getBoolean(LEADER);
    }

    static void join(EntityLiving entity, int raidId, int wave) {
        NBTTagCompound root = root(entity);
        root.setInteger(RAID, raidId);
        root.setInteger(WAVE, wave);
        save(entity, root);
    }

    static void setLeader(EntityLiving entity, boolean leader) {
        NBTTagCompound root = root(entity);
        root.setBoolean(LEADER, leader);
        save(entity, root);
    }

    static void leave(EntityLiving entity) {
        NBTTagCompound root = root(entity);
        root.removeTag(RAID);
        root.removeTag(WAVE);
        save(entity, root);
    }

    private static NBTTagCompound root(EntityLiving entity) {
        return entity.getEntityData().getCompoundTag(VillagerBackport.MOD_ID);
    }

    private static void save(EntityLiving entity, NBTTagCompound root) {
        entity.getEntityData().setTag(VillagerBackport.MOD_ID, root);
    }
}
