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

    @Config.LangKey("config.villagerbackport.jobs")
    @Config.Comment("Workstations villagers go to work at.")
    public static final Jobs jobs = new Jobs();

    @Config.LangKey("config.villagerbackport.workstations")
    @Config.Comment("What the workstation blocks accept and produce.")
    public static final Workstations workstations = new Workstations();

    @Config.LangKey("config.villagerbackport.villages")
    @Config.Comment("Workstation buildings added to generated villages.")
    public static final Villages villages = new Villages();

    @Config.LangKey("config.villagerbackport.structures")
    @Config.Comment("Workstations put into structures that already generate.")
    public static final Structures structures = new Structures();

    @Config.LangKey("config.villagerbackport.homes")
    @Config.Comment("Beds, sleeping, breeding and iron golems.")
    public static final Homes homes = new Homes();

    @Config.LangKey("config.villagerbackport.display")
    @Config.Comment("What the trade screen shows.")
    public static final Display display = new Display();

    private ModConfig() {
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
                "Play a sound when a villager restocks at its workstation.",
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
                "How sharply demand moves the price of premium trades. 1.14's value is 0.2.",
                "",
                "1.14 stores a multiplier per trade and uses exactly two: 0.05 for ordinary trades and",
                "0.2 for the premium ones - enchanted books, enchanted tools and armour, maps, bells,",
                "saddles. In VillagerTrades that is seven trades at 0.05 against thirty-five at 0.2.",
                "",
                "1.12.2 recipes have no such field, so a trade counts as premium here when what it",
                "sells is enchanted or is a filled map. That covers the cases players notice, chiefly",
                "the enchanted book trade, whose price would otherwise climb four times too slowly."
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
                "1.14 does this when they gather at the meeting point, handing over only the part of an",
                "opinion above its sharing floor and at most ten of it - so a killing travels but",
                "arrives much weaker, and trading barely travels at all."
        })
        public boolean gossipSpreads = true;

        @Config.Comment({
                "How close two villagers must be to pass an opinion between them, in blocks."
        })
        @Config.RangeDouble(min = 1.0D, max = 32.0D)
        public double gossipRange = 8.0D;

        @Config.Comment({
                "Ceiling on how far demand can accumulate in either direction.",
                "1.14 leaves demand unbounded and relies on the stack-size cap to bound the price.",
                "That works, but it lets a value in saved data grow without limit, so it is capped",
                "here. The default is high enough that the stack-size cap is still what binds."
        })
        @Config.RangeInt(min = 1, max = 1024)
        public int maxDemand = 128;

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
                "How many times a trade the player SELLS into can be used before running dry.",
                "1.14's value is 16; 1.12.2 defaults to 7. Set 0 to leave trades as their author",
                "made them.",
                "",
                "This is what makes the experience values above add up. They are 1.14's per-trade",
                "amounts, calibrated against 1.14's stock - paying them over less than half the",
                "trades means a villager sells out with the next level still out of reach.",
                "",
                "Trades that already allow more than this keep what they have, so a mod that",
                "deliberately made something scarce is not overridden."
        })
        @Config.RangeInt(min = 0, max = 64)
        public int sellingTradeStock = 16;

        @Config.Comment({
                "How many times a trade the player BUYS from can be used before running dry.",
                "1.14's value is 12; 1.12.2 defaults to 7. Set 0 to leave trades untouched.",
                "Lower than the selling figure, exactly as in 1.14."
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
                "Off by default. On, a village with no lectern anywhere has librarians that never",
                "restock, which is 1.14's behaviour but unforgiving on a world built before this",
                "mod was installed. Off, they restock freely until they find one."
        })
        public boolean strictJobSite = false;

        @Config.Comment({
                "How far a villager will look for an unclaimed workstation, in blocks.",
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
                "How far up and down a villager looks for a workstation, in blocks.",
                "Much shallower than the horizontal reach on purpose: workstations sit at about the",
                "villager's own level, and the search cost grows with the volume.",
                "",
                "Deep enough for a two-storey house and for a village built across a slope, which is",
                "most of them once a pack is generating its own terrain.",
                "",
                "Still much shallower than the horizontal reach, but now for the sake of what a",
                "villager ought to consider its own rather than to save any work - a workstation two",
                "floors below is somebody else's."
        })
        @Config.RangeInt(min = 1, max = 32)
        public int searchHeight = 8;

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
                "Ticks a villager may hold a workstation it has never reached before giving it up.",
                "",
                "Holding a claim is what stops a villager looking for work, so a claim on a block it",
                "cannot actually walk to - across a river, behind a wall, on the far side of a door",
                "it will not open - would freeze it for good, and it would ignore a workstation put",
                "down at its feet. This is the escape hatch.",
                "",
                "Only ever applies before the first arrival. Once a villager has stood at its",
                "workstation the claim is kept however far it wanders afterwards."
        })
        @Config.RangeInt(min = 20, max = 24000)
        public int abandonUnreachedTicks = 600;

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
                "Which block each career works at, as 'modid:block=career'.",
                "",
                "Career names are 1.12.2's own, which abbreviates three of them: 'armor', 'weapon'",
                "and 'tool' are the armorer, weaponsmith and toolsmith. The others are farmer,",
                "fisherman, shepherd, fletcher, librarian, cartographer, cleric, butcher, leather",
                "and nitwit.",
                "",
                "Only the two workstations 1.12.2 already has are mapped by default. The rest of",
                "1.14's are blocks this mod has yet to add; entries naming a block that is not",
                "installed are ignored, so pointing a career at another mod's block is safe."
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
                "Add workstation buildings to villages as they generate.",
                "",
                "1.12.2's fourteen village piece types contain no workstations at all - and no beds",
                "either, since both arrived with 1.14 - so a village that generates normally has",
                "almost nothing for a villager to be employed at. This fills that gap the same way",
                "1.14 does, by building the job sites in.",
                "",
                "These are registered as ordinary weighted village pieces, which is the same route",
                "structure mods use for the village houses they add. Vanilla houses, another mod's",
                "houses and these all mix into one village with no compatibility work on either side.",
                "",
                "Only affects villages generated after this is turned on. Villages already on disk",
                "are laid out and will not change."
        })
        @Config.RequiresMcRestart
        public boolean enabled = true;

        @Config.Comment({
                "How often a workshop is chosen while a village lays itself out.",
                "",
                "Vanilla's own weights run from 3 for a small hut to 20 for a church. Higher means",
                "picked more often, and the limits below are what actually cap the count."
        })
        @Config.RangeInt(min = 0, max = 100)
        @Config.RequiresMcRestart
        public int workshopWeight = 8;

        @Config.Comment({
                "Most workshops one village may have, before its size is added.",
                "",
                "A fixed cap rather than a rolled one, deliberately. Vanilla rolls its limits from",
                "the same random the whole village layout is then built with, and a mod drawing from",
                "that stream makes villages stop being reproducible from their seed - Forge asks each",
                "mod's handler for its weight in an order that varies between launches, so the stream",
                "ends up in a different place each time and the village grows a different way.",
                "",
                "Nothing is lost by fixing it: pieces still compete for the spots along the roads, so",
                "two villages with the same cap are not alike."
        })
        @Config.RangeInt(min = 1, max = 32)
        @Config.RequiresMcRestart
        public int workshopLimit = 6;

        @Config.Comment({
                "How often an open market stall is chosen.",
                "",
                "Stalls have no door, so unlike workshops they do not enlarge the village that holds",
                "them - 1.12.2 counts doors to decide how many villagers a village supports."
        })
        @Config.RangeInt(min = 0, max = 100)
        @Config.RequiresMcRestart
        public int stallWeight = 5;

        @Config.Comment({
                "Most stalls one village may have, before its size is added.",
                "",
                "Set higher than the workshops on purpose. A stall has no door, and 1.12.2 sizes a",
                "village - and so the number of villagers it supports - by counting doors. A stall",
                "therefore adds somewhere to work without adding anybody to work there, which is the",
                "only lever here that improves the ratio rather than moving both ends of it."
        })
        @Config.RangeInt(min = 1, max = 32)
        @Config.RequiresMcRestart
        public int stallLimit = 8;

        @Config.Comment({
                "How often the bell square is chosen. Limited to one per village either way.",
                "Set to 0 to leave it out entirely."
        })
        @Config.RangeInt(min = 0, max = 100)
        @Config.RequiresMcRestart
        public int meetingPointWeight = 15;

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

    public static final class Structures {

        @Config.Comment({
                "Turn some of the crafting tables and furnaces structures generate with into",
                "workstations.",
                "",
                "Adding village buildings only reaches villages. This reaches everything: vanilla",
                "structures, and in a pack with a structure mod the hundreds of houses scattered",
                "across the world, every one of which already says somebody works here by putting",
                "a workbench and an oven in the corner.",
                "",
                "Those blocks are already indoors, on a floor, where whoever built the structure",
                "thought a workbench belonged - so this inherits their judgement instead of guessing",
                "at a spot, and needs to know nothing about the mod that placed them.",
                "",
                "Only ever touches chunks as they generate, so a crafting table a player placed is",
                "never at risk, and a furnace with anything in it is always left alone."
        })
        @Config.RequiresMcRestart
        public boolean replaceWorkbenches = true;

        @Config.Comment({
                "Only touch structures that are part of a village.",
                "",
                "Without this, anything generated anywhere is fair game - a lodge in the woods, a",
                "dungeon library, a lone farm - and workstations end up scattered across the world",
                "where no villager will ever stand at one.",
                "",
                "A position counts when it falls inside a village structure piece, which is also how",
                "mods that add village buildings register them - they declare their houses as vanilla",
                "village pieces, so those are inside a village by this test as surely as vanilla's",
                "own are.",
                "",
                "Turn this off if a pack's world generator does not keep village structures. The test",
                "can only be answered by the generator, and one that cannot answer says no to",
                "everything - which turns all of this off rather than misplacing it."
        })
        public boolean villagesOnly = true;

        @Config.Comment({
                "How far from a village's centre still counts as part of it, in blocks.",
                "",
                "Villages sprawl, and the outlying field or the house at the end of the path is as",
                "much village as the well is. Too small and the edges of a large village are left out;",
                "too large and a lone structure a little way off gets swept in with it."
        })
        @Config.RangeInt(min = 16, max = 512)
        public int villageReach = 128;

        @Config.Comment({
                "Put a composter in among a large field of crops.",
                "",
                "A field is the one part of a village that plainly belongs to somebody and yet holds",
                "nothing to work at - the crops are the job, and there is no block that says so. So a",
                "field big enough to be worth farming gets a composter dropped into it, standing in",
                "for one of the crops, and the village gains a farmer it can actually employ.",
                "",
                "Mega villages benefit most: they are mostly fields and houses, and short on the",
                "crafting tables the rest of this works from."
        })
        public boolean composterInCropFields = true;

        @Config.Comment({
                "How many crop blocks must be in a chunk before it is worth a composter.",
                "",
                "Any crop counts, not only wheat - carrots, potatoes and beetroot are as much a farm as",
                "wheat is, and so is whatever a pack grows, since nearly all of them are built on the",
                "same crop block.",
                "",
                "Counted per chunk, so this is roughly 'a field this big'. A vanilla village field runs",
                "to about 28 crops in its fullest chunk, for scale."
        })
        @Config.RangeInt(min = 8, max = 256)
        public int cropsPerComposter = 20;

        @Config.Comment("Most composters one chunk of field may be given.")
        @Config.RangeInt(min = 1, max = 8)
        public int maxComposterPerChunk = 1;

        @Config.Comment({
                "Put a lectern in among a large wall of bookshelves.",
                "",
                "The same idea as the composter in a field: a library is unmistakably somewhere work",
                "happens and has nothing in it to work at. Some structure mods build very large ones,",
                "and a librarian is the trade most players actually want.",
                "",
                "One of the bookshelves becomes the lectern, so the room keeps its shape."
        })
        public boolean lecternInLibraries = true;

        @Config.Comment({
                "How many bookshelves must be in a chunk before it is worth a lectern.",
                "A shelf or two in somebody's house is not a library; a wall of them is."
        })
        @Config.RangeInt(min = 4, max = 256)
        public int bookshelvesPerLectern = 24;

        @Config.Comment("Most lecterns one chunk of library may be given.")
        @Config.RangeInt(min = 1, max = 8)
        public int maxLecternPerChunk = 1;

        @Config.Comment({
                "What may be replaced, how often, and with what.",
                "",
                "Each entry is 'block=chance' or 'block=chance=replacement,replacement'. The chance",
                "runs from 0 to 1 and is rolled per block. Naming replacements limits that block to",
                "those; leaving them off draws from every workstation in the mapping above.",
                "",
                "Furnaces are held to the two that are ovens, so a structure's hearth still reads as",
                "one. Crafting tables can become anything, which is what puts the variety about.",
                "",
                "Cauldrons are in the list because packs scatter far more of them than any village",
                "needs leatherworkers - a cauldron is already a workstation, so rerolling one trades a",
                "job nobody wanted for one somebody did. A block is never replaced with itself.",
                "",
                "Raise the chances for a world where nearly every house has a trade in it; lower them",
                "to keep a workbench meaning what it used to."
        })
        public String[] replacements = {
                "minecraft:crafting_table=0.6",
                "minecraft:cauldron=0.5",
                "minecraft:furnace=0.5=villagerbackport:smoker,villagerbackport:blast_furnace",
                "minecraft:lit_furnace=0.5=villagerbackport:smoker,villagerbackport:blast_furnace",
        };
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

    public static final class Homes {

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
                "eligible. Eligible means: employed, slept within the last 24000 ticks, worked at its",
                "job site within the last 36000, and no golem seen in the last 600.",
                "",
                "Those conditions are why this needs the rest of the mod running. A village whose",
                "villagers cannot reach a bed or a workstation will never produce a golem, exactly as",
                "in 1.14."
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
        public int sleepEndTime = 100;

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
                com.exiledradio.villagerbackport.village.StructureWorkstations.invalidate();
            }
        }

        /** Present so the class has a server-side reason to exist on a dedicated server. */
        @SubscribeEvent
        public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
            // No client sync needed in this phase - see the class comment on ModConfig.
        }
    }
}
