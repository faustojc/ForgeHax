package com.matt.forgehax.mods;

import net.minecraft.world.phys.Vec3;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FlyModVelocityTest {

  private static final double EPSILON = 1.0E-6D;
  private static final double SPEED = 0.5D;

  private static Vec3 move(double strafe, double forward, float yaw) {
    return FlyMod.horizontalVelocity(strafe, forward, yaw, SPEED);
  }

  @Test
  public void noInputMeansNoMovement() {
    assertEquals(Vec3.ZERO, move(0, 0, 0));
    assertEquals(Vec3.ZERO, move(0, 0, 137));
  }

  @Test
  public void forwardFollowsFacing() {
    // yaw 0 faces south (+Z), yaw 90 faces west (-X)
    Vec3 south = move(0, 1, 0);
    assertEquals(0.D, south.x, EPSILON);
    assertEquals(SPEED, south.z, EPSILON);

    Vec3 west = move(0, 1, 90);
    assertEquals(-SPEED, west.x, EPSILON);
    assertEquals(0.D, west.z, EPSILON);
  }

  @Test
  public void backwardsIsTheOppositeOfForwards() {
    Vec3 forward = move(0, 1, 42);
    Vec3 back = move(0, -1, 42);
    assertEquals(-forward.x, back.x, EPSILON);
    assertEquals(-forward.z, back.z, EPSILON);
  }

  @Test
  public void diagonalIsNotFaster() {
    assertEquals(SPEED, move(1, 1, 0).length(), EPSILON);
    assertEquals(SPEED, move(-1, 1, 213).length(), EPSILON);
  }

  @Test
  public void speedIsHonouredExactly() {
    assertEquals(SPEED, move(0, 1, 0).length(), EPSILON);
    assertEquals(2.5D, FlyMod.horizontalVelocity(0, 1, 77, 2.5D).length(), EPSILON);
  }

  @Test
  public void partialImpulseScalesDown() {
    // sneaking damps the impulse rather than normalising it back up to full speed
    assertEquals(SPEED * 0.3D, move(0, 0.3D, 0).length(), EPSILON);
  }

  @Test
  public void verticalIsNeverSetHere() {
    assertEquals(0.D, move(1, 1, 55).y, EPSILON);
  }

  private static final Vec3 COASTING = new Vec3(0.4D, 0.3D, -0.2D);

  private static Vec3 velocity(Vec3 target, int vertical, boolean momentum) {
    return FlyMod.flightVelocity(
        COASTING, target, target.lengthSqr() > 0.D, vertical, SPEED, momentum);
  }

  @Test
  public void startingMovementUsesCreativeFlightAcceleration() {
    Vec3 target = move(1, 1, 0);
    Vec3 result = FlyMod.flightVelocity(Vec3.ZERO, target, true, 1, SPEED, true);
    assertEquals(0.045D, result.horizontalDistance(), EPSILON);
    assertEquals(0.2D, result.y, EPSILON);
  }

  @Test
  public void reversingMovementBrakesBeforeChangingDirection() {
    Vec3 target = move(0, -1, 0);
    Vec3 result = FlyMod.flightVelocity(
        new Vec3(0.D, SPEED, SPEED), target, true, -1, SPEED, true);
    assertEquals(0.455D, result.z, EPSILON);
    assertEquals(0.3D, result.y, EPSILON);
  }

  @Test
  public void zeroConfiguredSpeedAddsNoAcceleration() {
    Vec3 result = FlyMod.flightVelocity(COASTING, Vec3.ZERO, true, 1, 0.D, true);
    assertEquals(COASTING, result);
  }

  @Test
  public void steeringAddsAccelerationToCarriedMomentum() {
    Vec3 target = new Vec3(0.1D, 0.D, 0.9D);
    Vec3 result = velocity(target, 1, true);
    assertEquals(0.409D, result.x, EPSILON);
    assertEquals(-0.119D, result.z, EPSILON);
    assertEquals(0.5D, result.y, EPSILON);
  }

  // Vanilla applies drag after moving. Feed that stored velocity into the next input tick.
  private static Vec3 afterTravel(Vec3 movement) {
    return movement.multiply((double) 0.91F, 0.6D, (double) 0.91F);
  }

  @Test
  public void heldMovementBuildsUpToConfiguredSpeedWithoutOvershooting() {
    for (double speed : new double[] {0.05D, SPEED, 2.5D, 5.D}) {
      for (boolean momentum : new boolean[] {true, false}) {
        Vec3 current = Vec3.ZERO;
        Vec3 target = FlyMod.horizontalVelocity(1, 1, 0, speed);
        double previousHorizontal = 0.D;
        double previousVertical = 0.D;
        for (int tick = 0; tick < 200; tick++) {
          Vec3 movement = FlyMod.flightVelocity(current, target, true, 1, speed, momentum);
          double horizontal = movement.horizontalDistance();
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
    Vec3 current = afterTravel(new Vec3(SPEED, SPEED, 0.D));
    Vec3 released = FlyMod.flightVelocity(current, Vec3.ZERO, false, 0, SPEED, true);
    assertEquals(0.455D, released.x, EPSILON);
    assertEquals(0.3D, released.y, EPSILON);
    Vec3 next = FlyMod.flightVelocity(afterTravel(released), Vec3.ZERO, false, 0, SPEED, true);
    assertEquals(0.41405D, next.x, EPSILON);
    assertEquals(0.18D, next.y, EPSILON);
  }

  @Test
  public void releasingTheKeysKeepsTheSpeedForVanillaDragToEatAway() {
    Vec3 result = velocity(Vec3.ZERO, 0, true);
    assertEquals(COASTING, result);
  }

  @Test
  public void axesCoastIndependently() {
    // Still climbing, but no longer steering: only the vertical accelerates.
    Vec3 climbing = velocity(Vec3.ZERO, 1, true);
    assertEquals(COASTING.x, climbing.x, EPSILON);
    assertEquals(COASTING.z, climbing.z, EPSILON);
    assertEquals(0.5D, climbing.y, EPSILON);

    // Steering flat: only the horizontal accelerates.
    Vec3 flat = velocity(new Vec3(SPEED, 0.D, 0.D), 0, true);
    assertEquals(0.445D, flat.x, EPSILON);
    assertEquals(COASTING.y, flat.y, EPSILON);
  }

  @Test
  public void descendingIsTheOppositeOfClimbing() {
    Vec3 climbing = FlyMod.flightVelocity(Vec3.ZERO, Vec3.ZERO, false, 1, SPEED, true);
    Vec3 descending = FlyMod.flightVelocity(Vec3.ZERO, Vec3.ZERO, false, -1, SPEED, true);
    assertEquals(-climbing.y, descending.y, EPSILON);
  }

  @Test
  public void momentumOffStopsDead() {
    assertEquals(Vec3.ZERO, velocity(Vec3.ZERO, 0, false));
  }

  @Test
  public void momentumOffStillHonoursActiveInput() {
    Vec3 result = velocity(new Vec3(SPEED, 0.D, 0.D), 1, false);
    assertEquals(0.445D, result.x, EPSILON);
    assertEquals(0.5D, result.y, EPSILON);
    assertEquals(COASTING.z, result.z, EPSILON);
  }
}
