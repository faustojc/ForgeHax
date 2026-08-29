package com.matt.forgehax.mods;

import com.google.common.collect.ImmutableList;
import com.matt.forgehax.mods.managers.PositionRotationManager;
import com.matt.forgehax.mods.managers.PositionRotationManager.RotationState;
import com.matt.forgehax.mods.services.TickRateService;
import com.matt.forgehax.util.SimpleTimer;
import com.matt.forgehax.util.Utils;
import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.common.PriorityEnum;
import com.matt.forgehax.util.entity.EntityUtils;
import com.matt.forgehax.util.math.Angle;
import com.matt.forgehax.util.math.AngleHelper;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.mod.ToggleMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.*;

import static com.matt.forgehax.Helper.*;

/**
 * Created on 6/27/2017 by fr1kin
 */
@RegisterMod
public class KillAura extends ToggleMod implements PositionRotationManager.MovementUpdateListener {
  private static Entity target = null;
  private final SimpleTimer attackTimer = new SimpleTimer();
  private final Setting<TargetModes> mode =
      getCommandStub()
          .builders()
          .<TargetModes>newSettingEnumBuilder()
          .name("mode")
          .description("How a target is picked from all valid entities")
          .defaultTo(TargetModes.CLOSEST)
          .build();
  private final Setting<Double> range =
      getCommandStub()
          .builders()
          .<Double>newSettingBuilder()
          .name("range")
          .description("Attack range")
          .defaultTo(4.5D)
          .min(0.D)
          .max(6.D)
          .build();
  private final Setting<Integer> fov =
      getCommandStub()
          .builders()
          .<Integer>newSettingBuilder()
          .name("fov")
          .description("Field of view a target must be within")
          .defaultTo(180)
          .min(0)
          .max(180)
          .build();
  private final Setting<Float> cooldownPercent =
      getCommandStub()
          .builders()
          .<Float>newSettingBuilder()
          .name("cooldown-percent")
          .description("Minimum attack cooldown percent for the next strike")
          .defaultTo(100F)
          .min(0F)
          .build();
  private final Setting<Long> attackDelay =
      getCommandStub()
          .builders()
          .<Long>newSettingBuilder()
          .name("attack-delay")
          .description("Extra delay between attacks in milliseconds")
          .defaultTo(0L)
          .min(0L)
          .build();
  private final Setting<Boolean> lagCompensation =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("lag-compensation")
          .description("Compensate the attack cooldown for server lag")
          .defaultTo(true)
          .build();
  private final Setting<Boolean> rotate =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("rotate")
          .description("Aim at the target before attacking")
          .defaultTo(true)
          .build();
  private final Setting<Boolean> silent =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("silent")
          .description("Only send the rotation to the server, don't move the camera")
          .defaultTo(true)
          .build();
  private final Setting<Boolean> swing =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("swing")
          .description("Swing the arm when attacking")
          .defaultTo(true)
          .build();
  private final Setting<Boolean> visCheck =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("trace")
          .description("Only attack targets that are visible")
          .defaultTo(false)
          .build();
  private final Setting<Boolean> holdTarget =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("hold-target")
          .description("Keep the first target until it becomes invalid")
          .defaultTo(false)
          .build();
  private final Setting<Boolean> multi =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("multi")
          .description("Attack every valid target at once instead of only the selected one")
          .defaultTo(false)
          .build();
  /** Reused between movement updates; attack tasks receive an immutable snapshot when needed. */
  private final List<Entity> validTargets = new ArrayList<>();

  public KillAura() {
    super(Category.COMBAT, "KillAura", false, "Attack anything within given parameters");
  }

  @Nullable
  public static Entity getTarget() {
    return target;
  }

  @Override
  protected void onEnabled() {
    PositionRotationManager.getManager().register(this, PriorityEnum.HIGHEST);
  }

  @Override
  protected void onDisabled() {
    PositionRotationManager.getManager().unregister(this);
    target = null;
  }

