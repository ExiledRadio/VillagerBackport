package com.exiledradio.villagerbackport.cat;

import com.exiledradio.villagerbackport.ModConfig;

import net.minecraft.block.BlockChest;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.ai.EntityAIAvoidEntity;
import net.minecraft.entity.ai.EntityAITasks;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.List;

/**
 * What cats do to other things: creepers and phantoms keep away from them, and a cat sitting on a
 * chest stops it being opened.
 *
 * <h2>Creepers</h2>
 * 1.12.2's creeper already runs from ocelots (six blocks, at 1.0 and 1.2 speed); this adds the same goal
 * for cats, to every creeper and every subclass of one, as it joins the world.
 *
 * <h2>Phantoms</h2>
 * 1.12.2 has no phantom, so this is for whichever mod brings one: any living thing registered as
 * {@code phantom}. 1.14's phantom checks for cats within 16 blocks each time it would carry on a swoop,
 * and if it finds any, gives the swoop up while each cat hisses at it. Other mods' phantoms have their
 * own AI, so this does the part that works for any of them: twice a second, a phantom with a cat
 * within 16 blocks loses its target and is nudged upward, and the cats hiss - once per broken-off
 * attack, not every half second. The attack can resume once it is clear of the cats.
 */
public final class CatFear {

    /** 1.14's check distance. */
    private static final double PHANTOM_RANGE = 16.0D;

    @SubscribeEvent
    public void onJoin(EntityJoinWorldEvent event) {
        if (event.getWorld().isRemote || !ModConfig.cats.creepersFearCats || !(event.getEntity() instanceof EntityCreeper)) {
            return;
        }
        EntityCreeper creeper = (EntityCreeper) event.getEntity();
        for (EntityAITasks.EntityAITaskEntry entry : creeper.tasks.taskEntries) {
            if (entry.action instanceof AvoidCats) {
                return;
            }
        }
        creeper.tasks.addTask(3, new AvoidCats(creeper));
    }

    /** The creeper's ocelot goal, pointed at cats; a class of its own so it is added only once. */
    static final class AvoidCats extends EntityAIAvoidEntity<EntityCat> {
        AvoidCats(EntityCreeper creeper) {
            super(creeper, EntityCat.class, 6.0F, 1.0D, 1.2D);
        }
    }

    @SubscribeEvent
    public void onLivingUpdate(LivingEvent.LivingUpdateEvent event) {
        if (!(event.getEntityLiving() instanceof EntityLiving) || event.getEntityLiving().world.isRemote
                || !ModConfig.cats.phantomsFearCats) {
            return;
        }
        EntityLiving mob = (EntityLiving) event.getEntityLiving();
        if (mob.ticksExisted % 10 != 0 || mob.getAttackTarget() == null || !isPhantom(mob)) {
            return;
        }
        List<EntityCat> cats = mob.world.getEntitiesWithinAABB(EntityCat.class, mob.getEntityBoundingBox().grow(PHANTOM_RANGE));
        if (cats.isEmpty()) {
            return;
        }
        for (EntityCat cat : cats) {
            cat.hiss();
        }
        mob.setAttackTarget(null);
        mob.motionY = Math.max(mob.motionY, 0.3D);
        mob.velocityChanged = true;
    }

    static boolean isPhantom(EntityLiving mob) {
        ResourceLocation id = EntityList.getKey(mob);
        return id != null && "phantom".equals(id.getPath());
    }

    /**
     * 1.14's {@code ChestBlock.isCatSittingOnChest}: a sitting cat on top keeps the lid shut. Checked
     * for both halves of a double chest, as 1.14 does.
     */
    @SubscribeEvent
    public void onChestOpen(PlayerInteractEvent.RightClickBlock event) {
        // A sneaking player is not opening the chest (they are placing on it), as in vanilla.
        if (event.getEntityPlayer().isSneaking()
                || !(event.getWorld().getBlockState(event.getPos()).getBlock() instanceof BlockChest)) {
            return;
        }
        if (catSittingOn(event, event.getPos())) {
            return;
        }
        for (net.minecraft.util.EnumFacing side : net.minecraft.util.EnumFacing.Plane.HORIZONTAL) {
            BlockPos other = event.getPos().offset(side);
            if (event.getWorld().getBlockState(other).getBlock() == event.getWorld().getBlockState(event.getPos()).getBlock()
                    && catSittingOn(event, other)) {
                return;
            }
        }
    }

    private static boolean catSittingOn(PlayerInteractEvent.RightClickBlock event, BlockPos pos) {
        AxisAlignedBB above = new AxisAlignedBB(pos.getX(), pos.getY() + 1, pos.getZ(), pos.getX() + 1, pos.getY() + 2, pos.getZ() + 1);
        for (EntityCat cat : event.getWorld().getEntitiesWithinAABB(EntityCat.class, above)) {
            if (cat.isSitting()) {
                // Handled, as 1.14's chest is: the click is spent, nothing opens, nothing is placed.
                event.setCanceled(true);
                event.setCancellationResult(net.minecraft.util.EnumActionResult.SUCCESS);
                return true;
            }
        }
        return false;
    }
}
