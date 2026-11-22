package com.exiledradio.villagerbackport.selftest;

import com.exiledradio.villagerbackport.ModConfig;
import com.exiledradio.villagerbackport.block.ContainerCartographyTable;
import com.exiledradio.villagerbackport.block.MapLocking;
import com.exiledradio.villagerbackport.compat.VillagerAccess;
import com.exiledradio.villagerbackport.data.VillagerTradeData;
import com.exiledradio.villagerbackport.trade.EnchantedTrades;
import com.exiledradio.villagerbackport.trade.LevelGate;
import com.exiledradio.villagerbackport.trade.ModernTrades;
import com.exiledradio.villagerbackport.trade.VillagerLevel;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.ContainerMerchant;
import net.minecraft.item.Item;
import net.minecraft.item.ItemMap;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.village.MerchantRecipe;
import net.minecraft.village.MerchantRecipeList;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * {@code -Pselftest=trades26}: the 26.x villager changes - 26.1's enchanted trades, 26.1's master librarian (with
 * Game Drops Backport, or its stand-in under {@code fake-gd}), 26.3's level-up on an open trade screen, and 26.3's
 * cartography table on explorer maps.
 */
public final class Trades26SelfTest implements SelfTest {

    @Override
    public String name() {
        return "trades26";
    }

    @Override
    public void run(Check c) {
        WorldServer world = DimensionManager.getWorld(0);
        boolean enchanted = ModConfig.trades.modernEnchantedTrades;
        boolean live = ModConfig.trades.tradesUpdateWhileOpen;
        try {
            enchantedBooks(c);
            enchantedGear(c);
            librarian(c, world);
            liveLevelUp(c, world);
            cartography(c, world);
            trader(c);
            owners(c);
        } finally {
            ModConfig.trades.modernEnchantedTrades = enchanted;
            ModConfig.trades.tradesUpdateWhileOpen = live;
        }
    }

    private static void enchantedBooks(Check c) {
        List<Enchantment> pool = EnchantedTrades.tradeable();
        c.ok(pool.contains(net.minecraft.init.Enchantments.MENDING) && pool.contains(net.minecraft.init.Enchantments.FROST_WALKER)
                && pool.contains(net.minecraft.init.Enchantments.BINDING_CURSE), "#tradeable holds mending, frost walker and the curses");
        boolean untradeable = false;
        for (Enchantment e : pool) {
            String path = e.getRegistryName().getPath();
            untradeable |= path.equals("soul_speed") || path.equals("swift_sneak") || path.equals("wind_burst");
        }
        c.ok(!untradeable, "#tradeable leaves out soul speed, swift sneak and wind burst");

        Random random = new Random(26_1L);
        int bad = 0;
        boolean sawDouble = false;
        boolean sawCap = false;
        for (int i = 0; i < 4000; i++) {
            MerchantRecipe r = EnchantedTrades.book(12, random);
            if (r == null || r.getItemToBuy().getItem() != Items.EMERALD || r.getSecondItemToBuy().getItem() != Items.BOOK
                    || r.getItemToSell().getItem() != Items.ENCHANTED_BOOK || r.getMaxTradeUses() != 12) {
                bad++;
                continue;
            }
            Map<Enchantment, Integer> held = EnchantmentHelper.getEnchantments(r.getItemToSell());
            if (held.size() != 1) {
                bad++;
                continue;
            }
            Map.Entry<Enchantment, Integer> e = held.entrySet().iterator().next();
            int level = e.getValue();
            int low = 2 + 3 * level;
            int high = low + 4 + 10 * level;
            if (e.getKey().isTreasureEnchantment()) {
                low *= 2;
                high *= 2;
                sawDouble = true;
            }
            int cost = r.getItemToBuy().getCount();
            sawCap |= cost == 64;
            if (cost < Math.min(low, 64) || cost > Math.min(high, 64) || level < e.getKey().getMinLevel() || level > e.getKey().getMaxLevel()) {
                bad++;
            }
        }
        c.eq(0, bad, "4000 librarian books: emeralds then a book, one enchantment, 26.1's cost");
        c.ok(sawDouble && sawCap, "treasure books cost double, capped at 64");
    }

    private static void enchantedGear(Check c) {
        Random random = new Random(26_3L);
        ModConfig.trades.modernEnchantedTrades = true;
        int bad = 0;
        for (int i = 0; i < 1000; i++) {
            MerchantRecipe r = EnchantedTrades.item(Items.DIAMOND_SWORD, 12, 3, random);
            if (r == null) {
                continue;
            }
            int cost = r.getItemToBuy().getCount();
            if (!r.getItemToSell().isItemEnchanted() || cost < 17 || cost > 31 || r.getMaxTradeUses() != 3) {
                bad++;
            }
        }
        c.eq(0, bad, "enchanted swords: always enchanted, 12 + level 5 to 19 emeralds");

        // An item that can never be enchanted rolls nothing: 26.1 discards it, 1.12.2's way sells it plain.
        c.ok(EnchantedTrades.item(Items.STICK, 1, 3, random) == null, "26.1: an unenchanted item is not offered");
        ModConfig.trades.modernEnchantedTrades = false;
        MerchantRecipe plain = EnchantedTrades.item(Items.STICK, 1, 3, random);
        c.ok(plain != null && !plain.getItemToSell().isItemEnchanted(), "off: 1.12.2 sells it unenchanted");
        ModConfig.trades.modernEnchantedTrades = true;
    }

