package com.matt.forgehax.util.projectile;

import com.matt.forgehax.util.math.Angle;
import net.minecraft.world.phys.Vec3;
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

  private static final Vec3 SHOOT_POS = new Vec3(0.5D, 65.0D, 0.5D);

  /**
   * Simulate the shot the same way EntityArrow does and return how close it came to the target
   */
  private static double closestApproach(
      Angle angle, double force, Vec3 inherited, Vec3 target, int ticks) {
    Vec3 velocity = angle.getDirectionVector().normalize().scale(force).add(inherited);
    Vec3 position = SHOOT_POS;
    double closest = position.distanceTo(target);

    for (int i = 0; i < ticks; i++) {
      Vec3 next = position.add(velocity);
      closest = Math.min(closest, distanceToSegment(target, position, next));
      position = next;
      velocity =
          new Vec3(
              velocity.x * ARROW_DRAG,
              velocity.y * ARROW_DRAG - ARROW_GRAVITY,
              velocity.z * ARROW_DRAG);
    }
    return closest;
  }

  private static double distanceToSegment(Vec3 point, Vec3 start, Vec3 end) {
    Vec3 line = end.subtract(start);
    double lengthSquared = line.lengthSqr();
    if (lengthSquared <= 0.D) {
      return point.distanceTo(start);
    }
    double t = point.subtract(start).dot(line) / lengthSquared;
    t = Math.max(0.D, Math.min(1.D, t));
    return point.distanceTo(start.add(line.scale(t)));
  }

  private static void assertLandsOn(Vec3 target, Vec3 inherited) {
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
    assertLandsOn(new Vec3(30.0D, 65.0D, 12.0D), Vec3.ZERO);
  }

  @Test
  public void solvedShotLandsOnATargetAbove() {
    assertLandsOn(new Vec3(-18.0D, 78.0D, 6.0D), Vec3.ZERO);
  }

  @Test
  public void solvedShotLandsOnATargetBelow() {
    assertLandsOn(new Vec3(9.0D, 41.0D, -25.0D), Vec3.ZERO);
  }

  @Test
  public void solvedShotLandsOnAPointBlankTarget() {
    assertLandsOn(new Vec3(3.0D, 65.5D, 1.0D), Vec3.ZERO);
  }

  @Test
  public void solvedShotCancelsOutTheMotionItInheritsFromTheShooter() {
    // sprinting sideways, EntityArrow#shoot adds this on top of the aimed velocity
    assertLandsOn(new Vec3(35.0D, 66.0D, -4.0D), new Vec3(0.28D, 0.0D, -0.13D));
    assertLandsOn(new Vec3(20.0D, 62.0D, 20.0D), new Vec3(-0.2D, 0.35D, 0.2D));
  }

  @Test
  public void unreachableTargetHasNoSolution() {
    assertNull(
        Projectile.BOW.solveLaunch(
            SHOOT_POS, new Vec3(4000.0D, 65.0D, 0.0D), Projectile.BOW.getForce(20), Vec3.ZERO));
  }

  @Test
  public void flightTimeIsWhenTheShotArrives() {
    Vec3 target = new Vec3(40.0D, 60.0D, 0.0D);
    double force = Projectile.BOW.getForce(20);
    Projectile.LaunchSolution solution =
        Projectile.BOW.solveLaunch(SHOOT_POS, target, force, Vec3.ZERO);

    assertNotNull(solution);
    // the shot has not arrived a tick early and has already gone past a tick late
    int arrival = (int) Math.ceil(solution.getFlightTicks());
    assertTrue(closestApproach(solution.getAngle(), force, Vec3.ZERO, target, arrival) < 0.05D);
    assertTrue(closestApproach(solution.getAngle(), force, Vec3.ZERO, target, arrival - 2) > 0.5D);
  }

  @Test
  public void theFlatTrajectoryIsPickedOverTheLobbedOne() {
    Vec3 target = new Vec3(25.0D, 65.0D, 0.0D);
    double force = Projectile.BOW.getForce(20);
    Projectile.LaunchSolution solution =
        Projectile.BOW.solveLaunch(SHOOT_POS, target, force, Vec3.ZERO);

    assertNotNull(solution);
    // a lobbed shot at this range would be well over 45 degrees up and take far longer to land
    assertTrue(
        "expected a flat shot, got " + solution.getAngle(),
        solution.getAngle().normalize().inDegrees().getPitch() > -45.0F);
  }
}
