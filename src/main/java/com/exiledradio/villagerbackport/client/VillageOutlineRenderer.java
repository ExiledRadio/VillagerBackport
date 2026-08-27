package com.exiledradio.villagerbackport.client;

import com.exiledradio.villagerbackport.network.PacketVillageOutline;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Draws the beds of a village, and a box around the lot.
 *
 * <p>Green for a bed somebody has claimed, red for one going spare, and a white box containing them
 * all. The colours are the useful part: a village full of red is a village of villagers who cannot
 * get to their beds, and that looks identical to a working village until somebody draws it.
 *
 * <p>Shown for a fixed time and then forgotten. Nothing about it refreshes - it is a photograph
 * rather than a display - so the command is worth running again after changing anything.
 */
@SideOnly(Side.CLIENT)
public final class VillageOutlineRenderer {

    private static final List<BlockPos> BEDS = new ArrayList<BlockPos>();
    private static final Set<BlockPos> CLAIMED = new HashSet<BlockPos>();

    private static long hideAt;

    /** Called on the client thread when an outline arrives. */
    public static void show(PacketVillageOutline message) {
        BEDS.clear();
        CLAIMED.clear();

        BEDS.addAll(message.getBeds());
        CLAIMED.addAll(message.getClaimed());

        hideAt = System.currentTimeMillis() + message.getSeconds() * 1000L;
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        if (BEDS.isEmpty() || System.currentTimeMillis() > hideAt) {
            return;
        }

        Entity viewer = Minecraft.getMinecraft().getRenderViewEntity();
        if (viewer == null) {
            return;
        }

        // Drawn in world coordinates, so the camera's own position comes off first - the projection
        // is already centred on the viewer.
        float partial = event.getPartialTicks();
        double x = viewer.lastTickPosX + (viewer.posX - viewer.lastTickPosX) * partial;
        double y = viewer.lastTickPosY + (viewer.posY - viewer.lastTickPosY) * partial;
        double z = viewer.lastTickPosZ + (viewer.posZ - viewer.lastTickPosZ) * partial;

        GlStateManager.pushMatrix();
        GlStateManager.translate(-x, -y, -z);

        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.glLineWidth(2.0F);

        // Through walls deliberately. Half the point is seeing beds inside houses you are stood
        // outside of.
        GlStateManager.disableDepth();

        for (BlockPos bed : BEDS) {
            boolean taken = CLAIMED.contains(bed);

            RenderGlobal.drawSelectionBoundingBox(
                    new AxisAlignedBB(bed).grow(0.002D),
                    taken ? 0.2F : 1.0F,
                    taken ? 1.0F : 0.2F,
                    0.2F,
                    0.9F);
        }

        AxisAlignedBB box = boxOf();
        if (box != null) {
            RenderGlobal.drawSelectionBoundingBox(box, 1.0F, 1.0F, 1.0F, 0.6F);
        }

        GlStateManager.enableDepth();
        GlStateManager.disableBlend();
        GlStateManager.enableLighting();
        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();
    }

    /** @return a box containing every bed, worked out here so nothing has to be sent for it. */
    @Nullable
    private static AxisAlignedBB boxOf() {
        if (BEDS.isEmpty()) {
            return null;
        }

        BlockPos first = BEDS.get(0);
        int minX = first.getX();
        int minY = first.getY();
        int minZ = first.getZ();
        int maxX = minX;
        int maxY = minY;
        int maxZ = minZ;

        for (BlockPos bed : BEDS) {
            minX = Math.min(minX, bed.getX());
            minY = Math.min(minY, bed.getY());
            minZ = Math.min(minZ, bed.getZ());
            maxX = Math.max(maxX, bed.getX());
            maxY = Math.max(maxY, bed.getY());
            maxZ = Math.max(maxZ, bed.getZ());
        }

        return new AxisAlignedBB(minX, minY, minZ, maxX + 1, maxY + 1, maxZ + 1);
    }
}
