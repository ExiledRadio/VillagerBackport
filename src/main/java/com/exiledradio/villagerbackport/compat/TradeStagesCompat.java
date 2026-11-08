package com.exiledradio.villagerbackport.compat;

import com.exiledradio.villagerbackport.VillagerBackport;
import com.exiledradio.villagerbackport.restock.WorkSounds;
import com.exiledradio.villagerbackport.trade.TradeTables;

import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.village.MerchantRecipe;
import net.minecraft.village.MerchantRecipeList;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.VillagerRegistry;

import java.lang.reflect.Method;

/**
 * Lets TradeStages Legacy's rules see this mod's trades.
 *
 * <h2>What was missing</h2>
 * TradeStages Legacy locks trades behind game stages by matching a rule's profession, career and trade
 * level against labels it stores on each trade. It writes those labels while vanilla's
 * {@code populateBuyingList} runs. This mod builds its villagers' trades itself (1.14's tables, levels
 * unlocked by experience), so vanilla never runs for them and the labels were never written. Rules
 * that only name an item still worked, but every rule naming a career or a level matched nothing.
 *
 * <p>This writes the same labels onto this mod's trades: the Forge profession's registry name, the
 * career's name, and the level each trade unlocked at, which this mod already records. The labels go
 * on before the trading screen opens and again whenever a level unlocks, so a villager from an older
 * world gets them the first time somebody trades with it.
 *
 * <p>Through reflection, so this mod neither needs nor ships any of TradeStages Legacy's code. Its
 * labels are methods its own mixin adds to every {@code MerchantRecipe}; when it is not installed
 * none of this runs.
 */
public final class TradeStagesCompat {

    public static final String MOD_ID = "tradestages";

    private static final String WRAPPER = "tradestages.mixinwrapper.IMerchantRecipeWrapper";

    private static boolean resolved;
    private static Class<?> wrapper;
    private static Method getLevel;
    private static Method setLevel;
    private static Method setCareer;
    private static Method setProfession;

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (!event.getWorld().isRemote && event.getTarget() instanceof EntityVillager) {
            label((EntityVillager) event.getTarget());
        }
    }

    /** Writes TradeStages Legacy's labels onto every trade this villager has. */
    public static void label(EntityVillager villager) {
        if (!available()) {
            return;
        }

        MerchantRecipeList list = VillagerAccess.getBuyingList(villager);
        if (list == null || list.isEmpty()) {
            return;
        }

        VillagerRegistry.VillagerProfession profession = villager.getProfessionForge();
        VillagerRegistry.VillagerCareer career = WorkSounds.careerOf(villager);
        String professionName = profession == null || profession.getRegistryName() == null
                ? "minecraft:villager" : profession.getRegistryName().toString();
        String careerName = career == null ? "unknown" : career.getName();

        try {
            for (int i = 0; i < list.size(); i++) {
                MerchantRecipe recipe = list.get(i);
                if (!wrapper.isInstance(recipe)) {
                    continue;
                }

                setProfession.invoke(recipe, professionName);
                setCareer.invoke(recipe, careerName);

                // This mod's record of where each level ends. A list vanilla built has no record,
                // and TradeStages Legacy labeled those itself, so their levels are left alone.
                int tier = TradeTables.tierOf(villager, i);
                if (tier > 0) {
                    setLevel.invoke(recipe, tier);
                } else if ((Integer) getLevel.invoke(recipe) <= 0) {
                    setLevel.invoke(recipe, Math.max(1, VillagerAccess.getCareerLevel(villager)));
                }
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            VillagerBackport.LOGGER.warn("Could not label trades for TradeStages Legacy; its career and level"
                    + " rules will not match this villager's trades.", e);
            wrapper = null;
        }
    }

    private static boolean available() {
        if (!resolved) {
            resolved = true;

            if (Loader.isModLoaded(MOD_ID)) {
                try {
                    wrapper = Class.forName(WRAPPER);
                    getLevel = wrapper.getMethod("tsl$getTradeLevel");
                    setLevel = wrapper.getMethod("tsl$setTradeLevel", int.class);
                    setCareer = wrapper.getMethod("tsl$setCareer", String.class);
                    setProfession = wrapper.getMethod("tsl$setProfession", String.class);
                    VillagerBackport.LOGGER.info("TradeStages Legacy found: labeling this mod's trades for its rules.");
                } catch (ReflectiveOperationException e) {
                    VillagerBackport.LOGGER.warn("TradeStages Legacy is installed but its trade labels"
                            + " could not be found; its career and level rules will not see this mod's trades.");
                    wrapper = null;
                }
            }
        }

        return wrapper != null;
    }
}
