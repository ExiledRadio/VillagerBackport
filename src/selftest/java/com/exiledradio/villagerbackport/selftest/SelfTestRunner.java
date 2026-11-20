package com.exiledradio.villagerbackport.selftest;

import com.exiledradio.villagerbackport.VillagerBackport;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Arrays;
import java.util.List;

/**
 * Runs the headless self-tests on a dedicated dev server, then stops it:
 * <pre>    ./gradlew runServer -Pselftest=all      (delete eclipse/world first)</pre>
 * Waits a second after the server starts, runs each requested test, logs one line per failure and
 * {@code SELFTEST DONE pass=N fail=M}. Compiled in only with {@code -Pselftest}; never in a release jar.
 */
public final class SelfTestRunner {

    /** Every test. Add a line per new one. */
    private static final SelfTest[] TESTS = {
            new LookFeelSelfTest(),
            new WaterlogSelfTest(),
            new PickleMoveSelfTest(),
            new LocateSelfTest(),
            new Templates118SelfTest(),
            new FisherPoolSelfTest(),
            new FlatSelfTest(),
            new StewSelfTest(),
            new SnowSelfTest(),
            new JobPathSelfTest(),
            new MechanicsSelfTest(),
    };

    private final List<String> wanted;
    private int ticks;

    private SelfTestRunner(List<String> wanted) {
        this.wanted = wanted;
    }

    public static void start(String which) {
        VillagerBackport.LOGGER.info("SELFTEST requested: {}", which);
        if ("jobvillage".equals(which)) {
            // A measurement in a generated village; see JobVillageBench.
            JobVillageBench.start();
            return;
        }
        if (which.startsWith("night")) {
            // A tick-driven measurement, not a pass/fail test; see NightBench.
            NightBench.start(which);
            return;
        }
        MinecraftForge.EVENT_BUS.register(new SelfTestRunner(Arrays.asList(which.split(","))));
    }

    @SubscribeEvent
    public void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || ++this.ticks != 20) {
            return;
        }
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        Check check = new Check();
        for (SelfTest test : TESTS) {
            if (this.wanted.contains("all") || this.wanted.contains(test.name())) {
                check.section(test.name());
                try {
                    test.run(check);
                } catch (Throwable t) {
                    VillagerBackport.LOGGER.error("SELFTEST {} threw", test.name(), t);
                    check.fail(test.name() + " threw " + t);
                }
            }
        }
        VillagerBackport.LOGGER.info("SELFTEST DONE pass={} fail={}", check.passed(), check.failed());
        server.initiateShutdown();
    }
}
