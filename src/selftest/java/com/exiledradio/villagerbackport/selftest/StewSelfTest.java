package com.exiledradio.villagerbackport.selftest;

import com.exiledradio.villagerbackport.flora.StewEffects;
import net.minecraft.block.BlockFlower;
import net.minecraft.init.Blocks;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemStack;

/** {@code -Pselftest=stew}: suspicious stew's flower table, and {@code StewEffects.add} for later versions' flowers. */
public final class StewSelfTest implements SelfTest {

    @Override
    public String name() {
        return "stew";
    }

    @Override
    public void run(Check c) {
        StewEffects.Entry poppy = StewEffects.forStack(new ItemStack(Blocks.RED_FLOWER, 1, BlockFlower.EnumFlowerType.POPPY.getMeta()));
        c.ok(poppy != null && poppy.effect == MobEffects.NIGHT_VISION && poppy.ticks == 100, "a poppy: Night Vision, 5 s");
        int before = StewEffects.table().size();
        // A stand-in for a later version's flower: the dead bush, which has no entry.
        ItemStack flower = new ItemStack(Blocks.DEADBUSH);
        c.ok(StewEffects.forStack(flower) == null, "a dead bush has no stew effect");
        StewEffects.add(flower, MobEffects.LUCK, 60);
        StewEffects.Entry added = StewEffects.forStack(flower);
        c.ok(added != null && added.effect == MobEffects.LUCK && added.ticks == 60, "an added flower is found");
        c.eq(before + 1, StewEffects.table().size(), "the table grows by one, after this mod's flowers");
        c.ok(StewEffects.forStack(new ItemStack(Blocks.RED_FLOWER, 1, BlockFlower.EnumFlowerType.POPPY.getMeta())) != null, "the poppy is still there");
    }
}
