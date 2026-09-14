package com.matt.forgehax.mods;

import com.matt.forgehax.mods.managers.PositionRotationManager;
import com.matt.forgehax.mods.managers.PositionRotationManager.RotationState;
import com.matt.forgehax.util.Utils;
import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.common.PriorityEnum;
import com.matt.forgehax.util.entity.EntityUtils;
import com.matt.forgehax.util.key.Bindings;
import com.matt.forgehax.util.math.Angle;
import com.matt.forgehax.util.math.AngleHelper;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.mod.ToggleMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;
import com.matt.forgehax.util.projectile.Projectile;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.Objects;

import static com.matt.forgehax.Helper.getLocalPlayer;
import static com.matt.forgehax.Helper.getWorld;

/**
 * Aim assist only. It never attacks on its own, it corrects where the swing or the shot the player
 * is already making goes. KillAura is the mod that attacks by itself.
 */
@RegisterMod
public class Aimbot extends ToggleMod implements PositionRotationManager.MovementUpdateListener {

  /**
   * Weight of the newest tick in the smoothed target velocity, ~1.5 tick time constant
   */
  private static final double VELOCITY_SMOOTHING = 0.45D;
  /**
   * Aim point and flight time depend on each other, this is how many times we bounce between them
   */
  private static final int PREDICTION_ITERATIONS = 4;
  /**
   * Flight time is settled once it stops moving by more than a quarter tick
   */
  private static final double PREDICTION_TOLERANCE = 0.25D;
  /**
   * ponytail: nothing here tracks where the ground is, so an airborne target is only followed for
   * about the length of a jump before its height is held flat. Track the last onGround y if
   * leading targets falling down long drops ever matters.
   */
  private static final int PREDICTION_FALL_TICKS = 20;

  private static Entity target = null;
  private final Setting<Boolean> silent =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("silent")
          .description("Only send the rotation to the server, don't move the camera")
          .defaultTo(true)
          .build();
  private final Setting<Boolean> holdTarget =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("hold-target")
          .description("Keep the first target until it becomes invalid")
          .defaultTo(false)
          .build();
  private final Setting<Boolean> visCheck =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("trace")
          .description("Only aim at targets that are visible")
          .defaultTo(false)
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
  private final Setting<Double> range =
      getCommandStub()
          .builders()
          .<Double>newSettingBuilder()
          .name("range")
          .description("Melee aim range")
          .defaultTo(4.5D)
          .build();
  private final Setting<Boolean> projectileAimbot =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("proj-aimbot")
          .description("Aim projectile weapons while they are being used")
          .defaultTo(true)
          .build();
  private final Setting<Boolean> projectileTraceCheck =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("projectile-trace")
          .description("Check the trace of each target if holding a weapon that fires a projectile")
          .defaultTo(true)
          .build();
  private final Setting<Double> projectileRange =
      getCommandStub()
          .builders()
          .<Double>newSettingBuilder()
          .name("projectile-range")
          .description("Projectile aim range")
          .defaultTo(100D)
          .build();
  private final Setting<Boolean> projectilePredict =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("proj-predict")
          .description("Lead the shot to where the target will be when the projectile arrives")
          .defaultTo(true)
          .build();
  private final Setting<Double> projectileLead =
      getCommandStub()
          .builders()
          .<Double>newSettingBuilder()
          .name("proj-lead")
          .description("How much of the predicted lead to apply, lower it against jittery targets")
          .defaultTo(1.D)
          .min(0.D)
          .max(1.D)
          .build();
  private final Setting<Selector> selector =
      getCommandStub()
          .builders()
          .<Selector>newSettingEnumBuilder()
          .name("selector")
          .description("The method used to select a target from a group")
          .defaultTo(Selector.CROSSHAIR)
          .build();
  private Entity velocityTarget = null;
  private Vec3 smoothedVelocity = Vec3.ZERO;

  public Aimbot() {
    super(Category.COMBAT, "Aimbot", false, "Aim at what you are attacking or shooting at");
  }

  @Nullable
  public static Entity getTarget() {
    return target;
  }

  /**
   * Below KillAura on purpose. Both aim at the same shared target, but only the first listener to
   * move the view gets its follow up tasks run, and KillAura's is the swing.
   */
  @Override
  protected void onEnabled() {
    PositionRotationManager.getManager().register(this, PriorityEnum.HIGH);
  }

  @Override
  public void onDisabled() {
    PositionRotationManager.getManager().unregister(this);
    forgetTarget();
  }

