package com.exiledradio.villagerbackport.cat;

import com.google.gson.JsonObject;

import net.minecraftforge.common.crafting.IConditionFactory;
import net.minecraftforge.common.crafting.JsonContext;

import java.util.function.BooleanSupplier;

/**
 * {@code villagerbackport:cats} - true when this mod adds cats. Keeps <i>A Complete Catalogue</i> out
 * of the advancement screen when there are no cats to tame.
 */
public final class CatCondition implements IConditionFactory {

    @Override
    public BooleanSupplier parse(JsonContext context, JsonObject json) {
        return CatContent::enabled;
    }
}
