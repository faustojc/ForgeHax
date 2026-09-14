package com.matt.forgehax.util.projectile;

import com.google.common.collect.Lists;
import com.matt.forgehax.util.entity.EntityUtils;
import com.matt.forgehax.util.math.Angle;
import com.matt.forgehax.util.math.AngleHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;

import static com.matt.forgehax.Helper.getLocalPlayer;
import static com.matt.forgehax.Helper.getWorld;

/**
 * Created on 6/21/2017 by fr1kin
 */
public enum Projectile implements IProjectile {
  NULL() {
    @Override
    public Item getItem() {
      return null;
    }

    @Override
    public double getForce(int charge) {
      return 0;
    }

    @Override
    public double getMaxForce() {
      return 0;
    }

    @Override
    public double getMinForce() {
      return 0;
    }

    @Override
    public double getGravity() {
      return 0;
    }

    @Override
    public double getDrag() {
      return 0;
    }

    @Override
    public double getWaterDrag() {
      return 0;
    }

    @Override
    public double getProjectileSize() {
      return 0;
    }
  },
  BOW() {
    @Override
    public Item getItem() {
      return Items.BOW;
    }

    @Override
    public double getForce(int charge) {
      // ItemBow#getArrowVelocity, the arrow is then shot at that velocity * 3
      double force = (double) charge / 20.0D;
      force = (force * force + force * 2.0D) / 3.0D;
      if (force > 1.0D) {
        force = 1.0D;
      }
      return force * 3.0D;
    }

    @Override
    public double getMaxForce() {
      return 3.D;
    }

    @Override
    public double getMinForce() {
      return 0.15D;
    }

    @Override
    public double getGravity() {
      return 0.05D;
    }

    @Override
    public double getWaterDrag() {
      return 0.6D;
    }

    @Override
    public double getProjectileSize() {
      return 0.5D;
    }
  },
  SNOWBALL() {
    @Override
    public Item getItem() {
      return Items.SNOWBALL;
    }
  },
  EGG() {
    @Override
    public Item getItem() {
      return Items.EGG;
    }
  },
  FISHING_ROD() {
    @Override
    public Item getItem() {
      return Items.FISHING_ROD;
    }

    @Override
    public double getGravity() {
      return 0.03999999910593033D;
    }

    @Override
    public double getDrag() {
      return 0.92D;
    }
  },
  ENDER_PEARL() {
    @Override
    public Item getItem() {
      return Items.ENDER_PEARL;
    }
  };

  private static final int MAX_ITERATIONS =
      1000; // fail safe to prevent infinite loops. MUST be greater than 1
  private static final double SHOOT_POS_OFFSET = 0.10000000149011612D;

  /**
   * Nothing a bow can reach stays in the air anywhere near this long, it only exists so the solver
   * gives up instead of scanning forever on an unreachable target
   */
  private static final double SOLVER_MAX_FLIGHT_TICKS = 200.D;

  /**
   * The scan brackets the answer to within a tick, this refines it to ~1e-5 ticks
   */
  private static final int SOLVER_BISECTIONS = 24;

  private static HitResult rayTraceCheckEntityCollisions(
      Vec3 start, Vec3 end, AABB bb, double motionX, double motionY, double motionZ) {
    HitResult trace =
        getWorld().clip(
            new ClipContext(
                start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, getLocalPlayer()));

    if (trace.getType() != HitResult.Type.MISS) {
      end = trace.getLocation();
    }

    EntityHitResult entityTrace =
        ProjectileUtil.getEntityHitResult(
            getWorld(),
            getLocalPlayer(),
            start,
            end,
            bb.expandTowards(motionX, motionY, motionZ).inflate(1.D),
            Entity::canBeCollidedWith);
    return entityTrace == null ? (trace.getType() == HitResult.Type.MISS ? null : trace) : entityTrace;
  }

  /**
   * Where the projectile entity actually spawns, see EntityArrow(World, EntityLivingBase)
   */
  public static Vec3 getEntityShootPos(Entity entity) {
    return EntityUtils.getEyePos(entity).subtract(0.D, SHOOT_POS_OFFSET, 0.D);
  }

  private static Vec3 getShootPosFacing(Entity entity, Angle angleFacing) {
    return getEntityShootPos(entity)
        .subtract(
            Math.cos(angleFacing.inRadians().getYaw() - AngleHelper.HALF_PI) * 0.16D,
            0.D,
            Math.sin(angleFacing.inRadians().getYaw() - AngleHelper.HALF_PI) * 0.16D
        );
  }

  private static Angle getAngleFacing(Angle angle) {
    return Angle.radians(
        -angle.inRadians().getPitch(), (float) (angle.inRadians().getYaw() + (Math.PI / 2.D)));
  }

  public static Projectile getProjectileByItem(Item item) {
    if (item != null) {
      for (Projectile p : values()) {
        if (p.getItem() != null && p.getItem().equals(item)) {
          return p;
        }
      }
    }
    return NULL;
  }

