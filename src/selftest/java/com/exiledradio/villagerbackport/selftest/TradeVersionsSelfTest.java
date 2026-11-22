package com.exiledradio.villagerbackport.selftest;

import com.exiledradio.backportcore.presentation.GameVersion;
import com.exiledradio.villagerbackport.trade.ModernTrades;
import com.exiledradio.villagerbackport.trade.TradeVersion;
import com.exiledradio.villagerbackport.trader.TraderTrades;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.village.MerchantRecipe;
import net.minecraft.village.MerchantRecipeList;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * {@code -Pselftest=tradeversions}: the trade tables of each version, as the newest installed backport picks them
 * ({@link TradeVersion}), each checked at its version with its rows' items (or their {@code fake-*} stand-ins).
 */
public final class TradeVersionsSelfTest implements SelfTest {

    @Override
    public String name() {
        return "tradeversions";
    }

    @Override
    public void run(Check c) {
        c.eq(TradeVersion.V1_14, TradeVersion.of(GameVersion.parse("1.16.4")), "Nether Update's 1.16.4: 1.14's tables");
        c.eq(TradeVersion.V1_17, TradeVersion.of(GameVersion.parse("1.18.2")), "Caves & Cliffs' 1.18.2: 1.17's");
        c.eq(TradeVersion.V1_19, TradeVersion.of(GameVersion.parse("1.19.4")), "Wild Update's 1.19.4: 1.19's");
        c.eq(TradeVersion.V1_20, TradeVersion.of(GameVersion.parse("1.20.5")), "Trails & Tales' 1.20.5: 1.20's");
        c.eq(TradeVersion.V1_21, TradeVersion.of(GameVersion.parse("1.21.11")), "Tricky Trials' 1.21.11: 1.21's");
        c.eq(TradeVersion.V26, TradeVersion.of(GameVersion.parse("26.3")), "Game Drops' 26.3: 26.1's");
        c.eq(TradeVersion.V1_14, TradeVersion.current(), "this mod alone (dev environment): 1.14's");
        try {
            for (TradeVersion v : TradeVersion.values()) {
                TradeVersion.override(v);
                check(c, v);
            }
        } finally {
            TradeVersion.override(null);
        }
    }

    private static void check(Check c, TradeVersion v) {
        String at = " (" + v + ")";
        boolean v17 = v.atLeast(TradeVersion.V1_17);
        boolean v21 = v.atLeast(TradeVersion.V1_21);
        boolean v26 = v.atLeast(TradeVersion.V26);

        // Librarian 1: 1.14's three bookshelves for 6 emeralds; 1.17's one for 9.
        MerchantRecipe shelf = find(career("minecraft:librarian/librarian", 1), Item.getItemFromBlock(Blocks.BOOKSHELF));
        c.ok(shelf != null && (v17 ? shelf.getItemToSell().getCount() == 1 && shelf.getItemToBuy().getCount() == 9
                : shelf.getItemToSell().getCount() == 3 && shelf.getItemToBuy().getCount() == 6), "librarian's bookshelf" + at);
        // Librarian 5: the name tag up to 1.21.11; 26.1's candles, none without a candle mod.
        c.eq(!v26, find(career("minecraft:librarian/librarian", 5), Items.NAME_TAG) != null, "master librarian's name tag" + at);
        // Farmer 4: cake for 1 emerald, 3 from 26.1.
        MerchantRecipe cake = find(career("minecraft:farmer/farmer", 4), Items.CAKE);
        c.ok(cake != null && cake.getItemToBuy().getCount() == (v26 ? 3 : 1), "farmer's cake" + at);
        // Cartographer 1 (current Java's table from 1.21): paper stocked 12, 1.14's 16.
        MerchantRecipe paper = find(career("minecraft:librarian/cartographer", 1), Items.PAPER, true);
        c.ok(paper != null && paper.getMaxTradeUses() == (v21 ? 12 : 16), "cartographer's paper stock" + at);
        // Mason 3: Caves & Cliffs' dripstone from 1.17.
        if (FakeCavesAndCliffs.active()) {
            Item dripstone = Item.getByNameOrId("cavesandcliffsbackport:dripstone_block");
            c.eq(v17, find(career("minecraft:smith/mason", 3), dripstone) != null, "mason's dripstone block" + at);
        }

        // Wandering trader.
        List<MerchantRecipe> trader = TraderTrades.examples(new Random(v.ordinal()));
        c.eq(v21, find(trader, Item.getItemFromBlock(Blocks.HAY_BLOCK), true) != null,
                "the trader buys hay (1.21's buying pool)" + at);
        MerchantRecipe vine = find(trader, Item.getItemFromBlock(Blocks.VINE));
        c.ok(vine != null && vine.getItemToSell().getCount() == (v21 ? 3 : 1), "the trader's vines" + at);
        MerchantRecipe ice = find(trader, Item.getItemFromBlock(Blocks.PACKED_ICE));
        c.ok(ice != null && ice.getItemToBuy().getCount() == (v21 ? 1 : 3), "the trader's packed ice" + at);
        c.eq(v21, find(trader, Item.getItemFromBlock(Blocks.LOG)) != null, "the trader's logs (1.21)" + at);
        MerchantRecipe slime = find(trader, Items.SLIME_BALL);
        c.ok(slime != null && slime.getItemToBuy().getCount() == 4, "slime balls cost 4 emeralds" + at);
        if (FakeCavesAndCliffs.active()) {
            c.eq(v17, find(trader, Item.getByNameOrId("cavesandcliffsbackport:moss_block")) != null, "the trader's moss blocks (1.17)" + at);
        }
        if (FakeGameDrops.active()) {
            c.eq(v26, find(trader, Item.getByNameOrId("gamedropsbackport:sulfur_spike")) != null, "the trader's sulfur spikes (26.2)" + at);
        }
        int buys = 0;
        MerchantRecipeList rolled = TraderTrades.roll(new Random(7));
        for (MerchantRecipe r : rolled) {
            buys += r.getItemToSell().getItem() == Items.EMERALD ? 1 : 0;
        }
        c.eq(v21 ? 2 : 0, buys, "a fresh trader buys " + (v21 ? "two things" : "nothing") + at);
    }

    /** @return one example of each trade in the career's level pool */
    private static List<MerchantRecipe> career(String key, int level) {
        List<MerchantRecipe> out = new ArrayList<MerchantRecipe>();
        List<List<EntityVillager.ITradeList>> tiers = ModernTrades.asCareerTrades(key);
        if (tiers != null && tiers.size() >= level) {
            for (EntityVillager.ITradeList trade : tiers.get(level - 1)) {
                MerchantRecipeList list = new MerchantRecipeList();
                trade.addMerchantRecipe(null, list, new Random(level));
                out.addAll(list);
            }
        }
        return out;
    }

    private static MerchantRecipe find(List<MerchantRecipe> list, Item item) {
        return find(list, item, false);
    }

    /** @return the first trade selling (or with {@code bought}, asking for) the item, or null */
    private static MerchantRecipe find(List<MerchantRecipe> list, Item item, boolean bought) {
        if (item == null) {
            return null;
        }
        for (MerchantRecipe r : list) {
            ItemStack s = bought ? r.getItemToBuy() : r.getItemToSell();
            if (s.getItem() == item) {
                return r;
            }
        }
        return null;
    }
}
