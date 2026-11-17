package com.exiledradio.villagerbackport.mechanics;

import com.exiledradio.villagerbackport.ModConfig;
import com.exiledradio.villagerbackport.VillagerBackport;
import com.exiledradio.villagerbackport.illager.EntityPillager;
import com.exiledradio.villagerbackport.illager.EntityRavager;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.passive.EntityParrot;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.world.NoteBlockEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import javax.annotation.Nullable;

/**
 * 1.14's sounds for things 1.12.2 has: the six note block instruments (19w09a) and the parrot's pillager and ravager
 * imitations (19w11a). 1.14 also had parrots imitate pandas; 1.15 (19w41a) took that out, so it is not here.
 */
public final class MechanicsSounds {

    public static SoundEvent ironXylophone;
    public static SoundEvent cowBell;
    public static SoundEvent didgeridoo;
    public static SoundEvent bit;
    public static SoundEvent banjo;
    public static SoundEvent pling;
    public static SoundEvent imitatePillager;
    public static SoundEvent imitateRavager;

    private MechanicsSounds() {
    }

    static void init() {
        MinecraftForge.EVENT_BUS.register(new MechanicsSounds.Events());
    }

    /** Init: the parrot learns the two raiders (1.12.2 keeps the list in a static map). */
    static void registerImitations() {
        EntityParrot.registerMimicSound(EntityPillager.class, imitatePillager);
        EntityParrot.registerMimicSound(EntityRavager.class, imitateRavager);
    }

    /** 1.14's {@code NoteBlockInstrument.byState} for the six new instruments, by the block under the note block. */
    @Nullable
    public static SoundEvent instrument(IBlockState below) {
        Block block = below.getBlock();
        if (block == Blocks.IRON_BLOCK) {
            return ironXylophone;
        } else if (block == Blocks.SOUL_SAND) {
            return cowBell;
        } else if (block == Blocks.PUMPKIN) {
            // 1.12.2's pumpkin is the one that grows, which 1.13 made the plain (uncarved) pumpkin 1.14 names here.
            return didgeridoo;
        } else if (block == Blocks.EMERALD_BLOCK) {
            return bit;
        } else if (block == Blocks.HAY_BLOCK) {
            return banjo;
        } else if (block == Blocks.GLOWSTONE) {
            return pling;
        }
        return null;
    }

    public static final class Events {

        @SubscribeEvent
        public void onSounds(RegistryEvent.Register<SoundEvent> event) {
            ironXylophone = sound(event, "block.note_block.iron_xylophone");
            cowBell = sound(event, "block.note_block.cow_bell");
            didgeridoo = sound(event, "block.note_block.didgeridoo");
            bit = sound(event, "block.note_block.bit");
            banjo = sound(event, "block.note_block.banjo");
            pling = sound(event, "block.note_block.pling");
            imitatePillager = sound(event, "entity.parrot.imitate.pillager");
            imitateRavager = sound(event, "entity.parrot.imitate.ravager");
        }

        private static SoundEvent sound(RegistryEvent.Register<SoundEvent> event, String name) {
            ResourceLocation id = new ResourceLocation(VillagerBackport.MOD_ID, name);
            SoundEvent sound = new SoundEvent(id).setRegistryName(id);
            event.getRegistry().register(sound);
            return sound;
        }

        /**
         * A note block over one of the six blocks plays its 1.14 instrument. 1.12.2's block event has no way to name a
         * new sound, so the server cancels it and plays the note itself, with vanilla's pitch, volume, category and
         * note particle; the cancelled event is never sent on to clients.
         */
        @SubscribeEvent(priority = EventPriority.LOW)
        public void onNote(NoteBlockEvent.Play event) {
            World world = event.getWorld();
            if (!ModConfig.mechanics.noteBlockInstruments || world.isRemote || event.isCanceled()) {
                return;
            }
            BlockPos pos = event.getPos();
            SoundEvent sound = instrument(world.getBlockState(pos.down()));
            if (sound == null) {
                return;
            }
            event.setCanceled(true);
            int note = event.getVanillaNoteId();
            float pitch = (float) Math.pow(2.0D, (note - 12) / 12.0D);
            world.playSound((EntityPlayer) null, pos, sound, SoundCategory.RECORDS, 3.0F, pitch);
            if (world instanceof WorldServer) {
                // Count 0: the client takes the offsets as the particle's speed, and the note particle reads its
                // color from the first.
                ((WorldServer) world).spawnParticle(EnumParticleTypes.NOTE, pos.getX() + 0.5D, pos.getY() + 1.2D,
                        pos.getZ() + 0.5D, 0, note / 24.0D, 0.0D, 0.0D, 1.0D);
            }
        }
    }
}
