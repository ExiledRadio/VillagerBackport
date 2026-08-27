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
        // A client without the mod can still join a server that has it: the trading changes are all
        // server-side, and the server sends the resulting trade list the way it always does. Such a
        // client will not see this mod's blocks, so a server using them should expect it installed.
        acceptableRemoteVersions = "*"
)
public final class VillagerBackport {

    public static final String MOD_ID = "villagerbackport";
    public static final String MOD_NAME = "1.14 Villager Backport";
    public static final String VERSION = "@VERSION@";

    public static final Logger LOGGER = LogManager.getLogger(MOD_NAME);

    /** Required by Forge's GUI system, which keys screens on the owning mod instance. */
    @Mod.Instance(MOD_ID)
    public static VillagerBackport instance;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        // Resolve the private villager fields once, up front. If this fails the handler below
        // still registers but every villager check short-circuits, so the pack loads normally
        // and the failure is one log line rather than an exception per villager per tick.
        VillagerAccess.init();

        NetworkHandler.init();
        net.minecraftforge.fml.common.network.NetworkRegistry.INSTANCE.registerGuiHandler(this, new GuiHandler());

        MinecraftForge.EVENT_BUS.register(new RestockHandler());
        MinecraftForge.EVENT_BUS.register(new PricingHandler());
        MinecraftForge.EVENT_BUS.register(new ModConfig.EventHandler());
        MinecraftForge.EVENT_BUS.register(new com.exiledradio.villagerbackport.trade.LevelSyncHandler());
        MinecraftForge.EVENT_BUS.register(new com.exiledradio.villagerbackport.trade.ReputationEvents());
        MinecraftForge.EVENT_BUS.register(new com.exiledradio.villagerbackport.trade.Zombification());
        MinecraftForge.EVENT_BUS.register(new com.exiledradio.villagerbackport.home.HomeClaims());
        MinecraftForge.EVENT_BUS.register(new com.exiledradio.villagerbackport.home.SleepSync());
        MinecraftForge.EVENT_BUS.register(new com.exiledradio.villagerbackport.home.BedInteraction());
        MinecraftForge.EVENT_BUS.register(new com.exiledradio.villagerbackport.home.VillageWatch());
        MinecraftForge.EVENT_BUS.register(new com.exiledradio.villagerbackport.home.PanicWatch());
        MinecraftForge.EVENT_BUS.register(new com.exiledradio.villagerbackport.home.BedBreeding());
        MinecraftForge.EVENT_BUS.register(new com.exiledradio.villagerbackport.job.JobSiteAttacher());
        MinecraftForge.EVENT_BUS.register(new com.exiledradio.villagerbackport.job.JobSiteClaims());
        MinecraftForge.EVENT_BUS.register(com.exiledradio.villagerbackport.job.WorkstationIndex.class);

        // Before anything can load a world: the piece names have to be known to the save format
        // ahead of a village being read back, and the creation handlers ahead of one being laid out.
        com.exiledradio.villagerbackport.village.VillagePieces.register();
        com.exiledradio.villagerbackport.village.StructureWorkstations.register();

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
        com.exiledradio.villagerbackport.block.StonecutterRecipes.invalidate();
    }

    /** Registers the commands this mod adds, which is the one that draws a village. */
    @Mod.EventHandler
    public void serverStarted(net.minecraftforge.fml.common.event.FMLServerStartingEvent event) {
        event.registerServerCommand(new com.exiledradio.villagerbackport.home.VillageCommand());
    }

}
