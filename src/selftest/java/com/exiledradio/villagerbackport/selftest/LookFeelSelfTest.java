package com.exiledradio.villagerbackport.selftest;

import com.exiledradio.backportcore.presentation.Contribution;
import com.exiledradio.backportcore.presentation.GameVersion;
import com.exiledradio.backportcore.presentation.LoadingRevision;
import com.exiledradio.backportcore.presentation.Presentation;
import com.exiledradio.backportcore.presentation.Splashes;
import com.exiledradio.backportcore.presentation.option.OptionScreen;
import com.exiledradio.backportcore.presentation.option.PresentationOption;
import com.exiledradio.villagerbackport.VillagerBackport;
import com.exiledradio.villagerbackport.lookfeel.LookAndFeel;
import com.exiledradio.villagerbackport.lookfeel.ScrollAccumulator;
import net.minecraft.util.ResourceLocation;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 1.14's look and feel registered with Backport Core, its assets, the scroll arithmetic and the lang overrides. */
public final class LookFeelSelfTest implements SelfTest {

    private static final String ID = VillagerBackport.MOD_ID;

    @Override
    public String name() {
        return "lookfeel";
    }

    @Override
    public void run(Check c) throws Exception {
        registration(c);
        panorama(c);
        splashes(c);
        options(c);
        scrolling(c);
        lang(c);
    }

    private static void registration(Check c) {
        Contribution v114 = Presentation.contribution("1.14");
        Contribution v1141 = Presentation.contribution("1.14.1");
        c.ok(v114 != null && v114.owners().contains(ID), "1.14 is registered by " + ID + ": " + v114);
        c.ok(v1141 != null && v1141.owners().contains(ID), "1.14.1 is registered by " + ID + ": " + v1141);
        c.eq(GameVersion.parse("1.14.1"), Presentation.newest(), "the newest installed version (this mod alone)");
        c.eq(LoadingRevision.GRID_1_14, Presentation.loadingRevision(), "the world-loading screen revision");
    }

    private static void panorama(Check c) throws IOException {
        List<Presentation.Panorama> all = Presentation.panoramas();
        Presentation.Panorama last = all.isEmpty() ? null : all.get(all.size() - 1);
        if (!c.ok(last != null && last.version.equals(GameVersion.parse("1.14")), "the newest panorama is 1.14's: " + all.size())) {
            return;
        }
        for (int i = 0; i < 6; i++) {
            c.eq(new ResourceLocation(ID, "textures/gui/title/background/panorama_" + i + ".png"), last.faces[i], "panorama face " + i);
            int[] size = pngSize(last.faces[i]);
            c.ok(size != null && size[0] == 1024 && size[1] == 1024, "face " + i + " is 1.14.4's 1024x1024 image: " + Arrays.toString(size));
        }
        ResourceLocation overlay = Presentation.panoramaOverlay();
        c.eq(new ResourceLocation(ID, "textures/gui/title/background/panorama_overlay.png"), overlay, "the panorama overlay");
        int[] size = overlay == null ? null : pngSize(overlay);
        c.ok(size != null && size[0] == 16 && size[1] == 128, "1.14.4's overlay is the 16x128 gradient: " + Arrays.toString(size));
    }

    private static final String[] ADDED = {"Rainbow turtle?", "Something funny!", "Look mum, I'm in a splash!", "Ping the human!",
            "10 years of Mining and Crafting!", "{PLAYER} IS YOU"};
    private static final String[] REMOVED = {"Made by Notch!", "The Work of Notch!", "Woo, minecraftforum!", "110813!",
            "Hobo humping slobo babe!"};

