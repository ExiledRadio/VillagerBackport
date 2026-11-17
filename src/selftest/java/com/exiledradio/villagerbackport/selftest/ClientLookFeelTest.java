package com.exiledradio.villagerbackport.selftest;

import com.exiledradio.backportcore.presentation.Contribution;
import com.exiledradio.backportcore.presentation.DebugKeys;
import com.exiledradio.backportcore.presentation.GameVersion;
import com.exiledradio.backportcore.presentation.LoadingRevision;
import com.exiledradio.backportcore.presentation.Presentation;
import com.exiledradio.backportcore.presentation.client.PanoramaRenderer;
import com.exiledradio.backportcore.presentation.client.TitleSplashes;
import com.exiledradio.villagerbackport.VillagerBackport;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

import java.util.List;
import java.util.Map;

/**
 * 1.14's look and feel on the client, for {@code gradlew runClient -Pselftest=client}: at the title screen it checks
 * what Backport Core shows (newest version, panorama, overlay, splash pool, loading revision, F3 keys) and the lang
 * overrides, all in code; nothing reaches the window. Logs {@code LOOKFEEL ...} lines and
 * {@code CLIENT SELFTEST DONE pass=N fail=M}, then closes the game.
 */
@SideOnly(Side.CLIENT)
public final class ClientLookFeelTest {

    private int ticks;
    private int pass;
    private int fail;
    private boolean done;

    public static void register() {
        MinecraftForge.EVENT_BUS.register(new ClientLookFeelTest());
        VillagerBackport.LOGGER.info("CLIENT SELFTEST registered");
    }

