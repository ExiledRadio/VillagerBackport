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
 *
 * <h2>1.18.2's versions</h2>
 * Mojang changed some of the files after 1.14: more jigsaws on the outpost's feature plate and on some houses
 * and streets, terminators that ask for nothing, a resized savanna mason, fixed beds, doors and snow. Where the
 * world generator lays villages out as 1.18.2 does, the same converter's output of 1.18.2's files is used
 * instead, from {@code structures/v1_18/}, which holds only the files that changed.
 */
public final class ModernTemplate {

    private static final Map<String, ModernTemplate> CACHE = new HashMap<String, ModernTemplate>();
    private static final Map<String, ModernTemplate> CACHE_118 = new HashMap<String, ModernTemplate>();

    /** Where 1.18.2's changed templates are kept, beside 1.14's. */
    private static final String V118 = "v1_18/";

    /** A missing template, so a bad pool entry costs one log line rather than a crash. */
    private static final ModernTemplate EMPTY = new ModernTemplate("empty", new NBTTagCompound());

    final String location;
    final BlockPos size;
    final PaletteEntry[] palette;
    final List<BlockEntry> blocks = new ArrayList<BlockEntry>();
    final List<Jigsaw> jigsaws = new ArrayList<Jigsaw>();
    private List<Jigsaw> sortedJigsaws;
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

