package com.exiledradio.villagerbackport.crossbow;

import com.google.gson.JsonObject;

import net.minecraftforge.common.crafting.IConditionFactory;
import net.minecraftforge.common.crafting.JsonContext;

import java.util.function.BooleanSupplier;

/**
 * {@code villagerbackport:crossbow}: true when this mod adds the crossbow. Its recipe and the four
 * crossbow advancements carry it, so they are only loaded alongside the item.
 *
 * <p>Named in {@code _factories.json} files rather than registered from code: Forge clears every
 * condition before it loads recipes, and again before advancements, and reads back only those named
 * in that folder's own factory file - so it is named in both.
 */
public final class CrossbowCondition implements IConditionFactory {

    @Override
    public BooleanSupplier parse(JsonContext context, JsonObject json) {
        return CrossbowContent::enabled;
    }
}
