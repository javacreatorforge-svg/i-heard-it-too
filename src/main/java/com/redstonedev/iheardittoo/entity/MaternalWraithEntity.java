package com.redstonedev.iheardittoo.entity;

import com.redstonedev.iheardittoo.init.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreakDoorGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WallClimberNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.PlayState;
import software.bernie.geckolib3.core.builder.AnimationBuilder;
import software.bernie.geckolib3.core.builder.ILoopType.EDefaultLoopTypes;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;
import software.bernie.geckolib3.util.GeckoLibUtil;

public class MaternalWraithEntity extends Monster implements IAnimatable {

    /** Behavior modes. STALKING is the default; transitions to WANDERING after 3 min unnoticed,
     *  or to AGGRESSIVE on trigger. AGGRESSIVE becomes ENRAGED after chasing too long. */
    public enum Mode { STALKING, WANDERING, AGGRESSIVE, ENRAGED }

    /** Per-spawn chase gait pick - 50/50 RUN or CRAWL. */
    public enum Gait { RUN, CRAWL }

    private static final EntityDataAccessor<Integer> DATA_MODE =
            SynchedEntityData.defineId(MaternalWraithEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_GAIT =
            SynchedEntityData.defineId(MaternalWraithEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_CLIMBING =
            SynchedEntityData.defineId(MaternalWraithEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_TALKING =
            SynchedEntityData.defineId(MaternalWraithEntity.class, EntityDataSerializers.BOOLEAN);

    // Speed tuning
    private static final double SPEED_STALK   = 0.10D;   // crawls in slowly while stalking
    private static final double SPEED_WANDER  = 0.18D;
    private static final double SPEED_RUN     = 0.32D;   // fast but outrunnable while sprinting
    private static final double SPEED_CRAWL   = 0.38D;   // even faster - catches up
    private static final double SPEED_ENRAGED = 0.45D;   // mad sarah - even faster on top of that

    private final AnimationFactory factory = GeckoLibUtil.createFactory(this);

    private int aliveTicks = 0;
    private int lastNoticedTicks = 0;
    private int chaseStartTicks = -1;     // when AGGRESSIVE started
    private int wanderStartTicks = -1;
    private int sarahVoiceCooldown;       // stalking ambient
    private int chaseSarahCooldown;       // chase shout
    private int talkingTicksRemaining = 0;
    private boolean clientChaseSoundStarted = false;

    public MaternalWraithEntity(EntityType<? extends MaternalWraithEntity> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
        this.xpReward = 0;
        this.maxUpStep = 1.0F;
        this.sarahVoiceCooldown = 60 + this.random.nextInt(80); // first voice ~3-7s after spawn
        this.chaseSarahCooldown = 200;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 80.0D)
                .add(Attributes.ATTACK_DAMAGE, 12.0D)
                .add(Attributes.MOVEMENT_SPEED, SPEED_STALK)
                .add(Attributes.FOLLOW_RANGE, 64.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_MODE, Mode.STALKING.ordinal());
        this.entityData.define(DATA_GAIT, Gait.RUN.ordinal());
        this.entityData.define(DATA_CLIMBING, false);
        this.entityData.define(DATA_TALKING, false);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        WallClimberNavigation nav = new WallClimberNavigation(this, level);
        nav.setCanOpenDoors(true);
        nav.setCanPassDoors(true);
        return nav;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new BreakDoorGoal(this, d -> true));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, true));
        this.goalSelector.addGoal(3, new OpenDoorGoal(this, true));
        this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 64.0F, 1.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1,
                new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    // === Accessors ============================================================

    public Mode getMode() {
        int idx = this.entityData.get(DATA_MODE);
        Mode[] vs = Mode.values();
        return vs[Math.max(0, Math.min(vs.length - 1, idx))];
    }

    public Gait getGait() {
        int idx = this.entityData.get(DATA_GAIT);
        Gait[] vs = Gait.values();
        return vs[Math.max(0, Math.min(vs.length - 1, idx))];
    }

    public boolean isTalking()  { return this.entityData.get(DATA_TALKING); }
    public boolean isClimbing() { return this.entityData.get(DATA_CLIMBING); }

    public boolean isAggressiveMode() { return getMode() == Mode.AGGRESSIVE || getMode() == Mode.ENRAGED; }

