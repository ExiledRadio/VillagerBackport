package com.exiledradio.villagerbackport.raid;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.WorldServer;

import java.util.Collections;
import java.util.List;

/**
 * {@code /raid start [level] [x y z] | stop [x y z] | status | patrol [x y z]} - this mod's raids, by hand.
 *
 * <p>{@code start} does what a Raid Omen running out does: starts a raid on the village there, or
 * raises the one already there by that many levels. Only when this mod runs the raids.
 */
public final class RaidCommand extends CommandBase {

    @Override
    public String getName() {
        return "raid";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/raid start [level 1-5] [x y z] | stop [x y z] | status | patrol [x y z]";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 0) {
            throw new WrongUsageException(getUsage(sender));
        }
        if (!Raids.usingOwnRaids()) {
            throw new CommandException("This mod's raids are not in use (compat.raids).");
        }

        WorldServer world = (WorldServer) sender.getEntityWorld();
        RaidManager manager = RaidManager.get(world);

        switch (args[0]) {
            case "start": {
                int level = args.length > 1 ? parseInt(args[1], 1, VillageRaid.MAX_LEVEL) : 1;
                BlockPos at = args.length > 4 ? parseBlockPos(sender, args, 2, false) : sender.getPosition();
                if (!RaidVillages.isVillage(world, at)) {
                    throw new CommandException("There is no village at " + at.getX() + " " + at.getY() + " " + at.getZ() + ".");
                }
                VillageRaid raid = manager.startOrExtend(at, level);
                if (raid == null) {
                    throw new CommandException("No raid could start there: raids are off, or that raid is at full strength.");
                }
                tell(sender, "Raid " + raid.getId() + " at " + describe(raid.getCenter()) + ", level " + raid.getBadOmenLevel());
                break;
            }
            case "stop": {
                BlockPos at = args.length > 3 ? parseBlockPos(sender, args, 1, false) : sender.getPosition();
                VillageRaid raid = manager.findRaid(at);
                if (raid == null) {
                    throw new CommandException("There is no raid here.");
                }
                raid.stop();
                tell(sender, "Raid " + raid.getId() + " stopped.");
                break;
            }
            case "patrol": {
                // Several tries, as the spot is random and may be water, a wall or too bright.
                BlockPos around = args.length > 3 ? parseBlockPos(sender, args, 1, false) : sender.getPosition();
                int spawned = 0;
                for (int i = 0; i < 20 && spawned == 0; i++) {
                    spawned = com.exiledradio.villagerbackport.illager.PatrolSpawner.spawnNear(world, around, world.rand);
                }
                if (spawned == 0) {
                    throw new CommandException("No room for a patrol 24 to 48 blocks from here.");
                }
                tell(sender, "A patrol of " + spawned + " pillager(s) is out, led by a captain.");
                break;
            }
            case "status":
                if (manager.all().isEmpty()) {
                    tell(sender, "No raids in this dimension.");
                }
                for (VillageRaid raid : manager.all()) {
                    tell(sender, "Raid " + raid.getId() + " at " + describe(raid.getCenter()) + ": level " + raid.getBadOmenLevel()
                            + ", wave " + raid.getGroupsSpawned() + "/" + raid.getNumGroups() + ", " + raid.getRaiderCount()
                            + " raiders, " + (raid.isVictory() ? "won" : raid.isLoss() ? "lost" : raid.isStarted() ? "under way" : "starting"));
                }
                break;
            default:
                throw new WrongUsageException(getUsage(sender));
        }
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos target) {
        return args.length == 1 ? getListOfStringsMatchingLastWord(args, "start", "stop", "status", "patrol") : Collections.<String>emptyList();
    }

    private static String describe(BlockPos pos) {
        return pos.getX() + " " + pos.getY() + " " + pos.getZ();
    }

    private static void tell(ICommandSender sender, String message) {
        TextComponentString line = new TextComponentString("[raid] " + message);
        line.getStyle().setColor(TextFormatting.DARK_AQUA);
        sender.sendMessage(line);
    }
}
