package com.exiledradio.villagerbackport.trade;

import com.exiledradio.villagerbackport.compat.ConvertedVillagers;
import com.exiledradio.villagerbackport.compat.VillagerAccess;
import com.exiledradio.villagerbackport.data.VillagerTradeData;
import com.exiledradio.villagerbackport.job.Employment;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentData;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemEnchantedBook;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.village.MerchantRecipe;
import net.minecraft.village.MerchantRecipeList;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * {@code /librarian <enchantment> [level] [villager level] [price] [cured]} - spawns a librarian
 * that sells the given enchanted book.
 *
 * <p>For putting back a librarian lost to a bad cure: one that came back as a Sussyberian, or with
 * a fresh roll of trades in place of the book it was kept for. It is the librarian a lectern would
 * have hired - this mod's first level of trades, at the 1.14 price for the book - with the book
 * made the one asked for. A higher villager level unlocks that level's trades too.
 *
 * <p>It starts with a little experience, as any villager a player has traded with has, so it keeps
 * its trades whether or not there is a free lectern nearby; without that, a librarian with no
 * workstation loses its job and the book with it. {@code cured} adds the gossip a cure leaves for
 * the player who ran the command, so the book comes at the cured price.
 */
public final class LibrarianCommand extends CommandBase {

    private static final String CURED = "cured";

    /** The stock 1.14 gives an enchanted book trade. */
    private static final int STOCK = 12;

    @Override
    public String getName() {
        return "librarian";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/librarian <enchantment> [level] [villager level 1-5] [price] [cured]";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length < 1) {
            throw new WrongUsageException(getUsage(sender));
        }

        Enchantment enchantment = find(args[0]);

        boolean cured = false;
        List<String> numbers = new ArrayList<String>();
        for (int i = 1; i < args.length; i++) {
            if (CURED.equalsIgnoreCase(args[i])) {
                cured = true;
            } else {
                numbers.add(args[i]);
            }
        }
        if (numbers.size() > 3) {
            throw new WrongUsageException(getUsage(sender));
        }

        int level = numbers.size() > 0
                ? parseInt(numbers.get(0), enchantment.getMinLevel(), enchantment.getMaxLevel())
                : enchantment.getMaxLevel();
        int villagerLevel = numbers.size() > 1 ? parseInt(numbers.get(1), 1, VillagerLevel.MAX_LEVEL) : 1;

        World world = sender.getEntityWorld();
        EntityVillager villager = new EntityVillager(world);
        int price = numbers.size() > 2 ? parseInt(numbers.get(2), 1, 64) : price(enchantment, level, villager);

        Vec3d at = sender.getPositionVector();
        Entity entity = sender.getCommandSenderEntity();
        villager.setLocationAndAngles(at.x, at.y, at.z, entity != null ? entity.rotationYaw : 0.0F, 0.0F);
        villager.onInitialSpawn(world.getDifficultyForLocation(new BlockPos(villager)), null);

        // Never a berian: EagleMixins rolls every librarian that joins the world.
        ConvertedVillagers.notBerian(villager);

        if (!Employment.assign(villager, "librarian", false)) {
            throw new CommandException("No librarian career is registered.");
        }
        MerchantRecipeList list = VillagerAccess.getBuyingList(villager);
        if (list == null || list.isEmpty()) {
            throw new CommandException("Could not build a librarian's trades.");
        }

        sellBook(list, list.size(), enchantment, level, price);

        for (int tier = 2; tier <= villagerLevel; tier++) {
            TradeTables.unlockTier(villager, tier);
        }
        VillagerTradeData.setAppliedLevel(villager, villagerLevel);
        VillagerTradeData.setXp(villager, Math.max(1, VillagerLevel.xpForLevel(villagerLevel)));

        EntityPlayer player = entity instanceof EntityPlayer ? (EntityPlayer) entity : null;
        if (cured && player != null) {
            Gossip.add(villager, player.getUniqueID(), GossipType.MAJOR_POSITIVE, 20);
            Gossip.add(villager, player.getUniqueID(), GossipType.MINOR_POSITIVE, 25);
        }

