package com.exiledradio.villagerbackport.migrate;

import com.exiledradio.villagerbackport.VillagerBackport;

import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import javax.annotation.Nullable;

/**
 * The ominous bottle, which 1.21 added and which Tricky Trials Backport owns since its 1.0.2 (Backport Core 1.4.5's
 * ownership table). Village &amp; Pillage 2.2.0 to 2.7.3 had its own, {@code villagerbackport:ominous_bottle}
 * (metadata 0-4 = Bad Omen I-V, as Tricky Trials' and Deeper Depths'), and worlds still hold it.
 *
 * <ul>
 *   <li><b>With Tricky Trials' bottle</b> (installed, trials on), captains drop it (1.21) and the old name is
 *   pointed at it (Forge's {@code MissingMappings}): the same item and metadata, so every stack keeps its count and
 *   level wherever it is.</li>
 *   <li><b>Without it, with Deeper Depths</b>, the old name is pointed at Deeper Depths' bottle, the same way.</li>
 *   <li><b>With neither</b>, {@link ItemOminousBottleStandIn} takes the old name, so old bottles keep their count
 *   and level and become Tricky Trials' the day it is added. It is named and drawn as an ominous bottle and does
 *   nothing else: Village &amp; Pillage alone has 1.14's raids, where captains give Bad Omen and there is no
 *   bottle.</li>
 * </ul>
 * Which bottle is there is read from the registry itself, at the lowest priority of the item event, so a Tricky
 * Trials with its trials switched off counts as no bottle.
 */
@Mod.EventBusSubscriber(modid = VillagerBackport.MOD_ID)
public final class OminousBottles {

    public static final ResourceLocation OLD_BOTTLE = new ResourceLocation(VillagerBackport.MOD_ID, "ominous_bottle");
    public static final ResourceLocation TRICKY_TRIALS_BOTTLE = new ResourceLocation("trickytrialsbackport", "ominous_bottle");
    public static final ResourceLocation DEEPER_DEPTHS_BOTTLE = new ResourceLocation("deeperdepths", "ominous_bottle");

    /** Bottle levels I to V, metadata 0 to 4. */
    public static final int LEVELS = 5;

    private static Item standIn;

    private OminousBottles() {
    }

    /** @return the stand-in under the old name, or null when another mod's bottle takes it */
    @Nullable
    public static Item standIn() {
        return standIn;
    }

    /** @return Tricky Trials' ominous bottle, or Deeper Depths', or null when there is none */
    @Nullable
    public static Item bottle() {
        Item item = item(TRICKY_TRIALS_BOTTLE);
        return item != null ? item : item(DEEPER_DEPTHS_BOTTLE);
    }

    /** @return true when captains drop ominous bottles, as in 1.21, instead of giving Bad Omen, as in 1.14 */
    public static boolean captainsDropBottles() {
        return bottle() != null;
    }

    /** @return one ominous bottle of {@code level} (0 = I), or empty when there is no bottle */
    public static ItemStack bottle(int level) {
        Item item = bottle();
        return item == null ? ItemStack.EMPTY : new ItemStack(item, 1, Math.max(0, Math.min(LEVELS - 1, level)));
    }

    /** Lowest priority, so every other mod's items are in first. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void registerItems(RegistryEvent.Register<Item> event) {
        if (event.getRegistry().containsKey(TRICKY_TRIALS_BOTTLE) || event.getRegistry().containsKey(DEEPER_DEPTHS_BOTTLE)) {
            return;
        }
        standIn = new ItemOminousBottleStandIn();
        standIn.setRegistryName(OLD_BOTTLE);
        event.getRegistry().register(standIn);
    }

    @SubscribeEvent
    public static void missingItems(RegistryEvent.MissingMappings<Item> event) {
        for (RegistryEvent.MissingMappings.Mapping<Item> mapping : event.getAllMappings()) {
            if (OLD_BOTTLE.equals(mapping.key)) {
                Item target = bottle();
                if (target != null) {
                    VillagerBackport.LOGGER.info("{} from an older world is now {}.", mapping.key, target.getRegistryName());
                    mapping.remap(target);
                }
            }
        }
    }

    @Nullable
    private static Item item(ResourceLocation name) {
        Item item = ForgeRegistries.ITEMS.containsKey(name) ? ForgeRegistries.ITEMS.getValue(name) : null;
        return item == null || item == Items.AIR || !name.equals(item.getRegistryName()) ? null : item;
    }
}
