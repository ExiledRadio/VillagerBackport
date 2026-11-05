package com.exiledradio.villagerbackport.raid;

import com.exiledradio.villagerbackport.VillagerBackport;

import net.minecraft.client.gui.Gui;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Bad Omen, Raid Omen and Hero of the Village.
 *
 * <p>Registered whatever raid system is in use, so a world that has had them never finds an effect
 * missing. Bad Omen from other mods counts too - Deeper Depths and Raids Backport each register one -
 * so an omen picked up from any of them starts this mod's raids.
 */
@Mod.EventBusSubscriber(modid = VillagerBackport.MOD_ID)
public final class ModEffects {

    /** Current Java's colours for the three. */
    private static final int BAD_OMEN_COLOUR = 0x0B6138;
    private static final int RAID_OMEN_COLOUR = 0xDE4058;
    private static final int HERO_COLOUR = 0x44FF44;

    /** Other mods' Bad Omen, which this mod's raids accept as their own. */
    private static final ResourceLocation[] OTHER_BAD_OMENS = {
            new ResourceLocation("deeperdepths", "bad_omen"),
            new ResourceLocation("raids", "bad_omen")
    };

    public static Potion badOmen;
    public static Potion raidOmen;
    public static Potion heroOfTheVillage;

    @Nullable
    private static List<Potion> badOmens;

    private ModEffects() {
    }

    @SubscribeEvent
    public static void register(RegistryEvent.Register<Potion> event) {
        badOmen = new Effect("bad_omen", BAD_OMEN_COLOUR);
        raidOmen = new Effect("raid_omen", RAID_OMEN_COLOUR);
        heroOfTheVillage = new Effect("hero_of_the_village", HERO_COLOUR);
        event.getRegistry().registerAll(badOmen, raidOmen, heroOfTheVillage);
    }

    /** @return the Bad Omen this entity has, from whichever mod, or null */
    @Nullable
    public static PotionEffect badOmenOn(EntityLivingBase entity) {
        for (Potion omen : badOmens()) {
            PotionEffect effect = entity.getActivePotionEffect(omen);
            if (effect != null) {
                return effect;
            }
        }
        return null;
    }

    /** Takes every mod's Bad Omen off: it has been turned into a raid. */
    public static void clearBadOmen(EntityLivingBase entity) {
        for (Potion omen : badOmens()) {
            entity.removePotionEffect(omen);
        }
    }

    public static boolean isBadOmen(Potion potion) {
        return badOmens().contains(potion);
    }

    private static List<Potion> badOmens() {
        if (badOmens == null) {
            List<Potion> found = new ArrayList<Potion>();
            found.add(badOmen);
            for (ResourceLocation name : OTHER_BAD_OMENS) {
                Potion potion = ForgeRegistries.POTIONS.getValue(name);
                if (potion != null) {
                    found.add(potion);
                }
            }
            badOmens = found;
        }
        return badOmens;
    }

    /**
     * An effect drawn from its own icon, as 1.14 draws every effect, rather than from vanilla's
     * shared sheet of them, which has no room for more.
     */
    static final class Effect extends Potion {

        private final ResourceLocation icon;

        Effect(String name, int colour) {
            // Neutral, as 1.14 has all three: none of them is a harm to be cured.
            super(false, colour);
            setPotionName("effect." + VillagerBackport.MOD_ID + "." + name);
            setRegistryName(new ResourceLocation(VillagerBackport.MOD_ID, name));
            this.icon = new ResourceLocation(VillagerBackport.MOD_ID, "textures/mob_effect/" + name + ".png");
        }

        /** Drawing on the client only; the server never loads these. */
        @Override
        @SideOnly(Side.CLIENT)
        public boolean hasStatusIcon() {
            return false;
        }

        @Override
        @SideOnly(Side.CLIENT)
        public void renderInventoryEffect(PotionEffect effect, Gui gui, int x, int y, float z) {
            com.exiledradio.villagerbackport.client.EffectIcons.draw(this.icon, x + 6, y + 7, 1.0F);
        }

        @Override
        @SideOnly(Side.CLIENT)
        public void renderHUDEffect(PotionEffect effect, Gui gui, int x, int y, float z, float alpha) {
            com.exiledradio.villagerbackport.client.EffectIcons.draw(this.icon, x + 3, y + 3, alpha);
        }
    }
}
