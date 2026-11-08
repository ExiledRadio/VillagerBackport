package com.exiledradio.villagerbackport.woodset;

import net.minecraft.block.BlockButtonWood;
import net.minecraft.block.BlockPressurePlate;
import net.minecraft.block.BlockTrapDoor;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * 1.14's trapdoor, button and pressure plate in the five woods that are not oak.
 *
 * <h2>Vanilla's own blocks, in another wood</h2>
 * Each is a subclass of the exact vanilla class the oak one is, built with the oak one's material,
 * hardness, sound and sensitivity (see {@code Block.registerBlocks}). So opening, closing, the
 * redstone signal, how long a button stays pressed, what a pressure plate counts, which sound each
 * plays, a trapdoor over a ladder being climbable, and pistons breaking them, are all vanilla's own
 * code and cannot drift from the oak ones. The only thing each changes is its map color, which 1.14
 * takes from the planks.
 *
 * <h2>Why three classes in one file</h2>
 * Their constructors are protected, so each needs a subclass to exist at all, and none of them needs
 * more than a constructor and a color.
 */
public final class BlockWoodRedstone {

    private BlockWoodRedstone() {
    }

    /** {@code <wood>_trapdoor}: oak's {@code new BlockTrapDoor(Material.WOOD).setHardness(3.0F)}. */
    public static class Trapdoor extends BlockTrapDoor implements com.exiledradio.backportcore.waterlog.Waterloggable {

        public final Wood wood;

        public Trapdoor(Wood wood) {
            super(Material.WOOD);
            this.wood = wood;
            this.setHardness(3.0F);
            this.setSoundType(SoundType.WOOD);
            this.disableStats();
        }

        @Override
        @SuppressWarnings("deprecation")
        public MapColor getMapColor(IBlockState state, IBlockAccess world, BlockPos pos) {
            return this.wood.planks.getMapColor();
        }
    }

    /** {@code <wood>_button}: oak's {@code new BlockButtonWood().setHardness(0.5F)}. */
    public static class Button extends BlockButtonWood {

        public final Wood wood;

        public Button(Wood wood) {
            this.wood = wood;
            this.setHardness(0.5F);
            this.setSoundType(SoundType.WOOD);
        }
    }

    /**
     * {@code <wood>_pressure_plate}: oak's
     * {@code new BlockPressurePlate(Material.WOOD, Sensitivity.EVERYTHING).setHardness(0.5F)}.
     */
    public static class PressurePlate extends BlockPressurePlate {

        public final Wood wood;

        public PressurePlate(Wood wood) {
            super(Material.WOOD, Sensitivity.EVERYTHING);
            this.wood = wood;
            this.setHardness(0.5F);
            this.setSoundType(SoundType.WOOD);
        }

        @Override
        @SuppressWarnings("deprecation")
        public MapColor getMapColor(IBlockState state, IBlockAccess world, BlockPos pos) {
            return this.wood.planks.getMapColor();
        }
    }
}
