package com.exiledradio.villagerbackport.campfire;

import com.google.gson.JsonObject;
import net.minecraftforge.common.crafting.IConditionFactory;
import net.minecraftforge.common.crafting.JsonContext;

import java.util.function.BooleanSupplier;

/**
 * {@code villagerbackport:campfire} in a recipe's conditions: the recipe exists only while this mod
 * adds the campfire. Without it a disabled campfire would leave a recipe pointing at an item that was
 * never registered, which Forge reports as an error on every start.
 */
public final class CampfireCondition implements IConditionFactory {

    @Override
    public BooleanSupplier parse(JsonContext context, JsonObject json) {
        return CampfireContent::enabled;
    }
}
