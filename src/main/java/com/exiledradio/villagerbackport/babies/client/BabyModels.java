package com.exiledradio.villagerbackport.babies.client;

import com.exiledradio.backportcore.presentation.Presentation;
import com.exiledradio.villagerbackport.VillagerBackport;
import com.exiledradio.villagerbackport.babies.client.render.BabyWrapper;

import net.minecraft.client.renderer.entity.Render;

import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.lang.reflect.Field;

/**
 * Whether babies draw 26.1's baby models (the redrawn foxes, cats, pandas and villagers) or this mod's 1.14 ones.
 *
 * <p>26.1 belongs to Game Drops Backport, so the new babies come with it: while it is installed they follow its own
 * {@code babies.enabled} and {@code babies.babyModels} settings, read every frame so its config screen applies at once.
 * Without it they show only when Core's look reaches 26.1 (a later 26.x backport).
 */
@SideOnly(Side.CLIENT)
public final class BabyModels {

    private static final String GAME_DROPS = "gamedropsbackport";

    private static Boolean gameDrops;
    private static Object babiesConfig;
    private static Field enabled;
    private static Field babyModels;
    private static boolean configFailed;

    private BabyModels() {
    }

    /** @return true when babies draw 26.1's models */
    public static boolean active() {
        if (gameDrops == null) {
            gameDrops = Loader.isModLoaded(GAME_DROPS);
        }
        if (gameDrops) {
            return gameDropsSays();
        }
        return Presentation.isAtLeast("26.1");
    }

    /**
     * @return the renderer under a baby wrapper (this mod's, or Game Drops' around vanilla's horses, cows and the
     *         rest: both have {@code vanilla()}), or {@code render} itself, for adding layers to what adults use
     */
    public static Render<?> unwrap(Render<?> render) {
        if (render instanceof BabyWrapper) {
            return ((BabyWrapper<?>) render).vanilla();
        }
        if (render != null && render.getClass().getSimpleName().equals("BabyWrapper")) {
            try {
                Object inner = render.getClass().getMethod("vanilla").invoke(render);
                if (inner instanceof Render) {
                    return (Render<?>) inner;
                }
            } catch (ReflectiveOperationException | RuntimeException e) {
                VillagerBackport.LOGGER.warn("Could not see under {}", render.getClass().getName(), e);
            }
        }
        return render;
    }

    private static boolean gameDropsSays() {
        if (configFailed) {
            return true;
        }
        try {
            if (babiesConfig == null) {
                Class<?> config = Class.forName("com.exiledradio.gamedropsbackport.ModConfig");
                babiesConfig = config.getField("babies").get(null);
                enabled = babiesConfig.getClass().getField("enabled");
                babyModels = babiesConfig.getClass().getField("babyModels");
            }
            return enabled.getBoolean(babiesConfig) && babyModels.getBoolean(babiesConfig);
        } catch (ReflectiveOperationException | RuntimeException e) {
            // A Game Drops without these settings: its babies are always on, so these are too.
            configFailed = true;
            VillagerBackport.LOGGER.info("Game Drops Backport has no babies.babyModels setting; 26.1 babies always shown");
            return true;
        }
    }
}
