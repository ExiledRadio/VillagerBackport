package com.exiledradio.villagerbackport.flora;

import com.exiledradio.villagerbackport.VillagerBackport;
import com.exiledradio.villagerbackport.block.BannerPatterns;
import com.exiledradio.villagerbackport.block.Compostables;
import com.exiledradio.villagerbackport.trade.ModernTrades;
import com.mojang.authlib.GameProfile;

import net.minecraft.block.Block;
import net.minecraft.block.BlockCauldron;
import net.minecraft.entity.Entity;
import net.minecraft.entity.boss.EntityWither;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.EntityWitherSkeleton;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.passive.EntityCow;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.entity.passive.EntityMooshroom;
import net.minecraft.entity.passive.EntitySheep;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.passive.EntityWolf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerInteractionManager;
import net.minecraft.tileentity.BannerPattern;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.village.MerchantRecipe;
import net.minecraft.village.MerchantRecipeList;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.BabyEntitySpawnEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.oredict.OreDictionary;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * A headless self-test of the flora package, for a dedicated dev server. Does nothing unless the JVM
 * is started with {@code -Dvillagerbackport.floraSelfTest=true}.
 *
 * <p>Two hundred ticks after the server starts it builds a small test area high above spawn and works
 * through everything the package does that a server can see - registration, every recipe, stew
 * effects, mooshroom conversion, feeding, milking and shearing, horse armor, cauldron washing, sign
 * dyeing, flower pots, the wither rose, worldgen and bone meal, and the trades - logging one
 * {@code [FloraSelfTest] PASS} or {@code FAIL} line per check, then a summary, and stops the server.
 *
 * <p>Interactions go through Forge's own hooks with a real (not fake) {@link EntityPlayerMP} that
 * is never added to the world, the way a click from a connected player would reach them.
 */
@Mod.EventBusSubscriber(modid = VillagerBackport.MOD_ID)
public final class FloraSelfTest {

    private static final boolean ENABLED = Boolean.getBoolean("villagerbackport.floraSelfTest");

    private static int ticks;
    private static boolean done;
    private static int passed;
    private static int failed;

    private FloraSelfTest() {
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (!ENABLED || done || event.phase != TickEvent.Phase.END || ++ticks < 200) {
            return;
        }
        done = true;

        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        try {
            run(server);
        } catch (Throwable t) {
            VillagerBackport.LOGGER.error("[FloraSelfTest] FAIL crashed", t);
            failed++;
        }
        VillagerBackport.LOGGER.info("[FloraSelfTest] SUMMARY passed={} failed={}", passed, failed);
        server.initiateShutdown();
    }

    private static void check(String name, boolean ok, Object detail) {
        if (ok) {
            passed++;
            VillagerBackport.LOGGER.info("[FloraSelfTest] PASS {} ({})", name, detail);
        } else {
            failed++;
            VillagerBackport.LOGGER.error("[FloraSelfTest] FAIL {} ({})", name, detail);
        }
    }

    private static void run(MinecraftServer server) {
        WorldServer world = server.getWorld(0);
        BlockPos spawn = world.getSpawnPoint();
        BlockPos base = new BlockPos(spawn.getX(), 200, spawn.getZ());

        EntityPlayerMP player = new EntityPlayerMP(server, world,
                new GameProfile(UUID.fromString("00000000-0000-0000-0000-00000000f10a"), "FloraTester"),
                new PlayerInteractionManager(world));
        player.setPosition(base.getX() + 0.5D, base.getY() + 1, base.getZ() + 0.5D);
        // A connection that goes nowhere: packets the player would be sent (potion effects, inventory)
        // queue on a channel that never opens.
        new net.minecraft.network.NetHandlerPlayServer(server,
                new net.minecraft.network.NetworkManager(net.minecraft.network.EnumPacketDirection.SERVERBOUND), player);
        // Lightning would otherwise set the test area alight.
        world.getGameRules().setOrCreateGameRule("doFireTick", "false");

        registration();
        recipes(world);
        stew(world, player);
        mooshrooms(world, player, base.add(10, 0, 0));
        horses(world, player, base.add(20, 0, 0));
        signs(world, player, base.add(30, 0, 0));
        pots(world, player, base.add(40, 0, 0));
        witherRose(world, base.add(50, 0, 0));
        animals(world, player, base.add(60, 0, 0));
        trades();
        worldgen(world);
    }

    // ---------------------------------------------------------------------------------------------

