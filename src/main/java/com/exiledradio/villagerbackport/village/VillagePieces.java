package com.exiledradio.villagerbackport.village;

import com.exiledradio.villagerbackport.VillagerBackport;

import net.minecraft.util.EnumFacing;
import net.minecraft.world.gen.structure.MapGenStructureIO;
import net.minecraft.world.gen.structure.StructureComponent;
import net.minecraft.world.gen.structure.StructureVillagePieces;
import net.minecraftforge.fml.common.registry.VillagerRegistry;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Registers this mod's village buildings with vanilla village generation.
 *
 * <h2>Why generation and not placement after the fact</h2>
 * 1.12.2's villages predate job sites entirely: fourteen piece types, not one of which contains a
 * workstation, and no beds either - beds only became village furniture in 1.14. So a village that
 * generates normally has almost nothing for a villager to be employed at, and the employment this
 * mod adds has nothing to attach to.
 *
 * <p>Adding pieces is the same door Recurrent Complex comes through, which is worth knowing because
 * that is how RLCraft's own village houses are generated - their structure files declare
 * {@code "type":"vanilla"} and are registered as weighted vanilla village pieces. So RLCraft's
 * houses and these are not two systems that have to be reconciled: they are entries in one weighted
 * list, mixed into one village, and neither needs to know the other exists. A village in that pack
 * comes out with vanilla houses, RLCraft houses and these workshops side by side.
 *
 * <h2>Weights and limits</h2>
 * The weight is how often a piece is picked while the layout walks outwards; the limit caps how many
 * a single village may have. Vanilla's own range from a weight of 3 for a hut to 20 for a church,
 * and the limits scale with village size.
 *
 * <p>One trap worth recording: {@code getStructureVillageWeightedPieceList} <em>removes</em> any
 * entry whose limit rolled zero, so a limit that can come out zero means the piece is simply absent
 * from that village. The limits here are all at least one for that reason.
 *
 * <h2>Why no limit is rolled</h2>
 * Vanilla picks each of its limits with {@code MathHelper.getInt(random, ...)}, and copying that
 * here made villages stop being reproducible from their seed - the same seed laid its villages out
 * differently on each run, growing east one time and west the next.
 *
 * <p>The random being drawn from is the one the whole village layout is then built with, and Forge
 * asks each mod's handler for its weight by iterating a map keyed on the handler's class. Class hash
 * codes are identity-based, so that iteration order changes from one launch to the next; with two
 * handlers drawing from the stream, the order they draw in decides where the stream is left, and
 * every later decision about the village shifts with it.
 *
 * <p>So these limits are worked out arithmetically and nothing is drawn at all. That leaves the
 * stream exactly where vanilla left it, which is what makes a seed mean the same thing twice. The
 * variety lost is not really lost: pieces still compete for the spots along the roads, so two
 * villages with the same limits are not alike.
 */
public final class VillagePieces {

    private VillagePieces() {
    }

    /**
     * Makes the pieces known to the save format.
     *
     * <p>The save names are prefixed because they share a namespace with every other mod's village
     * pieces, and a collision would have one mod's building loaded as another's.
     */
    public static void register() {
        MapGenStructureIO.registerStructureComponent(VillageWorkshop.class, "VBP:Workshop");
        MapGenStructureIO.registerStructureComponent(VillageMarketStall.class, "VBP:Stall");
        MapGenStructureIO.registerStructureComponent(VillageMeetingPoint.class, "VBP:MeetingPoint");

        // The workshops, stalls and bell squares this mod used to add are no longer built - 1.14 never
        // had them, and its own villages put the workstations inside the houses. They stay registered
        // above so villages already saved with them still load.
        stabiliseHandlerOrder();
    }

    /**
     * Makes the order mods' village pieces are offered in stable from one launch to the next.
     *
     * <h2>What goes wrong without it</h2>
     * Forge keeps the creation handlers in a {@code HashMap} keyed on the handler's component class,
     * and hands them to village generation by iterating it. A {@code Class} hashes by identity, and
     * identity hashes depend on how much of the JVM's hashing has already happened - so that
     * iteration order is not the same twice.
     *
     * <p>Order is not cosmetic here. Vanilla picks a piece by rolling against the total weight and
     * walking the list subtracting as it goes, so the same roll lands on a different piece if the
     * list is in a different order. The whole village follows from those picks: it grew east one run
     * and west the next, on one seed, with no change but the order.
     *
     * <p>Swapping the map for a {@code LinkedHashMap} makes it registration order instead, and mods
     * load in a fixed order. Anything already registered is carried over sorted by class name, so
     * mods that got in first are ordered too rather than keeping whatever the hash map had.
     *
     * <p>This helps every mod that adds village pieces, not only this one - Recurrent Complex, which
     * is how RLCraft's houses arrive, is subject to exactly the same thing. It is also entirely
     * optional: if the field cannot be reached the only loss is that a seed stops meaning the same
     * thing twice, so a failure here is logged and generation carries on.
     */
    private static void stabiliseHandlerOrder() {
        try {
            Field field = VillagerRegistry.class.getDeclaredField("villageCreationHandlers");
            field.setAccessible(true);

            Object current = field.get(VillagerRegistry.instance());
            if (!(current instanceof Map) || current instanceof LinkedHashMap) {
                return;
            }

            @SuppressWarnings("unchecked")
            Map<Class<?>, VillagerRegistry.IVillageCreationHandler> existing =
                    (Map<Class<?>, VillagerRegistry.IVillageCreationHandler>) current;

            List<Class<?>> keys = new ArrayList<Class<?>>(existing.keySet());
            Collections.sort(keys, new Comparator<Class<?>>() {
                @Override
                public int compare(Class<?> a, Class<?> b) {
                    return a.getName().compareTo(b.getName());
                }
            });

            Map<Class<?>, VillagerRegistry.IVillageCreationHandler> stable =
                    new LinkedHashMap<Class<?>, VillagerRegistry.IVillageCreationHandler>();

            for (Class<?> key : keys) {
                stable.put(key, existing.get(key));
            }

            field.set(VillagerRegistry.instance(), stable);

            VillagerBackport.LOGGER.info(
                    "Village piece order fixed to registration order ({} already registered).",
                    keys.size());
        } catch (Exception e) {
            VillagerBackport.LOGGER.warn(
                    "Could not fix the village piece order; villages may lay out differently on the "
                            + "same seed from one launch to the next.", e);
        }
    }

    /**
     * The enclosed workshop: a real house, so it is allowed to be as common as vanilla's own small
     * houses and to scale with the village.
     */
}
