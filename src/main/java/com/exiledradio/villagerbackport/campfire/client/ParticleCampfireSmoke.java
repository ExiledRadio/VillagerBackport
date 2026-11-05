package com.exiledradio.villagerbackport.campfire.client;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 1.14's {@code CampfireParticle}: the big, slow, long-lived smoke plume.
 *
 * <p>Numbers straight from 1.14.4: three times normal size, a quarter-block hitbox so it stops under
 * a ceiling, a lifetime of 80-129 ticks (280-329 over a hay bale - about 10 blocks of rise against
 * about 24), almost no gravity, a slow random sideways drift, and a fade over its last 60 ticks. The
 * cosy smoke is 0.9 opaque, the signal smoke 0.95.
 *
 * <p>It moves with collision, so a block directly above the campfire stops it, as in 1.14. Unlike
 * vanilla 1.12.2 particles it is added straight to the particle manager, which draws it however far
 * away the player is - 1.14 spawns campfire smoke "always visible" for the same reason: a signal fire
 * is meant to be seen from a distance.
 */
@SideOnly(Side.CLIENT)
public class ParticleCampfireSmoke extends Particle {

    public ParticleCampfireSmoke(World world, double x, double y, double z, double motionX, double motionY,
                                 double motionZ, boolean signal, TextureAtlasSprite sprite) {
        super(world, x, y, z);
        this.particleScale *= 3.0F;
        setSize(0.25F, 0.25F);
        this.particleMaxAge = signal ? this.rand.nextInt(50) + 280 : this.rand.nextInt(50) + 80;
        this.particleGravity = 3.0E-6F;
        this.motionX = motionX;
        this.motionY = motionY + this.rand.nextFloat() / 500.0F;
        this.motionZ = motionZ;
        this.particleAlpha = signal ? 0.95F : 0.9F;
        setParticleTexture(sprite);
    }

    /** Layer 1: drawn from the block atlas, where {@link CampfireClient} stitched the frames. */
    @Override
    public int getFXLayer() {
        return 1;
    }

    @Override
    public boolean shouldDisableDepth() {
        return true;
    }

    @Override
    public void onUpdate() {
        this.prevPosX = this.posX;
        this.prevPosY = this.posY;
        this.prevPosZ = this.posZ;
        if (this.particleAge++ >= this.particleMaxAge || this.particleAlpha <= 0.0F) {
            setExpired();
            return;
        }
        this.motionX += this.rand.nextFloat() / 5000.0F * (this.rand.nextBoolean() ? 1 : -1);
        this.motionZ += this.rand.nextFloat() / 5000.0F * (this.rand.nextBoolean() ? 1 : -1);
        this.motionY -= this.particleGravity;
        move(this.motionX, this.motionY, this.motionZ);
        if (this.particleAge >= this.particleMaxAge - 60 && this.particleAlpha > 0.01F) {
            this.particleAlpha -= 0.015F;
        }
    }
}
