package com.exiledradio.villagerbackport.network;

import io.netty.buffer.ByteBuf;

import com.exiledradio.villagerbackport.VillagerBackport;

import com.exiledradio.villagerbackport.ModConfig;

import net.minecraft.entity.IMerchant;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.ContainerMerchant;
import net.minecraft.inventory.InventoryMerchant;
import net.minecraft.item.ItemStack;
import net.minecraft.village.MerchantRecipe;
import net.minecraft.village.MerchantRecipeList;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import javax.annotation.Nullable;

import java.lang.reflect.Field;

/**
 * Asks the server to load the selected trade's cost into the merchant's input slots.
 *
 * <h2>Why this is a packet and not a handful of clicks</h2>
 * It used to be clicks. The screen moved the items itself, through the same
 * {@code windowClick} path a player uses by hand - pick a stack up, put it down in the slot - which
 * has the advantage of needing no server code at all, and one fatal disadvantage: it is not atomic.
 *
 * <p>Every click is a packet the server answers in its own time, and it answers by checking that the
 * slot held what the client thought it held. A player shift-clicking the result slot to trade in
 * bulk is sending clicks of their own against the same container at the same time. Interleave the
 * two and the server finds a slot it does not recognise, rejects the transaction and re-sends the
 * whole container - and the trade the player was making quietly does not happen. Nothing is broken
 * afterwards, which is what makes it so confusing to watch: the trade simply refuses.
 *
 * <p>1.14 does not have this problem because its refill is not clicks. {@code MerchantContainer}
 * moves the items on the server, in one operation, with the player's input never in flight. This is
 * that: one request, one atomic move, one container update.
 */
public class PacketRefillTrade implements IMessage {

    private int recipeIndex;

    /**
     * Whether this refill may clear the slots first.
     *
     * <p>True when the player picked a trade from the list, which is a deliberate "I want this one
     * now" and should swap out whatever was loaded for the last one. False when it is the keyboard
     * topping the same trade back up, which must never take anything out - see the note in
     * {@code refill} on what happens when it does.
     */
    private boolean replace;

    public PacketRefillTrade() {
    }

