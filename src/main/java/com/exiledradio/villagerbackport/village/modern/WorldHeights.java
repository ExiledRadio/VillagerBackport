package com.exiledradio.villagerbackport.village.modern;

import com.exiledradio.villagerbackport.VillagerBackport;

import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

import java.lang.reflect.Method;

/**
 * A world's build height range, and where in a chunk each height is stored.
 *
 * <p>1.12.2 builds from y 0 to 255, sixteen sections a chunk. Farmer's Future Delight's Caves &amp;
 * Cliffs world type stretches that to 1.18's -64 to 319: Forge's own world methods are patched to
 * accept the new heights, and a chunk gets eight more sections - y -64 to -1 stored in slots 16 to
 * 19, y 256 to 319 in 20 to 23. Village generation reads and writes across the whole range, so it
 * asks here rather than assuming 0 to 255. Without that mod, or in any other world, the answers are
 * vanilla's.
 */
public final class WorldHeights {

    private static final String FFD_HOOKS = "xy177.farmersfuturedelight.core.FFDHeightHooks";

    private static boolean looked;
    private static Method minY;
    private static Method maxYExclusive;
    private static Method storageIndex;
    private static Method enterFullHeight;

    private WorldHeights() {
    }

    private static synchronized void lookUp() {
        if (looked) {
            return;
        }
        looked = true;
        try {
            Class<?> hooks = Class.forName(FFD_HOOKS, false, WorldHeights.class.getClassLoader());
            minY = hooks.getMethod("minY", World.class);
            maxYExclusive = hooks.getMethod("maxYExclusive", World.class);
            storageIndex = hooks.getMethod("storageIndex", int.class, World.class);
            enterFullHeight = hooks.getMethod("enterExtendedWorldgenHeight");
            VillagerBackport.LOGGER.info("Farmer's Future Delight found; villages follow its world heights.");
        } catch (ClassNotFoundException e) {
            // Not installed: vanilla heights.
        } catch (Exception e) {
            VillagerBackport.LOGGER.warn("Farmer's Future Delight's height API has changed; assuming vanilla heights.", e);
            minY = null;
            maxYExclusive = null;
            storageIndex = null;
            enterFullHeight = null;
        }
    }

    /**
     * Opens the whole height range for world generation, until the returned scope is closed.
     *
     * <p>Farmer's Future Delight places vanilla structures - villages among them - inside a "legacy"
     * scope that clips the world back to 0 to 255, so structures written for 1.12.2 cannot wander into
     * the new depths. A 1.14 village has to reach the real terrain, which can sit above 255 or, in a
     * deep valley, below 0, so it asks for the full range the way that mod's own compatibility list
     * does for Village Names. Anywhere else this does nothing.
     */
    public static AutoCloseable enterFullHeight() {
        lookUp();
        if (enterFullHeight != null) {
            try {
                Object scope = enterFullHeight.invoke(null);
                if (scope instanceof AutoCloseable) {
                    return (AutoCloseable) scope;
                }
            } catch (Exception ignored) {
                // Fall through.
            }
        }
        return NOTHING;
    }

    private static final AutoCloseable NOTHING = new AutoCloseable() {
        @Override
        public void close() {
        }
    };

    /** @return the lowest buildable y */
    public static int minY(World world) {
        lookUp();
        if (minY != null) {
            try {
                return (Integer) minY.invoke(null, world);
            } catch (Exception ignored) {
                // Fall through to vanilla.
            }
        }
        return 0;
    }

    /** @return one above the highest buildable y */
    public static int maxYExclusive(World world) {
        lookUp();
        if (maxYExclusive != null) {
            try {
                return (Integer) maxYExclusive.invoke(null, world);
            } catch (Exception ignored) {
                // Fall through to vanilla.
            }
        }
        return world.getHeight();
    }

    /** @return the chunk section slot holding this y, or -1 if it is outside the world */
    public static int storageIndex(World world, int y) {
        lookUp();
        if (storageIndex != null) {
            try {
                return (Integer) storageIndex.invoke(null, y, world);
            } catch (Exception ignored) {
                // Fall through to vanilla.
            }
        }
        return y >= 0 && y < 256 ? y >> 4 : -1;
    }

    /** @return the top of the highest section with anything in it, or {@link #minY} if the column is empty */
    public static int topOfFilledSections(World world, Chunk chunk) {
        ExtendedBlockStorage[] storage = chunk.getBlockStorageArray();
        int bottom = minY(world);
        for (int y = maxYExclusive(world) - 16; y >= bottom; y -= 16) {
            int index = storageIndex(world, y);
            if (index >= 0 && index < storage.length && storage[index] != Chunk.NULL_BLOCK_STORAGE
                    && !storage[index].isEmpty()) {
                return (y & ~15) + 15;
            }
        }
        return bottom;
    }
}
