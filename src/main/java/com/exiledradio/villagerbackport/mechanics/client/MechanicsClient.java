package com.exiledradio.villagerbackport.mechanics.client;

import com.exiledradio.villagerbackport.ModConfig;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiScreenBook;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** The client side of {@code mechanics}: 1.14's horse armor drawing and book editor. */
@SideOnly(Side.CLIENT)
public final class MechanicsClient {

    private MechanicsClient() {
    }

    /** Client pre-init. */
    public static void init() {
        HorseArmorClient.init();
        MinecraftForge.EVENT_BUS.register(new MechanicsClient.Events());
    }

    public static final class Events {

        /**
         * A book and quill opens in 1.14's editor. Only vanilla's own screen for an unsigned book is replaced (a mod's
         * own book screen is left alone); lowest priority, so a mod that swaps the screen first keeps its own.
         */
        @SubscribeEvent(priority = EventPriority.LOWEST)
        public void onGuiOpen(GuiOpenEvent event) {
            GuiScreen gui = event.getGui();
            if (!ModConfig.mechanics.modernBookEditor || gui == null || gui.getClass() != GuiScreenBook.class) {
                return;
            }
            try {
                boolean unsigned = ObfuscationReflectionHelper.getPrivateValue(GuiScreenBook.class, (GuiScreenBook) gui, "field_146475_i");
                ItemStack book = ObfuscationReflectionHelper.getPrivateValue(GuiScreenBook.class, (GuiScreenBook) gui, "field_146474_h");
                EntityPlayer player = ObfuscationReflectionHelper.getPrivateValue(GuiScreenBook.class, (GuiScreenBook) gui, "field_146468_g");
                if (unsigned && book.getItem() == Items.WRITABLE_BOOK) {
                    event.setGui(new GuiBookEditor(player, book));
                }
            } catch (RuntimeException e) {
                // Keep vanilla's screen.
            }
        }
    }
}
