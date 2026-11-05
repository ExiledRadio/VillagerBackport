package com.exiledradio.villagerbackport.raid;

import com.exiledradio.villagerbackport.ModConfig;
import com.exiledradio.villagerbackport.VillagerBackport;
import com.exiledradio.villagerbackport.block.ModSounds;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.DimensionType;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.GameRules;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.event.entity.living.PotionEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Everything that drives this mod's own raids from outside them.
 *
 * <ul>
 *   <li>A player with Bad Omen in a village gets Raid Omen for 30 seconds, and the raid starts where
 *       they were when it runs out - current Java's rule. With {@code raids.raidOmen} off it starts
 *       at once, as in 1.14.</li>
 *   <li>Killing a captain - a raider wearing the ominous banner, outside a raid - gives Bad Omen, one
 *       level more than the killer had, up to V, for 100 minutes, as in 1.14; or, with
 *       {@code raids.captainDropsOminousBottle}, drops an ominous bottle of a random level, as in
 *       current Java.</li>
 *   <li>Raiders saved mid-raid rejoin it as they load, never despawn while in it, and make whoever
 *       kills them a hero of the village.</li>
 * </ul>
 *
 * <p>Registered always; every handler stands down unless this mod's raids are the ones in use.
 */
public final class RaidEvents {

    /** Current Java's Raid Omen: 30 seconds of warning. */
    private static final int RAID_OMEN_TICKS = 600;

    /** 1.14's Bad Omen from a captain: 100 minutes. */
    private static final int BAD_OMEN_TICKS = 120000;

