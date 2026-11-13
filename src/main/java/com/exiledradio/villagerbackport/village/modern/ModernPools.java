package com.exiledradio.villagerbackport.village.modern;

import com.exiledradio.villagerbackport.VillagerBackport;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureBoundingBox;

import javax.annotation.Nullable;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 1.14's village template pools: {@code JigsawPattern}, its elements and its rule processors.
 *
 * <p>Read from {@code pools.json}, which the converter generates from 1.14.4's five
 * {@code *VillagePools} classes, so weights, fallbacks and processor rules are 1.14's own.
 */
public final class ModernPools {

    public static final String EMPTY = "empty";

    private static Map<String, Pool> pools;

    public enum Projection {
        RIGID,
        TERRAIN_MATCHING
    }

    /** {@code JigsawPattern}: a weighted list of elements and the pool to fall back on. */
    public static final class Pool {
        final String name;
        final String fallback;
        /** Each element repeated by its weight, as 1.14 stores them. */
        final List<Element> expanded = new ArrayList<Element>();
        /** Each distinct element once, in file order. Pieces save their element as an index into this. */
        final List<Element> distinct = new ArrayList<Element>();
        private int maxHeight = Integer.MIN_VALUE;
        private int maxHeight118 = Integer.MIN_VALUE;

        Pool(String name, String fallback) {
            this.name = name;
            this.fallback = fallback;
        }

        /** {@code func_214944_a}: one element at random, weighted. */
        Element random(Random random) {
            return this.expanded.get(random.nextInt(this.expanded.size()));
        }

        /** {@code func_214943_b}: every weighted entry, shuffled. */
        List<Element> shuffled(Random random) {
            return shuffled(random, false);
        }

        /**
         * @param fastutil shuffled as 1.18.2's {@code StructureTemplatePool.getShuffledTemplates} does, with fastutil's
         *                 {@code ObjectArrays.shuffle}: the same swaps as {@code Collections.shuffle}, but it also draws
         *                 {@code nextInt(1)} for the last element, so the random ends one draw further on
         */
        List<Element> shuffled(Random random, boolean fastutil) {
            List<Element> copy = new ArrayList<Element>(this.expanded);
            if (!fastutil) {
                Collections.shuffle(copy, random);
                return copy;
            }
            for (int i = copy.size(); i-- != 0; ) {
                int p = random.nextInt(i + 1);
                copy.set(p, copy.set(i, copy.get(p)));
            }
            return copy;
        }

        int size() {
            return this.expanded.size();
        }

        /**
         * {@code func_214945_a}: the tallest element, for reserving room above a piece.
         *
         * @param modern measured on 1.18.2's templates (see {@link ModernTemplate#get(String, boolean)})
         */
        int maxHeight(boolean modern) {
            int cached = modern ? this.maxHeight118 : this.maxHeight;
            if (cached == Integer.MIN_VALUE) {
                int max = 0;
                for (Element e : this.expanded) {
                    StructureBoundingBox box = e.boundingBox(BlockPos.ORIGIN, Rotation.NONE, modern);
                    max = Math.max(max, box.getYSize());
                }
                cached = max;
                if (modern) {
                    this.maxHeight118 = max;
                } else {
                    this.maxHeight = max;
                }
            }
            return cached;
        }
    }

    /** {@code JigsawPiece}: one thing a pool can place. */
    public abstract static class Element {
        final Projection projection;
        /** Set once the element is in a pool, so a piece can save it by pool and index. */
        Pool pool;
        int index;

        Element(Projection projection) {
            this.projection = projection;
        }

        /**
         * The element's jigsaws placed at {@code at}, shuffled. {@code modern}: from 1.18.2's templates (see
         * {@link ModernTemplate#get(String, boolean)}), listed in 1.18.2's order before the shuffle (see
         * {@link ModernTemplate#jigsaws}), not 1.14's.
         */
        abstract List<ModernTemplate.PlacedJigsaw> jigsaws(BlockPos at, Rotation rotation, Random random, boolean modern);

        final List<ModernTemplate.PlacedJigsaw> jigsaws(BlockPos at, Rotation rotation, Random random) {
            return jigsaws(at, rotation, random, false);
        }

