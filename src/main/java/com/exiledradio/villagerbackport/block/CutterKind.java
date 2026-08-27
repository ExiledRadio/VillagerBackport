package com.exiledradio.villagerbackport.block;

import com.exiledradio.villagerbackport.ModConfig;

import net.minecraft.block.material.Material;

/**
 * The two saws, and the only thing that differs between them.
 *
 * <h2>One machine, two materials</h2>
 * A stonecutter and a fletching table do the same job: take one whole block, offer every shape that
 * block could be cut into, and hand back the chosen one. 1.14 only shipped the stone one - its
 * fletching table does nothing at all - so the wooden one is this mod's own idea rather than a port,
 * and the honest way to build it was to notice that the saw already written did not care what it was
 * cutting.
 *
 * <p>What actually differs is a single question: which blocks is this saw willing to accept. So that
 * is what lives here, along with the settings each one reads. Everything else - reading the crafting
 * recipes backwards, working out how many a cut yields, the screen, the container - is shared.
 *
 * <p>The material test is what makes the split natural rather than arbitrary: a stonecutter works
 * rock and a fletching table works wood, and the game already records which is which on every block
 * in it, including every block a mod added.
 */
public enum CutterKind {

    STONE(Material.ROCK) {
        @Override
        boolean derive() {
            return ModConfig.workstations.deriveStonecutterRecipes;
        }

        @Override
        String[] extras() {
            return ModConfig.workstations.extraStonecutterRecipes;
        }

        @Override
        String[] blocked() {
            return ModConfig.workstations.blockedStonecutterInputs;
        }
    },

    WOOD(Material.WOOD) {
        @Override
        boolean derive() {
            return ModConfig.workstations.deriveFletchingRecipes;
        }

        @Override
        String[] extras() {
            return ModConfig.workstations.extraFletchingRecipes;
        }

        @Override
        String[] blocked() {
            return ModConfig.workstations.blockedFletchingInputs;
        }
    };

    final Material material;

    CutterKind(Material material) {
        this.material = material;
    }

    /** Whether to work the list out from the crafting recipes already in the game. */
    abstract boolean derive();

    /** Cuts to add on top of whatever was worked out. */
    abstract String[] extras();

    /** Blocks this saw will not accept, whatever the rules decided. */
    abstract String[] blocked();
}
