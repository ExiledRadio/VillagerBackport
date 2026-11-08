package com.exiledradio.villagerbackport.selftest;

import com.exiledradio.backportcore.waterlog.Waterlogging;
import com.exiledradio.villagerbackport.VillagerBackport;
import com.exiledradio.villagerbackport.migrate.BlockSeaPickleKept;
import com.exiledradio.villagerbackport.migrate.ItemSeaPickleStandIn;
import com.exiledradio.villagerbackport.migrate.UpdateAquaticPickles;
import com.exiledradio.villagerbackport.trader.TraderTrades;
import com.exiledradio.villagerbackport.village.modern.ModernTemplate;

import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.village.MerchantRecipe;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Random;

/**
 * Sea pickles leaving Village &amp; Pillage for Update Aquatic Backport, and the ones older worlds hold.
 *
 * <p>Runs in three games on one world, each a plain {@code -Pselftest=all} (or {@code picklemove}) run:
 * <ol>
 *   <li><b>A fresh world, no Update Aquatic</b>: Village &amp; Pillage offers no sea pickle (no creative tab,
 *   recipe, trade or village use), and the kept pickle behaves. It then lays out pickles as 2.3.0 to 2.5.0
 *   left them: one to four, dry, in real water, and with 2.4.0's stored flag, and a chest of pickle items.
 *   A marker file in the world folder records that.</li>
 *   <li><b>The same world again, no Update Aquatic</b>: everything laid out is still there, unchanged.</li>
 *   <li><b>The same world with {@code fake-ua} in the run directory</b> ({@link FakeUpdateAquatic}): the old
 *   name is missing, so every pickle and item is Update Aquatic's now, with the same count and water, and
 *   the stored flags became real water. The trader sells Update Aquatic's pickles and villages place them.</li>
 * </ol>
 */
public final class PickleMoveSelfTest implements SelfTest {

    private static final String MARKER = "picklemove-selftest.txt";
    private static final int X0 = 6000;
    private static final int Y = 200;
    private static final int Z0 = 6000;

    private WorldServer world;

    @Override
    public String name() {
        return "picklemove";
    }

    // ---- fixed places, the same in every run

    private static BlockPos dry(int k) {
        return new BlockPos(X0 + k * 3, Y, Z0);
    }

    private static BlockPos wet(int k) {
        return new BlockPos(X0 + k * 3, Y, Z0 + 4);
    }

    private static BlockPos stored(int k) {
        return new BlockPos(X0 + k * 3, Y, Z0 + 8);
    }

    private static BlockPos chest() {
        return new BlockPos(X0, Y, Z0 + 12);
    }

    @Override
    public void run(Check c) throws Exception {
        this.world = DimensionManager.getWorld(0);
        for (int dx = -32; dx <= 48; dx += 16) {
            for (int dz = -32; dz <= 64; dz += 16) {
                this.world.getChunk(new BlockPos(X0 + dx, Y, Z0 + dz));
            }
        }
        File marker = new File(this.world.getSaveHandler().getWorldDirectory(), MARKER);
        String phase = marker.exists() ? new String(Files.readAllBytes(marker.toPath()), StandardCharsets.UTF_8).trim() : "";
        Block ua = block(UpdateAquaticPickles.UA_PICKLE);
        VillagerBackport.LOGGER.info("SELFTEST picklemove: Update Aquatic's pickle {}, world marker '{}'",
                ua == null ? "absent" : ua.getRegistryName() + " (" + ua.getClass().getSimpleName() + ")", phase);

        common(c, ua);
        if (ua == null) {
            keptBehaves(c);
            if (phase.isEmpty()) {
                layOut();
                Files.write(marker.toPath(), "laid out".getBytes(StandardCharsets.UTF_8));
                VillagerBackport.LOGGER.info("SELFTEST picklemove: older-world pickles laid out; run again, then again with fake-ua");
            } else if ("laid out".equals(phase)) {
                laidOutStillKept(c);
            } else {
                VillagerBackport.LOGGER.info("SELFTEST picklemove: this world already moved to Update Aquatic; delete it to start over");
            }
        } else if (!phase.isEmpty()) {
            nowUpdateAquatics(c, ua);
            Files.write(marker.toPath(), "moved".getBytes(StandardCharsets.UTF_8));
        }
    }

    // ---- helpers

    private static Block block(ResourceLocation name) {
        Block b = ForgeRegistries.BLOCKS.containsKey(name) ? ForgeRegistries.BLOCKS.getValue(name) : null;
        return b == null || b == Blocks.AIR ? null : b;
    }

    private static Item item(ResourceLocation name) {
        Item i = ForgeRegistries.ITEMS.containsKey(name) ? ForgeRegistries.ITEMS.getValue(name) : null;
        return i == null || i == Items.AIR ? null : i;
    }

