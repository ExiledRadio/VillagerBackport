package com.exiledradio.villagerbackport.village.modern;

import com.exiledradio.villagerbackport.VillagerBackport;
import com.exiledradio.villagerbackport.compat.VillagerAccess;

import net.minecraft.block.BlockBush;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.monster.EntityIronGolem;
import net.minecraft.entity.monster.EntityZombieVillager;
import net.minecraft.entity.passive.EntityCow;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.entity.passive.EntitySheep;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.StructureComponent;
import net.minecraft.world.gen.structure.template.TemplateManager;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * One placed element of a 1.14 village: 1.14's {@code AbstractVillagePiece}.
 *
 * <h2>Placing</h2>
 * Each chunk places the part of the piece inside it, as 1.14 does. A rigid piece goes exactly where
 * the layout put it. A terrain-matching piece - a street - is laid onto the ground block by block,
 * 1.14's {@code GravityStructureProcessor(WORLD_SURFACE_WG, -1)}: each column one below the surface,
 * so the path replaces the grass rather than sitting on it.
 *
 * <h2>Standing in for 1.14's terrain shaping</h2>
 * 1.14 also bends the terrain itself around a village as it generates, cutting hills back from
 * houses and building ground up under them. 1.12.2's generator cannot be told to, so a rigid piece
 * does the same with blocks: the template's air above floor level clears whatever terrain is inside
 * the building's footprint, and the column under each foundation block is filled down to solid
 * ground with the biome's own filler. Without either, a house on a slope is half buried on one side
 * and standing on stilts on the other.
 */
public class ModernVillagePiece extends StructureComponent {

    /** How deep a foundation is filled before giving up, for a house on the edge of a cliff. */
    private static final int MAX_FOUNDATION = 24;

    private String poolName;
    private int elementIndex;
    private BlockPos pos;
    private int groundLevelDelta;
    private Rotation rotation;

    /** For loading from disk. */
    public ModernVillagePiece() {
    }

    ModernVillagePiece(ModernPools.Element element, BlockPos pos, int groundLevelDelta, Rotation rotation,
                       StructureBoundingBox box) {
        super(0);
        this.poolName = element.pool.name;
        this.elementIndex = element.index;
        this.pos = pos;
        this.groundLevelDelta = groundLevelDelta;
        this.rotation = rotation;
        this.boundingBox = box;
    }

    ModernPools.Element element() {
        ModernPools.Pool pool = ModernPools.get(this.poolName);
        if (pool == null || this.elementIndex < 0 || this.elementIndex >= pool.distinct.size()) {
            return new ModernPools.EmptyElement();
        }
        return pool.distinct.get(this.elementIndex);
    }

    BlockPos pos() {
        return this.pos;
    }

    Rotation rotation() {
        return this.rotation;
    }

    int groundLevelDelta() {
        return this.groundLevelDelta;
    }

    @Override
    public void offset(int x, int y, int z) {
        super.offset(x, y, z);
        this.pos = this.pos.add(x, y, z);
    }

    @Override
    protected void writeStructureToNBT(NBTTagCompound tag) {
        tag.setString("Pool", this.poolName);
        tag.setInteger("Element", this.elementIndex);
        tag.setInteger("PosX", this.pos.getX());
        tag.setInteger("PosY", this.pos.getY());
        tag.setInteger("PosZ", this.pos.getZ());
        tag.setInteger("GroundLevelDelta", this.groundLevelDelta);
        tag.setString("Rotation", this.rotation.name());
    }

    @Override
    protected void readStructureFromNBT(NBTTagCompound tag, TemplateManager templates) {
        this.poolName = tag.getString("Pool");
        this.elementIndex = tag.getInteger("Element");
        this.pos = new BlockPos(tag.getInteger("PosX"), tag.getInteger("PosY"), tag.getInteger("PosZ"));
        this.groundLevelDelta = tag.getInteger("GroundLevelDelta");
        try {
            this.rotation = Rotation.valueOf(tag.getString("Rotation"));
        } catch (IllegalArgumentException e) {
            this.rotation = Rotation.NONE;
        }
    }

