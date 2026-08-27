package com.exiledradio.villagerbackport.block;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;

import java.util.Locale;

/**
 * A furnace that is faster but pickier.
 *
 * <h2>Built on vanilla's furnace</h2>
 * Inventory, fuel handling, hopper wiring, experience accounting and saving are all identical to a
 * furnace, so this extends {@link TileEntityFurnace} and inherits every bit of it.
 *
 * <h2>Except the tick, which has to be rewritten</h2>
 * Vanilla's {@code update()} cannot be reused, and not for stylistic reasons. When the burn state
 * changes it calls {@code BlockFurnace.setState}, which reads {@code BlockFurnace.FACING} off
 * whatever block is at the position and then <em>replaces that block with a vanilla furnace</em>. On
 * a smoker the property read alone throws - a block property is identified by object, and this block
 * carries its own - and had it succeeded, the block would have turned into a furnace the moment it
 * lit.
 *
 * <p>So the loop is written out here. It reads and writes the same four values through
 * {@code getField} and {@code setField} rather than reaching for private fields, so what is
 * duplicated is only the sequencing, and the lit state goes onto a property of this block instead of
 * swapping the block for a different one.
 *
 * <h2>Where the recipe restriction is enforced</h2>
 * On the way in, not on the way through: the screen's input slot via {@link ContainerFurnaceVariant},
 * and hoppers via {@link #isItemValidForSlot}. Between them nothing disallowed can get in, so the
 * loop below does not need to test it again.
 */
public abstract class TileEntityFurnaceVariant extends TileEntityFurnace {

    /** Field ids as vanilla numbers them, named so the loop reads as something. */
    private static final int BURN_TIME = 0;
    private static final int CURRENT_ITEM_BURN_TIME = 1;
    private static final int COOK_TIME = 2;
    private static final int TOTAL_COOK_TIME = 3;

    private static final int INPUT = 0;
    private static final int FUEL = 1;
    private static final int OUTPUT = 2;

    public abstract FurnaceKind kind();

    /** Shown on the screen, and the fallback when this mod's language file is not loaded. */
    protected abstract String plainName();

