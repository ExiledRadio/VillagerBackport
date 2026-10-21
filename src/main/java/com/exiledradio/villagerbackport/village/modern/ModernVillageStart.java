package com.exiledradio.villagerbackport.village.modern;

import com.exiledradio.villagerbackport.VillagerBackport;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.StructureStart;
import net.minecraftforge.common.BiomeDictionary;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Random;

/**
 * A 1.14 village, laid out the way 1.14 lays one out.
 *
 * <p>The layout is 1.14.4's {@code JigsawManager.Assembler}, line for line where 1.12.2 allows: a
 * town centre at a random rotation, then breadth-first through every jigsaw, six levels deep, each
 * jigsaw trying the pool's elements in weighted random order and each element in random rotations
 * until one fits. "Fits" is 1.14's free-space test - inside the 80-block cube around the centre, or
 * inside the parent for a piece attached to its inside, and clear of everything already placed.
 * 1.14 holds that space as a voxel shape; here it is the containing box and a list of boxes cut out
 * of it, which answers the same question.
 */
public class ModernVillageStart extends StructureStart {

    /** 1.14's {@code VillageConfig} size for every village type. */
    private static final int DEPTH = 6;

    /** Half the side of the cube a village must fit inside. */
    private static final int REACH = 80;

    public enum Type {
        PLAINS, DESERT, SAVANNA, TAIGA, SNOWY;

        String startPool() {
            return "village/" + name().toLowerCase(java.util.Locale.ROOT) + "/town_centers";
        }

        /** 1.14 gives each village biome its own type. A modded biome is placed by what it is like. */
        static Type of(Biome biome) {
            if (BiomeDictionary.hasType(biome, BiomeDictionary.Type.SNOWY)) {
                return SNOWY;
            }
            if (BiomeDictionary.hasType(biome, BiomeDictionary.Type.SANDY)
                    && !BiomeDictionary.hasType(biome, BiomeDictionary.Type.BEACH)) {
                return DESERT;
            }
            if (BiomeDictionary.hasType(biome, BiomeDictionary.Type.SAVANNA)) {
                return SAVANNA;
            }
            if (BiomeDictionary.hasType(biome, BiomeDictionary.Type.CONIFEROUS)) {
                return TAIGA;
            }
            return PLAINS;
        }

        /**
         * The kind an Open Terrain Generator biome config names ({@code VillageType}), or null for
         * none. OTG has no snowy kind, so a snowy biome's wood or taiga village becomes 1.14's snowy.
         */
        @javax.annotation.Nullable
        static Type ofOtg(String kind, @javax.annotation.Nullable Biome biome) {
            boolean snowy = biome != null && BiomeDictionary.hasType(biome, BiomeDictionary.Type.SNOWY);
            switch (kind.toLowerCase(java.util.Locale.ROOT)) {
                case "wood":
                    return snowy ? SNOWY : PLAINS;
                case "taiga":
                    return snowy ? SNOWY : TAIGA;
                case "sandstone":
                    return DESERT;
                case "savanna":
                    return SAVANNA;
                default:
                    return null;
            }
        }
    }

    /** For loading from disk. */
    public ModernVillageStart() {
    }

    public ModernVillageStart(World world, Random random, int chunkX, int chunkZ, TerrainHeights heights, Type type,
                              int size) {
        super(chunkX, chunkZ);
        new Assembler(heights, random, this.components).assemble(type.startPool(), new BlockPos(chunkX * 16, 0, chunkZ * 16));
        // Other mods' village buildings - Recurrent Complex's houses, Waystones' waystone - on plots
        // beside the streets. See ModVillagePieces.
        ModVillagePieces.add(world, this.components, heights, chunkX, chunkZ, size);
        this.updateBoundingBox();
    }

    /** 1.14 keeps every village, however few pieces fitted; there is no 1.12.2-style size cull. */
    @Override
    public boolean isSizeableStructure() {
        return !this.components.isEmpty();
    }

    /**
     * Saves 1.12.2's "Valid" flag too. Mods that read villages from the saved data look for it -
     * Antique Atlas treats a village without it as fake and erases what it drew there.
     */
    @Override
    public void writeToNBT(net.minecraft.nbt.NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setBoolean("Valid", isSizeableStructure());
    }

