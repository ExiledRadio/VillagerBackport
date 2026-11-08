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
import com.exiledradio.villagerbackport.migrate.BlockSeaPickleKept;
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
 * (current Java's list), and lanterns, scaffolding and campfires in and out of water. Sea pickles are
 * Update Aquatic's now; the ones older worlds hold are {@link PickleMoveSelfTest}'s.
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
                || b instanceof BlockScaffolding
                // A 2.3.0 to 2.5.0 sea pickle kept for Update Aquatic, which owns them now; it keeps its water.
                || b instanceof BlockSeaPickleKept;
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
        for (String name : new String[] {"lantern", "campfire", "scaffolding"}) {
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
}
