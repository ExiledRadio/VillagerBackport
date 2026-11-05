package com.exiledradio.villagerbackport.flora;

import com.exiledradio.villagerbackport.ModConfig;
import com.exiledradio.villagerbackport.VillagerBackport;
import com.exiledradio.villagerbackport.network.NetworkHandler;

import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.passive.EntityCow;
import net.minecraft.entity.passive.EntityMooshroom;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.EntityStruckByLightningEvent;
import net.minecraftforge.event.entity.living.BabyEntitySpawnEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Brown mooshrooms, on 1.12.2's own mooshroom.
 *
 * <h2>Not a new entity</h2>
 * A brown mooshroom in 1.14 is the same entity as a red one with a different {@code Type}. Here it
 * is the same too: 1.12.2's {@link EntityMooshroom} with a flag in its Forge data, which is saved
 * with it and survives anything that saves and reloads the entity. Nothing about red mooshrooms
 * changes, no entity is replaced, and a mod that knows 1.12.2's mooshroom still knows every
 * mooshroom in the world. The client learns which ones are brown from {@link PacketMooshroomType}
 * and draws those with 1.14's brown texture and brown mushrooms.
 *
 * <h2>What makes one brown</h2>
 * <ul>
 * <li><b>Lightning.</b> A bolt turns a red mooshroom brown and a brown one red, with 1.14's convert
 * sound, and does it no harm. A bolt lingers for a few ticks and strikes everything under it on each
 * of them, so the bolt's id is remembered and each bolt converts a mooshroom once, as in 1.14.</li>
 * <li><b>Breeding.</b> Two parents of the same color have a calf of their color, except one time in
 * 1024 it is the other; parents of different colors have either, at even odds.</li>
 * </ul>
 * Nothing spawns brown. Natural mooshrooms are red, as in 1.14.
 *
 * <h2>What a brown one does</h2>
 * <ul>
 * <li><b>Fed a small flower</b>, it eats it (eat sound, effect particles) and remembers the flower's
 * stew effect. Fed another before being milked, it puffs smoke and leaves the second flower alone.
 * Calves cannot be fed.</li>
 * <li><b>Milked with a bowl</b> after eating, it gives that flower's suspicious stew and forgets the
 * flower; until it eats again it gives ordinary mushroom stew. This works in creative too, as it has
 * since 1.16.</li>
 * <li><b>Sheared</b>, it drops brown mushrooms rather than red - however it was sheared, by a player,
 * a dispenser or another mod's shears. See {@link #onJoinWorld}.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = VillagerBackport.MOD_ID)
public final class Mooshrooms {

    private static final String BROWN = VillagerBackport.MOD_ID + ":BrownMooshroom";
    private static final String EFFECT = VillagerBackport.MOD_ID + ":StewEffect";
    private static final String DURATION = VillagerBackport.MOD_ID + ":StewEffectDuration";

    /** The last bolt that converted each mooshroom, so a lingering bolt converts it only once. */
    private static final Map<Entity, UUID> LAST_BOLT = new WeakHashMap<Entity, UUID>();

    /** Where a brown mooshroom was just sheared, so the red mushrooms it drops can be made brown. */
    @Nullable
    private static Shearing shearing;

    private Mooshrooms() {
    }

    private static boolean active() {
        return FloraContent.enabled() && ModConfig.flora.brownMooshrooms;
    }

    /** @return true if this mooshroom is brown. Correct on the client once it has been told. */
    public static boolean isBrown(Entity mooshroom) {
        return mooshroom.getEntityData().getBoolean(BROWN);
    }

    /** Makes a mooshroom brown or red, and tells everyone who can see it. */
    public static void setBrown(EntityMooshroom mooshroom, boolean brown) {
        NBTTagCompound data = mooshroom.getEntityData();
        if (brown) {
            data.setBoolean(BROWN, true);
        } else {
            data.removeTag(BROWN);
            clearStew(mooshroom);
        }
        sync(mooshroom);
    }

    /** Client side: what the server said. */
    public static void setBrownClient(Entity mooshroom, boolean brown) {
        if (brown) {
            mooshroom.getEntityData().setBoolean(BROWN, true);
        } else {
            mooshroom.getEntityData().removeTag(BROWN);
        }
    }

    private static void sync(EntityMooshroom mooshroom) {
        if (!(mooshroom.world instanceof WorldServer)) {
            return;
        }
        PacketMooshroomType packet = new PacketMooshroomType(mooshroom.getEntityId(), isBrown(mooshroom));
        for (EntityPlayer player : ((WorldServer) mooshroom.world).getEntityTracker().getTrackingPlayers(mooshroom)) {
            if (player instanceof EntityPlayerMP) {
                NetworkHandler.sendFlora(packet, (EntityPlayerMP) player);
            }
        }
    }

