package com.exiledradio.villagerbackport.block;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapData;

import javax.annotation.Nullable;

/**
 * 1.14's locked maps: a map that never changes again.
 *
 * <h2>How, without a {@code locked} field</h2>
 * 1.12.2's {@code MapData} has nowhere to say "locked", and the update a lock has to stop happens
 * inside {@code ItemMap.onUpdate}, which has no hook. But that update already has a condition: it
 * only draws while the holder stands in the dimension the map belongs to. A locked map is therefore
 * a copy of the map whose dimension is one no world uses. Nothing can ever stand in it, so nothing
 * ever draws on it again, and the vanilla code needs no change at all.
 *
 * <p>As in 1.14, locking makes a new map rather than freezing the old one: the map that went in
 * keeps updating everywhere else it is held, and the locked one is a snapshot with a map id of its
 * own. Copies of a locked map share its id, so they are locked too.
 *
 * <p>One difference from 1.14: a locked map does not show its holder's position, because the same
 * dimension test decides whether the holder's marker is drawn.
 */
public final class MapLocking {

    /** "LOCK". A dimension id no world has, so nothing is ever drawn onto a locked map. */
    public static final int LOCKED_DIMENSION = 0x4C4F434B;

    /** On the stack too, so the client can say "Locked" - the dimension is never sent to it. */
    public static final String LOCKED_TAG = "villagerbackport:locked";

    /** Asks the result slot to lock the map when it is taken, as the zoom tag asks vanilla to zoom. */
    static final String LOCK_REQUEST = "villagerbackport:lock";

    private MapLocking() {
    }

    /** @return true if this map is locked, judged from its data where that is available */
    public static boolean isLocked(ItemStack stack, @Nullable MapData data) {
        if (data != null && data.dimension == LOCKED_DIMENSION) {
            return true;
        }
        return stack.hasTagCompound() && stack.getTagCompound().getBoolean(LOCKED_TAG);
    }

    /** @return the map marked to be locked once it is taken out of the table */
    static ItemStack lockRequest(ItemStack map) {
        ItemStack request = map.copy();
        request.setCount(1);
        if (!request.hasTagCompound()) {
            request.setTagCompound(new NBTTagCompound());
        }
        request.getTagCompound().setBoolean(LOCK_REQUEST, true);
        request.getTagCompound().setBoolean(LOCKED_TAG, true);
        return request;
    }

    /**
     * Turns a lock request into a locked map: a new map id holding a snapshot of the old one.
     *
     * <p>Server side only. The client keeps the request until the server's copy of the stack arrives.
     */
    static void complete(ItemStack stack, World world) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null || !tag.getBoolean(LOCK_REQUEST)) {
            return;
        }
        tag.removeTag(LOCK_REQUEST);
        if (world.isRemote) {
            return;
        }

        MapData source = Items.FILLED_MAP.getMapData(stack, world);
        if (source == null || source.dimension == LOCKED_DIMENSION) {
            return;
        }

        int id = world.getUniqueDataId("map");
        String name = "map_" + id;
        MapData locked = new MapData(name);
        locked.xCenter = source.xCenter;
        locked.zCenter = source.zCenter;
        locked.scale = source.scale;
        locked.trackingPosition = source.trackingPosition;
        locked.unlimitedTracking = source.unlimitedTracking;
        locked.dimension = LOCKED_DIMENSION;
        System.arraycopy(source.colors, 0, locked.colors, 0, source.colors.length);
        locked.markDirty();
        world.setData(name, locked);

        stack.setItemDamage(id);
    }
}
