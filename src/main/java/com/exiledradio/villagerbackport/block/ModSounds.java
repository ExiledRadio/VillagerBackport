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

    /** The horn each raid wave arrives to, heard from up to 64 blocks. */
    public static SoundEvent raidHorn;

    /** Current Java's chimes for gaining Bad Omen and Raid Omen. */
    public static SoundEvent badOmen;
    public static SoundEvent raidOmen;

    public static SoundEvent pillagerAmbient;
    public static SoundEvent pillagerHurt;
    public static SoundEvent pillagerDeath;
    public static SoundEvent pillagerCelebrate;
    public static SoundEvent ravagerAmbient;
    public static SoundEvent ravagerHurt;
    public static SoundEvent ravagerDeath;
    public static SoundEvent ravagerStep;
    public static SoundEvent ravagerAttack;
    public static SoundEvent ravagerRoar;
    public static SoundEvent ravagerStunned;
    public static SoundEvent ravagerCelebrate;
    public static SoundEvent crossbowShoot;
    public static SoundEvent crossbowLoadingStart;
    public static SoundEvent crossbowLoadingMiddle;
    public static SoundEvent crossbowLoadingEnd;
    /** The start of a load with Quick Charge I, II and III, in place of {@link #crossbowLoadingStart}. */
    public static SoundEvent[] crossbowQuickCharge = new SoundEvent[3];
    public static SoundEvent ominousBottleDispose;

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

    /** The hammering at a smithing table as gear is upgraded. 1.16's own three samples. */
    public static SoundEvent smithingTableUse;

    /** Seeds going into farmland. 1.12.2 plants in silence. */
    public static SoundEvent cropPlant;

    /** A crop being broken or harvested, in place of 1.12.2's grass. */
    public static SoundEvent cropBreak;

    /** Nether wart going into soul sand. */
    public static SoundEvent netherWartPlant;

    /** Nether wart being broken, the planting samples pitched down. */
    public static SoundEvent netherWartBreak;

    private ModSounds() {
    }

    /**
     * 1.14's {@code entity.villager.work_*}: the sound each profession makes at its workstation, by
     * 1.14's profession name.
     */
    public static final java.util.Map<String, SoundEvent> WORK = new java.util.HashMap<String, SoundEvent>();

    /** 1.14's professions that have a work sound. */
    private static final String[] WORKERS = {"armorer", "butcher", "cartographer", "cleric", "farmer", "fisherman",
            "fletcher", "leatherworker", "librarian", "mason", "shepherd", "toolsmith", "weaponsmith"};

    @SubscribeEvent
    public static void register(RegistryEvent.Register<SoundEvent> event) {
        bellUse = create(event, "block.bell.use");
        bellResonate = create(event, "block.bell.resonate");
        raidHorn = create(event, "event.raid.horn");
        badOmen = create(event, "event.mob_effect.bad_omen");
        raidOmen = create(event, "event.mob_effect.raid_omen");
        pillagerAmbient = create(event, "entity.pillager.ambient");
        pillagerHurt = create(event, "entity.pillager.hurt");
        pillagerDeath = create(event, "entity.pillager.death");
        pillagerCelebrate = create(event, "entity.pillager.celebrate");
        ravagerAmbient = create(event, "entity.ravager.ambient");
        ravagerHurt = create(event, "entity.ravager.hurt");
        ravagerDeath = create(event, "entity.ravager.death");
        ravagerStep = create(event, "entity.ravager.step");
        ravagerAttack = create(event, "entity.ravager.attack");
        ravagerRoar = create(event, "entity.ravager.roar");
        ravagerStunned = create(event, "entity.ravager.stunned");
        ravagerCelebrate = create(event, "entity.ravager.celebrate");
        crossbowShoot = create(event, "item.crossbow.shoot");
        crossbowLoadingStart = create(event, "item.crossbow.loading_start");
        crossbowLoadingMiddle = create(event, "item.crossbow.loading_middle");
        crossbowLoadingEnd = create(event, "item.crossbow.loading_end");
        for (int level = 1; level <= 3; level++) {
            crossbowQuickCharge[level - 1] = create(event, "item.crossbow.quick_charge_" + level);
        }
        // Nothing plays it since the ominous bottle became Tricky Trials' (2.7.4); kept registered for saved worlds.
        ominousBottleDispose = create(event, "item.ominous_bottle.dispose");
        grindstoneUse = create(event, "block.grindstone.use");
        cartographyTableUse = create(event, "ui.cartography_table.take_result");
        stonecutterUse = create(event, "ui.stonecutter.take_result");
        loomSelectPattern = create(event, "ui.loom.select_pattern");
        loomTakeResult = create(event, "ui.loom.take_result");
        bookPut = create(event, "item.book.put");
        bookPageTurn = create(event, "item.book.page_turn");
        smithingTableUse = create(event, "block.smithing_table.use");
        cropPlant = create(event, "item.crop.plant");
        cropBreak = create(event, "block.crop.break");
        netherWartPlant = create(event, "item.nether_wart.plant");
        netherWartBreak = create(event, "block.nether_wart.break");
        for (String profession : WORKERS) {
            WORK.put(profession, create(event, "entity.villager.work_" + profession));
        }
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