    private static Object prop(IBlockState state, String name) {
        for (IProperty<?> p : state.getPropertyKeys()) {
            if (p.getName().equals(name)) {
                return state.getValue(p);
            }
        }
        return null;
    }

    private void stone(BlockPos pos) {
        for (int dy = 0; dy <= 2; dy++) {
            this.world.setBlockState(pos.up(dy), Blocks.AIR.getDefaultState(), 2 | Waterlogging.REMOVE_WATER);
        }
        this.world.setBlockState(pos.down(), Blocks.STONE.getDefaultState(), 2);
    }

    private IBlockState keptState(int pickles, boolean flag) {
        return UpdateAquaticPickles.kept().getDefaultState().withProperty(BlockSeaPickleKept.PICKLES, pickles)
                .withProperty(BlockSeaPickleKept.WATERLOGGED, flag);
    }

    /** A state written straight into the chunk, as a chunk load does: no onBlockAdded, no water. */
    private void raw(BlockPos pos, IBlockState state) {
        Chunk chunk = this.world.getChunk(pos);
        chunk.getBlockStorageArray()[pos.getY() >> 4].set(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15, state);
    }

    private int light(BlockPos pos) {
        this.world.checkLightFor(EnumSkyBlock.BLOCK, pos);
        return this.world.getLightFor(EnumSkyBlock.BLOCK, pos);
    }

    // ---- every run

    /** What Village &amp; Pillage offers: no sea pickle of its own; Update Aquatic's when it is there. */
    private void common(Check c, Block ua) throws Exception {
        Item uaItem = item(UpdateAquaticPickles.UA_PICKLE);
        if (ua == null) {
            Block kept = block(UpdateAquaticPickles.OLD_PICKLE);
            Item standIn = item(UpdateAquaticPickles.OLD_PICKLE);
            c.ok(kept instanceof BlockSeaPickleKept && kept == UpdateAquaticPickles.kept(), "without Update Aquatic, the kept pickle holds the old name: " + kept);
            c.ok(standIn instanceof ItemSeaPickleStandIn && standIn == UpdateAquaticPickles.standIn(), "and the stand-in item does: " + standIn);
            c.ok(kept != null && kept.getCreativeTab() == null, "the kept pickle is in no creative tab");
            c.ok(standIn != null && standIn.getCreativeTab() == null, "the stand-in is in no creative tab");
            c.ok(!(standIn instanceof ItemBlock), "the stand-in places nothing");
            c.ok(standIn != null && FurnaceRecipes.instance().getSmeltingResult(new ItemStack(standIn)).isEmpty(), "the stand-in smelts to nothing");
            c.eq("Sea Pickle", standIn == null ? null : new ItemStack(standIn).getDisplayName(), "the stand-in is named");
        } else {
            c.ok(UpdateAquaticPickles.kept() == null && UpdateAquaticPickles.standIn() == null, "with Update Aquatic, nothing under the old name");
            Block old = ForgeRegistries.BLOCKS.getValue(UpdateAquaticPickles.OLD_PICKLE);
            c.ok(old == Blocks.AIR || old == ua, "the old block name is unregistered or Update Aquatic's: " + old);
        }

        int recipes = 0;
        for (IRecipe recipe : ForgeRegistries.RECIPES) {
            ResourceLocation out = recipe.getRecipeOutput().getItem().getRegistryName();
            if (out != null && out.getPath().equals("sea_pickle") && VillagerBackport.MOD_ID.equals(out.getNamespace())) {
                recipes++;
            }
        }
        c.eq(0, recipes, "no recipe makes a Village & Pillage sea pickle");

        boolean sells = false;
        for (MerchantRecipe recipe : TraderTrades.examples(new Random(1))) {
            ResourceLocation id = recipe.getItemToSell().getItem().getRegistryName();
            if (id != null && "sea_pickle".equals(id.getPath())) {
                sells = true;
                c.ok(uaItem != null && recipe.getItemToSell().getItem() == uaItem, "the trader's sea pickle is Update Aquatic's: " + id);
            }
        }
        c.eq(uaItem != null, sells, "the trader sells sea pickles exactly when Update Aquatic is installed");

        for (String house : new String[] {"village/desert/houses/desert_small_house_8", "village/desert/zombie/houses/desert_small_house_8"}) {
            IBlockState s = paletteState(house, "minecraft:sea_pickle");
            if (ua != null) {
                c.ok(s != null && s.getBlock() == ua && Integer.valueOf(2).equals(prop(s, "pickles"))
                        && Boolean.FALSE.equals(prop(s, "waterlogged")), house + ": Update Aquatic's two dry pickles: " + s);
            } else {
                c.ok(s != null && s.getBlock() == Blocks.AIR, house + ": the stand stays bare without Update Aquatic: " + s);
            }
        }
    }

