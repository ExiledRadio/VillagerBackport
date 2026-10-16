package com.exiledradio.villagerbackport.trade;

import com.exiledradio.villagerbackport.ModConfig;
import com.exiledradio.villagerbackport.compat.VillagerAccess;
import com.exiledradio.villagerbackport.home.HomeSite;
import com.exiledradio.villagerbackport.home.PanicWatch;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.village.MerchantRecipe;
import net.minecraft.village.MerchantRecipeList;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * A villager holding up what it would give you for the item in your hand.
 *
 * <h2>The game's rule</h2>
 * A player within four blocks that the villager can see becomes the villager's focus, and it looks at
 * them. While that player holds an item one of the villager's trades takes, the villager holds up
 * what that trade gives - an emerald to a player holding wheat, if it buys wheat. With more than one
 * such trade it cycles through them every two seconds. It keeps it up for as long as 45 seconds, and
 * stops once the player walks further than about four blocks off or puts away anything it wants.
 *
 * <p>Not during the gathering or sleeping parts of the day, not while being traded with, and only
 * trades that still have stock count. The item in its hand is for show: it is never dropped on death.
 *
 * <p>1.12.2 has none of this, and its villager model does not draw a held item at all - the client
 * half is {@code LayerVillagerHeldItem}.
 */
public final class ShowWares {

    /** The game's reach for picking a player to look at, four blocks. */
    private static final double FOCUS_RANGE_SQ = 16.0D;

    /** How far the player can drift before the villager stops showing, the game's 17 squared. */
    private static final double KEEP_RANGE_SQ = 17.0D;

    /** How long it shows something it has found a trade for, and how long it waits otherwise. */
    private static final int SHOWING_TICKS = 900;
    private static final int IDLE_TICKS = 40;

    /** Ticks between offers when there are several. */
    private static final int CYCLE_TICKS = 40;

    private static final Map<EntityVillager, Showing> SHOWING = new WeakHashMap<EntityVillager, Showing>();

    @SubscribeEvent
    public void onLivingUpdate(LivingEvent.LivingUpdateEvent event) {
        if (!(event.getEntity() instanceof EntityVillager) || event.getEntity().world.isRemote) {
            return;
        }

        EntityVillager villager = (EntityVillager) event.getEntity();
        Showing showing = SHOWING.get(villager);

        if (!mayShow(villager)) {
            if (showing != null || !villager.getHeldItemMainhand().isEmpty()) {
                stop(villager);
            }
            return;
        }

        if (showing == null) {
            EntityPlayer player = playerToFocusOn(villager);
            if (player == null) {
                // Nobody to show anything to. Also clears an item left in the hand by a save made
                // mid-show, which the world remembers and this does not.
                if (!villager.getHeldItemMainhand().isEmpty()) {
                    stop(villager);
                }
                return;
            }
            showing = new Showing(player);
            SHOWING.put(villager, showing);
        }

        EntityPlayer player = showing.player;
        if (!player.isEntityAlive() || player.world != villager.world
                || player.getDistanceSq(villager) > KEEP_RANGE_SQ || showing.remaining <= 0) {
            stop(villager);
            return;
        }

        villager.getLookHelper().setLookPositionWithEntity(player, 30.0F, 30.0F);
        showing.update(villager, player.getHeldItemMainhand());
        showing.remaining--;
    }

    private static boolean mayShow(EntityVillager villager) {
        return ModConfig.display.showTradeItems
                && villager.isEntityAlive()
                && !villager.isChild()
                && villager.getCustomer() == null
                && !HomeSite.isSleeping(villager)
                && !com.exiledradio.villagerbackport.home.EntityAISleep.isBedtime(villager)
                && !PanicWatch.isPanicking(villager);
    }

    @Nullable
    private static EntityPlayer playerToFocusOn(EntityVillager villager) {
        EntityPlayer nearest = villager.world.getClosestPlayerToEntity(villager, Math.sqrt(FOCUS_RANGE_SQ));
        if (nearest == null || nearest.isSpectator() || !villager.canEntityBeSeen(nearest)) {
            return null;
        }
        return nearest;
    }

    private static void stop(EntityVillager villager) {
        SHOWING.remove(villager);
        villager.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, ItemStack.EMPTY);
    }

    private static final class Showing {
        final EntityPlayer player;
        ItemStack lastHeld = ItemStack.EMPTY;
        boolean seen;
        final List<ItemStack> wares = new ArrayList<ItemStack>();
        int remaining = IDLE_TICKS;
        int cycle;
        int index;

        Showing(EntityPlayer player) {
            this.player = player;
        }

        void update(EntityVillager villager, ItemStack held) {
            if (!this.seen || !ItemStack.areItemsEqual(this.lastHeld, held)) {
                this.seen = true;
                this.lastHeld = held.copy();
                this.wares.clear();
                this.cycle = 0;
                this.index = 0;

                if (!held.isEmpty()) {
                    findWares(villager, held);
                    if (!this.wares.isEmpty()) {
                        this.remaining = SHOWING_TICKS;
                        hold(villager, this.wares.get(0));
                    }
                }
            }

            if (this.wares.isEmpty()) {
                villager.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, ItemStack.EMPTY);
                this.remaining = Math.min(this.remaining, IDLE_TICKS);
                return;
            }

            if (this.wares.size() >= 2 && ++this.cycle >= CYCLE_TICKS) {
                this.cycle = 0;
                this.index = (this.index + 1) % this.wares.size();
                hold(villager, this.wares.get(this.index));
            }
        }

        private void findWares(EntityVillager villager, ItemStack held) {
            MerchantRecipeList recipes = VillagerAccess.getBuyingList(villager);
            if (recipes == null) {
                return;
            }

            for (MerchantRecipe recipe : recipes) {
                if (recipe.isRecipeDisabled()) {
                    continue;
                }
                if (ItemStack.areItemsEqual(held, recipe.getItemToBuy())
                        || ItemStack.areItemsEqual(held, recipe.getSecondItemToBuy())) {
                    this.wares.add(recipe.getItemToSell().copy());
                }
            }
        }

        private static void hold(EntityVillager villager, ItemStack stack) {
            villager.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, stack.copy());
            // On show only: the game never drops a trade item a villager is holding up.
            villager.setDropChance(EntityEquipmentSlot.MAINHAND, 0.0F);
        }
    }
}