    private static final String OMEN_POS = "RaidOmenPos";

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event) {
        if (!event.getWorld().isRemote) {
            GameRules rules = event.getWorld().getGameRules();
            if (!rules.hasRule(RaidManager.DISABLE_RAIDS)) {
                rules.addGameRule(RaidManager.DISABLE_RAIDS, "false", GameRules.ValueType.BOOLEAN_VALUE);
            }
        }
    }

    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.world instanceof WorldServer && Raids.usingOwnRaids()) {
            RaidManager.get((WorldServer) event.world).tick();
        }
    }

    /** Bad Omen, carried into a village, becomes a raid. */
    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        EntityPlayer player = event.player;
        if (event.phase != TickEvent.Phase.END || !(player.world instanceof WorldServer) || !Raids.usingOwnRaids()
                || (player.ticksExisted + player.getEntityId()) % 10 != 0) {
            return;
        }

        PotionEffect omen = ModEffects.badOmenOn(player);
        if (omen == null || player.isSpectator() || !canRaidHere((WorldServer) player.world)) {
            return;
        }

        BlockPos pos = new BlockPos(player);
        if (!RaidVillages.isVillage(player.world, pos)) {
            return;
        }

        WorldServer world = (WorldServer) player.world;
        VillageRaid existing = RaidManager.get(world).findRaid(pos);
        ModEffects.clearBadOmen(player);

        if (existing != null && existing.isStarted() && existing.getBadOmenLevel() >= VillageRaid.MAX_LEVEL) {
            // A raid at full strength takes the omen and gives nothing for it, as in 1.14.
            return;
        }

        if (ModConfig.raids.raidOmen) {
            NBTTagCompound data = player.getEntityData();
            data.setLong(VillagerBackport.MOD_ID + ":" + OMEN_POS, pos.toLong());
            player.addPotionEffect(new PotionEffect(ModEffects.raidOmen, RAID_OMEN_TICKS, omen.getAmplifier(), false, true));
        } else {
            RaidManager.get(world).startOrExtend(pos, omen.getAmplifier() + 1);
        }
    }

    /** The Raid Omen has run out: the raid begins where it was taken. */
    @SubscribeEvent
    public void onPotionExpired(PotionEvent.PotionExpiryEvent event) {
        PotionEffect effect = event.getPotionEffect();
        EntityLivingBase entity = event.getEntityLiving();
        if (effect == null || effect.getPotion() != ModEffects.raidOmen || !(entity.world instanceof WorldServer)
                || !Raids.usingOwnRaids() || !(entity instanceof EntityPlayerMP) || ((EntityPlayerMP) entity).isSpectator()) {
            return;
        }

        WorldServer world = (WorldServer) entity.world;
        if (!canRaidHere(world)) {
            return;
        }

        String key = VillagerBackport.MOD_ID + ":" + OMEN_POS;
        BlockPos at = entity.getEntityData().hasKey(key)
                ? BlockPos.fromLong(entity.getEntityData().getLong(key)) : new BlockPos(entity);
        entity.getEntityData().removeTag(key);

        RaidManager.get(world).startOrExtend(at, effect.getAmplifier() + 1);
    }

    /** The omens' own sounds, as current Java plays them when one takes hold. */
    @SubscribeEvent
    public void onPotionAdded(PotionEvent.PotionAddedEvent event) {
        EntityLivingBase entity = event.getEntityLiving();
        if (entity.world.isRemote || event.getOldPotionEffect() != null) {
            return;
        }

        Potion potion = event.getPotionEffect().getPotion();
        SoundEvent sound = potion == ModEffects.raidOmen ? ModSounds.raidOmen
                : potion == ModEffects.badOmen ? ModSounds.badOmen : null;
        if (sound != null) {
            entity.world.playSound(null, entity.posX, entity.posY, entity.posZ, sound, SoundCategory.NEUTRAL, 1.0F, 1.0F);
        }
    }

    @SubscribeEvent
    public void onDeath(LivingDeathEvent event) {
        if (!(event.getEntityLiving() instanceof EntityLiving) || !(event.getEntityLiving().world instanceof WorldServer)
                || !Raids.usingOwnRaids()) {
            return;
        }

        EntityLiving dead = (EntityLiving) event.getEntityLiving();
        WorldServer world = (WorldServer) dead.world;
        Entity killer = event.getSource().getTrueSource();
        RaidManager manager = RaidManager.get(world);
        VillageRaid raid = manager.raidOf(dead);

        if (raid != null) {
            if (RaiderData.isLeader(dead)) {
                raid.removeLeader(RaiderData.wave(dead));
            }
            if (killer instanceof EntityPlayer) {
                raid.addHero(killer);
            }
            raid.leaveRaid(dead, false);
            return;
        }

        // A captain killed away from any raid: 1.14's Bad Omen, or current Java's ominous bottle.
        if (killer instanceof EntityPlayer && OminousBanner.isOminous(dead.getItemStackFromSlot(EntityEquipmentSlot.HEAD))
                && manager.findRaid(new BlockPos(dead)) == null) {
            if (ModConfig.raids.captainDropsOminousBottle) {
                dead.entityDropItem(ominousBottle(dead.getRNG().nextInt(5)), 0.0F);
                return;
            }
            EntityPlayer player = (EntityPlayer) killer;
            PotionEffect had = player.getActivePotionEffect(ModEffects.badOmen);
            int level = had == null ? 0 : Math.min(had.getAmplifier() + 1, 4);
            player.removePotionEffect(ModEffects.badOmen);
            player.addPotionEffect(new PotionEffect(ModEffects.badOmen, BAD_OMEN_TICKS, level, false, false));
        }
    }

    /** Deeper Depths' bottle when it is installed, so a pack has one ominous bottle, not two. */
    private static net.minecraft.item.ItemStack ominousBottle(int level) {
        net.minecraft.item.Item theirs = net.minecraftforge.fml.common.registry.ForgeRegistries.ITEMS.getValue(
                new net.minecraft.util.ResourceLocation("deeperdepths", "ominous_bottle"));
        net.minecraft.item.Item item = theirs != null && theirs != net.minecraft.init.Items.AIR
                ? theirs : com.exiledradio.villagerbackport.illager.IllagerContent.ominousBottle;
        return new net.minecraft.item.ItemStack(item, 1, level);
    }

    @SubscribeEvent
    public void onJoinWorld(EntityJoinWorldEvent event) {
        if (event.getWorld() instanceof WorldServer && event.getEntity() instanceof EntityLiving
                && Raids.usingOwnRaids() && RaiderData.raidId((EntityLiving) event.getEntity()) != 0) {
            RaidManager.get((WorldServer) event.getWorld()).rejoin((EntityLiving) event.getEntity());
        }
    }

    @SubscribeEvent
    public void onDespawnCheck(LivingSpawnEvent.AllowDespawn event) {
        if (event.getEntityLiving() instanceof EntityLiving && event.getWorld() instanceof WorldServer
                && Raids.usingOwnRaids()
                && RaidManager.get((WorldServer) event.getWorld()).raidOf(event.getEntityLiving()) != null) {
            event.setResult(Event.Result.DENY);
        }
    }

    private static boolean canRaidHere(WorldServer world) {
        return world.getDifficulty() != EnumDifficulty.PEACEFUL
                && world.provider.getDimensionType() != DimensionType.NETHER
                && !world.getGameRules().getBoolean(RaidManager.DISABLE_RAIDS);
    }
}