    // ---------------------------------------------------------------------------------------------

    /** Free space: a box with boxes cut out of it. */
    private static final class Space {
        private final AxisAlignedBB outer;
        private final List<AxisAlignedBB> taken = new ArrayList<AxisAlignedBB>();

        Space(AxisAlignedBB outer) {
            this.outer = outer;
        }

        /** 1.14 tests the candidate shrunk by a quarter block, so touching faces do not count. */
        boolean fits(StructureBoundingBox box) {
            AxisAlignedBB b = aabb(box).shrink(0.25D);
            if (b.minX < this.outer.minX || b.minY < this.outer.minY || b.minZ < this.outer.minZ
                    || b.maxX > this.outer.maxX || b.maxY > this.outer.maxY || b.maxZ > this.outer.maxZ) {
                return false;
            }
            for (AxisAlignedBB t : this.taken) {
                if (t.intersects(b)) {
                    return false;
                }
            }
            return true;
        }

        void take(StructureBoundingBox box) {
            this.taken.add(aabb(box));
        }
    }

    private static AxisAlignedBB aabb(StructureBoundingBox box) {
        return new AxisAlignedBB(box.minX, box.minY, box.minZ, box.maxX + 1, box.maxY + 1, box.maxZ + 1);
    }

    /** One piece waiting for its jigsaws to be filled. */
    private static final class Entry {
        final ModernVillagePiece piece;
        final Space space;
        final int depth;

        Entry(ModernVillagePiece piece, Space space, int depth) {
            this.piece = piece;
            this.space = space;
            this.depth = depth;
        }
    }

    private static final class Assembler {
        private final TerrainHeights heights;
        private final Random random;
        private final List<net.minecraft.world.gen.structure.StructureComponent> pieces;
        private final Deque<Entry> queue = new ArrayDeque<Entry>();

        Assembler(TerrainHeights heights, Random random, List<net.minecraft.world.gen.structure.StructureComponent> pieces) {
            this.heights = heights;
            this.random = random;
            this.pieces = pieces;
        }

        void assemble(String startPool, BlockPos origin) {
            ModernPools.Pool pool = ModernPools.get(startPool);
            if (pool == null || pool.size() == 0) {
                VillagerBackport.LOGGER.warn("No village start pool {}.", startPool);
                return;
            }
            Rotation rotation = Rotation.values()[this.random.nextInt(Rotation.values().length)];
            ModernPools.Element element = pool.random(this.random);
            StructureBoundingBox box = element.boundingBox(origin, rotation);
            ModernVillagePiece start = new ModernVillagePiece(element, origin, element.groundLevelDelta(), rotation, box);

            int cx = (box.maxX + box.minX) / 2;
            int cz = (box.maxZ + box.minZ) / 2;
            int ground = this.heights.height(cx, cz);
            start.offset(0, ground - (box.minY + start.groundLevelDelta()), 0);
            this.pieces.add(start);

            Space space = new Space(new AxisAlignedBB(cx - REACH, ground - REACH, cz - REACH,
                    cx + REACH + 1, ground + REACH + 1, cz + REACH + 1));
            space.take(start.getBoundingBox());
            this.queue.addLast(new Entry(start, space, 0));

            while (!this.queue.isEmpty()) {
                Entry entry = this.queue.removeFirst();
                expand(entry.piece, entry.space, entry.depth);
            }
        }