    private static void splashes(Check c) throws IOException {
        List<String> added = lines(new ResourceLocation(ID, "texts/splashes_added.txt"));
        List<String> removed = lines(new ResourceLocation(ID, "texts/splashes_removed.txt"));
        c.eq(16, added.size(), "1.14.4 adds this many splashes to 1.13.2's");
        c.eq(5, removed.size(), "1.14.4 removes this many");
        List<String> base = lines("/assets/minecraft/texts/splashes.txt");
        c.ok(base.size() > 300, "1.12.2's splashes.txt is readable: " + base.size());
        List<String> pool = Splashes.pool(base, Presentation.installed(), f -> lines(f));
        for (String s : ADDED) {
            c.ok(pool.contains(s), "the splash pool has 1.14's \"" + s + "\"");
        }
        for (String s : REMOVED) {
            c.ok(base.contains(s) && !pool.contains(s), "the splash pool lost \"" + s + "\"");
        }
        Set<String> expected = new LinkedHashSet<String>(base);
        expected.removeAll(removed);
        expected.addAll(added);
        expected.add("{PLAYER} IS YOU");
        c.eq(expected.size(), pool.size(), "pool size: 1.12.2's set less 1.14's removals plus its additions");
        c.eq("STEVE IS YOU", Splashes.personalize("{PLAYER} IS YOU", "Steve"), "the IS YOU splash names the player in capitals");
    }

    private static void options(Check c) {
        Map<String, PresentationOption> byId = new HashMap<String, PresentationOption>();
        for (PresentationOption o : Presentation.options()) {
            byId.put(o.id(), o);
        }
        PresentationOption[] mine = {LookAndFeel.SCROLL_SENSITIVITY, LookAndFeel.DISCRETE_SCROLLING};
        String[] labels = {"options.mouseWheelSensitivity", "options.discrete_mouse_scroll"};
        for (int i = 0; i < mine.length; i++) {
            PresentationOption o = mine[i];
            c.ok(byId.get(o.id()) == o, o.id() + " is registered");
            c.eq(OptionScreen.MOUSE, o.screen(), o.id() + "'s screen");
            c.eq(OptionScreen.MOUSE, o.screen().resolve(Presentation.newest()), o.id() + " shows on Core's own Mouse Settings at 1.14");
            c.eq(GameVersion.parse("1.14"), o.since(), o.id() + "'s since");
            c.eq(labels[i], o.labelKey(), o.id() + "'s label");
        }
        c.eq("INVERT_MOUSE", LookAndFeel.SCROLL_SENSITIVITY.after(), "Scroll Sensitivity follows Invert Mouse");
        c.eq(LookAndFeel.SCROLL_SENSITIVITY.id(), LookAndFeel.DISCRETE_SCROLLING.after(), "Discrete Scrolling follows Scroll Sensitivity");
        c.eq(0.01, LookAndFeel.SCROLL_SENSITIVITY.min(), "Scroll Sensitivity min");
        c.eq(10.0, LookAndFeel.SCROLL_SENSITIVITY.max(), "Scroll Sensitivity max");
        c.eq(1.0, LookAndFeel.SCROLL_SENSITIVITY.defaultValue(), "Scroll Sensitivity default");
        c.eq("1.00", LookAndFeel.SCROLL_SENSITIVITY.valueText(), "Scroll Sensitivity shows two decimals, as 1.14.4's %.2f");
        c.eq(false, LookAndFeel.DISCRETE_SCROLLING.defaultValue(), "Discrete Scrolling default");
        List<String> mouse = new ArrayList<String>();
        for (PresentationOption o : Presentation.optionsOn(OptionScreen.MOUSE)) {
            mouse.add(o.id());
        }
        c.ok(mouse.indexOf(LookAndFeel.DISCRETE_SCROLLING.id()) == mouse.indexOf(LookAndFeel.SCROLL_SENSITIVITY.id()) + 1,
                "Mouse Settings lists Scroll Sensitivity, then Discrete Scrolling: " + mouse);
    }