  @Override
  public void onLocalPlayerMovementUpdate(RotationState.Local state) {
    final EntityPlayerSP player = getLocalPlayer();
    final World world = getWorld();

    if (player == null || world == null || player.isSpectator() || !player.isEntityAlive()) {
      target = null;
      return;
    }

    final Vec3d eyes = EntityUtils.getEyePos(player);
    final Vec3d look = player.getLookVec().normalize();
    final Angle angles = AngleHelper.getAngleFacingInDegrees(look);

    final Entity previousTarget = target;
    boolean previousTargetValid = false;
    validTargets.clear();
    for (Entity entity : world.loadedEntityList) {
      if (isValidTarget(eyes, angles, entity)) {
        validTargets.add(entity);
        if (entity == previousTarget) {
          previousTargetValid = true;
        }
      }
    }

    Entity current = previousTarget;
    if (!holdTarget.get() || current == null || !previousTargetValid) {
      current = findTarget(validTargets, eyes, angles);
    }
    target = current;

    if (current == null) {
      return;
    }

    final Entity found = current;

    if (rotate.get()) {
      state.setViewAngles(Utils.getLookAtAngles(found).normalize(), silent.get());
    }

    if (!canAttack(player)) {
      return;
    }

    final List<Entity> victims =
        multi.get()
            ? ImmutableList.copyOf(validTargets)
            : ImmutableList.of(found);

    state.invokeLater(
        rs -> {
          if (player != getLocalPlayer() || world != getWorld()) {
            return;
          }

          boolean attacked = false;
          for (Entity victim : victims) {
            if (world.getEntityByID(victim.getEntityId()) == victim && Targets.isAttackable(victim)) {
              getPlayerController().attackEntity(player, victim);
              attacked = true;
            }
          }

          if (attacked) {
            attackTimer.start();
          }
          if (attacked && swing.get()) {
            player.swingArm(EnumHand.MAIN_HAND);
          }
        });
  }

  private Entity findTarget(
      final List<Entity> candidates, final Vec3d eyes, final Angle angles) {
    Entity best = null;
    double bestScore = Double.MAX_VALUE;
    for (Entity entity : candidates) {
      final double score = sorting(eyes, angles, entity);
      if (best == null || score < bestScore) {
        best = entity;
        bestScore = score;
      }
    }
    return best;
  }

  private double sorting(final Vec3d eyes, final Angle angles, final Entity entity) {
    switch (mode.get()) {
      case HEALTH:
        return EntityUtils.isLiving(entity)
            ? ((EntityLivingBase) entity).getHealth()
            : Float.MAX_VALUE;
      case CROSSHAIR:
        Angle diff = angles.sub(Utils.getLookAtAngles(entity).normalize()).normalize();
        return Math.abs(diff.getPitch()) + Math.abs(diff.getYaw());
      case CLOSEST:
      default:
        return getAttackPosition(entity).subtract(eyes).lengthSquared();
    }
  }

  /**
   * Cheap checks first, the shared filters and the ray trace are the expensive ones and only run on
   * entities that are already in range
   */
  private boolean isValidTarget(final Vec3d eyes, final Angle angles, final Entity entity) {
    return Targets.isValidTarget(entity)
        && isInRange(eyes, entity)
        && isInFov(angles, entity)
        && isVisible(entity);
  }

  /**
   * Measured against the hitbox, not the entity center. Large modded mobs (parasites, bosses) have
   * a center several blocks away from the part of them that is standing on top of you, so a
   * center-based check drops them from the target list entirely.
   */
  private boolean isInRange(Vec3d eyes, Entity entity) {
    double dist = range.get();
    if (dist <= 0.D) {
      return true;
    }
    return EntityUtils.getDistanceSq(eyes, entity.getEntityBoundingBox()) <= dist * dist;
  }

  private boolean isInFov(Angle angles, Entity entity) {
    double max = fov.get();
    if (max >= 180) {
      return true;
    }
    Angle diff = angles.sub(Utils.getLookAtAngles(entity).normalize()).normalize();
    return Math.abs(diff.getPitch()) <= max && Math.abs(diff.getYaw()) <= max;
  }

  private boolean isVisible(Entity entity) {
    return !visCheck.get() || getLocalPlayer().canEntityBeSeen(entity);
  }

  private Vec3d getAttackPosition(Entity entity) {
    return EntityUtils.getInterpolatedPos(entity, 1)
                      .addVector(0, entity.getEyeHeight() / 2, 0);
  }

  private double getLagComp() {
    TickRateService.TickRateData tickData = TickRateService.getTickData();
    return lagCompensation.get() && tickData.getSampleSize() > 0
        ? -(20.D - tickData.getPoint().getAverage())
        : 0.D;
  }

  private boolean canAttack(EntityPlayerSP player) {
    if (!attackTimer.hasTimeElapsed(attackDelay.get())) {
      return false;
    }
    final float cdRatio = cooldownPercent.get() / 100F;
    final float cdOffset = cdRatio <= 1F ? 0F : -(player.getCooldownPeriod() * (cdRatio - 1F));
    return player.getCooledAttackStrength((float) getLagComp() + cdOffset) >= Math.min(1F, cdRatio);
  }

  enum TargetModes {
    CLOSEST,
    CROSSHAIR,
    HEALTH,
  }
}
