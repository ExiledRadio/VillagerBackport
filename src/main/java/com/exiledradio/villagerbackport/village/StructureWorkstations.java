package com.exiledradio.villagerbackport.village;

import com.exiledradio.villagerbackport.ModConfig;
import com.exiledradio.villagerbackport.VillagerBackport;
import com.exiledradio.villagerbackport.block.BlockWorkstation;

import net.minecraft.block.Block;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.BlockCrops;
import net.minecraft.block.BlockTrapDoor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraftforge.fml.common.IWorldGenerator;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.oredict.OreDictionary;

import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.Random;

/**
 * Finds the things a structure already contains that ought to have been a workstation, and makes
 * them one.
 *
 * <h2>What it looks for</h2>
 * Four kinds of thing, all of them furniture somebody built to mean "work happens here":
 *
 * <ul>
 * <li>crafting tables and furnaces, the plainest signal a house belongs to somebody who works;
 * <li>cauldrons, which packs scatter far more of than any village needs leatherworkers - rerolling
 *     one trades a job nobody wanted for one somebody did;
 * <li>a plank block framed by wooden trapdoors, the bench that turns up all over the larger
 *     villages, which keeps its frame and gains a purpose;
 * <li>a field of wheat large enough to be worth farming, which gets a composter stood among the
 *     crops - the one part of a village that obviously belongs to somebody and holds nothing to
 *     work at.
 * </ul>
 *
 * <p>The last two matter most in the mega villages a pack like RLCraft generates: those are mostly
 * fields and houses and short on crafting tables, so the rest of this finds little there.
 *
 * <h2>Why this and not more buildings</h2>
 * Adding village buildings only reaches villages, and only ones generated afterwards. The wider
 * problem is that a great many structures - vanilla's and, in a pack like RLCraft, several hundred
 * of Recurrent Complex's - are full of houses that plainly belong to somebody who works, and the
 * only sign of it is a crafting table and a furnace in the corner.
 *
 * <p>Those two blocks turn out to be an unusually good anchor. They are already indoors, already on
 * a floor, already where the builder thought a workbench belonged - so swapping one for a workstation
 * needs no placement logic at all, and inherits the judgement of whoever authored the structure. It
 * also costs nothing per structure mod: a crafting table is a crafting table whoever placed it.
 *
 * <h2>Why scanning fresh chunks is safe</h2>
 * This runs as a world generator, which fires once per chunk the first time it is populated - so
 * every block it looks at was put there moments ago by generation. A chunk a player has ever seen is
 * never revisited, and a crafting table someone built cannot be taken from them.
 *
 * <p>Running late matters as well as running once: Forge orders world generators by weight, and this
 * one is deliberately heavy so it goes after the generators that build the structures. Vanilla's own
 * structures are placed earlier still, during chunk population proper, which is before any world
 * generator runs at all.
 *
 * <p>One gap worth naming: a structure anchored in one chunk can write blocks into a neighbour that
 * was already generated, and those are not seen. It makes this miss a share of what it could catch,
 * which is tolerable for something that is a chance to begin with.
 */
public final class StructureWorkstations implements IWorldGenerator {

    /**
     * Heavy enough to sort after structure generators. Forge runs world generators in ascending
     * weight order, and Recurrent Complex's sit far below this.
     */
    private static final int GENERATION_WEIGHT = 2000;

    /** What a block may be turned into, and how often. */
    private static final class Rule {

        final float chance;
        final List<Block> replacements;

        Rule(float chance, List<Block> replacements) {
            this.chance = chance;
            this.replacements = replacements;
        }

        /** @return one of this rule's replacements, or one of the whole pool if it named none */
        @Nullable
        Block pick(Random random, Block replacing) {
            if (this.replacements.isEmpty()) {
                return WorkstationPool.pick(random, replacing);
            }

            List<Block> pool = new ArrayList<Block>();
            for (Block block : this.replacements) {
                if (block != replacing) {
                    pool.add(block);
                }
            }

            return pool.isEmpty() ? null : pool.get(random.nextInt(pool.size()));
        }
    }

    private static Map<Block, Rule> rules;
    private static Set<Block> planks;