    private static void registration() {
        check("blocks registered", FloraContent.cornflower != null && FloraContent.lilyOfTheValley != null
                && FloraContent.witherRose != null && FloraContent.pottedWitherRose != null, FloraContent.blocks().size());
        check("items registered", FloraContent.items().size() == 10, FloraContent.items());
        check("ore dictionary dyeWhite has white_dye", contains("dyeWhite", FloraContent.whiteDye), "dyeWhite");
        check("ore dictionary dyeBlack has black_dye", contains("dyeBlack", FloraContent.blackDye), "dyeBlack");
        check("ore dictionary dyeBrown has brown_dye", contains("dyeBrown", FloraContent.brownDye), "dyeBrown");
        check("ore dictionary dyeBlue has blue_dye", contains("dyeBlue", FloraContent.blueDye), "dyeBlue");
        check("ore dictionary dye has blue_dye", contains("dye", FloraContent.blueDye), "dye");

        BannerPattern globe = GlobePattern.globe();
        check("globe pattern added", globe != null && "glb".equals(globe.getHashname()), globe);
        int index = BannerPatterns.indexOf(globe);
        check("globe is a special loom pattern", index > BannerPatterns.FREE_COUNT && !BannerPatterns.isFree(index),
                "index=" + index + " free=" + BannerPatterns.FREE_COUNT + " count=" + BannerPatterns.count());
        check("free loom patterns unchanged", BannerPatterns.FREE_COUNT == 34, BannerPatterns.FREE_COUNT);
        check("globe pattern item", FloraContent.globeBannerPattern != null
                && FloraContent.globeBannerPattern.getPattern() == globe, FloraContent.globeBannerPattern);

        check("leather horse armor type", ItemLeatherHorseArmor.type().getProtection() == 3,
                ItemLeatherHorseArmor.type());
        check("flowers compost at 65%", Compostables.chanceFor(new ItemStack(FloraContent.cornflower)) == 0.65F
                && Compostables.chanceFor(new ItemStack(FloraContent.witherRose)) == 0.65F,
                Compostables.chanceFor(new ItemStack(FloraContent.cornflower)));
        check("stew stacks to 1", FloraContent.suspiciousStew.getItemStackLimit(new ItemStack(FloraContent.suspiciousStew)) == 1,
                "1");
    }

    private static boolean contains(String ore, Item item) {
        for (ItemStack stack : OreDictionary.getOres(ore)) {
            if (stack.getItem() == item) {
                return true;
            }
        }
        return false;
    }

    // ---------------------------------------------------------------------------------------------

    private static final Container NOBODY = new Container() {
        @Override
        public boolean canInteractWith(EntityPlayer player) {
            return false;
        }
    };

    private static ItemStack craft(WorldServer world, ItemStack... grid) {
        InventoryCrafting inv = new InventoryCrafting(NOBODY, 3, 3);
        for (int i = 0; i < grid.length; i++) {
            if (grid[i] != null) {
                inv.setInventorySlotContents(i, grid[i].copy());
            }
        }
        return CraftingManager.findMatchingResult(inv, world);
    }

    private static ItemStack dye(EnumDyeColor color) {
        return new ItemStack(Items.DYE, 1, color.getDyeDamage());
    }

