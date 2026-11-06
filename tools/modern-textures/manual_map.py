"""Hand-written 1.12.2 -> 1.13+ texture names the automatic pass could not find or got wrong."""

B = {
    'anvil_base': 'anvil', 'anvil_top_damaged_0': 'anvil_top', 'anvil_top_damaged_1': 'chipped_anvil_top',
    'anvil_top_damaged_2': 'damaged_anvil_top',
    'cocoa_stage_0': 'cocoa_stage0', 'cocoa_stage_1': 'cocoa_stage1', 'cocoa_stage_2': 'cocoa_stage2',
    'comparator_off': 'comparator', 'door_wood_lower': 'oak_door_bottom', 'door_wood_upper': 'oak_door_top',
    'double_plant_fern_bottom': 'large_fern_bottom', 'double_plant_fern_top': 'large_fern_top',
    'double_plant_grass_bottom': 'tall_grass_bottom', 'double_plant_grass_top': 'tall_grass_top',
    'double_plant_paeonia_bottom': 'peony_bottom', 'double_plant_paeonia_top': 'peony_top',
    'double_plant_rose_bottom': 'rose_bush_bottom', 'double_plant_rose_top': 'rose_bush_top',
    'double_plant_sunflower_back': 'sunflower_back', 'double_plant_sunflower_bottom': 'sunflower_bottom',
    'double_plant_sunflower_top': 'sunflower_top', 'double_plant_sunflower_front': 'sunflower_front',
    'double_plant_syringa_bottom': 'lilac_bottom', 'double_plant_syringa_top': 'lilac_top',
    'end_bricks': 'end_stone_bricks', 'farmland_dry': 'farmland', 'farmland_wet': 'farmland_moist',
    'flower_houstonia': 'azure_bluet', 'glass_silver': 'light_gray_stained_glass',
    'grass_side_overlay': 'grass_block_side_overlay', 'grass_side_snowed': 'grass_block_snow',
    'hardened_clay': 'terracotta', 'mob_spawner': 'spawner',
    'mushroom_block_skin_brown': 'brown_mushroom_block', 'mushroom_block_skin_red': 'red_mushroom_block',
    'mushroom_block_skin_stem': 'mushroom_stem', 'mushroom_block_inside': 'mushroom_block_inside',
    'noteblock': 'note_block', 'observer_back_lit': 'observer_back_on', 'prismarine_dark': 'dark_prismarine',
    'prismarine_rough': 'prismarine', 'pumpkin_face_off': 'carved_pumpkin', 'pumpkin_face_on': 'jack_o_lantern',
    'quartz_block_chiseled': 'chiseled_quartz_block', 'quartz_block_chiseled_top': 'chiseled_quartz_block_top',
    'quartz_block_lines': 'quartz_pillar', 'quartz_block_lines_top': 'quartz_pillar_top',
    'quartz_block_bottom': 'quartz_block_bottom',
    'quartz_ore': 'nether_quartz_ore', 'rail_activator_powered': 'activator_rail_on',
    'rail_detector_powered': 'detector_rail_on', 'rail_golden': 'powered_rail', 'rail_golden_powered': 'powered_rail_on',
    'rail_normal_turned': 'rail_corner', 'red_nether_brick': 'red_nether_bricks',
    'red_sandstone_carved': 'chiseled_red_sandstone', 'red_sandstone_smooth': 'cut_red_sandstone',
    'redstone_lamp_off': 'redstone_lamp', 'sandstone_carved': 'chiseled_sandstone', 'sandstone_smooth': 'cut_sandstone',
    'slime': 'slime_block', 'sponge_wet': 'wet_sponge', 'stone_andesite_smooth': 'polished_andesite',
    'stone_diorite_smooth': 'polished_diorite', 'stone_granite_smooth': 'polished_granite',
    'stone_slab_side': 'smooth_stone_slab_side', 'stone_slab_top': 'smooth_stone',
    'stonebrick_carved': 'chiseled_stone_bricks', 'stonebrick_cracked': 'cracked_stone_bricks',
    'stonebrick_mossy': 'mossy_stone_bricks', 'trapdoor': 'oak_trapdoor', 'waterlily': 'lily_pad', 'web': 'cobweb',
    'tallgrass': 'grass', 'pumpkin_stem_connected': 'attached_pumpkin_stem',
    'melon_stem_connected': 'attached_melon_stem', 'pumpkin_stem_disconnected': 'pumpkin_stem',
    'melon_stem_disconnected': 'melon_stem',
}

