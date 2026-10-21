"""1.14 block states -> 1.12.2 block states.

map_state(name, props) returns (names, props12, te) where names is a list of candidate registry names
(the first that exists at runtime wins), props12 uses 1.12.2 property names and values, and te is an
optional tile entity tag (plain dict) to merge into the block's tile entity.
"""

COLOURS = ["white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "light_gray",
           "cyan", "purple", "blue", "brown", "green", "red", "black"]
# 1.12.2 EnumDyeColor names, same order. Light gray is "silver".
COLOURS12 = ["white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "silver",
             "cyan", "purple", "blue", "brown", "green", "red", "black"]

WOODS = ["oak", "spruce", "birch", "jungle", "acacia", "dark_oak"]


class Unmapped(Exception):
    pass


def colour_of(name, suffix):
    """'light_gray_wool' with suffix '_wool' -> index, or None"""
    if not name.endswith(suffix):
        return None
    c = name[: -len(suffix)]
    return COLOURS.index(c) if c in COLOURS else None


def log_block(wood):
    return ("minecraft:log", wood) if wood in ("oak", "spruce", "birch", "jungle") else ("minecraft:log2", wood)


def pick(props, *keys):
    return {k: props[k] for k in keys if k in props}


def button_facing(props):
    face = props.get("face", "wall")
    if face == "floor":
        return "up"
    if face == "ceiling":
        return "down"
    return props.get("facing", "north")