    @Override
    public boolean addComponentParts(World world, Random random, StructureBoundingBox area) {
        try {
            place(element(), world, random, area);
        } catch (RuntimeException e) {
            // A broken piece must never take the chunk down with it.
            VillagerBackport.LOGGER.warn("Could not place village piece {} #{}.", this.poolName, this.elementIndex, e);
        }
        return true;
    }

    private void place(ModernPools.Element element, World world, Random random, StructureBoundingBox area) {
        if (element instanceof ModernPools.SingleElement) {
            placeTemplate((ModernPools.SingleElement) element, world, random, area);
        } else if (element instanceof ModernPools.FeatureElement) {
            if (area.isVecInside(this.pos)) {
                BlockPos ground = new BlockPos(this.pos.getX(), surface(world, this.pos.getX(), this.pos.getZ()), this.pos.getZ());
                VillageFeatures.place(((ModernPools.FeatureElement) element).feature, world, random, ground);
            }
        } else if (element instanceof ModernPools.ListElement) {
            for (ModernPools.Element sub : ((ModernPools.ListElement) element).elements) {
                place(sub, world, random, area);
            }
        }
    }

    private void placeTemplate(ModernPools.SingleElement element, World world, Random random, StructureBoundingBox area) {
        ModernTemplate template = element.template();
        boolean rigid = element.projection == ModernPools.Projection.RIGID;
        int floor = this.boundingBox.minY + this.groundLevelDelta;
        Set<Long> foundation = new HashSet<Long>();

        for (ModernTemplate.BlockEntry block : template.blocks) {
            BlockPos rel = ModernTemplate.transform(block.pos, this.rotation);
            int x = this.pos.getX() + rel.getX();
            int z = this.pos.getZ() + rel.getZ();
            if (x < area.minX || x > area.maxX || z < area.minZ || z > area.maxZ) {
                continue;
            }
            int y = rigid ? this.pos.getY() + rel.getY() : surface(world, x, z) - 1 + rel.getY();
            BlockPos at = new BlockPos(x, y, z);
            if (!area.isVecInside(at)) {
                continue;
            }

            ModernTemplate.PaletteEntry entry = template.palette[block.state];
            IBlockState state = entry.state();

            if (state != null && state.getBlock() == Blocks.AIR) {
                // 1.14 places no template air at all (BlockIgnoreStructureProcessor.AIR_AND_STRUCTURE_BLOCK)
                // and shapes the terrain instead. The air above a rigid piece's floor stands in for that.
                if (rigid && y >= floor) {
                    clear(world, at);
                }
                continue;
            }

            state = ModernPools.process(element.processors, entry, state, world, at);
            if (state == null) {
                continue;
            }
            state = state.withRotation(this.rotation);
            if (state.getBlock() instanceof net.minecraft.block.BlockTorch) {
                setWithoutSupportCheck(world, at, state);
            } else {
                world.setBlockState(at, state, 2);
            }

            if (block.nbt != null) {
                applyTileEntity(world, at, block.nbt, random);
            }

            if (rigid && block.pos.getY() == 0 && state.getMaterial().isSolid()) {
                foundation.add(at.toLong());
            }
            if (!rigid) {
                clearPlantsAbove(world, at);
            }
        }

        for (Long packed : foundation) {
            fillBelow(world, BlockPos.fromLong(packed));
        }

        if (rigid) {
            for (ModernTemplate.EntityEntry entity : template.entities) {
                BlockPos rel = ModernTemplate.transform(entity.block, this.rotation);
                BlockPos at = this.pos.add(rel);
                if (area.isVecInside(at)) {
                    spawn(world, entity, random);
                }
            }
        }
    }

    /**
     * 1.14's surface: one above the highest block that is solid or liquid, ignoring leaves and plants.
     *
     * <p>Not {@code World.getTopSolidOrLiquidBlock}: despite its name 1.12.2's version only stops at
     * blocks that block movement, so it looks straight through water to the sea floor. A street laid
     * with it runs along the bottom of a lake instead of becoming 1.14's plank bridge across it.
     */
    static int surface(World world, int x, int z) {
        net.minecraft.world.chunk.Chunk chunk = world.getChunk(x >> 4, z >> 4);
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos(x, chunk.getTopFilledSegment() + 15, z);
        while (at.getY() > 0) {
            IBlockState state = chunk.getBlockState(at);
            Material m = state.getMaterial();
            if ((m.blocksMovement() || m.isLiquid()) && !state.getBlock().isLeaves(state, world, at)
                    && !state.getBlock().isFoliage(world, at) && m != Material.WOOD) {
                return at.getY() + 1;
            }
            at.setY(at.getY() - 1);
        }
        return 0;
    }

