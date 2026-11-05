package com.exiledradio.villagerbackport.flora;

import net.minecraft.block.Block;
import net.minecraft.block.BlockFlower;
import net.minecraft.init.Blocks;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Which effect each small flower puts in a suspicious stew, and for how long.
 *
 * <h2>The table</h2>
 * Current Java's, from minecraft.wiki's Suspicious Stew page - several durations changed after 1.14
 * (the cornflower's Jump Boost went from six seconds to five, the lily of the valley's Poison from
 * twelve to eleven, and so on), and the plan is to follow the game as it is now:
 *
 * <pre>
 *   allium              Fire Resistance   3 s
 *   azure bluet         Blindness        11 s
 *   blue orchid         Saturation       7 ticks
 *   dandelion           Saturation       7 ticks
 *   cornflower          Jump Boost        5 s
 *   lily of the valley  Poison           11 s
 *   oxeye daisy         Regeneration      7 s
 *   poppy               Night Vision      5 s
 *   tulips (all four)   Weakness          7 s
 *   wither rose         Wither            7 s
 * </pre>
 *
 * <p>Later versions' torchflower and eyeblossoms have no 1.12.2 block to belong to. Future MC's
 * cornflower, lily of the valley and wither rose count as ours, so a mooshroom fed one of those, or
 * a stew crafted with one, works the same.
 *
 * <p>The amplifier is always zero: 1.14 never made a stronger stew.
 */
public final class StewEffects {

    /** One flower's entry. */
    public static final class Entry {
        public final ItemStack flower;
        public final Potion effect;
        public final int ticks;

        Entry(ItemStack flower, Potion effect, int ticks) {
            this.flower = flower;
            this.effect = effect;
            this.ticks = ticks;
        }
    }

    private static List<Entry> table;

    private StewEffects() {
    }

    /** @return every flower's entry, 1.12.2's flowers first. Built on first use, once items exist. */
    public static synchronized List<Entry> table() {
        if (table != null) {
            return table;
        }

        List<Entry> entries = new ArrayList<Entry>();
        red(entries, BlockFlower.EnumFlowerType.ALLIUM, MobEffects.FIRE_RESISTANCE, 60);
        red(entries, BlockFlower.EnumFlowerType.HOUSTONIA, MobEffects.BLINDNESS, 220);
        red(entries, BlockFlower.EnumFlowerType.BLUE_ORCHID, MobEffects.SATURATION, 7);
        entries.add(new Entry(new ItemStack(Blocks.YELLOW_FLOWER, 1, 0), MobEffects.SATURATION, 7));
        red(entries, BlockFlower.EnumFlowerType.OXEYE_DAISY, MobEffects.REGENERATION, 140);
        red(entries, BlockFlower.EnumFlowerType.POPPY, MobEffects.NIGHT_VISION, 100);
        red(entries, BlockFlower.EnumFlowerType.RED_TULIP, MobEffects.WEAKNESS, 140);
        red(entries, BlockFlower.EnumFlowerType.ORANGE_TULIP, MobEffects.WEAKNESS, 140);
        red(entries, BlockFlower.EnumFlowerType.WHITE_TULIP, MobEffects.WEAKNESS, 140);
        red(entries, BlockFlower.EnumFlowerType.PINK_TULIP, MobEffects.WEAKNESS, 140);

        ours(entries, FloraContent.cornflower, "cornflower", MobEffects.JUMP_BOOST, 100);
        ours(entries, FloraContent.lilyOfTheValley, "lily_of_the_valley", MobEffects.POISON, 220);
        ours(entries, FloraContent.witherRose, "wither_rose", MobEffects.WITHER, 140);

        table = Collections.unmodifiableList(entries);
        return table;
    }

    private static void red(List<Entry> entries, BlockFlower.EnumFlowerType type, Potion effect, int ticks) {
        entries.add(new Entry(new ItemStack(Blocks.RED_FLOWER, 1, type.getMeta()), effect, ticks));
    }

    /** Our flower, and Future MC's of the same name when it is installed. */
    private static void ours(List<Entry> entries, @Nullable Block block, String name, Potion effect, int ticks) {
        if (block != null) {
            entries.add(new Entry(new ItemStack(block), effect, ticks));
        }
        Block theirs = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("futuremc", name));
        if (theirs != null && theirs != Blocks.AIR) {
            entries.add(new Entry(new ItemStack(theirs), effect, ticks));
        }
    }

    /** @return the entry for a held flower, or null if it is not a small flower with a stew effect */
    @Nullable
    public static Entry forStack(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof ItemBlock)) {
            return null;
        }
        for (Entry entry : table()) {
            if (entry.flower.getItem() == stack.getItem()
                    && (!entry.flower.getHasSubtypes() || entry.flower.getMetadata() == stack.getMetadata())) {
                return entry;
            }
        }
        return null;
    }
}
