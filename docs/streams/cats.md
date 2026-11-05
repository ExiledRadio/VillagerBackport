# Stream B: cats

Branch `v14-cats`. Package `com.exiledradio.villagerbackport.cat` (client half in `cat/client/`). Entity id 107.
Packet ids 12-13 are **unused**: everything a client needs (coat, collar, lying, settling) travels as entity
data parameters, so no packet was needed. Config category `ModConfig.cats`.

## Changelog section

## New: cats

Cats and ocelots are two animals, as in 1.14. Old worlds' tamed ocelots become cats.

*   **Cats**, with 1.14's model, sounds and all **eleven coats**: tabby, tuxedo, red, Siamese, British shorthair, calico, Persian, ragdoll, white, Jellie and black. Black cats belong to witches, or turn up under a full moon.
*   **Taming**: stray cats run from you, but creep after you if you're holding raw cod or salmon, meowing for it. Feed one and, one time in three, it's yours, and sits.
*   **Tamed cats** follow you, sit or stand when you use them, heal on fish and breed on fish. A kitten takes one parent's coat, and its owner. Their **collar** starts red; use a dye on it to change it.
*   They sit on **chests** (which then won't open), lit **furnaces** and the foot of **beds**, and lie down on beds.
*   **Morning gifts**: a cat curls up at the foot of the bed while you sleep, and in the morning has seven chances in ten of having brought you something: string, feathers, rabbit hide or foot, raw chicken or rotten flesh - or a phantom membrane, if a mod adds one.
*   **Creepers keep away from cats**, as from ocelots. With a mod that adds **phantoms**, a phantom breaks off its attack when a cat is within 16 blocks, and the cat hisses at it.
*   **Where they live**: villages have cats from the start (1.14's own cat spots in each village), and more strays turn up in a village with more than four beds villagers sleep in, up to five cats. A black cat comes to a **witch hut** that has none, and stays.
*   **A Complete Catalogue**: tame one of every coat.
*   Spawn egg in the creative tab.

## Changed: ocelots

*   **Ocelots can't be tamed any more**, as in 1.14. Feed one raw cod or salmon instead and, one time in three, it comes to **trust** you: it stops running from players and won't despawn. A trusting ocelot breeds on fish.
*   Ocelots have 1.14's own calls and death sounds.
*   **Tamed ocelots in old worlds become cats** when they load, keeping their owner, name, age and sitting: the tuxedo, red and Siamese ocelots become those cats, as 1.14's world upgrade did.

## Config (`cats`)

*   `enabled` - cats and 1.14's ocelots at all (restart). Off: no cat entity, ocelots as 1.12.2 has them.
*   `spawnInVillages`, `spawnAtWitchHuts` - the two cat spawners.
*   `modernOcelots` - ocelots trust instead of taming (restart).
*   `convertTamedOcelots` - old tamed ocelots become cats.
*   `creepersFearCats` (restart), `phantomsFearCats`.

Future MC (0.2.21, checked) adds no cats and no ocelot changes, so no step-aside mode was needed.

## Notes for the integrator

*   Shared files touched: `ModConfig.java` (only inside `Cats`), `VillagerBackport.java` (one line registering the
    dev-only `/cattest`), `village/modern/ModernVillagePiece.java` (a `"cat"` kind that asks `EntityCat.fromTemplate`),
    `advancements/_factories.json` (the `cats` condition), `sounds.json` and `en_us.lang` (appended),
    `tools/village-port/convert.py` (now keeps cat entities), and the ten `structures/village/common/animals/cat_*.nbt`
    templates, which gained their cat (kind `cat`, `variant` = 1.14's `CatType`; they had been converted without it).
*   `/cattest` is registered only when `fml.deobfuscatedEnvironment` is set, so it never ships to players.
*   1.14's own `cat_jellie` piece really holds an all-black cat (CatType 10); kept as 1.14 has it.

## Tested headlessly (dedicated dev server, `runServer`, flat world, `/cattest`)

All on the dedicated server, which also proves the client classes stay off the server's class path.

*   Taming: raw cod/salmon tames one time in three; owner set; tamed cat ordered to sit; owner's empty hand withdraws
    the order; owner's blue dye recolors the collar; clownfish never tames.
*   Taming all eleven coats grants *A Complete Catalogue* (11 of 11 criteria; advancement loads under its condition).
*   Ocelots: exactly one flee goal (the trust-aware one replaces 1.12.2's); fish through the real Forge interaction
    event earns trust, never tames; trusting ocelot is persistent and goes into love on salmon.
*   Conversion: four tamed ocelots (skins 0-3) joining the world become tabby/tuxedo/red/Siamese cats with owner,
    name and sit order kept, and no tamed ocelot left behind.
*   Village spawning: six claimed beds count as six occupied homes; the spot is a village; the spawner makes stray,
    non-persistent cats until there are five in range and then stops; no cat away from the village.
*   Morning gift: 20 gifts drop 20 items from the 1.14 table.
*   NBT round trip of coat, collar and owner; two tamed cats in love can mate, kitten gets a parent's coat and the
    owner; strays can't breed.
*   Creepers get the avoid-cats goal once.
*   All ten village cat templates carry a cat that `fromTemplate` builds as persistent.
*   `build --rerun-tasks -PdeployDir=` clean; release jar checked to be reobfuscated (SRG names).

## Not tested (needs the in-game check)

*   Anything visual: the model, the eleven textures, collar tinting, sitting/creeping/running poses, the lying-down
    roll and the head-lowered settle, kitten scale.
*   Sounds in game (beg, eat, stray meow, purring while lying, ocelot calls and death, hiss).
*   AI over time with a real player: following, sit-on-chest/furnace/bed, lie-on-bed, curling up on a sleeping
    owner's bed and the gift arriving on waking at dawn (the gift itself is tested; the dawn trigger isn't).
*   Cat on a chest blocking it from opening (needs a client click).
*   A real village generating its cat pieces (templates and `fromTemplate` are tested; generation isn't).
*   Witch hut black cat (needs a swamp hut; only "spawn isn't a witch hut" is tested).
*   Phantoms fleeing cats (needs a phantom mod in the dev env).
*   Tamed ocelots converting when loaded from an old save on disk (tested through spawning, which fires the same
    join event).
