package com.exiledradio.villagerbackport.crossbow;

import com.exiledradio.villagerbackport.VillagerBackport;
import com.exiledradio.villagerbackport.block.ModSounds;

import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.init.Items;
import net.minecraft.item.EnumAction;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArrow;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.stats.StatList;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 1.14's crossbow, with current Java's durability.
 *
 * <h2>Loading and firing</h2>
 * Hold use to load: a second and a quarter, a quarter less for each level of Quick Charge. Letting go
 * once it is drawn loads it, and it stays loaded - in a chest, in another slot, through a relog -
 * until the next use fires it. It loads an arrow (any kind, from anywhere in the inventory) or a
 * firework rocket held in either hand; a creative player needs no ammunition.
 *
 * <p>What is loaded is kept on the stack in 1.14's own tags, {@code Charged} and
 * {@code ChargedProjectiles}, so a crossbow carried out of this mod into a world with Future MC or
 * Crossbows Backport reads the same way to them.
 *
 * <h2>The enchantments</h2>
 * Multishot loads three of the one arrow and fires them ten degrees apart; only the middle one can be
 * picked up again. Piercing sends arrows through that many targets more. Unbreaking and Mending
 * apply as to any tool; the bow's own enchantments do not.
 *
 * <h2>Use time</h2>
 * 1.14 lets a crossbow be held drawn indefinitely and loads it on release. 1.12.2 finishes any use
 * when its time runs out, before release, so the use time here is effectively endless, as the bow's
 * is, and the draw is measured from it.
 */
public final class ItemCrossbow extends Item {

    private static final String CHARGED = "Charged";
    private static final String PROJECTILES = "ChargedProjectiles";
    private static final int USE_TIME = 72000;

    /** Arrow and rocket speeds, and the spread of 1.14's crossbow. */
    private static final float ARROW_SPEED = 3.15F;
    private static final float FIREWORK_SPEED = 1.6F;
    private static final float INACCURACY = 1.0F;

    public ItemCrossbow() {
        setMaxStackSize(1);
        setMaxDamage(465);

        addPropertyOverride(id("pulling"), (stack, world, entity) ->
                entity != null && entity.isHandActive() && entity.getActiveItemStack() == stack && !isCharged(stack) ? 1.0F : 0.0F);
        addPropertyOverride(id("pull"), (stack, world, entity) ->
                entity == null || isCharged(stack) ? 0.0F
                        : (float) (USE_TIME - entity.getItemInUseCount()) / chargeTicks(stack));
        addPropertyOverride(id("charged"), (stack, world, entity) -> isCharged(stack) ? 1.0F : 0.0F);
        addPropertyOverride(id("firework"), (stack, world, entity) ->
                isCharged(stack) && holds(stack, Items.FIREWORKS) ? 1.0F : 0.0F);
    }

    private static ResourceLocation id(String name) {
        return new ResourceLocation(VillagerBackport.MOD_ID, name);
    }

