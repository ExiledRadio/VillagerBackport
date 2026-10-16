package com.exiledradio.villagerbackport;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;

/**
 * Configuration, defaulting to 1.14's own numbers.
 *
 * <p>Everything here is server-side behaviour, so the values that matter are the ones on the
 * host. Nothing is synced to clients yet because nothing in this phase is visible client-side -
 * the client only ever sees the resulting trade list, which vanilla already sends.
 */
@Config(modid = VillagerBackport.MOD_ID, name = "villagerbackport")
public final class ModConfig {

    @Config.LangKey("config.villagerbackport.restock")
    @Config.Comment("How villagers replenish their trades.")
    public static final Restock restock = new Restock();

    @Config.LangKey("config.villagerbackport.pricing")
    @Config.Comment("Supply-and-demand pricing and villager experience.")
    public static final Pricing pricing = new Pricing();

    @Config.LangKey("config.villagerbackport.trades")
    @Config.Comment("Which trades villagers offer.")
    public static final Trades trades = new Trades();

    @Config.LangKey("config.villagerbackport.jobs")
    @Config.Comment("Workstations villagers go to work at.")
    public static final Jobs jobs = new Jobs();

    @Config.LangKey("config.villagerbackport.workstations")
    @Config.Comment("What the workstation blocks accept and produce.")
    public static final Workstations workstations = new Workstations();

    @Config.LangKey("config.villagerbackport.villages")
    @Config.Comment("Which villages generate: 1.14's or 1.12.2's.")
    public static final Villages villages = new Villages();

    @Config.LangKey("config.villagerbackport.homes")
    @Config.Comment("Beds, sleeping, breeding and iron golems.")
    public static final Homes homes = new Homes();

    @Config.LangKey("config.villagerbackport.display")
    @Config.Comment("What the trade screen shows.")
    public static final Display display = new Display();

    @Config.LangKey("config.villagerbackport.compat")
    @Config.Comment("Getting along with other mods that add the same blocks.")
    public static final Compat compat = new Compat();

    private ModConfig() {
    }

    public static final class Trades {

        @Config.Comment({
                "Which trades new villagers get.",
                "",
                "MODERN: 1.14's trade tables. Each level offers two trades picked at random from that",
                "level's list, so two farmers can sell different things. 1.14 items 1.12.2 does not",
                "have (crossbows, campfires, lanterns, sweet berries, scutes) are only offered when a",
                "mod that adds them is installed.",
                "LEGACY: 1.12.2's own trades, as each career registers them - including any trades",
                "another mod has added to a career.",
                "",
                "Villagers someone has already traded with keep the trades they have, and keep",
                "unlocking from the table they started on, whatever this is set to. Careers another",
                "mod adds always use their own trades."
        })
        public com.exiledradio.villagerbackport.trade.TradeTables.Table table =
                com.exiledradio.villagerbackport.trade.TradeTables.Table.MODERN;

        @Config.Comment({
                "Rebuild the trades of villagers nobody has traded with yet, so they match the table",
                "above.",
                "",
                "This is how villages that existed before switching tables get the new trades. A",
                "villager anyone has traded with, even once, is never touched."
        })
        public boolean rebuildUntradedVillagers = true;
    }

    public static final class Restock {

        @Config.Comment({
                "Master switch for timed restocking.",
                "When false the mod does nothing and villagers use vanilla 1.12.2 behaviour,",
                "where a restock only happens after a lucky roll on a successful trade and a",
                "villager whose trades all run out can lock permanently."
        })
        public boolean enabled = true;

        @Config.Comment({
                "Restocks a villager may perform per day. 1.14's value is 2.",
                "This is the main throttle on trade farming: raising it lets a player drain a",
                "villager more times per day, and 0 disables restocking as surely as the master",
                "switch does."
        })
        @Config.RangeInt(min = 0, max = 64)
        public int maxRestocksPerDay = 2;

        @Config.Comment({
                "Ticks a villager must wait between restocks. 1.14's value is 2400 (two minutes).",
                "Lowering this makes villagers replenish faster within their daily allowance."
        })
        @Config.RangeInt(min = 20, max = 24000)
        public int cooldownTicks = 2400;

        @Config.Comment({
                "Ticks of elapsed game time after which a villager's daily restock allowance is",
                "refunded, independent of the day/night cycle. 1.14's value is 12000 (half a day).",
                "This is the fallback that keeps villagers working on worlds where the daylight",
                "cycle is frozen or the time has been moved by a command."
        })
        @Config.RangeInt(min = 1200, max = 240000)
        public int dailyResetTicks = 12000;

