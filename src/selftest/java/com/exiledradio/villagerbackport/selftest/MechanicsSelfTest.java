package com.exiledradio.villagerbackport.selftest;

import com.exiledradio.backportcore.pose.Pose;
import com.exiledradio.backportcore.pose.Poses;
import com.exiledradio.villagerbackport.ModConfig;
import com.exiledradio.villagerbackport.illager.EntityPillager;
import com.exiledradio.villagerbackport.illager.EntityRavager;
import com.exiledradio.villagerbackport.mechanics.Cartography;
import com.exiledradio.villagerbackport.mechanics.EntityAIRaidBreakDoor;
import com.exiledradio.villagerbackport.mechanics.MechanicsSounds;
import com.exiledradio.villagerbackport.mechanics.ModernSiege;
import com.exiledradio.villagerbackport.trade.ModernTrades;
import com.google.common.base.Predicate;
import com.mojang.authlib.GameProfile;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.ai.EntityAITasks;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityTNTPrimed;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.entity.monster.EntityEvoker;
import net.minecraft.entity.monster.EntityGuardian;
import net.minecraft.entity.monster.EntitySkeleton;
import net.minecraft.entity.monster.EntityStray;
import net.minecraft.entity.monster.EntityVindicator;
import net.minecraft.entity.monster.EntityWitherSkeleton;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.passive.EntityParrot;
import net.minecraft.entity.passive.EntitySquid;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.passive.EntityWolf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.projectile.EntityTippedArrow;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemMap;
import net.minecraft.item.ItemRecord;
import net.minecraft.item.ItemStack;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.NetworkManager;
import net.minecraft.server.management.PlayerInteractionManager;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.village.MerchantRecipe;
import net.minecraft.village.MerchantRecipeList;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.MapDecoration;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.event.world.NoteBlockEvent;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * {@code -Pselftest=mechanics}: the 1.14 (and 1.18, 1.21.5) behavior changes in {@code mechanics/} and the
 * cartographer's table, each with its setting on, and the ones with a cheap "off" with it off. Everything is built
 * in the sky far from spawn, at x 12000.
 */
public final class MechanicsSelfTest implements SelfTest {

    private static final int X = 12000;
    private static final int Y = 200;
    private static final int Z = 12000;

    private WorldServer world;

    @Override
    public String name() {
        return "mechanics";
    }

    @Override
    public void run(Check c) throws Exception {
        this.world = DimensionManager.getWorld(0);
        this.world.getChunkProvider().provideChunk(X >> 4, Z >> 4);
        EnumDifficulty difficulty = this.world.getDifficulty();
        this.world.getWorldInfo().setDifficulty(EnumDifficulty.NORMAL);
        try {
            crouch(c);
            doors(c);
            explosions(c);
            sticks(c);
            illagers(c);
            invisibility(c);
            noteBlocks(c);
            parrots(c);
            despawns(c);
            sieges(c);
            chorus(c);
            discs(c);
            wolves(c);
            fuel(c);
            torches(c);
            cartographer(c);
        } finally {
            this.world.getWorldInfo().setDifficulty(difficulty);
        }
    }

    // ------------------------------------------------------------------------------------------------

    private BlockPos at(int dx, int dy, int dz) {
        return new BlockPos(X + dx, Y + dy, Z + dz);
    }

    private void fill(BlockPos from, BlockPos to, IBlockState state) {
        for (BlockPos p : BlockPos.getAllInBox(from, to)) {
            this.world.setBlockState(p, state, 2);
        }
    }

    private EntityPlayerMP player(String name) {
        EntityPlayerMP player = new EntityPlayerMP(this.world.getMinecraftServer(), this.world,
                new GameProfile(UUID.nameUUIDFromBytes(("vpselftest:" + name).getBytes()), "Mech" + name),
                new PlayerInteractionManager(this.world));
        NetworkManager manager = new NetworkManager(EnumPacketDirection.SERVERBOUND);
        new io.netty.channel.embedded.EmbeddedChannel(manager);
        player.connection = new NetHandlerPlayServer(this.world.getMinecraftServer(), manager, player);
        player.interactionManager.setGameType(GameType.SURVIVAL);
        ObfuscationReflectionHelper.setPrivateValue(EntityPlayerMP.class, player, 0, "field_147101_bU");
        return player;
    }

