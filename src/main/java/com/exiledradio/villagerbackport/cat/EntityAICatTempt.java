package com.exiledradio.villagerbackport.cat;

import net.minecraft.entity.ai.EntityAITempt;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import java.util.Collections;

/**
 * A stray cat creeping after a player who holds raw cod or salmon. 1.14's {@code CatTemptGoal}.
 *
 * <p>Only strays: a tamed cat follows its owner anyway. 1.12.2's tempt goal matches items, not
 * metadata, so {@link #isTempting} is overridden to turn away clownfish and pufferfish, which share
 * the fish item with cod and salmon.
 *
 * <p>1.14 also lets a stray settle on one player for a while and stop being startled by them; 1.12.2's
 * goal keeps its "scared by movement" flag private and final, so a cat here stays as skittish as an
 * ocelot was. A small difference no one is likely to notice.
 */
public class EntityAICatTempt extends EntityAITempt {

    private final EntityCat cat;

    public EntityAICatTempt(EntityCat cat, double speed, boolean scared) {
        super(cat, speed, scared, Collections.singleton(Items.FISH));
        this.cat = cat;
    }

    @Override
    protected boolean isTempting(ItemStack stack) {
        return CatContent.isFish(stack);
    }

    @Override
    public boolean shouldExecute() {
        return !this.cat.isTamed() && super.shouldExecute();
    }
}