        @Config.Comment({
                "Only let villagers restock during working hours.",
                "",
                "This is what makes 'twice a day' mean twice a day. 1.14 restocks from inside the task",
                "that has a villager work at its job site, and that task only runs during the WORK part",
                "of the villager schedule - so the daily allowance resets far more often than a villager",
                "is ever in a position to spend it. Without the window, a villager stood at its",
                "workstation can spend both restocks every time the allowance comes back, right through",
                "the night, which is several times a day rather than twice."
        })
        public boolean workHoursOnly = true;

        @Config.Comment({
                "Play a sound each time a villager uses its workstation, about every 15 seconds",
                "while it is at work, whether or not it restocks.",
                "",
                "1.14 gives each profession its own work sound and plays it as the villager uses its",
                "job site. The sound is taken from the workstation rather than the profession here,",
                "which comes to the same thing - a librarian is the villager at the lectern - and it",
                "means a workstation this mod did not define still makes a noise of the right material."
        })
        public boolean playWorkSound = true;

        @Config.Comment({
                "When the working day starts and ends, as time of day in ticks.",
                "",
                "1.14's villager schedule turns WORK on at 2000 and off again at 9000, where the",
                "villagers gather instead. Dawn is 0 and dusk is 12000. A start later than the end",
                "makes a window that runs through midnight, for a pack that has moved the day around."
        })
        @Config.RangeInt(min = 0, max = 23999)
        public int workStartTime = 2000;

        @Config.RangeInt(min = 0, max = 23999)
        public int workEndTime = 9000;
    }

    public static final class Pricing {

        @Config.Comment({
                "Adjust prices by demand, the way 1.14 does.",
                "A trade used heavily between restocks gets more expensive; one left alone drifts",
                "back toward its base price. Demand never discounts below the base price - in 1.14",
                "discounts come from gossip and Hero of the Village, which are not implemented yet.",
                "Turning this off leaves every trade at the price its author set."
        })
        public boolean demandPricing = true;

        @Config.Comment({
                "How sharply demand moves prices. 1.14 sets this per-trade, mostly 0.05.",
                "The surcharge is floor(basePrice * demand * this), so at 0.05 a trade sold out",
                "repeatedly climbs roughly 5% of its base price per point of accumulated demand.",
                "Raising it makes popular trades expensive much faster."
        })
        @Config.RangeDouble(min = 0.0D, max = 2.0D)
        public double priceMultiplier = 0.05D;

        @Config.Comment({
                "How sharply demand and reputation move the price of premium trades. 1.14's value",
                "is 0.2.",
                "",
                "1.14 stores a multiplier per trade and uses exactly two: 0.05 for ordinary trades and",
                "0.2 for the premium ones. 1.12.2 recipes have no such field, so a trade counts as",
                "premium here when what it sells is one of 1.14's premium items: enchanted books,",
                "explorer maps, enchanted gear other than the iron sword and bow, and plain iron and",
                "chainmail armour, shields, the iron axe, stone tools, the diamond hoe, saddles and",
                "leather armour."
        })
        @Config.RangeDouble(min = 0.0D, max = 2.0D)
        public double premiumPriceMultiplier = 0.2D;

        @Config.Comment({
                "Let villagers form opinions of players, and price accordingly.",
                "",
                "1.14's gossip: trading with a villager, hurting one, killing one in front of others",
                "and curing one of zombiehood all leave a mark, and a villager gives a discount in",
                "proportion to what it thinks of you. Opinions wear off daily and spread between",
                "villagers who are stood near each other.",
                "",
                "It runs both ways, as in 1.14: a villager that watched you kill its neighbours will",
                "charge you more, and one you cured of zombiehood will charge you less. Only the demand",
                "part of the price is floored at zero; this part is signed."
        })
        public boolean gossipEnabled = true;

        @Config.Comment({
                "Let villagers repeat their opinions to each other.",
                "",
                "1.14 does this when they gather at the meeting point. An opinion loses strength each",
                "time it is retold and a villager keeps only the strongest version it has heard - so a",
                "killing travels but arrives weaker, and trading barely travels at all."
        })
        public boolean gossipSpreads = true;

        @Config.Comment({
                "How close two villagers must stand to swap gossip, in blocks. 1.14's value is",
                "2.24, the square root of 5: they have to walk right up to each other.",
                "",
                "Each villager talks at most once a minute, and hears ten weighted picks from what",
                "its partner knows rather than all of it, so news travels round a village over a",
                "day or so rather than in minutes."
        })
        @Config.RangeDouble(min = 1.0D, max = 32.0D)
        public double gossipRange = 2.24D;

        @Config.Comment({
                "Ceiling on how far demand can accumulate in either direction. 0 means no ceiling,",
                "which is 1.14's behaviour and the default.",
                "",
                "With no ceiling a trade that is sold out every day keeps getting dearer until it",
                "reaches a full stack, and one left alone for weeks builds up a long way the other",
                "way, so it takes as long to start rising again. Set a number to bound both: at 128,",
                "a 1-emerald trade at the ordinary rate tops out at 7."
        })
        @Config.RangeInt(min = 0, max = 100000)
        public int maxDemand = 0;

