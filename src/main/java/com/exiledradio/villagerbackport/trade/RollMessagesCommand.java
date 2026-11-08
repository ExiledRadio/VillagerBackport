package com.exiledradio.villagerbackport.trade;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;

import javax.annotation.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * {@code /rollmessages on|off}: whether this player is told what a villager's new book trades rolled.
 *
 * <p>Per player, and open to everyone. The "Rolled:" line is clickable to run {@code off}, so a player
 * who is not rerolling anything can be rid of it in one click without needing to be an operator - and
 * without switching it off for every other player on the server, which is what the config does.
 */
public final class RollMessagesCommand extends CommandBase {

    public static final String NAME = "rollmessages";

    /** Kept in the persisted tag so it survives death, as a player's own setting should. */
    private static final String HIDDEN = "villagerbackport:hideBookRolls";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/" + NAME + " <on|off>";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        return sender instanceof EntityPlayer;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        EntityPlayerMP player = getCommandSenderAsPlayer(sender);
        if (args.length != 1 || !("on".equalsIgnoreCase(args[0]) || "off".equalsIgnoreCase(args[0]))) {
            throw new WrongUsageException(getUsage(sender));
        }

        boolean on = "on".equalsIgnoreCase(args[0]);
        persisted(player).setBoolean(HIDDEN, !on);

        ITextComponent line = new TextComponentString(on
                ? "Book roll messages are on for you again. "
                : "Book roll messages are off for you. ");
        line.getStyle().setColor(TextFormatting.GRAY);
        ITextComponent undo = new TextComponentString(on ? "[Turn off]" : "[Turn back on]");
        undo.getStyle()
                .setColor(TextFormatting.AQUA)
                .setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/" + NAME + (on ? " off" : " on")))
                .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        new TextComponentString("Click, or type /" + NAME + (on ? " off" : " on"))));
        player.sendMessage(line.appendSibling(undo));
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args,
                                          @Nullable BlockPos targetPos) {
        return args.length == 1 ? getListOfStringsMatchingLastWord(args, "on", "off") : Collections.<String>emptyList();
    }

    /** @return true unless this player has turned the messages off for themselves */
    public static boolean wantsMessages(EntityPlayer player) {
        return !persisted(player).getBoolean(HIDDEN);
    }

    private static NBTTagCompound persisted(EntityPlayer player) {
        NBTTagCompound data = player.getEntityData();
        if (!data.hasKey(EntityPlayer.PERSISTED_NBT_TAG, 10)) {
            data.setTag(EntityPlayer.PERSISTED_NBT_TAG, new NBTTagCompound());
        }
        return data.getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
    }
}