        /** @param modern the box of 1.18.2's template rather than 1.14's */
        abstract StructureBoundingBox boundingBox(BlockPos at, Rotation rotation, boolean modern);

        final StructureBoundingBox boundingBox(BlockPos at, Rotation rotation) {
            return boundingBox(at, rotation, false);
        }

        /** 1.14's default ground level delta, used for every element type. */
        int groundLevelDelta() {
            return 1;
        }

        boolean isEmpty() {
            return false;
        }
    }

    public static final class EmptyElement extends Element {
        EmptyElement() {
            super(Projection.RIGID);
        }

        @Override
        List<ModernTemplate.PlacedJigsaw> jigsaws(BlockPos at, Rotation rotation, Random random, boolean modern) {
            return Collections.emptyList();
        }

        @Override
        StructureBoundingBox boundingBox(BlockPos at, Rotation rotation, boolean modern) {
            return new StructureBoundingBox(at, at);
        }

        @Override
        boolean isEmpty() {
            return true;
        }
    }

    /** {@code SingleJigsawPiece}: one structure file, with its processors. */
    public static final class SingleElement extends Element {
        final String location;
        final List<List<Rule>> processors;

        /**
         * 1.14's {@code IntegrityProcessor}: the share of the template's blocks that are placed, each
         * kept or dropped by a roll seeded from its position. 1 places them all.
         */
        float integrity = 1.0F;

        SingleElement(String location, List<List<Rule>> processors, Projection projection) {
            super(projection);
            this.location = location;
            this.processors = processors;
        }

        ModernTemplate template() {
            return template(false);
        }

        /** @param modern 1.18.2's version of the template, where Mojang changed it after 1.14 */
        ModernTemplate template(boolean modern) {
            return ModernTemplate.get(this.location, modern);
        }

        @Override
        List<ModernTemplate.PlacedJigsaw> jigsaws(BlockPos at, Rotation rotation, Random random, boolean modern) {
            return template(modern).jigsaws(at, rotation, random, modern);
        }

        @Override
        StructureBoundingBox boundingBox(BlockPos at, Rotation rotation, boolean modern) {
            return template(modern).boundingBox(at, rotation);
        }
    }

    /** {@code FeatureJigsawPiece}: a tree, a hay pile, a patch of flowers. One block, facing down. */
    public static final class FeatureElement extends Element {
        final String feature;
        private final ModernTemplate.Jigsaw jigsaw;

        FeatureElement(String feature, Projection projection) {
            super(projection);
            this.feature = feature;
            this.jigsaw = new ModernTemplate.Jigsaw(BlockPos.ORIGIN, net.minecraft.util.EnumFacing.DOWN, EMPTY,
                    "minecraft:bottom");
        }

        @Override
        List<ModernTemplate.PlacedJigsaw> jigsaws(BlockPos at, Rotation rotation, Random random, boolean modern) {
            return Collections.singletonList(new ModernTemplate.PlacedJigsaw(at, net.minecraft.util.EnumFacing.DOWN, null, this.jigsaw));
        }

        @Override
        StructureBoundingBox boundingBox(BlockPos at, Rotation rotation, boolean modern) {
            return new StructureBoundingBox(at, at);
        }
    }

    /** {@code ListJigsawPiece}: several elements placed together; the first supplies the jigsaws. */
    public static final class ListElement extends Element {
        final List<Element> elements;

        ListElement(List<Element> elements, Projection projection) {
            super(projection);
            this.elements = elements;
        }

        @Override
        List<ModernTemplate.PlacedJigsaw> jigsaws(BlockPos at, Rotation rotation, Random random, boolean modern) {
            return this.elements.get(0).jigsaws(at, rotation, random, modern);
        }