        world.spawnEntity(villager);

        TextComponentString line = new TextComponentString("[librarian] level " + villagerLevel + " librarian selling ");
        line.appendSibling(new TextComponentTranslation(enchantment.getName()));
        line.appendText(" " + level + " for " + price + " emeralds" + (cured && player != null ? ", cured by you" : ""));
        line.getStyle().setColor(TextFormatting.DARK_AQUA);
        sender.sendMessage(line);
    }

    /**
     * Makes the first level's enchanted book the one asked for.
     *
     * <p>1.14's novice librarian has two of paper, a book and bookshelves, so a third of them have no
     * book at that level. Then the bookshelves make way for it, which keeps the first level the
     * same size and every trade after it on the level it was recorded at.
     */
    private static void sellBook(MerchantRecipeList list, int firstLevelEnd, Enchantment enchantment, int level,
                                 int price) {
        ItemStack book = ItemEnchantedBook.getEnchantedItemStack(new EnchantmentData(enchantment, level));
        MerchantRecipe trade = new MerchantRecipe(new ItemStack(Items.BOOK), new ItemStack(Items.EMERALD, price),
                book, 0, STOCK);

        int replace = firstLevelEnd - 1;
        for (int i = 0; i < firstLevelEnd; i++) {
            if (list.get(i).getItemToSell().getItem() == Items.ENCHANTED_BOOK) {
                replace = i;
                break;
            }
        }
        list.set(replace, trade);
    }

    /** {@code EnchantBookForEmeraldsTrade}'s price: doubled for a treasure enchantment, at most 64. */
    private static int price(Enchantment enchantment, int level, EntityVillager villager) {
        int price = 2 + villager.getRNG().nextInt(5 + level * 10) + 3 * level;
        if (enchantment.isTreasureEnchantment()) {
            price *= 2;
        }
        return Math.min(price, 64);
    }

    /**
     * Finds an enchantment by registry name, by the name without its mod - "strengthenedvitality" -
     * or by its English name with or without spaces and underscores.
     */
    private static Enchantment find(String name) throws CommandException {
        Enchantment exact = name.indexOf(':') >= 0 ? Enchantment.getEnchantmentByLocation(name) : null;
        if (exact != null) {
            return exact;
        }

        String wanted = simplify(name);
        List<Enchantment> matches = new ArrayList<Enchantment>();
        for (ResourceLocation key : Enchantment.REGISTRY.getKeys()) {
            Enchantment enchantment = Enchantment.REGISTRY.getObject(key);
            if (enchantment == null) {
                continue;
            }
            if (simplify(key.getPath()).equals(wanted) || simplify(englishName(enchantment)).equals(wanted)) {
                matches.add(enchantment);
            }
        }

        if (matches.size() == 1) {
            return matches.get(0);
        }
        if (matches.isEmpty()) {
            throw new CommandException("No enchantment called " + name + ". Press Tab for the list.");
        }
        List<String> names = new ArrayList<String>();
        for (Enchantment enchantment : matches) {
            names.add(String.valueOf(Enchantment.REGISTRY.getNameForObject(enchantment)));
        }
        throw new CommandException(name + " could be any of " + names + "; use the full name.");
    }

    @SuppressWarnings("deprecation")
    private static String englishName(Enchantment enchantment) {
        return net.minecraft.util.text.translation.I18n.translateToLocal(enchantment.getName());
    }

    private static String simplify(String name) {
        return name.toLowerCase(Locale.ROOT).replace(" ", "").replace("_", "").replace("-", "");
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args,
                                          @Nullable BlockPos target) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, Enchantment.REGISTRY.getKeys());
        }
        if (args.length >= 2) {
            return getListOfStringsMatchingLastWord(args, CURED);
        }
        return Collections.emptyList();
    }
}
