package com.exiledradio.villagerbackport.campfire;

import com.exiledradio.villagerbackport.ModConfig;
import com.exiledradio.villagerbackport.VillagerBackport;

import net.minecraft.item.Item;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * What a campfire cooks, and into what.
 *
 * <h2>1.14's list, in 1.12.2's terms</h2>
 * 1.14 gives the campfire a recipe type of its own, {@code campfire_cooking}, with nine recipes: beef,
 * chicken, mutton, porkchop, rabbit, cod, salmon, potato and kelp. Every one is a furnace recipe too,
 * and every one turns a food into a food. 1.12.2 has no recipe type to add, so the rule is that one
 * instead: <b>a furnace recipe whose input and output are both {@link ItemFood}</b>. On vanilla 1.12.2
 * that is exactly 1.14's list less kelp, which 1.12.2 does not have, and nothing else - the chorus
 * fruit's furnace recipe makes an inedible popped fruit, and iron ore is no food at all. A mod's raw
 * foods are picked up without anyone having to list them, as they would be by a datapack in 1.14.
 *
 * <h2>The config lists</h2>
 * {@code campfire.extraFoods} adds what the rule misses - an input with no furnace recipe, or one
 * whose mod does not use {@code ItemFood} - and {@code campfire.excludedFoods} takes things away. Both
 * are read once, the first time anything asks, by which point every mod's furnace recipes are in.
 */
public final class CampfireRecipes {

    /** One parsed {@code extraFoods} entry. */
    private static final class Extra {
        final Item input;
        final int inputMeta;
        @Nullable
        final ItemStack result;

        Extra(Item input, int inputMeta, @Nullable ItemStack result) {
            this.input = input;
            this.inputMeta = inputMeta;
            this.result = result;
        }

        boolean matches(ItemStack stack) {
            return stack.getItem() == this.input && (this.inputMeta < 0 || stack.getMetadata() == this.inputMeta);
        }
    }

    /** Item and metadata, -1 for "any". */
    private static final class Key {
        final Item item;
        final int meta;

        Key(Item item, int meta) {
            this.item = item;
            this.meta = meta;
        }

        boolean matches(ItemStack stack) {
            return stack.getItem() == this.item && (this.meta < 0 || stack.getMetadata() == this.meta);
        }
    }

    @Nullable
    private static List<Extra> extras;
    @Nullable
    private static List<Key> excluded;

    private CampfireRecipes() {
    }

    /**
     * @return what one of this cooks into, as a new stack, or empty if a campfire will not take it
     */
    public static ItemStack result(ItemStack input) {
        if (input.isEmpty()) {
            return ItemStack.EMPTY;
        }
        load();
        for (Key key : excluded) {
            if (key.matches(input)) {
                return ItemStack.EMPTY;
            }
        }
        for (Extra extra : extras) {
            if (extra.matches(input)) {
                ItemStack out = extra.result != null ? extra.result
                        : FurnaceRecipes.instance().getSmeltingResult(input);
                return out.isEmpty() ? ItemStack.EMPTY : sized(out);
            }
        }
        if (!(input.getItem() instanceof ItemFood)) {
            return ItemStack.EMPTY;
        }
        ItemStack out = FurnaceRecipes.instance().getSmeltingResult(input);
        if (out.isEmpty() || !(out.getItem() instanceof ItemFood)) {
            return ItemStack.EMPTY;
        }
        return sized(out);
    }

    /**
     * @return every campfire recipe, input to result: the furnace's food recipes it takes and the config's extra foods
     *         (Backport Core's campfire recipe book reads it, so its recipes unlock and toast as 1.14's
     *         {@code campfire_cooking} do)
     */
    public static java.util.Map<ItemStack, ItemStack> recipes() {
        load();
        java.util.Map<ItemStack, ItemStack> out = new java.util.LinkedHashMap<ItemStack, ItemStack>();
        for (java.util.Map.Entry<ItemStack, ItemStack> e : FurnaceRecipes.instance().getSmeltingList().entrySet()) {
            ItemStack r = result(e.getKey());
            if (!r.isEmpty()) {
                out.put(e.getKey(), r);
            }
        }
        for (Extra extra : extras) {
            ItemStack in = new ItemStack(extra.input, 1, extra.inputMeta < 0 ? 0 : extra.inputMeta);
            ItemStack r = result(in);
            if (!r.isEmpty()) {
                out.put(in, r);
            }
        }
        return out;
    }

    /** @return true if a campfire takes this */
    public static boolean cooks(ItemStack input) {
        return !result(input).isEmpty();
    }

    /** One input always makes the recipe's own count, as a furnace does; copied so nobody shares it. */
    private static ItemStack sized(ItemStack out) {
        return out.copy();
    }

    private static synchronized void load() {
        if (extras != null) {
            return;
        }
        List<Extra> extraList = new ArrayList<Extra>();
        for (String line : ModConfig.campfire.extraFoods) {
            String entry = line.trim();
            if (entry.isEmpty()) {
                continue;
            }
            String[] halves = entry.split("=", 2);
            Key in = parse(halves[0].trim());
            if (in == null) {
                VillagerBackport.LOGGER.warn("campfire.extraFoods: no item '{}'; skipped.", halves[0].trim());
                continue;
            }
            ItemStack result = null;
            if (halves.length == 2) {
                Key out = parse(halves[1].trim());
                if (out == null) {
                    VillagerBackport.LOGGER.warn("campfire.extraFoods: no item '{}'; skipped.", halves[1].trim());
                    continue;
                }
                result = new ItemStack(out.item, 1, Math.max(0, out.meta));
            }
            extraList.add(new Extra(in.item, in.meta, result));
        }
        List<Key> excludedList = new ArrayList<Key>();
        for (String line : ModConfig.campfire.excludedFoods) {
            String entry = line.trim();
            if (entry.isEmpty()) {
                continue;
            }
            Key key = parse(entry);
            if (key == null) {
                VillagerBackport.LOGGER.warn("campfire.excludedFoods: no item '{}'; skipped.", entry);
                continue;
            }
            excludedList.add(key);
        }
        excluded = excludedList;
        extras = extraList;
    }

    /** {@code modid:item} or {@code modid:item:meta}. */
    @Nullable
    private static Key parse(String text) {
        String name = text;
        int meta = -1;
        int last = text.lastIndexOf(':');
        if (last > 0 && text.indexOf(':') != last) {
            try {
                meta = Integer.parseInt(text.substring(last + 1));
                name = text.substring(0, last);
            } catch (NumberFormatException ignored) {
                // Not a number: the whole thing is the name.
            }
        }
        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(name));
        if (item == null || item == net.minecraft.init.Items.AIR) {
            return null;
        }
        return new Key(item, meta);
    }
}