    /** @return true if a brown mooshroom has eaten a flower it has not been milked for */
    public static boolean hasStew(Entity mooshroom) {
        return mooshroom.getEntityData().hasKey(EFFECT, 3);
    }

    private static void clearStew(Entity mooshroom) {
        mooshroom.getEntityData().removeTag(EFFECT);
        mooshroom.getEntityData().removeTag(DURATION);
    }

    // ---------------------------------------------------------------------------------------------
    // Sync
    // ---------------------------------------------------------------------------------------------

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (active() && event.getTarget() instanceof EntityMooshroom && isBrown(event.getTarget())
                && event.getEntityPlayer() instanceof EntityPlayerMP) {
            NetworkHandler.sendFlora(new PacketMooshroomType(event.getTarget().getEntityId(), true),
                    (EntityPlayerMP) event.getEntityPlayer());
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Lightning and breeding
    // ---------------------------------------------------------------------------------------------

    @SubscribeEvent
    public static void onLightning(EntityStruckByLightningEvent event) {
        if (!active() || !(event.getEntity() instanceof EntityMooshroom)) {
            return;
        }

        // 1.14's MooshroomEntity.onStruckByLightning does not call the base method: no fire, no damage.
        event.setCanceled(true);

        EntityMooshroom mooshroom = (EntityMooshroom) event.getEntity();
        if (mooshroom.world.isRemote) {
            return;
        }

        UUID bolt = event.getLightning().getUniqueID();
        if (bolt.equals(LAST_BOLT.get(mooshroom))) {
            return;
        }
        LAST_BOLT.put(mooshroom, bolt);

        setBrown(mooshroom, !isBrown(mooshroom));
        play(mooshroom, FloraContent.mooshroomConvert, 2.0F);
    }

    @SubscribeEvent
    public static void onBabySpawn(BabyEntitySpawnEvent event) {
        if (!active() || !(event.getChild() instanceof EntityMooshroom)
                || !(event.getParentA() instanceof EntityMooshroom) || !(event.getParentB() instanceof EntityMooshroom)) {
            return;
        }

        EntityMooshroom child = (EntityMooshroom) event.getChild();
        boolean a = isBrown(event.getParentA());
        boolean b = isBrown(event.getParentB());
        boolean brown;
        if (a == b) {
            // 1.14's MooshroomEntity.func_213445_a: the same type, mutating one time in 1024.
            brown = child.getRNG().nextInt(1024) == 0 ? !a : a;
        } else {
            brown = child.getRNG().nextBoolean();
        }
        if (brown) {
            child.getEntityData().setBoolean(BROWN, true);
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Feeding and milking
    // ---------------------------------------------------------------------------------------------

    /**
     * Before the mooshroom's own {@code processInteract}, so a bowl on a brown mooshroom that has eaten
     * gives suspicious stew rather than mushroom stew.
     *
     * <p>The client does not know whether a mooshroom has eaten - only the server does - so on the
     * client every flower or bowl used on a brown adult is cancelled and left to the server, whose
     * reply puts the right stew in the player's hand. Forge sends the click to the server before the
     * client's event runs, so cancelling here loses nothing.
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (!active() || !(event.getTarget() instanceof EntityMooshroom)) {
            return;
        }

        EntityMooshroom mooshroom = (EntityMooshroom) event.getTarget();
        EntityPlayer player = event.getEntityPlayer();
        ItemStack held = event.getItemStack();
        boolean adult = mooshroom.getGrowingAge() >= 0;

        if (held.getItem() == Items.BOWL && adult) {
            if (!isBrown(mooshroom) || (!mooshroom.world.isRemote && !hasStew(mooshroom))) {
                // Ordinary mushroom stew: vanilla's own code gives it. 1.14 added the sound.
                if (!mooshroom.world.isRemote && !player.capabilities.isCreativeMode) {
                    play(mooshroom, FloraContent.mooshroomMilk, 1.0F);
                }
                return;
            }
            if (!mooshroom.world.isRemote) {
                milkStew(mooshroom, player, held, event);
            }
            succeed(event);
            return;
        }

        StewEffects.Entry entry = StewEffects.forStack(held);
        if (entry == null || !adult || !isBrown(mooshroom)) {
            return;
        }

        if (!mooshroom.world.isRemote) {
            feed(mooshroom, player, held, entry);
        }
        succeed(event);
    }

    private static void milkStew(EntityMooshroom mooshroom, EntityPlayer player, ItemStack bowl,
                                 PlayerInteractEvent.EntityInteract event) {
        NBTTagCompound data = mooshroom.getEntityData();
        Potion effect = Potion.getPotionById(data.getInteger(EFFECT));
        int ticks = data.getInteger(DURATION);
        clearStew(mooshroom);

        ItemStack stew = ItemSuspiciousStew.create(effect, ticks);
        bowl.shrink(1);
        if (bowl.isEmpty()) {
            player.setHeldItem(event.getHand(), stew);
        } else if (!player.inventory.addItemStackToInventory(stew)) {
            player.dropItem(stew, false);
        }
        play(mooshroom, FloraContent.mooshroomSuspiciousMilk, 1.0F);
    }

    private static void feed(EntityMooshroom mooshroom, EntityPlayer player, ItemStack flower,
                             StewEffects.Entry entry) {
        WorldServer world = (WorldServer) mooshroom.world;

        if (hasStew(mooshroom)) {
            // 1.14: two puffs of smoke, and the flower is not taken.
            for (int i = 0; i < 2; i++) {
                world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL,
                        mooshroom.posX + mooshroom.getRNG().nextDouble() / 2.0D,
                        mooshroom.posY + mooshroom.height / 2.0F,
                        mooshroom.posZ + mooshroom.getRNG().nextDouble() / 2.0D,
                        1, 0.0D, 0.0D, 0.0D, 0.05D);
            }
            return;
        }

        if (!player.capabilities.isCreativeMode) {
            flower.shrink(1);
        }

        // 1.14: four bursts of effect particles.
        for (int i = 0; i < 4; i++) {
            world.spawnParticle(EnumParticleTypes.SPELL_MOB,
                    mooshroom.posX + mooshroom.getRNG().nextDouble() / 2.0D,
                    mooshroom.posY + mooshroom.height / 2.0F,
                    mooshroom.posZ + mooshroom.getRNG().nextDouble() / 2.0D,
                    0, 0.0D, 0.0D, 0.0D, 0.2D);
        }

        mooshroom.getEntityData().setInteger(EFFECT, Potion.getIdFromPotion(entry.effect));
        mooshroom.getEntityData().setInteger(DURATION, entry.ticks);
        play(mooshroom, FloraContent.mooshroomEat, 2.0F);
    }

    private static void succeed(PlayerInteractEvent.EntityInteract event) {
        event.setCanceled(true);
        event.setCancellationResult(EnumActionResult.SUCCESS);
    }

    private static void play(Entity entity, @Nullable SoundEvent sound, float volume) {
        if (sound != null) {
            entity.world.playSound(null, entity.posX, entity.posY, entity.posZ, sound, SoundCategory.NEUTRAL,
                    volume, 1.0F);
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Shearing
    // ---------------------------------------------------------------------------------------------

    /** A brown mooshroom that has just been sheared: where, and on which tick. */
    private static final class Shearing {
        final World world;
        final double x;
        final double y;
        final double z;
        final long tick;
        int remaining = 5;

        Shearing(World world, Entity mooshroom) {
            this.world = world;
            this.x = mooshroom.posX;
            this.y = mooshroom.posY;
            this.z = mooshroom.posZ;
            this.tick = world.getTotalWorldTime();
        }

        boolean covers(Entity item) {
            return this.remaining > 0 && item.world == this.world && item.world.getTotalWorldTime() == this.tick
                    && item.getDistanceSq(this.x, this.y + 1.0D, this.z) < 9.0D;
        }
    }

    /**
     * Turns a sheared brown mooshroom's red mushrooms brown.
     *
     * <p>Shearing is Forge's {@code IShearable}, which 1.12.2's mooshroom implements by removing
     * itself, spawning a cow in its place and handing back five red mushrooms for the shears to drop -
     * all in one call, the same tick, whichever shears did it. So the cow arriving where a brown
     * mooshroom has just been removed marks a shearing, and red mushrooms appearing beside it on the
     * same tick are its drop. Catching it here rather than at the shears is what covers dispensers and
     * other mods' shears too.
     */
    @SubscribeEvent
    public static void onJoinWorld(EntityJoinWorldEvent event) {
        Entity entity = event.getEntity();
        if (entity.world.isRemote || !active()) {
            return;
        }

        if (entity instanceof EntityCow && !(entity instanceof EntityMooshroom)) {
            for (EntityMooshroom mooshroom : entity.world.getEntitiesWithinAABB(EntityMooshroom.class,
                    entity.getEntityBoundingBox().grow(0.25D))) {
                if (mooshroom.isDead && isBrown(mooshroom)
                        && mooshroom.getDistanceSq(entity) < 0.01D) {
                    shearing = new Shearing(entity.world, mooshroom);
                    return;
                }
            }
            // A cow from anything else - a red mooshroom sheared, a cow spawned - ends the last one.
            shearing = null;
            return;
        }

        if (entity instanceof EntityItem && shearing != null) {
            EntityItem item = (EntityItem) entity;
            ItemStack stack = item.getItem();
            if (stack.getItem() == Item.getItemFromBlock(Blocks.RED_MUSHROOM) && shearing.covers(item)) {
                item.setItem(new ItemStack(Blocks.BROWN_MUSHROOM, stack.getCount()));
                shearing.remaining -= stack.getCount();
            } else if (shearing.world.getTotalWorldTime() != shearing.tick) {
                shearing = null;
            }
        }
    }
}
