package com.exiledradio.villagerbackport.building;

import com.exiledradio.villagerbackport.VillagerBackport;
import com.exiledradio.villagerbackport.block.CutterKind;
import com.exiledradio.villagerbackport.block.StonecutterRecipes;
import com.mojang.authlib.GameProfile;

import net.minecraft.block.Block;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.BlockWall;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.client.CPacketUpdateSign;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerInteractionManager;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.oredict.OreDictionary;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Headless test of the building blocks, kept outside the source tree so it never ships.
 *
 * <p>To run: copy into src/main/java/com/exiledradio/villagerbackport/building/, create an empty file
 * eclipse/building-selftest, delete the eclipse/buildtest world, and runServer. The first server tick
 * runs every check, logs "SELFTEST DONE pass=N fail=M [...]" and stops the server. Remove the copy
 * before building a release.
 */
@Mod.EventBusSubscriber(modid = VillagerBackport.MOD_ID)
public final class BuildingSelfTest {

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
        if (ran || event.phase != TickEvent.Phase.END || !new File("building-selftest").exists()) {
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

    private static void run(MinecraftServer server) throws Exception {

        WorldServer world = server.getWorld(0);

        // 1. registration
        int blocks = 0;
        int items = 0;
        List<String> names = new ArrayList<String>();
        for (BuildingTable.Full f : BuildingTable.FULL_BLOCKS) {
            names.add(f.name);
        }
        for (BuildingTable.Row r : BuildingTable.ROWS) {
            if (r.has(BuildingTable.Shape.SLAB)) {
                names.add(r.name + "_slab");
                check(block(r.name + "_double_slab") != null, "double " + r.name);
                blocks++;
            }
            if (r.has(BuildingTable.Shape.STAIRS)) {
                names.add(r.name + "_stairs");
            }
            if (r.has(BuildingTable.Shape.WALL)) {
                names.add(r.name + "_wall");
            }
        }
        for (String n : names) {
            check(block(n) != null, "block " + n);
            check(item(n) != null && Item.getItemFromBlock(block(n)) == item(n), "item " + n);
            blocks++;
            items++;
        }
        for (SignWood w : SignWood.values()) {
            check(block(w.name + "_sign") != null && block(w.name + "_wall_sign") != null, "sign blocks " + w.name);
            check(item(w.name + "_sign") instanceof ItemWoodSign, "sign item " + w.name);
            blocks += 2;
            items++;
        }
        VillagerBackport.LOGGER.info("SELFTEST registered blocks={} items={}", blocks, items);
        check(blocks == 72 && items == 51, "counts " + blocks + "/" + items);

        // 2. crafting
        EntityPlayerMP player = realPlayer(server, world);
        for (BuildingTable.Row r : BuildingTable.ROWS) {
            ItemStack base = baseStack(r);
            if (r.has(BuildingTable.Shape.SLAB)) {
                expect(world, grid(3, 1, base, base, base), new ItemStack(block(r.name + "_slab"), 6), r.name + " slab recipe");
            }
            if (r.has(BuildingTable.Shape.STAIRS)) {
                expect(world, grid(3, 3, base, ItemStack.EMPTY, ItemStack.EMPTY, base, base, ItemStack.EMPTY, base, base, base),
                        new ItemStack(block(r.name + "_stairs"), 4), r.name + " stairs recipe");
            }
            if (r.has(BuildingTable.Shape.WALL)) {
                expect(world, grid(3, 2, base, base, base, base, base, base), new ItemStack(block(r.name + "_wall"), 6), r.name + " wall recipe");
            }
        }
        ItemStack stick = new ItemStack(Items.STICK);
        for (SignWood w : SignWood.values()) {
            ItemStack p = new ItemStack(Blocks.PLANKS, 1, w.planksMeta);
            expect(world, grid(3, 3, p, p, p, p, p, p, ItemStack.EMPTY, stick, ItemStack.EMPTY),
                    new ItemStack(item(w.name + "_sign"), 3), w.name + " sign recipe");
        }
        ItemStack oak = new ItemStack(Blocks.PLANKS, 1, 0);
        expect(world, grid(3, 3, oak, oak, oak, oak, oak, oak, ItemStack.EMPTY, stick, ItemStack.EMPTY), new ItemStack(Items.SIGN, 3), "oak sign");
        ItemStack smooth = new ItemStack(block("smooth_stone"));
        expect(world, grid(3, 1, smooth, smooth, smooth), new ItemStack(Blocks.STONE_SLAB, 6, 0), "smooth stone slab");
        ItemStack ss = new ItemStack(Blocks.SANDSTONE, 1, 0);
        expect(world, grid(3, 1, ss, ss, ss), new ItemStack(Blocks.STONE_SLAB, 6, 1), "sandstone slab");
        ItemStack rs = new ItemStack(Blocks.RED_SANDSTONE, 1, 1);
        expect(world, grid(3, 1, rs, rs, rs), new ItemStack(Blocks.STONE_SLAB2, 6, 0), "chiseled red sandstone slab");

        ItemStack sb = new ItemStack(Blocks.STONEBRICK, 1, 0);
        expect(world, grid(3, 1, sb, sb, sb), new ItemStack(Blocks.STONE_SLAB, 6, 5), "stone brick slab");
        ItemStack nb = new ItemStack(Blocks.NETHER_BRICK);
        ItemStack nbi = new ItemStack(Items.NETHERBRICK);
        expect(world, grid(3, 2, nb, nbi, nb, nb, nbi, nb), new ItemStack(Blocks.NETHER_BRICK_FENCE, 6), "nether brick fence");
        ItemStack cut = new ItemStack(Blocks.SANDSTONE, 1, 2);
        expect(world, grid(3, 1, cut, cut, cut), new ItemStack(block("cut_sandstone_slab"), 6), "cut sandstone slab not vanilla");

        // 3. smelting
        check(same(FurnaceRecipes.instance().getSmeltingResult(new ItemStack(Blocks.STONE)), new ItemStack(block("smooth_stone"))), "smelt stone");
        check(same(FurnaceRecipes.instance().getSmeltingResult(new ItemStack(Blocks.SANDSTONE)), new ItemStack(block("smooth_sandstone"))), "smelt sandstone");
        check(same(FurnaceRecipes.instance().getSmeltingResult(new ItemStack(Blocks.RED_SANDSTONE)), new ItemStack(block("smooth_red_sandstone"))), "smelt red sandstone");
        check(same(FurnaceRecipes.instance().getSmeltingResult(new ItemStack(Blocks.QUARTZ_BLOCK)), new ItemStack(block("smooth_quartz"))), "smelt quartz");
        check(same(FurnaceRecipes.instance().getSmeltingResult(new ItemStack(Blocks.STONEBRICK)), new ItemStack(Blocks.STONEBRICK, 1, 2)), "smelt bricks");
        check(FurnaceRecipes.instance().getSmeltingResult(new ItemStack(Blocks.STONE, 1, 5)).isEmpty(), "andesite does not smelt");

        // 4. stonecutter: every new shape is reachable from its base
        StonecutterRecipes cutter = StonecutterRecipes.of(CutterKind.STONE);
        for (BuildingTable.Row r : BuildingTable.ROWS) {
            List<ItemStack> out = cutter.resultsFor(baseStack(r));
            if (r.has(BuildingTable.Shape.SLAB)) {
                check(contains(out, new ItemStack(block(r.name + "_slab"), 2)), "cut " + r.name + " slab " + out);
            }
            if (r.has(BuildingTable.Shape.STAIRS)) {
                check(contains(out, new ItemStack(block(r.name + "_stairs"), 1)), "cut " + r.name + " stairs");
            }
            if (r.has(BuildingTable.Shape.WALL)) {
                check(contains(out, new ItemStack(block(r.name + "_wall"), 1)), "cut " + r.name + " wall");
            }
        }
        List<ItemStack> fromAndesite = cutter.resultsFor(new ItemStack(Blocks.STONE, 1, 5));
        check(contains(fromAndesite, new ItemStack(block("polished_andesite_slab"), 2)), "chain andesite->polished slab " + fromAndesite);
        List<ItemStack> fromStone = cutter.resultsFor(new ItemStack(Blocks.STONE));
        check(contains(fromStone, new ItemStack(block("stone_brick_wall"), 1)), "chain stone->stone brick wall " + fromStone);
        check(contains(cutter.resultsFor(smooth), new ItemStack(Blocks.STONE_SLAB, 2, 0)), "cut smooth stone slab");
        VillagerBackport.LOGGER.info("SELFTEST cut stone -> {}", fromStone);

        // 5. ore dictionary
        check(OreDictionary.getOres("stoneSmooth").size() == 1, "ore stoneSmooth");
        check(OreDictionary.getOres("wall").size() == 12, "ore wall " + OreDictionary.getOres("wall").size());
        check(OreDictionary.getOres("slab").size() == 16, "ore slab");
        check(OreDictionary.getOres("stair").size() == 14, "ore stair");

        // 6. placing and breaking
        BlockPos origin = new BlockPos(world.getSpawnPoint().getX(), 200, world.getSpawnPoint().getZ());
        int x = 0;
        for (String n : names) {
            BlockPos at = origin.add(x, 0, 0);
            x += 2;
            world.setBlockState(at.down(), Blocks.STONE.getDefaultState());
            Block b = block(n);
            ItemStack held = new ItemStack(b);
            player.setHeldItem(EnumHand.MAIN_HAND, held.copy());
            player.setPosition(at.getX() + 0.5, at.getY() + 1, at.getZ() - 2);
            held.getItem().onItemUse(player, world, at.down(), EnumHand.MAIN_HAND, EnumFacing.UP, 0.5F, 1.0F, 0.5F);
            IBlockState placed = world.getBlockState(at);
            check(placed.getBlock() == b, "place " + n + " got " + placed);
            if (n.endsWith("_slab")) {
                // second slab makes a double
                player.setHeldItem(EnumHand.MAIN_HAND, held.copy());
                held.getItem().onItemUse(player, world, at, EnumHand.MAIN_HAND, EnumFacing.UP, 0.5F, 0.5F, 0.5F);
                Block dbl = block(n.replace("_slab", "_double_slab"));
                check(world.getBlockState(at).getBlock() == dbl, "double " + n + " got " + world.getBlockState(at));
                NonNullList<ItemStack> drops = NonNullList.create();
                dbl.getDrops(drops, world, at, world.getBlockState(at), 0);
                int count = 0;
                for (ItemStack d : drops) {
                    count += d.getItem() == held.getItem() ? d.getCount() : 100;
                }
                check(count == 2, n + " double drops " + drops);
            } else {
                NonNullList<ItemStack> drops = NonNullList.create();
                b.getDrops(drops, world, at, placed, 0);
                check(drops.size() == 1 && drops.get(0).getItem() == held.getItem(), n + " drops " + drops);
            }
            check(b.getHarvestTool(placed.getBlock().getDefaultState()) != null, n + " harvest tool");
            check(world.destroyBlock(at, true) && world.isAirBlock(at), "break " + n);
        }

        // walls join each other, vanilla walls and stone; stairs make corners
        BlockPos w = origin.add(0, 0, 6);
        world.setBlockState(w, block("andesite_wall").getDefaultState());
        world.setBlockState(w.north(), block("brick_wall").getDefaultState());
        world.setBlockState(w.south(), Blocks.COBBLESTONE_WALL.getDefaultState());
        IBlockState actual = world.getBlockState(w).getActualState(world, w);
        check(actual.getValue(BlockWall.NORTH) && actual.getValue(BlockWall.SOUTH) && !actual.getValue(BlockWall.EAST), "wall joins " + actual);
        BlockPos s = origin.add(0, 0, 12);
        world.setBlockState(s, block("granite_stairs").getDefaultState().withProperty(BlockStairs.FACING, EnumFacing.NORTH));
        world.setBlockState(s.north(), block("granite_stairs").getDefaultState().withProperty(BlockStairs.FACING, EnumFacing.EAST));
        IBlockState corner = world.getBlockState(s).getActualState(world, s);
        check(corner.getValue(BlockStairs.SHAPE) != BlockStairs.EnumShape.STRAIGHT, "stairs corner " + corner);

        // 7. signs
        for (SignWood wood : SignWood.values()) {
            BlockPos base = origin.add(wood.ordinal() * 3, 0, 20);
            world.setBlockState(base, Blocks.STONE.getDefaultState());
            ItemWoodSign signItem = (ItemWoodSign) item(wood.name + "_sign");
            player.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(signItem, 2));
            signItem.onItemUse(player, world, base, EnumHand.MAIN_HAND, EnumFacing.UP, 0.5F, 1.0F, 0.5F);
            TileEntity te = world.getTileEntity(base.up());
            check(world.getBlockState(base.up()).getBlock() == block(wood.name + "_sign"), "standing sign " + wood.name);
            check(te instanceof TileEntityWoodSign && te instanceof TileEntitySign, "sign tile " + te);
            check(player.getHeldItemMainhand().getCount() == 1, "sign used");
            if (te instanceof TileEntitySign) {
                TileEntitySign sign = (TileEntitySign) te;
                check(sign.getPlayer() == player && sign.getIsEditable(), "editor opened");
                net.minecraft.network.PacketBuffer buf = new net.minecraft.network.PacketBuffer(io.netty.buffer.Unpooled.buffer());
                buf.writeBlockPos(base.up());
                for (String line : new String[] {"Hello", wood.name, "", "x"}) {
                    buf.writeString(line);
                }
                CPacketUpdateSign packet = new CPacketUpdateSign();
                packet.readPacketData(buf);
                player.connection.processUpdateSign(packet);
                check("Hello".equals(sign.signText[0].getUnformattedText()), "sign text " + sign.signText[0]);
                NBTTagCompound tag = sign.writeToNBT(new NBTTagCompound());
                check("villagerbackport:wood_sign".equals(tag.getString("id")) && tag.getString("Text1").contains("Hello"), "sign nbt " + tag);
                check(sign.getUpdatePacket() != null, "sign update packet");
            }
            signItem.onItemUse(player, world, base, EnumHand.MAIN_HAND, EnumFacing.EAST, 1.0F, 0.5F, 0.5F);
            check(world.getBlockState(base.east()).getBlock() == block(wood.name + "_wall_sign"), "wall sign " + wood.name);
            check(world.getTileEntity(base.east()) instanceof TileEntityWoodSign, "wall sign tile");
            // take the support away: both pop off as items
            world.setBlockToAir(base);
            check(world.isAirBlock(base.up()) && world.isAirBlock(base.east()), "signs pop " + wood.name);
            List<net.minecraft.entity.item.EntityItem> dropped = world.getEntitiesWithinAABB(net.minecraft.entity.item.EntityItem.class,
                    new net.minecraft.util.math.AxisAlignedBB(base).grow(2));
            int signs = 0;
            for (net.minecraft.entity.item.EntityItem e : dropped) {
                if (e.getItem().getItem() == signItem) {
                    signs += e.getItem().getCount();
                }
                e.setDead();
            }
            check(signs == 2, "sign drops " + wood.name + " " + signs);
        }

        // 8. villages resolve to these blocks
        Class<?> bb = Class.forName("com.exiledradio.villagerbackport.village.modern.BackportBlocks");
        java.lang.reflect.Method resolve = bb.getDeclaredMethod("resolve", String.class, NBTTagCompound.class, NBTTagCompound.class);
        resolve.setAccessible(true);
        String[][] cases = {
                {"minecraft:diorite_wall", "diorite_wall"}, {"minecraft:granite_stairs", "granite_stairs"},
                {"minecraft:smooth_sandstone", "smooth_sandstone"}, {"minecraft:smooth_sandstone_stairs", "smooth_sandstone_stairs"},
                {"minecraft:smooth_stone", "smooth_stone"}, {"minecraft:mossy_cobblestone_slab", "mossy_cobblestone_slab"},
                {"minecraft:spruce_wall_sign", "spruce_wall_sign"}, {"minecraft:sandstone_wall", "sandstone_wall"},
                {"minecraft:end_stone_brick_wall", "end_stone_brick_wall"},
        };
        for (String[] c : cases) {
            NBTTagCompound srcProps = new NBTTagCompound();
            NBTTagCompound props = new NBTTagCompound();
            if (c[0].endsWith("_sign")) {
                srcProps.setString("facing", "east");
                props.setString("facing", "east");
            }
            if (c[0].endsWith("_slab")) {
                srcProps.setString("type", "top");
                props.setString("half", "top");
            }
            IBlockState st = (IBlockState) resolve.invoke(null, c[0], srcProps, props);
            check(st != null && st.getBlock() == block(c[1]), "village " + c[0] + " -> " + st);
        }
        NBTTagCompound dbl = new NBTTagCompound();
        dbl.setString("type", "double");
        IBlockState st = (IBlockState) resolve.invoke(null, "minecraft:diorite_slab", dbl, new NBTTagCompound());
        check(st != null && st.getBlock() == block("diorite_double_slab"), "village double slab " + st);
    }

    private static EntityPlayerMP realPlayer(MinecraftServer server, WorldServer world) {
        EntityPlayerMP player = new EntityPlayerMP(server, world, new GameProfile(UUID.randomUUID(), "BuildTester"),
                new PlayerInteractionManager(world));
        player.connection = new NetHandlerPlayServer(server, new NetworkManager(EnumPacketDirection.SERVERBOUND), player);
        return player;
    }

    private static ItemStack baseStack(BuildingTable.Row r) {
        Block b = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(r.base));
        return new ItemStack(b, 1, r.baseMeta);
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

    private static boolean contains(List<ItemStack> list, ItemStack want) {
        for (ItemStack s : list) {
            if (same(s, want) && s.getCount() == want.getCount()) {
                return true;
            }
        }
        return false;
    }
}
