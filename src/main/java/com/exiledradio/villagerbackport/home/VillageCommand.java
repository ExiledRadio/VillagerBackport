package com.exiledradio.villagerbackport.home;

import com.exiledradio.villagerbackport.network.NetworkHandler;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.village.Village;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * {@code /village} - draws the village you are standing in.
 *
 * <p>Puts a box round every bed within range, green where a villager has claimed one and red where
 * it is going spare, with a white box containing the lot. Everything it draws is read from the same
 * index and the same claims the villagers use, so the picture is what they see rather than a second
 * opinion assembled for the occasion.
 *
 * <p>The counts printed alongside are the ones that decide behaviour: beds against villagers tells
 * you whether the village can grow, and how many villagers are eligible for a golem tells you why
 * one has or has not appeared.
 */
public final class VillageCommand extends CommandBase {

    /** How far out to look when the command is given no radius of its own. */
    private static final int DEFAULT_RADIUS = 64;

    /** How long the client keeps the outline up, in seconds. */
    private static final int SHOW_FOR = 60;

    @Override
    public String getName() {
        return "village";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/village [radius] - draws the beds and villagers around you";
    }

    /**
     * Available to everyone.
     *
     * <p>It reads state and draws lines for the player who asked; there is nothing here worth
     * restricting, and a debug view that needs operator rights is no use on somebody else's server.
     */
    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args)
            throws CommandException {

        EntityPlayerMP player = getCommandSenderAsPlayer(sender);
        World world = player.world;

        int radius = args.length > 0 ? parseInt(args[0], 8, 256) : DEFAULT_RADIUS;
        BlockPos centre = new BlockPos(player);

        List<BlockPos> beds = VillageOutline.bedsAround(world, centre, radius);
        Set<BlockPos> claimed = VillageOutline.claimedAround(world, centre, radius);

        // Only the claims that point at a bed we are actually drawing, or the counts disagree with
        // the picture for villagers sleeping just outside the radius.
        List<BlockPos> claimedHere = new ArrayList<BlockPos>();
        for (BlockPos bed : beds) {
            if (claimed.contains(bed)) {
                claimedHere.add(bed);
            }
        }

        NetworkHandler.sendVillageOutline(player, beds, claimedHere, SHOW_FOR);

        report(player, world, centre, radius, beds, claimedHere);
    }

    private void report(EntityPlayerMP player, World world, BlockPos centre, int radius,
                        List<BlockPos> beds, List<BlockPos> claimed) {

        AxisAlignedBB box = new AxisAlignedBB(centre).grow(radius);
        List<EntityVillager> villagers =
                world.getEntitiesWithinAABB(EntityVillager.class, box);

        long now = world.getTotalWorldTime();
        int eligible = 0;
        int asleep = 0;

        for (EntityVillager villager : villagers) {
            if (GolemSpawner.isEligible(villager, now)) {
                eligible++;
            }
            if (HomeSite.isSleeping(villager)) {
                asleep++;
            }
        }

        say(player, "beds: " + beds.size() + " (" + claimed.size() + " claimed, "
                + (beds.size() - claimed.size()) + " free)");
        say(player, "villagers: " + villagers.size() + " (" + asleep + " asleep, "
                + eligible + " eligible for a golem)");

        Village village = world.getVillageCollection() == null
                ? null
                : world.getVillageCollection().getNearestVillage(centre, radius);

        if (village == null) {
            say(player, "no vanilla village here - doors decide that, and it has not found enough");
        } else {
            say(player, "vanilla village at " + village.getCenter().getX() + ", "
                    + village.getCenter().getY() + ", " + village.getCenter().getZ()
                    + " - radius " + village.getVillageRadius() + ", "
                    + village.getNumVillageDoors() + " doors, "
                    + village.getNumVillagers() + " villagers");
        }
    }

    private static void say(EntityPlayerMP player, String message) {
        TextComponentString line = new TextComponentString("[village] " + message);
        line.getStyle().setColor(TextFormatting.DARK_AQUA);
        player.sendMessage(line);
    }
}
