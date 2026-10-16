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
            List<Element> copy = new ArrayList<Element>(this.expanded);
            Collections.shuffle(copy, random);
            return copy;
        }

        int size() {
            return this.expanded.size();
        }

        /** {@code func_214945_a}: the tallest element, for reserving room above a piece. */
        int maxHeight() {
            if (this.maxHeight == Integer.MIN_VALUE) {
                int max = 0;
                for (Element e : this.expanded) {
                    StructureBoundingBox box = e.boundingBox(BlockPos.ORIGIN, Rotation.NONE);
                    max = Math.max(max, box.getYSize());
                }
                this.maxHeight = max;
            }
            return this.maxHeight;
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

        abstract List<ModernTemplate.PlacedJigsaw> jigsaws(BlockPos at, Rotation rotation, Random random);

        abstract StructureBoundingBox boundingBox(BlockPos at, Rotation rotation);

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
        List<ModernTemplate.PlacedJigsaw> jigsaws(BlockPos at, Rotation rotation, Random random) {
            return Collections.emptyList();
        }

        @Override
        StructureBoundingBox boundingBox(BlockPos at, Rotation rotation) {
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

        SingleElement(String location, List<List<Rule>> processors, Projection projection) {
            super(projection);
            this.location = location;
            this.processors = processors;
        }

        ModernTemplate template() {
            return ModernTemplate.get(this.location);
        }

        @Override
        List<ModernTemplate.PlacedJigsaw> jigsaws(BlockPos at, Rotation rotation, Random random) {
            return template().jigsaws(at, rotation, random);
        }

        @Override
        StructureBoundingBox boundingBox(BlockPos at, Rotation rotation) {
            return template().boundingBox(at, rotation);
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
        List<ModernTemplate.PlacedJigsaw> jigsaws(BlockPos at, Rotation rotation, Random random) {
            return Collections.singletonList(new ModernTemplate.PlacedJigsaw(at, net.minecraft.util.EnumFacing.DOWN, this.jigsaw));
        }

        @Override
        StructureBoundingBox boundingBox(BlockPos at, Rotation rotation) {
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
        List<ModernTemplate.PlacedJigsaw> jigsaws(BlockPos at, Rotation rotation, Random random) {
            return this.elements.get(0).jigsaws(at, rotation, random);
        }

        @Override
        StructureBoundingBox boundingBox(BlockPos at, Rotation rotation) {
            StructureBoundingBox box = null;
            for (Element e : this.elements) {
                StructureBoundingBox b = e.boundingBox(at, rotation);
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
     * @return the state to place in place of the template's
     */
    @Nullable
    static IBlockState process(List<List<Rule>> processors, ModernTemplate.PaletteEntry source, @Nullable IBlockState state,
                               World world, BlockPos pos) {
        for (List<Rule> rules : processors) {
            Random random = new Random(positionSeed(pos));
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
    private static long positionSeed(BlockPos pos) {
        long seed = (long) (pos.getX() * 3129871) ^ (long) pos.getZ() * 116129781L ^ (long) pos.getY();
        return seed * seed * 42317861L + seed * 11L;
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

        String path = "/assets/" + VillagerBackport.MOD_ID + "/structures/village/pools.json";
        InputStream in = ModernPools.class.getResourceAsStream(path);
        if (in == null) {
            VillagerBackport.LOGGER.error("The 1.14 village pools are missing from the jar; villages will be empty.");
            return out;
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
            VillagerBackport.LOGGER.error("Could not read the 1.14 village pools.", e);
        } finally {
            try {
                in.close();
            } catch (Exception ignored) {
                // Nothing useful to do.
            }
        }
        return out;
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
        for (JsonElement proc : json.getAsJsonArray("processors")) {
            List<Rule> rules = new ArrayList<Rule>();
            for (JsonElement rule : proc.getAsJsonArray()) {
                rules.add(new Rule(rule.getAsJsonObject()));
            }
            processors.add(rules);
        }
        return new SingleElement(json.get("location").getAsString(), processors, projection);
    }
}
