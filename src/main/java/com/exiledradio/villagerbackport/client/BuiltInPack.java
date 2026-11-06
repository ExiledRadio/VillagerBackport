package com.exiledradio.villagerbackport.client;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nullable;

import com.exiledradio.villagerbackport.VillagerBackport;

import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.data.IMetadataSection;
import net.minecraft.client.resources.data.MetadataSerializer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * A resource pack that lives inside this mod's jar, under {@code villagerbackport_packs/<name>/}.
 *
 * <p>Forge only serves a mod's {@code assets/} folder, and serves all of it all the time. These packs
 * sit outside it so they can be switched on and off. Each one carries an {@code index.txt} listing its
 * files, written by {@code tools/modern-textures/generate.py}. The game asks every pack about every
 * texture, model and sound it loads, and answering from a set is much cheaper than asking the class
 * loader, which would search every mod jar in the pack for each miss.
 */
@SideOnly(Side.CLIENT)
final class BuiltInPack implements IResourcePack {

    private final String name;
    private final String root;
    private final Set<String> files = new HashSet<>();
    private final Set<String> domains = new HashSet<>();

    BuiltInPack(String name) {
        this.name = name;
        this.root = "/villagerbackport_packs/" + name + "/";
        try (InputStream in = open("index.txt")) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("assets/")) {
                    files.add(line);
                    int slash = line.indexOf('/', 7);
                    if (slash > 7) {
                        domains.add(line.substring(7, slash));
                    }
                }
            }
        } catch (IOException e) {
            VillagerBackport.LOGGER.error("Built-in texture pack '{}' is missing its index; it will do nothing.", name, e);
        }
    }

    int size() {
        return files.size();
    }

    private static String path(ResourceLocation location) {
        return "assets/" + location.getNamespace() + "/" + location.getPath();
    }

    private InputStream open(String file) throws IOException {
        InputStream in = BuiltInPack.class.getResourceAsStream(root + file);
        if (in == null) {
            throw new FileNotFoundException(root + file);
        }
        return in;
    }

    @Override
    public InputStream getInputStream(ResourceLocation location) throws IOException {
        String file = path(location);
        if (!files.contains(file)) {
            throw new FileNotFoundException(file);
        }
        return open(file);
    }

    @Override
    public boolean resourceExists(ResourceLocation location) {
        return files.contains(path(location));
    }

    @Override
    public Set<String> getResourceDomains() {
        return domains;
    }

    /** The default packs are never listed or described anywhere, so there is no pack.mcmeta to read. */
    @Nullable
    @Override
    public <T extends IMetadataSection> T getPackMetadata(MetadataSerializer serializer, String section) {
        return null;
    }

    @Override
    public BufferedImage getPackImage() throws IOException {
        throw new FileNotFoundException("pack.png");
    }

    @Override
    public String getPackName() {
        return VillagerBackport.MOD_ID + ":" + name;
    }
}
