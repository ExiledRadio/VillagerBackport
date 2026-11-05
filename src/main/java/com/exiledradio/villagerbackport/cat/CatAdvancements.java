package com.exiledradio.villagerbackport.cat;

import com.exiledradio.villagerbackport.VillagerBackport;

import net.minecraft.advancements.Advancement;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ResourceLocation;

/**
 * <i>A Complete Catalogue</i>: tame a cat of every coat.
 *
 * <p>1.14 does it with a {@code catType} condition on the tame trigger, which 1.12.2's trigger has no
 * room for. So, as with the crossbow's advancements, the advancement is this mod's own, with one
 * impossible criterion per coat named as {@link EntityCat#TYPES} names them, granted from code when a
 * cat is tamed. Cats that came from old tamed ocelots do not count - 1.14's did not either.
 *
 * <p>It sits under vanilla's <i>Best Friends Forever</i>, as 1.14's does, and only exists with
 * {@code cats.enabled} on ({@link CatCondition}).
 */
public final class CatAdvancements {

    public static final ResourceLocation COMPLETE_CATALOGUE = new ResourceLocation(VillagerBackport.MOD_ID, "husbandry/complete_catalogue");

    private CatAdvancements() {
    }

    static void tamed(EntityPlayerMP player, EntityCat cat) {
        Advancement advancement = player.getServer().getAdvancementManager().getAdvancement(COMPLETE_CATALOGUE);
        if (advancement != null) {
            player.getAdvancements().grantCriterion(advancement, cat.getTypeName());
        }
    }
}