    // ---------------------------------------------------------------------------------------------
    // Use

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (isCharged(stack)) {
            if (!world.isRemote) {
                fire(world, player, stack, holds(stack, Items.FIREWORKS) ? FIREWORK_SPEED : ARROW_SPEED, INACCURACY);
            }
            return new ActionResult<ItemStack>(EnumActionResult.SUCCESS, stack);
        }
        if (player.capabilities.isCreativeMode || !findAmmo(player).isEmpty()) {
            player.setActiveHand(hand);
            return new ActionResult<ItemStack>(EnumActionResult.SUCCESS, stack);
        }
        return new ActionResult<ItemStack>(EnumActionResult.FAIL, stack);
    }

    @Override
    public void onUsingTick(ItemStack stack, EntityLivingBase user, int count) {
        if (user.world.isRemote || isCharged(stack)) {
            return;
        }
        // The sounds play a fifth of the way into the draw and half way, as 1.14's do; with Quick
        // Charge the start sound is quicker and there is no middle one.
        int quick = CrossbowContent.level(CrossbowContent.quickCharge, stack);
        int charge = chargeTicks(stack);
        int used = USE_TIME - count;
        SoundEvent sound = null;
        if (crossed(used, charge, 0.2F)) {
            sound = quick == 0 ? ModSounds.crossbowLoadingStart : ModSounds.crossbowQuickCharge[Math.min(quick, 3) - 1];
        } else if (quick == 0 && crossed(used, charge, 0.5F)) {
            sound = ModSounds.crossbowLoadingMiddle;
        }
        if (sound != null) {
            user.world.playSound(null, user.posX, user.posY, user.posZ, sound, category(user), 0.5F, 1.0F);
        }
    }

    private static boolean crossed(int used, int charge, float point) {
        return used >= point * charge && used - 1 < point * charge;
    }

    @Override
    public void onPlayerStoppedUsing(ItemStack stack, World world, EntityLivingBase user, int timeLeft) {
        if (world.isRemote || isCharged(stack) || USE_TIME - timeLeft < chargeTicks(stack)) {
            return;
        }
        if (load(user, stack)) {
            world.playSound(null, user.posX, user.posY, user.posZ, ModSounds.crossbowLoadingEnd, category(user),
                    1.0F, 1.0F / (world.rand.nextFloat() * 0.5F + 1.0F) + 0.2F);
        }
    }

    @Override
    public int getMaxItemUseDuration(ItemStack stack) {
        return USE_TIME;
    }

    /** The bow's pose while drawing, the nearest 1.12.2 has to 1.14's own. */
    @Override
    public EnumAction getItemUseAction(ItemStack stack) {
        return EnumAction.BOW;
    }

    @Override
    public int getItemEnchantability() {
        return 1;
    }

    /** Loading and firing change the stack's tags; neither should look like swapping items. */
    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || oldStack.getItem() != newStack.getItem();
    }

    /** 1.14's {@code getChargeTime}: 25 ticks, five fewer for each level of Quick Charge. */
    public static int chargeTicks(ItemStack stack) {
        return Math.max(0, 25 - 5 * CrossbowContent.level(CrossbowContent.quickCharge, stack));
    }

    private static SoundCategory category(EntityLivingBase user) {
        return user instanceof EntityPlayer ? SoundCategory.PLAYERS : SoundCategory.HOSTILE;
    }

    // ---------------------------------------------------------------------------------------------
    // Loading

    /**
     * 1.14's ammunition: an arrow or rocket in either hand, else the first arrow in the inventory.
     * Rockets are only ever taken from a hand.
     */
    private static ItemStack findAmmo(EntityLivingBase user) {
        for (EnumHand hand : new EnumHand[]{EnumHand.OFF_HAND, EnumHand.MAIN_HAND}) {
            ItemStack held = user.getHeldItem(hand);
            if (held.getItem() instanceof ItemArrow || held.getItem() == Items.FIREWORKS) {
                return held;
            }
        }
        if (user instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) user;
            for (int slot = 0; slot < player.inventory.getSizeInventory(); slot++) {
                ItemStack stack = player.inventory.getStackInSlot(slot);
                if (stack.getItem() instanceof ItemArrow) {
                    return stack;
                }
            }
        }
        return ItemStack.EMPTY;
    }

    /** Takes one piece of ammunition (none in creative) and loads it, three times over with Multishot. */
    private static boolean load(EntityLivingBase user, ItemStack crossbow) {
        boolean creative = user instanceof EntityPlayer && ((EntityPlayer) user).capabilities.isCreativeMode;
        ItemStack ammo = findAmmo(user);
        ItemStack one = ammo.isEmpty() ? (creative ? new ItemStack(Items.ARROW) : ItemStack.EMPTY) : ammo.copy();
        if (one.isEmpty()) {
            return false;
        }
        one.setCount(1);

        int copies = CrossbowContent.level(CrossbowContent.multishot, crossbow) > 0 ? 3 : 1;
        NBTTagList projectiles = new NBTTagList();
        for (int i = 0; i < copies; i++) {
            projectiles.appendTag(one.writeToNBT(new NBTTagCompound()));
        }
        tag(crossbow).setTag(PROJECTILES, projectiles);
        tag(crossbow).setBoolean(CHARGED, true);

        if (!creative && !ammo.isEmpty()) {
            ammo.shrink(1);
            if (ammo.isEmpty() && user instanceof EntityPlayer) {
                ((EntityPlayer) user).inventory.deleteStack(ammo);
            }
        }
        return true;
    }

    public static boolean isCharged(ItemStack stack) {
        return stack.hasTagCompound() && stack.getTagCompound().getBoolean(CHARGED);
    }

    private static List<ItemStack> projectiles(ItemStack stack) {
        List<ItemStack> list = new ArrayList<ItemStack>();
        if (stack.hasTagCompound()) {
            NBTTagList tags = stack.getTagCompound().getTagList(PROJECTILES, 10);
            for (int i = 0; i < tags.tagCount(); i++) {
                ItemStack projectile = new ItemStack(tags.getCompoundTagAt(i));
                if (!projectile.isEmpty()) {
                    list.add(projectile);
                }
            }
        }
        return list;
    }

    private static boolean holds(ItemStack stack, Item item) {
        for (ItemStack projectile : projectiles(stack)) {
            if (projectile.getItem() == item) {
                return true;
            }
        }
        return false;
    }

    private static NBTTagCompound tag(ItemStack stack) {
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }
        return stack.getTagCompound();
    }

    // ---------------------------------------------------------------------------------------------
    // Firing

    /** 1.14's {@code fireProjectiles}: everything loaded, the second and third ten degrees either side. */
    private static void fire(World world, EntityLivingBase shooter, ItemStack crossbow, float speed, float inaccuracy) {
        List<ItemStack> loaded = projectiles(crossbow);
        tag(crossbow).setTag(PROJECTILES, new NBTTagList());
        tag(crossbow).setBoolean(CHARGED, false);

        boolean creative = shooter instanceof EntityPlayer && ((EntityPlayer) shooter).capabilities.isCreativeMode;
        float[] pitches = pitches(shooter.getRNG());
        float[] angles = {0.0F, -10.0F, 10.0F};
        int shot = CrossbowAdvancements.newShot();
        for (int i = 0; i < loaded.size() && i < 3; i++) {
            launch(world, shooter, crossbow, loaded.get(i), pitches[i], creative || i > 0, speed, inaccuracy, angles[i], shot);
        }

        if (shooter instanceof EntityPlayerMP) {
            CrossbowAdvancements.shot((EntityPlayerMP) shooter);
            net.minecraft.stats.StatBase stat = StatList.getObjectUseStats(crossbow.getItem());
            if (stat != null) {
                ((EntityPlayerMP) shooter).addStat(stat);
            }
        }
    }

    private static void launch(World world, EntityLivingBase shooter, ItemStack crossbow, ItemStack ammo, float pitch,
                               boolean noPickup, float speed, float inaccuracy, float angle, int shot) {
        boolean firework = ammo.getItem() == Items.FIREWORKS;
        Entity projectile;
        if (firework) {
            projectile = new EntityCrossbowFirework(world, ammo, shooter);
        } else {
            EntityArrow arrow = arrow(world, shooter, crossbow, ammo);
            if (noPickup) {
                arrow.pickupStatus = EntityArrow.PickupStatus.CREATIVE_ONLY;
            }
            projectile = arrow;
        }

        Vec3d direction = spread(shooter, angle);
        ((net.minecraft.entity.IProjectile) projectile).shoot(direction.x, direction.y, direction.z, speed, inaccuracy);
        CrossbowAdvancements.tag(projectile, shot);

        crossbow.damageItem(firework ? 3 : 1, shooter);
        world.spawnEntity(projectile);
        world.playSound(null, shooter.posX, shooter.posY, shooter.posZ, ModSounds.crossbowShoot, SoundCategory.PLAYERS, 1.0F, pitch);
    }

    /**
     * An arrow of the kind loaded. Vanilla's three kinds are this mod's own subclasses, which know how
     * to pierce; another mod's arrow is made by its own item and flies as it always does.
     */
    public static EntityArrow arrow(World world, EntityLivingBase shooter, ItemStack crossbow, ItemStack ammo) {
        Item item = ammo.getItem();
        EntityArrow arrow;
        if (item == Items.SPECTRAL_ARROW) {
            arrow = new EntityCrossbowSpectralArrow(world, shooter);
        } else if (item == Items.ARROW || item == Items.TIPPED_ARROW || !(item instanceof ItemArrow)) {
            EntityCrossbowArrow tipped = new EntityCrossbowArrow(world, shooter);
            tipped.setPotionEffect(item instanceof ItemArrow ? ammo : new ItemStack(Items.ARROW));
            arrow = tipped;
        } else {
            arrow = ((ItemArrow) item).createArrow(world, ammo, shooter);
        }
        if (shooter instanceof EntityPlayer) {
            arrow.setIsCritical(true);
        }
        int pierce = CrossbowContent.level(CrossbowContent.piercing, crossbow);
        if (pierce > 0 && arrow instanceof Piercing.Arrow) {
            ((Piercing.Arrow) arrow).piercing().setLevel(pierce);
        }
        return arrow;
    }

    /**
     * Where the shooter is looking, turned by an angle about the shooter's own up - so a multishot's
     * side arrows spread across the crosshair however steeply it is aimed, as 1.14's do.
     */
    private static Vec3d spread(EntityLivingBase shooter, float angle) {
        Vec3d look = shooter.getLook(1.0F);
        if (angle == 0.0F) {
            return look;
        }
        Vec3d up = direction(shooter.rotationPitch - 90.0F, shooter.rotationYaw);
        double radians = Math.toRadians(angle);
        return look.scale(Math.cos(radians)).add(up.crossProduct(look).scale(Math.sin(radians)));
    }

    /** {@code Entity.getVectorForRotation}, which is protected. */
    private static Vec3d direction(float pitch, float yaw) {
        float cosYaw = MathHelper.cos(-yaw * 0.017453292F - (float) Math.PI);
        float sinYaw = MathHelper.sin(-yaw * 0.017453292F - (float) Math.PI);
        float cosPitch = -MathHelper.cos(-pitch * 0.017453292F);
        float sinPitch = MathHelper.sin(-pitch * 0.017453292F);
        return new Vec3d(sinYaw * cosPitch, sinPitch, cosYaw * cosPitch);
    }

    /** 1.14's three shot pitches: the middle at 1, the sides a little higher, one more than the other. */
    private static float[] pitches(Random random) {
        boolean high = random.nextBoolean();
        return new float[]{1.0F, sidePitch(high, random), sidePitch(!high, random)};
    }

    private static float sidePitch(boolean high, Random random) {
        return 1.0F / (random.nextFloat() * 0.5F + 1.8F) + (high ? 0.63F : 0.43F);
    }

    // ---------------------------------------------------------------------------------------------

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
        List<ItemStack> loaded = projectiles(stack);
        if (!isCharged(stack) || loaded.isEmpty()) {
            return;
        }
        ItemStack first = loaded.get(0);
        tooltip.add(I18n.format("item.villagerbackport.crossbow.projectile", first.getDisplayName()));
        if (flag.isAdvanced() && first.getItem() == Items.FIREWORKS) {
            List<String> lines = new ArrayList<String>();
            Items.FIREWORKS.addInformation(first, world, lines, flag);
            for (String line : lines) {
                tooltip.add("  " + net.minecraft.util.text.TextFormatting.GRAY + line);
            }
        }
    }
}
