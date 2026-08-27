package com.exiledradio.villagerbackport.compat;

import com.exiledradio.villagerbackport.VillagerBackport;

import net.minecraft.entity.passive.EntityVillager;
import net.minecraftforge.fml.common.registry.VillagerRegistry;
import net.minecraft.village.MerchantRecipe;
import net.minecraft.village.MerchantRecipeList;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import javax.annotation.Nullable;
import java.lang.reflect.Field;

/**
 * Reflective access to the parts of {@link EntityVillager} that vanilla keeps private.
 *
 * <p>1.12.2 exposes almost nothing about a villager's trading state. {@code buyingList},
 * {@code careerLevel}, {@code careerId} and {@code timeUntilReset} are all private with no
 * accessors, so any mod that wants to reason about restocking has to either coremod the class
 * or reach in reflectively. Reflection is the quieter of the two: it reads and writes the same
 * fields vanilla does without changing the bytecode other mods are patching.
 *
 * <p>Every lookup is resolved once during {@link #init()} and cached. If a lookup fails - a
 * coremod renamed a field, a future Forge build moved something - the field stays null and
 * {@link #isAvailable()} reports false, which makes the whole mod stand down rather than throw
 * once per villager per tick. Failing silent-but-disabled is the right call here because the
 * mod is an enhancement: a pack that loads without restocking is a pack that still works.
 */
public final class VillagerAccess {

    /** {@code private MerchantRecipeList buyingList} - the villager's live trade list. */
    @Nullable
    private static Field buyingList;

    /**
     * {@code private int timeUntilReset} - vanilla's countdown to its own restock. We read it so
     * we can stay out of the way while vanilla is already mid-restock, and never write it.
     */
    @Nullable
    private static Field timeUntilReset;

    /**
     * {@code private int careerLevel} - which tier of trades the villager has unlocked. Read to
     * work out how much experience a trade should be worth; never written.
     */
    @Nullable
    private static Field careerLevel;

    /** {@code private int careerId} - which career within the profession the villager took. */
    @Nullable
    private static Field careerId;

    /**
     * {@code private boolean needsInitilization} (vanilla's spelling) - vanilla's flag for "add the
     * next tier of trades when the reset countdown expires". Cleared so tiers unlock on level-up
     * rather than on restock.
     */
    @Nullable
    private static Field needsInitilization;

    /** {@code private void populateBuyingList()} - appends the next career tier's trades. */
    @Nullable
    private static java.lang.reflect.Method populateBuyingList;

    /** {@code private List<VillagerCareer> careers} on VillagerProfession. */
    @Nullable
    private static Field careersField;

    /** Set once {@link #init()} has run, whatever the outcome, so we only probe once. */
    private static boolean initialised;

    /** True when every field we need resolved. When false the mod disables itself. */
    private static boolean available;

    private VillagerAccess() {
    }

    /**
     * Resolves the field handles. Safe to call more than once; only the first call does work.
     *
     * <p>Both MCP names and SRG names are attempted. In a dev workspace fields carry their MCP
     * names ({@code buyingList}); in a shipped jar running against a reobfuscated client they
     * carry SRG names ({@code field_70963_i}). ReflectionHelper takes the candidates in order and
     * returns the first that resolves, which is what makes one build work in both environments.
     */
    public static void init() {
        if (initialised) {
            return;
        }
        initialised = true;

        buyingList = find(EntityVillager.class, "buyingList", "field_70963_i");
        timeUntilReset = find(EntityVillager.class, "timeUntilReset", "field_70961_j");
        careerLevel = find(EntityVillager.class, "careerLevel", "field_175562_bw");
        careerId = find(EntityVillager.class, "careerId", "field_175563_bv");
        needsInitilization = find(EntityVillager.class, "needsInitilization", "field_70959_by");
        populateBuyingList = findMethod(EntityVillager.class, "populateBuyingList", "func_175554_cu");
        careersField = find(VillagerRegistry.VillagerProfession.class, "careers", "careers");

        available = buyingList != null && timeUntilReset != null && careerLevel != null
                && careerId != null && needsInitilization != null && populateBuyingList != null;

        if (!available) {
            VillagerBackport.LOGGER.error(
                    "Could not resolve EntityVillager's private trading fields. Villager Trading "
                            + "Revamp is standing down - villagers will use vanilla 1.12.2 restock "
                            + "behaviour. This usually means another coremod has rewritten "
                            + "EntityVillager.");
        }
    }