    /** 1.14.4's MouseHandler.onScroll arithmetic. */
    private static void scrolling(Check c) {
        c.eq("[1, -1]", sums(1.0, -1.0), "one notch each way moves one slot each way");
        c.eq("[0, 0, 0, 1]", sums(0.25, 0.25, 0.25, 0.25), "a quarter-notch sensitivity needs four notches per slot");
        c.eq("[0, 0]", sums(0.5, -0.5), "a change of direction starts the sum again");
        c.eq("[3]", sums(3.0), "three notches at once (changeCurrentItem then moves one slot, as 1.14.4's swapPaint)");
        c.eq(1.0, ScrollAccumulator.amount(1.0), "one notch at the default sensitivity is one");
        LookAndFeel.SCROLL_SENSITIVITY.set(0.5);
        c.eq(0.5, ScrollAccumulator.amount(1.0), "Scroll Sensitivity 0.50 halves it");
        LookAndFeel.DISCRETE_SCROLLING.set(true);
        c.eq(0.5, ScrollAccumulator.amount(0.1), "Discrete Scrolling counts a small movement as a whole notch");
        LookAndFeel.DISCRETE_SCROLLING.reset();
        LookAndFeel.SCROLL_SENSITIVITY.reset();
    }

    private static String sums(double... amounts) {
        ScrollAccumulator a = new ScrollAccumulator();
        int[] out = new int[amounts.length];
        for (int i = 0; i < amounts.length; i++) {
            out[i] = a.add(amounts[i]);
        }
        return Arrays.toString(out);
    }

    private static final String[][] LANG = {
            {"options.mouseWheelSensitivity", "Scroll Sensitivity"},
            {"options.discrete_mouse_scroll", "Discrete Scrolling"},
            {"options.chat.opacity", "Chat Text Opacity"},
            {"menu.paused", "Game paused"},
    };

    private static final String[] LANGUAGES = {"de_de", "es_ar", "es_es", "es_mx", "es_uy", "es_ve", "fr_fr", "it_it", "ja_jp",
            "ko_kr", "nl_nl", "pl_pl", "pt_br", "ru_ru", "tr_tr", "uk_ua", "zh_cn", "zh_tw"};

    private static void lang(Check c) throws IOException {
        Map<String, String> en = langFile("en_us");
        for (String[] kv : LANG) {
            c.eq(kv[1], en.get(kv[0]), "en_us " + kv[0]);
        }
        for (String code : LANGUAGES) {
            Map<String, String> l = langFile(code);
            int have = 0;
            for (String[] kv : LANG) {
                if (l.get(kv[0]) != null && !l.get(kv[0]).isEmpty()) {
                    have++;
                }
            }
            c.eq(LANG.length, have, code + " has 1.14's option and reworded vanilla keys");
        }
    }

    // ------------------------------------------------------------------------------------------

    private static Map<String, String> langFile(String code) throws IOException {
        Map<String, String> out = new HashMap<String, String>();
        for (String line : lines(new ResourceLocation(ID, "lang/" + code + ".lang"))) {
            int eq = line.indexOf('=');
            if (eq > 0 && !line.startsWith("#")) {
                out.put(line.substring(0, eq), line.substring(eq + 1));
            }
        }
        return out;
    }

    private static List<String> lines(ResourceLocation rl) throws IOException {
        return lines("/assets/" + rl.getNamespace() + "/" + rl.getPath());
    }

    private static List<String> lines(String path) throws IOException {
        InputStream in = LookFeelSelfTest.class.getResourceAsStream(path);
        if (in == null) {
            throw new IOException("not on the classpath: " + path);
        }
        List<String> out = new ArrayList<String>();
        BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        try {
            String line;
            while ((line = r.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    out.add(line);
                }
            }
        } finally {
            r.close();
        }
        return out;
    }

    private static int[] pngSize(ResourceLocation rl) throws IOException {
        InputStream in = LookFeelSelfTest.class.getResourceAsStream("/assets/" + rl.getNamespace() + "/" + rl.getPath());
        if (in == null) {
            return null;
        }
        byte[] b = read(in);
        if (b.length < 24 || b[1] != 'P' || b[2] != 'N' || b[3] != 'G') {
            return null;
        }
        return new int[] {readInt(b, 16), readInt(b, 20)};
    }

    private static int readInt(byte[] b, int at) {
        return (b[at] & 0xFF) << 24 | (b[at + 1] & 0xFF) << 16 | (b[at + 2] & 0xFF) << 8 | (b[at + 3] & 0xFF);
    }

    private static byte[] read(InputStream in) throws IOException {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
            return out.toByteArray();
        } finally {
            in.close();
        }
    }
}
