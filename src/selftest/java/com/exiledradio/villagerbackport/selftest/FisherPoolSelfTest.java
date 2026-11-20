package com.exiledradio.villagerbackport.selftest;

import com.exiledradio.backportcore.waterlog.Waterlogging;
import com.exiledradio.villagerbackport.village.modern.ModernPools;
import com.exiledradio.villagerbackport.village.modern.ModernTemplate;
import com.exiledradio.villagerbackport.village.modern.ModernVillagePiece;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraftforge.common.DimensionManager;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Random;

/**
 * The savanna fisher cottage's pool, placed as a village places it: every template water cell is still water,
 * and the two fence posts 1.14 waterlogs stand in water (with Fluidlogged API), not in a dry hole.
 */
public final class FisherPoolSelfTest implements SelfTest {

    private static final String POOL = "village/savanna/houses";
    private static final String LOCATION = "village/savanna/houses/savanna_fisher_cottage_1";

    @Override
    public String name() {
        return "fisherpool";
    }

    @Override
    public void run(Check c) throws Exception {
        c.section("savanna fisher cottage pool");
        WorldServer world = DimensionManager.getWorld(0);
        BlockPos at = new BlockPos(6000, 150, 6000);
        for (int dx = -16; dx <= 32; dx += 16) {
            for (int dz = -16; dz <= 32; dz += 16) {
                world.getChunk(at.add(dx, 0, dz));
            }
        }

        ModernTemplate template = ModernTemplate.get(LOCATION);
        BlockPos size = (BlockPos) field(template, "size");
        for (int x = -2; x < size.getX() + 2; x++) {
            for (int z = -2; z < size.getZ() + 2; z++) {
                for (int y = -1; y < size.getY() + 2; y++) {
                    world.setBlockState(at.add(x, y, z), y < 0 ? Blocks.STONE.getDefaultState() : Blocks.AIR.getDefaultState(), 2);
                }
            }
        }

        Object element = null;
        for (Object e : (List<?>) field(ModernPools.get(POOL), "distinct")) {
            if (e instanceof ModernPools.SingleElement && LOCATION.equals(field(e, "location"))) {
                element = e;
            }
        }
        if (!c.ok(element != null, "the savanna houses pool has the fisher cottage")) {
            return;
        }
        StructureBoundingBox box = new StructureBoundingBox(at, at.add(size.getX() - 1, size.getY() - 1, size.getZ() - 1));
        Constructor<ModernVillagePiece> ctor = ModernVillagePiece.class.getDeclaredConstructor(
                Class.forName("com.exiledradio.villagerbackport.village.modern.ModernPools$Element"),
                BlockPos.class, int.class, Rotation.class, StructureBoundingBox.class, boolean.class);
        ctor.setAccessible(true);
        ModernVillagePiece piece = ctor.newInstance(element, at, 0, Rotation.NONE, box, false);
        piece.addComponentParts(world, new Random(1), box);

        Object[] palette = (Object[]) field(template, "palette");
        int water = 0;
        int logged = 0;
        for (Object block : (List<?>) field(template, "blocks")) {
            BlockPos pos = at.add((BlockPos) field(block, "pos"));
            Object entry = palette[(Integer) field(block, "state")];
            String source = (String) field(entry, "source");
            NBTTagCompound props = (NBTTagCompound) field(entry, "sourceProps");
            IBlockState placed = world.getBlockState(pos);
            if ("minecraft:water".equals(source)) {
                water++;
                c.ok(placed.getBlock() == Blocks.WATER && placed.getValue(BlockLiquid.LEVEL) == 0,
                        "still water at " + pos + ": " + placed);
            } else if ("true".equals(props.getString("waterlogged"))) {
                logged++;
                c.ok(placed.getBlock() == Blocks.ACACIA_FENCE, "the waterlogged fence post is there at " + pos + ": " + placed);
                if (Waterlogging.isAvailable()) {
                    c.ok(Waterlogging.hasWaterSource(world, pos), "the fence post at " + pos + " stands in water");
                }
            }
        }
        c.eq(15, water, "the pool's water cells");
        c.eq(2, logged, "the pool's waterlogged fence posts");
        c.ok(Waterlogging.isAvailable(), "self-test runs with Fluidlogged API");
    }

    private static Object field(Object o, String name) throws Exception {
        Class<?> k = o.getClass();
        while (k != null) {
            try {
                Field f = k.getDeclaredField(name);
                f.setAccessible(true);
                return f.get(o);
            } catch (NoSuchFieldException e) {
                k = k.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }
}
