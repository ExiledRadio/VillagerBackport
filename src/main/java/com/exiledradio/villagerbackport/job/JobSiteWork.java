package com.exiledradio.villagerbackport.job;

import com.exiledradio.villagerbackport.ModConfig;
import com.exiledradio.villagerbackport.compat.VillagerAccess;

import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.village.MerchantRecipe;
import net.minecraft.village.MerchantRecipeList;

/**
 * Whether a villager may restock, and whether it has reason to go to work.
 *
 * <h2>The gate</h2>
 * In 1.14 restocking happens inside the task that walks a villager to its workstation, so reaching
 * the workstation is a precondition rather than a check - a villager that cannot get to its lectern
 * simply never runs the code that refills its trades. This applies the same rule explicitly.
 *
 * <h2>Why it fails open</h2>
 * A career with no workstation registered is not gated. Most of 1.14's workstations are blocks this
 * mod has not added yet, and gating a librarian on a lectern that does not exist would leave it
 * permanently sold out - which is precisely the bug this mod was written to fix. As blocks are
 * added and mapped, their careers become gated on their own; nothing here needs changing.
 *
 * <p>The same reasoning covers a villager whose workstation was destroyed: it reverts to restocking
 * freely until it finds another, rather than being stranded by a broken block.
 */
public final class JobSiteWork {

    private JobSiteWork() {
    }

    /**
     * @return true if the villager is allowed to restock right now
     */
    public static boolean canRestock(EntityVillager villager) {
        if (!ModConfig.jobs.enabled || !ModConfig.jobs.requireJobSite) {
            return true;
        }

        // No workstation defined for this career - ungated. See the note above on failing open.
        if (JobSiteRegistry.siteFor(villager) == null) {
            return true;
        }

        // Has a workstation type but no reachable one claimed. Also ungated, deliberately: a village
        // that has not been given the right blocks should trade worse, not stop working entirely.
        if (JobSite.validated(villager) == null) {
            return !ModConfig.jobs.strictJobSite;
        }

        return JobSite.isAtSite(villager);
    }

    /**
     * @return true if the villager has trades worth replenishing.
     *
     * <p>Used to decide whether walking to the workstation is worth doing at all. A villager with
     * nothing sold out has no reason to make the trip, and sending it anyway would have villagers
     * permanently clustered around their workstations rather than going about the village.
     */
    public static boolean needsToWork(EntityVillager villager) {
        MerchantRecipeList recipes = VillagerAccess.getBuyingList(villager);
        if (recipes == null || recipes.isEmpty()) {
            return false;
        }

        for (MerchantRecipe recipe : recipes) {
            if (recipe.isRecipeDisabled()) {
                return true;
            }
        }

        return false;
    }
}