    private static boolean sized(EntityPlayer p, float h, float eyes) {
        return p.height == h && Math.abs(p.getEyeHeight() - eyes) < 1.0E-5F;
    }

    private static String size(EntityPlayer p) {
        return p.width + " x " + p.height + ", eyes " + p.getEyeHeight() + ", pose " + Poses.get(p);
    }

    @SuppressWarnings("unchecked")
    private static Predicate<Object> selector(EntityLiving mob, Class<?> targetClass) {
        for (EntityAITasks.EntityAITaskEntry e : mob.targetTasks.taskEntries) {
            if (e.action instanceof EntityAINearestAttackableTarget) {
                Class<?> cls = ObfuscationReflectionHelper.getPrivateValue(EntityAINearestAttackableTarget.class,
                        (EntityAINearestAttackableTarget<?>) e.action, "field_75307_b");
                if (cls == targetClass) {
                    return (Predicate<Object>) ObfuscationReflectionHelper.getPrivateValue(EntityAINearestAttackableTarget.class,
                            (EntityAINearestAttackableTarget<?>) e.action, "field_82643_g");
                }
            }
        }
        return null;
    }

    private <T extends EntityLiving> T spawn(T mob, BlockPos pos) {
        mob.setLocationAndAngles(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, 0.0F, 0.0F);
        this.world.spawnEntity(mob);
        return mob;
    }

    // ------------------------------------------------------------------------------------------------

    private void crouch(Check c) {
        boolean was = ModConfig.mechanics.shortCrouch;
        ModConfig.mechanics.shortCrouch = true;
        fill(at(-3, -1, -3), at(6, -1, 3), Blocks.STONE.getDefaultState());
        fill(at(-3, 0, -3), at(6, 3, 3), Blocks.AIR.getDefaultState());
        c.ok(Poses.active() && Poses.shortCrouch(), "short crouch: on in Backport Core's pose engine");
        EntityPlayerMP p = player("crouch");
        p.setLocationAndAngles(X + 0.5D, Y, Z + 0.5D, 0.0F, 0.0F);
        p.onUpdateEntity();
        c.ok(sized(p, 1.8F, 1.62F), "standing: 1.8, eyes 1.62 (" + size(p) + ")");
        p.setSneaking(true);
        p.onUpdateEntity();
        c.ok(Poses.get(p) == Pose.SNEAKING && sized(p, 1.5F, 1.27F), "sneaking: 1.5 tall, eyes 1.27 (" + size(p) + ")");
        // Under a 1.5 ceiling (top slabs one up), let go of sneak: held crouched.
        fill(at(-1, 1, -1), at(1, 1, 1), Blocks.STONE_SLAB.getDefaultState()
                .withProperty(net.minecraft.block.BlockSlab.HALF, net.minecraft.block.BlockSlab.EnumBlockHalf.TOP));
        p.onUpdateEntity();
        p.setSneaking(false);
        p.onUpdateEntity();
        c.ok(Poses.get(p) == Pose.SNEAKING && sized(p, 1.5F, 1.27F), "under a 1.5 ceiling, key up: held crouched (" + size(p) + ")");
        fill(at(-1, 1, -1), at(1, 1, 1), Blocks.AIR.getDefaultState());
        p.onUpdateEntity();
        c.ok(Poses.get(p) == Pose.STANDING && sized(p, 1.8F, 1.62F), "ceiling gone: stands (" + size(p) + ")");
        ModConfig.mechanics.shortCrouch = false;
        p.setSneaking(true);
        p.onUpdateEntity();
        p.onUpdateEntity();
        c.ok(sized(p, 1.65F, 1.54F), "setting off: 1.12.2's 1.65 crouch, eyes 1.54 (" + size(p) + ")");
        ModConfig.mechanics.shortCrouch = was;
    }

