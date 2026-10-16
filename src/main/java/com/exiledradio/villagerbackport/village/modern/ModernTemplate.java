package com.exiledradio.villagerbackport.village.modern;

import com.exiledradio.villagerbackport.VillagerBackport;

import com.google.common.base.Optional;
import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import javax.annotation.Nullable;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * One of 1.14's village structure files, converted to 1.12.2 blocks.
 *
 * <h2>Where these come from</h2>
 * Mojang's own {@code data/minecraft/structures/village/*.nbt}, run through the converter in
 * {@code tools/village-port}. That rewrites every block into 1.12.2's names and properties ahead of
 * time, so nothing here needs to know 1.14 exists, while keeping each block's original 1.14 name
 * beside it - 1.14's pool processors ("swap 80% of the cobblestone for mossy cobblestone") are
 * written against those names, and matching on the converted ones would merge blocks 1.14 told
 * apart, such as stripped and plain logs.
 *
 * <p>Jigsaw blocks are pulled out into their own list. In the world each one is replaced by its
 * final state, as 1.14's {@code JigsawReplacementStructureProcessor} does.
 */
public final class ModernTemplate {

    private static final Map<String, ModernTemplate> CACHE = new HashMap<String, ModernTemplate>();

    /** A missing template, so a bad pool entry costs one log line rather than a crash. */
    private static final ModernTemplate EMPTY = new ModernTemplate("empty", new NBTTagCompound());

    final String location;
    final BlockPos size;
    final PaletteEntry[] palette;
    final List<BlockEntry> blocks = new ArrayList<BlockEntry>();
    final List<Jigsaw> jigsaws = new ArrayList<Jigsaw>();
    final List<EntityEntry> entities = new ArrayList<EntityEntry>();

    /** One palette slot: the 1.12.2 state to place, and the 1.14 block it was. */
    static final class PaletteEntry {
        final String[] names;
        final NBTTagCompound props;
        final String source;
        final NBTTagCompound sourceProps;
        private IBlockState state;
        private boolean resolved;

        PaletteEntry(NBTTagCompound tag) {
            NBTTagList list = tag.getTagList("Names", 8);
            this.names = new String[list.tagCount()];
            for (int i = 0; i < this.names.length; i++) {
                this.names[i] = list.getStringTagAt(i);
            }
            this.props = tag.getCompoundTag("Props");
            this.source = tag.getString("Src");
            this.sourceProps = tag.getCompoundTag("SrcProps");
        }

        /** @return the state to place, or null for "place nothing" */
        @Nullable
        IBlockState state() {
            if (!this.resolved) {
                this.state = resolve(this.names, this.props);
                this.resolved = true;
            }
            return this.state;
        }

        boolean isAir() {
            IBlockState s = state();
            return s != null && s.getBlock() == Blocks.AIR;
        }
    }

    static final class BlockEntry {
        final BlockPos pos;
        final int state;
        @Nullable
        final NBTTagCompound nbt;

        BlockEntry(BlockPos pos, int state, @Nullable NBTTagCompound nbt) {
            this.pos = pos;
            this.state = state;
            this.nbt = nbt;
        }
    }

    /** A jigsaw connector, in template coordinates. */
    static final class Jigsaw {
        final BlockPos pos;
        final EnumFacing facing;
        final String pool;
        final String attachment;

        Jigsaw(BlockPos pos, EnumFacing facing, String pool, String attachment) {
            this.pos = pos;
            this.facing = facing;
            this.pool = pool;
            this.attachment = attachment;
        }
    }

    static final class EntityEntry {
        final double x;
        final double y;
        final double z;
        final BlockPos block;
        final NBTTagCompound tag;

        EntityEntry(NBTTagCompound tag) {
            NBTTagList pos = tag.getTagList("pos", 6);
            this.x = pos.getDoubleAt(0);
            this.y = pos.getDoubleAt(1);
            this.z = pos.getDoubleAt(2);
            int[] b = tag.getIntArray("block");
            this.block = new BlockPos(b[0], b[1], b[2]);
            this.tag = tag;
        }
    }

    /** A jigsaw placed in the world: where it is and which way it faces after rotation. */
    static final class PlacedJigsaw {
        final BlockPos pos;
        final EnumFacing facing;
        final Jigsaw source;

        PlacedJigsaw(BlockPos pos, EnumFacing facing, Jigsaw source) {
            this.pos = pos;
            this.facing = facing;
            this.source = source;
        }

        /** 1.14's {@code JigsawBlock.func_220171_a}: facing each other, and the same attachment type. */
        boolean attachesTo(PlacedJigsaw other) {
            return this.facing == other.facing.getOpposite() && this.source.attachment.equals(other.source.attachment);
        }
    }