    /** Blocks between two workstations stood among the same crop field or library. */
    private static final int MIN_SPACING = 6;

    private static final ResourceLocation COMPOSTER =
            new ResourceLocation(VillagerBackport.MOD_ID, "composter");
    private static final ResourceLocation LECTERN =
            new ResourceLocation(VillagerBackport.MOD_ID, "lectern");

    public static void register() {
        if (!ModConfig.structures.replaceWorkbenches) {
            return;
        }

        GameRegistry.registerWorldGenerator(new StructureWorkstations(), GENERATION_WEIGHT);
        VillagerBackport.LOGGER.info("Structure furniture and fields may become workstations.");
    }

    /** Drops the parsed rules so a config change is picked up. */
    public static void invalidate() {
        rules = null;
        planks = null;
    }

    private static Map<Block, Rule> rules() {
        if (rules != null) {
            return rules;
        }

        Map<Block, Rule> parsed = new HashMap<Block, Rule>();

        for (String entry : ModConfig.structures.replacements) {
            // 'source=chance' or 'source=chance=a,b,c'. The third part is optional and means "only
            // these", which is what keeps a furnace becoming something that is recognisably an oven.
            String[] parts = entry.split("=");

            if (parts.length < 2) {
                VillagerBackport.LOGGER.warn(
                        "Ignoring malformed replacement '{}'; expected 'modid:block=chance'.", entry);
                continue;
            }

            Block source = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(parts[0].trim()));
            if (source == null) {
                VillagerBackport.LOGGER.info(
                        "Replacement source '{}' is not present; ignoring it.", parts[0].trim());
                continue;
            }

            float chance;
            try {
                chance = Float.parseFloat(parts[1].trim());
            } catch (NumberFormatException e) {
                VillagerBackport.LOGGER.warn(
                        "Ignoring replacement '{}'; '{}' is not a chance between 0 and 1.",
                        entry, parts[1].trim());
                continue;
            }

            if (chance <= 0.0F) {
                continue;
            }

            List<Block> replacements = new ArrayList<Block>();

            if (parts.length > 2) {
                for (String name : parts[2].split(",")) {
                    Block block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(name.trim()));
                    if (block != null) {
                        replacements.add(block);
                    }
                }
            }