I = {
    'chorus_fruit_popped': 'popped_chorus_fruit', 'dye_powder_black': 'ink_sac', 'dye_powder_blue': 'lapis_lazuli',
    'dye_powder_brown': 'cocoa_beans', 'dye_powder_white': 'bone_meal', 'dye_powder_red': 'red_dye',
    'dye_powder_yellow': 'yellow_dye', 'dye_powder_green': 'green_dye', 'dye_powder_silver': 'light_gray_dye',
    'fireworks': 'firework_rocket', 'fish_clownfish_raw': 'tropical_fish', 'melon': 'melon_slice',
    'melon_speckled': 'glistering_melon_slice', 'netherbrick': 'nether_brick', 'potion_bottle_drinkable': 'potion',
    'potion_bottle_lingering': 'lingering_potion', 'potion_bottle_splash': 'splash_potion',
    'record_11': 'music_disc_11', 'reeds': 'sugar_cane', 'sign': 'oak_sign', 'totem': 'totem_of_undying',
}
for c in ('cyan', 'gray', 'light_blue', 'lime', 'magenta', 'orange', 'pink', 'purple'):
    I['dye_powder_' + c] = c + '_dye'

E = {
    'bed/silver': 'bed/light_gray', 'cow/mooshroom': 'cow/red_mooshroom',
    'endercrystal/endercrystal': 'end_crystal/end_crystal', 'illager/fangs': 'illager/evoker_fangs',
    'illager/illusionist': 'illager/illusioner', 'snowman': 'snow_golem',
    'llama/llama_brown': 'llama/brown', 'llama/llama_creamy': 'llama/creamy', 'llama/llama_gray': 'llama/gray',
    'llama/llama_white': 'llama/white',
}
for w in ('acacia', 'birch', 'jungle', 'oak', 'spruce'):
    E['boat/boat_' + w] = 'boat/' + w
E['boat/boat_darkoak'] = 'boat/dark_oak'

MANUAL = {}
MANUAL.update({'blocks/' + k: 'block/' + v for k, v in B.items()})
MANUAL.update({'items/' + k: 'item/' + v for k, v in I.items()})
MANUAL.update({'entity/' + k: 'entity/' + v for k, v in E.items()})

# 1.12.2 textures never replaced, whatever 1.14 did to them:
#  villagers and zombie villagers - 1.14 split them into biome + profession layers. This mod draws its
#    own villager skins, and a 1.14 base texture on 1.12's renderer would lose the clothes.
#  horses - 1.14 remade the horse model; its 64x64 textures don't fit 1.12's 128x128 UV layout.
#  water - 1.13 made it grayscale and tints it with a per-biome water color 1.12.2 doesn't have,
#    so it would render gray.
#  explosion, sweep - particle sheets, not blocks, items or mobs.
#  banner_overlay, flower_paeonia, llama/llama - unused in 1.12.2, with nothing in 1.14 to map to.
EXCLUDE = ('entity/villager/', 'entity/zombie_villager/', 'entity/zombie/zombie_villager', 'entity/horse/',
           'blocks/water_', 'entity/explosion', 'entity/sweep', 'items/banner_overlay', 'blocks/flower_paeonia')
EXCLUDE_EXACT = ('entity/llama/llama',)

# 1.15 changed these files, but only to fit its rewritten entity renderer: chests and the dragon's wings
# were re-laid-out, and banner/shield patterns became white masks. None of it is a new look, and none of
# it fits 1.12's renderer, so these take their 1.14.4 version.
LAYOUT_ONLY_115 = ('entity/chest/', 'entity/enderdragon/', 'entity/banner/', 'entity/shield/')

# Textures outside blocks/, items/, entity/ and models/armor that 1.15 really did change.
EXTRA_115 = {'misc/enchanted_item_glint': 'misc/enchanted_item_glint'}

# The mod's own copies of 1.14 textures that 1.12.2 also had. With the option off, these go back
# to their 1.12.2 look: {mod texture: 1.12.2 vanilla texture}.
CLASSIC_MOD = {
    'entity/cat/black': 'entity/cat/black',
    'entity/cat/red': 'entity/cat/red',
    'entity/cat/siamese': 'entity/cat/siamese',
}
