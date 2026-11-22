package com.exiledradio.villagerbackport.trade;

import com.exiledradio.villagerbackport.ModConfig;
import com.exiledradio.villagerbackport.compat.VillagerAccess;
import com.exiledradio.villagerbackport.data.VillagerTradeData;
import com.exiledradio.villagerbackport.network.NetworkHandler;
import com.exiledradio.villagerbackport.network.PacketTradeList;

import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.ContainerMerchant;
import net.minecraft.village.MerchantRecipe;
import net.minecraft.village.MerchantRecipeList;

/**
 * 26.3's trade screen that keeps up with the villager: "New villager trades, unlocked from a villager leveling up
 * their profession, will now update even if the player has the trading UI open."
 *
 * <p>26.3's {@code Villager.rewardTradeXp} levels the villager inside the trade that earns it
 * ({@code increaseMerchantCareer}); {@code updateTrades} adds the level's trades and, with a player trading, runs
 * {@code updateSpecialPrices}, which prices the offers for that player and sends them to the open screen
 * ({@code sendMerchantOffers}, with the new level and experience). {@link LevelGate} does the first part; this is the
 * rest: the new trades priced for the player as {@link PricingHandler} prices a screen being opened, then the list
 * and the level data sent again.
 */
public final class TradeScreen {

    private TradeScreen() {
    }

    /**
     * Prices the trades from {@code from} on for the villager's customer and sends that player the whole list and the
     * villager's level data. Does nothing without a customer whose open screen is this villager's.
     */
    static void refresh(EntityVillager villager, int from) {
        EntityPlayer customer = villager.getCustomer();
        MerchantRecipeList recipes = VillagerAccess.getBuyingList(villager);
        if (!(customer instanceof EntityPlayerMP) || !(customer.openContainer instanceof ContainerMerchant) || recipes == null) {
            return;
        }
        if (ModConfig.pricing.demandPricing) {
            int reputation = Gossip.reputation(villager, customer);
            for (int i = Math.max(0, from); i < recipes.size(); i++) {
                MerchantRecipe recipe = recipes.get(i);
                PriceEngine.applyPrice(villager, recipe, TradeKey.of(villager, recipe),
                        Gossip.specialPriceFor(reputation, PriceEngine.multiplierFor(recipe)), customer);
            }
        }
        EntityPlayerMP player = (EntityPlayerMP) customer;
        // A fake player (another mod's machine, a test) has no connection to send on.
        if (player.connection == null || player instanceof net.minecraftforge.common.util.FakePlayer) {
            return;
        }
        if (NetworkHandler.channel() != null) {
            NetworkHandler.channel().sendTo(new PacketTradeList(player.openContainer.windowId, recipes), player);
        }
        if (ModConfig.display.showVillagerLevel) {
            NetworkHandler.sendVillagerData(player,
                    VillagerTradeData.getXp(villager),
                    Math.max(VillagerLevel.MIN_LEVEL, VillagerTradeData.getAppliedLevel(villager)),
                    TradeXp.forList(villager, recipes),
                    TradeXp.usesList(recipes),
                    PriceEngine.baseList(villager, recipes));
        }
    }
}
