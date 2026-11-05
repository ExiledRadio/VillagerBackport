package com.exiledradio.villagerbackport.bamboo;

import java.util.Random;

/**
 * A panda's genes, and the personality two of them make.
 *
 * <p>Every panda carries two genes, a main one and a hidden one. Its personality is its main gene,
 * unless that gene is recessive - brown or weak - in which case the panda only shows it when the
 * hidden gene matches, and is otherwise normal. So a brown panda always has two brown genes, and two
 * normal-looking pandas can have a brown cub.
 *
 * <p>Ids, names, which genes are recessive and how often each turns up are 1.14's.
 */
public enum PandaGene {
    NORMAL(0, "normal", false),
    LAZY(1, "lazy", false),
    WORRIED(2, "worried", false),
    PLAYFUL(3, "playful", false),
    BROWN(4, "brown", true),
    WEAK(5, "weak", true),
    AGGRESSIVE(6, "aggressive", false);

    private static final PandaGene[] BY_ID = values();

    private final int id;
    private final String name;
    private final boolean recessive;

    PandaGene(int id, String name, boolean recessive) {
        this.id = id;
        this.name = name;
        this.recessive = recessive;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public boolean isRecessive() {
        return recessive;
    }

    /** @return the personality a panda with these two genes shows */
    public static PandaGene personality(PandaGene main, PandaGene hidden) {
        if (main.isRecessive()) {
            return main == hidden ? main : NORMAL;
        }
        return main;
    }

    public static PandaGene byId(int id) {
        return id < 0 || id >= BY_ID.length ? NORMAL : BY_ID[id];
    }

    public static PandaGene byName(String name) {
        for (PandaGene gene : BY_ID) {
            if (gene.name.equals(name)) {
                return gene;
            }
        }
        return NORMAL;
    }

    /**
     * A gene for a panda that has no parents, at 1.14's odds: lazy, worried, playful and aggressive
     * 1 in 16 each, weak 5 in 16, brown 2 in 16, and normal the remaining 5.
     */
    public static PandaGene random(Random rand) {
        int i = rand.nextInt(16);
        if (i == 0) {
            return LAZY;
        } else if (i == 1) {
            return WORRIED;
        } else if (i == 2) {
            return PLAYFUL;
        } else if (i == 4) {
            return AGGRESSIVE;
        } else if (i < 9) {
            return WEAK;
        } else if (i < 11) {
            return BROWN;
        }
        return NORMAL;
    }
}
