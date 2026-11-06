package com.exiledradio.villagerbackport.migrate;

import com.exiledradio.villagerbackport.VillagerBackport;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * {@code /futuremc preview|convert [radius|loaded]}, {@code /futuremc check} and {@code /futuremc dump}:
 * turning Future MC's blocks and items in a world into this mod's, while Future MC is still installed.
 *
 * <ul>
 *   <li>{@code preview}: counts what {@code convert} would change, changing nothing.</li>
 *   <li>{@code convert}: every Future MC block that has a counterpart here, in the loaded chunks
 *       within the radius of you (64 by default) or in every loaded chunk, keeping each block's
 *       facing, axis, half and everything else it has, and its contents. Then the items: in every
 *       container, item frame and dropped item in that area, and in the inventory and ender chest of
 *       every player in it.</li>
 *   <li>{@code check}: tries every state Future MC can save, without touching the world, and lists any
 *       property that wouldn't carry across exactly.</li>
 *   <li>{@code dump}: writes out how Future MC saves each block, so worlds can still be converted
 *       after it's removed (see {@link FutureMcTable}).</li>
 * </ul>
 *
 * Blocks are swapped without updating their neighbors, so nothing next to them reacts - no falling
 * scaffolding, no dropped torches - and the contents are taken out first, so the old block can't
 * drop them as it goes.
 */
public final class FutureMcCommand extends CommandBase {

    private static final int DEFAULT_RADIUS = 64;
    private static final int MAX_RADIUS = 512;

