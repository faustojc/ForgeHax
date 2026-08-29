package com.matt.forgehax.util.projectile;

import com.matt.forgehax.util.math.Angle;
import net.minecraft.init.Bootstrap;
import net.minecraft.util.math.Vec3d;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * The launch solver replaces a tick by tick search with a closed form, so these walk the same
 * integration minecraft does (move, apply drag, apply gravity) and check the shot actually goes
 * where the solver claims it does.
 */
public class ProjectileSolverTest {

  private static final double ARROW_DRAG = 0.99D;
  private static final double ARROW_GRAVITY = 0.05D;

  private static final Vec3d SHOOT_POS = new Vec3d(0.5D, 65.0D, 0.5D);

  /**
   * Projectile#isNull reads Items.BOW, which throws until the item registry has been built
   */
  @BeforeClass
  public static void bootstrapItemRegistry() {
    Bootstrap.register();
  }

  /**
   * Simulate the shot the same way EntityArrow does and return how close it came to the target
   */
  private static double closestApproach(
      Angle angle, double force, Vec3d inherited, Vec3d target, int ticks) {
    Vec3d velocity = angle.getDirectionVector().normalize().scale(force).add(inherited);
    Vec3d position = SHOOT_POS;
    double closest = position.distanceTo(target);

    for (int i = 0; i < ticks; i++) {
      Vec3d next = position.add(velocity);
      closest = Math.min(closest, distanceToSegment(target, position, next));
      position = next;
      velocity =
          new Vec3d(
              velocity.x * ARROW_DRAG,
              velocity.y * ARROW_DRAG - ARROW_GRAVITY,
              velocity.z * ARROW_DRAG);
    }
    return closest;
  }

  private static double distanceToSegment(Vec3d point, Vec3d start, Vec3d end) {
    Vec3d line = end.subtract(start);
    double lengthSquared = line.lengthSquared();
    if (lengthSquared <= 0.D) {
      return point.distanceTo(start);
    }
    double t = point.subtract(start).dotProduct(line) / lengthSquared;
    t = Math.max(0.D, Math.min(1.D, t));
    return point.distanceTo(start.add(line.scale(t)));
  }

  private static void assertLandsOn(Vec3d target, Vec3d inherited) {
    double force = Projectile.BOW.getForce(20);
    Projectile.LaunchSolution solution =
        Projectile.BOW.solveLaunch(SHOOT_POS, target, force, inherited);

    assertNotNull("no solution for " + target, solution);

    double miss = closestApproach(solution.getAngle(), force, inherited, target, 200);
    assertTrue("missed " + target + " by " + miss, miss < 0.05D);
  }

  @Test
  public void fullChargeMatchesVanillaArrowSpeed() {
    // ItemBow#getArrowVelocity clamps at 1 and the arrow is shot at velocity * 3
    assertTrue(Math.abs(Projectile.BOW.getForce(20) - 3.0D) < 1.0E-9D);
    assertTrue(Projectile.BOW.getForce(10) < Projectile.BOW.getForce(20));
    assertTrue(Projectile.BOW.getForce(40) == Projectile.BOW.getForce(20));
  }

  @Test
  public void solvedShotLandsOnALevelTarget() {
    assertLandsOn(new Vec3d(30.0D, 65.0D, 12.0D), Vec3d.ZERO);
  }

  @Test
  public void solvedShotLandsOnATargetAbove() {
    assertLandsOn(new Vec3d(-18.0D, 78.0D, 6.0D), Vec3d.ZERO);
  }

  @Test
  public void solvedShotLandsOnATargetBelow() {
    assertLandsOn(new Vec3d(9.0D, 41.0D, -25.0D), Vec3d.ZERO);
  }

  @Test
  public void solvedShotLandsOnAPointBlankTarget() {
    assertLandsOn(new Vec3d(3.0D, 65.5D, 1.0D), Vec3d.ZERO);
  }

  @Test
  public void solvedShotCancelsOutTheMotionItInheritsFromTheShooter() {
    // sprinting sideways, EntityArrow#shoot adds this on top of the aimed velocity
    assertLandsOn(new Vec3d(35.0D, 66.0D, -4.0D), new Vec3d(0.28D, 0.0D, -0.13D));
    assertLandsOn(new Vec3d(20.0D, 62.0D, 20.0D), new Vec3d(-0.2D, 0.35D, 0.2D));
  }

  @Test
  public void unreachableTargetHasNoSolution() {
    assertNull(
        Projectile.BOW.solveLaunch(
            SHOOT_POS, new Vec3d(4000.0D, 65.0D, 0.0D), Projectile.BOW.getForce(20), Vec3d.ZERO));
  }

  @Test
  public void flightTimeIsWhenTheShotArrives() {
    Vec3d target = new Vec3d(40.0D, 60.0D, 0.0D);
    double force = Projectile.BOW.getForce(20);
    Projectile.LaunchSolution solution =
        Projectile.BOW.solveLaunch(SHOOT_POS, target, force, Vec3d.ZERO);

    assertNotNull(solution);
    // the shot has not arrived a tick early and has already gone past a tick late
    int arrival = (int) Math.ceil(solution.getFlightTicks());
    assertTrue(closestApproach(solution.getAngle(), force, Vec3d.ZERO, target, arrival) < 0.05D);
    assertTrue(closestApproach(solution.getAngle(), force, Vec3d.ZERO, target, arrival - 2) > 0.5D);
  }

  @Test
  public void theFlatTrajectoryIsPickedOverTheLobbedOne() {
    Vec3d target = new Vec3d(25.0D, 65.0D, 0.0D);
    double force = Projectile.BOW.getForce(20);
    Projectile.LaunchSolution solution =
        Projectile.BOW.solveLaunch(SHOOT_POS, target, force, Vec3d.ZERO);

    assertNotNull(solution);
    // a lobbed shot at this range would be well over 45 degrees up and take far longer to land
    assertTrue(
        "expected a flat shot, got " + solution.getAngle(),
        solution.getAngle().normalize().inDegrees().getPitch() > -45.0F);
  }
}
