package com.exiledradio.villagerbackport.block;

import com.exiledradio.villagerbackport.ModConfig;

import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.List;

/**
 * A village bell: ring it and every villager in earshot lights up.
 *
 * <h2>What it does here, and why it differs</h2>
 * In 1.14 a bell makes nearby <em>raiders</em> glow, so a village under attack can see where the
 * attackers are. 1.12.2 has no raids and no illagers to highlight, which would leave the block with
 * nothing to do at all.
 *
 * <p>So it highlights villagers instead. The mechanic survives intact - ring the bell, see who is
 * where through the walls - and it is genuinely useful in a way the original is not without raids:
 * finding the villager you were looking for in a village you have built up.
 *
 * <h2>1.14's timing, kept</h2>
 * The ring is immediate; the resonance and the glow follow a moment later. That gap is what makes a
 * bell feel like a bell rather than a button, so it is preserved with a scheduled block update -
 * which also means the block needs no tile entity of its own.
 */
public class BlockBell extends BlockWorkstationShaped {

    public BlockBell(Material material, SoundType sound, float hardness,
                     AxisAlignedBB shape, boolean cutout) {
        super(material, sound, hardness, shape, cutout);
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Override
    public net.minecraft.tileentity.TileEntity createTileEntity(World world, IBlockState state) {
        return new TileEntityBell();
    }

    /**
     * Passes block events on to the tile entity, which is what makes the swing reach clients.
     *
     * <p>Vanilla routes them this way for chest lids and note blocks too: the server posts an event,
     * every client watching the block receives it, and each starts the animation locally.
     */
    @Override
    public boolean eventReceived(IBlockState state, World world, BlockPos pos, int id, int param) {
        super.eventReceived(state, world, pos, id, param);

        net.minecraft.tileentity.TileEntity tile = world.getTileEntity(pos);
        return tile != null && tile.receiveClientEvent(id, param);
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player,
                                    EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (!world.isRemote) {
            ring(world, pos, facing);
        }
        return true;
    }

    /**
     * @param struck the side the bell was hit on, which decides which way it swings
     */
    private void ring(World world, BlockPos pos, EnumFacing struck) {
        world.playSound(null, pos, ModSounds.bellUse, SoundCategory.BLOCKS, 2.0F, 1.0F);

        // A vertical hit has no axis to swing along, so treat it as a strike from the north.
        EnumFacing swingAxis = struck.getAxis() == EnumFacing.Axis.Y ? EnumFacing.NORTH : struck;
        world.addBlockEvent(pos, this, TileEntityBell.EVENT_RING, swingAxis.getIndex());

        highlightVillagers(world, pos);
    }

    /**
     * The resonance, and the highlight that comes with it.
     *
     * <p>Both land on the ring rather than a beat later. 1.14 delays this because it is waiting to
     * see whether a raid is under way before deciding to highlight anything - there is no such
     * question here, so the pause was two seconds of nothing and read as lag rather than as a bell
     * ringing out.
     */
    private void highlightVillagers(World world, BlockPos pos) {
        if (!ModConfig.workstations.bellHighlightsVillagers) {
            return;
        }

        world.playSound(null, pos, ModSounds.bellResonate, SoundCategory.BLOCKS, 1.0F, 1.0F);

        double radius = ModConfig.workstations.bellRadius;
        int duration = ModConfig.workstations.bellGlowTicks;

        // Gathered from a box, then filtered by true distance - a box alone would reach noticeably
        // further at the corners than along the axes.
        AxisAlignedBB box = new AxisAlignedBB(pos).grow(radius);
        List<EntityVillager> villagers = world.getEntitiesWithinAABB(EntityVillager.class, box);

        double radiusSq = radius * radius;
        for (EntityVillager villager : villagers) {
            if (villager.isEntityAlive() && villager.getDistanceSq(pos) <= radiusSq) {
                villager.addPotionEffect(new PotionEffect(MobEffects.GLOWING, duration, 0, false, false));
            }
        }
    }
}
