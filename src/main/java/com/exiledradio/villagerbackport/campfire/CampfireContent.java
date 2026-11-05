package com.exiledradio.villagerbackport.campfire;

import com.exiledradio.villagerbackport.ModConfig;
import com.exiledradio.villagerbackport.ModCreativeTab;
import com.exiledradio.villagerbackport.VillagerBackport;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;

import javax.annotation.Nullable;

/**
 * 1.14's campfire: the block, its item, its tile entity and its crackle.
 *
 * <h2>Switching it off</h2>
 * {@code campfire.enabled} decides, once, at startup. Off, nothing here is registered and every field
 * is null; the recipe carries a {@link CampfireCondition} so it goes too, villages fall back to Future
 * MC's campfire or an empty spot, and the fisherman's campfire trade drops out of his pool exactly as
 * it did before this mod had one.
 *
 * <h2>Living with Future MC</h2>
 * Future MC has a campfire of its own, under the same name. Both stay registered - one may already be
 * standing in a world - and {@link com.exiledradio.villagerbackport.compat.FutureMc} hides whichever
 * half {@code compat.futureMcDuplicates} says to, as it does for the barrel and the lantern: no recipe,
 * no creative tab entry, and villages build with the other one.
 */
@Mod.EventBusSubscriber(modid = VillagerBackport.MOD_ID)
public final class CampfireContent {

    @Nullable
    public static BlockCampfire campfire;

    @Nullable
    public static SoundEvent crackle;

    private CampfireContent() {
    }

    /** @return true if this mod adds the campfire in this game - fixed for the whole session */
    public static boolean enabled() {
        return ModConfig.campfire.enabled;
    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        if (!enabled()) {
            VillagerBackport.LOGGER.info("campfire.enabled is off; this mod adds no campfire.");
            return;
        }
        campfire = new BlockCampfire();
        campfire.setRegistryName(new ResourceLocation(VillagerBackport.MOD_ID, "campfire"));
        campfire.setTranslationKey(VillagerBackport.MOD_ID + ".campfire");
        campfire.setCreativeTab(ModCreativeTab.TAB);
        event.getRegistry().register(campfire);

        GameRegistry.registerTileEntity(TileEntityCampfire.class,
                new ResourceLocation(VillagerBackport.MOD_ID, "campfire"));
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        if (campfire == null) {
            return;
        }
        ItemBlock item = new ItemBlock(campfire);
        item.setRegistryName(campfire.getRegistryName());
        event.getRegistry().register(item);
    }

    @SubscribeEvent
    public static void registerSounds(RegistryEvent.Register<SoundEvent> event) {
        if (!enabled()) {
            return;
        }
        ResourceLocation id = new ResourceLocation(VillagerBackport.MOD_ID, "block.campfire.crackle");
        crackle = new SoundEvent(id).setRegistryName(id);
        event.getRegistry().register(crackle);
    }
}
