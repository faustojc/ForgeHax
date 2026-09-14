package com.matt.forgehax.mods;

import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.mod.ToggleMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import static com.matt.forgehax.Helper.getLocalPlayer;

/**
 * Creative-style flight. Vanilla's own flying branch in {@code Player#travel} is what removes
 * gravity and fall damage; its speed contribution is zeroed out so the velocity set here is the
 * only thing moving the player. Movement still goes through {@code move(MoverType.SELF, ..)}, so
 * blocks are collided with normally.
 */
@RegisterMod
public class FlyMod extends ToggleMod {

  private static final float VANILLA_FLYING_SPEED = 0.05F;
  private static final double HORIZONTAL_DRAG = (double) 0.91F;
  private static final double VERTICAL_DRAG = 0.6D;

  public final Setting<Double> horizontalSpeed =
      getCommandStub()
          .builders()
          .<Double>newSettingBuilder()
          .name("x-speed")
          .description("Target horizontal speed in blocks per tick")
          .defaultTo(0.5D)
          .min(0.D)
          .max(5.D)
          .build();

  public final Setting<Double> verticalSpeed =
      getCommandStub()
          .builders()
          .<Double>newSettingBuilder()
          .name("y-speed")
          .description("Target vertical speed in blocks per tick")
          .defaultTo(0.5D)
          .min(0.D)
          .max(5.D)
          .build();

  public final Setting<Boolean> momentum =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("momentum")
          .description("Coast to a stop on released keys instead of halting instantly")
          .defaultTo(true)
          .build();

  private boolean previouslyFlying = false;

  public FlyMod() {
    super(Category.PLAYER, "Fly", false, "Enables flying");
  }

  @Override
  public void onEnabled() {
    LocalPlayer player = getLocalPlayer();
    if (player != null) {
      previouslyFlying = player.getAbilities().flying;
    }
  }

  @Override
  public void onDisabled() {
    LocalPlayer player = getLocalPlayer();
    if (player == null) {
      return;
    }

    Abilities abilities = player.getAbilities();
    abilities.setFlyingSpeed(VANILLA_FLYING_SPEED);
    // leaving this on would keep real creative flight from ever landing
    abilities.flying = previouslyFlying && abilities.mayfly;
    // whatever speed was built up is left alone, so gravity takes over from it the way it does
    // when creative flight is switched off mid-air
  }

  @SubscribeEvent
  public void onMovementInputUpdate(MovementInputUpdateEvent event) {
    LocalPlayer player = getLocalPlayer();
    if (player == null || event.getEntity() != player) {
      return;
    }

    // LivingTickEvent runs before input.tick(); use this tick's input so presses, reversals and
    // releases affect movement immediately instead of driving the previous direction for a tick.
    Input input = event.getInput();
    Abilities abilities = player.getAbilities();
    int vertical = (input.jumping ? 1 : 0) - (input.shiftKeyDown ? 1 : 0);

    // LocalPlayer#aiStep clears the flying flag on landing and sends an abilities packet with it.
    // Re-asserting it while stood on a block would post that packet every single tick, so resting
    // on the ground is left to vanilla - exactly how creative behaves. Jump to take off again.
    if (player.onGround() && vertical <= 0) {
      abilities.setFlyingSpeed(VANILLA_FLYING_SPEED);
      return;
    }

    // mayfly stays untouched: setting it makes vanilla toggle flight on a double jump and send
    // ServerboundPlayerAbilitiesPacket, which tells the server exactly what we are doing
    abilities.flying = true;
    abilities.setFlyingSpeed(0.F);

    Vec3 horizontal =
        horizontalVelocity(
            input.leftImpulse,
            input.forwardImpulse,
            player.getYRot(),
            horizontalSpeed.getAsDouble());

    player.setDeltaMovement(
        flightVelocity(
            player.getDeltaMovement(),
            horizontal,
            input.leftImpulse != 0.F || input.forwardImpulse != 0.F,
            vertical,
            verticalSpeed.getAsDouble(),
            momentum.get()));
  }

  /**
   * Adds acceleration to the velocity already damped by vanilla's previous travel tick.
   * LivingEntity applies horizontal drag after moving; Player applies vertical flight drag.
   * Acceleration = target speed * (1 - drag), so the speed used to move approaches the configured
   * target without snapping to it or applying drag twice. Released axes receive no acceleration;
   * with momentum off they stop immediately instead of coasting.
   */
  static Vec3 flightVelocity(
      Vec3 current, Vec3 target, boolean steering, int vertical, double verticalSpeed,
      boolean momentum) {
    double x = steering ? current.x + target.x * (1.D - HORIZONTAL_DRAG)
        : (momentum ? current.x : 0.D);
    double z = steering ? current.z + target.z * (1.D - HORIZONTAL_DRAG)
        : (momentum ? current.z : 0.D);
    double y = vertical != 0 ? current.y + vertical * verticalSpeed * (1.D - VERTICAL_DRAG)
        : (momentum ? current.y : 0.D);

    return new Vec3(x, y, z);
  }

  /**
   * Rotates the movement input into world space the same way {@code Entity#getInputVector} does,
   * scaled to {@code speed} rather than vanilla's flying speed. Input is normalised, so holding two
   * directions is not faster than one.
   */
  static Vec3 horizontalVelocity(double strafe, double forward, float yawDegrees, double speed) {
    double magnitude = Math.sqrt(strafe * strafe + forward * forward);
    if (magnitude < 1.0E-5D) {
      return Vec3.ZERO;
    }

    double scale = speed / Math.max(magnitude, 1.D);
    strafe *= scale;
    forward *= scale;

    float yaw = yawDegrees * ((float) Math.PI / 180.F);
    float sin = Mth.sin(yaw);
    float cos = Mth.cos(yaw);
    return new Vec3(strafe * cos - forward * sin, 0.D, forward * cos + strafe * sin);
  }
}
