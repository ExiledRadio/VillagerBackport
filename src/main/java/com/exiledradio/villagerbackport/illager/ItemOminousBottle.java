package com.exiledradio.villagerbackport.illager;

import com.exiledradio.villagerbackport.block.ModSounds;
import com.exiledradio.villagerbackport.raid.ModEffects;

import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumAction;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Current Java's ominous bottle: drink it for Bad Omen, I to V by the bottle.
 *
 * <p>The level is the item's metadata, 0 to 4, as Deeper Depths and Raids Backport store theirs.
 * Captains only drop it with {@code raids.captainDropsOminousBottle} on; with Deeper Depths
 * installed they drop its bottle instead, which gives its Bad Omen, which this mod's raids accept.
 */
public final class ItemOminousBottle extends Item {

    public static final int LEVELS = 5;

    /** 100 minutes, as from a captain. */
    private static final int DURATION = 120000;

    public ItemOminousBottle() {
        setMaxStackSize(64);
        setHasSubtypes(true);
    }

    @Override
    public int getMaxItemUseDuration(ItemStack stack) {
        return 32;
    }

    @Override
    public EnumAction getItemUseAction(ItemStack stack) {
        return EnumAction.DRINK;
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        player.setActiveHand(hand);
        return new ActionResult<ItemStack>(EnumActionResult.SUCCESS, player.getHeldItem(hand));
    }

    @Override
    public ItemStack onItemUseFinish(ItemStack stack, World world, EntityLivingBase entity) {
        if (!world.isRemote) {
            world.playSound(null, entity.posX, entity.posY, entity.posZ, ModSounds.ominousBottleDispose,
                    entity.getSoundCategory(), 1.0F, 1.0F);
            ModEffects.clearBadOmen(entity);
            entity.addPotionEffect(new PotionEffect(ModEffects.badOmen, DURATION, level(stack), false, false));
        }
        if (!(entity instanceof EntityPlayer) || !((EntityPlayer) entity).capabilities.isCreativeMode) {
            stack.shrink(1);
        }
        return stack;
    }

    public static int level(ItemStack stack) {
        return Math.max(0, Math.min(LEVELS - 1, stack.getMetadata()));
    }

    @Override
    public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> items) {
        if (isInCreativeTab(tab)) {
            for (int level = 0; level < LEVELS; level++) {
                items.add(new ItemStack(this, 1, level));
            }
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
        StringBuilder line = new StringBuilder(I18n.format("effect.villagerbackport.bad_omen"));
        int level = level(stack);
        if (level > 0) {
            // 1.12.2 names potion levels only up to III (potion.potency.2); its enchantment levels run to X.
            line.append(' ').append(I18n.format("enchantment.level." + (level + 1)));
        }
        line.append(" (").append(Potion.getPotionDurationString(new PotionEffect(ModEffects.badOmen, DURATION), 1.0F)).append(')');
        tooltip.add(TextFormatting.BLUE + line.toString());
    }
}