    public void setMode(Mode mode) {
        Mode old = getMode();
        this.entityData.set(DATA_MODE, mode.ordinal());
        if (old != mode) {
            if (mode == Mode.AGGRESSIVE) {
                // 50/50 run or crawl - per spec.
                Gait g = this.random.nextBoolean() ? Gait.RUN : Gait.CRAWL;
                this.entityData.set(DATA_GAIT, g.ordinal());
                chaseStartTicks = aliveTicks;
                if (!this.level.isClientSide) {
                    this.level.playSound(null, this.getX(), this.getY(), this.getZ(),
                            ModSounds.CHASE_SARAH.get(), SoundSource.HOSTILE, 1.2F, 1.0F);
                }
            } else if (mode == Mode.ENRAGED) {
                if (!this.level.isClientSide) {
                    this.level.playSound(null, this.getX(), this.getY(), this.getZ(),
                            ModSounds.MAD_SARAH.get(), SoundSource.HOSTILE, 1.5F, 1.0F);
                }
            } else if (mode == Mode.WANDERING) {
                wanderStartTicks = aliveTicks;
                stopChaseThemeForNearbyPlayers();
            } else if (mode == Mode.STALKING) {
                stopChaseThemeForNearbyPlayers();
            }
        }
    }

    public void setClimbing(boolean climbing) { this.entityData.set(DATA_CLIMBING, climbing); }

    private void setTalking(boolean talking) { this.entityData.set(DATA_TALKING, talking); }

    @Override public boolean onClimbable() { return this.isClimbing(); }

    // === Tick =================================================================

    @Override
    public void tick() {
        super.tick();
        if (this.level.isClientSide) {
            if (!clientChaseSoundStarted && isAggressiveMode()) {
                clientChaseSoundStarted = true;
                net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(
                        net.minecraftforge.api.distmarker.Dist.CLIENT,
                        () -> () -> com.redstonedev.iheardittoo.client.sound.ClientChaseSoundStarter.start(this));
            }
            if (clientChaseSoundStarted && !isAggressiveMode()) {
                clientChaseSoundStarted = false;
            }
        } else {
            // Wall climbing detection
            this.setClimbing(this.horizontalCollision);
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level.isClientSide) return;

        aliveTicks++;

        // 8-minute hard lifetime cap.
        if (aliveTicks >= 9600) {
            this.discard();
            return;
        }

        if (sarahVoiceCooldown > 0) sarahVoiceCooldown--;
        if (chaseSarahCooldown > 0) chaseSarahCooldown--;
        if (talkingTicksRemaining > 0) {
            talkingTicksRemaining--;
            if (talkingTicksRemaining == 0) setTalking(false);
        }

        // Track player notice for the 3-min stalking timer.
        Player nearest = this.level.getNearestPlayer(this, 64.0D);
        if (nearest != null) {
            if (this.distanceTo(nearest) < 12.0D || isPlayerStaringAt(nearest)) {
                lastNoticedTicks = aliveTicks;
            }
        }

        // Sync the desired speed to whatever mode demands.
        double targetSpeed = computeTargetSpeed();
        AttributeInstance attr = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attr != null && Math.abs(attr.getBaseValue() - targetSpeed) > 1e-6) {
            attr.setBaseValue(targetSpeed);
        }

        Mode mode = getMode();

