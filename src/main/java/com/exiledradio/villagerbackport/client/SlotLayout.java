package com.exiledradio.villagerbackport.client;

import com.exiledradio.villagerbackport.VillagerBackport;

import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * Moves an existing container's slots to 1.14's merchant layout, client-side.
 *
 * <h2>Why move slots instead of building a new container</h2>
 * 1.14's merchant screen puts the player inventory 100 pixels further right than 1.12.2's, to make
 * room for the trade list down the left. The obvious way to get that is a custom {@code Container}
 * with the new coordinates - but that would mean replacing {@code ContainerMerchant}, which is
 * exactly the class FermiumMixins patches, and would take this mod's screen out of reach of every
 * other mod that hooks the vanilla merchant container.
 *
 * <p>Slot coordinates are only ever used client-side, for drawing and for hit-testing the mouse.
 * The server never reads them. So the entire layout change can be made on the client's copy of the
 * container, leaving vanilla's class - and every patch applied to it - completely untouched.
 *
 * <h2>The final-field problem</h2>
 * {@link Slot#xPos} and {@link Slot#yPos} are {@code public final int}. Reflection can write a final
 * instance field, but only after clearing the {@code FINAL} bit from the field's modifiers, which is
 * what {@link #unfinalise} does. This works on Java 8, which is the only thing 1.12.2 runs on.
 *
 * <p>If any of that fails the layout is left alone and {@link #isAvailable()} reports false, which
 * makes the caller fall back to the vanilla screen rather than draw a 1.14 layout over 1.12.2 slot
 * positions - which would look correct but put the click targets in the wrong place.
 */
@SideOnly(Side.CLIENT)
public final class SlotLayout {

    /** 1.14 merchant slot positions: first cost, second cost, result. */
    private static final int[][] TRADE_SLOTS = {{136, 37}, {162, 37}, {220, 37}};

    /** 1.14 player inventory origin. 1.12.2 uses x=8; the trade list needs the space. */
    private static final int INVENTORY_X = 108;
    private static final int INVENTORY_Y = 84;
    private static final int HOTBAR_Y = 142;
    private static final int SLOT_PITCH = 18;

    @Nullable
    private static Field xPos;

    @Nullable
    private static Field yPos;

    private static boolean resolved;
    private static boolean available;

    private SlotLayout() {
    }

    public static boolean isAvailable() {
        resolve();
        return available;
    }

    /**
     * Repositions a merchant container's slots in place.
     *
     * <p>Assumes vanilla's ordering, which {@code ContainerMerchant} fixes: three trade slots, then
     * 27 inventory slots in three rows of nine, then the nine hotbar slots. Anything past that is
     * left where it is - a mod that added a slot of its own gets to keep its own position rather
     * than being dragged into a layout it knows nothing about.
     */
    public static void apply(Container container) {
        if (!isAvailable()) {
            return;
        }

        for (int i = 0; i < container.inventorySlots.size(); i++) {
            Slot slot = container.inventorySlots.get(i);

            if (i < TRADE_SLOTS.length) {
                move(slot, TRADE_SLOTS[i][0], TRADE_SLOTS[i][1]);
            } else if (i < TRADE_SLOTS.length + 27) {
                int index = i - TRADE_SLOTS.length;
                move(slot, INVENTORY_X + (index % 9) * SLOT_PITCH, INVENTORY_Y + (index / 9) * SLOT_PITCH);
            } else if (i < TRADE_SLOTS.length + 36) {
                int index = i - TRADE_SLOTS.length - 27;
                move(slot, INVENTORY_X + index * SLOT_PITCH, HOTBAR_Y);
            }
        }
    }

    private static void move(Slot slot, int x, int y) {
        try {
            xPos.setInt(slot, x);
            yPos.setInt(slot, y);
        } catch (IllegalAccessException e) {
            // Reported once at resolve time; failing per-slot here would spam the log.
        }
    }

    private static void resolve() {
        if (resolved) {
            return;
        }
        resolved = true;

        try {
            xPos = unfinalise(ReflectionHelper.findField(Slot.class, "xPos", "field_75223_e"));
            yPos = unfinalise(ReflectionHelper.findField(Slot.class, "yPos", "field_75221_f"));
            available = true;
        } catch (Exception e) {
            VillagerBackport.LOGGER.error(
                    "Could not make Slot positions writable; falling back to the vanilla trade "
                            + "screen. Trading itself is unaffected.", e);
            available = false;
        }
    }

    /** Clears the FINAL modifier so the field can be written. */
    private static Field unfinalise(Field field) throws Exception {
        field.setAccessible(true);
        Field modifiers = Field.class.getDeclaredField("modifiers");
        modifiers.setAccessible(true);
        modifiers.setInt(field, field.getModifiers() & ~Modifier.FINAL);
        return field;
    }
}
