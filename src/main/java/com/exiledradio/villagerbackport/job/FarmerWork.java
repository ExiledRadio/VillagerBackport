package com.exiledradio.villagerbackport.job;

import com.exiledradio.villagerbackport.ModConfig;
import com.exiledradio.villagerbackport.block.BlockComposter;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.init.Items;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * What a farmer does at its composter: bakes bread and composts the seeds it has no use for.
 *
 * <p>Current Java's {@code WorkAtComposter}, which runs each time a farmer uses its workstation -
 * here, each time it arrives at the composter during work hours, no more than once every fifteen
 * seconds, which is the same cadence. 1.14 baked bread on restock instead; 20w13a (1.16) moved it to
 * the composter and added the composting, and the wiki describes the newer behaviour, so that is the
 * one followed.
 *
 * <h2>Bread</h2>
 * Three wheat make one bread, at most three loaves a visit, and none once the farmer carries more
 * than 36. Bread is the food worth most for breeding (see {@code VillagerFood}), and farmers share
 * it, so this is what keeps a village growing - without it wheat only piles up.
 *
 * <h2>Composting</h2>
 * A farmer keeps ten of each kind of seed for replanting and composts the rest, up to twenty items a
 * visit, stopping once the composter is full. A full composter is emptied first, the bone meal
 * landing on top of it for anyone to take. Current Java stops at level 7 and lets the composter
 * ripen on its own; this mod's composter has no ripening step - its last item takes it straight to
 * ready - so the farmer fills it to ready and collects next visit.
 */
public final class FarmerWork {

    /** Current Java's {@code WorkAtComposter.COMPOSTABLE_ITEMS}. */
    private static final Item[] SEEDS = {Items.WHEAT_SEEDS, Items.BEETROOT_SEEDS};

    /** Seeds of each kind kept back for planting. */
    private static final int KEEP = 10;

    /** Most items composted in one visit. */
    private static final int PER_VISIT = 20;

    /** No more baking once a farmer carries this much bread. */
    private static final int BREAD_CAP = 36;

    /** Most loaves baked in one visit. */
    private static final int LOAVES_PER_VISIT = 3;

    private FarmerWork() {
    }

    /** Called when a farmer uses its workstation. Does nothing unless that is a composter. */
    public static void atWorkstation(EntityVillager villager, BlockPos site) {
        if (villager.world.isRemote || !FarmWork.isFarmer(villager)) {
            return;
        }

        IBlockState state = villager.world.getBlockState(site);
        if (!(state.getBlock() instanceof BlockComposter)) {
            return;
        }

        if (ModConfig.jobs.farmersBakeBread) {
            bakeBread(villager);
        }
        if (ModConfig.jobs.farmersCompost) {
            compost(villager.world, site, villager.getVillagerInventory());
        }
    }

    static void bakeBread(EntityVillager villager) {
        InventoryBasic inventory = villager.getVillagerInventory();
        if (count(inventory, Items.BREAD) > BREAD_CAP) {
            return;
        }

        int loaves = Math.min(LOAVES_PER_VISIT, count(inventory, Items.WHEAT) / 3);
        if (loaves == 0) {
            return;
        }

        remove(inventory, Items.WHEAT, loaves * 3);
        ItemStack left = inventory.addItem(new ItemStack(Items.BREAD, loaves));
        if (!left.isEmpty()) {
            villager.entityDropItem(left, 0.5F);
        }
    }

    private static void compost(World world, BlockPos pos, InventoryBasic inventory) {
        if (BlockComposter.isReady(world, pos)) {
            BlockComposter.takeBoneMeal(world, pos);
        }

        int budget = PER_VISIT;
        int[] seen = new int[SEEDS.length];
        boolean any = false;

        // Last slot first, as current Java does, so the seeds kept are the ones nearest the front.
        for (int slot = inventory.getSizeInventory() - 1; slot >= 0 && budget > 0; slot--) {
            ItemStack stack = inventory.getStackInSlot(slot);
            int kind = indexOf(stack.getItem());
            if (stack.isEmpty() || kind < 0) {
                continue;
            }

            seen[kind] += stack.getCount();
            int spare = Math.min(Math.min(seen[kind] - KEEP, budget), stack.getCount());
            for (int i = 0; i < spare; i++) {
                if (BlockComposter.isReady(world, pos)) {
                    break;
                }
                BlockComposter.compostOne(world, pos, stack, false);
                stack.shrink(1);
                budget--;
                any = true;
            }
            if (stack.isEmpty()) {
                inventory.setInventorySlotContents(slot, ItemStack.EMPTY);
            }
            if (BlockComposter.isReady(world, pos)) {
                break;
            }
        }

        if (any) {
            BlockComposter.playFill(world, pos);
        }
    }

    private static int indexOf(Item item) {
        for (int i = 0; i < SEEDS.length; i++) {
            if (SEEDS[i] == item) {
                return i;
            }
        }
        return -1;
    }

    private static int count(InventoryBasic inventory, Item item) {
        int total = 0;
        for (int i = 0; i < inventory.getSizeInventory(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (stack.getItem() == item) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private static void remove(InventoryBasic inventory, Item item, int amount) {
        for (int i = 0; i < inventory.getSizeInventory() && amount > 0; i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (stack.getItem() == item) {
                int take = Math.min(amount, stack.getCount());
                stack.shrink(take);
                amount -= take;
                if (stack.isEmpty()) {
                    inventory.setInventorySlotContents(i, ItemStack.EMPTY);
                }
            }
        }
    }
}
