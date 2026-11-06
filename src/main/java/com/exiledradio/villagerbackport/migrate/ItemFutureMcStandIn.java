package com.exiledradio.villagerbackport.migrate;

import com.exiledradio.villagerbackport.VillagerBackport;
import com.exiledradio.villagerbackport.flora.FloraContent;
import com.exiledradio.villagerbackport.flora.ItemModernDye;

import net.minecraft.block.BlockPlanks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.oredict.OreDictionary;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Map;

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
 */
@Mod.EventBusSubscriber(modid = VillagerBackport.MOD_ID)
public class ItemFutureMcStandIn extends Item {

    /** Future MC item name (its path) to the stand-in it loads as. */
    static final Map<String, ItemFutureMcStandIn> BY_FUTURE_MC_NAME = new LinkedHashMap<String, ItemFutureMcStandIn>();

    private static final String[] WOODS = {"oak", "spruce", "birch", "jungle", "acacia", "dark_oak"};

    /** Future MC's name for it: "dye", "oak_wood" and so on. */
    private final String futureMcName;

    ItemFutureMcStandIn(String futureMcName) {
        this.futureMcName = futureMcName;
        this.setHasSubtypes(true);
        this.setMaxDamage(0);
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
        BY_FUTURE_MC_NAME.put(futureMcName, item);
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

    /** @return what this stand-in stack really is */
    public ItemStack real(ItemStack stack) {
        return real(this.futureMcName, stack.getMetadata(), stack.getCount());
    }

    public String futureMcName() {
        return this.futureMcName;
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        ItemStack real = real(stack);
        return real.isEmpty() ? super.getItemStackDisplayName(stack) : real.getDisplayName();
    }

    @Override
    public void onUpdate(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (world.isRemote || !(entity instanceof EntityPlayer)) {
            return;
        }
        ItemStack real = real(stack);
        EntityPlayer player = (EntityPlayer) entity;
        if (!real.isEmpty() && player.inventory.getStackInSlot(slot) == stack) {
            player.inventory.setInventorySlotContents(slot, real);
        }
    }
}
