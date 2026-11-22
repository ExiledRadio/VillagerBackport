package com.exiledradio.villagerbackport.selftest;

import com.exiledradio.backportcore.migrate.StandInItem;
import com.exiledradio.villagerbackport.illager.EntityPillager;
import com.exiledradio.villagerbackport.migrate.OminousBottles;
import com.exiledradio.villagerbackport.raid.ModEffects;
import com.exiledradio.villagerbackport.raid.OminousBanner;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.EntityVindicator;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.MinecraftForge;
import com.mojang.authlib.GameProfile;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.NetworkManager;
import net.minecraft.server.management.PlayerInteractionManager;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * {@code -Pselftest=ominous}: captains and the ominous bottle, which is Tricky Trials Backport's since 2.7.4.
 *
 * <p>Alone (1.14): a captain a player kills gives Bad Omen, one level more each time; nothing drops; old bottles load as
 * a stand-in. With Tricky Trials' bottle (the real mod, or the file {@code fake-tt} in the run directory, see
 * {@link FakeTrickyTrials}): a pillager captain drops a bottle I-V and gives no Bad Omen; other captains drop nothing;
 * the old name is renamed to Tricky Trials' bottle.
 */
public final class OminousSelfTest implements SelfTest {

    @Override
    public String name() {
        return "ominous";
    }

    @Override
    public void run(Check c) {
        WorldServer world = DimensionManager.getWorld(0);
        boolean withTrials = Loader.isModLoaded("trickytrialsbackport") || FakeTrickyTrials.active();
        Item trials = ForgeRegistries.ITEMS.getValue(OminousBottles.TRICKY_TRIALS_BOTTLE);
        c.ok(withTrials == OminousBottles.captainsDropBottles(), "captains drop bottles exactly when Tricky Trials' bottle is here ("
                + (withTrials ? "with" : "without") + " it)");

        Item old = ForgeRegistries.ITEMS.getValue(OminousBottles.OLD_BOTTLE);
        boolean oldIsStandIn = old instanceof StandInItem && OminousBottles.OLD_BOTTLE.equals(old.getRegistryName());
        if (withTrials) {
            c.ok(OminousBottles.standIn() == null && !oldIsStandIn, "no villagerbackport:ominous_bottle of ours with Tricky Trials");
            c.ok(OminousBottles.bottle() == trials, "the bottle is Tricky Trials'");
        } else {
            c.ok(oldIsStandIn && OminousBottles.standIn() == old, "alone, old bottles load as the stand-in");
            c.ok(old.getCreativeTab() == null, "the stand-in is in no creative tab");
            c.ok(((StandInItem) old).real(3, 2).isEmpty(), "the stand-in stays itself while no bottle is here");
            c.ok(OminousBottles.bottle(2).isEmpty(), "no bottle to drop");
        }
        remap(c, withTrials, trials);

        BlockPos at = world.getSpawnPoint().up(120);
        // A real server player whose connection goes nowhere: effects send packets a fake player can't take.
        EntityPlayerMP player = new EntityPlayerMP(world.getMinecraftServer(), world,
                new GameProfile(UUID.nameUUIDFromBytes("selftest:ominous".getBytes()), "TestOminous"), new PlayerInteractionManager(world));
        player.connection = new NetHandlerPlayServer(world.getMinecraftServer(), new NetworkManager(EnumPacketDirection.SERVERBOUND), player);
        player.setPosition(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        player.clearActivePotions();

        // A pillager captain killed by a player.
        int before = bottles(world, at).size();
        kill(world, new EntityPillager(world), at, DamageSource.causePlayerDamage(player));
        List<EntityItem> dropped = bottles(world, at);
        PotionEffect omen = player.getActivePotionEffect(ModEffects.badOmen);
        if (withTrials) {
            c.eq(before + 1, dropped.size(), "a pillager captain drops one ominous bottle");
            ItemStack stack = dropped.isEmpty() ? ItemStack.EMPTY : dropped.get(dropped.size() - 1).getItem();
            c.ok(stack.getItem() == trials && stack.getMetadata() >= 0 && stack.getMetadata() < OminousBottles.LEVELS,
                    "of Tricky Trials', level I-V (" + stack.getMetadata() + ")");
            c.ok(omen == null, "and gives no Bad Omen (1.21)");
            // Whatever killed it (1.21's loot table has no player condition).
            kill(world, new EntityPillager(world), at, DamageSource.OUT_OF_WORLD);
            c.eq(before + 2, bottles(world, at).size(), "a captain that fell out of the world drops one too");
            // Other illager captains drop none (MC-270014).
            kill(world, new EntityVindicator(world), at, DamageSource.causePlayerDamage(player));
            c.eq(before + 2, bottles(world, at).size(), "a vindicator captain drops no bottle");
            c.ok(player.getActivePotionEffect(ModEffects.badOmen) == null, "and gives no Bad Omen");
        } else {
            c.eq(before, dropped.size(), "alone, a captain drops no bottle");
            c.ok(omen != null && omen.getAmplifier() == 0 && omen.getDuration() > 119000, "and gives Bad Omen I for 100 minutes (1.14)");
            kill(world, new EntityVindicator(world), at, DamageSource.causePlayerDamage(player));
            omen = player.getActivePotionEffect(ModEffects.badOmen);
            c.ok(omen != null && omen.getAmplifier() == 1, "a second captain raises it to II");
            kill(world, new EntityPillager(world), at, DamageSource.OUT_OF_WORLD);
            omen = player.getActivePotionEffect(ModEffects.badOmen);
            c.ok(omen != null && omen.getAmplifier() == 1, "a captain no player killed gives nothing");
        }
        player.clearActivePotions();
        for (EntityItem item : bottles(world, at)) {
            item.setDead();
        }
    }

    /** Forge's MissingMappings for an older world's bottle, as it loads. */
    private static void remap(Check c, boolean withTrials, Item trials) {
        RegistryEvent.MissingMappings.Mapping<Item> mapping = new RegistryEvent.MissingMappings.Mapping<Item>(
                ForgeRegistries.ITEMS, ForgeRegistries.ITEMS, OminousBottles.OLD_BOTTLE, 4000);
        RegistryEvent.MissingMappings<Item> event = new RegistryEvent.MissingMappings<Item>(new ResourceLocation("minecraft", "items"),
                ForgeRegistries.ITEMS, Collections.singletonList(mapping));
        OminousBottles.missingItems(event);
        if (withTrials) {
            c.ok(mapping.getAction() == RegistryEvent.MissingMappings.Action.REMAP && mapping.getTarget() == trials,
                    "an older world's villagerbackport:ominous_bottle becomes Tricky Trials'");
        } else {
            c.ok(mapping.getAction() == RegistryEvent.MissingMappings.Action.DEFAULT, "alone, nothing to rename it to");
        }
    }

    private static void kill(WorldServer world, EntityLiving mob, BlockPos at, DamageSource source) {
        mob.setPosition(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        mob.setItemStackToSlot(EntityEquipmentSlot.HEAD, OminousBanner.create());
        MinecraftForge.EVENT_BUS.post(new LivingDeathEvent(mob, source));
    }

    private static List<EntityItem> bottles(WorldServer world, BlockPos at) {
        return world.getEntitiesWithinAABB(EntityItem.class, new AxisAlignedBB(at).grow(4.0),
                e -> e != null && e.getItem().getItem().getRegistryName() != null
                        && "ominous_bottle".equals(e.getItem().getItem().getRegistryName().getPath()));
    }
}
