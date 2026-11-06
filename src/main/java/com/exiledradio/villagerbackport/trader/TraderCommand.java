package com.exiledradio.villagerbackport.trader;


import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.WorldServer;


/**
 * {@code /wanderingtrader status | spawn [x y z] | timer <ticks>} - the trader by hand.
 *
 * <ul>
 *   <li>{@code status}: the countdown and chance, then every loaded trader - where, how long it has
 *       left, whether it is invisible, what it holds, its llamas, and anything hunting it.</li>
 *   <li>{@code spawn}: what the timer does when its rolls succeed, at a position (yours by default):
 *       a bell nearby if there is one, a spot within the radius, two llamas. The trader leaves on the
 *       usual clock.</li>
 *   <li>{@code timer}: sets the countdown, so the next real attempt comes sooner.</li>
 * </ul>
 */
public final class TraderCommand extends CommandBase {

    @Override
    public String getName() {
        return "wanderingtrader";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/wanderingtrader status | spawn [x y z] | timer <ticks>";
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
        WorldServer world = server.getWorld(0);
        switch (args[0]) {
            case "status":
                status(world, sender);
                return;
            case "spawn": {
                BlockPos at = position(world, sender, args);
                EntityWanderingTrader trader = TraderSpawner.spawnNear(world, at, false);
                if (trader == null) {
                    throw new CommandException("No spot within " + com.exiledradio.villagerbackport.ModConfig.trader.spawnRadius
                            + " blocks of " + describe(at) + " would do (ten tries).");
                }
                say(sender, "Wandering trader at " + describe(new BlockPos(trader)) + ", heading for "
                        + describe(trader.getWanderTarget()) + ".");
                return;
            }
            case "timer":
                if (args.length < 2) {
                    throw new WrongUsageException(getUsage(sender));
                }
                TraderSpawner.setDelay(world, parseInt(args[1], 0));
                say(sender, "Next spawn attempt in " + args[1] + " ticks (checked once a minute).");
                return;
            default:
                throw new WrongUsageException(getUsage(sender));
        }
    }

    private static BlockPos position(WorldServer world, ICommandSender sender, String[] args) throws CommandException {
        if (args.length >= 4) {
            return parseBlockPos(sender, args, 1, false);
        }
        // The server console has no position of its own; use the world spawn.
        return sender.getCommandSenderEntity() != null ? sender.getPosition() : world.getSpawnPoint();
    }

    private static void status(WorldServer world, ICommandSender sender) {
        int[] timer = TraderSpawner.status(world);
        say(sender, "Next attempt in " + timer[0] + " ticks at " + timer[1] + "% (then 1 in 10). Last trader may still be around: "
                + TraderSpawner.previousStillAround(world) + ". doTraderSpawning="
                + world.getGameRules().getString(TraderSpawner.GAME_RULE));
        for (Entity entity : world.loadedEntityList) {
            if (!(entity instanceof EntityWanderingTrader) || !entity.isEntityAlive()) {
                continue;
            }
            EntityWanderingTrader trader = (EntityWanderingTrader) entity;
            int llamas = 0;
            StringBuilder llamaTargets = new StringBuilder();
            for (EntityTraderLlama llama : world.getEntitiesWithinAABB(EntityTraderLlama.class,
                    trader.getEntityBoundingBox().grow(16.0D))) {
                if (llama.getLeashHolder() == trader) {
                    llamas++;
                    if (llama.getAttackTarget() != null) {
                        llamaTargets.append(' ').append(entityName(llama.getAttackTarget()));
                    }
                }
            }
            StringBuilder hunters = new StringBuilder();
            for (EntityLiving mob : world.getEntitiesWithinAABB(EntityLiving.class, trader.getEntityBoundingBox().grow(32.0D))) {
                if (mob.getAttackTarget() == trader) {
                    hunters.append(' ').append(entityName(mob));
                }
            }
            say(sender, "Trader " + describe(new BlockPos(trader)) + " leaves in " + trader.getDespawnDelay()
                    + " ticks; invisible=" + trader.isInvisible() + " holding=" + trader.getHeldItemMainhand().getDisplayName()
                    + " drinking=" + trader.isHandActive() + " day=" + world.isDaytime()
                    + " target=" + describe(trader.getWanderTarget()) + " home=" + describe(trader.getHomePosition())
                    + " llamas=" + llamas + " (targeting:" + (llamaTargets.length() == 0 ? " nothing" : llamaTargets.toString())
                    + ") hp=" + trader.getHealth() + " trades=" + trader.getOffers().size()
                    + " hunted by:" + (hunters.length() == 0 ? " nothing" : hunters.toString()));
        }
    }

    private static String entityName(Entity entity) {
        net.minecraft.util.ResourceLocation key = net.minecraft.entity.EntityList.getKey(entity);
        return key == null ? entity.getClass().getSimpleName() : key.toString();
    }

    private static String describe(BlockPos pos) {
        return pos == null ? "nowhere" : pos.getX() + " " + pos.getY() + " " + pos.getZ();
    }

    private static void say(ICommandSender sender, String text) {
        sender.sendMessage(new TextComponentString(text));
    }
}