    private static void librarian(Check c, WorldServer world) {
        Item candle = ForgeRegistries.ITEMS.getValue(new net.minecraft.util.ResourceLocation("cavesandcliffsbackport", "yellow_candle"));
        Item deeper = ForgeRegistries.ITEMS.getValue(new net.minecraft.util.ResourceLocation("deeperdepths", "yellow_candle"));
        boolean candles = candle != null && candle != Items.AIR || deeper != null && deeper != Items.AIR;
        Item dandelion = ForgeRegistries.ITEMS.getValue(new net.minecraft.util.ResourceLocation("gamedropsbackport", "golden_dandelion"));
        boolean gameDrops = dandelion != null && dandelion != Items.AIR;

        EntityVillager v = new EntityVillager(world, 1);
        MerchantRecipeList list = new MerchantRecipeList();
        ModernTrades.addTier("minecraft:librarian/librarian", 5, v, list, new Random(5));
        boolean nameTag = false;
        for (MerchantRecipe r : list) {
            nameTag |= r.getItemToSell().getItem() == Items.NAME_TAG;
        }
        c.ok(nameTag == (!candles && !gameDrops), "master librarian's name tag: only without candles and without Game Drops"
                + " (candles " + candles + ", Game Drops " + gameDrops + ", " + list.size() + " trades)");
        if (gameDrops && !candles) {
            c.eq(0, list.size(), "with Game Drops and no candle mod a master librarian has nothing new to sell");
        }
    }

    private static void liveLevelUp(Check c, WorldServer world) {
        EntityVillager v = new EntityVillager(world, 1);
        v.setPosition(0.5D, 200.0D, 0.5D);
        world.spawnEntity(v);
        FakePlayer player = FakePlayerFactory.getMinecraft(world);
        try {
            // A librarian at level one, with no workstation to employ it: its list made here.
            VillagerAccess.setCareerId(v, 1);
            MerchantRecipeList first = new MerchantRecipeList();
            ModernTrades.addTier("minecraft:librarian/librarian", 1, v, first, new Random(1));
            VillagerAccess.setBuyingList(v, first);
            VillagerAccess.setCareerLevel(v, 1);
            VillagerTradeData.setAppliedLevel(v, 1);
            MerchantRecipeList list = VillagerAccess.getBuyingList(v);
            int applied = VillagerTradeData.getAppliedLevel(v);
            c.ok(list != null && !list.isEmpty() && applied == 1, "a new librarian has its first level of trades (" + applied + ")");
            if (list == null) {
                return;
            }

            player.openContainer = new ContainerMerchant(player.inventory, v, world);
            v.setCustomer(player);

            ModConfig.trades.tradesUpdateWhileOpen = false;
            VillagerTradeData.setXp(v, VillagerLevel.xpForNextLevel(1));
            LevelGate.noteTrade(v);
            LevelGate.apply(v);
            c.eq(1, VillagerTradeData.getAppliedLevel(v), "1.14: the level waits while the screen is open");

            ModConfig.trades.tradesUpdateWhileOpen = true;
            int before = VillagerAccess.getBuyingList(v).size();
            LevelGate.apply(v);
            c.eq(2, VillagerTradeData.getAppliedLevel(v), "26.3: the level lands with the screen open");
            c.ok(VillagerAccess.getBuyingList(v).size() > before, "26.3: the new level's trades are added while trading ("
                    + before + " -> " + VillagerAccess.getBuyingList(v).size() + ")");
        } finally {
            v.setCustomer(null);
            player.openContainer = player.inventoryContainer;
            v.setDead();
        }
    }

    private static void cartography(Check c, WorldServer world) {
        Item explorer = FakeGameDrops.explorerMap != null ? FakeGameDrops.explorerMap : new ItemMap() {
        };
        FakePlayer player = FakePlayerFactory.getMinecraft(world);
        ContainerCartographyTable table = new ContainerCartographyTable(player.inventory, world, new BlockPos(0, 200, 0));
        ItemStack filled = ItemMap.setupNewMap(world, 0, 0, (byte) 2, true, true);
        ItemStack map = new ItemStack(explorer, 1, filled.getMetadata());

        c.ok(table.getSlot(ContainerCartographyTable.SLOT_MAP).isItemValid(map), "an explorer map goes in the map slot");

        ItemStack copy = result(table, map, new ItemStack(Items.MAP));
        c.ok(copy.getItem() == explorer && copy.getCount() == 2 && copy.getMetadata() == map.getMetadata(),
                "an empty map copies an explorer map: two of the same item and map");
        c.ok(result(table, map, new ItemStack(Items.PAPER)).isEmpty(), "paper does not zoom an explorer map out (26.3)");
        ItemStack locked = result(table, map, new ItemStack(Blocks.GLASS_PANE));
        c.ok(locked.getItem() == explorer && MapLocking.isLocked(locked, null), "a glass pane locks an explorer map");
        c.ok(!result(table, filled, new ItemStack(Items.PAPER)).isEmpty(), "paper still zooms a filled map out");

        table.getSlot(ContainerCartographyTable.SLOT_MAP).putStack(ItemStack.EMPTY);
        table.getSlot(ContainerCartographyTable.SLOT_MODIFIER).putStack(ItemStack.EMPTY);
    }