    private void doors(Check c) {
        EntityVindicator v = spawn(new EntityVindicator(this.world), at(0, 0, 0));
        boolean found = false;
        for (EntityAITasks.EntityAITaskEntry e : v.tasks.taskEntries) {
            found |= e.action instanceof EntityAIRaidBreakDoor;
        }
        c.ok(found, "vindicators get the raid door-breaking goal");
        int tasks = v.tasks.taskEntries.size();
        MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.entity.EntityJoinWorldEvent(v, this.world));
        c.eq(tasks, v.tasks.taskEntries.size(), "the goal is added once");
        EntityAIRaidBreakDoor goal = new EntityAIRaidBreakDoor(v);
        boolean any = false;
        for (int i = 0; i < 50; i++) {
            any |= goal.shouldExecute();
        }
        c.ok(!any, "no raid: the goal never starts");
        v.setDead();
    }

    private String lastItems = "";

    private int itemsIn(AxisAlignedBB box) {
        int n = 0;
        Map<String, Integer> kinds = new java.util.TreeMap<String, Integer>();
        for (EntityItem item : this.world.getEntitiesWithinAABB(EntityItem.class, box)) {
            n += item.getItem().getCount();
            kinds.merge(item.getItem().getItem().getRegistryName() + "@" + (int) item.posY, item.getItem().getCount(), Integer::sum);
            item.setDead();
        }
        this.lastItems = kinds.toString();
        return n;
    }

    private int countBlocks(BlockPos from, BlockPos to, net.minecraft.block.Block block) {
        int n = 0;
        for (BlockPos p : BlockPos.getAllInBox(from, to)) {
            if (this.world.getBlockState(p).getBlock() == block) {
                n++;
            }
        }
        return n;
    }

    private void explosions(Check c) {
        boolean was = ModConfig.mechanics.tntDropsAll;
        ModConfig.mechanics.tntDropsAll = true;
        // A 5 x 5 x 5 block of dirt; TNT in the middle.
        BlockPos o = at(30, 0, 0);
        fill(o.add(-7, -7, -7), o.add(7, 7, 7), Blocks.AIR.getDefaultState());
        fill(o.add(-2, -2, -2), o.add(2, 2, 2), Blocks.DIRT.getDefaultState());
        this.world.setBlockToAir(o);
        int before = countBlocks(o.add(-2, -2, -2), o.add(2, 2, 2), Blocks.DIRT);
        EntityTNTPrimed tnt = new EntityTNTPrimed(this.world, o.getX() + 0.5D, o.getY(), o.getZ() + 0.5D, null);
        this.world.newExplosion(tnt, o.getX() + 0.5D, o.getY() + 0.5D, o.getZ() + 0.5D, 4.0F, false, true);
        int broken = before - countBlocks(o.add(-2, -2, -2), o.add(2, 2, 2), Blocks.DIRT);
        int drops = itemsIn(new AxisAlignedBB(o).grow(8.0D));
        c.ok(broken > 20, "TNT broke the dirt (" + broken + ")");
        c.eq(broken, drops, "TNT: every broken block drops " + this.lastItems);

        // A creeper keeps 1.12.2's 1 in 3.
        BlockPos q = at(60, 0, 0);
        fill(q.add(-7, -7, -7), q.add(7, 7, 7), Blocks.AIR.getDefaultState());
        fill(q.add(-2, -2, -2), q.add(2, 2, 2), Blocks.DIRT.getDefaultState());
        this.world.setBlockToAir(q);
        before = countBlocks(q.add(-2, -2, -2), q.add(2, 2, 2), Blocks.DIRT);
        EntityCreeper creeper = new EntityCreeper(this.world);
        creeper.setPosition(q.getX() + 0.5D, q.getY(), q.getZ() + 0.5D);
        this.world.newExplosion(creeper, q.getX() + 0.5D, q.getY() + 0.5D, q.getZ() + 0.5D, 3.0F, false, true);
        broken = before - countBlocks(q.add(-2, -2, -2), q.add(2, 2, 2), Blocks.DIRT);
        drops = itemsIn(new AxisAlignedBB(q).grow(8.0D));
        c.ok(broken > 10 && drops < broken, "creeper: decay kept (" + drops + " of " + broken + ")");

        // TNT with the setting off: decay again.
        ModConfig.mechanics.tntDropsAll = false;
        BlockPos r = at(90, 0, 0);
        fill(r.add(-7, -7, -7), r.add(7, 7, 7), Blocks.AIR.getDefaultState());
        fill(r.add(-2, -2, -2), r.add(2, 2, 2), Blocks.DIRT.getDefaultState());
        this.world.setBlockToAir(r);
        before = countBlocks(r.add(-2, -2, -2), r.add(2, 2, 2), Blocks.DIRT);
        this.world.newExplosion(new EntityTNTPrimed(this.world, r.getX() + 0.5D, r.getY(), r.getZ() + 0.5D, null),
                r.getX() + 0.5D, r.getY() + 0.5D, r.getZ() + 0.5D, 4.0F, false, true);
        broken = before - countBlocks(r.add(-2, -2, -2), r.add(2, 2, 2), Blocks.DIRT);
        drops = itemsIn(new AxisAlignedBB(r).grow(8.0D));
        c.ok(drops < broken, "setting off: TNT decays (" + drops + " of " + broken + ")");
        ModConfig.mechanics.tntDropsAll = was;
    }

    private int sticksFrom(IBlockState leaves, int fortune, boolean silk, int tries) {
        int sticks = 0;
        BlockPos pos = at(0, 5, 0);
        for (int i = 0; i < tries; i++) {
            NonNullList<ItemStack> drops = NonNullList.create();
            leaves.getBlock().getDrops(drops, this.world, pos, leaves, fortune);
            ForgeEventFactory.fireBlockHarvesting(drops, this.world, pos, leaves, fortune, 1.0F, silk, null);
            for (ItemStack s : drops) {
                if (s.getItem() == Items.STICK) {
                    if (s.getCount() < 1 || s.getCount() > 2) {
                        return -1;
                    }
                    sticks++;
                }
            }
        }
        return sticks;
    }

    private void sticks(Check c) {
        IBlockState oak = Blocks.LEAVES.getDefaultState();
        int n = sticksFrom(oak, 0, false, 10000);
        c.ok(n >= 140 && n <= 270, "oak leaves: sticks about 2% of the time (" + n + " in 10000)");
        int f3 = sticksFrom(Blocks.LEAVES2.getDefaultState(), 3, false, 10000);
        c.ok(f3 >= 250 && f3 <= 430, "acacia leaves, Fortune III: about 3.3% (" + f3 + " in 10000)");
        c.eq(0, sticksFrom(oak, 0, true, 2000), "Silk Touch: no sticks");
        ModConfig.mechanics.leavesDropSticks = false;
        c.eq(0, sticksFrom(oak, 0, false, 2000), "setting off: no sticks");
        ModConfig.mechanics.leavesDropSticks = true;
    }

    private void illagers(Check c) {
        EntityVillager adult = spawn(new EntityVillager(this.world), at(2, 0, 0));
        EntityVillager baby = spawn(new EntityVillager(this.world), at(2, 0, 1));
        baby.setGrowingAge(-24000);
        EntityLiving[] illagers = {new EntityVindicator(this.world), new EntityEvoker(this.world),
                new net.minecraft.entity.monster.EntityIllusionIllager(this.world), new EntityPillager(this.world), new EntityRavager(this.world)};
        for (EntityLiving mob : illagers) {
            spawn(mob, at(0, 0, 0));
            Predicate<Object> s = selector(mob, EntityVillager.class);
            String n = mob.getClass().getSimpleName();
            if (!c.ok(s != null, n + ": has a villager target task")) {
                continue;
            }
            c.ok(s.apply(adult), n + ": targets an adult villager");
            c.ok(!s.apply(baby), n + ": leaves a baby villager alone");
            ModConfig.mechanics.illagersSpareBabies = false;
            c.ok(s.apply(baby), n + ": setting off, targets a baby (1.12.2)");
            ModConfig.mechanics.illagersSpareBabies = true;
            mob.setDead();
        }
        adult.setDead();
        baby.setDead();
    }

    private void invisibility(Check c) {
        EntityZombie zombie = spawn(new EntityZombie(this.world), at(0, 0, 0));
        EntityVillager near = spawn(new EntityVillager(this.world), at(1, 0, 0));
        EntityVillager far = spawn(new EntityVillager(this.world), at(10, 0, 0));
        Predicate<Object> s = selector(zombie, EntityVillager.class);
        c.ok(s != null && s.apply(far), "zombie: sees a villager 10 blocks off");
        far.setInvisible(true);
        near.setInvisible(true);
        c.ok(!s.apply(far), "zombie: an invisible villager 10 blocks off goes unseen");
        c.ok(s.apply(near), "zombie: an invisible villager 1 block off is seen (2-block minimum)");
        ModConfig.mechanics.invisibilityHides = false;
        c.ok(s.apply(far), "setting off: the zombie sees it (1.12.2)");
        ModConfig.mechanics.invisibilityHides = true;
        EntityVindicator vindicator = spawn(new EntityVindicator(this.world), at(0, 0, 2));
        c.ok(!selector(vindicator, EntityVillager.class).apply(far), "vindicator: an invisible villager goes unseen");

        EntityGuardian guardian = spawn(new EntityGuardian(this.world), at(0, 0, 4));
        EntitySquid squid = spawn(new EntitySquid(this.world), at(9, 0, 4));
        Predicate<Object> g = selector(guardian, EntityLivingBase.class);
        c.ok(g != null && g.apply(squid), "guardian: sees a squid 9 blocks off");
        squid.setInvisible(true);
        c.ok(!g.apply(squid), "guardian: an invisible squid 9 blocks off goes unseen");
        // The rule itself: an invisible player with no armor, 0.7 x 0.1 of the range; each armor piece more.
        EntityPlayerMP p = player("invisible");
        p.setInvisible(true);
        c.ok(Math.abs(com.exiledradio.villagerbackport.mechanics.Targeting.visibility(guardian, p) - 0.07D) < 1.0E-6D, "visibility: invisible, bare, 0.07");
        p.setItemStackToSlot(net.minecraft.inventory.EntityEquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
        p.setItemStackToSlot(net.minecraft.inventory.EntityEquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        c.ok(Math.abs(com.exiledradio.villagerbackport.mechanics.Targeting.visibility(guardian, p) - 0.35D) < 1.0E-6D, "visibility: invisible, two pieces, 0.35");
        for (EntityLiving e : new EntityLiving[] {zombie, near, far, vindicator, guardian, squid}) {
            e.setDead();
        }
    }

    private void noteBlocks(Check c) {
        BlockPos note = at(0, 0, 8);
        this.world.setBlockState(note, Blocks.NOTEBLOCK.getDefaultState(), 2);
        Object[][] cases = {{Blocks.IRON_BLOCK, "iron_xylophone"}, {Blocks.SOUL_SAND, "cow_bell"}, {Blocks.PUMPKIN, "didgeridoo"},
                {Blocks.EMERALD_BLOCK, "bit"}, {Blocks.HAY_BLOCK, "banjo"}, {Blocks.GLOWSTONE, "pling"}};
        for (Object[] k : cases) {
            this.world.setBlockState(note.down(), ((net.minecraft.block.Block) k[0]).getDefaultState(), 2);
            NoteBlockEvent.Play e = new NoteBlockEvent.Play(this.world, note, Blocks.NOTEBLOCK.getDefaultState(), 5, 0);
            MinecraftForge.EVENT_BUS.post(e);
            net.minecraft.util.SoundEvent sound = MechanicsSounds.instrument(this.world.getBlockState(note.down()));
            c.ok(e.isCanceled() && sound != null && sound.getRegistryName().getPath().equals("block.note_block." + k[1]),
                    "note block on " + k[0] + ": plays " + k[1]);
            c.ok(ForgeRegistries.SOUND_EVENTS.containsKey(new ResourceLocation("villagerbackport", "block.note_block." + k[1])),
                    "sound registered: " + k[1]);
        }
        this.world.setBlockState(note.down(), Blocks.STONE.getDefaultState(), 2);
        NoteBlockEvent.Play stone = new NoteBlockEvent.Play(this.world, note, Blocks.NOTEBLOCK.getDefaultState(), 5, 1);
        MinecraftForge.EVENT_BUS.post(stone);
        c.ok(!stone.isCanceled(), "note block on stone: vanilla's bass drum");
        ModConfig.mechanics.noteBlockInstruments = false;
        this.world.setBlockState(note.down(), Blocks.GLOWSTONE.getDefaultState(), 2);
        NoteBlockEvent.Play off = new NoteBlockEvent.Play(this.world, note, Blocks.NOTEBLOCK.getDefaultState(), 5, 2);
        MinecraftForge.EVENT_BUS.post(off);
        c.ok(!off.isCanceled(), "setting off: glowstone stays vanilla's");
        ModConfig.mechanics.noteBlockInstruments = true;
    }

    @SuppressWarnings("unchecked")
    private void parrots(Check c) {
        Map<Class<?>, net.minecraft.util.SoundEvent> mimic = ObfuscationReflectionHelper.getPrivateValue(EntityParrot.class, null, "MIMIC_SOUNDS");
        c.ok(mimic.get(EntityPillager.class) == MechanicsSounds.imitatePillager && MechanicsSounds.imitatePillager != null, "parrots imitate pillagers");
        c.ok(mimic.get(EntityRavager.class) == MechanicsSounds.imitateRavager && MechanicsSounds.imitateRavager != null, "parrots imitate ravagers");
    }

    private void despawns(Check c) {
        EntityPlayerMP p = player("despawn");
        p.setLocationAndAngles(X + 0.5D, Y, Z + 0.5D, 0.0F, 0.0F);
        this.world.playerEntities.add(p);
        try {
            EntityZombie far = new EntityZombie(this.world);
            far.setLocationAndAngles(X + 200.5D, Y, Z + 0.5D, 0.0F, 0.0F);
            c.eq(Event.Result.DENY, ForgeEventFactory.canEntitySpawn(far, this.world, (float) far.posX, (float) far.posY, (float) far.posZ, false),
                    "a zombie 200 blocks from the only player is refused");
            EntityZombie near = new EntityZombie(this.world);
            near.setLocationAndAngles(X + 60.5D, Y, Z + 0.5D, 0.0F, 0.0F);
            c.eq(Event.Result.DEFAULT, ForgeEventFactory.canEntitySpawn(near, this.world, (float) near.posX, (float) near.posY, (float) near.posZ, false),
                    "a zombie 60 blocks off: left to vanilla");
            c.eq(Event.Result.DEFAULT, ForgeEventFactory.canEntitySpawn(far, this.world, (float) far.posX, (float) far.posY, (float) far.posZ, true),
                    "a spawner's zombie far away: left to vanilla");
            net.minecraft.entity.passive.EntityCow cow = new net.minecraft.entity.passive.EntityCow(this.world);
            cow.setLocationAndAngles(X + 200.5D, Y, Z + 0.5D, 0.0F, 0.0F);
            c.eq(Event.Result.DEFAULT, ForgeEventFactory.canEntitySpawn(cow, this.world, (float) cow.posX, (float) cow.posY, (float) cow.posZ, false),
                    "a cow far away (never despawns): left to vanilla");
            ModConfig.mechanics.refuseInstantDespawns = false;
            c.eq(Event.Result.DEFAULT, ForgeEventFactory.canEntitySpawn(far, this.world, (float) far.posX, (float) far.posY, (float) far.posZ, false),
                    "setting off: left to vanilla");
            ModConfig.mechanics.refuseInstantDespawns = true;
        } finally {
            this.world.playerEntities.remove(p);
        }
    }

    private void sieges(Check c) {
        ModernSiege siege = ModernSiege.of(this.world);
        if (!c.ok(siege != null, "the overworld's siege is 1.14's")) {
            return;
        }
        long time = this.world.getWorldTime();
        this.world.setWorldTime(18000L);
        siege.forceTonight();
        for (int i = 0; i < 10; i++) {
            siege.tick();
        }
        c.eq(0, siege.zombiesToSpawn(), "no player in a village: no siege");
        this.world.setWorldTime(time);
    }

    private void chorus(Check c) {
        BlockPos flower = at(0, 0, 12);
        this.world.setBlockState(flower.down(), Blocks.END_STONE.getDefaultState(), 2);
        this.world.setBlockState(flower, Blocks.CHORUS_FLOWER.getDefaultState(), 2);
        EntityTippedArrow arrow = new EntityTippedArrow(this.world, flower.getX() + 0.5D, flower.getY() + 3.0D, flower.getZ() + 0.5D);
        RayTraceResult hit = new RayTraceResult(new Vec3d(flower.getX() + 0.5D, flower.getY() + 1.0D, flower.getZ() + 0.5D), EnumFacing.UP, flower);
        MinecraftForge.EVENT_BUS.post(new ProjectileImpactEvent.Arrow(arrow, hit));
        c.ok(this.world.isAirBlock(flower), "an arrow breaks a chorus flower");
        boolean dropped = false;
        for (EntityItem item : this.world.getEntitiesWithinAABB(EntityItem.class, new AxisAlignedBB(flower).grow(2.0D))) {
            dropped |= item.getItem().getItem() == net.minecraft.item.Item.getItemFromBlock(Blocks.CHORUS_FLOWER);
            item.setDead();
        }
        c.ok(dropped, "the chorus flower drops");
    }

    private List<EntityItem> dropsOfCreeperKilledBy(EntityLivingBase killer) {
        EntityCreeper creeper = new EntityCreeper(this.world);
        creeper.setPosition(X, Y, Z);
        ArrayList<EntityItem> drops = new ArrayList<EntityItem>();
        MinecraftForge.EVENT_BUS.post(new LivingDropsEvent(creeper, DamageSource.causeMobDamage(killer), drops, 0, true));
        return drops;
    }

    private void discs(Check c) {
        boolean disc = false;
        for (EntityItem i : dropsOfCreeperKilledBy(new EntityStray(this.world))) {
            disc |= i.getItem().getItem() instanceof ItemRecord;
        }
        c.ok(disc, "a creeper killed by a stray drops a disc");
        disc = false;
        for (EntityItem i : dropsOfCreeperKilledBy(new EntityWitherSkeleton(this.world))) {
            disc |= i.getItem().getItem() instanceof ItemRecord;
        }
        c.ok(disc, "a creeper killed by a wither skeleton drops a disc");
        c.eq(0, dropsOfCreeperKilledBy(new EntitySkeleton(this.world)).size(), "a skeleton's kill: left to vanilla (no second disc)");
        c.eq(0, dropsOfCreeperKilledBy(new EntityZombie(this.world)).size(), "a zombie's kill: no disc");
    }

    private void wolves(Check c) {
        EntityPlayerMP owner = player("wolfowner");
        owner.setLocationAndAngles(X, Y, Z, 0.0F, 0.0F);
        this.world.playerEntities.add(owner);
        try {
            EntityWolf wolf = new EntityWolf(this.world);
            wolf.setPosition(X, Y, Z);
            wolf.setTamedBy(owner);
            EntityZombie victim = new EntityZombie(this.world);
            victim.setPosition(X + 1, Y, Z);
            MinecraftForge.EVENT_BUS.post(new LivingDeathEvent(victim, DamageSource.causeMobDamage(wolf)));
            EntityPlayer credited = ObfuscationReflectionHelper.getPrivateValue(EntityLivingBase.class, victim, "field_70717_bb");
            c.ok(credited == owner, "a tamed wolf's kill counts as its owner's");
            EntityWolf wild = new EntityWolf(this.world);
            EntityZombie other = new EntityZombie(this.world);
            MinecraftForge.EVENT_BUS.post(new LivingDeathEvent(other, DamageSource.causeMobDamage(wild)));
            c.ok(ObfuscationReflectionHelper.getPrivateValue(EntityLivingBase.class, other, "field_70717_bb") == null, "a wild wolf's kill counts for no one");
        } finally {
            this.world.playerEntities.remove(owner);
        }
    }

    private void fuel(Check c) {
        c.eq(100, TileEntityFurnace.getItemBurnTime(new ItemStack(Blocks.DEADBUSH)), "a dead bush burns for 100 ticks");
        ModConfig.mechanics.deadBushFuel = false;
        c.eq(0, TileEntityFurnace.getItemBurnTime(new ItemStack(Blocks.DEADBUSH)), "setting off: no fuel");
        ModConfig.mechanics.deadBushFuel = true;
    }

    private void torches(Check c) {
        BlockPos pane = at(0, 0, 16);
        this.world.setBlockState(pane, Blocks.GLASS_PANE.getDefaultState(), 2);
        this.world.setBlockToAir(pane.up());
        c.ok(Blocks.TORCH.canPlaceBlockAt(this.world, pane.up()), "a torch stands on a glass pane");
        this.world.setBlockState(pane, Blocks.IRON_BARS.getDefaultState(), 2);
        c.ok(Blocks.TORCH.canPlaceBlockAt(this.world, pane.up()), "a torch stands on iron bars");
        ModConfig.mechanics.torchesOnPanes = false;
        c.ok(!Blocks.TORCH.canPlaceBlockAt(this.world, pane.up()), "setting off: not on iron bars (1.12.2)");
        ModConfig.mechanics.torchesOnPanes = true;
        // Already so in 1.12.2 (glass's top is a solid face): checked, nothing to add.
        this.world.setBlockState(pane, Blocks.GLASS.getDefaultState(), 2);
        c.ok(Blocks.REDSTONE_WIRE.canPlaceBlockAt(this.world, pane.up()), "redstone dust goes on glass (already 1.12.2's)");
    }

    private void cartographer(Check c) {
        for (String icon : new String[] {"plains_village", "desert_village", "savanna_village", "snowy_village", "taiga_village",
                "swamp_hut", "jungle_temple", "trial_chambers"}) {
            c.ok(Cartography.icon(icon).ordinal() >= 10, "map icon added: " + icon);
        }
        String key = "minecraft:librarian/cartographer";
        EntityVillager v = spawn(new EntityVillager(this.world), at(0, 0, 20));
        com.exiledradio.villagerbackport.data.VillagerTypes.set(v, "plains");
        boolean was = ModConfig.trades.modernCartographer;
        ModConfig.trades.modernCartographer = true;
        Random random = new Random(42L);
        int maps = 0;
        boolean wrongType = false;
        for (int round = 0; round < 6; round++) {
            MerchantRecipeList list = new MerchantRecipeList();
            int added = ModernTrades.addTier(key, 2, v, list, random);
            c.ok(added >= 1, "apprentice: trades offered (" + added + ")");
            for (MerchantRecipe r : list) {
                ItemStack sold = r.getItemToSell();
                if (sold.getItem() == Items.FILLED_MAP) {
                    maps++;
                    String name = sold.hasDisplayName() ? "" : sold.getTagCompound().getCompoundTag("display").getString("LocName");
                    // A plains cartographer sells the taiga and savanna village maps, nothing else.
                    wrongType |= !name.equals("filled_map.village_taiga") && !name.equals("filled_map.village_savanna");
                    c.eq(8, r.getItemToBuy().getCount(), "a village map costs 8 emeralds");
                    c.ok(r.getSecondItemToBuy().getItem() == Items.COMPASS, "and a compass");
                }
            }
        }
        c.ok(maps > 0, "apprentice plains cartographers sell village maps (" + maps + ")");
        c.ok(!wrongType, "only the taiga and savanna village maps for a plains cartographer");
        boolean trial = false;
        boolean monument = false;
        for (int round = 0; round < 6; round++) {
            MerchantRecipeList list = new MerchantRecipeList();
            ModernTrades.addTier(key, 3, v, list, random);
            for (MerchantRecipe r : list) {
                String name = r.getItemToSell().hasTagCompound() ? r.getItemToSell().getTagCompound().getCompoundTag("display").getString("LocName") : "";
                trial |= "filled_map.trial_chambers".equals(name);
                monument |= "filled_map.monument".equals(name);
            }
        }
        boolean tricky = com.exiledradio.backportcore.locate.StructureLocate.known("minecraft:trial_chambers");
        c.ok(tricky || !trial, "no trial chambers map without a mod that builds them (known: " + tricky + ")");
        // Legacy: 1.14's table, the monument map at apprentice.
        ModConfig.trades.modernCartographer = false;
        boolean legacyMonument = false;
        for (int round = 0; round < 4; round++) {
            MerchantRecipeList list = new MerchantRecipeList();
            ModernTrades.addTier(key, 2, v, list, random);
            for (MerchantRecipe r : list) {
                legacyMonument |= r.getItemToSell().getItem() == Items.FILLED_MAP;
                c.ok(r.getItemToSell().getItem() == Items.FILLED_MAP || r.getItemToBuy().getItem() == net.minecraft.item.Item.getItemFromBlock(Blocks.GLASS_PANE),
                        "setting off: 1.14's apprentice trades");
            }
        }
        ModConfig.trades.modernCartographer = was;
        v.setDead();
    }
}
