package com.exiledradio.villagerbackport.block;

import com.exiledradio.villagerbackport.ModConfig;
import com.exiledradio.villagerbackport.VillagerBackport;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.registries.IForgeRegistryModifiable;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Diamond gear to netherite gear, for the smithing table - from whichever netherite mod is installed.
 *
 * <h2>Why this is not one table</h2>
 * 1.12.2 has no netherite, and the mods that add it agree on nothing. Five unrelated ones share the
 * modid {@code netherite} with four different armour naming schemes between them, two name their
 * items in Spanish and Turkish, and only four of eighteen register the {@code ingotNetherite} ore
 * name. So the upgrades are gathered from every place a mod might have said what they are, in order
 * of how much that source can be trusted:
 * <ol>
 * <li><b>Future MC's smithing recipes</b>, which also carry Spartan Netherite's when that mod is set
 * to add them, and any CraftTweaker has added.</li>
 * <li><b>Spartan Netherite's own map</b> of diamond weapons and shields to netherite ones.</li>
 * <li><b>Crafting recipes</b> that upgrade a diamond item with a netherite ingot in the grid - how
 * most of these mods do it.</li>
 * <li><b>A short table</b> for the mods whose own smithing tables are hard-coded and whose item names
 * no pattern could guess.</li>
 * <li><b>Names</b>, for mods that only craft netherite gear from scratch: {@code diamond} swapped for
 * {@code netherite} in the item's name, and the common spellings of the vanilla pieces, in every
 * mod that adds a netherite ingot. Configurable, since it gives those items an upgrade path their
 * own mod never offered.</li>
 * </ol>
 *
 * <h2>The ingot decides, when there is a choice</h2>
 * A pack with two netherite mods has two netherite swords for one diamond sword. Each upgrade
 * remembers the ingot it was found with - the one in its mod's recipe, or the one its mod adds - so
 * that mod's ingot makes that mod's sword. An ingot that no upgrade for the item names still works,
 * and takes the first one found; 1.16 has one netherite, and a player holding an ingot and a diamond
 * sword expects a netherite sword. Upgrades whose source named some other material (a CraftTweaker
 * recipe in Future MC's table, say) take only that material.
 *
 * <p>Ingots are found by name and given the {@code ingotNetherite} ore name when they lack it, which
 * is also what lets Spartan Netherite's anvil upgrade work alongside these mods.
 *
 * <p>Built once, on first use: everything it reads is complete by the end of loading, and nothing
 * asks before then.
 */
public final class SmithingUpgrades {

    public static final String INGOT_ORE = "ingotNetherite";

    /** Registry paths that are a netherite ingot whatever the mod: netherite_ingot, netheriteingot. */
    private static final Pattern INGOT_PATH = Pattern.compile("netherite_?ingot");

    /** Ingots no pattern finds. */
    private static final String[] NAMED_INGOTS = {
            "netheriteplusmodesp:netherite",
            "netheritesbut:netheritekulcesi",
    };

    /** Another mod's non-standard ore names for its ingot (Netherite Flex). */
    private static final String[] OTHER_INGOT_ORES = {"NetheriteIngot"};

    private static final String[] TOOLS = {"sword", "pickaxe", "axe", "shovel", "hoe"};
    private static final String[] ARMOUR = {"helmet", "chestplate", "leggings", "boots"};

    /**
     * The mods with their own hard-coded smithing tables: vanilla piece, then each mod's item.
     * Same order as {@link #TOOLS} followed by {@link #ARMOUR}.
     */
    private static final String[][] TABLE = {
            // Nether+ (remag)
            {"netherplus:netheritetoolssword", "netherplus:netheritetoolspickaxe", "netherplus:netheritetoolsaxe",
                    "netherplus:netheritetoolsshovel", "netherplus:netheritetoolshoe",
                    "netherplus:netheritearmorhelmet", "netherplus:netheritearmorbody",
                    "netherplus:netheritearmorlegs", "netherplus:netheritearmorboots"},
            // NetheritePlusMOD [ESP]
            {"netheriteplusmodesp:espada", "netheriteplusmodesp:pico", "netheriteplusmodesp:hacha",
                    "netheriteplusmodesp:pala", "netheriteplusmodesp:azada",
                    "netheriteplusmodesp:armaduranetheritehelmet", "netheriteplusmodesp:armaduranetheritebody",
                    "netheriteplusmodesp:armaduranetheritelegs", "netheriteplusmodesp:armaduranetheriteboots"},
            // Netherites But 1.12.2
            {"netheritesbut:netheritekilic", "netheritesbut:netheritekazma", "netheritesbut:netheritebalta",
                    "netheritesbut:netheritekurek", "netheritesbut:netheritecapa",
                    "netheritesbut:netheritesethelmet", "netheritesbut:netheritesetbody",
                    "netheritesbut:netheritesetlegs", "netheritesbut:netheritesetboots"},
            // NetheriteUpdate (AincraftTeam)
            {"netheriteupdate:netheritesword", "netheriteupdate:netheritepickaxe", "netheriteupdate:netheriteaxe",
                    "netheriteupdate:netheriteshovel", "netheriteupdate:netheritehoe",
                    "netheriteupdate:netheritearmorhelmet", "netheriteupdate:netheritearmorbody",
                    "netheriteupdate:netheritearmorlegs", "netheriteupdate:netheritearmorboots"},
    };

    /** How each mod spells the start of a netherite tool or armour piece's name. */
    private static final String[] TOOL_PREFIXES = {"netherite_", "netherite", "netheritetools"};
    private static final String[] ARMOUR_PREFIXES = {
            "netherite_", "netherite", "netheritearmor", "netheritearmor_", "netherite_armor", "netherite_armor_"};

    /** One upgrade: this diamond item, plus the addition, makes the result. */
    public static final class Upgrade {

        public final Item base;

        /** What goes in the second slot by preference; null for any netherite ingot. */
        @Nullable
        private final Ingredient addition;

        private final ItemStack result;

        /** Where it was found, for the log. */
        final String source;

        /** Written out as a recipe rather than guessed; see {@link #put}. */
        final boolean exact;

        Upgrade(Item base, @Nullable Ingredient addition, ItemStack result, String source, boolean exact) {
            this.base = base;
            this.addition = addition;
            this.result = result;
            this.source = source;
            this.exact = exact;
        }

        /** @return true if this addition is the one this upgrade was found with */
        boolean prefers(ItemStack stack) {
            return this.addition == null ? isIngot(stack) : this.addition.apply(stack);
        }

        /** @return true if any netherite ingot will do, should none prefer it */
        boolean takesAnyIngot() {
            if (this.addition == null) {
                return true;
            }
            for (ItemStack stack : this.addition.getMatchingStacks()) {
                if (isIngot(stack)) {
                    return true;
                }
            }
            return false;
        }

        /** @return what can go in the second slot, for display */
        public List<ItemStack> additions() {
            if (this.addition == null) {
                return ingots();
            }
            List<ItemStack> stacks = new ArrayList<ItemStack>();
            Collections.addAll(stacks, this.addition.getMatchingStacks());
            return stacks;
        }

        /** @return a fresh copy of the plain result */
        public ItemStack result() {
            return this.result.copy();
        }
    }

    private static Map<Item, List<Upgrade>> upgrades;

    private SmithingUpgrades() {
    }

    // ---------------------------------------------------------------------------------------------
    // Ingots
    // ---------------------------------------------------------------------------------------------

    /**
     * Gives every netherite ingot the {@code ingotNetherite} ore name. Called from postInit: ore
     * ingredients read the ore list live, so recipes already registered see the additions.
     */
    public static void registerIngots() {
        if (!ModConfig.workstations.netheriteUpgrades) {
            return;
        }

        Set<Item> found = new LinkedHashSet<Item>();

        for (Item item : ForgeRegistries.ITEMS) {
            ResourceLocation name = item.getRegistryName();
            if (name != null && INGOT_PATH.matcher(name.getPath()).matches()) {
                found.add(item);
            }
        }
        for (String name : NAMED_INGOTS) {
            Item item = item(name);
            if (item != null) {
                found.add(item);
            }
        }
        for (String ore : OTHER_INGOT_ORES) {
            for (ItemStack stack : OreDictionary.getOres(ore, false)) {
                found.add(stack.getItem());
            }
        }

        int added = 0;
        for (Item item : found) {
            ItemStack stack = new ItemStack(item);
            if (!hasOre(stack, INGOT_ORE)) {
                OreDictionary.registerOre(INGOT_ORE, stack);
                added++;
            }
        }

        if (added > 0) {
            VillagerBackport.LOGGER.info("Registered {} netherite ingots as {}.", added, INGOT_ORE);
        }
    }

    private static boolean hasOre(ItemStack stack, String ore) {
        int wanted = OreDictionary.getOreID(ore);
        for (int id : OreDictionary.getOreIDs(stack)) {
            if (id == wanted) {
                return true;
            }
        }
        return false;
    }

    public static boolean isIngot(ItemStack stack) {
        return !stack.isEmpty() && hasOre(stack, INGOT_ORE);
    }

    public static List<ItemStack> ingots() {
        return new ArrayList<ItemStack>(OreDictionary.getOres(INGOT_ORE, false));
    }

    // ---------------------------------------------------------------------------------------------
    // Lookup
    // ---------------------------------------------------------------------------------------------

    /** @return true if some netherite mod is installed and upgrades are switched on */
    public static boolean isActive() {
        return ModConfig.workstations.netheriteUpgrades && !all().isEmpty();
    }

    /** @return true if this item can go in the first slot */
    public static boolean isUpgradable(ItemStack base) {
        return !base.isEmpty() && all().containsKey(base.getItem());
    }

    /** @return the upgrade these two make, or null */
    @Nullable
    public static Upgrade find(ItemStack base, ItemStack addition) {
        if (base.isEmpty() || addition.isEmpty()) {
            return null;
        }
        List<Upgrade> options = all().get(base.getItem());
        if (options == null) {
            return null;
        }
        for (Upgrade upgrade : options) {
            if (upgrade.prefers(addition)) {
                return upgrade;
            }
        }
        if (isIngot(addition)) {
            for (Upgrade upgrade : options) {
                if (upgrade.takesAnyIngot()) {
                    return upgrade;
                }
            }
        }
        return null;
    }

    public static List<Upgrade> list() {
        List<Upgrade> list = new ArrayList<Upgrade>();
        for (List<Upgrade> options : all().values()) {
            list.addAll(options);
        }
        return list;
    }

    private static synchronized Map<Item, List<Upgrade>> all() {
        if (upgrades == null) {
            upgrades = build();
        }
        return upgrades;
    }

    // ---------------------------------------------------------------------------------------------
    // Building
    // ---------------------------------------------------------------------------------------------

    private static Map<Item, List<Upgrade>> build() {
        Map<Item, List<Upgrade>> map = new LinkedHashMap<Item, List<Upgrade>>();
        if (!ModConfig.workstations.netheriteUpgrades) {
            return map;
        }

        // Future MC's recipes are taken whether or not anything is netherite: with Future MC's own
        // netherite switched off, a pack can still give its smithing table recipes in CraftTweaker,
        // and those are what Future MC's table would have done.
        fromFutureMc(map);
        if (OreDictionary.getOres(INGOT_ORE, false).isEmpty()) {
            log(map);
            return map;
        }

        fromSpartanNetherite(map);
        List<ResourceLocation> craftingUpgrades = fromCraftingRecipes(map);
        fromTable(map);
        if (ModConfig.workstations.netheriteNameMatching) {
            fromNames(map);
        }

        if (ModConfig.workstations.removeCraftingUpgrades && !craftingUpgrades.isEmpty()
                && ForgeRegistries.RECIPES instanceof IForgeRegistryModifiable) {
            for (ResourceLocation name : craftingUpgrades) {
                ((IForgeRegistryModifiable<IRecipe>) ForgeRegistries.RECIPES).remove(name);
            }
            VillagerBackport.LOGGER.info("Removed {} crafting-grid netherite upgrades; the smithing table does them.",
                    craftingUpgrades.size());
        }

        log(map);
        return map;
    }

    private static void log(Map<Item, List<Upgrade>> map) {
        Map<String, Integer> bySource = new LinkedHashMap<String, Integer>();
        List<Upgrade> all = new ArrayList<Upgrade>();
        for (List<Upgrade> options : map.values()) {
            all.addAll(options);
        }
        for (Upgrade upgrade : all) {
            Integer n = bySource.get(upgrade.source);
            bySource.put(upgrade.source, n == null ? 1 : n + 1);
        }
        VillagerBackport.LOGGER.info("Smithing table: {} netherite upgrades for {} items ({}).",
                all.size(), map.size(), bySource);
        for (Upgrade upgrade : all) {
            VillagerBackport.LOGGER.debug("  {} -> {} ({})", upgrade.base.getRegistryName(),
                    upgrade.result.getItem().getRegistryName(), upgrade.source);
        }
    }

    /** Adds an upgrade unless one already makes the same item from the same base. */
    private static void put(Map<Item, List<Upgrade>> map, Item base, @Nullable Ingredient addition,
                            ItemStack result, String source) {
        put(map, base, addition, result, source, false);
    }

    /**
     * @param exact a recipe someone wrote out (Future MC's, CraftTweaker's): kept as written, whatever
     *              it makes and however many, where a guessed upgrade must be one piece of gear
     */
    private static void put(Map<Item, List<Upgrade>> map, Item base, @Nullable Ingredient addition,
                            ItemStack result, String source, boolean exact) {
        if (base == null || result.isEmpty()) {
            return;
        }
        if (!exact && (result.getItem() == base || result.getMaxStackSize() != 1)) {
            return;
        }
        List<Upgrade> options = map.get(base);
        if (options == null) {
            options = new ArrayList<Upgrade>();
            map.put(base, options);
        }
        for (Upgrade existing : options) {
            if (existing.result.getItem() == result.getItem()
                    && existing.result.getMetadata() == result.getMetadata()
                    && !(exact && existing.exact)) {
                return;
            }
        }
        // An ore-dictionary ingredient matches every mod's ingot once they all share the ore name.
        // Where the result's own mod has an ingot the ingredient accepts, prefer that one only, so
        // that mod's ingot keeps choosing that mod's gear.
        Ingredient own = ingotsOf(result.getItem());
        if (addition != null && own != null) {
            for (ItemStack ingot : own.getMatchingStacks()) {
                if (addition.apply(ingot)) {
                    addition = own;
                    break;
                }
            }
        }

        ItemStack single = result.copy();
        if (!exact) {
            single.setCount(1);
        }
        options.add(new Upgrade(base, addition, single, source, exact));
    }

    /** @return the netherite ingots this item's mod adds, as an ingredient, or null if it adds none */
    @Nullable
    private static Ingredient ingotsOf(Item result) {
        ResourceLocation name = result.getRegistryName();
        if (name == null) {
            return null;
        }
        List<ItemStack> own = new ArrayList<ItemStack>();
        for (ItemStack ingot : ingots()) {
            ResourceLocation ingotName = ingot.getItem().getRegistryName();
            if (ingotName != null && name.getNamespace().equals(ingotName.getNamespace())) {
                own.add(ingot);
            }
        }
        return own.isEmpty() ? null : Ingredient.fromStacks(own.toArray(new ItemStack[0]));
    }

    /** Future MC's smithing recipes, read by reflection - see the class comment on {@code FutureMc}. */
    private static void fromFutureMc(Map<Item, List<Upgrade>> map) {
        if (!Loader.isModLoaded("futuremc")) {
            return;
        }
        try {
            Class<?> recipesClass = Class.forName("thedarkcolour.futuremc.recipe.smithing.SmithingRecipes");
            Object instance = recipesClass.getField("INSTANCE").get(null);
            List<?> recipes = (List<?>) recipesClass.getMethod("getRecipes").invoke(instance);

            for (Object recipe : recipes) {
                Class<?> type = recipe.getClass();
                Ingredient input = (Ingredient) type.getMethod("getInput").invoke(recipe);
                Ingredient material = (Ingredient) type.getMethod("getMaterial").invoke(recipe);
                ItemStack output = (ItemStack) type.getMethod("getOutput").invoke(recipe);

                for (ItemStack stack : input.getMatchingStacks()) {
                    put(map, stack.getItem(), material, output, "Future MC", true);
                }
            }
        } catch (Exception e) {
            VillagerBackport.LOGGER.warn("Could not read Future MC's smithing recipes.", e);
        }
    }

    /** Spartan Netherite's public diamond-to-netherite map. */
    private static void fromSpartanNetherite(Map<Item, List<Upgrade>> map) {
        if (!Loader.isModLoaded("spartannetherite")) {
            return;
        }
        try {
            Class<?> type = Class.forName("com.invadermonky.spartannetherite.registry.ModRecipesSN");
            Field field = type.getField("recipeAssociations");
            Map<?, ?> associations = (Map<?, ?>) field.get(null);

            for (Map.Entry<?, ?> entry : associations.entrySet()) {
                if (entry.getKey() instanceof Item && entry.getValue() instanceof ItemStack) {
                    put(map, (Item) entry.getKey(), null, (ItemStack) entry.getValue(), "Spartan Netherite");
                }
            }
        } catch (Exception e) {
            VillagerBackport.LOGGER.warn("Could not read Spartan Netherite's upgrades.", e);
        }
    }

    /**
     * Crafting recipes of exactly two ingredients, one a diamond item and the other a netherite
     * ingot, making a single item.
     *
     * @return the names of the recipes found, in case the config asks for them to go
     */
    private static List<ResourceLocation> fromCraftingRecipes(Map<Item, List<Upgrade>> map) {
        List<ResourceLocation> found = new ArrayList<ResourceLocation>();

        for (IRecipe recipe : ForgeRegistries.RECIPES) {
            ItemStack output = recipe.getRecipeOutput();
            if (output.isEmpty() || output.getMaxStackSize() != 1) {
                continue;
            }

            List<Ingredient> used = new ArrayList<Ingredient>();
            for (Ingredient ingredient : recipe.getIngredients()) {
                if (ingredient != Ingredient.EMPTY && ingredient.getMatchingStacks().length > 0) {
                    used.add(ingredient);
                }
            }
            if (used.size() != 2) {
                continue;
            }

            Ingredient base = null;
            Ingredient ingot = null;
            for (int i = 0; i < 2; i++) {
                if (isDiamondGear(used.get(i)) && matchesIngot(used.get(1 - i))) {
                    base = used.get(i);
                    ingot = used.get(1 - i);
                }
            }
            if (base == null) {
                continue;
            }

            boolean any = false;
            for (ItemStack stack : base.getMatchingStacks()) {
                if (stack.getItem() != output.getItem()) {
                    put(map, stack.getItem(), ingot, output, "crafting recipes");
                    any = true;
                }
            }
            if (any && recipe.getRegistryName() != null) {
                found.add(recipe.getRegistryName());
            }
        }

        return found;
    }

    private static boolean isDiamondGear(Ingredient ingredient) {
        for (ItemStack stack : ingredient.getMatchingStacks()) {
            ResourceLocation name = stack.getItem().getRegistryName();
            if (name == null || !name.getPath().contains("diamond") || stack.getMaxStackSize() != 1) {
                return false;
            }
        }
        return true;
    }

    private static boolean matchesIngot(Ingredient ingredient) {
        for (ItemStack stack : ingredient.getMatchingStacks()) {
            if (isIngot(stack)) {
                return true;
            }
        }
        return false;
    }

    private static void fromTable(Map<Item, List<Upgrade>> map) {
        for (String[] row : TABLE) {
            for (int i = 0; i < row.length; i++) {
                Item base = item("minecraft:diamond_" + (i < TOOLS.length ? TOOLS[i] : ARMOUR[i - TOOLS.length]));
                Item result = item(row[i]);
                if (result != null) {
                    put(map, base, ingotsOf(result), new ItemStack(result), "known mods");
                }
            }
        }
    }

    /**
     * Name guesses for every diamond item, in its own mod and in every mod that adds a netherite
     * ingot - the first spelling that exists in each. One already found another way is not added
     * twice.
     */
    private static void fromNames(Map<Item, List<Upgrade>> map) {
        Set<String> namespaces = new LinkedHashSet<String>();
        for (ItemStack ingot : ingots()) {
            ResourceLocation name = ingot.getItem().getRegistryName();
            if (name != null) {
                namespaces.add(name.getNamespace());
            }
        }
        namespaces.add("spartannetherite");

        for (Item base : ForgeRegistries.ITEMS) {
            ResourceLocation name = base.getRegistryName();
            if (name == null || !name.getPath().contains("diamond")
                    || base.getItemStackLimit() != 1) {
                continue;
            }

            List<String> paths = new ArrayList<String>();
            paths.add(name.getPath().replace("diamond", "netherite"));
            if ("minecraft".equals(name.getNamespace())) {
                addVanillaSpellings(paths, name.getPath());
            }

            Set<String> where = new LinkedHashSet<String>();
            where.add(name.getNamespace());
            where.addAll(namespaces);

            for (String namespace : where) {
                for (String path : paths) {
                    Item result = item(namespace + ":" + path);
                    if (result != null && result != base && result.getItemStackLimit() == 1) {
                        put(map, base, ingotsOf(result), new ItemStack(result), "names");
                        break;
                    }
                }
            }
        }
    }

    /** The other ways mods spell a vanilla diamond piece's netherite twin. */
    private static void addVanillaSpellings(List<String> paths, String diamondPath) {
        String piece = diamondPath.startsWith("diamond_") ? diamondPath.substring("diamond_".length()) : null;
        if (piece == null) {
            return;
        }

        for (String tool : TOOLS) {
            if (tool.equals(piece)) {
                for (String prefix : TOOL_PREFIXES) {
                    paths.add(prefix + tool);
                }
                if ("pickaxe".equals(tool)) {
                    paths.add("netheritepicaxe");
                    paths.add("netherpickaxe");
                }
                return;
            }
        }

        for (String armour : ARMOUR) {
            if (armour.equals(piece)) {
                List<String> spellings = new ArrayList<String>();
                spellings.add(armour);
                if ("chestplate".equals(armour)) {
                    spellings.add("body");
                } else if ("leggings".equals(armour)) {
                    spellings.add("legs");
                }
                for (String prefix : ARMOUR_PREFIXES) {
                    for (String spelling : spellings) {
                        paths.add(prefix + spelling);
                    }
                }
                return;
            }
        }

        if ("horse_armor".equals(piece)) {
            paths.add("netheritehorsearmor");
        }
    }

    @Nullable
    private static Item item(String name) {
        ResourceLocation id = new ResourceLocation(name);
        if (!ForgeRegistries.ITEMS.containsKey(id)) {
            return null;
        }
        return ForgeRegistries.ITEMS.getValue(id);
    }
}