  public static Projectile getProjectileByItemStack(ItemStack item) {
    return item == null ? NULL : getProjectileByItem(item.getItem());
  }

  // ####################################################################################################

  public boolean isNull() {
    // NULL is the only constant without an item, and going through getItem() would drag the
    // item registry into the pure-math launch solver
    return this == NULL;
  }

  // ####################################################################################################

  @Nullable
  public SimulationResult getSimulatedTrajectory(
      Vec3 shootPos, Angle angle, double force, int factor) throws IllegalArgumentException {
    return getSimulatedTrajectory(shootPos, angle, force, factor, Vec3.ZERO, MAX_ITERATIONS);
  }

  /**
   * @param inherited velocity the projectile picks up from whoever shot it, see
   *                  EntityArrow#shoot(Entity, float, float, float, float, float)
   * @param maxSteps  how many ticks to simulate before giving up, every step is a ray trace so
   *                  callers that only care about the first part of the flight should say so
   */
  @Nullable
  public SimulationResult getSimulatedTrajectory(
      Vec3 shootPos, Angle angle, double force, int factor, Vec3 inherited, int maxSteps)
      throws IllegalArgumentException {
    if (isNull()) {
      return null;
    }

    Entity hitEntity = null;

    double[] forward = angle.getForwardVector();
    Vec3 v =
        new Vec3(forward[0], forward[1], forward[2]).normalize().scale(force).add(inherited);

    double velocityX = v.x;
    double velocityY = v.y;
    double velocityZ = v.z;

    double distanceTraveledSq = 0.D;

    HitResult trace;

    List<Vec3> points = Lists.newArrayList();
    points.add(shootPos); // add the initial position

    Vec3 next = new Vec3(shootPos.x, shootPos.y, shootPos.z);
    Vec3 previous = next;

    for (int index = points.size(), n = 0; index < Math.min(maxSteps, MAX_ITERATIONS); index++) {
      next = next.add(velocityX, velocityY, velocityZ);

      AABB bb = getBoundBox(next);
      trace = rayTraceCheckEntityCollisions(previous, next, bb, velocityX, velocityY, velocityZ);

      if (trace != null) {
        if (trace instanceof EntityHitResult) {
          hitEntity = ((EntityHitResult) trace).getEntity();
        }
        distanceTraveledSq += previous.distanceToSqr(trace.getLocation());
        // add final vector even if index % factor != 0
        points.add(trace.getLocation());
        break;
      }
      // only add every nth entry
      if (n == factor) {
        points.add(next);
        n = 0;
      } else {
        n++;
      }

      distanceTraveledSq += previous.distanceToSqr(next);

      // in the void, stop
      if (next.y <= 0) {
        break;
      }

      double d = isWater(bb) ? getWaterDrag() : getDrag();

      velocityX = (velocityX * d);
      velocityY = (velocityY * d) - getGravity();
      velocityZ = (velocityZ * d);

      previous = next;
    }
    return new SimulationResult(points, distanceTraveledSq, hitEntity);
  }

  @Nullable
  public SimulationResult getSimulatedTrajectoryFromEntity(
      Entity shooter, Angle angle, double force, int factor) {
    angle = getAngleFacing(angle);
    return getSimulatedTrajectory(getShootPosFacing(shooter, angle), angle, force, factor);
  }

  /**
   * Find the angle that lands the projectile on targetPos, and how long it spends in the air
   * getting there.
   *
   * <p>Minecraft integrates a projectile as {@code p += v} then {@code v = v * drag - gravity}, so
   * after t ticks the launch velocity needed to cover a horizontal distance x and a height change
   * y is closed form, with {@code S(t) = (1 - drag^t) / (1 - drag)} and the terminal vertical
   * velocity {@code vt = -gravity / (1 - drag)}:
   *
   * <pre>
   *   horizontal = x / S(t)
   *   vertical   = vt + (y - t * vt) / S(t)
   * </pre>
   *
   * <p>Horizontal drag is isotropic so the shot always travels along the bearing to the target,
   * which leaves flight time as the only unknown. The required launch speed is large for a very
   * short flight, dips, then grows again as the shot has to be lobbed, so scanning a tick at a
   * time until it drops to the speed we can actually produce and bisecting that bracket finds the
   * flat trajectory (the first root, the one that arrives soonest and is easiest to lead).
   *
   * @param inherited velocity the projectile picks up from the shooter, which is added on top of
   *                  the aimed velocity and therefore has to be cancelled out of the aim
   */
  @Nullable
  public LaunchSolution solveLaunch(
      Vec3 shootPos, Vec3 targetPos, double force, Vec3 inherited) {
    final double drag = getDrag();
    if (isNull() || force <= 0.D || drag <= 0.D || drag >= 1.D) {
      return null;
    }

    final double dx = targetPos.x - shootPos.x;
    final double dz = targetPos.z - shootPos.z;
    final double dy = targetPos.y - shootPos.y;
    final double horizontal = Math.sqrt(dx * dx + dz * dz);
    // bearing, undefined when the target is straight overhead so any direction will do
    final double bearingX = horizontal > 0.D ? dx / horizontal : 0.D;
    final double bearingZ = horizontal > 0.D ? dz / horizontal : 0.D;

    double lowTicks = 1.D / 16.D;
    if (residual(horizontal, dy, lowTicks, bearingX, bearingZ, force, inherited) <= 0.D) {
      return null; // the target is close enough to be inside the first step, nothing to solve
    }

    double highTicks = -1.D;
    for (double t = 1.D; t <= SOLVER_MAX_FLIGHT_TICKS; t += 1.D) {
      if (residual(horizontal, dy, t, bearingX, bearingZ, force, inherited) <= 0.D) {
        highTicks = t;
        break;
      }
      lowTicks = t;
    }

    if (highTicks < 0.D) {
      return null; // out of reach at this force
    }

    for (int i = 0; i < SOLVER_BISECTIONS; i++) {
      double mid = (lowTicks + highTicks) / 2.D;
      if (residual(horizontal, dy, mid, bearingX, bearingZ, force, inherited) > 0.D) {
        lowTicks = mid;
      } else {
        highTicks = mid;
      }
    }

    Vec3 aim = getLaunchVelocity(horizontal, dy, highTicks, bearingX, bearingZ).subtract(inherited);
    return new LaunchSolution(AngleHelper.getAngleFacingInDegrees(aim).normalize(), highTicks);
  }