            parsed.put(source, new Rule(Math.min(chance, 1.0F), replacements));
        }

        rules = parsed;
        return rules;
    }

    @Override
    public void generate(Random random, int chunkX, int chunkZ, World world,
                         IChunkGenerator generator, IChunkProvider provider) {
        Map<Block, Rule> rules = rules();
        Chunk chunk = world.getChunk(chunkX, chunkZ);
        int originX = chunkX << 4;
        int originZ = chunkZ << 4;
        List<BlockPos> crops = new ArrayList<BlockPos>();
        List<BlockPos> bookshelves = new ArrayList<BlockPos>();

        if (!nearVillage(world, generator, chunkX, chunkZ)) {
            return;
        }

        // Walking the chunk's own storage rather than asking the world for each position: the
        // sections a chunk has no blocks in are absent entirely, so most of a column costs nothing.
        for (ExtendedBlockStorage section : chunk.getBlockStorageArray()) {
            if (section == Chunk.NULL_BLOCK_STORAGE) {
                continue;
            }

            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        IBlockState state = section.get(x, y, z);
                        Block block = state.getBlock();
                        BlockPos pos = new BlockPos(
                                originX + x, section.getYLocation() + y, originZ + z);

                        // Any crop, not only wheat: carrots, potatoes and beetroot are as much a
                        // farm as wheat is, and nearly every mod's crop is a BlockCrops too.
                        if (block instanceof BlockCrops) {
                            crops.add(pos);
                            continue;
                        }

                        if (block == Blocks.BOOKSHELF) {
                            bookshelves.add(pos);
                            continue;
                        }

                        Rule rule = rules.get(block);

                        if (rule != null) {
                            if (random.nextFloat() < rule.chance) {
                                replaceWith(world, chunk, pos, state,
                                        rule.pick(random, block), random);
                            }
                            continue;
                        }

                        if (isTrapdoorFramed(world, chunk, pos, block)
                                && random.nextDouble() < ModConfig.structures.trapdoorTableChance) {
                            replaceWith(world, chunk, pos, state,
                                    WorkstationPool.pick(random, block), random);
                        }
                    }
                }
            }
        }

        if (ModConfig.structures.composterInCropFields) {
            standAmong(chunk, crops, COMPOSTER, random,
                    ModConfig.structures.cropsPerComposter,
                    ModConfig.structures.maxComposterPerChunk);
        }

        if (ModConfig.structures.lecternInLibraries) {
            standAmong(chunk, bookshelves, LECTERN, random,
                    ModConfig.structures.bookshelvesPerLectern,
                    ModConfig.structures.maxLecternPerChunk);
        }
    }

    /**
     * @return true if this chunk belongs to a village, or if we are not being asked to care
     *
     * <h2>Asked by where the village is, not by what the position is inside</h2>
     * The obvious test - {@code isInsideStructure} - wants the position to fall inside one of the
     * village's component boxes, and during generation it answers no even for a block in the middle
     * of a village field. Measured: every crop in a generated field came back false.
     *
     * <p>So the question asked is where the nearest village is instead, which the generator will
     * answer, and anything within reach of one counts. That is a looser line than the component
     * boxes and a truer one: the garden behind a house and the field across the path are village as
     * much as the house is.
     *
     * <p>Worked out once per chunk rather than per block. It is a search over the structure data, so
     * asking it thousands of times a chunk would be the most expensive thing here by far.
     */
    private boolean nearVillage(World world, IChunkGenerator generator, int chunkX, int chunkZ) {
        if (!ModConfig.structures.villagesOnly) {
            return true;
        }

        BlockPos centre = new BlockPos((chunkX << 4) + 8, 64, (chunkZ << 4) + 8);

        try {
            // findUnexplored false: only villages this world has actually laid out, so this never
            // causes structure data to be generated as a side effect of asking.
            BlockPos village = generator.getNearestStructurePos(world, "Village", centre, false);

            if (village == null) {
                return false;
            }

            int reach = ModConfig.structures.villageReach;
            return Math.abs(village.getX() - centre.getX()) <= reach
                    && Math.abs(village.getZ() - centre.getZ()) <= reach;
        } catch (RuntimeException e) {
            // A generator need not implement this. Treat a refusal as "no village here".
            return false;
        }
    }

    /**
     * Stands a workstation in among a lot of something that implies one.

     * <h2>The shape of the idea</h2>
     * A field of crops and a wall of bookshelves have the same thing in common: each is plainly
     * somebody's work and neither contains anything to work at. Where there is enough of one to mean
     * business, one of them becomes the workstation that was missing - a composter among the crops, a
     * lectern among the books.
     *
     * <p>Put in place of one of the blocks counted rather than beside them, because the middle of the
     * field is where a farmer already spends its time and the edge is usually path or fence, and
     * because taking one shelf out of a wall of them leaves the room looking as built.
     */
    private void standAmong(Chunk chunk, List<BlockPos> among, ResourceLocation what, Random random,
                            int needed, int limit) {
        if (among.size() < needed) {
            return;
        }

        Block block = ForgeRegistries.BLOCKS.getValue(what);
        if (block == null) {
            return;
        }

        int allowed = Math.min(among.size() / needed, limit);
        List<BlockPos> placed = new ArrayList<BlockPos>();

        for (int attempt = 0; attempt < allowed * 8 && placed.size() < allowed; attempt++) {
            BlockPos pos = among.get(random.nextInt(among.size()));

            // Spread them out, or a large field ends up with a huddle in one corner of it.
            boolean crowded = false;
            for (BlockPos other : placed) {
                if (other.distanceSq(pos) < MIN_SPACING * MIN_SPACING) {
                    crowded = true;
                    break;
                }
            }

            if (!crowded) {
                chunk.setBlockState(pos, block.getDefaultState());
                placed.add(pos);
            }
        }
    }

    /**
     * @return true if this is one of the plank blocks framed by trapdoors
     *
     * <p>Only the four sides are counted, which is where they are hung. Anything across a chunk
     * border that has not generated yet counts as no trapdoor rather than being asked about - asking
     * would generate that chunk from inside generation.
     */
    private boolean isTrapdoorFramed(World world, Chunk chunk, BlockPos pos, Block block) {
        if (!ModConfig.structures.trapdoorFramedTables || !planks().contains(block)) {
            return false;
        }

        int sides = 0;

        for (EnumFacing facing : EnumFacing.HORIZONTALS) {
            BlockPos side = pos.offset(facing);
            Chunk owner = chunk;

            if ((side.getX() >> 4) != chunk.x || (side.getZ() >> 4) != chunk.z) {
                owner = world.getChunkProvider().getLoadedChunk(side.getX() >> 4, side.getZ() >> 4);
                if (owner == null) {
                    continue;
                }
            }

            IBlockState state = owner.getBlockState(side);

            if (state.getBlock() instanceof BlockTrapDoor && state.getMaterial() == Material.WOOD) {
                sides++;
            }
        }

        return sides >= ModConfig.structures.trapdoorTableMinSides;
    }

    /**
     * @return every block the ore dictionary calls a wooden plank
     *
     * <p>Read from the ore dictionary rather than named, because the villages these appear in are
     * built by structure mods out of whatever wood they please.
     */
    private static Set<Block> planks() {
        if (planks == null) {
            Set<Block> found = new HashSet<Block>();

            for (ItemStack stack : OreDictionary.getOres("plankWood")) {
                Block block = Block.getBlockFromItem(stack.getItem());
                if (block != Blocks.AIR) {
                    found.add(block);
                }
            }

            planks = found;
        }

        return planks;
    }

    /** Puts a workstation where something else was, keeping a facing if the old block had one. */
    private void replaceWith(World world, Chunk chunk, BlockPos pos, IBlockState old,
                             @Nullable Block target, Random random) {
        if (target == null || target == old.getBlock()) {
            return;
        }

        // Some structures generate a furnace with something in it. Replacing one would break it open
        // and scatter the contents, so those are left exactly as they were found.
        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof IInventory && !isEmpty((IInventory) tile)) {
            return;
        }

        IBlockState state = target.getDefaultState();

        if (state.getPropertyKeys().contains(BlockWorkstation.FACING)) {
            state = state.withProperty(BlockWorkstation.FACING, facingFor(chunk, pos, old, random));
        }

        // Written straight to the chunk rather than through the world. World.setBlockState notifies
        // the neighbouring blocks and pushes a lighting update outwards, and either can reach into a
        // chunk that has not generated yet - which forces it to generate, from inside generation.
        // That is cascading chunk generation, and it was measurable here: the same seed laid its
        // villages out differently with this running than without it. The chunk's own method does
        // the tile entity bookkeeping without any of the notification.
        chunk.setBlockState(pos, state);
    }

    private static boolean isEmpty(IInventory inventory) {
        for (int slot = 0; slot < inventory.getSizeInventory(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /**
     * @return which way the replacement should face
     *
     * <p>A furnace already faces into the room it was built for, so that answer is simply kept. A
     * crafting table has no facing to keep, so the next best thing is to turn the workstation towards
     * whichever side is open - a lectern facing into a wall would be the one obvious way this could
     * look wrong.
     *
     * <p>Only sides within this chunk are considered. Asking the world about a block across the
     * border would generate the chunk it is in, and a workbench against the far wall of a house is
     * not worth generating a chunk early for - the random fallback covers it.
     */
    private static EnumFacing facingFor(Chunk chunk, BlockPos pos, IBlockState old, Random random) {
        if (old.getPropertyKeys().contains(BlockHorizontal.FACING)) {
            return old.getValue(BlockHorizontal.FACING);
        }

        List<EnumFacing> open = new ArrayList<EnumFacing>();

        for (EnumFacing facing : EnumFacing.HORIZONTALS) {
            BlockPos side = pos.offset(facing);

            if ((side.getX() >> 4) != chunk.x || (side.getZ() >> 4) != chunk.z) {
                continue;
            }

            if (chunk.getBlockState(side).getMaterial() == Material.AIR) {
                open.add(facing);
            }
        }

        return open.isEmpty()
                ? EnumFacing.HORIZONTALS[random.nextInt(EnumFacing.HORIZONTALS.length)]
                : open.get(random.nextInt(open.size()));
    }
}