    public PacketRefillTrade(int recipeIndex, boolean replace) {
        this.recipeIndex = recipeIndex;
        this.replace = replace;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.recipeIndex = buf.readInt();
        this.replace = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.recipeIndex);
        buf.writeBoolean(this.replace);
    }

    public static class Handler implements IMessageHandler<PacketRefillTrade, IMessage> {

        /** Forgets a player's selection when their trade screen closes. */
        public static void forget(EntityPlayerMP player) {
            LAST_INDEX.remove(player.getUniqueID());
        }

        /** The trade each player last asked to load, so a top-up knows what to reload. */
        private static final java.util.Map<java.util.UUID, Integer> LAST_INDEX =
                new java.util.HashMap<java.util.UUID, Integer>();

        @Nullable
        private static Field merchant;

        private static boolean resolved;

        @Override
        public IMessage onMessage(final PacketRefillTrade message, MessageContext ctx) {
            final EntityPlayerMP player = ctx.getServerHandler().player;

            player.getServerWorld().addScheduledTask(new Runnable() {
                @Override
                public void run() {
                    refill(player, message.recipeIndex, message.replace);
                }
            });

            return null;
        }

        /**
         * Fills the input slots for one trade, taking only from the player's own inventory.
         *
         * <p>Everything is checked here rather than trusted from the message: that the player has a
         * merchant screen open at all, that the index names a real trade, and that the items being
         * moved are the player's. A client asking for something impossible gets nothing.
         */
        private static void refill(EntityPlayerMP player, int index, boolean replace) {
            if (!(player.openContainer instanceof ContainerMerchant)) {
                return;
            }

            // Not while the player is holding something on the cursor. Moving stacks underneath a
            // drag in progress is how an item ends up belonging to nobody.
            if (!player.inventory.getItemStack().isEmpty()) {
                return;
            }

            ContainerMerchant container = (ContainerMerchant) player.openContainer;
            InventoryMerchant inventory = container.getMerchantInventory();

            // The trade is ready to go, so there is nothing to do and nothing to say.
            //
            // This is the whole difference between a refill that helps and one that fights the
            // player. Holding the key down asks this question many times a second; answering it by
            // moving items and re-sending the window each time overwrites whatever the client had
            // predicted for the click it just made, so the click appears to do nothing at all. A
            // refill that keeps quiet unless the trade actually cannot proceed can never do that.
            if (!inventory.getStackInSlot(2).isEmpty()) {
                return;
            }

            MerchantRecipeList recipes = recipesOf(inventory, player);
            if (recipes == null || index < 0 || index >= recipes.size()) {
                return;
            }

            MerchantRecipe recipe = recipes.get(index);

            // 1.14's moveFromInventoryToPaymentSlot, in order: empty both slots back to the player,
            // give up entirely if either will not fit, and only load the cost if both came away
            // empty. The leftovers matter - a slot holding sixteen paper against a trade that costs
            // twenty-four is why it has to clear before it fills.

            if (!giveBack(player, container, inventory, 0) || !giveBack(player, container, inventory, 1)) {
                return;
            }

            if (!inventory.getStackInSlot(0).isEmpty() || !inventory.getStackInSlot(1).isEmpty()) {
                return;
            }

            boolean moved = load(container, inventory, 0, recipe.getItemToBuy());
            moved |= load(container, inventory, 1, recipe.getSecondItemToBuy());

            if (!moved) {
                return;
            }

            container.setCurrentRecipeIndex(index);
            inventory.markDirty();
            container.detectAndSendChanges();

            // Said in full, because the client has been predicting clicks of its own and its idea of
            // these slots is now out of date. Only ever reached when something actually moved.
            player.sendContainerToPlayer(container);

        }

        /**
         * Reloads the payment slot as soon as a trade empties it, without waiting to be asked.
         *
         * <h2>Why the keyboard is not enough</h2>
         * A trade costing more than half a stack can only be paid for once per load: sixty-four
         * paper against a price of fifty-six buys one trade and leaves eight, and the result slot
         * empties until the slot is filled again. Refilling on a keypress means a packet out, a move,
         * and a window update back - and a player clicking ten times a second spends nine of those
         * clicks on an empty slot, because the payment has not arrived yet.
         *
         * <p>Doing it here, on the villager's own tick, closes that window: the slot is refilled
         * within a tick or two of the trade that emptied it, server-side, before the next click can
         * arrive. It only ever fires when the result slot is empty and the player has more of what
         * the trade wants, so a villager nobody is buying from costs nothing.
         */
        public static void topUp(EntityPlayerMP player) {
            if (!ModConfig.display.autoRefillWhileTrading) {
                return;
            }

            if (!(player.openContainer instanceof ContainerMerchant)) {
                return;
            }

            Integer index = LAST_INDEX.get(player.getUniqueID());
            if (index != null) {
                // refill() returns immediately unless the trade actually cannot proceed.
                refill(player, index.intValue(), false);
            }
        }

        /**
         * @return true if this slot is holding all it usefully can of what the trade wants
         *
         * <p>Either the trade wants nothing here, or the slot is already at the item's stack limit.
         * Anything less and there may be more in the player's inventory worth pulling in.
         */
        private static boolean loadedFully(InventoryMerchant inventory, int slot, ItemStack wanted) {
            if (wanted.isEmpty()) {
                return inventory.getStackInSlot(slot).isEmpty();
            }

            ItemStack held = inventory.getStackInSlot(slot);
            return !held.isEmpty() && matches(wanted, held)
                    && held.getCount() >= wanted.getMaxStackSize();
        }

        /**
         * Sends whatever is in an input slot back to the player.
         *
         * @return false if it would not fit, which is 1.14's signal to abandon the whole move rather
         *         than half-empty a trade the player cannot get back
         */
        private static boolean giveBack(EntityPlayerMP player, ContainerMerchant container,
                                        InventoryMerchant inventory, int slot) {
            ItemStack held = inventory.getStackInSlot(slot);
            if (held.isEmpty()) {
                return true;
            }

            if (!player.inventory.addItemStackToInventory(held.copy())) {
                return false;
            }

            inventory.setInventorySlotContents(slot, ItemStack.EMPTY);
            return true;
        }

        /**
         * Loads one input slot from the player's inventory, up to the item's stack limit.
         *
         * @return true if anything moved
         */
        private static boolean load(ContainerMerchant container, InventoryMerchant inventory,
                                    int slot, ItemStack wanted) {
            if (wanted.isEmpty()) {
                return false;
            }

            boolean moved = false;

            // Slots 3 to 38 are the player's inventory on this container - the same range 1.14
            // scans, and the reason armour and the offhand are left alone.
            for (int i = 3; i < 39; i++) {
                ItemStack candidate = container.inventorySlots.get(i).getStack();

                if (candidate.isEmpty() || !matches(wanted, candidate)) {
                    continue;
                }

                ItemStack held = inventory.getStackInSlot(slot);
                int have = held.isEmpty() ? 0 : held.getCount();
                int take = Math.min(wanted.getMaxStackSize() - have, candidate.getCount());

                if (take <= 0) {
                    break;
                }

                ItemStack filled = candidate.copy();
                filled.setCount(have + take);
                candidate.shrink(take);

                inventory.setInventorySlotContents(slot, filled);
                moved = true;

                if (have + take >= wanted.getMaxStackSize()) {
                    break;
                }
            }

            return moved;
        }

        /**
         * @return the trades on the merchant behind this screen
         *
         * <h2>Found by type rather than by name</h2>
         * {@code InventoryMerchant} keeps its merchant private with no accessor, and the field's name
         * differs between a development workspace and a running game. It is, however, the only field
         * of its type on the class, so looking it up by type finds it under either mapping without
         * this needing to know what either of them calls it.
         */
        @Nullable
        private static MerchantRecipeList recipesOf(InventoryMerchant inventory, EntityPlayerMP player) {
            Field field = merchantField();
            if (field == null) {
                return null;
            }

            try {
                IMerchant merchant = (IMerchant) field.get(inventory);
                return merchant == null ? null : merchant.getRecipes(player);
            } catch (IllegalAccessException e) {
                return null;
            }
        }

        @Nullable
        private static Field merchantField() {
            if (resolved) {
                return merchant;
            }

            resolved = true;

            for (Field field : InventoryMerchant.class.getDeclaredFields()) {
                if (IMerchant.class.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    merchant = field;
                    return merchant;
                }
            }

            VillagerBackport.LOGGER.warn(
                    "Could not find the merchant behind a trade screen; refilling is off.");
            return null;
        }

        /** The item and its damage and tags, ignoring how many - what a trade actually matches on. */
        private static boolean matches(ItemStack wanted, ItemStack candidate) {
            return ItemStack.areItemsEqual(wanted, candidate)
                    && ItemStack.areItemStackTagsEqual(wanted, candidate);
        }
    }
}
