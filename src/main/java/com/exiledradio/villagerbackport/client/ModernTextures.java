package com.exiledradio.villagerbackport.client;

import java.util.List;

import com.exiledradio.villagerbackport.ModConfig;
import com.exiledradio.villagerbackport.VillagerBackport;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.DefaultResourcePack;
import net.minecraft.client.resources.IResourcePack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 1.14's Texture Update for vanilla's blocks, items and mobs, and the option to keep 1.12.2's.
 *
 * <p>Two packs ship inside the jar (see {@link BuiltInPack}); exactly one is active:
 * <ul>
 * <li><b>modern</b> - 1.14.4's textures, plus 1.15's enchantment glint, saved under 1.12.2's file names.
 * It goes straight above vanilla, so every other mod's deliberate override of a vanilla texture,
 * and every resource pack the player picks, still wins over it.</li>
 * <li><b>classic</b> - 1.12.2's look for the few of this mod's own textures that are 1.14 copies of
 * something 1.12.2 already had (the three cat skins an ocelot could be tamed into). It goes straight
 * above this mod's own assets.</li>
 * </ul>
 *
 * <h2>Why at construction</h2>
 * FML loads every resource pack once, after mods are constructed and before pre-init, and the texture
 * atlas is first stitched from that load. A pack added in pre-init would sit unused until the player
 * reloaded resources. So {@link #install()} runs from the construction event, which is after FML has
 * listed the mods' own packs and before that load.
 */
@SideOnly(Side.CLIENT)
public final class ModernTextures {

    private static BuiltInPack modern;
    private static BuiltInPack classic;
    private static boolean active;

    private ModernTextures() {
    }

    public static void install() {
        // ConfigManager has already synced in the normal course of construction, but whether that runs
        // before or after this mod's construction handler is up to Guava's event bus, so make sure.
        ConfigManager.sync(VillagerBackport.MOD_ID, Config.Type.INSTANCE);
        modern = new BuiltInPack("modern");
        classic = new BuiltInPack("classic");
        active = ModConfig.textures.modernTextures;
        apply(active);
        MinecraftForge.EVENT_BUS.register(new ModernTextures.Listener());
        VillagerBackport.LOGGER.info("Texture Update: {} ({} modern textures, {} classic).",
                active ? "1.14 textures" : "1.12.2 textures", modern.size(), classic.size());
    }

    private static List<IResourcePack> defaultPacks() {
        return ObfuscationReflectionHelper.getPrivateValue(Minecraft.class, Minecraft.getMinecraft(),
                "field_110449_ao");
    }

    private static void apply(boolean useModern) {
        List<IResourcePack> packs = defaultPacks();
        packs.remove(modern);
        packs.remove(classic);
        if (useModern) {
            int at = 0;
            for (int i = 0; i < packs.size(); i++) {
                if (packs.get(i) instanceof DefaultResourcePack) {
                    at = i + 1;
                    break;
                }
            }
            packs.add(at, modern);
        } else {
            IResourcePack ours = FMLClientHandler.instance().getResourcePackFor(VillagerBackport.MOD_ID);
            int at = ours == null ? -1 : packs.indexOf(ours);
            packs.add(at < 0 ? packs.size() : at + 1, classic);
        }
    }

    /** Switches packs when the option is changed from the in-game config screen. */
    public static final class Listener {

        // Post, not On: ModConfig's own handler copies the screen's values into the fields on the
        // On event, and the order two handlers of one event run in is not defined.
        @SubscribeEvent
        public void onConfigChanged(ConfigChangedEvent.PostConfigChangedEvent event) {
            if (!VillagerBackport.MOD_ID.equals(event.getModID()) || ModConfig.textures.modernTextures == active) {
                return;
            }
            active = ModConfig.textures.modernTextures;
            apply(active);
            Minecraft mc = Minecraft.getMinecraft();
            mc.addScheduledTask(mc::refreshResources);
        }
    }
}
