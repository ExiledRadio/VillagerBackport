package com.exiledradio.villagerbackport.selftest;

import com.exiledradio.backportcore.waterlog.Waterloggable;
import com.exiledradio.backportcore.waterlog.Waterlogging;
import com.exiledradio.villagerbackport.VillagerBackport;
import com.exiledradio.villagerbackport.bamboo.BlockScaffolding;
import com.exiledradio.villagerbackport.block.BlockLantern;
import com.exiledradio.villagerbackport.building.BlockBuildingSlab;
import com.exiledradio.villagerbackport.building.BlockBuildingStairs;
import com.exiledradio.villagerbackport.building.BlockBuildingWall;
import com.exiledradio.villagerbackport.building.BlockWoodSign;
import com.exiledradio.villagerbackport.campfire.BlockCampfire;
import com.exiledradio.villagerbackport.woodset.BlockSeaPickle;
import com.exiledradio.villagerbackport.woodset.BlockWoodRedstone;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemDye;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Waterlogging through Backport Core's Fluidlogged API adapter: which of this mod's blocks hold water
 * (current Java's list), lanterns, scaffolding, campfires and sea pickles in and out of water, and the
 * one-time move of a 2.4.0 sea pickle's stored {@code waterlogged} flag into real water.
 */
public final class WaterlogSelfTest implements SelfTest {

    private static final String ID = VillagerBackport.MOD_ID;

    private WorldServer world;
    private FakePlayer player;
    /** Next free test spot: each gets its own stone cup, four blocks apart. */
    private int nextX;
    private static final int Y = 200;
    private static final int X0 = 4000;
    private static final int Z0 = 4000;

    @Override
    public String name() {
        return "waterlog";
    }

    @Override
    public void run(Check c) throws Exception {
        this.world = DimensionManager.getWorld(0);
        this.player = FakePlayerFactory.getMinecraft(this.world);
        this.nextX = X0;
        // Lighting only updates where everything within 16 blocks is loaded.
        for (int dx = -32; dx <= 256; dx += 16) {
            for (int dz = -32; dz <= 32; dz += 16) {
                this.world.getChunk(new BlockPos(X0 + dx, Y, Z0 + dz));
            }
        }
        whichHoldWater(c);
        lanterns(c);
        scaffolding(c);
        campfire(c);
        seaPickleWater(c);
        seaPickleLight(c);
        seaPickleStoredFlag(c);
        seaPickleBoneMeal(c);
    }

    // ---- helpers

    private static Block block(String name) {
        return ForgeRegistries.BLOCKS.getValue(new ResourceLocation(ID, name));
    }

    /** A stone cup: floor and four walls around an air spot, open above. */
    private BlockPos cup() {
        BlockPos pos = new BlockPos(this.nextX, Y, Z0);
        this.nextX += 4;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int dy = -1; dy <= 3; dy++) {
                    this.world.setBlockState(pos.add(dx, dy, dz), Blocks.AIR.getDefaultState(), 2 | Waterlogging.REMOVE_WATER);
                }
            }
        }
        this.world.setBlockState(pos.down(), Blocks.STONE.getDefaultState(), 2);
        for (EnumFacing side : EnumFacing.HORIZONTALS) {
            this.world.setBlockState(pos.offset(side), Blocks.STONE.getDefaultState(), 2);
        }
        return pos;
    }

    /** A cup holding a still water source. */
    private BlockPos waterCup() {
        BlockPos pos = cup();
        this.world.setBlockState(pos, Blocks.WATER.getDefaultState(), 3);
        return pos;
    }

    private void hold(ItemStack stack) {
        this.player.setHeldItem(EnumHand.MAIN_HAND, stack);
    }

    /** Stands the fake player over the spot, looking straight down, and uses what it holds. */
    private ItemStack useFromAbove(BlockPos pos, ItemStack stack) {
        hold(stack);
        this.player.setLocationAndAngles(pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 0.0F, 90.0F);
        return stack.getItem().onItemRightClick(this.world, this.player, EnumHand.MAIN_HAND).getResult();
    }

    private int blockLight(BlockPos pos) {
        return this.world.getLightFor(EnumSkyBlock.BLOCK, pos);
    }

    // ---- which blocks hold water

    /** @return whether current Java waterlogs this block of ours */
    private static boolean javaWaterlogs(Block b) {
        if (b instanceof BlockBuildingSlab) {
            return !((BlockBuildingSlab) b).isDouble();
        }
        return b instanceof BlockBuildingStairs || b instanceof BlockBuildingWall
                || b instanceof BlockWoodSign.Standing || b instanceof BlockWoodSign.Wall
                || b instanceof BlockWoodRedstone.Trapdoor
                || b instanceof BlockLantern || b instanceof BlockCampfire
                || b instanceof BlockScaffolding || b instanceof BlockSeaPickle;
    }

    private void whichHoldWater(Check c) {
        BlockPos spot = cup();
        int checked = 0;
        List<String> wrong = new ArrayList<String>();
        for (Block b : ForgeRegistries.BLOCKS.getValuesCollection()) {
            ResourceLocation name = b.getRegistryName();
            if (name == null || !ID.equals(name.getNamespace())) {
                continue;
            }
            boolean expected = javaWaterlogs(b);
            boolean holds = Waterlogging.canHoldWater(b.getDefaultState(), this.world, spot);
            checked++;
            if (expected != holds || (expected && !(b instanceof Waterloggable))) {
                wrong.add(name.getPath() + (expected ? " should hold water" : " should not hold water"));
            }
        }
        c.ok(checked > 50, "checked every block of ours: " + checked);
        c.ok(wrong.isEmpty(), "current Java's waterloggable list: " + wrong);
        for (String name : new String[] {"bell", "grindstone", "lectern", "composter", "barrel", "stonecutter",
                "smithing_table", "loom", "blast_furnace", "smoker", "bamboo", "sweet_berry_bush", "blue_ice",
                "cornflower", "oak_button", "oak_pressure_plate"}) {
            Block b = block(name);
            if (b == null || b == Blocks.AIR) {
                continue;
            }
            c.ok(!Waterlogging.canHoldWater(b.getDefaultState(), this.world, spot), name + " holds no water (current Java)");
        }
        for (String name : new String[] {"lantern", "campfire", "scaffolding", "sea_pickle"}) {
            Block b = block(name);
            c.ok(b != null && Waterlogging.canHoldWater(b.getDefaultState(), this.world, spot), name + " holds water");
        }
    }

    /** Placed into a source it keeps it; drained and refilled; the block itself is unchanged throughout. */
    private void holdsWater(Check c, String what, IBlockState state, boolean bucketFromAbove) {
        BlockPos pos = waterCup();
        if (what.startsWith("hanging")) {
            this.world.setBlockState(pos.up(), Blocks.STONE.getDefaultState(), 2);
        }
        this.world.setBlockState(pos, state, 3);
        c.ok(this.world.getBlockState(pos).getBlock() == state.getBlock(), what + " placed into water");
        c.ok(Waterlogging.hasWaterSource(this.world, pos), what + " placed into a water source keeps it");
        c.ok(Waterlogging.setWater(this.world, pos, false, 3) && !Waterlogging.hasWater(this.world, pos), what + " drains");
        c.ok(this.world.getBlockState(pos).getBlock() == state.getBlock(), what + " stays when drained");
        c.ok(Waterlogging.setWater(this.world, pos, true, 3) && Waterlogging.hasWaterSource(this.world, pos), what + " fills again");
        c.ok(this.world.getBlockState(pos).getBlock() == state.getBlock(), what + " stays when filled");
        if (bucketFromAbove) {
            ItemStack after = useFromAbove(pos, new ItemStack(Items.BUCKET));
            c.ok(after.getItem() == Items.WATER_BUCKET && !Waterlogging.hasWater(this.world, pos)
                    && this.world.getBlockState(pos).getBlock() == state.getBlock(), what + ": an empty bucket takes its water (" + after + ")");
            after = useFromAbove(pos, new ItemStack(Items.WATER_BUCKET));
            c.ok(after.getItem() == Items.BUCKET && Waterlogging.hasWaterSource(this.world, pos)
                    && this.world.getBlockState(pos).getBlock() == state.getBlock(), what + ": a water bucket waterlogs it (" + after + ")");
        }
    }

    private void lanterns(Check c) {
        Block lantern = block("lantern");
        holdsWater(c, "standing lantern", lantern.getDefaultState().withProperty(BlockLantern.HANGING, false), true);
        holdsWater(c, "hanging lantern", lantern.getDefaultState().withProperty(BlockLantern.HANGING, true), false);
    }

    private void scaffolding(Check c) {
        holdsWater(c, "scaffolding", block("scaffolding").getDefaultState(), true);
    }

    private void campfire(Check c) {
        Block campfire = block("campfire");
        IBlockState lit = campfire.getDefaultState().withProperty(BlockCampfire.LIT, true);
        holdsWater(c, "campfire", campfire.getDefaultState().withProperty(BlockCampfire.LIT, false), true);

        // Placed into water: placed out.
        BlockPos wet = waterCup();
        IBlockState placed = campfire.getStateForPlacement(this.world, wet, EnumFacing.UP, 0.5F, 1.0F, 0.5F, 0, this.player);
        c.ok(!placed.getValue(BlockCampfire.LIT), "a campfire placed into water is placed out");
        BlockPos dry = cup();
        placed = campfire.getStateForPlacement(this.world, dry, EnumFacing.UP, 0.5F, 1.0F, 0.5F, 0, this.player);
        c.ok(placed.getValue(BlockCampfire.LIT), "a campfire placed dry is placed lit");

        // Water reaching a lit one puts it out.
        BlockPos pos = cup();
        this.world.setBlockState(pos, lit, 3);
        c.ok(BlockCampfire.isLit(this.world.getBlockState(pos)), "the campfire starts lit");
        Waterlogging.setWater(this.world, pos, true, 3);
        c.ok(Waterlogging.hasWaterSource(this.world, pos), "the campfire holds the water");
        c.ok(!BlockCampfire.isLit(this.world.getBlockState(pos)), "water puts a lit campfire out");

        // Water bucket from above on a lit one.
        BlockPos pos2 = cup();
        this.world.setBlockState(pos2, lit, 3);
        ItemStack after = useFromAbove(pos2, new ItemStack(Items.WATER_BUCKET));
        c.ok(Waterlogging.hasWaterSource(this.world, pos2) && !BlockCampfire.isLit(this.world.getBlockState(pos2)),
                "a water bucket waterlogs a lit campfire and puts it out (" + after + ")");

        // Can't be lit while waterlogged.
        hold(new ItemStack(Items.FLINT_AND_STEEL));
        IBlockState state = this.world.getBlockState(pos);
        state.getBlock().onBlockActivated(this.world, pos, state, this.player, EnumHand.MAIN_HAND, EnumFacing.UP, 0.5F, 1.0F, 0.5F);
        c.ok(!BlockCampfire.isLit(this.world.getBlockState(pos)), "flint and steel cannot light a waterlogged campfire");
        hold(new ItemStack(Items.FIRE_CHARGE));
        state = this.world.getBlockState(pos);
        state.getBlock().onBlockActivated(this.world, pos, state, this.player, EnumHand.MAIN_HAND, EnumFacing.UP, 0.5F, 1.0F, 0.5F);
        c.ok(!BlockCampfire.isLit(this.world.getBlockState(pos)), "a fire charge cannot light a waterlogged campfire");
        BlockCampfire.ignite(this.world, pos, this.world.getBlockState(pos));
        c.ok(!BlockCampfire.isLit(this.world.getBlockState(pos)), "a burning arrow cannot light a waterlogged campfire");

        // Drained, it stays out until lit, then lights.
        Waterlogging.setWater(this.world, pos, false, 3);
        c.ok(!BlockCampfire.isLit(this.world.getBlockState(pos)), "draining does not relight it");
        hold(new ItemStack(Items.FLINT_AND_STEEL));
        state = this.world.getBlockState(pos);
        state.getBlock().onBlockActivated(this.world, pos, state, this.player, EnumHand.MAIN_HAND, EnumFacing.UP, 0.5F, 1.0F, 0.5F);
        c.ok(BlockCampfire.isLit(this.world.getBlockState(pos)), "flint and steel relights it once drained");
    }

    private void seaPickleWater(Check c) {
        Block pickle = block("sea_pickle");
        holdsWater(c, "sea pickle", pickle.getDefaultState(), true);

        // A player placing a pickle on the floor of a water source.
        BlockPos pos = waterCup();
        hold(new ItemStack(pickle));
        this.player.setLocationAndAngles(pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 0.0F, 90.0F);
        player.getHeldItemMainhand().onItemUse(this.player, this.world, pos.down(), EnumHand.MAIN_HAND, EnumFacing.UP, 0.5F, 1.0F, 0.5F);
        IBlockState here = this.world.getBlockState(pos);
        c.ok(here.getBlock() == pickle, "a player places a pickle into water: " + here);
        c.ok(Waterlogging.hasWaterSource(this.world, pos), "a pickle placed into water is waterlogged");
        c.ok(here.getBlock() == pickle && !here.getValue(BlockSeaPickle.WATERLOGGED), "the stored flag stays false");
        c.ok(here.getActualState(this.world, pos).getValue(BlockSeaPickle.WATERLOGGED), "the actual state reports the water");

        // Adding a pickle keeps the water.
        hold(new ItemStack(pickle));
        here.getBlock().onBlockActivated(this.world, pos, here, this.player, EnumHand.MAIN_HAND, EnumFacing.UP, 0.5F, 0.5F, 0.5F);
        here = this.world.getBlockState(pos);
        c.eq(2, here.getValue(BlockSeaPickle.PICKLES), "a second pickle added");
        c.ok(Waterlogging.hasWaterSource(this.world, pos), "adding a pickle keeps the water");

        // Draining never breaks it.
        Waterlogging.setWater(this.world, pos, false, 3);
        c.ok(this.world.getBlockState(pos).getBlock() == pickle, "a drained pickle stays");
        c.ok(!this.world.getBlockState(pos).getActualState(this.world, pos).getValue(BlockSeaPickle.WATERLOGGED), "the actual state reports it dry");
    }

    private void seaPickleLight(Check c) {
        Block pickle = block("sea_pickle");
        // Twenty blocks apart, so no cup's light reaches the next one over its walls.
        for (int n = 1; n <= 4; n++) {
            this.nextX += 16;
            BlockPos pos = waterCup();
            this.world.setBlockState(pos, pickle.getDefaultState().withProperty(BlockSeaPickle.PICKLES, n), 3);
            c.eq(3 + 3 * n, blockLight(pos), n + " waterlogged pickles' light");
            Waterlogging.setWater(this.world, pos, false, 3);
            c.eq(0, blockLight(pos), n + " dry pickles' light");
            Waterlogging.setWater(this.world, pos, true, 3);
            c.eq(3 + 3 * n, blockLight(pos), n + " pickles' light once water returns");
        }
        this.nextX += 16;
        BlockPos dry = cup();
        this.world.setBlockState(dry, pickle.getDefaultState().withProperty(BlockSeaPickle.PICKLES, 4), 3);
        c.eq(0, blockLight(dry), "a pickle placed dry gives no light");
    }

    private void seaPickleStoredFlag(Check c) throws Exception {
        Block pickle = block("sea_pickle");
        IBlockState stored = pickle.getDefaultState().withProperty(BlockSeaPickle.PICKLES, 3).withProperty(BlockSeaPickle.WATERLOGGED, true);
        c.eq(6, pickle.getMetaFromState(stored), "2.4.0's metadata for three waterlogged pickles");
        c.eq(stored, pickle.getStateFromMeta(6), "2.4.0's metadata reads back");

        // Placed with the flag (a structure, a command): moved into real water at once.
        BlockPos pos = cup();
        this.world.setBlockState(pos, stored, 3);
        IBlockState here = this.world.getBlockState(pos);
        c.ok(here.getBlock() == pickle && !here.getValue(BlockSeaPickle.WATERLOGGED), "the stored flag is cleared on placement");
        c.ok(Waterlogging.hasWaterSource(this.world, pos), "the stored flag became a water source");
        c.eq(12, blockLight(pos), "three pickles moved into water glow 12");

        // A saved 2.4.0 chunk: the state written straight into storage, as a chunk load does, then
        // Core's chunk conversion.
        BlockPos old = cup();
        Chunk chunk = this.world.getChunk(old);
        ExtendedBlockStorage storage = chunk.getBlockStorageArray()[old.getY() >> 4];
        storage.set(old.getX() & 15, old.getY() & 15, old.getZ() & 15, stored);
        c.ok(this.world.getBlockState(old).getValue(BlockSeaPickle.WATERLOGGED) && !Waterlogging.hasWater(this.world, old),
                "a 2.4.0 pickle: flag stored, no real water");
        Class<?> migration = Class.forName("com.exiledradio.backportcore.waterlog.WaterlogMigration");
        Method convert = migration.getDeclaredMethod("convert", net.minecraft.world.World.class, Chunk.class);
        convert.setAccessible(true);
        int moved = (Integer) convert.invoke(null, this.world, chunk);
        c.ok(moved >= 1, "the chunk conversion found the stored pickle: " + moved);
        here = this.world.getBlockState(old);
        c.ok(here.getBlock() == pickle && !here.getValue(BlockSeaPickle.WATERLOGGED), "the chunk conversion cleared the flag");
        c.ok(Waterlogging.hasWaterSource(this.world, old), "the chunk conversion gave it a water source");
        c.eq(3, here.getValue(BlockSeaPickle.PICKLES), "the count is unchanged");
        c.eq(0, (Integer) convert.invoke(null, this.world, chunk), "a second conversion finds nothing");

        // A stored pickle reached by an update before its chunk conversion.
        BlockPos late = cup();
        chunk = this.world.getChunk(late);
        chunk.getBlockStorageArray()[late.getY() >> 4].set(late.getX() & 15, late.getY() & 15, late.getZ() & 15, stored);
        this.world.neighborChanged(late, Blocks.STONE, late.down());
        here = this.world.getBlockState(late);
        c.ok(!here.getValue(BlockSeaPickle.WATERLOGGED) && Waterlogging.hasWaterSource(this.world, late),
                "a stored pickle's first update moves its flag into real water");
    }

    private void seaPickleBoneMeal(Check c) throws Exception {
        Block pickle = block("sea_pickle");
        // No coral blocks in this game: bone meal does nothing, wet or dry.
        BlockPos pos = waterCup();
        this.world.setBlockState(pos, pickle.getDefaultState(), 3);
        c.ok(!ItemDye.applyBonemeal(new ItemStack(Items.DYE, 1, EnumDyeColor.WHITE.getDyeDamage()), this.world, pos, this.player, null),
                "bone meal does nothing on a pickle over stone");

        // Stand prismarine in for a coral block to watch current Java's spread.
        Field coralField = BlockSeaPickle.class.getDeclaredField("CORAL_BLOCKS");
        coralField.setAccessible(true);
        ResourceLocation[] coral = (ResourceLocation[]) coralField.get(null);
        ResourceLocation was = coral[0];
        coral[0] = Blocks.PRISMARINE.getRegistryName();
        try {
            BlockPos center = new BlockPos(this.nextX + 4, Y, Z0);
            this.nextX += 12;
            for (int dx = -3; dx <= 3; dx++) {
                for (int dz = -3; dz <= 3; dz++) {
                    for (int dy = 0; dy <= 2; dy++) {
                        this.world.setBlockState(center.add(dx, dy, dz), Blocks.AIR.getDefaultState(), 2 | Waterlogging.REMOVE_WATER);
                    }
                    boolean edge = Math.abs(dx) == 3 || Math.abs(dz) == 3;
                    this.world.setBlockState(center.add(dx, -1, dz), Blocks.PRISMARINE.getDefaultState(), 2);
                    this.world.setBlockState(center.add(dx, 0, dz), edge ? Blocks.STONE.getDefaultState() : Blocks.WATER.getDefaultState(), 2);
                }
            }
            this.world.setBlockState(center, pickle.getDefaultState(), 3);
            c.ok(Waterlogging.hasWaterSource(this.world, center), "the pickle on coral is waterlogged");
            boolean used = false;
            for (int i = 0; i < 6; i++) {
                used |= ItemDye.applyBonemeal(new ItemStack(Items.DYE, 1, EnumDyeColor.WHITE.getDyeDamage()), this.world, center, this.player, null);
            }
            c.ok(used, "bone meal works on a waterlogged pickle on coral");
            c.eq(4, this.world.getBlockState(center).getValue(BlockSeaPickle.PICKLES), "the pickle becomes four");
            int spread = 0;
            boolean allWet = true;
            boolean inDiamond = true;
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    BlockPos at = center.add(dx, 0, dz);
                    if (at.equals(center) || this.world.getBlockState(at).getBlock() != pickle) {
                        continue;
                    }
                    spread++;
                    allWet &= Waterlogging.hasWaterSource(this.world, at);
                    inDiamond &= Math.abs(dx) + Math.abs(dz) <= 2;
                }
            }
            c.ok(spread > 0, "pickles spread across the coral: " + spread);
            c.ok(allWet, "the spread pickles are waterlogged");
            c.ok(inDiamond, "the spread stays in the five-wide diamond");

            // Dry on coral: nothing.
            BlockPos dry = center.add(0, 0, 2);
            Waterlogging.placeDry(this.world, dry, pickle.getDefaultState(), 3);
            c.ok(!ItemDye.applyBonemeal(new ItemStack(Items.DYE, 1, EnumDyeColor.WHITE.getDyeDamage()), this.world, dry, this.player, null),
                    "bone meal does nothing on a dry pickle on coral");
        } finally {
            coral[0] = was;
        }
        Item unused = Item.getItemFromBlock(pickle);
        c.ok(unused != null, "the sea pickle item exists");
    }
}
