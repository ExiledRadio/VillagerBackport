package com.exiledradio.villagerbackport.lookfeel.client;

import com.exiledradio.backportcore.presentation.Contribution;
import com.exiledradio.backportcore.presentation.Presentation;
import com.exiledradio.backportcore.presentation.client.PresentationHooks;
import com.exiledradio.villagerbackport.lookfeel.ScrollAccumulator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

/** 1.14's look and feel on the client: F3+N and F3+Esc (1.14.1), and the hotbar scroll's Mouse Settings. */
@SideOnly(Side.CLIENT)
public final class LookAndFeelClient {

    /** LWJGL 2 reports one wheel notch as 120; GLFW, which 1.14's {@code MouseHandler} reads, as 1.0. */
    static final double NOTCH = 120.0;

    /** For the client self-test. */
    public static volatile int pauseOnlyScreens;

    private final ScrollAccumulator scroll = new ScrollAccumulator();

    private LookAndFeelClient() {
    }

    /** For the client self-test. */
    public static volatile int debugEdits;

    /**
     * 1.14.2's F3 text: in singleplayer the server's chunk cache follows the client's ({@code getServerChunkStats},
     * the integrated server world's own {@code ServerChunkCache: n Drop: m}).
     */
    static void debugText(com.exiledradio.backportcore.presentation.client.DebugText.Lines lines) {
        Minecraft mc = lines.mc;
        net.minecraft.server.integrated.IntegratedServer server = mc.getIntegratedServer();
        if (server == null || mc.player == null || mc.world == null) {
            return;
        }
        net.minecraft.world.WorldServer world = server.getWorld(mc.player.dimension);
        String client = mc.world.getProviderName();
        int at = lines.left.indexOf(client);
        if (world != null && at >= 0) {
            lines.left.add(at + 1, world.getChunkProvider().makeString());
            debugEdits++;
        }
    }

    public static void init() {
        Contribution v1141 = Presentation.contribution("1.14.1");
        if (v1141 != null) {
            v1141.debugKey(Keyboard.KEY_N, "debug.creative_spectator.help", LookAndFeelClient::creativeSpectator);
        }
        MinecraftForge.EVENT_BUS.register(new LookAndFeelClient());
        com.exiledradio.backportcore.presentation.client.DebugText.add("1.14.2", "server chunk cache", LookAndFeelClient::debugText);
    }

    /** 1.14.4 {@code KeyboardHandler}, F3+N: creative goes to spectator, anything else to creative. */
    static boolean creativeSpectator() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null) {
            return false;
        }
        if (!mc.player.canUseCommand(2, "")) {
            PresentationHooks.debugFeedback("debug.creative_spectator.error");
        } else if (mc.player.isCreative()) {
            mc.player.sendChatMessage("/gamemode spectator");
        } else {
            mc.player.sendChatMessage("/gamemode creative");
        }
        return true;
    }

    /**
     * 1.14.1, F3+Esc: the game pauses without the pause menu (1.14.4's {@code PauseScreen(false)}: no buttons, no
     * dark background, "Game paused" at the top). 1.12.2 opens its menu on Esc before it looks at F3, so the menu
     * is swapped as it opens.
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    public void onGuiOpen(GuiOpenEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (event.getGui() != null && event.getGui().getClass() == GuiIngameMenu.class && mc.currentScreen == null
                && Keyboard.isKeyDown(Keyboard.KEY_F3) && Keyboard.isKeyDown(Keyboard.KEY_ESCAPE)) {
            event.setGui(new GuiPauseOnly());
            pauseOnlyScreens++;
        }
    }

    /**
     * 1.14.4 {@code MouseHandler.onScroll} for the hotbar: the wheel's movement times Scroll Sensitivity (or its
     * sign, with Discrete Scrolling) is added up, and one slot moves each time the sum passes a whole notch; a
     * change of direction starts the sum again. Screens keep 1.12.2's scrolling.
     */
    @SubscribeEvent
    public void onMouse(MouseEvent event) {
        int wheel = event.getDwheel();
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP player = mc.player;
        if (wheel == 0 || event.getButton() != -1 || mc.currentScreen != null || player == null) {
            return;
        }
        event.setCanceled(true);
        int slots = this.scroll.add(ScrollAccumulator.amount(wheel / NOTCH));
        if (slots == 0) {
            return;
        }
        if (player.isSpectator()) {
            if (mc.ingameGUI.getSpectatorGui().isMenuActive()) {
                mc.ingameGUI.getSpectatorGui().onMouseScroll(-slots);
            } else {
                player.capabilities.setFlySpeed(MathHelper.clamp(player.capabilities.getFlySpeed() + slots * 0.005F, 0.0F, 0.2F));
            }
        } else {
            player.inventory.changeCurrentItem(slots);
        }
    }
}