        @Config.Comment({
                "Scales the experience villagers earn from trading. 1.0 uses 1.14's own values.",
                "",
                "1.14 sets experience per trade, based on the tier the trade unlocked at and which",
                "way it goes. Selling goods to a villager pays 2/10/20/30/30 across tiers one to",
                "five; buying goods from one pays 1/5/10/15/30. Those are the values used here.",
                "",
                "Scaling with the tier is what keeps progress steady, because each level costs more",
                "than the last - the thresholds are 10, 70, 150 and 250. Raise this to level",
                "villagers faster, lower it to make mastery a longer project."
        })
        @Config.RangeDouble(min = 0.0D, max = 10.0D)
        public double xpMultiplier = 1.0D;

        @Config.Comment({
                "Unlock new trades when a villager levels up, as 1.14 does, instead of when it",
                "restocks.",
                "",
                "1.12.2 appends a whole new tier of trades roughly every time a villager is traded",
                "with - a first trade or one bulk trade is enough. That runs on trade volume alone,",
                "so with an experience level also on screen the two disagree: the villager stocks",
                "master-tier goods while still reading as Novice.",
                "",
                "Leaving this on makes the level mean what it says. Turning it off restores 1.12.2's",
                "behaviour, where the level is decorative and trades unlock on their own schedule.",
                "Restocking is unaffected either way."
        })
        public boolean levelGatedTrades = true;

        @Config.Comment({
                "How many times a trade the player SELLS into can be used before running dry, for",
                "trades 1.14 has no figure of its own for. 1.12.2 defaults to 7. Set 0 to leave",
                "selling trades as their author made them, including the ones below.",
                "",
                "Trades that match one of 1.14's are set to 1.14's own stock for that trade instead:",
                "16 for everyday goods like crops, wool and meat, 12 for valuable ones like iron,",
                "gold, diamonds and books.",
                "",
                "This is what makes the experience values above add up. They are 1.14's per-trade",
                "amounts, calibrated against 1.14's stock - paying them over less than half the",
                "trades means a villager sells out with the next level still out of reach.",
                "",
                "Trades this figure applies to are only ever raised to it, so a mod that",
                "deliberately made something scarce is not overridden."
        })
        @Config.RangeInt(min = 0, max = 64)
        public int sellingTradeStock = 16;

        @Config.Comment({
                "How many times a trade the player BUYS from can be used before running dry, for",
                "trades 1.14 has no figure of its own for. 1.14's usual value is 12; 1.12.2 defaults",
                "to 7. Set 0 to leave buying trades untouched, including the ones below.",
                "",
                "Trades that match one of 1.14's are set to 1.14's own stock for that trade instead:",
                "3 for every enchanted tool, weapon and piece of armour, 16 for a few everyday",
                "goods like bread and wool, 12 for the rest."
        })
        @Config.RangeInt(min = 0, max = 64)
        public int buyingTradeStock = 12;
    }

    public static final class Jobs {

        @Config.Comment({
                "Villagers walk to a workstation to restock, as they do in 1.14.",
                "Turning this off removes the behaviour entirely - no seeking, no gating."
        })
        public boolean enabled = true;

        @Config.Comment({
                "Require a villager to be at its workstation before it can restock.",
                "",
                "This is what makes workstations matter rather than being decoration. It fails open",
                "on purpose: a career with no workstation block registered is never gated, so a",
                "villager can not end up permanently sold out waiting at a block that does not",
                "exist. As workstation blocks are added and mapped below, their careers become",
                "gated on their own."
        })
        public boolean requireJobSite = true;

        @Config.Comment({
                "Villagers start with no profession and take one from a workstation, as in 1.14.",
                "",
                "A villager with no job has no trades at all. Place a workstation near one and it",
                "will claim it and take that job; two villagers can never share a workstation.",
                "",
                "Losing the workstation only costs a villager its job if it has never been traded",
                "with - 1.14's exact rule is no experience and still at the first level. Trade with",
                "one even once and its profession is permanent; it simply cannot restock until it",
                "finds another workstation.",
                "",
                "Villagers that already have trades or experience when this is first switched on",
                "are left alone, so installing the mod on an existing world does not put anyone out",
                "of work."
        })
        public boolean professionsFromWorkstations = true;

        @Config.Comment({
                "Also gate villagers whose career HAS a workstation but that have not found one.",
                "",
                "On by default, which is 1.14's rule: a villager only restocks at its own workstation,",
                "so a village with no lectern anywhere has librarians that never restock. Turn it off",
                "for a world built before this mod was installed, where villages may be missing the",
                "blocks; off, villagers restock freely until they find one.",
                "",
                "Careers this mod has no workstation for, such as ones added by other mods, are never",
                "gated either way."
        })
        public boolean strictJobSite = true;

