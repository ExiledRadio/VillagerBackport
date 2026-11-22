package com.exiledradio.villagerbackport.selftest;

import com.exiledradio.backportcore.waterlog.Waterloggable;
import com.exiledradio.backportcore.waterlog.Waterlogging;
import com.exiledradio.villagerbackport.VillagerBackport;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.io.File;

/**
 * Test only, never shipped: stands in for Update Aquatic Backport's sea pickle in this mod's dev
 * environment, which doesn't have that mod. Registers {@code updateaquaticbackport:sea_pickle} with the
 * same properties and metadata as Update Aquatic's (and Village &amp; Pillage 2.3.0 to 2.5.0's), holding
 * water through Backport Core and moving a stored {@code waterlogged} flag into real water as Update
 * Aquatic does. Only when the file {@code fake-ua} exists in the run directory and the real mod is not
 * loaded.
 */
@Mod.EventBusSubscriber(modid = VillagerBackport.MOD_ID)
public final class FakeUpdateAquatic {

    /** Plain stand-ins, by Update Aquatic's names, for the trader's trades. */
    static final String[] TRADER_ITEMS = {"kelp", "tube_coral_block", "brain_coral_block", "bubble_coral_block",
            "fire_coral_block", "horn_coral_block", "nautilus_shell", "pufferfish_bucket", "tropical_fish_bucket", "blue_ice"};

    static Block pickle;
    static Item pickleItem;

    private FakeUpdateAquatic() {
    }

    static boolean active() {
        return new File("fake-ua").exists() && !Loader.isModLoaded("updateaquaticbackport");
    }

    public static class Pickle extends Block implements Waterloggable {
        public static final PropertyInteger PICKLES = PropertyInteger.create("pickles", 1, 4);
        public static final PropertyBool WATERLOGGED = PropertyBool.create("waterlogged");

        Pickle() {
            super(Material.PLANTS);
            setDefaultState(this.blockState.getBaseState().withProperty(PICKLES, 1).withProperty(WATERLOGGED, false));
        }

        @Override
        protected BlockStateContainer createBlockState() {
            return new BlockStateContainer(this, new IProperty[] {PICKLES, WATERLOGGED});
        }

        @Override
        @SuppressWarnings("deprecation")
        public IBlockState getStateFromMeta(int meta) {
            return getDefaultState().withProperty(PICKLES, (meta & 3) + 1).withProperty(WATERLOGGED, (meta & 4) != 0);
        }

        @Override
        public int getMetaFromState(IBlockState state) {
            return (state.getValue(PICKLES) - 1) | (state.getValue(WATERLOGGED) ? 4 : 0);
        }

        @Override
        @SuppressWarnings("deprecation")
        public boolean isOpaqueCube(IBlockState state) {
            return false;
        }

        @Override
        @SuppressWarnings("deprecation")
        public boolean isFullCube(IBlockState state) {
            return false;
        }
    }

    @SubscribeEvent
    public static void blocks(RegistryEvent.Register<Block> event) {
        if (!active()) {
            return;
        }
        Pickle block = new Pickle();
        block.setRegistryName(new ResourceLocation("updateaquaticbackport", "sea_pickle"));
        event.getRegistry().register(block);
        pickle = block;
        Waterlogging.migrateStoredProperty(block, Pickle.WATERLOGGED);
        VillagerBackport.LOGGER.info("SELFTEST fake Update Aquatic sea pickle registered");
    }

    @SubscribeEvent
    public static void items(RegistryEvent.Register<Item> event) {
        if (!active()) {
            return;
        }
        pickleItem = new ItemBlock(pickle).setRegistryName(pickle.getRegistryName());
        event.getRegistry().register(pickleItem);
        // What the wandering trader sells by Update Aquatic's names (TraderTrades).
        for (String name : TRADER_ITEMS) {
            event.getRegistry().register(new Item().setRegistryName(new ResourceLocation("updateaquaticbackport", name)));
        }
    }
}
