package com.exiledradio.villagerbackport.migrate;

import com.exiledradio.backportcore.migrate.StandInItem;
import com.exiledradio.villagerbackport.VillagerBackport;
import com.exiledradio.villagerbackport.flora.FloraContent;
import com.exiledradio.villagerbackport.flora.ItemModernDye;

import net.minecraft.block.BlockPlanks;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.oredict.OreDictionary;

import javax.annotation.Nullable;

/**
 * Future MC items, in a world Future MC has been removed from, that can't simply be renamed.
 *
 * <p>Forge can point an old item name at one new item, keeping its damage value. That's wrong for
 * two kinds of Future MC item:
 * <ul>
 *   <li>its one dye item, white, blue, brown and black by damage, where this mod has four items;</li>
 *   <li>its six bark woods, each its own item, which are 1.12.2's logs, one item with the wood in
 *   the damage value.</li>
 * </ul>
 * Each of those names points at a stand-in here instead. A stand-in looks and is named like what it
 * stands for, works in recipes as that (the dyes as dyes, the woods as logs), and turns into it the
 * moment it's in a player's inventory; {@code /futuremc} and the chunk conversion turn the ones in
 * containers straight into the real thing. None is in a creative tab or has a recipe: nothing makes
 * one except loading such a world.
 *
 * <p>The behaviour is Backport Core's {@link StandInItem}; what each one stands for is this mod's. The
 * registry names ({@code villagerbackport:future_mc_*}) are in worlds already: never rename them.
 */
@Mod.EventBusSubscriber(modid = VillagerBackport.MOD_ID)
public class ItemFutureMcStandIn extends StandInItem {

    private static final String[] WOODS = {"oak", "spruce", "birch", "jungle", "acacia", "dark_oak"};

    ItemFutureMcStandIn(String futureMcName) {
        super("futuremc", futureMcName);
    }

    /** After the default priority, so this mod's dyes are registered first. */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void register(RegistryEvent.Register<Item> event) {
        if (FloraContent.whiteDye != null) {
            ItemFutureMcStandIn dye = add(event, "dye");
            for (int meta = 0; meta < 4; meta++) {
                ItemModernDye real = dye(meta);
                OreDictionary.registerOre(real.oreName(), new ItemStack(dye, 1, meta));
                OreDictionary.registerOre("dye", new ItemStack(dye, 1, meta));
            }
        }
        for (String wood : WOODS) {
            OreDictionary.registerOre("logWood", add(event, wood + "_wood"));
        }
    }

    private static ItemFutureMcStandIn add(RegistryEvent.Register<Item> event, String futureMcName) {
        ItemFutureMcStandIn item = new ItemFutureMcStandIn(futureMcName);
        item.setRegistryName(new ResourceLocation(VillagerBackport.MOD_ID, "future_mc_" + futureMcName));
        event.getRegistry().register(item);
        return item;
    }

    /** @return this mod's dye for a Future MC dye damage value: white, blue, brown, black */
    @Nullable
    private static ItemModernDye dye(int meta) {
        switch (meta) {
            case 0:
                return FloraContent.whiteDye;
            case 1:
                return FloraContent.blueDye;
            case 2:
                return FloraContent.brownDye;
            case 3:
                return FloraContent.blackDye;
            default:
                return null;
        }
    }

    /**
     * @param futureMcName Future MC's item name, without its namespace
     * @return what one of that item with this damage really is, or empty if nothing
     */
    public static ItemStack real(String futureMcName, int meta, int count) {
        if ("dye".equals(futureMcName)) {
            ItemModernDye dye = dye(meta);
            return dye == null ? ItemStack.EMPTY : new ItemStack(dye, count);
        }
        if (futureMcName.endsWith("_wood")) {
            String wood = futureMcName.substring(0, futureMcName.length() - "_wood".length());
            for (BlockPlanks.EnumType type : BlockPlanks.EnumType.values()) {
                if (type.getName().equals(wood)) {
                    // 1.12.2's logs: the first four in "log", acacia and dark oak in "log2".
                    return type.getMetadata() < 4
                            ? new ItemStack(Blocks.LOG, count, type.getMetadata())
                            : new ItemStack(Blocks.LOG2, count, type.getMetadata() - 4);
                }
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack real(int damage, int count) {
        return real(oldName(), damage, count);
    }

    public String futureMcName() {
        return oldName();
    }
}
