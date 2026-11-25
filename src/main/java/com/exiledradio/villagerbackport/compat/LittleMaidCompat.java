package com.exiledradio.villagerbackport.compat;

import com.exiledradio.villagerbackport.VillagerBackport;
import com.exiledradio.villagerbackport.crossbow.CrossbowContent;

import net.minecraft.item.Item;
import net.minecraftforge.fml.common.Loader;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * Lets LittleMaidReengaged's maids use the crossbow as their archer weapon.
 *
 * <p>A maid becomes an archer, and picks her weapon, through her mode triggers: an item counts when it is
 * an {@code ItemBow} or is listed as a trigger for the archer's {@code "Archer:Bow"} selector. Each new maid
 * starts from a copy of the default triggers, and a saved maid's own changes are put back on top of that
 * copy, so adding the crossbow to the defaults at load complete reaches every maid. A pack can still turn it
 * off for a maid, or with a "-" entry in LittleMaidReengaged's trigger config. Reflection only: nothing here
 * loads when LittleMaidReengaged is absent.
 *
 * <p>Firing needs one more thing, in {@code ItemCrossbow}: a maid draws and releases on her own timer, so a
 * fake player's release loads the crossbow at any draw, and her next use fires it.
 */
public final class LittleMaidCompat {

    public static final String MOD_ID = "lmreengaged";

    private static final String MODE_TRIGGER = "net.blacklab.lmr.entity.littlemaid.trigger.ModeTrigger";
    private static final String STATUS = MODE_TRIGGER + "$Status";
    private static final String ARCHER_BOW = "Archer:Bow";

    private LittleMaidCompat() {
    }

    /** Load complete: LittleMaidReengaged registered its triggers during its own init. */
    public static void install() {
        if (!Loader.isModLoaded(MOD_ID) || CrossbowContent.crossbow == null) {
            return;
        }
        try {
            boolean added = addArcherTrigger(CrossbowContent.crossbow);
            VillagerBackport.LOGGER.info(added ? "LittleMaidReengaged: maids can use the crossbow as archers."
                    : "LittleMaidReengaged: the crossbow is already set in the maids' archer triggers.");
        } catch (ReflectiveOperationException | RuntimeException e) {
            VillagerBackport.LOGGER.warn("LittleMaidReengaged: could not add the crossbow to the maids' archer triggers; "
                    + "add \"archer:villagerbackport:crossbow\" to its Trigger.ItemIds config instead.", e);
        }
    }

    /**
     * Adds {@code item} to the default {@code "Archer:Bow"} triggers, leaving an entry the config already made
     * (a trigger, or "-" to keep it out) as it is.
     *
     * @return true when it was added
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    static boolean addArcherTrigger(Item item) throws ReflectiveOperationException {
        Class<?> trigger = Class.forName(MODE_TRIGGER);
        Field defaults = trigger.getDeclaredField("defaultTrigger");
        defaults.setAccessible(true);
        Method map = trigger.getDeclaredMethod("getTriggerMap", String.class);
        map.setAccessible(true);
        Map<Item, Object> triggers = (Map<Item, Object>) map.invoke(defaults.get(null), ARCHER_BOW);
        if (triggers.containsKey(item)) {
            return false;
        }
        Class<? extends Enum> status = (Class<? extends Enum>) Class.forName(STATUS);
        triggers.put(item, Enum.valueOf(status, "TRIGGER"));
        return true;
    }
}
