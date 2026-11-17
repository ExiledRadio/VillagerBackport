package com.exiledradio.villagerbackport.selftest;

import com.exiledradio.villagerbackport.mechanics.client.GuiBookEditor;
import com.exiledradio.villagerbackport.mechanics.client.HorseArmorClient;

import net.minecraft.client.Minecraft;
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
 * (typing, the cursor, word and page clicks, page limits) driven by hand, and horse armor taken out of the baked
 * horse texture for the layer.
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

        ok.accept(!com.exiledradio.villagerbackport.mechanics.Mechanics.crouch(),
                "crouch: not connected to a server that runs this mod, 1.12.2's crouch on the client");
        ok.accept(HorseArmorClient.active(), "horse armor: drawn as a layer");
        String baked = ObfuscationReflectionHelper.getPrivateValue(HorseArmorType.class, HorseArmorType.IRON, "field_188586_e");
        ok.accept(baked == null, "horse armor: iron no longer baked into the horse texture");
        ok.accept(HorseArmorClient.texture(new ItemStack(Items.DIAMOND_HORSE_ARMOR)) != null
                && HorseArmorClient.texture(new ItemStack(Items.DIAMOND_HORSE_ARMOR)).getPath().endsWith("horse_armor_diamond.png"),
                "horse armor: the layer has diamond's texture");
    }
}