    /** @return true when reflection succeeded and the mod may safely operate. */
    public static boolean isAvailable() {
        return available;
    }

    @Nullable
    private static Field find(Class<?> owner, String... names) {
        try {
            Field f = ReflectionHelper.findField(owner, names);
            f.setAccessible(true);
            return f;
        } catch (ReflectionHelper.UnableToFindFieldException e) {
            VillagerBackport.LOGGER.error("Unable to find field {} on {}", names[0], owner.getName(), e);
            return null;
        }
    }

    /**
     * @return the villager's live trade list, or null if it has not been built yet.
     *
     * <p>Deliberately does <em>not</em> call {@code populateBuyingList()} to force one into
     * existence. A villager only builds its list the first time a player opens its GUI, and
     * building it early would hand every villager in every loaded chunk a trade list it may
     * never need - both a memory cost and a behaviour change other mods could notice. A villager
     * with no list has nothing to restock, so we skip it and check again next time.
     */
    @Nullable
    public static MerchantRecipeList getBuyingList(EntityVillager villager) {
        if (buyingList == null) {
            return null;
        }
        try {
            return (MerchantRecipeList) buyingList.get(villager);
        } catch (IllegalAccessException e) {
            return null;
        }
    }

    /**
     * @return true when vanilla's own restock countdown is running.
     *
     * <p>Vanilla arms this for 40 ticks after a lucky trade and then re-enables disabled recipes
     * and levels the villager's career up. We hold off while it runs so the two systems never
     * restock the same villager on the same tick.
     */
    public static boolean isVanillaResetPending(EntityVillager villager) {
        if (timeUntilReset == null) {
            return false;
        }
        try {
            return timeUntilReset.getInt(villager) > 0;
        } catch (IllegalAccessException e) {
            return false;
        }
    }

    @Nullable
    private static java.lang.reflect.Method findMethod(Class<?> owner, String... names) {
        try {
            java.lang.reflect.Method m = ReflectionHelper.findMethod(owner, names[0], names[1]);
            m.setAccessible(true);
            return m;
        } catch (RuntimeException e) {
            VillagerBackport.LOGGER.error("Unable to find method {} on {}", names[0], owner.getName(), e);
            return null;
        }
    }

    /**
     * Clears vanilla's "unlock the next tier on the next restock" flag.
     *
     * <p>This is what decouples trade unlocking from restocking. Vanilla arms the flag inside
     * {@code useRecipe} and, forty ticks later, calls {@code populateBuyingList()} - which bumps the
     * career level and appends a whole new tier of trades. That is 1.12.2's entire progression
     * model, and it runs on trade volume alone, so a villager gains tiers simply by being traded
     * with. With a 1.14-style experience level also in play the two fight: the villager unlocks
     * master trades while still reading as Novice.
     *
     * <p>Clearing the flag leaves the rest of vanilla's reset intact - the regeneration effect still
     * plays - while the tier unlock moves to {@link #unlockTier}, driven by experience.
     *
     * @return true if the flag was set and has now been cleared
     */
    public static boolean clearPendingTierUnlock(EntityVillager villager) {
        if (needsInitilization == null) {
            return false;
        }
        try {
            if (!needsInitilization.getBoolean(villager)) {
                return false;
            }
            needsInitilization.setBoolean(villager, false);
            return true;
        } catch (IllegalAccessException e) {
            return false;
        }
    }

