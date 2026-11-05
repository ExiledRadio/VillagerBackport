package com.exiledradio.villagerbackport.building;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Every slab, stair and wall 1.14 added, as one table.
 *
 * <h2>Why a table</h2>
 * 1.14 added forty-two shaped stone blocks, and each one differs from the next only in which block it
 * is cut from, what it looks like and which of the three shapes exist. Writing a class per block would
 * be forty-two copies of the same class; writing a registration line per block would be forty-two
 * chances to forget its item, its recipe, its model or its stonecutter entry. So each material is one
 * row below, and everything about it - registration, items, crafting recipes, ore dictionary names,
 * models, blockstates and language entries - is derived from that row.
 *
 * <p>The model, blockstate and language files are generated from this file by
 * {@code tools/building/generate.py}, which reads the rows below as text. Keep each row on one line
 * in the form it already has, and rerun the script after changing one.
 *
 * <h2>What a row says</h2>
 * <ul>
 *   <li><b>name</b> - the 1.14 prefix, so {@code andesite} becomes {@code andesite_slab},
 *       {@code andesite_stairs} and {@code andesite_wall}, which are 1.14's own names.</li>
 *   <li><b>base</b> - the block it is made from, as {@code modid:name} plus metadata. Hardness,
 *       blast resistance and sound are copied from it, as 1.14 does, and its recipes take it.</li>
 *   <li><b>textures</b> - side, top and bottom, as 1.12.2 texture names. Vanilla's own textures, so a
 *       resource pack that retextures andesite retextures andesite stairs with it.</li>
 *   <li><b>shapes</b> - which of slab, stairs and wall 1.14 has for it. Many materials only got some:
 *       polished granite has no wall, brick only gained one, cut sandstone only has a slab.</li>
 * </ul>
 *
 * <h2>What is not here</h2>
 * Shapes 1.12.2 already has (stone brick stairs, the cobblestone and mossy cobblestone walls, the
 * smooth stone slab, which is 1.12.2's "Stone Slab") are left to vanilla. The four smooth blocks that
 * 1.12.2 never had as blocks of their own are listed in {@link #FULL_BLOCKS}; they come first because
 * some rows are made from them.
 */
public final class BuildingTable {

    /** The three shapes a material can be cut into. */
    public enum Shape {
        SLAB, STAIRS, WALL
    }

    private static final Shape SLAB = Shape.SLAB;
    private static final Shape STAIRS = Shape.STAIRS;
    private static final Shape WALL = Shape.WALL;

    /** What a full block looks like on the map; the four smooth blocks each match their source. */
    public enum MapTint {
        STONE, SAND, ADOBE, QUARTZ
    }

    /**
     * The whole blocks 1.14 added that 1.12.2 lacks.
     *
     * <p>1.12.2 has all four as looks - the seamless double stone slab is smooth stone, and so on - but
     * only as the doubled form of a slab: no item, no recipe, and breaking one drops two slabs. 1.14
     * made each a block in its own right, crafted by smelting, and so are these.
     *
     * <p>None of these duplicates a 1.12.2 block. 1.12.2's "Smooth Sandstone" and "Smooth Red
     * Sandstone" are the blocks 1.14 renamed to cut sandstone, and are used as such below; this mod's
     * language file gives them their 1.14 names.
     */
    public static final List<Full> FULL_BLOCKS = Collections.unmodifiableList(Arrays.asList(
            full("smooth_stone", "stone_slab_top", MapTint.STONE, "minecraft:stone", 0),
            full("smooth_sandstone", "sandstone_top", MapTint.SAND, "minecraft:sandstone", 0),
            full("smooth_red_sandstone", "red_sandstone_top", MapTint.ADOBE, "minecraft:red_sandstone", 0),
            full("smooth_quartz", "quartz_block_bottom", MapTint.QUARTZ, "minecraft:quartz_block", 0)
    ));

    /** The rows. Order is the creative menu's order. */
    public static final List<Row> ROWS = Collections.unmodifiableList(Arrays.asList(
            row("stone", "minecraft:stone", 0, "stone|stone|stone", SLAB, STAIRS),
            row("granite", "minecraft:stone", 1, "stone_granite|stone_granite|stone_granite", SLAB, STAIRS, WALL),
            row("polished_granite", "minecraft:stone", 2, "stone_granite_smooth|stone_granite_smooth|stone_granite_smooth", SLAB, STAIRS),
            row("diorite", "minecraft:stone", 3, "stone_diorite|stone_diorite|stone_diorite", SLAB, STAIRS, WALL),
            row("polished_diorite", "minecraft:stone", 4, "stone_diorite_smooth|stone_diorite_smooth|stone_diorite_smooth", SLAB, STAIRS),
            row("andesite", "minecraft:stone", 5, "stone_andesite|stone_andesite|stone_andesite", SLAB, STAIRS, WALL),
            row("polished_andesite", "minecraft:stone", 6, "stone_andesite_smooth|stone_andesite_smooth|stone_andesite_smooth", SLAB, STAIRS),
            row("mossy_cobblestone", "minecraft:mossy_cobblestone", 0, "cobblestone_mossy|cobblestone_mossy|cobblestone_mossy", SLAB, STAIRS),
            row("stone_brick", "minecraft:stonebrick", 0, "stonebrick|stonebrick|stonebrick", WALL),
            row("mossy_stone_brick", "minecraft:stonebrick", 1, "stonebrick_mossy|stonebrick_mossy|stonebrick_mossy", SLAB, STAIRS, WALL),
            row("brick", "minecraft:brick_block", 0, "brick|brick|brick", WALL),
            row("sandstone", "minecraft:sandstone", 0, "sandstone_normal|sandstone_top|sandstone_bottom", WALL),
            row("smooth_sandstone", "villagerbackport:smooth_sandstone", 0, "sandstone_top|sandstone_top|sandstone_top", SLAB, STAIRS),
            row("cut_sandstone", "minecraft:sandstone", 2, "sandstone_smooth|sandstone_top|sandstone_top", SLAB),
            row("red_sandstone", "minecraft:red_sandstone", 0, "red_sandstone_normal|red_sandstone_top|red_sandstone_bottom", WALL),
            row("smooth_red_sandstone", "villagerbackport:smooth_red_sandstone", 0, "red_sandstone_top|red_sandstone_top|red_sandstone_top", SLAB, STAIRS),
            row("cut_red_sandstone", "minecraft:red_sandstone", 2, "red_sandstone_smooth|red_sandstone_top|red_sandstone_top", SLAB),
            row("smooth_quartz", "villagerbackport:smooth_quartz", 0, "quartz_block_bottom|quartz_block_bottom|quartz_block_bottom", SLAB, STAIRS),
            row("prismarine", "minecraft:prismarine", 0, "prismarine_rough|prismarine_rough|prismarine_rough", WALL),
            row("nether_brick", "minecraft:nether_brick", 0, "nether_brick|nether_brick|nether_brick", WALL),
            row("red_nether_brick", "minecraft:red_nether_brick", 0, "red_nether_brick|red_nether_brick|red_nether_brick", SLAB, STAIRS, WALL),
            row("end_stone_brick", "minecraft:end_bricks", 0, "end_bricks|end_bricks|end_bricks", SLAB, STAIRS, WALL)
    ));

    private BuildingTable() {
    }

    private static Full full(String name, String texture, MapTint tint, String smeltedFrom, int smeltedMeta) {
        return new Full(name, texture, tint, smeltedFrom, smeltedMeta);
    }

    private static Row row(String name, String base, int meta, String textures, Shape... shapes) {
        String[] tex = textures.split("\\|");
        return new Row(name, base, meta, tex[0], tex[1], tex[2], EnumSet.copyOf(Arrays.asList(shapes)));
    }

    /** One of the smooth blocks: one texture on every face, smelted from a vanilla block. */
    public static final class Full {
        public final String name;
        public final String texture;
        public final MapTint tint;
        public final String smeltedFrom;
        public final int smeltedMeta;

        Full(String name, String texture, MapTint tint, String smeltedFrom, int smeltedMeta) {
            this.name = name;
            this.texture = texture;
            this.tint = tint;
            this.smeltedFrom = smeltedFrom;
            this.smeltedMeta = smeltedMeta;
        }
    }

    /** One material and the shapes 1.14 cuts it into. */
    public static final class Row {
        public final String name;
        public final String base;
        public final int baseMeta;
        public final String side;
        public final String top;
        public final String bottom;
        public final Set<Shape> shapes;

        Row(String name, String base, int baseMeta, String side, String top, String bottom, Set<Shape> shapes) {
            this.name = name;
            this.base = base;
            this.baseMeta = baseMeta;
            this.side = side;
            this.top = top;
            this.bottom = bottom;
            this.shapes = Collections.unmodifiableSet(shapes);
        }

        public boolean has(Shape shape) {
            return this.shapes.contains(shape);
        }
    }
}
