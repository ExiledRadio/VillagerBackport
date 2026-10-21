package com.exiledradio.villagerbackport.village.modern;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * {@code /locate}, answered the way 1.15 answers it.
 *
 * <p>Replaces vanilla's command under the same name. Every structure 1.12.2 can find still works,
 * mods' included, since the search itself is still {@code World.findNearestStructure}. What changes
 * is the reply: 1.15's "The nearest Village is at [x, ~, z] (n blocks away)", with the coordinates in
 * green brackets that put a {@code /tp} to them in the chat box when clicked.
 *
 * <p>It also takes one 1.14 village type at a time - {@code village_plains}, {@code village_desert},
 * {@code village_savanna}, {@code village_taiga}, {@code village_snowy} - which later versions of the
 * game added. Those only find 1.14-style villages, so they need {@code villages.style = MODERN}.
 */
public class LocateCommand extends CommandBase {

    /** 1.12.2's own structure names, as its tab completion lists them. */
    private static final String[] VANILLA = {"Stronghold", "Monument", "Village", "Mansion", "EndCity", "Fortress",
            "Temple", "Mineshaft"};

    /** How far out a typed village search goes, in village grid cells (32 chunks each by default). */
    private static final int MAX_RINGS = 100;

    @Override
    public String getName() {
        return "locate";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "commands.villagerbackport.locate.usage";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length != 1) {
            throw new WrongUsageException(getUsage(sender));
        }
        World world = sender.getEntityWorld();
        BlockPos from = sender.getPosition();

        ModernVillageStart.Type type = villageType(args[0]);
        String name;
        BlockPos found;

        if (type != null) {
            MapGenModernVillage villages = MapGenModernVillage.of(world);
            if (villages == null) {
                throw whyNoModernVillages(world);
            }
            name = args[0].toLowerCase(Locale.ROOT);
            found = villages.findNearest(world, from, type, MAX_RINGS);
        } else {
            // 1.12.2's names are case-sensitive; take the vanilla spelling whatever was typed.
            name = canonical(args[0]);
            found = world.findNearestStructure(name, from, false);
        }

        if (found == null) {
            throw new CommandException("commands.locate.failure", name);
        }

        int distance = MathHelper.floor(Math.sqrt(sq(found.getX() - from.getX()) + sq(found.getZ() - from.getZ())));
        sender.sendMessage(new TextComponentTranslation("commands.villagerbackport.locate.success", name,
                coordinates(found), distance));
    }

    /** 1.15's {@code TextComponentUtils.wrapInSquareBrackets} coordinates: green, click to fill in a teleport. */
    private static ITextComponent coordinates(BlockPos pos) {
        ITextComponent text = new TextComponentString("[" + pos.getX() + ", ~, " + pos.getZ() + "]");
        text.getStyle()
                .setColor(TextFormatting.GREEN)
                .setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,
                        "/tp @s " + pos.getX() + " ~ " + pos.getZ()))
                .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        new TextComponentTranslation("commands.villagerbackport.locate.tooltip")));
        return text;
    }

    private static double sq(int value) {
        return (double) value * (double) value;
    }

    @Nullable
    private static ModernVillageStart.Type villageType(String arg) {
        String lower = arg.toLowerCase(Locale.ROOT);
        if (!lower.startsWith("village_")) {
            return null;
        }
        String type = lower.substring("village_".length());
        if ("snow".equals(type)) {
            type = "snowy";
        }
        for (ModernVillageStart.Type t : ModernVillageStart.Type.values()) {
            if (t.name().toLowerCase(Locale.ROOT).equals(type)) {
                return t;
            }
        }
        return null;
    }

    private static String canonical(String arg) {
        for (String name : VANILLA) {
            if (name.equalsIgnoreCase(arg)) {
                return name;
            }
        }
        return arg;
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args,
                                          @Nullable BlockPos targetPos) {
        if (args.length != 1) {
            return Collections.emptyList();
        }
        List<String> options = new ArrayList<String>();
        Collections.addAll(options, VANILLA);
        for (ModernVillageStart.Type t : ModernVillageStart.Type.values()) {
            options.add("village_" + t.name().toLowerCase(Locale.ROOT));
        }
        return getListOfStringsMatchingLastWord(args, options);
    }

    /**
     * Says why this world has no 1.14 villages to locate, so a player in a world with its own terrain
     * generator (Dregora's OTG, say) learns that is the reason rather than suspecting the mod.
     */
    private static CommandException whyNoModernVillages(World world) {
        if (com.exiledradio.villagerbackport.ModConfig.villages.style != MapGenModernVillage.Style.MODERN) {
            return new CommandException("commands.villagerbackport.locate.legacy");
        }
        if (world instanceof net.minecraft.world.WorldServer) {
            net.minecraft.world.gen.IChunkGenerator generator =
                    ((net.minecraft.world.WorldServer) world).getChunkProvider().chunkGenerator;
            if (generator != null && !(generator instanceof net.minecraft.world.gen.ChunkGeneratorOverworld)) {
                return new CommandException("commands.villagerbackport.locate.othergen", ownerOf(generator.getClass(), world));
            }
        }
        return new CommandException("commands.villagerbackport.locate.nomodern");
    }

    /**
     * @return the name of the mod a generator comes from, or for one of vanilla's own (a superflat
     * world, say) the world type's name
     */
    private static String ownerOf(Class<?> type, World world) {
        try {
            java.io.File source = ModVillagePieces.jarOf(type);
            for (net.minecraftforge.fml.common.ModContainer mod : net.minecraftforge.fml.common.Loader.instance().getModList()) {
                if (mod.getSource() != null && source.equals(mod.getSource().getCanonicalFile())) {
                    return mod.getName();
                }
            }
        } catch (Exception | LinkageError e) {
            // Fall through to the class name.
        }
        return '"' + world.getWorldType().getName() + '"';
    }
}
