package com.exiledradio.villagerbackport;

import com.exiledradio.villagerbackport.compat.VillagerAccess;
import com.exiledradio.villagerbackport.network.NetworkHandler;
import com.exiledradio.villagerbackport.restock.RestockHandler;
import com.exiledradio.villagerbackport.trade.PricingHandler;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Villager Trading Revamp - 1.14's village trading behaviour, rebuilt for 1.12.2.
 *
 * <p>Covers restocking on 1.14's clock, supply-and-demand pricing, villager experience and levels,
 * 1.14's trade screen and villager appearance, employment from workstations, and the twelve
 * workstation blocks themselves.
 *
 * <p>Nothing here patches a class or replaces an entity. Villager behaviour is reached through
 * events and reflection into the fields vanilla already keeps, so trades contributed by any other
 * mod are restocked and priced on the same terms without this mod knowing they exist - which is what
 * lets it sit in a pack of two hundred mods without arguing with any of them.
 *
 * <p>The blocks are its own rather than borrowed from mods that already back-port them, so it runs
 * on a bare 1.12.2 install with nothing else present.
 */
@Mod(
        modid = VillagerBackport.MOD_ID,
        name = VillagerBackport.MOD_NAME,
        version = VillagerBackport.VERSION,
        acceptedMinecraftVersions = "[1.12.2]",
        // Backport Core holds the code the backports share: textures, Future MC migration, duplicate
        // hiding, smithing upgrades and the terrain the villages are planned on.
        dependencies = "required-after:backportcore@[1.1.0,);required-after:fluidlogged_api@[3.3.3,)",
        // A client without the mod can still join a server that has it: the trading changes are all
        // server-side, and the server sends the resulting trade list the way it always does. Such a
        // client will not see this mod's blocks, so a server using them should expect it installed.
        acceptableRemoteVersions = "*"
)
public final class VillagerBackport {

    public static final String MOD_ID = "villagerbackport";
    public static final String MOD_NAME = "Village & Pillage Backport";
    public static final String VERSION = "@VERSION@";

    public static final Logger LOGGER = LogManager.getLogger(MOD_NAME);

    /** Required by Forge's GUI system, which keys screens on the owning mod instance. */
    @Mod.Instance(MOD_ID)
    public static VillagerBackport instance;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        // 1.14's structure placement numbers, for Backport Core's table (seed parity with 1.14).
        com.exiledradio.villagerbackport.worldgen.Placement114.register();
        // Resolve the private villager fields once, up front. If this fails the handler below
        // still registers but every villager check short-circuits, so the pack loads normally
        // and the failure is one log line rather than an exception per villager per tick.
        VillagerAccess.init();

        NetworkHandler.init();

        // 1.14's look and feel (panorama, splashes, options, F3 keys) for Backport Core's presentation module.
        com.exiledradio.villagerbackport.lookfeel.LookAndFeel.preInit();

        // What this mod tells Backport Core: its Future MC pairs, both for hiding duplicates and for
        // converting worlds; how the smithing table is set up; and that its 1.13 blocks (which Core's
        // ownership table says it owns) are only there with villageBlocks.enabled on.
        com.exiledradio.villagerbackport.compat.FutureMc.register();
        com.exiledradio.villagerbackport.migrate.FutureMcPairs.register();
        com.exiledradio.backportcore.smithing.SmithingUpgrades.configure(
                () -> ModConfig.workstations.netheriteUpgrades,
                () -> ModConfig.workstations.netheriteNameMatching,
                () -> ModConfig.workstations.removeCraftingUpgrades);
        com.exiledradio.backportcore.ownership.BlockOwnership.provides(MOD_ID,
                com.exiledradio.villagerbackport.woodset.WoodsetContent::enabled);
        net.minecraftforge.fml.common.network.NetworkRegistry.INSTANCE.registerGuiHandler(this, new GuiHandler());