    /**
     * Places a torch without 1.12.2's placement check.
     *
     * <p>{@code BlockTorch.onBlockAdded} drops a torch on the spot if the block it is fixed to is not
     * there yet. A building is placed one chunk at a time, so a torch on a wall that falls in the
     * neighbouring chunk always arrives first and always dropped - every lamp and wall torch on a
     * chunk edge came off as an item. 1.14's torches only check when a neighbour changes, which is
     * what writing straight into the chunk gives: the wall arrives later without disturbing it.
     */
    private static void setWithoutSupportCheck(World world, BlockPos at, IBlockState state) {
        net.minecraft.world.chunk.Chunk chunk = world.getChunk(at);
        net.minecraft.world.chunk.storage.ExtendedBlockStorage[] storage = chunk.getBlockStorageArray();
        int section = at.getY() >> 4;
        if (section < 0 || section >= storage.length) {
            return;
        }
        if (storage[section] == net.minecraft.world.chunk.Chunk.NULL_BLOCK_STORAGE) {
            storage[section] = new net.minecraft.world.chunk.storage.ExtendedBlockStorage(section << 4,
                    world.provider.hasSkyLight());
        }
        storage[section].set(at.getX() & 15, at.getY() & 15, at.getZ() & 15, state);
        chunk.markDirty();
        world.checkLight(at);
    }

    private static void clear(World world, BlockPos at) {
        IBlockState existing = world.getBlockState(at);
        // Water is left alone: emptying a cell beside a lake lets the lake in.
        if (existing.getMaterial() != Material.AIR && !existing.getMaterial().isLiquid()) {
            world.setBlockState(at, Blocks.AIR.getDefaultState(), 2);
        }
    }

    /** A path laid through long grass should not have grass standing on it. */
    private static void clearPlantsAbove(World world, BlockPos at) {
        for (int i = 1; i <= 2; i++) {
            BlockPos above = at.up(i);
            IBlockState state = world.getBlockState(above);
            if (state.getBlock() instanceof BlockBush || state.getMaterial() == Material.VINE) {
                world.setBlockState(above, Blocks.AIR.getDefaultState(), 2);
            } else {
                break;
            }
        }
    }

    private static void fillBelow(World world, BlockPos foundation) {
        IBlockState filler = world.getBiome(foundation).fillerBlock;
        if (filler.getBlock() == Blocks.SAND) {
            // Sand would fall straight back out from under a house; 1.12.2 puts sandstone under its
            // own sand for the same reason.
            filler = Blocks.SANDSTONE.getDefaultState();
        } else if (filler.getMaterial() == Material.AIR || filler.getBlock() instanceof net.minecraft.block.BlockFalling) {
            filler = Blocks.DIRT.getDefaultState();
        }
        BlockPos at = foundation.down();
        for (int i = 0; i < MAX_FOUNDATION && at.getY() > 0; i++) {
            IBlockState existing = world.getBlockState(at);
            Material m = existing.getMaterial();
            if (m.isSolid() && m != Material.LEAVES && m != Material.PLANTS && m != Material.WOOD) {
                return;
            }
            world.setBlockState(at, filler, 2);
            at = at.down();
        }
    }

    private void applyTileEntity(World world, BlockPos at, NBTTagCompound extra, Random random) {
        TileEntity te = world.getTileEntity(at);
        if (te == null) {
            return;
        }
        NBTTagCompound tag = te.writeToNBT(new NBTTagCompound());
        tag.merge(extra);
        if (tag.hasKey("LootTable")) {
            tag.setLong("LootTableSeed", random.nextLong());
        }
        tag.setInteger("x", at.getX());
        tag.setInteger("y", at.getY());
        tag.setInteger("z", at.getZ());
        te.readFromNBT(tag);
        te.rotate(this.rotation);
        te.markDirty();
    }

