package com.exiledradio.villagerbackport.raid;

import com.exiledradio.villagerbackport.ModConfig;
import com.exiledradio.villagerbackport.VillagerBackport;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;
import net.minecraft.village.MerchantRecipe;
import net.minecraftforge.event.village.MerchantTradeOffersEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.common.registry.VillagerRegistry;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Where villagers find out about raids, raiders and heroes.
 *
 * <h2>Raiders by name</h2>
 * Several mods add pillagers and ravagers, and none of them share a class. 1.14 decides who is a
 * raider by a tag of entity types; this does the same by registry name, ignoring whose mod it is: a
 * {@code pillager} is a pillager whether it is Raids Backport's or anybody else's. 1.12.2's own
 * illagers carry their old names ({@code vindication_illager} and so on), so both spellings count.
 */
public final class Raids {

    /** Whose raids villagers live through - see {@code compat.raids}. */
    public enum Source {
        AUTO,
        OURS,
        RAIDS_BACKPORT,
        NONE
    }

    /** 1.14's {@code #raiders} tag, under every name the 1.12.2 mods use for its members. */
    private static final Set<String> RAIDER_NAMES = new HashSet<String>(Arrays.asList(
            "pillager", "ravager", "witch",
            "vindicator", "vindication_illager",
            "evoker", "evocation_illager",
            "illusioner", "illusion_illager"));

    /** Every Hero of the Village effect a mod might grant. The first one registered is used. */
    private static final ResourceLocation[] HERO_EFFECTS = {
            new ResourceLocation(VillagerBackport.MOD_ID, "hero_of_the_village"),
            new ResourceLocation(RaidsBackportRaids.MOD_ID, "hero_of_the_village")
    };


    private static RaidView view = RaidView.NONE;

    @Nullable
    private static RaidsBackportRaids raidsBackport;

    private static boolean ownRaids;

    /** Read once, the first time a hero is asked about; potions are all registered by then. */
    @Nullable
    private static Potion[] heroEffects;

    @Nullable
    private static Set<String> extraRaiders;

    /** Picks the raid system. Run in postInit, once every mod has loaded its classes. */
    public static void init() {
        Source source = ModConfig.compat.raids;
        boolean installed = Loader.isModLoaded(RaidsBackportRaids.MOD_ID);

        if ((source == Source.AUTO || source == Source.RAIDS_BACKPORT) && installed) {
            raidsBackport = RaidsBackportRaids.create();
            if (raidsBackport != null) {
                view = raidsBackport;
                VillagerBackport.LOGGER.info("Villagers will live through Raids Backport's raids.");
                return;
            }
        }

        // Ours whenever Raids Backport is not taking them: on its own, or asked for by name.
        if (source == Source.OURS || (source == Source.AUTO && raidsBackport == null)) {
            ownRaids = true;
            view = RaidManager.VIEW;
            VillagerBackport.LOGGER.info("Raids are this mod's own.");
        }
    }

    /** @return true when this mod runs the raids itself */
    public static boolean usingOwnRaids() {
        return ownRaids;
    }

    public static RaidView view() {
        return view;
    }

    /** @return true when Raids Backport is the raid system in use */
    public static boolean usingRaidsBackport() {
        return raidsBackport != null;
    }

    /**
     * @return true if this mob is one of 1.14's raiders, whatever mod it comes from
     *
     * <p>A raider in the tag sense: true for a vindicator out on its own as much as for one in a
     * raid, which is what 1.14's bell and villagers go by.
     */
    public static boolean isRaider(Entity entity) {
        ResourceLocation name = EntityList.getKey(entity);
        if (name == null) {
            return false;
        }

        String path = name.getPath().toLowerCase(Locale.ROOT);
        return RAIDER_NAMES.contains(path) || extras().contains(name.toString()) || view.inRaid(entity);
    }

    /**
     * @return how close this mob has to be to frighten a villager, or 0 if this class says nothing
     *
     * <p>1.14's distances for the two raiders 1.12.2 never had. The others keep their own entries
     * in {@code PanicWatch}.
     */
    public static double frightRange(Entity entity) {
        ResourceLocation name = EntityList.getKey(entity);
        if (name == null) {
            return 0.0D;
        }

        String path = name.getPath();
        if ("pillager".equals(path)) {
            return 15.0D;
        }
        if ("ravager".equals(path)) {
            return 12.0D;
        }
        // Witches are raiders but do not frighten villagers in 1.14; other mods' raiders do.
        if (extras().contains(name.toString())) {
            return 10.0D;
        }
        return 0.0D;
    }

    /** @return this player's Hero of the Village amplifier, or -1 if they are not a hero */
    public static int heroLevel(EntityPlayer player) {
        for (Potion effect : heroEffects()) {
            PotionEffect active = player.getActivePotionEffect(effect);
            if (active != null) {
                return active.getAmplifier();
            }
        }
        return -1;
    }

    /**
     * 1.14's hero discount: 30% off, plus 6.25% a level, of the trade's base price, and always at
     * least one.
     *
     * @return the discount as a negative number, or 0 for a player who is not a hero
     */
    public static int heroDiscount(EntityPlayer player, int basePrice) {
        if (!ModConfig.raids.heroDiscount) {
            return 0;
        }

        int level = heroLevel(player);
        if (level < 0) {
            return 0;
        }

        int discount = (int) Math.floor((0.3D + 0.0625D * level) * basePrice);
        return -Math.max(discount, 1);
    }

    /** @return true if another mod already gives heroes gifts for this career */
    static boolean othersGiveGiftsFor(@Nullable VillagerRegistry.VillagerCareer career) {
        return raidsBackport != null && career != null && raidsBackport.givesGiftsFor(career);
    }

    /**
     * Takes Raids Backport's own hero discount off the trades it has just priced.
     *
     * <h2>Why</h2>
     * It would otherwise be counted twice - once by it, once by this mod - and only its half would
     * be charged without being shown, because it draws its prices on the vanilla trade screen this
     * mod replaces. It also works the discount out from whatever the price is at that moment, which
     * by then already includes demand and reputation; 1.14 takes it from the base price.
     *
     * <p>Raids Backport sets its discount from this same event, which fires every time the trade
     * list is read - so it is cleared here every time too, last of all. Only when this mod is the
     * one pricing trades: with demand pricing off, Raids Backport's discount is the only one there is
     * and is left alone.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onTradeOffers(MerchantTradeOffersEvent event) {
        if (raidsBackport == null || event.getList() == null
                || !ModConfig.pricing.demandPricing || !ModConfig.raids.heroDiscount) {
            return;
        }

        for (MerchantRecipe recipe : event.getList()) {
            raidsBackport.clearDiscount(recipe);
        }
    }

    private static Potion[] heroEffects() {
        if (heroEffects == null) {
            java.util.List<Potion> found = new java.util.ArrayList<Potion>();
            for (ResourceLocation name : HERO_EFFECTS) {
                Potion potion = ForgeRegistries.POTIONS.getValue(name);
                if (potion != null) {
                    found.add(potion);
                }
            }
            heroEffects = found.toArray(new Potion[0]);
        }
        return heroEffects;
    }

    private static Set<String> extras() {
        if (extraRaiders == null) {
            Set<String> names = new HashSet<String>();
            for (String name : ModConfig.raids.extraRaiders) {
                if (name != null && !name.trim().isEmpty()) {
                    names.add(new ResourceLocation(name.trim()).toString());
                }
            }
            extraRaiders = names;
        }
        return extraRaiders;
    }

    /** Forgets the extra raider list, so a config change in game takes effect. */
    public static void reloadConfig() {
        extraRaiders = null;
    }
}