        MinecraftForge.EVENT_BUS.register(new RestockHandler());
        MinecraftForge.EVENT_BUS.register(new PricingHandler());
        MinecraftForge.EVENT_BUS.register(new ModConfig.EventHandler());
        MinecraftForge.EVENT_BUS.register(new com.exiledradio.villagerbackport.trade.LevelSyncHandler());
        MinecraftForge.EVENT_BUS.register(new com.exiledradio.villagerbackport.trade.ReputationEvents());
        MinecraftForge.EVENT_BUS.register(new com.exiledradio.villagerbackport.trade.Zombification());
        MinecraftForge.EVENT_BUS.register(new com.exiledradio.villagerbackport.compat.ConvertedVillagers());
        MinecraftForge.EVENT_BUS.register(new com.exiledradio.villagerbackport.home.HomeClaims());
        MinecraftForge.EVENT_BUS.register(new com.exiledradio.villagerbackport.home.SleepSync());
        MinecraftForge.EVENT_BUS.register(new com.exiledradio.villagerbackport.home.BedInteraction());
        MinecraftForge.EVENT_BUS.register(new com.exiledradio.villagerbackport.home.VillageWatch());
        MinecraftForge.EVENT_BUS.register(new com.exiledradio.villagerbackport.home.PanicWatch());
        MinecraftForge.EVENT_BUS.register(new com.exiledradio.villagerbackport.home.SleepPose());
        MinecraftForge.EVENT_BUS.register(new com.exiledradio.villagerbackport.trade.RefusalHandler());
        MinecraftForge.EVENT_BUS.register(new com.exiledradio.villagerbackport.job.SpawnSource());
        MinecraftForge.EVENT_BUS.register(new com.exiledradio.villagerbackport.home.Persistence());
        MinecraftForge.EVENT_BUS.register(new com.exiledradio.villagerbackport.trade.TradeExperience());
        MinecraftForge.EVENT_BUS.register(new com.exiledradio.villagerbackport.trade.ShowWares());
        MinecraftForge.EVENT_BUS.register(new com.exiledradio.villagerbackport.home.BedBreeding());
        MinecraftForge.EVENT_BUS.register(new com.exiledradio.villagerbackport.job.JobSiteAttacher());
        MinecraftForge.EVENT_BUS.register(new com.exiledradio.villagerbackport.job.JobSiteClaims());
        MinecraftForge.EVENT_BUS.register(new com.exiledradio.villagerbackport.job.MobGriefingExemption());
        MinecraftForge.EVENT_BUS.register(new com.exiledradio.villagerbackport.data.VillagerTypes());
        MinecraftForge.EVENT_BUS.register(com.exiledradio.villagerbackport.job.WorkstationIndex.class);
        MinecraftForge.EVENT_BUS.register(new com.exiledradio.villagerbackport.raid.Raids());
        MinecraftForge.EVENT_BUS.register(new com.exiledradio.villagerbackport.raid.RaidEvents());
        MinecraftForge.EVENT_BUS.register(new com.exiledradio.villagerbackport.illager.PatrolSpawner());
        MinecraftForge.EVENT_BUS.register(new com.exiledradio.villagerbackport.crossbow.CrossbowAdvancements());
        // Before anything can load a world: the piece names have to be known to the save format
        // ahead of a village being read back, and the creation handlers ahead of one being laid out.
        com.exiledradio.villagerbackport.village.VillagePieces.register();
        com.exiledradio.villagerbackport.village.modern.MapGenModernVillage.register();
        com.exiledradio.villagerbackport.village.modern.OutpostGenerator.register();

        // Client-only rendering lives in a class of its own that a server never loads. The call
        // itself is safe to compile in here because it names no client type - see ClientSetup for
        // why a guarded method on this class was not enough, and crashed dedicated servers.
        if (event.getSide().isClient()) {
            com.exiledradio.villagerbackport.client.ClientSetup.init();
        }