        @Config.Comment({
                "How far a villager will look for an unclaimed workstation, in blocks, in every",
                "direction - up and down as far as sideways, as in 1.14.",
                "",
                "48 is 1.14's own reach, and this reaches it the same way 1.14 does: the",
                "workstations in each chunk are indexed, and a villager queries that index instead of",
                "reading the world. Searching the world directly costs its volume, which is what kept",
                "this at 12 before and left villagers in a large village permanently jobless - the",
                "nearest free workstation was simply further off than they could afford to look.",
                "",
                "Reach is close to free now: a search at 48 looks at 49 chunks and reads a short list",
                "from each. Raising this costs far less than it used to."
        })
        @Config.RangeInt(min = 4, max = 128)
        public int searchRadius = 48;

        @Config.Comment({
                "How close a villager must be to its workstation to count as working at it.",
                "",
                "Measured from the villager to the middle of the block, so the default of 1.75 means",
                "standing against it - straight on or diagonally, on the same level or one step up or",
                "down. A block with so much as a one block gap to it scores just over 2 and does not",
                "count, which is what keeps a villager from working through a wall.",
                "",
                "Raising this past 2 lets villagers work at a distance, walls included."
        })
        @Config.RangeDouble(min = 1.0D, max = 8.0D)
        public double workingDistance = 1.75D;

        @Config.Comment({
                "Let villagers path as far as they are allowed to look for work.",
                "",
                "A navigator will not look for a path further than the entity's follow range, and a",
                "villager's is about sixteen blocks - so without this a villager cannot walk to, or",
                "even be asked about, anything further off than that. It claims a workstation across",
                "the village, gets a path that stops short, and stands at the end of it.",
                "",
                "The extra reach is lent for the one path being worked out and taken straight back.",
                "It used to be left in place, which was a bad mistake: follow range is what every",
                "path an entity asks for is limited by, so a villager kept at the larger figure also",
                "searched a far larger volume every time it wandered, went indoors or fled something.",
                "In a village of any size that alone made everything move in jerks.",
                "",
                "Turn it off to leave villagers exactly as vanilla pathfinds them, in which case the",
                "search radius is best lowered to about 16 to match what they can actually reach."
        })
        public boolean extendPathfindingRange = true;

        @Config.Comment("How fast a villager walks to work.")
        @Config.RangeDouble(min = 0.1D, max = 2.0D)
        public double walkSpeed = 0.6D;

        @Config.Comment({
                "Which blocks each career works at, as 'modid:block=career', one block per line.",
                "",
                "Career names are 1.12.2's own, which abbreviates three of them: 'armor', 'weapon'",
                "and 'tool' are the armorer, weaponsmith and toolsmith. The others are farmer,",
                "fisherman, shepherd, fletcher, librarian, cartographer, cleric, butcher, leather,",
                "mason and nitwit.",
                "",
                "A career can have as many workstations as you like: give it one line per block,",
                "and its villagers work at whichever they reach first. 'minecraft:bookshelf=librarian'",
                "added beside the lectern line makes both employ a librarian. A block can only",
                "belong to one career, though - if two lines name the same block, the first wins.",
                "",
                "Entries naming a block that is not installed are ignored, so pointing a career at",
                "another mod's block is safe. Future MC's workstations do not need listing here;",
                "see compat.futureMcWorkstations."
        })
        public String[] workstations = {
                // This mod's own workstations, one per 1.14 profession.
                "villagerbackport:lectern=librarian",
                "villagerbackport:barrel=fisherman",
                "villagerbackport:composter=farmer",
                "villagerbackport:cartography_table=cartographer",
                "villagerbackport:fletching_table=fletcher",
                "villagerbackport:smithing_table=tool",
                "villagerbackport:loom=shepherd",
                "villagerbackport:blast_furnace=armor",
                "villagerbackport:smoker=butcher",
                "villagerbackport:grindstone=weapon",
                "villagerbackport:stonecutter=mason",

                // The two 1.12.2 already had, which 1.14 uses for the same jobs.
                "minecraft:brewing_stand=cleric",
                "minecraft:cauldron=leather"
        };
    }

    public static final class Villages {

        @Config.Comment({
                "Which villages generate.",
                "",
                "MODERN: 1.14's villages, built from 1.14's own structure files - plains, desert,",
                "savanna, taiga and snowy villages, with the workstations, beds and bells inside the",
                "houses where 1.14 puts them, and the occasional abandoned zombie village. Blocks",
                "1.12.2 does not have are swapped for the nearest it does.",
                "LEGACY: 1.12.2's villages, with workstations put into the existing houses.",
                "",
                "MODERN needs the vanilla overworld generator to lay a village out. In a world made",
                "by another generator villages stay LEGACY.",
                "",
                "Only affects villages generated after the change. Villages already on disk keep",
                "the layout they were generated with."
        })
        @Config.RequiresMcRestart
        public com.exiledradio.villagerbackport.village.modern.MapGenModernVillage.Style style =
                com.exiledradio.villagerbackport.village.modern.MapGenModernVillage.Style.MODERN;

