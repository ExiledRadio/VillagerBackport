package com.exiledradio.villagerbackport.woodset;

import com.exiledradio.villagerbackport.ModConfig;
import com.exiledradio.villagerbackport.ModCreativeTab;
import com.exiledradio.villagerbackport.VillagerBackport;
import com.mojang.authlib.GameProfile;

import net.minecraft.block.Block;
import net.minecraft.block.BlockButton;
import net.minecraft.block.BlockLog;
import net.minecraft.block.BlockNewLog;
import net.minecraft.block.BlockOldLog;
import net.minecraft.block.BlockPlanks;
import net.minecraft.block.BlockPressurePlate;
import net.minecraft.block.BlockTrapDoor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.NetworkManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerInteractionManager;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.oredict.OreDictionary;

import java.io.File;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

/**
 * Headless test of the village blocks, kept outside the source tree so it never ships.
 *
 * <p>To run: copy into src/main/java/com/exiledradio/villagerbackport/woodset/, create an empty file
 * eclipse/woodset-selftest, and runServer. The first server tick runs every check, writes every
 * block's valid states to eclipse/woodset-states.txt (for tools/woodset/check_blockstates.py), logs
 * "SELFTEST DONE pass=N fail=M [...]" and stops the server. Remove the copy before building a
 * release.
 */
@Mod.EventBusSubscriber(modid = VillagerBackport.MOD_ID)
public final class WoodsetSelfTest {

    private static boolean ran;
    private static int pass;
    private static final List<String> FAIL = new ArrayList<String>();

