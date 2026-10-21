package com.exiledradio.villagerbackport.village.modern;

import com.exiledradio.villagerbackport.VillagerBackport;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.gen.structure.MapGenStructureData;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Draws 1.14 villages on Antique Atlas maps, as the atlas draws 1.12.2's.
 *
 * <h2>Why they were missing</h2>
 * Antique Atlas finds villages by reading the saved "Village" structure data and turning each piece
 * it recognises - by 1.12.2's piece ids, {@code ViSH} for a small house, {@code ViBH} for a library,
 * {@code ViStart} for the well - into a tile in that piece's chunk, with a village marker at the
 * well. 1.14 villages are made of this mod's own pieces, whose ids it has never heard of, so it drew
 * nothing; in Open Terrain Generator worlds the data is saved as "OTGVillage", which it never reads.
 *
 * <h2>What this does</h2>
 * The same thing, for this mod's villages: reads the saved data the same way, picks the atlas tile
 * closest to each 1.14 building (a library is a library, a weaponsmith a smithy, a lamp post a torch,
 * a farm a field, the town centre the well) and puts the marker on the town centre. Each village is
 * drawn once and remembered, so villages generated before this existed are drawn too.
 *
 * <p>Talks to the atlas through its API by reflection, so nothing here needs the atlas installed.
 */
@Mod.EventBusSubscriber(modid = VillagerBackport.MOD_ID)
public final class AtlasVillages {

    private static final String ATLAS = "antiqueatlas";

    /** How often each world is checked for villages not yet drawn. */
    private static final int INTERVAL = 200;

    /** The atlas's tile names, best first: where two pieces share a chunk, the higher one is drawn. */
    private static final String[] PRIORITY = {
            "npcVillageChurch", "npcVillageLibrary", "npcVillageSmithy", "npcVillageButchersShop",
            "npcVillageLHouse", "npcVillageSmallHouse", "npcVillageHut", "npcVillageFarmlandLarge",
            "npcVillageFarmlandSmall", "npcVillageWell", "npcVillageTorch"};

    private static Boolean available;
    private static Object tiles;
    private static Method putTile;
    private static Object markers;
    private static Method putMarker;
    private static Object villageMarker;

    private AtlasVillages() {
    }

