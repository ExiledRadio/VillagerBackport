package com.exiledradio.villagerbackport.trade;

import com.exiledradio.villagerbackport.ModConfig;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.village.MerchantRecipeList;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Says in chat what a villager's new enchanted book trades turned out to be.
 *
 * <h2>What this is for</h2>
 * Rerolling a librarian means breaking and replacing its lectern until the book it offers is the one
 * you want. The roll itself is instant; finding out what it rolled is not - it means walking up to
 * the villager and opening the trade screen, every single time, and the answer is usually no.
 *
 * <p>So the result is reported as it happens. The reroll becomes break, replace, read the line that
 * appeared, and only walk over when it says something worth walking over for.
 *
 * <p>Only enchanted books are announced. Every other trade a villager rolls is knowable at a glance
 * from what it is selling, and announcing those would bury the one line that mattered.
 */
public final class BookAnnouncer {

    /**
     * How far away a player is told, in blocks.
     *
     * <p>Comfortably past the distance anyone stands at while rerolling, and short of hearing about
     * villagers elsewhere in the village taking jobs of their own.
     */
    private static final double HEARING_RANGE = 32.0D;

    private BookAnnouncer() {
    }

    /**
     * Announces the enchanted books among a villager's trades from the given index onward.
     *
     * @param from the first trade to look at, so unlocking a tier reports only what it added
     */
    public static void announce(EntityVillager villager, MerchantRecipeList recipes, int from) {
        if (!ModConfig.display.announceEnchantedBooks || recipes == null || villager.world.isRemote) {
            return;
        }

        // A villager can be offering the same book twice - two trades, two prices, one enchantment -
        // and reading the same line twice tells you nothing you did not know from the first.
        Set<String> said = new HashSet<String>();

        for (int index = Math.max(0, from); index < recipes.size(); index++) {
            ItemStack sold = recipes.get(index).getItemToSell();

            if (sold.isEmpty() || sold.getItem() != Items.ENCHANTED_BOOK) {
                continue;
            }

            ITextComponent name = nameOf(sold);
            if (name != null && said.add(name.getSiblings().toString())) {
                tell(villager, rolled(name));
            }
        }
    }

    /**
     * @return the finished line, as "Rolled: Unbreaking III"
     *
     * <h2>Why the wording is not a translation key of its own</h2>
     * It was, and it came out in chat as the raw key. A key only resolves against a language table
     * that has it, and this line is built on the server: the server's table is vanilla's own, and
     * mod language files are not in it. Vanilla's enchantment keys are, which is why the part that
     * actually matters - the enchantment's name - is still a component and still arrives in the
     * player's own language, renamed by whatever mod renamed it.
     */
    private static ITextComponent rolled(ITextComponent name) {
        ITextComponent message = new TextComponentString("Rolled: ");
        message.getStyle().setColor(TextFormatting.GRAY);

        return message.appendSibling(name);
    }

    /**
     * @return the enchantments on the book, written out, or null if it carries none
     *
     * <p>Built from translation components rather than translated here. This runs on the server,
     * which has no business deciding what language the player reads - and enchantment names are
     * exactly the kind of thing another mod may have renamed on the client.
     */
    private static ITextComponent nameOf(ItemStack book) {
        ITextComponent line = new TextComponentString("");
        boolean first = true;

        for (Map.Entry<Enchantment, Integer> entry : EnchantmentHelper.getEnchantments(book).entrySet()) {
            Enchantment enchantment = entry.getKey();
            if (enchantment == null) {
                continue;
            }

            if (!first) {
                line.appendText(", ");
            }
            first = false;

            line.appendSibling(new TextComponentTranslation(enchantment.getName()));

            // Vanilla writes level one enchantments without a numeral, and everything else with the
            // roman numeral its own translation supplies.
            int level = entry.getValue() == null ? 1 : entry.getValue();
            if (level != 1) {
                line.appendText(" ");
                line.appendSibling(new TextComponentTranslation("enchantment.level." + level));
            }
        }

        if (first) {
            // An enchanted book carrying nothing is not worth a line.
            return null;
        }

        line.getStyle().setColor(TextFormatting.AQUA);
        return line;
    }

    /** Sends the line to everyone close enough to have been the one who caused it. */
    private static void tell(EntityVillager villager, ITextComponent message) {
        if (message == null) {
            return;
        }

        for (EntityPlayer player : villager.world.playerEntities) {
            if (player.getDistanceSq(villager) <= HEARING_RANGE * HEARING_RANGE) {
                player.sendMessage(message);
            }
        }
    }
}