        @Config.Comment({
                "In LEGACY villages, put workstations into the houses as they generate: a lectern in",
                "the library, a smoker at the butcher's, a blast furnace and smithing table at the",
                "blacksmith's, a brewing stand in the church, a composter beside each field, a bell at",
                "the well, and one at random in each small house.",
                "",
                "1.12.2's village houses have no workstations at all, so without this a 1.12.2 village",
                "has almost nowhere for a villager to work."
        })
        public boolean legacyHouseWorkstations = true;

        @Config.Comment({
                "How often each workstation is chosen relative to the others, as 'modid:block=weight'.",
                "",
                "Anything not listed weighs 1. A weight of 2 means picked about twice as often, which",
                "is a noticeable lean rather than a takeover - with a dozen workstations in the pool a",
                "weight of 2 is roughly one in seven rather than one in thirteen.",
                "",
                "The lectern is weighted up because a librarian is the trade most players actually",
                "want. Raise a weight to make that job commoner in new villages, or add an entry at 1",
                "to bring one back to level."
        })
        public String[] workstationWeights = {
                "villagerbackport:lectern=2",
        };

        @Config.Comment({
                "Workstations that may not be built into a village, as 'modid:block'.",
                "",
                "The pool is otherwise every block the workstation mapping above names, so the",
                "blocks a village is built with and the blocks a villager will take a job at are",
                "always the same set. Use this where a pack would rather a particular job were not",
                "handed out for free."
        })
        public String[] excludedWorkstations = {};
    }

    public static final class Workstations {

        @Config.Comment({
                "Ringing a bell makes nearby villagers glow.",
                "",
                "1.14 highlights raiders instead, so a village under attack can see where the",
                "attackers are. 1.12.2 has neither raids nor illagers, which would leave the bell",
                "with nothing to do - so it highlights villagers, which keeps the mechanic and is",
                "arguably more useful: it finds the one you were looking for."
        })
        public boolean bellHighlightsVillagers = true;

        @Config.Comment("How far a bell reaches, in blocks. 1.14 uses 32.")
        @Config.RangeDouble(min = 4.0D, max = 128.0D)
        public double bellRadius = 32.0D;

        @Config.Comment("How long villagers glow for, in ticks. 60 is three seconds.")
        @Config.RangeInt(min = 20, max = 1200)
        public int bellGlowTicks = 60;

        @Config.Comment({
                "Extra things a composter accepts, beyond 1.14's own list.",
                "",
                "Each entry is 'what=chance', where chance is how likely it is to raise the fill",
                "level. 1.14 bands its own at 0.3, 0.5, 0.65, 0.85 and 1.0, roughly by how much",
                "plant matter is in the thing.",
                "",
                "'what' is either an ore dictionary name - 'oredict:treeLeaves' - or an item, as",
                "'modid:name' or 'modid:name:meta'. Ore dictionary entries are the useful ones for",
                "mod support: they pick up every mod's leaves, saplings and crops at once without",
                "naming any of them, so Dynamic Trees, Better Nether and Rustic are covered by the",
                "defaults below without this mod knowing they exist.",
                "",
                "Entries naming something that is not installed are ignored."
        })
        public String[] extraCompostables = {
                "oredict:treeLeaves=0.3",
                "oredict:treeSapling=0.3",
                "oredict:seedAny=0.3",
                "oredict:listAllseed=0.3",
                "oredict:sugarcane=0.5",
                "oredict:vine=0.5",
                "oredict:crop=0.65",
                "oredict:listAllfruit=0.65",
                "oredict:listAllveggie=0.65",
                "oredict:listAllgrain=0.65",
                "oredict:flower=0.65",
                "oredict:listAllflower=0.65",
                "oredict:blockCactus=0.5",
                "oredict:mushroom=0.65",
        };

        @Config.Comment({
                "Work out what the stonecutter can cut by reading the crafting recipes already in",
                "the game.",
                "",
                "Any recipe that turns some number of one stone block into stairs, slabs, walls or",
                "another whole block describes a shape a saw could cut, so reading those backwards",
                "produces the whole list - vanilla's and every mod's - without naming a single mod.",
                "That is what makes Chisel, Quark, Rustic and anything else work here without a",
                "compatibility patch.",
                "",
                "Recipes are only accepted when they cannot be turned into a profit: inputs must be",
                "whole blocks of rock, and whole-block results must divide exactly and never yield",
                "more than they took. Turn this off to use only the entries listed below.",
                "",
                "The list is built once, the first time a stonecutter is opened."
        })
        public boolean deriveStonecutterRecipes = true;