    // ---------------------------------------------------------------------------------------------
    // Entities
    // ---------------------------------------------------------------------------------------------

    private void spawn(World world, ModernTemplate.EntityEntry entry, Random random) {
        double rx = entry.x;
        double rz = entry.z;
        double x;
        double z;
        // 1.14's Template.transformedVec3d. Unlike block positions, an entity's position rotates
        // about the far corner of its block, hence the 1.
        switch (this.rotation) {
            case CLOCKWISE_90:
                x = 1.0D - rz;
                z = rx;
                break;
            case CLOCKWISE_180:
                x = 1.0D - rx;
                z = 1.0D - rz;
                break;
            case COUNTERCLOCKWISE_90:
                x = rz;
                z = 1.0D - rx;
                break;
            default:
                x = rx;
                z = rz;
        }
        double wx = this.pos.getX() + x;
        double wy = this.pos.getY() + entry.y;
        double wz = this.pos.getZ() + z;

        NBTTagCompound tag = entry.tag;
        String kind = tag.getString("kind");
        Entity entity = create(world, kind, tag, random);
        if (entity == null) {
            return;
        }
        float yaw = tag.getFloat("yaw");
        entity.setLocationAndAngles(wx, wy, wz, yaw, 0.0F);
        entity.rotationYaw = entity.getRotatedYaw(this.rotation);
        if (entity instanceof EntityLiving) {
            ((EntityLiving) entity).rotationYawHead = entity.rotationYaw;
            ((EntityLiving) entity).renderYawOffset = entity.rotationYaw;
        }
        world.spawnEntity(entity);
    }

    private static Entity create(World world, String kind, NBTTagCompound tag, Random random) {
        if ("villager".equals(kind) || "nitwit".equals(kind)) {
            // 1.14's village villagers are unemployed or nitwits, never pre-employed. Building the
            // villager the way 1.12.2's own village pieces do and then dropping the trades it was
            // handed leaves it exactly where this mod's job system expects a newcomer to be: no
            // trades, no experience, so it is set unemployed - or, with the nitwit profession, made
            // a nitwit - and goes looking for a workstation.
            EntityVillager villager = new EntityVillager(world, "nitwit".equals(kind) ? 5 : 0);
            villager.finalizeMobSpawn(world.getDifficultyForLocation(new BlockPos(villager)), null, false);
            VillagerAccess.clearBuyingList(villager);
            if (tag.getBoolean("baby")) {
                villager.setGrowingAge(-24000);
            }
            return villager;
        }
        if ("zombie_villager".equals(kind)) {
            EntityZombieVillager zombie = new EntityZombieVillager(world);
            zombie.enablePersistence();
            return zombie;
        }
        if ("iron_golem".equals(kind)) {
            return new EntityIronGolem(world);
        }
        if ("cow".equals(kind)) {
            return new EntityCow(world);
        }
        if ("pig".equals(kind)) {
            return new EntityPig(world);
        }
        if ("sheep".equals(kind)) {
            EntitySheep sheep = new EntitySheep(world);
            sheep.setFleeceColor(EnumDyeColor.byMetadata(tag.getByte("color") & 15));
            return sheep;
        }
        if ("horse".equals(kind)) {
            EntityHorse horse = new EntityHorse(world);
            NBTTagCompound data = horse.writeToNBT(new NBTTagCompound());
            data.setInteger("Variant", tag.getInteger("variant"));
            horse.readFromNBT(data);
            return horse;
        }
        if ("armor_stand".equals(kind)) {
            EntityArmorStand stand = new EntityArmorStand(world);
            NBTTagList armour = tag.getTagList("armor", 8);
            EntityEquipmentSlot[] slots = {EntityEquipmentSlot.FEET, EntityEquipmentSlot.LEGS,
                    EntityEquipmentSlot.CHEST, EntityEquipmentSlot.HEAD};
            for (int i = 0; i < armour.tagCount() && i < slots.length; i++) {
                String id = armour.getStringTagAt(i);
                if (!id.isEmpty()) {
                    Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(id));
                    if (item != null) {
                        stand.setItemStackToSlot(slots[i], new ItemStack(item));
                    }
                }
            }
            return stand;
        }
        return null;
    }
}
