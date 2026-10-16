package com.exiledradio.villagerbackport.client;

import com.exiledradio.villagerbackport.block.MapLocking;

import net.minecraft.init.Items;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** 1.14's grey "Locked" line under a locked map's name. */
@SideOnly(Side.CLIENT)
public final class MapLockTooltip {

    @SubscribeEvent
    public void onTooltip(ItemTooltipEvent event) {
        if (event.getItemStack().getItem() == Items.FILLED_MAP && MapLocking.isLocked(event.getItemStack(), null)) {
            event.getToolTip().add(1, TextFormatting.GRAY + Translate.of("filled_map.villagerbackport.locked", "Locked"));
        }
    }
}
