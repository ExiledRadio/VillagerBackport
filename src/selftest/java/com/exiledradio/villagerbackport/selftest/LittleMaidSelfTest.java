package com.exiledradio.villagerbackport.selftest;

import com.exiledradio.villagerbackport.compat.LittleMaidCompat;
import com.exiledradio.villagerbackport.crossbow.CrossbowContent;
import com.exiledradio.villagerbackport.crossbow.ItemCrossbow;

import com.mojang.authlib.GameProfile;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.fml.common.Loader;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * {@code -Pselftest=littlemaid}: the crossbow in LittleMaidReengaged maids' hands.
 *
 * <p>A fake player (a maid's avatar) that draws for 16 ticks, a maid's draw, loads the crossbow, and its next use
 * fires an arrow; a mob drawing as long still needs the full charge. With LittleMaidReengaged installed, the
 * crossbow is in the maids' default {@code "Archer:Bow"} triggers.
 */
public final class LittleMaidSelfTest implements SelfTest {

    private static final int USE_TIME = 72000;
    private static final int MAID_DRAW = 16;

    @Override
    public String name() {
        return "littlemaid";
    }

    @Override
    public void run(Check c) {
        ItemCrossbow crossbow = CrossbowContent.crossbow;
        if (!c.ok(crossbow != null, "the crossbow is registered")) {
            return;
        }
        WorldServer world = DimensionManager.getWorld(0);
        double x = world.getSpawnPoint().getX() + 0.5, y = 200, z = world.getSpawnPoint().getZ() + 0.5;

        // A fake player, as a maid's avatar is: arrows in its inventory, a 16-tick draw.
        FakePlayer maid = FakePlayerFactory.get(world, new GameProfile(UUID.fromString("3d5a1f4e-7c2b-4b6a-9e1d-2f8c6a4b0e11"), "[SelfTestMaid]"));
        maid.setPosition(x, y, z);
        maid.inventory.clear();
        ItemStack stack = new ItemStack(crossbow);
        maid.setHeldItem(EnumHand.MAIN_HAND, stack);
        maid.inventory.setInventorySlotContents(9, new ItemStack(Items.ARROW, 8));
        crossbow.onItemRightClick(world, maid, EnumHand.MAIN_HAND);
        crossbow.onPlayerStoppedUsing(stack, world, maid, USE_TIME - MAID_DRAW);
        c.ok(ItemCrossbow.isCharged(stack), "a fake player's 16-tick draw loads the crossbow");
        c.eq(7, maid.inventory.getStackInSlot(9).getCount(), "loading took one arrow from the fake player's inventory");

        int before = arrows(world, x, y, z).size();
        crossbow.onItemRightClick(world, maid, EnumHand.MAIN_HAND);
        c.ok(!ItemCrossbow.isCharged(stack), "its next use fires");
        c.eq(before + 1, arrows(world, x, y, z).size(), "an arrow flew");
        for (EntityArrow arrow : arrows(world, x, y, z)) {
            arrow.setDead();
        }

        // A mob drawing for 16 ticks: not yet, as 1.14's crossbow (25 ticks).
        EntityZombie zombie = new EntityZombie(world);
        zombie.setPosition(x, y, z);
        ItemStack held = new ItemStack(crossbow);
        zombie.setHeldItem(EnumHand.MAIN_HAND, held);
        zombie.setHeldItem(EnumHand.OFF_HAND, new ItemStack(Items.ARROW, 8));
        crossbow.onPlayerStoppedUsing(held, world, zombie, USE_TIME - MAID_DRAW);
        c.ok(!ItemCrossbow.isCharged(held), "a mob's 16-tick draw does not load it");
        crossbow.onPlayerStoppedUsing(held, world, zombie, USE_TIME - 25);
        c.ok(ItemCrossbow.isCharged(held), "a mob's 25-tick draw does");

        if (Loader.isModLoaded(LittleMaidCompat.MOD_ID)) {
            c.ok(archerTrigger(crossbow), "LittleMaidReengaged: the crossbow is in the maids' default archer triggers");
        } else {
            c.ok(true, "LittleMaidReengaged not installed: trigger check skipped");
        }
    }

    private static List<EntityArrow> arrows(WorldServer world, double x, double y, double z) {
        return world.getEntitiesWithinAABB(EntityArrow.class, new AxisAlignedBB(x - 8, y - 8, z - 8, x + 8, y + 8, z + 8));
    }

    @SuppressWarnings("unchecked")
    private static boolean archerTrigger(Item item) {
        try {
            Class<?> trigger = Class.forName("net.blacklab.lmr.entity.littlemaid.trigger.ModeTrigger");
            Field defaults = trigger.getDeclaredField("defaultTrigger");
            defaults.setAccessible(true);
            Method map = trigger.getDeclaredMethod("getTriggerMap", String.class);
            map.setAccessible(true);
            Map<Item, Object> triggers = (Map<Item, Object>) map.invoke(defaults.get(null), "Archer:Bow");
            Object status = triggers.get(item);
            Method isTriggerable = trigger.getMethod("isTriggerable", String.class, Item.class, boolean.class);
            Object maidCopy = trigger.getMethod("getDefaultInstance").invoke(null);
            boolean forMaid = (Boolean) isTriggerable.invoke(maidCopy, "Archer:Bow", item, false);
            return status != null && "TRIGGER".equals(status.toString()) && forMaid;
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }
}