            // The structure files were converted before the bell knew how it hangs; 1.14's own
            // property is still beside the converted ones, and the names are the same.
            if ("minecraft:bell".equals(this.source) && this.sourceProps.hasKey("attachment")
                    && !this.props.hasKey("attachment")) {
                this.props.setString("attachment", this.sourceProps.getString("attachment"));
            }
            // Likewise the grindstone's face, from before it could hang on a wall.
            if ("minecraft:grindstone".equals(this.source) && this.sourceProps.hasKey("face")
                    && !this.props.hasKey("face")) {
                this.props.setString("face", this.sourceProps.getString("face"));
            }
        }

        /** @return the state to place, or null for "place nothing" */
        @Nullable
        IBlockState state() {
            if (!this.resolved) {
                IBlockState backport = BackportBlocks.resolve(this.source, this.sourceProps, this.props);
                this.state = backport != null ? backport : resolve(this.names, this.props);
                // With Future MC's blocks kept, its barrel, lantern or bell goes in place of this
                // mod's hidden one, keeping 1.14's own properties.
                if (this.state != null) {
                    java.util.Map<String, String> extra = new java.util.HashMap<String, String>();
                    for (String key : this.sourceProps.getKeySet()) {
                        extra.put(key, this.sourceProps.getString(key));
                    }
                    this.state = com.exiledradio.villagerbackport.compat.FutureMc.preferred(this.state, extra);
                }
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

    /**
     * A jigsaw connector, in template coordinates.
     *
     * <p>1.14 gives a jigsaw one attachment type, and two jigsaws join when theirs are equal. 1.16 and later give it a
     * name and a target instead, and it joins a jigsaw named its target; Mojang's upgrade of 1.14's files sets both to
     * the old attachment type, so a 1.14 file reads here as that. A 1.16 jigsaw also has a top, the way it is turned
     * about its front, and a joint: a rollable one joins whatever way the other is turned, an aligned one only a
     * jigsaw turned the same way. 1.14's horizontal jigsaws are aligned with their top up, its vertical ones rollable.
     */
    static final class Jigsaw {
        final BlockPos pos;
        final EnumFacing facing;
        final String pool;
        final String name;
        final String target;
        /** null where it never matters: a rollable 1.14 jigsaw. */
        @Nullable
        final EnumFacing top;
        final boolean rollable;

        /** A 1.14 jigsaw. */
        Jigsaw(BlockPos pos, EnumFacing facing, String pool, String attachment) {
            this(pos, facing, pool, attachment, attachment, facing.getAxis().isHorizontal() ? EnumFacing.UP : null, "");
        }

        /** @param joint 1.16's "rollable" or "aligned", or "" for the default: aligned when horizontal */
        Jigsaw(BlockPos pos, EnumFacing facing, String pool, String name, String target, @Nullable EnumFacing top,
               String joint) {
            this.pos = pos;
            this.facing = facing;
            this.pool = pool;
            this.name = name;
            this.target = target;
            this.top = top;
            this.rollable = joint.isEmpty() ? !facing.getAxis().isHorizontal() : "rollable".equals(joint);
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
        @Nullable
        final EnumFacing top;
        final Jigsaw source;

        PlacedJigsaw(BlockPos pos, EnumFacing facing, @Nullable EnumFacing top, Jigsaw source) {
            this.pos = pos;
            this.facing = facing;
            this.top = top;
            this.source = source;
        }

        /**
         * 1.18.2's {@code JigsawBlock.canAttach}, with this the parent's jigsaw: facing each other, turned alike unless
         * this one is rollable, and the other named this one's target. For 1.14's files that is 1.14's
         * {@code JigsawBlock.func_220171_a}: facing each other, and the same attachment type.
         */
        boolean attachesTo(PlacedJigsaw other) {
            return this.facing == other.facing.getOpposite()
                    && (this.source.rollable || this.top == other.top)
                    && this.source.target.equals(other.source.name);
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
            BlockPos pos = new BlockPos(p[0], p[1], p[2]);
            EnumFacing facing = EnumFacing.byName(j.getString("facing"));
            if (j.hasKey("target")) {
                this.jigsaws.add(new Jigsaw(pos, facing, j.getString("pool"), j.getString("name"), j.getString("target"),
                        EnumFacing.byName(j.getString("top")), j.getString("joint")));
            } else {
                this.jigsaws.add(new Jigsaw(pos, facing, j.getString("pool"), j.getString("attach")));
            }
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

    /**
     * @param modern 1.18.2's version where it differs from 1.14's, for villages laid out as 1.18.2 lays them out;
     *               1.14's file where Mojang left it alone
     */
    public static synchronized ModernTemplate get(String location, boolean modern) {
        if (!modern) {
            return get(location);
        }
        ModernTemplate template = CACHE_118.get(location);
        if (template == null) {
            template = ModernTemplate.class.getResource(path(V118 + location)) != null ? load(V118 + location) : get(location);
            CACHE_118.put(location, template);
        }
        return template;
    }

    private static String path(String location) {
        return "/assets/" + VillagerBackport.MOD_ID + "/structures/" + location + ".nbt";
    }

    private static ModernTemplate load(String location) {
        String path = path(location);
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

    /**
     * The template's jigsaws placed at {@code at}, shuffled as 1.14 shuffles them.
     *
     * <p>What the shuffle starts from decides which jigsaw it lands on. 1.14 lists them as the structure file does;
     * {@code sorted} lists them as 1.18.2 does, whose {@code StructureTemplate.buildInfoList} sorts a template's
     * blocks by y, then x, then z, in template coordinates.
     */
    List<PlacedJigsaw> jigsaws(BlockPos at, Rotation rotation, java.util.Random random, boolean sorted) {
        List<Jigsaw> source = sorted ? sortedJigsaws() : this.jigsaws;
        List<PlacedJigsaw> out = new ArrayList<PlacedJigsaw>(source.size());
        for (Jigsaw j : source) {
            out.add(new PlacedJigsaw(transform(j.pos, rotation).add(at), rotation.rotate(j.facing),
                    j.top == null ? null : rotation.rotate(j.top), j));
        }
        if (random != null) {
            Collections.shuffle(out, random);
        }
        return out;
    }

    private synchronized List<Jigsaw> sortedJigsaws() {
        if (this.sortedJigsaws == null) {
            List<Jigsaw> sorted = new ArrayList<Jigsaw>(this.jigsaws);
            sorted.sort(java.util.Comparator.<Jigsaw>comparingInt(j -> j.pos.getY())
                    .thenComparingInt(j -> j.pos.getX()).thenComparingInt(j -> j.pos.getZ()));
            this.sortedJigsaws = sorted;
        }
        return this.sortedJigsaws;
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