    private static void recipes(WorldServer world) {
        expect(world, "bone meal -> white dye", FloraContent.whiteDye, dye(EnumDyeColor.WHITE));
        expect(world, "lily of the valley -> white dye", FloraContent.whiteDye, new ItemStack(FloraContent.lilyOfTheValley));
        expect(world, "ink sac -> black dye", FloraContent.blackDye, dye(EnumDyeColor.BLACK));
        expect(world, "wither rose -> black dye", FloraContent.blackDye, new ItemStack(FloraContent.witherRose));
        expect(world, "cocoa beans -> brown dye", FloraContent.brownDye, dye(EnumDyeColor.BROWN));
        expect(world, "lapis -> blue dye", FloraContent.blueDye, dye(EnumDyeColor.BLUE));
        expect(world, "cornflower -> blue dye", FloraContent.blueDye, new ItemStack(FloraContent.cornflower));

        ItemStack white = craft(world, new ItemStack(FloraContent.whiteDye));
        check("white dye alone crafts nothing", white.isEmpty(), white);

        ItemStack wool = craft(world, new ItemStack(FloraContent.blackDye), new ItemStack(Blocks.WOOL, 1, 0));
        check("black dye + wool -> black wool (ore dictionary)",
                wool.getItem() == Item.getItemFromBlock(Blocks.WOOL) && wool.getMetadata() == EnumDyeColor.BLACK.getMetadata(), wool);

        ItemStack lightBlue = craft(world, new ItemStack(FloraContent.blueDye), new ItemStack(FloraContent.whiteDye));
        check("blue + white dye -> light blue dye", lightBlue.getItem() == Items.DYE
                && lightBlue.getMetadata() == EnumDyeColor.LIGHT_BLUE.getDyeDamage(), lightBlue);

        ItemStack glass = craft(world, new ItemStack(Blocks.GLASS), new ItemStack(Blocks.GLASS), new ItemStack(Blocks.GLASS),
                new ItemStack(Blocks.GLASS), new ItemStack(FloraContent.brownDye), new ItemStack(Blocks.GLASS),
                new ItemStack(Blocks.GLASS), new ItemStack(Blocks.GLASS), new ItemStack(Blocks.GLASS));
        check("brown dye stains glass", glass.getItem() == Item.getItemFromBlock(Blocks.STAINED_GLASS)
                && glass.getMetadata() == EnumDyeColor.BROWN.getMetadata(), glass);

        ItemStack bookAndQuill = craft(world, new ItemStack(Items.BOOK), new ItemStack(Items.FEATHER),
                new ItemStack(FloraContent.blackDye));
        // Forge's ore dictionary rewrite already lets any dyeBlack make a book and quill in 1.12.2; noted, not asserted.
        VillagerBackport.LOGGER.info("[FloraSelfTest] INFO black dye in the book and quill recipe gives {}", bookAndQuill);

        ItemStack boneBlock = craft(world, new ItemStack(FloraContent.whiteDye), new ItemStack(FloraContent.whiteDye),
                new ItemStack(FloraContent.whiteDye), new ItemStack(FloraContent.whiteDye), new ItemStack(FloraContent.whiteDye),
                new ItemStack(FloraContent.whiteDye), new ItemStack(FloraContent.whiteDye), new ItemStack(FloraContent.whiteDye),
                new ItemStack(FloraContent.whiteDye));
        check("nine white dye make no bone block", boneBlock.getItem() != Item.getItemFromBlock(Blocks.BONE_BLOCK), boneBlock);

        // Leather armor through ModernDyeRecipe, compared with lapis.
        ItemStack helmetModern = craft(world, new ItemStack(Items.LEATHER_HELMET), new ItemStack(FloraContent.blueDye));
        ItemStack helmetVanilla = craft(world, new ItemStack(Items.LEATHER_HELMET), dye(EnumDyeColor.BLUE));
        check("blue dye dyes leather armor like lapis", helmetModern.getItem() == Items.LEATHER_HELMET
                && Items.LEATHER_HELMET.hasColor(helmetModern)
                && Items.LEATHER_HELMET.getColor(helmetModern) == Items.LEATHER_HELMET.getColor(helmetVanilla),
                Integer.toHexString(Items.LEATHER_HELMET.getColor(helmetModern)));

        ItemStack star = craft(world, new ItemStack(Items.GUNPOWDER), new ItemStack(FloraContent.whiteDye));
        int[] colors = star.hasTagCompound() ? star.getTagCompound().getCompoundTag("Explosion").getIntArray("Colors") : new int[0];
        check("white dye colors a firework star", star.getItem() == Items.FIREWORK_CHARGE && colors.length == 1
                && colors[0] == net.minecraft.item.ItemDye.DYE_COLORS[EnumDyeColor.WHITE.getDyeDamage()], Arrays.toString(colors));

        ItemStack banner = new ItemStack(Items.BANNER, 1, EnumDyeColor.RED.getDyeDamage());
        ItemStack striped = craft(world, null, null, null, null, banner, null,
                new ItemStack(FloraContent.blackDye), new ItemStack(FloraContent.blackDye), new ItemStack(FloraContent.blackDye));
        check("black dye adds a banner pattern", striped.getItem() == Items.BANNER && striped.hasTagCompound()
                && striped.getTagCompound().getCompoundTag("BlockEntityTag").getTagList("Patterns", 10).tagCount() == 1, striped.getTagCompound());

        ItemStack L = new ItemStack(Items.LEATHER);
        ItemStack armor = craft(world, L, null, L, L, L, L, L, null, L);
        check("leather horse armor recipe", armor.getItem() == FloraContent.leatherHorseArmor, armor);
        ItemStack dyed = craft(world, armor, new ItemStack(Items.DYE, 1, EnumDyeColor.RED.getDyeDamage()));
        check("leather horse armor dyes red", dyed.getItem() == FloraContent.leatherHorseArmor && ItemLeatherHorseArmor.hasColor(dyed)
                && ItemLeatherHorseArmor.getColor(dyed) == helmetColor(world, new ItemStack(Items.DYE, 1, EnumDyeColor.RED.getDyeDamage())),
                Integer.toHexString(ItemLeatherHorseArmor.getColor(dyed)));
        ItemStack dyedModern = craft(world, armor, new ItemStack(FloraContent.blueDye));
        check("leather horse armor dyes with a new dye, as leather armor does",
                ItemLeatherHorseArmor.getColor(dyedModern) == helmetColor(world, dye(EnumDyeColor.BLUE)),
                Integer.toHexString(ItemLeatherHorseArmor.getColor(dyedModern)));

        // Every flower's stew recipe.
        for (StewEffects.Entry entry : StewEffects.table()) {
            ItemStack stew = craft(world, new ItemStack(Items.BOWL), new ItemStack(Blocks.RED_MUSHROOM),
                    new ItemStack(Blocks.BROWN_MUSHROOM), entry.flower);
            check("stew recipe " + entry.flower.getDisplayName(), stew.getItem() == FloraContent.suspiciousStew
                    && effectOf(stew) == entry.effect && durationOf(stew) == entry.ticks, stew.getTagCompound());
        }
        ItemStack plain = craft(world, new ItemStack(Items.BOWL), new ItemStack(Blocks.RED_MUSHROOM),
                new ItemStack(Blocks.BROWN_MUSHROOM));
        check("mushroom stew recipe unchanged", plain.getItem() == Items.MUSHROOM_STEW, plain);
    }

