package com.exiledradio.villagerbackport.job;

import com.exiledradio.villagerbackport.ModConfig;
import com.exiledradio.villagerbackport.block.CropSounds;
import com.exiledradio.villagerbackport.restock.RestockRules;

import net.minecraft.block.Block;
import net.minecraft.block.BlockCrops;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.ai.EntityAIMoveToBlock;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.EnumPlantType;
import net.minecraftforge.common.IPlantable;
import net.minecraftforge.event.ForgeEventFactory;

import javax.annotation.Nullable;

/**
 * A farmer tending its fields: harvesting ripe crops and replanting empty farmland.
 *
 * <p>Takes the place of 1.12.2's {@code EntityAIHarvestFarmland}, keeping its walking and its
 * planting, and changing when and where it farms to 1.14's rules:
 * <ul>
 * <li><b>During work hours only.</b> 1.14 farms as part of the farmer's working day; 1.12.2 farmed at
 * any hour, all night included.</li>
 * <li><b>Around its composter.</b> 1.14 farms the farmland near the farmer's job site - it strolls
 * between farmland within four blocks of itself while within six of the composter, so roughly ten
 * blocks of it. 1.12.2 farmed any field within sixteen blocks of wherever it stood, which in a large
 * village meant everyone's fields. The reach is {@code jobs.farmRadius}.</li>
 * <li><b>Harvests while it has room.</b> 1.14 harvests whenever the farmer has an empty slot or a
 * seed stack to add to. 1.12.2 stopped harvesting once a farmer had "enough" food, and never started
 * again until it gave some away.</li>
 * <li><b>Looks more often.</b> 1.12.2 searched for work every ten to twenty seconds; 1.14.3 made
 * farmers "spend more time farming", and this searches every two to four.</li>
 * </ul>
 * A farmer whose career has no workstation registered still farms, at any hour, wherever it is - the
 * same fail-open rule as the rest of the job system. With {@code jobs.modernFarming} off it farms as
 * 1.12.2's did.
 *
 * <p>Plants with the planting sound 1.14's farmers make. Harvesting needs nothing extra: crops are
 * broken with {@code destroyBlock}, which plays the crop's own break sound.
 */
public class EntityAIFarm extends EntityAIMoveToBlock {

    private static final int SEARCH = 16;

    private static final int HARVEST = 0;
    private static final int PLANT = 1;
    private static final int NONE = -1;

    private final EntityVillager villager;

    private boolean hasSeeds;
    private boolean hasRoom;
    private int task = NONE;

    /** The composter this farmer works from, or null to farm anywhere. */
    @Nullable
    private BlockPos site;

    public EntityAIFarm(EntityVillager villager, double speed) {
        super(villager, speed, SEARCH);
        this.villager = villager;
    }

    @Override
    public boolean shouldExecute() {
        if (this.runDelay <= 0) {
            if (!ForgeEventFactory.getMobGriefingEvent(this.villager.world, this.villager)) {
                return false;
            }

            if (ModConfig.jobs.modernFarming) {
                if (!RestockRules.isWorkingHours(this.villager.world)) {
                    return false;
                }
                this.site = JobSite.validated(this.villager);
                if (this.site == null && JobSiteRegistry.hasSite(this.villager)) {
                    // Between composters: nothing to farm around until it claims another.
                    return false;
                }
                this.hasRoom = hasRoom(this.villager.getVillagerInventory());
            } else {
                this.site = null;
                this.hasRoom = this.villager.wantsMoreFood();
            }

            this.task = NONE;
            this.hasSeeds = this.villager.isFarmItemInInventory();
            if (!this.hasSeeds && !this.hasRoom) {
                return false;
            }
        }

        boolean found = super.shouldExecute();

        // 1.12.2 waits ten to twenty seconds after a search that found nothing.
        if (!found && ModConfig.jobs.modernFarming && this.runDelay > 80) {
            this.runDelay = 40 + this.villager.getRNG().nextInt(40);
        }
        return found;
    }

