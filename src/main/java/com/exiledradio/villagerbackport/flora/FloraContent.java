package com.exiledradio.villagerbackport.flora;

import com.exiledradio.villagerbackport.ModConfig;
import com.exiledradio.villagerbackport.ModCreativeTab;
import com.exiledradio.villagerbackport.VillagerBackport;
import com.exiledradio.villagerbackport.item.ItemBannerPattern;

import net.minecraft.block.Block;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.oredict.OreDictionary;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Registers stream F's content: 1.14's three new flowers, the four dyes that stopped being other
 * things, suspicious stew, leather horse armor and the Globe banner pattern.
 *
 * <h2>What lives where</h2>
 * Blocks and items are created here and nowhere else, so everything else in this package can treat
 * a null field as "this is switched off" - which is what {@code flora.enabled} does: with it off
 * none of the registration methods below registers anything, every field stays null, and the
 * handlers that read them (mooshrooms, sign dyeing, wither roses, worldgen) each check
 * {@link #enabled()} before touching a field.
 *
 * <h2>The dyes</h2>
 * 1.14 split white, black, brown and blue dye off from bone meal, ink sacs, cocoa beans and lapis
 * lazuli, which in 1.12.2 <em>are</em> those dyes. This mod adds the four dyes as items of their own
 * and leaves the old four exactly as they were - bone meal still grows things, an ink sac is still
 * black dye to every 1.12.2 recipe - so nothing that worked before stops working.
 *
 * <p>The new dyes join the ore dictionary under {@code dyeWhite}, {@code dyeBlack}, {@code dyeBrown}
 * and {@code dyeBlue}, and under {@code dye}. Forge has already rewritten every vanilla shaped and
 * shapeless recipe that took a dye to take the ore dictionary name instead, and most mods write
 * theirs that way too, so that one line is what makes stained glass, wool, terracotta, concrete
 * powder, beds and other mods' recipes accept them. The handful of vanilla recipes that look for
 * {@code Items.DYE} by hand - leather armor dyeing, firework stars, banner patterns - are covered
 * by {@link ModernDyeRecipe}.
 *
 * <h2>Registry names</h2>
 * 1.14's own, under this mod's domain: {@code villagerbackport:cornflower},
 * {@code villagerbackport:white_dye}, {@code villagerbackport:suspicious_stew} and so on. Changing
 * any of them deletes the item from every world that has it.
 */
@Mod.EventBusSubscriber(modid = VillagerBackport.MOD_ID)
public final class FloraContent {

    public static BlockFloraFlower cornflower;
    public static BlockFloraFlower lilyOfTheValley;
    public static BlockWitherRose witherRose;

    public static BlockFloraPot pottedCornflower;
    public static BlockFloraPot pottedLilyOfTheValley;
    public static BlockFloraPot pottedWitherRose;

    public static ItemModernDye whiteDye;
    public static ItemModernDye blackDye;
    public static ItemModernDye brownDye;
    public static ItemModernDye blueDye;

    public static ItemSuspiciousStew suspiciousStew;
    public static ItemLeatherHorseArmor leatherHorseArmor;
    public static ItemBannerPattern globeBannerPattern;

    public static SoundEvent mooshroomConvert;
    public static SoundEvent mooshroomEat;
    public static SoundEvent mooshroomMilk;
    public static SoundEvent mooshroomSuspiciousMilk;

    /** Every item this class registered, for the client to give models to. */
    private static final List<Item> ITEMS = new ArrayList<Item>();

    /** Every block that is drawn from a model, for the same reason. */
    private static final List<Block> BLOCKS = new ArrayList<Block>();

    private FloraContent() {
    }

    /** @return true if stream F's content is registered in this game */
    public static boolean enabled() {
        return ModConfig.flora.enabled;
    }

    /** @return the items registered here, in registration order. Empty when switched off. */
    public static List<Item> items() {
        return Collections.unmodifiableList(ITEMS);
    }

    /** @return the blocks registered here. Empty when switched off. */
    public static List<Block> blocks() {
        return Collections.unmodifiableList(BLOCKS);
    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        if (!enabled()) {
            return;
        }

        cornflower = block(event, "cornflower", new BlockFloraFlower());
        lilyOfTheValley = block(event, "lily_of_the_valley", new BlockFloraFlower());
        witherRose = block(event, "wither_rose", new BlockWitherRose());

        pottedCornflower = block(event, "potted_cornflower", new BlockFloraPot(cornflower));
        pottedLilyOfTheValley = block(event, "potted_lily_of_the_valley", new BlockFloraPot(lilyOfTheValley));
        pottedWitherRose = block(event, "potted_wither_rose", new BlockFloraPot(witherRose));

        FloraWorldGen.register();
    }

    private static <T extends Block> T block(RegistryEvent.Register<Block> event, String name, T block) {
        block.setRegistryName(new ResourceLocation(VillagerBackport.MOD_ID, name));
        block.setTranslationKey(VillagerBackport.MOD_ID + "." + name);
        event.getRegistry().register(block);
        BLOCKS.add(block);
        return block;
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        // Asked for whether or not stream F is on: the loom's pattern list is built from the enum,
        // and it must be the same list on both sides of a connection. See GlobePattern.
        GlobePattern.globe();

        if (!enabled()) {
            return;
        }

        // The flowers' items, in 1.14's creative order: cornflower, lily of the valley, wither rose.
        item(event, "cornflower", new ItemBlock(cornflower));
        item(event, "lily_of_the_valley", new ItemBlock(lilyOfTheValley));
        item(event, "wither_rose", new ItemBlock(witherRose));

        // 1.14 composts every small flower at 65%.
        for (Block flower : new Block[]{cornflower, lilyOfTheValley, witherRose}) {
            com.exiledradio.villagerbackport.block.Compostables.register(0.65F, Item.getItemFromBlock(flower));
        }

        whiteDye = item(event, "white_dye", new ItemModernDye(EnumDyeColor.WHITE));
        blackDye = item(event, "black_dye", new ItemModernDye(EnumDyeColor.BLACK));
        brownDye = item(event, "brown_dye", new ItemModernDye(EnumDyeColor.BROWN));
        blueDye = item(event, "blue_dye", new ItemModernDye(EnumDyeColor.BLUE));

        suspiciousStew = item(event, "suspicious_stew", new ItemSuspiciousStew());
        leatherHorseArmor = item(event, "leather_horse_armor", new ItemLeatherHorseArmor());

        if (GlobePattern.globe() != null) {
            globeBannerPattern = item(event, "globe_banner_pattern",
                    new ItemBannerPattern(GlobePattern.globe(), "Globe"));
        }

        for (ItemModernDye dye : new ItemModernDye[]{whiteDye, blackDye, brownDye, blueDye}) {
            OreDictionary.registerOre(dye.oreName(), dye);
            OreDictionary.registerOre("dye", dye);
        }
    }

    private static <T extends Item> T item(RegistryEvent.Register<Item> event, String name, T item) {
        item.setRegistryName(new ResourceLocation(VillagerBackport.MOD_ID, name));
        item.setTranslationKey(VillagerBackport.MOD_ID + "." + name);
        item.setCreativeTab(ModCreativeTab.TAB);
        event.getRegistry().register(item);
        ITEMS.add(item);
        return item;
    }

    @SubscribeEvent
    public static void registerSounds(RegistryEvent.Register<SoundEvent> event) {
        if (!enabled()) {
            return;
        }

        mooshroomConvert = sound(event, "entity.mooshroom.convert");
        mooshroomEat = sound(event, "entity.mooshroom.eat");
        mooshroomMilk = sound(event, "entity.mooshroom.milk");
        mooshroomSuspiciousMilk = sound(event, "entity.mooshroom.suspicious_milk");
    }

    private static SoundEvent sound(RegistryEvent.Register<SoundEvent> event, String name) {
        ResourceLocation id = new ResourceLocation(VillagerBackport.MOD_ID, name);
        SoundEvent sound = new SoundEvent(id).setRegistryName(id);
        event.getRegistry().register(sound);
        return sound;
    }

    @SubscribeEvent
    public static void registerRecipes(RegistryEvent.Register<IRecipe> event) {
        if (enabled()) {
            FloraRecipes.register(event.getRegistry());
        }
    }
}
