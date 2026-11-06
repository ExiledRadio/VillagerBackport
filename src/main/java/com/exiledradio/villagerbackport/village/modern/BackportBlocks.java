package com.exiledradio.villagerbackport.village.modern;

import com.exiledradio.villagerbackport.ModConfig;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The real block, from a backport mod, for 1.14 blocks a village uses that 1.12.2 does not have.
 *
 * <p>The structure files carry each block's 1.14 name beside the stand-in 1.12.2 block the converter
 * chose (stripped logs as logs, blue ice as packed ice, smooth sandstone as cut sandstone). When Future
 * MC, Oceanic Expanse, Quark or Farmer's Future Delight is installed and has the 1.14 block, that
 * block is placed instead. With none of them, this mod's own 1.14 building blocks (slabs, stairs,
 * walls, smooth blocks and wood signs, when {@code building.enabled} is on) and village blocks
 * (stripped logs and wood, wood trapdoors, buttons and pressure plates, blue ice and sea pickles,
 * when {@code villageBlocks.enabled} is on) are. With
 * {@code villages.backportBlocks} off, or a block nobody provides, the stand-ins are placed as before.
 *
 * <p>A backport block is given the 1.14 properties it understands, then the converted 1.12.2 ones (a
 * button's {@code facing} is only right in the converted form), then any fixed ones the table names.
 * A property or value the block does not have is skipped, so one table entry fits several mods.
 */
final class BackportBlocks {

    private static final String VB = "villagerbackport:";

    private static final String[] WOODS = {"oak", "spruce", "birch", "jungle", "acacia", "dark_oak"};

    /**
     * 1.14 blocks this mod adds itself, placed whatever {@code villages.backportBlocks} says: that
     * setting is about borrowing other mods' blocks, and these are not borrowed. Each still goes
     * through {@code FutureMc.preferred} afterwards, so with Future MC's kept, its twin is placed.
     * A block switched off in this mod's config is not registered, and the converted names - Future
     * MC's, then air - are used as before.
     */
    private static final Map<String, String> OWN = new HashMap<String, String>();

    static {
        OWN.put("campfire", "villagerbackport:campfire");
    }

    /** 1.14 block name, with {@code #double} for a double slab, to the blocks to try in order. */
    private static final Map<String, List<Option>> TABLE = new HashMap<String, List<Option>>();

    static {
        for (String wood : WOODS) {
            // Future MC leaves its stripped logs out when Oceanic Expanse is installed and its
            // Oceanic Expanse option is on, so Farmer's Future Delight's are worth having as well.
            // This mod's own (villageBlocks.enabled) come last, as with the building blocks below.
            add("stripped_" + wood + "_log", "futuremc:stripped_" + wood + "_log",
                    "farmers_future_delight:stripped_" + wood + "_log", VB + "stripped_" + wood + "_log");
            add("stripped_" + wood + "_wood", "futuremc:stripped_" + wood + "_wood",
                    "farmers_future_delight:stripped_" + wood + "_wood", VB + "stripped_" + wood + "_wood");
            if (!"oak".equals(wood)) {
                // 1.12.2 has one trapdoor, button and pressure plate, all oak. The 1.14 properties
                // (facing, half, open, powered) carry across by name; a button's 1.14 face/facing
                // pair is overridden by the converted 1.12.2 facing, as for Quark's.
                add(wood + "_trapdoor", "futuremc:" + wood + "_trapdoor", "quark:" + wood + "_trapdoor",
                        VB + wood + "_trapdoor");
                add(wood + "_button", "quark:" + wood + "_button", VB + wood + "_button");
                add(wood + "_pressure_plate", "quark:" + wood + "_pressure_plate", VB + wood + "_pressure_plate");
                add(wood + "_wall_sign", "futuremc:" + wood + "_wall_sign", VB + wood + "_wall_sign");
                add(wood + "_sign", "futuremc:" + wood + "_standing_sign", VB + wood + "_sign");
            }
        }

        add("blue_ice", "futuremc:blue_ice", "oe:blue_ice", VB + "blue_ice");
        add("smooth_stone", "futuremc:smooth_stone", VB + "smooth_stone");

        TABLE.put("smooth_sandstone", Arrays.asList(new Option("futuremc:smooth_sandstone"),
                new Option("quark:sandstone_new", "variant", "sandstone_smooth"), new Option(VB + "smooth_sandstone")));
        add("smooth_sandstone_slab", "futuremc:smooth_sandstone_slab", "quark:sandstone_smooth_slab",
                VB + "smooth_sandstone_slab");
        add("smooth_sandstone_slab#double", "futuremc:smooth_sandstone_double_slab", "quark:sandstone_smooth_slab_double",
                VB + "smooth_sandstone_double_slab");
        add("smooth_sandstone_stairs", "quark:sandstone_smooth_stairs", VB + "smooth_sandstone_stairs");

        for (String stone : new String[] {"andesite", "diorite", "granite"}) {
            add(stone + "_slab", "futuremc:" + stone + "_slab", "quark:stone_" + stone + "_slab", VB + stone + "_slab");
            add(stone + "_slab#double", "futuremc:" + stone + "_double_slab", "quark:stone_" + stone + "_slab_double",
                    VB + stone + "_double_slab");
            add(stone + "_stairs", "quark:stone_" + stone + "_stairs", VB + stone + "_stairs");
            add(stone + "_wall", "futuremc:" + stone + "_wall", "quark:stone_" + stone + "_wall", VB + stone + "_wall");
        }
        add("sandstone_wall", "futuremc:sandstone_wall", "quark:sandstone_wall", VB + "sandstone_wall");

        // Pillager outposts' mossy cobblestone, which 1.12.2 has no stairs or slab for.
        add("mossy_cobblestone_stairs", "quark:cobblestone_mossy_stairs", VB + "mossy_cobblestone_stairs");
        add("mossy_cobblestone_slab", "quark:cobblestone_mossy_slab", VB + "mossy_cobblestone_slab");
        add("mossy_cobblestone_slab#double", "quark:cobblestone_mossy_slab_double", VB + "mossy_cobblestone_double_slab");

        // This mod's own building blocks (building.enabled) come last, so a pack that already has
        // another mod's keeps building with those, as it did before this mod had any.
        for (String name : new String[] {"stone", "polished_andesite", "polished_diorite", "polished_granite",
                "mossy_stone_brick", "smooth_red_sandstone", "smooth_quartz", "red_nether_brick", "end_stone_brick",
                "cut_sandstone", "cut_red_sandstone"}) {
            addIfAbsent(name + "_slab", VB + name + "_slab");
            addIfAbsent(name + "_slab#double", VB + name + "_double_slab");
            addIfAbsent(name + "_stairs", VB + name + "_stairs");
        }
        for (String name : new String[] {"brick", "prismarine", "stone_brick", "mossy_stone_brick", "red_sandstone",
                "nether_brick", "red_nether_brick", "end_stone_brick"}) {
            addIfAbsent(name + "_wall", VB + name + "_wall");
        }
        addIfAbsent("smooth_red_sandstone", VB + "smooth_red_sandstone");
        addIfAbsent("smooth_quartz", VB + "smooth_quartz");

        // Oceanic Expanse names the count and the water differently. This mod's own uses 1.14's
        // names, pickles and waterlogged, so they carry across by name.
        TABLE.put("sea_pickle", Arrays.asList(new Option("oe:sea_pickle") {
            @Override
            NBTTagCompound fixed(NBTTagCompound source) {
                NBTTagCompound out = new NBTTagCompound();
                out.setString("amount", source.hasKey("pickles") ? source.getString("pickles") : "1");
                out.setString("in_water", source.hasKey("waterlogged") ? source.getString("waterlogged") : "false");
                return out;
            }
        }, new Option(VB + "sea_pickle")));
    }

    private BackportBlocks() {
    }

    /**
     * @param source 1.14 block name, e.g. {@code minecraft:stripped_spruce_log}
     * @return the backport mod's state for it, or null to place the converted 1.12.2 block
     */
    @Nullable
    static IBlockState resolve(String source, NBTTagCompound sourceProps, NBTTagCompound props) {
        if (source.startsWith("minecraft:")) {
            String own = OWN.get(source.substring("minecraft:".length()));
            if (own != null) {
                NBTTagCompound merged = new NBTTagCompound();
                merged.merge(sourceProps);
                merged.merge(props);
                IBlockState state = ModernTemplate.resolve(new String[] {own}, merged);
                if (state != null) {
                    return state;
                }
            }
        }
        if (!ModConfig.villages.backportBlocks || !source.startsWith("minecraft:")) {
            return null;
        }
        String key = source.substring("minecraft:".length());
        if ("double".equals(sourceProps.getString("type"))) {
            key += "#double";
        }
        List<Option> options = TABLE.get(key);
        if (options == null) {
            return null;
        }
        for (Option option : options) {
            NBTTagCompound merged = new NBTTagCompound();
            merged.merge(sourceProps);
            merged.merge(props);
            merged.merge(option.fixed(sourceProps));
            IBlockState state = ModernTemplate.resolve(new String[] {option.name}, merged);
            if (state != null) {
                return state;
            }
        }
        return null;
    }

    private static void add(String source, String... names) {
        List<Option> options = new ArrayList<Option>();
        for (String name : names) {
            options.add(new Option(name));
        }
        TABLE.put(source, options);
    }

    /** Adds a name only if no line above has it, for the long tail no 1.14 village uses today. */
    private static void addIfAbsent(String source, String name) {
        if (!TABLE.containsKey(source)) {
            add(source, name);
        }
    }

    private static class Option {
        final String name;
        private final String[] fixed;

        Option(String name, String... fixed) {
            this.name = name;
            this.fixed = fixed;
        }

        NBTTagCompound fixed(NBTTagCompound source) {
            NBTTagCompound out = new NBTTagCompound();
            for (int i = 0; i + 1 < this.fixed.length; i += 2) {
                out.setString(this.fixed[i], this.fixed[i + 1]);
            }
            return out;
        }
    }
}