        @Config.Comment({
                "Extra cuts, added on top of whatever was worked out above.",
                "",
                "Each entry is 'input=output', where both are 'modid:name' or 'modid:name:meta', and",
                "the output may end with '*count' to produce more than one - '*2' is what slabs use.",
                "",
                "The defaults are the four chiseled blocks 1.14 cuts straight from their block. They",
                "are listed here rather than worked out because 1.12.2 crafts them from slabs, and",
                "deriving from a slab recipe would halve the cost of everything made that way.",
                "",
                "Entries naming something that is not installed are ignored."
        })
        public String[] extraStonecutterRecipes = {
                "minecraft:stone=minecraft:stonebrick:3",
                "minecraft:stonebrick=minecraft:stonebrick:3",
                "minecraft:sandstone=minecraft:sandstone:1",
                "minecraft:red_sandstone=minecraft:red_sandstone:1",
                "minecraft:quartz_block=minecraft:quartz_block:1",
        };

        @Config.Comment({
                "Work out what the fletching table can cut the same way, but for wood.",
                "",
                "1.14's fletching table does nothing at all - it gives a villager a job and has no",
                "use of its own. This makes it the stonecutter's twin: a log becomes planks, planks",
                "become stairs and slabs, in one click and without the offcuts a crafting grid would",
                "have given you.",
                "",
                "Reading the recipes backwards works as well on wood as on stone, and covers every",
                "mod's wood without naming any of them."
        })
        public boolean deriveFletchingRecipes = true;

        @Config.Comment({
                "Extra cuts for the fletching table, in the same 'input=output' form as above.",
                "",
                "The defaults are the shapes a crafting grid makes out of planks at a loss - a",
                "trapdoor costs six planks for two, a fence gate needs sticks - which the derivation",
                "will not take because it only keeps cuts that break even.",
                "",
                "Entries naming something that is not installed are ignored."
        })
        public String[] extraFletchingRecipes = {
                "minecraft:planks=minecraft:trapdoor",
                "minecraft:planks:1=minecraft:trapdoor",
                "minecraft:planks:2=minecraft:trapdoor",
                "minecraft:planks:3=minecraft:trapdoor",
                "minecraft:planks:4=minecraft:trapdoor",
                "minecraft:planks:5=minecraft:trapdoor",
        };

        @Config.Comment({
                "Blocks the fletching table will not accept, whatever the rules decided.",
                "Each entry is 'modid:name' or 'modid:name:meta'."
        })
        public String[] blockedFletchingInputs = {};

        @Config.Comment({
                "Blocks the stonecutter will not accept, whatever the rules above decided.",
                "",
                "Each entry is 'modid:name' or 'modid:name:meta'. Use this if a mod's recipes make",
                "the saw offer something a pack would rather it did not."
        })
        public String[] blockedStonecutterInputs = {};

        @Config.Comment({
                "Make the loom the only way to apply a banner pattern.",
                "",
                "1.14 removed patterning from the crafting table outright, so this is what exact",
                "1.14 behaviour looks like. It is off by default because it takes something away:",
                "an existing world's players and any pack recipe built on the crafting route would",
                "both stop working the moment it is turned on.",
                "",
                "The loom itself works either way - this only decides whether the old route also",
                "still does. Duplicating a banner in a crafting table is untouched, as it is in 1.14."
        })
        public boolean loomOnlyBannerPatterns = false;
    }

    public static final class Compat {

        @Config.Comment({
                "Let villagers work at Future MC's workstations as well as this mod's own.",
                "",
                "Future MC adds ten of the same job blocks - barrel, composter, cartography table,",
                "fletching table, smithing table, loom, blast furnace, smoker, grindstone and",
                "stonecutter. With this on, each employs the same villager its twin here does, so",
                "a fisherman takes either barrel. Does nothing unless Future MC is installed.",
                "",
                "These are added on top of jobs.workstations. A line there that names one of",
                "Future MC's blocks takes priority over this."
        })
        public boolean futureMcWorkstations = true;

        @Config.Comment({
                "Which copy of each block stays visible when Future MC is installed too.",
                "",
                "Both mods add the same eleven blocks with the same recipes, which puts two of",
                "everything in JEI and leaves two recipes competing for each crafting grid. This",
                "picks one of each pair to keep:",
                "",
                "  KEEP_OURS       keep this mod's blocks, hide Future MC's",
                "  KEEP_FUTURE_MC  keep Future MC's blocks, hide this mod's",
                "  SHOW_BOTH       hide nothing",
                "",
                "A hidden block still exists and still works - anything already placed is",
                "untouched, and villagers still take jobs at it. It loses its crafting recipe and",
                "its place in JEI and the creative menu, and villages are no longer built with it.",
                "",
                "Two exceptions. The smithing table kept is always Future MC's, because theirs",
                "upgrades to netherite and this mod's is decorative. And the lectern is never",
                "hidden, because Future MC has none.",
                "",
                "Does nothing unless Future MC is installed."
        })
        @Config.RequiresMcRestart
        public com.exiledradio.villagerbackport.compat.FutureMc.Duplicates futureMcDuplicates =
                com.exiledradio.villagerbackport.compat.FutureMc.Duplicates.KEEP_OURS;
    }