    /** The state a village file's palette entry places, through the real path. */
    private static IBlockState paletteState(String location, String source) throws Exception {
        ModernTemplate template = ModernTemplate.get(location);
        Field paletteField = ModernTemplate.class.getDeclaredField("palette");
        paletteField.setAccessible(true);
        Object[] palette = (Object[]) paletteField.get(template);
        for (Object entry : palette) {
            Field sourceField = entry.getClass().getDeclaredField("source");
            sourceField.setAccessible(true);
            if (source.equals(sourceField.get(entry))) {
                Method state = entry.getClass().getDeclaredMethod("state");
                state.setAccessible(true);
                return (IBlockState) state.invoke(entry);
            }
        }
        return null;
    }

    // ---- without Update Aquatic

    /** The kept pickle: holds water, lights in it, pops off as stand-ins, converts a stored flag. */
    private void keptBehaves(Check c) throws Exception {
        Block kept = UpdateAquaticPickles.kept();
        BlockPos base = new BlockPos(X0, Y, Z0 + 40);
        c.ok(!kept.canPlaceBlockAt(this.world, base), "the kept pickle can't be placed");
        for (int k = 1; k <= 4; k++) {
            IBlockState s = keptState(k, k % 2 == 0);
            c.eq(s, kept.getStateFromMeta(kept.getMetaFromState(s)), "2.3.0's metadata reads back for " + s);
        }

        // In water it glows, dry it is dark, and the water stays when drained and refilled.
        BlockPos w = base.add(2, 0, 0);
        stone(w);
        Waterlogging.placeWithWater(this.world, w, keptState(3, false), 3);
        c.ok(Waterlogging.hasWaterSource(this.world, w), "a kept pickle holds water");
        c.eq(12, light(w), "three kept pickles in water glow 12");
        Waterlogging.setWater(this.world, w, false, 3);
        c.eq(0, light(w), "drained, dark");
        c.ok(this.world.getBlockState(w).getBlock() == kept, "and still there");

        // A 2.4.0 chunk's stored flag becomes real water, keeping the count.
        BlockPos s = new BlockPos(X0, Y, Z0 + 56);
        stone(s);
        raw(s, keptState(2, true));
        Class<?> migration = Class.forName("com.exiledradio.backportcore.waterlog.WaterlogMigration");
        Method convert = migration.getDeclaredMethod("convert", net.minecraft.world.World.class, Chunk.class);
        convert.setAccessible(true);
        int moved = (Integer) convert.invoke(null, this.world, this.world.getChunk(s));
        IBlockState here = this.world.getBlockState(s);
        c.ok(moved >= 1 && !here.getValue(BlockSeaPickleKept.WATERLOGGED) && Waterlogging.hasWaterSource(this.world, s),
                "a 2.4.0 kept pickle's stored flag became real water: moved=" + moved + " " + here);
        c.eq(2, here.getValue(BlockSeaPickleKept.PICKLES), "with its count");

        // Pops off as its pickles, as stand-ins.
        BlockPos p = base.add(6, 0, 0);
        stone(p);
        Waterlogging.placeDry(this.world, p, keptState(3, false), 3);
        this.world.setBlockState(p.down(), Blocks.AIR.getDefaultState(), 3);
        c.ok(this.world.isAirBlock(p), "a kept pickle pops off without support");
        int dropped = 0;
        for (EntityItem e : this.world.getEntitiesWithinAABB(EntityItem.class, new AxisAlignedBB(p).grow(2))) {
            if (e.getItem().getItem() == UpdateAquaticPickles.standIn()) {
                dropped += e.getItem().getCount();
                e.setDead();
            }
        }
        c.eq(3, dropped, "and drops its three pickles as stand-ins");
    }

    /** As 2.3.0 to 2.5.0 left them: one to four, dry, in real water and with 2.4.0's stored flag; a chest of items. */
    private void layOut() {
        for (int k = 1; k <= 4; k++) {
            stone(dry(k));
            Waterlogging.placeDry(this.world, dry(k), keptState(k, false), 3);
            stone(wet(k));
            Waterlogging.placeWithWater(this.world, wet(k), keptState(k, false), 3);
            stone(stored(k));
            raw(stored(k), keptState(k, true));
        }
        stone(chest());
        this.world.setBlockState(chest(), Blocks.CHEST.getDefaultState(), 3);
        TileEntity te = this.world.getTileEntity(chest());
        if (te instanceof TileEntityChest) {
            ((TileEntityChest) te).setInventorySlotContents(0, new ItemStack(UpdateAquaticPickles.standIn(), 5));
            ((TileEntityChest) te).setInventorySlotContents(1, new ItemStack(UpdateAquaticPickles.standIn(), 64));
            te.markDirty();
        }
    }

