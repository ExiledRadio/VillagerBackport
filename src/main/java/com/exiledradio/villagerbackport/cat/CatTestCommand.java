package com.exiledradio.villagerbackport.cat;

import com.exiledradio.villagerbackport.home.HomeSite;
import com.exiledradio.villagerbackport.raid.RaidVillages;
import com.mojang.authlib.GameProfile;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.ai.EntityAITasks;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.entity.passive.EntityOcelot;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerInteractionManager;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

/**
 * {@code /cattest} - the headless checks for cats, registered only in the development environment.
 *
 * <p>Each subcommand prints PASS or FAIL lines a test script can grep for. The "player" is a real
 * {@link EntityPlayerMP} that never joins: Forge refuses advancements to a {@code FakePlayer}, and the
 * taming code wants a player to own the cat.
 */
public final class CatTestCommand extends CommandBase {

    private static final UUID TESTER = UUID.fromString("c47c47c4-7c47-4c47-8c47-c47c47c47c47");

    /** Registers the command only when running from the development workspace. */
    public static void registerIfDev(FMLServerStartingEvent event) {
        Object dev = net.minecraft.launchwrapper.Launch.blackboard.get("fml.deobfuscatedEnvironment");
        if (CatContent.enabled() && Boolean.TRUE.equals(dev)) {
            event.registerServerCommand(new CatTestCommand());
        }
    }

    @Override
    public String getName() {
        return "cattest";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/cattest tame|ocelot|convert|report|village|gift|nbt|breed|creeper|template";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    private static EntityPlayerMP tester(MinecraftServer server, WorldServer world) {
        EntityPlayerMP player = new EntityPlayerMP(server, world, new GameProfile(TESTER, "CatTester"),
                new PlayerInteractionManager(world));
        return player;
    }

    private static void say(ICommandSender sender, boolean pass, String what) {
        String line = (pass ? "CATTEST PASS " : "CATTEST FAIL ") + what;
        sender.sendMessage(new TextComponentString(line));
        com.exiledradio.villagerbackport.VillagerBackport.LOGGER.info(line);
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 0) {
            throw new WrongUsageException(getUsage(sender));
        }
        WorldServer world = server.getWorld(0);
        BlockPos spawn = world.getTopSolidOrLiquidBlock(world.getSpawnPoint());