    /** The color 1.12.2's own leather armor dyeing gives an undyed helmet with this one dye. */
    private static int helmetColor(WorldServer world, ItemStack dye) {
        return Items.LEATHER_HELMET.getColor(craft(world, new ItemStack(Items.LEATHER_HELMET), dye));
    }

    private static void expect(WorldServer world, String name, Item result, ItemStack input) {
        ItemStack out = craft(world, input);
        check(name, out.getItem() == result && out.getCount() == 1, out);
    }

    private static Potion effectOf(ItemStack stew) {
        if (!stew.hasTagCompound()) {
            return null;
        }
        NBTTagCompound entry = stew.getTagCompound().getTagList("Effects", 10).getCompoundTagAt(0);
        return Potion.getPotionById(entry.getByte("EffectId"));
    }

    private static int durationOf(ItemStack stew) {
        return stew.hasTagCompound() ? stew.getTagCompound().getTagList("Effects", 10).getCompoundTagAt(0).getInteger("EffectDuration") : -1;
    }

    // ---------------------------------------------------------------------------------------------

    private static void stew(WorldServer world, EntityPlayerMP player) {
        for (StewEffects.Entry entry : StewEffects.table()) {
            player.clearActivePotions();
            player.getFoodStats().setFoodLevel(20);
            ItemStack stew = ItemSuspiciousStew.create(entry.effect, entry.ticks);
            player.setHeldItem(EnumHand.MAIN_HAND, stew);
            ItemStack left = stew.getItem().onItemUseFinish(stew, world, player);
            PotionEffect effect = player.getActivePotionEffect(entry.effect);
            check("eating " + entry.flower.getDisplayName() + " stew", effect != null && effect.getDuration() == entry.ticks
                    && left.getItem() == Items.BOWL, effect);
        }
        player.clearActivePotions();
    }

    // ---------------------------------------------------------------------------------------------