  /**
   * Launch velocity that puts the projectile on the target after the given number of ticks
   */
  private Vec3 getLaunchVelocity(
      double horizontal, double dy, double ticks, double bearingX, double bearingZ) {
    final double drag = getDrag();
    final double sum = (1.D - Math.pow(drag, ticks)) / (1.D - drag);
    final double terminal = -getGravity() / (1.D - drag);
    final double speed = horizontal / sum;
    return new Vec3(
        bearingX * speed, terminal + (dy - ticks * terminal) / sum, bearingZ * speed);
  }

  /**
   * How much faster than the projectile the shot would have to be launched to arrive in the given
   * number of ticks. Positive means too slow, negative means the shot is over powered for that
   * flight time.
   */
  private double residual(
      double horizontal,
      double dy,
      double ticks,
      double bearingX,
      double bearingZ,
      double force,
      Vec3 inherited) {
    return getLaunchVelocity(horizontal, dy, ticks, bearingX, bearingZ)
               .subtract(inherited)
               .length()
        - force;
  }

  /**
   * Whether the shot survives long enough to reach the target instead of burying itself in
   * something on the way. The simulation stops on the first thing it touches, so a path that lasts
   * at least as long as the solved flight time was never interrupted.
   */
  public boolean isPathClear(
      Vec3 shootPos,
      LaunchSolution solution,
      double force,
      Vec3 inherited,
      @Nullable Entity target) {
    final int steps = (int) Math.ceil(solution.getFlightTicks());
    SimulationResult result =
        getSimulatedTrajectory(
            shootPos, getAngleFacing(solution.getAngle()), force, 0, inherited, steps + 1);

    if (result == null) {
      return false;
    }
    if (target != null && Objects.equals(target, result.getHitEntity())) {
      return true;
    }
    return result.getPathTraveled().size() - 1 >= steps;
  }

  /**
   * Whether a shot at the entity where it stands right now would land
   */
  public boolean canHitEntity(Vec3 shooterPos, Entity targetEntity) {
    if (isNull()) {
      return false;
    }

    Vec3 targetPos = EntityUtils.getOBBCenter(targetEntity);
    double force = getMaxForce();

    LaunchSolution solution = solveLaunch(shooterPos, targetPos, force, Vec3.ZERO);
    return solution != null
        && isPathClear(shooterPos, solution, force, Vec3.ZERO, targetEntity);
  }

  /**
   * Where to aim and how long the projectile stays in the air on the way there
   */
  public static class LaunchSolution {

    private final Angle angle;
    private final double flightTicks;

    private LaunchSolution(Angle angle, double flightTicks) {
      this.angle = angle;
      this.flightTicks = flightTicks;
    }

    /**
     * Minecraft convention, in degrees
     */
    public Angle getAngle() {
      return angle;
    }

    public double getFlightTicks() {
      return flightTicks;
    }
  }

  private AABB getBoundBox(Vec3 pos) {
    double mp = getProjectileSize() / 2.D;
    return new AABB(
        pos.x - mp, pos.y - mp, pos.z - mp, pos.x + mp, pos.y + mp, pos.z + mp);
  }

  private static boolean isWater(AABB box) {
    for (int x = Mth.floor(box.minX); x <= Mth.floor(box.maxX); x++) {
      for (int y = Mth.floor(box.minY); y <= Mth.floor(box.maxY); y++) {
        for (int z = Mth.floor(box.minZ); z <= Mth.floor(box.maxZ); z++) {
          if (getWorld().getFluidState(new BlockPos(x, y, z)).is(FluidTags.WATER)) {
            return true;
          }
        }
      }
    }
    return false;
  }
}
