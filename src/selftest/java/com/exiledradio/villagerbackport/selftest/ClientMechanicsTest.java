package com.exiledradio.villagerbackport.selftest;

import com.exiledradio.backportcore.presentation.LookArea;
import com.exiledradio.backportcore.presentation.Presentation;
import com.exiledradio.villagerbackport.mechanics.client.GuiBookEditor;
import com.exiledradio.villagerbackport.mechanics.client.GuiBookSign;
import com.exiledradio.villagerbackport.mechanics.client.HorseArmorClient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.entity.passive.HorseArmorType;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

import java.util.function.BiConsumer;

/**
 * The client half of {@code mechanics}, run from {@link ClientLookFeelTest} at the title screen: 1.14's book editor
 * (typing, the cursor, word and page clicks, page limits, signing on its page and on 1.21.6's signing screen) driven
 * by hand, and horse armor taken out of the baked horse texture for the layer.
 */
@SideOnly(Side.CLIENT)
final class ClientMechanicsTest {

    private ClientMechanicsTest() {
    }

    private static void type(GuiBookEditor gui, String s) throws Exception {
        for (char ch : s.toCharArray()) {
            gui.press(ch, 0);
        }
    }

    private static void type(GuiBookSign gui, String s) throws Exception {
        for (char ch : s.toCharArray()) {
            gui.press(ch, 0);
        }
    }

    private static String tag(ItemStack book, String key) {
        return book.getTagCompound() == null ? null : book.getTagCompound().hasKey(key) ? book.getTagCompound().getString(key) : null;
    }

    /** Signing: on the editor's page below 1.21.6 (1.14), on {@link GuiBookSign} from 1.21.6. Leaves the title screen open. */
    private static void signing(Minecraft mc, BiConsumer<Boolean, String> ok) throws Exception {
        String me = mc.getSession().getUsername();
        boolean newer = Presentation.isAtLeast(LookArea.MENUS, "1.21.6");
        ok.accept(new GuiBookEditor(null, new ItemStack(Items.WRITABLE_BOOK)).usesSignScreen() == newer,
                "book: signs on its own screen exactly from 1.21.6 (now " + Presentation.newest(LookArea.MENUS) + ")");

        // 1.14: Sign turns the editor's page into the title page; Enter signs.
        ItemStack old = new ItemStack(Items.WRITABLE_BOOK);
        GuiBookEditor editor = new GuiBookEditor(null, old, false);
        mc.displayGuiScreen(editor);
        type(editor, "page one");
        editor.pressSign();
        ok.accept(mc.currentScreen == editor, "book 1.14: Sign stays in the editor (" + mc.currentScreen + ")");
        type(editor, "Old Title Here!!!");
        editor.press('\0', Keyboard.KEY_RETURN);
        ok.accept("Old Title Here!!".equals(tag(old, "title")) && me.equals(tag(old, "author")),
                "book 1.14: Enter signs, 16 characters (" + tag(old, "title") + " by " + tag(old, "author") + ")");
        ok.accept(mc.currentScreen instanceof GuiMainMenu, "book 1.14: signing closes the editor");

        // 1.21.6: Sign opens the signing screen.
        ItemStack fresh = new ItemStack(Items.WRITABLE_BOOK);
        GuiBookEditor modern = new GuiBookEditor(null, fresh, true);
        mc.displayGuiScreen(modern);
        type(modern, "page");
        modern.pressSign();
        ok.accept(mc.currentScreen instanceof GuiBookSign, "book 1.21.6: Sign opens the signing screen (" + mc.currentScreen + ")");
        if (!(mc.currentScreen instanceof GuiBookSign)) {
            return;
        }
        GuiBookSign sign = (GuiBookSign) mc.currentScreen;
        ok.accept(!sign.canFinalize(), "book 1.21.6: Sign and Close is off with no title");
        type(sign, "  ");
        ok.accept(!sign.canFinalize(), "book 1.21.6: and with a blank one");
        sign.press('\b', Keyboard.KEY_BACK);
        sign.press('\b', Keyboard.KEY_BACK);
        type(sign, "A Long Title That Goes On");
        ok.accept("A Long Title Th".equals(sign.titleText()), "book 1.21.6: the title stops at 15 characters (" + sign.titleText() + ")");
        ok.accept(sign.canFinalize(), "book 1.21.6: Sign and Close is on with a title");
        sign.pressCancel();
        ok.accept(mc.currentScreen == modern && "page".equals(modern.pageText()), "book 1.21.6: Cancel goes back to the editor, page kept");
        modern.pressSign();
        ok.accept(mc.currentScreen == sign && "A Long Title Th".equals(sign.titleText()), "book 1.21.6: Sign again keeps the title ("
                + (mc.currentScreen == sign ? sign.titleText() : String.valueOf(mc.currentScreen)) + ")");
        sign.press('\0', Keyboard.KEY_ESCAPE);
        ok.accept(mc.currentScreen instanceof GuiMainMenu && tag(fresh, "title") == null, "book 1.21.6: Escape closes without signing");
        mc.displayGuiScreen(modern);
        modern.pressSign();
        sign.press('\0', Keyboard.KEY_RETURN);
        ok.accept("A Long Title Th".equals(tag(fresh, "title")) && me.equals(tag(fresh, "author")) && mc.currentScreen instanceof GuiMainMenu,
                "book 1.21.6: Enter signs and closes (" + tag(fresh, "title") + " by " + tag(fresh, "author") + ")");
        ItemStack button = new ItemStack(Items.WRITABLE_BOOK);
        GuiBookEditor third = new GuiBookEditor(null, button, true);
        mc.displayGuiScreen(third);
        third.pressSign();
        type((GuiBookSign) mc.currentScreen, "Pressed");
        ((GuiBookSign) mc.currentScreen).pressFinalize();
        ok.accept("Pressed".equals(tag(button, "title")) && mc.currentScreen instanceof GuiMainMenu, "book 1.21.6: Sign and Close signs");
    }