    /**
     * Stops vanilla's post-trade reset from firing at all.
     *
     * <p>1.12.2 and 1.14 share the same reset block - count down, then unlock a tier and apply a
     * regeneration effect - but arm it on completely different conditions. 1.14 arms it only when
     * the villager can actually level up, which is why the regeneration particles read as a
     * level-up cue there. 1.12.2 arms it on an ordinary trade, so the same particles fire after
     * trades that changed nothing.
     *
     * <p>Zeroing the countdown skips the whole block, because vanilla guards it with
     * {@code timeUntilReset > 0}. The flag is cleared too, so nothing is left armed if a tick is
     * ever missed. The level-up cue is then applied deliberately, only on a real level-up.
     */
    public static void cancelPendingReset(EntityVillager villager) {
        clearPendingTierUnlock(villager);

        if (timeUntilReset == null) {
            return;
        }
        try {
            if (timeUntilReset.getInt(villager) > 0) {
                timeUntilReset.setInt(villager, 0);
            }
        } catch (IllegalAccessException e) {
            // Nothing useful to do; the reset simply runs as vanilla intended.
        }
    }

    /**
     * Appends the trades for one career tier, as a level-up would in 1.14.
     *
     * <p>Works by setting the career level to one below the target and invoking vanilla's own
     * {@code populateBuyingList()}, which increments it and then appends
     * {@code getCareer(id).getTrades(careerLevel - 1)}. Going through vanilla's method rather than
     * building trades directly means careers registered by other mods are picked up exactly as they
     * intend, with no knowledge of them needed here.
     *
     * <p>A tier of 1 is passed through as a career level of 0, which sends vanilla down its
     * first-time branch: it picks a career and populates tier one. That is precisely what should
     * happen for a villager that has never traded.
     *
     * @param tier the tier to unlock, 1 to 5
     */
    public static void unlockTier(EntityVillager villager, int tier) {
        if (populateBuyingList == null || careerLevel == null) {
            return;
        }
        try {
            careerLevel.setInt(villager, Math.max(0, tier - 1));
            populateBuyingList.invoke(villager);
        } catch (Exception e) {
            VillagerBackport.LOGGER.warn("Failed to unlock trade tier {} for a villager", tier, e);
        }
    }

    /**
     * @return the villager's career id, or 0 if it cannot be read.
     *
     * <p>Identifies which career within a profession the villager took - a librarian versus a
     * cartographer, say. Stored one-based, with 0 meaning "not yet assigned".
     */
    public static int getCareerId(EntityVillager villager) {
        if (careerId == null) {
            return 0;
        }
        try {
            return careerId.getInt(villager);
        } catch (IllegalAccessException e) {
            return 0;
        }
    }

    /**
     * @return the villager's career level counter, or 1 if it cannot be read.
     *
     * <p><b>This is not a tier.</b> {@code populateBuyingList()} increments it on every call and
     * only then asks the career for that level's trades - and the call happens on every restock,
     * whether or not any trades remain to unlock. On a long-lived villager it climbs far past the
     * number of tiers its career actually defines, so it says how many times a villager has
     * restocked rather than how far it has progressed. Deriving a trade's tier from it produces
     * wildly inflated values; use the trade's position in the list instead, which is what
     * {@link com.exiledradio.villagerbackport.trade.TradeTier} does.
     */
    public static int getCareerLevel(EntityVillager villager) {
        if (careerLevel == null) {
            return 1;
        }
        try {
            return Math.max(1, careerLevel.getInt(villager));
        } catch (IllegalAccessException e) {
            return 1;
        }
    }

