package com.exiledradio.villagerbackport.selftest;

import com.exiledradio.villagerbackport.VillagerBackport;
import com.exiledradio.villagerbackport.babies.client.BabyModels;
import com.exiledradio.villagerbackport.babies.client.render.BabyWrapper;
import com.exiledradio.villagerbackport.bamboo.EntityPanda;
import com.exiledradio.villagerbackport.cat.EntityCat;
import com.exiledradio.villagerbackport.fox.EntityFox;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.init.Items;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.lang.reflect.Field;

/**
 * 26.1's babies drawn in a world, for {@code gradlew runClient -Pselftest=clientbabies}: a flat creative world with a
 * row of baby and adult foxes, cats, pandas and villagers, photographed with the new babies forced on (as if Game
 * Drops were installed) and then off, into {@code run/vpbabies/screenshots}. Logs {@code CLIENT SELFTEST DONE}.
 */
@SideOnly(Side.CLIENT)
public final class ClientBabiesTest {

    private static final String WORLD = "vp-babies-selftest";
    private static final String[] TEXTURES = {"fox/fox_baby", "fox/fox_sleep_baby", "fox/fox_snow_baby", "fox/fox_snow_sleep_baby",
        "cat/cat_tabby_baby", "cat/cat_all_black_baby", "cat/cat_collar_baby", "panda/panda_baby", "panda/aggressive_panda_baby",
        "villager/villager_baby", "villager/baby/plains", "villager/baby/snow"};

    private int phase;
    private int ticks;
    private int pass;
    private int fail;
    private File shots;

    public static void register() {
        MinecraftForge.EVENT_BUS.register(new ClientBabiesTest());
        VillagerBackport.LOGGER.info("CLIENT SELFTEST babies registered");
    }

    private void ok(boolean condition, String what) {
        if (condition) {
            this.pass++;
            VillagerBackport.LOGGER.info("CLIENT SELFTEST ok {}", what);
        } else {
            this.fail++;
            VillagerBackport.LOGGER.error("CLIENT SELFTEST FAIL {}", what);
        }
    }

    /** Buzzy Bees' and Update Aquatic's switches, when their jars are in the run's mods folder. */
    private static final String[] OTHERS = {"com.exiledradio.buzzybeesbackport.babies.client.BabyModels",
        "com.exiledradio.updateaquaticbackport.babies.client.BabyModels"};

    /** As if Game Drops were installed with its babies on, or not installed at all: here and in the other owners. */
    private static void force(boolean on) throws Exception {
        force(BabyModels.class, on);
        for (String other : OTHERS) {
            try {
                force(Class.forName(other), on);
            } catch (ClassNotFoundException e) {
                // That mod is not in this run.
            }
        }
    }

    private static void force(Class<?> switches, boolean on) throws Exception {
        Field gameDrops = switches.getDeclaredField("gameDrops");
        gameDrops.setAccessible(true);
        gameDrops.set(null, on);
        Field configFailed = switches.getDeclaredField("configFailed");
        configFailed.setAccessible(true);
        configFailed.set(null, on);
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        mc.inGameHasFocus = false;
        try {
            if (this.phase == 0 && mc.currentScreen instanceof GuiMainMenu && ++this.ticks >= 100) {
                this.shots = new File(mc.gameDir, "vpbabies");
                FileUtils.deleteQuietly(new File(this.shots, "screenshots"));
                this.shots.mkdirs();
                for (String t : TEXTURES) {
                    ResourceLocation loc = new ResourceLocation(VillagerBackport.MOD_ID, "textures/entity/" + t + ".png");
                    boolean found;
                    try {
                        found = mc.getResourceManager().getResource(loc) != null;
                    } catch (Exception e) {
                        found = false;
                    }
                    ok(found, "texture " + loc);
                }
                force(true);
                ok(BabyModels.active(), "26.1 babies on while Game Drops' babies are");
                force(false);
                ok(!BabyModels.active(), "26.1 babies off without Game Drops (Core's look is " +
                        com.exiledradio.backportcore.presentation.Presentation.newest() + ")");
                ok(mc.getRenderManager().entityRenderMap.get(EntityVillager.class) instanceof BabyWrapper,
                        "the villager renderer is wrapped for babies");
                force(true);
                this.phase = 1;
                this.ticks = 0;
                FileUtils.deleteQuietly(new File(new File(mc.gameDir, "saves"), WORLD));
                mc.gameSettings.pauseOnLostFocus = false;
                mc.launchIntegratedServer(WORLD, WORLD, new WorldSettings(1L, GameType.CREATIVE, false, false, WorldType.FLAT));
            } else if (this.phase == 1 && ++this.ticks > 2400) {
                ok(false, "the test world never opened");
                finish(mc);
            } else if (this.phase == 1 && mc.world != null && mc.player != null && mc.getIntegratedServer() != null && this.ticks >= 100) {
                this.phase = 2;
                this.ticks = 0;
                final WorldServer ws = mc.getIntegratedServer().worlds[0];
                mc.getIntegratedServer().addScheduledTask(new Runnable() {
                    @Override
                    public void run() {
                        spawn(ws);
                    }
                });
            } else if (this.phase == 2 && ++this.ticks == 80) {
                mc.gameSettings.hideGUI = true;
                int seen = 0;
                for (Object o : mc.world.loadedEntityList) {
                    if (o instanceof EntityAgeable && ((EntityAgeable) o).isChild()) {
                        seen++;
                    }
                }
                ok(seen >= 5, "the babies reached the client: " + seen);
            } else if (this.phase == 2 && this.ticks == 120) {
                shot("babies_26_1.png");
                force(false);
            } else if (this.phase == 2 && this.ticks == 150) {
                shot("babies_1_14.png");
                force(true);
            } else if (this.phase == 2 && this.ticks > 150) {
                finish(mc);
            }
        } catch (Throwable t) {
            VillagerBackport.LOGGER.error("CLIENT SELFTEST threw", t);
            this.fail++;
            finish(mc);
        }
    }

