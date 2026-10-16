package com.exiledradio.villagerbackport.trade;

import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.List;

/**
 * The experience a player gets for trading.
 *
 * <p>The game gives 3 to 6 experience per trade, and 5 more on the trade that earns the villager a
 * level. 1.12.2 gives the same 3 to 6 but adds its 5 on a trade's first use after each restock, and
 * on one trade in five at random, which has nothing to do with levels here.
 *
 * <p>The orb is 1.12.2's, so it is left to appear and has its value set again as it joins the world:
 * 3 to 6, the bonus taken out. The bonus is paid separately, by {@link #levelUpBonus}, at the moment
 * a trade is found to have earned a level.
 */
public final class TradeExperience {

    /** The game's bonus for the trade that levels a villager up. */
    private static final int LEVEL_UP_BONUS = 5;

    @SubscribeEvent
    public void onJoin(EntityJoinWorldEvent event) {
        if (event.getWorld().isRemote || !(event.getEntity() instanceof EntityXPOrb)) {
            return;
        }

        EntityXPOrb orb = (EntityXPOrb) event.getEntity();

        // 1.12.2 drops a trade's orb exactly half a block above a villager that is mid-trade. Nothing
        // else puts an orb in that spot at that moment.
        AxisAlignedBB at = new AxisAlignedBB(orb.posX, orb.posY - 0.5D, orb.posZ,
                orb.posX, orb.posY - 0.5D, orb.posZ).grow(0.01D);
        List<EntityVillager> villagers = event.getWorld().getEntitiesWithinAABB(EntityVillager.class, at);

        for (EntityVillager villager : villagers) {
            if (villager.getCustomer() != null && Math.abs(villager.posY + 0.5D - orb.posY) < 0.01D) {
                orb.xpValue = 3 + villager.getRNG().nextInt(4);
                return;
            }
        }
    }

    /** Pays the level-up bonus for the trade that just earned one. */
    static void levelUpBonus(EntityVillager villager) {
        villager.world.spawnEntity(new EntityXPOrb(villager.world,
                villager.posX, villager.posY + 0.5D, villager.posZ, LEVEL_UP_BONUS));
    }
}
