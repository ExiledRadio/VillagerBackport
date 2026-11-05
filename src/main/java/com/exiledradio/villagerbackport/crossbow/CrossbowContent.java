package com.exiledradio.villagerbackport.crossbow;

import com.exiledradio.villagerbackport.ModConfig;
import com.exiledradio.villagerbackport.ModCreativeTab;
import com.exiledradio.villagerbackport.VillagerBackport;

import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnumEnchantmentType;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.util.EnumHelper;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityEntryBuilder;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;

/**
 * The player's crossbow: the item, its three enchantments and the projectiles it fires.
 *
 * <h2>Stepping aside</h2>
 * Future MC and Crossbows Backport each add a crossbow of their own. With either installed this one
 * is not registered at all (unless {@code compat.crossbow} says otherwise), so a pack never has two:
 * everything here is then null, and pillagers, outposts and fletchers use theirs. The recipe and the
 * advancements carry a {@link CrossbowCondition} so they go with it.
 */
@Mod.EventBusSubscriber(modid = VillagerBackport.MOD_ID)
public final class CrossbowContent {

    public enum Mode {
        AUTO, OURS, OFF
    }

    /** Network ids within this mod; the pillager and ravager have 100 and 101. */
    private static final int ARROW_ID = 102;
    private static final int SPECTRAL_ARROW_ID = 103;
    private static final int FIREWORK_ID = 104;

    @Nullable
    public static ItemCrossbow crossbow;
    @Nullable
    public static Enchantment quickCharge;
    @Nullable
    public static Enchantment multishot;
    @Nullable
    public static Enchantment piercing;

    private CrossbowContent() {
    }

    /** @return true if this mod adds the crossbow in this game - fixed for the whole session */
    public static boolean enabled() {
        switch (ModConfig.compat.crossbow) {
            case OURS:
                return true;
            case OFF:
                return false;
            default:
                return !Loader.isModLoaded("futuremc") && !Loader.isModLoaded("crossbows");
        }
    }

    /** @return the level of one of this mod's crossbow enchantments on a stack, 0 when it has none */
    public static int level(@Nullable Enchantment enchantment, net.minecraft.item.ItemStack stack) {
        return enchantment == null ? 0 : net.minecraft.enchantment.EnchantmentHelper.getEnchantmentLevel(enchantment, stack);
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        if (!enabled()) {
            VillagerBackport.LOGGER.info("Another mod's crossbow is installed; this mod adds none (compat.crossbow).");
            return;
        }
        crossbow = new ItemCrossbow();
        crossbow.setRegistryName(new ResourceLocation(VillagerBackport.MOD_ID, "crossbow"));
        crossbow.setTranslationKey(VillagerBackport.MOD_ID + ".crossbow");
        crossbow.setCreativeTab(ModCreativeTab.TAB);
        event.getRegistry().register(crossbow);
    }

    @SubscribeEvent
    public static void registerEnchantments(RegistryEvent.Register<Enchantment> event) {
        if (!enabled()) {
            return;
        }
        EnumEnchantmentType type = EnumHelper.addEnchantmentType("VILLAGERBACKPORT_CROSSBOW",
                item -> item instanceof ItemCrossbow);
        // Enchanted books of the three show in this mod's tab, as Combat shows the bow's.
        ModCreativeTab.TAB.setRelevantEnchantmentTypes(type);

        // Current Java's numbers.
        quickCharge = new CrossbowEnchantment("quick_charge", Enchantment.Rarity.UNCOMMON, type, 3, 12, 20);
        multishot = new CrossbowEnchantment("multishot", Enchantment.Rarity.RARE, type, 1, 20, 0);
        piercing = new CrossbowEnchantment("piercing", Enchantment.Rarity.COMMON, type, 4, 1, 10);
        event.getRegistry().registerAll(quickCharge, multishot, piercing);
    }

    @SubscribeEvent
    public static void registerEntities(RegistryEvent.Register<EntityEntry> event) {
        if (!enabled()) {
            return;
        }
        event.getRegistry().registerAll(
                EntityEntryBuilder.create().entity(EntityCrossbowArrow.class)
                        .id(new ResourceLocation(VillagerBackport.MOD_ID, "crossbow_arrow"), ARROW_ID)
                        .name(VillagerBackport.MOD_ID + ".crossbow_arrow").tracker(64, 20, true).build(),
                EntityEntryBuilder.create().entity(EntityCrossbowSpectralArrow.class)
                        .id(new ResourceLocation(VillagerBackport.MOD_ID, "crossbow_spectral_arrow"), SPECTRAL_ARROW_ID)
                        .name(VillagerBackport.MOD_ID + ".crossbow_spectral_arrow").tracker(64, 20, true).build(),
                EntityEntryBuilder.create().entity(EntityCrossbowFirework.class)
                        .id(new ResourceLocation(VillagerBackport.MOD_ID, "crossbow_firework"), FIREWORK_ID)
                        .name(VillagerBackport.MOD_ID + ".crossbow_firework").tracker(64, 10, true).build());
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public static void registerModels(ModelRegistryEvent event) {
        if (crossbow != null) {
            ModelLoader.setCustomModelResourceLocation(crossbow, 0,
                    new ModelResourceLocation(crossbow.getRegistryName(), "inventory"));
        }
    }
}