    private static double bx;
    private static double by;
    private static double bz;

    /** A row ahead of the player, who is put where the camera looks at it: each baby with its adult behind it. */
    private static void spawn(WorldServer ws) {
        net.minecraft.entity.player.EntityPlayerMP player = ws.getMinecraftServer().getPlayerList().getPlayers().get(0);
        bx = Math.floor(player.posX) + 0.5D;
        by = Math.floor(player.posY);
        bz = Math.floor(player.posZ) + 0.5D;
        player.capabilities.isFlying = true;
        player.sendPlayerAbilities();
        player.connection.setPlayerLocation(bx + 1.5D, by + 1.5D, bz - 4.5D, 0.0F, 22.0F);
        EntityFox babyFox = new EntityFox(ws);
        babyFox.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(Items.EMERALD));
        place(ws, babyFox, -3.0D, true);
        place(ws, new EntityFox(ws), -3.0D, false);
        EntityCat babyCat = new EntityCat(ws);
        babyCat.setCatType(0);
        babyCat.setTamed(true);
        babyCat.setCollarColor(EnumDyeColor.LIME);
        place(ws, babyCat, -1.5D, true);
        EntityCat cat = new EntityCat(ws);
        cat.setCatType(0);
        place(ws, cat, -1.5D, false);
        place(ws, new EntityPanda(ws), 0.0D, true);
        place(ws, new EntityPanda(ws), 0.0D, false);
        place(ws, new EntityVillager(ws, 0), 1.5D, true);
        place(ws, new EntityVillager(ws, 1), 3.0D, true);
        place(ws, new EntityVillager(ws, 0), 1.5D, false);
        other(ws, "buzzybeesbackport:bee", 4.5D);
        other(ws, "updateaquaticbackport:turtle", 6.0D);
    }

    /** Another owner's baby and adult, when that mod is in this run. */
    private static void other(WorldServer ws, String id, double x) {
        for (boolean baby : new boolean[]{true, false}) {
            net.minecraft.entity.Entity e = net.minecraft.entity.EntityList.createEntityByIDFromName(new ResourceLocation(id), ws);
            if (!(e instanceof EntityLiving)) {
                return;
            }
            place(ws, (EntityLiving) e, x, baby);
        }
    }

    private static void place(WorldServer ws, EntityLiving e, double x, boolean baby) {
        e.setLocationAndAngles(bx + x, by, bz + (baby ? 1.5D : 4.0D), 180.0F, 0.0F);
        e.rotationYawHead = 180.0F;
        e.renderYawOffset = 180.0F;
        e.setNoAI(true);
        if (e instanceof EntityAgeable) {
            ((EntityAgeable) e).setGrowingAge(baby ? -24000 : 0);
        }
        e.enablePersistence();
        ws.spawnEntity(e);
    }

    private void shot(String name) {
        Minecraft mc = Minecraft.getMinecraft();
        ScreenShotHelper.saveScreenshot(this.shots, name, mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
        File f = new File(new File(this.shots, "screenshots"), name);
        ok(f.isFile() && f.length() > 1000, "saved " + name);
    }

    private void finish(Minecraft mc) {
        this.phase = 3;
        VillagerBackport.LOGGER.info("CLIENT SELFTEST DONE pass={} fail={}", this.pass, this.fail);
        mc.shutdown();
    }
}
