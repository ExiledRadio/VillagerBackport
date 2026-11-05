package com.exiledradio.villagerbackport.trader;

import com.mojang.authlib.GameProfile;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.ai.EntityAITasks;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.ContainerMerchant;
import net.minecraft.item.ItemStack;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.NetworkManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerInteractionManager;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.village.MerchantRecipe;
import net.minecraft.village.MerchantRecipeList;
import net.minecraft.world.WorldServer;

import java.util.UUID;

/**
 * {@code /wanderingtrader status | spawn [x y z] | timer <ticks> | awake <seconds> | selftest [x y z]} - the trader by
 * hand, and a check that it works on a server with nobody on it.
 *
 * <ul>
 *   <li>{@code status}: the countdown and chance, then every loaded trader - where, how long it has
 *       left, whether it is invisible, what it holds, its llamas, and anything hunting it.</li>
 *   <li>{@code spawn}: what the timer does when its rolls succeed, at a position (yours by default):
 *       a bell nearby if there is one, a spot within the radius, two llamas. The trader leaves on the
 *       usual clock.</li>
 *   <li>{@code timer}: sets the countdown, so the next real attempt comes sooner.</li>
 *   <li>{@code awake}: a testing aid. A server with nobody on it stops ticking mobs 15 seconds after
 *       the last player leaves, which freezes the trader mid-drink; this keeps the overworld ticking
 *       them for that many seconds, so its day and night behavior can be watched from the console.</li>
 *   <li>{@code selftest}: spawns a trader, then trades with it as a stand-in player - a real
 *       {@code EntityPlayerMP} on a connection to nowhere, never added to the world - and reports each
 *       step. Then a zombie and a pillager are put in the world for a moment to check they would hunt
 *       it. Everything it creates except the trader and its llamas is removed again.</li>
 * </ul>
 */
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = com.exiledradio.villagerbackport.VillagerBackport.MOD_ID)
public final class TraderCommand extends CommandBase {

    /** Ticks left of {@code awake}. */
    private static int awakeTicks;

    @net.minecraftforge.fml.common.eventhandler.SubscribeEvent
    public static void onWorldTick(net.minecraftforge.fml.common.gameevent.TickEvent.WorldTickEvent event) {
        if (awakeTicks > 0 && event.phase == net.minecraftforge.fml.common.gameevent.TickEvent.Phase.START
                && event.world instanceof WorldServer && event.world.provider.getDimension() == 0) {
            awakeTicks--;
            ((WorldServer) event.world).resetUpdateEntityTick();
        }
    }