    private static void floor(WorldServer world, BlockPos center, Block block) {
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                world.setBlockState(center.add(dx, -1, dz), block.getDefaultState());
                for (int dy = 0; dy < 4; dy++) {
                    world.setBlockToAir(center.add(dx, dy, dz));
                }
            }
        }
    }

    private static <T extends Entity> T spawn(WorldServer world, T entity, BlockPos pos) {
        entity.setLocationAndAngles(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, 0.0F, 0.0F);
        world.spawnEntity(entity);
        return entity;
    }

    /** A click on an entity, as {@code NetHandlerPlayServer.processUseEntity} delivers it. */
    private static void click(EntityPlayerMP player, Entity target, ItemStack held) {
        player.setHeldItem(EnumHand.MAIN_HAND, held);
        if (ForgeHooks.onInteractEntity(player, target, EnumHand.MAIN_HAND) == null) {
            player.interactOn(target, EnumHand.MAIN_HAND);
        }
    }

    private static void mooshrooms(WorldServer world, EntityPlayerMP player, BlockPos pos) {
        floor(world, pos, Blocks.MYCELIUM);
        EntityMooshroom mooshroom = spawn(world, new EntityMooshroom(world), pos);
        float health = mooshroom.getHealth();
        check("new mooshroom is red", !Mooshrooms.isBrown(mooshroom), "red");

        EntityLightningBolt bolt = new EntityLightningBolt(world, pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, false);
        world.addWeatherEffect(bolt);
        for (int i = 0; i < 4; i++) {
            bolt.onUpdate();
        }
        check("lightning turns a red mooshroom brown, once per bolt", Mooshrooms.isBrown(mooshroom),
                "brown=" + Mooshrooms.isBrown(mooshroom));
        check("lightning does not hurt or burn a mooshroom", mooshroom.getHealth() == health && !mooshroom.isBurning(),
                mooshroom.getHealth());
        NBTTagCompound saved = mooshroom.writeToNBT(new NBTTagCompound());
        EntityMooshroom reloaded = new EntityMooshroom(world);
        reloaded.readFromNBT(saved);
        check("brown survives save and load", Mooshrooms.isBrown(reloaded), saved.getCompoundTag("ForgeData"));

        // Feeding.
        ItemStack flowers = new ItemStack(FloraContent.cornflower, 2);
        click(player, mooshroom, flowers);
        check("brown mooshroom eats a flower", flowers.getCount() == 1 && Mooshrooms.hasStew(mooshroom), flowers);
        click(player, mooshroom, flowers);
        check("second flower is refused", flowers.getCount() == 1, flowers);

        ItemStack bowl = new ItemStack(Items.BOWL);
        click(player, mooshroom, bowl);
        ItemStack held = player.getHeldItemMainhand();
        check("milking gives cornflower stew", held.getItem() == FloraContent.suspiciousStew
                && effectOf(held) == MobEffects.JUMP_BOOST && durationOf(held) == 100, held.getTagCompound());
        check("milking forgets the flower", !Mooshrooms.hasStew(mooshroom), "hasStew=" + Mooshrooms.hasStew(mooshroom));

        click(player, mooshroom, new ItemStack(Items.BOWL));
        check("then plain mushroom stew", player.getHeldItemMainhand().getItem() == Items.MUSHROOM_STEW, player.getHeldItemMainhand());

        // Red mooshroom ignores flowers.
        EntityMooshroom red = spawn(world, new EntityMooshroom(world), pos.add(2, 0, 0));
        ItemStack poppy = new ItemStack(Blocks.RED_FLOWER, 1, 0);
        click(player, red, poppy);
        check("red mooshroom does not eat flowers", poppy.getCount() == 1 && !Mooshrooms.hasStew(red), poppy);
        click(player, red, new ItemStack(Items.BOWL));
        check("red mooshroom still gives mushroom stew", player.getHeldItemMainhand().getItem() == Items.MUSHROOM_STEW,
                player.getHeldItemMainhand());

        // A second bolt converts back.
        EntityLightningBolt second = new EntityLightningBolt(world, pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, false);
        world.addWeatherEffect(second);
        second.onUpdate();
        second.onUpdate();
        check("a new bolt turns it red again", !Mooshrooms.isBrown(mooshroom), "brown=" + Mooshrooms.isBrown(mooshroom));
        Mooshrooms.setBrown(mooshroom, true);
        // The second bolt struck the red one beside it too; it is red again for what follows.
        check("the same bolt converted the mooshroom beside it", Mooshrooms.isBrown(red), "brown=" + Mooshrooms.isBrown(red));
        Mooshrooms.setBrown(red, false);

        // Breeding.
        int brownCalves = 0;
        int mixedBrown = 0;
        for (int i = 0; i < 400; i++) {
            EntityMooshroom calf = new EntityMooshroom(world);
            MinecraftForge.EVENT_BUS.post(new BabyEntitySpawnEvent(mooshroom, reloaded, calf));
            if (Mooshrooms.isBrown(calf)) {
                brownCalves++;
            }
            EntityMooshroom mixed = new EntityMooshroom(world);
            MinecraftForge.EVENT_BUS.post(new BabyEntitySpawnEvent(mooshroom, red, mixed));
            if (Mooshrooms.isBrown(mixed)) {
                mixedBrown++;
            }
        }
        check("two brown parents have brown calves", brownCalves >= 395, brownCalves + "/400");
        check("brown and red parents have either, about evenly", mixedBrown > 150 && mixedBrown < 250, mixedBrown + "/400");

        // Shearing with real shears, through Forge's IShearable path.
        mooshroom.setPosition(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
        ItemStack shears = new ItemStack(Items.SHEARS);
        player.setHeldItem(EnumHand.MAIN_HAND, shears);
        boolean sheared = Items.SHEARS.itemInteractionForEntity(shears, player, mooshroom, EnumHand.MAIN_HAND);
        int brownMushrooms = 0;
        int redMushrooms = 0;
        for (EntityItem item : world.getEntitiesWithinAABB(EntityItem.class, mooshroom.getEntityBoundingBox().grow(3.0D))) {
            if (item.getItem().getItem() == Item.getItemFromBlock(Blocks.BROWN_MUSHROOM)) {
                brownMushrooms += item.getItem().getCount();
            }
            if (item.getItem().getItem() == Item.getItemFromBlock(Blocks.RED_MUSHROOM)) {
                redMushrooms += item.getItem().getCount();
            }
            item.setDead();
        }
        List<EntityCow> cows = world.getEntitiesWithinAABB(EntityCow.class, mooshroom.getEntityBoundingBox().grow(1.0D));
        boolean cow = false;
        for (EntityCow c : cows) {
            cow |= !(c instanceof EntityMooshroom);
        }
        check("sheared brown mooshroom drops 5 brown mushrooms", sheared && brownMushrooms == 5 && redMushrooms == 0 && cow,
                "brown=" + brownMushrooms + " red=" + redMushrooms + " cow=" + cow);

        Items.SHEARS.itemInteractionForEntity(new ItemStack(Items.SHEARS), player, red, EnumHand.MAIN_HAND);
        int redAgain = 0;
        for (EntityItem item : world.getEntitiesWithinAABB(EntityItem.class, red.getEntityBoundingBox().grow(3.0D))) {
            if (item.getItem().getItem() == Item.getItemFromBlock(Blocks.RED_MUSHROOM)) {
                redAgain += item.getItem().getCount();
            }
            item.setDead();
        }
        check("sheared red mooshroom still drops red mushrooms", redAgain == 5, redAgain);
    }

    // ---------------------------------------------------------------------------------------------

    private static void horses(WorldServer world, EntityPlayerMP player, BlockPos pos) {
        floor(world, pos, Blocks.GRASS);
        EntityHorse horse = spawn(world, new EntityHorse(world), pos);
        horse.setHorseTamed(true);
        ItemStack armor = new ItemStack(FloraContent.leatherHorseArmor);
        ItemLeatherHorseArmor.setColor(armor, 0x123456);
        check("horse accepts leather horse armor", horse.isArmor(armor) && horse.replaceItemInInventory(401, armor), armor);
        check("leather horse armor gives 3 armor points", horse.getTotalArmorValue() == 3, horse.getTotalArmorValue());
        check("horse reports the leather armor type", horse.getHorseArmorType() == ItemLeatherHorseArmor.type(),
                horse.getHorseArmorType());
        NBTTagCompound saved = horse.writeToNBT(new NBTTagCompound());
        EntityHorse reloaded = new EntityHorse(world);
        reloaded.readFromNBT(saved);
        check("horse armor survives save and load", reloaded.getTotalArmorValue() == 3, saved.getCompoundTag("ArmorItem"));

        // Washing in a cauldron.
        BlockPos cauldron = pos.add(2, 0, 2);
        world.setBlockState(cauldron, Blocks.CAULDRON.getDefaultState().withProperty(BlockCauldron.LEVEL, 3));
        ItemStack dirty = new ItemStack(FloraContent.leatherHorseArmor);
        ItemLeatherHorseArmor.setColor(dirty, 0xFF0000);
        player.setHeldItem(EnumHand.MAIN_HAND, dirty);
        ForgeHooks.onRightClickBlock(player, EnumHand.MAIN_HAND, cauldron, EnumFacing.UP, new Vec3d(0.5D, 1.0D, 0.5D));
        check("cauldron washes leather horse armor", !ItemLeatherHorseArmor.hasColor(dirty)
                && world.getBlockState(cauldron).getValue(BlockCauldron.LEVEL) == 2,
                "level=" + world.getBlockState(cauldron).getValue(BlockCauldron.LEVEL));
    }

    // ---------------------------------------------------------------------------------------------

    private static void signs(WorldServer world, EntityPlayerMP player, BlockPos pos) {
        floor(world, pos, Blocks.STONE);
        world.setBlockState(pos, Blocks.STANDING_SIGN.getDefaultState());
        TileEntitySign sign = (TileEntitySign) world.getTileEntity(pos);
        check("new sign text is black", SignColor.get(sign) == EnumDyeColor.BLACK && SignColor.textColor(sign) == 0, "black");

        ItemStack red = new ItemStack(Items.DYE, 2, EnumDyeColor.RED.getDyeDamage());
        player.setHeldItem(EnumHand.MAIN_HAND, red);
        ForgeHooks.onRightClickBlock(player, EnumHand.MAIN_HAND, pos, EnumFacing.NORTH, new Vec3d(0.5D, 0.5D, 0.5D));
        check("red dye dyes sign text", SignColor.get(sign) == EnumDyeColor.RED && red.getCount() == 1,
                Integer.toHexString(SignColor.textColor(sign)));
        ForgeHooks.onRightClickBlock(player, EnumHand.MAIN_HAND, pos, EnumFacing.NORTH, new Vec3d(0.5D, 0.5D, 0.5D));
        check("same color is free", red.getCount() == 1, red);

        ItemStack blue = new ItemStack(FloraContent.blueDye);
        player.setHeldItem(EnumHand.MAIN_HAND, blue);
        ForgeHooks.onRightClickBlock(player, EnumHand.MAIN_HAND, pos, EnumFacing.NORTH, new Vec3d(0.5D, 0.5D, 0.5D));
        check("new blue dye dyes sign text", SignColor.get(sign) == EnumDyeColor.BLUE && blue.isEmpty()
                && SignColor.textColor(sign) == 0x000066, Integer.toHexString(SignColor.textColor(sign)));

        NBTTagCompound update = sign.getUpdateTag();
        TileEntitySign copy = new TileEntitySign();
        copy.readFromNBT(update);
        check("sign color is in the update tag and survives reload", SignColor.get(copy) == EnumDyeColor.BLUE,
                update.getCompoundTag("ForgeData"));
    }

    // ---------------------------------------------------------------------------------------------

    private static void pots(WorldServer world, EntityPlayerMP player, BlockPos pos) {
        floor(world, pos, Blocks.STONE);
        world.setBlockState(pos, Blocks.FLOWER_POT.getDefaultState());
        ItemStack flower = new ItemStack(FloraContent.witherRose, 2);
        player.setHeldItem(EnumHand.MAIN_HAND, flower);
        ForgeHooks.onRightClickBlock(player, EnumHand.MAIN_HAND, pos, EnumFacing.UP, new Vec3d(0.5D, 0.4D, 0.5D));
        check("wither rose goes into a flower pot", world.getBlockState(pos).getBlock() == FloraContent.pottedWitherRose
                && flower.getCount() == 1, world.getBlockState(pos));

        player.setHeldItem(EnumHand.MAIN_HAND, ItemStack.EMPTY);
        world.getBlockState(pos).getBlock().onBlockActivated(world, pos, world.getBlockState(pos), player,
                EnumHand.MAIN_HAND, EnumFacing.UP, 0.5F, 0.4F, 0.5F);
        check("taking it out gives the rose back and leaves an empty pot",
                world.getBlockState(pos).getBlock() == Blocks.FLOWER_POT
                        && player.getHeldItemMainhand().getItem() == Item.getItemFromBlock(FloraContent.witherRose),
                world.getBlockState(pos) + " " + player.getHeldItemMainhand());

        world.setBlockState(pos, FloraContent.pottedCornflower.getDefaultState());
        net.minecraft.util.NonNullList<ItemStack> drops = net.minecraft.util.NonNullList.create();
        FloraContent.pottedCornflower.getDrops(drops, world, pos, world.getBlockState(pos), 0);
        check("a broken potted cornflower drops the pot and the flower", drops.size() == 2, drops);
    }

    // ---------------------------------------------------------------------------------------------

    private static void witherRose(WorldServer world, BlockPos pos) {
        floor(world, pos, Blocks.NETHERRACK);
        check("wither rose stands on netherrack", FloraContent.witherRose.canPlaceBlockAt(world, pos), "netherrack");
        world.setBlockState(pos.add(1, -1, 0), Blocks.STONE.getDefaultState());
        check("but not on stone", !FloraContent.witherRose.canPlaceBlockAt(world, pos.add(1, 0, 0)), "stone");
        check("cornflower does not stand on netherrack", !FloraContent.cornflower.canPlaceBlockAt(world, pos), "netherrack");

        world.setBlockState(pos, FloraContent.witherRose.getDefaultState());
        EntityZombie zombie = spawn(world, new EntityZombie(world), pos);
        FloraContent.witherRose.onEntityCollision(world, pos, world.getBlockState(pos), zombie);
        PotionEffect withered = zombie.getActivePotionEffect(MobEffects.WITHER);
        check("touching a wither rose withers a zombie", withered != null && withered.getDuration() == 40, withered);

        EntityWitherSkeleton skeleton = spawn(world, new EntityWitherSkeleton(world), pos);
        FloraContent.witherRose.onEntityCollision(world, pos, world.getBlockState(pos), skeleton);
        check("but not a wither skeleton", skeleton.getActivePotionEffect(MobEffects.WITHER) == null, "immune");
        zombie.setDead();
        skeleton.setDead();
        world.setBlockToAir(pos);

        // A Wither kill.
        EntityWither wither = new EntityWither(world);
        wither.setLocationAndAngles(pos.getX() + 0.5D, pos.getY() + 30, pos.getZ() + 0.5D, 0.0F, 0.0F);
        EntityZombie victim = spawn(world, new EntityZombie(world), pos.add(1, 0, 1));
        victim.setNoAI(true);
        victim.attackEntityFrom(DamageSource.causeMobDamage(wither), 1000.0F);
        check("a mob the Wither kills leaves a planted wither rose",
                world.getBlockState(pos.add(1, 0, 1)).getBlock() == FloraContent.witherRose,
                world.getBlockState(pos.add(1, 0, 1)));

        world.setBlockState(pos.add(2, 0, 2), Blocks.STONE.getDefaultState());
        EntityZombie onStone = spawn(world, new EntityZombie(world), pos.add(2, 1, 2));
        world.setBlockState(pos.add(2, 0, 2), Blocks.STONE.getDefaultState());
        onStone.setNoAI(true);
        onStone.attackEntityFrom(DamageSource.causeMobDamage(wither), 1000.0F);
        boolean dropped = false;
        for (EntityItem item : world.getEntitiesWithinAABB(EntityItem.class, onStone.getEntityBoundingBox().grow(2.0D))) {
            dropped |= item.getItem().getItem() == Item.getItemFromBlock(FloraContent.witherRose);
        }
        check("where it cannot be planted it drops as an item", dropped, "dropped=" + dropped);

        EntityZombie other = spawn(world, new EntityZombie(world), pos.add(-2, 0, -2));
        other.attackEntityFrom(DamageSource.GENERIC, 1000.0F);
        check("other deaths leave nothing", world.isAirBlock(pos.add(-2, 0, -2)), world.getBlockState(pos.add(-2, 0, -2)));
    }

    // ---------------------------------------------------------------------------------------------

    private static void animals(WorldServer world, EntityPlayerMP player, BlockPos pos) {
        floor(world, pos, Blocks.GRASS);
        EntitySheep sheep = spawn(world, new EntitySheep(world), pos);
        sheep.setFleeceColor(EnumDyeColor.WHITE);
        ItemStack brown = new ItemStack(FloraContent.brownDye);
        click(player, sheep, brown);
        check("brown dye dyes a sheep", sheep.getFleeceColor() == EnumDyeColor.BROWN && brown.isEmpty(), sheep.getFleeceColor());

        EntityWolf wolf = spawn(world, new EntityWolf(world), pos.add(2, 0, 0));
        wolf.setTamedBy(player);
        ItemStack blue = new ItemStack(FloraContent.blueDye);
        click(player, wolf, blue);
        check("blue dye dyes a tamed wolf's collar", wolf.getCollarColor() == EnumDyeColor.BLUE && blue.isEmpty()
                && !wolf.isSitting(), wolf.getCollarColor());

        // Bone meal growing flowers in plains.
        Biome plains = Biomes.PLAINS;
        world.setBlockState(pos.add(-2, -1, -2), Blocks.GRASS.getDefaultState());
        int cornflowers = 0;
        Random random = new Random(1);
        for (int i = 0; i < 400; i++) {
            world.setBlockToAir(pos.add(-2, 0, -2));
            plains.plantFlower(world, random, pos.add(-2, 0, -2));
            if (world.getBlockState(pos.add(-2, 0, -2)).getBlock() == FloraContent.cornflower) {
                cornflowers++;
            }
        }
        check("plains bone meal can grow cornflowers", cornflowers > 10, cornflowers + "/400");
        int lilies = 0;
        for (int i = 0; i < 400; i++) {
            world.setBlockToAir(pos.add(-2, 0, -2));
            Biomes.FOREST.plantFlower(world, random, pos.add(-2, 0, -2));
            if (world.getBlockState(pos.add(-2, 0, -2)).getBlock() == FloraContent.lilyOfTheValley) {
                lilies++;
            }
        }
        check("plain forest bone meal does not grow lilies", lilies == 0, lilies);
        lilies = 0;
        for (int i = 0; i < 400; i++) {
            world.setBlockToAir(pos.add(-2, 0, -2));
            Biomes.MUTATED_FOREST.plantFlower(world, random, pos.add(-2, 0, -2));
            if (world.getBlockState(pos.add(-2, 0, -2)).getBlock() == FloraContent.lilyOfTheValley) {
                lilies++;
            }
        }
        check("flower forest bone meal grows lilies", lilies > 5, lilies + "/400");
    }

    // ---------------------------------------------------------------------------------------------

    private static void trades() {
        Random random = new Random(7);
        List<MerchantRecipe> farmer = offers("minecraft:farmer/farmer", 3, random);
        int stews = 0;
        for (MerchantRecipe recipe : farmer) {
            if (recipe.getItemToSell().getItem() == FloraContent.suspiciousStew && effectOf(recipe.getItemToSell()) != null
                    && recipe.getItemToBuy().getCount() == 1) {
                stews++;
            }
        }
        check("expert farmer pool has six suspicious stews", stews == 6, stews + " of " + farmer.size());

        boolean horseArmor = false;
        for (MerchantRecipe recipe : offers("minecraft:butcher/leather", 3, random)) {
            horseArmor |= recipe.getItemToSell().getItem() == FloraContent.leatherHorseArmor
                    && ItemLeatherHorseArmor.hasColor(recipe.getItemToSell()) && recipe.getItemToBuy().getCount() == 6;
        }
        check("expert leatherworker sells dyed leather horse armor for 6", horseArmor, "horseArmor=" + horseArmor);

        boolean globe = false;
        for (MerchantRecipe recipe : offers("minecraft:librarian/cartographer", 4, random)) {
            globe |= recipe.getItemToSell().getItem() == FloraContent.globeBannerPattern && recipe.getItemToBuy().getCount() == 8;
        }
        check("master cartographer sells the Globe pattern for 8", globe, "globe=" + globe);

        boolean whiteDye = false;
        for (MerchantRecipe recipe : offers("minecraft:farmer/shepherd", 1, random)) {
            whiteDye |= recipe.getItemToBuy().getItem() == FloraContent.whiteDye;
        }
        check("apprentice shepherd buys 1.14's white dye", whiteDye, "whiteDye=" + whiteDye);
    }

    private static List<MerchantRecipe> offers(String career, int level, Random random) {
        List<MerchantRecipe> out = new ArrayList<MerchantRecipe>();
        List<List<EntityVillager.ITradeList>> tables = ModernTrades.asCareerTrades(career);
        if (tables == null || tables.size() <= level) {
            return out;
        }
        for (EntityVillager.ITradeList trade : tables.get(level)) {
            MerchantRecipeList list = new MerchantRecipeList();
            trade.addMerchantRecipe(null, list, random);
            out.addAll(list);
        }
        return out;
    }

    // ---------------------------------------------------------------------------------------------

    private static void worldgen(WorldServer world) {
        check("generator registered", FloraWorldGen.active(), "active");
        Random random = new Random(3);
        count(world, random, Biomes.PLAINS, FloraContent.cornflower, "cornflowers in plains");
        count(world, random, Biomes.FOREST, FloraContent.lilyOfTheValley, "lilies of the valley in forest");
    }

    /** Finds the biome, loads chunks there so they populate, and counts the flower in them. */
    private static void count(WorldServer world, Random random, Biome biome, Block flower, String name) {
        BlockPos found = world.getBiomeProvider().findBiomePosition(4000, 4000, 3000, Arrays.asList(biome), random);
        if (found == null) {
            check(name, false, "no " + biome.getBiomeName() + " found");
            return;
        }
        int cx = found.getX() >> 4;
        int cz = found.getZ() >> 4;
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                world.getChunkProvider().provideChunk(cx + dx, cz + dz);
            }
        }
        int flowers = 0;
        int chunks = 0;
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                net.minecraft.world.chunk.Chunk chunk = world.getChunk(cx + dx, cz + dz);
                if (!chunk.isTerrainPopulated()) {
                    continue;
                }
                chunks++;
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        for (int y = 50; y < 140; y++) {
                            if (chunk.getBlockState(x, y, z).getBlock() == flower) {
                                flowers++;
                            }
                        }
                    }
                }
            }
        }
        check(name, flowers > 0, flowers + " in " + chunks + " populated chunks near " + found);
    }

}