        private void expand(ModernVillagePiece parent, Space parentSpace, int depth) {
            ModernPools.Element element = parent.element();
            boolean parentRigid = element.projection == ModernPools.Projection.RIGID;
            StructureBoundingBox parentBox = parent.getBoundingBox();
            int parentMinY = parentBox.minY;
            Space inside = null;

            jigsaws:
            for (ModernTemplate.PlacedJigsaw jigsaw : element.jigsaws(parent.pos(), parent.rotation(), this.random)) {
                EnumFacing facing = jigsaw.facing;
                BlockPos from = jigsaw.pos;
                BlockPos target = from.offset(facing);
                int fromRelY = from.getY() - parentMinY;
                int surface = -1;

                ModernPools.Pool pool = ModernPools.get(jigsaw.source.pool);
                if (pool == null || (pool.size() == 0 && !ModernPools.EMPTY.equals(pool.name))) {
                    VillagerBackport.LOGGER.debug("Empty or missing village pool {}.", jigsaw.source.pool);
                    continue;
                }
                ModernPools.Pool fallback = ModernPools.get(pool.fallback);

                Space space;
                if (parentBox.isVecInside(target)) {
                    if (inside == null) {
                        inside = new Space(aabb(parentBox));
                    }
                    space = inside;
                } else {
                    space = parentSpace;
                }

                List<ModernPools.Element> candidates = new ArrayList<ModernPools.Element>();
                if (depth != DEPTH) {
                    candidates.addAll(pool.shuffled(this.random));
                }
                if (fallback != null) {
                    candidates.addAll(fallback.shuffled(this.random));
                }

                for (ModernPools.Element candidate : candidates) {
                    if (candidate.isEmpty()) {
                        break;
                    }

                    for (Rotation rotation : shuffledRotations()) {
                        List<ModernTemplate.PlacedJigsaw> theirs = candidate.jigsaws(BlockPos.ORIGIN, rotation, this.random);
                        StructureBoundingBox bare = candidate.boundingBox(BlockPos.ORIGIN, rotation);
                        int headroom = headroom(theirs, bare);

                        for (ModernTemplate.PlacedJigsaw their : theirs) {
                            if (!jigsaw.attachesTo(their)) {
                                continue;
                            }
                            BlockPos theirPos = their.pos;
                            BlockPos at = new BlockPos(target.getX() - theirPos.getX(), target.getY() - theirPos.getY(),
                                    target.getZ() - theirPos.getZ());
                            StructureBoundingBox box = candidate.boundingBox(at, rotation);
                            boolean childRigid = candidate.projection == ModernPools.Projection.RIGID;
                            int theirY = theirPos.getY();
                            int dy = fromRelY - theirY + facing.getYOffset();

                            int minY;
                            if (parentRigid && childRigid) {
                                minY = parentMinY + dy;
                            } else {
                                if (surface == -1) {
                                    surface = this.heights.height(from.getX(), from.getZ());
                                }
                                minY = surface - theirY;
                            }

                            int shift = minY - box.minY;
                            StructureBoundingBox placed = new StructureBoundingBox(box);
                            placed.offset(0, shift, 0);
                            BlockPos placedAt = at.add(0, shift, 0);
                            if (headroom > 0) {
                                int height = Math.max(headroom + 1, placed.maxY - placed.minY);
                                placed.maxY = placed.minY + height;
                            }

                            if (space.fits(placed)) {
                                space.take(placed);
                                int groundLevelDelta = childRigid ? parent.groundLevelDelta() - dy : candidate.groundLevelDelta();
                                ModernVillagePiece child = new ModernVillagePiece(candidate, placedAt, groundLevelDelta, rotation, placed);
                                this.pieces.add(child);
                                if (depth + 1 <= DEPTH) {
                                    this.queue.addLast(new Entry(child, space, depth + 1));
                                }
                                continue jigsaws;
                            }
                        }
                    }
                }
            }
        }

        /**
         * 1.14 reserves room above a short piece for whatever its inward-facing jigsaws will bring -
         * the villager standing in a house, say - so that space is not handed to a neighbour first.
         */
        private int headroom(List<ModernTemplate.PlacedJigsaw> jigsaws, StructureBoundingBox bare) {
            if (bare.getYSize() > 16) {
                return 0;
            }
            int max = 0;
            for (ModernTemplate.PlacedJigsaw j : jigsaws) {
                if (!bare.isVecInside(j.pos.offset(j.facing))) {
                    continue;
                }
                ModernPools.Pool p = ModernPools.get(j.source.pool);
                ModernPools.Pool f = p == null ? null : ModernPools.get(p.fallback);
                max = Math.max(max, Math.max(p == null ? 0 : p.maxHeight(), f == null ? 0 : f.maxHeight()));
            }
            return max;
        }

        private List<Rotation> shuffledRotations() {
            List<Rotation> rotations = new ArrayList<Rotation>(Arrays.asList(Rotation.values()));
            Collections.shuffle(rotations, this.random);
            return rotations;
        }
    }
}
