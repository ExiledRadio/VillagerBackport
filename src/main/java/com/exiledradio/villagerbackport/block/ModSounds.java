package com.exiledradio.villagerbackport.block;

import com.exiledradio.villagerbackport.VillagerBackport;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Sounds this mod ships.
 *
 * <h2>Why these are ours and not vanilla's</h2>
 * Bells arrived with the block in 1.14, so 1.12.2 has no bell sound at all. The nearest substitute
 * was the note block's bell instrument, which is a real bell but pitched for music - noticeably
 * higher and shorter than the deep toll the block should have.
 *
 * <p>So 1.14's own three samples are shipped instead: two ring variants and the resonance.
 */
@Mod.EventBusSubscriber(modid = VillagerBackport.MOD_ID)
public final class ModSounds {

    /** The ring, which alternates between two samples exactly as 1.14 does. */
    public static SoundEvent bellUse;

    /** The lingering tone that follows, and which the highlight lands on. */
    public static SoundEvent bellResonate;

    /** The grinding noise, one of three samples chosen at random. */
    public static SoundEvent grindstoneUse;

    /** Pen on parchment, played when a map is copied or widened. Also three samples. */
    public static SoundEvent cartographyTableUse;

    /**
     * The saw. Two samples, each also at a lower pitch, which is how 1.14 gets four sounds out of
     * two files - worth keeping, since this is the one that plays over and over while cutting a
     * stack.
     */
    public static SoundEvent stonecutterUse;

    /** The clack of a design being chosen at a loom. One of five samples. */
    public static SoundEvent loomSelectPattern;

    /** The shuttle, played when the patterned banner is taken. */
    public static SoundEvent loomTakeResult;

    /** A book being set down on a lectern. 1.12.2 has no book sounds at all. */
    public static SoundEvent bookPut;

    /** A page turning, heard by everyone near the lectern rather than only the reader. */
    public static SoundEvent bookPageTurn;

    private ModSounds() {
    }

    @SubscribeEvent
    public static void register(RegistryEvent.Register<SoundEvent> event) {
        bellUse = create(event, "block.bell.use");
        bellResonate = create(event, "block.bell.resonate");
        grindstoneUse = create(event, "block.grindstone.use");
        cartographyTableUse = create(event, "ui.cartography_table.take_result");
        stonecutterUse = create(event, "ui.stonecutter.take_result");
        loomSelectPattern = create(event, "ui.loom.select_pattern");
        loomTakeResult = create(event, "ui.loom.take_result");
        bookPut = create(event, "item.book.put");
        bookPageTurn = create(event, "item.book.page_turn");
    }

    private static SoundEvent create(RegistryEvent.Register<SoundEvent> event, String name) {
        ResourceLocation id = new ResourceLocation(VillagerBackport.MOD_ID, name);

        // The registry name and the sounds.json key have to match, which is what ties the event to
        // the files listed there.
        SoundEvent sound = new SoundEvent(id).setRegistryName(id);
        event.getRegistry().register(sound);
        return sound;
    }
}
