package com.exiledradio.villagerbackport.trade;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

import java.util.Collections;
import java.util.List;

/**
 * {@code /discount [radius]} - villagers treat you as the player who cured them.
 *
 * <p>With no argument it is the villager you are looking at; with a radius it is every villager
 * that close. It adds exactly what a cure adds - 20 major and 25 minor positive gossip about you -
 * so the discount is the cure discount, no more, and repeating it changes nothing once both are at
 * their caps.
 *
 * <p>It exists to give back a discount a player already earned and lost: a cure that came back with
 * the wrong villager's record, or a librarian put back with {@code /librarian}.
 */
public final class DiscountCommand extends CommandBase {

    @Override
    public String getName() {
        return "discount";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/discount [radius] - the villager you are looking at, or every villager within radius, gives you the cure discount";
    }

    /** Operators only: it is the reward for a cure, without the cure. */
    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        EntityPlayerMP player = getCommandSenderAsPlayer(sender);
        List<EntityVillager> villagers;

        if (args.length > 0) {
            int radius = parseInt(args[0], 1, 256);
            villagers = player.world.getEntitiesWithinAABB(
                    EntityVillager.class, player.getEntityBoundingBox().grow(radius));
        } else {
            EntityVillager seen = ForgiveCommand.lookedAt(player);
            if (seen == null) {
                throw new CommandException("Look at a villager, or give a radius: /discount 8");
            }
            villagers = Collections.singletonList(seen);
        }

        for (EntityVillager villager : villagers) {
            Gossip.add(villager, player.getUniqueID(), GossipType.MAJOR_POSITIVE, 20);
            Gossip.add(villager, player.getUniqueID(), GossipType.MINOR_POSITIVE, 25);
        }

        TextComponentString line = new TextComponentString(
                "[discount] " + villagers.size() + " villager(s) now give you the cure discount");
        line.getStyle().setColor(TextFormatting.DARK_AQUA);
        player.sendMessage(line);
    }
}
