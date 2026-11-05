package com.exiledradio.villagerbackport.flora;

import com.exiledradio.villagerbackport.VillagerBackport;

import net.minecraft.block.BlockCauldron;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.passive.EntityWolf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * The two dye-related clicks 1.12.2 hard-codes to its own items.
 *
 * <h2>Wolf collars</h2>
 * A tamed wolf's {@code processInteract} recolors its collar only for {@code Items.DYE}; anything
 * else in its owner's hand makes it sit or stand. So the new dyes are caught first: an owner using a
 * dye of a different color recolors the collar and spends the dye (not in creative), exactly as with
 * 1.12.2's dyes. The same color falls through to the wolf, which sits, as it does for vanilla dye.
 *
 * <h2>Washing leather horse armor</h2>
 * A cauldron with water in it takes the dye out of leather armor and lowers the water a level. 1.12.2
 * does that only for {@code ItemArmor} made of leather; this does the same for leather horse armor,
 * as 1.14's cauldron does.
 */
@Mod.EventBusSubscriber(modid = VillagerBackport.MOD_ID)
public final class DyeInteractions {

    private DyeInteractions() {
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!FloraContent.enabled() || !(event.getTarget() instanceof EntityWolf)) {
            return;
        }

        ItemStack held = event.getItemStack();
        if (!(held.getItem() instanceof ItemModernDye)) {
            return;
        }

        EntityWolf wolf = (EntityWolf) event.getTarget();
        EntityPlayer player = event.getEntityPlayer();
        EnumDyeColor color = ((ItemModernDye) held.getItem()).getColor();
        // By id rather than isOwner, which looks the owner up in the world and fails for one not in it.
        if (!wolf.isTamed() || !player.getUniqueID().equals(wolf.getOwnerId()) || wolf.getCollarColor() == color) {
            return;
        }

        if (!wolf.world.isRemote) {
            wolf.setCollarColor(color);
            if (!player.capabilities.isCreativeMode) {
                held.shrink(1);
            }
        }
        event.setCanceled(true);
        event.setCancellationResult(EnumActionResult.SUCCESS);
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        ItemStack held = event.getItemStack();
        if (!FloraContent.enabled() || !(held.getItem() instanceof ItemLeatherHorseArmor)
                || !ItemLeatherHorseArmor.hasColor(held)) {
            return;
        }

        World world = event.getWorld();
        IBlockState state = world.getBlockState(event.getPos());
        if (state.getBlock() != Blocks.CAULDRON || state.getValue(BlockCauldron.LEVEL) <= 0) {
            return;
        }

        if (!world.isRemote) {
            ItemLeatherHorseArmor.removeColor(held);
            Blocks.CAULDRON.setWaterLevel(world, event.getPos(), state, state.getValue(BlockCauldron.LEVEL) - 1);
            event.getEntityPlayer().addStat(net.minecraft.stats.StatList.ARMOR_CLEANED);
        }
        event.setCanceled(true);
        event.setCancellationResult(EnumActionResult.SUCCESS);
    }
}