    @SubscribeEvent
    public static void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.world instanceof WorldServer)
                || event.world.getTotalWorldTime() % INTERVAL != 0 || !available()) {
            return;
        }
        WorldServer world = (WorldServer) event.world;
        Drawn drawn = Drawn.of(world);
        for (String name : new String[] {"Village", "OTGVillage"}) {
            MapGenStructureData data = (MapGenStructureData) world.getPerWorldStorage()
                    .getOrLoadData(MapGenStructureData.class, name);
            if (data == null) {
                continue;
            }
            NBTTagCompound features = data.getTagCompound();
            for (String key : features.getKeySet()) {
                NBTBase start = features.getTag(key);
                if (!(start instanceof NBTTagCompound)
                        || !"VBP_ModernVillage".equals(((NBTTagCompound) start).getString("id"))) {
                    continue;
                }
                // Villages saved before this mod wrote 1.12.2's "Valid" flag: the atlas erases what is
                // drawn on a village without it. See ModernVillageStart.writeToNBT.
                NBTTagCompound village = (NBTTagCompound) start;
                if (!village.hasKey("Valid")) {
                    village.setBoolean("Valid", true);
                    data.markDirty();
                }
                String id = world.provider.getDimension() + ":" + name + ":" + key;
                if (drawn.has(id)) {
                    continue;
                }
                try {
                    draw(world, (NBTTagCompound) start);
                } catch (Exception e) {
                    VillagerBackport.LOGGER.warn("Could not draw a 1.14 village on the atlas.", e);
                }
                drawn.add(id);
            }
        }
    }

    private static void draw(World world, NBTTagCompound start) throws Exception {
        NBTTagList children = start.getTagList("Children", 10);
        Map<Long, String> chunkTiles = new HashMap<Long, String>();
        Integer centreX = null;
        Integer centreZ = null;

        for (int i = 0; i < children.tagCount(); i++) {
            NBTTagCompound child = children.getCompoundTagAt(i);
            int[] bb = child.getIntArray("BB");
            if (bb.length < 6) {
                continue;
            }
            int cx = ((bb[0] + bb[3]) / 2) >> 4;
            int cz = ((bb[2] + bb[5]) / 2) >> 4;
            String tile = tileFor(child);
            if (i == 0 && "VBP_ModernPiece".equals(child.getString("id"))) {
                // The first piece is the town centre, where 1.14 puts the bell.
                centreX = (bb[0] + bb[3]) / 2;
                centreZ = (bb[2] + bb[5]) / 2;
                tile = "npcVillageWell";
            }
            if (tile == null) {
                continue;
            }
            long chunk = ((long) cx << 32) | (cz & 0xFFFFFFFFL);
            String current = chunkTiles.get(chunk);
            if (current == null || rank(tile) < rank(current)) {
                chunkTiles.put(chunk, tile);
            }
        }

        for (Map.Entry<Long, String> entry : chunkTiles.entrySet()) {
            int cx = (int) (entry.getKey() >> 32);
            int cz = (int) (long) entry.getKey();
            putTile.invoke(tiles, world, entry.getValue(), cx, cz);
        }
        if (centreX != null && autoMarkers()) {
            putMarker.invoke(markers, world, Boolean.FALSE, villageMarker, "gui.antiqueatlas.marker.village",
                    centreX, centreZ);
        }
    }

    /** The atlas tile nearest in meaning to a piece, or null to draw nothing (streets, decorations). */
    private static String tileFor(NBTTagCompound child) {
        String id = child.getString("id");
        if (id.startsWith("Rc:")) {
            // RLCraft's and Recurrent Complex's houses, inns and libraries.
            return "npcVillageLHouse";
        }
        if (!"VBP_ModernPiece".equals(id)) {
            return null;
        }
        String location = location(child.getString("Pool"), child.getInteger("Element"));
        if (location == null) {
            return null;
        }
        String name = location.substring(location.lastIndexOf('/') + 1);
        if (name.contains("lamp")) {
            return "npcVillageTorch";
        }
        if (!location.contains("/houses/")) {
            return null;
        }
        if (name.contains("temple")) {
            return "npcVillageChurch";
        }
        if (name.contains("library")) {
            return "npcVillageLibrary";
        }
        if (name.contains("smith") || name.contains("armorer")) {
            return "npcVillageSmithy";
        }
        if (name.contains("butcher")) {
            return "npcVillageButchersShop";
        }
        if (name.contains("large_farm")) {
            return "npcVillageFarmlandLarge";
        }
        if (name.contains("farm")) {
            return "npcVillageFarmlandSmall";
        }
        if (name.contains("big_house") || name.contains("medium_house")) {
            return "npcVillageLHouse";
        }
        if (name.contains("small_house")) {
            return "npcVillageSmallHouse";
        }
        if (name.contains("accessory") || name.contains("decoration")) {
            return null;
        }
        // Fisher, fletcher, cartographer, mason, shepherd, tannery, stable, animal pen.
        return "npcVillageHut";
    }

    private static String location(String pool, int index) {
        ModernPools.Pool p = ModernPools.get(pool);
        if (p == null || index < 0 || index >= p.distinct.size()) {
            return null;
        }
        ModernPools.Element element = p.distinct.get(index);
        while (element instanceof ModernPools.ListElement && !((ModernPools.ListElement) element).elements.isEmpty()) {
            element = ((ModernPools.ListElement) element).elements.get(0);
        }
        return element instanceof ModernPools.SingleElement ? ((ModernPools.SingleElement) element).location : null;
    }

    private static int rank(String tile) {
        for (int i = 0; i < PRIORITY.length; i++) {
            if (PRIORITY[i].equals(tile)) {
                return i;
            }
        }
        return PRIORITY.length;
    }

    // ---------------------------------------------------------------------------------------------

    private static boolean available() {
        if (available != null) {
            return available.booleanValue();
        }
        available = Boolean.FALSE;
        if (!Loader.isModLoaded(ATLAS)) {
            return false;
        }
        try {
            Class<?> api = Class.forName("hunternif.mc.atlas.api.AtlasAPI");
            tiles = api.getField("tiles").get(null);
            markers = api.getField("markers").get(null);
            Class<?> markerType = Class.forName("hunternif.mc.atlas.registry.MarkerType");
            putTile = Class.forName("hunternif.mc.atlas.api.TileAPI")
                    .getMethod("putCustomGlobalTile", World.class, String.class, int.class, int.class);
            putMarker = Class.forName("hunternif.mc.atlas.api.MarkerAPI")
                    .getMethod("putGlobalMarker", World.class, boolean.class, markerType, String.class, int.class, int.class);
            villageMarker = Class.forName("hunternif.mc.atlas.registry.MarkerTypes").getField("VILLAGE").get(null);
            available = Boolean.TRUE;
            VillagerBackport.LOGGER.info("Antique Atlas is installed; 1.14 villages will be drawn on atlas maps.");
        } catch (Exception e) {
            VillagerBackport.LOGGER.warn("Could not reach Antique Atlas's API; 1.14 villages will not show on atlas maps.", e);
        }
        return available.booleanValue();
    }

    /** The atlas's own "automatically mark villages" setting, which its village markers follow. */
    private static boolean autoMarkers() {
        try {
            Object gameplay = Class.forName("hunternif.mc.atlas.SettingsConfig").getField("gameplay").get(null);
            Field field = gameplay.getClass().getField("autoVillageMarkers");
            return field.getBoolean(gameplay);
        } catch (Exception e) {
            return true;
        }
    }

    /** Which villages have been drawn, saved with the world so each is drawn once. */
    public static final class Drawn extends WorldSavedData {

        private static final String NAME = "villagerbackport_atlas";

        private final Set<String> ids = new HashSet<String>();

        public Drawn(String name) {
            super(name);
        }

        static Drawn of(WorldServer world) {
            // Global storage, so one list covers every dimension; ids carry the dimension.
            Drawn data = (Drawn) world.getMapStorage().getOrLoadData(Drawn.class, NAME);
            if (data == null) {
                data = new Drawn(NAME);
                world.getMapStorage().setData(NAME, data);
            }
            return data;
        }

        boolean has(String id) {
            return this.ids.contains(id);
        }

        void add(String id) {
            this.ids.add(id);
            markDirty();
        }

        @Override
        public void readFromNBT(NBTTagCompound tag) {
            this.ids.clear();
            NBTTagList list = tag.getTagList("Drawn", 8);
            for (int i = 0; i < list.tagCount(); i++) {
                this.ids.add(list.getStringTagAt(i));
            }
        }

        @Override
        public NBTTagCompound writeToNBT(NBTTagCompound tag) {
            NBTTagList list = new NBTTagList();
            for (String id : this.ids) {
                list.appendTag(new net.minecraft.nbt.NBTTagString(id));
            }
            tag.setTag("Drawn", list);
            return tag;
        }
    }
}