    private ModernTemplate(String location, NBTTagCompound tag) {
        this.location = location;
        int[] s = tag.getIntArray("size");
        this.size = s.length == 3 ? new BlockPos(s[0], s[1], s[2]) : new BlockPos(1, 1, 1);

        NBTTagList pal = tag.getTagList("palette", 10);
        this.palette = new PaletteEntry[pal.tagCount()];
        for (int i = 0; i < this.palette.length; i++) {
            this.palette[i] = new PaletteEntry(pal.getCompoundTagAt(i));
        }

        NBTTagList blockList = tag.getTagList("blocks", 10);
        for (int i = 0; i < blockList.tagCount(); i++) {
            NBTTagCompound b = blockList.getCompoundTagAt(i);
            int[] p = b.getIntArray("pos");
            this.blocks.add(new BlockEntry(new BlockPos(p[0], p[1], p[2]), b.getInteger("state"),
                    b.hasKey("nbt") ? b.getCompoundTag("nbt") : null));
        }

        NBTTagList jigsawList = tag.getTagList("jigsaws", 10);
        for (int i = 0; i < jigsawList.tagCount(); i++) {
            NBTTagCompound j = jigsawList.getCompoundTagAt(i);
            int[] p = j.getIntArray("pos");
            this.jigsaws.add(new Jigsaw(new BlockPos(p[0], p[1], p[2]), EnumFacing.byName(j.getString("facing")),
                    j.getString("pool"), j.getString("attach")));
        }

        NBTTagList entityList = tag.getTagList("entities", 10);
        for (int i = 0; i < entityList.tagCount(); i++) {
            this.entities.add(new EntityEntry(entityList.getCompoundTagAt(i)));
        }
    }

    /** @param location as 1.14 names it, e.g. {@code village/plains/houses/plains_small_house_1} */
    public static synchronized ModernTemplate get(String location) {
        ModernTemplate template = CACHE.get(location);
        if (template == null) {
            template = load(location);
            CACHE.put(location, template);
        }
        return template;
    }

    private static ModernTemplate load(String location) {
        String path = "/assets/" + VillagerBackport.MOD_ID + "/structures/" + location + ".nbt";
        InputStream in = ModernTemplate.class.getResourceAsStream(path);
        if (in == null) {
            VillagerBackport.LOGGER.warn("Village structure {} is missing from the jar.", location);
            return EMPTY;
        }
        try {
            return new ModernTemplate(location, CompressedStreamTools.readCompressed(in));
        } catch (Exception e) {
            VillagerBackport.LOGGER.warn("Could not read village structure {}.", location, e);
            return EMPTY;
        } finally {
            try {
                in.close();
            } catch (Exception ignored) {
                // Nothing useful to do.
            }
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Placement geometry, matching 1.14's Template with no mirror and the pivot at the origin
    // ---------------------------------------------------------------------------------------------

    /** 1.14's {@code Template.getTransformedPos} for a rotation about the origin. */
    static BlockPos transform(BlockPos pos, Rotation rotation) {
        int x = pos.getX();
        int z = pos.getZ();
        switch (rotation) {
            case CLOCKWISE_90:
                return new BlockPos(-z, pos.getY(), x);
            case CLOCKWISE_180:
                return new BlockPos(-x, pos.getY(), -z);
            case COUNTERCLOCKWISE_90:
                return new BlockPos(z, pos.getY(), -x);
            default:
                return pos;
        }
    }

    /** The bounding box the template occupies when placed at {@code at} with {@code rotation}. */
    StructureBoundingBox boundingBox(BlockPos at, Rotation rotation) {
        BlockPos a = transform(BlockPos.ORIGIN, rotation);
        BlockPos b = transform(new BlockPos(this.size.getX() - 1, this.size.getY() - 1, this.size.getZ() - 1), rotation);
        return new StructureBoundingBox(
                Math.min(a.getX(), b.getX()) + at.getX(), Math.min(a.getY(), b.getY()) + at.getY(),
                Math.min(a.getZ(), b.getZ()) + at.getZ(),
                Math.max(a.getX(), b.getX()) + at.getX(), Math.max(a.getY(), b.getY()) + at.getY(),
                Math.max(a.getZ(), b.getZ()) + at.getZ());
    }

    /** The template's jigsaws placed at {@code at}, shuffled as 1.14 shuffles them. */
    List<PlacedJigsaw> jigsaws(BlockPos at, Rotation rotation, java.util.Random random) {
        List<PlacedJigsaw> out = new ArrayList<PlacedJigsaw>(this.jigsaws.size());
        for (Jigsaw j : this.jigsaws) {
            out.add(new PlacedJigsaw(transform(j.pos, rotation).add(at), rotation.rotate(j.facing), j));
        }
        if (random != null) {
            Collections.shuffle(out, random);
        }
        return out;
    }

    // ---------------------------------------------------------------------------------------------
    // Block resolution
    // ---------------------------------------------------------------------------------------------

    /**
     * Turns a converted palette entry into a state: the first of its candidate blocks that exists,
     * with every property that block understands applied.
     */
    @Nullable
    static IBlockState resolve(String[] names, NBTTagCompound props) {
        for (String name : names) {
            ResourceLocation id = new ResourceLocation(name);
            if (!ForgeRegistries.BLOCKS.containsKey(id)) {
                continue;
            }
            Block block = ForgeRegistries.BLOCKS.getValue(id);
            if (block == null) {
                continue;
            }
            IBlockState state = block.getDefaultState();
            for (String key : props.getKeySet()) {
                state = with(state, key, props.getString(key));
            }
            return state;
        }
        return null;
    }

    private static IBlockState with(IBlockState state, String name, String value) {
        IProperty<?> property = state.getBlock().getBlockState().getProperty(name);
        return property == null ? state : withValue(state, property, value);
    }

    private static <T extends Comparable<T>> IBlockState withValue(IBlockState state, IProperty<T> property, String value) {
        Optional<T> parsed = property.parseValue(value);
        return parsed.isPresent() ? state.withProperty(property, parsed.get()) : state;
    }
}