    static void run(Minecraft mc, BiConsumer<Boolean, String> ok) throws Exception {
        GuiBookEditor gui = new GuiBookEditor(null, new ItemStack(Items.WRITABLE_BOOK));
        gui.setWorldAndResolution(mc, 427, 240);
        type(gui, "hello world");
        ok.accept("hello world".equals(gui.pageText()), "book: typing (" + gui.pageText() + ")");
        for (int i = 0; i < 5; i++) {
            gui.press('\0', Keyboard.KEY_LEFT);
        }
        type(gui, "big ");
        ok.accept("hello big world".equals(gui.pageText()), "book: the cursor moves and text goes in at it (" + gui.pageText() + ")");
        gui.press('\0', Keyboard.KEY_HOME);
        gui.press('\0', Keyboard.KEY_DELETE);
        ok.accept("ello big world".equals(gui.pageText()), "book: Home, then Delete (" + gui.pageText() + ")");
        gui.press('\0', Keyboard.KEY_END);
        gui.press('\0', Keyboard.KEY_BACK);
        ok.accept("ello big worl".equals(gui.pageText()), "book: End, then Backspace (" + gui.pageText() + ")");
        // Two quick clicks on the first word select it; a third selects the page.
        gui.clickText(0, 2);
        gui.clickText(0, 2);
        ok.accept(gui.selectionFrom() == 0 && gui.selectionTo() == 4, "book: double click selects the word (" + gui.selectionFrom() + ".." + gui.selectionTo() + ")");
        gui.clickText(0, 2);
        ok.accept(gui.selectionFrom() == 0 && gui.selectionTo() == gui.pageText().length(), "book: third click selects the page");
        type(gui, "x");
        ok.accept("x".equals(gui.pageText()), "book: typing replaces the selection (" + gui.pageText() + ")");
        gui.press('\0', Keyboard.KEY_RETURN);
        type(gui, "y");
        ok.accept("x\ny".equals(gui.pageText()), "book: Enter is a new line");
        StringBuilder lots = new StringBuilder();
        for (int i = 0; i < 400; i++) {
            lots.append(i % 7 == 6 ? ' ' : 'w');
        }
        type(gui, lots.toString());
        int len = gui.pageText().length();
        ok.accept(len < 256 && len > 50, "book: a page fills up and stops (" + len + " characters)");
        for (int i = 0; i < 130; i++) {
            gui.press('\0', Keyboard.KEY_NEXT);
        }
        ok.accept(gui.pageCount() == 100, "book: 100 pages at most (" + gui.pageCount() + ")");
        signing(mc, ok);

        ok.accept(!com.exiledradio.villagerbackport.mechanics.Mechanics.crouch(),
                "crouch: not connected to a server that runs this mod, 1.12.2's crouch on the client");
        ok.accept(HorseArmorClient.active(), "horse armor: drawn as a layer");
        String baked = ObfuscationReflectionHelper.getPrivateValue(HorseArmorType.class, HorseArmorType.IRON, "field_188586_e");
        ok.accept(baked == null, "horse armor: iron no longer baked into the horse texture");
        ok.accept(HorseArmorClient.texture(new ItemStack(Items.DIAMOND_HORSE_ARMOR)) != null
                && HorseArmorClient.texture(new ItemStack(Items.DIAMOND_HORSE_ARMOR)).getPath().endsWith("horse_armor_diamond.png"),
                "horse armor: the layer has diamond's texture");

        // Last, since it stays installed: a 1.21.6 backport makes a book editor sign on its own screen.
        if (!Presentation.isAtLeast(LookArea.MENUS, "1.21.6")) {
            Presentation.version("1.21.6");
            ok.accept(new GuiBookEditor(null, new ItemStack(Items.WRITABLE_BOOK)).usesSignScreen(),
                    "book: with 1.21.6 installed the editor signs on its own screen (now " + Presentation.newest(LookArea.MENUS) + ")");
        }
    }
}