  @Override
  public void onLocalPlayerMovementUpdate(RotationState.Local state) {
    final LocalPlayer player = getLocalPlayer();
    final Level world = getWorld();

    if (player == null || world == null || player.isSpectator() || !player.isAlive()) {
      forgetTarget();
      return;
    }

    final Projectile projectile = getHeldProjectile();
    final boolean useProjectile = projectileAimbot.get() && !projectile.isNull();

    if (!isAttacking(player, useProjectile)) {
      forgetTarget();
      return;
    }

    final Vec3 eyes = EntityUtils.getEyePos(player);
    final Vec3 look = player.getLookAngle().normalize();
    final Angle angles = AngleHelper.getAngleFacingInDegrees(look);

    Entity found = target;
    if (!holdTarget.get() || found == null || !filterTarget(eyes, look, angles, found)) {
      found = findTarget(world, eyes, look, angles);
    }
    target = found;

    if (found == null) {
      return;
    }

    if (useProjectile) {
      aimProjectile(state, player, projectile, found);
    } else {
      state.setViewAngles(Utils.getLookAtAngles(found).normalize(), silent.get());
    }
  }

  /**
   * The whole point of the mod: it aims, it never swings. Melee follows the attack key, projectiles
   * follow the item actually being drawn or thrown.
   */
  private boolean isAttacking(LocalPlayer player, boolean useProjectile) {
    return useProjectile
        ? player.isUsingItem() || Bindings.use.isPressed()
        : Bindings.attack.isPressed();
  }

  private void forgetTarget() {
    target = null;
    velocityTarget = null;
    smoothedVelocity = Vec3.ZERO;
  }

  private Projectile getHeldProjectile() {
    return Projectile.getProjectileByItemStack(getLocalPlayer().getMainHandItem());
  }

  /**
   * The force the shot leaves with, which is the full draw and not the draw so far. A bow reaches
   * six blocks five ticks into its draw, so solving against the instantaneous charge left every
   * target unreachable - and therefore unacquirable, since acquisition needs a launch solution -
   * for the first half of every draw. Throwables ignore the charge entirely and their max is their
   * only force.
   */
  private double getForce(Projectile projectile) {
    return projectile.getMaxForce();
  }

  private boolean isVisible(Entity entity, boolean useProjectile) {
    if (useProjectile && Boolean.TRUE.equals(projectileTraceCheck.get())) {
      // no lead here, this runs over every candidate entity and only decides whether the target is
      // worth acquiring at all
      final LocalPlayer player = getLocalPlayer();
      Projectile projectile = getHeldProjectile();
      Vec3 shootPos = Projectile.getEntityShootPos(player);
      Vec3 inherited = getInheritedMotion();
      double force = getForce(projectile);

      Projectile.LaunchSolution solution =
          projectile.solveLaunch(shootPos, getAttackPosition(entity), force, inherited);
      return solution != null
          && projectile.isPathClear(shootPos, solution, force, inherited, entity);
    } else {
      return !visCheck.get() || getLocalPlayer().hasLineOfSight(entity);
    }
  }

  /**
   * Velocity the projectile picks up from the player, see EntityArrow#shoot(Entity, ...). It is
   * added on top of the aimed velocity, so the aim has to be bent to cancel it out.
   */
  private Vec3 getInheritedMotion() {
    LocalPlayer player = getLocalPlayer();
    Vec3 motion = player.getDeltaMovement();
    return new Vec3(motion.x, player.onGround() ? 0.D : motion.y, motion.z);
  }

  private Vec3 getAttackPosition(Entity entity) {
    return EntityUtils.getInterpolatedPos(entity, 1).add(0, entity.getEyeHeight() / 2, 0);
  }

  /**
   * Which entities may be targeted is shared with KillAura through the Targets module, only the
   * geometry is the aimbot's own.
   */
  private boolean filterTarget(Vec3 eyes, Vec3 viewNormal, Angle angles, Entity entity) {
    final boolean useProjectile = projectileAimbot.get() && !getHeldProjectile().isNull();
    return Targets.isValidTarget(entity)
        && isInRange(eyes, entity, useProjectile)
        && isInFov(angles, getAttackPosition(entity).subtract(eyes))
        && isVisible(entity, useProjectile);
  }

  private boolean isInRange(Vec3 eyes, Entity entity, boolean useProjectile) {
    double dist = useProjectile ? projectileRange.get() : range.get();
    if (dist <= 0.D) {
      return true;
    }
    return EntityUtils.getDistanceSq(eyes, entity.getBoundingBox()) <= dist * dist;
  }

  private boolean isInFov(Angle angle, Vec3 pos) {
    double max = this.fov.get();
    if (max >= 180) {
      return true;
    }
    Angle look = AngleHelper.getAngleFacingInDegrees(pos);
    Angle diff = angle.sub(look.getPitch(), look.getYaw()).normalize();
    return Math.abs(diff.getPitch()) <= max && Math.abs(diff.getYaw()) <= max;
  }

  private double selecting(final Vec3 eyes, final Vec3 viewNormal, final Entity entity) {
    Aimbot.Selector aimSelector = selector.get();

    if (aimSelector == Selector.DISTANCE) {
      return getAttackPosition(entity).subtract(eyes).lengthSqr();
    }

    return getAttackPosition(entity)
        .subtract(eyes)
        .normalize()
        .subtract(viewNormal)
        .lengthSqr();
  }

