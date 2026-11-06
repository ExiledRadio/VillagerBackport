package com.exiledradio.villagerbackport.migrate;

import com.exiledradio.villagerbackport.VillagerBackport;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import javax.annotation.Nullable;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Which of this mod's blocks and items stands in for each of Future MC's, by name alone.
 *
 * <p>Nothing here needs Future MC to be installed: the conversion command uses it with Future MC
 * loaded, and {@link FutureMcRemoved} uses it for worlds Future MC has been taken out of, where the
 * only thing left of a Future MC block is its old name and numbers.
 *
 * <h2>Blocks</h2>
 * A Future MC block becomes this mod's block of the same name ({@code futuremc:stripped_oak_log}
 * becomes {@code villagerbackport:stripped_oak_log}), or the one {@link #RENAMES} names. A Future MC
 * block with no counterpart here is left alone.
 *
 * <h2>Metadata</h2>
 * A world without Future MC still holds each of its blocks as a number and a metadata value. What
 * that metadata meant is in {@code migration/futuremc_states.json}: for each block, the state Future
 * MC 0.2.21 decodes each metadata value to, written out by {@code /futuremc dump} on a server with
 * Future MC installed.
 */
public final class FutureMcTable {

    public static final String FUTURE_MC = "futuremc";

    private static final String VB = VillagerBackport.MOD_ID + ":";
    private static final String STATES = "/assets/villagerbackport/migration/futuremc_states.json";

    /**
     * Future MC blocks whose counterpart has another name, as {@code "block"} or
     * {@code "block|property=value,property=value"} for properties to force.
     */
    private static final Map<String, String> RENAMES = new HashMap<String, String>();

    static {
        for (String wood : new String[] {"spruce", "birch", "jungle", "acacia", "dark_oak"}) {
            // 1.14 calls the standing sign "spruce_sign"; Future MC calls it "spruce_standing_sign".
            RENAMES.put(wood + "_standing_sign", VB + wood + "_sign");
        }
        // Future MC's "mossy stone" slab is drawn with mossy cobblestone, 1.14's mossy cobblestone slab.
        RENAMES.put("mossy_stone_slab", VB + "mossy_cobblestone_slab");
        RENAMES.put("mossy_stone_double_slab", VB + "mossy_cobblestone_double_slab");
        // 1.14's bark-on-all-sides wood is 1.12.2's own six-sided log. 1.12.2 draws it the same from
        // every side, so its axis is "none" whichever way Future MC's faced.
        RENAMES.put("oak_wood", "minecraft:log|variant=oak,axis=none");
        RENAMES.put("spruce_wood", "minecraft:log|variant=spruce,axis=none");
        RENAMES.put("birch_wood", "minecraft:log|variant=birch,axis=none");
        RENAMES.put("jungle_wood", "minecraft:log|variant=jungle,axis=none");
        RENAMES.put("acacia_wood", "minecraft:log2|variant=acacia,axis=none");
        RENAMES.put("dark_oak_wood", "minecraft:log2|variant=dark_oak,axis=none");
    }

    /**
     * Future MC properties under another name here: {@code "block property"} to
     * {@code "property"}, with {@code true}/{@code false} becoming {@code 1}/{@code 0} where this mod
     * counts what Future MC flags.
     */
    private static final Map<String, String> PROPERTY_RENAMES = new HashMap<String, String>();

    /**
     * Future MC properties with nothing to go to here, on purpose: 1.14's cartography, fletching and
     * smithing tables don't turn.
     */
    private static final java.util.Set<String> IGNORED = new java.util.HashSet<String>(java.util.Arrays.asList(
            "cartography_table facing", "fletching_table facing", "smithing_table facing"));

    static {
        // 1.14 counts bamboo's thickness as age 0/1 and its maturity as stage 0/1.
        PROPERTY_RENAMES.put("bamboo thick", "age");
        PROPERTY_RENAMES.put("bamboo mature", "stage");
        PROPERTY_RENAMES.put("campfire signal", "signal_fire");
    }

    /**
     * @return Future MC's property as this mod's {name, value}, or null if it is deliberately left out
     */
    @Nullable
    public static String[] property(String path, String name, String value) {
        String key = path + " " + name;
        if (IGNORED.contains(key)) {
            return null;
        }
        String renamed = PROPERTY_RENAMES.get(key);
        if (renamed == null) {
            return new String[] {name, value};
        }
        if ("age".equals(renamed) || "stage".equals(renamed)) {
            value = "true".equals(value) ? "1" : "0";
        }
        return new String[] {renamed, value};
    }

    /** Future MC's one dye item, meta 0 to 3, to this mod's four. */
    private static final String[] DYES = {"white_dye", "blue_dye", "brown_dye", "black_dye"};

    private static Map<String, Map<Integer, Map<String, String>>> states;

    private FutureMcTable() {
    }

    /** A Future MC block's counterpart: the block, and any properties it must have. */
    public static final class Target {
        public final String path;
        public final Block block;
        public final Map<String, String> fixed;

        Target(String path, Block block, Map<String, String> fixed) {
            this.path = path;
            this.block = block;
            this.fixed = fixed;
        }
    }

    /** @return the counterpart for a Future MC block name, or null if this mod has none (or it is switched off) */
    @Nullable
    public static Target block(ResourceLocation name) {
        if (!FUTURE_MC.equals(name.getNamespace())) {
            return null;
        }
        String spec = RENAMES.get(name.getPath());
        if (spec == null) {
            spec = VB + name.getPath();
        }
        Map<String, String> fixed = Collections.emptyMap();
        int bar = spec.indexOf('|');
        if (bar >= 0) {
            fixed = new LinkedHashMap<String, String>();
            for (String pair : spec.substring(bar + 1).split(",")) {
                String[] kv = pair.split("=");
                fixed.put(kv[0], kv[1]);
            }
            spec = spec.substring(0, bar);
        }
        ResourceLocation target = new ResourceLocation(spec);
        if (!ForgeRegistries.BLOCKS.containsKey(target)) {
            return null;
        }
        Block block = ForgeRegistries.BLOCKS.getValue(target);
        return block == null || block == Blocks.AIR ? null : new Target(name.getPath(), block, fixed);
    }

    /** @return the item for a Future MC item name and damage that isn't a block's, or null */
    @Nullable
    public static ItemTarget item(ResourceLocation name, int damage) {
        if (!FUTURE_MC.equals(name.getNamespace())) {
            return null;
        }
        if ("dye".equals(name.getPath())) {
            if (damage < 0 || damage >= DYES.length) {
                return null;
            }
            Item dye = itemNamed(VB + DYES[damage]);
            return dye == null ? null : new ItemTarget(dye, 0);
        }
        Item same = itemNamed(VB + name.getPath());
        return same == null ? null : new ItemTarget(same, damage);
    }

    /** An item and the damage it should carry. */
    public static final class ItemTarget {
        public final Item item;
        public final int damage;

        ItemTarget(Item item, int damage) {
            this.item = item;
            this.damage = damage;
        }
    }

    @Nullable
    private static Item itemNamed(String name) {
        ResourceLocation key = new ResourceLocation(name);
        if (!ForgeRegistries.ITEMS.containsKey(key)) {
            return null;
        }
        Item item = ForgeRegistries.ITEMS.getValue(key);
        return item == null || item == Items.AIR ? null : item;
    }

    /**
     * @return the properties Future MC decodes this block and metadata to, from the recorded table,
     * or null if the table doesn't have it
     */
    @Nullable
    public static Map<String, String> recordedState(String path, int meta) {
        Map<Integer, Map<String, String>> block = recorded().get(path);
        if (block == null) {
            return null;
        }
        Map<String, String> state = block.get(meta);
        // A metadata value Future MC never writes decodes to its default, as Minecraft's own does.
        return state != null ? state : block.get(0);
    }

    /** @return true if the recorded table has this Future MC block */
    public static boolean recorded(String path) {
        return recorded().containsKey(path);
    }

    private static Map<String, Map<Integer, Map<String, String>>> recorded() {
        if (states != null) {
            return states;
        }
        Map<String, Map<Integer, Map<String, String>>> out = new HashMap<String, Map<Integer, Map<String, String>>>();
        InputStream in = FutureMcTable.class.getResourceAsStream(STATES);
        if (in == null) {
            VillagerBackport.LOGGER.warn("{} is missing; Future MC blocks can only be converted while Future MC is installed.", STATES);
        } else {
            try {
                JsonObject root = new JsonParser().parse(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
                for (Map.Entry<String, JsonElement> block : root.entrySet()) {
                    Map<Integer, Map<String, String>> metas = new HashMap<Integer, Map<String, String>>();
                    for (Map.Entry<String, JsonElement> meta : block.getValue().getAsJsonObject().entrySet()) {
                        Map<String, String> props = new LinkedHashMap<String, String>();
                        for (Map.Entry<String, JsonElement> prop : meta.getValue().getAsJsonObject().entrySet()) {
                            props.put(prop.getKey(), prop.getValue().getAsString());
                        }
                        metas.put(Integer.parseInt(meta.getKey()), props);
                    }
                    out.put(block.getKey(), metas);
                }
            } catch (RuntimeException e) {
                VillagerBackport.LOGGER.error("Couldn't read {}.", STATES, e);
            } finally {
                try {
                    in.close();
                } catch (java.io.IOException ignored) {
                    // nothing to do
                }
            }
        }
        states = out;
        return states;
    }
}