    private void ok(boolean condition, String what) {
        if (condition) {
            this.pass++;
        } else {
            this.fail++;
            VillagerBackport.LOGGER.error("CLIENT SELFTEST FAIL {}", what);
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (event.phase != TickEvent.Phase.END || this.done || !(mc.currentScreen instanceof GuiMainMenu) || ++this.ticks < 400) {
            return;
        }
        this.done = true;
        try {
            run(mc);
        } catch (Throwable t) {
            VillagerBackport.LOGGER.error("CLIENT SELFTEST threw", t);
            this.fail++;
        }
        try {
            ClientMechanicsTest.run(mc, this::ok);
        } catch (Throwable t) {
            VillagerBackport.LOGGER.error("CLIENT SELFTEST mechanics threw", t);
            this.fail++;
        }
        VillagerBackport.LOGGER.info("CLIENT SELFTEST DONE pass={} fail={}", this.pass, this.fail);
        mc.shutdown();
    }

    private void run(Minecraft mc) {
        GameVersion newest = Presentation.newest();
        LoadingRevision loading = Presentation.loadingRevision();
        ResourceLocation[] faces = PanoramaRenderer.faces();
        List<String> pool = TitleSplashes.lastPool;
        StringBuilder installed = new StringBuilder();
        for (Contribution c : Presentation.installed()) {
            installed.append(c.version()).append(c.owners()).append(' ');
        }
        VillagerBackport.LOGGER.info("LOOKFEEL installed: {}", installed.toString().trim());
        VillagerBackport.LOGGER.info("LOOKFEEL newest={} panorama={} overlay={} loading={} splashes={} panoramaFrames={}",
                newest, faces == null ? null : faces[0], Presentation.panoramaOverlay(), loading,
                pool == null ? -1 : pool.size(), PanoramaRenderer.frames);
        boolean newestIsMine = newest.equals(GameVersion.parse("1.14.2"));

        ok(PanoramaRenderer.modern(), "the title screen is drawn the 1.13+ way");
        ok(PanoramaRenderer.frames > 20, "the panorama was drawn: " + PanoramaRenderer.frames + " frames");
        if (newestIsMine) {
            ok(faces != null && faces[0].getNamespace().equals(VillagerBackport.MOD_ID), "1.14's village panorama is shown: "
                    + (faces == null ? null : faces[0]));
            ok(Presentation.panoramaOverlay() != null && Presentation.panoramaOverlay().getNamespace().equals(VillagerBackport.MOD_ID),
                    "1.14's overlay is used: " + Presentation.panoramaOverlay());
            ok(loading == LoadingRevision.GRID_1_14, "the world-loading screen is 1.14's grid: " + loading);
        }
        ok(mc.loadingScreen != null && mc.loadingScreen.getClass().getName().endsWith("WorldLoadingScreen"),
                "Core's loading screen is installed: " + (mc.loadingScreen == null ? null : mc.loadingScreen.getClass().getName()));

        ok(pool != null, "the title screen drew from Core's splash pool");
        if (pool != null) {
            for (String s : new String[] {"Rainbow turtle?", "Ping the human!", "{PLAYER} IS YOU"}) {
                ok(pool.contains(s), "1.14's splash \"" + s + "\" is in the pool");
            }
            for (String s : new String[] {"Made by Notch!", "Woo, minecraftforum!", "Hobo humping slobo babe!"}) {
                ok(!pool.contains(s), "1.14 removed \"" + s + "\"");
            }
        }

        Map<Integer, DebugKeys.Key> keys = DebugKeys.map(Presentation.installed());
        DebugKeys.Key n = keys.get(Keyboard.KEY_N);
        ok(n != null && n.action != null, "F3+N is changed (1.14.1's, or a later version's): " + n);
        VillagerBackport.LOGGER.info("LOOKFEEL F3 keys: {}", keys.values());

        String[][] lang = {
                {"options.mouseWheelSensitivity", "Scroll Sensitivity"},
                {"options.discrete_mouse_scroll", "Discrete Scrolling"},
                {"options.chat.opacity", "Chat Text Opacity"},
                {"menu.paused", "Game paused"},
        };
        for (String[] kv : lang) {
            ok(kv[1].equals(I18n.format(kv[0])), "I18n " + kv[0] + " = \"" + kv[1] + "\", got \"" + I18n.format(kv[0]) + "\"");
        }
        ok(com.exiledradio.backportcore.presentation.client.DebugText.steps().names(newest).contains("1.14.2 server chunk cache"),
                "1.14.2's F3 line is registered: " + com.exiledradio.backportcore.presentation.client.DebugText.steps().names(newest));
        ok(Presentation.titleAccessibilityIcon() != null, "a title Accessibility icon is registered");
        boolean icon = false;
        try {
            @SuppressWarnings("unchecked")
            java.util.List<net.minecraft.client.gui.GuiButton> buttons = (java.util.List<net.minecraft.client.gui.GuiButton>)
                    net.minecraftforge.fml.relauncher.ReflectionHelper.findField(net.minecraft.client.gui.GuiScreen.class, "buttonList", "field_146292_n").get(mc.currentScreen);
            for (net.minecraft.client.gui.GuiButton b : buttons) {
                icon |= b instanceof com.exiledradio.backportcore.presentation.client.TitleScreenExtras.IconButton;
            }
        } catch (Exception e) {
            icon = false;
        }
        ok(icon, "the title screen has 1.14's Accessibility button");
        if (newestIsMine) {
            ok(com.exiledradio.backportcore.presentation.client.StartupScreen.lastDrawn == com.exiledradio.backportcore.presentation.VanillaRevisions.Startup.BAR_1_14,
                    "the startup screen drew 1.14's revision: " + com.exiledradio.backportcore.presentation.client.StartupScreen.lastDrawn);
        }
        try {
            String pack = mc.getResourceManager().getResource(new net.minecraft.util.ResourceLocation("textures/gui/title/mojang.png")).getResourcePackName();
            ok("backportcore:vanilla_by_version".equals(pack) || !newestIsMine, "mojang.png comes from Core's pack: " + pack);
        } catch (Exception e) {
            ok(false, "mojang.png loads: " + e);
        }
    }
}
