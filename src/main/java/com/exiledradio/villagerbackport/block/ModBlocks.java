package com.exiledradio.villagerbackport.block;

import com.exiledradio.villagerbackport.VillagerBackport;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.List;

/**
 * The twelve villager workstations, registered as this mod's own blocks.
 *
 * <h2>Why ours and not another mod's</h2>
 * Several of these exist already in mods that back-port 1.14 content, and binding to those would
 * have been less work. It would also have made this mod useless on its own - installing it would
 * mean installing whatever provides the barrel and whatever provides the smoker, and a librarian's
 * behaviour would depend on which of them a pack happened to include. Shipping them here means the
 * mod works on a bare 1.12.2 install, which is worth the duplication in packs that have both.
 *
 * <p>Nothing stops a pack pointing a career at another mod's block instead - the workstation mapping
 * is configuration, and these are only its defaults.
 *
 * <h2>Shapes</h2>
 * Most of these are genuinely full cubes in 1.14 and are registered as such. The four that are not -
 * lectern, grindstone, stonecutter and bell - carry their own outline, ported alongside 1.14's own
 * models. The composter is a cube outwardly but hollow on top, and keeps 1.14's model with a full
 * collision box, which is close enough until it does something.
 */
@Mod.EventBusSubscriber(modid = VillagerBackport.MOD_ID)
public final class ModBlocks {

    private static final List<Block> BLOCKS = new ArrayList<Block>();

    /** Outlines for the blocks that are not full cubes, in sixteenths as the models are. */
    private static final AxisAlignedBB LECTERN_SHAPE = box(0, 0, 0, 16, 15, 16);
    private static final AxisAlignedBB GRINDSTONE_SHAPE = box(2, 0, 2, 14, 16, 14);
    private static final AxisAlignedBB STONECUTTER_SHAPE = box(0, 0, 0, 16, 9, 16);
    private static final AxisAlignedBB BELL_SHAPE = box(0, 0, 0, 16, 16, 16);

    private ModBlocks() {
    }

    private static AxisAlignedBB box(double x1, double y1, double z1, double x2, double y2, double z2) {
        return new AxisAlignedBB(x1 / 16.0D, y1 / 16.0D, z1 / 16.0D, x2 / 16.0D, y2 / 16.0D, z2 / 16.0D);
    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        // Hardness follows 1.14's own: wooden workstations are quick to break, stone ones are not.
        Material wood = Material.WOOD;
        Material rock = Material.ROCK;

        // Plain cubes that do not care which way they face.
        add(event, "barrel", new BlockBarrel(wood, SoundType.WOOD, 2.5F));
        add(event, "composter", new BlockComposter(wood, SoundType.WOOD, 0.6F));
        add(event, "cartography_table", new BlockCartographyTable(wood, SoundType.WOOD, 2.5F));
        add(event, "fletching_table", new BlockFletchingTable(wood, SoundType.WOOD, 2.5F));
        add(event, "smithing_table", new BlockWorkstation(wood, SoundType.WOOD, 2.5F));

        // Cubes with a front face.
        add(event, "loom", new BlockLoom(wood, SoundType.WOOD, 2.5F));
        add(event, "blast_furnace", new BlockFurnaceVariant(rock, SoundType.STONE, 3.5F, FurnaceKind.BLAST_FURNACE));
        add(event, "smoker", new BlockFurnaceVariant(rock, SoundType.STONE, 3.5F, FurnaceKind.SMOKER));

        // Shaped, all directional, and all with transparency in their textures.
        add(event, "lectern", new BlockLectern(wood, SoundType.WOOD, 2.5F, LECTERN_SHAPE));
        add(event, "grindstone", new BlockGrindstone(rock, SoundType.STONE, 2.0F, GRINDSTONE_SHAPE, true));
        add(event, "stonecutter", new BlockStonecutter(rock, SoundType.STONE, 3.5F, STONECUTTER_SHAPE, true));
        add(event, "bell", new BlockBell(rock, SoundType.METAL, 5.0F, BELL_SHAPE, true));

        // Registered alongside the blocks so it exists before any world loads one.
        net.minecraftforge.fml.common.registry.GameRegistry.registerTileEntity(
                TileEntityComposter.class, new ResourceLocation(VillagerBackport.MOD_ID, "composter"));
        net.minecraftforge.fml.common.registry.GameRegistry.registerTileEntity(
                TileEntityBarrel.class, new ResourceLocation(VillagerBackport.MOD_ID, "barrel"));
        net.minecraftforge.fml.common.registry.GameRegistry.registerTileEntity(
                TileEntityFurnaceVariant.Smoker.class, new ResourceLocation(VillagerBackport.MOD_ID, "smoker"));
        net.minecraftforge.fml.common.registry.GameRegistry.registerTileEntity(
                TileEntityFurnaceVariant.Blast.class, new ResourceLocation(VillagerBackport.MOD_ID, "blast_furnace"));
        net.minecraftforge.fml.common.registry.GameRegistry.registerTileEntity(
                TileEntityBell.class, new ResourceLocation(VillagerBackport.MOD_ID, "bell"));
        net.minecraftforge.fml.common.registry.GameRegistry.registerTileEntity(
                TileEntityLectern.class, new ResourceLocation(VillagerBackport.MOD_ID, "lectern"));

        VillagerBackport.LOGGER.info("Registered {} villager workstations.", BLOCKS.size());
    }

    private static void add(RegistryEvent.Register<Block> event, String name, Block block) {
        block.setRegistryName(new ResourceLocation(VillagerBackport.MOD_ID, name));
        block.setTranslationKey(VillagerBackport.MOD_ID + "." + name);

        event.getRegistry().register(block);
        BLOCKS.add(block);
    }

    /**
     * Registers the item form of each block.
     *
     * <p>Driven off the list built during block registration rather than repeating the names, so the
     * two can never drift apart - a block added above cannot be forgotten here.
     */
    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        for (Block block : BLOCKS) {
            ItemBlock item = new ItemBlockWorkstation(block);
            item.setRegistryName(block.getRegistryName());
            event.getRegistry().register(item);
        }
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public static void registerModels(ModelRegistryEvent event) {
        // A lectern's book and its redstone pulse are part of its state but change nothing about the
        // block's own model - the book is drawn by a renderer. Ignoring them here keeps the
        // blockstates file to the four facings it already had, instead of sixteen identical entries.
        for (Block block : BLOCKS) {
            if (block instanceof BlockLectern) {
                ModelLoader.setCustomStateMapper(block,
                        new net.minecraft.client.renderer.block.statemap.StateMap.Builder()
                                .ignore(BlockLectern.HAS_BOOK, BlockLectern.POWERED)
                                .build());
            }
        }

        for (Block block : BLOCKS) {
            Item item = Item.getItemFromBlock(block);
            ModelLoader.setCustomModelResourceLocation(item, 0,
                    new ModelResourceLocation(block.getRegistryName(), "inventory"));
        }
    }
}