    /**
     * Sets which career within its profession a villager follows.
     *
     * <p>Zero means none, which 1.12.2 uses for a villager that has not built a trade list yet and
     * which this mod reuses to mean unemployed - there is no equivalent of 1.14's NONE profession to
     * borrow, and a career of zero already renders and behaves as "no job".
     */
    public static void setCareerId(EntityVillager villager, int id) {
        if (careerId == null) {
            return;
        }
        try {
            careerId.setInt(villager, id);
        } catch (IllegalAccessException e) {
            VillagerBackport.LOGGER.warn("Could not set a villager's career.", e);
        }
    }

    /** Sets the career level counter, used to reset progression when a villager changes job. */
    public static void setCareerLevel(EntityVillager villager, int level) {
        if (careerLevel == null) {
            return;
        }
        try {
            careerLevel.setInt(villager, level);
        } catch (IllegalAccessException e) {
            VillagerBackport.LOGGER.warn("Could not set a villager's career level.", e);
        }
    }

    /**
     * Throws away a villager's trades so they are rebuilt for its new job.
     *
     * <p>Setting the list to null rather than emptying it is deliberate: vanilla treats null as "not
     * built yet" and repopulates on the next request, whereas an empty list is taken at face value
     * and the villager would offer nothing forever.
     */
    public static void clearBuyingList(EntityVillager villager) {
        if (buyingList == null) {
            return;
        }
        try {
            buyingList.set(villager, null);
        } catch (IllegalAccessException e) {
            VillagerBackport.LOGGER.warn("Could not clear a villager's trades.", e);
        }
    }

    /** Replaces a villager's trade list outright. */
    public static void setBuyingList(EntityVillager villager, MerchantRecipeList list) {
        if (buyingList == null) {
            return;
        }
        try {
            buyingList.set(villager, list);
        } catch (IllegalAccessException e) {
            VillagerBackport.LOGGER.warn("Could not set a villager's trades.", e);
        }
    }

    /**
     * @return every career a profession offers, in registration order.
     *
     * <p>The list is private and {@code getCareer} is no substitute: given an id it does not know,
     * it returns the first career rather than failing, so probing it cannot discover where the list
     * ends. Reading the list directly is the only way to enumerate them or to find one by name.
     */
    @SuppressWarnings("unchecked")
    public static java.util.List<VillagerRegistry.VillagerCareer> getCareers(
            VillagerRegistry.VillagerProfession profession) {
        if (careersField == null || profession == null) {
            return java.util.Collections.emptyList();
        }
        try {
            java.util.List<VillagerRegistry.VillagerCareer> careers =
                    (java.util.List<VillagerRegistry.VillagerCareer>) careersField.get(profession);
            return careers == null ? java.util.Collections.<VillagerRegistry.VillagerCareer>emptyList() : careers;
        } catch (IllegalAccessException e) {
            return java.util.Collections.emptyList();
        }
    }

    /**
     * Clears a recipe's use count, the 1.12.2 equivalent of 1.14's {@code MerchantOffer.resetUses}.
     *
     * <p>{@link MerchantRecipe} tracks uses in a private {@code toolUses} field and offers
     * {@code incrementToolUses()} but no way to decrease it - vanilla never needed one, because
     * vanilla restocks by <em>raising</em> maxTradeUses instead. Raising it is the wrong shape
     * here: it makes the cap drift upward every restock, so a villager traded with for long
     * enough ends up with effectively unlimited stock. Instead we round-trip the recipe through
     * its own NBT, which is public on both sides, and zero the use count on the way through.
     * That leaves maxTradeUses exactly where the recipe's author - vanilla or another mod - set
     * it, which is what keeps trades from inflating.
     *
     * @return true if the recipe was reset.
     */
    public static boolean resetUses(MerchantRecipe recipe) {
        try {
            net.minecraft.nbt.NBTTagCompound tag = recipe.writeToTags();
            tag.setInteger("uses", 0);
            recipe.readFromTags(tag);
            return true;
        } catch (RuntimeException e) {
            VillagerBackport.LOGGER.warn("Failed to reset uses on a merchant recipe", e);
            return false;
        }
    }
}