        if (VillagerAccess.isAvailable()) {
            LOGGER.info("Villager restocking active (max {}/day, {} tick cooldown).",
                    ModConfig.restock.maxRestocksPerDay, ModConfig.restock.cooldownTicks);
        }
    }

    /**
     * Careers are added here rather than in preInit because the vanilla profession they attach to has
     * to exist first, and registries are only populated once preInit is over.
     */
    @Mod.EventHandler
    public void init(net.minecraftforge.fml.common.event.FMLInitializationEvent event) {
        com.exiledradio.villagerbackport.job.MasonCareer.register();
        com.exiledradio.villagerbackport.fox.FoxContent.init();
    }

    /** Catches recipes for hidden duplicates that were added after the recipe registry event. */
    @Mod.EventHandler
    public void postInit(net.minecraftforge.fml.common.event.FMLPostInitializationEvent event) {
        com.exiledradio.villagerbackport.compat.FutureMc.removeLateRecipes();
        com.exiledradio.villagerbackport.block.CropSounds.apply();
        com.exiledradio.backportcore.smithing.SmithingUpgrades.registerIngots();
        com.exiledradio.villagerbackport.raid.Raids.init();
    }

    /** Every mod's recipes are in by now, so the smithing table's upgrades can be gathered. */
    @Mod.EventHandler
    public void loadComplete(net.minecraftforge.fml.common.event.FMLLoadCompleteEvent event) {
        com.exiledradio.backportcore.smithing.SmithingUpgrades.isActive();
    }

    /**
     * Drops both saws' tables so the next use rebuilds them.
     *
     * <p>It is worked out from the crafting recipes in the registry, and a client's copy of that
     * registry belongs to whatever server it last joined - so a table built against the previous one
     * would offer shapes this world has never heard of.
     */
    @Mod.EventHandler
    public void serverStarting(net.minecraftforge.fml.common.event.FMLServerAboutToStartEvent event) {
        // Belt and braces: if another JEI plugin crashed JEI's startup, its runtime never arrived and
        // the careers' own trades were never put back. No world may build trades from the 1.14 view.
        com.exiledradio.villagerbackport.compat.JeiTradeLists.restore();

        com.exiledradio.villagerbackport.block.StonecutterRecipes.invalidate();
    }

    /**
     * Starts the headless self-test when a test build asks for one ({@code -Pselftest=all}). Reached by name,
     * so a release jar, which has no test classes, never links against them.
     */
    @Mod.EventHandler
    public void selfTest(net.minecraftforge.fml.common.event.FMLServerStartedEvent event) {
        String selftest = System.getProperty(MOD_ID + ".selftest");
        if (selftest == null || selftest.isEmpty() || selftest.equals("client")) {
            return;
        }
        try {
            Class.forName("com.exiledradio.villagerbackport.selftest.SelfTestRunner").getMethod("start", String.class).invoke(null, selftest);
        } catch (ReflectiveOperationException e) {
            LOGGER.error("SELFTEST DONE pass=0 fail=1: '{}' requested but the test classes are not in this build.", selftest, e);
            net.minecraftforge.fml.common.FMLCommonHandler.instance().getMinecraftServerInstance().initiateShutdown();
        }
    }

    /** Registers the commands this mod adds: the one that draws a village, /forgive, /librarian and /discount. */
    @Mod.EventHandler
    public void serverStarted(net.minecraftforge.fml.common.event.FMLServerStartingEvent event) {
        event.registerServerCommand(new com.exiledradio.villagerbackport.home.VillageCommand());
        event.registerServerCommand(new com.exiledradio.villagerbackport.trade.ForgiveCommand());
        event.registerServerCommand(new com.exiledradio.villagerbackport.trade.LibrarianCommand());
        event.registerServerCommand(new com.exiledradio.villagerbackport.trade.DiscountCommand());
        event.registerServerCommand(new com.exiledradio.villagerbackport.raid.RaidCommand());
        if (com.exiledradio.villagerbackport.trader.TraderContent.enabled()) {
            event.registerServerCommand(new com.exiledradio.villagerbackport.trader.TraderCommand());
        }
        // Registered after vanilla's, so it takes the /locate name over.
        event.registerServerCommand(new com.exiledradio.villagerbackport.village.modern.LocateCommand());
    }

}
