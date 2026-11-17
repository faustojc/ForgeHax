package com.matt.forgehax.mods;

import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.mod.ToggleMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.player.PlayerCapabilities;
import net.minecraft.util.MovementInput;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.event.InputUpdateEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import static com.matt.forgehax.Helper.getLocalPlayer;

/**
 * Creative-style flight. Vanilla's own flying branch in {@code EntityPlayer#travel} is what
 * removes gravity and fall damage; its speed contribution is zeroed out so the velocity set here
 * is the only thing moving the player. Movement still goes through {@code move(MoverType.SELF,
 * ..)}, so blocks are collided with normally.
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
    EntityPlayerSP player = getLocalPlayer();
    if (player != null) {
      previouslyFlying = player.capabilities.isFlying;
    }
  }

  @Override
  public void onDisabled() {
    EntityPlayerSP player = getLocalPlayer();
    if (player == null) {
      return;
    }

    PlayerCapabilities abilities = player.capabilities;
    abilities.setFlySpeed(VANILLA_FLYING_SPEED);
    // leaving this on would keep real creative flight from ever landing
    abilities.isFlying = previouslyFlying && abilities.allowFlying;
    // whatever speed was built up is left alone, so gravity takes over from it the way it does
    // when creative flight is switched off mid-air
  }

  @SubscribeEvent
  public void onInputUpdate(InputUpdateEvent event) {
    EntityPlayerSP player = getLocalPlayer();
    if (player == null || event.getEntityPlayer() != player) {
      return;
    }

    // the event fires right after updatePlayerMoveState(); use this tick's input so presses,
    // reversals and releases affect movement immediately instead of driving the previous
    // direction for a tick.
    MovementInput input = event.getMovementInput();
    PlayerCapabilities abilities = player.capabilities;
    int vertical = (input.jump ? 1 : 0) - (input.sneak ? 1 : 0);

    // EntityPlayerSP#onLivingUpdate clears the flying flag on landing and sends an abilities
    // packet with it. Re-asserting it while stood on a block would post that packet every single
    // tick, so resting on the ground is left to vanilla - exactly how creative behaves. Jump to
    // take off again.
    if (player.onGround && vertical <= 0) {
      abilities.setFlySpeed(VANILLA_FLYING_SPEED);
      return;
    }

    // allowFlying stays untouched: setting it makes vanilla toggle flight on a double jump and
    // send CPacketPlayerAbilities, which tells the server exactly what we are doing
    abilities.isFlying = true;
    abilities.setFlySpeed(0.F);

    // updatePlayerMoveState() scales the impulse by 0.3 while sneaking, and vanilla only undoes it
    // later in onLivingUpdate for a flying player (the same (float) (x / 0.3D)). Flight is not
    // slowed by sneaking, so undo it here on local copies; the MovementInput itself is left alone
    // so vanilla's own undo still sees what it expects.
    float strafe = input.sneak ? (float) ((double) input.moveStrafe / 0.3D) : input.moveStrafe;
    float forward = input.sneak ? (float) ((double) input.moveForward / 0.3D) : input.moveForward;

    Vec3d horizontal =
        horizontalVelocity(strafe, forward, player.rotationYaw, horizontalSpeed.getAsDouble());

    Vec3d velocity =
        flightVelocity(
            new Vec3d(player.motionX, player.motionY, player.motionZ),
            horizontal,
            strafe != 0.F || forward != 0.F,
            vertical,
            verticalSpeed.getAsDouble(),
            momentum.get());
    player.motionX = velocity.x;
    player.motionY = velocity.y;
    player.motionZ = velocity.z;
  }

  /**
   * Adds acceleration to the velocity already damped by vanilla's previous travel tick.
   * EntityLivingBase applies horizontal drag after moving; EntityPlayer applies vertical flight
   * drag. Acceleration = target speed * (1 - drag), so the speed used to move approaches the
   * configured target without snapping to it or applying drag twice. Released axes receive no
   * acceleration; with momentum off they stop immediately instead of coasting.
   */
  static Vec3d flightVelocity(
      Vec3d current, Vec3d target, boolean steering, int vertical, double verticalSpeed,
      boolean momentum) {
    double x = steering ? current.x + target.x * (1.D - HORIZONTAL_DRAG)
        : (momentum ? current.x : 0.D);
    double z = steering ? current.z + target.z * (1.D - HORIZONTAL_DRAG)
        : (momentum ? current.z : 0.D);
    double y = vertical != 0 ? current.y + vertical * verticalSpeed * (1.D - VERTICAL_DRAG)
        : (momentum ? current.y : 0.D);

    return new Vec3d(x, y, z);
  }

  /**
   * Rotates the movement input into world space the same way {@code Entity#moveRelative} does,
   * scaled to {@code speed} rather than vanilla's flying speed. Input is normalised, so holding two
   * directions is not faster than one.
   */
  static Vec3d horizontalVelocity(double strafe, double forward, float yawDegrees, double speed) {
    double magnitude = Math.sqrt(strafe * strafe + forward * forward);
    if (magnitude < 1.0E-5D) {
      return Vec3d.ZERO;
    }

    double scale = speed / Math.max(magnitude, 1.D);
    strafe *= scale;
    forward *= scale;

    float yaw = yawDegrees * ((float) Math.PI / 180.F);
    float sin = MathHelper.sin(yaw);
    float cos = MathHelper.cos(yaw);
    return new Vec3d(strafe * cos - forward * sin, 0.D, forward * cos + strafe * sin);
  }
}
