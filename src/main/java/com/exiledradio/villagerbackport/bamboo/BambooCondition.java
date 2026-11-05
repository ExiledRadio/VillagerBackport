package com.exiledradio.villagerbackport.bamboo;

import com.google.gson.JsonObject;

import net.minecraftforge.common.crafting.IConditionFactory;
import net.minecraftforge.common.crafting.JsonContext;

import java.util.function.BooleanSupplier;

/**
 * {@code villagerbackport:bamboo}: true when this mod adds bamboo and scaffolding
 * ({@code bamboo.enabled}). The stick and scaffolding recipes carry it, so they are only loaded
 * alongside the items they use.
 *
 * <p>Named in {@code recipes/_factories.json}, like {@code CrossbowCondition}, because Forge only
 * reads back the conditions named in that folder's own factory file.
 */
public final class BambooCondition implements IConditionFactory {

    @Override
    public BooleanSupplier parse(JsonContext context, JsonObject json) {
        return BambooContent::enabled;
    }
}