    private void laidOutStillKept(Check c) throws Exception {
        finishMigrations();
        Block kept = UpdateAquaticPickles.kept();
        for (int k = 1; k <= 4; k++) {
            IBlockState d = this.world.getBlockState(dry(k));
            c.ok(d.getBlock() == kept && d.getValue(BlockSeaPickleKept.PICKLES) == k && !Waterlogging.hasWater(this.world, dry(k)),
                    "reloaded: dry " + k + " still kept and dry: " + d);
            IBlockState w = this.world.getBlockState(wet(k));
            c.ok(w.getBlock() == kept && w.getValue(BlockSeaPickleKept.PICKLES) == k && Waterlogging.hasWaterSource(this.world, wet(k)),
                    "reloaded: wet " + k + " still kept, in its water: " + w);
            c.eq(3 + 3 * k, light(wet(k)), "reloaded: wet " + k + " glows");
            IBlockState s = this.world.getBlockState(stored(k));
            c.ok(s.getBlock() == kept && s.getValue(BlockSeaPickleKept.PICKLES) == k
                            && (s.getValue(BlockSeaPickleKept.WATERLOGGED) || Waterlogging.hasWaterSource(this.world, stored(k))),
                    "reloaded: 2.4.0 " + k + " still kept and wet: " + s);
        }
        chestHolds(c, UpdateAquaticPickles.standIn(), "reloaded");
    }

    private void chestHolds(Check c, Item item, String when) {
        TileEntity te = this.world.getTileEntity(chest());
        c.ok(te instanceof TileEntityChest, when + ": the chest is there");
        if (te instanceof TileEntityChest) {
            ItemStack a = ((TileEntityChest) te).getStackInSlot(0);
            ItemStack b = ((TileEntityChest) te).getStackInSlot(1);
            c.ok(a.getItem() == item && a.getCount() == 5, when + ": chest slot 0 holds 5 pickles: " + a + " " + a.getItem().getRegistryName());
            c.ok(b.getItem() == item && b.getCount() == 64, when + ": chest slot 1 holds 64 pickles: " + b + " " + b.getItem().getRegistryName());
        }
    }

    // ---- with Update Aquatic, in a world laid out without it

    /**
     * Runs Backport Core's chunk-load water migration for the chunks this test loaded: it is queued as
     * they load and done at the end of the world tick, after this test has already run.
     */
    private void finishMigrations() throws Exception {
        Class<?> events = Class.forName("com.exiledradio.backportcore.waterlog.WaterlogMigration$Events");
        java.lang.reflect.Constructor<?> make = events.getDeclaredConstructor();
        make.setAccessible(true);
        Object handler = make.newInstance();
        Method tick = events.getDeclaredMethod("onWorldTick", net.minecraftforge.fml.common.gameevent.TickEvent.WorldTickEvent.class);
        tick.setAccessible(true);
        for (int i = 0; i < 20; i++) {
            tick.invoke(handler, new net.minecraftforge.fml.common.gameevent.TickEvent.WorldTickEvent(
                    net.minecraftforge.fml.relauncher.Side.SERVER, net.minecraftforge.fml.common.gameevent.TickEvent.Phase.END, this.world));
        }
    }

    private void nowUpdateAquatics(Check c, Block ua) throws Exception {
        finishMigrations();
        for (int k = 1; k <= 4; k++) {
            IBlockState d = this.world.getBlockState(dry(k));
            c.ok(d.getBlock() == ua && Integer.valueOf(k).equals(prop(d, "pickles")) && !Waterlogging.hasWater(this.world, dry(k)),
                    "dry " + k + " is Update Aquatic's, dry, same count: " + d);
            IBlockState w = this.world.getBlockState(wet(k));
            c.ok(w.getBlock() == ua && Integer.valueOf(k).equals(prop(w, "pickles")) && Waterlogging.hasWaterSource(this.world, wet(k)),
                    "wet " + k + " is Update Aquatic's, in its water, same count: " + w);
            IBlockState s = this.world.getBlockState(stored(k));
            c.ok(s.getBlock() == ua && Integer.valueOf(k).equals(prop(s, "pickles")) && Waterlogging.hasWaterSource(this.world, stored(k))
                            && Boolean.FALSE.equals(prop(s, "waterlogged")),
                    "2.4.0 " + k + " is Update Aquatic's, its stored flag now real water: " + s);
        }
        chestHolds(c, item(UpdateAquaticPickles.UA_PICKLE), "moved");
        List<EntityItem> spilled = this.world.getEntitiesWithinAABB(EntityItem.class, new AxisAlignedBB(chest()).grow(1));
        c.ok(spilled.isEmpty(), "nothing spilled at the chest");
    }
}
