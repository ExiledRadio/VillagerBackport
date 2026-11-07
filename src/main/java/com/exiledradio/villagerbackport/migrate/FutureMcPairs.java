package com.exiledradio.villagerbackport.migrate;

import com.exiledradio.backportcore.migrate.FutureMcTable;
import com.exiledradio.villagerbackport.ModConfig;
import com.exiledradio.villagerbackport.VillagerBackport;
import com.exiledradio.villagerbackport.block.TileEntityLectern;
import com.exiledradio.villagerbackport.campfire.TileEntityCampfire;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

/**
 * This mod's part of Backport Core's Future MC table: which of this mod's blocks and items stand in
 * for Future MC's, where the names alone don't say.
 *
 * <p>Core does the converting - {@code /futuremc}, and worlds Future MC has been taken out of (its
 * {@code migrate} package). A Future MC block becomes this mod's block of the same name unless it is
 * renamed here; what each metadata value meant is in {@code migration/futuremc_states.json}, written by
 * {@code /futuremc dump} on a server with Future MC 0.2.21 installed.
 */
public final class FutureMcPairs {

    private static final String VB = VillagerBackport.MOD_ID + ":";

    /** Future MC's one dye item, meta 0 to 3, to this mod's four. */
    private static final String[] DYES = {"white_dye", "blue_dye", "brown_dye", "black_dye"};

    private FutureMcPairs() {
    }

    /** From pre-init. */
    public static void register() {
        FutureMcTable.Contributor table = FutureMcTable.contributor(VillagerBackport.MOD_ID,
                () -> ModConfig.compat.convertRemovedFutureMc);

        for (String wood : new String[] {"spruce", "birch", "jungle", "acacia", "dark_oak"}) {
            // 1.14 calls the standing sign "spruce_sign"; Future MC calls it "spruce_standing_sign".
            table.rename(wood + "_standing_sign", VB + wood + "_sign");
        }
        // Future MC's "mossy stone" slab is drawn with mossy cobblestone, 1.14's mossy cobblestone slab.
        table.rename("mossy_stone_slab", VB + "mossy_cobblestone_slab");
        table.rename("mossy_stone_double_slab", VB + "mossy_cobblestone_double_slab");
        // 1.14's bark-on-all-sides wood is 1.12.2's own six-sided log. 1.12.2 draws it the same from
        // every side, so its axis is "none" whichever way Future MC's faced.
        table.rename("oak_wood", "minecraft:log|variant=oak,axis=none");
        table.rename("spruce_wood", "minecraft:log|variant=spruce,axis=none");
        table.rename("birch_wood", "minecraft:log|variant=birch,axis=none");
        table.rename("jungle_wood", "minecraft:log|variant=jungle,axis=none");
        table.rename("acacia_wood", "minecraft:log2|variant=acacia,axis=none");
        table.rename("dark_oak_wood", "minecraft:log2|variant=dark_oak,axis=none");

        // 1.14 counts bamboo's thickness as age 0/1 and its maturity as stage 0/1.
        table.renameProperty("bamboo", "thick", "age", true);
        table.renameProperty("bamboo", "mature", "stage", true);
        table.renameProperty("campfire", "signal", "signal_fire", false);
        // 1.14's cartography, fletching and smithing tables don't turn.
        table.ignoreProperty("cartography_table", "facing");
        table.ignoreProperty("fletching_table", "facing");
        table.ignoreProperty("smithing_table", "facing");

        table.items((path, damage) -> {
            if (!"dye".equals(path)) {
                return null;
            }
            if (damage < 0 || damage >= DYES.length) {
                return null;
            }
            Item dye = FutureMcTable.itemNamed(VB + DYES[damage]);
            return dye == null ? null : new FutureMcTable.ItemTarget(dye, 0);
        });

        table.recordedStates("/assets/villagerbackport/migration/futuremc_states.json");

        table.contents((ours, stack) -> {
            if (ours instanceof TileEntityLectern) {
                TileEntityLectern lectern = (TileEntityLectern) ours;
                if (!lectern.hasBook()) {
                    lectern.setBook(stack.copy());
                    return ItemStack.EMPTY;
                }
                return stack;
            }
            if (ours instanceof TileEntityCampfire) {
                TileEntityCampfire campfire = (TileEntityCampfire) ours;
                while (!stack.isEmpty() && campfire.placeFood(stack, ModConfig.campfire.cookTime)) {
                    // placeFood takes one at a time
                }
                return stack;
            }
            return null;
        });

        table.itemFixer((target, stack) -> {
            if (target instanceof com.exiledradio.villagerbackport.flora.ItemSuspiciousStew && stack.hasKey("tag", 10)) {
                stew(stack.getCompoundTag("tag"));
            }
        });
    }

    /**
     * Future MC keeps a stew's effect as a potion does ({@code CustomPotionEffects}); this mod keeps it
     * as 1.14 does ({@code Effects}).
     */
    private static void stew(NBTTagCompound tag) {
        NBTTagList potions = tag.getTagList("CustomPotionEffects", 10);
        if (potions.isEmpty()) {
            return;
        }
        NBTTagList effects = tag.getTagList("Effects", 10);
        for (int i = 0; i < potions.tagCount(); i++) {
            NBTTagCompound potion = potions.getCompoundTagAt(i);
            NBTTagCompound effect = new NBTTagCompound();
            effect.setByte("EffectId", potion.getByte("Id"));
            effect.setInteger("EffectDuration", potion.getInteger("Duration"));
            effects.appendTag(effect);
        }
        tag.setTag("Effects", effects);
        tag.removeTag("CustomPotionEffects");
    }
}