    private static void check(boolean ok, String what) {
        if (ok) {
            pass++;
        } else {
            FAIL.add(what);
            VillagerBackport.LOGGER.error("SELFTEST FAIL: {}", what);
        }
    }

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent event) {
        if (ran || event.phase != TickEvent.Phase.END || !new File("woodset-selftest").exists()) {
            return;
        }
        ran = true;
        try {
            run(FMLCommonHandler.instance().getMinecraftServerInstance());
        } catch (Throwable t) {
            FAIL.add("exception " + t);
            VillagerBackport.LOGGER.error("SELFTEST exception", t);
        }
        VillagerBackport.LOGGER.info("SELFTEST DONE pass={} fail={} {}", pass, FAIL.size(), FAIL);
        FMLCommonHandler.instance().getMinecraftServerInstance().initiateShutdown();
    }

    private static Block block(String name) {
        Block b = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(VillagerBackport.MOD_ID, name));
        return b == Blocks.AIR ? null : b;
    }

    private static Item item(String name) {
        return ForgeRegistries.ITEMS.getValue(new ResourceLocation(VillagerBackport.MOD_ID, name));
    }

    /** Vanilla's DefaultStateMapper variant string: properties sorted by name, "normal" when none. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    static String variant(IBlockState state) {
        Map<String, String> sorted = new TreeMap<String, String>();
        for (Map.Entry<IProperty<?>, Comparable<?>> e : state.getProperties().entrySet()) {
            IProperty p = e.getKey();
            sorted.put(p.getName(), p.getName((Comparable) e.getValue()));
        }
        if (sorted.isEmpty()) {
            return "normal";
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : sorted.entrySet()) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(e.getKey()).append('=').append(e.getValue());
        }
        return sb.toString();
    }

    private static void run(MinecraftServer server) throws Exception {
        WorldServer world = server.getWorld(0);
        EntityPlayerMP player = realPlayer(server, world);

        // 1. registration -------------------------------------------------------------------
        List<String> names = new ArrayList<String>();
        for (Wood w : Wood.values()) {
            names.add("stripped_" + w.name + "_log");
            names.add("stripped_" + w.name + "_wood");
        }
        for (Wood w : Wood.values()) {
            if (w.hasOwnRedstone()) {
                names.add(w.name + "_trapdoor");
                names.add(w.name + "_button");
                names.add(w.name + "_pressure_plate");
            }
        }
        names.add("blue_ice");
        names.add("sea_pickle");
        for (String n : names) {
            Block b = block(n);
            check(b != null, "block " + n);
            check(item(n) instanceof ItemBlock && Item.getItemFromBlock(b) == item(n), "item " + n);
            check(b != null && b.getCreativeTab() == ModCreativeTab.TAB, "creative tab " + n);
        }
        check(names.size() == 29 && WoodsetContent.blocks().size() == 29, "counts " + names.size() + "/" + WoodsetContent.blocks().size());

        // 2. every valid state, for the blockstate check -----------------------------------------
        PrintWriter out = new PrintWriter(new File("woodset-states.txt"), "UTF-8");
        int states = 0;
        for (Block b : WoodsetContent.blocks()) {
            for (IBlockState s : b.getBlockState().getValidStates()) {
                out.println(b.getRegistryName() + "\t" + variant(s));
                states++;
            }
            // meta round trip: every state survives a save
            for (IBlockState s : b.getBlockState().getValidStates()) {
                check(b.getStateFromMeta(b.getMetaFromState(s)) == s, "meta round trip " + s);
            }
        }
        out.close();
        VillagerBackport.LOGGER.info("SELFTEST wrote {} states", states);

        // 3. crafting ---------------------------------------------------------------------------
        for (Wood w : Wood.values()) {
            ItemStack log = new ItemStack(block("stripped_" + w.name + "_log"));
            ItemStack bark = new ItemStack(block("stripped_" + w.name + "_wood"));
            ItemStack planks4 = new ItemStack(Blocks.PLANKS, 4, w.planksMeta());
            expect(world, grid(1, 1, log), planks4, w.name + " planks from stripped log");
            expect(world, grid(1, 1, bark), planks4, w.name + " planks from stripped wood");
            expect(world, grid(2, 2, log, log, log, log), new ItemStack(bark.getItem(), 3), w.name + " stripped wood");
            ItemStack p = new ItemStack(Blocks.PLANKS, 1, w.planksMeta());
            if (w.hasOwnRedstone()) {
                expect(world, grid(3, 2, p, p, p, p, p, p), new ItemStack(block(w.name + "_trapdoor"), 2), w.name + " trapdoor");
                expect(world, grid(1, 1, p), new ItemStack(block(w.name + "_button")), w.name + " button");
                expect(world, grid(2, 1, p, p), new ItemStack(block(w.name + "_pressure_plate")), w.name + " pressure plate");
            } else {
                expect(world, grid(3, 2, p, p, p, p, p, p), new ItemStack(Blocks.TRAPDOOR, 2), "oak trapdoor");
                expect(world, grid(1, 1, p), new ItemStack(Blocks.WOODEN_BUTTON), "oak button");
                expect(world, grid(2, 1, p, p), new ItemStack(Blocks.WOODEN_PRESSURE_PLATE), "oak pressure plate");
            }
        }
        // vanilla's recipes still exist under their names, only narrowed
        for (String r : new String[] {"trapdoor", "wooden_button", "wooden_pressure_plate"}) {
            check(ForgeRegistries.RECIPES.getValue(new ResourceLocation("minecraft", r)) != null, "vanilla recipe kept " + r);
        }
        ItemStack pi = new ItemStack(Blocks.PACKED_ICE);
        expect(world, grid(3, 3, pi, pi, pi, pi, pi, pi, pi, pi, pi), new ItemStack(block("blue_ice")), "blue ice");

        // 4. smelting, fuel, fire, ore dictionary -----------------------------------------------
        for (Wood w : Wood.values()) {
            for (String kind : new String[] {"_log", "_wood"}) {
                Block b = block("stripped_" + w.name + kind);
                check(same(FurnaceRecipes.instance().getSmeltingResult(new ItemStack(b)), new ItemStack(Items.COAL, 1, 1)), "charcoal " + b);
                check(TileEntityFurnace.getItemBurnTime(new ItemStack(b)) == 300, "fuel " + b);
                check(Blocks.FIRE.getFlammability(b) == 5 && Blocks.FIRE.getEncouragement(b) == 5, "flammable " + b);
                check(containsItem(OreDictionary.getOres("logWood"), new ItemStack(b)), "logWood " + b);
            }
            if (w.hasOwnRedstone()) {
                check(TileEntityFurnace.getItemBurnTime(new ItemStack(block(w.name + "_button"))) == 100, "fuel button " + w.name);
                check(TileEntityFurnace.getItemBurnTime(new ItemStack(block(w.name + "_trapdoor"))) == 300, "fuel trapdoor " + w.name);
                check(TileEntityFurnace.getItemBurnTime(new ItemStack(block(w.name + "_pressure_plate"))) == 300, "fuel plate " + w.name);
            }
        }
        check(same(FurnaceRecipes.instance().getSmeltingResult(new ItemStack(block("sea_pickle"))), new ItemStack(Items.DYE, 1, 10)), "pickle -> lime dye");

        BlockPos origin = new BlockPos(world.getSpawnPoint().getX(), 200, world.getSpawnPoint().getZ());

        // 5. axe stripping ----------------------------------------------------------------------
        player.interactionManager.setGameType(GameType.SURVIVAL);
        int i = 0;
        for (Block logBlock : new Block[] {Blocks.LOG, Blocks.LOG2}) {
            for (IBlockState s : logBlock.getBlockState().getValidStates()) {
                BlockPlanks.EnumType type = logBlock == Blocks.LOG ? s.getValue(BlockOldLog.VARIANT) : s.getValue(BlockNewLog.VARIANT);
                Wood w = Wood.of(type);
                BlockLog.EnumAxis axis = s.getValue(BlockLog.LOG_AXIS);
                BlockPos at = origin.add(i++ * 2, 0, 0);
                world.setBlockState(at, s);
                ItemStack axe = new ItemStack(Items.IRON_AXE);
                player.setHeldItem(EnumHand.MAIN_HAND, axe);
                EnumActionResult r = player.interactionManager.processRightClickBlock(player, world, axe, EnumHand.MAIN_HAND, at,
                        EnumFacing.UP, 0.5F, 1.0F, 0.5F);
                IBlockState got = world.getBlockState(at);
                Block want = block("stripped_" + w.name + (axis == BlockLog.EnumAxis.NONE ? "_wood" : "_log"));
                BlockLog.EnumAxis wantAxis = axis == BlockLog.EnumAxis.NONE ? BlockLog.EnumAxis.Y : axis;
                check(r == EnumActionResult.SUCCESS, "strip result " + s + " " + r);
                check(got.getBlock() == want && got.getValue(BlockLog.LOG_AXIS) == wantAxis, "strip " + s + " -> " + got);
                check(axe.getItemDamage() == 1, "axe damage " + axe.getItemDamage());
                // a second click does nothing more
                processAgain(player, world, axe, at);
                check(world.getBlockState(at) == got && axe.getItemDamage() == 1, "strip twice " + got);
            }
        }
        check(i == 24, "strip cases " + i);
        BlockPos at = origin.add(0, 0, 4);
        IBlockState spruce = Blocks.LOG.getDefaultState().withProperty(BlockOldLog.VARIANT, BlockPlanks.EnumType.SPRUCE)
                .withProperty(BlockLog.LOG_AXIS, BlockLog.EnumAxis.X);
        world.setBlockState(at, spruce);
        ItemStack stick = new ItemStack(Items.STICK);
        player.setHeldItem(EnumHand.MAIN_HAND, stick);
        processAgain(player, world, stick, at);
        check(world.getBlockState(at) == spruce, "stick does not strip");
        ItemStack gold = new ItemStack(Items.GOLDEN_AXE);
        player.setHeldItem(EnumHand.MAIN_HAND, gold);
        PlayerInteractEvent.RightClickBlock pre = new PlayerInteractEvent.RightClickBlock(player, EnumHand.MAIN_HAND, at, EnumFacing.UP,
                new Vec3d(at));
        pre.setCanceled(true);
        MinecraftForge.EVENT_BUS.post(pre);
        check(world.getBlockState(at) == spruce && gold.getItemDamage() == 0, "already-cancelled click left alone");
        ModConfig.villageBlocks.axeStripping = false;
        processAgain(player, world, gold, at);
        check(world.getBlockState(at) == spruce, "axeStripping=false");
        ModConfig.villageBlocks.axeStripping = true;
        processAgain(player, world, gold, at);
        check(world.getBlockState(at).getBlock() == block("stripped_spruce_log")
                && world.getBlockState(at).getValue(BlockLog.LOG_AXIS) == BlockLog.EnumAxis.X, "golden axe strips");
        Item toolAxe = new Item() {
            @Override
            public Set<String> getToolClasses(ItemStack stack) {
                return Collections.singleton("axe");
            }
        };
        check(AxeStripping.isAxe(new ItemStack(toolAxe)) && !AxeStripping.isAxe(new ItemStack(Items.IRON_PICKAXE)), "tool class axe");
        check(AxeStripping.stripped(Blocks.PLANKS.getDefaultState()) == null, "planks not strippable");

        // 6. logs place with an axis, burn, keep leaves alive --------------------------------------
        at = origin.add(0, 0, 8);
        world.setBlockState(at, Blocks.STONE.getDefaultState());
        Block slog = block("stripped_birch_log");
        ItemStack held = new ItemStack(slog);
        player.setHeldItem(EnumHand.MAIN_HAND, held);
        held.getItem().onItemUse(player, world, at, EnumHand.MAIN_HAND, EnumFacing.EAST, 1.0F, 0.5F, 0.5F);
        IBlockState placed = world.getBlockState(at.east());
        check(placed.getBlock() == slog && placed.getValue(BlockLog.LOG_AXIS) == BlockLog.EnumAxis.X, "log placed on east face " + placed);
        check(slog.isWood(world, at.east()) && slog.canSustainLeaves(placed, world, at.east()), "log is wood");
        check("axe".equals(slog.getHarvestTool(placed)), "log harvest tool");
        check(placed.withRotation(Rotation.CLOCKWISE_90).getValue(BlockLog.LOG_AXIS) == BlockLog.EnumAxis.Z, "log rotates");
        List<ItemStack> drops = slog.getDrops(world, at.east(), placed, 0);
        check(drops.size() == 1 && drops.get(0).getItem() == Item.getItemFromBlock(slog) && drops.get(0).getMetadata() == 0, "log drops " + drops);

        // 7. trapdoors, buttons and plates behave as the oak ones ----------------------------------
        for (Wood w : Wood.values()) {
            if (!w.hasOwnRedstone()) {
                continue;
            }
            int z = 12 + w.ordinal() * 4;
            compareTrapdoor(world, player, origin.add(0, 0, z), block(w.name + "_trapdoor"), w.name);
            compareTrapdoor(world, player, origin.add(20, 0, z), Blocks.TRAPDOOR, "oak");
            compareButton(world, player, origin.add(40, 0, z), block(w.name + "_button"), w.name);
            comparePlate(world, origin.add(60, 0, z), block(w.name + "_pressure_plate"), w.name);
            check(block(w.name + "_button").tickRate(world) == Blocks.WOODEN_BUTTON.tickRate(world), "button tick rate " + w.name);
        }
        comparePlate(world, origin.add(80, 0, 12), Blocks.WOODEN_PRESSURE_PLATE, "oak");

        // 8. blue ice ---------------------------------------------------------------------------
        Block blue = block("blue_ice");
        at = origin.add(0, 0, 40);
        world.setBlockState(at, blue.getDefaultState());
        check(Math.abs(blue.getSlipperiness(blue.getDefaultState(), world, at, null) - 0.989F) < 1e-6, "blue ice slipperiness");
        check(blue.getDrops(world, at, blue.getDefaultState(), 0).isEmpty(), "blue ice drops nothing");
        check(blue.canSilkHarvest(world, at, blue.getDefaultState(), player), "blue ice silk touch");
        check(!blue.getTickRandomly() && blue.getDefaultState().getMaterial() == Material.PACKED_ICE, "blue ice never melts");
        check("pickaxe".equals(blue.getHarvestTool(blue.getDefaultState())), "blue ice pickaxe");
        check(Math.abs(blue.getBlockHardness(blue.getDefaultState(), world, at) - 2.8F) < 1e-6, "blue ice hardness");

        // 9. sea pickles ------------------------------------------------------------------------
        Block pickle = block("sea_pickle");
        Item pickleItem = Item.getItemFromBlock(pickle);
        at = origin.add(0, 0, 50);
        world.setBlockState(at, Blocks.STONE.getDefaultState());
        ItemStack pickles = new ItemStack(pickleItem, 8);
        player.setHeldItem(EnumHand.MAIN_HAND, pickles);
        pickleItem.onItemUse(player, world, at, EnumHand.MAIN_HAND, EnumFacing.UP, 0.5F, 1.0F, 0.5F);
        BlockPos p = at.up();
        IBlockState ps = world.getBlockState(p);
        check(ps.getBlock() == pickle && ps.getValue(BlockSeaPickle.PICKLES) == 1 && !ps.getValue(BlockSeaPickle.WATERLOGGED), "pickle placed dry " + ps);
        check(ps.getLightValue(world, p) == 0, "dry pickle dark");
        check(ps.getCollisionBoundingBox(world, p) == null, "pickle no collision");
        for (int n = 2; n <= 5; n++) {
            boolean added = pickle.onBlockActivated(world, p, world.getBlockState(p), player, EnumHand.MAIN_HAND, EnumFacing.UP, 0.5F, 0.5F, 0.5F);
            check(added == (n <= 4), "add pickle " + n + " " + added);
        }
        check(world.getBlockState(p).getValue(BlockSeaPickle.PICKLES) == 4 && pickles.getCount() == 4, "four pickles, " + pickles.getCount() + " left");
        world.setBlockState(p.north(), Blocks.WATER.getDefaultState(), 3);
        ps = world.getBlockState(p);
        check(ps.getValue(BlockSeaPickle.WATERLOGGED) && ps.getLightValue(world, p) == 15, "water beside lights " + ps + " " + ps.getLightValue(world, p));
        world.setBlockToAir(p.north());
        check(!world.getBlockState(p).getValue(BlockSeaPickle.WATERLOGGED), "drained goes dark");
        for (int n = 1; n <= 4; n++) {
            IBlockState wet = pickle.getDefaultState().withProperty(BlockSeaPickle.PICKLES, n).withProperty(BlockSeaPickle.WATERLOGGED, true);
            check(wet.getLightValue() == 3 + 3 * n, "light " + n);
            AxisAlignedBB box = wet.getBoundingBox(world, p);
            check(box.maxY <= 7 / 16.0 + 1e-9, "box " + n);
        }
        // remove the support: four pickles pop off
        world.setBlockToAir(at);
        check(world.isAirBlock(p), "pickle pops");
        check(countDrops(world, p, pickleItem) == 4, "pickle drops 4");
        check(!pickle.canPlaceBlockAt(world, p), "no pickle on air");
        // on the floor of a pool
        BlockPos floor = origin.add(10, 0, 50);
        world.setBlockState(floor, Blocks.STONE.getDefaultState());
        for (EnumFacing side : new EnumFacing[] {EnumFacing.UP, EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.EAST, EnumFacing.WEST}) {
            world.setBlockState(floor.up().offset(side), Blocks.WATER.getDefaultState(), 2);
        }
        world.setBlockState(floor.up(), Blocks.WATER.getDefaultState(), 2);
        player.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(pickleItem, 1));
        pickleItem.onItemUse(player, world, floor, EnumHand.MAIN_HAND, EnumFacing.UP, 0.5F, 1.0F, 0.5F);
        ps = world.getBlockState(floor.up());
        check(ps.getBlock() == pickle && ps.getValue(BlockSeaPickle.WATERLOGGED) && ps.getLightValue(world, floor.up()) == 6, "pickle under water " + ps);
        // a lone pickle replacing the only water
        BlockPos lone = origin.add(20, 0, 50);
        world.setBlockState(lone, Blocks.STONE.getDefaultState());
        world.setBlockState(lone.up(), Blocks.WATER.getDefaultState(), 2);
        player.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(pickleItem, 1));
        pickleItem.onItemUse(player, world, lone, EnumHand.MAIN_HAND, EnumFacing.UP, 0.5F, 1.0F, 0.5F);
        check(world.getBlockState(lone.up()).getValue(BlockSeaPickle.WATERLOGGED), "pickle replacing a source is wet");

        // 10. villages --------------------------------------------------------------------------
        villages(world, origin);

        // 11. Future MC pairs (Future MC is not installed here) ----------------------------------
        check(WoodsetContent.futureMcPairs().size() == 18, "future mc pairs " + WoodsetContent.futureMcPairs().size());
        for (Block b : WoodsetContent.blocks()) {
            check(!com.exiledradio.villagerbackport.compat.FutureMc.isHidden(b), "not hidden " + b);
        }
        // the hidden-items guard never names a minecraft item
        for (Item it : com.exiledradio.villagerbackport.compat.FutureMc.hiddenItems()) {
            check(!"minecraft".equals(it.getRegistryName().getNamespace()), "hidden vanilla item " + it);
        }

        // 12. a real village ---------------------------------------------------------------------
        realVillage(world);
    }

    private static void processAgain(EntityPlayerMP player, WorldServer world, ItemStack stack, BlockPos at) {
        player.interactionManager.processRightClickBlock(player, world, stack, EnumHand.MAIN_HAND, at, EnumFacing.UP, 0.5F, 1.0F, 0.5F);
    }

    private static void compareTrapdoor(WorldServer world, EntityPlayerMP player, BlockPos at, Block trapdoor, String what) {
        world.setBlockState(at, Blocks.STONE.getDefaultState());
        ItemStack held = new ItemStack(trapdoor);
        player.setHeldItem(EnumHand.MAIN_HAND, held);
        held.getItem().onItemUse(player, world, at, EnumHand.MAIN_HAND, EnumFacing.SOUTH, 0.5F, 0.75F, 1.0F);
        BlockPos t = at.south();
        IBlockState s = world.getBlockState(t);
        check(s.getBlock() == trapdoor && s.getValue(BlockTrapDoor.FACING) == EnumFacing.SOUTH
                && s.getValue(BlockTrapDoor.HALF) == BlockTrapDoor.DoorHalf.TOP && !s.getValue(BlockTrapDoor.OPEN), what + " trapdoor placed " + s);
        trapdoor.onBlockActivated(world, t, s, player, EnumHand.MAIN_HAND, EnumFacing.UP, 0.5F, 0.5F, 0.5F);
        check(world.getBlockState(t).getValue(BlockTrapDoor.OPEN), what + " trapdoor opens by hand");
        trapdoor.onBlockActivated(world, t, world.getBlockState(t), player, EnumHand.MAIN_HAND, EnumFacing.UP, 0.5F, 0.5F, 0.5F);
        check(!world.getBlockState(t).getValue(BlockTrapDoor.OPEN), what + " trapdoor closes by hand");
        world.setBlockState(t.south(), Blocks.REDSTONE_BLOCK.getDefaultState(), 3);
        check(world.getBlockState(t).getValue(BlockTrapDoor.OPEN), what + " trapdoor opens by redstone");
        world.setBlockToAir(t.south());
        check(!world.getBlockState(t).getValue(BlockTrapDoor.OPEN), what + " trapdoor closes without redstone");
        check(Math.abs(trapdoor.getBlockHardness(s, world, t) - 3.0F) < 1e-6, what + " trapdoor hardness");
    }

    private static void compareButton(WorldServer world, EntityPlayerMP player, BlockPos at, Block button, String what) {
        world.setBlockState(at, Blocks.STONE.getDefaultState());
        ItemStack held = new ItemStack(button);
        player.setHeldItem(EnumHand.MAIN_HAND, held);
        held.getItem().onItemUse(player, world, at, EnumHand.MAIN_HAND, EnumFacing.EAST, 1.0F, 0.5F, 0.5F);
        BlockPos b = at.east();
        IBlockState s = world.getBlockState(b);
        check(s.getBlock() == button && s.getValue(BlockButton.FACING) == EnumFacing.EAST && !s.getValue(BlockButton.POWERED), what + " button placed " + s);
        button.onBlockActivated(world, b, s, player, EnumHand.MAIN_HAND, EnumFacing.EAST, 0.5F, 0.5F, 0.5F);
        s = world.getBlockState(b);
        check(s.getValue(BlockButton.POWERED) && s.getWeakPower(world, b, EnumFacing.EAST) == 15
                && s.getStrongPower(world, b, EnumFacing.EAST) == 15 && world.isBlockPowered(at), what + " button powers " + s);
        // released by a scheduled tick, as the oak one is
        check(world.isUpdateScheduled(b, button), what + " button scheduled release");
    }

    private static void comparePlate(WorldServer world, BlockPos at, Block plate, String what) {
        world.setBlockState(at, Blocks.STONE.getDefaultState());
        world.setBlockState(at.up(), plate.getDefaultState());
        EntityItem item = new EntityItem(world, at.getX() + 0.5, at.getY() + 1.05, at.getZ() + 0.5, new ItemStack(Items.STICK));
        world.spawnEntity(item);
        plate.onEntityCollision(world, at.up(), world.getBlockState(at.up()), item);
        IBlockState s = world.getBlockState(at.up());
        check(s.getValue(BlockPressurePlate.POWERED) && world.isBlockPowered(at), what + " plate pressed by an item " + s);
        item.setDead();
    }

    private static int countDrops(WorldServer world, BlockPos at, Item want) {
        int n = 0;
        for (EntityItem e : world.getEntitiesWithinAABB(EntityItem.class, new AxisAlignedBB(at).grow(2))) {
            if (e.getItem().getItem() == want) {
                n += e.getItem().getCount();
            }
            e.setDead();
        }
        return n;
    }

    // ------------------------------------------------------------------------------------------
    // Villages
    // ------------------------------------------------------------------------------------------

    /** Every palette entry of every structure file that names one of these blocks resolves to it. */
    private static void villages(WorldServer world, BlockPos origin) throws Exception {
        Class<?> bb = Class.forName("com.exiledradio.villagerbackport.village.modern.BackportBlocks");
        Method resolve = bb.getDeclaredMethod("resolve", String.class, NBTTagCompound.class, NBTTagCompound.class);
        resolve.setAccessible(true);
        NBTTagCompound none = new NBTTagCompound();
        check(((IBlockState) resolve.invoke(null, "minecraft:blue_ice", none, none)).getBlock() == block("blue_ice"), "resolve blue ice");

        Class<?> tc = Class.forName("com.exiledradio.villagerbackport.village.modern.ModernTemplate");
        Method get = tc.getDeclaredMethod("get", String.class);
        Field paletteF = tc.getDeclaredField("palette");
        paletteF.setAccessible(true);
        Class<?> pc = Class.forName("com.exiledradio.villagerbackport.village.modern.ModernTemplate$PaletteEntry");
        Field srcF = pc.getDeclaredField("source");
        Field srcPropsF = pc.getDeclaredField("sourceProps");
        Method state = pc.getDeclaredMethod("state");
        srcF.setAccessible(true);
        srcPropsF.setAccessible(true);
        state.setAccessible(true);

        // The source tree, from the run directory: in a dev run the resources are inside a jar.
        File root = new File("../src/main/resources/assets/villagerbackport/structures");
        List<String> locations = new ArrayList<String>();
        collect(root, "", locations);
        check(locations.size() == 492, "structure files " + locations.size());
        Map<String, Integer> seen = new TreeMap<String, Integer>();
        int files = 0;
        for (String loc : locations) {
            Object template = get.invoke(null, loc);
            boolean any = false;
            for (Object entry : (Object[]) paletteF.get(template)) {
                String src = (String) srcF.get(entry);
                String key = src.startsWith("minecraft:") ? src.substring(10) : src;
                if (block(key) == null || !WoodsetContent.blocks().contains(block(key))) {
                    continue;
                }
                IBlockState s = (IBlockState) state.invoke(entry);
                NBTTagCompound sp = (NBTTagCompound) srcPropsF.get(entry);
                String what = loc + " " + src + sp;
                check(s != null && s.getBlock() == block(key), what + " -> " + s);
                if (s == null) {
                    continue;
                }
                for (String prop : new String[] {"axis", "facing", "half", "open", "powered", "pickles"}) {
                    if (!sp.hasKey(prop)) {
                        continue;
                    }
                    String v = valueOf(s, prop);
                    if (v == null) {
                        // 1.12.2's trapdoor has no powered: it reads its neighbors instead.
                        check(!(s.getBlock() instanceof BlockLog), what + " log lacks " + prop);
                        continue;
                    }
                    // buttons take the converted 1.12.2 facing; for a wall button it equals 1.14's
                    check(sp.getString(prop).equals(v), what + " " + prop + "=" + v);
                }
                Integer c = seen.get(key);
                seen.put(key, c == null ? 1 : c + 1);
                any = true;
            }
            if (any) {
                files++;
            }
        }
        VillagerBackport.LOGGER.info("SELFTEST village palettes using these blocks: {} files, {}", files, seen);
        for (String need : new String[] {"stripped_spruce_log", "stripped_oak_log", "stripped_acacia_log", "stripped_spruce_wood",
                "stripped_oak_wood", "spruce_trapdoor", "jungle_trapdoor", "jungle_button", "spruce_pressure_plate",
                "acacia_pressure_plate", "blue_ice", "sea_pickle"}) {
            check(seen.containsKey(need), "villages use " + need);
        }
    }

    private static void collect(File dir, String prefix, List<String> out) {
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        for (File f : files) {
            if (f.isDirectory()) {
                collect(f, prefix + f.getName() + "/", out);
            } else if (f.getName().endsWith(".nbt")) {
                out.add(prefix + f.getName().substring(0, f.getName().length() - 4));
            }
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static String valueOf(IBlockState s, String name) {
        for (IProperty p : s.getPropertyKeys()) {
            if (p.getName().equals(name)) {
                return p.getName((Comparable) s.getValue(p));
            }
        }
        return null;
    }

    /** Generates the nearest taiga and snowy village and counts these blocks in the world. */
    private static void realVillage(WorldServer world) {
        com.exiledradio.villagerbackport.village.modern.MapGenModernVillage gen =
                com.exiledradio.villagerbackport.village.modern.MapGenModernVillage.of(world);
        check(gen != null, "modern villages on");
        if (gen == null) {
            return;
        }
        for (com.exiledradio.villagerbackport.village.modern.ModernVillageStart.Type type
                : new com.exiledradio.villagerbackport.village.modern.ModernVillageStart.Type[] {
                com.exiledradio.villagerbackport.village.modern.ModernVillageStart.Type.TAIGA,
                com.exiledradio.villagerbackport.village.modern.ModernVillageStart.Type.SNOWY}) {
            BlockPos center = gen.findNearest(world, world.getSpawnPoint(), type, 60);
            check(center != null, "found " + type);
            if (center == null) {
                continue;
            }
            int cx = center.getX() >> 4;
            int cz = center.getZ() >> 4;
            long t0 = System.currentTimeMillis();
            for (int dx = -7; dx <= 7; dx++) {
                for (int dz = -7; dz <= 7; dz++) {
                    world.getChunkProvider().provideChunk(cx + dx, cz + dz);
                }
            }
            Map<String, Integer> counts = new TreeMap<String, Integer>();
            BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
            for (int x = (cx - 6) << 4; x < (cx + 7) << 4; x++) {
                for (int z = (cz - 6) << 4; z < (cz + 7) << 4; z++) {
                    for (int y = 40; y < 140; y++) {
                        m.setPos(x, y, z);
                        Block b = world.getBlockState(m).getBlock();
                        if (b.getRegistryName() != null && VillagerBackport.MOD_ID.equals(b.getRegistryName().getNamespace())
                                && WoodsetContent.blocks().contains(b)) {
                            String k = b.getRegistryName().getPath();
                            Integer c = counts.get(k);
                            counts.put(k, c == null ? 1 : c + 1);
                        }
                    }
                }
            }
            VillagerBackport.LOGGER.info("SELFTEST real {} village at {} ({} ms): {}", type, center, System.currentTimeMillis() - t0, counts);
            check(!counts.isEmpty(), "real " + type + " village has village blocks");
        }
    }

    // ------------------------------------------------------------------------------------------
    // Helpers, as in BuildingSelfTest
    // ------------------------------------------------------------------------------------------

    private static EntityPlayerMP realPlayer(MinecraftServer server, WorldServer world) {
        EntityPlayerMP player = new EntityPlayerMP(server, world, new GameProfile(UUID.randomUUID(), "WoodTester"),
                new PlayerInteractionManager(world));
        player.connection = new NetHandlerPlayServer(server, new NetworkManager(EnumPacketDirection.SERVERBOUND), player);
        return player;
    }

    private static InventoryCrafting grid(int w, int h, ItemStack... stacks) {
        InventoryCrafting inv = new InventoryCrafting(new Container() {
            @Override
            public boolean canInteractWith(EntityPlayer playerIn) {
                return true;
            }
        }, 3, 3);
        for (int row = 0; row < h; row++) {
            for (int col = 0; col < w; col++) {
                inv.setInventorySlotContents(row * 3 + col, stacks[row * w + col].copy());
            }
        }
        return inv;
    }

    private static void expect(WorldServer world, InventoryCrafting inv, ItemStack want, String what) {
        ItemStack got = CraftingManager.findMatchingResult(inv, world);
        check(same(got, want) && got.getCount() == want.getCount(), what + " got " + got);
    }

    private static boolean same(ItemStack a, ItemStack b) {
        return !a.isEmpty() && a.getItem() == b.getItem() && a.getMetadata() == b.getMetadata();
    }

    private static boolean containsItem(List<ItemStack> list, ItemStack want) {
        for (ItemStack s : list) {
            if (s.getItem() == want.getItem()) {
                return true;
            }
        }
        return false;
    }
}