def map_state(full, props):
    props = dict(props or {})
    if not full.startswith("minecraft:"):
        raise Unmapped(full)
    n = full[len("minecraft:"):]

    def one(name, p=None, te=None):
        return [name if ":" in name else "minecraft:" + name], p or {}, te

    # ---- air and fluids
    if n == "structure_void":
        # A jigsaw's way of saying "leave whatever is here". No candidates means place nothing.
        return [], {}, None
    if n in ("air", "cave_air", "void_air"):
        return one("air")
    if n == "water":
        return one("water", {"level": props.get("level", "0")})
    if n == "lava":
        return one("lava", {"level": props.get("level", "0")})

    # ---- wood family
    for wood in WOODS:
        if n == wood + "_log" or n == "stripped_" + wood + "_log":
            b, v = log_block(wood)
            return [b], {"variant": v, "axis": props.get("axis", "y")}, None
        if n == wood + "_wood":
            b, v = log_block(wood)
            return [b], {"variant": v, "axis": "none"}, None
        if n == "stripped_" + wood + "_wood":
            return one("planks", {"variant": wood})
        if n == wood + "_planks":
            return one("planks", {"variant": wood})
        if n == wood + "_slab":
            t = props.get("type", "bottom")
            if t == "double":
                return one("double_wooden_slab", {"variant": wood})
            return one("wooden_slab", {"variant": wood, "half": t})
        if n == wood + "_stairs":
            return one(("oak" if wood == "oak" else wood) + "_stairs", pick(props, "facing", "half"))
        if n == wood + "_door":
            return one("wooden_door" if wood == "oak" else wood + "_door",
                       pick(props, "facing", "half", "hinge", "open", "powered"))
        if n == wood + "_fence":
            return one("fence" if wood == "oak" else wood + "_fence")
        if n == wood + "_fence_gate":
            return one("fence_gate" if wood == "oak" else wood + "_fence_gate", pick(props, "facing", "open", "powered"))
        if n == wood + "_pressure_plate":
            return one("wooden_pressure_plate", {"powered": props.get("powered", "false")})
        if n == wood + "_trapdoor":
            return one("trapdoor", pick(props, "facing", "half", "open"))
        if n == wood + "_button":
            return one("wooden_button", {"facing": button_facing(props), "powered": props.get("powered", "false")})
        if n == wood + "_leaves":
            leaf = "leaves" if wood in ("oak", "spruce", "birch", "jungle") else "leaves2"
            return one(leaf, {"variant": wood, "decayable": "false", "check_decay": "false"})
        if n == wood + "_sapling":
            return one("sapling", {"type": wood, "stage": "0"})
        if n == wood + "_wall_sign":
            return one("wall_sign", pick(props, "facing"))
        if n == wood + "_sign":
            return one("standing_sign", {"rotation": props.get("rotation", "0")})
        if n == "potted_" + wood + "_sapling":
            return one("flower_pot", {}, {"Item": "minecraft:sapling", "Data": WOODS.index(wood)})

    # ---- colours
    c = colour_of(n, "_wool")
    if c is not None:
        return one("wool", {"color": COLOURS12[c]})
    c = colour_of(n, "_carpet")
    if c is not None:
        return one("carpet", {"color": COLOURS12[c]})
    c = colour_of(n, "_stained_glass_pane")
    if c is not None:
        return one("stained_glass_pane", {"color": COLOURS12[c]})
    c = colour_of(n, "_stained_glass")
    if c is not None:
        return one("stained_glass", {"color": COLOURS12[c]})
    c = colour_of(n, "_glazed_terracotta")
    if c is not None:
        return one(COLOURS12[c] + "_glazed_terracotta", pick(props, "facing"))
    c = colour_of(n, "_terracotta")
    if c is not None:
        return one("stained_hardened_clay", {"color": COLOURS12[c]})
    c = colour_of(n, "_bed")
    if c is not None:
        return one("bed", pick(props, "facing", "part", "occupied"), {"color": c})
    c = colour_of(n, "_wall_banner")
    if c is not None:
        # 1.12.2 stores the banner's base as dye damage, which runs the other way.
        return one("wall_banner", pick(props, "facing"), {"Base": 15 - c})
    c = colour_of(n, "_banner")
    if c is not None:
        return one("standing_banner", {"rotation": props.get("rotation", "0")}, {"Base": 15 - c})

    # ---- stone family
    simple = {
        "stone": ("stone", {"variant": "stone"}),
        "granite": ("stone", {"variant": "granite"}),
        "polished_granite": ("stone", {"variant": "smooth_granite"}),
        "diorite": ("stone", {"variant": "diorite"}),
        "polished_diorite": ("stone", {"variant": "smooth_diorite"}),
        "andesite": ("stone", {"variant": "andesite"}),
        "polished_andesite": ("stone", {"variant": "smooth_andesite"}),
        "cobblestone": ("cobblestone", {}),
        "mossy_cobblestone": ("mossy_cobblestone", {}),
        "stone_bricks": ("stonebrick", {"variant": "stonebrick"}),
        "mossy_stone_bricks": ("stonebrick", {"variant": "mossy_stonebrick"}),
        "cracked_stone_bricks": ("stonebrick", {"variant": "cracked_stonebrick"}),
        "chiseled_stone_bricks": ("stonebrick", {"variant": "chiseled_stonebrick"}),
        "bricks": ("brick_block", {}),
        "sandstone": ("sandstone", {"type": "sandstone"}),
        "chiseled_sandstone": ("sandstone", {"type": "chiseled_sandstone"}),
        "cut_sandstone": ("sandstone", {"type": "smooth_sandstone"}),
        # 1.12.2 has no plain smooth sandstone; its cut sandstone is the closest face.
        "smooth_sandstone": ("sandstone", {"type": "smooth_sandstone"}),
        "smooth_stone": ("double_stone_slab", {"variant": "stone", "seamless": "true"}),
        "terracotta": ("hardened_clay", {}),
        "clay": ("clay", {}),
        "gravel": ("gravel", {}),
        "sand": ("sand", {"variant": "sand"}),
        "red_sand": ("sand", {"variant": "red_sand"}),
        "dirt": ("dirt", {"variant": "dirt"}),
        "coarse_dirt": ("dirt", {"variant": "coarse_dirt"}),
        "podzol": ("dirt", {"variant": "podzol"}),
        "grass_block": ("grass", {}),
        "grass_path": ("grass_path", {}),
        "bookshelf": ("bookshelf", {}),
        "crafting_table": ("crafting_table", {}),
        "glass": ("glass", {}),
        "glass_pane": ("glass_pane", {}),
        "iron_bars": ("iron_bars", {}),
        "snow_block": ("snow", {}),
        "packed_ice": ("packed_ice", {}),
        # 1.12.2 has no blue ice; packed ice is the same block a step down.
        "blue_ice": ("packed_ice", {}),
        "ice": ("ice", {}),
        "obsidian": ("obsidian", {}),
        "melon": ("melon_block", {}),
        "cobweb": ("web", {}),
        "dead_bush": ("deadbush", {}),
        "dandelion": ("yellow_flower", {"type": "dandelion"}),
        "poppy": ("red_flower", {"type": "poppy"}),
        "blue_orchid": ("red_flower", {"type": "blue_orchid"}),
        "allium": ("red_flower", {"type": "allium"}),
        "azure_bluet": ("red_flower", {"type": "houstonia"}),
        "red_tulip": ("red_flower", {"type": "red_tulip"}),
        "orange_tulip": ("red_flower", {"type": "orange_tulip"}),
        "white_tulip": ("red_flower", {"type": "white_tulip"}),
        "pink_tulip": ("red_flower", {"type": "pink_tulip"}),
        "oxeye_daisy": ("red_flower", {"type": "oxeye_daisy"}),
        "grass": ("tallgrass", {"type": "tall_grass"}),
        "fern": ("tallgrass", {"type": "fern"}),
        "brewing_stand": ("brewing_stand", {}),
        "smithing_table": ("villagerbackport:smithing_table", {}),
        "cartography_table": ("villagerbackport:cartography_table", {}),
        "fletching_table": ("villagerbackport:fletching_table", {}),
        "slime_block": ("slime", {}),
        "glowstone": ("glowstone", {}),
        "magma_block": ("magma", {}),
        "netherrack": ("netherrack", {}),
    }
    if n in simple:
        name, p = simple[n]
        return one(name, dict(p))

    if n == "carrots":
        return one("carrots", {"age": props.get("age", "0")})
    if n == "potatoes":
        return one("potatoes", {"age": props.get("age", "0")})
    if n == "beetroots":
        return one("beetroots", {"age": props.get("age", "0")})
    if n == "wheat":
        return one("wheat", {"age": props.get("age", "0")})
    if n in ("melon_stem", "pumpkin_stem"):
        return one(n, {"age": props.get("age", "0")})
    if n == "pumpkin":
        return one("pumpkin", {"facing": "south"})
    if n == "carved_pumpkin":
        return one("pumpkin", pick(props, "facing"))
    if n == "jack_o_lantern":
        return one("lit_pumpkin", pick(props, "facing"))
    if n == "hay_block":
        return one("hay_block", {"axis": props.get("axis", "y")})
    if n == "farmland":
        return one("farmland", {"moisture": props.get("moisture", "0")})
    if n == "snow":
        return one("snow_layer", {"layers": props.get("layers", "1")})
    if n == "cactus":
        return one("cactus", {"age": props.get("age", "0")})
    if n == "tall_grass":
        return one("double_plant", {"variant": "double_grass", "half": props.get("half", "lower")})
    if n == "large_fern":
        return one("double_plant", {"variant": "double_fern", "half": props.get("half", "lower")})

    # slabs, stairs, walls in stone
    if n.endswith("_slab"):
        base = n[: -len("_slab")]
        variant = {"cobblestone": "cobblestone", "sandstone": "sand", "smooth_sandstone": "sand",
                   "cut_sandstone": "sand", "smooth_stone": "stone", "stone": "stone", "stone_brick": "stone_brick",
                   "brick": "brick", "diorite": "quartz", "granite": "stone", "andesite": "stone",
                   "quartz": "quartz"}.get(base)
        if variant is None:
            raise Unmapped(full)
        t = props.get("type", "bottom")
        if t == "double":
            return one("double_stone_slab", {"variant": variant})
        return one("stone_slab", {"variant": variant, "half": t})
    if n.endswith("_stairs"):
        base = n[: -len("_stairs")]
        name = {"cobblestone": "stone_stairs", "sandstone": "sandstone_stairs",
                "smooth_sandstone": "sandstone_stairs", "stone_brick": "stone_brick_stairs",
                "brick": "brick_stairs", "diorite": "quartz_stairs", "granite": "stone_stairs",
                "andesite": "stone_stairs", "quartz": "quartz_stairs"}.get(base)
        if name is None:
            raise Unmapped(full)
        return one(name, pick(props, "facing", "half"))
    if n.endswith("_wall") and not n.endswith("_wall_sign") and not n.endswith("_wall_banner") and n != "wall_torch":
        base = n[: -len("_wall")]
        if base == "mossy_cobblestone":
            return one("cobblestone_wall", {"variant": "mossy_cobblestone"})
        if base == "sandstone":
            # No sandstone wall exists in 1.12.2. The desert houses use them as roof parapets, where
            # a sandstone block reads as the same edge and a cobblestone wall would not.
            return one("sandstone", {"type": "sandstone"})
        return one("cobblestone_wall", {"variant": "cobblestone"})

    # ---- lighting
    if n == "torch":
        return one("torch", {"facing": "up"})
    if n == "wall_torch":
        return one("torch", pick(props, "facing"))
    if n == "lantern":
        return one("villagerbackport:lantern", {"hanging": props.get("hanging", "false")})
    if n == "campfire":
        # A lit campfire indoors. 1.12.2 has nothing that burns without spreading, so the spot is left
        # empty unless a mod that adds a campfire is installed.
        return ["futuremc:campfire", "minecraft:air"], pick(props, "facing", "lit"), None
    if n == "ladder":
        return one("ladder", pick(props, "facing"))
    if n == "stone_button":
        return one("stone_button", {"facing": button_facing(props), "powered": props.get("powered", "false")})
    if n == "lever":
        return one("lever", {"facing": "north", "powered": "false"})

    # ---- containers and job sites
    if n == "chest":
        return one("chest", pick(props, "facing"))
    if n == "furnace":
        return one("lit_furnace" if props.get("lit") == "true" else "furnace", pick(props, "facing"))
    if n == "cauldron":
        return one("cauldron", {"level": props.get("level", "0")})
    if n in ("barrel",):
        return one("villagerbackport:barrel", {"facing": props.get("facing", "up"), "open": "false"})
    if n in ("blast_furnace", "smoker"):
        return one("villagerbackport:" + n, {"facing": props.get("facing", "north"), "lit": "false"})
    if n in ("loom", "stonecutter", "lectern", "grindstone"):
        return one("villagerbackport:" + n, {"facing": props.get("facing", "north")})
    if n == "composter":
        return one("villagerbackport:composter", {"level": props.get("level", "0")})
    if n == "bell":
        return one("villagerbackport:bell", {"facing": props.get("facing", "north"),
                                             "attachment": props.get("attachment", "floor")})

    # ---- pots
    pots = {"potted_cactus": ("minecraft:cactus", 0), "potted_dandelion": ("minecraft:yellow_flower", 0),
            "potted_dead_bush": ("minecraft:deadbush", 0), "potted_poppy": ("minecraft:red_flower", 0),
            "potted_fern": ("minecraft:tallgrass", 2), "potted_red_mushroom": ("minecraft:red_mushroom", 0),
            "potted_brown_mushroom": ("minecraft:brown_mushroom", 0)}
    if n in pots:
        item, data = pots[n]
        return one("flower_pot", {}, {"Item": item, "Data": data})
    if n == "flower_pot":
        return one("flower_pot")

    if n in ("sea_pickle", "sweet_berry_bush", "kelp", "kelp_plant", "seagrass", "tall_seagrass"):
        return one("air")

    raise Unmapped(full)


FACINGS = ["down", "up", "north", "south", "west", "east"]
