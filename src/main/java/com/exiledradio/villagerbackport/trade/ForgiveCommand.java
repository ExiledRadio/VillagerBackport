package com.exiledradio.villagerbackport.trade;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

import java.util.Collections;
import java.util.List;

/**
 * {@code /forgive} - makes villagers drop what they hold against you.
 *
 * <p>With no argument it is the villager you are looking at; with a radius it is every villager
 * that close. Only the grudges go - hurting and killing - and only the ones about the player who
 * ran it. Goodwill from trading and curing is left exactly as it was.
 *
 * <p>It exists because a grudge otherwise takes up to ten days to wear off, and because villages
 * that lived through the version where grudges added up are still carrying the result.
 */
public final class ForgiveCommand extends CommandBase {

    /** How far away the villager being looked at may stand, a little past arm's reach. */
    private static final double LOOK_REACH = 8.0D;

    @Override
    public String getName() {
        return "forgive";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/forgive [radius] - the villager you are looking at, or every villager within radius, drops its grudges against you";
    }

    /**
     * Operators only, unlike {@code /village}.
     *
     * <p>That one only reads. This one undoes the cost of killing a villager, which on a server is
     * the whole point of there being a cost.
     */
    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args)
            throws CommandException {

        EntityPlayerMP player = getCommandSenderAsPlayer(sender);
        List<EntityVillager> villagers;

        if (args.length > 0) {
            int radius = parseInt(args[0], 1, 256);
            villagers = player.world.getEntitiesWithinAABB(
                    EntityVillager.class, player.getEntityBoundingBox().grow(radius));
        } else {
            EntityVillager seen = lookedAt(player);
            if (seen == null) {
                throw new CommandException("Look at a villager, or give a radius: /forgive 32");
            }
            villagers = Collections.singletonList(seen);
        }

        int forgiven = 0;
        for (EntityVillager villager : villagers) {
            if (Gossip.forgive(villager, player.getUniqueID())) {
                forgiven++;
            }
        }

        TextComponentString line = new TextComponentString(forgiven == 0
                ? "[forgive] no grudges against you among " + villagers.size() + " villager(s)"
                : "[forgive] " + forgiven + " of " + villagers.size() + " villager(s) dropped a grudge against you");
        line.getStyle().setColor(TextFormatting.DARK_AQUA);
        player.sendMessage(line);
    }

    /** @return the nearest villager the player's line of sight passes through, or null */
    private static EntityVillager lookedAt(EntityPlayerMP player) {
        Vec3d eyes = player.getPositionEyes(1.0F);
        Vec3d end = eyes.add(player.getLookVec().scale(LOOK_REACH));
        AxisAlignedBB reach = player.getEntityBoundingBox().grow(LOOK_REACH);

        EntityVillager nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (EntityVillager villager : player.world.getEntitiesWithinAABB(EntityVillager.class, reach)) {
            AxisAlignedBB box = villager.getEntityBoundingBox().grow(0.3D);
            if (!box.contains(eyes) && box.calculateIntercept(eyes, end) == null) {
                continue;
            }

            double distance = villager.getDistanceSq(player);
            if (distance < nearestDistance) {
                nearest = villager;
                nearestDistance = distance;
            }
        }

        return nearest;
    }
}
