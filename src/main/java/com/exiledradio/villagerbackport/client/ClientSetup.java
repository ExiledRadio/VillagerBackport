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
        MinecraftForge.EVENT_BUS.register(new MerchantOverlay());
        MinecraftForge.EVENT_BUS.register(new LayerInstaller());
        MinecraftForge.EVENT_BUS.register(new VillagerLevelCache());
        MinecraftForge.EVENT_BUS.register(new SleepCache());
        MinecraftForge.EVENT_BUS.register(new SleepRenderer());
        MinecraftForge.EVENT_BUS.register(new VillageOutlineRenderer());

        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityBell.class, new TileEntityBellRenderer());
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityLectern.class, new TileEntityLecternRenderer());
    }
}
