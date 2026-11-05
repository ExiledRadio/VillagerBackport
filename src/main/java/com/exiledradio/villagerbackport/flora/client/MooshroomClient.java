package com.exiledradio.villagerbackport.flora.client;

import com.exiledradio.villagerbackport.flora.PacketMooshroomType;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Where {@link PacketMooshroomType} lands on the client. A class of its own so the packet's handler,
 * which a server also loads, names no client type in any signature.
 */
@SideOnly(Side.CLIENT)
public final class MooshroomClient {

    private MooshroomClient() {
    }

    public static void accept(PacketMooshroomType message) {
        FloraClient.acceptMooshroom(message.getEntityId(), message.isBrown());
    }
}