    public static final class Homes {

        @Config.Comment({
                "Grown villagers gather at the village bell in the afternoon, as in 1.14.",
                "",
                "They head for the nearest bell within 48 blocks, drift around near it and walk up",
                "to each other to mingle, which is also when gossip spreads fastest. A villager with",
                "no bell in reach carries on wandering."
        })
        public boolean gatherAtBell = true;

        @Config.Comment("When the gathering starts, in ticks of the day. 1.14's value is 9000 (15:00).")
        @Config.RangeInt(min = 0, max = 23999)
        public int gatherStartTime = 9000;

        @Config.Comment("When the gathering ends, in ticks of the day. 1.14's value is 11000 (17:00).")
        @Config.RangeInt(min = 0, max = 23999)
        public int gatherEndTime = 11000;

        @Config.Comment({
                "Baby villagers play, as in 1.14: from 09:00 to 12:00 and 16:00 to 18:00 they chase",
                "each other around the village and bounce on beds.",
                "",
                "Replaces 1.12.2's own baby play, which had them follow each other at any hour."
        })
        public boolean babiesPlay = true;


        @Config.Comment({
                "Master switch for beds. When false nothing here happens and villagers behave as",
                "1.12.2 built them: doors decide the village, and nobody sleeps."
        })
        public boolean enabled = true;

        @Config.Comment({
                "Villagers claim a bed and sleep in it at night, as they do from 1.14 onward.",
                "",
                "1.12.2 has no sleeping mob of any kind, so this is built rather than enabled: the",
                "villager walks to its bed, lies in it until morning, and is drawn lying down.",
                "",
                "It is not decoration. From 1.14 a villager only counts towards an iron golem if it",
                "has slept within the last day, so switching this off switches golem spawning off",
                "with it - see golemSpawning."
        })
        public boolean sleepAtNight = true;

        @Config.Comment({
                "Beds rather than doors decide whether a village can grow.",
                "",
                "1.12.2 counts doors: villagers breed while the population is under 35% of the door",
                "count, which is why a wall of doors breeds a village. 1.14 counts beds instead, and",
                "a baby needs a free one to be born into.",
                "",
                "This gates births on a free bed. It does not remove 1.12.2's own door rule, which",
                "still applies first - beds are an additional requirement rather than a replacement,",
                "because the door rule lives inside a vanilla AI task that cannot be edited without",
                "a coremod."
        })
        public boolean bedsForBreeding = true;

        @Config.Comment({
                "Villagers spawn iron golems when frightened, on 1.14's rules.",
                "",
                "A panicking villager tries every 100 ticks and needs two others nearby that are also",
                "eligible. Eligible means: grown, slept within the last 24000 ticks, and no golem seen",
                "in the last 600. A job is not needed, so nitwits and unemployed villagers count.",
                "",
                "That is why this needs the rest of the mod running. A village whose villagers cannot",
                "reach a bed will never produce a golem, as in the game."
        })
        public boolean golemSpawning = true;

        @Config.Comment("How far a villager will look for a bed, in blocks.")
        @Config.RangeInt(min = 4, max = 128)
        public int searchRadius = 48;

        @Config.Comment({
                "When villagers go to bed and when they get up, as time of day in ticks.",
                "",
                "1.14's schedule turns REST on at 12000 - dusk - and leaves it at daybreak. A start",
                "later than the end makes a window running through midnight, which this one does."
        })
        @Config.RangeInt(min = 0, max = 23999)
        public int sleepStartTime = 12000;

        @Config.RangeInt(min = 0, max = 23999)
        public int sleepEndTime = 10;

        @Config.Comment({
                "Report village life in chat: beds claimed, villagers sleeping, gossip exchanged, and",
                "every attempt at an iron golem including the ones that come to nothing.",
                "",
                "These are the parts of the mod that cannot be checked by looking. A golem depends on",
                "who has slept, who has worked and how many of them are standing together, none of",
                "which is visible - so when no golem appears there is otherwise no way to tell which",
                "condition was the one that failed. The near misses are the useful half.",
                "",
                "Off by default. Only players within 48 blocks are told."
        })
        public boolean debugMessages = false;
    }

    public static final class Display {

