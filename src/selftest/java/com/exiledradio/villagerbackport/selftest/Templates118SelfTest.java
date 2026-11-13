package com.exiledradio.villagerbackport.selftest;

import com.exiledradio.villagerbackport.village.modern.ModernTemplate;

import net.minecraft.util.math.BlockPos;

import java.lang.reflect.Field;
import java.util.List;

/**
 * 1.18.2's templates for villages laid out as 1.18.2 lays them out (ModernTemplate.get(String, boolean)): the changed
 * files come from structures/v1_18/, the rest are 1.14's own, and the 1.12 path still reads 1.14's.
 */
public final class Templates118SelfTest implements SelfTest {

    @Override
    public String name() {
        return "templates118";
    }

    @Override
    public void run(Check c) throws Exception {
        c.section("1.18.2 templates");

        String plate = "pillager_outpost/feature_plate";
        c.eq(2, jigsaws(ModernTemplate.get(plate, false)).size(), "1.14's outpost feature plate has 2 jigsaws");
        c.eq(16, jigsaws(ModernTemplate.get(plate, true)).size(), "1.18.2's outpost feature plate has 16 jigsaws");
        c.ok(ModernTemplate.get(plate, false) == ModernTemplate.get(plate), "the 1.12 path reads 1.14's file");

        String street = "village/plains/streets/straight_01";
        c.ok(ModernTemplate.get(street, true) == ModernTemplate.get(street, false),
                "an unchanged file is 1.14's on both paths: " + street);

        String mason = "village/savanna/houses/savanna_mason_1";
        c.ok(!size(ModernTemplate.get(mason, true)).equals(size(ModernTemplate.get(mason, false))),
                "1.18.2's savanna mason is resized: " + size(ModernTemplate.get(mason, true)));

        List<?> terminator = jigsaws(ModernTemplate.get("village/plains/terminators/terminator_01", true));
        c.ok(terminator.size() == 1 && "empty".equals(field(terminator.get(0), "pool")),
                "1.18.2's plains terminator asks for nothing");
        c.eq("minecraft:street", field(terminator.get(0), "name"), "a 1.18.2 jigsaw keeps its name");

        List<?> old = jigsaws(ModernTemplate.get("village/plains/terminators/terminator_01", false));
        c.ok(old.size() == 1 && field(old.get(0), "name").equals(field(old.get(0), "target")),
                "a 1.14 jigsaw's attachment type is both its name and target");
    }

    private static List<?> jigsaws(ModernTemplate t) throws Exception {
        return (List<?>) field(t, "jigsaws");
    }

    private static BlockPos size(ModernTemplate t) throws Exception {
        return (BlockPos) field(t, "size");
    }

    private static Object field(Object o, String name) throws Exception {
        Field f = o.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f.get(o);
    }
}
