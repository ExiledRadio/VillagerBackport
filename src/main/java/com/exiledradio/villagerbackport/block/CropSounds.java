package com.exiledradio.villagerbackport.block;

import com.exiledradio.villagerbackport.ModConfig;
import com.exiledradio.villagerbackport.VillagerBackport;

import net.minecraft.block.Block;
import net.minecraft.block.BlockCrops;
import net.minecraft.block.BlockNetherWart;
import net.minecraft.block.BlockStem;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.lang.reflect.Field;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * 1.14's crop sounds: a soft rustle when seeds go in, and a crunch when a crop is broken.
 *
 * <p>1.12.2 crops use the generic plant sounds, which are grass, and planting makes no sound at
 * all - seeds are not block items, so nothing plays the block's place sound. 1.14 gave crops their
 * own sound type, and its farmers play the planting sound as they replant.
 *
 * <h2>Which blocks</h2>
 * The three sound types are 1.14's {@code SoundType.CROP}, {@code STEM} and {@code NETHER_WART}.
 * Wheat, carrots, potatoes and beetroot take the crop sounds, as does every other mod's
 * {@link BlockCrops} still using the plain plant sounds - a mod that chose its own keeps them.
 * Pumpkin and melon stems already sound like wood in 1.12.2 and only gain the planting sound, as
 * in 1.14. Nether wart gets its own.
 *
 * <p>The sound type is a field on the block, set here once loading is over, on both sides: the
 * client plays break and step sounds from its own copy of the block.
 */
@Mod.EventBusSubscriber(modid = VillagerBackport.MOD_ID)
public final class CropSounds {

    /** Blocks given a new sound type, to the type they were given. */
    private static final Map<Block, SoundType> CHANGED = new IdentityHashMap<Block, SoundType>();

    private CropSounds() {
    }

    /** Called from postInit, once every mod's crops are registered. */
    public static void apply() {
        if (!ModConfig.sounds.cropSounds) {
            return;
        }

        // Built here rather than as constants: the sound events only exist once the registry event
        // has run, and a SoundType holding null would play nothing.
        SoundType crop = new SoundType(1.0F, 1.0F, ModSounds.cropBreak, SoundEvents.BLOCK_GRASS_STEP,
                ModSounds.cropPlant, SoundEvents.BLOCK_GRASS_HIT, SoundEvents.BLOCK_GRASS_FALL);
        SoundType stem = new SoundType(1.0F, 1.0F, SoundEvents.BLOCK_WOOD_BREAK, SoundEvents.BLOCK_WOOD_STEP,
                ModSounds.cropPlant, SoundEvents.BLOCK_WOOD_HIT, SoundEvents.BLOCK_WOOD_FALL);
        SoundType netherWart = new SoundType(1.0F, 1.0F, ModSounds.netherWartBreak, SoundEvents.BLOCK_STONE_STEP,
                ModSounds.netherWartPlant, SoundEvents.BLOCK_STONE_HIT, SoundEvents.BLOCK_STONE_FALL);

        Field field;
        try {
            field = ReflectionHelper.findField(Block.class, "blockSoundType", "field_149762_H");
        } catch (RuntimeException e) {
            VillagerBackport.LOGGER.warn("Could not reach the block sound field; crops keep 1.12.2's sounds.", e);
            return;
        }

        for (Block block : ForgeRegistries.BLOCKS) {
            SoundType current;
            try {
                current = (SoundType) field.get(block);
            } catch (IllegalAccessException e) {
                continue;
            }

            SoundType replacement = null;
            // 1.12.2 never gives nether wart a sound type, so it breaks with the default stone sound.
            if (block instanceof BlockNetherWart && (current == SoundType.STONE || current == SoundType.PLANT)) {
                replacement = netherWart;
            } else if (block instanceof BlockStem && (current == SoundType.WOOD || current == SoundType.PLANT)) {
                replacement = stem;
            } else if (block instanceof BlockCrops && current == SoundType.PLANT) {
                replacement = crop;
            }

            if (replacement != null) {
                try {
                    field.set(block, replacement);
                    CHANGED.put(block, replacement);
                } catch (IllegalAccessException e) {
                    // Leave this one as it was.
                }
            }
        }

        VillagerBackport.LOGGER.info("Gave {} crop blocks 1.14's crop sounds.", CHANGED.size());
    }

    /** @return true if this block was given one of the crop sound types */
    public static boolean hasCropSound(Block block) {
        return CHANGED.containsKey(block);
    }

    /**
     * Plays the planting sound when a player plants seeds.
     *
     * <p>A block item already plays its block's place sound, so only seeds and the like - items that
     * place a crop without being its block item - are given one here. Played to everyone nearby,
     * the planter included, since nothing on the planter's side played it already.
     */
    @SubscribeEvent
    public static void onPlace(BlockEvent.PlaceEvent event) {
        IBlockState placed = event.getPlacedBlock();
        if (!hasCropSound(placed.getBlock())) {
            return;
        }

        EntityPlayer player = event.getPlayer();
        ItemStack held = player == null ? ItemStack.EMPTY : player.getHeldItem(event.getHand());
        if (held.getItem() instanceof ItemBlock) {
            return;
        }

        // What a block item plays its place sound at.
        SoundType type = CHANGED.get(placed.getBlock());
        event.getWorld().playSound(null, event.getPos(), type.getPlaceSound(), SoundCategory.BLOCKS,
                (type.getVolume() + 1.0F) / 2.0F, type.getPitch() * 0.8F);
    }

    /**
     * Plays a crop's planting sound as a villager plants it, at full volume and pitch as 1.14's
     * farmers do.
     */
    public static void playVillagerPlant(World world, BlockPos pos) {
        SoundType type = CHANGED.get(world.getBlockState(pos).getBlock());
        if (type != null) {
            world.playSound(null, pos, type.getPlaceSound(), SoundCategory.BLOCKS, 1.0F, 1.0F);
        }
    }
}
