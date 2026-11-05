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

    @Config.LangKey("config.villagerbackport.raids")
    @Config.Comment("Raids, raiders and Hero of the Village, as villagers live through them.")
    public static final Raids raids = new Raids();

    @Config.LangKey("config.villagerbackport.display")
    @Config.Comment("What the trade screen shows.")
    public static final Display display = new Display();

    @Config.LangKey("config.villagerbackport.compat")
    @Config.Comment("Getting along with other mods that add the same blocks.")
    public static final Compat compat = new Compat();

    @Config.LangKey("config.villagerbackport.sounds")
    @Config.Comment("1.14 sounds for vanilla blocks.")
    public static final Sounds sounds = new Sounds();

    @Config.LangKey("config.villagerbackport.trader")
    @Config.Comment("The wandering trader and its llamas.")
    public static final Trader trader = new Trader();

    @Config.LangKey("config.villagerbackport.cats")
    @Config.Comment("Cats, and ocelots as 1.14 changed them.")
    public static final Cats cats = new Cats();

    @Config.LangKey("config.villagerbackport.foxes")
    @Config.Comment("Foxes and sweet berries.")
    public static final Foxes foxes = new Foxes();

    @Config.LangKey("config.villagerbackport.bamboo")
    @Config.Comment("Bamboo, scaffolding, the bamboo jungle and pandas.")
    public static final Bamboo bamboo = new Bamboo();

    @Config.LangKey("config.villagerbackport.campfire")
    @Config.Comment("The campfire.")
    public static final Campfire campfire = new Campfire();

    @Config.LangKey("config.villagerbackport.flora")
    @Config.Comment("1.14's flowers, dyes, suspicious stew and leather horse armor.")
    public static final Flora flora = new Flora();

    @Config.LangKey("config.villagerbackport.building")
    @Config.Comment("1.14's stone slabs, stairs and walls, smooth stone and wood signs.")
    public static final Building building = new Building();

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

        @Config.Comment({
                "Farmers farm by 1.14's rules: during work hours only, on the farmland around their",
                "composter, harvesting whenever they have room for the crop.",
                "",
                "Off, they farm as in 1.12.2: at any hour, any field within 16 blocks of wherever",
                "they stand, and only until they have 'enough' food."
        })
        public boolean modernFarming = true;

        @Config.Comment({
                "How far from its composter a farmer farms, in blocks, with modernFarming on.",
                "1.14's farmers reach about 10."
        })
        @Config.RangeInt(min = 2, max = 48)
        public int farmRadius = 10;

        @Config.Comment({
                "Farmers bake bread from wheat when they work at their composter, as in current",
                "Minecraft: 3 wheat make 1 bread, up to 3 loaves a visit, and none once they carry",
                "more than 36. Bread is what villagers share to breed."
        })
        public boolean farmersBakeBread = true;

        @Config.Comment({
                "Farmers compost the seeds they do not need when they work at their composter, as",
                "in current Minecraft (added in 1.16): they keep 10 of each kind, compost up to 20",
                "items a visit, and empty a full composter, leaving the bone meal on top of it."
        })
        public boolean farmersCompost = true;

        @Config.Comment({
                "Let villagers farm and pick up food and seeds even with the mobGriefing gamerule",
                "off.",
                "",
                "As in vanilla, a villager only harvests, replants and picks items up while",
                "mobGriefing is on, so a world that turns it off to stop creeper damage has farmers",
                "standing idle beside ripe fields and villagers ignoring bread thrown to them. On,",
                "they do both anyway. Everything else mobGriefing covers stays off, villagers",
                "trampling farmland included."
        })
        public boolean ignoreMobGriefing = false;

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
                "1.12.2 does not have are swapped for the nearest it does, unless a backport mod",
                "adds them (see backportBlocks).",
                "LEGACY: 1.12.2's villages, with workstations put into the existing houses.",
                "",
                "MODERN needs a world generator it can lay a village out on: vanilla's, superflat,",
                "Biomes O' Plenty's, Open Terrain Generator's, Realistic Terrain Generation's,",
                "Farmer's Future Delight's Caves & Cliffs world type, or Caves Not Cliffs. In a world",
                "made by another generator villages stay LEGACY.",
                "",
                "Only affects villages generated after the change. Villages already on disk keep",
                "the layout they were generated with."
        })
        @Config.RequiresMcRestart
        public com.exiledradio.villagerbackport.village.modern.MapGenModernVillage.Style style =
                com.exiledradio.villagerbackport.village.modern.MapGenModernVillage.Style.MODERN;

        @Config.Comment({
                "Build MODERN villages with the real 1.14 blocks when a backport mod adds them, rather",
                "than 1.12.2's nearest block.",
                "",
                "Future MC: stripped logs and wood, blue ice, smooth sandstone and its slabs, smooth",
                "stone, sandstone, diorite and granite walls, diorite slabs, and spruce, jungle and",
                "acacia trapdoors and signs.",
                "Quark: the same smooth sandstone, slabs, walls and trapdoors, diorite and granite",
                "stairs, and spruce, jungle and acacia buttons and pressure plates.",
                "Oceanic Expanse: blue ice and sea pickles.",
                "",
                "With none of them installed nothing changes. Turn this off to keep 1.12.2's blocks",
                "even with them installed."
        })
        @Config.RequiresMcRestart
        public boolean backportBlocks = true;

        @Config.Comment({
                "Put other mods' village buildings in 1.14 villages.",
                "",
                "Mods add buildings to 1.12.2 villages through Forge's village hook: RLCraft's",
                "village houses (Recurrent Complex), Waystones' village waystone, and others. 1.14",
                "villages are built from 1.14's own buildings, so without this they have none of",
                "them. With it, those buildings are placed beside the village's streets, about as",
                "many as they would make up in a 1.12.2 village of the same size.",
                "",
                "The log lists the pieces on offer, by the names modPieceExclusions takes, the first",
                "time a 1.14 village is made."
        })
        public boolean modPieces = true;

        @Config.Comment({
                "Most other-mod buildings added to one 1.14 village. The waystone does not count."
        })
        @Config.RangeInt(min = 0, max = 64)
        public int maxModPieces = 10;

        @Config.Comment({
                "Place Waystones' village waystone first, next to the town centre where the bell is,",
                "whenever Waystones rolls one for the village (its own 'Generate in Villages' chance).",
                "Off, it is just one more piece placed anywhere along the streets."
        })
        public boolean waystoneNearBell = true;

        @Config.Comment({
                "Other mods' village buildings to leave out of 1.14 villages.",
                "",
                "Each entry is a mod id ('reccomplex' leaves out all of Recurrent Complex's), a mod id",
                "and piece class ('waystones:ComponentVillageWaystone'), or for Recurrent Complex a",
                "structure ('reccomplex:P_VillageInn'). The log lists what is on offer."
        })
        public String[] modPieceExclusions = {};

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
                "Upgrade diamond gear to netherite at the smithing table, as in 1.16, when a mod",
                "that adds netherite is installed.",
                "",
                "Diamond item plus netherite ingot makes the netherite item, keeping its",
                "enchantments, name and damage. The upgrades are read from the netherite mod",
                "itself: Future MC's smithing recipes, Spartan Netherite, and crafting recipes",
                "that upgrade a diamond item with an ingot, with a built-in list for mods that",
                "hide theirs. Without a netherite mod the table does nothing, as in 1.14. Every",
                "netherite ingot found is also given the 'ingotNetherite' ore dictionary name.",
                "",
                "With Future MC installed, its smithing recipes are read too, so this mod's table",
                "does everything Future MC's does. Which of the two tables is kept follows",
                "compat.futureMcDuplicates."
        })
        public boolean netheriteUpgrades = true;

        @Config.Comment({
                "Also guess upgrades from item names, for netherite mods that only craft netherite",
                "gear from ingots and never offered an upgrade: 'diamond_sword' becomes",
                "'netherite_sword', and the usual spellings of each piece are tried, in every mod",
                "that adds a netherite ingot. Gives those items an upgrade path their own mod did",
                "not have."
        })
        public boolean netheriteNameMatching = true;

        @Config.Comment({
                "Remove other mods' crafting-grid netherite upgrades (diamond item plus ingot in a",
                "crafting table), so the smithing table is the only way, as in 1.16."
        })
        public boolean removeCraftingUpgrades = false;



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
                "Both mods add the same twelve blocks with the same recipes, which puts two of",
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
                "The lectern is never hidden, because Future MC has none.",
                "",
                "Does nothing unless Future MC is installed."
        })
        @Config.RequiresMcRestart
        public com.exiledradio.villagerbackport.compat.FutureMc.Duplicates futureMcDuplicates =
                com.exiledradio.villagerbackport.compat.FutureMc.Duplicates.KEEP_OURS;

        @Config.Comment({
                "Keep villagers ordinary through zombification and curing.",
                "",
                "A zombie villager that was one of your villagers is never made an Infernal Mobs",
                "elite, a Scaling Health blight or a Champion, and gets no Scaling Health bonus",
                "health. The villager it is cured back into is never turned into a Sussyberian or",
                "Mentalberian (Dregora's EagleMixins), which refuse to trade.",
                "",
                "With this on, a Sussyberian or Mentalberian that was once cured is turned back into",
                "the librarian it was when its chunk next loads. Ones that generated as berians are",
                "left alone. Does nothing for mods that are not installed."
        })
        public boolean convertedStayOrdinary = true;

        @Config.Comment({
                "Whose raids there are.",
                "",
                "  AUTO            Raids Backport's when it is installed, otherwise this mod's",
                "  OURS            this mod's, even with Raids Backport installed",
                "  RAIDS_BACKPORT  Raids Backport's (no raids at all without it)",
                "  NONE            no raids at all: villagers still hide when a bell rings",
                "",
                "With OURS and Raids Backport both installed, both mods start raids: pick one.",
                "",
                "With Raids Backport, its raids, raiders and Hero of the Village effect drive",
                "everything in the raids section: hiding, ringing the bell, celebrating, gifts and",
                "the discount. Its own discount is then taken off trades, so the hero discount is",
                "counted once and the trade screen shows the price actually charged."
        })
        @Config.RequiresMcRestart
        public com.exiledradio.villagerbackport.raid.Raids.Source raids =
                com.exiledradio.villagerbackport.raid.Raids.Source.AUTO;

        @Config.Comment({
                "Whether this mod adds the crossbow, with Quick Charge, Multishot and Piercing.",
                "",
                "  AUTO  only when neither Future MC nor Crossbows Backport is installed",
                "  OURS  always, alongside theirs",
                "  OFF   never",
                "",
                "Without it, pillagers, outposts and fletchers use another mod's crossbow (see",
                "raids.crossbows), and its recipe, enchantments and advancements are gone. Turning",
                "it off in a world that has crossbows deletes them."
        })
        @Config.RequiresMcRestart
        public com.exiledradio.villagerbackport.crossbow.CrossbowContent.Mode crossbow =
                com.exiledradio.villagerbackport.crossbow.CrossbowContent.Mode.AUTO;
    }

    public static final class Raids {

        @Config.Comment({
                "Ringing a bell with raiders nearby makes them glow, as in 1.14.",
                "",
                "Raiders are the game's: pillagers, vindicators, evokers, illusioners, ravagers and",
                "witches, whichever mod adds them, plus anything in extraRaiders. The resonance",
                "sounds a quarter of a second after the ring, and the raiders within 48 blocks",
                "light up two seconds after that, for three seconds."
        })
        public boolean bellRevealsRaiders = true;

        @Config.Comment({
                "Villagers within 32 blocks run home when a bell is rung, as in 1.14.",
                "",
                "With no raid on they stay inside for fifteen seconds, or until fifteen seconds",
                "after the bell. During a raid they hide until it is over."
        })
        public boolean hideOnBell = true;

        @Config.Comment({
                "Villagers live through raids as they do in 1.14.",
                "",
                "Before the first wave they gather at the bell and ring it. Once raiders arrive",
                "they go home and stay there. After a victory they come out and celebrate."
        })
        public boolean raidBehaviour = true;

        @Config.Comment({
                "Hero of the Village takes 30% off every trade, plus 6.25% a level, as in 1.14.",
                "",
                "Counted from the trade's base price and stacked with reputation, inside the same",
                "limits. Needs pricing.demandPricing, which is where prices are set."
        })
        public boolean heroDiscount = true;

        @Config.Comment({
                "Villagers throw a Hero of the Village gifts now and then, as in 1.14.",
                "",
                "What each profession gives is a loot table in",
                "villagerbackport:gameplay/hero_of_the_village/<profession>_gift. Babies give a",
                "poppy and villagers without a job give wheat seeds. With Raids Backport, its own",
                "gifts are left as they are and this only fills in the professions it has none for."
        })
        public boolean heroGifts = true;

        @Config.Comment({
                "Bad Omen turns into 30 seconds of Raid Omen in a village, and the raid starts when",
                "that runs out, as in current Java. Off: the raid starts the moment a player with",
                "Bad Omen walks in, as in 1.14."
        })
        public boolean raidOmen = true;

        @Config.Comment("How many waves a raid has on easy. 1.14 uses 3.")
        @Config.RangeInt(min = 1, max = 7)
        public int easyWaves = 3;

        @Config.Comment("How many waves a raid has on normal. 1.14 uses 5.")
        @Config.RangeInt(min = 1, max = 7)
        public int normalWaves = 5;

        @Config.Comment("How many waves a raid has on hard. 1.14 uses 7.")
        @Config.RangeInt(min = 1, max = 7)
        public int hardWaves = 7;

        @Config.Comment({
                "Which mob a raid sends for each of 1.14's five raiders, by registry name. The first",
                "one installed is used; a raider with none installed is left out of the waves.",
                "",
                "This mod's own pillager and ravager come first; another mod's are used instead",
                "if they are listed ahead of them."
        })
        public String[] pillagers = {"villagerbackport:pillager", "raids:pillager", "minecraft:vindication_illager"};

        @Config.Comment({
                "The crossbow a pillager carries, by registry name: the first one installed. This",
                "mod's own is only there when compat.crossbow adds it. With none of them installed",
                "a pillager carries a stand-in that only pillagers can use and that never drops.",
                "",
                "The first of these that players can have is also the one outpost chests hold."
        })
        public String[] crossbows = {"villagerbackport:crossbow", "futuremc:crossbow", "crossbows:crossbow",
                "spartanweaponry:crossbow_wood", "villagerbackport:pillager_crossbow"};

        @Config.Comment({
                "A captain killed away from a raid drops an ominous bottle of a random level, as in",
                "current Java, instead of giving Bad Omen straight away, as in 1.14. Deeper Depths'",
                "bottle is dropped when it is installed."
        })
        public boolean captainDropsOminousBottle = false;

        @Config.Comment({
                "Pillager patrols roam the overworld, as in 1.14: from the fifth day, by day, now and",
                "then a band of pillagers appears 24 to 48 blocks from a player who is not in a",
                "village, led by a captain with the ominous banner. Only with this mod's raids."
        })
        public boolean patrols = true;

        @Config.Comment({
                "Pillager outposts generate, as in 1.14: a watchtower with tents, logs, targets and",
                "iron golem cages around it, manned by pillagers that spawn there day and night.",
                "On the village grid, one candidate in five, never within ten chunks of a village.",
                "New chunks only. Only with this mod's raids."
        })
        public boolean outposts = true;

        @Config.Comment({
                "Outposts also generate in other mods' biomes that are like 1.14's outpost biomes:",
                "plains, desert, savanna, coniferous forest or snowy flats, but not hills,",
                "mountains or water. Off: only the five vanilla biomes 1.14 uses."
        })
        public boolean outpostsInModdedBiomes = true;

        @Config.Comment("See pillagers.")
        public String[] vindicators = {"minecraft:vindication_illager"};

        @Config.Comment("See pillagers.")
        public String[] evokers = {"minecraft:evocation_illager"};

        @Config.Comment("See pillagers.")
        public String[] witches = {"minecraft:witch"};

        @Config.Comment("See pillagers.")
        public String[] ravagers = {"villagerbackport:ravager", "raids:ravager"};

        @Config.Comment({
                "More mobs to treat as raiders, by registry name: e.g. 'somemod:marauder'.",
                "",
                "Raiders frighten villagers like a vindicator does and glow when a bell rings."
        })
        public String[] extraRaiders = {};
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

    public static final class Sounds {

        @Config.Comment({
                "Give crops 1.14's sounds: a rustle when seeds are planted, by players and by farmer",
                "villagers, and a crunch when a crop is broken.",
                "",
                "Wheat, carrots, potatoes and beetroot, pumpkin and melon stems, and nether wart, plus",
                "any other mod's crop that still uses the plain plant sounds. Takes effect on restart."
        })
        public boolean cropSounds = true;
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
                "Turning this off only hides the badge. The villager's level still reaches each",
                "player, since its career and biome outfit travel with it."
        })
        public boolean showLevelBadge = true;

        @Config.Comment({
                "Which 1.14 outfit other mods' villagers wear, one per line, as 'profession=outfit' or",
                "'profession/career=outfit'. Professions by registry name, careers by name.",
                "",
                "By default another mod's villager keeps its own texture, with the rank badge drawn",
                "on, since there is no telling which part of a whole 1.12.2 skin is the clothes. A",
                "line here dresses it as a vanilla villager instead: biome clothing, the outfit, its",
                "hat and the badge.",
                "",
                "Outfits: armorer, butcher, cartographer, cleric, farmer, fisherman, fletcher,",
                "leatherworker, librarian, mason, nitwit, shepherd, toolsmith, weaponsmith, or none",
                "for the biome clothing alone.",
                "",
                "Examples:",
                "  somemod:chef=butcher",
                "  minecraft:farmer/beekeeper=farmer   (a career a mod added to a vanilla profession)",
                "",
                "A career a mod adds to a vanilla profession wears that profession's outfit unless",
                "it is listed here: the farmer's, librarian's, cleric's, toolsmith's or butcher's."
        })
        public String[] modOutfits = {};

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
                com.exiledradio.villagerbackport.raid.Raids.reloadConfig();
            }
        }

        /** Present so the class has a server-side reason to exist on a dedicated server. */
        @SubscribeEvent
        public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
            // No client sync needed in this phase - see the class comment on ModConfig.
        }
    }

    /** Filled in by its 1.14 completion stream (docs/V14_COMPLETION_PLAN.md). */
    public static final class Trader {

        @Config.Comment("Whether this mod adds the wandering trader and trader llamas.")
        @Config.RequiresMcRestart
        public boolean enabled = true;

        @Config.Comment({
                "Whether a wandering trader turns up on its own, near a random player in the overworld.",
                "Off leaves spawn eggs and /wanderingtrader spawn as the only way to get one. The",
                "doTraderSpawning game rule (added to every world, as current Java has it) does the same per",
                "world, and doMobSpawning off stops it too."
        })
        public boolean naturalSpawning = true;

        @Config.Comment({
                "Ticks between spawn attempts. 1.14's value is 24000, one in-game day. The countdown is",
                "kept per world and carries on across restarts."
        })
        @Config.RangeInt(min = 1200, max = 1200000)
        public int spawnDelay = 24000;

        @Config.Comment({
                "The chance, in percent, of the first attempt after a trader has spawned. Each attempt that",
                "fails raises it by spawnChanceStep, up to maxSpawnChance. 1.14: 25, then 50, then 75.",
                "Every attempt then has a further one in ten roll before a trader really appears, so the",
                "overall chances are 2.5%, 5% and 7.5% - as in 1.14."
        })
        @Config.RangeInt(min = 0, max = 100)
        public int minSpawnChance = 25;

        @Config.Comment("How much each failed attempt raises the chance by, in percent. 1.14's value is 25.")
        @Config.RangeInt(min = 0, max = 100)
        public int spawnChanceStep = 25;

        @Config.Comment("The most the chance can climb to, in percent. 1.14's value is 75.")
        @Config.RangeInt(min = 0, max = 100)
        public int maxSpawnChance = 75;

        @Config.Comment({
                "How far from the chosen player, or from the bell it prefers, a trader may appear, in",
                "blocks. Also how far from the player a bell is looked for. 1.14's value is 48."
        })
        @Config.RangeInt(min = 8, max = 128)
        public int spawnRadius = 48;

        @Config.Comment({
                "Ticks a naturally spawned trader stays before it leaves. 1.14's value is 48000 (40",
                "minutes). The clock only runs while the trader is loaded and nobody is trading with it.",
                "Traders from spawn eggs and commands never leave. 0 means natural ones never leave either."
        })
        @Config.RangeInt(min = 0, max = 1200000)
        public int despawnTicks = 48000;

        @Config.Comment("Trader llamas leashed to each naturally spawned trader. 1.14's value is 2.")
        @Config.RangeInt(min = 0, max = 4)
        public int llamas = 2;

        @Config.Comment({
                "Whether a new trader waits until the last one has gone. 1.14 only remembers the last",
                "trader; with this on, another is not sent while that one is still loaded, or while it",
                "could still be somewhere in unloaded chunks (less than despawnTicks since it arrived)."
        })
        public boolean oneAtATime = true;
    }

    /** Filled in by its 1.14 completion stream (docs/V14_COMPLETION_PLAN.md). */
    public static final class Cats {

        @Config.Comment("Whether this mod adds cats, and 1.14's ocelots.")
        @Config.RequiresMcRestart
        public boolean enabled = true;

        @Config.Comment({"Stray cats turn up in villages, as in 1.14: about once a minute, near a random player,",
                "where a village has more than four occupied beds within 48 blocks and fewer than five cats."})
        public boolean spawnInVillages = true;

        @Config.Comment("A black cat turns up at a witch hut that has none, as in 1.14.")
        public boolean spawnAtWitchHuts = true;

        @Config.Comment({"Ocelots behave as in 1.14: they can no longer be tamed, and feeding one raw cod or salmon",
                "earns its trust instead, so it stops running from players. Off leaves 1.12.2's ocelots alone."})
        @Config.RequiresMcRestart
        public boolean modernOcelots = true;

        @Config.Comment({"Tamed ocelots from before this mod became cats when they load, keeping their owner, name and",
                "coat (tuxedo, red and Siamese). This is how 1.14 itself upgraded old worlds."})
        public boolean convertTamedOcelots = true;

        @Config.Comment("Creepers run from cats, as they do from ocelots.")
        @Config.RequiresMcRestart
        public boolean creepersFearCats = true;

        @Config.Comment({"Phantoms break off an attack when a cat is within 16 blocks, and the cat hisses. Works with",
                "any mod's phantom (anything registered as 'phantom'); 1.12.2 itself has none."})
        public boolean phantomsFearCats = true;
    }

    /** Filled in by its 1.14 completion stream (docs/V14_COMPLETION_PLAN.md). */
    public static final class Foxes {

        @Config.Comment({
                "Whether this mod adds foxes, sweet berries and sweet berry bushes.",
                "",
                "Off, none of them is registered, so a world that has them loses them. With Future",
                "MC installed, which adds the same three, compat.futureMcDuplicates decides whose",
                "are used; this only switches this mod's off altogether."
        })
        @Config.RequiresMcRestart
        public boolean enabled = true;

        @Config.Comment({
                "Whether sweet berry bushes generate in taigas, as in 1.14.",
                "",
                "Taiga-like biomes are found by the Forge biome dictionary (CONIFEROUS), so modded",
                "taigas get them too. Nothing generates in a biome listed in bushBiomeBlacklist."
        })
        @Config.RequiresWorldRestart
        public boolean generateBushes = true;

        @Config.Comment({
                "Bush patches tried per chunk in an ordinary (not snowy, not giant) taiga.",
                "",
                "1.14 tries one patch per chunk here, at a random height up to twice the surface, so",
                "only some of them land on the ground. That is copied as it is."
        })
        @Config.RangeInt(min = 0, max = 16)
        @Config.RequiresWorldRestart
        public int taigaPatchesPerChunk = 1;

        @Config.Comment({
                "One chunk in this many gets a bush patch in a snowy or giant tree taiga.",
                "",
                "1.14's snowy taigas use one in twelve. 1.14.4 has no bushes in giant tree taigas;",
                "current Java does, rarely, so they share the snowy taiga's rate. 0 turns both off."
        })
        @Config.RangeInt(min = 0, max = 1000)
        @Config.RequiresWorldRestart
        public int rarePatchChance = 12;

        @Config.Comment("Biomes (registry names, like minecraft:taiga) that never get sweet berry bushes.")
        @Config.RequiresWorldRestart
        public String[] bushBiomeBlacklist = {};

        @Config.Comment({
                "Fox spawn weight in taiga-like biomes. 1.14 uses 8, the same as wolves in a taiga.",
                "0 stops foxes spawning naturally; spawn eggs and breeding still work."
        })
        @Config.RangeInt(min = 0, max = 100)
        @Config.RequiresMcRestart
        public int spawnWeight = 8;

        @Config.Comment("The smallest group of foxes that spawns together. 1.14 uses 2.")
        @Config.RangeInt(min = 1, max = 16)
        @Config.RequiresMcRestart
        public int minGroup = 2;

        @Config.Comment("The largest group of foxes that spawns together. 1.14 uses 4.")
        @Config.RangeInt(min = 1, max = 16)
        @Config.RequiresMcRestart
        public int maxGroup = 4;

        @Config.Comment({
                "Biomes (registry names) foxes spawn in, on top of every CONIFEROUS biome.",
                "A snowy biome gets snow foxes, any other red ones."
        })
        @Config.RequiresMcRestart
        public String[] extraFoxBiomes = {};

        @Config.Comment("Biomes (registry names) foxes never spawn in, even when they are CONIFEROUS.")
        @Config.RequiresMcRestart
        public String[] foxBiomeBlacklist = {};
    }

    /** Filled in by its 1.14 completion stream (docs/V14_COMPLETION_PLAN.md). */
    public static final class Bamboo {

        @Config.Comment({
                "Whether this mod adds bamboo, scaffolding, the bamboo jungle and pandas.",
                "",
                "With Future MC installed too, both mods add bamboo and scaffolding. Which copy",
                "stays visible follows compat.futureMcDuplicates, as it does for the workstations."
        })
        @Config.RequiresMcRestart
        public boolean enabled = true;

        @Config.Comment({
                "Generate the bamboo jungle and bamboo jungle hills biomes.",
                "",
                "As in 1.14 they are not biomes of their own climate: some jungles are bamboo",
                "jungles instead, and some jungle hills bamboo jungle hills. Works with any world",
                "type that places vanilla's jungle - the default, large biomes, amplified, and most",
                "biome mods. World types that do not use vanilla's biome layers (Open Terrain",
                "Generator presets, for one) never contain them.",
                "",
                "Turning this off also leaves the two biomes unregistered."
        })
        @Config.RequiresMcRestart
        public boolean bambooJungle = true;

        @Config.Comment({
                "One jungle area in this many becomes a bamboo jungle.",
                "",
                "1.14 turns one jungle in ten into a bamboo jungle. Lower is more common."
        })
        @Config.RangeInt(min = 1, max = 1000)
        @Config.RequiresMcRestart
        public int bambooJungleRarity = 10;

        @Config.Comment({
                "Scatter bamboo through ordinary jungles and jungle hills, as 1.14 does.",
                "",
                "Sparse - a stand every few chunks. Does nothing while Future MC is installed,",
                "which already puts its own bamboo there."
        })
        public boolean jungleBamboo = true;

        @Config.Comment({
                "Spawn weight of pandas in ordinary jungles and jungle hills (1.14: 1).",
                "",
                "Pandas mostly live in bamboo jungles; this is the rare one met elsewhere. 0 keeps",
                "them out of ordinary jungles."
        })
        @Config.RangeInt(min = 0, max = 1000)
        @Config.RequiresMcRestart
        public int pandaJungleWeight = 1;

        @Config.Comment("Spawn weight of pandas in bamboo jungles (1.14: 80).")
        @Config.RangeInt(min = 0, max = 1000)
        @Config.RequiresMcRestart
        public int pandaBambooJungleWeight = 80;
    }

    /** Filled in by its 1.14 completion stream (docs/V14_COMPLETION_PLAN.md). */
    public static final class Campfire {

        @Config.Comment({
                "Whether this mod adds the campfire.",
                "When false it is not registered: villages place Future MC's campfire if it is installed,",
                "and leave the spot empty if not."
        })
        @Config.RequiresMcRestart
        public boolean enabled = true;

        @Config.Comment({
                "How long one item takes to cook, in ticks. 1.14's is 600 (30 seconds), three times",
                "a furnace."
        })
        @Config.RangeInt(min = 1, max = 72000)
        public int cookTime = 600;

        @Config.Comment({
                "What the campfire cooks. By default: every furnace recipe whose input and output",
                "are both food, which is 1.14's own list (raw meat, raw fish, potatoes) plus any",
                "mod's raw foods.",
                "",
                "Add an item the default leaves out as 'modid:item' (cooked as the furnace would)",
                "or 'modid:item=modid:result' (cooked into that). Add ':meta' after either to pick",
                "one variant, e.g. 'minecraft:fish:1=minecraft:cooked_fish:1'."
        })
        public String[] extraFoods = {};

        @Config.Comment({
                "Items the campfire refuses, even though the rule above would cook them,",
                "as 'modid:item' or 'modid:item:meta'."
        })
        public String[] excludedFoods = {};
    }

    /** Filled in by its 1.14 completion stream (docs/V14_COMPLETION_PLAN.md). */
    public static final class Flora {

        @Config.Comment({
                "Whether this mod adds 1.14's flowers, dyes, suspicious stew and leather horse armor.",
                "",
                "Covers the cornflower, lily of the valley and wither rose (and their flower pots),",
                "white, black, brown and blue dye, suspicious stew, brown mooshrooms, leather horse",
                "armor, the Globe banner pattern and dyeing sign text. Turning it off in a world that",
                "has any of them deletes them.",
                "",
                "With Future MC installed, which of the two mods' flowers, dyes and stew stay visible",
                "follows compat.futureMcDuplicates, as the workstations do."
        })
        @Config.RequiresMcRestart
        public boolean enabled = true;

        @Config.Comment({
                "Generate cornflowers in plains, sunflower plains and flower forests, and lilies of",
                "the valley in forests, birch forests, dark forests and flower forests, as 1.14 does.",
                "Modded biomes join in by their biome dictionary types: plains that are not snowy or",
                "savanna get cornflowers, forests that are not coniferous, snowy or jungle get lilies.",
                "",
                "Also decides whether bone meal on grass can grow them (cornflowers in plains,",
                "sunflower plains and flower forests, lilies only in flower forests)."
        })
        @Config.RequiresMcRestart
        public boolean generateFlowers = true;

        @Config.Comment({
                "Brown mooshrooms. Lightning turns a red mooshroom brown and a brown one red. Feed a",
                "brown one a small flower, then milk it with a bowl, for that flower's suspicious stew.",
                "Sheared, it drops brown mushrooms. Red mooshrooms are unchanged."
        })
        public boolean brownMooshrooms = true;

        @Config.Comment({
                "Right-clicking a sign with any dye recolors its text, as in 1.14. The color is kept",
                "with the sign and drawn on every client that has this mod."
        })
        public boolean dyeableSigns = true;

        @Config.Comment({
                "A mob, player or villager the Wither kills leaves a wither rose: planted where it",
                "died if the block is free and mobGriefing allows it, dropped as an item otherwise."
        })
        public boolean witherKillsPlantRoses = true;
    }

    /** Filled in by its 1.14 completion stream (docs/V14_COMPLETION_PLAN.md). */
    public static final class Building {

        @Config.Comment("Whether this mod adds 1.14's building blocks.")
        @Config.RequiresMcRestart
        public boolean enabled = true;
    }
}