  @Nullable
  private Entity findTarget(final Level world, final Vec3 eyes, final Vec3 viewNormal, final Angle angles) {
    double rangeMax = Math.max(this.range.get(), this.projectileRange.get());
    final net.minecraft.world.phys.AABB search;
    if (rangeMax > 0.D) {
      assert getLocalPlayer() != null;
      search = getLocalPlayer().getBoundingBox().inflate(rangeMax + 2.D);
    } else {search = new net.minecraft.world.phys.AABB(-3.0E7D, -3.0E7D, -3.0E7D, 3.0E7D, 3.0E7D, 3.0E7D);}
    return world
        .getEntities((Entity) null, search, entity -> true)
        .stream()
        .filter(entity -> filterTarget(eyes, viewNormal, angles, entity))
        .min(Comparator.comparingDouble(entity -> selecting(eyes, viewNormal, entity)))
        .orElse(null);
  }

  private void aimProjectile(
      RotationState.Local state, LocalPlayer player, Projectile projectile, Entity tar) {
    final Vec3 shootPos = Projectile.getEntityShootPos(player);
    final Vec3 inherited = getInheritedMotion();
    final double force = getForce(projectile);

    Projectile.LaunchSolution solution = solveWithLead(projectile, shootPos, inherited, force, tar);

    if (solution != null) {
      state.setViewAngles(solution.getAngle(), silent.get());
    }
  }

  /**
   * Aim point and flight time each depend on the other, so start from where the target is standing
   * and let the two settle. It converges in two or three passes for anything a bow can reach.
   */
  @Nullable
  private Projectile.LaunchSolution solveWithLead(
      Projectile projectile, Vec3 shootPos, Vec3 inherited, double force, Entity tar) {
    final Vec3 instant = EntityUtils.getInterpolatedAmount(tar, 1.D);
    final Vec3 velocity = updateTargetVelocity(tar, instant);
    final Vec3 lead =
        Boolean.TRUE.equals(projectilePredict.get())
            ? velocity.scale(projectileLead.get() * getLeadConfidence(instant, velocity))
            : Vec3.ZERO;

    double flightTicks = 0.D;
    Projectile.LaunchSolution solution = null;

    for (int i = 0; i < PREDICTION_ITERATIONS; i++) {
      solution =
          projectile.solveLaunch(shootPos, predictPosition(tar, lead, flightTicks), force, inherited);

      if (solution == null) {
        return null;
      }
      if (Math.abs(solution.getFlightTicks() - flightTicks) < PREDICTION_TOLERANCE) {
        break;
      }
      flightTicks = solution.getFlightTicks();
    }
    return solution;
  }

  /**
   * Velocity taken from the position delta rather than motionX/Y/Z, which the client only
   * interpolates for other players and is routinely stale or flat zero.
   */
  private Vec3 updateTargetVelocity(Entity tar, Vec3 instant) {
    if (!Objects.equals(tar, velocityTarget)) {
      velocityTarget = tar;
      smoothedVelocity = instant;
      return instant;
    }
    smoothedVelocity =
        smoothedVelocity.scale(1.D - VELOCITY_SMOOTHING).add(instant.scale(VELOCITY_SMOOTHING));
    return smoothedVelocity;
  }

  /**
   * A target that just changed direction has an instant velocity far from its recent average, and
   * extrapolating a stale heading is what makes a lead miss. Shrink the lead by how far the two
   * have diverged instead of committing to it.
   *
   * <p>ponytail: this is a cheap stand in for the adaptive filtering the tracking literature uses
   * (IMM over constant velocity/constant turn models). Swap it out if leading strafing players is
   * still not good enough.
   */
  private double getLeadConfidence(Vec3 instant, Vec3 smoothed) {
    double speed = smoothed.length();
    if (speed < 1.0E-4D) {
      return 0.D;
    }
    return Utils.clamp(1.D - instant.subtract(smoothed).length() / speed, 0.D, 1.D);
  }

  /**
   * Where the target will be in the given number of ticks. Horizontal movement carries on as is,
   * which is what a player holding a movement key actually does, while an airborne target follows
   * the minecraft fall curve.
   */
  private Vec3 predictPosition(Entity tar, Vec3 lead, double ticks) {
    Vec3 base = getAttackPosition(tar);

    if (ticks <= 0.D || lead.lengthSqr() <= 0.D) {
      return base;
    }

    double y = base.y;
    if (!tar.onGround()) {
      double motionY = lead.y;
      for (int i = 0; i < Math.min(ticks, PREDICTION_FALL_TICKS); i++) {
        y += motionY;
        motionY = (motionY - 0.08D) * 0.98D;
      }
    }
    return new Vec3(base.x + lead.x * ticks, y, base.z + lead.z * ticks);
  }

  enum Selector {
    CROSSHAIR,
    DISTANCE,
  }
}