        if (mode == Mode.STALKING) {
            tickStalking(nearest);
        } else if (mode == Mode.WANDERING) {
            tickWandering(nearest);
        } else if (mode == Mode.AGGRESSIVE) {
            tickAggressive(nearest);
        } else { // ENRAGED
            tickEnraged(nearest);
        }
    }

    private double computeTargetSpeed() {
        Mode m = getMode();
        if (m == Mode.STALKING)  return SPEED_STALK;
        if (m == Mode.WANDERING) return SPEED_WANDER;
        if (m == Mode.ENRAGED)   return SPEED_ENRAGED;
        // AGGRESSIVE - crawl is faster than run, per spec.
        return getGait() == Gait.CRAWL ? SPEED_CRAWL : SPEED_RUN;
    }

    // --- STALKING -------------------------------------------------------------

    private void tickStalking(Player nearest) {
        if (nearest != null) {
            lockYawTo(nearest);

            // Aggression triggers per spec:
            //   - player sprints near her (loud noise)
            //   - player gets too close
            //   - player is being attacked / hits her (handled in hurt())
            double dist = this.distanceTo(nearest);
            if (nearest.isSprinting() && dist < 24.0D) {
                setMode(Mode.AGGRESSIVE);
                return;
            }
            if (dist < 3.5D) {
                setMode(Mode.AGGRESSIVE);
                return;
            }
        }

        // Sarah voice every ~10s with talking animation overlaid.
        if (sarahVoiceCooldown <= 0) {
            this.level.playSound(null, this.getX(), this.getY(), this.getZ(),
                    ModSounds.SARAH_VOICE.get(), SoundSource.HOSTILE, 1.0F, 1.0F);
            setTalking(true);
            talkingTicksRemaining = 26; // ~1.3s, matches sarah.ogg's talking animation length
            sarahVoiceCooldown = 200; // 10 seconds
        }

        // 3 minutes (3600 ticks) of ignore -> transition to WANDERING.
        if ((aliveTicks - lastNoticedTicks) >= 3600) {
            setMode(Mode.WANDERING);
        }
    }

    // --- WANDERING ------------------------------------------------------------

    private void tickWandering(Player nearest) {
        // 90-second wander window. If she finds the player (line-of-sight + < 16 blocks),
        // aggro. Otherwise despawn.
        int wandered = aliveTicks - wanderStartTicks;
        if (nearest != null) {
            double dist = this.distanceTo(nearest);
            if (dist < 16.0D && this.hasLineOfSight(nearest)) {
                setMode(Mode.AGGRESSIVE);
                return;
            }
        }
        if (wandered >= 1800) {
            // Didn't find anyone in 90 seconds - despawn.
            this.discard();
        }
    }

    // --- AGGRESSIVE -----------------------------------------------------------

    private void tickAggressive(Player nearest) {
        // Periodic chase-sarah shout while chasing.
        if (chaseSarahCooldown <= 0 && nearest != null) {
            this.level.playSound(null, this.getX(), this.getY(), this.getZ(),
                    ModSounds.CHASE_SARAH.get(), SoundSource.HOSTILE, 1.2F, 1.0F);
            chaseSarahCooldown = 200; // every 10s
        }

        // After 30 seconds of chasing, get annoyed and ENRAGE (mad sarah).
        if (chaseStartTicks >= 0 && aliveTicks - chaseStartTicks > 600) {
            setMode(Mode.ENRAGED);
        }
    }

    // --- ENRAGED --------------------------------------------------------------

    private void tickEnraged(Player nearest) {
        // Faster than aggressive. Still plays chase shout periodically.
        if (chaseSarahCooldown <= 0 && nearest != null) {
            this.level.playSound(null, this.getX(), this.getY(), this.getZ(),
                    ModSounds.CHASE_SARAH.get(), SoundSource.HOSTILE, 1.3F, 0.9F);
            chaseSarahCooldown = 240;
        }
    }

    // === Helpers ==============================================================

    private void lockYawTo(Player p) {
        double dx = p.getX() - this.getX();
        double dz = p.getZ() - this.getZ();
        float yaw = (float) (Math.atan2(dz, dx) * (180D / Math.PI)) - 90.0F;
        this.setYBodyRot(yaw);
        this.setYHeadRot(yaw);
        this.yHeadRotO = yaw;
        this.yBodyRotO = yaw;
        this.yRotO     = yaw;
        this.setYRot(yaw);
    }

    private boolean isPlayerStaringAt(Player p) {
        if (this.distanceTo(p) > 48.0D) return false;
        double dx = this.getX() - p.getX();
        double dy = this.getEyeY() - p.getEyeY();
        double dz = this.getZ() - p.getZ();
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 0.001D) return false;
        dx /= len; dy /= len; dz /= len;
        Vec3 look = p.getViewVector(1.0F);
        double dot = look.x * dx + look.y * dy + look.z * dz;
        return dot > 0.85D && p.hasLineOfSight(this);
    }

    private void stopChaseThemeForNearbyPlayers() {
        if (!(this.level instanceof ServerLevel)) return;
        ServerLevel sl = (ServerLevel) this.level;
        ResourceLocation sound = ModSounds.CHASE_THEME.get().getLocation();
        ClientboundStopSoundPacket pkt = new ClientboundStopSoundPacket(sound, SoundSource.HOSTILE);
        for (ServerPlayer sp : sl.players()) {
            if (sp.distanceToSqr(this) < 96.0D * 96.0D) sp.connection.send(pkt);
        }
    }

    // === Animations ===========================================================

    @Override
    public void registerControllers(AnimationData data) {
        data.addAnimationController(new AnimationController<>(this, "loco", 4, this::locoPredicate));
        data.addAnimationController(new AnimationController<>(this, "talk", 0, this::talkPredicate));
    }

    private <E extends IAnimatable> PlayState locoPredicate(AnimationEvent<E> event) {
        AnimationController<?> controller = event.getController();
        Mode m = getMode();
        if (m == Mode.AGGRESSIVE || m == Mode.ENRAGED) {
            // Run or crawl - decided per-spawn.
            String anim = getGait() == Gait.CRAWL
                    ? "animation.maternal_wraith.crawl"
                    : "animation.maternal_wraith.run";
            controller.setAnimation(new AnimationBuilder().loop(anim));
            return PlayState.CONTINUE;
        }
        if (m == Mode.WANDERING && event.isMoving()) {
            // Wandering uses run animation but at lower speed - reads as a slow gait visually
            // because GeckoLib doesn't tie animation rate to entity speed by default.
            controller.setAnimation(new AnimationBuilder().loop("animation.maternal_wraith.run"));
            return PlayState.CONTINUE;
        }
        controller.setAnimation(new AnimationBuilder().loop("animation.maternal_wraith.idle"));
        return PlayState.CONTINUE;
    }

    private <E extends IAnimatable> PlayState talkPredicate(AnimationEvent<E> event) {
        AnimationController<?> controller = event.getController();
        if (isTalking()) {
            controller.setAnimation(new AnimationBuilder()
                    .addAnimation("animation.maternal_wraith.talking", EDefaultLoopTypes.PLAY_ONCE));
            return PlayState.CONTINUE;
        }
        return PlayState.STOP;
    }

    @Override public AnimationFactory getFactory() { return factory; }

    // === Sounds ===============================================================

    @Override protected SoundEvent getHurtSound(DamageSource s) { return ModSounds.MAD_SARAH.get(); }
    @Override protected SoundEvent getDeathSound()              { return ModSounds.MAD_SARAH.get(); }
    @Override protected float getSoundVolume()                  { return 1.0F; }

    // === Damage / attack ======================================================

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean result = super.hurt(source, amount);
        if (result && source.getEntity() instanceof LivingEntity) {
            // Hitting her = aggro. Unless already aggressive/enraged.
            Mode m = getMode();
            if (m == Mode.STALKING || m == Mode.WANDERING) {
                setMode(Mode.AGGRESSIVE);
            }
        }
        return result;
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        this.swing(InteractionHand.MAIN_HAND);
        boolean ok = super.doHurtTarget(target);
        if (ok && target instanceof Player && !target.isAlive()) {
            // Killed the player - despawn per spec.
            stopChaseThemeForNearbyPlayers();
            this.discard();
        }
        return ok;
    }

    // === Removal hook =========================================================

    @Override
    public void remove(RemovalReason reason) {
        stopChaseThemeForNearbyPlayers();
        super.remove(reason);
    }

    // === NBT ==================================================================

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Mode",             this.entityData.get(DATA_MODE));
        tag.putInt("Gait",             this.entityData.get(DATA_GAIT));
        tag.putInt("AliveTicks",       aliveTicks);
        tag.putInt("LastNoticed",      lastNoticedTicks);
        tag.putInt("ChaseStart",       chaseStartTicks);
        tag.putInt("WanderStart",      wanderStartTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(DATA_MODE, tag.getInt("Mode"));
        this.entityData.set(DATA_GAIT, tag.getInt("Gait"));
        aliveTicks       = tag.getInt("AliveTicks");
        lastNoticedTicks = tag.getInt("LastNoticed");
        chaseStartTicks  = tag.getInt("ChaseStart");
        wanderStartTicks = tag.getInt("WanderStart");
    }

    /** Used by spawn-marker logic in ForgeEvents to know where this Wraith came from. */
    public void markSpawnPos(BlockPos p) { /* reserved for future use */ }
}
