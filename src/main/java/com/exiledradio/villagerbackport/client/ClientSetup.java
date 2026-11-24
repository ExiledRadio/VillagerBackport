package com.exiledradio.villagerbackport.client;

import com.exiledradio.villagerbackport.block.TileEntityBell;
import com.exiledradio.villagerbackport.block.TileEntityLectern;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Everything that only exists on a client, kept in a class a server never loads.
 *
 * <h2>Why a separate class and not a separate method</h2>
 * This began as a method on the mod class, guarded so a server would never call it. That is not
 * enough, and the mod crashed every dedicated server on startup because of it.
 *
 * <p>The reason is that the JVM verifies bytecode a whole class at a time, before any of it runs.
 * Verifying a method body means checking that each argument is assignable to the parameter it is
 * passed as, and answering that question requires loading the parameter's type. So
 * {@code ClientRegistry.bindTileEntitySpecialRenderer(Class, TileEntitySpecialRenderer)} pulls
 * {@code TileEntitySpecialRenderer} in while the mod class is being verified - which is long before
 * the guard that would have stopped it being called. Forge's side transformer then refuses to hand
 * over a client-only class on a server, and loading the mod fails.
 *
 * <p>Moving it to its own class is what actually works: nothing here is named in any descriptor the
 * mod class carries, so a server verifying that class has no reason to look at this one. The call
 * that reaches it takes no arguments and returns nothing, which is resolved when it first runs
 * rather than when the caller is verified - and on a server it never runs.
 *
 * <p>The same trap is why {@link com.exiledradio.villagerbackport.GuiHandler} declares its screens as
 * {@code Object}: assignability to {@code Object} is the one case the verifier can answer without
 * loading anything.
 */
@SideOnly(Side.CLIENT)
public final class ClientSetup {

    private ClientSetup() {
    }

    public static void init() {
        com.exiledradio.villagerbackport.mechanics.client.MechanicsClient.init();
        MinecraftForge.EVENT_BUS.register(new MerchantOverlay());
        MinecraftForge.EVENT_BUS.register(new MapLockTooltip());
        MinecraftForge.EVENT_BUS.register(new LayerInstaller());
        MinecraftForge.EVENT_BUS.register(new VillagerLevelCache());
        MinecraftForge.EVENT_BUS.register(new SleepCache());
        MinecraftForge.EVENT_BUS.register(new SleepPoseClient());
        MinecraftForge.EVENT_BUS.register(new SleepRenderer());
        MinecraftForge.EVENT_BUS.register(new VillageOutlineRenderer());
        MinecraftForge.EVENT_BUS.register(new CrossbowClient());
        CrossbowClient.registerRenderers();
        com.exiledradio.villagerbackport.bamboo.client.BambooClient.init();
        // `runClient -Pselftest=client`: the look-and-feel title-screen check, reached by name (a release jar has no test classes).
        String selftest = System.getProperty(com.exiledradio.villagerbackport.VillagerBackport.MOD_ID + ".selftest");
        // `-Pselftest=clientbabies`: 26.1's babies photographed in a flat world instead.
        if (selftest != null && selftest.contains("client")) {
            try {
                Class.forName("com.exiledradio.villagerbackport.selftest."
                        + (selftest.equals("clientbabies") ? "ClientBabiesTest" : "ClientLookFeelTest")).getMethod("register").invoke(null);
            } catch (ReflectiveOperationException e) {
                com.exiledradio.villagerbackport.VillagerBackport.LOGGER.error("CLIENT SELFTEST DONE pass=0 fail=1: the test classes are not in this build", e);
            }
        }

        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityBell.class, new TileEntityBellRenderer());
        net.minecraftforge.fml.client.registry.RenderingRegistry.registerEntityRenderingHandler(
                com.exiledradio.villagerbackport.illager.EntityPillager.class, RenderPillager::new);
        net.minecraftforge.fml.client.registry.RenderingRegistry.registerEntityRenderingHandler(
                com.exiledradio.villagerbackport.illager.EntityRavager.class, RenderRavager::new);
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityLectern.class, new TileEntityLecternRenderer());
    }
}
