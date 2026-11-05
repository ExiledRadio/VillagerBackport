package com.exiledradio.villagerbackport.block;

import com.exiledradio.villagerbackport.ModConfig;
import com.exiledradio.villagerbackport.raid.Raids;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.PotionEffect;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ITickable;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;

import javax.annotation.Nullable;
import java.util.List;

/**
 * A bell's swing, and the raiders it gives away.
 *
 * <h2>State that only the client needs</h2>
 * None of this is saved or sent as tile entity data. A swing lasts two and a half seconds and means
 * nothing afterwards, so persisting it would be pointless - a bell rung as the world saved should
 * not still be swinging when it loads.
 *
 * <p>The ring is announced with a block event instead: the server calls
 * {@code world.addBlockEvent}, which every client watching the block receives and turns into a
 * swing locally. That is how 1.14 does it and how vanilla drives chest lids and note blocks - one
 * tiny packet, no synchronised state, and clients out of range simply never hear about it.
 */
public class TileEntityBell extends TileEntity implements ITickable {

    /** Ticks a swing lasts before the bell settles, matching 1.14. */
    public static final int SWING_TICKS = 50;

    /** Block event id for "the bell was rung", paired with the struck side. */
    public static final int EVENT_RING = 1;

    /** 1.14's raider check: raiders this close when the bell rings set it resonating. */
    private static final double RAIDER_HEARING = 32.0D;

    /** 1.14's reveal: raiders this close light up once the resonance has rung out. */
    private static final double RAIDER_REVEAL = 48.0D;

    /** Ticks into the swing before 1.14 listens for raiders, and from then to the glow. */
    private static final int LISTEN_AT = 5;
    private static final int REVEAL_AFTER = 40;

    /** How long a revealed raider glows: three seconds. */
    private static final int GLOW_TICKS = 60;

    private boolean ringing;
    private int ringTicks;
    private EnumFacing struckFrom = EnumFacing.NORTH;

    /** The raiders in reach when the bell was rung, found once and reused for the whole swing. */
    @Nullable
    private List<EntityLivingBase> raidersAtRing;
    private long raidersFoundAt = Long.MIN_VALUE;

    private boolean resonating;
    private int resonateTicks;

    public boolean isRinging() {
        return this.ringing;
    }

    public int ringTicks() {
        return this.ringTicks;
    }

    public EnumFacing struckFrom() {
        return this.struckFrom;
    }

    /**
     * Starts a swing, or restarts one already under way.
     *
     * <p>Restarting rather than ignoring the second ring is deliberate: striking a swinging bell
     * again should set it going afresh, not be swallowed.
     */
    @Override
    public boolean receiveClientEvent(int id, int param) {
        if (id != EVENT_RING) {
            return super.receiveClientEvent(id, param);
        }

        findRaiders();
        this.resonateTicks = 0;
        this.struckFrom = EnumFacing.byIndex(param);
        this.ringTicks = 0;
        this.ringing = true;
        return true;
    }

    /**
     * Notes who is in reach, as 1.14 does when the ring event arrives - on the server and on every
     * watching client alike, since the reveal is a glow on one and particles on the other.
     *
     * <p>Kept for three seconds and not looked for again in that time, so a bell struck repeatedly
     * is not searching a 96-block box every time.
     */
    private void findRaiders() {
        if (this.world == null || !ModConfig.raids.bellRevealsRaiders) {
            this.raidersAtRing = null;
            return;
        }

        long now = this.world.getTotalWorldTime();
        if (this.raidersAtRing == null || now > this.raidersFoundAt + 60L) {
            this.raidersFoundAt = now;
            this.raidersAtRing = this.world.getEntitiesWithinAABB(EntityLivingBase.class,
                    new AxisAlignedBB(this.pos).grow(RAIDER_REVEAL), Raids::isRaider);
        }
    }

    /** @return true if a raider was within hearing of this bell when it was last rung */
    public boolean hasRaidersNearby() {
        return anyWithin(RAIDER_HEARING);
    }

    private boolean anyWithin(double range) {
        if (this.raidersAtRing == null) {
            return false;
        }
        for (EntityLivingBase raider : this.raidersAtRing) {
            if (raider.isEntityAlive() && raider.getDistanceSqToCenter(this.pos) <= range * range) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void update() {
        if (this.ringing) {
            this.ringTicks++;
        }

        // 1.14's order: the raider check looks at the swing before it can have ended, so a bell is
        // only ever listening for the first quarter-second after a strike.
        if (this.ringing && this.ringTicks >= LISTEN_AT && this.resonateTicks == 0 && hasRaidersNearby()) {
            this.resonating = true;
            if (!this.world.isRemote) {
                this.world.playSound(null, this.pos, ModSounds.bellResonate, SoundCategory.BLOCKS, 1.0F, 1.0F);
            }
        }

        if (this.ringTicks >= SWING_TICKS) {
            this.ringing = false;
            this.ringTicks = 0;
        }

        if (this.resonating) {
            if (this.resonateTicks < REVEAL_AFTER) {
                this.resonateTicks++;
            } else {
                reveal();
                this.resonating = false;
            }
        }
    }

    /** The glow on the server, and 1.14's trail of particles pointing at each raider on clients. */
    private void reveal() {
        if (this.raidersAtRing == null) {
            return;
        }

        if (!this.world.isRemote) {
            for (EntityLivingBase raider : this.raidersAtRing) {
                if (raider.isEntityAlive() && raider.getDistanceSqToCenter(this.pos) <= RAIDER_REVEAL * RAIDER_REVEAL) {
                    raider.addPotionEffect(new PotionEffect(MobEffects.GLOWING, GLOW_TICKS));
                }
            }
            return;
        }

        // 1.14's colour walk and particle count: fewer particles per raider the more there are.
        int colour = 16700985;
        int count = this.raidersAtRing.size();
        int perRaider = MathHelper.clamp((count - 21) / -2, 3, 15);

        for (EntityLivingBase raider : this.raidersAtRing) {
            if (!raider.isEntityAlive() || raider.getDistanceSqToCenter(this.pos) > RAIDER_REVEAL * RAIDER_REVEAL) {
                continue;
            }

            double dx = raider.posX - this.pos.getX();
            double dz = raider.posZ - this.pos.getZ();
            float distance = MathHelper.sqrt(dx * dx + dz * dz);
            if (distance <= 0.0F) {
                continue;
            }

            double x = this.pos.getX() + 0.5D + dx / distance;
            double z = this.pos.getZ() + 0.5D + dz / distance;

            for (int i = 0; i < perRaider; i++) {
                colour += 5;
                this.world.spawnParticle(EnumParticleTypes.SPELL_MOB, x, this.pos.getY() + 0.5D, z,
                        (colour >> 16 & 255) / 255.0D, (colour >> 8 & 255) / 255.0D, (colour & 255) / 255.0D);
            }
        }
    }

    /**
     * Keeps the tile entity across block state changes, so a bell does not lose its swing to an
     * unrelated update - the same guard the barrel and furnaces needed.
     */
    @Override
    public boolean shouldRefresh(net.minecraft.world.World world, net.minecraft.util.math.BlockPos pos,
                                 net.minecraft.block.state.IBlockState oldState,
                                 net.minecraft.block.state.IBlockState newState) {
        return oldState.getBlock() != newState.getBlock();
    }

    /**
     * The bell body is drawn beyond the block it sits in, so the renderer has to keep working when
     * the block itself is off screen.
     */
    @Override
    public net.minecraft.util.math.AxisAlignedBB getRenderBoundingBox() {
        return new net.minecraft.util.math.AxisAlignedBB(this.pos).grow(1.0D);
    }
}
