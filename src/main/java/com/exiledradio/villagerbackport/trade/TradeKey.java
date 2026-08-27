package com.exiledradio.villagerbackport.trade;

import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import com.exiledradio.villagerbackport.compat.VillagerAccess;

import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.village.MerchantRecipe;
import net.minecraft.village.MerchantRecipeList;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * A stable identity for a {@link MerchantRecipe}.
 *
 * <p>1.14 never needs this: a {@code MerchantOffer} carries its own {@code demand} and
 * {@code specialPrice} fields, so per-trade state lives on the trade. 1.12.2's
 * {@link MerchantRecipe} has nowhere to put extra state and no id to key it by, so anything this
 * mod wants to remember about a single trade has to be stored beside the villager under a key we
 * derive from the trade's own contents.
 *
 * <p><b>Stack counts are deliberately excluded from the key.</b> This is the subtle part. Demand
 * pricing works by rewriting the count on the buy stack, so if the count were part of the identity,
 * the first price adjustment would change the trade's key, the stored state would be orphaned, and
 * the next adjustment would start over from a base it had already inflated. Prices would compound
 * every restock and run away. Keying on item identity alone keeps a trade recognisable across every
 * price change we make to it.
 *
 * <p>The sell stack's NBT <em>is</em> included, because trades that differ only by NBT are common
 * and genuinely distinct - a librarian's Sharpness book and Mending book are the same item and
 * metadata, and RLCraft Villager Tomes generates whole trade lists that differ by nothing else.
 * Buy-side NBT is ignored: vanilla and mod trades overwhelmingly buy plain items, and including it
 * would make the key sensitive to incidental tags on the stack a mod happened to construct.
 */
public final class TradeKey {

    /**
     * Keys already computed, so a trade is only ever hashed once.
     *
     * <p>Building a key means serialising the sell stack's NBT, which for an enchanted book is not
     * cheap. This is called for every trade a villager has, on every poll - and a villager with a
     * customer is polled five times a second so its experience bar keeps up with the trades being
     * made. Recomputing throughout a bulk trade was enough to be felt as a stutter.
     *
     * <p>Weakly keyed, so entries disappear when the villager's trade list drops the recipe. Keys
     * are derived from item identity only, which no part of this mod changes, so a cached entry
     * stays correct for as long as the recipe object lives - price adjustments rewrite stack counts
     * and restocks rewrite use counts, and the key deliberately depends on neither.
     *
     * <p>Only ever touched from the server thread: the two callers are the villager tick and the
     * interact handler, both server-side.
     */
    private static final Map<MerchantRecipe, String> CACHE = new WeakHashMap<MerchantRecipe, String>();

    private TradeKey() {
    }

    /**
     * Builds the storage key for a trade, or returns the one already computed for it.
     *
     * @return a string safe to use as an NBT compound key
     */
    public static String of(EntityVillager villager, MerchantRecipe recipe) {
        String cached = CACHE.get(recipe);
        if (cached != null) {
            return cached;
        }

        String base = compute(recipe);
        String key = base + ordinal(villager, recipe, base);

        CACHE.put(recipe, key);
        return key;
    }

    /**
     * @return a suffix separating trades that would otherwise share a key, or nothing for the first
     *
     * <h2>Why identical trades need separating</h2>
     * The key is built from item identity and deliberately not from counts, because it has to
     * survive the price changing - that is the whole point of it. The cost is that a villager
     * offering the same items twice produces one key for both, and the two then share a base price
     * and a demand value. Vanilla re-rolls prices when it unlocks a tier, so the pair drift apart
     * and one of them ends up priced from the other's record.
     *
     * <p>Counting how many earlier trades in the villager's own list already carry this key
     * separates them without reintroducing the price into the key. Order is stable because vanilla
     * only ever appends a tier to the end of the list.
     *
     * <p>The first of a kind gets no suffix at all, so every key a world already has keeps working
     * and nothing needs migrating.
     */
    private static String ordinal(EntityVillager villager, MerchantRecipe recipe, String base) {
        MerchantRecipeList recipes = VillagerAccess.getBuyingList(villager);
        if (recipes == null) {
            return "";
        }

        int seen = 0;
        for (MerchantRecipe other : recipes) {
            if (other == recipe) {
                break;
            }
            if (base.equals(compute(other))) {
                seen++;
            }
        }

        return seen == 0 ? "" : "." + seen;
    }

    private static String compute(MerchantRecipe recipe) {
        StringBuilder sb = new StringBuilder();
        appendStack(sb, recipe.getItemToBuy(), false);
        sb.append('|');
        appendStack(sb, recipe.getSecondItemToBuy(), false);
        sb.append('|');
        appendStack(sb, recipe.getItemToSell(), true);

        // The assembled string can be long - an enchanted book's NBT is not short - and it becomes
        // an NBT key stored once per trade per villager. Hashing keeps the saved data small and
        // bounded. A collision would mean two trades on the same villager sharing a demand value,
        // which is a mispriced trade rather than a crash; at 64 bits across the handful of trades
        // one villager has, it is not a realistic concern.
        return Long.toHexString(hash64(sb.toString()));
    }

    private static void appendStack(StringBuilder sb, ItemStack stack, boolean includeNbt) {
        if (stack.isEmpty()) {
            sb.append("empty");
            return;
        }

        ResourceLocation id = stack.getItem().getRegistryName();
        sb.append(id == null ? "unknown" : id.toString());
        sb.append('#').append(stack.getMetadata());

        if (includeNbt && stack.hasTagCompound()) {
            sb.append('@').append(stack.getTagCompound());
        }
    }

    /**
     * FNV-1a. Chosen over {@link String#hashCode()} because that is only 32 bits, which across the
     * trades of many villagers in a long-lived world is inside the range where collisions start to
     * be likely rather than theoretical.
     */
    private static long hash64(String s) {
        long hash = 0xcbf29ce484222325L;
        for (int i = 0; i < s.length(); i++) {
            hash ^= s.charAt(i);
            hash *= 0x100000001b3L;
        }
        return hash;
    }
}