        @Override
        StructureBoundingBox boundingBox(BlockPos at, Rotation rotation, boolean modern) {
            StructureBoundingBox box = null;
            for (Element e : this.elements) {
                StructureBoundingBox b = e.boundingBox(at, rotation, modern);
                if (box == null) {
                    box = new StructureBoundingBox(b);
                } else {
                    box.expandTo(b);
                }
            }
            return box;
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Rule processors
    // ---------------------------------------------------------------------------------------------

    /** {@code RuleEntry}: when the template block and the world block both match, place the output. */
    static final class Rule {
        final JsonObject input;
        final JsonObject location;
        final String[] outputNames;
        final NBTTagCompound outputProps;
        private IBlockState output;
        private boolean resolved;

        Rule(JsonObject json) {
            this.input = json.getAsJsonObject("input");
            this.location = json.getAsJsonObject("location");
            JsonObject out = json.getAsJsonObject("output");
            JsonArray names = out.getAsJsonArray("names");
            this.outputNames = new String[names.size()];
            for (int i = 0; i < names.size(); i++) {
                this.outputNames[i] = names.get(i).getAsString();
            }
            this.outputProps = new NBTTagCompound();
            for (Map.Entry<String, JsonElement> e : out.getAsJsonObject("props").entrySet()) {
                this.outputProps.setString(e.getKey(), e.getValue().getAsString());
            }
        }

        @Nullable
        IBlockState output() {
            if (!this.resolved) {
                this.output = ModernTemplate.resolve(this.outputNames, this.outputProps);
                this.resolved = true;
            }
            return this.output;
        }

        boolean matches(ModernTemplate.PaletteEntry source, World world, BlockPos pos, Random random) {
            return test(this.input, source, null, null, random) && test(this.location, null, world, pos, random);
        }
    }

    private static boolean test(JsonObject test, @Nullable ModernTemplate.PaletteEntry source,
                                @Nullable World world, @Nullable BlockPos pos, Random random) {
        String kind = test.get("kind").getAsString();
        if ("always".equals(kind)) {
            return true;
        }
        if ("world_block".equals(kind)) {
            ResourceLocation here = world.getBlockState(pos).getBlock().getRegistryName();
            for (JsonElement name : test.getAsJsonArray("blocks")) {
                if (here != null && here.toString().equals(name.getAsString())) {
                    return true;
                }
            }
            return false;
        }
        if (source == null) {
            return false;
        }
        if ("block".equals(kind)) {
            return source.source.equals(test.get("block").getAsString());
        }
        if ("random_block".equals(kind)) {
            return source.source.equals(test.get("block").getAsString())
                    && random.nextFloat() < test.get("probability").getAsFloat();
        }
        if ("tag".equals(kind)) {
            // The only tag 1.14's village processors use is "doors".
            return "doors".equals(test.get("tag").getAsString()) && source.source.endsWith("_door");
        }
        if ("state".equals(kind)) {
            JsonObject state = test.getAsJsonObject("state");
            if (!source.source.equals(state.get("name").getAsString())) {
                return false;
            }
            for (Map.Entry<String, JsonElement> e : state.getAsJsonObject("props").entrySet()) {
                if (!e.getValue().getAsString().equals(source.sourceProps.getString(e.getKey()))) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    /**
     * {@code RuleStructureProcessor}: the first rule that matches decides the block. Seeded from the
     * position, as 1.14 does, so the same village rebuilt from its seed weathers the same way.
     *
     * @param modern seeded as 1.18.2 seeds it (see positionSeed)
     * @return the state to place in place of the template's
     */
    @Nullable
    static IBlockState process(List<List<Rule>> processors, ModernTemplate.PaletteEntry source, @Nullable IBlockState state,
                               World world, BlockPos pos, boolean modern) {
        for (List<Rule> rules : processors) {
            Random random = new Random(positionSeed(pos, modern));
            for (Rule rule : rules) {
                if (rule.matches(source, world, pos, random)) {
                    state = rule.output();
                    break;
                }
            }
        }
        return state;
    }

    /**
     * {@code MathHelper.getPositionRandom}, which 1.12.2 marks client-only, so a dedicated server
     * does not have it.
     */
    /** @return true if 1.14's {@code IntegrityProcessor} keeps the block placed here */
    static boolean keeps(SingleElement element, BlockPos pos, boolean modern) {
        return element.integrity >= 1.0F || new Random(positionSeed(pos, modern)).nextFloat() <= element.integrity;
    }

    /**
     * @param modern 1.18.2's {@code Mth.getSeed}, which drops the low 16 bits; without it, 1.12.2's
     *               {@code getCoordinateRandom}, as this port has always seeded 1.14's villages
     */
    private static long positionSeed(BlockPos pos, boolean modern) {
        long seed = (long) (pos.getX() * 3129871) ^ (long) pos.getZ() * 116129781L ^ (long) pos.getY();
        seed = seed * seed * 42317861L + seed * 11L;
        return modern ? seed >> 16 : seed;
    }

    // ---------------------------------------------------------------------------------------------
    // Loading
    // ---------------------------------------------------------------------------------------------

    /** @return the pool by its 1.14 name without the namespace, or null if there is none */
    @Nullable
    public static synchronized Pool get(String name) {
        if (pools == null) {
            pools = load();
        }
        if (name.startsWith("minecraft:")) {
            name = name.substring("minecraft:".length());
        }
        return pools.get(name);
    }

    private static Map<String, Pool> load() {
        Map<String, Pool> out = new HashMap<String, Pool>();
        Pool empty = new Pool(EMPTY, EMPTY);
        out.put(EMPTY, empty);

        for (String kind : KINDS) {
            loadFile(kind, out);
        }
        return out;
    }

    /** Every jigsaw structure ported from 1.14, by its folder under {@code structures/}. */
    private static final String[] KINDS = {"village", "pillager_outpost"};

    private static void loadFile(String kind, Map<String, Pool> out) {
        String path = "/assets/" + VillagerBackport.MOD_ID + "/structures/" + kind + "/pools.json";
        InputStream in = ModernPools.class.getResourceAsStream(path);
        if (in == null) {
            VillagerBackport.LOGGER.error("The 1.14 {} pools are missing from the jar; they will be empty.", kind);
            return;
        }
        try {
            JsonObject root = new JsonParser().parse(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            for (Map.Entry<String, JsonElement> e : root.entrySet()) {
                JsonObject json = e.getValue().getAsJsonObject();
                Pool pool = new Pool(e.getKey(), json.get("fallback").getAsString());
                for (JsonElement entry : json.getAsJsonArray("elements")) {
                    JsonObject o = entry.getAsJsonObject();
                    Element element = element(o.getAsJsonObject("element"));
                    element.pool = pool;
                    element.index = pool.distinct.size();
                    pool.distinct.add(element);
                    int weight = o.get("weight").getAsInt();
                    for (int i = 0; i < weight; i++) {
                        pool.expanded.add(element);
                    }
                }
                out.put(e.getKey(), pool);
            }
        } catch (Exception e) {
            VillagerBackport.LOGGER.error("Could not read the 1.14 {} pools.", kind, e);
        } finally {
            try {
                in.close();
            } catch (Exception ignored) {
                // Nothing useful to do.
            }
        }
    }

    private static Element element(JsonObject json) {
        String type = json.get("type").getAsString();
        Projection projection = json.has("projection") && "terrain_matching".equals(json.get("projection").getAsString())
                ? Projection.TERRAIN_MATCHING : Projection.RIGID;
        if ("empty".equals(type)) {
            return new EmptyElement();
        }
        if ("feature".equals(type)) {
            return new FeatureElement(json.get("feature").getAsString(), projection);
        }
        if ("list".equals(type)) {
            List<Element> elements = new ArrayList<Element>();
            for (JsonElement e : json.getAsJsonArray("elements")) {
                elements.add(element(e.getAsJsonObject()));
            }
            return new ListElement(elements, projection);
        }
        List<List<Rule>> processors = new ArrayList<List<Rule>>();
        float integrity = 1.0F;
        for (JsonElement proc : json.getAsJsonArray("processors")) {
            if (proc.isJsonObject() && proc.getAsJsonObject().has("integrity")) {
                integrity = proc.getAsJsonObject().get("integrity").getAsFloat();
                continue;
            }
            List<Rule> rules = new ArrayList<Rule>();
            for (JsonElement rule : proc.getAsJsonArray()) {
                rules.add(new Rule(rule.getAsJsonObject()));
            }
            processors.add(rules);
        }
        SingleElement single = new SingleElement(json.get("location").getAsString(), processors, projection);
        single.integrity = integrity;
        return single;
    }
}