        switch (args[0]) {
            case "tame":
                tame(server, world, spawn, sender);
                break;
            case "ocelot":
                ocelot(server, world, spawn, sender);
                break;
            case "convert":
                convertStart(world, spawn, sender);
                break;
            case "report":
                convertReport(world, spawn, sender);
                break;
            case "village":
                village(world, spawn, sender);
                break;
            case "gift":
                gift(server, world, spawn, sender);
                break;
            case "nbt":
                nbt(world, sender);
                break;
            case "breed":
                breed(world, sender);
                break;
            case "creeper":
                creeper(world, spawn, sender);
                break;
            case "template":
                template(world, sender);
                break;
            default:
                throw new WrongUsageException(getUsage(sender));
        }
    }

    private static EntityCat newCat(WorldServer world, BlockPos at) {
        EntityCat cat = new EntityCat(world);
        cat.setLocationAndAngles(at.getX() + 0.5D, at.getY(), at.getZ() + 0.5D, 0.0F, 0.0F);
        cat.onInitialSpawn(world.getDifficultyForLocation(at), null);
        world.spawnEntity(cat);
        return cat;
    }

    private static void tame(MinecraftServer server, WorldServer world, BlockPos spawn, ICommandSender sender) {
        EntityPlayerMP player = tester(server, world);
        player.setPosition(spawn.getX() + 0.5D, spawn.getY(), spawn.getZ() + 0.5D);
        Advancement catalogue = server.getAdvancementManager().getAdvancement(CatAdvancements.COMPLETE_CATALOGUE);
        say(sender, catalogue != null, "advancement complete_catalogue loaded");

        int tamedTypes = 0;
        for (int type = 0; type < EntityCat.TYPES.length; type++) {
            EntityCat cat = newCat(world, spawn.add(2, 0, 0));
            cat.setCatType(type);
            int fish = 0;
            while (!cat.isTamed() && fish < 60) {
                player.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(Items.FISH, 1, fish % 2));
                cat.processInteract(player, EnumHand.MAIN_HAND);
                fish++;
            }
            if (cat.isTamed() && TESTER.equals(cat.getOwnerId()) && cat.isOrderedToSit()) {
                tamedTypes++;
            }
            if (type == 0) {
                say(sender, cat.isTamed(), "cat tamed after " + fish + " fish (1 in 3)");
                say(sender, TESTER.equals(cat.getOwnerId()), "owner set");
                say(sender, cat.isOrderedToSit() && cat.isSitting(), "sits once tamed");
                // Owner taps with an empty hand: stands up.
                player.setHeldItem(EnumHand.MAIN_HAND, ItemStack.EMPTY);
                cat.processInteract(player, EnumHand.MAIN_HAND);
                say(sender, !cat.isOrderedToSit(), "empty hand withdraws the sit order");
                // Dye the collar.
                player.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(Items.DYE, 1, EnumDyeColor.BLUE.getDyeDamage()));
                cat.processInteract(player, EnumHand.MAIN_HAND);
                say(sender, cat.getCollarColor() == EnumDyeColor.BLUE, "collar dyed blue (" + cat.getCollarColor() + ")");
                // Clownfish does not tame.
                EntityCat stray = newCat(world, spawn.add(-2, 0, 0));
                for (int i = 0; i < 30; i++) {
                    player.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(Items.FISH, 1, 2));
                    stray.processInteract(player, EnumHand.MAIN_HAND);
                }
                say(sender, !stray.isTamed(), "clownfish never tames");
                stray.setDead();
            }
            cat.setDead();
        }
        say(sender, tamedTypes == EntityCat.TYPES.length, "tamed all " + tamedTypes + " coats");
        if (catalogue != null) {
            AdvancementProgress progress = player.getAdvancements().getProgress(catalogue);
            int done = 0;
            for (String ignored : progress.getCompletedCriteria()) {
                done++;
            }
            say(sender, progress.isDone(), "A Complete Catalogue granted (" + done + " of 11 criteria)");
        }
    }

    private static void ocelot(MinecraftServer server, WorldServer world, BlockPos spawn, ICommandSender sender) {
        EntityPlayerMP player = tester(server, world);
        player.setPosition(spawn.getX() + 0.5D, spawn.getY(), spawn.getZ() + 0.5D);
        EntityOcelot ocelot = new EntityOcelot(world);
        ocelot.setLocationAndAngles(spawn.getX() + 1.5D, spawn.getY(), spawn.getZ() + 0.5D, 0.0F, 0.0F);
        world.spawnEntity(ocelot);

        int flee = 0;
        for (EntityAITasks.EntityAITaskEntry entry : ocelot.tasks.taskEntries) {
            if (entry.action instanceof OcelotTrust.AvoidPlayersUntilTrusted) {
                flee++;
            }
            if (entry.action.getClass() == net.minecraft.entity.ai.EntityAIAvoidEntity.class) {
                flee += 100;
            }
        }
        say(sender, flee == 1, "ocelot has exactly the trust-aware flee goal (" + flee + ")");

        int fish = 0;
        while (!OcelotTrust.isTrusting(ocelot) && fish < 60) {
            player.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(Items.FISH, 1, 0));
            EnumActionResult result = ForgeHooks.onInteractEntity(player, ocelot, EnumHand.MAIN_HAND);
            if (result == null) {
                say(sender, false, "fish interaction was not intercepted");
                return;
            }
            fish++;
        }
        say(sender, OcelotTrust.isTrusting(ocelot), "ocelot trusts after " + fish + " fish");
        say(sender, !ocelot.isTamed(), "ocelot was not tamed");
        say(sender, ocelot.isNoDespawnRequired(), "trusting ocelot no longer despawns");

        player.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(Items.FISH, 1, 1));
        ForgeHooks.onInteractEntity(player, ocelot, EnumHand.MAIN_HAND);
        say(sender, ocelot.isInLove(), "trusting ocelot breeds on salmon");
        ocelot.setDead();
    }

    private static void convertStart(WorldServer world, BlockPos spawn, ICommandSender sender) {
        for (int skin = 0; skin <= 3; skin++) {
            EntityOcelot ocelot = new EntityOcelot(world);
            ocelot.setLocationAndAngles(spawn.getX() + 0.5D + skin * 2, spawn.getY(), spawn.getZ() + 4.5D, 0.0F, 0.0F);
            ocelot.setTamed(true);
            ocelot.setOwnerId(TESTER);
            ocelot.setTameSkin(skin);
            ocelot.setCustomNameTag("Old" + skin);
            ocelot.setSitting(skin == 2);
            world.spawnEntity(ocelot);
        }
        say(sender, true, "spawned 4 tamed ocelots for conversion");
    }

    private static void convertReport(WorldServer world, BlockPos spawn, ICommandSender sender) {
        AxisAlignedBB box = new AxisAlignedBB(spawn.add(-4, -4, 0), spawn.add(12, 4, 9));
        List<EntityOcelot> ocelots = world.getEntitiesWithinAABB(EntityOcelot.class, box);
        int alive = 0;
        for (EntityOcelot ocelot : ocelots) {
            if (!ocelot.isDead && ocelot.isTamed()) {
                alive++;
            }
        }
        say(sender, alive == 0, "no tamed ocelots left (" + alive + ")");
        List<EntityCat> cats = world.getEntitiesWithinAABB(EntityCat.class, box);
        int good = 0;
        for (EntityCat cat : cats) {
            String name = cat.getCustomNameTag();
            if (!name.startsWith("Old")) {
                continue;
            }
            int skin = Integer.parseInt(name.substring(3));
            int expected = skin >= 1 ? skin : 0;
            boolean ok = cat.isTamed() && TESTER.equals(cat.getOwnerId()) && cat.getCatType() == expected
                    && cat.isOrderedToSit() == (skin == 2);
            // The order, not the pose: 1.12.2's sit goal sits any pet whose owner is offline, and
            // the test owner never joins.
            say(sender, ok, "converted " + name + " -> " + cat.getTypeName() + " tamed=" + cat.isTamed()
                    + " ordered to sit=" + cat.isOrderedToSit());
            if (ok) {
                good++;
            }
            cat.setDead();
        }
        say(sender, good == 4, "all 4 conversions kept owner, coat, name and sitting");
    }

    private static void village(WorldServer world, BlockPos spawn, ICommandSender sender) {
        // Six beds in a row, each claimed by a villager standing by it.
        BlockPos base = spawn.add(20, 0, 20);
        for (int i = 0; i < 6; i++) {
            BlockPos foot = base.add(i * 2, 0, 0);
            BlockPos head = foot.north();
            world.setBlockState(foot, Blocks.BED.getDefaultState()
                    .withProperty(net.minecraft.block.BlockBed.FACING, net.minecraft.util.EnumFacing.NORTH)
                    .withProperty(net.minecraft.block.BlockBed.PART, net.minecraft.block.BlockBed.EnumPartType.FOOT), 3);
            world.setBlockState(head, Blocks.BED.getDefaultState()
                    .withProperty(net.minecraft.block.BlockBed.FACING, net.minecraft.util.EnumFacing.NORTH)
                    .withProperty(net.minecraft.block.BlockBed.PART, net.minecraft.block.BlockBed.EnumPartType.HEAD), 3);
            EntityVillager villager = new EntityVillager(world);
            villager.setLocationAndAngles(foot.getX() + 0.5D, foot.getY(), foot.getZ() + 2.5D, 0.0F, 0.0F);
            villager.enablePersistence();
            world.spawnEntity(villager);
            HomeSite.set(villager, head);
        }
        BlockPos spot = base.add(4, 0, 6);
        int homes = CatSpawner.occupiedHomes(world, spot);
        say(sender, homes == 6, "occupied homes counted: " + homes);
        say(sender, RaidVillages.isVillage(world, spot), "spot is in a village");

        int spawned = 0;
        for (int i = 0; i < 10; i++) {
            BlockPos at = spot.add(i % 3, 0, i / 3);
            say(sender, true, "attempt " + i + " at " + at + ": spawnable="
                    + net.minecraft.world.WorldEntitySpawner.canCreatureTypeSpawnAtLocation(
                    net.minecraft.entity.EntityLiving.SpawnPlacementType.ON_GROUND, world, at)
                    + " village=" + RaidVillages.isVillage(world, at) + " homes=" + CatSpawner.occupiedHomes(world, at)
                    + " cats=" + world.getEntitiesWithinAABB(EntityCat.class, new AxisAlignedBB(at).grow(48.0D, 8.0D, 48.0D)).size());
            EntityCat cat = CatSpawner.trySpawnAt(world, at);
            if (cat != null) {
                spawned++;
                say(sender, !cat.isTamed() && !cat.isNoDespawnRequired(), "village cat is a stray " + cat.getTypeName());
            }
        }
        int total = world.getEntitiesWithinAABB(EntityCat.class, new AxisAlignedBB(spot).grow(48.0D, 8.0D, 48.0D)).size();
        say(sender, total == 5 && spawned >= 1, "village cap of five: " + spawned + " new, " + total + " in range after 10 attempts");

        // Away from the village: nothing.
        BlockPos far = spawn.add(-100, 0, -100);
        world.getChunk(far);
        far = world.getTopSolidOrLiquidBlock(far);
        say(sender, CatSpawner.trySpawnAt(world, far) == null, "no cat outside a village");
        say(sender, !CatSpawner.isWitchHut(world, spawn), "spawn is not a witch hut");
    }

    private static void gift(MinecraftServer server, WorldServer world, BlockPos spawn, ICommandSender sender) {
        BlockPos home = spawn.add(0, 0, -6);
        EntityCat cat = newCat(world, home);
        AxisAlignedBB around = new AxisAlignedBB(home).grow(12.0D);
        int before = world.getEntitiesWithinAABB(EntityItem.class, around).size();
        for (int i = 0; i < 20; i++) {
            cat.setPosition(home.getX() + 0.5D, home.getY(), home.getZ() + 0.5D);
            EntityAICatRelaxOnOwner.giveMorningGift(cat);
        }
        List<EntityItem> items = world.getEntitiesWithinAABB(EntityItem.class, around);
        StringBuilder kinds = new StringBuilder();
        for (EntityItem item : items) {
            kinds.append(item.getItem().getItem().getRegistryName().getPath()).append(' ');
            item.setDead();
        }
        say(sender, items.size() - before == 20, "20 morning gifts dropped: " + kinds.toString().trim());
        cat.setDead();
    }

    private static void nbt(WorldServer world, ICommandSender sender) {
        EntityCat cat = new EntityCat(world);
        cat.setCatType(7);
        cat.setCollarColor(EnumDyeColor.LIME);
        cat.setTamed(true);
        cat.setOwnerId(TESTER);
        NBTTagCompound tag = cat.writeToNBT(new NBTTagCompound());
        EntityCat copy = new EntityCat(world);
        copy.readFromNBT(tag);
        say(sender, copy.getCatType() == 7 && copy.getCollarColor() == EnumDyeColor.LIME && TESTER.equals(copy.getOwnerId()),
                "NBT round trip: " + copy.getTypeName() + " " + copy.getCollarColor());
    }

    private static void breed(WorldServer world, ICommandSender sender) {
        EntityCat a = new EntityCat(world);
        EntityCat b = new EntityCat(world);
        a.setCatType(3);
        b.setCatType(5);
        a.setTamed(true);
        a.setOwnerId(TESTER);
        b.setTamed(true);
        b.setOwnerId(TESTER);
        a.setInLove(null);
        b.setInLove(null);
        say(sender, a.canMateWith(b), "two tamed cats in love can mate");
        EntityCat kitten = a.createChild(b);
        say(sender, (kitten.getCatType() == 3 || kitten.getCatType() == 5) && TESTER.equals(kitten.getOwnerId()) && kitten.isTamed(),
                "kitten takes a parent's coat (" + kitten.getTypeName() + ") and owner");
        EntityCat wild = new EntityCat(world);
        wild.setInLove(null);
        say(sender, !wild.canMateWith(a), "a stray cannot breed");
    }

    private static void creeper(WorldServer world, BlockPos spawn, ICommandSender sender) {
        EntityCreeper creeper = new EntityCreeper(world);
        creeper.setLocationAndAngles(spawn.getX() + 0.5D, spawn.getY(), spawn.getZ() - 10.5D, 0.0F, 0.0F);
        world.spawnEntity(creeper);
        int found = 0;
        for (EntityAITasks.EntityAITaskEntry entry : creeper.tasks.taskEntries) {
            if (entry.action instanceof CatFear.AvoidCats) {
                found++;
            }
        }
        say(sender, found == 1, "creeper runs from cats (" + found + " goal)");
        creeper.setDead();
    }

    private static void template(WorldServer world, ICommandSender sender) {
        int cats = 0;
        for (String coat : new String[]{"black", "british", "calico", "jellie", "persian", "ragdoll", "red", "siamese", "tabby", "white"}) {
            String path = "/assets/villagerbackport/structures/village/common/animals/cat_" + coat + ".nbt";
            try (InputStream in = CatTestCommand.class.getResourceAsStream(path)) {
                NBTTagList entities = CompressedStreamTools.readCompressed(in).getTagList("entities", 10);
                if (entities.tagCount() == 1 && "cat".equals(entities.getCompoundTagAt(0).getString("kind"))) {
                    EntityCat cat = EntityCat.fromTemplate(world, entities.getCompoundTagAt(0).getInteger("variant"));
                    if (cat != null && cat.isNoDespawnRequired()) {
                        cats++;
                    }
                }
            } catch (Exception e) {
                say(sender, false, "template " + coat + ": " + e);
            }
        }
        say(sender, cats == 10, "all 10 village cat pieces carry a cat (" + cats + ")");
    }
}