    @Override
    public boolean shouldContinueExecuting() {
        return this.task >= 0 && super.shouldContinueExecuting();
    }

    @Override
    public void updateTask() {
        super.updateTask();
        this.villager.getLookHelper().setLookPosition(this.destinationBlock.getX() + 0.5D,
                this.destinationBlock.getY() + 1, this.destinationBlock.getZ() + 0.5D, 10.0F,
                this.villager.getVerticalFaceSpeed());

        if (!getIsAboveDestination()) {
            return;
        }

        World world = this.villager.world;
        BlockPos crop = this.destinationBlock.up();
        IBlockState state = world.getBlockState(crop);
        Block block = state.getBlock();

        if (this.task == HARVEST && block instanceof BlockCrops && ((BlockCrops) block).isMaxAge(state)) {
            world.destroyBlock(crop, true);
        } else if (this.task == PLANT && state.getMaterial() == Material.AIR) {
            if (plant(world, crop)) {
                CropSounds.playVillagerPlant(world, crop);
            }
        }

        this.task = NONE;
        this.runDelay = 10;
    }

    /** Plants the first thing in its inventory that grows here, in 1.12.2's order of preference. */
    private boolean plant(World world, BlockPos pos) {
        InventoryBasic inventory = this.villager.getVillagerInventory();

        for (int i = 0; i < inventory.getSizeInventory(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (stack.isEmpty()) {
                continue;
            }

            IBlockState plant = plantFor(stack.getItem(), world, pos);
            if (plant != null) {
                world.setBlockState(pos, plant, 3);
                stack.shrink(1);
                if (stack.isEmpty()) {
                    inventory.setInventorySlotContents(i, ItemStack.EMPTY);
                }
                return true;
            }
        }
        return false;
    }

    @Nullable
    private static IBlockState plantFor(Item item, World world, BlockPos pos) {
        if (item == Items.WHEAT_SEEDS) {
            return Blocks.WHEAT.getDefaultState();
        }
        if (item == Items.POTATO) {
            return Blocks.POTATOES.getDefaultState();
        }
        if (item == Items.CARROT) {
            return Blocks.CARROTS.getDefaultState();
        }
        if (item == Items.BEETROOT_SEEDS) {
            return Blocks.BEETROOTS.getDefaultState();
        }
        // Other mods' seeds, as Forge's patch to 1.12.2's goal allows.
        if (item instanceof IPlantable && ((IPlantable) item).getPlantType(world, pos) == EnumPlantType.Crop) {
            return ((IPlantable) item).getPlant(world, pos);
        }
        return null;
    }

    @Override
    protected boolean shouldMoveTo(World world, BlockPos pos) {
        if (world.getBlockState(pos).getBlock() != Blocks.FARMLAND || !inReach(pos)) {
            return false;
        }

        IBlockState above = world.getBlockState(pos.up());
        Block block = above.getBlock();

        if (block instanceof BlockCrops && ((BlockCrops) block).isMaxAge(above) && this.hasRoom
                && (this.task == HARVEST || this.task < 0)) {
            this.task = HARVEST;
            return true;
        }

        if (above.getMaterial() == Material.AIR && this.hasSeeds && (this.task == PLANT || this.task < 0)) {
            this.task = PLANT;
            return true;
        }

        return false;
    }

    private boolean inReach(BlockPos farmland) {
        if (this.site == null) {
            return true;
        }
        int radius = ModConfig.jobs.farmRadius;
        return Math.abs(farmland.getX() - this.site.getX()) <= radius
                && Math.abs(farmland.getZ() - this.site.getZ()) <= radius
                && Math.abs(farmland.getY() - this.site.getY()) <= 4;
    }

    /** 1.14's test: an empty slot, or a seed stack the harvest's seeds can join. */
    private static boolean hasRoom(InventoryBasic inventory) {
        for (int i = 0; i < inventory.getSizeInventory(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (stack.isEmpty() || stack.getItem() == Items.WHEAT_SEEDS || stack.getItem() == Items.BEETROOT_SEEDS) {
                return true;
            }
        }
        return false;
    }
}
