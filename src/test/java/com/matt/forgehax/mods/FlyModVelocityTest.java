package com.matt.forgehax.mods;

import net.minecraft.util.math.Vec3d;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FlyModVelocityTest {

  private static final double EPSILON = 1.0E-6D;
  private static final double SPEED = 0.5D;

  // Vec3d has no horizontalDistance()
  private static double horizontalDistance(Vec3d v) {
    return Math.sqrt(v.x * v.x + v.z * v.z);
  }

  private static Vec3d move(double strafe, double forward, float yaw) {
    return FlyMod.horizontalVelocity(strafe, forward, yaw, SPEED);
  }

  @Test
  public void noInputMeansNoMovement() {
    assertEquals(Vec3d.ZERO, move(0, 0, 0));
    assertEquals(Vec3d.ZERO, move(0, 0, 137));
  }

  @Test
  public void forwardFollowsFacing() {
    // yaw 0 faces south (+Z), yaw 90 faces west (-X)
    Vec3d south = move(0, 1, 0);
    assertEquals(0.D, south.x, EPSILON);
    assertEquals(SPEED, south.z, EPSILON);

    Vec3d west = move(0, 1, 90);
    assertEquals(-SPEED, west.x, EPSILON);
    assertEquals(0.D, west.z, EPSILON);
  }

  @Test
  public void backwardsIsTheOppositeOfForwards() {
    Vec3d forward = move(0, 1, 42);
    Vec3d back = move(0, -1, 42);
    assertEquals(-forward.x, back.x, EPSILON);
    assertEquals(-forward.z, back.z, EPSILON);
  }

  @Test
  public void diagonalIsNotFaster() {
    assertEquals(SPEED, move(1, 1, 0).lengthVector(), EPSILON);
    assertEquals(SPEED, move(-1, 1, 213).lengthVector(), EPSILON);
  }

  @Test
  public void speedIsHonouredExactly() {
    assertEquals(SPEED, move(0, 1, 0).lengthVector(), EPSILON);
    assertEquals(2.5D, FlyMod.horizontalVelocity(0, 1, 77, 2.5D).lengthVector(), EPSILON);
  }

  @Test
  public void partialImpulseScalesDown() {
    // sneaking damps the impulse rather than normalising it back up to full speed
    assertEquals(SPEED * 0.3D, move(0, 0.3D, 0).lengthVector(), EPSILON);
  }

  @Test
  public void verticalIsNeverSetHere() {
    assertEquals(0.D, move(1, 1, 55).y, EPSILON);
  }

  private static final Vec3d COASTING = new Vec3d(0.4D, 0.3D, -0.2D);

  private static Vec3d velocity(Vec3d target, int vertical, boolean momentum) {
    return FlyMod.flightVelocity(
        COASTING, target, target.lengthSquared() > 0.D, vertical, SPEED, momentum);
  }

  @Test
  public void startingMovementUsesCreativeFlightAcceleration() {
    Vec3d target = move(1, 1, 0);
    Vec3d result = FlyMod.flightVelocity(Vec3d.ZERO, target, true, 1, SPEED, true);
    assertEquals(0.045D, horizontalDistance(result), EPSILON);
    assertEquals(0.2D, result.y, EPSILON);
  }

  @Test
  public void reversingMovementBrakesBeforeChangingDirection() {
    Vec3d target = move(0, -1, 0);
    Vec3d result = FlyMod.flightVelocity(
        new Vec3d(0.D, SPEED, SPEED), target, true, -1, SPEED, true);
    assertEquals(0.455D, result.z, EPSILON);
    assertEquals(0.3D, result.y, EPSILON);
  }

  @Test
  public void zeroConfiguredSpeedAddsNoAcceleration() {
    Vec3d result = FlyMod.flightVelocity(COASTING, Vec3d.ZERO, true, 1, 0.D, true);
    assertEquals(COASTING, result);
  }

  @Test
  public void steeringAddsAccelerationToCarriedMomentum() {
    Vec3d target = new Vec3d(0.1D, 0.D, 0.9D);
    Vec3d result = velocity(target, 1, true);
    assertEquals(0.409D, result.x, EPSILON);
    assertEquals(-0.119D, result.z, EPSILON);
    assertEquals(0.5D, result.y, EPSILON);
  }

  // Vanilla applies drag after moving. Feed that stored velocity into the next input tick.
  private static Vec3d afterTravel(Vec3d movement) {
    return new Vec3d(movement.x * (double) 0.91F, movement.y * 0.6D, movement.z * (double) 0.91F);
  }

  @Test
  public void heldMovementBuildsUpToConfiguredSpeedWithoutOvershooting() {
    for (double speed : new double[] {0.05D, SPEED, 2.5D, 5.D}) {
      for (boolean momentum : new boolean[] {true, false}) {
        Vec3d current = Vec3d.ZERO;
        Vec3d target = FlyMod.horizontalVelocity(1, 1, 0, speed);
        double previousHorizontal = 0.D;
        double previousVertical = 0.D;
        for (int tick = 0; tick < 200; tick++) {
          Vec3d movement = FlyMod.flightVelocity(current, target, true, 1, speed, momentum);
          double horizontal = horizontalDistance(movement);
          assertTrue(horizontal > previousHorizontal);
          assertTrue(horizontal <= speed + EPSILON);
          assertTrue(movement.y >= previousVertical);
          assertTrue(movement.y <= speed + EPSILON);
          previousHorizontal = horizontal;
          previousVertical = movement.y;
          current = afterTravel(movement);
        }
        assertEquals(speed, previousHorizontal, EPSILON);
        assertEquals(speed, previousVertical, EPSILON);
      }
    }
  }

  @Test
  public void releasingMovementCoastsDownWithCreativeFlightDrag() {
    Vec3d current = afterTravel(new Vec3d(SPEED, SPEED, 0.D));
    Vec3d released = FlyMod.flightVelocity(current, Vec3d.ZERO, false, 0, SPEED, true);
    assertEquals(0.455D, released.x, EPSILON);
    assertEquals(0.3D, released.y, EPSILON);
    Vec3d next = FlyMod.flightVelocity(afterTravel(released), Vec3d.ZERO, false, 0, SPEED, true);
    assertEquals(0.41405D, next.x, EPSILON);
    assertEquals(0.18D, next.y, EPSILON);
  }

  @Test
  public void releasingTheKeysKeepsTheSpeedForVanillaDragToEatAway() {
    Vec3d result = velocity(Vec3d.ZERO, 0, true);
    assertEquals(COASTING, result);
  }

  @Test
  public void axesCoastIndependently() {
    // Still climbing, but no longer steering: only the vertical accelerates.
    Vec3d climbing = velocity(Vec3d.ZERO, 1, true);
    assertEquals(COASTING.x, climbing.x, EPSILON);
    assertEquals(COASTING.z, climbing.z, EPSILON);
    assertEquals(0.5D, climbing.y, EPSILON);

    // Steering flat: only the horizontal accelerates.
    Vec3d flat = velocity(new Vec3d(SPEED, 0.D, 0.D), 0, true);
    assertEquals(0.445D, flat.x, EPSILON);
    assertEquals(COASTING.y, flat.y, EPSILON);
  }

  @Test
  public void descendingIsTheOppositeOfClimbing() {
    Vec3d climbing = FlyMod.flightVelocity(Vec3d.ZERO, Vec3d.ZERO, false, 1, SPEED, true);
    Vec3d descending = FlyMod.flightVelocity(Vec3d.ZERO, Vec3d.ZERO, false, -1, SPEED, true);
    assertEquals(-climbing.y, descending.y, EPSILON);
  }

  @Test
  public void momentumOffStopsDead() {
    assertEquals(Vec3d.ZERO, velocity(Vec3d.ZERO, 0, false));
  }

  @Test
  public void momentumOffStillHonoursActiveInput() {
    Vec3d result = velocity(new Vec3d(SPEED, 0.D, 0.D), 1, false);
    assertEquals(0.445D, result.x, EPSILON);
    assertEquals(0.5D, result.y, EPSILON);
    assertEquals(COASTING.z, result.z, EPSILON);
  }
}
