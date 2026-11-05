package com.exiledradio.villagerbackport.illager;

import net.minecraft.util.SoundEvent;

/** A raider with a cheer of its own for a village it has taken: 1.14's {@code getRaidLossSound}. */
public interface Celebrates {

    SoundEvent celebrateSound();
}