    @Override
    public String getName() {
        return "wanderingtrader";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/wanderingtrader status | spawn [x y z] | timer <ticks> | awake <seconds> | selftest [x y z]";
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
            case "awake":
                if (args.length < 2) {
                    throw new WrongUsageException(getUsage(sender));
                }
                awakeTicks = parseInt(args[1], 0, 3600) * 20;
                say(sender, "Keeping the overworld's mobs ticking for " + args[1] + " seconds.");
                return;
            case "selftest":
                selftest(server, world, sender, position(world, sender, args));
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

    private static void selftest(MinecraftServer server, WorldServer world, ICommandSender sender, BlockPos at) {
        int failures = 0;

        EntityWanderingTrader trader = TraderSpawner.spawnNear(world, at, false);
        if (trader == null) {
            say(sender, "FAIL spawn: no spot near " + describe(at));
            return;
        }
        say(sender, "ok   spawn: trader at " + describe(new BlockPos(trader)) + " heading for "
                + describe(trader.getWanderTarget()) + ", leaves in " + trader.getDespawnDelay());

        int llamas = 0;
        for (EntityTraderLlama llama : world.getEntitiesWithinAABB(EntityTraderLlama.class, trader.getEntityBoundingBox().grow(8.0D))) {
            if (llama.getLeashHolder() == trader) {
                llamas++;
            }
        }
        failures += check(sender, llamas == com.exiledradio.villagerbackport.ModConfig.trader.llamas,
                "llamas", llamas + " leashed to it");

        MerchantRecipeList offers = trader.getOffers();
        StringBuilder list = new StringBuilder();
        for (MerchantRecipe recipe : offers) {
            list.append(" [").append(recipe.getItemToBuy().getCount()).append(' ').append(recipe.getItemToBuy().getDisplayName())
                    .append(" -> ").append(recipe.getItemToSell().getCount()).append(' ').append(recipe.getItemToSell().getDisplayName())
                    .append(" x").append(recipe.getMaxTradeUses()).append(']');
        }
        failures += check(sender, offers.size() == 9, "trades", offers.size() + ":" + list);

        // A stand-in player: a real EntityPlayerMP, on a connection whose packets go nowhere.
        EntityPlayerMP player = new EntityPlayerMP(server, world,
                new GameProfile(UUID.nameUUIDFromBytes("villagerbackport:trader-selftest".getBytes()), "TraderSelfTest"),
                new PlayerInteractionManager(world));
        new NetHandlerPlayServer(server, new NetworkManager(EnumPacketDirection.SERVERBOUND), player);
        player.setPosition(trader.posX, trader.posY, trader.posZ + 1.0D);
        player.setHeldItem(EnumHand.MAIN_HAND, ItemStack.EMPTY);

        boolean opened = trader.processInteract(player, EnumHand.MAIN_HAND);
        boolean merchant = player.openContainer instanceof ContainerMerchant;
        failures += check(sender, opened && merchant && trader.getCustomer() == player,
                "open", "interact=" + opened + " container=" + player.openContainer.getClass().getSimpleName()
                        + " customer=" + (trader.getCustomer() == player));
        if (!merchant) {
            say(sender, failures + " failure(s).");
            return;
        }
        ContainerMerchant container = (ContainerMerchant) player.openContainer;

        // Buy the first thing it sells for emeralds, until it runs out.
        int index = -1;
        for (int i = 0; i < offers.size(); i++) {
            if (offers.get(i).getItemToBuy().getItem() == Items.EMERALD) {
                index = i;
                break;
            }
        }
        if (index < 0) {
            failures += check(sender, false, "trade", "nothing sold for emeralds");
        } else {
            MerchantRecipe recipe = offers.get(index);
            container.setCurrentRecipeIndex(index);
            int bought = 0;
            for (int attempt = 0; attempt < recipe.getMaxTradeUses() + 2; attempt++) {
                container.getSlot(0).putStack(recipe.getItemToBuy().copy());
                player.inventory.setItemStack(ItemStack.EMPTY);
                container.slotClick(2, 0, ClickType.PICKUP, player);
                ItemStack got = player.inventory.getItemStack();
                if (!got.isEmpty() && ItemStack.areItemsEqual(got, recipe.getItemToSell())) {
                    bought++;
                }
                container.getSlot(0).putStack(ItemStack.EMPTY);
            }
            failures += check(sender, bought == recipe.getMaxTradeUses() && recipe.isRecipeDisabled(),
                    "trade", "bought " + recipe.getItemToSell().getDisplayName() + " " + bought + " times of "
                            + recipe.getMaxTradeUses() + ", used=" + recipe.getToolUses()
                            + " crossedOut=" + recipe.isRecipeDisabled());
        }

        // Sell it something it buys, if it has such a trade.
        for (int i = 0; i < offers.size(); i++) {
            MerchantRecipe recipe = offers.get(i);
            if (recipe.getItemToSell().getItem() != Items.EMERALD) {
                continue;
            }
            container.setCurrentRecipeIndex(i);
            container.getSlot(0).putStack(recipe.getItemToBuy().copy());
            player.inventory.setItemStack(ItemStack.EMPTY);
            container.slotClick(2, 0, ClickType.PICKUP, player);
            ItemStack got = player.inventory.getItemStack();
            failures += check(sender, got.getItem() == Items.EMERALD && recipe.getToolUses() == 1,
                    "sell", recipe.getItemToBuy().getDisplayName() + " -> " + got.getCount() + " " + got.getDisplayName());
            break;
        }

        int before = trader.getDespawnDelay();
        trader.tickDespawn();
        failures += check(sender, trader.getDespawnDelay() == before, "clock", "paused while trading ("
                + before + " -> " + trader.getDespawnDelay() + ")");

        player.closeScreen();
        failures += check(sender, trader.getCustomer() == null, "close", "customer released");
        player.inventory.setItemStack(ItemStack.EMPTY);

        // The llamas: hurt the trader as a zombie would and ask each llama's defend goal whether it
        // takes the zombie on. Called here, outside any goal, so no task list is being walked.
        EntityZombie attacker = new EntityZombie(world);
        attacker.setLocationAndAngles(trader.posX + 3.0D, trader.posY, trader.posZ, 0.0F, 0.0F);
        world.spawnEntity(attacker);
        trader.attackEntityFrom(net.minecraft.util.DamageSource.causeMobDamage(attacker), 1.0F);
        int defending = 0;
        for (EntityTraderLlama llama : world.getEntitiesWithinAABB(EntityTraderLlama.class, trader.getEntityBoundingBox().grow(8.0D))) {
            for (EntityAITasks.EntityAITaskEntry entry : llama.targetTasks.taskEntries) {
                if (entry.action instanceof EntityTraderLlama.DefendTrader && entry.action.shouldExecute()) {
                    entry.action.startExecuting();
                    if (llama.getAttackTarget() == attacker) {
                        defending++;
                    }
                }
            }
        }
        world.removeEntity(attacker);
        failures += check(sender, defending == llamas, "defend", defending + " of " + llamas
                + " llamas turned on the trader's attacker");
        trader.setHealth(trader.getMaxHealth());

        // Hunters: a zombie and a pillager get a target task for it as they join.
        failures += checkHunter(sender, world, new EntityZombie(world), trader);
        failures += checkHunter(sender, world, new com.exiledradio.villagerbackport.illager.EntityPillager(world), trader);

        say(sender, failures == 0 ? "selftest passed." : "selftest: " + failures + " failure(s).");
    }

    private static int checkHunter(ICommandSender sender, WorldServer world, EntityCreature mob, EntityWanderingTrader trader) {
        mob.setLocationAndAngles(trader.posX + 3.0D, trader.posY, trader.posZ, 0.0F, 0.0F);
        world.spawnEntity(mob);
        boolean hunts = false;
        for (EntityAITasks.EntityAITaskEntry entry : mob.targetTasks.taskEntries) {
            if (entry.action instanceof TraderContent.HuntTrader) {
                hunts = true;
            }
        }
        world.removeEntity(mob);
        return check(sender, hunts, "hunter", entityName(mob) + " has a target task for the trader");
    }

    private static int check(ICommandSender sender, boolean ok, String what, String detail) {
        say(sender, (ok ? "ok   " : "FAIL ") + what + ": " + detail);
        return ok ? 0 : 1;
    }

    private static String describe(BlockPos pos) {
        return pos == null ? "nowhere" : pos.getX() + " " + pos.getY() + " " + pos.getZ();
    }

    private static void say(ICommandSender sender, String text) {
        sender.sendMessage(new TextComponentString(text));
    }
}