    /** Half a furnace's time - the reason to build one. */
    @Override
    public int getCookTime(ItemStack stack) {
        return FurnaceKind.COOK_TIME;
    }

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack) {
        if (index == INPUT) {
            return kind().accepts(stack) && super.isItemValidForSlot(index, stack);
        }
        return super.isItemValidForSlot(index, stack);
    }

    // ------------------------------------------------------------------- tick

    @Override
    public void update() {
        boolean wasBurning = isBurning();
        boolean changed = false;

        if (isBurning()) {
            setField(BURN_TIME, getField(BURN_TIME) - 1);
        }

        if (!this.world.isRemote) {
            ItemStack fuel = getStackInSlot(FUEL);

            if (isBurning() || (!fuel.isEmpty() && !getStackInSlot(INPUT).isEmpty())) {
                changed |= tryLight(fuel);
                changed |= cook();
            } else if (getField(COOK_TIME) > 0) {
                // Progress unwinds when the fire goes out rather than being lost outright, as it does
                // in vanilla, so briefly running dry costs some of the cook and not all of it.
                setField(COOK_TIME,
                        MathHelper.clamp(getField(COOK_TIME) - 2, 0, getField(TOTAL_COOK_TIME)));
            }

            if (wasBurning != isBurning()) {
                changed = true;
                setLit(isBurning());
            }
        }

        if (changed) {
            markDirty();
        }
    }

    /** Burns a piece of fuel, but only if there is something worth burning it for. */
    private boolean tryLight(ItemStack fuel) {
        if (isBurning() || !canSmelt()) {
            return false;
        }

        int burnTime = getItemBurnTime(fuel);
        if (burnTime <= 0) {
            return false;
        }

        setField(BURN_TIME, burnTime);
        setField(CURRENT_ITEM_BURN_TIME, burnTime);

        Item item = fuel.getItem();
        fuel.shrink(1);

        // Buckets and the like leave something behind rather than vanishing.
        if (fuel.isEmpty()) {
            setInventorySlotContents(FUEL, item.getContainerItem(fuel));
        }

        return true;
    }

    /** Advances the cook, and finishes it when the time is up. */
    private boolean cook() {
        if (!isBurning() || !canSmelt()) {
            setField(COOK_TIME, 0);
            return false;
        }

        setField(COOK_TIME, getField(COOK_TIME) + 1);

        if (getField(COOK_TIME) < getField(TOTAL_COOK_TIME)) {
            return false;
        }

        setField(COOK_TIME, 0);
        setField(TOTAL_COOK_TIME, getCookTime(getStackInSlot(INPUT)));
        smelt();
        return true;
    }

    /**
     * @return true if the input has a recipe and the result will fit
     *
     * <p>Vanilla's is private, so this is the same test written out: a recipe exists, and the output
     * slot is either empty or already holds the same thing with room to spare.
     */
    private boolean canSmelt() {
        ItemStack input = getStackInSlot(INPUT);
        if (input.isEmpty()) {
            return false;
        }

        ItemStack result = FurnaceRecipes.instance().getSmeltingResult(input);
        if (result.isEmpty()) {
            return false;
        }

        ItemStack output = getStackInSlot(OUTPUT);
        if (output.isEmpty()) {
            return true;
        }

        if (!output.isItemEqual(result)) {
            return false;
        }

        int total = output.getCount() + result.getCount();
        return total <= getInventoryStackLimit() && total <= output.getMaxStackSize();
    }

    private void smelt() {
        if (!canSmelt()) {
            return;
        }

        ItemStack input = getStackInSlot(INPUT);
        ItemStack result = FurnaceRecipes.instance().getSmeltingResult(input);
        ItemStack output = getStackInSlot(OUTPUT);

        if (output.isEmpty()) {
            setInventorySlotContents(OUTPUT, result.copy());
        } else if (output.isItemEqual(result)) {
            output.grow(result.getCount());
        }

        input.shrink(1);
    }

    /** Writes the burn state onto the block, which is what vanilla swaps whole blocks to achieve. */
    private void setLit(boolean lit) {
        IBlockState state = this.world.getBlockState(this.pos);

        if (state.getBlock() instanceof BlockFurnaceVariant
                && state.getValue(BlockFurnaceVariant.LIT) != lit) {
            this.world.setBlockState(this.pos, state.withProperty(BlockFurnaceVariant.LIT, lit), 3);
        }
    }

    /**
     * Keeps this tile entity alive when only the lit flag changes.
     *
     * <p>Without it, lighting the fire would discard the contents and the burn progress with them -
     * the same trap the barrel hit when its lid opened.
     */
    @Override
    public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newState) {
        return oldState.getBlock() != newState.getBlock();
    }

    // ------------------------------------------------------------------ names

    @Override
    public Container createContainer(InventoryPlayer playerInventory, EntityPlayer player) {
        return new ContainerFurnaceVariant(playerInventory, this);
    }

    @Override
    public String getName() {
        return hasCustomName() ? super.getName() : nameKey();
    }

    @Override
    public ITextComponent getDisplayName() {
        return new TextComponentString(
                hasCustomName() ? super.getName() : Names.translateOr(nameKey(), plainName()));
    }

    private String nameKey() {
        return "container.villagerbackport."
                + plainName().toLowerCase(Locale.ENGLISH).replace(' ', '_');
    }

    /** Cooks food at double speed. */
    public static class Smoker extends TileEntityFurnaceVariant {
        @Override
        public FurnaceKind kind() {
            return FurnaceKind.SMOKER;
        }

        @Override
        protected String plainName() {
            return "Smoker";
        }
    }

    /** Smelts ores at double speed. */
    public static class Blast extends TileEntityFurnaceVariant {
        @Override
        public FurnaceKind kind() {
            return FurnaceKind.BLAST_FURNACE;
        }

        @Override
        protected String plainName() {
            return "Blast Furnace";
        }
    }
}