    /** Under {@code fake-ua}: the wandering trader sells Update Aquatic's ocean items with only that mod installed. */
    private static void trader(Check c) {
        java.util.Set<String> sold = new java.util.HashSet<String>();
        int nameTags = 0;
        for (MerchantRecipe r : com.exiledradio.villagerbackport.trader.TraderTrades.examples(new Random(13))) {
            if (r.getItemToSell().getItem().getRegistryName() != null) {
                sold.add(r.getItemToSell().getItem().getRegistryName().toString());
            }
            nameTags += r.getItemToSell().getItem() == Items.NAME_TAG ? 1 : 0;
        }
        // With Game Drops its own name tag trade (through Core) is the one; this mod's steps aside.
        c.eq(FakeGameDrops.active() ? 0 : 1, nameTags, "this mod's trader name tag trade, unless Game Drops adds its own");
        if (FakeGameDrops.active()) {
            c.ok(sold.contains("gamedropsbackport:sulfur_spike"), "the wandering trader sells Game Drops' sulfur spikes (26.2)");
        }
        if (FakeCavesAndCliffs.active()) {
            for (String name : new String[] {"small_dripleaf", "rooted_dirt", "moss_block"}) {
                c.ok(sold.contains("cavesandcliffsbackport:" + name), "the wandering trader sells Caves & Cliffs' " + name);
            }
        }
        if (!FakeUpdateAquatic.active()) {
            return;
        }
        for (String name : FakeUpdateAquatic.TRADER_ITEMS) {
            c.ok(sold.contains("updateaquaticbackport:" + name), "the wandering trader sells Update Aquatic's " + name);
        }
    }

    /** Under {@code fake-ua} / {@code fake-cc}: the villager trades wired to Update Aquatic's and Caves &amp; Cliffs' items. */
    private static void owners(Check c) {
        if (FakeUpdateAquatic.active()) {
            c.ok(sells("minecraft:farmer/fisherman", 1, "updateaquaticbackport:cod_bucket"), "a novice fisherman sells Update Aquatic's cod bucket");
            c.ok(sells("minecraft:butcher/butcher", 4, "updateaquaticbackport:dried_kelp_block"), "an expert butcher buys Update Aquatic's dried kelp blocks");
            c.ok(sells("minecraft:priest/cleric", 4, "updateaquaticbackport:scute"), "an expert cleric buys Update Aquatic's scutes");
            c.ok(sells("minecraft:butcher/leather", 4, "updateaquaticbackport:scute"), "an expert leatherworker buys Update Aquatic's scutes");
        }
        if (FakeCavesAndCliffs.active()) {
            c.ok(sells("minecraft:smith/mason", 3, "cavesandcliffsbackport:dripstone_block"), "a journeyman mason sells Caves & Cliffs' dripstone blocks (1.17)");
        } else {
            c.ok(!sells("minecraft:smith/mason", 3, "cavesandcliffsbackport:dripstone_block"), "without Caves & Cliffs no dripstone trade");
        }
    }

    /** @return true if a trade in the career's level pool buys or sells the named item */
    private static boolean sells(String career, int level, String item) {
        List<List<net.minecraft.entity.passive.EntityVillager.ITradeList>> tiers = ModernTrades.asCareerTrades(career);
        if (tiers == null || tiers.size() < level) {
            return false;
        }
        for (net.minecraft.entity.passive.EntityVillager.ITradeList trade : tiers.get(level - 1)) {
            MerchantRecipeList list = new MerchantRecipeList();
            trade.addMerchantRecipe(null, list, new Random(level));
            for (MerchantRecipe r : list) {
                for (ItemStack s : new ItemStack[] {r.getItemToBuy(), r.getSecondItemToBuy(), r.getItemToSell()}) {
                    if (!s.isEmpty() && s.getItem().getRegistryName() != null && item.equals(s.getItem().getRegistryName().toString())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static ItemStack result(ContainerCartographyTable table, ItemStack map, ItemStack modifier) {
        table.getSlot(ContainerCartographyTable.SLOT_MAP).putStack(map.copy());
        table.getSlot(ContainerCartographyTable.SLOT_MODIFIER).putStack(modifier);
        return table.getSlot(ContainerCartographyTable.SLOT_RESULT).getStack().copy();
    }
}
