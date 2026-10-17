package com.exiledradio.villagerbackport.village.modern;

import com.exiledradio.villagerbackport.ModConfig;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The real block, from a backport mod, for 1.14 blocks a village uses that 1.12.2 does not have.
 *
 * <p>The structure files carry each block's 1.14 name beside the stand-in 1.12.2 block the converter
 * chose (stripped logs as logs, blue ice as packed ice, smooth sandstone as cut sandstone). When Future
 * MC, Oceanic Expanse or Quark is installed and has the 1.14 block, that block is placed instead. With
 * none of them, or with {@code villages.backportBlocks} off, the stand-ins are placed as before.
 *
 * <p>A backport block is given the 1.14 properties it understands, then the converted 1.12.2 ones (a
 * button's {@code facing} is only right in the converted form), then any fixed ones the table names.
 * A property or value the block does not have is skipped, so one table entry fits several mods.
 */
final class BackportBlocks {

    private static final String[] WOODS = {"oak", "spruce", "birch", "jungle", "acacia", "dark_oak"};

    /** 1.14 block name, with {@code #double} for a double slab, to the blocks to try in order. */
    private static final Map<String, List<Option>> TABLE = new HashMap<String, List<Option>>();

    static {
        for (String wood : WOODS) {
            add("stripped_" + wood + "_log", "futuremc:stripped_" + wood + "_log");
            add("stripped_" + wood + "_wood", "futuremc:stripped_" + wood + "_wood");
            if (!"oak".equals(wood)) {
                // 1.12.2 has one trapdoor, button and pressure plate, all oak.
                add(wood + "_trapdoor", "futuremc:" + wood + "_trapdoor", "quark:" + wood + "_trapdoor");
                add(wood + "_button", "quark:" + wood + "_button");
                add(wood + "_pressure_plate", "quark:" + wood + "_pressure_plate");
                add(wood + "_wall_sign", "futuremc:" + wood + "_wall_sign");
                add(wood + "_sign", "futuremc:" + wood + "_standing_sign");
            }
        }

        add("blue_ice", "futuremc:blue_ice", "oe:blue_ice");
        add("smooth_stone", "futuremc:smooth_stone");

        TABLE.put("smooth_sandstone", Arrays.asList(new Option("futuremc:smooth_sandstone"),
                new Option("quark:sandstone_new", "variant", "sandstone_smooth")));
        add("smooth_sandstone_slab", "futuremc:smooth_sandstone_slab", "quark:sandstone_smooth_slab");
        add("smooth_sandstone_slab#double", "futuremc:smooth_sandstone_double_slab", "quark:sandstone_smooth_slab_double");

        for (String stone : new String[] {"andesite", "diorite", "granite"}) {
            add(stone + "_slab", "futuremc:" + stone + "_slab", "quark:stone_" + stone + "_slab");
            add(stone + "_slab#double", "futuremc:" + stone + "_double_slab", "quark:stone_" + stone + "_slab_double");
            add(stone + "_stairs", "quark:stone_" + stone + "_stairs");
            add(stone + "_wall", "futuremc:" + stone + "_wall", "quark:stone_" + stone + "_wall");
        }
        add("sandstone_wall", "futuremc:sandstone_wall", "quark:sandstone_wall");

        // Oceanic Expanse names the count and the water differently.
        TABLE.put("sea_pickle", Collections.singletonList(new Option("oe:sea_pickle") {
            @Override
            NBTTagCompound fixed(NBTTagCompound source) {
                NBTTagCompound out = new NBTTagCompound();
                out.setString("amount", source.hasKey("pickles") ? source.getString("pickles") : "1");
                out.setString("in_water", source.hasKey("waterlogged") ? source.getString("waterlogged") : "false");
                return out;
            }
        }));
    }

    private BackportBlocks() {
    }

    /**
     * @param source 1.14 block name, e.g. {@code minecraft:stripped_spruce_log}
     * @return the backport mod's state for it, or null to place the converted 1.12.2 block
     */
    @Nullable
    static IBlockState resolve(String source, NBTTagCompound sourceProps, NBTTagCompound props) {
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
