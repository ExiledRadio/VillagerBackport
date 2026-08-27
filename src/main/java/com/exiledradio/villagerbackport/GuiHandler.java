package com.exiledradio.villagerbackport;

import com.exiledradio.villagerbackport.block.ContainerFurnaceVariant;
import com.exiledradio.villagerbackport.block.TileEntityFurnaceVariant;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;

import javax.annotation.Nullable;

/**
 * Opens this mod's screens.
 *
 * <p>The barrel deliberately does not come through here: it reports itself as a chest-shaped
 * container, so vanilla's own window handling opens it with no handler involved. The furnaces do,
 * because they reuse vanilla's container but need their own background - and there is no vanilla
 * route for that pairing.
 */
public class GuiHandler implements IGuiHandler {

    public static final int FURNACE = 0;
    public static final int GRINDSTONE = 1;
    public static final int CARTOGRAPHY = 2;
    public static final int STONECUTTER = 3;
    public static final int LECTERN = 4;
    public static final int LOOM = 5;
    public static final int FLETCHING = 6;

    @Override
    @Nullable
    public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        TileEntity tile = world.getTileEntity(new BlockPos(x, y, z));

        if (id == FURNACE && tile instanceof TileEntityFurnaceVariant) {
            return new ContainerFurnaceVariant(player.inventory, (TileEntityFurnaceVariant) tile);
        }
        if (id == GRINDSTONE) {
            return new com.exiledradio.villagerbackport.block.ContainerGrindstone(
                    player.inventory, world, new BlockPos(x, y, z));
        }
        if (id == CARTOGRAPHY) {
            return new com.exiledradio.villagerbackport.block.ContainerCartographyTable(
                    player.inventory, world, new BlockPos(x, y, z));
        }
        if (id == STONECUTTER) {
            return cutter(player, world, new BlockPos(x, y, z),
                    com.exiledradio.villagerbackport.block.CutterKind.STONE, "stonecutter");
        }
        if (id == FLETCHING) {
            return cutter(player, world, new BlockPos(x, y, z),
                    com.exiledradio.villagerbackport.block.CutterKind.WOOD, "fletching_table");
        }
        if (id == LECTERN && tile instanceof com.exiledradio.villagerbackport.block.TileEntityLectern) {
            return new com.exiledradio.villagerbackport.block.ContainerLectern(
                    (com.exiledradio.villagerbackport.block.TileEntityLectern) tile);
        }
        if (id == LOOM) {
            return new com.exiledradio.villagerbackport.block.ContainerLoom(
                    player.inventory, world, new BlockPos(x, y, z));
        }
        return null;
    }

    @Override
    @Nullable
    public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        TileEntity tile = world.getTileEntity(new BlockPos(x, y, z));

        if (id == FURNACE && tile instanceof TileEntityFurnaceVariant) {
            return furnaceScreen((TileEntityFurnaceVariant) tile, player);
        }
        if (id == GRINDSTONE) {
            return grindstoneScreen(player, world, new BlockPos(x, y, z));
        }
        if (id == CARTOGRAPHY) {
            return cartographyScreen(player, world, new BlockPos(x, y, z));
        }
        if (id == STONECUTTER) {
            return cutterScreen(player, world, new BlockPos(x, y, z),
                    com.exiledradio.villagerbackport.block.CutterKind.STONE,
                    "stonecutter", "Stonecutter");
        }
        if (id == FLETCHING) {
            return cutterScreen(player, world, new BlockPos(x, y, z),
                    com.exiledradio.villagerbackport.block.CutterKind.WOOD,
                    "fletching_table", "Fletching Table");
        }
        if (id == LECTERN && tile instanceof com.exiledradio.villagerbackport.block.TileEntityLectern) {
            return lecternScreen((com.exiledradio.villagerbackport.block.TileEntityLectern) tile);
        }
        if (id == LOOM) {
            return loomScreen(player, world, new BlockPos(x, y, z));
        }
        return null;
    }

    /**
     * Split out so the client-only screen class is named in a method the server never executes -
     * the same reason the packet handlers do it.
     */
    private Object furnaceScreen(TileEntityFurnaceVariant furnace, EntityPlayer player) {
        return new com.exiledradio.villagerbackport.client.GuiFurnaceVariant(player.inventory, furnace);
    }

    private Object grindstoneScreen(EntityPlayer player, World world, BlockPos pos) {
        return new com.exiledradio.villagerbackport.client.GuiGrindstone(player.inventory,
                new com.exiledradio.villagerbackport.block.ContainerGrindstone(
                        player.inventory, world, pos));
    }

    private Object lecternScreen(com.exiledradio.villagerbackport.block.TileEntityLectern lectern) {
        return new com.exiledradio.villagerbackport.client.GuiLectern(
                new com.exiledradio.villagerbackport.block.ContainerLectern(lectern));
    }

    /**
     * Both saws, which are the same machine told to work a different material - see
     * {@code CutterKind}. The block is passed in so the container knows which one to check the
     * player is still standing at.
     */
    private com.exiledradio.villagerbackport.block.ContainerStonecutter cutter(
            EntityPlayer player, World world, BlockPos pos,
            com.exiledradio.villagerbackport.block.CutterKind kind, String name) {
        return new com.exiledradio.villagerbackport.block.ContainerStonecutter(
                player.inventory, world, pos, kind,
                net.minecraftforge.fml.common.registry.ForgeRegistries.BLOCKS.getValue(
                        new net.minecraft.util.ResourceLocation(
                                com.exiledradio.villagerbackport.VillagerBackport.MOD_ID, name)));
    }

    private Object cutterScreen(EntityPlayer player, World world, BlockPos pos,
                                com.exiledradio.villagerbackport.block.CutterKind kind,
                                String name, String fallbackTitle) {
        return new com.exiledradio.villagerbackport.client.GuiStonecutter(player.inventory,
                cutter(player, world, pos, kind, name),
                "container.villagerbackport." + name, fallbackTitle);
    }

    private Object loomScreen(EntityPlayer player, World world, BlockPos pos) {
        return new com.exiledradio.villagerbackport.client.GuiLoom(player.inventory,
                new com.exiledradio.villagerbackport.block.ContainerLoom(
                        player.inventory, world, pos));
    }

    private Object cartographyScreen(EntityPlayer player, World world, BlockPos pos) {
        return new com.exiledradio.villagerbackport.client.GuiCartographyTable(player.inventory,
                new com.exiledradio.villagerbackport.block.ContainerCartographyTable(
                        player.inventory, world, pos));
    }
}
