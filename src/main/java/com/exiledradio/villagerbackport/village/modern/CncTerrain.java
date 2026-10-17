package com.exiledradio.villagerbackport.village.modern;

import com.exiledradio.villagerbackport.VillagerBackport;

import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Terrain heights in a Caves Not Cliffs world.
 *
 * <p>That mod builds 1.18's terrain itself, from -64 to 319, and keeps each chunk's finished
 * terrain - after surface rules, before any feature - as a column of material ids, which it works
 * out on its own threads and caches. Reading the column for a chunk that does not exist yet gives
 * exactly the ground 1.14 lays a village out on, and asking early only means the column is ready
 * when the chunk itself is made.
 *
 * <p>The column cache and its block mapping are private to that mod, so they are reached by
 * reflection. If a later version moves them, villages in its worlds stay 1.12.2's rather than being
 * built at the wrong height.
 */
final class CncTerrain implements ColumnTerrain {

    private static final String GENERATOR = "net.celestiald.cavesnotcliffs.world.V118ChunkGenerator";

    private final Object columns;
    private final Object blockStates;
    private final MethodHandle column;
    private final MethodHandle materialId;
    private final MethodHandle stateFor;
    private final int top;
    private final int bottom;

    /** Material id to "is there ground here", filled in as ids turn up. 0 unknown, 1 air, 2 ground. */
    private byte[] solid = new byte[64];

    /** The column last read, since a layout asks about many blocks of the same chunk in a row. */
    private Object lastColumn;
    private long lastKey = Long.MIN_VALUE;

    private CncTerrain(Object columns, Object blockStates, MethodHandle column, MethodHandle materialId,
            MethodHandle stateFor, int bottom, int top) {
        this.columns = columns;
        this.blockStates = blockStates;
        this.column = column;
        this.materialId = materialId;
        this.stateFor = stateFor;
        this.bottom = bottom;
        this.top = top;
    }

    /** @return a reader for this world's terrain, or null if it is not a Caves Not Cliffs world or cannot be read */
    @Nullable
    static CncTerrain of(World world) {
        Object generator;
        try {
            Class<?> type = Class.forName(GENERATOR, false, CncTerrain.class.getClassLoader());
            generator = type.getMethod("forWorld", World.class).invoke(null, world);
        } catch (ClassNotFoundException e) {
            return null;
        } catch (Exception e) {
            VillagerBackport.LOGGER.warn("Caves Not Cliffs' generator could not be looked up; 1.14 villages "
                    + "cannot be laid out in its worlds.", e);
            return null;
        }
        if (generator == null) {
            return null;
        }
        try {
            Class<?> type = generator.getClass();
            Object columns = field(type, "columns").get(generator);
            Object blockStates = field(type, "blockStates").get(generator);
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            Method column = columns.getClass().getMethod("column", int.class, int.class);
            Method materialId = column.getReturnType().getMethod("materialId", int.class, int.class, int.class);
            Method stateFor = blockStates.getClass().getMethod("stateFor", int.class);
            Class<?> columnType = column.getReturnType();
            int bottom = columnType.getField("MIN_Y").getInt(null);
            int top = columnType.getField("MAX_Y").getInt(null);
            VillagerBackport.LOGGER.info("Laying out 1.14 villages on Caves Not Cliffs' terrain.");
            return new CncTerrain(columns, blockStates, lookup.unreflect(column), lookup.unreflect(materialId),
                    lookup.unreflect(stateFor), bottom, top);
        } catch (Exception e) {
            VillagerBackport.LOGGER.warn("Caves Not Cliffs' terrain could not be read, so villages in its worlds "
                    + "stay 1.12.2's.", e);
            return null;
        }
    }

    private static Field field(Class<?> type, String name) throws NoSuchFieldException {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    @Override
    public int height(int x, int z) {
        try {
            long key = ((long) (x >> 4) << 32) ^ ((z >> 4) & 0xFFFFFFFFL);
            if (key != this.lastKey) {
                this.lastColumn = this.column.invoke(this.columns, x >> 4, z >> 4);
                this.lastKey = key;
            }
            int lx = x & 15;
            int lz = z & 15;
            for (int y = this.top; y >= this.bottom; y--) {
                int id = (int) this.materialId.invoke(this.lastColumn, lx, y, lz);
                if (isGround(id)) {
                    return y + 1;
                }
            }
            return this.bottom;
        } catch (Throwable e) {
            VillagerBackport.LOGGER.warn("Reading Caves Not Cliffs' terrain failed at {}, {}.", x, z, e);
            return 64;
        }
    }

    /** Anything but air counts, water included, as in 1.14. */
    private boolean isGround(int id) throws Throwable {
        if (id >= this.solid.length) {
            byte[] bigger = new byte[Math.max(id + 1, this.solid.length * 2)];
            System.arraycopy(this.solid, 0, bigger, 0, this.solid.length);
            this.solid = bigger;
        }
        if (this.solid[id] == 0) {
            IBlockState state = (IBlockState) this.stateFor.invoke(this.blockStates, id);
            this.solid[id] = (byte) (state == null || state.getMaterial() == Material.AIR ? 1 : 2);
        }
        return this.solid[id] == 2;
    }
}