        @Config.Comment({
                "Announce in chat which enchantment a villager's new book trades came out as.",
                "",
                "Written when a villager takes a job and rolls its first trades, and when a level-up",
                "unlocks a tier carrying more books - so rerolling a librarian by replacing its",
                "lectern reports the result without opening the trade screen to look. Only players",
                "within 32 blocks are told, and only enchanted books are worth a line."
        })
        public boolean announceEnchantedBooks = true;

        @Config.Comment({
                "Key that reloads the selected trade's cost into the trading slots.",
                "",
                "Named as LWJGL names them: SPACE, LALT, TAB, F, and so on. 1.14 uses space, where",
                "holding it presses the focused trade button and reloads the trade over and over.",
                "",
                "It is configurable because space is contested. Inventory Tweaks binds its 'move",
                "everything' shortcut to space by default, and a shortcut that consumes the click gets",
                "there first - which looks exactly like trades being refused while the key is held.",
                "If that happens, either rebind this or rebind theirs.",
                "",
                "An unrecognised name falls back to space."
        })
        public String refillKey = "SPACE";

        @Config.Comment({
                "Keep the trading slots loaded automatically while a player is trading.",
                "",
                "A deliberate departure from 1.14, where reloading is always something the player asks",
                "for. It matters for trades costing more than half a stack - a price of 56 paper against",
                "a stack of 64 buys exactly one trade per load - where the alternative is pressing the",
                "refill key between every single exchange.",
                "",
                "Set false for strict 1.14 behaviour: the slots reload when you pick a trade or press",
                "the refill key, and at no other time."
        })
        public boolean autoRefillWhileTrading = true;

        @Config.Comment({
                "Show a villager's level and experience bar above the trade screen, as 1.14 does.",
                "Drawn on top of the vanilla screen rather than replacing it, so mods that patch or",
                "decorate the merchant screen keep working. Costs one small packet per trade screen",
                "opened. Turning this off also stops that packet being sent."
        })
        public boolean showVillagerLevel = true;

        @Config.Comment({
                "Replace the trade screen with a rebuild of 1.14's: a scrollable list of seven",
                "trades down the left, the villager's rank beside its name, and the experience bar.",
                "",
                "This swaps only the screen. Vanilla's merchant container is reused untouched, so",
                "mods that patch or hook it keep working, and trade selection still goes over",
                "vanilla's own channel - nothing changes server-side.",
                "",
                "Set to false to keep the vanilla 1.12.2 screen, with the level shown as a small",
                "overlay above it instead."
        })
        public boolean useModernTradeScreen = true;

        @Config.Comment({
                "Show a villager's rank as a badge on its chest, the way 1.14 does - stone, iron,",
                "gold, emerald or diamond as it levels from Novice to Master.",
                "",
                "Drawn as an extra layer on whatever villager renderer is in use rather than by",
                "replacing it, so mods that reshape or decorate villagers keep working.",
                "",
                "Costs one small packet per villager a player comes near, and one more whenever a",
                "villager levels up. Turning this off stops both."
        })
        public boolean showLevelBadge = true;

        @Config.Comment({
                "Villagers hold up what they would give you for the item in your hand, as in the game.",
                "Hold wheat near a farmer that buys wheat and it holds up an emerald; with several",
                "trades for the same item it cycles through them. Only when you are within about four",
                "blocks, and not while it sleeps or trades."
        })
        public boolean showTradeItems = true;

        @Config.Comment({
                "Give villagers 1.14's appearance - the redrawn body, a biome outfit, and a",
                "profession outfit, stacked the way 1.14 stacks them.",
                "",
                "Painted over the existing texture rather than replacing the villager renderer, so",
                "mods that reshape or decorate villagers keep working.",
                "",
                "Villagers whose career has no 1.14 counterpart are left completely alone, so ones",
                "added by other mods keep their own look.",
                "",
                "1.12.2 has no villager biome type, so the outfit is read from where the villager",
                "currently is - meaning one carried far from home will change clothes."
        })
        public boolean useModernVillagerSkins = true;
    }

    /**
     * Writes edits made through the in-game config screen back onto the static fields.
     * Without this, changes only take effect after a restart.
     */
    public static final class EventHandler {

        @SubscribeEvent
        public void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
            if (VillagerBackport.MOD_ID.equals(event.getModID())) {
                ConfigManager.sync(VillagerBackport.MOD_ID, Config.Type.INSTANCE);
                com.exiledradio.villagerbackport.job.JobSiteRegistry.load();
                com.exiledradio.villagerbackport.job.WorkstationIndex.invalidateAll();
                com.exiledradio.villagerbackport.block.Compostables.loadExtras();
                com.exiledradio.villagerbackport.block.StonecutterRecipes.invalidate();
            }
        }

        /** Present so the class has a server-side reason to exist on a dedicated server. */
        @SubscribeEvent
        public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
            // No client sync needed in this phase - see the class comment on ModConfig.
        }
    }
}