    @Override
    public String getName() {
        return "futuremc";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/futuremc preview [radius|loaded] | convert [radius|loaded] | check | dump";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos target) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, "preview", "convert", "check", "dump");
        }
        if (args.length == 2 && ("preview".equals(args[0]) || "convert".equals(args[0]))) {
            return getListOfStringsMatchingLastWord(args, "loaded", "64", "128", "256");
        }
        return Collections.emptyList();
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 0) {
            throw new WrongUsageException(getUsage(sender));
        }
        switch (args[0]) {
            case "preview":
            case "convert":
                needFutureMc();
                run(sender, args, "convert".equals(args[0]));
                return;
            case "check":
                needFutureMc();
                check(sender);
                return;
            case "dump":
                needFutureMc();
                dump(server, sender);
                return;
            default:
                throw new WrongUsageException(getUsage(sender));
        }
    }

    private static void needFutureMc() throws CommandException {
        if (!net.minecraftforge.fml.common.Loader.isModLoaded(FutureMcTable.FUTURE_MC)) {
            throw new CommandException("Future MC isn't installed. Its blocks are converted automatically "
                    + "when a world that had them loads without it.");
        }
    }

    // ------------------------------------------------------------------------------------------
    // preview / convert
    // ------------------------------------------------------------------------------------------

    /** What a run changed, or would change. */
    public static final class Report {
        final Map<String, Integer> blocks = new TreeMap<String, Integer>();
        final Map<String, Integer> skipped = new TreeMap<String, Integer>();
        final Map<String, Integer> dropped = new TreeMap<String, Integer>();
        int contents;
        int items;
        int chunks;

        void add(Map<String, Integer> map, String key) {
            Integer n = map.get(key);
            map.put(key, n == null ? 1 : n + 1);
        }

        int total(Map<String, Integer> map) {
            int sum = 0;
            for (int n : map.values()) {
                sum += n;
            }
            return sum;
        }
    }

    private static void run(ICommandSender sender, String[] args, boolean apply) throws CommandException {
        World world = sender.getEntityWorld();
        BlockPos center = sender.getPosition();
        int radius = DEFAULT_RADIUS;
        boolean loaded = args.length > 1 && "loaded".equals(args[1]);
        if (args.length > 1 && !loaded) {
            radius = parseInt(args[1], 1, MAX_RADIUS);
        }

        List<Chunk> chunks = new ArrayList<Chunk>();
        if (world instanceof WorldServer) {
            for (Chunk chunk : ((WorldServer) world).getChunkProvider().getLoadedChunks()) {
                if (loaded || near(chunk, center, radius)) {
                    chunks.add(chunk);
                }
            }
        }

        Report report = new Report();
        report.chunks = chunks.size();
        for (Chunk chunk : chunks) {
            blocks(world, chunk, center, loaded ? -1 : radius, apply, report);
        }
        items(world, chunks, center, loaded ? -1 : radius, apply, report);

        String where = loaded ? "every loaded chunk (" + chunks.size() + ")" : "loaded chunks within " + radius + " blocks";
        int blocks = report.total(report.blocks);
        sender.sendMessage(new TextComponentString(TextFormatting.GOLD + (apply ? "Converted " : "Would convert ")
                + blocks + " Future MC blocks and " + report.items + " items in " + where + "."));
        for (Map.Entry<String, Integer> entry : report.blocks.entrySet()) {
            sender.sendMessage(new TextComponentString("  " + entry.getValue() + " x " + entry.getKey()));
        }
        if (apply && report.contents > 0) {
            sender.sendMessage(new TextComponentString("  " + report.contents + " items moved into the new blocks."));
        }
        if (!report.dropped.isEmpty()) {
            sender.sendMessage(new TextComponentString(TextFormatting.YELLOW
                    + "Properties that didn't carry across (the block kept this mod's default for them):"));
            for (Map.Entry<String, Integer> entry : report.dropped.entrySet()) {
                sender.sendMessage(new TextComponentString("  " + entry.getValue() + " x " + entry.getKey()));
            }
        }
        if (!report.skipped.isEmpty()) {
            sender.sendMessage(new TextComponentString(TextFormatting.GRAY + "Left as Future MC's (this mod has no "
                    + report.total(report.skipped) + " counterpart): " + report.skipped));
        }
        if (!apply && (blocks > 0 || report.items > 0)) {
            sender.sendMessage(new TextComponentString(TextFormatting.GRAY + "Back up the world, then run /futuremc convert"
                    + (args.length > 1 ? " " + args[1] : "") + " to do it."));
        }
        VillagerBackport.LOGGER.info("/futuremc {}: {} blocks {}, {} items, {} contents, dropped properties {}, skipped {}",
                apply ? "convert" : "preview", blocks, report.blocks, report.items, report.contents, report.dropped, report.skipped);
    }

    private static boolean near(Chunk chunk, BlockPos center, int radius) {
        int minX = chunk.x << 4;
        int minZ = chunk.z << 4;
        return minX + 15 >= center.getX() - radius && minX <= center.getX() + radius
                && minZ + 15 >= center.getZ() - radius && minZ <= center.getZ() + radius;
    }

    private static boolean inRange(BlockPos pos, BlockPos center, int radius) {
        return radius < 0 || Math.abs(pos.getX() - center.getX()) <= radius && Math.abs(pos.getZ() - center.getZ()) <= radius;
    }

    private static void blocks(World world, Chunk chunk, BlockPos center, int radius, boolean apply, Report report) {
        List<BlockPos> found = new ArrayList<BlockPos>();
        for (ExtendedBlockStorage storage : chunk.getBlockStorageArray()) {
            if (storage == Chunk.NULL_BLOCK_STORAGE || storage.isEmpty()) {
                continue;
            }
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        Block block = storage.get(x, y, z).getBlock();
                        ResourceLocation name = block.getRegistryName();
                        if (name != null && FutureMcTable.FUTURE_MC.equals(name.getNamespace())) {
                            BlockPos pos = new BlockPos((chunk.x << 4) + x, storage.getYLocation() + y, (chunk.z << 4) + z);
                            if (inRange(pos, center, radius)) {
                                found.add(pos);
                            }
                        }
                    }
                }
            }
        }
        for (BlockPos pos : found) {
            convertBlock(world, pos, apply, report);
        }
    }

    /** One Future MC block, with Future MC installed. */
    public static void convertBlock(World world, BlockPos pos, boolean apply, Report report) {
        IBlockState old = world.getBlockState(pos);
        ResourceLocation name = old.getBlock().getRegistryName();
        FutureMcTable.Target target = name == null ? null : FutureMcTable.block(name);
        if (target == null) {
            if (name != null) {
                report.add(report.skipped, name.getPath());
            }
            return;
        }
        FutureMcConvert.Converted converted = FutureMcConvert.state(target, FutureMcConvert.props(old));
        report.add(report.blocks, name + " -> " + target.block.getRegistryName());
        for (String lost : converted.dropped) {
            report.add(report.dropped, name.getPath() + " " + lost);
        }
        if (!apply) {
            return;
        }

        TileEntity tile = world.getTileEntity(pos);
        NBTTagCompound saved = tile == null ? null : tile.writeToNBT(new NBTTagCompound());
        if (tile != null) {
            // Gone before the block changes, so the old block finds nothing to drop as it's replaced.
            world.removeTileEntity(pos);
        }
        // 2: tell clients. No 1, so neighbors aren't updated; 16, so observers don't fire.
        world.setBlockState(pos, converted.state, 2 | 16);
        if (saved != null) {
            List<ItemStack> leftovers = new ArrayList<ItemStack>();
            TileEntity replaced = world.getTileEntity(pos);
            report.contents += FutureMcConvert.fill(replaced, saved, leftovers);
            if (replaced != null) {
                replaced.markDirty();
                IBlockState now = world.getBlockState(pos);
                world.notifyBlockUpdate(pos, now, now, 2);
            }
            for (ItemStack stack : leftovers) {
                net.minecraft.inventory.InventoryHelper.spawnItemStack(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
            }
        }
    }

    /** Items in containers, frames, on the ground and on players in the area. */
    private static void items(World world, List<Chunk> chunks, BlockPos center, int radius, boolean apply, Report report) {
        for (Chunk chunk : chunks) {
            for (TileEntity tile : new ArrayList<TileEntity>(chunk.getTileEntityMap().values())) {
                if (inRange(tile.getPos(), center, radius)) {
                    report.items += inventory(tile, apply);
                }
            }
            for (net.minecraft.util.ClassInheritanceMultiMap<Entity> list : chunk.getEntityLists()) {
                List<Entity> entities = new ArrayList<Entity>();
                for (Entity entity : list.getByClass(Entity.class)) {
                    entities.add(entity);
                }
                for (Entity entity : entities) {
                    if (!inRange(entity.getPosition(), center, radius)) {
                        continue;
                    }
                    if (entity instanceof EntityItem) {
                        EntityItem drop = (EntityItem) entity;
                        ItemStack converted = FutureMcConvert.item(drop.getItem());
                        if (converted != drop.getItem()) {
                            report.items++;
                            if (apply) {
                                drop.setItem(converted);
                            }
                        }
                    } else if (entity instanceof EntityItemFrame) {
                        EntityItemFrame frame = (EntityItemFrame) entity;
                        ItemStack converted = FutureMcConvert.item(frame.getDisplayedItem());
                        if (converted != frame.getDisplayedItem()) {
                            report.items++;
                            if (apply) {
                                frame.setDisplayedItem(converted);
                            }
                        }
                    } else if (entity instanceof EntityPlayer) {
                        EntityPlayer player = (EntityPlayer) entity;
                        report.items += inventory(player.inventory, apply);
                        report.items += inventory(player.getInventoryEnderChest(), apply);
                    }
                }
            }
        }
    }

    private static int inventory(TileEntity tile, boolean apply) {
        if (tile instanceof IInventory) {
            return inventory((IInventory) tile, apply);
        }
        if (tile.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null)) {
            IItemHandler handler = tile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
            if (handler instanceof IItemHandlerModifiable) {
                int n = 0;
                for (int i = 0; i < handler.getSlots(); i++) {
                    ItemStack stack = handler.getStackInSlot(i);
                    ItemStack converted = FutureMcConvert.item(stack);
                    if (converted != stack) {
                        n++;
                        if (apply) {
                            ((IItemHandlerModifiable) handler).setStackInSlot(i, converted);
                        }
                    }
                }
                if (n > 0 && apply) {
                    tile.markDirty();
                }
                return n;
            }
        }
        return 0;
    }

    static int inventory(IInventory inventory, boolean apply) {
        int n = 0;
        for (int i = 0; i < inventory.getSizeInventory(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            ItemStack converted = FutureMcConvert.item(stack);
            if (converted != stack) {
                n++;
                if (apply) {
                    inventory.setInventorySlotContents(i, converted);
                }
            }
        }
        if (n > 0 && apply) {
            inventory.markDirty();
        }
        return n;
    }

    // ------------------------------------------------------------------------------------------
    // check / dump
    // ------------------------------------------------------------------------------------------

    private static void check(ICommandSender sender) {
        int pairs = 0;
        int states = 0;
        Map<String, Integer> dropped = new TreeMap<String, Integer>();
        List<String> without = new ArrayList<String>();
        for (Block block : ForgeRegistries.BLOCKS) {
            ResourceLocation name = block.getRegistryName();
            if (name == null || !FutureMcTable.FUTURE_MC.equals(name.getNamespace())) {
                continue;
            }
            FutureMcTable.Target target = FutureMcTable.block(name);
            if (target == null) {
                without.add(name.getPath());
                continue;
            }
            pairs++;
            for (IBlockState state : saveable(block)) {
                states++;
                for (String lost : FutureMcConvert.state(target, FutureMcConvert.props(state)).dropped) {
                    String key = name.getPath() + " -> " + target.block.getRegistryName() + ": " + lost;
                    Integer n = dropped.get(key);
                    dropped.put(key, n == null ? 1 : n + 1);
                }
            }
        }
        sender.sendMessage(new TextComponentString(TextFormatting.GOLD + "" + pairs + " Future MC blocks have a counterpart; "
                + states + " saveable states checked, " + dropped.size() + " with something that doesn't carry across."));
        for (Map.Entry<String, Integer> entry : dropped.entrySet()) {
            sender.sendMessage(new TextComponentString("  " + entry.getKey() + " (" + entry.getValue() + " states)"));
        }
        sender.sendMessage(new TextComponentString(TextFormatting.GRAY + "No counterpart: " + without));
        VillagerBackport.LOGGER.info("/futuremc check: {} pairs, {} states, mismatches {}, no counterpart {}",
                pairs, states, dropped, without);
    }

    /** Every state a block writes to a world: one per metadata value it saves as. */
    @SuppressWarnings("deprecation")
    public static List<IBlockState> saveable(Block block) {
        List<IBlockState> out = new ArrayList<IBlockState>();
        for (int meta = 0; meta < 16; meta++) {
            IBlockState state;
            try {
                state = block.getStateFromMeta(meta);
            } catch (RuntimeException e) {
                continue;
            }
            if (block.getMetaFromState(state) == meta) {
                out.add(state);
            }
        }
        return out;
    }

    private static void dump(MinecraftServer server, ICommandSender sender) throws CommandException {
        JsonObject root = new JsonObject();
        int blocks = 0;
        for (Block block : ForgeRegistries.BLOCKS) {
            ResourceLocation name = block.getRegistryName();
            if (name == null || !FutureMcTable.FUTURE_MC.equals(name.getNamespace()) || FutureMcTable.block(name) == null) {
                continue;
            }
            JsonObject metas = new JsonObject();
            for (IBlockState state : saveable(block)) {
                JsonObject props = new JsonObject();
                for (Map.Entry<String, String> entry : FutureMcConvert.props(state).entrySet()) {
                    props.addProperty(entry.getKey(), entry.getValue());
                }
                metas.add(String.valueOf(block.getMetaFromState(state)), props);
            }
            root.add(name.getPath(), metas);
            blocks++;
        }
        File file = new File(server.getDataDirectory(), "futuremc_states.json");
        try (Writer out = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            new GsonBuilder().setPrettyPrinting().create().toJson(root, out);
        } catch (java.io.IOException e) {
            throw new CommandException("Couldn't write " + file + ": " + e.getMessage());
        }
        sender.sendMessage(new TextComponentString("Wrote how Future MC saves " + blocks + " blocks to " + file.getAbsolutePath()));
    }
}
